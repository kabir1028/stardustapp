// VR CALIBRATION ACTIVITY - Separate calibration interface
package com.example.vrwebviewer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CalibrationActivity extends AppCompatActivity implements SensorEventListener {
    
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private float[] gyroValues = new float[3];
    private float[][] calibrationPoints = new float[5][3];
    private int calibrationStep = 0;
    
    private TextView instructionText, statusText, gyroValuesText;
    private Button captureButton, saveButton;
    private View gyroDot;
    private FrameLayout gyroBallContainer;
    private VRSettings vrSettings;
    private Handler handler;
    
    // Gyro ball visualization
    private float ballCenterX, ballCenterY;
    private float ballRadius = 70f; // Half of 160dp minus padding
    
    // Smoothing for gyro movement
    private float lastDotX = 0f, lastDotY = 0f;
    private final float smoothingFactor = 0.7f;
    
    // Direct gyro positioning - no baseline needed
    
    private String[] instructions = {
        "Hold phone in landscape, look straight ahead",
        "Tilt head UP - watch dot move up", 
        "Tilt head DOWN - watch dot move down",
        "Tilt head LEFT - watch dot move left",
        "Tilt head RIGHT - watch dot move right"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_calibration_landscape);
        
        vrSettings = new VRSettings(this);
        handler = new Handler(Looper.getMainLooper());
        
        initViews();
        setupSensors();
        updateUI();
    }
    
    private void initViews() {
        instructionText = findViewById(R.id.instruction_text);
        statusText = findViewById(R.id.status_text);
        gyroValuesText = findViewById(R.id.gyro_values);
        captureButton = findViewById(R.id.capture_button);
        saveButton = findViewById(R.id.save_button);
        gyroDot = findViewById(R.id.gyro_dot);
        gyroBallContainer = findViewById(R.id.gyro_ball_container);
        
        captureButton.setOnClickListener(v -> capturePosition());
        saveButton.setOnClickListener(v -> saveCalibration());
        
        findViewById(R.id.reset_button).setOnClickListener(v -> resetCalibration());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        // Initialize gyro ball center position
        gyroBallContainer.post(() -> {
            ballCenterX = gyroBallContainer.getWidth() / 2f;
            ballCenterY = gyroBallContainer.getHeight() / 2f;
        });
    }
    
    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }
    
    private void updateUI() {
        if (calibrationStep < instructions.length) {
            instructionText.setText(instructions[calibrationStep]);
            statusText.setText(String.format("Step %d of %d", calibrationStep + 1, instructions.length));
            captureButton.setText("Capture Position");
            captureButton.setEnabled(true);
            saveButton.setEnabled(false);
        } else {
            instructionText.setText("All positions captured!");
            statusText.setText("Ready to save calibration");
            captureButton.setEnabled(false);
            saveButton.setEnabled(true);
        }
    }
    
    private void capturePosition() {
        if (gyroValues != null && calibrationStep < calibrationPoints.length) {
            calibrationPoints[calibrationStep] = gyroValues.clone();
            
            // Visual feedback - flash the dot
            if (gyroDot != null) {
                gyroDot.animate()
                    .scaleX(2f).scaleY(2f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        gyroDot.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(200)
                            .start();
                    })
                    .start();
            }
            
            calibrationStep++;
            
            Toast.makeText(this, "Position " + calibrationStep + " captured! Gyro: " + 
                String.format("%.2f, %.2f, %.2f", gyroValues[0], gyroValues[1], gyroValues[2]), 
                Toast.LENGTH_SHORT).show();
            updateUI();
        }
    }
    
    private void saveCalibration() {
        vrSettings.saveCalibrationData(calibrationPoints);
        vrSettings.setVRCalibrated(true);
        
        Toast.makeText(this, "Calibration saved successfully!", Toast.LENGTH_LONG).show();
        finish();
    }
    
    private void resetCalibration() {
        calibrationStep = 0;
        calibrationPoints = new float[5][3];
        updateUI();
        Toast.makeText(this, "Calibration reset", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroValues = event.values.clone();
            updateGyroVisualization();
        }
    }
    
    private void updateGyroVisualization() {
        if (gyroValues == null || gyroDot == null || ballCenterX == 0) return;
        
        runOnUiThread(() -> {
            // Update gyro values display
            if (gyroValuesText != null) {
                gyroValuesText.setText(String.format("Gyro: X=%.2f Y=%.2f Z=%.2f", 
                    gyroValues[0], gyroValues[1], gyroValues[2]));
            }
            
            // Use absolute gyro values for direct positioning
            // When gyro is 0,0 dot should be at center
            float sensitivity = 100f; // Sensitivity for gyro to pixel conversion
            
            // Map gyro values directly to dot position
            // gyroValues[0] = pitch (forward/back) -> left/right movement
            // gyroValues[1] = roll (left/right) -> up/down movement  
            float dotX = ballCenterX + (gyroValues[0] * sensitivity);
            float dotY = ballCenterY - (gyroValues[1] * sensitivity); // Inverted for natural feel
            
            // Apply smoothing to reduce jitter
            if (lastDotX != 0f || lastDotY != 0f) {
                dotX = lastDotX * smoothingFactor + dotX * (1f - smoothingFactor);
                dotY = lastDotY * smoothingFactor + dotY * (1f - smoothingFactor);
            }
            
            // Constrain dot within circle
            float deltaX = dotX - ballCenterX;
            float deltaY = dotY - ballCenterY;
            float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            
            if (distance > ballRadius) {
                float scale = ballRadius / distance;
                dotX = ballCenterX + deltaX * scale;
                dotY = ballCenterY + deltaY * scale;
            }
            
            // Store for next smoothing
            lastDotX = dotX;
            lastDotY = dotY;
            
            // Update dot position
            gyroDot.setX(dotX - gyroDot.getWidth() / 2f);
            gyroDot.setY(dotY - gyroDot.getHeight() / 2f);
        });
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    
    @Override
    protected void onDestroy() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }
}