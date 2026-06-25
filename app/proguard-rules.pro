# SubSpy ProGuard Rules

# Keep Google API client classes
-keep class com.google.api.** { *; }
-keep class com.google.http.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }

# Keep Billing classes
-keep class com.android.vending.billing.** { *; }

# Keep data models
-keep class com.subspy.app.data.model.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
