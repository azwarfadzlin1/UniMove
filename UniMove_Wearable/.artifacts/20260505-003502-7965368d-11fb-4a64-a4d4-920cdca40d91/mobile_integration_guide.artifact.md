# Complete Mobile App Integration Guide

This guide provides step-by-step instructions to connect your UniMove Mobile App to the Watch App and sync data to Firebase.

## Step 1: Add Dependencies
In your **mobile app's** `build.gradle.kts` (Module: app), add the following:

```kotlin
dependencies {
    // Wear OS Data Layer API
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

    // Firebase (if not already added)
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
}
```

---

## Step 2: Create the Data Listener Service
Create a new file called `WatchDataListenerService.kt` in your mobile app (e.g., in a `tracking` or `wear` package).

```kotlin
package com.example.unimove.tracking // Use your mobile app's package name

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class WatchDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path ?: continue

                // We listen for both live updates and final summaries
                if (path.startsWith("/unimove/live_run") || path.startsWith("/unimove/run_session")) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val isEnded = dataMap.getBoolean("isEnded")

                    Log.d("UniMoveSync", "Received data from watch. Path: $path, Ended: $isEnded")

                    if (isEnded) {
                        syncFinalSessionToFirebase(dataMap)
                    } else {
                        // Optional: Update live UI on the phone if the app is open
                        updateLivePhoneUI(dataMap)
                    }
                }
            }
        }
    }

    private fun syncFinalSessionToFirebase(dataMap: com.google.android.gms.wearable.DataMap) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val sessionId = dataMap.getLong("sessionId")

        val payload = mapOf(
            "userId" to uid,
            "source" to "watch",
            "sessionId" to sessionId,
            "timestamp" to dataMap.getLong("timestamp"),
            "durationMs" to dataMap.getLong("elapsedMs"),
            "distanceMeters" to dataMap.getDouble("distanceMeters"),
            "steps" to dataMap.getLong("steps"),
            "avgHeartRate" to dataMap.getInt("heartRate"), // Watch sends avg HR in summary
            "paceMsPerKm" to dataMap.getLong("paceMsPerKm")
        )

        Firebase.firestore
            .collection("users")
            .document(uid)
            .collection("sessions")
            .document(sessionId.toString())
            .set(payload)
            .addOnSuccessListener {
                Log.d("UniMoveSync", "Successfully synced watch session $sessionId to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("UniMoveSync", "Failed to sync to Firebase", e)
            }
    }

    private fun updateLivePhoneUI(dataMap: com.google.android.gms.wearable.DataMap) {
        // Implement this if you want the phone to show live stats while the watch is running
    }
}
```

---

## Step 3: Register the Service in Manifest
In your **mobile app's** `AndroidManifest.xml`, add the service inside the `<application>` tag:

```xml
<service
    android:name=".tracking.WatchDataListenerService"
    android:exported="true">
    <intent-filter>
        <!-- This action allows Google Play Services to bind to your listener -->
        <action android:name="com.google.android.gms.wearable.BIND_LISTENER" />

        <!-- Optional: Filter by URI if you want to be specific -->
        <data android:scheme="wear" android:host="*" android:pathPrefix="/unimove" />
    </intent-filter>
</service>
```

---

## Step 4: Verification (How to know if it works)

### 1. Check Watch Logs (Logcat)
Connect your watch to Android Studio and look for "UniMove" in Logcat.
- You should see messages about `maybeSendUpdate`.
- If sync is successful, `uiState.lastSyncEpochMs` will be updated, and the watch UI will say "Sync ready" (if the phone is connected) or "Sync queued" (if offline).

### 2. Check Wear OS Data Layer (The "Wait" Test)
1. Start a run on the watch.
2. Wait 5-10 seconds.
3. If you have the **Wear OS Data Layer Editor** installed in Android Studio (View -> Tool Windows -> Wear OS Data Layer Editor), you can see the `DataItem`s appearing under `/unimove/live_run`.

### 3. Check Phone Logs
Connect your phone to Android Studio.
- Look for the tag `UniMoveSync`.
- When you stop the run on the watch, you should see: `"Received data from watch... Ended: true"` and `"Successfully synced watch session ... to Firebase"`.

### 4. Check Firebase Console
Go to your Firestore Database. You should see a new document under `users/{userId}/sessions/{sessionId}` with all the metrics (steps, heart rate, distance, etc.).
