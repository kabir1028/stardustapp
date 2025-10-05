// VR SETTINGS MANAGER - Centralized configuration for VR experience
package com.example.vrwebviewer;

import android.content.Context;
import android.content.SharedPreferences;

public class VRSettings {
    private static final String PREFS_NAME = "VRWebViewerPrefs";
    
    // Setting keys
    public static final String PREF_CUSTOM_URL = "custom_url";
    public static final String PREF_VR_FPS = "vr_fps";
    public static final String PREF_GYRO_SENSITIVITY = "gyro_sensitivity";
    public static final String PREF_SHOW_FPS = "show_fps";
    public static final String PREF_SHOW_DEBUG = "show_debug";
    public static final String PREF_AUTO_CALIBRATE = "auto_calibrate";
    public static final String PREF_HAPTIC_FEEDBACK = "haptic_feedback";
    public static final String PREF_CROSSHAIR_SIZE = "crosshair_size";
    public static final String PREF_CROSSHAIR_COLOR = "crosshair_color";
    public static final String PREF_CLICK_DELAY = "click_delay";
    public static final String PREF_BLE_ENABLED = "ble_enabled";
    public static final String PREF_SMOOTH_MOVEMENT = "smooth_movement";
    public static final String PREF_ZOOM_SENSITIVITY = "zoom_sensitivity";
    public static final String PREF_PERFORMANCE_MODE = "performance_mode";
    public static final String PREF_BATTERY_SAVER = "battery_saver";
    public static final String PREF_VR_HEAD_TRACKING = "vr_head_tracking";
    public static final String PREF_VR_MOVEMENT_SCALE = "vr_movement_scale";
    public static final String PREF_VR_YAW_LIMIT = "vr_yaw_limit";
    public static final String PREF_VR_PITCH_LIMIT = "vr_pitch_limit";
    public static final String PREF_VR_CALIBRATED = "vr_calibrated";
    public static final String PREF_CALIBRATION_DATA = "calibration_data";
    
    private SharedPreferences prefs;
    
    public VRSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // URL Settings
    public String getCustomUrl() {
        return prefs.getString(PREF_CUSTOM_URL, "https://trek.nasa.gov/moon/");
    }
    
    public void setCustomUrl(String url) {
        prefs.edit().putString(PREF_CUSTOM_URL, url).apply();
    }
    
    // Performance Settings
    public int getVrFps() {
        int baseFps = prefs.getInt(PREF_VR_FPS, 30);
        if (getBatterySaver()) {
            return Math.min(baseFps, 15);
        } else if (getPerformanceMode()) {
            return Math.max(baseFps, 45);
        }
        return baseFps;
    }
    
    public void setVrFps(int fps) {
        prefs.edit().putInt(PREF_VR_FPS, fps).apply();
    }
    
    // Gyro Settings
    public float getGyroSensitivity() {
        return prefs.getFloat(PREF_GYRO_SENSITIVITY, 2.5f);
    }
    
    public void setGyroSensitivity(float sensitivity) {
        prefs.edit().putFloat(PREF_GYRO_SENSITIVITY, sensitivity).apply();
    }
    
    public float getSmoothMovement() {
        return prefs.getFloat(PREF_SMOOTH_MOVEMENT, 0.92f);
    }
    
    public void setSmoothMovement(float smoothing) {
        prefs.edit().putFloat(PREF_SMOOTH_MOVEMENT, smoothing).apply();
    }
    
    // Display Settings
    public boolean getShowFps() {
        return prefs.getBoolean(PREF_SHOW_FPS, true);
    }
    
    public void setShowFps(boolean show) {
        prefs.edit().putBoolean(PREF_SHOW_FPS, show).apply();
    }
    
    public boolean getShowDebug() {
        return prefs.getBoolean(PREF_SHOW_DEBUG, true);
    }
    
    public void setShowDebug(boolean show) {
        prefs.edit().putBoolean(PREF_SHOW_DEBUG, show).apply();
    }
    
