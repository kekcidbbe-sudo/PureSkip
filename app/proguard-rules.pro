# PureSkip ProGuard Rules
# Keep accessibility service
-keep class com.nzsk.pureskip.accessibility.** { *; }

# Keep rules engine
-keep class com.nzsk.pureskip.rules.** { *; }

# Keep data classes
-keepclassmembers class com.nzsk.pureskip.** {
    <fields>;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
