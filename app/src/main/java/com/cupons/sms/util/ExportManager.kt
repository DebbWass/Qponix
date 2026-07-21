package com.cupons.sms.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.UsageLogDao
import com.cupons.sms.data.db.entity.CouponEntity
import com.cupons.sms.data.db.entity.UsageLogEntity
import com.cupons.sms.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val couponDao: CouponDao,
    private val usageLogDao: UsageLogDao,
    private val prefs: AppPreferences
) {
    companion object {
        private const val TAG            = "ExportManager"
        // גרסה 2: נוסף מערך usageLogs (יומן שימוש) לצד הקופונים
        const val BACKUP_VERSION = 2
        private const val MIME_JSON = "application/json"
    }

    /**
     * מייצא את כל הקופונים ויומן השימוש לקובץ JSON בתיקיית ההורדות.
     * מחזיר את שם הקובץ שנוצר, או null אם נכשל.
     */
    suspend fun exportToJson(): String? {
        return try {
            val coupons   = couponDao.getAllActive()
            val usageLogs = usageLogDao.getAll()
            val json      = buildBackupJson(coupons, usageLogs)

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "qponix_backup_$dateStr.json"

            val displayName = writeToDownloads(fileName, json.toString(2))
            if (displayName != null) {
                prefs.saveLastBackupTimestamp(System.currentTimeMillis())
                Log.i(TAG, "Exported ${coupons.size} coupons + ${usageLogs.size} logs to $displayName")
            }
            displayName
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            null
        }
    }

    /**
     * בונה את מבנה ה-JSON לגיבוי — פונקציה טהורה, ניתנת לבדיקה ביחידה.
     */
    fun buildBackupJson(
        coupons: List<CouponEntity>,
        usageLogs: List<UsageLogEntity>
    ): JSONObject {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("count", coupons.size)

        val couponsArray = JSONArray()
        coupons.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("smsId", c.smsId ?: JSONObject.NULL)
            obj.put("sender", c.sender)
            obj.put("rawSmsBody", c.rawSmsBody ?: JSONObject.NULL)
            obj.put("couponCode", c.couponCode)
            obj.put("originalAmount", c.originalAmount ?: JSONObject.NULL)
            obj.put("remainingBalance", c.remainingBalance ?: JSONObject.NULL)
            obj.put("currency", c.currency)
            obj.put("merchantName", c.merchantName ?: JSONObject.NULL)
            obj.put("websiteUrl", c.websiteUrl ?: JSONObject.NULL)
            obj.put("receivedAt", c.receivedAt)
            obj.put("expiresAt", c.expiresAt ?: JSONObject.NULL)
            obj.put("isUsed", c.isUsed)
            obj.put("isArchived", c.isArchived)
            obj.put("isDeleted", c.isDeleted)
            obj.put("notes", c.notes ?: JSONObject.NULL)
            obj.put("confidence", c.confidence)
            obj.put("isPending", c.isPending)
            obj.put("createdAt", c.createdAt)
            obj.put("updatedAt", c.updatedAt)
            couponsArray.put(obj)
        }
        root.put("coupons", couponsArray)

        val logsArray = JSONArray()
        usageLogs.forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("couponId", l.couponId)
            obj.put("amountUsed", l.amountUsed)
            obj.put("balanceAfter", l.balanceAfter)
            obj.put("usedAt", l.usedAt)
            obj.put("note", l.note ?: JSONObject.NULL)
            logsArray.put(obj)
        }
        root.put("usageLogs", logsArray)
        return root
    }

    /**
     * כותב את התוכן לתיקיית ההורדות. מחזיר את שם הקובץ, או null בכישלון.
     *
     * API 29+: MediaStore (Scoped Storage) — ללא צורך בהרשאה.
     * API 26–28: כתיבה ישירה עם הרשאת WRITE_EXTERNAL_STORAGE (maxSdk 28).
     */
    private fun writeToDownloads(fileName: String, content: String): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, MIME_JSON)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return null
            fileName
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(content)
            file.name
        }
    }
}
