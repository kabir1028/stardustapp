// ENHANCED VR ACTIVITY - Google VR SDK Integration
package com.example.vrwebviewer;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

public class EnhancedVrActivity extends Activity implements VRManager.VRListener {
    
    private FrameLayout vrContainer;
    private WebView leftWebView, rightWebView;
    private VRManager vrManager;
    private VRSettings vrSettings;
    private String destination;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        vrSettings = new VRSettings(this);
        destination = getIntent().getStringExtra("destination");
        
        initVR();
        setupWebViews();
    }
    
    private void initVR() {
        // Create VR container
        vrContainer = new FrameLayout(this);
        setContentView(vrContainer);
        
        // Initialize VR manager
        vrManager = new VRManager(this);
        vrManager.setVRListener(this);
        
        // Set fullscreen and immersive mode
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    
    private void setupWebViews() {
        // Create stereo WebViews for left and right eyes
        leftWebView = new WebView(this);
        rightWebView = new WebView(this);
        
        configureWebView(leftWebView);
        configureWebView(rightWebView);
        
        // Add WebViews to container in side-by-side layout
        FrameLayout.LayoutParams leftParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        leftParams.rightMargin = getResources().getDisplayMetrics().widthPixels / 2;
        
        FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        rightParams.leftMargin = getResources().getDisplayMetrics().widthPixels / 2;
        
        vrContainer.addView(leftWebView, leftParams);
        vrContainer.addView(rightWebView, rightParams);
        
        loadDestination();
    }
    
    private void configureWebView(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectVRControls(view);
            }
        });
    }
    
    private void loadDestination() {
        String url = getDestinationUrl(destination);
        leftWebView.loadUrl(url);
        rightWebView.loadUrl(url);
    }
    
    private String getDestinationUrl(String dest) {
        switch (dest != null ? dest.toLowerCase() : "moon") {
            case "mars": return "https://trek.nasa.gov/mars/";
            case "venus": return "https://trek.nasa.gov/venus/";
            case "galaxy": return "https://www.google.com/sky/";
            case "constellations": return "https://stellarium-web.org/";
            default: return "https://trek.nasa.gov/moon/";
        }
    }
    
    private void injectVRControls(WebView webView) {
        String vrScript = 
            "javascript:(function(){" +
            "var style = document.createElement('style');" +
            "style.innerHTML = 'body{transform-origin:center;transition:transform 0.1s;}';" +
            "document.head.appendChild(style);" +
            "window.vrRotation = {x:0,y:0,z:0};" +
            "window.updateVRView = function(x,y,z){" +
            "document.body.style.transform = 'rotateX('+x+'deg) rotateY('+y+'deg) rotateZ('+z+'deg)';" +
            "};" +
            "})()";
        
        webView.evaluateJavascript(vrScript, null);
    }
    
    @Override
    public void onHeadRotationChanged(float[] headMatrix) {
        // Extract rotation angles from matrix
        float[] angles = extractRotationAngles(headMatrix);
        
        runOnUiThread(() -> {
            updateWebViewRotation(leftWebView, angles);
            updateWebViewRotation(rightWebView, angles);
        });
    }
    
    @Override
    public void onEyeMatrixChanged(float[] leftEye, float[] rightEye) {
        // Apply stereo offset for each eye
        float[] leftAngles = extractRotationAngles(leftEye);
        float[] rightAngles = extractRotationAngles(rightEye);
        
        runOnUiThread(() -> {
            updateWebViewRotation(leftWebView, leftAngles);
            updateWebViewRotation(rightWebView, rightAngles);
        });
    }
    
    private float[] extractRotationAngles(float[] matrix) {
        float[] angles = new float[3];
        
        // Extract Euler angles from rotation matrix
        angles[0] = (float) Math.toDegrees(Math.atan2(matrix[6], matrix[10])); // X (pitch)
        angles[1] = (float) Math.toDegrees(Math.asin(-matrix[2])); // Y (yaw)
        angles[2] = (float) Math.toDegrees(Math.atan2(matrix[1], matrix[0])); // Z (roll)
        
        return angles;
    }
    
    private void updateWebViewRotation(WebView webView, float[] angles) {
        String script = String.format(
            "javascript:if(window.updateVRView) window.updateVRView(%f,%f,%f);",
            angles[0], angles[1], angles[2]
        );
        webView.evaluateJavascript(script, null);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        vrManager.startTracking();
    }
    
    @Override
    protected void onPause() {
        vrManager.stopTracking();
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        vrManager.stopTracking();
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Exiting VR Mode", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }
}