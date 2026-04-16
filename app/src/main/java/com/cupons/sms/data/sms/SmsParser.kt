package com.cupons.sms.data.sms

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmsParser — מנוע חילוץ נתונים מהודעות SMS.
 *
 * לוגיקת סינון (לפי סדר):
 * 0. חסימה מיידית: מילות שחורה = פרסומת ודאית
 * 1. תנאי הכרחי: מילת מפתח חזקה וספציפית לקופון/גיפטקארד
 * 2. תנאי הכרחי: קוד קופון ממשי בגוף ההודעה
 * 3. חישוב confidence מהמאפיינים הנוספים
 */
@Singleton
class SmsParser @Inject constructor() {

    companion object {
        private const val TAG = "SmsParser"

        const val CONFIDENCE_AUTO_IMPORT = 0.6f
        const val CONFIDENCE_SUGGEST     = 0.4f

        // ─── 0. רשימה שחורה — פרסומות ודאיות ───
        // אם מילה מהרשימה קיימת → חסימה מיידית ללא בדיקה נוספת
        private val PROMO_BLACKLIST = listOf(
            // קריאה לפעולה שיווקית = לא קופון
            "הירשם עכשיו", "הצטרף עכשיו", "הורד את האפליקציה", "הורידו את האפליקציה",
            "להסרה מהרשימה", "להסרה מרשימת", "unsubscribe", "stop to opt", "reply stop",
            "opt out", "להפסקת קבלת",
            // אישורי עסקה (לא קופון)
            "ההזמנה שלך", "הזמנתך", "אישור הזמנה", "חשבונית", "קבלה על",
            "your order", "order confirmation", "invoice",
            // קוד OTP / אימות — לא קופון!
            "קוד אימות", "קוד אחד", "קוד זמני", "קוד סיסמה", "verification code",
            "otp", "one-time", "one time password", "קוד כניסה", "קוד חד פעמי"
        )

        // ─── 1. מילות מפתח חזקות — תנאי הכרחי ───
        // המילים הספציפיות ביותר שמופיעות בהודעות קופון/גיפטקארד ישראליות
        private val STRONG_COUPON_KEYWORDS = listOf(
            // ─ עברית: קופון ─
            "קופון", "קוד קופון", "קוד הקופון", "קוד ההטבה", "קוד הנחה",
            "קוד מתנה", "קוד פרומו",
            // ─ עברית: גיפטקארד ─
            "גיפטקארד", "גיפט קארד", "כרטיס מתנה", "כרטיס גיפט",
            // ─ עברית: כרטיסי תו/מולטיפאס ─
            "מולטיפאס", "תו קניה", "תו קנייה",
            // ─ עברית: ביטויים שמעידים על קוד שניתן לשימוש ─
            "הקוד שלך", "הקוד שלך הוא", "הקוד הוא", "הקוד:", "השתמש בקוד",
            "לממש", "לפדות", "מימוש",
            // ─ אנגלית ─
            "coupon", "gift card", "giftcard", "gift-card",
            "voucher", "promo code", "discount code", "redemption code",
            "your code", "redeem", "use code", "enter code",
            // ─ לינקים מיוחדים ─
            "matana4u",
            "buyme", "buyme.co.il", "buy me", "buy-me"
        )

        // ─── 2. Patterns לחילוץ קוד קופון ───
        // חשוב: הסדר קובע — הספציפיים קודמים
        private val COUPON_CODE_PATTERNS = listOf(

            // Pattern 1: "קוד קופון: ABC123" / "קוד ההטבה: ABC123"
            Regex(
                """(?:קוד\s*(?:קופון|הקופון|ההטבה|הנחה|מתנה|פרומו|:))\s*[:\-]?\s*([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 2: "הקוד שלך: ABC123" / "הקוד הוא ABC123"
            Regex(
                """(?:הקוד\s*(?:שלך|הוא|למימוש)?)\s*[:\-]?\s*([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 3: "השתמש בקוד ABC123" / "use code ABC123"
            Regex(
                """(?:השתמש\s+בקוד|use\s+code|enter\s+code|apply\s+code)\s+([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 4: "coupon: ABC123" / "code: ABC123"
            Regex(
                """(?:coupon|code|promo|voucher|giftcode)\s*[:\-]\s*([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 5a: "קופון: ABC123" / "גיפטקארד: ABC123" / "גיפטקארד שלך: ABC123"
            // מכסה מקרים שבהם אין "קוד" לפני מילת המפתח העברית.
            // [^\s:]+ — כל תו שאינו רווח/נקודתיים (כולל אותיות עברית; \w לא מתאים לעברית ב-JVM)
            Regex(
                """(?:קופון|גיפטקארד|גיפט\s*קארד|כרטיס\s*מתנה)\s*(?:[^\s:]+\s*)?[:\-]\s*(?!https?://)([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 5b: "קוד קופון שלך: ABC123" — מילת מפתח + מילה אמצעית אופציונלית (עברית)
            Regex(
                """(?:קוד\s*(?:קופון|הקופון|ההטבה|הנחה|מתנה|פרומו))\s*(?:[^\s:]+\s*)?[:\-]\s*(?!https?://)([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 5c: קוד בתוך גרשיים מיד אחרי מילת מפתח
            // "הקופון שלך הוא "ABC123""
            Regex(
                """(?:קופון|coupon|code|קוד)\s*[:\-]?\s*["«»„"'`]([A-Z0-9][A-Z0-9\-]{3,24})["»"'`]""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 6: "#ABC123" — כשיש # לפני הקוד
            Regex("""#([A-Z][A-Z0-9]{3,19})\b"""),

            // Pattern 7: קוד אחרי gift card / giftcard
            Regex(
                """(?:gift\s*card|giftcard|voucher)\s*[:\-]?\s*([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 8: קוד מתוך URL של matana4u — למשל https://matana4u.co.il/redeem/ABC123
            Regex(
                """matana4u\.[a-z.]+/(?:[^/\s]+/)*([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 8b: קוד מתוך URL של buyme — https://buyme.co.il/link/ABC123
            Regex(
                """buyme\.co\.il/(?:[^/\s]+/)*([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 9: קוד כפרמטר URL — ?code=ABC123 / &coupon=ABC123
            Regex(
                """[?&](?:code|coupon|voucher|gift)=([A-Z0-9][A-Z0-9\-]{3,24})""",
                RegexOption.IGNORE_CASE
            ),
        )

        // ─── Patterns לחילוץ סכום ───
        private val AMOUNT_PATTERNS = listOf(
            Regex("""₪\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""(\d+(?:\.\d{1,2})?)\s*₪"""),
            Regex("""(\d+(?:\.\d{1,2})?)\s*(?:ש["\u2019]ח|שח|שקל(?:ים)?)"""),
            Regex("""(?:NIS)\s*(\d+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+(?:\.\d{1,2})?)\s*(?:NIS)""", RegexOption.IGNORE_CASE),
            Regex("""\$\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""(?:בשווי|ערך|worth)\s*[:\-]?\s*(\d+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        )

        // ─── Patterns לחילוץ מטבע ───
        private val CURRENCY_PATTERNS = mapOf(
            Regex("""₪|ש["\u2019]ח|שח|שקל(?:ים)?|NIS""") to "₪",
            Regex("""\$|USD""", RegexOption.IGNORE_CASE) to "$",
            Regex("""€|EUR""", RegexOption.IGNORE_CASE) to "€",
        )

        // ─── Patterns לחילוץ תאריך תפוגה ───
        // חשוב: הסדר קובע — patterns עם הקשר מילולי קודמים לפטרנים גנריים
        private val EXPIRY_PATTERNS = listOf(

            // Pattern 1: "תוקף עד" / "בתוקף עד" / "valid until" / "expires" + תאריך
            // מכסה: "בתוקף עד 31/12/2026", "valid until 2026-12-31"
            Regex(
                """(?:תוקף\s+עד|בתוקף\s+עד|valid\s+until|expires?(?:\s+on)?|עד\s+תאריך)\s*[:\-]?\s*(\d{1,2}[/.\-]\d{1,2}(?:[/.\-]\d{2,4})?)""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 2: "עד ה" + ספרות — עברית עם ה' הידיעה + ניקוד אפשרי
            // מכסה: "עד ה-4.4.26", "עד ה- 9.4", "עד ה 4/4", "עד ה4.4.26"
            Regex(
                """עד\s+ה['\u05be\-\s]*(\d{1,2}[/.\-]\d{1,2}(?:[/.\-]\d{2,4})?)"""
            ),

            // Pattern 3: "בתוקף" לבד (ללא "עד") + תאריך
            // מכסה: "בתוקף 24.2.26", "תוקף עד 9.4"
            Regex(
                """(?:בתוקף|תוקף)\s+(\d{1,2}[/.\-]\d{1,2}(?:[/.\-]\d{2,4})?)""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 4: "עד" + תאריך (ישיר, ללא ה')
            // מכסה: "עד 11.4", "עד 10.3.26", "עד 31/12/2025"
            Regex(
                """(?<!\p{L})עד\s+(\d{1,2}[/.\-]\d{1,2}(?:[/.\-]\d{2,4})?)(?![/.\-\d])"""
            ),

            // Pattern 5: "until" / "by" + date (English)
            Regex(
                """(?:until|by|before)\s+(\d{1,2}[/.\-]\d{1,2}(?:[/.\-]\d{2,4})?)""",
                RegexOption.IGNORE_CASE
            ),

            // Pattern 6: תאריך עם שנה 4 ספרות — dd/mm/yyyy  (generic fallback)
            Regex("""(\d{1,2}[/.\-]\d{1,2}[/.\-]\d{4})"""),

            // Pattern 7: תאריך עם שנה 2 ספרות — dd/mm/yy  (generic fallback)
            Regex("""(\d{1,2}[/.\-]\d{1,2}[/.\-]\d{2})\b"""),
        )

        private val URL_PATTERN = Regex("""https?://[^\s<>"]+""")

        // סף אורך מינימלי לשם דומיין שייחשב שם עסק
        private const val MIN_DOMAIN_NAME_LENGTH = 4
    }

    // ─── פונקציה ראשית ───

    fun parse(body: String, sender: String, receivedAt: Long): ParsedSmsData? {
        Log.d(TAG, "Parsing SMS from='$sender': ${body.take(100)}...")

        val bodyLower = body.lowercase()

        // שלב 0: חסימת פרסומות/OTP ברורות
        if (PROMO_BLACKLIST.any { kw -> bodyLower.contains(kw.lowercase()) }) {
            Log.d(TAG, "Blacklisted → skip")
            return null
        }

        // שלב 1: מילת מפתח חזקה — חובה
        val strongKeyword = STRONG_COUPON_KEYWORDS.firstOrNull { kw ->
            bodyLower.contains(kw.lowercase())
        } ?: run {
            Log.d(TAG, "No strong keyword → skip")
            return null
        }
        Log.d(TAG, "Keyword: '$strongKeyword'")

        // שלב 2: קוד קופון ממשי — חובה
        val couponCode = extractCouponCode(body) ?: run {
            Log.d(TAG, "Keyword found but no code pattern matched → skip")
            return null
        }
        Log.d(TAG, "Code: '$couponCode'")

        // שלב 3: חישוב confidence
        var confidence = 0.5f
        val amountResult = extractAmount(body).also { if (it != null) confidence += 0.25f }
        val expiresAt    = extractExpiry(body).also   { if (it != null) confidence += 0.15f }
        val url          = extractUrl(body).also      {
            if (it != null) {
                confidence += 0.10f
                // לינקים של matana4u הם קופונים ודאיים — boost נוסף
                if (it.contains("matana4u", ignoreCase = true)) confidence += 0.20f
            }
        }

        Log.d(TAG, "confidence=$confidence | amount=${amountResult?.amount} | expires=$expiresAt")

        return ParsedSmsData(
            couponCode               = couponCode,
            originalAmount           = amountResult?.amount,
            currency                 = extractCurrency(body),
            merchantName             = extractMerchant(body, sender, url),
            websiteUrl               = url,
            expiresAt                = expiresAt,
            confidence               = confidence,
            requiresUserConfirmation = confidence < CONFIDENCE_AUTO_IMPORT
        )
    }

    /**
     * חילוץ תאריך תפוגה בלבד — לשימוש בסריקה מחדש של קופונים קיימים ב-DB.
     * לא מריץ את כל ה-parse, רק בודק תאריכים.
     */
    fun extractExpiryOnly(body: String): Long? = extractExpiry(body)

    // ─── Helpers ───

    private fun extractCouponCode(body: String): String? {
        for (pattern in COUPON_CODE_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val code = match.groupValues[1].trim()
            if (code.length >= 4) return code.uppercase()
        }
        return null
    }

    private fun extractAmount(body: String): AmountResult? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val amount = match.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (amount != null && amount > 0 && amount < 100_000) return AmountResult(amount)
        }
        return null
    }

    private fun extractCurrency(body: String): String {
        for ((pattern, currency) in CURRENCY_PATTERNS) {
            if (pattern.containsMatchIn(body)) return currency
        }
        return "₪"
    }

    private fun extractUrl(body: String): String? = URL_PATTERN.find(body)?.value

    private fun extractExpiry(body: String): Long? {
        for (pattern in EXPIRY_PATTERNS) {
            val match = pattern.find(body) ?: continue
            parseDate(match.groupValues[1])?.let { return it }
        }
        return null
    }

    private fun parseDate(dateStr: String): Long? {
        return try {
            val parts = dateStr.trim().split(Regex("[/.\\-]"))
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            when (parts.size) {
                2 -> {
                    // DD.MM — שנה חסרה: נניח שנה נוכחית
                    val day   = parts[0].toInt()
                    val month = parts[1].toInt() - 1
                    java.util.Calendar.getInstance()
                        .apply { set(currentYear, month, day, 23, 59, 59) }
                        .timeInMillis
                }
                3 -> {
                    val day   = parts[0].toInt()
                    val month = parts[1].toInt() - 1
                    val year  = parts[2].toInt().let { if (it < 100) 2000 + it else it }
                    java.util.Calendar.getInstance()
                        .apply { set(year, month, day, 23, 59, 59) }
                        .timeInMillis
                }
                else -> null
            }
        } catch (e: Exception) { null }
    }

    /**
     * שם בית העסק — לפי סדר עדיפויות:
     * 1. שם השולח (אם אינו מספר טלפון)
     * 2. דומיין ה-URL — רק אם ארוך ומשמעותי (לא shortcode כמו "lk5")
     */
    private fun extractMerchant(body: String, sender: String, url: String?): String? {
        // עדיפות 1: שם השולח — אם אינו מספר טלפון גולמי
        val isPhoneNumber = sender.matches(Regex("""^\+?[\d\s\-().]+$"""))
        if (!isPhoneNumber) return sender.trim()

        // עדיפות 2: דומיין URL — רק אם ארוך מספיק לשם עסק
        if (url != null) {
            val domain = extractMeaningfulDomain(url)
            if (domain != null) return domain
        }
        return null
    }

    /**
     * חילוץ שם דומיין — מחזיר null אם הדומיין קצר מדי (shortcode/URL מקוצר).
     * למשל: "lk5.co.il" → null (קצר מדי), "papajohns.co.il" → "Papajohns"
     */
    private fun extractMeaningfulDomain(url: String): String? = try {
        val host = java.net.URL(url).host.removePrefix("www.")
        val mainPart = host.split(".").firstOrNull() ?: return null
        // מסנן שמות קצרים — URL מקוצרים, shortcodes
        if (mainPart.length < MIN_DOMAIN_NAME_LENGTH) null
        else mainPart.replaceFirstChar { it.uppercase() }
    } catch (e: Exception) { null }
}

data class ParsedSmsData(
    val couponCode: String,
    val originalAmount: Double?,
    val currency: String,
    val merchantName: String?,
    val websiteUrl: String?,
    val expiresAt: Long?,
    val confidence: Float,
    val requiresUserConfirmation: Boolean
)

private data class AmountResult(val amount: Double)
