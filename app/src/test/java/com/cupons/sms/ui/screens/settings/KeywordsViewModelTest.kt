package com.cupons.sms.ui.screens.settings

import app.cash.turbine.test
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeywordsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prefs: AppPreferences = mockk(relaxed = true)

    @Test
    fun `uiState reflects prefs flows`() = runTest {
        every { prefs.customKeywords } returns flowOf(setOf("מבצע"))
        every { prefs.customBlacklist } returns flowOf(setOf("ספאם"))

        val vm = KeywordsViewModel(prefs)
        vm.uiState.test {
            var state = awaitItem()
            while (state.customKeywords.isEmpty()) state = awaitItem()
            assertEquals(setOf("מבצע"), state.customKeywords)
            assertEquals(setOf("ספאם"), state.customBlacklist)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addKeyword delegates to atomic pref update`() = runTest {
        every { prefs.customKeywords } returns flowOf(emptySet())
        every { prefs.customBlacklist } returns flowOf(emptySet())

        val vm = KeywordsViewModel(prefs)
        vm.addKeyword("חדש")
        advanceUntilIdle()
        coVerify(exactly = 1) { prefs.addCustomKeyword("חדש") }
    }

    @Test
    fun `removeBlacklist delegates to atomic pref update`() = runTest {
        every { prefs.customKeywords } returns flowOf(emptySet())
        every { prefs.customBlacklist } returns flowOf(emptySet())

        val vm = KeywordsViewModel(prefs)
        vm.removeBlacklist("ישן")
        advanceUntilIdle()
        coVerify(exactly = 1) { prefs.removeBlacklistWord("ישן") }
    }
}
