package am.arthur.arcoach.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import am.arthur.arcoach.R;
import am.arthur.arcoach.database.WorkoutRepository;

public class ResultActivity extends BaseActivity {

    private TextView tvExerciseName;
    private TextView tvResultReps;
    private TextView tvGoodReps;
    private TextView tvResultTime;
    private TextView tvResultAccuracy;
    private TextView tvProgress;
    private TextView tvAchievement;
    private CardView cardAchievements;
    private Button btnTryAgain;
    private Button btnGoHome;

    private String exerciseType;
    private int totalReps;
    private int goodReps;
    private int timeInSeconds;
    private int accuracy;

    private WorkoutRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        setupBackButton(R.string.results_title);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getDataFromIntent();
        initViews();
        repository = new WorkoutRepository(this);
        displayResults();
        setupClickListeners();
    }

    private void getDataFromIntent() {
        exerciseType = getIntent().getStringExtra("EXERCISE_TYPE");
        totalReps = getIntent().getIntExtra("REPS", 0);
        goodReps = getIntent().getIntExtra("GOOD_REPS", 0);
        timeInSeconds = getIntent().getIntExtra("TIME", 0);
        accuracy = getIntent().getIntExtra("ACCURACY", 0);
    }

    private void initViews() {
        tvExerciseName = findViewById(R.id.tv_exercise_name);
        tvResultReps = findViewById(R.id.tv_result_reps);
        tvGoodReps = findViewById(R.id.tv_good_reps);
        tvResultTime = findViewById(R.id.tv_result_time);
        tvResultAccuracy = findViewById(R.id.tv_result_accuracy);
        tvProgress = findViewById(R.id.tv_progress);
        tvAchievement = findViewById(R.id.tv_achievement);
        cardAchievements = findViewById(R.id.card_achievements);
        btnTryAgain = findViewById(R.id.btn_try_again);
        btnGoHome = findViewById(R.id.btn_go_home);
    }

    private void displayResults() {
        if (exerciseType.equals("SQUATS")) {
            tvExerciseName.setText(R.string.exercise_squats);
        } else if (exerciseType.equals("PUSHUPS")) {
            tvExerciseName.setText(R.string.exercise_pushups);
        } else if (exerciseType.equals("JUMPING_JACKS")) {
            tvExerciseName.setText(R.string.exercise_jumping_jacks);
        } else if (exerciseType.equals("PLANK")) {
            tvExerciseName.setText(R.string.exercise_plank);
            tvResultReps.setText(totalReps + " sec");
        }

        // результаты
        tvResultReps.setText(String.valueOf(totalReps));
        tvGoodReps.setText(String.valueOf(goodReps));

        // время
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        tvResultTime.setText(String.format("%d:%02d", minutes, seconds));

        tvResultAccuracy.setText(accuracy + "%");

        showProgress();
        checkAchievements();
    }

    private void showProgress() {
        repository.getBestWorkout(exerciseType, new WorkoutRepository.OnBestWorkoutLoadedListener() {
            @Override
            public void onLoaded(am.arthur.arcoach.database.Workout bestWorkout) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bestWorkout != null && bestWorkout.getTotalReps() < totalReps) {
                            // новый рекорд
                            int improvement = totalReps - bestWorkout.getTotalReps();
                            tvProgress.setText("🎉 New record! +" + improvement + " reps!");
                            tvProgress.setVisibility(View.VISIBLE);
                        } else if (bestWorkout != null && bestWorkout.getTotalReps() > totalReps) {
                            // не рекорд, но показываем мотивацию
                            int diff = bestWorkout.getTotalReps() - totalReps;
                            tvProgress.setText("Your best: " + bestWorkout.getTotalReps() + " reps. Keep going!");
                            tvProgress.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }

    private void checkAchievements() {
        SharedPreferences shown = getSharedPreferences("achievements_shown", MODE_PRIVATE);

        repository.getStatistics(new WorkoutRepository.OnStatisticsLoadedListener() {
            @Override
            public void onLoaded(int totalWorkouts, int dbTotalReps, int totalTime, int streak) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String newId = null;
                        String newText = null;

                        if (totalWorkouts >= 1 && !shown.getBoolean("FIRST_WORKOUT", false)) {
                            newId = "FIRST_WORKOUT";
                            newText = getString(R.string.achievement_first_workout_title) + "! 🎯";
                        } else if (dbTotalReps >= 10 && !shown.getBoolean("FIRST_10_REPS", false)) {
                            newId = "FIRST_10_REPS";
                            newText = getString(R.string.achievement_first_10_reps_title) + "! 💪";
                        } else if (totalWorkouts >= 10 && !shown.getBoolean("WORKOUTS_10", false)) {
                            newId = "WORKOUTS_10";
                            newText = getString(R.string.achievement_10_workouts_title) + "! 🏋️";
                        } else if (totalWorkouts >= 50 && !shown.getBoolean("WORKOUTS_50", false)) {
                            newId = "WORKOUTS_50";
                            newText = getString(R.string.achievement_50_workouts_title) + "! 🔥";
                        } else if (totalWorkouts >= 100 && !shown.getBoolean("WORKOUTS_100", false)) {
                            newId = "WORKOUTS_100";
                            newText = getString(R.string.achievement_100_workouts_title) + "! ⭐";
                        } else if (dbTotalReps >= 100 && !shown.getBoolean("REPS_100", false)) {
                            newId = "REPS_100";
                            newText = getString(R.string.achievement_100_reps_title) + "! 🥉";
                        } else if (dbTotalReps >= 500 && !shown.getBoolean("REPS_500", false)) {
                            newId = "REPS_500";
                            newText = getString(R.string.achievement_500_reps_title) + "! 🥈";
                        } else if (dbTotalReps >= 1000 && !shown.getBoolean("REPS_1000", false)) {
                            newId = "REPS_1000";
                            newText = getString(R.string.achievement_1000_reps_title) + "! 🥇";
                        } else if (dbTotalReps >= 5000 && !shown.getBoolean("REPS_5000", false)) {
                            newId = "REPS_5000";
                            newText = getString(R.string.achievement_5000_reps_title) + "! 💎";
                        } else if (streak >= 3 && !shown.getBoolean("STREAK_3", false)) {
                            newId = "STREAK_3";
                            newText = getString(R.string.achievement_streak_3_title) + "! 🔥";
                        } else if (streak >= 7 && !shown.getBoolean("STREAK_7", false)) {
                            newId = "STREAK_7";
                            newText = getString(R.string.achievement_streak_7_title) + "! 🔥🔥";
                        } else if (streak >= 30 && !shown.getBoolean("STREAK_30", false)) {
                            newId = "STREAK_30";
                            newText = getString(R.string.achievement_streak_30_title) + "! 🔥🔥🔥";
                        } else if (streak >= 100 && !shown.getBoolean("STREAK_100", false)) {
                            newId = "STREAK_100";
                            newText = getString(R.string.achievement_streak_100_title) + "! 🚀";
                        } else if (accuracy >= 90 && !shown.getBoolean("ACCURACY_1", false)) {
                            newId = "ACCURACY_1";
                            newText = getString(R.string.achievement_accuracy_1_title) + "! ⭐";
                        }

                        if (newId != null) {
                            shown.edit().putBoolean(newId, true).apply();
                            tvAchievement.setText("🏆 " + newText);
                            cardAchievements.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }

    private void setupClickListeners() {
        btnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, PreWorkoutActivity.class);
                intent.putExtra("EXERCISE_TYPE", exerciseType);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        btnGoHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ResultActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
