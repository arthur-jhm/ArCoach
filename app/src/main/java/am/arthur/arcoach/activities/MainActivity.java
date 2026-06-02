package am.arthur.arcoach.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import am.arthur.arcoach.R;
import am.arthur.arcoach.auth.AuthManager;
import am.arthur.arcoach.cloud.SyncManager;
import am.arthur.arcoach.fragments.GuideFragment;
import am.arthur.arcoach.fragments.HomeFragment;
import am.arthur.arcoach.fragments.ProfileFragment;
import am.arthur.arcoach.fragments.StatisticsFragment;
import am.arthur.arcoach.utils.MyLog;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;

    private AuthManager authManager;
    private SyncManager syncManager;
    private ProgressDialog syncDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // инициализация Firebase
        authManager = new AuthManager(this);
        syncManager = new SyncManager(this);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                    toolbar.setTitle(R.string.app_name);
                } else if (itemId == R.id.nav_guide) {
                    selectedFragment = new GuideFragment();
                    toolbar.setTitle(R.string.nav_guide);
                } else if (itemId == R.id.nav_statistics) {
                    selectedFragment = new StatisticsFragment();
                    toolbar.setTitle(R.string.nav_statistics);
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                    toolbar.setTitle(R.string.nav_profile);
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        checkAndPerformFirstSync();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        return true;
    }

    // нажатия
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.menu_achievements) {
            startActivity(new Intent(MainActivity.this, AchievementsActivity.class));
            return true;
        } else if (itemId == R.id.menu_help) {
            startActivity(new Intent(MainActivity.this, HelpActivity.class));
            return true;
        } else if (itemId == R.id.menu_about) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
        } else if (itemId == R.id.menu_exit) {
            showExitDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();
                        System.exit(0);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    private void checkAndPerformFirstSync() {
        if (syncManager.shouldAutoSyncOnLogin()) {
            MyLog.d(TAG, "Checking for cloud data");
            showSyncDialog("Checking your data...");

            syncManager.autoRestoreOnFirstLogin(new SyncManager.SyncCallback() {
                @Override
                public void onSyncStarted() {
                    MyLog.d(TAG, "Auto-restore started");
                }

                @Override
                public void onSyncProgress(int progress, String message) {
                    updateSyncDialog(progress, message);
                }

                @Override
                public void onSyncComplete(boolean success, String message) {
                    dismissSyncDialog();

                    if (success && message.contains("downloaded")) {
                        Toast.makeText(MainActivity.this,
                                "✅ Data restored from cloud!",
                                Toast.LENGTH_LONG).show();
                        recreate();
                    }
                }
            });
        }
    }


    private void showSyncDialog(String message) {
        if (syncDialog == null) {
            syncDialog = new ProgressDialog(this);
            syncDialog.setCancelable(false);
            syncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            syncDialog.setMax(100);
        }

        syncDialog.setMessage(message);
        syncDialog.setProgress(0);
        syncDialog.show();
    }


    private void updateSyncDialog(int progress, String message) {
        if (syncDialog != null && syncDialog.isShowing()) {
            syncDialog.setProgress(progress);
            syncDialog.setMessage(message);
        }
    }


    private void dismissSyncDialog() {
        if (syncDialog != null && syncDialog.isShowing()) {
            syncDialog.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Автобэкап при выходе
        if (authManager.isLoggedIn() && syncManager.isAutoSyncEnabled()) {
            MyLog.d(TAG, "App stopped, performing auto-backup");

            // бэкап в фоне
            syncManager.autoBackupOnExit(new SyncManager.SyncCallback() {
                @Override
                public void onSyncStarted() {
                    MyLog.d(TAG, "Auto-backup started");
                }

                @Override
                public void onSyncProgress(int progress, String message) {
                    MyLog.d(TAG, "Auto-backup progress: " + progress + "% - " + message);
                }

                @Override
                public void onSyncComplete(boolean success, String message) {
                    if (success) {
                        MyLog.d(TAG, "Auto-backup complete");
                    } else {
                        MyLog.e(TAG, "Auto-backup failed: " + message);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (syncDialog != null && syncDialog.isShowing()) {
            syncDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showExitDialog();
    }
}