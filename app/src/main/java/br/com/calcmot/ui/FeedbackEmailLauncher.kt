package br.com.calcmot.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import br.com.calcmot.BuildConfig

object FeedbackEmailLauncher {
    internal fun createIntent(
        draft: FeedbackDraft,
        accessibilityEnabled: Boolean,
        monitoringEnabled: Boolean
    ): Intent {
        val subject = "CalcMot - ${draft.typeLabel}"
        val body = buildString {
            appendLine("Tipo: ${draft.typeLabel}")
            appendLine()
            appendLine(draft.message)
            if (draft.includeAppInfo) {
                appendLine()
                appendLine("--- Informações do app ---")
                appendLine("CalcMot ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Acessibilidade: ${if (accessibilityEnabled) "ativada" else "desativada"}")
                appendLine("Cálculo automático: ${if (monitoringEnabled) "ligado" else "pausado"}")
            }
        }.trim()
        val uri = Uri.parse(
            "mailto:${Uri.encode(CALCMOT_SUPPORT_EMAIL)}" +
                "?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )

        return Intent(Intent.ACTION_SENDTO, uri)
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(CALCMOT_SUPPORT_EMAIL))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
    }

    fun launch(
        context: Context,
        draft: FeedbackDraft,
        accessibilityEnabled: Boolean,
        monitoringEnabled: Boolean
    ): FeedbackSubmitResult {
        val intent = createIntent(draft, accessibilityEnabled, monitoringEnabled)

        return try {
            context.startActivity(intent)
            FeedbackSubmitResult.OPENED
        } catch (_: ActivityNotFoundException) {
            FeedbackSubmitResult.NO_EMAIL_APP
        }
    }
}
