package com.cupons.sms.ui.screens.home

import app.cash.turbine.test
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.repository.CouponRepository
import com.cupons.sms.domain.usecase.ImportFromSmsUseCase
import com.cupons.sms.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: CouponRepository = mockk(relaxed = true)
    private val importUseCase: ImportFromSmsUseCase = mockk(relaxed = true)
    private val prefs: AppPreferences = mockk(relaxed = true)

    private val now = System.currentTimeMillis()

    private fun coupon(id: Long, code: String, expires: Long?) = Coupon(
        id = id, sender = "S", couponCode = code, receivedAt = id, expiresAt = expires
    )

    private fun buildVm(nonArchived: List<Coupon>): HomeViewModel {
        every { repo.getNonArchivedCoupons() } returns flowOf(nonArchived)
        every { repo.getArchivedCoupons() } returns flowOf(emptyList())
        every { repo.getDeletedCoupons() } returns flowOf(emptyList())
        every { repo.getPendingCoupons() } returns flowOf(emptyList())
        every { prefs.notificationDaysBefore } returns flowOf(3)
        return HomeViewModel(repo, importUseCase, prefs)
    }

    @Test
    fun `splits coupons into active and expired by expiry`() = runTest {
        val active  = coupon(1, "ACTIVE", now + 10_000_000)
        val expired = coupon(2, "EXPIRED", now - 10_000)
        val noExpiry = coupon(3, "NOEXP", null)

        val vm = buildVm(listOf(active, expired, noExpiry))
        vm.uiState.test {
            // מדלגים על ה-emit הראשוני (isLoading=true) עד שהמצב מיושב
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertEquals(setOf("ACTIVE", "NOEXP"), state.activeCoupons.map { it.couponCode }.toSet())
            assertEquals(listOf("EXPIRED"), state.expiredCoupons.map { it.couponCode })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duplicate codes are deduplicated`() = runTest {
        val a = coupon(1, "DUP", now + 10_000_000)
        val b = coupon(2, "dup", now + 10_000_000) // אותו קוד, אותיות שונות
        val vm = buildVm(listOf(a, b))
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(1, state.activeCoupons.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTabChange updates tab and exits multi-select`() = runTest {
        val vm = buildVm(emptyList())
        vm.enterMultiSelectMode(5L)
        assertTrue(vm.uiState.value.isMultiSelectMode)

        vm.onTabChange(CouponTab.DELETED)
        assertEquals(CouponTab.DELETED, vm.uiState.value.selectedTab)
        assertFalse(vm.uiState.value.isMultiSelectMode)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `toggleSelection removing last id exits multi-select`() = runTest {
        val vm = buildVm(emptyList())
        vm.enterMultiSelectMode(1L)
        vm.toggleSelection(1L) // מסיר את האחרון
        assertFalse(vm.uiState.value.isMultiSelectMode)
    }
}
