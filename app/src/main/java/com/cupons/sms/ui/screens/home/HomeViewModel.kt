package com.cupons.sms.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.repository.CouponRepository
import com.cupons.sms.domain.usecase.ImportFromSmsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * HomeViewModel — מנהל את מצב מסך הרשימה הראשית.
 *
 * ארבעה טאבים:
 *  - ACTIVE   → קופונים שבתוקף (לא ממומשים, לא נמחקו, לא פגי תוקף)
 *  - EXPIRED  → קופונים שפג תוקפם
 *  - ARCHIVED → קופונים ממומשים / בארכיון
 *  - DELETED  → קופונים שנמחקו (מחיקה רכה)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CouponRepository,
    private val importFromSmsUseCase: ImportFromSmsUseCase,
    private val prefs: AppPreferences
) : ViewModel() {

    // ─── UI State ───
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ─── Search & Sort ───
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)

    private val _notifDays = MutableStateFlow(3)

    init {
        viewModelScope.launch {
            prefs.notificationDaysBefore.collect { _notifDays.value = it }
        }
        observeNonArchived()
        observeArchived()
        observeDeleted()
        observePending()
    }

    // ─── Observers ───

    /**
     * מאזין לקופונים שאינם בארכיון ומפצל ל"בתוקף", "פגי תוקף" ו"עומדים לפוג".
     */
    private fun observeNonArchived() {
        combine(
            repository.getNonArchivedCoupons(),
            _searchQuery,
            _sortOrder,
            _notifDays
        ) { coupons: List<Coupon>, query: String, sort: SortOrder, days: Int ->
            val filtered     = filterCoupons(coupons, query)
            val sorted       = sortCoupons(filtered, sort)
            val now          = System.currentTimeMillis()
            val soonDeadline = now + TimeUnit.DAYS.toMillis(days.toLong())
            Triple(
                sorted.filter { it.expiresAt == null || it.expiresAt > now },
                sorted.filter { it.expiresAt != null && it.expiresAt <= now },
                sorted.filter { it.expiresAt != null && it.expiresAt > now && it.expiresAt <= soonDeadline }
            )
        }
            .onEach { (active, expired, expiringSoon) ->
                _uiState.update {
                    it.copy(
                        activeCoupons       = active,
                        expiredCoupons      = expired,
                        expiringSoonCoupons = expiringSoon,
                        isLoading           = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * מאזין לקופונים ממומשים / בארכיון.
     */
    private fun observeArchived() {
        combine(
            repository.getArchivedCoupons(),
            _searchQuery,
            _sortOrder
        ) { coupons, query, sort ->
            sortCoupons(filterCoupons(coupons, query), sort)
        }
            .onEach { archived ->
                _uiState.update { it.copy(archivedCoupons = archived) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * מאזין לקופונים שנמחקו (מחיקה רכה).
     */
    private fun observeDeleted() {
        repository.getDeletedCoupons()
            .onEach { deleted ->
                _uiState.update { it.copy(deletedCoupons = deleted) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * מאזין לקופונים הממתינים לאישור — נשמרים ב-DB עם is_pending=1.
     */
    private fun observePending() {
        repository.getPendingCoupons()
            .onEach { pending ->
                _uiState.update { it.copy(pendingCoupons = pending) }
            }
            .launchIn(viewModelScope)
    }

    // ─── Helpers ───

    private fun filterCoupons(coupons: List<Coupon>, query: String): List<Coupon> {
        val filtered = if (query.isBlank()) coupons else coupons.filter { coupon ->
            coupon.couponCode.contains(query, ignoreCase = true) ||
            coupon.sender.contains(query, ignoreCase = true) ||
            coupon.merchantName?.contains(query, ignoreCase = true) == true
        }
        // מניעת כפילויות: שמור רק קופון אחד לכל קוד (העדכני ביותר — ראשון ברשימה)
        return filtered.distinctBy { it.couponCode.uppercase().trim() }
    }

    private fun sortCoupons(coupons: List<Coupon>, sort: SortOrder): List<Coupon> =
        when (sort) {
            SortOrder.DATE_DESC   -> coupons.sortedByDescending { it.receivedAt }
            SortOrder.DATE_ASC    -> coupons.sortedBy { it.receivedAt }
            SortOrder.AMOUNT_DESC -> coupons.sortedByDescending { it.displayBalance ?: 0.0 }
            SortOrder.AMOUNT_ASC  -> coupons.sortedBy { it.displayBalance ?: 0.0 }
            SortOrder.MERCHANT    -> coupons.sortedBy { it.merchantName ?: it.sender }
        }

    // ─── Tab Actions ───

    fun onTabChange(tab: CouponTab) {
        // כשמחליפים טאב, יוצאים ממצב בחירה מרובה
        _uiState.update { it.copy(selectedTab = tab, isMultiSelectMode = false, selectedIds = emptySet()) }
    }

    // ─── Multi-Select Actions ───

    /** כניסה למצב בחירה מרובה — מופעל בלחיצה ארוכה על קופון */
    fun enterMultiSelectMode(couponId: Long) {
        _uiState.update { it.copy(isMultiSelectMode = true, selectedIds = setOf(couponId)) }
    }

    /** החלפת מצב בחירה של פריט בודד */
    fun toggleSelection(couponId: Long) {
        _uiState.update { state ->
            val newIds = if (couponId in state.selectedIds) {
                state.selectedIds - couponId
            } else {
                state.selectedIds + couponId
            }
            // אם בוטלה הבחירה האחרונה — יצא ממצב בחירה מרובה
            state.copy(
                selectedIds       = newIds,
                isMultiSelectMode = newIds.isNotEmpty()
            )
        }
    }

    /** בחירת כל הפריטים בטאב הפעיל */
    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.currentCoupons.map { it.id }.toSet())
        }
    }

    /** ביטול מצב בחירה מרובה */
    fun clearSelection() {
        _uiState.update { it.copy(isMultiSelectMode = false, selectedIds = emptySet()) }
    }

    /** מחיקת כל הפריטים הנבחרים (מחיקה רכה) */
    fun deleteSelected() {
        val toDelete = _uiState.value.selectedIds.toSet()
        viewModelScope.launch {
            toDelete.forEach { id -> repository.deleteCoupon(id) }
        }
        clearSelection()
    }

    // ─── Search / Sort Actions ───

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
    }

    // ─── SMS Import ───

    fun importFromSms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importMessage = null) }
            try {
                val result = importFromSmsUseCase()
                val message = when {
                    result.imported > 0 ->
                        "נמצאו ${result.imported} קופונים חדשים ✓"
                    result.pendingConfirmation.isNotEmpty() ->
                        "נמצאו ${result.pendingConfirmation.size} פריטים שממתינים לאישורך"
                    else ->
                        "אין הודעות SMS חדשות עם קופונים"
                }
                _uiState.update {
                    it.copy(
                        isImporting    = false,
                        importMessage  = message,
                        pendingCoupons = result.pendingConfirmation
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, importMessage = "שגיאה בייבוא: ${e.message}")
                }
            }
        }
    }

    fun dismissImportMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }

    // ─── Coupon Actions ───

    fun markAsUsed(couponId: Long) {
        viewModelScope.launch { repository.markAsUsed(couponId) }
    }

    /** מחיקה רכה — הקופון יועבר לטאב "נמחקו" */
    fun deleteCoupon(couponId: Long) {
        viewModelScope.launch { repository.deleteCoupon(couponId) }
    }

    /** שחזור קופון מטאב "נמחקו" */
    fun restoreCoupon(couponId: Long) {
        viewModelScope.launch { repository.restoreCoupon(couponId) }
    }

    // ─── Pending Coupons ───

    fun confirmPendingCoupon(coupon: Coupon) {
        viewModelScope.launch {
            repository.approvePendingCoupon(coupon.id)
        }
    }

    fun dismissPendingCoupon(coupon: Coupon) {
        viewModelScope.launch {
            coupon.smsId?.let { prefs.addRejectedSmsId(it) }
            repository.deleteCoupon(coupon.id)
        }
    }

    fun dismissAllPending() {
        viewModelScope.launch {
            _uiState.value.pendingCoupons.forEach { repository.deleteCoupon(it.id) }
        }
    }
}

