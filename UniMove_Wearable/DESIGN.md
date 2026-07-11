# Design

## Surface

Native Wear OS Jetpack Compose UI for a 40mm round Samsung Galaxy Watch 7 class display.

## Visual Direction

Use a restrained black instrument-panel theme with one mint action accent, amber pause state, red stop/heart-rate state, and cyan pace/sync detail. The interface should feel purpose-built for activity tracking, not like a default Android sample.

## Screen Plan

Start screen:
- Show phone connection as a compact status pill at the top.
- Use a focused UniMove title and a short readiness message.
- If a previous run exists, show it as a compact last-run strip with distance and duration.
- Keep the start action as the dominant full-width button, with permission/support errors in plain text.

Live run screen:
- Make elapsed time and distance the primary stack.
- Show heart rate, pace, steps, and sync as compact metric rows.
- Keep pause/resume and stop as large circular controls near the bottom.
- Keep all content inside a ScalingLazyColumn so the 40mm round screen can scroll without clipping.
- In ambient mode, reduce to critical static information and avoid bright filled surfaces.

Summary screen:
- Lead with total distance, then duration, steps, average heart rate, and pace.
- Show sync status before the finish action.
- Keep the finish action at the bottom and scrollable.

## Component Rules

- Background: black.
- Surfaces: dark graphite with subtle borders only where hierarchy needs grouping.
- Primary action: mint.
- Pause/resume: amber for pause, mint for resume.
- Stop/error/heart rate: red.
- Pace/sync detail: cyan or muted gray depending on status.
- Shape: pill buttons and compact rounded panels only, no nested cards.
- Typography: system sans, bold numerals, short uppercase-style labels avoided except metric abbreviations.

## Runtime Behavior

While a tracking session is active, publish a Wear OS Ongoing Activity tied to the foreground tracking notification so the system keeps the app visible longer and provides a one-tap return path from the watch face. Observe ambient transitions so the Compose UI can switch to a lower-power version without changing the underlying tracking or phone-sync code.
