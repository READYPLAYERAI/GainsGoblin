package com.readyplayerai.gainsgoblin.ui.compose.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.data.backup.BackupFormat
import com.readyplayerai.gainsgoblin.data.backup.ImportMode
import com.readyplayerai.gainsgoblin.data.backup.ImportPreview
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar

/**
 * Settings screen scaffold with the shared M3 Expressive [FlexibleHeaderBar]
 * collapsing app bar and segmented list rows for backup, restore, and about.
 *
 * The theme is provided by the caller (the hosting Activity wraps this in
 * [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    hasUndoBackup: Boolean,
    importPreview: ImportPreview?,
    onImportPick: () -> Unit,
    onImportMode: (ImportMode) -> Unit,
    onImportCancel: () -> Unit,
    onExportClick: () -> Unit,
    onUndoClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onSourceCodeClick: () -> Unit,
    onLicenceClick: () -> Unit,
) {
    var showImportWarning by remember { mutableStateOf(false) }
    var showDeleteWarning by remember { mutableStateOf(false) }
    var showUndoWarning by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    SideEffect {
        val limitPx = with(density) { -(152.dp - 64.dp).toPx() }
        if (scrollBehavior.state.heightOffsetLimit != limitPx) {
            scrollBehavior.state.heightOffsetLimit = limitPx
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlexibleHeaderBar(
                title = stringResource(R.string.nav_settings),
                subtitle = stringResource(R.string.settings_subtitle),
                collapseFraction = scrollBehavior.state.collapsedFraction,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header_backup") {
                SettingsSectionHeader(stringResource(R.string.settings_section_backup))
            }
            item(key = "import") {
                SettingsRow(
                    stringResource(R.string.settings_import_title),
                    stringResource(R.string.settings_import_summary),
                    R.drawable.ic_settings_backup_restore_24px,
                ) { showImportWarning = true }
            }
            item(key = "export") {
                SettingsRow(
                    stringResource(R.string.settings_export_title),
                    stringResource(R.string.settings_export_summary),
                    R.drawable.ic_save_24px,
                    onClick = onExportClick,
                )
            }
            item(key = "undo") {
                SettingsRow(
                    stringResource(R.string.settings_undo_import_title),
                    stringResource(R.string.settings_undo_import_summary),
                    R.drawable.ic_history_24px,
                    enabled = hasUndoBackup,
                ) { showUndoWarning = true }
            }
            item(key = "clear") {
                SettingsRow(
                    stringResource(R.string.settings_clear_data_title),
                    stringResource(R.string.settings_clear_data_summary),
                    R.drawable.ic_delete_24px,
                ) { showDeleteWarning = true }
            }
            item(key = "header_about") {
                SettingsSectionHeader(stringResource(R.string.settings_section_about))
            }
            item(key = "source") {
                SettingsRow(
                    stringResource(R.string.settings_source_code_title),
                    stringResource(R.string.settings_source_code_summary),
                    R.drawable.github,
                    onClick = onSourceCodeClick,
                )
            }
            item(key = "licence") {
                SettingsRow(
                    stringResource(R.string.settings_licence_title),
                    stringResource(R.string.settings_licence_summary),
                    R.drawable.ic_menu_book_24px,
                    onClick = onLicenceClick,
                )
            }
        }
    }

    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = { showImportWarning = false },
            text = { Text(stringResource(R.string.settings_import_warning_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportWarning = false
                        onImportPick()
                    },
                ) { Text(stringResource(R.string.settings_import_choose_file)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarning = false }) {
                    Text(stringResource(R.string.exercises_cancel))
                }
            },
        )
    }

    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            text = { Text(stringResource(R.string.settings_delete_warning_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteWarning = false
                        onDeleteConfirm()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.settings_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarning = false }) {
                    Text(stringResource(R.string.exercises_cancel))
                }
            },
        )
    }

    if (showUndoWarning) {
        AlertDialog(
            onDismissRequest = { showUndoWarning = false },
            text = { Text(stringResource(R.string.settings_undo_import_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUndoWarning = false
                        onUndoClick()
                    },
                ) { Text(stringResource(R.string.settings_undo_import_title)) }
            },
            dismissButton = {
                TextButton(onClick = { showUndoWarning = false }) {
                    Text(stringResource(R.string.exercises_cancel))
                }
            },
        )
    }

    if (importPreview != null) {
        AlertDialog(
            onDismissRequest = onImportCancel,
            title = { Text(stringResource(R.string.settings_import_mode_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (importPreview.format) {
                            BackupFormat.GAINS_GOBLIN_JSON ->
                                stringResource(R.string.settings_import_format_json)
                            BackupFormat.GAINS_GOBLIN_CSV ->
                                stringResource(R.string.settings_import_format_legacy)
                            BackupFormat.FITNOTES_CSV ->
                                stringResource(R.string.settings_import_format_fitnotes)
                        }
                    )
                    Text(
                        stringResource(
                            R.string.settings_import_mode_summary,
                            importPreview.setCount,
                            importPreview.exerciseCount,
                        )
                    )
                    importPreview.warnings.forEach { warning ->
                        Text(warning)
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = { onImportMode(ImportMode.REPLACE) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text(stringResource(R.string.settings_import_replace)) }
                    TextButton(onClick = { onImportMode(ImportMode.MERGE) }) {
                        Text(stringResource(R.string.settings_import_merge))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onImportCancel) {
                    Text(stringResource(R.string.exercises_cancel))
                }
            },
        )
    }
}

/** Section label above a settings group. */
@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )
}

/**
 * M3 Expressive segmented list item: 16dp rounded container on
 * `surfaceContainerHighest`, 64dp min height, leading decorative icon, and a
 * two-line title + summary.
 */
@Composable
private fun SettingsRow(
    title: String,
    summary: String,
    @DrawableRes icon: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
