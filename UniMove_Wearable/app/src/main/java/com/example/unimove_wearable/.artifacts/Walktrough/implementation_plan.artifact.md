# Companion App Sync and Integration

Implement independent tracking on Wear OS with automatic syncing to the phone app, including a summary screen and local persistence.

## Proposed Changes

### Tracking Models

#### [TrackingModels.kt](file:///C:/Users/azwar/OneDrive/Desktop/UniMove_Wearable/app/src/main/java/com/example/unimove_wearable/tracking/TrackingModels.kt)

- Added `SUMMARY` to `TrackingPhase` enum.
- Added `RunSummary` data class to store session results (distance, steps, HR, duration, pace).
- Updated `TrackingUiState` to include `steps` and `summary`.

### Data Layer Contracts

#### [DataLayerContracts.kt](file:///C:/Users/azwar/OneDrive/Desktop/UniMove_Wearable/app/src/main/java/com/example/unimove_wearable/tracking/DataLayerContracts.kt)

- Added `KEY_STEPS` ("steps") to align with the phone app's expected keys.

### Fitness Tracker Logic

#### [FitnessTracker.kt](file:///C:/Users/azwar/OneDrive/Desktop/UniMove_Wearable/app/src/main/java/com/example/unimove_wearable/tracking/FitnessTracker.kt)

- **Steps Tracking**: Added `DataType.STEPS_TOTAL` to tracking configuration and capabilities check.
- **Average Heart Rate**: Added a list to collect heart rate samples during the run to calculate the average at the end.
- **Summary Phase**: Updated `onExerciseUpdateReceived` to transition to `SUMMARY` phase instead of clearing the state immediately.
- **Local Persistence**: Implemented `saveSummaryLocally` and `loadLastSummary` using `SharedPreferences` to ensure the last session is available even if the app restarts.
- **Sync Logic**: Enhanced `maybeSendUpdate` to include steps and ensure the summary is sent to the phone via the Wearable Data Layer.

### User Interface

#### [MainActivity.kt](file:///C:/Users/azwar/OneDrive/Desktop/UniMove_Wearable/app/src/main/java/com/example/unimove_wearable/presentation/MainActivity.kt)

- **Summary Screen**: Added a new `SummaryScreen` composable to display the results of the run.
- **WearApp Navigation**: Updated to show `SummaryScreen` when the phase is `SUMMARY`.
- **Start Screen Enhancement**: Added a "Last Run" card to `RunStartScreen` to show the summary of the previous session.

## Verification Plan

### Manual Verification
- Start a run on the watch: Verify that distance, steps, and heart rate are tracked.
- Stop the run: Verify that the `SummaryScreen` appears with accurate stats.
- Restart the app: Verify that the "Last Run" information is preserved on the start screen.
- Sync check: Verify (via logs or phone app if available) that `DataItem`s are created on `/unimove/live_run` and `/unimove/run_session`.
