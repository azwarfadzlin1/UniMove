# Implementation Plan - Fix Watch-Phone Sync and Connection Visibility

This plan addresses the issue where the Galaxy Watch 7 sessions are not being stored in the phone or Firebase, despite tracking working correctly. It also adds a connection indicator to the watch UI.

## User Review Required

> [!IMPORTANT]
> - **Path Handling**: I will improve the `WatchDataListenerService` in the phone app to correctly handle the `/unimove/watch_session/{sessionId}` path by using `startsWith` more robustly.
> - **Connection Indicator**: I will add a new `isPhoneConnected` state to the `FitnessTracker` and `TrackingUiState`, and display a visual indicator on the watch screen.
> - **Enhanced Logging**: I will add more verbose logging in `WatchSessionRepository` and `WatchDataListenerService` with the tag `WatchSyncDebug` to make it easier to trace in Logcat.

## Proposed Changes

### [Phone App] MyApp Sync Refinement

#### [WatchDataListenerService.kt](file:///C:/Users/azwar/OneDrive/Desktop/MyApp/app/src/main/java/com/example/unimove/wear/WatchDataListenerService.kt)
- Improve path matching logic.
- Add `WatchSyncDebug` tag for easier log filtering.

#### [WatchSessionRepository.kt](file:///C:/Users/azwar/OneDrive/Desktop/MyApp/app/src/main/java/com/example/unimove/wear/WatchSessionRepository.kt)
- Add more detailed logs during Firestore upload (success/failure).
- Use `WatchSyncDebug` tag.

---

### [Watch App] UniMove_Wearable UI and Tracking Improvements

#### [TrackingModels.kt](file:///C:/Users/azwar/OneDrive/Desktop/UniMove_Wearable/app/src/main/java/com/example/unimove_wearable/tracking/TrackingModels.kt)
- Add `val isPhoneConnected: Boolean = false` to `TrackingUiState`.

#### [FitnessTracker.kt](file:///C:/Users/azwar/OneDrive/Desktop/UniMove_Wearable/app/src/main/java/com/example/unimove_wearable/tracking/FitnessTracker.kt)
- Implement `NodeClient` listener to track phone connection status.
- Update `TrackingUiState.isPhoneConnected` in real-time.

#### [MainActivity.kt](file:///C:/Users/azwar/OneDrive/Desktop/UniMove_Wearable/app/src/main/java/com/example/unimove_wearable/presentation/MainActivity.kt)
- Add a small connection indicator icon (phone icon) in the `LiveRunScreen` and `RunStartScreen`.
- Green icon when connected, Grey/Red when disconnected.

## Verification Plan

### Automated Tests
- I will run `analyze_file` on all modified files to ensure syntax correctness.

### Manual Verification
1. **Logcat Monitoring**: I will instruct the user to run `adb logcat -s WatchSyncDebug` to verify the sync process.
2. **Connection Toggle**: I will ask the user to toggle Bluetooth on the phone and verify if the watch's connection indicator updates correctly.
3. **End-to-End Sync**: I will ask the user to finish a session on the watch and check if the phone's "Insights" screen updates (even if just the lifetime stats increase).
