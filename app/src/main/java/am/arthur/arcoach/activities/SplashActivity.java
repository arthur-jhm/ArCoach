package am.arthur.arcoach.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import am.arthur.arcoach.BuildConfig;
import am.arthur.arcoach.R;
import am.arthur.arcoach.auth.AuthManager;
import am.arthur.arcoach.utils.MyLog;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 2500; // 2.5 секунды
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView tagline = findViewById(R.id.splash_tagline);
        TextView version = findViewById(R.id.splash_version);
        version.setText(getString(R.string.splash_version, BuildConfig.VERSION_NAME));

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        logo.startAnimation(fadeIn);
        appName.startAnimation(slideUp);
        tagline.startAnimation(fadeIn);
        version.startAnimation(fadeIn);

        authManager = new AuthManager(this);

        // запуск MainActivity через 2.5 секунды
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuthAndNavigate();
            }
        }, SPLASH_DURATION);
    }


    private void checkAuthAndNavigate() {
        if (authManager.isLoggedIn()) {
            MyLog.d(TAG, "User is logged in, going to MainActivity");
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            MyLog.d(TAG, "User not logged in, going to LoginActivity");
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        finish();
    }
}
