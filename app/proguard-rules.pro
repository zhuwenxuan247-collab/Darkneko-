# Glance + Widget
-keep class androidx.glance.** { *; }
-keep class com.xc.quotewidget.** { *; }

# AlarmManager / BroadcastReceiver
-keep class * extends android.content.BroadcastReceiver

# Google Credential Manager
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-keepattributes *Annotation*

# DataStore Preferences
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Coil 图片加载
-keep class coil.** { *; }
