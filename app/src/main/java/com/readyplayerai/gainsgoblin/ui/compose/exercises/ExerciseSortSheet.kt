package com.readyplayerai.gainsgoblin.ui.compose.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R

/** The three approved sort orders for the Exercises library. */
enum class ExerciseSortMode { AZ, RECENT, SETS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSortSheet(
    selected: ExerciseSortMode,
    onSelect: (ExerciseSortMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.exercises_sort_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(10.dp))
            ExerciseSortRow(
                selected = selected == ExerciseSortMode.AZ,
                title = stringResource(R.string.exercises_sort_az),
                description = stringResource(R.string.exercises_sort_az_desc),
                onClick = { onSelect(ExerciseSortMode.AZ) },
            )
            ExerciseSortRow(
                selected = selected == ExerciseSortMode.RECENT,
                title = stringResource(R.string.exercises_sort_recent),
                description = stringResource(R.string.exercises_sort_recent_desc),
                onClick = { onSelect(ExerciseSortMode.RECENT) },
            )
            ExerciseSortRow(
                selected = selected == ExerciseSortMode.SETS,
                title = stringResource(R.string.exercises_sort_sets),
                description = stringResource(R.string.exercises_sort_sets_desc),
                onClick = { onSelect(ExerciseSortMode.SETS) },
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.exercises_sort_cancel))
            }
        }
    }
}

@Composable
private fun ExerciseSortRow(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