    // Crosshair Settings
    public float getCrosshairSize() {
        return prefs.getFloat(PREF_CROSSHAIR_SIZE, 1.0f);
    }
    
    public void setCrosshairSize(float size) {
        prefs.edit().putFloat(PREF_CROSSHAIR_SIZE, size).apply();
    }
    
    public int getCrosshairColor() {
        return prefs.getInt(PREF_CROSSHAIR_COLOR, 0xFFFFFFFF); // White default
    }
    
    public void setCrosshairColor(int color) {
        prefs.edit().putInt(PREF_CROSSHAIR_COLOR, color).apply();
    }
    
    public float getClickDelay() {
        return prefs.getFloat(PREF_CLICK_DELAY, 3.0f);
    }
    
    public void setClickDelay(float delay) {
        prefs.edit().putFloat(PREF_CLICK_DELAY, delay).apply();
    }
    
    // Interaction Settings
    public boolean getAutoCalibrate() {
        return prefs.getBoolean(PREF_AUTO_CALIBRATE, true);
    }
    
    public void setAutoCalibrate(boolean auto) {
        prefs.edit().putBoolean(PREF_AUTO_CALIBRATE, auto).apply();
    }
    
    public boolean getHapticFeedback() {
        return prefs.getBoolean(PREF_HAPTIC_FEEDBACK, true);
    }
    
    public void setHapticFeedback(boolean enabled) {
        prefs.edit().putBoolean(PREF_HAPTIC_FEEDBACK, enabled).apply();
    }
    
    // Connectivity Settings
    public boolean getBleEnabled() {
        return prefs.getBoolean(PREF_BLE_ENABLED, false);
    }
    
