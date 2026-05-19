# TDLib — keep all TDLib classes intact
-keep class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# Keep JNI-invoked methods
-keepclassmembers class * {
    native <methods>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Keep BuildConfig for TDLib params
-keep class com.invictus.smarttelegramfilter.BuildConfig { *; }

# Preserve stack traces
-keepattributes SourceFile,LineNumberTable
