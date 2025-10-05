// ENHANCED WEBSITE ACTIVITY - Full-featured web browser
// Features: Enhanced navigation, bookmarks, fullscreen, sharing
package com.example.vrwebviewer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class WebsiteActivity extends AppCompatActivity {
    private static final String TAG = "WebsiteActivity";
    
    private WebView webView;
    private TextView urlDisplay;
    private Button backButton;
    private Button forwardButton;
    private Button reloadButton;
    private Button vrButton;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private String currentUrl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_website);
        
        // Get URL from intent
        currentUrl = getIntent().getStringExtra("url");
        if (currentUrl == null) {
            currentUrl = "https://trek.nasa.gov/moon/";
        }
        
        Log.d(TAG, "WebsiteActivity created, loading: " + currentUrl);
        
        initializeViews();
        setupWebView();
        setupToolbar();
        
        // Load the website
        webView.loadUrl(currentUrl);
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        webView = findViewById(R.id.webview);
        urlDisplay = findViewById(R.id.url_display);
        backButton = findViewById(R.id.btn_back);
        forwardButton = findViewById(R.id.btn_forward);
        reloadButton = findViewById(R.id.btn_reload);
        vrButton = findViewById(R.id.btn_vr);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void setupWebView() {
        // Enable JavaScript and DOM storage
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSupportZoom(true);
        
        // Handle page loading events
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                Log.d(TAG, "Page loading started: " + url);
                urlDisplay.setText(url);
                progressBar.setVisibility(View.VISIBLE);
                updateNavigationButtons();
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page loading finished: " + url);
                progressBar.setVisibility(View.GONE);
                updateNavigationButtons();
                currentUrl = url;
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + description + " (Code: " + errorCode + ")");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WebsiteActivity.this, "Failed to load: " + description, Toast.LENGTH_LONG).show();
            }
        });
        
        // Handle page title and progress updates
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                Log.d(TAG, "Page title: " + title);
                getSupportActionBar().setTitle(title);
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void setupToolbar() {
        backButton.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                Log.d(TAG, "Going back");
                webView.goBack();
            }
        });
        
        forwardButton.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                Log.d(TAG, "Going forward");
                webView.goForward();
            }
        });
        
        reloadButton.setOnClickListener(v -> {
            Log.d(TAG, "Reloading page");
            webView.reload();
        });
        
        vrButton.setOnClickListener(v -> {
            Log.d(TAG, "Launching VR mode");
            Intent intent = new Intent(this, VrActivity.class);
            intent.putExtra("url", currentUrl);
            startActivity(intent);
        });
        
        // Initial button state
        updateNavigationButtons();
    }
    
    private void updateNavigationButtons() {
        backButton.setEnabled(webView.canGoBack());
        forwardButton.setEnabled(webView.canGoForward());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.website_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_share) {
            shareUrl();
            return true;
        } else if (id == R.id.action_fullscreen) {
            toggleFullscreen();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void shareUrl() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, currentUrl);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this website");
        startActivity(Intent.createChooser(shareIntent, "Share URL"));
    }
    
    private void toggleFullscreen() {
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        
        if ((uiOptions & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            // Enter fullscreen
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            getSupportActionBar().hide();
            findViewById(R.id.toolbar_container).setVisibility(View.GONE);
        } else {
            // Exit fullscreen
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getSupportActionBar().show();
            findViewById(R.id.toolbar_container).setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}