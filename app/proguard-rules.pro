# TeamFund R8 rules.
# Room, Compose, Navigation, DataStore and Lifecycle ship their own consumer keep rules,
# so only app-specific rules belong here.

# Room: keep the generated database implementation (looked up by reflection).
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Enum values are stored in the database by name (see Converters); keep the names stable.
-keepclassmembers enum com.teamfund.manager.domain.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep line numbers for readable crash stack traces (upload mapping.txt to Play Console).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
