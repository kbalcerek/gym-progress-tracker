package com.example.gymprogresstracker.ui.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogresstracker.R
import com.example.gymprogresstracker.data.Exercise
import com.example.gymprogresstracker.data.GymRepository
import com.example.gymprogresstracker.data.WorkoutSet
import com.example.gymprogresstracker.data.WorkoutSetView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parseReps(input: String): List<Int>? {
    val tokens = input.trim().split(Regex("[,\\s]+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    val reps = tokens.map { it.toIntOrNull() ?: return null }
    return if (reps.all { it > 0 }) reps else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogScreen(
    repository: GymRepository,
    onManageExercises: () -> Unit,
    vm: WorkoutLogViewModel = viewModel(factory = WorkoutLogViewModel.factory(repository))
) {
    val days by vm.workoutDays.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<WorkoutSetView?>(null) }
    var deletingSet by remember { mutableStateOf<WorkoutSet?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.nav_log)) },
            actions = {
                IconButton(onClick = onManageExercises) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = stringResource(R.string.nav_exercises))
                }
            }
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (days.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_data),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    days.forEach { day ->
                        stickyHeader(key = "date_${day.date}") {
                            DayHeader(date = day.date)
                        }
                        day.exercises.forEach { exerciseGroup ->
                            item(key = "group_${day.date}_${exerciseGroup.exerciseId}") {
                                ExerciseGroupItem(
                                    group = exerciseGroup,
                                    onEdit = { editingSet = it },
                                    onDelete = { setView ->
                                        deletingSet = WorkoutSet(
                                            id = setView.id,
                                            date = setView.date,
                                            exerciseId = setView.exerciseId,
                                            weightKg = setView.weightKg,
                                            reps = setView.reps,
                                            notes = setView.notes,
                                            setOrder = setView.setOrder
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_set))
            }
        }
    }

    if (showAddDialog) {
        AddEditSetDialog(
            exercises = exercises,
            onDismiss = { showAddDialog = false },
            onSave = { date, exerciseId, exerciseName, weight, repsList, notes ->
                vm.addSet(date, exerciseId, exerciseName, weight, repsList, notes)
                showAddDialog = false
            }
        )
    }

    editingSet?.let { sv ->
        AddEditSetDialog(
            exercises = exercises,
            initial = sv,
            onDismiss = { editingSet = null },
            onSave = { date, exerciseId, exerciseName, weight, repsList, notes ->
                vm.updateSet(
                    WorkoutSet(
                        id = sv.id,
                        date = date,
                        exerciseId = sv.exerciseId,
                        weightKg = weight,
                        reps = repsList.first(),
                        notes = notes,
                        setOrder = sv.setOrder
                    ),
                    exerciseId = exerciseId,
                    exerciseName = exerciseName
                )
                editingSet = null
            }
        )
    }

    deletingSet?.let { s ->
        AlertDialog(
            onDismissRequest = { deletingSet = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteSet(s); deletingSet = null }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSet = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    Text(
        text = date.format(dateFormatter),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
    HorizontalDivider()
}

private fun formatWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) "${kg.toInt()}" else "${kg}"
    // if (kg % 1.0 == 0.0) "${kg.toInt()}kg" else "${kg}kg"

@Composable
private fun ExerciseGroupItem(
    group: WorkoutExerciseGroup,
    onEdit: (WorkoutSetView) -> Unit,
    onDelete: (WorkoutSetView) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = group.sets.joinToString(", ") { formatWeight(it.weightKg) },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = group.sets.joinToString(", ") { it.reps.toString() },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = group.sets.mapNotNull { it.notes.takeIf(String::isNotBlank) }.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.2f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
        if (expanded) {
            group.sets.forEach { setView ->
                SetRow(
                    setView = setView,
                    onEdit = { onEdit(setView) },
                    onDelete = { onDelete(setView) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetRow(
    setView: WorkoutSetView,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onEdit, onLongClick = onDelete)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Set ${setView.setOrder + 1}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = "${formatWeight(setView.weightKg)} × ${setView.reps}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (setView.notes.isNotBlank()) {
            Text(
                text = setView.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSetDialog(
    exercises: List<Exercise>,
    initial: WorkoutSetView? = null,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Long?, String, Double, List<Int>, String) -> Unit
) {
    val isEditing = initial != null
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var selectedExercise by remember { mutableStateOf(exercises.find { it.id == initial?.exerciseId }) }
    var exerciseExpanded by remember { mutableStateOf(false) }
    var exerciseQuery by remember { mutableStateOf(initial?.exerciseName ?: "") }
    var weight by rememberSaveable { mutableStateOf(initial?.weightKg?.toString() ?: "") }
    var reps by rememberSaveable { mutableStateOf(initial?.reps?.toString() ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var showErrors by remember { mutableStateOf(false) }

    val filteredExercises = remember(exerciseQuery, exercises) {
        if (exerciseQuery.isBlank()) exercises
        else exercises.filter { it.name.contains(exerciseQuery, ignoreCase = true) }
    }
    val resolvedExercise = selectedExercise ?: exercises.find {
        it.name.equals(exerciseQuery.trim(), ignoreCase = true)
    }
    val showCreateOption = exerciseQuery.isNotBlank() && resolvedExercise == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) stringResource(R.string.add_set) else stringResource(R.string.edit_set)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Date row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedDate.format(dateFormatter),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.date))
                    }
                }

                // Exercise dropdown
                ExposedDropdownMenuBox(
                    expanded = exerciseExpanded,
                    onExpandedChange = { exerciseExpanded = it }
                ) {
                    OutlinedTextField(
                        value = exerciseQuery,
                        onValueChange = {
                            exerciseQuery = it
                            exerciseExpanded = true
                            selectedExercise = null
                        },
                        label = { Text(stringResource(R.string.exercise)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exerciseExpanded) },
                        isError = showErrors && exerciseQuery.isBlank(),
                        supportingText = if (showErrors && exerciseQuery.isBlank()) {
                            { Text(stringResource(R.string.error_exercise_required)) }
                        } else null,
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                            .fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = exerciseExpanded && (filteredExercises.isNotEmpty() || showCreateOption),
                        onDismissRequest = { exerciseExpanded = false }
                    ) {
                        if (showCreateOption) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_new_exercise_option, exerciseQuery.trim())) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    exerciseQuery = exerciseQuery.trim()
                                    exerciseExpanded = false
                                }
                            )
                        }
                        filteredExercises.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text(ex.name) },
                                onClick = {
                                    selectedExercise = ex
                                    exerciseQuery = ex.name
                                    exerciseExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(stringResource(R.string.weight_kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = showErrors && weight.toDoubleOrNull() == null,
                    supportingText = if (showErrors && weight.toDoubleOrNull() == null) {
                        { Text(stringResource(R.string.error_weight_invalid)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isEditing) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text(stringResource(R.string.reps)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showErrors && reps.toIntOrNull() == null,
                        supportingText = if (showErrors && reps.toIntOrNull() == null) {
                            { Text(stringResource(R.string.error_reps_invalid)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text(stringResource(R.string.reps)) },
                        placeholder = { Text(stringResource(R.string.reps_multi_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError = showErrors && parseReps(reps) == null,
                        supportingText = if (showErrors && parseReps(reps) == null) {
                            { Text(stringResource(R.string.error_reps_invalid_multi)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weight.toDoubleOrNull()
                    val repsList = if (isEditing) reps.toIntOrNull()?.let(::listOf) else parseReps(reps)
                    if (exerciseQuery.isBlank() || w == null || repsList == null) {
                        showErrors = true
                        return@TextButton
                    }
                    onSave(selectedDate, resolvedExercise?.id, exerciseQuery.trim(), w, repsList, notes.trim())
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
