package com.cupons.sms.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.MerchantUsage
import com.cupons.sms.data.db.dao.MonthlyUsage
import com.cupons.sms.data.db.dao.UsageLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading       : Boolean             = true,
    val totalSaved      : Double              = 0.0,
    val totalExpiredLost: Double              = 0.0,
    val activeCoupons   : Int                 = 0,
    val usedCoupons     : Int                 = 0,
    val monthlyUsage    : List<MonthlyUsage>  = emptyList(),
    val topMerchants    : List<MerchantUsage> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val couponDao  : CouponDao,
    private val usageLogDao: UsageLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val totalSaved       = usageLogDao.getTotalAmountUsed()
                val totalExpiredLost = couponDao.getTotalExpiredValue()
                val activeCoupons    = couponDao.getActiveCouponsCount()
                val usedCoupons      = couponDao.getUsedCouponsCount()
                val monthlyUsage     = usageLogDao.getMonthlyUsage()
                val topMerchants     = usageLogDao.getTopMerchants()

                _uiState.update {
                    it.copy(
                        isLoading        = false,
                        totalSaved       = totalSaved,
                        totalExpiredLost = totalExpiredLost,
                        activeCoupons    = activeCoupons,
                        usedCoupons      = usedCoupons,
                        monthlyUsage     = monthlyUsage,
                        topMerchants     = topMerchants
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
