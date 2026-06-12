package com.example.gymprogresstracker.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogresstracker.R
import com.example.gymprogresstracker.data.BodyweightEntry
import com.example.gymprogresstracker.data.Exercise
import com.example.gymprogresstracker.data.GymRepository
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.shape.rounded
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val axisDateFormatter = DateTimeFormatter.ofPattern("MM/dd")

private val seriesColorPalette = listOf(
    Color(0xFF1E88E5),
    Color(0xFFE53935),
    Color(0xFF43A047),
    Color(0xFFFB8C00),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFF6D4C41),
    Color(0xFFD81B60),
)

private fun seriesColor(colorIndex: Int): Color = seriesColorPalette[colorIndex % seriesColorPalette.size]

private val markerInfoKey = object : ExtraStore.Key<Map<Double, List<String>>>() {}

private val scatterRangeProvider = object : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val padding = if (minY == maxY) minY * 0.1 else (maxY - minY) * 0.1
        return (minY - padding).coerceAtLeast(0.0)
    }
    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val padding = if (minY == maxY) minY * 0.1 else (maxY - minY) * 0.1
        return maxY + padding
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    repository: GymRepository,
    vm: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(repository))
) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val bodyweightEntries by vm.bodyweightEntries.collectAsStateWithLifecycle()
    val exerciseSeries by vm.exerciseSeries.collectAsStateWithLifecycle()
    val selectedExerciseIds by vm.selectedExerciseIds.collectAsStateWithLifecycle()

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
            ExerciseMultiSelector(
                exercises = exercises,
                selectedIds = selectedExerciseIds,
                onToggle = { vm.toggleExercise(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (exerciseSeries.isNotEmpty()) {
                ChartLegend(series = exerciseSeries)
                Spacer(modifier = Modifier.height(4.dp))
                ExerciseScatterChart(series = exerciseSeries)
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
                valueFormatter = CartesianValueFormatter { _, x, _ ->
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

// Represents one Vico series: the k-th entry from each day of a given exercise.
// Splitting same-day entries across slots avoids Vico's Map<x,entry> deduplication
// (which keeps only the last value per x within a single series).
private data class SlottedSeries(val colorIndex: Int, val points: List<Pair<Long, Float>>)

private fun buildSlottedSeries(series: List<ExerciseSeriesData>): List<SlottedSeries> =
    series.flatMap { s ->
        val byDay = s.points.groupBy { it.date.toEpochDay() }
        val maxSlots = byDay.values.maxOfOrNull { it.size } ?: 0
        (0 until maxSlots).mapNotNull { slot ->
            val pts = byDay.entries
                .mapNotNull { (day, entries) -> entries.getOrNull(slot)?.let { day to it.weightKg.toFloat() } }
                .sortedBy { it.first }
            if (pts.isNotEmpty()) SlottedSeries(s.colorIndex, pts) else null
        }
    }

@Composable
private fun ExerciseScatterChart(series: List<ExerciseSeriesData>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    // Split same-day entries into separate Vico series (slot 0, slot 1, …).
    // Each Vico series has at most one entry per date, so Vico never deduplicates,
    // and the rangeProvider + animation pipeline work normally.
    val slottedSeries = remember(series) { buildSlottedSeries(series) }

    LaunchedEffect(slottedSeries) {
        modelProducer.runTransaction {
            lineSeries {
                slottedSeries.forEach { ss ->
                    series(
                        x = ss.points.map { it.first.toFloat() },
                        y = ss.points.map { it.second }
                    )
                }
            }
            extras { store ->
                store[markerInfoKey] = series
                    .flatMap { s ->
                        s.points.map { p ->
                            p.date.toEpochDay().toDouble() to
                                "${s.exerciseName}: ${p.weightKg} kg × ${p.reps}"
                        }
                    }
                    .groupBy({ it.first }, { it.second })
            }
        }
    }

    val lineProvider = remember(slottedSeries) {
        LineCartesianLayer.LineProvider.series(
            slottedSeries.map { ss ->
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(fill(Color.Transparent)),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.point(
                            shapeComponent(
                                fill = fill(seriesColor(ss.colorIndex)),
                                shape = CorneredShape.Pill
                            ),
                            8.dp
                        )
                    )
                )
            }
        )
    }

    val markerLabel = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        background = rememberShapeComponent(
            fill = fill(MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = CorneredShape.rounded(all = 4.dp)
        ),
        padding = insets(horizontal = 8.dp, vertical = 4.dp),
        lineCount = 8
    )

    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = remember {
            DefaultCartesianMarker.ValueFormatter { context, targets ->
                val info = context.model.extraStore.getOrNull(markerInfoKey).orEmpty()
                targets
                    .filterIsInstance<LineCartesianLayerMarkerTarget>()
                    .mapNotNull { t -> info[t.x] }
                    .flatten()
                    .joinToString("\n")
            }
        },
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = lineProvider,
                rangeProvider = scatterRangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, x, _ ->
                    LocalDate.ofEpochDay(x.toLong()).format(axisDateFormatter)
                }
            ),
            marker = marker
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun ChartLegend(series: List<ExerciseSeriesData>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(seriesColor(s.colorIndex), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(s.exerciseName, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseMultiSelector(
    exercises: List<Exercise>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val fieldText = when {
        selectedIds.isEmpty() -> stringResource(R.string.select_exercise)
        selectedIds.size <= 2 -> exercises.filter { it.id in selectedIds }.joinToString(", ") { it.name }
        else -> stringResource(R.string.selected_exercises_count, selectedIds.size)
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = fieldText,
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
                    leadingIcon = {
                        Checkbox(
                            checked = ex.id in selectedIds,
                            onCheckedChange = null
                        )
                    },
                    onClick = { onToggle(ex.id) }
                )
            }
        }
    }
}
