// STAR DUST VR - Space Exploration Platform
package com.example.vrwebviewer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private VRSettings vrSettings;
    private String currentDestination = "moon";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        vrSettings = new VRSettings(this);
        String savedDestination = vrSettings.getCustomUrl();
        currentDestination = (savedDestination != null && !savedDestination.isEmpty()) ? savedDestination : "moon";
        
        setupCardActions();
    }
    
    private void setupCardActions() {
        CardView galaxyCard = findViewById(R.id.card_galaxy);
        CardView vrCard = findViewById(R.id.card_vr);
        CardView constellationsCard = findViewById(R.id.card_constellations);
        CardView researchCard = findViewById(R.id.card_research);

        if (galaxyCard != null) {
            galaxyCard.setOnClickListener(v -> openGalaxyExplorer());
        }
        
        if (vrCard != null) {
            vrCard.setOnClickListener(v -> {
                if (vrSettings.isVRCalibrated()) {
                    showVRModeSelection();
                } else {
                    showCalibrationRequiredDialog();
                }
            });
        }
        
        if (constellationsCard != null) {
            constellationsCard.setOnClickListener(v -> openConstellations());
        }
        
        if (researchCard != null) {
            researchCard.setOnClickListener(v -> openResearchCenter());
        }
        
        // Planet exploration cards
        CardView marsCard = findViewById(R.id.card_mars);
        if (marsCard != null) {
            marsCard.setOnClickListener(v -> openPlanetExplorer("mars"));
        }
        
        CardView moonCard = findViewById(R.id.card_moon);
        if (moonCard != null) {
            moonCard.setOnClickListener(v -> openPlanetExplorer("moon"));
        }
        
        CardView venusCard = findViewById(R.id.card_venus);
        if (venusCard != null) {
            venusCard.setOnClickListener(v -> openPlanetExplorer("venus"));
        }
        
        // VR Tools cards
        CardView calibrationCard = findViewById(R.id.card_calibration);
        if (calibrationCard != null) {
            calibrationCard.setOnClickListener(v -> startVRCalibration());
        }
        
        CardView quickSettingsCard = findViewById(R.id.card_quick_settings);
        if (quickSettingsCard != null) {
            quickSettingsCard.setOnClickListener(v -> showQuickSettingsDialog());
        }
        
        // Space search functionality
        EditText searchBar = findViewById(R.id.search_bar);
        if (searchBar != null) {
            searchBar.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSpaceSearch(searchBar.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }
    
    private void showCalibrationRequiredDialog() {
        new AlertDialog.Builder(this)
            .setTitle("VR Calibration Required")
            .setMessage("Please complete VR calibration first to ensure optimal head tracking experience.")
            .setPositiveButton("Calibrate Now", (d, w) -> startVRCalibration())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void startVRCalibration() {
        Intent intent = new Intent(this, CalibrationActivity.class);
        startActivity(intent);
    }
    
    private void openGalaxyExplorer() {
        Intent intent = new Intent(this, WebsiteActivity.class);
        intent.putExtra("mode", "galaxy");
        intent.putExtra("title", "Galaxy Explorer");
        startActivity(intent);
    }
    
    private void showVRModeSelection() {
        new AlertDialog.Builder(this)
            .setTitle("Select VR Mode")
            .setMessage("Choose your VR control method:")
            .setPositiveButton("Standard VR", (d, w) -> openVRExperience("standard"))
            .setNegativeButton("Hardware Controller", (d, w) -> openVRExperience("hardware"))
            .setCancelable(true)
            .show();
    }
    
    private void openVRExperience() {
        openVRExperience("standard");
    }
    
    private void openVRExperience(String mode) {
        Intent intent = new Intent(this, VrActivity.class);
        intent.putExtra("destination", currentDestination);
        intent.putExtra("vr_mode", mode);
        startActivity(intent);
    }
    
    private void openConstellations() {
        Intent intent = new Intent(this, WebsiteActivity.class);
        intent.putExtra("mode", "constellations");
        intent.putExtra("title", "Star Constellations");
        startActivity(intent);
    }
    
    private void openResearchCenter() {
        Intent intent = new Intent(this, WebsiteActivity.class);
        intent.putExtra("mode", "research");
        intent.putExtra("title", "Research & Development");
        startActivity(intent);
    }
    
    private void openPlanetExplorer(String planet) {
        currentDestination = planet;
        Intent intent = new Intent(this, PlanetExplorerActivity.class);
        intent.putExtra("planet", planet);
        startActivity(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showAdvancedSettingsDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showAdvancedSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Star Dust VR - Settings");
        
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 10);
        
        // Default Destination
        TextView destLabel = new TextView(this);
        destLabel.setText("Default Space Destination:");
        destLabel.setTextSize(14f);
        destLabel.setPadding(0, 10, 0, 5);
        layout.addView(destLabel);
        
        android.widget.Spinner destSpinner = new android.widget.Spinner(this);
        String[] destinations = {"Moon", "Mars", "Venus", "Galaxy", "Constellations"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, destinations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destSpinner.setAdapter(adapter);
        layout.addView(destSpinner);
        
        // VR FPS Setting
        TextView fpsLabel = new TextView(this);
        fpsLabel.setText("VR Frame Rate: " + vrSettings.getVrFps() + " FPS");
        fpsLabel.setTextSize(14f);
        fpsLabel.setPadding(0, 20, 0, 5);
        layout.addView(fpsLabel);
        
        SeekBar fpsSeekBar = new SeekBar(this);
        fpsSeekBar.setMax(24);
        fpsSeekBar.setProgress(vrSettings.getVrFps() - 6);
        fpsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fpsLabel.setText("VR Frame Rate: " + (progress + 6) + " FPS");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(fpsSeekBar);
        
        // Gyro Sensitivity
        TextView sensitivityLabel = new TextView(this);
        float currentSensitivity = vrSettings.getGyroSensitivity();
        sensitivityLabel.setText("Gyro Sensitivity: " + String.format("%.1f", currentSensitivity));
        sensitivityLabel.setTextSize(14f);
        sensitivityLabel.setPadding(0, 20, 0, 5);
        layout.addView(sensitivityLabel);
        
        SeekBar sensitivitySeekBar = new SeekBar(this);
        sensitivitySeekBar.setMax(40);
        sensitivitySeekBar.setProgress((int)((currentSensitivity - 0.2f) * 10));
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sensitivity = 0.2f + (progress / 10f);
                sensitivityLabel.setText("Gyro Sensitivity: " + String.format("%.1f", sensitivity));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(sensitivitySeekBar);
        
        scrollView.addView(layout);
        builder.setView(scrollView);
        
        builder.setPositiveButton("Save Settings", (dialog, which) -> {
            String selectedDest = destSpinner.getSelectedItem().toString().toLowerCase();
            currentDestination = selectedDest;
            vrSettings.setCustomUrl(selectedDest);
            vrSettings.setVrFps(fpsSeekBar.getProgress() + 6);
            vrSettings.setGyroSensitivity(0.2f + (sensitivitySeekBar.getProgress() / 10f));
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showQuickSettingsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Quick Settings")
            .setMessage("VR FPS: " + vrSettings.getVrFps() + "\nGyro Sensitivity: " + vrSettings.getGyroSensitivity())
            .setPositiveButton("Advanced", (d, w) -> showAdvancedSettingsDialog())
            .setNegativeButton("OK", null)
            .show();
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Star Dust VR")
            .setMessage("Explore the universe in virtual reality\n\n" +
                    "Features:\n" +
                    "• Planet exploration with detailed data\n" +
                    "• Galaxy and constellation viewing\n" +
                    "• VR head tracking with gyroscope\n" +
                    "• Touch and crosshair controls\n" +
                    "• Research and development center\n" +
                    "• Customizable VR settings\n\n" +
                    "Version 2.0\n" +
                    "Built for immersive space exploration")
            .setPositiveButton("Settings", (d, w) -> showAdvancedSettingsDialog())
            .setNegativeButton("Close", null)
            .show();
    }
    
    private void performSpaceSearch(String query) {
        String lowerQuery = query.toLowerCase().trim();
        
        if (lowerQuery.contains("mars")) {
            openPlanetExplorer("mars");
            Toast.makeText(this, "Exploring Mars", Toast.LENGTH_SHORT).show();
        } else if (lowerQuery.contains("moon")) {
            openPlanetExplorer("moon");
            Toast.makeText(this, "Exploring Moon", Toast.LENGTH_SHORT).show();
        } else if (lowerQuery.contains("venus")) {
            openPlanetExplorer("venus");
            Toast.makeText(this, "Exploring Venus", Toast.LENGTH_SHORT).show();
        } else if (lowerQuery.contains("galaxy") || lowerQuery.contains("milky way")) {
            openGalaxyExplorer();
            Toast.makeText(this, "Exploring Galaxy", Toast.LENGTH_SHORT).show();
        } else if (lowerQuery.contains("constellation") || lowerQuery.contains("star")) {
            openConstellations();
            Toast.makeText(this, "Viewing Constellations", Toast.LENGTH_SHORT).show();
        } else {
            showSearchSuggestions(query);
        }
    }
    
    private void showSearchSuggestions(String query) {
        new AlertDialog.Builder(this)
            .setTitle("Space Search")
            .setMessage("Search suggestions:\n\n" +
                    "Mars - Red Planet\n" +
                    "Moon - Earth's Satellite\n" +
                    "Venus - Morning Star\n" +
                    "Galaxy - Milky Way\n" +
                    "Constellations - Star Patterns")
            .setPositiveButton("OK", null)
            .show();
    }
}