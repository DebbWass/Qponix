# ─── CuponsSMS ProGuard Rules ───

# Keep Room entities and DAOs
-keep class com.cupons.sms.data.db.entity.** { *; }
-keep interface com.cupons.sms.data.db.dao.** { *; }

# Keep Domain models
-keep class com.cupons.sms.domain.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Don't log sensitive data in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
