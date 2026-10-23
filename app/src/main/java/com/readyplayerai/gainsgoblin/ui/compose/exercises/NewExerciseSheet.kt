package com.readyplayerai.gainsgoblin.ui.compose.exercises

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewExerciseSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String, onResult: (Boolean) -> Unit) -> Unit,
    onAdded: (String) -> Unit,
) {
    val categories = stringArrayResource(R.array.Categories)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val nameInvalid = stringResource(R.string.exercises_name_invalid)
    val duplicate = stringResource(R.string.exercises_duplicate)
    val chooseBodyPart = stringResource(R.string.exercises_choose_body_part)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.exercises_new_exercise),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.exercises_new_exercise_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
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
                            selectedCategory = category
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
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.exercises_cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        val category = selectedCategory
                        when {
                            trimmed.isEmpty() -> error = nameInvalid
                            category == null -> error = chooseBodyPart
                            else -> onAdd(trimmed, category) { success ->
                                if (success) {
                                    onAdded(trimmed)
                                    onDismiss()
                                } else {
                                    error = duplicate
                                }
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.exercises_add_exercise))
                }
            }
        }
    }
}
