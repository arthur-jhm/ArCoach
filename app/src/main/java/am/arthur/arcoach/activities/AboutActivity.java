package am.arthur.arcoach.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

import am.arthur.arcoach.BuildConfig;
import am.arthur.arcoach.R;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setupBackButton(R.string.about_title);

        TextView versionView = findViewById(R.id.about_version);
        versionView.setText(getString(R.string.version, BuildConfig.VERSION_NAME));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}