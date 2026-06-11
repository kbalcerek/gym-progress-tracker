package com.example.gymprogresstracker.ui.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        Box(modifier = Modifier.weight(1f)) {
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
                            item(key = "exGroup_${day.date}_${exerciseGroup.exerciseId}") {
                                ExerciseGroupHeader(name = exerciseGroup.exerciseName)
                            }
                            items(exerciseGroup.sets, key = { "set_${it.id}" }) { setView ->
                                SetRow(
                                    setView = setView,
                                    onEdit = { editingSet = setView },
                                    onDelete = {
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
            onSave = { date, exerciseId, weight, reps, notes ->
                vm.addSet(date, exerciseId, weight, reps, notes)
                showAddDialog = false
            }
        )
    }

    editingSet?.let { sv ->
        AddEditSetDialog(
            exercises = exercises,
            initial = sv,
            onDismiss = { editingSet = null },
            onSave = { date, exerciseId, weight, reps, notes ->
                vm.updateSet(WorkoutSet(
                    id = sv.id,
                    date = date,
                    exerciseId = exerciseId,
                    weightKg = weight,
                    reps = reps,
                    notes = notes,
                    setOrder = sv.setOrder
                ))
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

@Composable
private fun ExerciseGroupHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
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
            text = "${setView.weightKg} kg × ${setView.reps}",
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
    onSave: (LocalDate, Long, Double, Int, String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var selectedExercise by remember { mutableStateOf(exercises.find { it.id == initial?.exerciseId }) }
    var exerciseExpanded by remember { mutableStateOf(false) }
    var exerciseQuery by remember { mutableStateOf(initial?.exerciseName ?: "") }
    var weight by rememberSaveable { mutableStateOf(initial?.weightKg?.toString() ?: "") }
    var reps by rememberSaveable { mutableStateOf(initial?.reps?.toString() ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }

    val filteredExercises = remember(exerciseQuery, exercises) {
        if (exerciseQuery.isBlank()) exercises
        else exercises.filter { it.name.contains(exerciseQuery, ignoreCase = true) }
    }

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
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                            .fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = exerciseExpanded && filteredExercises.isNotEmpty(),
                        onDismissRequest = { exerciseExpanded = false }
                    ) {
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text(stringResource(R.string.reps)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                    val ex = selectedExercise ?: exercises.find {
                        it.name.equals(exerciseQuery.trim(), ignoreCase = true)
                    } ?: return@TextButton
                    val w = weight.toDoubleOrNull() ?: return@TextButton
                    val r = reps.toIntOrNull() ?: return@TextButton
                    onSave(selectedDate, ex.id, w, r, notes.trim())
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