// ─── UI State ───

data class HomeUiState(
    val selectedTab: CouponTab           = CouponTab.ACTIVE,
    val activeCoupons: List<Coupon>      = emptyList(),
    val expiredCoupons: List<Coupon>     = emptyList(),
    val expiringSoonCoupons: List<Coupon> = emptyList(),
    val archivedCoupons: List<Coupon>    = emptyList(),
    val deletedCoupons: List<Coupon>     = emptyList(),
    val isLoading: Boolean               = true,
    val isImporting: Boolean             = false,
    val importMessage: String?           = null,
    val pendingCoupons: List<Coupon>     = emptyList(),
    // ─── Multi-Select ───
    val isMultiSelectMode: Boolean       = false,
    val selectedIds: Set<Long>           = emptySet()
) {
    /** הרשימה הפעילה לפי הטאב הנבחר */
    val currentCoupons: List<Coupon>
        get() = when (selectedTab) {
            CouponTab.ACTIVE        -> activeCoupons
            CouponTab.EXPIRING_SOON -> expiringSoonCoupons
            CouponTab.EXPIRED       -> expiredCoupons
            CouponTab.ARCHIVED      -> archivedCoupons
            CouponTab.DELETED       -> deletedCoupons
        }

    /** מזהי הקופונים העומדים לפוג */
    val expiringSoonIds: Set<Long>
        get() = expiringSoonCoupons.map { it.id }.toSet()

    /** האם כל הפריטים בטאב הנוכחי נבחרו */
    val isAllSelected: Boolean
        get() = currentCoupons.isNotEmpty() && selectedIds.containsAll(currentCoupons.map { it.id })
}

// ─── Enums ───

enum class CouponTab(val label: String) {
    ACTIVE("בתוקף"),
    EXPIRING_SOON("עומדים לפוג"),
    EXPIRED("פגי תוקף"),
    ARCHIVED("ארכיון"),
    DELETED("נמחקו")
}

enum class SortOrder {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, MERCHANT
}
