package com.cupons.sms.ui.screens.add

import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.repository.CouponRepository
import com.cupons.sms.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AddCouponViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: CouponRepository = mockk(relaxed = true)

    @Test
    fun `blank code sets error and does not save`() = runTest {
        val vm = AddCouponViewModel(repo)
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.couponCodeError)
        coVerify(exactly = 0) { repo.insertCoupon(any()) }
    }

    @Test
    fun `valid form saves coupon and flips isSaved`() = runTest {
        coEvery { repo.insertCoupon(any()) } returns 1L
        val vm = AddCouponViewModel(repo)
        vm.onCouponCodeChange("save50")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.isSaved)
        coVerify(exactly = 1) { repo.insertCoupon(any()) }
    }

    @Test
    fun `expiry is parsed to end of day`() = runTest {
        val captured = slot<Coupon>()
        coEvery { repo.insertCoupon(capture(captured)) } returns 1L

        val vm = AddCouponViewModel(repo)
        vm.onCouponCodeChange("CODE1")
        vm.onExpiryDateChange("15062025")
        vm.save()
        advanceUntilIdle()

        val expiry = captured.captured.expiresAt
        assertNotNull(expiry)
        val cal = Calendar.getInstance().apply { timeInMillis = expiry!! }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `onSavedHandled resets flag`() = runTest {
        coEvery { repo.insertCoupon(any()) } returns 1L
        val vm = AddCouponViewModel(repo)
        vm.onCouponCodeChange("X123")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.isSaved)

        vm.onSavedHandled()
        assertFalse(vm.isSaved)
    }

    @Test
    fun `code input is uppercased and filtered`() = runTest {
        val vm = AddCouponViewModel(repo)
        vm.onCouponCodeChange("ab-12!x")
        assertEquals("AB-12X", vm.couponCode)
    }
}
