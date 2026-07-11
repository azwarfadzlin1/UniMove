package com.example.unimove.wear

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

private const val TAG = "WatchSyncDebug"

class WatchDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: Received ${dataEvents.count} events")
        dataEvents.use { buffer ->
            buffer.forEach { event ->
                val uri = event.dataItem.uri
                val path = uri.path ?: return@forEach

                Log.d(
                    TAG,
                    "DATA LAYER EVENT: Type=${if (event.type == DataEvent.TYPE_CHANGED) "CHANGED" else "DELETED"}, Path=$path"
                )

                if (event.type != DataEvent.TYPE_CHANGED) return@forEach

                val isLiveUpdate = path == DataLayerContracts.LIVE_RUN_PATH
                val isSummary = path == DataLayerContracts.SESSION_SUMMARY_ROOT ||
                    path.startsWith("${DataLayerContracts.SESSION_SUMMARY_ROOT}/") ||
                    path == DataLayerContracts.SESSION_SUMMARY_ALT ||
                    path.startsWith("${DataLayerContracts.SESSION_SUMMARY_ALT}/")

                if (!isLiveUpdate && !isSummary) {
                    Log.v(TAG, "Ignoring unrelated path: $path")
                    return@forEach
                }

                try {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val sourceNodeId = uri.host
                    val sequenceNumber = if (map.containsKey(DataLayerContracts.KEY_SEQUENCE_NUMBER)) {
                        map.getLong(DataLayerContracts.KEY_SEQUENCE_NUMBER)
                    } else {
                        map.getLong(DataLayerContracts.KEY_TIMESTAMP)
                    }
                    Log.d(
                        TAG,
                        "Parsing DataMap [${if (isSummary) "SUMMARY" else "LIVE"}]. Keys: ${map.keySet()}"
                    )

                    val update = WatchSessionUpdate(
                        sessionId = map.getLong(DataLayerContracts.KEY_SESSION_ID),
                        timestamp = map.getLong(DataLayerContracts.KEY_TIMESTAMP),
                        sequenceNumber = sequenceNumber,
                        elapsedMs = map.getLong(DataLayerContracts.KEY_ELAPSED_MS),
                        distanceMeters = map.getDouble(DataLayerContracts.KEY_DISTANCE_METERS),
                        steps = map.getLong(DataLayerContracts.KEY_STEPS),
                        heartRate = map.getInt(DataLayerContracts.KEY_HEART_RATE),
                        paceMsPerKm = map.getLong(DataLayerContracts.KEY_PACE_MS_PER_KM),
                        isEnded = map.getBoolean(DataLayerContracts.KEY_IS_ENDED),
                    )

                    Log.i(
                        TAG,
                        "Received update: id=${update.sessionId}, seq=${update.sequenceNumber}, ended=${update.isEnded}, dist=${update.distanceMeters}m"
                    )

                    WatchSessionRepository.getInstance(applicationContext)
                        .onWatchUpdate(update, sourceNodeId = sourceNodeId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse DataItem: $path", e)
                }
            }
        }
    }
}
