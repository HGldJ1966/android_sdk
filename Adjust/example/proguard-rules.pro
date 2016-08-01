# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/pfms/Development/Android_SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class com.adjust.sdk.plugin.MacAddressUtil { java.lang.String getMacAddress(android.content.Context); }
-keep class com.adjust.sdk.plugin.AndroidIdUtil { java.lang.String getAndroidId(android.content.Context); }

-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}

-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo (android.content.Context);
}

-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId ();
    boolean isLimitAdTrackingEnabled();
}

-libraryjars <java.home>/lib/rt.jar

-dontwarn okio.**
-dontwarn org.apache.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static *** d(...);
    public static *** w(...);
    public static *** e(...);
    public static *** v(...);
    public static *** i(...);
    public static *** wtf(...);
}