package br.com.calcmot.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import br.com.calcmot.ui.FeedbackScreen
import br.com.calcmot.ui.FeedbackSuccessScreen
import br.com.calcmot.ui.HistoryEmptyScreen
import br.com.calcmot.ui.HelpScreen
import br.com.calcmot.ui.HomePermissionRequiredScreen
import br.com.calcmot.ui.HomeReadyScreen
import br.com.calcmot.ui.PremiumScreen
import br.com.calcmot.ui.PrivacyPolicyScreen
import br.com.calcmot.ui.theme.MetricaTheme

class PrototypePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            MetricaTheme {
                when (intent.getStringExtra("prototype")) {
                    "home-permission" -> HomePermissionRequiredScreen(
                        onMenu = {},
                        onActivatePermission = {},
                        onHowItWorks = {}
                    )

                    "home-ready" -> HomeReadyScreen(
                        onMenu = {},
                        onOpenUber = {},
                        onOpen99 = {},
                        onOpenGoal = {},
                        onOpenSettings = {},
                        onOpenHelp = {}
                    )

                    "premium" -> PremiumScreen(
                        onBack = { finish() },
                        onStartNow = {},
                        onSkip = { finish() }
                    )

                    "history-empty" -> HistoryEmptyScreen(
                        onBack = { finish() },
                        onOpenDriverApp = {},
                        onOpenDiagnostics = {}
                    )

                    "help" -> HelpScreen(
                        onBack = { finish() },
                        onOpenPrivacy = {},
                        onSupport = {}
                    )

                    "privacy" -> PrivacyPolicyScreen(
                        onBack = { finish() },
                        onSupport = {}
                    )

                    "feedback" -> FeedbackScreen(
                        onBack = { finish() },
                        onSubmit = { br.com.calcmot.ui.FeedbackSubmitResult.OPENED }
                    )

                    else -> FeedbackSuccessScreen(
                        onBack = { finish() },
                        onHome = { finish() },
                        onSendAnotherFeedback = {}
                    )
                }
            }
        }
    }
}
