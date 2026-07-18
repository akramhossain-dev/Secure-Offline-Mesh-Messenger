# ProGuard / R8 Rules — Release Build
# Offline Emergency Mesh Communication System

# ─────────────────────────────────────────────────────────────────────────────
# General Android Rules
# ─────────────────────────────────────────────────────────────────────────────

# Keep application class
-keep class com.mesh.emergency.app.MeshApplication { *; }

# Keep all public API surfaces
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Hilt — Dependency Injection
# ─────────────────────────────────────────────────────────────────────────────

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Coroutines
# ─────────────────────────────────────────────────────────────────────────────

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ─────────────────────────────────────────────────────────────────────────────
# Jetpack Compose
# ─────────────────────────────────────────────────────────────────────────────

-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Timber
# ─────────────────────────────────────────────────────────────────────────────

-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ─────────────────────────────────────────────────────────────────────────────
# DataStore / Protobuf
# ─────────────────────────────────────────────────────────────────────────────

-keep class androidx.datastore.** { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Remove debug logging in release
# ─────────────────────────────────────────────────────────────────────────────

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# ─────────────────────────────────────────────────────────────────────────────
# Room Database (A35.2)
# ─────────────────────────────────────────────────────────────────────────────

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-dontwarn androidx.room.**

