package com.readyplayerai.gainsgoblin.ui.compose.addexercise

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.AddExerciseUiState
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * M3 Expressive data-entry screen for logging the sets of a single exercise on
 * the selected day: a collapsing [FlexibleHeaderBar] titled with the exercise
 * name, weight/reps steppers, Save/Clear (or Update/Cancel) actions and a list
 * of the day's existing sets with edit/delete actions.
 *
 * The theme is provided by the caller (the hosting Activity wraps this in
 * [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
    uiState: AddExerciseUiState,
    onAddSet: (weight: Double, reps: Double) -> Unit,
    onUpdateSet: (date: String, setIndex: Int, weight: Double, reps: Double) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var weightText by remember { mutableStateOf("") }
    var repsText by remember { mutableStateOf("") }
    var fieldsInitialized by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var sheetSet by remember { mutableStateOf<WorkoutSet?>(null) }
    var deleteSet by remember { mutableStateOf<WorkoutSet?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // The bar collapses 152dp -> 64dp; give the scroll behavior the same 88dp
    // travel that MediumTopAppBar used to set internally.
    val density = LocalDensity.current
    SideEffect {
        val limitPx = with(density) { -(152.dp - 64.dp).toPx() }
        if (scrollBehavior.state.heightOffsetLimit != limitPx) {
            scrollBehavior.state.heightOffsetLimit = limitPx
        }
    }
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    // Sets for the selected date + exercise, in day order (index 0 = oldest).
    val todaySets = uiState.days
        .filter { it.Date == uiState.selectedDate }
        .flatMap { day -> (day.Sets ?: emptyList()).filter { it.exerciseName == uiState.exerciseName } }

    fun feedback(resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    // Legacy initEditTexts parity: prefill from the max-volume set, else blank.
    fun applyInitialValues() {
        val maxSet = todaySets.maxByOrNull { (it.reps ?: 0.0) * (it.weight ?: 0.0) }
        if (maxSet == null || (maxSet.reps ?: 0.0) == 0.0 || (maxSet.weight ?: 0.0) == 0.0) {
            weightText = ""
            repsText = ""
        } else {
            weightText = maxSet.weight?.toString().orEmpty()
            repsText = maxSet.reps?.toInt()?.toString().orEmpty()
        }
    }

    fun enterEditMode(set: WorkoutSet, index: Int) {
        weightText = set.weight?.toString().orEmpty()
        repsText = set.reps?.toInt()?.toString().orEmpty()
        editIndex = index
        keyboardController?.hide()
    }

    fun onSaveClicked() {
        keyboardController?.hide()

        val weight = weightText.toDoubleOrNull()
        val reps = repsText.toDoubleOrNull()
        if (weight == null || reps == null || weightText.isBlank() || repsText.isBlank()) {
            feedback(R.string.add_exercise_empty_inputs)
            return
        }
        if (weight <= 0.0 || reps <= 0.0) {
            feedback(R.string.add_exercise_invalid_inputs)
            return
        }

        val index = editIndex
        if (index == null) {
            onAddSet(weight, reps)
            feedback(R.string.add_exercise_set_added)
        } else {
            val set = todaySets.getOrNull(index) ?: return
            val day = uiState.days.firstOrNull { it.Date == uiState.selectedDate } ?: return
            val sets = day.Sets ?: return
            // The filtered instances are the same objects as in the day's list.
            val setIndex = sets.indexOfFirst { it === set }
            if (setIndex < 0) return
            onUpdateSet(day.Date.orEmpty(), setIndex, weight, reps)
            feedback(R.string.add_exercise_set_updated)
            editIndex = null
        }
    }

    fun onSecondaryClicked() {
        keyboardController?.hide()
        if (editIndex == null) {
            weightText = ""
            repsText = ""
            editIndex = null
        } else {
            editIndex = null
            applyInitialValues()
        }
    }

    LaunchedEffect(uiState.loaded) {
        if (!fieldsInitialized && uiState.loaded) {
            fieldsInitialized = true
            applyInitialValues()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlexibleHeaderBar(
                title = uiState.exerciseName.orEmpty(),
                subtitle = formatFullDate(uiState.selectedDate),
                collapseFraction = collapsedFraction,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "weight") {
                StepperGroup(
                    label = stringResource(R.string.add_exercise_weight_label),
                    value = weightText,
                    keyboardType = KeyboardType.Decimal,
                    decreaseCd = stringResource(R.string.add_exercise_cd_decrease_weight),
                    increaseCd = stringResource(R.string.add_exercise_cd_increase_weight),
                    onValueChange = { input ->
                        weightText = input.filter { ch -> ch.isDigit() || ch == '.' }
                    },
                    onDecrease = { weightText = stepWeightDown(weightText) },
                    onIncrease = { weightText = stepWeightUp(weightText) },
                )
            }

            item(key = "reps") {
                StepperGroup(
                    label = stringResource(R.string.add_exercise_reps_label),
                    value = repsText,
                    keyboardType = KeyboardType.Number,
                    decreaseCd = stringResource(R.string.add_exercise_cd_decrease_reps),
                    increaseCd = stringResource(R.string.add_exercise_cd_increase_reps),
                    onValueChange = { input ->
                        repsText = input.filter { ch -> ch.isDigit() || ch == '.' }
                    },
                    onDecrease = { repsText = stepRepsDown(repsText) },
                    onIncrease = { repsText = stepRepsUp(repsText) },
                )
            }

            item(key = "buttons") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onSaveClicked() },
                        modifier = Modifier.weight(1f).height(ButtonDefaults.MediumContainerHeight),
                    ) {
                        Text(
                            stringResource(
                                if (editIndex == null) R.string.exercises_save else R.string.add_exercise_update,
                            ),
                        )
                    }
                    OutlinedButton(
                        onClick = { onSecondaryClicked() },
                        modifier = Modifier.weight(1f).height(ButtonDefaults.MediumContainerHeight),
                    ) {
                        Text(
                            stringResource(
                                if (editIndex == null) R.string.add_exercise_clear else R.string.exercises_cancel,
                            ),
                        )
                    }
                }
            }

            if (todaySets.isNotEmpty()) {
                item(key = "sets_header") {
                    Text(
                        text = stringResource(R.string.add_exercise_todays_sets),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                itemsIndexed(
                    items = todaySets,
                    key = { index, _ -> index },
                ) { index, set ->
                    SetRow(
                        weight = set.weight?.toString().orEmpty(),
                        reps = set.reps?.toInt()?.toString().orEmpty(),
                        onClick = { enterEditMode(set, index) },
                        onMoreClick = { sheetSet = set },
                    )
                }
            }
        }
    }

    sheetSet?.let { set ->
        ModalBottomSheet(
            onDismissRequest = { sheetSet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                SheetActionRow(
                    label = stringResource(R.string.add_exercise_edit),
                    iconRes = R.drawable.ic_create_24px,
                    onClick = {
                        val index = todaySets.indexOfFirst { it === set }
                        if (index >= 0) {
                            enterEditMode(set, index)
                        }
                        sheetSet = null
                    },
                )
                SheetActionRow(
                    label = stringResource(R.string.exercises_delete),
                    iconRes = R.drawable.ic_delete_24px,
                    isError = true,
                    onClick = {
                        deleteSet = set
                        sheetSet = null
                    },
                )
            }
        }
    }

    deleteSet?.let { set ->
        AlertDialog(
            onDismissRequest = { deleteSet = null },
            title = { Text(stringResource(R.string.add_exercise_delete_title)) },
            text = { Text(stringResource(R.string.add_exercise_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSet(set)
                        feedback(R.string.add_exercise_set_deleted)
                        deleteSet = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.exercises_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSet = null }) {
                    Text(stringResource(R.string.exercises_cancel))
                }
            },
        )
    }
}

/**
 * A labelled stepper row: [-] [centered pill text field] [+].
 */
@Composable
private fun StepperGroup(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    decreaseCd: String,
    increaseCd: String,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    // Input width: fits "1000.00" (the largest realistic weight) in the field's
    // text style, plus M3's 16dp-per-side container padding, slack, then ×1.5
    // to fill the row (user spec).
    val textMeasurer = rememberTextMeasurer()
    val fieldTextStyle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    val fieldWidth = with(LocalDensity.current) {
        (
            textMeasurer.measure("1000.00", fieldTextStyle).size.width.toDp() +
                16.dp + 16.dp + 16.dp
            ) * 1.5f
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDecrease,
                // 48dp natural IconButton touch target ×1.25 (user spec).
                modifier = Modifier.size(60.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove_24px),
                    contentDescription = decreaseCd,
                )
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.requiredWidth(fieldWidth),
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = fieldTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
            )
            IconButton(
                onClick = onIncrease,
                // 48dp natural IconButton touch target ×1.25 (user spec).
                modifier = Modifier.size(60.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_24px),
                    contentDescription = increaseCd,
                )
            }
        }
    }
}

