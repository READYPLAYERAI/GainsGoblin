package com.readyplayerai.gainsgoblin.ui.compose.exercises

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.Exercise

private enum class EditAction { Rename, ChangeCategory }

/**
 * M3 Expressive bottom sheet with the exercise's actions (rename, change
 * category, delete), an inline edit view and a delete confirmation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseActionSheet(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onRename: (oldName: String, newName: String, newCategory: String?) -> Unit,
    onChangeCategory: (oldName: String, newName: String, newCategory: String?) -> Unit,
    onDelete: (name: String) -> Unit,
) {
    val context = LocalContext.current
    val categories = stringArrayResource(R.array.Categories)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var editAction by remember { mutableStateOf(EditAction.Rename) }
    var editName by remember(exercise) { mutableStateOf(exercise.Name.orEmpty()) }
    var selectedCategory by remember(exercise) {
        mutableStateOf(categories.firstOrNull { it.equals(exercise.BodyPart, ignoreCase = true) })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        if (showEdit) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(stringResource(R.string.exercises_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                            onClick = { selectedCategory = if (selected) null else category },
                            label = { Text(category) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_24px),
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showEdit = false }) {
                        Text(stringResource(R.string.exercises_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newName = editName.trim()
                            val newCategory = selectedCategory
                            if (newName.isEmpty() || newCategory == null) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.exercises_name_invalid),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                when (editAction) {
                                    EditAction.Rename ->
                                        onRename(exercise.Name.orEmpty(), newName, newCategory)

                                    EditAction.ChangeCategory ->
                                        onChangeCategory(exercise.Name.orEmpty(), newName, newCategory)
                                }
                                onDismiss()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.exercises_save))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = exercise.Name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                )
                SheetActionRow(
                    label = stringResource(R.string.exercises_rename),
                    iconRes = R.drawable.ic_create_24px,
                    onClick = {
                        editName = exercise.Name.orEmpty()
                        selectedCategory = categories.firstOrNull {
                            it.equals(exercise.BodyPart, ignoreCase = true)
                        }
                        editAction = EditAction.Rename
                        showEdit = true
                    },
                )
                SheetActionRow(
                    label = stringResource(R.string.exercises_change_category),
                    iconRes = R.drawable.ic_dashboard_24px,
                    onClick = {
                        editName = exercise.Name.orEmpty()
                        selectedCategory = categories.firstOrNull {
                            it.equals(exercise.BodyPart, ignoreCase = true)
                        }
                        editAction = EditAction.ChangeCategory
                        showEdit = true
                    },
                )
                SheetActionRow(
                    label = stringResource(R.string.exercises_delete),
                    iconRes = R.drawable.ic_delete_24px,
                    isError = true,
                    onClick = { showDelete = true },
                )
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.exercises_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.exercises_delete_body,
                        exercise.Name.orEmpty(),
                    ),
                )
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.exercises_cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(exercise.Name.orEmpty())
                        showDelete = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.exercises_delete))
                }
            },
        )
    }
}

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
