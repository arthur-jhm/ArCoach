package am.arthur.arcoach.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import am.arthur.arcoach.R;
import am.arthur.arcoach.activities.PreWorkoutActivity;
import am.arthur.arcoach.database.WorkoutRepository;

public class HomeFragment extends Fragment {

    private View cardSquats;
    private View cardPushups;
    private View cardJumpingJacks;
    private View cardPlank;
    private TextView tvStreak;
    private TextView tvTodayWorkouts;
    private TextView tvMotivation;

    private WorkoutRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        repository = new WorkoutRepository(requireContext());

        initViews(view);
        loadTodayStats();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        cardSquats = view.findViewById(R.id.card_squats);
        cardPushups = view.findViewById(R.id.card_pushups);
        cardJumpingJacks = view.findViewById(R.id.card_jumping_jacks);
        cardPlank = view.findViewById(R.id.card_plank);
        tvStreak = view.findViewById(R.id.tv_streak);
        tvTodayWorkouts = view.findViewById(R.id.tv_today_workouts);
        tvMotivation = view.findViewById(R.id.tv_motivation);
    }

    private void loadTodayStats() {
        // streak
        repository.getStatistics(new WorkoutRepository.OnStatisticsLoadedListener() {
            @Override
            public void onLoaded(int totalWorkouts, int totalReps, int totalTime, int streak) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        tvStreak.setText(getString(R.string.streak, streak));
                    }
                });
            }
        });

        // тренировки за сегодня
        repository.getTodayStatistics(new WorkoutRepository.OnTodayStatsLoadedListener() {
            @Override
            public void onLoaded(int todayWorkouts) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        tvTodayWorkouts.setText(getString(R.string.today_workouts, todayWorkouts));
                    }
                });
            }
        });

        // мотивационная фраза
        String[] motivations = {
                getString(R.string.motivation_1),
                getString(R.string.motivation_2),
                getString(R.string.motivation_3),
                getString(R.string.motivation_4),
                getString(R.string.motivation_5)
        };
        int random = (int) (Math.random() * motivations.length);
        tvMotivation.setText(motivations[random]);
    }

    private void setupClickListeners() {
        cardSquats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPreWorkout("SQUATS");
            }
        });

        cardPushups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPreWorkout("PUSHUPS");
            }
        });

        cardJumpingJacks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPreWorkout("JUMPING_JACKS");
            }
        });

        cardPlank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPreWorkout("PLANK");
            }
        });
    }

    private void openPreWorkout(String exerciseType) {
        Intent intent = new Intent(getActivity(), PreWorkoutActivity.class);
        intent.putExtra("EXERCISE_TYPE", exerciseType);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayStats();
    }
}