/**
 * M3 Expressive segmented list item: 16dp rounded container on
 * `surfaceContainerHighest`, 64dp min height, weight/reps content and a trailing
 * "more" button. Tapping the row enters edit mode.
 */
@Composable
private fun SetRow(
    weight: String,
    reps: String,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = weight,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = " " + stringResource(R.string.add_exercise_kg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = reps,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = " " + stringResource(R.string.add_exercise_reps_unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert_24px),
                    contentDescription = stringResource(R.string.add_exercise_cd_more),
                )
            }
        }
    }
}

/**
 * Bottom-sheet action row: leading icon + label, full-width tappable, matching
 * the exercise action sheet's visual.
 */
@Composable
private fun SheetActionRow(
    label: String,
    @DrawableRes iconRes: Int,
    isError: Boolean = false,
    onClick: () -> Unit,
) {
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Formats a "yyyy-MM-dd" date as a full date like Journal's header subtitle. */
private fun formatFullDate(date: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
    if (parsed != null) SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(parsed) else date
} catch (e: ParseException) {
    date
}

/** Weight down: blank/unparseable -> "1.0"; else value - 1.0, clamped at 0.0. */
private fun stepWeightDown(current: String): String {
    val value = current.toDoubleOrNull() ?: return "1.0"
    return (value - 1.0).coerceAtLeast(0.0).toString()
}

/** Weight up: blank/unparseable -> "1.0"; else value + 1.0. */
private fun stepWeightUp(current: String): String {
    val value = current.toDoubleOrNull() ?: return "1.0"
    return (value + 1.0).toString()
}

/** Reps down: blank/unparseable -> "1"; else value - 1, clamped at 0. */
private fun stepRepsDown(current: String): String {
    val value = current.toDoubleOrNull() ?: return "1"
    return (value.toInt() - 1).coerceAtLeast(0).toString()
}

/** Reps up: blank/unparseable -> "1"; else value + 1. */
private fun stepRepsUp(current: String): String {
    val value = current.toDoubleOrNull() ?: return "1"
    return (value.toInt() + 1).toString()
}
