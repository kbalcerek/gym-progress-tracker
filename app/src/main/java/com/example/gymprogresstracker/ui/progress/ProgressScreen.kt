package com.example.gymprogresstracker.ui.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogresstracker.R
import com.example.gymprogresstracker.data.BodyweightEntry
import com.example.gymprogresstracker.data.Exercise
import com.example.gymprogresstracker.data.ExerciseChartPoint
import com.example.gymprogresstracker.data.GymRepository
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val axisDateFormatter = DateTimeFormatter.ofPattern("MM/dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    repository: GymRepository,
    vm: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(repository))
) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val bodyweightEntries by vm.bodyweightEntries.collectAsStateWithLifecycle()
    val exercisePoints by vm.exerciseChartPoints.collectAsStateWithLifecycle()
    val selectedExerciseId by vm.selectedExerciseId.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_progress)) })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Bodyweight chart
            Text(
                text = stringResource(R.string.progress_bodyweight_chart),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (bodyweightEntries.size >= 2) {
                BodyweightChart(entries = bodyweightEntries)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_data))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Exercise chart
            Text(
                text = stringResource(R.string.progress_exercise_chart),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExerciseSelector(
                exercises = exercises,
                selectedId = selectedExerciseId,
                onSelect = { vm.selectExercise(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (exercisePoints.size >= 2) {
                ExerciseWeightChart(points = exercisePoints)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_data))
                }
            }
        }
    }
}

@Composable
private fun BodyweightChart(entries: List<BodyweightEntry>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(entries) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = entries.map { it.date.toEpochDay().toFloat() },
                    y = entries.map { it.weightKg.toFloat() }
                )
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { context, x, _ ->
                    LocalDate.ofEpochDay(x.toLong()).format(axisDateFormatter)
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun ExerciseWeightChart(points: List<ExerciseChartPoint>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = points.map { it.date.toEpochDay().toFloat() },
                    y = points.map { it.maxWeightKg.toFloat() }
                )
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { context, x, _ ->
                    LocalDate.ofEpochDay(x.toLong()).format(axisDateFormatter)
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelector(
    exercises: List<Exercise>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = exercises.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: stringResource(R.string.select_exercise),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.exercise)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            exercises.forEach { ex ->
                DropdownMenuItem(
                    text = { Text(ex.name) },
                    onClick = {
                        onSelect(ex.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
