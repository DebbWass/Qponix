package com.cupons.sms.ui.screens.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.repository.CouponRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * AddCouponViewModel — מנהל את טופס הוספת קופון ידנית.
 *
 * שדות:
 * - couponCode  (חובה)
 * - merchantName, amount, expiryDate, websiteUrl, notes (אופציונלי)
 *
 * לאחר שמירה מוצלחת, [isSaved] מועבר ל-true → הניווט חוזר אחורה.
 */
@HiltViewModel
class AddCouponViewModel @Inject constructor(
    private val repository: CouponRepository
) : ViewModel() {

    // ─── Form Fields ───
    var couponCode    by mutableStateOf("")   ; private set
    var merchantName  by mutableStateOf("")   ; private set
    var amount        by mutableStateOf("")   ; private set
    var expiryDate    by mutableStateOf("")   ; private set  // dd/MM/yyyy
    var websiteUrl    by mutableStateOf("")   ; private set
    var notes         by mutableStateOf("")   ; private set

    // ─── Validation & Status ───
    var couponCodeError by mutableStateOf<String?>(null) ; private set
    var isSaving        by mutableStateOf(false)          ; private set
    var isSaved         by mutableStateOf(false)          ; private set

    // ─── Field Setters ───
    fun onCouponCodeChange(v: String)   { couponCode    = v.uppercase().filter { it.isLetterOrDigit() || it == '-' }; couponCodeError = null }
    fun onMerchantNameChange(v: String) { merchantName  = v }
    fun onAmountChange(v: String)       { amount        = v.filter { it.isDigit() || it == '.' } }
    fun onExpiryDateChange(v: String)   { expiryDate    = formatExpiryInput(v) }
    fun onWebsiteUrlChange(v: String)   { websiteUrl    = v }
    fun onNotesChange(v: String)        { notes         = v }

    // ─── Save ───
    fun save() {
        if (couponCode.isBlank()) {
            couponCodeError = "קוד הקופון הוא שדה חובה"
            return
        }

        val amountDouble = amount.toDoubleOrNull()
        val expiry       = parseExpiryDate(expiryDate)

        val coupon = Coupon(
            id               = 0,
            smsId            = null,
            sender           = merchantName.ifBlank { "ידני" },
            rawSmsBody       = null,
            couponCode       = couponCode.trim(),
            originalAmount   = amountDouble,
            remainingBalance = amountDouble,
            currency         = "₪",
            merchantName     = merchantName.trim().ifBlank { null },
            websiteUrl       = websiteUrl.trim().ifBlank { null },
            receivedAt       = System.currentTimeMillis(),
            expiresAt        = expiry,
            isUsed           = false,
            isArchived       = false,
            isDeleted        = false,
            notes            = notes.trim().ifBlank { null }
        )

        viewModelScope.launch {
            isSaving = true
            repository.insertCoupon(coupon)
            isSaving = false
            isSaved  = true
        }
    }

    // ─── Helpers ───

    /**
     * פרמוט אוטומטי של שדה תאריך:
     * מוסיף "/" אחרי יום ואחרי חודש.
     */
    private fun formatExpiryInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return buildString {
            digits.forEachIndexed { i, c ->
                append(c)
                if (i == 1 || i == 3) append("/")
            }
        }
    }

    private fun parseExpiryDate(dateStr: String): Long? {
        if (dateStr.length < 8) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(dateStr.trim())?.time
        } catch (e: Exception) { null }
    }
}