    public void setBleEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_BLE_ENABLED, enabled).apply();
    }
    
    // Performance Modes
    public boolean getPerformanceMode() {
        return prefs.getBoolean(PREF_PERFORMANCE_MODE, false);
    }
    
    public void setPerformanceMode(boolean enabled) {
        prefs.edit().putBoolean(PREF_PERFORMANCE_MODE, enabled).apply();
    }
    
    public boolean getBatterySaver() {
        return prefs.getBoolean(PREF_BATTERY_SAVER, false);
    }
    
    public void setBatterySaver(boolean enabled) {
        prefs.edit().putBoolean(PREF_BATTERY_SAVER, enabled).apply();
    }
    
    // Zoom Settings
    public float getZoomSensitivity() {
        return prefs.getFloat(PREF_ZOOM_SENSITIVITY, 1.0f);
    }
    
    public void setZoomSensitivity(float sensitivity) {
        prefs.edit().putFloat(PREF_ZOOM_SENSITIVITY, sensitivity).apply();
    }
    
    // VR Headset Settings
    public boolean getVrHeadTracking() {
        return prefs.getBoolean(PREF_VR_HEAD_TRACKING, true);
    }
    
    public void setVrHeadTracking(boolean enabled) {
        prefs.edit().putBoolean(PREF_VR_HEAD_TRACKING, enabled).apply();
    }
    
    public float getVrMovementScale() {
        return prefs.getFloat(PREF_VR_MOVEMENT_SCALE, 2.5f);
    }
    
    public void setVrMovementScale(float scale) {
        prefs.edit().putFloat(PREF_VR_MOVEMENT_SCALE, scale).apply();
    }
    
    public float getVrYawLimit() {
        return prefs.getFloat(PREF_VR_YAW_LIMIT, 60f);
    }
    
    public void setVrYawLimit(float limit) {
        prefs.edit().putFloat(PREF_VR_YAW_LIMIT, limit).apply();
    }
    
    public float getVrPitchLimit() {
        return prefs.getFloat(PREF_VR_PITCH_LIMIT, 45f);
    }
    
    public void setVrPitchLimit(float limit) {
        prefs.edit().putFloat(PREF_VR_PITCH_LIMIT, limit).apply();
    }
    
    // VR Calibration Status
    public boolean isVRCalibrated() {
        return prefs.getBoolean(PREF_VR_CALIBRATED, false);
    }
    
    public void setVRCalibrated(boolean calibrated) {
        prefs.edit().putBoolean(PREF_VR_CALIBRATED, calibrated).apply();
    }
    
    // Calibration Data Storage
    public void saveCalibrationData(float[][] points) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < points.length; i++) {
            if (points[i] != null) {
                data.append(points[i][0]).append(",")
                    .append(points[i][1]).append(",")
                    .append(points[i][2]);
                if (i < points.length - 1) data.append(";");
            }
        }
        prefs.edit().putString(PREF_CALIBRATION_DATA, data.toString()).apply();
    }
    
    public float[][] getCalibrationData() {
        String data = prefs.getString(PREF_CALIBRATION_DATA, null);
        if (data == null || data.isEmpty()) return null;
        
        try {
            String[] points = data.split(";");
            float[][] result = new float[5][3];
            for (int i = 0; i < Math.min(points.length, 5); i++) {
                String[] values = points[i].split(",");
                if (values.length == 3) {
                    result[i] = new float[]{
                        Float.parseFloat(values[0]),
                        Float.parseFloat(values[1]),
                        Float.parseFloat(values[2])
                    };
                }
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
    
    // Bulk operations
    public void resetToDefaults() {
        prefs.edit()
            .putInt(PREF_VR_FPS, 30)
            .putFloat(PREF_GYRO_SENSITIVITY, 2.5f)
            .putFloat(PREF_CLICK_DELAY, 3.0f)
            .putFloat(PREF_CROSSHAIR_SIZE, 1.0f)
            .putInt(PREF_CROSSHAIR_COLOR, 0xFFFFFFFF)
            .putFloat(PREF_SMOOTH_MOVEMENT, 0.92f)
            .putFloat(PREF_ZOOM_SENSITIVITY, 1.0f)
            .putBoolean(PREF_SHOW_FPS, true)
            .putBoolean(PREF_SHOW_DEBUG, true)
            .putBoolean(PREF_AUTO_CALIBRATE, true)
            .putBoolean(PREF_HAPTIC_FEEDBACK, true)
            .putBoolean(PREF_BLE_ENABLED, false)
            .putBoolean(PREF_PERFORMANCE_MODE, false)
            .putBoolean(PREF_BATTERY_SAVER, false)
            .putBoolean(PREF_VR_HEAD_TRACKING, true)
            .putFloat(PREF_VR_MOVEMENT_SCALE, 2.5f)
            .putFloat(PREF_VR_YAW_LIMIT, 60f)
            .putFloat(PREF_VR_PITCH_LIMIT, 45f)
            .putBoolean(PREF_VR_CALIBRATED, false)
            .remove(PREF_CALIBRATION_DATA)
            .apply();
    }
    
    // Export settings as string for debugging
    public String exportSettings() {
        return String.format(
            "VR Settings:\n" +
            "FPS: %d | Sensitivity: %.1f | Smoothing: %.2f\n" +
            "Crosshair: %.1fx | Click Delay: %.1fs\n" +
            "Show FPS: %s | Debug: %s | Haptic: %s\n" +
            "Performance: %s | Battery Saver: %s | BLE: %s\n" +
            "VR Head Tracking: %s | Movement Scale: %.1fx | Limits: %.0f°/%.0f°",
            getVrFps(), getGyroSensitivity(), getSmoothMovement(),
            getCrosshairSize(), getClickDelay(),
            getShowFps() ? "ON" : "OFF",
            getShowDebug() ? "ON" : "OFF",
            getHapticFeedback() ? "ON" : "OFF",
            getPerformanceMode() ? "ON" : "OFF",
            getBatterySaver() ? "ON" : "OFF",
            getBleEnabled() ? "ON" : "OFF",
            getVrHeadTracking() ? "ON" : "OFF",
            getVrMovementScale(), getVrYawLimit(), getVrPitchLimit()
        );
    }
}