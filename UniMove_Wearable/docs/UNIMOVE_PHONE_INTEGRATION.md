# UniMove Wear OS integration

The phone and watch communicate through the Wear OS Data Layer. Matching build
types must use application ID `com.example.unimove` and the same signing
certificate. The wearable namespace remains `com.example.unimove_wearable`.
There is no application ID suffix on any wearable build type.

The wearable release build reads the phone release keystore settings from the
parent phone project's `local.properties`. Both debug builds use the standard
debug keystore for the current development user.

## Contract

Watch-to-phone `DataItem` paths:

- Live state: `/unimove/live_run`
- Completed session: `/unimove/watch_session/{sessionId}`

Phone-to-watch `MessageClient` paths:

- Commands: `/unimove/phone_command`
- Acknowledgements: `/unimove/phone_ack`

Live and final payload keys:

- `sessionId: Long`
- `timestamp: Long`
- `sequenceNumber: Long`
- `elapsedMs: Long`
- `distanceMeters: Double`
- `steps: Long`
- `heartRate: Int`
- `paceMsPerKm: Long`
- `isEnded: Boolean`

Final delivery may also include `deliveryAttempt: Long`. This is transport-only:
the phone ignores its value, while it ensures an explicit retry produces a
Data Layer change without modifying the final summary or sequence number.

Command payloads contain `command` with one of `START`, `PAUSE`, `RESUME`,
`STOP`, or `SYNC_STATUS`. `STOP` may also contain `sessionId`.

Acknowledgement payloads contain `sessionId`, `sequenceNumber`, and `accepted`.
An `accepted=false` acknowledgement means the phone already persisted that
update and is therefore also terminal for the matching pending summary.

## Delivery and ordering

- The watch creates a durable, monotonically unique `sessionId` for every run.
- Sequence numbers increase within a session. The final sequence is greater
  than every live sequence and remains fixed across retries.
- Live updates are published no more often than once every five seconds.
- A final summary is committed to watch storage before its `DataItem` is sent.
- `putDataItem()` success only confirms acceptance by the Data Layer transport.
  It does not clear pending state.
- Pending summaries are removed only by an acknowledgement matching both
  `sessionId` and `sequenceNumber`.
- Pending summaries survive process death and multiple offline runs. They are
  retried when a phone node reconnects and on `SYNC_STATUS`.
- The phone must persist an update before acknowledging it. Older or duplicate
  sequence numbers must never replace newer phone state; duplicates should be
  acknowledged with `accepted=false`.

## Execution and permissions

The watch starts a health foreground service before starting a Health Services
running exercise. Its ongoing activity reopens `MainActivity`; commands never
launch an activity from the background. The service remains foreground through
exercise shutdown and stops only after a summary or fatal start error.

Before starting, the watch validates running support for heart rate, distance,
and steps and requires:

- `ACTIVITY_RECOGNITION`
- `BODY_SENSORS` through API 35
- `android.permission.health.READ_HEART_RATE` on API 36+
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_HEALTH`
- `POST_NOTIFICATIONS` on API 33+ for the ongoing workout activity

Permissions must be granted from the watch UI. A remote start with missing
permission moves the watch UI to an actionable error state.

## Accounts and offline use

The watch has no Firebase or authentication dependency. It tracks and persists
runs without a connected or signed-in phone. The phone assigns the current
phone account when it uploads an acknowledged completed session.

Connection status is based only on `NodeClient.connectedNodes`. It indicates
reachability, not processing. The UI reports phone availability separately from
`Sync pending`, `Synced to phone`, and `Sync failed/retrying`.

## Device verification

1. Install phone and watch APKs of the same build type and certificate.
2. Pair the devices and grant watch motion and heart-rate permissions.
3. Send `START` from the phone and confirm the watch ongoing activity appears.
4. Confirm increasing live sequence numbers arrive at most every five seconds.
5. Pause, resume, disconnect, reconnect, and then stop the run.
6. Confirm one final sequence is retried until the phone ACKs the exact pair.
7. Confirm pending sync clears after either accepted or duplicate ACK.
8. Capture both sides with `adb logcat -s WatchSyncDebug`.
