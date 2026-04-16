package com.cupons.sms.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.entity.CouponEntity
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
    private val prefs: AppPreferences
) {
    companion object {
        private const val TAG            = "ExportManager"
        private const val BACKUP_VERSION = 1
    }

    /**
     * מייצא את כל הקופונים לקובץ JSON ב-Downloads.
     * מחזיר את הנתיב לקובץ שנוצר, או null אם נכשל.
     */
    suspend fun exportToJson(): File? {
        return try {
            val coupons = couponDao.getAllActive()
            val json = buildJson(coupons)

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "qponix_backup_$dateStr.json"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloadsDir.mkdirs()

            val file = File(downloadsDir, fileName)
            file.writeText(json.toString(2))

            prefs.saveLastBackupTimestamp(System.currentTimeMillis())
            Log.i(TAG, "Exported ${coupons.size} coupons to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            null
        }
    }

    private fun buildJson(coupons: List<CouponEntity>): JSONObject {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("count", coupons.size)

        val array = JSONArray()
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
            array.put(obj)
        }
        root.put("coupons", array)
        return root
    }
}
