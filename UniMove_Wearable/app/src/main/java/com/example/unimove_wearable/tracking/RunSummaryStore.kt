package com.example.unimove_wearable.tracking

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

internal class RunSummaryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun createSessionId(nowEpochMs: Long = System.currentTimeMillis()): Long = synchronized(lock) {
        val previous = preferences.getLong(KEY_LAST_CREATED_SESSION_ID, 0L)
        val next = maxOf(nowEpochMs, previous + 1L)
        check(preferences.edit().putLong(KEY_LAST_CREATED_SESSION_ID, next).commit()) {
            "Unable to persist the new workout identifier."
        }
        next
    }

    fun savePending(summary: RunSummary): Boolean = synchronized(lock) {
        val pending = readPendingLocked().associateBy { it.sessionId }.toMutableMap()
        val existing = pending[summary.sessionId]
        if (existing == null || summary.sequenceNumber >= existing.sequenceNumber) {
            pending[summary.sessionId] = summary
        }

        preferences.edit()
            .putString(KEY_PENDING_SUMMARIES, encodeSummaries(pending.values.sortedBy { it.sessionId }))
            .putString(KEY_LAST_SUMMARY, encodeSummary(summary).toString())
            .commit()
    }

    fun acknowledge(sessionId: Long, sequenceNumber: Long): Boolean = synchronized(lock) {
        val pending = readPendingLocked().toMutableList()
        val removed = pending.removeAll {
            it.sessionId == sessionId && it.sequenceNumber == sequenceNumber
        }
        if (!removed) return@synchronized false

        if (!preferences.edit()
                .putString(KEY_PENDING_SUMMARIES, encodeSummaries(pending))
                .commit()
        ) {
            Log.e(TAG, "Failed to persist acknowledgement for session=$sessionId seq=$sequenceNumber")
            return@synchronized false
        }
        true
    }

    fun pendingSummaries(): List<RunSummary> = synchronized(lock) {
        readPendingLocked()
    }

    fun lastSummary(): RunSummary? = synchronized(lock) {
        preferences.getString(KEY_LAST_SUMMARY, null)?.let(::decodeSummary)
            ?: migrateLegacySummaryLocked()
    }

    private fun readPendingLocked(): List<RunSummary> {
        val raw = preferences.getString(KEY_PENDING_SUMMARIES, null)
        if (raw != null) {
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        add(decodeSummary(array.getJSONObject(index)))
                    }
                }
            }.onFailure { Log.e(TAG, "Unable to read pending workout summaries", it) }
                .getOrDefault(emptyList())
        }

        val legacy = migrateLegacySummaryLocked()
        val hasLegacyPending = preferences.getBoolean(LEGACY_KEY_PENDING_SYNC, false)
        if (legacy != null && hasLegacyPending) {
            preferences.edit()
                .putString(KEY_PENDING_SUMMARIES, encodeSummaries(listOf(legacy)))
                .putString(KEY_LAST_SUMMARY, encodeSummary(legacy).toString())
                .remove(LEGACY_KEY_PENDING_SYNC)
                .commit()
            return listOf(legacy)
        }
        return emptyList()
    }

    private fun migrateLegacySummaryLocked(): RunSummary? {
        if (!preferences.contains(LEGACY_KEY_SESSION_ID)) return null

        val summary = RunSummary(
            sessionId = preferences.getLong(LEGACY_KEY_SESSION_ID, 0L),
            sequenceNumber = preferences.getLong(LEGACY_KEY_SEQUENCE_NUMBER, 0L),
            durationMs = preferences.getLong(LEGACY_KEY_DURATION_MS, 0L),
            distanceMeters = preferences.getFloat(LEGACY_KEY_DISTANCE_METERS, 0f).toDouble(),
            steps = preferences.getLong(LEGACY_KEY_STEPS, 0L),
            averageHeartRate = preferences.getInt(LEGACY_KEY_AVERAGE_HEART_RATE, 0)
                .takeIf { it > 0 },
            paceMsPerKm = preferences.getLong(LEGACY_KEY_PACE_MS_PER_KM, 0L),
            timestamp = preferences.getLong(LEGACY_KEY_TIMESTAMP, 0L),
        )
        preferences.edit().putString(KEY_LAST_SUMMARY, encodeSummary(summary).toString()).commit()
        return summary
    }

    private fun encodeSummaries(summaries: Collection<RunSummary>): String {
        return JSONArray().apply {
            summaries.forEach { put(encodeSummary(it)) }
        }.toString()
    }

    private fun encodeSummary(summary: RunSummary): JSONObject = JSONObject()
        .put(DataLayerContracts.KEY_SESSION_ID, summary.sessionId)
        .put(DataLayerContracts.KEY_SEQUENCE_NUMBER, summary.sequenceNumber)
        .put(DataLayerContracts.KEY_ELAPSED_MS, summary.durationMs)
        .put(DataLayerContracts.KEY_DISTANCE_METERS, summary.distanceMeters)
        .put(DataLayerContracts.KEY_STEPS, summary.steps)
        .put(DataLayerContracts.KEY_HEART_RATE, summary.averageHeartRate ?: JSONObject.NULL)
        .put(DataLayerContracts.KEY_PACE_MS_PER_KM, summary.paceMsPerKm)
        .put(DataLayerContracts.KEY_TIMESTAMP, summary.timestamp)

    private fun decodeSummary(raw: String): RunSummary? = runCatching {
        decodeSummary(JSONObject(raw))
    }.onFailure { Log.e(TAG, "Unable to read the last workout summary", it) }.getOrNull()

    private fun decodeSummary(json: JSONObject): RunSummary = RunSummary(
        sessionId = json.getLong(DataLayerContracts.KEY_SESSION_ID),
        sequenceNumber = json.getLong(DataLayerContracts.KEY_SEQUENCE_NUMBER),
        durationMs = json.getLong(DataLayerContracts.KEY_ELAPSED_MS),
        distanceMeters = json.getDouble(DataLayerContracts.KEY_DISTANCE_METERS),
        steps = json.getLong(DataLayerContracts.KEY_STEPS),
        averageHeartRate = if (json.isNull(DataLayerContracts.KEY_HEART_RATE)) {
            null
        } else {
            json.getInt(DataLayerContracts.KEY_HEART_RATE)
        },
        paceMsPerKm = json.getLong(DataLayerContracts.KEY_PACE_MS_PER_KM),
        timestamp = json.getLong(DataLayerContracts.KEY_TIMESTAMP),
    )

    private companion object {
        private const val TAG = "WatchSyncDebug"
        private const val PREFERENCES_NAME = "unimove_prefs"
        private const val KEY_PENDING_SUMMARIES = "pending_summaries_v2"
        private const val KEY_LAST_SUMMARY = "last_summary_v2"
        private const val KEY_LAST_CREATED_SESSION_ID = "last_created_session_id"

        private const val LEGACY_KEY_PENDING_SYNC = "pending_sync"
        private const val LEGACY_KEY_SESSION_ID = "last_sessionId"
        private const val LEGACY_KEY_SEQUENCE_NUMBER = "last_sequenceNumber"
        private const val LEGACY_KEY_DURATION_MS = "last_durationMs"
        private const val LEGACY_KEY_DISTANCE_METERS = "last_distanceMeters"
        private const val LEGACY_KEY_STEPS = "last_steps"
        private const val LEGACY_KEY_AVERAGE_HEART_RATE = "last_avgHeartRate"
        private const val LEGACY_KEY_PACE_MS_PER_KM = "last_paceMsPerKm"
        private const val LEGACY_KEY_TIMESTAMP = "last_timestamp"

        private val lock = Any()
    }
}
