// SPACE CONTENT VIEWER - Display space exploration content
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
    private Button backButton, forwardButton, reloadButton, vrButton;
    private ProgressBar progressBar;
    private String currentUrl;
    private String mode;
    private String planet;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mode = getIntent().getStringExtra("mode");
        planet = getIntent().getStringExtra("planet");
        String title = getIntent().getStringExtra("title");
        
        if ("planet".equals(mode) && planet != null) {
            // Launch planet explorer instead
            Intent intent = new Intent(this, PlanetExplorerActivity.class);
            intent.putExtra("planet", planet);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_website);
        
        currentUrl = getContentUrl();
        
        initializeViews();
        setupWebView();
        setupToolbar();
        
        if (title != null) {
            getSupportActionBar().setTitle(title);
        }
        
        webView.loadUrl(currentUrl);
    }
    
    private String getContentUrl() {
        switch (mode) {
            case "galaxy":
                return "https://www.nasa.gov/universe/galaxies/";
            case "constellations":
                return "https://www.constellation-guide.com/";
            case "research":
                return "https://www.nasa.gov/news/";
            default:
                return getIntent().getStringExtra("url") != null ? 
                       getIntent().getStringExtra("url") : "https://trek.nasa.gov/moon/";
        }
    }
    
    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
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
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                urlDisplay.setText(url);
                progressBar.setVisibility(View.VISIBLE);
                updateNavigationButtons();
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                updateNavigationButtons();
                currentUrl = url;
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WebsiteActivity.this, "Failed to load: " + description, Toast.LENGTH_LONG).show();
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                getSupportActionBar().setTitle(title);
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    private void setupToolbar() {
        backButton.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        forwardButton.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        reloadButton.setOnClickListener(v -> webView.reload());
        vrButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, VrActivity.class);
            if ("galaxy".equals(mode)) {
                intent.putExtra("destination", "galaxy");
            } else if ("constellations".equals(mode)) {
                intent.putExtra("destination", "constellations");
            } else {
                intent.putExtra("url", currentUrl);
            }
            startActivity(intent);
        });
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
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentUrl);
            startActivity(Intent.createChooser(shareIntent, "Share URL"));
            return true;
        } else if (id == R.id.action_fullscreen) {
            toggleFullscreen();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void toggleFullscreen() {
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        
        if ((uiOptions & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            getSupportActionBar().hide();
            findViewById(R.id.toolbar_container).setVisibility(View.GONE);
        } else {
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
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}