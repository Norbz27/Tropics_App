package com.example.tropics_app;

import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import android.view.Menu;
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
import com.google.firebase.auth.FirebaseUser;


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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navview);
        navigationView.setNavigationItemSelectedListener(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Menu menu = navigationView.getMenu();
            MenuItem accountsMenuItem = menu.findItem(R.id.nav_accounts);

            if (!userId.equals("WmYSRkbNXBWQgFmU9ll33vW0vfm2")) {
                accountsMenuItem.setVisible(false);
            }
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
            getSupportActionBar().setTitle("Home");
        }
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
        else if (id == R.id.nav_accounts) {
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new AccountsFragment()).commit();
            getSupportActionBar().setTitle("Accounts");
        }
        else if (id == R.id.nav_sign_out) {
            mAuth.signOut();
            /*SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, false);
            editor.apply();*/
            Intent intent = new Intent(this, SignInActivity.class); // Create intent for SignInActivity
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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