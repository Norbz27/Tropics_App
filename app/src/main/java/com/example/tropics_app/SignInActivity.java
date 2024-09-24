package com.example.tropics_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {
    Intent intent;
    private EditText username, password1;
    private Button btnSignin;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String Preference = "userpreferences";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        username = findViewById(R.id.edUsername);
        password1 = findViewById(R.id.edPassword);
        btnSignin = findViewById(R.id.btnSignIn);
        intent = new Intent(this, NavigationActivity.class);

        sharedPreferences = getSharedPreferences(Preference, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if(isLoggedIn){
            startActivity(new Intent(SignInActivity.this, NavigationActivity.class));
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.darkgray));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.darkgray));
        }


        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = username.getText().toString().trim();
                String password = password1.getText().toString().trim();
                signInUser(email, password);

            }
        });
    }

    private void signInUser(String email, String password){

        if(email.isEmpty()){
            username.setError("Enter Username");
            username.requestFocus();
            return;
        }
        if(password.isEmpty()){
            password1.setError("Enter Password");
            password1.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(KEY_IS_LOGGED_IN, true);
                        editor.apply();
                        startActivity(intent);
                    }
                    else{
                        Toast.makeText(SignInActivity.this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
                        username.setText("");
                        password1.setText("");
                    }
                }
            });
    }
}