package com.eyecare.pro

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eyecare.pro.presentation.cff.CffViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EyeCareSafetyUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun ishiharaRemainsUnavailableWithoutLicensedDataset() {
        composeRule.onNodeWithText("Tes Ishihara (menunggu materi resmi)")
            .performClick()

        composeRule.onNodeWithText("Tes belum tersedia")
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Aplikasi tidak membuat pola titik atau angka acak sebagai pengganti pelat Ishihara.",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun cffDisablesStimulusWhenRefreshRateCannotBeRead() {
        composeRule.setContent {
            MaterialTheme {
                CffScreen(
                    displayRefreshRateHz = 0f,
                    viewModel = CffViewModel()
                )
            }
        }

        composeRule.onNodeWithText(
            "Refresh rate perangkat tidak dapat dibaca; stimulasi dinonaktifkan."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Mulai stimulasi").assertIsNotEnabled()
    }

    @Test
    fun cffRequiresBothSafetyAcknowledgementsBeforeStart() {
        composeRule.onNodeWithText("Eksperimen kedip CFF/FFF")
            .performClick()

        val startButton = composeRule.onNodeWithText("Mulai stimulasi")
        startButton.assertIsNotEnabled()

        val warningAcknowledgement = composeRule.onNodeWithTag("cff-warning-acknowledgement")
        warningAcknowledgement.performScrollTo().performClick()
        warningAcknowledgement.assertIsOn()
        startButton.performScrollTo().assertIsNotEnabled()

        val riskConfirmation = composeRule.onNodeWithTag("cff-no-known-photosensitivity-risk")
        riskConfirmation.performScrollTo().performClick()
        riskConfirmation.assertIsOn()
        startButton.performScrollTo().assertIsEnabled()
    }
}
