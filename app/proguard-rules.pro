# UniMove Security Hardening ProGuard Rules

# --- General Android & Compose ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# --- Retrofit & OkHttp ---
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**

# --- Gson / Serialization ---
# Keep model classes used for JSON parsing to avoid field name mismatch
-keep class com.example.unimove.data.model.** { *; }
-keep class com.example.unimove.data.weather.** { *; }
-keep class com.example.unimove.data.groq.** { *; }
-keep class com.example.unimove.data.meal.** { *; }

# --- Vico Charts ---
-keep class com.patrykandpatrick.vico.** { *; }

# --- Lottie ---
-keep class com.airbnb.lottie.** { *; }
