package com.cupons.sms.ui.screens.settings

import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.data.prefs.BackupFrequency
import com.cupons.sms.data.sms.SmsParser
import com.cupons.sms.testutil.MainDispatcherRule
import com.cupons.sms.util.ExportManager
import com.cupons.sms.util.ImportManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prefs: AppPreferences = mockk(relaxed = true)
    private val exportManager: ExportManager = mockk(relaxed = true)
    private val importManager: ImportManager = mockk(relaxed = true)
    private val dao: CouponDao = mockk(relaxed = true)
    private val smsParser: SmsParser = mockk(relaxed = true)

    private fun buildVm(): SettingsViewModel {
        every { prefs.notificationsEnabled } returns flowOf(true)
        every { prefs.notificationDaysBefore } returns flowOf(3)
        every { prefs.backupFrequency } returns flowOf(BackupFrequency.OFF)
        every { prefs.driveBackupEnabled } returns flowOf(false)
        every { prefs.whatsappEnabled } returns flowOf(false)
        every { prefs.lastBackupTimestamp } returns flowOf(0L)
        return SettingsViewModel(prefs, exportManager, importManager, dao, smsParser)
    }

    @Test
    fun `backup sets isBackingUp but not isRescanning`() = runTest {
        // גיבוי חוסם עד שנשחרר את ה-Deferred — כדי לבדוק את הדגל בזמן הריצה
        val gate = CompletableDeferred<String?>()
        coEvery { exportManager.exportToJson() } coAnswers { gate.await() }

        val vm = buildVm()
        vm.backupNow()
        advanceUntilIdle()

        assertTrue(vm.backupState.value.isBackingUp)
        assertFalse(vm.backupState.value.isRescanning)

        gate.complete("file.json")
        advanceUntilIdle()
        assertFalse(vm.backupState.value.isBackingUp)
    }

    @Test
    fun `rescan sets isRescanning but not isBackingUp`() = runTest {
        val gate = CompletableDeferred<List<com.cupons.sms.data.db.entity.CouponEntity>>()
        coEvery { dao.getCouponsWithNoExpiry() } coAnswers { gate.await() }

        val vm = buildVm()
        vm.rescanExpiryDates()
        advanceUntilIdle()

        assertTrue(vm.backupState.value.isRescanning)
        assertFalse(vm.backupState.value.isBackingUp)

        gate.complete(emptyList())
        advanceUntilIdle()
        assertFalse(vm.backupState.value.isRescanning)
    }
}
