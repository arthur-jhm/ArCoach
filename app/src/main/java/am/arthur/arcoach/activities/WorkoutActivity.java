package am.arthur.arcoach.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

import am.arthur.arcoach.R;
import am.arthur.arcoach.database.Workout;
import am.arthur.arcoach.database.WorkoutRepository;
import am.arthur.arcoach.utils.OverlayView;
import am.arthur.arcoach.utils.PoseAnalyzer;
import am.arthur.arcoach.utils.UserPreferences;
import am.arthur.arcoach.utils.VoiceCoach;

public class WorkoutActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView tvRepCount;
    private TextView tvTimer;
    private TextView tvFeedback;
    private View qualityIndicator;
    private Button btnFinish;
    private ImageButton btnSwitchCamera;

    private String exerciseType;
    private PoseDetector poseDetector;
    private PoseAnalyzer poseAnalyzer;
    private VoiceCoach voiceCoach;
    private ExecutorService cameraExecutor;
    private Vibrator vibrator;

    private long startTime;
    private Handler timerHandler;
    private Runnable timerRunnable;

    private int lastRepCount = 0;
    private long lastVoiceTipTime = 0;
    private static final long VOICE_TIP_COOLDOWN_MS = 5000;

    private boolean isFrontCamera = false;
    private ProcessCameraProvider cameraProvider;
    private int cameraImageWidth = 0;
    private int currentDisplayRotation = -1;
    private DisplayManager displayManager;

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int displayId) {}
        @Override public void onDisplayRemoved(int displayId) {}
        @Override
        public void onDisplayChanged(int displayId) {
            int newRotation = getWindowManager().getDefaultDisplay().getRotation();
            if (currentDisplayRotation != newRotation && cameraProvider != null) {
                currentDisplayRotation = newRotation;
                cameraImageWidth = 0;
                bindCameraUseCases(cameraProvider);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        // Полноэкранный режим
        enableImmersiveMode();

        setContentView(R.layout.activity_workout);

        exerciseType = getIntent().getStringExtra("EXERCISE_TYPE");

        if ("PUSHUPS".equals(exerciseType) || "PLANK".equals(exerciseType)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        initViews();
        initMLKit();
        initVoiceCoach();
        initTimer();

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        currentDisplayRotation = getWindowManager().getDefaultDisplay().getRotation();
        displayManager.registerDisplayListener(displayListener,
                new Handler(Looper.getMainLooper()));

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWorkout();
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFrontCamera = !isFrontCamera;
                if (cameraProvider != null) {
                    bindCameraUseCases(cameraProvider);
                }
            }
        });
    }

    // Полноэкранный режим
    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            // Android 10 и ниже
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Восстановление полноэкранного режима при возврате
            enableImmersiveMode();
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        overlayView = findViewById(R.id.overlay_view);
        tvRepCount = findViewById(R.id.tv_rep_count);
        tvTimer = findViewById(R.id.tv_timer);
        tvFeedback = findViewById(R.id.tv_feedback);
        qualityIndicator = findViewById(R.id.quality_indicator);
        btnFinish = findViewById(R.id.btn_finish);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
    }

    private void initMLKit() {
        AccuratePoseDetectorOptions options = new AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .build();

        poseDetector = PoseDetection.getClient(options);
        poseAnalyzer = new PoseAnalyzer(this, exerciseType);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void initVoiceCoach() {
        UserPreferences prefs = new UserPreferences(this);

        if (prefs.isVoiceEnabled()) {
            voiceCoach = new VoiceCoach(this);

            int volume = prefs.getVolume();
            voiceCoach.setVolume(volume / 100f);  // Конвертируем 0-100 в 0.0-1.0
        } else {
            voiceCoach = null;
        }
    }

    private void initTimer() {
        startTime = System.currentTimeMillis();
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsed / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindCameraUseCases(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image Analysis для ML Kit
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(displayRotation)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @OptIn(markerClass = ExperimentalGetImage.class)
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                processImageProxy(imageProxy);
            }
        });

        CameraSelector cameraSelector = isFrontCamera
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;

        overlayView.setMirrored(isFrontCamera);
        cameraImageWidth = 0; // сбрасываем - прочитаем из первого кадра новой камеры

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                WorkoutActivity.this,
                cameraSelector,
                preview,
                imageAnalysis
        );
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        // ширина изображения в пространстве координат ML Kit
        if (cameraImageWidth == 0) {
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            int effectiveWidth = (rotation == 90 || rotation == 270)
                    ? imageProxy.getHeight()
                    : imageProxy.getWidth();
            cameraImageWidth = effectiveWidth;
            overlayView.setMirrorWidth(cameraImageWidth);
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        poseDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<Pose>() {
                    @Override
                    public void onSuccess(Pose pose) {
                        PoseAnalyzer.AnalysisResult result = poseAnalyzer.analyze(pose);

                        // Обновление UI
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI(result, pose);
                            }
                        });

                        imageProxy.close();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imageProxy.close();
                    }
                });
    }

    private void updateUI(PoseAnalyzer.AnalysisResult result, Pose pose) {
        tvRepCount.setText(String.valueOf(result.repCount));
        tvFeedback.setText(result.feedback);

        // индикатор качества
        if (result.qualityColor == Color.GREEN) {
            qualityIndicator.setBackgroundResource(R.drawable.circle_green);
        } else if (result.qualityColor == Color.YELLOW) {
            qualityIndicator.setBackgroundResource(R.drawable.circle_yellow);
        } else if (result.qualityColor == Color.RED) {
            qualityIndicator.setBackgroundResource(R.drawable.circle_red);
        }

        // скелет
        overlayView.setPose(pose, result.qualityColor);

        // голос и вибрация при новом повторении
        if (result.repCount > lastRepCount) {
            lastRepCount = result.repCount;

            // голосовые подсказки
            if (voiceCoach != null) {
                voiceCoach.announceRep(result.repCount);
            }

            // вибрация
            UserPreferences prefs = new UserPreferences(this);
            if (prefs.isVibrationEnabled() && vibrator != null) {
                vibrator.vibrate(100);
            }
        }

        // голосовая подсказка при плохой форме, 1 раз за 5 секунд
        if (voiceCoach != null && result.voiceTip != null) {
            long now = System.currentTimeMillis();
            if (now - lastVoiceTipTime >= VOICE_TIP_COOLDOWN_MS && !voiceCoach.isSpeaking()) {
                lastVoiceTipTime = now;
                voiceCoach.speak(result.voiceTip);
            }
        }
    }

    private void finishWorkout() {
        // Остановка таймера
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        // Вычисление времени
        long elapsed = System.currentTimeMillis() - startTime;
        int timeInSeconds = (int) (elapsed / 1000);

        // Вычисление точности
        int totalReps = poseAnalyzer.getRepCount();
        int goodReps = poseAnalyzer.getGoodReps();
        int accuracy = totalReps > 0 ? (goodReps * 100) / totalReps : 0;

        saveWorkoutToDatabase(exerciseType, totalReps, goodReps, timeInSeconds, accuracy);

        // Экран результатов
        Intent intent = new Intent(WorkoutActivity.this, ResultActivity.class);
        intent.putExtra("EXERCISE_TYPE", exerciseType);
        intent.putExtra("REPS", totalReps);
        intent.putExtra("TIME", timeInSeconds);
        intent.putExtra("ACCURACY", accuracy);
        intent.putExtra("GOOD_REPS", goodReps);
        startActivity(intent);
        finish();
    }

    private void saveWorkoutToDatabase(String exerciseType, int totalReps, int goodReps, int timeInSeconds, int accuracy) {
        WorkoutRepository repository = new WorkoutRepository(this);

        long timestamp = System.currentTimeMillis();
        String date = WorkoutRepository.getCurrentDate();

        Workout workout = new Workout(exerciseType, totalReps, goodReps, timeInSeconds, accuracy, timestamp, date);

        repository.insertWorkout(workout, new WorkoutRepository.OnWorkoutInsertedListener() {
            @Override
            public void onInserted() {
                // Тренировка сохранена
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }

        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        if (voiceCoach != null) {
            voiceCoach.shutdown();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        if (poseDetector != null) {
            poseDetector.close();
        }
    }
}
