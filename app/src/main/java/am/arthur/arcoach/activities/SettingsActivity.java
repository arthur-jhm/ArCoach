package am.arthur.arcoach.activities;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import am.arthur.arcoach.R;
import am.arthur.arcoach.auth.AuthManager;
import am.arthur.arcoach.cloud.FirestoreManager;
import am.arthur.arcoach.cloud.SyncManager;
import am.arthur.arcoach.database.Workout;
import am.arthur.arcoach.database.WorkoutRepository;
import am.arthur.arcoach.utils.MyLog;
import am.arthur.arcoach.utils.ReminderManager;
import am.arthur.arcoach.utils.UserPreferences;

public class SettingsActivity extends BaseActivity {
    private static final String TAG = "SettingsActivity";

    private SwitchCompat switchVoice;
    private SwitchCompat switchVibration;
    private SwitchCompat switchNotifications;
    private SeekBar seekBarVolume;
    private TextView tvVolumeValue;
    private CardView cardReminderTime;
    private CardView cardPermissionStatus;
    private Button btnSetReminderTime;
    private Button btnGrantPermission;
    private Button btnClearData;

    private Button btnExportData;
    private Button btnImportData;
    private ActivityResultLauncher<Intent> importLauncher;

    private UserPreferences userPreferences;
    private WorkoutRepository repository;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<Intent> exactAlarmSettingsLauncher;

    private FirestoreManager firestoreManager;
    private AuthManager authManager;
    private Button btnChangePassword;

    private LinearLayout layoutUserInfo;
    private LinearLayout layoutNotSignedIn;
    private TextView tvUserEmail;
    private TextView tvLastSync;
    private TextView tvCloudStorage;
    private Button btnSyncNow;
    private Button btnSignOut;
    private Button btnSignIn;
    private SyncManager syncManager;
    private SwitchCompat switchAutoSync;

    private AlertDialog conflictDialog;
    private int pendingLocalCount = 0;
    private int pendingCloudCount = 0;

    private CardView cardEmailVerification;
    private TextView tvVerificationStatus;
    private Button btnVerifyEmail;
    private Button btnResendVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupBackButton(R.string.settings_title);

        userPreferences = new UserPreferences(this);
        repository = new WorkoutRepository(this);

        // Firebase
        authManager = new AuthManager(this);
        firestoreManager = new FirestoreManager();
        syncManager = new SyncManager(this);

        setupPermissionLaunchers();
        initViews();
        loadSettings();
        setupListeners();
        updateAccountUI();

        updatePermissionStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void setupPermissionLaunchers() {
        // разрешения на уведомления
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    MyLog.d(TAG, "Permission result: " + isGranted);
                    if (isGranted) {
                        Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                        updatePermissionStatus();
                        checkExactAlarmPermission();
                    } else {
                        Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                        switchNotifications.setChecked(false);
                        showManualPermissionDialog();
                    }
                }
        );

        exactAlarmSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        android.app.AlarmManager alarmManager =
                                (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
                        if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                            MyLog.d(TAG, "Exact alarm permission granted");
                            setupReminder();
                        }
                    }
                }
        );

        // импорт файла
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            restoreFromBackup(uri);
                        }
                    }
                }
        );
    }

    private void initViews() {
        switchVoice = findViewById(R.id.switch_voice);
        switchVibration = findViewById(R.id.switch_vibration);
        switchNotifications = findViewById(R.id.switch_notifications);
        seekBarVolume = findViewById(R.id.seekbar_volume);
        tvVolumeValue = findViewById(R.id.tv_volume_value);
        cardReminderTime = findViewById(R.id.card_reminder_time);
        cardPermissionStatus = findViewById(R.id.card_permission_status);
        btnSetReminderTime = findViewById(R.id.btn_set_reminder_time);
        btnGrantPermission = findViewById(R.id.btn_grant_permission);
        btnClearData = findViewById(R.id.btn_clear_data);

        btnExportData = findViewById(R.id.btn_export_data);
        btnImportData = findViewById(R.id.btn_import_data);

        layoutUserInfo = findViewById(R.id.layout_user_info);
        layoutNotSignedIn = findViewById(R.id.layout_not_signed_in);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvLastSync = findViewById(R.id.tv_last_sync);
        tvCloudStorage = findViewById(R.id.tv_cloud_storage);
        btnSyncNow = findViewById(R.id.btn_sync_now);
        btnSignOut = findViewById(R.id.btn_sign_out);
        btnSignIn = findViewById(R.id.btn_sign_in);
        btnChangePassword = findViewById(R.id.btn_change_password);

        switchAutoSync = findViewById(R.id.switch_auto_sync);

        cardEmailVerification = findViewById(R.id.card_email_verification);
        tvVerificationStatus = findViewById(R.id.tv_verification_status);
        btnVerifyEmail = findViewById(R.id.btn_verify_email);
        btnResendVerification = findViewById(R.id.btn_resend_verification);
    }

    private void loadSettings() {
        switchVoice.setChecked(userPreferences.isVoiceEnabled());
        switchVibration.setChecked(userPreferences.isVibrationEnabled());
        switchNotifications.setChecked(userPreferences.isNotificationsEnabled());

        int volume = userPreferences.getVolume();
        seekBarVolume.setProgress(volume);
        tvVolumeValue.setText(volume + "%");

        String reminderTime = userPreferences.getReminderTime();
        btnSetReminderTime.setText(reminderTime);

        updateReminderTimeVisibility();
    }

    private void updatePermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            MyLog.d(TAG, "Permission status: " + hasPermission);

            // карточка предупреждения
            if (switchNotifications.isChecked() && !hasPermission) {
                cardPermissionStatus.setVisibility(View.VISIBLE);
            } else {
                cardPermissionStatus.setVisibility(View.GONE);
            }
        } else {
            // на Android < 13 разрешение не требуется
            cardPermissionStatus.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        // Голосовые подсказки
        switchVoice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userPreferences.setVoiceEnabled(isChecked);
                Toast.makeText(SettingsActivity.this,
                        isChecked ? R.string.voice_enabled : R.string.voice_disabled,
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Вибрация
        switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userPreferences.setVibrationEnabled(isChecked);
                Toast.makeText(SettingsActivity.this,
                        isChecked ? R.string.vibration_enabled : R.string.vibration_disabled,
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Уведомления
        switchNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Проверка и запрос разрешений
                    if (checkAndRequestNotificationPermission()) {
                        userPreferences.setNotificationsEnabled(true);
                        userPreferences.setReminderEnabled(true);
                        updateReminderTimeVisibility();

                        setupReminder();

                        String time = userPreferences.getReminderTime();
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.notifications_enabled) + " (" + time + ")",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Разрешение будет запрошено через launcher
                        buttonView.setChecked(false);
                    }
                } else {
                    userPreferences.setNotificationsEnabled(false);
                    userPreferences.setReminderEnabled(false);

                    updateReminderTimeVisibility();
                    ReminderManager.cancelReminder(SettingsActivity.this);
                    Toast.makeText(SettingsActivity.this,
                            R.string.notifications_disabled, Toast.LENGTH_SHORT).show();
                }
                updatePermissionStatus();
            }
        });

        // Громкость
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVolumeValue.setText(progress + "%");
                if (fromUser) {
                    userPreferences.setVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Временя напоминания
        btnSetReminderTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        // запрос разрешения
        btnGrantPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notification_permission_title)
                            .setMessage(R.string.notification_permission_message)
                            .setPositiveButton(R.string.request_again, (dialog, which) -> {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    // "Don't ask again" - открываем настройки
                    showManualPermissionDialog();
                }
            }
        });

        // Тест уведомления
        Button btnTestNotification = findViewById(R.id.btn_test_notification);
        if (btnTestNotification != null) {
            btnTestNotification.setOnClickListener(v -> testNotification());
        }

        // Очистка данных
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDataDialog();
            }
        });

        btnExportData.setOnClickListener(v -> exportData());
        btnImportData.setOnClickListener(v -> importData());

        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignOut.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out?")
                    .setMessage("Your data will remain on this device. Cloud backup will be disabled.")
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        MyLog.d(TAG, "User signing out");

                        syncManager.resetFirstSyncFlag();

                        authManager.signOut(() -> {
                            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        btnSyncNow.setOnClickListener(v -> performSync());

        switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            syncManager.setAutoSyncEnabled(isChecked);
            Toast.makeText(this,
                    isChecked ? "Auto-sync enabled" : "Auto-sync disabled",
                    Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnVerifyEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
                Toast.makeText(this, "Check your email for verification link", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Please check your email app", Toast.LENGTH_SHORT).show();
            }
        });

        btnResendVerification.setOnClickListener(v -> resendVerificationEmail());
    }


    private void resendVerificationEmail() {
        MyLog.d(TAG, "Resending verification email");

        btnResendVerification.setEnabled(false);
        btnResendVerification.setText("Sending...");

        authManager.sendEmailVerification(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                btnResendVerification.setEnabled(true);
                btnResendVerification.setText(R.string.resend_verification);

                Toast.makeText(SettingsActivity.this,
                        "✉️ Verification email sent! Check your inbox.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(String error) {
                btnResendVerification.setEnabled(true);
                btnResendVerification.setText(R.string.resend_verification);

                Toast.makeText(SettingsActivity.this,
                        "Failed to send email: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    private void showChangePasswordDialog() {
        MyLog.d(TAG, "=== SHOWING CHANGE PASSWORD DIALOG ===");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.input_current_password);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.input_new_password);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.input_confirm_password);

        new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setMessage("Enter your current password and new password")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = currentPasswordInput.getText().toString().trim();
                    String newPassword = newPasswordInput.getText().toString().trim();
                    String confirmPassword = confirmPasswordInput.getText().toString().trim();

                    MyLog.d(TAG, "User confirmed password change");

                    // Валидация
                    if (TextUtils.isEmpty(currentPassword)) {
                        Toast.makeText(this, "Please enter current password",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(newPassword)) {
                        Toast.makeText(this, "Please enter new password",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, R.string.passwords_dont_match,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentPassword.equals(newPassword)) {
                        Toast.makeText(this, R.string.password_must_be_different,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    changePasswordNow(currentPassword, newPassword);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    MyLog.d(TAG, "Password change cancelled");
                })
                .show();
    }


    private void changePasswordNow(String currentPassword, String newPassword) {
        MyLog.d(TAG, "=== CHANGING PASSWORD ===");

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Changing Password")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();

        progressDialog.show();

        authManager.changePassword(currentPassword, newPassword, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                progressDialog.dismiss();

                MyLog.d(TAG, "Password changed successfully!");

                Toast.makeText(SettingsActivity.this,
                        R.string.password_changed,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(String error) {
                progressDialog.dismiss();

                MyLog.e(TAG, "Failed to change password: " + error);

                Toast.makeText(SettingsActivity.this,
                        "Failed to change password: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Разрешение на уведомления
     */
    private boolean checkAndRequestNotificationPermission() {
        // Для Android 13+ (API 33+) нужно разрешение POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                MyLog.d(TAG, "Requesting notification permission");
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return false;
            }
        }

        checkExactAlarmPermission();
        return true;
    }

    private void showManualPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.enable_manually_title)
                .setMessage(R.string.enable_manually_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager =
                    (android.app.AlarmManager) getSystemService(ALARM_SERVICE);

            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                // Диалог с объяснением
                new AlertDialog.Builder(this)
                        .setTitle(R.string.exact_alarm_title)
                        .setMessage(R.string.exact_alarm_message)
                        .setPositiveButton(R.string.open_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                exactAlarmSettingsLauncher.launch(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        }
    }

    private void updateReminderTimeVisibility() {
        if (switchNotifications.isChecked()) {
            cardReminderTime.setVisibility(View.VISIBLE);
        } else {
            cardReminderTime.setVisibility(View.GONE);
        }
    }

    private void showTimePickerDialog() {
        String currentTime = userPreferences.getReminderTime();
        String[] parts = currentTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String time = String.format("%02d:%02d", hourOfDay, minute);
                        btnSetReminderTime.setText(time);
                        userPreferences.setReminderTime(time);
                        userPreferences.setReminderEnabled(true);

                        // Напоминание
                        if (switchNotifications.isChecked()) {
                            setupReminder();
                            Toast.makeText(SettingsActivity.this,
                                    getString(R.string.reminder_set, time),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                hour,
                minute,
                true  // 24-часовой формат
        );

        timePickerDialog.setTitle(R.string.settings_select_time);
        timePickerDialog.show();
    }

    private void setupReminder() {
        String time = userPreferences.getReminderTime();
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        MyLog.d(TAG, "Setting up reminder for " + time);
        ReminderManager.setDailyReminder(this, hour, minute);
    }

    private void testNotification() {
        MyLog.d(TAG, "Testing notification manually");

        boolean wasEnabled = userPreferences.isReminderEnabled();
        userPreferences.setReminderEnabled(true);

        Intent intent = new Intent(this, am.arthur.arcoach.utils.ReminderReceiver.class);
        sendBroadcast(intent);

        userPreferences.setReminderEnabled(wasEnabled);

        Toast.makeText(this, "Test notification sent! Check status above.", Toast.LENGTH_LONG).show();
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_data_title)
                .setMessage(R.string.clear_data_message)
                .setPositiveButton(R.string.yes_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearAllData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void clearAllData() {
        repository.clearAllData(new WorkoutRepository.OnDataClearedListener() {
            @Override
            public void onCleared() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SettingsActivity.this,
                                R.string.data_cleared,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void exportData() {
        new Thread(() -> {
            try {
                // все тренировки
                List<Workout> workouts = repository.getAllWorkoutsSync();

                runOnUiThread(() -> {
                    try {
                        JSONObject backup = new JSONObject();
                        backup.put("version", 1);
                        backup.put("exportDate", System.currentTimeMillis());
                        backup.put("appName", "ArCoach");

                        JSONArray workoutsArray = new JSONArray();
                        for (Workout workout : workouts) {
                            JSONObject w = new JSONObject();
                            w.put("exerciseType", workout.getExerciseType());
                            w.put("totalReps", workout.getTotalReps());
                            w.put("goodReps", workout.getGoodReps());
                            w.put("timeInSeconds", workout.getTimeInSeconds());
                            w.put("accuracy", workout.getAccuracy());
                            w.put("timestamp", workout.getTimestamp());
                            w.put("date", workout.getDate());
                            workoutsArray.put(w);
                        }
                        backup.put("workouts", workoutsArray);

                        // настройки профиля
                        JSONObject settings = new JSONObject();
                        settings.put("userName", userPreferences.getUserName(getString(R.string.user_name)));
                        settings.put("userAvatar", userPreferences.getUserAvatar("💪"));
                        settings.put("birthDate", userPreferences.getBirthDate());
                        settings.put("height", userPreferences.getHeight());
                        settings.put("weight", userPreferences.getWeight());

                        // настройки приложения
                        settings.put("voiceEnabled", userPreferences.isVoiceEnabled());
                        settings.put("vibrationEnabled", userPreferences.isVibrationEnabled());
                        settings.put("volume", userPreferences.getVolume());

                        backup.put("settings", settings);

                        // имя файла с датой
                        String filename = "ArCoach_Backup_" +
                                new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date()) + ".json";

                        // Сохраняем в файл
                        File file = new File(getExternalFilesDir(null), filename);
                        FileWriter writer = new FileWriter(file);
                        writer.write(backup.toString(2)); // Pretty print с отступами
                        writer.close();

                        MyLog.d(TAG, "Backup saved to: " + file.getAbsolutePath());

                        // Intent для отправки файла
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("application/json");

                        Uri uri = FileProvider.getUriForFile(
                                SettingsActivity.this,
                                getPackageName() + ".fileprovider",
                                file
                        );

                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "ArCoach Backup");
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                "Backup created on " + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_backup)));

                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.export_success) + "\n" + workouts.size() + " workouts",
                                Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        MyLog.e(TAG, "JSON error during export", e);
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.export_failed) + ": JSON error",
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        MyLog.e(TAG, "Export failed", e);
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.export_failed) + ": " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                MyLog.e(TAG, "Failed to get workouts", e);
                runOnUiThread(() ->
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.export_failed),
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void importData() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        String[] mimeTypes = {"application/json", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        importLauncher.launch(intent);
    }

    private void restoreFromBackup(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.import_failed) + ": Cannot open file",
                            Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                inputStream.close();

                JSONObject backup = new JSONObject(sb.toString());

                int version = backup.optInt("version", 1);
                if (version > 1) {
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.import_failed) + ": Unsupported backup version",
                            Toast.LENGTH_SHORT).show());
                    return;
                }

                JSONArray workoutsArray = backup.getJSONArray("workouts");
                JSONObject settings = backup.getJSONObject("settings");

                final int workoutsCount = workoutsArray.length();

                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.restore_title)
                            .setMessage(getString(R.string.restore_message) + "\n\n" +
                                    "Workouts: " + workoutsCount)
                            .setPositiveButton(R.string.restore, (dialog, which) -> {
                                // восстановление
                                performRestore(workoutsArray, settings);
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                });

            } catch (JSONException e) {
                MyLog.e(TAG, "JSON parse error", e);
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.import_failed) + ": Invalid backup file",
                        Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                MyLog.e(TAG, "Import failed", e);
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.import_failed) + ": " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void performRestore(JSONArray workoutsArray, JSONObject settings) {
        new Thread(() -> {
            try {
                // настройки профиля
                userPreferences.setUserName(settings.optString("userName", "User"));
                userPreferences.setUserAvatar(settings.optString("userAvatar", "💪"));
                userPreferences.setBirthDate(settings.optString("birthDate", ""));
                userPreferences.setHeight(settings.optInt("height", 0));

                double weightDouble = settings.optDouble("weight", 0.0);
                userPreferences.setWeight((float) weightDouble);

                // настройки приложения
                userPreferences.setVoiceEnabled(settings.optBoolean("voiceEnabled", true));
                userPreferences.setVibrationEnabled(settings.optBoolean("vibrationEnabled", true));
                userPreferences.setVolume(settings.optInt("volume", 80));

                // тренировки
                int restoredCount = 0;
                for (int i = 0; i < workoutsArray.length(); i++) {
                    try {
                        JSONObject w = workoutsArray.getJSONObject(i);

                        Workout workout = new Workout(
                                w.getString("exerciseType"),
                                w.getInt("totalReps"),
                                w.getInt("goodReps"),
                                w.getInt("timeInSeconds"),
                                w.getInt("accuracy"),
                                w.getLong("timestamp"),
                                w.getString("date")
                        );

                        repository.insertWorkoutSync(workout);
                        restoredCount++;

                    } catch (JSONException e) {
                        MyLog.e(TAG, "Failed to restore workout #" + i, e);
                    }
                }

                final int finalCount = restoredCount;
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            getString(R.string.restore_success) + "\n" +
                                    finalCount + " workouts restored",
                            Toast.LENGTH_LONG).show();

                    loadSettings();
                });

            } catch (Exception e) {
                MyLog.e(TAG, "Restore failed", e);
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.restore_failed) + ": " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void updateAccountUI() {
        if (authManager.isLoggedIn()) {
            // информация о пользователе
            layoutUserInfo.setVisibility(View.VISIBLE);
            layoutNotSignedIn.setVisibility(View.GONE);
            tvUserEmail.setText(authManager.getUserEmail());

            switchAutoSync.setChecked(syncManager.isAutoSyncEnabled());
            btnChangePassword.setVisibility(View.VISIBLE);

            updateEmailVerificationUI();

            loadBackupMetadata();

        } else {
            // кнопка входа
            layoutUserInfo.setVisibility(View.GONE);
            layoutNotSignedIn.setVisibility(View.VISIBLE);

            btnChangePassword.setVisibility(View.GONE);
            cardEmailVerification.setVisibility(View.GONE);
        }
    }


    private void updateEmailVerificationUI() {
        authManager.refreshEmailVerificationStatus(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                runOnUiThread(() -> {
                    if (user.isEmailVerified()) {
                        cardEmailVerification.setVisibility(View.GONE);
                        MyLog.d(TAG, "Email is verified");
                    } else {
                        cardEmailVerification.setVisibility(View.VISIBLE);
                        tvVerificationStatus.setText("⚠️ Your email is not verified. Please check your inbox.");
                        MyLog.d(TAG, "Email is NOT verified");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                MyLog.e(TAG, "Failed to check verification status: " + error);
                runOnUiThread(() -> {
                    cardEmailVerification.setVisibility(View.GONE);
                });
            }
        });
    }


    private void loadBackupMetadata() {
        MyLog.d(TAG, "Loading backup metadata...");

        firestoreManager.getSyncInfo(new FirestoreManager.SyncInfoCallback() {
            @Override
            public void onSuccess(long lastSyncTime, int workoutCount) {
                MyLog.d(TAG, "Backup metadata loaded: workouts=" + workoutCount + ", lastSync=" + lastSyncTime);
                runOnUiThread(() -> {
                    tvLastSync.setText(FirestoreManager.formatRelativeTime(lastSyncTime));
                    tvCloudStorage.setText(workoutCount + " workouts");
                });
            }

            @Override
            public void onFailure(String error) {
                MyLog.e(TAG, "Failed to load backup metadata: " + error);
                runOnUiThread(() -> {
                    tvLastSync.setText("Never");
                    tvCloudStorage.setText("0 workouts");
                });
            }
        });
    }


    private void performSync() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!authManager.isEmailVerified()) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Email Not Verified")
                    .setMessage("Please verify your email before syncing. Check your inbox for the verification link.")
                    .setPositiveButton("Resend Email", (dialog, which) -> {
                        resendVerificationEmail();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        MyLog.d(TAG, "=== MANUAL SYNC STARTED ===");

        btnSyncNow.setEnabled(false);
        btnSyncNow.setText(R.string.syncing);

        syncManager.performSyncWithConflictDialog(
                // Callback #1: Синхронизация
                new SyncManager.SyncCallback() {
                    @Override
                    public void onSyncStarted() {
                        MyLog.d(TAG, "Sync callback: started");
                    }

                    @Override
                    public void onSyncProgress(int progress, String message) {
                        MyLog.d(TAG, "Sync callback: progress=" + progress + ", message=" + message);
                        runOnUiThread(() -> {
                            btnSyncNow.setText(message);
                        });
                    }

                    @Override
                    public void onSyncComplete(boolean success, String message) {
                        MyLog.d(TAG, "Sync callback: complete, success=" + success + ", message=" + message);
                        runOnUiThread(() -> {
                            btnSyncNow.setEnabled(true);
                            btnSyncNow.setText(R.string.sync_now);

                            if (success) {
                                Toast.makeText(SettingsActivity.this,
                                        R.string.sync_success, Toast.LENGTH_SHORT).show();
                                loadBackupMetadata();
                            } else {
                                Toast.makeText(SettingsActivity.this,
                                        getString(R.string.sync_failed) + ": " + message,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                },
                // Callback #2: Конфликт
                new SyncManager.ConflictCallback() {
                    @Override
                    public void onConflictDetected(int localCount, int cloudCount) {
                        MyLog.d(TAG, "CONFLICT DETECTED in SettingsActivity: local=" + localCount + ", cloud=" + cloudCount);
                        runOnUiThread(() -> {
                            showSyncConflictDialog(localCount, cloudCount);
                        });
                    }
                }
        );
    }


    private void showSyncConflictDialog(int localCount, int cloudCount) {
        pendingLocalCount = localCount;
        pendingCloudCount = cloudCount;

        btnSyncNow.setEnabled(true);
        btnSyncNow.setText(R.string.sync_now);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sync_conflict, null);

        TextView tvLocalCount = dialogView.findViewById(R.id.tv_local_count);
        TextView tvCloudCount = dialogView.findViewById(R.id.tv_cloud_count);
        TextView tvRecommendation = dialogView.findViewById(R.id.tv_recommendation);

        tvLocalCount.setText(String.valueOf(localCount));
        tvCloudCount.setText(String.valueOf(cloudCount));

        // Рекомендация
        if (cloudCount > localCount) {
            tvRecommendation.setText("⬇️ Recommended: Download from cloud\n" +
                    "(Cloud has " + (cloudCount - localCount) + " more workouts that you don't have locally)");
            tvRecommendation.setTextColor(0xFF4CAF50); // Green
        } else {
            tvRecommendation.setText("⬆️ Recommended: Upload to cloud (cloud will get " +
                    (localCount - cloudCount) + " more workouts)");
            tvRecommendation.setTextColor(0xFF2196F3); // Blue
        }

        conflictDialog = new AlertDialog.Builder(this)
                .setTitle("⚠️ Sync Conflict Detected")
                .setView(dialogView)
                .setPositiveButton("⬇️ Download from Cloud", (dialog, which) -> {
                    handleDownloadChoice();
                })
                .setNegativeButton("⬆️ Upload to Cloud", (dialog, which) -> {
                    handleUploadChoice();
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Sync cancelled", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(true)
                .create();

        conflictDialog.show();

        conflictDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF4CAF50); // Green
        conflictDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF2196F3); // Blue
        conflictDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0xFF757575); // Gray
    }


    private void handleDownloadChoice() {
        MyLog.d(TAG, "User chose: Download from cloud");

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Confirm Download")
                .setMessage("This will DELETE your " + pendingLocalCount +
                        " local workouts and download " + pendingCloudCount +
                        " workouts from cloud.\n\nAre you sure?")
                .setPositiveButton("Yes, Download", (dialog, which) -> {
                    performForceDownload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void handleUploadChoice() {
        MyLog.d(TAG, "User chose: Upload to cloud");

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Confirm Upload")
                .setMessage("This will OVERWRITE " + pendingCloudCount +
                        " cloud workouts with your " + pendingLocalCount +
                        " local workouts.\n\nAre you sure?")
                .setPositiveButton("Yes, Upload", (dialog, which) -> {
                    performForceUpload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void performForceDownload() {
        btnSyncNow.setEnabled(false);
        btnSyncNow.setText("Downloading...");

        syncManager.forceDownloadFromCloud(new SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted() {
                MyLog.d(TAG, "Force download started");
            }

            @Override
            public void onSyncProgress(int progress, String message) {
                runOnUiThread(() -> {
                    btnSyncNow.setText(message);
                });
            }

            @Override
            public void onSyncComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    btnSyncNow.setEnabled(true);
                    btnSyncNow.setText(R.string.sync_now);

                    if (success) {
                        Toast.makeText(SettingsActivity.this,
                                "✅ Downloaded " + pendingCloudCount + " workouts from cloud",
                                Toast.LENGTH_LONG).show();
                        loadBackupMetadata();
                    } else {
                        Toast.makeText(SettingsActivity.this,
                                "❌ Download failed: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    /**
     * Принудительная загрузка
     */
    private void performForceUpload() {
        btnSyncNow.setEnabled(false);
        btnSyncNow.setText("Uploading...");

        syncManager.forceUploadToCloud(new SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted() {
                MyLog.d(TAG, "Force upload started");
            }

            @Override
            public void onSyncProgress(int progress, String message) {
                runOnUiThread(() -> {
                    btnSyncNow.setText(message);
                });
            }

            @Override
            public void onSyncComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    btnSyncNow.setEnabled(true);
                    btnSyncNow.setText(R.string.sync_now);

                    if (success) {
                        Toast.makeText(SettingsActivity.this,
                                "✅ Uploaded " + pendingLocalCount + " workouts to cloud",
                                Toast.LENGTH_LONG).show();
                        loadBackupMetadata();
                    } else {
                        Toast.makeText(SettingsActivity.this,
                                "❌ Upload failed: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
