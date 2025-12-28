package am.arthur.arcoach.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import am.arthur.arcoach.R;
import am.arthur.arcoach.adapters.AchievementAdapter;
import am.arthur.arcoach.database.WorkoutRepository;
import am.arthur.arcoach.utils.AchievementManager;

public class AchievementsActivity extends BaseActivity {

    private RecyclerView rvAchievements;
    private TextView tvSummary;
    private TextView tvPlaceholder;
    private WorkoutRepository repository;
    private AchievementManager achievementManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        setupBackButton(R.string.achievements_title0);

        repository = new WorkoutRepository(this);
        achievementManager = new AchievementManager(this);

        initViews();
        loadAchievements();
    }

    private void initViews() {
        rvAchievements = findViewById(R.id.rv_achievements);
        tvSummary = findViewById(R.id.tv_summary);
        tvPlaceholder = findViewById(R.id.tv_placeholder);

        rvAchievements.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadAchievements() {
        repository.getAchievementStats(new WorkoutRepository.OnAchievementStatsLoadedListener() {
            @Override
            public void onLoaded(int totalWorkouts, int totalReps, int totalTime, int streak,
                                 int squatReps, int pushupReps, int jumpingJackReps, int plankSeconds,
                                 int highAccuracySessions, int maxSingleSessionReps) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // все достижения со статусом
                        List<AchievementManager.AchievementItem> achievements =
                                achievementManager.getAllAchievements(totalWorkouts, totalReps, streak,
                                        squatReps, pushupReps, jumpingJackReps, plankSeconds,
                                        highAccuracySessions, totalTime, maxSingleSessionReps);

                        // разблокированные
                        int unlockedCount = 0;
                        for (AchievementManager.AchievementItem a : achievements) {
                            if (a.isUnlocked) unlockedCount++;
                        }

                        if (totalWorkouts == 0) {
                            // placeholder если ещё нет тренировок
                            tvPlaceholder.setVisibility(View.VISIBLE);
                            rvAchievements.setVisibility(View.GONE);
                            tvSummary.setVisibility(View.GONE);
                        } else {
                            tvPlaceholder.setVisibility(View.GONE);
                            rvAchievements.setVisibility(View.VISIBLE);
                            tvSummary.setVisibility(View.VISIBLE);

                            tvSummary.setText(getString(R.string.achievements_unlocked_summary,
                                    unlockedCount, achievements.size()));

                            AchievementAdapter adapter = new AchievementAdapter(
                                    buildListWithHeaders(achievements));
                            rvAchievements.setAdapter(adapter);
                        }
                    }
                });
            }
        });
    }

    private List<Object> buildListWithHeaders(List<AchievementManager.AchievementItem> achievements) {
        List<Object> items = new ArrayList<>();
        String lastCategory = null;
        for (AchievementManager.AchievementItem item : achievements) {
            String category = getCategoryKey(item.id);
            if (!category.equals(lastCategory)) {
                items.add(getCategoryTitle(category));
                lastCategory = category;
            }
            items.add(item);
        }
        return items;
    }

    private String getCategoryKey(String id) {
        if (id.startsWith("FIRST_"))         return "FIRST";
        if (id.startsWith("WORKOUTS_"))      return "WORKOUTS";
        if (id.startsWith("REPS_"))          return "REPS";
        if (id.startsWith("STREAK_"))        return "STREAK";
        if (id.startsWith("SQUATS_"))        return "SQUATS";
        if (id.startsWith("PUSHUPS_"))       return "PUSHUPS";
        if (id.startsWith("JUMPING_JACKS_")) return "JUMPING_JACKS";
        if (id.startsWith("PLANK_"))         return "PLANK";
        if (id.startsWith("ACCURACY_"))      return "ACCURACY";
        if (id.startsWith("TIME_"))          return "TIME";
        if (id.startsWith("SESSION_"))       return "SESSION";
        return "OTHER";
    }

    private String getCategoryTitle(String category) {
        switch (category) {
            case "FIRST":         return getString(R.string.category_first_steps);
            case "WORKOUTS":      return getString(R.string.category_workouts);
            case "REPS":          return getString(R.string.category_reps);
            case "STREAK":        return getString(R.string.category_streak);
            case "SQUATS":        return getString(R.string.category_squats);
            case "PUSHUPS":       return getString(R.string.category_pushups);
            case "JUMPING_JACKS": return getString(R.string.category_jumping_jacks);
            case "PLANK":         return getString(R.string.category_plank);
            case "ACCURACY":      return getString(R.string.category_accuracy);
            case "TIME":          return getString(R.string.category_time);
            case "SESSION":       return getString(R.string.category_session);
            default:              return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
