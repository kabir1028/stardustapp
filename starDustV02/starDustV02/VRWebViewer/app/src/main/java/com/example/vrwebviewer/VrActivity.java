// COMPLETE VR ACTIVITY - Stereoscopic view with touch/BLE controls
// Features: Split-screen VR, touch drag, BLE gyro, performance optimization
package com.example.vrwebviewer;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VrActivity extends AppCompatActivity implements BleManager.BleListener {
    private static final String TAG = "VrActivity";
    private static final int DEFAULT_CAPTURE_WIDTH = 1024;
    private static final int DEFAULT_CAPTURE_HEIGHT = 576;
    
    private WebView hiddenWebView;
    private ImageView leftEyeView;
    private ImageView rightEyeView;
    private TextView debugOverlay;
    private Button exitButton;
    private Button fullscreenButton;
    
    private Handler captureHandler;
    private ExecutorService backgroundExecutor;
    private Bitmap captureBitmap;
    private Canvas captureCanvas;
    
    private int captureFps = 12;
    private boolean isCapturing = false;
    private boolean isFullscreen = false;
    private long lastCaptureTime = 0;
    private int frameCount = 0;
    
    // Touch control variables
    private float lastTouchX = 0;
    private float lastTouchY = 0;
    private boolean isDragging = false;
    
    // BLE control
    private BleManager bleManager;
    private boolean bleEnabled = false;
    private float gyroSensitivity = 100.0f;
    
    private String websiteUrl;
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vr);
        
        // Get URL and settings
        websiteUrl = getIntent().getStringExtra("url");
        if (websiteUrl == null) {
            websiteUrl = "https://trek.nasa.gov/moon/";
        }
        
        prefs = getSharedPreferences("VRWebViewerPrefs", MODE_PRIVATE);
        captureFps = prefs.getInt("vr_fps", 12);
        bleEnabled = prefs.getBoolean("ble_enabled", false);
        
        Log.d(TAG, "VrActivity created, URL: " + websiteUrl + ", FPS: " + captureFps);
        
        initializeViews();
        setupWebView();
        setupCapture();
        setupBle();
        
        // Force landscape and keep screen on
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    private void initializeViews() {
        hiddenWebView = findViewById(R.id.hidden_webview);
        leftEyeView = findViewById(R.id.left_eye_view);
        rightEyeView = findViewById(R.id.right_eye_view);
        debugOverlay = findViewById(R.id.debug_overlay);
        exitButton = findViewById(R.id.btn_exit_vr);
        fullscreenButton = findViewById(R.id.btn_fullscreen);
        
        exitButton.setOnClickListener(v -> finish());
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        
        // Setup touch listeners for VR interaction
        View.OnTouchListener touchListener = this::handleTouch;
        leftEyeView.setOnTouchListener(touchListener);
        rightEyeView.setOnTouchListener(touchListener);
    }
    
    private void setupWebView() {
        hiddenWebView.getSettings().setJavaScriptEnabled(true);
        hiddenWebView.getSettings().setDomStorageEnabled(true);
        hiddenWebView.getSettings().setLoadWithOverviewMode(true);
        hiddenWebView.getSettings().setUseWideViewPort(true);
        
        hiddenWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "WebView loaded, starting capture");
                startCapture();
            }
        });
        
        hiddenWebView.loadUrl(websiteUrl);
    }
    
    private void setupCapture() {
        captureHandler = new Handler(Looper.getMainLooper());\n        backgroundExecutor = Executors.newSingleThreadExecutor();\n        \n        // Pre-allocate bitmap for capture\n        captureBitmap = Bitmap.createBitmap(DEFAULT_CAPTURE_WIDTH, DEFAULT_CAPTURE_HEIGHT, Bitmap.Config.RGB_565);\n        captureCanvas = new Canvas(captureBitmap);\n    }\n    \n    private void setupBle() {\n        if (bleEnabled) {\n            bleManager = new BleManager(this, this);\n            bleManager.startScanning();\n        }\n    }\n    \n    private void startCapture() {\n        if (isCapturing) return;\n        isCapturing = true;\n        \n        Runnable captureRunnable = new Runnable() {\n            @Override\n            public void run() {\n                if (!isCapturing) return;\n                \n                captureWebView();\n                \n                // Schedule next capture\n                long delay = 1000 / captureFps;\n                captureHandler.postDelayed(this, delay);\n            }\n        };\n        \n        captureHandler.post(captureRunnable);\n    }\n    \n    private void captureWebView() {\n        if (hiddenWebView == null || captureBitmap == null) return;\n        \n        try {\n            // Capture on UI thread\n            hiddenWebView.draw(captureCanvas);\n            \n            // Process on background thread\n            backgroundExecutor.execute(() -> {\n                try {\n                    // Create scaled copies for each eye\n                    Bitmap leftBitmap = Bitmap.createScaledBitmap(captureBitmap, \n                        leftEyeView.getWidth(), leftEyeView.getHeight(), false);\n                    Bitmap rightBitmap = Bitmap.createScaledBitmap(captureBitmap, \n                        rightEyeView.getWidth(), rightEyeView.getHeight(), false);\n                    \n                    // Update UI on main thread\n                    runOnUiThread(() -> {\n                        leftEyeView.setImageBitmap(leftBitmap);\n                        rightEyeView.setImageBitmap(rightBitmap);\n                        updateDebugInfo();\n                    });\n                } catch (Exception e) {\n                    Log.e(TAG, "Error processing capture", e);\n                }\n            });\n        } catch (Exception e) {\n            Log.e(TAG, "Error capturing WebView", e);\n        }\n    }\n    \n    private boolean handleTouch(View view, MotionEvent event) {\n        float x = event.getX();\n        float y = event.getY();\n        \n        // Map touch coordinates to WebView coordinates\n        float webX = (x / view.getWidth()) * hiddenWebView.getWidth();\n        float webY = (y / view.getHeight()) * hiddenWebView.getHeight();\n        \n        switch (event.getAction()) {\n            case MotionEvent.ACTION_DOWN:\n                isDragging = true;\n                lastTouchX = webX;\n                lastTouchY = webY;\n                injectMouseEvent("mousedown", webX, webY);\n                break;\n                \n            case MotionEvent.ACTION_MOVE:\n                if (isDragging) {\n                    injectMouseEvent("mousemove", webX, webY);\n                    lastTouchX = webX;\n                    lastTouchY = webY;\n                }\n                break;\n                \n            case MotionEvent.ACTION_UP:\n            case MotionEvent.ACTION_CANCEL:\n                if (isDragging) {\n                    injectMouseEvent("mouseup", webX, webY);\n                    isDragging = false;\n                }\n                break;\n        }\n        \n        return true;\n    }\n    \n    private void injectMouseEvent(String eventType, float x, float y) {\n        String script = String.format(\n            "var element = document.elementFromPoint(%f, %f); " +\n            "if (element) { " +\n            "  var event = new MouseEvent('%s', { " +\n            "    clientX: %f, clientY: %f, bubbles: true " +\n            "  }); " +\n            "  element.dispatchEvent(event); " +\n            "}", \n            x, y, eventType, x, y\n        );\n        \n        hiddenWebView.evaluateJavascript(script, null);\n    }\n    \n    private void toggleFullscreen() {\n        View decorView = getWindow().getDecorView();\n        \n        if (!isFullscreen) {\n            // Enter fullscreen\n            decorView.setSystemUiVisibility(\n                View.SYSTEM_UI_FLAG_FULLSCREEN |\n                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |\n                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY\n            );\n            findViewById(R.id.vr_controls).setVisibility(View.GONE);\n            isFullscreen = true;\n        } else {\n            // Exit fullscreen\n            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);\n            findViewById(R.id.vr_controls).setVisibility(View.VISIBLE);\n            isFullscreen = false;\n        }\n    }\n    \n    private void updateDebugInfo() {\n        frameCount++;\n        long currentTime = System.currentTimeMillis();\n        \n        if (currentTime - lastCaptureTime >= 1000) {\n            float actualFps = frameCount * 1000.0f / (currentTime - lastCaptureTime);\n            String debugText = String.format("FPS: %.1f | BLE: %s", \n                actualFps, bleEnabled && bleManager != null && bleManager.isConnected() ? "ON" : "OFF");\n            debugOverlay.setText(debugText);\n            \n            frameCount = 0;\n            lastCaptureTime = currentTime;\n        }\n    }\n    \n    // BLE Listener implementation\n    @Override\n    public void onBleConnected() {\n        runOnUiThread(() -> {\n            Toast.makeText(this, "BLE Gyro Connected", Toast.LENGTH_SHORT).show();\n        });\n    }\n    \n    @Override\n    public void onBleDisconnected() {\n        runOnUiThread(() -> {\n            Toast.makeText(this, "BLE Gyro Disconnected", Toast.LENGTH_SHORT).show();\n        });\n    }\n    \n    @Override\n    public void onGyroData(float gx, float gy, float gz) {\n        // Convert gyro data to mouse movement\n        float deltaX = gx * gyroSensitivity;\n        float deltaY = gy * gyroSensitivity;\n        \n        // Apply deadzone\n        if (Math.abs(deltaX) < 0.01f) deltaX = 0;\n        if (Math.abs(deltaY) < 0.01f) deltaY = 0;\n        \n        if (deltaX != 0 || deltaY != 0) {\n            // Simulate mouse movement at center of screen\n            float centerX = hiddenWebView.getWidth() / 2.0f + deltaX;\n            float centerY = hiddenWebView.getHeight() / 2.0f + deltaY;\n            \n            injectMouseEvent("mousemove", centerX, centerY);\n        }\n    }\n    \n    @Override\n    public void onButtonData(boolean left, boolean right, boolean up, boolean down) {\n        // Handle button presses from BLE device\n        if (left || right || up || down) {\n            float centerX = hiddenWebView.getWidth() / 2.0f;\n            float centerY = hiddenWebView.getHeight() / 2.0f;\n            injectMouseEvent("click", centerX, centerY);\n        }\n    }\n    \n    @Override\n    protected void onPause() {\n        super.onPause();\n        isCapturing = false;\n        if (bleManager != null) {\n            bleManager.disconnect();\n        }\n    }\n    \n    @Override\n    protected void onResume() {\n        super.onResume();\n        if (hiddenWebView != null) {\n            startCapture();\n        }\n        if (bleEnabled && bleManager != null) {\n            bleManager.startScanning();\n        }\n    }\n    \n    @Override\n    protected void onDestroy() {\n        isCapturing = false;\n        \n        if (captureHandler != null) {\n            captureHandler.removeCallbacksAndMessages(null);\n        }\n        \n        if (backgroundExecutor != null) {\n            backgroundExecutor.shutdown();\n        }\n        \n        if (bleManager != null) {\n            bleManager.disconnect();\n        }\n        \n        if (hiddenWebView != null) {\n            hiddenWebView.destroy();\n        }\n        \n        if (captureBitmap != null && !captureBitmap.isRecycled()) {\n            captureBitmap.recycle();\n        }\n        \n        super.onDestroy();\n    }\n}