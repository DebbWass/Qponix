package com.cupons.sms.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.MerchantUsage
import com.cupons.sms.data.db.dao.MonthlyUsage
import com.cupons.sms.data.db.dao.UsageLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    // מזהה טעינה נוכחי — ריצת רענון חדשה מבטלת קודמת (מונע חפיפה/last-write-wins)
    private var loadJob: Job? = null

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // ריצה מקבילית — שש שאילתות עצמאיות
                coroutineScope {
                    val totalSaved       = async { usageLogDao.getTotalAmountUsed() }
                    val totalExpiredLost = async { couponDao.getTotalExpiredValue() }
                    val activeCoupons    = async { couponDao.getActiveCouponsCount() }
                    val usedCoupons      = async { couponDao.getUsedCouponsCount() }
                    val monthlyUsage     = async { usageLogDao.getMonthlyUsage() }
                    val topMerchants     = async { usageLogDao.getTopMerchants() }

                    // המתנה לכולן (awaitAll מבטיח שכולן הושלמו לפני העדכון)
                    awaitAll(
                        totalSaved, totalExpiredLost, activeCoupons,
                        usedCoupons, monthlyUsage, topMerchants
                    )

                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            totalSaved       = totalSaved.await(),
                            totalExpiredLost = totalExpiredLost.await(),
                            activeCoupons    = activeCoupons.await(),
                            usedCoupons      = usedCoupons.await(),
                            monthlyUsage     = monthlyUsage.await(),
                            topMerchants     = topMerchants.await()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
