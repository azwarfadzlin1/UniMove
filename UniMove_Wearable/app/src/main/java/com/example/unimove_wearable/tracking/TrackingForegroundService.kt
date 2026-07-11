package com.example.unimove_wearable.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import com.example.unimove_wearable.R
import com.example.unimove_wearable.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrackingForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundStarted = false
    private var trackingRequested = false

    override fun onCreate() {
        super.onCreate()
        FitnessTracker.initialize(this)
        createNotificationChannel()
        serviceScope.launch {
            FitnessTracker.uiState.collectLatest(::handleTrackerState)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                trackingRequested = true
                startTrackingForeground(FitnessTracker.uiState.value)
                FitnessTracker.startTracking()
            }

            ACTION_STOP -> {
                val requestedSessionId = intent.takeIf { it.hasExtra(EXTRA_SESSION_ID) }
                    ?.getLongExtra(EXTRA_SESSION_ID, 0L)
                FitnessTracker.stopTracking(requestedSessionId)
                if (FitnessTracker.uiState.value.phase !in ACTIVE_PHASES) {
                    stopForegroundAndSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleTrackerState(state: TrackingUiState) {
        if (!trackingRequested) return
        when (state.phase) {
            TrackingPhase.STARTING,
            TrackingPhase.RUNNING,
            TrackingPhase.PAUSED,
            TrackingPhase.STOPPING,
            -> {
                if (!foregroundStarted) {
                    startTrackingForeground(state)
                } else {
                    val canPostNotification = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    if (canPostNotification) {
                        getSystemService(NotificationManager::class.java)
                            .notify(NOTIFICATION_ID, buildNotification(state))
                    } else {
                        Log.w(TAG, "Notification permission was revoked during the workout")
                    }
                }
            }

            TrackingPhase.SUMMARY,
            TrackingPhase.ERROR,
            -> stopForegroundAndSelf()

            TrackingPhase.IDLE -> Unit
        }
    }

    private fun startTrackingForeground(state: TrackingUiState) {
        val notification = buildNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
        Log.d(TAG, "Health foreground service started")
    }

    private fun buildNotification(state: TrackingUiState): Notification {
        val activityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val content = when (state.phase) {
            TrackingPhase.STARTING -> "Preparing run sensors"
            TrackingPhase.PAUSED -> "Run paused"
            TrackingPhase.STOPPING -> "Saving completed run"
            else -> "Run in progress"
        }
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ongoing_run)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(content)
            .setContentIntent(activityIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
            .setStaticIcon(R.drawable.ic_ongoing_run)
            .setTouchIntent(activityIntent)
            .setTitle(content)
            .build()
            .apply(applicationContext)
        return notificationBuilder.build()
    }

    private fun stopForegroundAndSelf() {
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        trackingRequested = false
        stopSelf()
        Log.d(TAG, "Health foreground service stopped")
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        private const val TAG = "WatchSyncDebug"
        private const val CHANNEL_ID = "unimove_tracking"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.unimove_wearable.action.START_TRACKING"
        const val ACTION_STOP = "com.example.unimove_wearable.action.STOP_TRACKING"
        const val EXTRA_SESSION_ID = "sessionId"

        private val ACTIVE_PHASES = setOf(
            TrackingPhase.STARTING,
            TrackingPhase.RUNNING,
            TrackingPhase.PAUSED,
            TrackingPhase.STOPPING,
        )
    }
}
