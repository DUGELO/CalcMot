package br.com.calcmot.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import br.com.calcmot.securityrecording.domain.RecordingPhase
import br.com.calcmot.securityrecording.domain.RecordingRuntimeSnapshot
import br.com.calcmot.securityrecording.ui.SecurityPage
import br.com.calcmot.securityrecording.ui.SecurityRecordingActiveContent
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Rule
import org.junit.Test

class SecurityRecordingUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun preparingOffersSafeStopWithoutShowingRec() {
        show(RecordingRuntimeSnapshot("session", RecordingPhase.PREPARING, 1))

        composeRule.onNodeWithText("Iniciando gravação…").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SECURITY_RECORDING_STOP).assertIsDisplayed()
        composeRule.onAllNodesWithText("REC").assertCountEquals(0)
    }

    @Test
    fun recordingShowsConfirmedRecAndStop() {
        show(RecordingRuntimeSnapshot("session", RecordingPhase.RECORDING, 2, audioVideoConfirmed = true))

        composeRule.onNodeWithText("REC").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SECURITY_RECORDING_STOP).assertIsDisplayed()
    }

    @Test
    fun failedStateShowsUserMessageAndNoTechnicalCode() {
        show(
            RecordingRuntimeSnapshot(
                null,
                RecordingPhase.FAILED,
                3,
                "A câmera demorou demais para responder. Tente novamente."
            )
        )

        composeRule.onNodeWithText("A câmera demorou demais para responder. Tente novamente.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("preparation_timeout").assertCountEquals(0)
    }

    @Test fun recordingWithoutAudioEvidenceDoesNotShowRec() {
        show(RecordingRuntimeSnapshot("session", RecordingPhase.RECORDING, 2, audioVideoConfirmed = false))
        composeRule.onAllNodesWithText("REC").assertCountEquals(0)
        composeRule.onNodeWithTag(UiTestTags.SECURITY_RECORDING_STOP).assertIsDisplayed()
    }
    @Test fun rotatingShowsStopAndNoRecOrPause() {
        show(RecordingRuntimeSnapshot("session", RecordingPhase.ROTATING, 2))
        composeRule.onNodeWithText("Trocando arquivo…").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SECURITY_RECORDING_STOP).assertIsDisplayed()
        composeRule.onAllNodesWithText("REC").assertCountEquals(0)
        composeRule.onAllNodesWithText("Pausar").assertCountEquals(0)
    }
    @Test fun pausedShowsResumeAndNoRec() {
        show(RecordingRuntimeSnapshot("session", RecordingPhase.PAUSED, 2))
        composeRule.onNodeWithText("Retomar gravação").assertIsDisplayed()
        composeRule.onAllNodesWithText("REC").assertCountEquals(0)
    }

    private fun show(snapshot: RecordingRuntimeSnapshot) {
        composeRule.setContent {
            MetricaTheme {
                SecurityPage("Gravação de segurança", {}) {
                    SecurityRecordingActiveContent(snapshot, {}, {})
                }
            }
        }
    }
}
