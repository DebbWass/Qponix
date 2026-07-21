package com.cupons.sms.ui.screens.statistics

import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.MerchantUsage
import com.cupons.sms.data.db.dao.MonthlyUsage
import com.cupons.sms.data.db.dao.UsageLogDao
import com.cupons.sms.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val couponDao: CouponDao = mockk(relaxed = true)
    private val usageLogDao: UsageLogDao = mockk(relaxed = true)

    @Test
    fun `aggregates statistics from daos`() = runTest {
        coEvery { usageLogDao.getTotalAmountUsed() } returns 150.0
        coEvery { couponDao.getTotalExpiredValue(any()) } returns 40.0
        coEvery { couponDao.getActiveCouponsCount() } returns 5
        coEvery { couponDao.getUsedCouponsCount() } returns 3
        coEvery { usageLogDao.getMonthlyUsage() } returns listOf(MonthlyUsage("2026-01", 150.0))
        coEvery { usageLogDao.getTopMerchants() } returns listOf(MerchantUsage("Shop", 2))

        val vm = StatisticsViewModel(couponDao, usageLogDao)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(150.0, state.totalSaved, 0.001)
        assertEquals(40.0, state.totalExpiredLost, 0.001)
        assertEquals(5, state.activeCoupons)
        assertEquals(3, state.usedCoupons)
        assertEquals(1, state.monthlyUsage.size)
        assertEquals("Shop", state.topMerchants.first().merchant)
    }

    @Test
    fun `zero data produces safe defaults`() = runTest {
        coEvery { usageLogDao.getTotalAmountUsed() } returns 0.0
        coEvery { couponDao.getTotalExpiredValue(any()) } returns 0.0
        coEvery { couponDao.getActiveCouponsCount() } returns 0
        coEvery { couponDao.getUsedCouponsCount() } returns 0
        coEvery { usageLogDao.getMonthlyUsage() } returns emptyList()
        coEvery { usageLogDao.getTopMerchants() } returns emptyList()

        val vm = StatisticsViewModel(couponDao, usageLogDao)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0.0, state.totalSaved, 0.001)
        assertEquals(0, state.activeCoupons)
        assertEquals(0, state.topMerchants.size)
    }
}
