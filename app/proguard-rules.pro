# Google Play Services / AdMob Keep Rules
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }


# 保留 WorkManager 相關類別與初始化 Provider
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.startup.InitializationProvider { *; }


# For WebViews
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn javax.cache.CacheException
-dontwarn javax.cache.spi.CachingProvider
-dontwarn org.osgi.service.component.annotations.Component
