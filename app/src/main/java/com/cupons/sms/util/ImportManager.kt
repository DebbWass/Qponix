package com.cupons.sms.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.UsageLogDao
import com.cupons.sms.data.db.entity.CouponEntity
import com.cupons.sms.data.db.entity.UsageLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(
    val imported : Int = 0,
    val skipped  : Int = 0,
    val error    : String? = null
)

@Singleton
class ImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val couponDao: CouponDao,
    private val usageLogDao: UsageLogDao
) {
    companion object {
        private const val TAG = "ImportManager"
        private val SUPPORTED_VERSIONS = 1..2
    }

    /**
     * מייבא קופונים (ובגרסה 2 — גם יומן שימוש) מקובץ JSON.
     * קופונים קיימים (לפי smsId) מדולגים.
     */
    suspend fun importFromUri(uri: Uri): ImportResult {
        return try {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText()
                ?: return ImportResult(error = "לא ניתן לקרוא את הקובץ")

            importFromJson(content)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            ImportResult(error = "שגיאה בייבוא: ${e.message}")
        }
    }

    /**
     * ליבת הייבוא — מקבל מחרוזת JSON. מופרד לצורך בדיקות יחידה.
     */
    suspend fun importFromJson(content: String): ImportResult {
        val root    = JSONObject(content)
        val version = root.optInt("version", 0)
        if (version !in SUPPORTED_VERSIONS) {
            return ImportResult(error = "פורמט גיבוי לא נתמך (version=$version)")
        }

        val array      = root.getJSONArray("coupons")
        var imported   = 0
        var skipped    = 0

        val existingIds = couponDao.getAllSmsIds().toSet()
        // מיפוי id ישן (מהגיבוי) ל-rowId חדש — לצורך שיוך יומן השימוש
        val oldToNewCouponId = HashMap<Long, Long>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val oldId = obj.optLong("id", 0L)
            val smsId = obj.optString("smsId").takeIf { it != "null" && it.isNotBlank() }

            // דלג אם ה-SMS ID כבר קיים
            if (smsId != null && smsId in existingIds) {
                skipped++
                continue
            }

            val entity = CouponEntity(
                smsId            = smsId,
                sender           = obj.optString("sender", ""),
                rawSmsBody       = obj.optString("rawSmsBody").takeIf { it != "null" },
                couponCode       = obj.optString("couponCode", ""),
                originalAmount   = obj.optDouble("originalAmount").takeIf { !it.isNaN() },
                remainingBalance = obj.optDouble("remainingBalance").takeIf { !it.isNaN() },
                currency         = obj.optString("currency", "₪"),
                merchantName     = obj.optString("merchantName").takeIf { it != "null" },
                websiteUrl       = obj.optString("websiteUrl").takeIf { it != "null" },
                receivedAt       = obj.optLong("receivedAt", System.currentTimeMillis()),
                expiresAt        = obj.optLong("expiresAt", 0).takeIf { it > 0 },
                isUsed           = obj.optBoolean("isUsed", false),
                isArchived       = obj.optBoolean("isArchived", false),
                isDeleted        = obj.optBoolean("isDeleted", false),
                notes            = obj.optString("notes").takeIf { it != "null" },
                confidence       = obj.optDouble("confidence", 0.5).toFloat(),
                isPending        = obj.optBoolean("isPending", false),
                createdAt        = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt        = obj.optLong("updatedAt", System.currentTimeMillis())
            )

            val newId = couponDao.insert(entity)
            if (newId > 0) {
                imported++
                if (oldId > 0L) oldToNewCouponId[oldId] = newId
            } else {
                skipped++
            }
        }

        // גרסה 2: ייבוא יומן השימוש, ממופה ל-id הקופון החדש
        if (version >= 2) {
            val logs = root.optJSONArray("usageLogs")
            if (logs != null) {
                for (i in 0 until logs.length()) {
                    val obj = logs.getJSONObject(i)
                    val oldCouponId = obj.optLong("couponId", 0L)
                    val newCouponId = oldToNewCouponId[oldCouponId] ?: continue
                    usageLogDao.insert(
                        UsageLogEntity(
                            couponId     = newCouponId,
                            amountUsed   = obj.optDouble("amountUsed", 0.0),
                            balanceAfter = obj.optDouble("balanceAfter", 0.0),
                            usedAt       = obj.optLong("usedAt", System.currentTimeMillis()),
                            note         = obj.optString("note").takeIf { it != "null" }
                        )
                    )
                }
            }
        }

        Log.i(TAG, "Import complete: imported=$imported, skipped=$skipped")
        return ImportResult(imported = imported, skipped = skipped)
    }
}
