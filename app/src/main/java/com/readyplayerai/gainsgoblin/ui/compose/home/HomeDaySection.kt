package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private val TNUM = TextStyle(fontFeatureSettings = "tnum")

private fun tnum(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight? = null) =
    if (weight == null) TNUM.copy(fontSize = size) else TNUM.copy(fontSize = size, fontWeight = weight)

private data class DayPrCelebration(
    val exerciseName: String?,
    val isRepPr: Boolean,
    @param:StringRes val labelRes: Int,
)

/**
 * One section of Home's vertical journal feed: renders exactly what the
 * old pager page rendered — exercise cards for a training day, or the big
 * rest-day empty state for an empty day. The day header lives in the flexible app bar.
 */
@Composable
fun HomeDaySection(
    day: WorkoutDay,
    realDay: WorkoutDay?,
    categoryFor: (String?) -> String?,
    onExerciseClick: (exerciseName: String?, date: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = realDay?.Exercises
    if (!exercises.isNullOrEmpty()) {
        val pr = remember(exercises) { findPr(exercises) }
        exercises.forEach { exercise ->
            ExerciseCard(
                exercise = exercise,
                day = day,
                categoryFor = categoryFor,
                onExerciseClick = onExerciseClick,
                pr = pr,
            )
        }
    } else {
        RestDayEmptyState(day = day)
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    day: WorkoutDay,
    categoryFor: (String?) -> String?,
    onExerciseClick: (exerciseName: String?, date: String?) -> Unit,
    pr: DayPrCelebration?,
) {
    val category = categoryFor(exercise.Exercise)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onExerciseClick(exercise.Exercise, day.Date) }
            .padding(horizontal = 6.dp, vertical = 10.dp),
    ) {
        ExerciseHeader(exercise = exercise, category = category)

        val prSetIndex = if (pr != null && pr.exerciseName == exercise.Exercise) {
            val sets = exercise.Sets ?: emptyList()
            sets.indexOfFirst { set ->
                if (pr.isRepPr) set.reps == exercise.MaxReps else set.weight == exercise.MaxWeight
            }
        } else -1
        val sets = exercise.Sets ?: emptyList()
        sets.forEachIndexed { index, set ->
            SetRow(
                set = set,
                index = index,
                isPr = index == prSetIndex,
                prLabelRes = pr?.labelRes,
            )
        }
    }
}

@Composable
private fun ExerciseHeader(exercise: WorkoutExercise, category: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryMarker(category = category, size = 18.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.Exercise ?: "",
                style = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = category ?: "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetRow(
    set: WorkoutSet,
    index: Int,
    isPr: Boolean,
    prLabelRes: Int?,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isPr) 6.dp else 0.dp)
            .then(
                if (isPr) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp)
                .padding(vertical = 9.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = set.weight?.toString() ?: "",
                    style = tnum(
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
                        text = stringResource(R.string.home_set_label, index + 1),
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
                    style = tnum(size = 18.sp, weight = FontWeight(650)),
                    color = onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "reps",
                    fontSize = 13.sp,
                    color = onSurfaceVariant,
                )
            }
        }

        val comment = set.comment
        if (!comment.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_create_24px),
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

@Composable
private fun RestDayEmptyState(day: WorkoutDay) {
    val weekday = remember(day.Date) { weekdayOf(day.Date) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 26.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(44.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fitness_center_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.home_rest_day),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_rest_day_hint, weekday),
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 250.dp),
            )
        }
    }
}

private fun findPr(exercises: List<WorkoutExercise>): DayPrCelebration? {
    exercises.firstOrNull { it.MaxRepsPR }?.let {
        return DayPrCelebration(it.Exercise, isRepPr = true, labelRes = R.string.home_pr_rep)
    }
    exercises.firstOrNull { it.MaxWeightPR }?.let {
        return DayPrCelebration(it.Exercise, isRepPr = false, labelRes = R.string.home_pr_weight)
    }
    return null
}

private fun weekdayOf(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) SimpleDateFormat("EEEE", Locale.getDefault()).format(parsed) else ""
} catch (e: ParseException) {
    ""
}
