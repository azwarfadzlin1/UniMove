<div align="center">

# UniMove

### A campus fitness companion that makes movement social, measurable, and fun.

[![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-JVM_17-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_%2B_Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)

</div>

UniMove is a native Android application developed as a final-year project to help university students build healthier routines. It combines live GPS workouts, campus challenges, leaderboards, social activity, nutrition logging, AI-assisted guidance, and a companion Wear OS experience.

## Highlights

- Live route, distance, pace, and workout tracking
- Campus challenges, achievements, and weekly leaderboards
- Community run sharing and engagement
- Wear OS workout capture and phone synchronization
- Personal statistics, nutrition logging, and contextual guidance
- Firebase Authentication and owner-scoped Firestore data

## Architecture

The application uses Kotlin, Jetpack Compose, Material 3, MVVM-style view models, Coroutines, DataStore, Firebase Auth/Firestore, Google Maps, Play Services Location, and the Wear OS Data Layer.

## Local setup

1. Clone the repository and open it with a current Android Studio release.
2. Use JDK 17 and install Android SDK 36.
3. Register your own Android app in Firebase and place its untracked `google-services.json` in `app/`.
4. Create an untracked `local.properties` using the placeholders below.

```properties
MAPS_API_KEY=your_restricted_android_maps_key
MAPBOX_ACCESS_TOKEN=your_public_mapbox_token
WEATHER_API_KEY=your_weather_provider_key
KALO_API_KEY=your_kalori_api_key
USDA_FDC_API_KEY=your_usda_key
GROQ_API_KEY=your_groq_key
RELEASE_STORE_FILE=C:\\path\\to\\release.keystore
RELEASE_STORE_PASSWORD=replace_me
RELEASE_KEY_ALIAS=replace_me
RELEASE_KEY_PASSWORD=replace_me
```

5. Deploy `firestore.rules` and `firestore.indexes.json` to a development Firebase project.
6. Run `gradlew.bat test lint` on Windows or `./gradlew test lint` on macOS/Linux.

Never commit credentials, signing files, `local.properties`, or `google-services.json`. Mobile applications cannot keep embedded service credentials secret; production AI and paid-service calls should be routed through an authenticated backend.

## Related projects

- [UniMove Wearable](https://github.com/azwarfadzlin1/UniMove_Wearable) — Wear OS workout companion
- UniMove Admin — programme-level participation dashboard

## Security and privacy

Location, fitness, and profile data are sensitive. Review [SECURITY.md](SECURITY.md) before deploying, restrict every cloud API key to the correct Android package/signing certificate, and test Firestore rules against unauthorized clients.

## Project status

This is an academic portfolio project under active development. It is not a medical device and must not be used for diagnosis or emergency response.
