package com.cupons.sms.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.data.prefs.BackupFrequency
import com.cupons.sms.data.sms.SmsParser
import com.cupons.sms.util.ExportManager
import com.cupons.sms.util.ImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val notificationsEnabled  : Boolean         = true,
    val notificationDaysBefore: Int             = 3,
    val backupFrequency       : BackupFrequency = BackupFrequency.OFF,
    val driveBackupEnabled    : Boolean         = false,
    val whatsappEnabled       : Boolean         = false,
    val lastBackupTimestamp   : Long            = 0L
)

data class BackupUiState(
    val isBackingUp  : Boolean = false,
    val isRescanning : Boolean = false,
    val backupMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs        : AppPreferences,
    private val exportManager: ExportManager,
    private val importManager: ImportManager,
    private val dao          : CouponDao,
    private val smsParser    : SmsParser
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.notificationsEnabled,
        prefs.notificationDaysBefore,
        prefs.backupFrequency,
        prefs.driveBackupEnabled,
        prefs.whatsappEnabled
    ) { notifEnabled, days, freq, driveEnabled, waEnabled ->
        SettingsUiState(
            notificationsEnabled   = notifEnabled,
            notificationDaysBefore = days,
            backupFrequency        = freq,
            driveBackupEnabled     = driveEnabled,
            whatsappEnabled        = waEnabled
        )
    }.combine(prefs.lastBackupTimestamp) { state, ts ->
        state.copy(lastBackupTimestamp = ts)
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }

    fun setNotificationDaysBefore(days: Int) {
        viewModelScope.launch { prefs.setNotificationDaysBefore(days) }
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch { prefs.setBackupFrequency(frequency) }
    }

    fun setDriveBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setDriveBackupEnabled(enabled) }
    }

    fun setWhatsappEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setWhatsappEnabled(enabled) }
    }

    fun backupNow() {
        viewModelScope.launch {
            _backupState.update { it.copy(isBackingUp = true, backupMessage = null) }
            val fileName = exportManager.exportToJson()
            _backupState.update {
                it.copy(
                    isBackingUp   = false,
                    backupMessage = if (fileName != null) "גובה בהצלחה ל: $fileName" else "הגיבוי נכשל"
                )
            }
        }
    }

    fun restoreFromFile(uri: Uri) {
        viewModelScope.launch {
            _backupState.update { it.copy(isBackingUp = true, backupMessage = null) }
            val result = importManager.importFromUri(uri)
            _backupState.update {
                it.copy(
                    isBackingUp   = false,
                    backupMessage = result.error
                        ?: "שוחזרו ${result.imported} קופונים (${result.skipped} כפולים דולגו)"
                )
            }
        }
    }

    fun dismissBackupMessage() {
        _backupState.update { it.copy(backupMessage = null) }
    }

    /**
     * סריקה מחדש של תאריכי תפוגה — מעדכן קופונים קיימים ב-DB שאין להם תאריך.
     * משתמש ב-EXPIRY_PATTERNS המעודכנים כדי לחלץ תאריך מגוף ה-SMS המקורי.
     */
    fun rescanExpiryDates() {
        viewModelScope.launch {
            _backupState.update { it.copy(isRescanning = true, backupMessage = null) }
            try {
                val coupons = dao.getCouponsWithNoExpiry()
                var updated = 0
                coupons.forEach { entity ->
                    val body = entity.rawSmsBody ?: return@forEach
                    val expiresAt = smsParser.extractExpiryOnly(body, entity.receivedAt)
                    if (expiresAt != null) {
                        dao.updateExpiresAt(entity.id, expiresAt)
                        updated++
                    }
                }
                _backupState.update {
                    it.copy(
                        isRescanning  = false,
                        backupMessage = if (updated > 0)
                            "עודכנו $updated קופונים עם תאריך תפוגה ✓"
                        else
                            "לא נמצאו קופונים לעדכון (${coupons.size} נבדקו)"
                    )
                }
            } catch (e: Exception) {
                _backupState.update {
                    it.copy(isRescanning = false, backupMessage = "שגיאה בסריקה: ${e.message}")
                }
            }
        }
    }
}
