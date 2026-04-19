package com.cupons.sms.data.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmsReader — קורא את כל הודעות ה-SMS מה-ContentProvider.
 *
 * אסטרטגיית כפילויות:
 * - סורק את כל ה-inbox בכל פעם (ללא LIMIT ללא timestamp)
 * - מדלג על IDs שכבר טופלו (יובאו / נדחו) — מועבר ב-idsToSkip
 * - כך אף קופון לא מפוספס, גם אם הגיע באיחור
 */
@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: SmsParser
) {
    companion object {
        private const val TAG = "SmsReader"
    }

    /**
     * סורק את ה-Inbox וה-Sent ומחזיר הודעות שזוהו כקופונים.
     *
     * שני מקורות נסרקים כדי לתפוס גם הודעות ששלחת לעצמך (נשמרות ב-Sent).
     *
     * @param idsToSkip  מזהי SMS לדלג עליהם (כבר יובאו או נדחו בעבר)
     */
    fun readAll(idsToSkip: Set<String>, customKeywords: List<String> = emptyList()): List<RawSmsMessage> {
        val results = mutableListOf<RawSmsMessage>()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )

        // נסרוק גם Inbox וגם Sent — הודעה ששלחת לעצמך תופיע ב-Sent
        val urisToScan = listOf(
            Telephony.Sms.Inbox.CONTENT_URI to "Inbox",
            Telephony.Sms.Sent.CONTENT_URI  to "Sent"
        )

        for ((uri, label) in urisToScan) {
            try {
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC"
                )?.use { cursor ->

                    val idCol   = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addrCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                    Log.d(TAG, "[$label] Total SMS: ${cursor.count} | idsToSkip: ${idsToSkip.size}")

                    while (cursor.moveToNext()) {
                        val smsId  = cursor.getString(idCol) ?: continue
                        val sender = cursor.getString(addrCol) ?: ""
                        val body   = cursor.getString(bodyCol) ?: ""
                        val date   = cursor.getLong(dateCol)

                        // דלג על IDs שכבר טופלו — מניעת כפילויות
                        if (smsId in idsToSkip) continue

                        // נסה לפרסר
                        val parsed = parser.parse(body, sender, date, customKeywords) ?: continue

                        results.add(RawSmsMessage(smsId, sender, body, date, parsed))
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "[$label] Missing READ_SMS permission", e)
            } catch (e: Exception) {
                Log.e(TAG, "[$label] Error reading SMS", e)
            }
        }

        Log.d(TAG, "Found ${results.size} new coupon candidates total")
        return results
    }
}

data class RawSmsMessage(
    val smsId: String,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val parsed: ParsedSmsData
)
