package br.com.calcmot.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedbackScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankMessageIsRejectedBeforeOpeningEmail() {
        var submitted = false
        renderFeedback {
            submitted = true
            FeedbackSubmitResult.OPENED
        }

        composeRule.onNodeWithTag(UiTestTags.FEEDBACK_SUBMIT_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule.onAllNodesWithText("Escreva uma mensagem para continuar.").assertCountEquals(1)
        composeRule.runOnIdle { assertFalse(submitted) }
    }

    @Test
    fun formStateSurvivesSavedInstanceStateRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            MetricaTheme {
                FeedbackScreen(
                    onBack = {},
                    onSubmit = { FeedbackSubmitResult.OPENED }
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.FEEDBACK_TYPE_PROBLEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.FEEDBACK_MESSAGE_INPUT)
            .performTextInput("O botão não abriu o app")
        composeRule.onNodeWithTag(UiTestTags.FEEDBACK_INCLUDE_APP_INFO_SWITCH)
            .performScrollTo()
            .performClick()
            .assertIsOn()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(UiTestTags.FEEDBACK_TYPE_PROBLEM).assertIsSelected()
        composeRule.onNodeWithTag(UiTestTags.FEEDBACK_MESSAGE_INPUT)
            .assertTextContains("O botão não abriu o app")
        composeRule.onNodeWithTag(UiTestTags.FEEDBACK_INCLUDE_APP_INFO_SWITCH).assertIsOn()
    }

    @Test
    fun emailIntentContainsOnlyApprovedDiagnostics() {
        val intent = FeedbackEmailLauncher.createIntent(
            draft = FeedbackDraft(
                typeLabel = "Problema",
                message = "Não consegui abrir o aplicativo.",
                includeAppInfo = true
            ),
            accessibilityEnabled = true,
            monitoringEnabled = false
        )

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data?.scheme)
        assertEquals(CALCMOT_SUPPORT_EMAIL, intent.data?.schemeSpecificPart?.substringBefore('?'))
        val body = intent.getStringExtra(Intent.EXTRA_TEXT)
        assertNotNull(body)
        assertTrue(body!!.contains("Acessibilidade: ativada"))
        assertTrue(body.contains("Cálculo automático: pausado"))
        assertFalse(body.contains("passageiro", ignoreCase = true))
        assertFalse(body.contains("endereço", ignoreCase = true))
        assertFalse(body.contains("oferta", ignoreCase = true))
    }

    private fun renderFeedback(onSubmit: (FeedbackDraft) -> FeedbackSubmitResult) {
        composeRule.setContent {
            MetricaTheme {
                FeedbackScreen(onBack = {}, onSubmit = onSubmit)
            }
        }
    }
}
