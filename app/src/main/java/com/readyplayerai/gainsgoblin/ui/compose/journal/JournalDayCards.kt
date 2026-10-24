package com.readyplayerai.gainsgoblin.ui.compose.journal

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import kotlin.math.roundToInt

private val JOURNAL_TNUM = TextStyle(fontFeatureSettings = "tnum")

private fun journalTnum(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight? = null) =
    if (weight == null) {
        JOURNAL_TNUM.copy(fontSize = size)
    } else {
        JOURNAL_TNUM.copy(fontSize = size, fontWeight = weight)
    }

internal data class JournalPrCelebration(
    val exerciseName: String?,
    val isRepPr: Boolean,
    @param:StringRes val labelRes: Int,
)

internal fun findJournalPr(exercises: List<WorkoutExercise>): JournalPrCelebration? {
    exercises.firstOrNull { it.MaxRepsPR }?.let {
        return JournalPrCelebration(it.Exercise, isRepPr = true, labelRes = R.string.journal_pr_rep)
    }
    exercises.firstOrNull { it.MaxWeightPR }?.let {
        return JournalPrCelebration(it.Exercise, isRepPr = false, labelRes = R.string.journal_pr_weight)
    }
    return null
}

@Composable
internal fun JournalExerciseCard(
    exercise: WorkoutExercise,
    category: String?,
    pr: JournalPrCelebration?,
    state: JournalCardState,
    callbacks: JournalCardCallbacks,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val titleInteraction = remember { MutableInteractionSource() }
    val titlePressed by titleInteraction.collectIsPressedAsState()
    val baseCardColor = if (state.expanded) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val cardColor = if (titlePressed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f).compositeOver(baseCardColor)
    } else {
        baseCardColor
    }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val chevronRotation by animateFloatAsState(
        targetValue = if (state.expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "journalChevron",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (state.expanded) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (state.expanded) 6.dp else 0.dp,
        ),
    ) {
        Column(modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = titleInteraction,
                        indication = null,
                        onClick = callbacks.onToggle,
                    )
                    .padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryMarker(category = category, size = 18.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.Exercise ?: "",
                        style = TextStyle(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.01).em,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.journal_exercise_sets,
                            exercise.Sets?.size ?: 0,
                            category ?: "",
                            exercise.Sets?.size ?: 0,
                        ),
                        fontSize = 13.sp,
                        color = onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = callbacks.onToggle,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.expanded) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            Color.Transparent
                        },
                        contentColor = onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more_24px),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation),
                    )
                }
            }

            val prSetIndex = if (pr != null && pr.exerciseName == exercise.Exercise) {
                val sets = exercise.Sets ?: emptyList()
                sets.indexOfFirst { set ->
                    if (pr.isRepPr) set.reps == exercise.MaxReps else set.weight == exercise.MaxWeight
                }
            } else {
                -1
            }
            val sets = exercise.Sets ?: emptyList()
            val exerciseComment = exercise.Comment
            sets.forEachIndexed { index, set ->
                JournalSetRow(
                    set = set,
                    index = index,
                    isPr = index == prSetIndex,
                    prLabelRes = pr?.labelRes,
                    expanded = state.expanded,
                    editing = state.composer.editingSetIndex == index,
                    hideComment = !exerciseComment.isNullOrBlank() &&
                        set.comment == exerciseComment,
                    onClick = {
                        if (state.expanded) {
                            callbacks.onSetClick(index)
                        } else {
                            callbacks.onToggle()
                        }
                    },
                    onMoreClick = { callbacks.onSetMore(index) },
                )
            }
            if (!state.expanded && !exerciseComment.isNullOrBlank()) {
                Surface(
                    onClick = callbacks.onToggle,
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_format_quote_24px),
                            contentDescription = null,
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = exerciseComment,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.expanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut(),
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_note_24px),
                            contentDescription = null,
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        BasicTextField(
                            value = state.note,
                            onValueChange = callbacks.onNoteChange,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (!it.isFocused) callbacks.onNoteSave() },
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            singleLine = true,
                            decorationBox = { inner ->
                                Box {
                                    if (state.note.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.journal_exercise_note_hint),
                                            fontSize = 13.5.sp,
                                            color = onSurfaceVariant,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                    JournalComposer(
                        state = state.composer,
                        onWeightChange = callbacks.onWeightChange,
                        onRepsChange = callbacks.onRepsChange,
                        onStepWeight = callbacks.onStepWeight,
                        onStepReps = callbacks.onStepReps,
                        onLogSet = callbacks.onLogSet,
                        onCancelEdit = callbacks.onCancelEdit,
                    )
                    if (state.isDraft) {
                        Text(
                            text = stringResource(R.string.journal_draft_hint),
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                            fontSize = 11.5.sp,
                            color = onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalSetRow(
    set: WorkoutSet,
    index: Int,
    isPr: Boolean,
    prLabelRes: Int?,
    expanded: Boolean,
    editing: Boolean,
    hideComment: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val rowShape = RoundedCornerShape(if (isPr) 20.dp else 18.dp)
    val rowColor = when {
        isPr -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.tertiaryContainer,
            if (expanded) 0.70f else 0.55f,
        )
        expanded -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isPr) 6.dp else 0.dp,
                    vertical = if (expanded) 3.dp else 0.dp,
                ),
            shape = rowShape,
            color = rowColor,
            border = if (editing) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(vertical = 8.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = formatWeightDisplay(set.weight),
                        style = journalTnum(
                            size = 23.sp,
                            weight = if (isPr) FontWeight(650) else FontWeight.Medium,
                        ),
                        color = onSurface,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "kg",
                        fontSize = 14.sp,
                        color = onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isPr && prLabelRes != null) {
                        PrBadge(labelRes = prLabelRes)
                    } else {
                        Text(
                            text = stringResource(R.string.journal_set_label, index + 1),
                            fontSize = 12.5.sp,
                            color = onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = set.reps?.roundToInt()?.toString() ?: "",
                        style = journalTnum(size = 18.sp, weight = FontWeight(650)),
                        color = onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "reps",
                        fontSize = 13.sp,
                        color = onSurfaceVariant,
                    )
                }

                if (expanded) {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        val comment = if (hideComment) null else set.comment
        if (!comment.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_note_24px),
                    contentDescription = null,
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = comment,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PrBadge(@StringRes labelRes: Int) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "prBadge",
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_star_fill0_wght400_grad0_opsz24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(labelRes),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.05.em,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

private fun formatWeightDisplay(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

