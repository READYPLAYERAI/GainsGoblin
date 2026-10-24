package com.readyplayerai.gainsgoblin.ui.compose.journal

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.JournalUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The Journal screen: one continuous vertical journal, newest-first, with today as item 0 and
 * the past flowing down. Each day is a sticky header followed by its exercise cards; rest days
 * render only their header.
 * A shared FlexibleHeaderBar collapses as the list scrolls and shows the active day's month.
 *
 * The theme is expected to be provided by the caller (integration functions
 * wrap [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun JournalScreen(
    uiState: JournalUiState,
    categoryFor: (String?) -> String?,
    onSelectDate: (date: String) -> Unit,
    scrollToTopTick: StateFlow<Int>? = null,
    onExportRawData: () -> Unit,
    onRetryStorage: () -> Unit,
    onAddSet: (date: String, exerciseName: String, category: String?, reps: Double, weight: Double, comment: String?) -> Unit,
    onUpdateSet: (date: String, exerciseName: String, setIndex: Int, weight: Double, reps: Double) -> Unit,
    onDeleteSet: (date: String, exerciseName: String, setIndex: Int) -> Unit,
    onSetNote: (date: String, exerciseName: String, setIndex: Int, note: String?) -> Unit,
    onExerciseNote: (date: String, exerciseName: String, note: String?) -> Unit,
    onAddExercise: (name: String, category: String, onResult: (Boolean) -> Unit) -> Unit,
) {
    if (!uiState.loaded || uiState.infiniteDays.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator(modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.journal_loading),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    if (uiState.storageError != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.storage_recovery_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.storage_recovery_body))
                    Text(
                        text = uiState.storageError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onRetryStorage) {
                    Text(stringResource(R.string.storage_recovery_retry))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onExportRawData,
                    enabled = uiState.hasLegacyRecoveryData,
                ) {
                    Text(stringResource(R.string.storage_recovery_export))
                }
            },
        )
    }

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    SideEffect {
        val limitPx = with(density) { -(152.dp - 64.dp).toPx() }
        if (scrollBehavior.state.heightOffsetLimit != limitPx) {
            scrollBehavior.state.heightOffsetLimit = limitPx
        }
    }
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var expanded by remember { mutableStateOf<ExerciseKey?>(null) }
    var composer by remember { mutableStateOf(JournalComposerState()) }
    var noteText by remember { mutableStateOf("") }
    var setActions by remember { mutableStateOf<JournalSetTarget?>(null) }
    var scrollToKey by remember { mutableStateOf<ExerciseKey?>(null) }
    var draft by remember { mutableStateOf<JournalDraft?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showNewExercise by remember { mutableStateOf(false) }
    var newExerciseSeed by remember { mutableStateOf("") }

    val realDaysByDate = remember(uiState.realDays) {
        uiState.realDays.associateBy { it.Date }
    }
    val prByDate = remember(uiState.realDays) {
        uiState.realDays.associate { it.Date to findJournalPr(it.Exercises.orEmpty()) }
    }

    fun effectiveDraft(date: String?): JournalDraft? {
        val d = draft ?: return null
        if (d.date != date) return null
        val alreadyReal = realDaysByDate[date]?.Exercises?.any { it.Exercise == d.name } == true
        return if (alreadyReal) null else d
    }

    // Maps each day index to the index of its sticky header in the LazyColumn. A day adds one
    // item for its header, plus one item per exercise on training days, plus one for a draft.
    val dayStartIndices = remember(uiState.infiniteDays, uiState.realDays, draft) {
        val indices = IntArray(uiState.infiniteDays.size)
        var itemIndex = 0
        uiState.infiniteDays.forEachIndexed { index, day ->
            indices[index] = itemIndex
            itemIndex += 1 // sticky day header
            val realExercises = realDaysByDate[day.Date]?.Exercises
            if (!realExercises.isNullOrEmpty()) {
                itemIndex += realExercises.size
            }
            if (draft != null &&
                draft?.date == day.Date &&
                realExercises?.any { it.Exercise == draft?.name } != true
            ) {
                itemIndex += 1 // draft card
            }
        }
        indices
    }

    // The active day is the last day whose header starts at or before the first visible item.
    val activeDayIndex by remember(dayStartIndices, uiState.infiniteDays) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            var lo = 0
            var hi = dayStartIndices.lastIndex
            var result = 0
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                if (dayStartIndices[mid] <= first) {
                    result = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            result.coerceIn(0, uiState.infiniteDays.lastIndex)
        }
    }
    val activeDay = uiState.infiniteDays.getOrNull(activeDayIndex)
    val monthLabel = remember(activeDay?.Date) { formatMonthYear(activeDay?.Date) }

    fun saveOpenNoteIfChanged() {
        val key = expanded ?: return
        val exercise = realDaysByDate[key.date]?.Exercises
            ?.firstOrNull { it.Exercise == key.name } ?: return
        if (noteText != exercise.Comment.orEmpty()) {
            onExerciseNote(key.date, key.name, noteText.ifBlank { null })
        }
    }

    fun initialComposer(date: String, name: String, exercise: WorkoutExercise?): JournalComposerState {
        val last = exercise?.Sets?.lastOrNull()
        val fallback = lastPerformed(uiState.realDays, name)
        val weight = last?.weight ?: fallback?.set?.weight
        val reps = last?.reps ?: fallback?.set?.reps
        val source = when {
            !exercise?.Sets.isNullOrEmpty() -> context.getString(
                R.string.journal_set_from,
                exercise?.Sets?.size ?: 0,
            )
            fallback != null -> context.getString(
                R.string.journal_last_trained,
                formatWeightValue(fallback.set.weight),
                formatRepsValue(fallback.set.reps),
            )
            else -> ""
        }
        return JournalComposerState(
            weight = formatWeightValue(weight),
            reps = formatRepsValue(reps),
            sourceLabel = source,
        )
    }

    fun beginExpand(date: String, name: String, exercise: WorkoutExercise?) {
        saveOpenNoteIfChanged()
        expanded = ExerciseKey(date, name)
        noteText = exercise?.Comment.orEmpty()
        composer = initialComposer(date, name, exercise)
    }

    fun collapseExpanded() {
        if (expanded == null) return
        saveOpenNoteIfChanged()
        expanded = null
        composer = JournalComposerState()
        noteText = ""
    }

    fun toggleCard(date: String, name: String, exercise: WorkoutExercise?) {
        val key = ExerciseKey(date, name)
        if (expanded == key) collapseExpanded() else beginExpand(date, name, exercise)
    }

    fun startEdit(index: Int, set: WorkoutSet) {
        composer = JournalComposerState(
            weight = formatWeightValue(set.weight),
            reps = formatRepsValue(set.reps),
            editingSetIndex = index,
            originalWeight = set.weight,
            originalReps = set.reps,
            sourceLabel = "",
        )
    }

    fun cancelEdit(exercise: WorkoutExercise?) {
        val key = expanded ?: return
        composer = initialComposer(key.date, key.name, exercise)
    }

    fun stepWeight(delta: Int) {
        val current = composer.weight.toDoubleOrNull() ?: 0.0
        composer = composer.copy(weight = formatWeightValue((current + delta).coerceAtLeast(0.0)))
    }

    fun stepReps(delta: Int) {
        val current = composer.reps.toDoubleOrNull() ?: 0.0
        composer = composer.copy(reps = (current.roundToInt() + delta).coerceAtLeast(0).toString())
    }

    fun toast(@StringRes resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    fun logSet(
        date: String,
        name: String,
        category: String?,
        exercise: WorkoutExercise?,
    ) {
        val weightText = composer.weight
        val repsText = composer.reps
        if (weightText.isBlank() || repsText.isBlank()) {
            toast(R.string.add_exercise_empty_inputs)
            return
        }
        val weight = weightText.toDoubleOrNull()
        val reps = repsText.toDoubleOrNull()
        if (weight == null || reps == null || weight <= 0.0 || reps <= 0.0) {
            toast(R.string.add_exercise_invalid_inputs)
            return
        }
        val editingIndex = composer.editingSetIndex
        if (editingIndex != null) {
            onUpdateSet(date, name, editingIndex, weight, reps)
            composer = composer.copy(
                editingSetIndex = null,
                originalWeight = null,
                originalReps = null,
                sourceLabel = context.getString(R.string.journal_set_from, editingIndex + 1),
            )
        } else {
            onAddSet(date, name, category, reps, weight, noteText.ifBlank { null })
            val count = (exercise?.Sets?.size ?: 0) + 1
            composer = composer.copy(
                sourceLabel = context.getString(R.string.journal_set_from, count),
            )
        }
    }

    // Persist the selected date whenever the active day changes.
    LaunchedEffect(activeDayIndex, uiState.infiniteDays) {
        activeDay?.Date?.let(onSelectDate)
    }

    // Bottom-nav HOME reselect hook (null = disabled).
    LaunchedEffect(scrollToTopTick) {
        scrollToTopTick?.collect { tick ->
            if (tick > 0) listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiState.realDays, draft) {
        val d = draft ?: return@LaunchedEffect
        if (realDaysByDate[d.date]?.Exercises?.any { it.Exercise == d.name } == true) {
            draft = null
        }
    }

    LaunchedEffect(uiState.realDays, expanded, draft) {
        val key = expanded ?: return@LaunchedEffect
        val real = realDaysByDate[key.date]?.Exercises?.any { it.Exercise == key.name } == true
        val isCurrentDraft = draft != null && draft?.date == key.date && draft?.name == key.name
        if (!real && !isCurrentDraft) {
            expanded = null
            composer = JournalComposerState()
            noteText = ""
        }
    }

    LaunchedEffect(scrollToKey, uiState.realDays, draft) {
        val key = scrollToKey ?: return@LaunchedEffect
        val dayIndex = uiState.infiniteDays.indexOfFirst { it.Date == key.date }
        if (dayIndex < 0) {
            scrollToKey = null
            return@LaunchedEffect
        }
        val realExercises = realDaysByDate[key.date]?.Exercises.orEmpty()
        val exerciseIndex = realExercises.indexOfFirst { it.Exercise == key.name }
        val start = dayStartIndices[dayIndex]
        val target = if (exerciseIndex >= 0) {
            start + 1 + exerciseIndex
        } else {
            start + 1 + realExercises.size
        }
        val offset = with(density) { (-80).dp.roundToPx() }
        listState.animateScrollToItem(target, offset)
        scrollToKey = null
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            JournalAppBar(
                monthLabel = monthLabel,
                collapseFraction = scrollBehavior.state.collapsedFraction,
                onDateIconClick = { showDatePicker = true },
            )
        },
        floatingActionButton = {
            if (expanded == null && activeDay?.Date != null) {
                ExtendedFloatingActionButton(
                    onClick = { showPicker = true },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24px),
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(
                            text = if (activeDayIndex == 0) {
                                stringResource(R.string.journal_add_exercise)
                            } else {
                                stringResource(
                                    R.string.journal_add_to_day,
                                    formatWeekday(activeDay.Date),
                                )
                            },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            uiState.infiniteDays.forEachIndexed { index, day ->
                val realDay = realDaysByDate[day.Date]
                val exercises = realDay?.Exercises.orEmpty()
                val keyDate = day.Date ?: index.toString()

                stickyHeader(key = "day-header-$keyDate") {
                    JournalDayHeader(
                        day = day,
                        realDay = realDay,
                        isToday = index == 0,
                        draftExerciseCount = if (effectiveDraft(day.Date) != null) 1 else 0,
                    )
                }

                if (exercises.isNotEmpty()) {
                    itemsIndexed(
                        items = exercises,
                        key = { exerciseIndex, exercise ->
                            "ex-$keyDate-$exerciseIndex-${exercise.Exercise}"
                        },
                    ) { exerciseIndex, exercise ->
                        val key = ExerciseKey(day.Date.orEmpty(), exercise.Exercise.orEmpty())
                        val isExpanded = expanded == key
                        val name = exercise.Exercise.orEmpty()
                        val date = day.Date.orEmpty()
                        JournalExerciseCard(
                            exercise = exercise,
                            category = categoryFor(exercise.Exercise),
                            pr = prByDate[day.Date],
                            state = if (isExpanded) {
                                JournalCardState(expanded = true, composer = composer, note = noteText)
                            } else {
                                JournalCardState()
                            },
                            callbacks = JournalCardCallbacks(
                                onToggle = { toggleCard(date, name, exercise) },
                                onSetClick = { index ->
                                    exercise.Sets?.getOrNull(index)?.let { startEdit(index, it) }
                                },
                                onSetMore = { index ->
                                    exercise.Sets?.getOrNull(index)?.let {
                                        setActions = JournalSetTarget(date, name, index, it)
                                    }
                                },
                                onNoteChange = { noteText = it },
                                onNoteSave = { saveOpenNoteIfChanged() },
                                onWeightChange = { composer = composer.copy(weight = it) },
                                onRepsChange = { composer = composer.copy(reps = it) },
                                onStepWeight = { stepWeight(it) },
                                onStepReps = { stepReps(it) },
                                onLogSet = { logSet(date, name, categoryFor(exercise.Exercise), exercise) },
                                onCancelEdit = { cancelEdit(exercise) },
                            ),
                            modifier = if (exerciseIndex == 0) {
                                Modifier.padding(top = 12.dp)
                            } else {
                                Modifier
                            },
                        )
                    }
                }

                val draftForDay = effectiveDraft(day.Date)
                if (draftForDay != null) {
                    item(key = "draft-${draftForDay.date}-${draftForDay.name}") {
                        val key = ExerciseKey(draftForDay.date, draftForDay.name)
                        val draftExercise = WorkoutExercise().apply {
                            Exercise = draftForDay.name
                            Sets = arrayListOf()
                            Comment = noteText
                        }
                        JournalExerciseCard(
                            exercise = draftExercise,
                            category = draftForDay.category,
                            pr = null,
                            state = JournalCardState(
                                expanded = expanded == key,
                                isDraft = true,
                                composer = composer,
                                note = noteText,
                            ),
                            callbacks = JournalCardCallbacks(
                                onToggle = {
                                    if (expanded == key) {
                                        draft = null
                                        expanded = null
                                        composer = JournalComposerState()
                                        noteText = ""
                                    } else {
                                        beginExpand(draftForDay.date, draftForDay.name, null)
                                    }
                                },
                                onSetClick = {},
                                onSetMore = {},
                                onNoteChange = { noteText = it },
                                onNoteSave = {},
                                onWeightChange = { composer = composer.copy(weight = it) },
                                onRepsChange = { composer = composer.copy(reps = it) },
                                onStepWeight = { stepWeight(it) },
                                onStepReps = { stepReps(it) },
                                onLogSet = {
                                    logSet(
                                        draftForDay.date,
                                        draftForDay.name,
                                        draftForDay.category,
                                        null,
                                    )
                                },
                                onCancelEdit = {
                                    composer = initialComposer(draftForDay.date, draftForDay.name, null)
                                },
                            ),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }

    setActions?.let { target ->
        JournalSetActionsSheet(
            setIndex = target.index,
            set = target.set,
            onDismiss = { setActions = null },
            onEdit = {
                startEdit(target.index, target.set)
                setActions = null
            },
            onSaveNote = { note ->
                onSetNote(target.date, target.exerciseName, target.index, note)
                setActions = null
            },
            onDelete = {
                onDeleteSet(target.date, target.exerciseName, target.index)
                setActions = null
            },
        )
    }

    // Date -> index into the (descending) feed for the picker.
    val dateToIndex = remember(uiState.infiniteDays) {
        val map = HashMap<String, Int>(uiState.infiniteDays.size)
        uiState.infiniteDays.forEachIndexed { index, day -> day.Date?.let { map[it] = index } }
        map
    }

    val pickerDate = activeDay?.Date

    if (showPicker && pickerDate != null) {
        val alreadyInDay = buildSet {
            realDaysByDate[pickerDate]?.Exercises?.forEach { exercise ->
                exercise.Exercise?.let { add(it) }
            }
            if (draft?.date == pickerDate) {
                draft?.name?.let { add(it) }
            }
        }
        JournalExercisePicker(
            targetDateLabel = formatFullDate(pickerDate),
            exercises = uiState.knownExercises,
            days = uiState.realDays,
            alreadyInDay = alreadyInDay,
            onPick = { exercise ->
                showPicker = false
                val name = exercise.Name.orEmpty()
                val existing = realDaysByDate[pickerDate]?.Exercises
                    ?.firstOrNull { it.Exercise == name }
                val existingDraft = draft?.takeIf {
                    it.date == pickerDate && it.name == name
                }
                when {
                    existing != null -> {
                        beginExpand(pickerDate, name, existing)
                        scrollToKey = ExerciseKey(pickerDate, name)
                    }
                    existingDraft != null -> {
                        beginExpand(pickerDate, name, null)
                        scrollToKey = ExerciseKey(pickerDate, name)
                    }
                    else -> {
                        draft = JournalDraft(
                            pickerDate,
                            name,
                            exercise.BodyPart.orEmpty(),
                        )
                        beginExpand(pickerDate, name, null)
                        noteText = ""
                        scrollToKey = ExerciseKey(pickerDate, name)
                    }
                }
            },
            onCreate = { query ->
                showPicker = false
                newExerciseSeed = query
                showNewExercise = true
            },
            onDismiss = { showPicker = false },
        )
    }

    if (showNewExercise && pickerDate != null) {
        JournalNewExerciseSheet(
            initialName = newExerciseSeed,
            onDismiss = { showNewExercise = false },
            onAdd = { name, category, onResult ->
                onAddExercise(name, category, onResult)
            },
            onAdded = { name, category ->
                showNewExercise = false
                draft = JournalDraft(pickerDate, name, category)
                beginExpand(pickerDate, name, null)
                noteText = ""
                scrollToKey = ExerciseKey(pickerDate, name)
            },
        )
    }

    if (showDatePicker) {
        JournalDatePickerDialog(
            initialDateString = activeDay?.Date,
            // windowFirstDay/windowLastDay must be CHRONOLOGICAL (first <= last):
            // the feed is DESCENDING, so the oldest day is last(), today is first().
            windowFirstDay = uiState.infiniteDays.last().Date,
            windowLastDay = uiState.infiniteDays.first().Date,
            onConfirm = { date ->
                showDatePicker = false
                dateToIndex[date]?.let { dayIndex ->
                    scope.launch { listState.animateScrollToItem(dayStartIndices[dayIndex]) }
                }
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

private fun formatWeightValue(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

private fun formatRepsValue(value: Double?): String {
    if (value == null) return ""
    return value.roundToInt().toString()
}

private fun formatWeekday(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) {
        SimpleDateFormat("EEE", Locale.getDefault()).format(parsed)
    } else {
        ""
    }
} catch (e: Exception) {
    ""
}

private fun formatFullDate(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(parsed)
    } else {
        date ?: ""
    }
} catch (e: Exception) {
    date ?: ""
}

private val utcDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/** Parses an app date string (yyyy-MM-dd) into UTC-midnight epoch millis, or null if unparseable. */
private fun dateStringToUtcMillis(date: String): Long? = try {
    utcDateFormatter.parse(date)?.time
} catch (e: ParseException) {
    null
}

/** Formats UTC-midnight epoch millis back into an app date string (yyyy-MM-dd). */
private fun utcMillisToDateString(millis: Long): String = utcDateFormatter.format(millis)

/**
 * The M3 modal date picker dialog (spec: m3.material.io date pickers),
 * restricted to the five-year past day window.
 */
@Composable
private fun JournalDatePickerDialog(
    initialDateString: String?,
    windowFirstDay: String?,
    windowLastDay: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val windowFirstMillis = windowFirstDay?.let(::dateStringToUtcMillis)
    val windowLastMillis = windowLastDay?.let(::dateStringToUtcMillis)
    val windowFirstYear = windowFirstDay?.take(4)?.toIntOrNull()
    val windowLastYear = windowLastDay?.take(4)?.toIntOrNull()

    val selectableDates = remember(windowFirstDay, windowLastDay) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val first = windowFirstMillis ?: return true
                val last = windowLastMillis ?: return true
                return utcTimeMillis in first..last
            }

            override fun isSelectableYear(year: Int): Boolean {
                val first = windowFirstYear ?: return true
                val last = windowLastYear ?: return true
                return year in first..last
            }
        }
    }

    val initialSelectedMillis = dateStringToUtcMillis(initialDateString ?: "")
    val state = if (windowFirstYear != null && windowLastYear != null) {
        rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis,
            yearRange = windowFirstYear..windowLastYear,
            selectableDates = selectableDates,
        )
    } else {
        rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis,
            selectableDates = selectableDates,
        )
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis
                        ?.let { onConfirm(utcMillisToDateString(it)) }
                        ?: onDismiss()
                },
            ) {
                Text(stringResource(R.string.journal_date_picker_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.exercises_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}
