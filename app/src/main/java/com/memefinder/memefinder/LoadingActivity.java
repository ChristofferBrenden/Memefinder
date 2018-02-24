package com.memefinder.memefinder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * Loading screen activity. This class gets the images to use and sends them to MainActivity
 */
public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        final Context context = this;

        Thread thread = new Thread() {

            @Override
            public void run() {
                Helpers.initApp(context);
                ArrayList<Image> images = Helpers.populateImages();

                // Start main screen
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                // If fetched images from sources, put them in intent
                intent.putExtra("images", images);

                startActivity(intent);

                finish();
            }
        };
        thread.start();
    }
}
