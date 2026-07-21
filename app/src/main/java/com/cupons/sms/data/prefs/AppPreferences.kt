package com.cupons.sms.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore instance — singleton ברמת האפליקציה */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/** תדירות גיבוי אוטומטי */
enum class BackupFrequency(val label: String) {
    OFF("כבוי"),
    DAILY("יומי"),
    WEEKLY("שבועי"),
    MONTHLY("חודשי")
}

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val REJECTED_SMS_IDS        = stringPreferencesKey("rejected_sms_ids")
        private val LAST_SCAN_TIMESTAMP     = longPreferencesKey("last_scan_timestamp")

        // ─── הגדרות התראות ───
        private val NOTIFICATIONS_ENABLED   = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_DAYS_BEFORE= intPreferencesKey("notification_days_before")

        // ─── הגדרות גיבוי ───
        private val BACKUP_FREQUENCY        = stringPreferencesKey("backup_frequency")
        private val DRIVE_BACKUP_ENABLED    = booleanPreferencesKey("drive_backup_enabled")
        private val LAST_BACKUP_TIMESTAMP   = longPreferencesKey("last_backup_timestamp")

        // ─── מילות מפתח מותאמות ───
        private val CUSTOM_KEYWORDS         = stringPreferencesKey("custom_keywords")
        private val CUSTOM_BLACKLIST        = stringPreferencesKey("custom_blacklist")

        // ─── WhatsApp ───
        private val WHATSAPP_ENABLED        = booleanPreferencesKey("whatsapp_enabled")
    }

    // ─── Rejected SMS IDs ───

    suspend fun getRejectedSmsIds(): Set<String> {
        val raw = context.dataStore.data.first()[REJECTED_SMS_IDS] ?: return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    suspend fun addRejectedSmsId(smsId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[REJECTED_SMS_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet()
                ?: mutableSetOf()
            current.add(smsId)
            prefs[REJECTED_SMS_IDS] = current.joinToString(",")
        }
    }

    // ─── Last Scan Timestamp ───

    suspend fun getLastScanTimestamp(): Long =
        context.dataStore.data.first()[LAST_SCAN_TIMESTAMP] ?: 0L

    suspend fun saveLastScanTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs -> prefs[LAST_SCAN_TIMESTAMP] = timestamp }
    }

    // ─── הגדרות התראות ───

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }

    val notificationDaysBefore: Flow<Int> =
        context.dataStore.data.map { it[NOTIFICATION_DAYS_BEFORE] ?: 3 }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotificationDaysBefore(days: Int) {
        context.dataStore.edit { it[NOTIFICATION_DAYS_BEFORE] = days }
    }

    // ─── הגדרות גיבוי ───

    val backupFrequency: Flow<BackupFrequency> =
        context.dataStore.data.map {
            BackupFrequency.entries.find { e -> e.name == it[BACKUP_FREQUENCY] }
                ?: BackupFrequency.OFF
        }

    val driveBackupEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[DRIVE_BACKUP_ENABLED] ?: false }

    val lastBackupTimestamp: Flow<Long> =
        context.dataStore.data.map { it[LAST_BACKUP_TIMESTAMP] ?: 0L }

    suspend fun setBackupFrequency(frequency: BackupFrequency) {
        context.dataStore.edit { it[BACKUP_FREQUENCY] = frequency.name }
    }

    suspend fun setDriveBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DRIVE_BACKUP_ENABLED] = enabled }
    }

    suspend fun saveLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { it[LAST_BACKUP_TIMESTAMP] = timestamp }
    }

    // ─── מילות מפתח מותאמות ───

    val customKeywords: Flow<Set<String>> =
        context.dataStore.data.map {
            it[CUSTOM_KEYWORDS]?.split(",")?.filter { w -> w.isNotBlank() }?.toSet() ?: emptySet()
        }

    val customBlacklist: Flow<Set<String>> =
        context.dataStore.data.map {
            it[CUSTOM_BLACKLIST]?.split(",")?.filter { w -> w.isNotBlank() }?.toSet() ?: emptySet()
        }

    suspend fun setCustomKeywords(keywords: Set<String>) {
        context.dataStore.edit { it[CUSTOM_KEYWORDS] = keywords.joinToString(",") }
    }

    suspend fun setCustomBlacklist(words: Set<String>) {
        context.dataStore.edit { it[CUSTOM_BLACKLIST] = words.joinToString(",") }
    }

    /**
     * הוספה/הסרה אטומיות של מילות מפתח/רשימה שחורה.
     * כל ה-read-modify-write מתבצע בתוך edit{} יחיד (טרנזקציוני) —
     * מונע "lost update" כששתי הוספות מהירות קוראות אותו snapshot.
     * הקידוד הוא CSV, לכן פסיקים מנוקים בקלט (אחרת הם שוברים את הפירוק).
     */
    suspend fun addCustomKeyword(word: String) = addToCsvSet(CUSTOM_KEYWORDS, word)
    suspend fun removeCustomKeyword(word: String) = removeFromCsvSet(CUSTOM_KEYWORDS, word)
    suspend fun addBlacklistWord(word: String) = addToCsvSet(CUSTOM_BLACKLIST, word)
    suspend fun removeBlacklistWord(word: String) = removeFromCsvSet(CUSTOM_BLACKLIST, word)

    private suspend fun addToCsvSet(key: Preferences.Key<String>, rawWord: String) {
        val word = rawWord.trim().replace(",", "")
        if (word.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[key].toCsvSet()
            current.add(word)
            prefs[key] = current.joinToString(",")
        }
    }

    private suspend fun removeFromCsvSet(key: Preferences.Key<String>, rawWord: String) {
        val word = rawWord.trim().replace(",", "")
        context.dataStore.edit { prefs ->
            val current = prefs[key].toCsvSet()
            current.remove(word)
            prefs[key] = current.joinToString(",")
        }
    }

    private fun String?.toCsvSet(): MutableSet<String> =
        this?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()

    // ─── WhatsApp ───

    val whatsappEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[WHATSAPP_ENABLED] ?: false }

    suspend fun setWhatsappEnabled(enabled: Boolean) {
        context.dataStore.edit { it[WHATSAPP_ENABLED] = enabled }
    }
}
