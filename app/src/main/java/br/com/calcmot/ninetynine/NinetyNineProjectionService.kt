package br.com.calcmot.ninetynine

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.delay

class NinetyNineProjectionService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var width: Int = 0
    private var height: Int = 0
    private var densityDpi: Int = 0

    override fun onCreate() {
        super.onCreate()
        activeService = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val resultData = intent?.projectionData()
        if (resultCode == Activity.RESULT_OK && resultData != null && mediaProjection == null) {
            configureProjection(resultCode, resultData)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (activeService === this) activeService = null
        releaseProjection()
        super.onDestroy()
    }

    private fun configureProjection(resultCode: Int, resultData: Intent) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, resultData)?.also { projection ->
            projection.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        stopSelf()
                    }
                },
                null
            )
        }
        val metrics = currentDisplayMetrics()
        width = metrics.widthPixels
        height = metrics.heightPixels
        densityDpi = metrics.densityDpi
        imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            MAX_IMAGES
        )
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "CalcMot-99-Capture",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun currentDisplayMetrics(): DisplayMetrics {
        return DisplayMetrics().also { metrics ->
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
                .getRealMetrics(metrics)
        }
    }

    private suspend fun captureLatest(targetBounds: Rect): Bitmap? {
        val reader = imageReader ?: return null
        repeat(CAPTURE_ATTEMPTS) {
            val image = reader.acquireLatestImage()
            if (image != null) {
                return image.useToBitmap()?.cropAndRecycleSource(targetBounds)
            }
            delay(CAPTURE_RETRY_DELAY_MS)
        }
        return null
    }

    private fun Image.useToBitmap(): Bitmap? {
        return try {
            val plane = planes.firstOrNull() ?: return null
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val paddedWidth = width + rowPadding / pixelStride
            Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(plane.buffer)
            }
        } finally {
            close()
        }
    }

    private fun Bitmap.cropAndRecycleSource(requestedBounds: Rect): Bitmap? {
        val bounds = Rect(
            requestedBounds.left.coerceIn(0, width),
            requestedBounds.top.coerceIn(0, height),
            requestedBounds.right.coerceIn(0, width),
            requestedBounds.bottom.coerceIn(0, height)
        )
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            recycle()
            return null
        }
        val cropped = Bitmap.createBitmap(this, bounds.left, bounds.top, bounds.width(), bounds.height())
        recycle()
        return cropped
    }

    private fun releaseProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(br.com.calcmot.R.mipmap.ic_launcher)
            .setContentTitle("CalcMot")
            .setContentText("Leitura de ofertas da 99 ativa")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Leitura de ofertas da 99",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    private fun Intent.projectionData(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            getParcelableExtra(EXTRA_RESULT_DATA)
        }
    }

    companion object {
        const val ACTION_START = "br.com.calcmot.action.START_99_PROJECTION"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        @Volatile
        private var activeService: NinetyNineProjectionService? = null

        val isReady: Boolean
            get() = activeService?.mediaProjection != null

        suspend fun capture(targetBounds: Rect): Bitmap? {
            return activeService?.captureLatest(targetBounds)
        }

        private const val CHANNEL_ID = "calcmot_99_projection"
        private const val NOTIFICATION_ID = 9901
        private const val MAX_IMAGES = 2
        private const val CAPTURE_ATTEMPTS = 5
        private const val CAPTURE_RETRY_DELAY_MS = 40L
    }
}
