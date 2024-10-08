package com.example.tropics_app;

import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import android.view.MenuItem;
import android.view.Window;


import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.navigation.NavigationView;


public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String Preference = "userpreferences";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(Preference, MODE_PRIVATE);
        setContentView(R.layout.activity_navigation);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.darkgray));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.darkgray));
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navview);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toogle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toogle);
        toogle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
            getSupportActionBar().setTitle("Home");
        }
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Reload the fragment when back button is pressed
                reloadFragment(); // Make sure this method is defined in your Activity
            }
        };

        // Attach the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new HomeFragment()).commit();
            getSupportActionBar().setTitle("Home");
        } else if (id == R.id.nav_apt) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new AppointmentFragment()).commit();
            getSupportActionBar().setTitle("Appointment");
        } else if (id == R.id.nav_calendar) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new CalendarFragment()).commit();
            getSupportActionBar().setTitle("Calendar");
        } else if (id == R.id.nav_sales) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new SalesFragment()).commit();
            getSupportActionBar().setTitle("Sales");
        } else if (id == R.id.nav_customer_report) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new CustomerFragment()).commit();
            getSupportActionBar().setTitle("Customer Report");
        } else if (id == R.id.nav_salary) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new SalaryFragment()).commit();
            getSupportActionBar().setTitle("Salary Computation");
        } else if (id == R.id.nav_inventory) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new InventoryFragment()).commit();
            getSupportActionBar().setTitle("Inventory");
        } else if (id == R.id.nav_service) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new ServiceFragment()).commit();
            getSupportActionBar().setTitle("Service");
        }
        else if (id == R.id.nav_sign_out) {
            mAuth.signOut();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, false);
            editor.apply();
            Intent intent = new Intent(this, SignInActivity.class); // Create intent for SignInActivity
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser(){

    }
    private void reloadFragment() {
        // Reload the current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.framelayout);
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .detach(currentFragment) // Detach the current fragment
                    .attach(currentFragment) // Re-attach it to reload
                    .commit();
        }
    }
    @Override
    public void onBackPressed(){
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }else {
            super.onBackPressed();
        }
    }

}