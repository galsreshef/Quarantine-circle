package com.thegalos.quarantinecircle.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.thegalos.quarantinecircle.R;
import com.thegalos.quarantinecircle.fragments.Splash;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (savedInstanceState != null) {
            Fragment fragment = getSupportFragmentManager().getFragment(savedInstanceState, "Fragment");
            if (fragment != null) {
                if (fragment.getTag() != null)
                    if (fragment.getTag().equals("Main"))
                        getSupportFragmentManager().beginTransaction().replace(R.id.flAppFragment, fragment, "Main").commit();
                    else if (fragment.getTag().equals("Splash"))
                        getSupportFragmentManager().beginTransaction().replace(R.id.flAppFragment, new Splash(), "Splash").commit();

            }

        }
        getSupportFragmentManager().beginTransaction().replace(R.id.flAppFragment, new Splash(), "Splash").commit();

/*
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're in day time
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're at night!
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // We don't know what mode we're in, assume notnight
        }
*/

        // Ad Mob
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("bundle state" , "Activity saved to bundle");
        Fragment fragment;
        if (getSupportFragmentManager().findFragmentByTag("Main") != null)
            fragment = getSupportFragmentManager().findFragmentByTag("Main");
        else
            fragment = getSupportFragmentManager().findFragmentByTag("Splash");

        assert fragment != null;
        getSupportFragmentManager().putFragment(outState,"Fragment", fragment);
    }
}