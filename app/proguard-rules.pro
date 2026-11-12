# ---------------------------------------------------------------------------
# R8 / ProGuard rules
# ---------------------------------------------------------------------------

# Google API client + generated service models rely on reflection and generic
# signatures. Without these rules the Sheets/Drive clients crash at runtime.
-keep class com.google.api.client.googleapis.json.GoogleJsonResponse { *; }
-keep class com.google.api.client.googleapis.services.json.* { *; }
-keep class com.google.api.services.sheets.model.** { *; }
-keep class com.google.api.services.drive.model.** { *; }
-keepclassmembers class * extends com.google.api.client.json.GenericJson { *; }
-keepclassmembers class * extends com.google.api.client.http.HttpRequest { *; }

# Gson (used by GoogleJsonFactory) requires these.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.**
-keep class sun.reflect.** { *; }

# Google Play services / Credential Manager
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Kotlin metadata used by kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class com.messmate.bd.** {
    *** Companion;
}
-keepclasseswithmembers class com.messmate.bd.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager workers are instantiated reflectively.
-keep class com.messmate.bd.data.work.** { *; }
