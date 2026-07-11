package com.example.unimove_wearable.tracking

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

private const val TAG = "WatchSyncDebug"

class PhoneSyncListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        FitnessTracker.initialize(applicationContext)

        when (messageEvent.path) {
            DataLayerContracts.PHONE_ACK_PATH -> handleAckMessage(messageEvent)
            DataLayerContracts.PHONE_COMMAND_PATH -> handleCommandMessage(messageEvent)
            else -> super.onMessageReceived(messageEvent)
        }
    }

    private fun handleAckMessage(messageEvent: MessageEvent) {
        runCatching {
            val payload = JSONObject(String(messageEvent.data, Charsets.UTF_8))
            val sessionId = payload.getLong(DataLayerContracts.KEY_SESSION_ID)
            val sequenceNumber = payload.getLong(DataLayerContracts.KEY_SEQUENCE_NUMBER)
            val accepted = payload.optBoolean(DataLayerContracts.KEY_ACCEPTED, true)
            Log.d(
                TAG,
                "Received phone ACK session=$sessionId seq=$sequenceNumber accepted=$accepted",
            )
            FitnessTracker.handlePhoneAck(
                sessionId = sessionId,
                sequenceNumber = sequenceNumber,
                accepted = accepted,
            )
        }.onFailure { error ->
            Log.e(TAG, "Failed to parse phone ack message", error)
        }
    }

    private fun handleCommandMessage(messageEvent: MessageEvent) {
        runCatching {
            val payload = JSONObject(String(messageEvent.data, Charsets.UTF_8))
            val command = payload.getString(DataLayerContracts.KEY_COMMAND)
            val sessionId = payload.takeIf { it.has(DataLayerContracts.KEY_SESSION_ID) }
                ?.getLong(DataLayerContracts.KEY_SESSION_ID)
            Log.i(TAG, "Received phone command=$command session=${sessionId ?: "unspecified"}")

            when (command) {
                DataLayerContracts.COMMAND_START -> {
                    val intent = Intent(this, TrackingForegroundService::class.java).apply {
                        action = TrackingForegroundService.ACTION_START
                    }
                    ContextCompat.startForegroundService(this, intent)
                }
                DataLayerContracts.COMMAND_STOP -> {
                    val intent = Intent(this, TrackingForegroundService::class.java).apply {
                        action = TrackingForegroundService.ACTION_STOP
                        sessionId?.let {
                            putExtra(TrackingForegroundService.EXTRA_SESSION_ID, it)
                        }
                    }
                    startService(intent)
                }
                DataLayerContracts.COMMAND_PAUSE -> FitnessTracker.pauseTracking()
                DataLayerContracts.COMMAND_RESUME -> FitnessTracker.resumeTracking()
                DataLayerContracts.COMMAND_SYNC_STATUS -> FitnessTracker.retryPendingSummarySync()
                else -> Log.w(TAG, "Ignoring unknown phone command=$command")
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to handle phone command message", error)
        }
    }
}
