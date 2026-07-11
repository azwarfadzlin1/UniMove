package com.example.unimove.wear

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

private const val DB_NAME    = "unimove_watch.db"
private const val DB_VERSION = 4
private const val TABLE      = "watch_sessions"

/**
 * Lightweight SQLiteOpenHelper-based storage for watch session state.
 * Intentionally avoids Room annotation processing (KAPT/KSP) to stay
 * compatible with Kotlin 2.3.0 and AGP 9.x without code-gen toolchain issues.
 */
class WatchDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    /** Emits whenever rows change — used to drive the observe flow. */
    private val _changes = MutableSharedFlow<Unit>(replay = 1)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE (
                sessionId      INTEGER PRIMARY KEY,
                timestamp      INTEGER NOT NULL,
                sequenceNumber INTEGER NOT NULL DEFAULT 0,
                elapsedMs      INTEGER NOT NULL,
                distanceMeters REAL    NOT NULL,
                steps          INTEGER NOT NULL DEFAULT 0,
                heartRate      INTEGER NOT NULL,
                paceMsPerKm    INTEGER NOT NULL DEFAULT 0,
                isEnded        INTEGER NOT NULL DEFAULT 0,
                isSynced       INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL(
                "ALTER TABLE $TABLE ADD COLUMN sequenceNumber INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    // ── DAO methods ─────────────────────────────────────────────────────────

    suspend fun upsert(entity: WatchSessionEntity) = withContext(Dispatchers.IO) {
        val rowId = writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            entity.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
        check(rowId != -1L) {
            "Failed to persist watch session ${entity.sessionId} sequence ${entity.sequenceNumber}"
        }
        _changes.emit(Unit)
    }

    fun observeLatest(): Flow<WatchSessionEntity?> =
        _changes
            .onStart { emit(Unit) }           // seed so subscribers get the current value
            .map { queryLatest() }

    suspend fun getLatest(): WatchSessionEntity? = withContext(Dispatchers.IO) { queryLatest() }

    suspend fun markSynced(sessionId: Long) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL(
            "UPDATE $TABLE SET isSynced = 1 WHERE sessionId = ?",
            arrayOf(sessionId)
        )
    }

    suspend fun getPendingSync(): List<WatchSessionEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WatchSessionEntity>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE WHERE isEnded = 1 AND isSynced = 0", null
        ).use { c ->
            while (c.moveToNext()) list += c.toEntity()
        }
        list
    }

    suspend fun pruneOldSynced() = withContext(Dispatchers.IO) {
        writableDatabase.execSQL(
            """DELETE FROM $TABLE
               WHERE isSynced = 1
                 AND sessionId NOT IN (
                     SELECT sessionId FROM $TABLE
                     WHERE isSynced = 1
                     ORDER BY timestamp DESC LIMIT 5
                 )"""
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun queryLatest(): WatchSessionEntity? =
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE ORDER BY timestamp DESC LIMIT 1", null
        ).use { c -> if (c.moveToFirst()) c.toEntity() else null }

    private fun WatchSessionEntity.toContentValues() = ContentValues().apply {
        put("sessionId",       sessionId)
        put("timestamp",       timestamp)
        put("sequenceNumber",  sequenceNumber)
        put("elapsedMs",       elapsedMs)
        put("distanceMeters",  distanceMeters)
        put("steps",           steps)
        put("heartRate",       heartRate)
        put("paceMsPerKm",     paceMsPerKm)
        put("isEnded",         if (isEnded) 1 else 0)
        put("isSynced",        if (isSynced) 1 else 0)
    }

    private fun android.database.Cursor.toEntity() = WatchSessionEntity(
        sessionId      = getLong(getColumnIndexOrThrow("sessionId")),
        timestamp      = getLong(getColumnIndexOrThrow("timestamp")),
        sequenceNumber = getLong(getColumnIndexOrThrow("sequenceNumber")),
        elapsedMs      = getLong(getColumnIndexOrThrow("elapsedMs")),
        distanceMeters = getDouble(getColumnIndexOrThrow("distanceMeters")),
        steps          = getLong(getColumnIndexOrThrow("steps")),
        heartRate      = getInt(getColumnIndexOrThrow("heartRate")),
        paceMsPerKm    = getLong(getColumnIndexOrThrow("paceMsPerKm")),
        isEnded        = getInt(getColumnIndexOrThrow("isEnded")) != 0,
        isSynced       = getInt(getColumnIndexOrThrow("isSynced")) != 0,
    )

    companion object {
        @Volatile private var INSTANCE: WatchDatabase? = null
        fun getInstance(context: Context): WatchDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchDatabase(context).also { INSTANCE = it }
            }
    }
}
