package com.example.unimove.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

private const val MESSAGE_TAG = "WatchSyncDebug"

enum class WatchCommandType {
    START,
    PAUSE,
    RESUME,
    STOP,
    SYNC_STATUS
}

class WatchMessageGateway private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val commandSendMutex = Mutex()

    suspend fun sendAck(
        nodeId: String,
        sessionId: Long,
        sequenceNumber: Long,
        accepted: Boolean
    ) {
        val payload = JSONObject()
            .put(DataLayerContracts.KEY_SESSION_ID, sessionId)
            .put(DataLayerContracts.KEY_SEQUENCE_NUMBER, sequenceNumber)
            .put(DataLayerContracts.KEY_ACCEPTED, accepted)
            .toString()
            .toByteArray(Charsets.UTF_8)

        var lastError: Throwable? = null
        repeat(ACK_SEND_ATTEMPTS) { attempt ->
            val result = runCatching {
                messageClient
                    .sendMessage(nodeId, DataLayerContracts.PHONE_ACK_PATH, payload)
                    .await()
            }
            if (result.isSuccess) {
                Log.d(MESSAGE_TAG, "Sent ACK for session=$sessionId seq=$sequenceNumber to $nodeId")
                return
            }

            lastError = result.exceptionOrNull()
            if (attempt < ACK_SEND_ATTEMPTS - 1) {
                delay(ACK_RETRY_DELAY_MS)
            }
        }

        throw lastError ?: IllegalStateException(
            "ACK failed for session=$sessionId seq=$sequenceNumber"
        )
    }

    suspend fun sendCommandToConnectedNodes(
        command: WatchCommandType,
        sessionId: Long? = null
    ) = commandSendMutex.withLock {
        val payloadObject = JSONObject()
            .put(DataLayerContracts.KEY_COMMAND, command.name)
        if (sessionId != null) {
            payloadObject.put(DataLayerContracts.KEY_SESSION_ID, sessionId)
        }
        val payload = payloadObject.toString().toByteArray(Charsets.UTF_8)

        // Wear sync is optional. Some Android phones expose the Play Services
        // wearable classes but do not provide Wearable.API at runtime. In that
        // case connectedNodes fails with API_UNAVAILABLE instead of returning
        // an empty list, so treat it exactly like having no paired watch.
        val nodes = runCatching { nodeClient.connectedNodes.await() }
            .getOrElse { error ->
                Log.i(
                    MESSAGE_TAG,
                    "Wear OS API unavailable; ${command.name} will run phone-only",
                    error
                )
                return@withLock
            }
        if (nodes.isEmpty()) {
            Log.w(MESSAGE_TAG, "No connected Wear OS node; ${command.name} was not sent")
            return@withLock
        }

        nodes.forEach { node ->
            var lastError: Throwable? = null
            repeat(COMMAND_SEND_ATTEMPTS) { attempt ->
                val result = runCatching {
                    messageClient
                        .sendMessage(node.id, DataLayerContracts.PHONE_COMMAND_PATH, payload)
                        .await()
                }
                if (result.isSuccess) {
                    Log.d(MESSAGE_TAG, "Sent ${command.name} command to node ${node.id}")
                    return@forEach
                }

                lastError = result.exceptionOrNull()
                if (attempt < COMMAND_SEND_ATTEMPTS - 1) {
                    delay(COMMAND_RETRY_DELAY_MS)
                }
            }

            lastError?.let { error ->
                Log.w(
                    MESSAGE_TAG,
                    "Failed to send ${command.name} command to node ${node.id} after $COMMAND_SEND_ATTEMPTS attempts",
                    error
                )
            }
        }
    }

    companion object {
        private const val ACK_SEND_ATTEMPTS = 3
        private const val ACK_RETRY_DELAY_MS = 300L
        private const val COMMAND_SEND_ATTEMPTS = 3
        private const val COMMAND_RETRY_DELAY_MS = 400L

        @Volatile private var INSTANCE: WatchMessageGateway? = null

        fun getInstance(context: Context): WatchMessageGateway =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchMessageGateway(context).also { INSTANCE = it }
            }
    }
}
