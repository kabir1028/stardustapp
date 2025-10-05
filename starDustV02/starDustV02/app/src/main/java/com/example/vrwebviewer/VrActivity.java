// PRO-LEVEL VR ACTIVITY - Optimized gyro and crosshair interaction
package com.example.vrwebviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VrActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "VrActivity";
    private static final long MENU_TRIGGER_DELAY = 1500;
    private static final long CROSSHAIR_CLICK_DELAY = 2000; // 2 seconds for crosshair click

    private WebView hiddenWebView;
    private ImageView leftEyeView, rightEyeView;
    private View leftCrosshair, rightCrosshair, menuTrigger;
    private LinearLayout vrMenu;
    private TextView debugOverlay, instructionsOverlay;

    private Handler captureHandler, menuHandler, uiHandler, crosshairHandler;
    private ExecutorService backgroundExecutor;
    private Bitmap captureBitmap, leftDistortedBitmap, rightDistortedBitmap;
    private Canvas captureCanvas, leftCanvas, rightCanvas;
    private Matrix leftMatrix, rightMatrix;
    private Paint crosshairPaint, uiPaint;

    // Pro VR features
    private GestureDetector gestureDetector;
    private Vibrator vibrator;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];
    private boolean isCalibrated = false;
    private float[] calibrationOffset = new float[3];

    // Screen dimensions
    private int screenWidth, screenHeight, eyeWidth, eyeHeight;
    private int webViewWidth, webViewHeight;

    // VR-Optimized movement system for headset use
    private SensorManager sensorManager;
    private Sensor gyroSensor, accelerometer, magnetometer;
    private float[] gyroValues = new float[3];
    private float[] accelValues = new float[3];
    private float[] magnetValues = new float[3];
    private float[] rotationVector = new float[4];

    // VR Head tracking system - Enhanced for VR headset
    private float crosshairX = 0.5f, crosshairY = 0.5f;
    private float targetX = 0.5f, targetY = 0.5f;
    private float sensitivity = 2.2f; // Increased for better response
    private float smoothing = 0.65f; // Reduced for less jitter

    // VR Orientation tracking
    private float[] baseOrientation = new float[3];
    private float[] currentOrientation = new float[3];
    private boolean isVRCalibrated = false;

    // Enhanced calibration tutorial system
    private boolean isCalibrationTutorial = false;
    private int calibrationStep = 0;
    private float[][] calibrationPoints = new float[5][3];
    private String[] calibrationInstructions = {
        "Look straight ahead at CENTER",
        "Tilt head UP (look at ceiling)",
        "Tilt head DOWN (look at floor)",
        "Turn head LEFT (comfortable angle)",
        "Turn head RIGHT (comfortable angle)"
    };
    private String[] calibrationTips = {
        "Keep head level and look forward",
        "Tilt head back naturally",
        "Tilt head down naturally",
        "Turn left as far as comfortable",
        "Turn right as far as comfortable"
    };
    private int calibrationCountdown = 0;
    private TextView calibrationOverlay;

    // VR Movement constraints - Optimized for full screen access
    private float maxHeadYaw = 30f;   // Reduced angle for full screen coverage
    private float maxHeadPitch = 25f; // Reduced angle for full screen coverage
    private float vrMovementScale = 1.8f; // Optimized scale factor

    // Crosshair click system
    private boolean isCrosshairHovering = false;
    private long crosshairHoverStart = 0;
    private float crosshairClickProgress = 0f;

    // Performance and visual enhancements
    private int captureFps = 30; // REDUCED from 60 for better performance
    private boolean isCapturing = false;
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private float zoom = 1.0f;
    private boolean showUI = true;

    // Menu and interaction
    private boolean isMenuVisible = false;
    private boolean isMenuTriggering = false;
    private float menuProgress = 0f;

    private String websiteUrl;
    private VRSettings vrSettings;
    private float crosshairSize = 1.0f;
    private float clickDelay = 3.0f;
    private String vrMode = "standard"; // "standard" or "hardware"
    private boolean isHardwareMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_vr);

            websiteUrl = getIntent().getStringExtra("url");
            String destination = getIntent().getStringExtra("destination");
            
            if (websiteUrl == null) {
                if (destination != null) {
                    switch (destination) {
                        case "mars":
                            websiteUrl = "https://nasa2-git-main-arur17s-projects.vercel.app/";
                            break;
                        case "moon":
                            websiteUrl = "https://vr-git-main-mohammad-kasims-projects.vercel.app?_vercel_share=U9Uj7doFmcCeN6TkbQEt2GclFMvyMQfj";
                            break;
                        case "venus":
                            websiteUrl = "https://nasa2-git-main-arur17s-projects.vercel.app/";
                            break;
                        case "galaxy":
                            websiteUrl = "https://www.nasa.gov/universe/galaxies/";
                            break;
                        case "constellations":
                            websiteUrl = "https://www.constellation-guide.com/";
                            break;
                        default:
                            websiteUrl = "https://vr-git-main-mohammad-kasims-projects.vercel.app?_vercel_share=U9Uj7doFmcCeN6TkbQEt2GclFMvyMQfj";
                            break;
                    }
                } else {
                    websiteUrl = "https://trek.nasa.gov/moon/";
                }
            }

            vrSettings = new VRSettings(this);
            loadSettings();

            initializeProVR();
            getScreenDimensions();

            if (!initializeViews()) {
                finish();
                return;
            }

            setupWebView();
            setupCapture();
            setupSensors();
            setupGestures();
            setupMenuSystem();

            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            enterImmersiveMode();

            vrMode = getIntent().getStringExtra("vr_mode");
            if (vrMode == null) vrMode = "standard";
            isHardwareMode = "hardware".equals(vrMode);
            
            boolean calibrationMode = getIntent().getBooleanExtra("calibration_mode", false);

            if (calibrationMode || (!vrSettings.isVRCalibrated() && !isHardwareMode)) {
                showCalibrationInstructions();
            } else {
                loadSavedCalibration();
                if (isHardwareMode) {
                    setupHardwareMode();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "VR init failed: " + e.getMessage());
            showToast("VR initialization failed: " + e.getMessage());
            finish();
        }
    }

    private void getScreenDimensions() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        eyeWidth = screenWidth / 2;
        eyeHeight = screenHeight;

        // Optimized WebView dimensions
        webViewWidth = eyeWidth;
        webViewHeight = eyeHeight;
    }

    private boolean initializeViews() {
        try {
            hiddenWebView = findViewById(R.id.hidden_webview);
            leftEyeView = findViewById(R.id.left_eye_view);
            rightEyeView = findViewById(R.id.right_eye_view);
            leftCrosshair = findViewById(R.id.left_crosshair);
            rightCrosshair = findViewById(R.id.right_crosshair);
            menuTrigger = findViewById(R.id.menu_trigger);
            vrMenu = findViewById(R.id.vr_menu);
            debugOverlay = findViewById(R.id.debug_overlay);

            if (hiddenWebView == null || leftEyeView == null || rightEyeView == null) {
                Log.e(TAG, "Critical views not found in layout");
                return false;
            }

            // Initialize matrices and bitmaps with safety checks
            leftMatrix = new Matrix();
            rightMatrix = new Matrix();

            if (eyeWidth > 0 && eyeHeight > 0) {
                leftDistortedBitmap = Bitmap.createBitmap(eyeWidth, eyeHeight, Bitmap.Config.RGB_565);
                rightDistortedBitmap = Bitmap.createBitmap(eyeWidth, eyeHeight, Bitmap.Config.RGB_565);
                leftCanvas = new Canvas(leftDistortedBitmap);
                rightCanvas = new Canvas(rightDistortedBitmap);
            }

            if (leftCrosshair != null && rightCrosshair != null) {
                updateCrosshairPositions();
            }

            // Hide instructions after 3 seconds
            if (uiHandler != null) {
                uiHandler.postDelayed(() -> {
                    View instructions = findViewById(R.id.instructions_text);
                    if (instructions != null) {
                        instructions.animate().alpha(0f).setDuration(500).withEndAction(()
                                -> instructions.setVisibility(View.GONE)).start();
                    }
                }, 3000);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            return false;
        }
    }

    private void loadSettings() {
        try {
            captureFps = vrSettings.getVrFps();
            sensitivity = vrSettings.getGyroSensitivity();
            smoothing = vrSettings.getSmoothMovement();
            crosshairSize = vrSettings.getCrosshairSize();
            clickDelay = vrSettings.getClickDelay();
            maxHeadYaw = vrSettings.getVrYawLimit();
            maxHeadPitch = vrSettings.getVrPitchLimit();
            vrMovementScale = vrSettings.getVrMovementScale();
        } catch (Exception e) {
            Log.e(TAG, "Settings load error: " + e.getMessage());
        }
    }

    private void initializeProVR() {
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            uiHandler = new Handler(Looper.getMainLooper());
            crosshairHandler = new Handler(Looper.getMainLooper());

            crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            crosshairPaint.setColor(vrSettings.getCrosshairColor());
            crosshairPaint.setStrokeWidth(2f * crosshairSize);
            crosshairPaint.setStyle(Paint.Style.STROKE);

            uiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            uiPaint.setColor(Color.WHITE);
            uiPaint.setTextSize(20f);
            uiPaint.setTextAlign(Paint.Align.CENTER);
        } catch (Exception e) {
            Log.e(TAG, "ProVR init error: " + e.getMessage());
        }
    }

    private void setupWebView() {
        try {
            if (webViewWidth > 0 && webViewHeight > 0) {
                hiddenWebView.getLayoutParams().width = webViewWidth;
                hiddenWebView.getLayoutParams().height = webViewHeight;
            }

            hiddenWebView.getSettings().setJavaScriptEnabled(true);
            hiddenWebView.getSettings().setDomStorageEnabled(true);
            hiddenWebView.getSettings().setLoadWithOverviewMode(true);
            hiddenWebView.getSettings().setUseWideViewPort(true);
            hiddenWebView.getSettings().setBuiltInZoomControls(false);
            hiddenWebView.getSettings().setDisplayZoomControls(false);
            hiddenWebView.getSettings().setTextZoom(110);

            // Enable hardware mode support
            if (isHardwareMode) {
                hiddenWebView.setVisibility(View.VISIBLE);
                hiddenWebView.setFocusable(true);
                hiddenWebView.setFocusableInTouchMode(true);
            }

            hiddenWebView.getSettings().setUserAgentString(
                    "Mozilla/5.0 (VR; Android 12; The Star Dust) AppleWebKit/537.36 Chrome/120.0.0.0 VR Safari/537.36"
            );

            hiddenWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    injectVROptimizations();
                    startCapture();
                }
            });

            hiddenWebView.loadUrl(websiteUrl);
        } catch (Exception e) {
            Log.e(TAG, "WebView setup error: " + e.getMessage());
        }
    }

    private void injectVROptimizations() {
        try {
            String vrCSS = "javascript:(function() {"
                    + "var style = document.createElement('style');"
                    + "style.textContent = '"
                    + "body { font-size: 16px !important; line-height: 1.5 !important; }"
                    + "a, button { min-height: 40px !important; padding: 10px !important; }"
                    + "input, select { font-size: 16px !important; padding: 10px !important; }"
                    + "';"
                    + "document.head.appendChild(style);"
                    + "})()";
            hiddenWebView.evaluateJavascript(vrCSS, null);
        } catch (Exception e) {
            Log.e(TAG, "VR optimization error: " + e.getMessage());
        }
    }

    private void setupCapture() {
        try {
            captureHandler = new Handler(Looper.getMainLooper());
            backgroundExecutor = Executors.newSingleThreadExecutor();
            if (webViewWidth > 0 && webViewHeight > 0) {
                captureBitmap = Bitmap.createBitmap(webViewWidth, webViewHeight, Bitmap.Config.RGB_565);
                captureCanvas = new Canvas(captureBitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Capture setup error: " + e.getMessage());
        }
    }

    private void setupSensors() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                }
                if (magnetometer != null) {
                    sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
                }
                if (gyroSensor != null) {
                    sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Sensor setup error: " + e.getMessage());
        }
    }

    private void setupGestures() {
        try {
            gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    performHapticFeedback();
                    toggleUI();
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (isCalibrationTutorial) {
                        captureCalibrationPoint();
                    } else {
                        performClick(e.getX(), e.getY());
                    }
                    return true;
                }
            });

            View.OnTouchListener touchListener = (v, event) -> {
                if (gestureDetector != null) {
                    gestureDetector.onTouchEvent(event);
                }
                
                // Direct WebView click using JavaScript
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float webX = (event.getX() / eyeWidth) * hiddenWebView.getWidth();
                    float webY = (event.getY() / eyeHeight) * hiddenWebView.getHeight();
                    
                    String clickScript = String.format(
                        "(function() {"
                        + "  var element = document.elementFromPoint(%f, %f);"
                        + "  if (element) {"
                        + "    var event = new MouseEvent('click', {"
                        + "      view: window,"
                        + "      bubbles: true,"
                        + "      cancelable: true,"
                        + "      clientX: %f,"
                        + "      clientY: %f"
                        + "    });"
                        + "    element.dispatchEvent(event);"
                        + "    if (element.click) element.click();"
                        + "    if (element.href && element.tagName === 'A') {"
                        + "      window.location.href = element.href;"
                        + "    }"
                        + "  }"
                        + "})();", webX, webY, webX, webY);
                    
                    hiddenWebView.evaluateJavascript(clickScript, null);
                    performHapticFeedback();
                }
                return true;
            };

            if (leftEyeView != null) {
                leftEyeView.setOnTouchListener(touchListener);
            }
            if (rightEyeView != null) {
                rightEyeView.setOnTouchListener(touchListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Gesture setup error: " + e.getMessage());
        }
    }

    private void setupMenuSystem() {
        try {
            menuHandler = new Handler(Looper.getMainLooper());

            if (menuTrigger != null) {
                menuTrigger.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startMenuTrigger();
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            cancelMenuTrigger();
                            return true;
                    }
                    return false;
                });
            }

            setupMenuActions();
        } catch (Exception e) {
            Log.e(TAG, "Menu setup error: " + e.getMessage());
        }
    }

    private void startMenuTrigger() {
        if (isMenuTriggering) {
            return;
        }
        isMenuTriggering = true;

        animateMenuProgress();

        menuHandler.postDelayed(() -> {
            if (isMenuTriggering) {
                showMenu();
                performHapticFeedback();
                isMenuTriggering = false;
            }
        }, MENU_TRIGGER_DELAY);
    }

    private void animateMenuProgress() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isMenuTriggering) {
                    menuProgress = Math.min(1.0f, menuProgress + 0.04f);
                    uiHandler.postDelayed(this, 60);
                } else {
                    menuProgress = 0f;
                }
            }
        });
    }

    private void cancelMenuTrigger() {
        isMenuTriggering = false;
        menuProgress = 0f;
        menuHandler.removeCallbacksAndMessages(null);
    }

    private void showMenu() {
        vrMenu.setVisibility(View.VISIBLE);
        vrMenu.setAlpha(0f);
        vrMenu.setScaleX(0.9f);
        vrMenu.setScaleY(0.9f);
        vrMenu.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        isMenuVisible = true;
    }

    private void hideMenu() {
        vrMenu.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(200)
                .withEndAction(() -> {
                    vrMenu.setVisibility(View.GONE);
                    isMenuVisible = false;
                }).start();
    }

    private void updateCrosshairPositions() {
        if (leftCrosshair == null || rightCrosshair == null) {
            return;
        }

        // Enhanced smooth interpolation with adaptive smoothing
        float lerpFactor = 1f - smoothing;
        float distance = Math.abs(targetX - crosshairX) + Math.abs(targetY - crosshairY);
        float adaptiveFactor = Math.min(1.5f, 1f + distance * 2f); // Faster when moving more
        
        crosshairX += (targetX - crosshairX) * lerpFactor * adaptiveFactor;
        crosshairY += (targetY - crosshairY) * lerpFactor * adaptiveFactor;

        float leftX = crosshairX * eyeWidth - leftCrosshair.getWidth() / 2f;
        float rightX = crosshairX * eyeWidth - rightCrosshair.getWidth() / 2f;
        float y = crosshairY * eyeHeight - leftCrosshair.getHeight() / 2f;

        leftCrosshair.setX(Math.max(0, Math.min(leftX, eyeWidth - leftCrosshair.getWidth())));
        leftCrosshair.setY(Math.max(0, Math.min(y, eyeHeight - leftCrosshair.getHeight())));
        rightCrosshair.setX(Math.max(0, Math.min(rightX, eyeWidth - rightCrosshair.getWidth())));
        rightCrosshair.setY(Math.max(0, Math.min(y, eyeHeight - rightCrosshair.getHeight())));

        // Enhanced crosshair feedback
        float movement = Math.abs(targetX - crosshairX) + Math.abs(targetY - crosshairY);
        float alpha = Math.min(1f, Math.max(0.7f, 1f - movement * 3f));

        leftCrosshair.setAlpha(alpha);
        rightCrosshair.setAlpha(alpha);
        leftCrosshair.setScaleX(crosshairSize);
        leftCrosshair.setScaleY(crosshairSize);
        rightCrosshair.setScaleX(crosshairSize);
        rightCrosshair.setScaleY(crosshairSize);

        // Enhanced hover detection for clicking
        checkCrosshairHover();
    }

    private void checkCrosshairHover() {
        // Skip hover detection in hardware mode (uses physical buttons)
        if (isHardwareMode || !isVRCalibrated) {
            return;
        }
        
        float movement = Math.abs(targetX - crosshairX) + Math.abs(targetY - crosshairY);
        boolean isStable = movement < 0.012f;

        if (isStable && !isCrosshairHovering) {
            isCrosshairHovering = true;
            crosshairHoverStart = System.currentTimeMillis();
            startCrosshairClickAnimation();
        } else if (!isStable && isCrosshairHovering) {
            isCrosshairHovering = false;
            crosshairClickProgress = 0f;
            if (crosshairHandler != null) {
                crosshairHandler.removeCallbacksAndMessages(null);
            }
        }
    }

    private void startCrosshairClickAnimation() {
        if (crosshairHandler == null) {
            return;
        }

        crosshairHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isCrosshairHovering) {
                    return;
                }

                long elapsed = System.currentTimeMillis() - crosshairHoverStart;
                long adjustedDelay = (long) (clickDelay * 1000);
                crosshairClickProgress = Math.min(1f, elapsed / (float) adjustedDelay);

                // Visual feedback during hover
                if (crosshairClickProgress > 0.1f) {
                    float scale = 1f + (crosshairClickProgress * 0.3f);
                    if (leftCrosshair != null) {
                        leftCrosshair.setScaleX(scale * crosshairSize);
                        leftCrosshair.setScaleY(scale * crosshairSize);
                    }
                    if (rightCrosshair != null) {
                        rightCrosshair.setScaleX(scale * crosshairSize);
                        rightCrosshair.setScaleY(scale * crosshairSize);
                    }
                }

                if (crosshairClickProgress >= 1f) {
                    // Execute touch click
                    performCrosshairClick();
                    isCrosshairHovering = false;
                    crosshairClickProgress = 0f;

                    // Reset crosshair scale
                    if (leftCrosshair != null) {
                        leftCrosshair.setScaleX(crosshairSize);
                        leftCrosshair.setScaleY(crosshairSize);
                    }
                    if (rightCrosshair != null) {
                        rightCrosshair.setScaleX(crosshairSize);
                        rightCrosshair.setScaleY(crosshairSize);
                    }
                } else {
                    crosshairHandler.postDelayed(this, 50);
                }
            }
        });
    }

    private void performCrosshairClick() {
        try {
            runOnUiThread(() -> {
                float webX = crosshairX * hiddenWebView.getWidth();
                float webY = crosshairY * hiddenWebView.getHeight();

                String clickScript = String.format(
                        "(function() {"
                        + "  var x = %f;"
                        + "  var y = %f;"
                        + "  var element = document.elementFromPoint(x, y);"
                        + "  if (element) {"
                        + "    var event = new MouseEvent('click', {"
                        + "      view: window,"
                        + "      bubbles: true,"
                        + "      cancelable: true,"
                        + "      clientX: x,"
                        + "      clientY: y"
                        + "    });"
                        + "    element.dispatchEvent(event);"
                        + "    if (element.click) element.click();"
                        + "    if (element.href && element.tagName === 'A') {"
                        + "      window.location.href = element.href;"
                        + "    }"
                        + "    return 'clicked';"
                        + "  }"
                        + "  return 'none';"
                        + "})();", webX, webY);

                hiddenWebView.evaluateJavascript(clickScript, null);
                performHapticFeedback();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in crosshair click", e);
        }
    }

    private void startCapture() {
        if (isCapturing) {
            return;
        }
        isCapturing = true;

        Runnable captureRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCapturing) {
                    return;
                }
                captureAndRender();
                captureHandler.postDelayed(this, 1000 / captureFps);
            }
        };
        captureHandler.post(captureRunnable);
    }

    private void captureAndRender() {
        if (hiddenWebView == null || captureBitmap == null) {
            return;
        }

        try {
            hiddenWebView.draw(captureCanvas);

            backgroundExecutor.execute(() -> {
                try {
                    renderVRFrames();
                    runOnUiThread(() -> {
                        leftEyeView.setImageBitmap(leftDistortedBitmap);
                        rightEyeView.setImageBitmap(rightDistortedBitmap);
                        updateCrosshairPositions();
                        updateDebugInfo();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Render error", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Capture error", e);
        }
    }

    private void renderVRFrames() {
        // Calculate viewport with crosshair centering and zoom
        float viewportX = (crosshairX - 0.5f) * webViewWidth * 0.6f;
        float viewportY = (crosshairY - 0.5f) * webViewHeight * 0.6f;

        // Render both eyes identically (no stereoscopic offset for simplicity)
        renderEye(leftCanvas, leftDistortedBitmap, viewportX, viewportY, zoom);
        renderEye(rightCanvas, rightDistortedBitmap, viewportX, viewportY, zoom);
    }

    private void renderEye(Canvas canvas, Bitmap bitmap, float offsetX, float offsetY, float scale) {
        canvas.drawColor(Color.BLACK);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-offsetX, -offsetY);
        matrix.postScale(scale, scale);

        // Center in eye view
        float centerX = (eyeWidth - webViewWidth * scale) / 2f;
        float centerY = (eyeHeight - webViewHeight * scale) / 2f;
        matrix.postTranslate(centerX, centerY);

        canvas.drawBitmap(captureBitmap, matrix, null);

        // Draw crosshair click progress if hovering
        if (isCrosshairHovering && crosshairClickProgress > 0) {
            drawCrosshairProgress(canvas);
        }
    }

    private void drawCrosshairProgress(Canvas canvas) {
        float centerX = crosshairX * eyeWidth;
        float centerY = crosshairY * eyeHeight;

        Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.WHITE);
        progressPaint.setAlpha((int) (200 * crosshairClickProgress));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(4f * crosshairSize);

        float radius = 25f * crosshairSize;
        float sweepAngle = 360f * crosshairClickProgress;

        RectF oval = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null) {
            return;
        }

        try {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    if (gyroValues == null) {
                        gyroValues = new float[3];
                    }
                    System.arraycopy(event.values, 0, gyroValues, 0, Math.min(3, event.values.length));
                    if (isVRCalibrated || isCalibrationTutorial) {
                        processGyroMovement();
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    if (accelValues == null) {
                        accelValues = new float[3];
                    }
                    System.arraycopy(event.values, 0, accelValues, 0, Math.min(3, event.values.length));
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (magnetValues == null) {
                        magnetValues = new float[3];
                    }
                    System.arraycopy(event.values, 0, magnetValues, 0, Math.min(3, event.values.length));
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSensorChanged", e);
        }
    }

    private void processGyroMovement() {
        // Skip gyro movement in hardware mode (uses ESP32 controller)
        if (isHardwareMode || (!isVRCalibrated && !isCalibrationTutorial)) {
            return;
        }

        try {
            if (gyroValues != null && calibrationOffset != null) {
                float rollRate = gyroValues[0] - calibrationOffset[0];
                float pitchRate = gyroValues[1] - calibrationOffset[1];

                // Increased dead zone to prevent drift
                if (Math.abs(rollRate) < 0.002f) rollRate = 0;
                if (Math.abs(pitchRate) < 0.002f) pitchRate = 0;

                // Apply drift compensation
                rollRate *= 0.98f;
                pitchRate *= 0.98f;

                float deltaX = -rollRate * sensitivity * 0.08f;
                float deltaY = pitchRate * sensitivity * 0.08f;

                // Limit movement speed
                float maxDelta = 0.02f;
                deltaX = Math.max(-maxDelta, Math.min(maxDelta, deltaX));
                deltaY = Math.max(-maxDelta, Math.min(maxDelta, deltaY));

                if (Math.abs(deltaX) > 0.001f || Math.abs(deltaY) > 0.001f) {
                    targetX += deltaX;
                    targetY += deltaY;
                    targetX = Math.max(0.1f, Math.min(0.9f, targetX));
                    targetY = Math.max(0.1f, Math.min(0.9f, targetY));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in processGyroMovement", e);
        }
    }

    private float normalizeAngle(float angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    private void recenterCrosshair() {
        try {
            // Reset calibration with current gyro position
            if (gyroValues != null) {
                if (calibrationOffset == null) calibrationOffset = new float[3];
                System.arraycopy(gyroValues, 0, calibrationOffset, 0, 3);
            }
            
            // Force center position
            targetX = 0.5f;
            targetY = 0.5f;
            crosshairX = 0.5f;
            crosshairY = 0.5f;
            
            showToast("Crosshair recentered");
        } catch (Exception e) {
            Log.e(TAG, "Error recentering crosshair", e);
        }
    }

    private void calibrateVR() {
        try {
            if (gyroValues == null) gyroValues = new float[3];
            if (calibrationOffset == null) calibrationOffset = new float[3];
            if (accelValues == null) accelValues = new float[3];
            if (magnetValues == null) magnetValues = new float[3];
            if (baseOrientation == null) baseOrientation = new float[3];

            uiHandler.postDelayed(() -> {
                if (gyroValues != null && calibrationOffset != null) {
                    System.arraycopy(gyroValues, 0, calibrationOffset, 0, 3);
                }

                if (accelValues != null && magnetValues != null) {
                    float[] remappedMatrix = new float[9];
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magnetValues)) {
                        SensorManager.remapCoordinateSystem(rotationMatrix,
                                SensorManager.AXIS_Z, SensorManager.AXIS_X, remappedMatrix);
                        SensorManager.getOrientation(remappedMatrix, baseOrientation);
                    }
                }

                targetX = crosshairX = 0.5f;
                targetY = crosshairY = 0.5f;
                isVRCalibrated = true;
                showToast("VR Calibrated");
            }, 500);

        } catch (Exception e) {
            Log.e(TAG, "Error in calibrateVR", e);
            isVRCalibrated = true;
        }
    }

    private void showCalibrationInstructions() {
        uiHandler.postDelayed(() -> {
            showToast("Starting VR calibration tutorial...");
            uiHandler.postDelayed(this::startCalibrationTutorial, 1000);
        }, 1000);
    }

    private void startCalibrationTutorial() {
        isCalibrationTutorial = true;
        calibrationStep = 0;
        
        // Initialize calibration offset for movement during tutorial
        if (calibrationOffset == null) calibrationOffset = new float[3];
        
        // Wait for gyro values then set current position as baseline
        uiHandler.postDelayed(() -> {
            if (gyroValues != null) {
                System.arraycopy(gyroValues, 0, calibrationOffset, 0, 3);
            }
        }, 500);
        
        showNextCalibrationStep();
    }

    private void showNextCalibrationStep() {
        if (calibrationStep >= calibrationInstructions.length) {
            completeCalibrationTutorial();
            return;
        }

        // Show instruction with visual feedback
        showCalibrationUI();

        // Start countdown
        calibrationCountdown = 5;
        startCalibrationCountdown();
    }

    private void showCalibrationUI() {
        if (calibrationOverlay == null) {
            // Create blur background
            android.widget.FrameLayout blurBg = new android.widget.FrameLayout(this);
            blurBg.setBackgroundColor(0xDD000000);
            
            // Create popup container
            android.widget.LinearLayout popup = new android.widget.LinearLayout(this);
            popup.setOrientation(android.widget.LinearLayout.VERTICAL);
            popup.setPadding(50, 30, 50, 30);
            popup.setGravity(android.view.Gravity.CENTER);
            
            // Add rounded corners
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(15f);
            shape.setColor(0xEE1A1A1A);
            shape.setStroke(2, 0xFF444444);
            popup.setBackground(shape);
            
            calibrationOverlay = new TextView(this);
            calibrationOverlay.setTextColor(0xFFFFFFFF);
            calibrationOverlay.setTextSize(16f);
            calibrationOverlay.setGravity(android.view.Gravity.CENTER);
            calibrationOverlay.setLineSpacing(6f, 1.1f);
            
            popup.addView(calibrationOverlay);
            
            android.widget.FrameLayout.LayoutParams popupParams = new android.widget.FrameLayout.LayoutParams(
                (int)(screenWidth * 0.7f), android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
            popupParams.gravity = android.view.Gravity.CENTER;
            
            blurBg.addView(popup, popupParams);
            
            android.widget.FrameLayout.LayoutParams bgParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
            
            ((android.widget.FrameLayout)findViewById(android.R.id.content)).addView(blurBg, bgParams);
        }
        
        String[] arrows = {"CENTER", "UP", "DOWN", "LEFT", "RIGHT"};
        String instruction = calibrationInstructions[calibrationStep];
        String tip = calibrationTips[calibrationStep];
        String progress = String.format("Step %d of %d", calibrationStep + 1, calibrationInstructions.length);
        
        calibrationOverlay.setText(String.format("%s\n\n%s\n\n%s\n\nGet ready...", 
            progress, instruction, tip));
        ((View)calibrationOverlay.getParent()).setVisibility(View.VISIBLE);
    }

    private void startCalibrationCountdown() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isCalibrationTutorial || calibrationStep >= calibrationInstructions.length) {
                    return;
                }

                if (calibrationCountdown > 0) {
                    String instruction = calibrationInstructions[calibrationStep];
                    String tip = calibrationTips[calibrationStep];
                    String progress = String.format("Step %d of %d", calibrationStep + 1, calibrationInstructions.length);

                    String[] arrows = {"CENTER", "UP", "DOWN", "LEFT", "RIGHT"};
                    
                    if (calibrationCountdown <= 3) {
                        calibrationOverlay.setText(String.format("%s\n\n%s %s\n\n%s\n\nCapturing in %d...",
                                progress, arrows[calibrationStep], instruction, tip, calibrationCountdown));
                        performHapticFeedback();
                    } else {
                        calibrationOverlay.setText(String.format("%s\n\n%s %s\n\n%s\n\nGet ready... %d",
                                progress, arrows[calibrationStep], instruction, tip, calibrationCountdown));
                    }

                    calibrationCountdown--;
                    uiHandler.postDelayed(this, 1000);
                } else {
                    captureCalibrationPoint();
                }
            }
        });
    }

    private void captureCalibrationPoint() {
        if (gyroValues != null && calibrationStep < calibrationPoints.length) {
            System.arraycopy(gyroValues, 0, calibrationPoints[calibrationStep], 0, 3);

            // Show capture feedback
            calibrationOverlay.setText(String.format("Position %d captured!\n\nProcessing...", calibrationStep + 1));
            performHapticFeedback();

            // If this is the center position (step 0), immediately set as calibration offset
            if (calibrationStep == 0) {
                System.arraycopy(gyroValues, 0, calibrationOffset, 0, 3);
                isVRCalibrated = true; // Enable movement immediately after center capture
            }

            calibrationStep++;

            if (calibrationStep < calibrationInstructions.length) {
                uiHandler.postDelayed(this::showNextCalibrationStep, 1500);
            } else {
                uiHandler.postDelayed(this::completeCalibrationTutorial, 1000);
            }
        }
    }

    private void completeCalibrationTutorial() {
        isCalibrationTutorial = false;

        // Validate calibration data
        boolean calibrationValid = true;
        for (int i = 0; i < calibrationPoints.length; i++) {
            if (calibrationPoints[i] == null) {
                calibrationValid = false;
                break;
            }
        }

        boolean calibrationMode = getIntent().getBooleanExtra("calibration_mode", false);

        if (calibrationValid) {
            // Set center as calibration offset
            System.arraycopy(calibrationPoints[0], 0, calibrationOffset, 0, 3);
            
            // Save calibration data
            saveCalibrationData();
            
            // IMMEDIATELY enable VR tracking
            isVRCalibrated = true;
            
            // Reset crosshair to center
            targetX = crosshairX = 0.5f;
            targetY = crosshairY = 0.5f;

            if (calibrationMode) {
                // Show success message and return to home
                calibrationOverlay.setText("CALIBRATION COMPLETE!\n\nMovement mapping optimized\nCalibration saved\nReady for VR experience\n\nReturning to home...");
                
                uiHandler.postDelayed(() -> {
                    finish(); // Return to main activity
                }, 3000);
            } else {
                // Hide overlay and continue VR
                calibrationOverlay.setText("CALIBRATION COMPLETE!\n\nMovement mapping optimized\nReady for VR experience\n\nMove your head to test!");
                
                uiHandler.postDelayed(() -> {
                    if (calibrationOverlay != null && calibrationOverlay.getParent() != null) {
                        ((View)calibrationOverlay.getParent()).setVisibility(View.GONE);
                    }
                }, 3000);
            }
        } else {
            // Fallback calibration
            calibrateVR();
            calibrationOverlay.setText("Using basic calibration\n\nVR Ready!");

            uiHandler.postDelayed(() -> {
                if (calibrationOverlay != null && calibrationOverlay.getParent() != null) {
                    ((View)calibrationOverlay.getParent()).setVisibility(View.GONE);
                }
            }, 2000);
        }

        performHapticFeedback();
    }

    private void saveCalibrationData() {
        vrSettings.saveCalibrationData(calibrationPoints);
        vrSettings.setVRCalibrated(true);
    }

    private void loadSavedCalibration() {
        float[][] savedPoints = vrSettings.getCalibrationData();
        if (savedPoints != null && savedPoints.length == 5) {
            calibrationPoints = savedPoints;
            if (calibrationPoints[0] != null) {
                System.arraycopy(calibrationPoints[0], 0, calibrationOffset, 0, 3);
                isVRCalibrated = true;
                targetX = crosshairX = 0.5f;
                targetY = crosshairY = 0.5f;
            }
        }
    }

    private float getDirectionalDelta(int direction) {
        if (calibrationPoints[0] == null || calibrationPoints[direction] == null) {
            return 0;
        }

        float centerValue = calibrationPoints[0][1];
        float directionValue = calibrationPoints[direction][1];
        float currentValue = gyroValues[1];

        float directionRange = Math.abs(directionValue - centerValue);
        float currentOffset = currentValue - centerValue;

        if (directionRange < 0.1f) {
            return 0;
        }

        return Math.max(0, Math.min(1, Math.abs(currentOffset) / directionRange))
                * Math.signum(currentOffset) * Math.signum(directionValue - centerValue);
    }

    private void performHapticFeedback() {
        if (vrSettings.getHapticFeedback() && vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30);
            }
        }
    }

    private void performClick(float x, float y) {
        try {
            float webX = (x / eyeWidth) * hiddenWebView.getWidth();
            float webY = (y / eyeHeight) * hiddenWebView.getHeight();

            String script = String.format(
                    "(function() {"
                    + "  var element = document.elementFromPoint(%f, %f);"
                    + "  if (element) {"
                    + "    var event = new MouseEvent('click', {"
                    + "      view: window,"
                    + "      bubbles: true,"
                    + "      cancelable: true,"
                    + "      clientX: %f,"
                    + "      clientY: %f"
                    + "    });"
                    + "    element.dispatchEvent(event);"
                    + "    if (element.click) element.click();"
                    + "    if (element.href && element.tagName === 'A') {"
                    + "      window.location.href = element.href;"
                    + "    }"
                    + "  }"
                    + "})();", webX, webY, webX, webY);

            hiddenWebView.evaluateJavascript(script, null);
            performHapticFeedback();
        } catch (Exception e) {
            Log.e(TAG, "Error in performClick", e);
        }
    }

    private void toggleUI() {
        showUI = !showUI;
        if (vrSettings.getShowFps() || vrSettings.getShowDebug()) {
            debugOverlay.setVisibility(showUI ? View.VISIBLE : View.GONE);
        } else {
            debugOverlay.setVisibility(View.GONE);
        }
    }

    private void setupMenuActions() {
        try {
            View zoomIn = findViewById(R.id.menu_zoom_in);
            if (zoomIn != null) {
                zoomIn.setOnClickListener(v -> {
                    zoom = Math.min(2.5f, zoom + 0.25f);
                    performHapticFeedback();
                    hideMenu();
                });
            }

            View zoomOut = findViewById(R.id.menu_zoom_out);
            if (zoomOut != null) {
                zoomOut.setOnClickListener(v -> {
                    zoom = Math.max(0.7f, zoom - 0.25f);
                    performHapticFeedback();
                    hideMenu();
                });
            }

            View reset = findViewById(R.id.menu_reset);
            if (reset != null) {
                reset.setOnClickListener(v -> {
                    recenterCrosshair();
                    performHapticFeedback();
                    hideMenu();
                });
            }

            View info = findViewById(R.id.menu_info);
            if (info != null) {
                info.setOnClickListener(v -> {
                    showPageInfo();
                    hideMenu();
                });
            }

            View settings = findViewById(R.id.menu_settings);
            if (settings != null) {
                settings.setOnClickListener(v -> {
                    refreshSettings();
                    performHapticFeedback();
                    showToast("Settings refreshed");
                    hideMenu();
                });
            }

            View exit = findViewById(R.id.menu_exit);
            if (exit != null) {
                exit.setOnClickListener(v -> {
                    performHapticFeedback();
                    finish();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Menu actions error: " + e.getMessage());
        }
    }

    private void refreshSettings() {
        try {
            loadSettings();
            if (crosshairPaint != null) {
                crosshairPaint.setStrokeWidth(2f * crosshairSize);
                crosshairPaint.setColor(vrSettings.getCrosshairColor());
            }
        } catch (Exception e) {
            Log.e(TAG, "Settings refresh error: " + e.getMessage());
        }
    }

    private void showPageInfo() {
        String vrStatus = isVRCalibrated ? "Calibrated" : "Not Calibrated";
        hiddenWebView.evaluateJavascript(
                "alert('The Star Dust VR\\n\\nPage: ' + document.title + '\\nURL: ' + window.location.href + '\\nZoom: " + String.format("%.1f", zoom) + "x\\nFPS: " + captureFps + "\\nVR Status: " + vrStatus + "\\nHead Tracking: Active');",
                null);
    }

    private void setupHardwareMode() {
        targetX = crosshairX = 0.5f;
        targetY = crosshairY = 0.5f;
        isVRCalibrated = true;
        
        showToast("Hardware Controller Mode - Use ESP32 controller for navigation");
        
        // Hide crosshair and make WebView interactive
        if (leftCrosshair != null) leftCrosshair.setVisibility(View.GONE);
        if (rightCrosshair != null) rightCrosshair.setVisibility(View.GONE);
        
        // Make WebView visible and focusable for hardware input
        if (hiddenWebView != null) {
            hiddenWebView.setVisibility(View.VISIBLE);
            hiddenWebView.setAlpha(0f); // Invisible but interactive
            hiddenWebView.bringToFront();
            hiddenWebView.requestFocus();
        }
    }
    
    private void showToast(String message) {
        runOnUiThread(() -> android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes
    }

    private void updateDebugInfo() {
        if (!vrSettings.getShowFps() && !vrSettings.getShowDebug()) {
            debugOverlay.setVisibility(View.GONE);
            return;
        }

        frameCount++;
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFrameTime >= 1000) {
            float actualFps = frameCount * 1000.0f / (currentTime - lastFrameTime);

            StringBuilder debugText = new StringBuilder();
            if (vrSettings.getShowFps()) {
                debugText.append(String.format("FPS: %.1f", actualFps));
            }
            if (vrSettings.getShowDebug()) {
                if (debugText.length() > 0) {
                    debugText.append(" | ");
                }
                debugText.append(String.format("Zoom: %.1fx | Click: %.1f | Sens: %.1f",
                        zoom, crosshairClickProgress, sensitivity));
            }

            debugOverlay.setText(debugText.toString());
            frameCount = 0;
            lastFrameTime = currentTime;
        }
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onBackPressed() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isCapturing = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hiddenWebView != null && captureBitmap != null) {
            startCapture();
        }
        setupSensors();
        enterImmersiveMode();
    }

    @Override
    protected void onDestroy() {
        isCapturing = false;

        try {
            if (captureHandler != null) {
                captureHandler.removeCallbacksAndMessages(null);
            }
            if (menuHandler != null) {
                menuHandler.removeCallbacksAndMessages(null);
            }
            if (uiHandler != null) {
                uiHandler.removeCallbacksAndMessages(null);
            }
            if (crosshairHandler != null) {
                crosshairHandler.removeCallbacksAndMessages(null);
            }
            if (backgroundExecutor != null) {
                backgroundExecutor.shutdown();
            }
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
            if (hiddenWebView != null) {
                hiddenWebView.destroy();
            }

            // Clean up bitmaps safely
            if (captureBitmap != null && !captureBitmap.isRecycled()) {
                captureBitmap.recycle();
            }
            if (leftDistortedBitmap != null && !leftDistortedBitmap.isRecycled()) {
                leftDistortedBitmap.recycle();
            }
            if (rightDistortedBitmap != null && !rightDistortedBitmap.isRecycled()) {
                rightDistortedBitmap.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error: " + e.getMessage());
        }

        super.onDestroy();
    }
}
