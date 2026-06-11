package com.example.gymprogresstracker.data.sync

import android.net.Uri
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.example.gymprogresstracker.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class DropboxAuthManager(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("dropbox_access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("dropbox_refresh_token")
        private val EXPIRES_AT = longPreferencesKey("dropbox_expires_at")
        val LAST_BACKUP = longPreferencesKey("dropbox_last_backup")

        val requestConfig: DbxRequestConfig by lazy {
            DbxRequestConfig.newBuilder("GymProgressTracker/1.0").build()
        }

        // Held in memory during the auth redirect round-trip
        private var pendingVerifier: String? = null
    }

    val isLinked: Flow<Boolean> = dataStore.data.map { it[ACCESS_TOKEN] != null }
    val lastBackupTs: Flow<Long?> = dataStore.data.map { it[LAST_BACKUP] }

    fun buildAuthUrl(): String? {
        val appKey = BuildConfig.DROPBOX_APP_KEY
        if (appKey == "placeholder") return null
        val verifier = generateVerifier()
        pendingVerifier = verifier
        val challenge = sha256Base64Url(verifier)
        val redirectUri = "db-$appKey://2/token"
        return "https://www.dropbox.com/oauth2/authorize" +
            "?client_id=$appKey" +
            "&response_type=code" +
            "&redirect_uri=${Uri.encode(redirectUri)}" +
            "&code_challenge=${Uri.encode(challenge)}" +
            "&code_challenge_method=S256" +
            "&token_access_type=offline"
    }

    suspend fun handleAuthCode(code: String): Boolean {
        val appKey = BuildConfig.DROPBOX_APP_KEY
        val verifier = pendingVerifier ?: return false
        val redirectUri = "db-$appKey://2/token"
        return withContext(Dispatchers.IO) {
            try {
                val response = exchangeCode(code, appKey, redirectUri, verifier) ?: return@withContext false
                dataStore.edit {
                    it[ACCESS_TOKEN] = response.accessToken
                    if (response.refreshToken != null) it[REFRESH_TOKEN] = response.refreshToken
                    it[EXPIRES_AT] = System.currentTimeMillis() + (response.expiresInSecs ?: 14400L) * 1000L
                }
                pendingVerifier = null
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun getClient(): DbxClientV2? = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        var accessToken = prefs[ACCESS_TOKEN] ?: return@withContext null
        val expiresAt = prefs[EXPIRES_AT] ?: 0L

        if (System.currentTimeMillis() >= expiresAt - 60_000L) {
            val refreshToken = prefs[REFRESH_TOKEN]
            val appKey = BuildConfig.DROPBOX_APP_KEY
            if (!refreshToken.isNullOrEmpty()) {
                val refreshed = refreshAccessToken(refreshToken, appKey)
                if (refreshed != null) {
                    dataStore.edit {
                        it[ACCESS_TOKEN] = refreshed.accessToken
                        it[EXPIRES_AT] = System.currentTimeMillis() + (refreshed.expiresInSecs ?: 14400L) * 1000L
                    }
                    accessToken = refreshed.accessToken
                } else {
                    dataStore.edit { it.remove(ACCESS_TOKEN) }
                    return@withContext null
                }
            }
        }
        DbxClientV2(requestConfig, accessToken)
    }

    suspend fun unlink() {
        dataStore.edit {
            it.remove(ACCESS_TOKEN)
            it.remove(REFRESH_TOKEN)
            it.remove(EXPIRES_AT)
        }
    }

    suspend fun recordBackup() {
        dataStore.edit { it[LAST_BACKUP] = System.currentTimeMillis() }
    }

    private fun generateVerifier(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun sha256Base64Url(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private data class TokenResponse(
        val accessToken: String,
        val refreshToken: String?,
        val expiresInSecs: Long?
    )

    private fun post(urlStr: String, body: String): String? {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.outputStream.write(body.toByteArray(Charsets.UTF_8))
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null
        } finally {
            conn.disconnect()
        }
    }

    private val lenientJson = Json { ignoreUnknownKeys = true }

    private fun parseTokenResponse(json: String): TokenResponse? = try {
        val obj = lenientJson.parseToJsonElement(json).jsonObject
        val access = obj["access_token"]?.jsonPrimitive?.content ?: return null
        val refresh = obj["refresh_token"]?.jsonPrimitive?.content
        val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull
        TokenResponse(access, refresh, expiresIn)
    } catch (e: Exception) { null }

    private fun exchangeCode(code: String, appKey: String, redirectUri: String, verifier: String): TokenResponse? {
        val body = "code=${URLEncoder.encode(code, "UTF-8")}" +
            "&grant_type=authorization_code" +
            "&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}" +
            "&code_verifier=${URLEncoder.encode(verifier, "UTF-8")}" +
            "&client_id=${URLEncoder.encode(appKey, "UTF-8")}"
        return post("https://api.dropboxapi.com/oauth2/token", body)?.let { parseTokenResponse(it) }
    }

    private fun refreshAccessToken(refreshToken: String, appKey: String): TokenResponse? {
        val body = "grant_type=refresh_token" +
            "&refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}" +
            "&client_id=${URLEncoder.encode(appKey, "UTF-8")}"
        return post("https://api.dropboxapi.com/oauth2/token", body)?.let { parseTokenResponse(it) }
    }
}
