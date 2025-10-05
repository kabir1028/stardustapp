// COMPLETE VR WEB VIEWER - Enhanced Main Activity
// Features: Modern UI, Settings, URL input, VR mode, BLE support
package com.example.vrwebviewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "VRWebViewerPrefs";
    private static final String PREF_CUSTOM_URL = "custom_url";
    private static final String PREF_VR_FPS = "vr_fps";
    private static final String PREF_BLE_ENABLED = "ble_enabled";

    private SharedPreferences prefs;
    private String currentUrl = "https://trek.nasa.gov/moon/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Enhanced MainActivity created");

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize preferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUrl = prefs.getString(PREF_CUSTOM_URL, currentUrl);

        setupButtons();
    }

    private void setupButtons() {
        Button exploreButton = findViewById(R.id.btn_explore);
        Button vrButton = findViewById(R.id.btn_vr);
        Button settingsButton = findViewById(R.id.btn_settings);
        Button exitButton = findViewById(R.id.btn_exit);

        // Explore button - launches website activity
        exploreButton.setOnClickListener(v -> {
            Log.d(TAG, "Explore button clicked");
            Intent intent = new Intent(this, WebsiteActivity.class);
            intent.putExtra("url", currentUrl);
            startActivity(intent);
        });

        // VR button - launches VR experience
        vrButton.setOnClickListener(v -> {
            Log.d(TAG, "VR button clicked");
            Intent intent = new Intent(this, VrActivity.class);
            intent.putExtra("url", currentUrl);
            startActivity(intent);
        });

        // Settings button
        settingsButton.setOnClickListener(v -> showSettingsDialog());

        // Exit button
        exitButton.setOnClickListener(v -> {
            Log.d(TAG, "Exit button clicked");
            finish();
        });
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        // Create custom layout for settings
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // URL input
        EditText urlInput = new EditText(this);
        urlInput.setHint("Website URL");
        urlInput.setText(currentUrl);
        urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        layout.addView(urlInput);

        // FPS setting
        EditText fpsInput = new EditText(this);
        fpsInput.setHint("VR FPS (6-30)");
        fpsInput.setText(String.valueOf(prefs.getInt(PREF_VR_FPS, 12)));
        fpsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(fpsInput);

        // BLE toggle
        Switch bleSwitch = new Switch(this);
        bleSwitch.setText("Enable BLE Gyro Control");
        bleSwitch.setChecked(prefs.getBoolean(PREF_BLE_ENABLED, false));
        layout.addView(bleSwitch);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUrl = urlInput.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                if (!newUrl.startsWith("http")) {
                    newUrl = "https://" + newUrl;
                }
                currentUrl = newUrl;
                prefs.edit().putString(PREF_CUSTOM_URL, newUrl).apply();
            }

            try {
                int fps = Integer.parseInt(fpsInput.getText().toString());
                if (fps >= 6 && fps <= 30) {
                    prefs.edit().putInt(PREF_VR_FPS, fps).apply();
                }
            } catch (NumberFormatException e) {
                // Keep default
            }

            prefs.edit().putBoolean(PREF_BLE_ENABLED, bleSwitch.isChecked()).apply();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("VR Web Viewer")
                .setMessage("Version 1.0\n\nFeatures:\n• Website exploration\n• VR split-screen mode\n• Touch drag controls\n• BLE gyro support\n• Performance optimization")
                .setPositiveButton("OK", null)
                .show();
    }
}
