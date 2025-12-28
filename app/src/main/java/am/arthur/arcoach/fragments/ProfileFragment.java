package am.arthur.arcoach.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import am.arthur.arcoach.R;
import am.arthur.arcoach.activities.AchievementsActivity;
import am.arthur.arcoach.activities.EditProfileActivity;
import am.arthur.arcoach.database.WorkoutRepository;
import am.arthur.arcoach.utils.AchievementManager;
import am.arthur.arcoach.utils.UserPreferences;

public class ProfileFragment extends Fragment {

    private TextView tvUserAvatar;
    private TextView tvUserName;
    private TextView tvUserLevel;
    private TextView tvUserRank;
    private TextView tvTotalAchievements;
    private CardView cardAchievements;

    private TextView tvAge;
    private TextView tvHeight;
    private TextView tvWeight;
    private TextView tvBMI;
    private LinearLayout layoutBMI;

    private Button btnEditProfile;

    private WorkoutRepository repository;
    private UserPreferences userPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        repository = new WorkoutRepository(requireContext());
        userPreferences = new UserPreferences(requireContext());

        initViews(view);
        loadProfile();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        tvUserAvatar = view.findViewById(R.id.tv_user_avatar);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserLevel = view.findViewById(R.id.tv_user_level);
        tvUserRank = view.findViewById(R.id.tv_user_rank);
        tvTotalAchievements = view.findViewById(R.id.tv_total_achievements);
        cardAchievements = view.findViewById(R.id.card_achievements);

        tvAge = view.findViewById(R.id.tv_age);
        tvHeight = view.findViewById(R.id.tv_height);
        tvWeight = view.findViewById(R.id.tv_weight);
        tvBMI = view.findViewById(R.id.tv_bmi);
        layoutBMI = view.findViewById(R.id.layout_bmi);

        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
    }

    private void loadProfile() {
        String userName = userPreferences.getUserName(getString(R.string.user_name));
        String userAvatar = userPreferences.getUserAvatar("💪");

        tvUserName.setText(userName);
        tvUserAvatar.setText(userAvatar);

        loadPersonalInfo();

        // Вычисление уровня на основе количества тренировок
        repository.getAchievementStats(new WorkoutRepository.OnAchievementStatsLoadedListener() {
            @Override
            public void onLoaded(int totalWorkouts, int totalReps, int totalTime, int streak,
                                 int squatReps, int pushupReps, int jumpingJackReps, int plankSeconds,
                                 int highAccuracySessions, int maxSingleSessionReps) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        int level = (totalWorkouts / 10) + 1;
                        tvUserLevel.setText(getString(R.string.user_level, level));

                        String rank;
                        if (level < 3) {
                            rank = getString(R.string.user_rank_beginner);
                        } else if (level < 6) {
                            rank = getString(R.string.user_rank_intermediate);
                        } else if (level < 10) {
                            rank = getString(R.string.user_rank_advanced);
                        } else {
                            rank = getString(R.string.user_rank_expert);
                        }
                        tvUserRank.setText(rank);

                        AchievementManager mgr = new AchievementManager(requireContext());
                        java.util.List<AchievementManager.AchievementItem> all =
                                mgr.getAllAchievements(totalWorkouts, totalReps, streak,
                                        squatReps, pushupReps, jumpingJackReps, plankSeconds,
                                        highAccuracySessions, totalTime, maxSingleSessionReps);

                        int unlocked = 0;
                        for (AchievementManager.AchievementItem a : all) {
                            if (a.isUnlocked) unlocked++;
                        }
                        tvTotalAchievements.setText(getString(R.string.achievements_count,
                                unlocked, all.size()));
                    }
                });
            }
        });
    }

    private void loadPersonalInfo() {
        // Возраст
        int age = userPreferences.getAge();
        if (age > 0) {
            tvAge.setText(age + " " + getString(R.string.years_old));
        } else {
            tvAge.setText("-");
        }

        // Рост
        int height = userPreferences.getHeight();
        if (height > 0) {
            tvHeight.setText(height + " " + getString(R.string.cm));
        } else {
            tvHeight.setText("-");
        }

        // Вес
        float weight = userPreferences.getWeight();
        if (weight > 0) {
            tvWeight.setText(String.format("%.1f %s", weight, getString(R.string.kg)));
        } else {
            tvWeight.setText("-");
        }

        // BMI
        float bmi = userPreferences.getBMI();
        if (bmi > 0) {
            String category = userPreferences.getBMICategory(requireContext());
            tvBMI.setText(String.format("%.1f (%s)", bmi, category));

            // цвет в зависимости от категории
            if (category.equals("Normal")) {
                tvBMI.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (category.equals("Underweight") || category.equals("Overweight")) {
                tvBMI.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvBMI.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }

            layoutBMI.setVisibility(View.VISIBLE);
        } else {
            layoutBMI.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
            }
        });

        cardAchievements.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AchievementsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }
}
