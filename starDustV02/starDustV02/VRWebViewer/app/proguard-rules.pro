# VR Web Viewer ProGuard Rules

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep BLE classes
-keep class com.example.vrwebviewer.BleManager { *; }
-keep interface com.example.vrwebviewer.BleManager$BleListener { *; }

# Keep WebView classes
-keep class android.webkit.** { *; }

# Keep JSON parsing
-keep class org.json.** { *; }

# Keep Bluetooth classes
-keep class android.bluetooth.** { *; }