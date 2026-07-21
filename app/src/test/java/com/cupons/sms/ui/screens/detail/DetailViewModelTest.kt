package com.cupons.sms.ui.screens.detail

import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.repository.CouponRepository
import com.cupons.sms.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: CouponRepository = mockk(relaxed = true)

    private fun vmFor(couponId: Long): DetailViewModel {
        every { repo.getUsageLog(couponId) } returns flowOf(emptyList())
        return DetailViewModel(repo, couponId)
    }

    @Test
    fun `loads existing coupon`() = runTest {
        val coupon = Coupon(id = 1, sender = "S", couponCode = "C1", receivedAt = 0L)
        coEvery { repo.getCouponById(1) } returns coupon

        val vm = vmFor(1)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("C1", state.coupon?.couponCode)
        assertFalse(state.isLoading)
        assertFalse(state.notFound)
    }

    @Test
    fun `missing coupon sets notFound instead of endless loading`() = runTest {
        coEvery { repo.getCouponById(99) } returns null

        val vm = vmFor(99)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.coupon)
        assertFalse(state.isLoading)
        assertTrue(state.notFound)
    }

    @Test
    fun `deleteCoupon flips isDeleted`() = runTest {
        coEvery { repo.getCouponById(1) } returns Coupon(id = 1, sender = "S", couponCode = "C", receivedAt = 0L)
        val vm = vmFor(1)
        advanceUntilIdle()

        vm.deleteCoupon()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isDeleted)
    }
}
