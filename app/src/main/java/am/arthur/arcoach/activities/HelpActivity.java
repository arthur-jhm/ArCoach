package am.arthur.arcoach.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;

import am.arthur.arcoach.R;

public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        setupBackButton(R.string.help_title);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}