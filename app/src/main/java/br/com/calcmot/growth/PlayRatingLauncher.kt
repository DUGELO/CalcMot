package br.com.calcmot.growth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Auto: official unmodified Play Store review sheet (may be suppressed by Play quota).
 * Manual: navigates to the actual Play Store listing, never uses the quota-limited API.
 */
internal object PlayRatingLauncher {
    private const val STORE_PACKAGE = "br.com.calcmot"
    private const val WEB_URL = "https://play.google.com/store/apps/details?id=$STORE_PACKAGE"

    fun requestNativeReview(activity: Activity, onFinished: () -> Unit) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful || activity.isFinishing || activity.isDestroyed) {
                onFinished()
                return@addOnCompleteListener
            }
            manager.launchReviewFlow(activity, request.result)
                .addOnCompleteListener {
                    // Google does not report whether the dialog was shown or a rating submitted.
                    onFinished()
                }
        }
    }

    fun openPlayStore(context: Context) {
        val market = Intent(
            Intent.ACTION_VIEW, Uri.parse("market://details?id=$STORE_PACKAGE")
        ).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val web = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(web)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Não foi possível abrir a Play Store.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
