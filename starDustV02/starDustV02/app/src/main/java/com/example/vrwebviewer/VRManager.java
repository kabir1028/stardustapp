// VR MANAGER - Enhanced VR with Google Cardboard SDK
package com.example.vrwebviewer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.view.Display;
import android.view.WindowManager;

// Custom VR implementation without Google VR SDK

public class VRManager implements SensorEventListener {
    
    private Context context;
    private SensorManager sensorManager;
    private Sensor rotationSensor, gyroSensor;
    private VRSettings vrSettings;
    
    // VR matrices
    private float[] headMatrix = new float[16];
    private float[] eyeMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    
    // Head tracking
    private float[] rotationMatrix = new float[16];
    private float[] orientation = new float[3];
    private float[] gyroValues = new float[3];
    
    // Movement smoothing
    private float[] smoothedRotation = new float[3];
    private long lastUpdateTime = 0;
    
    public interface VRListener {
        void onHeadRotationChanged(float[] headMatrix);
        void onEyeMatrixChanged(float[] leftEye, float[] rightEye);
    }
    
    private VRListener vrListener;
    
    public VRManager(Context context) {
        this.context = context;
        this.vrSettings = new VRSettings(context);
        initSensors();
        initMatrices();
    }
    
    private void initSensors() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }
    
    private void initMatrices() {
        Matrix.setIdentityM(headMatrix, 0);
        Matrix.setIdentityM(eyeMatrix, 0);
        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        
        // Setup projection matrix for VR
        float fov = 90.0f;
        float aspect = getScreenAspectRatio();
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, 0.1f, 1000.0f);
    }
    
    private float getScreenAspectRatio() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        return (float) size.x / size.y / 2.0f; // Divide by 2 for stereo
    }
    
    public void startTracking() {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }
    
    public void stopTracking() {
        sensorManager.unregisterListener(this);
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            handleRotationVector(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            handleGyroscope(event.values, currentTime);
        }
        
        updateHeadMatrix();
        lastUpdateTime = currentTime;
    }
    
    private void handleRotationVector(float[] values) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
        SensorManager.getOrientation(rotationMatrix, orientation);
        
        // Apply sensitivity and limits
        float sensitivity = vrSettings.getGyroSensitivity();
        float yawLimit = vrSettings.getVrYawLimit();
        float pitchLimit = vrSettings.getVrPitchLimit();
        
        orientation[0] = Math.max(-yawLimit, Math.min(yawLimit, orientation[0] * sensitivity));
        orientation[1] = Math.max(-pitchLimit, Math.min(pitchLimit, orientation[1] * sensitivity));
    }
    
    private void handleGyroscope(float[] values, long currentTime) {
        if (lastUpdateTime == 0) return;
        
        float dt = (currentTime - lastUpdateTime) / 1000.0f;
        float smoothing = vrSettings.getSmoothMovement();
        
        // Integrate gyroscope data
        for (int i = 0; i < 3; i++) {
            gyroValues[i] = values[i] * dt * vrSettings.getGyroSensitivity();
            smoothedRotation[i] = smoothedRotation[i] * smoothing + gyroValues[i] * (1 - smoothing);
        }
    }
    
    private void updateHeadMatrix() {
        // Create head transformation matrix
        Matrix.setIdentityM(headMatrix, 0);
        
        // Apply rotations
        Matrix.rotateM(headMatrix, 0, (float) Math.toDegrees(orientation[1]), 1, 0, 0); // Pitch
        Matrix.rotateM(headMatrix, 0, (float) Math.toDegrees(orientation[0]), 0, 1, 0); // Yaw
        Matrix.rotateM(headMatrix, 0, (float) Math.toDegrees(orientation[2]), 0, 0, 1); // Roll
        
        if (vrListener != null) {
            vrListener.onHeadRotationChanged(headMatrix);
            
            // Generate eye matrices for stereo rendering
            float[] leftEye = new float[16];
            float[] rightEye = new float[16];
            generateEyeMatrices(leftEye, rightEye);
            vrListener.onEyeMatrixChanged(leftEye, rightEye);
        }
    }
    
    private void generateEyeMatrices(float[] leftEye, float[] rightEye) {
        float eyeSeparation = 0.064f; // 64mm IPD
        
        // Left eye
        Matrix.setIdentityM(leftEye, 0);
        Matrix.translateM(leftEye, 0, -eyeSeparation / 2, 0, 0);
        Matrix.multiplyMM(leftEye, 0, headMatrix, 0, leftEye, 0);
        
        // Right eye
        Matrix.setIdentityM(rightEye, 0);
        Matrix.translateM(rightEye, 0, eyeSeparation / 2, 0, 0);
        Matrix.multiplyMM(rightEye, 0, headMatrix, 0, rightEye, 0);
    }
    
    public void setVRListener(VRListener listener) {
        this.vrListener = listener;
    }
    
    public float[] getHeadMatrix() {
        return headMatrix.clone();
    }
    
    public float[] getProjectionMatrix() {
        return projectionMatrix.clone();
    }
    
    public void calibrateCenter() {
        // Reset orientation to current position as center
        Matrix.setIdentityM(headMatrix, 0);
        smoothedRotation = new float[3];
        orientation = new float[3];
    }
    
    public boolean isVRReady() {
        return rotationSensor != null || gyroSensor != null;
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}