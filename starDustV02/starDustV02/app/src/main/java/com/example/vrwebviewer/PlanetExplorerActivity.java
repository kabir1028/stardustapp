// PLANET EXPLORER - Display planet data and images
package com.example.vrwebviewer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class PlanetExplorerActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planet_explorer);
        
        String planet = getIntent().getStringExtra("planet");
        setupPlanetData(planet);
        
        CardView vrButton = findViewById(R.id.vr_explore_button);
        vrButton.setOnClickListener(v -> {
            // Show VR mode selection for planets too
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select VR Mode")
                .setMessage("Choose your VR control method:")
                .setPositiveButton("Standard VR", (d, w) -> {
                    Intent intent = new Intent(this, VrActivity.class);
                    intent.putExtra("destination", planet);
                    intent.putExtra("vr_mode", "standard");
                    startActivity(intent);
                })
                .setNegativeButton("Hardware Controller", (d, w) -> {
                    Intent intent = new Intent(this, VrActivity.class);
                    intent.putExtra("destination", planet);
                    intent.putExtra("vr_mode", "hardware");
                    startActivity(intent);
                })
                .show();
        });
    }
    
    private void setupPlanetData(String planet) {
        TextView titleText = findViewById(R.id.planet_title);
        TextView descText = findViewById(R.id.planet_description);
        TextView factsText = findViewById(R.id.planet_facts);
        ImageView planetImage = findViewById(R.id.planet_image);
        
        switch (planet) {
            case "mars":
                titleText.setText("Mars - The Red Planet");
                descText.setText("Mars is the fourth planet from the Sun and the second-smallest planet in the Solar System. Known as the Red Planet due to iron oxide on its surface.");
                factsText.setText("• Distance from Sun: 227.9 million km\n• Day length: 24h 37m\n• Year length: 687 Earth days\n• Moons: Phobos, Deimos\n• Largest volcano: Olympus Mons\n• Temperature: -80°C to 20°C");
                break;
                
            case "moon":
                titleText.setText("Moon - Earth's Satellite");
                descText.setText("The Moon is Earth's only natural satellite. It influences Earth's tides and has been a subject of human fascination for millennia.");
                factsText.setText("• Distance from Earth: 384,400 km\n• Diameter: 3,474 km\n• Gravity: 1/6th of Earth's\n• Age: 4.5 billion years\n• Phases: New, Crescent, Quarter, Gibbous, Full\n• Temperature: -173°C to 127°C");
                break;
                
            case "venus":
                titleText.setText("Venus - The Morning Star");
                descText.setText("Venus is the second planet from the Sun and the hottest planet in our solar system. Often called Earth's twin due to similar size.");
                factsText.setText("• Distance from Sun: 108.2 million km\n• Day length: 243 Earth days\n• Year length: 225 Earth days\n• Atmosphere: 96% CO2\n• Surface pressure: 92x Earth's\n• Temperature: 462°C (hottest planet)");
                break;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}