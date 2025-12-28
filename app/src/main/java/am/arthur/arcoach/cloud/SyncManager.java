package am.arthur.arcoach.cloud;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import am.arthur.arcoach.R;
import am.arthur.arcoach.auth.AuthManager;
import am.arthur.arcoach.database.Workout;
import am.arthur.arcoach.database.WorkoutRepository;
import am.arthur.arcoach.utils.MyLog;
import am.arthur.arcoach.utils.UserPreferences;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String PREF_LAST_SYNC = "last_sync_timestamp";
    private static final String PREF_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    private static final String PREF_FIRST_SYNC_DONE = "first_sync_done";

    private Context context;
    private AuthManager authManager;
    private FirestoreManager firestoreManager;
    private WorkoutRepository repository;
    private UserPreferences userPreferences;

    public interface SyncCallback {
        void onSyncStarted();
        void onSyncProgress(int progress, String message);
        void onSyncComplete(boolean success, String message);
    }

    public interface ConflictCallback {
        void onConflictDetected(int localCount, int cloudCount);
    }

    public SyncManager(Context context) {
        this.context = context;
        this.authManager = new AuthManager(context);
        this.firestoreManager = new FirestoreManager();
        this.repository = new WorkoutRepository(context);
        this.userPreferences = new UserPreferences(context);

        MyLog.d(TAG, "SyncManager initialized");
    }

    public boolean shouldAutoSyncOnLogin() {
        if (!authManager.isLoggedIn()) {
            return false;
        }

        MyLog.d(TAG, "Should auto sync on login: true (always check cloud on login)");
        return true;
    }

    /**
     * Автоматическое восстановление при входе
     * ВСЕГДА проверяет облако и восстанавливает если данные есть
     */
    public void autoRestoreOnFirstLogin(SyncCallback callback) {
        if (!authManager.isLoggedIn()) {
            callback.onSyncComplete(false, "User not logged in");
            return;
        }

        MyLog.d(TAG, "Checking cloud for data on login");
        callback.onSyncStarted();

        // Количество локальных тренировок
        new Thread(() -> {
            try {
                List<Workout> localWorkouts = repository.getAllWorkoutsSync();
                int localCount = localWorkouts.size();

                MyLog.d(TAG, "Local workouts count: " + localCount);

                // есть ли данные в облаке
                firestoreManager.getSyncInfo(new FirestoreManager.SyncInfoCallback() {
                    @Override
                    public void onSuccess(long lastSyncTime, int cloudCount) {
                        MyLog.d(TAG, "Cloud workouts count: " + cloudCount);

                        if (cloudCount > 0 && localCount == 0) {
                            // В облаке есть данные, локально пусто -> ВОССТАНАВЛИВАЕМ
                            MyLog.d(TAG, "Cloud has data (" + cloudCount + "), local is empty - downloading");
                            downloadAndRestore(callback);

                        } else if (cloudCount == 0 && localCount > 0) {
                            // Локально есть данные, облако пусто -> ЗАГРУЖАЕМ
                            MyLog.d(TAG, "Local has data (" + localCount + "), cloud is empty - uploading");
                            performUpload(callback);

                        } else if (cloudCount > 0 && localCount > 0) {
                            // Оба не пусты -> КОНФЛИКТ
                            MyLog.d(TAG, "Both cloud and local have data - skipping auto-sync");
                            callback.onSyncComplete(true, "Data exists locally and in cloud");
                            markFirstSyncDone();

                        } else {
                            // Оба пусты -> ничего не делаем
                            MyLog.d(TAG, "Both cloud and local are empty");
                            callback.onSyncComplete(true, "No data to sync");
                            markFirstSyncDone();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        if (localCount > 0) {
                            MyLog.d(TAG, "Cannot check cloud, uploading local data");
                            performUpload(callback);
                        } else {
                            MyLog.d(TAG, "No cloud data, no local data");
                            callback.onSyncComplete(true, "No data to sync");
                            markFirstSyncDone();
                        }
                    }
                });

            } catch (Exception e) {
                MyLog.e(TAG, "Error checking local data", e);
                callback.onSyncComplete(false, "Error: " + e.getMessage());
            }
        }).start();
    }


    private void downloadAndRestore(SyncCallback callback) {
        firestoreManager.downloadAllData(new FirestoreManager.DownloadCallback() {
            @Override
            public void onProgress(int progress, String message) {
                callback.onSyncProgress(progress, message);
            }

            @Override
            public void onSuccess(List<Workout> workouts, Map<String, Object> profileData) {
                new Thread(() -> {
                    try {
                        if (profileData != null && !profileData.isEmpty()) {
                            restoreProfile(profileData);
                        }

                        callback.onSyncProgress(70, "Restoring workouts...");

                        MyLog.d(TAG, "CLEARING local database before restore");
                        repository.deleteAllWorkoutsSync();

                        // Восстанавливаем тренировки
                        int restored = 0;
                        for (Workout workout : workouts) {
                            repository.insertWorkoutSync(workout);
                            restored++;

                            if (restored % 10 == 0 || restored == workouts.size()) {
                                int progress = 70 + ((restored * 25) / workouts.size());
                                final int finalRestored = restored;
                                callback.onSyncProgress(progress, "Restored " + finalRestored + " workouts...");
                            }
                        }

                        saveLastSyncTime();
                        markFirstSyncDone();

                        MyLog.d(TAG, "Restore complete: " + restored + " workouts (replaced all local data)");
                        callback.onSyncComplete(true, "Restored " + restored + " workouts");

                    } catch (Exception e) {
                        MyLog.e(TAG, "Restore failed", e);
                        callback.onSyncComplete(false, "Restore failed: " + e.getMessage());
                    }
                }).start();
            }

            @Override
            public void onFailure(String error) {
                MyLog.e(TAG, "Download failed: " + error);
                callback.onSyncComplete(false, "Download failed: " + error);
            }
        });
    }


    public void autoBackupOnExit(SyncCallback callback) {
        if (!authManager.isLoggedIn()) {
            MyLog.d(TAG, "Auto backup skipped - user not logged in");
            callback.onSyncComplete(true, "Not logged in");
            return;
        }

        if (!isAutoSyncEnabled()) {
            MyLog.d(TAG, "Auto backup skipped - auto sync disabled");
            callback.onSyncComplete(true, "Auto sync disabled");
            return;
        }

        MyLog.d(TAG, "Starting auto backup on exit");
        callback.onSyncStarted();
        performUpload(callback);
    }


    public void performUpload(SyncCallback callback) {
        new Thread(() -> {
            try {
                List<Workout> workouts = repository.getAllWorkoutsSync();

                Map<String, Object> profileData = new HashMap<>();
                profileData.put("email", authManager.getUserEmail());
                profileData.put("userId", authManager.getUserId());
                profileData.put("userName", userPreferences.getUserName(context.getString(R.string.user_name)));
                profileData.put("userAvatar", userPreferences.getUserAvatar("💪"));
                profileData.put("birthDate", userPreferences.getBirthDate());
                profileData.put("height", userPreferences.getHeight());
                profileData.put("weight", userPreferences.getWeight());
                profileData.put("voiceEnabled", userPreferences.isVoiceEnabled());
                profileData.put("vibrationEnabled", userPreferences.isVibrationEnabled());
                profileData.put("volume", userPreferences.getVolume());

                // Загружаем в Firestore
                firestoreManager.uploadAllData(workouts, profileData, new FirestoreManager.UploadCallback() {
                    @Override
                    public void onProgress(int progress, String message) {
                        callback.onSyncProgress(progress, message);
                    }

                    @Override
                    public void onSuccess() {
                        MyLog.d(TAG, "Upload successful");
                        saveLastSyncTime();
                        markFirstSyncDone();
                        callback.onSyncComplete(true, "Sync complete");
                    }

                    @Override
                    public void onFailure(String error) {
                        MyLog.e(TAG, "Upload failed: " + error);
                        callback.onSyncComplete(false, "Upload failed: " + error);
                    }
                });

            } catch (Exception e) {
                MyLog.e(TAG, "Error preparing upload", e);
                callback.onSyncComplete(false, "Error: " + e.getMessage());
            }
        }).start();
    }


    private void restoreProfile(Map<String, Object> profileData) {
        try {
            if (profileData.containsKey("userName")) {
                userPreferences.setUserName((String) profileData.get("userName"));
            }
            if (profileData.containsKey("userAvatar")) {
                userPreferences.setUserAvatar((String) profileData.get("userAvatar"));
            }
            if (profileData.containsKey("birthDate")) {
                userPreferences.setBirthDate((String) profileData.get("birthDate"));
            }
            if (profileData.containsKey("height")) {
                userPreferences.setHeight(((Long) profileData.get("height")).intValue());
            }
            if (profileData.containsKey("weight")) {
                double weight = (Double) profileData.get("weight");
                userPreferences.setWeight((float) weight);
            }
            if (profileData.containsKey("voiceEnabled")) {
                userPreferences.setVoiceEnabled((Boolean) profileData.get("voiceEnabled"));
            }
            if (profileData.containsKey("vibrationEnabled")) {
                userPreferences.setVibrationEnabled((Boolean) profileData.get("vibrationEnabled"));
            }
            if (profileData.containsKey("volume")) {
                userPreferences.setVolume(((Long) profileData.get("volume")).intValue());
            }

            MyLog.d(TAG, "Profile data restored");
        } catch (Exception e) {
            MyLog.e(TAG, "Error restoring profile", e);
        }
    }

    private void saveLastSyncTime() {
        userPreferences.getSharedPreferences()
                .edit()
                .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                .apply();
    }

    public long getLastSyncTime() {
        return userPreferences.getSharedPreferences()
                .getLong(PREF_LAST_SYNC, 0);
    }

    private void markFirstSyncDone() {
        userPreferences.getSharedPreferences()
                .edit()
                .putBoolean(PREF_FIRST_SYNC_DONE, true)
                .apply();
    }

    public void resetFirstSyncFlag() {
        userPreferences.getSharedPreferences()
                .edit()
                .putBoolean(PREF_FIRST_SYNC_DONE, false)
                .apply();
    }

    public boolean isAutoSyncEnabled() {
        return userPreferences.getSharedPreferences()
                .getBoolean(PREF_AUTO_SYNC_ENABLED, true);
    }

    public void setAutoSyncEnabled(boolean enabled) {
        userPreferences.getSharedPreferences()
                .edit()
                .putBoolean(PREF_AUTO_SYNC_ENABLED, enabled)
                .apply();
    }


    public void performSyncWithConflictDialog(SyncCallback callback, ConflictCallback conflictCallback) {
        MyLog.d(TAG, "=== performSyncWithConflictDialog STARTED ===");

        new Thread(() -> {
            try {
                // локальные данные
                List<Workout> localWorkouts = repository.getAllWorkoutsSync();
                int localCount = localWorkouts.size();

                MyLog.d(TAG, "Sync with dialog: local workouts = " + localCount);

                // облачные данные
                firestoreManager.getSyncInfo(new FirestoreManager.SyncInfoCallback() {
                    @Override
                    public void onSuccess(long lastSyncTime, int cloudCount) {
                        MyLog.d(TAG, "==========================================");
                        MyLog.d(TAG, "SYNC INFO RECEIVED:");
                        MyLog.d(TAG, "  Local workouts:  " + localCount);
                        MyLog.d(TAG, "  Cloud workouts:  " + cloudCount);
                        MyLog.d(TAG, "  Difference:   " + (localCount - cloudCount));
                        MyLog.d(TAG, "==========================================");

                        // 1: Локально пусто, в облаке есть -> СКАЧИВАЕМ
                        if (localCount == 0 && cloudCount > 0) {
                            MyLog.d(TAG, "CASE 1: Local is empty, cloud has " + cloudCount + " → AUTO-DOWNLOAD");
                            callback.onSyncProgress(10, "Downloading from cloud...");
                            downloadAndRestore(callback);
                            return;
                        }

                        // 2: В облаке пусто, локально есть -> ЗАГРУЖАЕМ
                        if (localCount > 0 && cloudCount == 0) {
                            MyLog.d(TAG, "CASE 2: Cloud is empty, local has " + localCount + " → AUTO-UPLOAD");
                            callback.onSyncProgress(10, "Uploading to cloud...");
                            performUpload(callback);
                            return;
                        }

                        // 3: Локально БОЛЬШЕ -> добавили новые тренировки -> ЗАГРУЖАЕМ
                        if (localCount > cloudCount) {
                            MyLog.d(TAG, "CASE 3: Local > Cloud (" + localCount + " > " + cloudCount + ") → AUTO-UPLOAD (new workouts added locally)");
                            callback.onSyncProgress(10, "Uploading new workouts to cloud...");
                            performUpload(callback);
                            return;
                        }

                        // 4: В облаке БОЛЬШЕ -> ВОЗМОЖНА ПОТЕРЯ -> СПРАШИВАЕМ!
                        if (localCount < cloudCount) {
                            MyLog.w(TAG, "CASE 4: Cloud > Local (" + cloudCount + " > " + localCount + ") → CONFLICT! (possible data loss)");

                            // callback для показа диалога
                            if (conflictCallback != null) {
                                MyLog.d(TAG, "Calling conflictCallback.onConflictDetected()");
                                conflictCallback.onConflictDetected(localCount, cloudCount);
                            } else {
                                // Fallback: скачиваем
                                MyLog.d(TAG, "No conflictCallback, auto-downloading from cloud");
                                callback.onSyncProgress(10, "Cloud has more data, downloading...");
                                downloadAndRestore(callback);
                            }
                            return;
                        }

                        // 5: Одинаковое количество -> обычная синхронизация
                        MyLog.d(TAG, "CASE 5: Local == Cloud (" + localCount + ") → NORMAL SYNC (update timestamp)");
                        performUpload(callback);
                    }

                    @Override
                    public void onFailure(String error) {
                        // Невозможно проверить облако -> загружаем как обычно
                        MyLog.e(TAG, "Cannot check cloud data: " + error + ", proceeding with upload");
                        performUpload(callback);
                    }
                });

            } catch (Exception e) {
                MyLog.e(TAG, "Sync with dialog failed", e);
                callback.onSyncComplete(false, "Error: " + e.getMessage());
            }
        }).start();
    }


    public void forceUploadToCloud(SyncCallback callback) {
        MyLog.d(TAG, "=== FORCE UPLOAD: Overwriting cloud data with local data ===");
        callback.onSyncStarted();
        callback.onSyncProgress(10, "Force uploading to cloud...");
        performUpload(callback);
    }


    public void forceDownloadFromCloud(SyncCallback callback) {
        MyLog.d(TAG, "=== FORCE DOWNLOAD: Overwriting local data with cloud data ===");
        callback.onSyncStarted();
        callback.onSyncProgress(10, "Force downloading from cloud...");
        downloadAndRestore(callback);
    }
}
