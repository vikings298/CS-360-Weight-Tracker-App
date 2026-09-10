package com.example.mcnaneyprojectone;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class Main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the app on the main home page
        setContentView(R.layout.home_page);
    }
}