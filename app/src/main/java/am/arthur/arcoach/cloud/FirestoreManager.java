package am.arthur.arcoach.cloud;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import am.arthur.arcoach.database.Workout;
import am.arthur.arcoach.utils.MyLog;


public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_WORKOUTS = "workouts";
    private static final String COLLECTION_PROFILE = "profile";
    private static final String DOC_PROFILE_DATA = "data";
    private static final String DOC_SYNC_METADATA = "sync_metadata";

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public interface UploadCallback {
        void onProgress(int progress, String message);
        void onSuccess();
        void onFailure(String error);
    }

    public interface DownloadCallback {
        void onProgress(int progress, String message);
        void onSuccess(List<Workout> workouts, Map<String, Object> profileData);
        void onFailure(String error);
    }

    public interface SyncInfoCallback {
        void onSuccess(long lastSyncTime, int workoutCount);
        void onFailure(String error);
    }

    /**
     * Конструктор
     */
    public FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();

        MyLog.d(TAG, "FirestoreManager initialized");
    }

    /**
     * Проверить что пользователь залогинен
     */
    private boolean isUserLoggedIn() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            MyLog.e(TAG, "User not logged in");
            return false;
        }
        return true;
    }

    /**
     * Получить ID пользователя
     */
    private String getUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Загрузить все данные в Firestore
     */
    public void uploadAllData(List<Workout> workouts, Map<String, Object> profileData,
                              UploadCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = getUserId();
        MyLog.d(TAG, "Starting upload for user: " + userId);

        callback.onProgress(10, "Preparing data...");

        WriteBatch batch = db.batch();

        try {
            // 1. Сохраняем профиль
            Map<String, Object> profile = new HashMap<>(profileData);
            profile.put("lastUpdated", System.currentTimeMillis());

            // Добавляем email и userId для поиска в Firebase Console
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                profile.put("email", user.getEmail());
                profile.put("userId", user.getUid());
            }

            batch.set(db.collection(COLLECTION_USERS)
                            .document(userId)
                            .collection(COLLECTION_PROFILE)
                            .document(DOC_PROFILE_DATA),
                    profile);

            callback.onProgress(30, "Uploading profile...");

            // 2. Сохраняем тренировки
            int total = workouts.size();
            int uploaded = 0;

            for (Workout workout : workouts) {
                Map<String, Object> workoutData = new HashMap<>();
                workoutData.put("exerciseType", workout.getExerciseType());
                workoutData.put("totalReps", workout.getTotalReps());
                workoutData.put("goodReps", workout.getGoodReps());
                workoutData.put("timeInSeconds", workout.getTimeInSeconds());
                workoutData.put("accuracy", workout.getAccuracy());
                workoutData.put("timestamp", workout.getTimestamp());
                workoutData.put("date", workout.getDate());

                String workoutId = String.valueOf(workout.getTimestamp());

                batch.set(db.collection(COLLECTION_USERS)
                                .document(userId)
                                .collection(COLLECTION_WORKOUTS)
                                .document(workoutId),
                        workoutData);

                uploaded++;
                if (uploaded % 10 == 0 || uploaded == total) {
                    int progress = 30 + ((uploaded * 60) / total);
                    callback.onProgress(progress, "Uploading workouts " + uploaded + "/" + total);
                }
            }

            // 3. Сохраняем метаданные синхронизации
            Map<String, Object> syncMetadata = new HashMap<>();
            syncMetadata.put("lastSyncTime", System.currentTimeMillis());
            syncMetadata.put("version", 1);

            // Добавляем email для поиска
            if (user != null) {
                syncMetadata.put("email", user.getEmail());
                syncMetadata.put("userId", user.getUid());
            }

            batch.set(db.collection(COLLECTION_USERS)
                            .document(userId)
                            .collection(COLLECTION_PROFILE)
                            .document(DOC_SYNC_METADATA),
                    syncMetadata);

            callback.onProgress(95, "Finalizing...");

            batch.commit()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            MyLog.d(TAG, "Upload successful: " + workouts.size() + " workouts");
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            MyLog.e(TAG, "Upload failed", e);
                            callback.onFailure("Upload failed: " + e.getMessage());
                        }
                    });

        } catch (Exception e) {
            MyLog.e(TAG, "Error preparing upload", e);
            callback.onFailure("Error preparing data: " + e.getMessage());
        }
    }

    /**
     * Скачать все данные из Firestore
     */
    public void downloadAllData(DownloadCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = getUserId();
        MyLog.d(TAG, "Starting download for user: " + userId);

        callback.onProgress(10, "Checking cloud data...");

        // Загрузка профиля
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILE)
                .document(DOC_PROFILE_DATA)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String, Object> profileData = new HashMap<>();

                        if (documentSnapshot.exists()) {
                            profileData = documentSnapshot.getData();
                            MyLog.d(TAG, "Profile downloaded");
                        } else {
                            MyLog.d(TAG, "No profile data found");
                        }

                        callback.onProgress(30, "Downloading workouts...");

                        // Загружаем тренировки
                        Map<String, Object> finalProfileData = profileData;
                        db.collection(COLLECTION_USERS)
                                .document(userId)
                                .collection(COLLECTION_WORKOUTS)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        List<Workout> workouts = new ArrayList<>();

                                        int total = queryDocumentSnapshots.size();
                                        int loaded = 0;

                                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                            try {
                                                String exerciseType = doc.getString("exerciseType");
                                                int totalReps = doc.getLong("totalReps").intValue();
                                                int goodReps = doc.getLong("goodReps").intValue();
                                                int timeInSeconds = doc.getLong("timeInSeconds").intValue();
                                                int accuracy = doc.getLong("accuracy").intValue();
                                                long timestamp = doc.getLong("timestamp");
                                                String date = doc.getString("date");

                                                Workout workout = new Workout(
                                                        exerciseType,
                                                        totalReps,
                                                        goodReps,
                                                        timeInSeconds,
                                                        accuracy,
                                                        timestamp,
                                                        date
                                                );

                                                workouts.add(workout);

                                                loaded++;
                                                if (loaded % 10 == 0 || loaded == total) {
                                                    int progress = 30 + ((loaded * 65) / total);
                                                    callback.onProgress(progress,
                                                            "Downloaded " + loaded + "/" + total + " workouts");
                                                }

                                            } catch (Exception e) {
                                                MyLog.e(TAG, "Error parsing workout", e);
                                            }
                                        }

                                        MyLog.d(TAG, "Download successful: " + workouts.size() + " workouts");
                                        callback.onSuccess(workouts, finalProfileData);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        MyLog.e(TAG, "Failed to download workouts", e);
                                        callback.onFailure("Failed to download workouts: " + e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        MyLog.e(TAG, "Failed to download profile", e);
                        callback.onFailure("Failed to download profile: " + e.getMessage());
                    }
                });
    }

    /**
     * Получить информацию о последней синхронизации
     * СЧИТАЕТ РЕАЛЬНОЕ количество workout-ов в коллекции
     */
    public void getSyncInfo(SyncInfoCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = getUserId();

        MyLog.d(TAG, "Getting sync info for user: " + userId);

        // Получаем метаданные для lastSyncTime
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILE)
                .document(DOC_SYNC_METADATA)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot metadataDoc) {
                        long lastSyncTime = 0;

                        if (metadataDoc.exists()) {
                            lastSyncTime = metadataDoc.getLong("lastSyncTime");
                            MyLog.d(TAG, "Metadata found, lastSyncTime: " + lastSyncTime);
                        } else {
                            MyLog.d(TAG, "No metadata found");
                        }

                        final long finalLastSyncTime = lastSyncTime;

                        db.collection(COLLECTION_USERS)
                                .document(userId)
                                .collection(COLLECTION_WORKOUTS)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot workoutsSnapshot) {
                                        int realWorkoutCount = workoutsSnapshot.size();

                                        MyLog.d(TAG, "Real workout count in Firestore: " + realWorkoutCount);

                                        callback.onSuccess(finalLastSyncTime, realWorkoutCount);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        MyLog.e(TAG, "Failed to count workouts", e);
                                        callback.onSuccess(finalLastSyncTime, 0);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        MyLog.e(TAG, "Failed to get metadata", e);

                        // Если нет метаданных, посчитаем workout-ы
                        db.collection(COLLECTION_USERS)
                                .document(userId)
                                .collection(COLLECTION_WORKOUTS)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot workoutsSnapshot) {
                                        int realWorkoutCount = workoutsSnapshot.size();

                                        MyLog.d(TAG, "No metadata, but counted workouts: " + realWorkoutCount);

                                        callback.onSuccess(0, realWorkoutCount);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e2) {
                                        MyLog.e(TAG, "Failed to count workouts (no metadata)", e2);
                                        callback.onFailure("Failed to get sync info: " + e2.getMessage());
                                    }
                                });
                    }
                });
    }

    /**
     * Удалить все данные пользователя
     */
    public void deleteAllUserData(OnSuccessListener<Void> callback) {
        if (!isUserLoggedIn()) {
            return;
        }

        String userId = getUserId();
        MyLog.d(TAG, "Deleting all data for user: " + userId);

        // Удаляем документ пользователя
        db.collection(COLLECTION_USERS)
                .document(userId)
                .delete()
                .addOnSuccessListener(callback)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        MyLog.e(TAG, "Failed to delete user data", e);
                    }
                });
    }

    /**
     * Форматировать относительное время
     */
    public static String formatRelativeTime(long timestamp) {
        if (timestamp == 0) {
            return "Never";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return days / 7 + " week" + (days / 7 > 1 ? "s" : "") + " ago";
        }
    }
}
