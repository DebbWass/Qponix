package com.cupons.sms.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.model.UsageLog
import com.cupons.sms.domain.repository.CouponRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * DetailViewModel — מנהל את מסך פרטי קופון בודד.
 *
 * משתמש ב-AssistedInject כדי לאפשר יצירת ViewModel נפרד לכל עמוד ב-HorizontalPager.
 * ה-couponId מוזרק ישירות ולא דרך SavedStateHandle.
 */
@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    private val repository: CouponRepository,
    @Assisted val couponId: Long
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(couponId: Long): DetailViewModel
    }

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadCoupon()
        observeUsageLog()
    }

    private fun loadCoupon() {
        viewModelScope.launch {
            val coupon = repository.getCouponById(couponId)
            _uiState.update {
                it.copy(
                    coupon    = coupon,
                    isLoading = false,
                    notFound  = coupon == null
                )
            }
        }
    }

    private fun observeUsageLog() {
        repository.getUsageLog(couponId)
            .onEach { log ->
                _uiState.update { it.copy(usageLog = log) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * עדכון יתרה + תאריך תפוגה בפעולה אחת.
     *
     * @param newBalance  היתרה החדשה
     * @param note        הערה אופציונלית (תישמר בלוג השימוש)
     * @param newExpiry   תאריך תפוגה חדש (Unix ms), או null להסרה
     */
    fun updateDetailsAndBalance(newBalance: Double, note: String? = null, newExpiry: Long?) {
        viewModelScope.launch {
            val current = _uiState.value.coupon ?: return@launch
            val amountUsed = (current.remainingBalance ?: current.originalAmount ?: 0.0) - newBalance

            repository.updateBalance(
                couponId   = couponId,
                newBalance = newBalance,
                amountUsed = maxOf(amountUsed, 0.0),
                note       = note
            )

            if (newExpiry != current.expiresAt) {
                repository.updateCoupon(current.copy(expiresAt = newExpiry))
            }

            loadCoupon()
            _uiState.update { it.copy(successMessage = "הפרטים עודכנו — יתרה: ${current.formatAmount(newBalance)}") }
        }
    }

    fun markAsUsed() {
        viewModelScope.launch {
            repository.markAsUsed(couponId)
            _uiState.update { it.copy(isMarkedUsed = true) }
        }
    }

    fun deleteCoupon() {
        viewModelScope.launch {
            repository.deleteCoupon(couponId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            val current = _uiState.value.coupon ?: return@launch
            repository.updateCoupon(current.copy(notes = notes))
            loadCoupon()
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

data class DetailUiState(
    val coupon: Coupon? = null,
    val usageLog: List<UsageLog> = emptyList(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isMarkedUsed: Boolean = false,
    val isDeleted: Boolean = false,
    val successMessage: String? = null
)
