package am.arthur.arcoach.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import am.arthur.arcoach.R;

public class PreWorkoutActivity extends BaseActivity {

    private TextView tvExerciseName;
    private TextView tvTitle;
    private TextView tvInstructions;
    private Button btnStartWorkout;
    private String exerciseType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_workout);

        exerciseType = getIntent().getStringExtra("EXERCISE_TYPE");

        if (exerciseType == null) {
            throw new IllegalStateException("ExerciseType is null");
        }

        if (exerciseType.equals("SQUATS")){
            setupBackButton(R.string.exercise_squats);
        } else if (exerciseType.equals("PUSHUPS")) {
            setupBackButton(R.string.exercise_pushups);
        } else if (exerciseType.equals("JUMPING_JACKS")) {
            setupBackButton(R.string.exercise_jumping_jacks);
        } else if (exerciseType.equals("PLANK")) {
            setupBackButton(R.string.exercise_plank);
        }

        // инициализация
        tvExerciseName = findViewById(R.id.tv_exercise_name);
        tvTitle = findViewById(R.id.tv_title);
        tvInstructions = findViewById(R.id.tv_instructions);
        btnStartWorkout = findViewById(R.id.btn_start_workout);

        setupExerciseInfo();
        setupClickListeners();
    }

    private void setupExerciseInfo() {
        if (exerciseType.equals("SQUATS")) {
            tvExerciseName.setText(R.string.exercise_squats);
            tvInstructions.setText(R.string.instructions_squats);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.exercise_squats);
            }
        } else if (exerciseType.equals("PUSHUPS")) {
            tvExerciseName.setText(R.string.exercise_pushups);
            tvInstructions.setText(R.string.instructions_pushups);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.exercise_pushups);
            }
        } else if (exerciseType.equals("JUMPING_JACKS")) {
            tvExerciseName.setText(R.string.exercise_jumping_jacks);
            tvInstructions.setText(R.string.instructions_jumping_jacks);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.exercise_jumping_jacks);
            }
        } else if (exerciseType.equals("PLANK")) {
            tvExerciseName.setText(R.string.exercise_plank);
            tvInstructions.setText(R.string.instructions_plank);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.exercise_plank);
            }
        }

        tvTitle.setText(R.string.how_to_prepare);
    }

    private void setupClickListeners() {
        btnStartWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PreWorkoutActivity.this, WorkoutActivity.class);
                intent.putExtra("EXERCISE_TYPE", exerciseType);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}