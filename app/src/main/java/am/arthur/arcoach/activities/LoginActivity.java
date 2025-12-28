package am.arthur.arcoach.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import am.arthur.arcoach.R;
import am.arthur.arcoach.auth.AuthManager;
import am.arthur.arcoach.utils.MyLog;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private AuthManager authManager;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnSignIn;
    private Button btnSignUp;
    private TextView tvForgotPassword;
    private TextView tvContinueWithoutAccount;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_sign_in);
        btnSignUp = findViewById(R.id.btn_sign_up);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvContinueWithoutAccount = findViewById(R.id.tv_continue_without_account);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnSignIn.setOnClickListener(v -> signIn());
        btnSignUp.setOnClickListener(v -> signUp());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        tvContinueWithoutAccount.setOnClickListener(v -> {
            MyLog.d(TAG, "User chose to continue without account");
            goToMainActivity();
        });
    }


    private void signIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showLoading();

        authManager.signIn(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                hideLoading();
                Toast.makeText(LoginActivity.this,
                        "Welcome back!", Toast.LENGTH_SHORT).show();
                goToMainActivity();
            }

            @Override
            public void onFailure(String error) {
                hideLoading();
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        authManager.signUp(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                MyLog.d(TAG, "Sign up successful, sending verification email");

                authManager.sendEmailVerification(new AuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        hideLoading();
                        showVerificationSentDialog(email);
                    }

                    @Override
                    public void onFailure(String error) {
                        hideLoading();

                        MyLog.e(TAG, "Failed to send verification email: " + error);

                        new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("Account Created")
                                .setMessage("Your account was created successfully, but we couldn't send the verification email. You can resend it from Settings.")
                                .setPositiveButton("Continue", (dialog, which) -> {
                                    goToMainActivity();
                                })
                                .show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                hideLoading();
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void showForgotPasswordDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to receive a password reset link")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = input.getText().toString().trim();

                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Please enter your email",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showLoading();

                    authManager.resetPassword(email, new AuthManager.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this,
                                    "Password reset email sent!", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            hideLoading();
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }


    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);
        btnSignUp.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnSignIn.setEnabled(true);
        btnSignUp.setEnabled(true);
    }


    private void showVerificationSentDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("✉️ Verify Your Email")
                .setMessage("We've sent a verification link to:\n\n" +
                        email + "\n\n" +
                        "Please check your inbox (and spam folder) and click the link to verify your email address.\n\n" +
                        "You can use the app now, but some features may be limited until verification.")
                .setPositiveButton("Got it!", (dialog, which) -> {
                    goToMainActivity();
                })
                .setCancelable(false)
                .show();
    }

}

