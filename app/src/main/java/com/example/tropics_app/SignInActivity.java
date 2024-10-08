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
import android.app.Dialog;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    private TextView tvForgotPass;  // Add this for Forgot Password TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        mAuth = FirebaseAuth.getInstance();
        username = findViewById(R.id.edUsername);
        password1 = findViewById(R.id.edPassword);
        btnSignin = findViewById(R.id.btnSignIn);
        tvForgotPass = findViewById(R.id.tvForgotPass);  // Initialize your TextView here
        intent = new Intent(this, NavigationActivity.class);

        sharedPreferences = getSharedPreferences(Preference, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn) {
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

        // Set an OnClickListener for the "Forgot Password?" text
        tvForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();  // Show the dialog when "Forgot Password?" is clicked
            }
        });
    }

    private void signInUser(String email, String password) {
        if (email.isEmpty()) {
            username.setError("Enter Username");
            username.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            password1.setError("Enter Password");
            password1.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(KEY_IS_LOGGED_IN, true);
                            editor.apply();
                            startActivity(intent);
                        } else {
                            Toast.makeText(SignInActivity.this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
                            username.setText("");
                            password1.setText("");
                        }
                    }
                });
    }

    private void showForgotPasswordDialog() {
        // Create and show a dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.forgotpassword);  // Inflate forgotpassword.xml layout

        EditText emailInput = dialog.findViewById(R.id.emailInput);
        Button sendResetLinkButton = dialog.findViewById(R.id.submitButton);

        // Set click listener for the reset link button
        sendResetLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                if (email.isEmpty()) {
                    emailInput.setError("Enter email");
                    return;
                }
                sendPasswordResetEmail(email, dialog);
            }
        });

        dialog.show();  // Show the dialog
    }

    private void sendPasswordResetEmail(String email, Dialog dialog) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignInActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();  // Close the dialog
                        } else {
                            Toast.makeText(SignInActivity.this, "Error sending reset email", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
