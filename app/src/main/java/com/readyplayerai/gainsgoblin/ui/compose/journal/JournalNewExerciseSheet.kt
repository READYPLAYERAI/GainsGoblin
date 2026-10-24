package com.readyplayerai.gainsgoblin.ui.compose.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker

/**
 * Journal-local bottom sheet for creating a new library exercise, prefilled with
 * the current search query. Validates name/category and reports duplicate names
 * through the [onAdd] result callback.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun JournalNewExerciseSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String, onResult: (Boolean) -> Unit) -> Unit,
    onAdded: (name: String, category: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = stringArrayResource(R.array.Categories)
    val invalidMessage = stringResource(R.string.exercises_name_invalid)
    val duplicateMessage = stringResource(R.string.journal_exercise_duplicate)

    var name by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.top_bar_add_exercise),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            )
            Text(
                text = stringResource(R.string.journal_new_exercise_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text(stringResource(R.string.exercises_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.exercises_category_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val selected = selectedCategory.equals(category, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCategory = if (selected) null else category
                            error = null
                        },
                        label = { Text(category) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24px),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            {
                                CategoryMarker(
                                    category = category,
                                    size = FilterChipDefaults.IconSize,
                                )
                            }
                        },
                    )
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.exercises_cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        val category = selectedCategory
                        if (trimmed.isEmpty() || category == null) {
                            error = invalidMessage
                        } else {
                            onAdd(trimmed, category) { success ->
                                if (success) {
                                    onAdded(trimmed, category)
                                } else {
                                    error = duplicateMessage
                                }
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.journal_add_exercise))
                }
            }
        }
    }
}
