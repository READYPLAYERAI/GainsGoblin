package com.readyplayerai.gainsgoblin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.data.backup.BackupExportResult
import com.readyplayerai.gainsgoblin.data.backup.ImportMode
import com.readyplayerai.gainsgoblin.data.backup.ImportPreview
import com.readyplayerai.gainsgoblin.data.backup.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _hasUndoBackup = MutableStateFlow(false)
    val hasUndoBackup: StateFlow<Boolean> = _hasUndoBackup.asStateFlow()

    init {
        viewModelScope.launch { refreshBackupState() }
    }

    suspend fun clearWorkouts() = repository.clearWorkouts()

    suspend fun previewImport(uri: Uri): ImportPreview = repository.previewImport(uri)

    suspend fun importBackup(uri: Uri, mode: ImportMode): ImportResult {
        val result = repository.importBackup(uri, mode)
        refreshBackupState()
        return result
    }

    suspend fun exportBackup(uri: Uri): BackupExportResult = repository.exportBackup(uri)

    suspend fun undoLastImport(): Boolean {
        val result = repository.undoLastImport()
        refreshBackupState()
        return result
    }

    suspend fun refreshBackupState() {
        _hasUndoBackup.value = repository.lastImportBackupName() != null
    }
}
