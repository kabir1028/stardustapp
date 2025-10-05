// SIMPLE VR ACTIVITY - Crash-proof fallback
package com.example.vrwebviewer;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SimpleVrActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Simple VR layout
            WebView webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            
            String url = getIntent().getStringExtra("url");
            if (url == null) url = "https://trek.nasa.gov/moon/";
            
            webView.loadUrl(url);
            setContentView(webView);
            
            Toast.makeText(this, "Simple VR Mode - Rotate device for VR", Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "VR not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}