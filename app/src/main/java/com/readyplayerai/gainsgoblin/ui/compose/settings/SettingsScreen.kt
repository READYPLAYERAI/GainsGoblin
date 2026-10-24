package com.readyplayerai.gainsgoblin.ui.compose.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.data.backup.BackupFormat
import com.readyplayerai.gainsgoblin.data.backup.ImportMode
import com.readyplayerai.gainsgoblin.data.backup.ImportPreview
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar

/**
 * Settings screen scaffold with the shared M3 Expressive [FlexibleHeaderBar],
 * a primaryContainer backup hero with connected Export/Import actions and an
 * inline undo row, then Exercises-matched list rows for About and a final
 * errorContainer Clear data action.
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
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header_backup") {
                SettingsSectionHeader(stringResource(R.string.settings_section_backup))
            }
            item(key = "backup_hero") {
                BackupHero(
                    hasUndoBackup = hasUndoBackup,
                    onExportClick = onExportClick,
                    onImportClick = { showImportWarning = true },
                    onUndoClick = { showUndoWarning = true },
                )
            }
            item(key = "header_about") {
                SettingsSectionHeader(stringResource(R.string.settings_section_about))
            }
            item(key = "about_rows") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_source_code_title),
                        summary = stringResource(R.string.settings_source_code_summary),
                        icon = R.drawable.ic_code_24px,
                        trailingIcon = R.drawable.ic_open_in_new_24px,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        summaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onSourceCodeClick,
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_licence_title),
                        summary = stringResource(R.string.settings_licence_summary),
                        icon = R.drawable.ic_menu_book_24px,
                        trailingIcon = R.drawable.ic_open_in_new_24px,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        summaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onLicenceClick,
                    )
                }
            }
            item(key = "header_danger") {
                SettingsSectionHeader(
                    stringResource(R.string.settings_section_danger),
                    danger = true,
                )
            }
            item(key = "clear") {
                SettingsRow(
                    title = stringResource(R.string.settings_clear_data_title),
                    summary = stringResource(R.string.settings_clear_data_summary),
                    icon = R.drawable.ic_delete_24px,
                    trailingIcon = R.drawable.ic_keyboard_arrow_right_24px,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    summaryColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                    iconContainerColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.12f),
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = { showDeleteWarning = true },
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

@Composable
private fun SettingsSectionHeader(text: String, danger: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (danger) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    summary: String,
    @DrawableRes icon: Int,
    @DrawableRes trailingIcon: Int,
    containerColor: Color,
    contentColor: Color,
    summaryColor: Color,
    iconContainerColor: Color,
    iconTint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconTint,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight(500),
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = summaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun BackupHero(
    hasUndoBackup: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onUndoClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cloud_upload_24px),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_hero_kicker),
                        fontSize = 11.sp,
                        fontWeight = FontWeight(700),
                        letterSpacing = 1.1.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                    Text(
                        text = stringResource(R.string.settings_hero_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight(650),
                        lineHeight = 26.sp,
                        letterSpacing = (-0.2).sp,
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_hero_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Button(
                    onClick = onExportClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        bottomStart = 20.dp,
                        topEnd = 8.dp,
                        bottomEnd = 8.dp,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save_24px),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_export_action),
                        fontSize = 14.sp,
                        fontWeight = FontWeight(650),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalButton(
                    onClick = onImportClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        bottomStart = 8.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                    ),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_backup_restore_24px),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_import_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight(650),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = hasUndoBackup, onClick = onUndoClick)
                    .alpha(if (hasUndoBackup) 1f else 0.4f)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_history_24px),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_undo_import_title),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight(650),
                    )
                    Text(
                        text = stringResource(R.string.settings_undo_import_summary),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(
                        if (hasUndoBackup) {
                            R.string.settings_undo_action
                        } else {
                            R.string.settings_undo_unavailable
                        },
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight(700),
                )
            }
        }
    }
}
