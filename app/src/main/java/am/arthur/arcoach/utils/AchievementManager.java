package am.arthur.arcoach.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import am.arthur.arcoach.R;
import am.arthur.arcoach.database.WorkoutRepository;

public class AchievementManager {

    private final Context context;
    private final WorkoutRepository repository;

    public AchievementManager(Context context) {
        this.context = context;
        this.repository = new WorkoutRepository(context);
    }

    public List<AchievementItem> getAllAchievements(int totalWorkouts, int totalReps, int streak,
                                                    int squatReps, int pushupReps, int jumpingJackReps, int plankSeconds,
                                                    int highAccuracySessions, int totalTimeSeconds, int maxSingleSessionReps) {
        List<AchievementItem> achievements = new ArrayList<>();

        // КАТЕГОРИЯ: Первые шаги
        achievements.add(new AchievementItem("FIRST_WORKOUT", "🎯",
                context.getString(R.string.achievement_first_workout_title),
                context.getString(R.string.achievement_first_workout_desc),
                totalWorkouts >= 1,
                totalWorkouts,
                1
        ));

        achievements.add(new AchievementItem("FIRST_10_REPS", "💪",
                context.getString(R.string.achievement_first_10_reps_title),
                context.getString(R.string.achievement_first_10_reps_desc),
                totalReps >= 10,
                totalReps,
                10
        ));

        // КАТЕГОРИЯ: Тренировки
        achievements.add(new AchievementItem("WORKOUTS_10", "🏋️",
                context.getString(R.string.achievement_10_workouts_title),
                context.getString(R.string.achievement_10_workouts_desc),
                totalWorkouts >= 10,
                totalWorkouts,
                10
        ));

        achievements.add(new AchievementItem("WORKOUTS_25", "🏃",
                context.getString(R.string.achievement_25_workouts_title),
                context.getString(R.string.achievement_25_workouts_desc),
                totalWorkouts >= 25,
                totalWorkouts,
                25
        ));

        achievements.add(new AchievementItem("WORKOUTS_50", "🔥",
                context.getString(R.string.achievement_50_workouts_title),
                context.getString(R.string.achievement_50_workouts_desc),
                totalWorkouts >= 50,
                totalWorkouts,
                50
        ));

        achievements.add(new AchievementItem("WORKOUTS_100", "⭐",
                context.getString(R.string.achievement_100_workouts_title),
                context.getString(R.string.achievement_100_workouts_desc),
                totalWorkouts >= 100,
                totalWorkouts,
                100
        ));

        achievements.add(new AchievementItem("WORKOUTS_200", "🏆",
                context.getString(R.string.achievement_200_workouts_title),
                context.getString(R.string.achievement_200_workouts_desc),
                totalWorkouts >= 200,
                totalWorkouts,
                200
        ));

        achievements.add(new AchievementItem("WORKOUTS_500", "👑",
                context.getString(R.string.achievement_500_workouts_title),
                context.getString(R.string.achievement_500_workouts_desc),
                totalWorkouts >= 500,
                totalWorkouts,
                500
        ));

        // КАТЕГОРИЯ: Повторения
        achievements.add(new AchievementItem("REPS_100", "🥉",
                context.getString(R.string.achievement_100_reps_title),
                context.getString(R.string.achievement_100_reps_desc),
                totalReps >= 100,
                totalReps,
                100
        ));

        achievements.add(new AchievementItem("REPS_250", "🥈",
                context.getString(R.string.achievement_250_reps_title),
                context.getString(R.string.achievement_250_reps_desc),
                totalReps >= 250,
                totalReps,
                250
        ));

        achievements.add(new AchievementItem("REPS_500", "🥇",
                context.getString(R.string.achievement_500_reps_title),
                context.getString(R.string.achievement_500_reps_desc),
                totalReps >= 500,
                totalReps,
                500
        ));

        achievements.add(new AchievementItem("REPS_1000", "💎",
                context.getString(R.string.achievement_1000_reps_title),
                context.getString(R.string.achievement_1000_reps_desc),
                totalReps >= 1000,
                totalReps,
                1000
        ));

        achievements.add(new AchievementItem("REPS_2500", "🔱",
                context.getString(R.string.achievement_2500_reps_title),
                context.getString(R.string.achievement_2500_reps_desc),
                totalReps >= 2500,
                totalReps,
                2500
        ));

        achievements.add(new AchievementItem("REPS_5000", "🌟",
                context.getString(R.string.achievement_5000_reps_title),
                context.getString(R.string.achievement_5000_reps_desc),
                totalReps >= 5000,
                totalReps,
                5000
        ));

        achievements.add(new AchievementItem("REPS_10000", "🏅",
                context.getString(R.string.achievement_10000_reps_title),
                context.getString(R.string.achievement_10000_reps_desc),
                totalReps >= 10000,
                totalReps,
                10000
        ));

        // КАТЕГОРИЯ: Streak (дни подряд)
        achievements.add(new AchievementItem("STREAK_3", "🔥",
                context.getString(R.string.achievement_streak_3_title),
                context.getString(R.string.achievement_streak_3_desc),
                streak >= 3,
                streak,
                3
        ));

        achievements.add(new AchievementItem("STREAK_7", "⚡",
                context.getString(R.string.achievement_streak_7_title),
                context.getString(R.string.achievement_streak_7_desc),
                streak >= 7,
                streak,
                7
        ));

        achievements.add(new AchievementItem("STREAK_14", "💫",
                context.getString(R.string.achievement_streak_14_title),
                context.getString(R.string.achievement_streak_14_desc),
                streak >= 14,
                streak,
                14
        ));

        achievements.add(new AchievementItem("STREAK_30", "🌙",
                context.getString(R.string.achievement_streak_30_title),
                context.getString(R.string.achievement_streak_30_desc),
                streak >= 30,
                streak,
                30
        ));

        achievements.add(new AchievementItem("STREAK_60", "🌕",
                context.getString(R.string.achievement_streak_60_title),
                context.getString(R.string.achievement_streak_60_desc),
                streak >= 60,
                streak,
                60
        ));

        achievements.add(new AchievementItem("STREAK_100", "🚀",
                context.getString(R.string.achievement_streak_100_title),
                context.getString(R.string.achievement_streak_100_desc),
                streak >= 100,
                streak,
                100
        ));

        achievements.add(new AchievementItem("STREAK_365", "🏅",
                context.getString(R.string.achievement_streak_365_title),
                context.getString(R.string.achievement_streak_365_desc),
                streak >= 365,
                streak,
                365
        ));

        // КАТЕГОРИЯ: Приседания
        achievements.add(new AchievementItem("SQUATS_50", "🦵",
                context.getString(R.string.achievement_squats_50_title),
                context.getString(R.string.achievement_squats_50_desc),
                squatReps >= 50,
                squatReps,
                50
        ));

        achievements.add(new AchievementItem("SQUATS_500", "🏃",
                context.getString(R.string.achievement_squats_500_title),
                context.getString(R.string.achievement_squats_500_desc),
                squatReps >= 500,
                squatReps,
                500
        ));

        achievements.add(new AchievementItem("SQUATS_2000", "🏋️",
                context.getString(R.string.achievement_squats_2000_title),
                context.getString(R.string.achievement_squats_2000_desc),
                squatReps >= 2000,
                squatReps,
                2000
        ));

        // КАТЕГОРИЯ: Отжимания
        achievements.add(new AchievementItem("PUSHUPS_50", "👊",
                context.getString(R.string.achievement_pushups_50_title),
                context.getString(R.string.achievement_pushups_50_desc),
                pushupReps >= 50,
                pushupReps,
                50
        ));

        achievements.add(new AchievementItem("PUSHUPS_500", "💪",
                context.getString(R.string.achievement_pushups_500_title),
                context.getString(R.string.achievement_pushups_500_desc),
                pushupReps >= 500,
                pushupReps,
                500
        ));

        achievements.add(new AchievementItem("PUSHUPS_2000", "🏆",
                context.getString(R.string.achievement_pushups_2000_title),
                context.getString(R.string.achievement_pushups_2000_desc),
                pushupReps >= 2000,
                pushupReps,
                2000
        ));

        // КАТЕГОРИЯ: Прыжки
        achievements.add(new AchievementItem("JUMPING_JACKS_100", "🤸",
                context.getString(R.string.achievement_jacks_100_title),
                context.getString(R.string.achievement_jacks_100_desc),
                jumpingJackReps >= 100,
                jumpingJackReps,
                100
        ));

        achievements.add(new AchievementItem("JUMPING_JACKS_1000", "⚡",
                context.getString(R.string.achievement_jacks_1000_title),
                context.getString(R.string.achievement_jacks_1000_desc),
                jumpingJackReps >= 1000,
                jumpingJackReps,
                1000
        ));

        achievements.add(new AchievementItem("JUMPING_JACKS_5000", "🌪️",
                context.getString(R.string.achievement_jacks_5000_title),
                context.getString(R.string.achievement_jacks_5000_desc),
                jumpingJackReps >= 5000,
                jumpingJackReps,
                5000
        ));

        // КАТЕГОРИЯ: Планка (в секундах)
        achievements.add(new AchievementItem("PLANK_60", "🧘",
                context.getString(R.string.achievement_plank_60_title),
                context.getString(R.string.achievement_plank_60_desc),
                plankSeconds >= 60,
                plankSeconds,
                60
        ));

        achievements.add(new AchievementItem("PLANK_600", "💪",
                context.getString(R.string.achievement_plank_600_title),
                context.getString(R.string.achievement_plank_600_desc),
                plankSeconds >= 600,
                plankSeconds,
                600
        ));

        achievements.add(new AchievementItem("PLANK_3600", "🏅",
                context.getString(R.string.achievement_plank_3600_title),
                context.getString(R.string.achievement_plank_3600_desc),
                plankSeconds >= 3600,
                plankSeconds,
                3600
        ));

        // КАТЕГОРИЯ: Точность
        achievements.add(new AchievementItem("ACCURACY_1", "⭐",
                context.getString(R.string.achievement_accuracy_1_title),
                context.getString(R.string.achievement_accuracy_1_desc),
                highAccuracySessions >= 1,
                highAccuracySessions,
                1
        ));

        achievements.add(new AchievementItem("ACCURACY_5", "🌟",
                context.getString(R.string.achievement_accuracy_5_title),
                context.getString(R.string.achievement_accuracy_5_desc),
                highAccuracySessions >= 5,
                highAccuracySessions,
                5
        ));

        achievements.add(new AchievementItem("ACCURACY_20", "🌠",
                context.getString(R.string.achievement_accuracy_20_title),
                context.getString(R.string.achievement_accuracy_20_desc),
                highAccuracySessions >= 20,
                highAccuracySessions,
                20
        ));

        // КАТЕГОРИЯ: Суммарное время
        int totalTimeHours = totalTimeSeconds / 3600;
        achievements.add(new AchievementItem("TIME_1H", "⏱️",
                context.getString(R.string.achievement_time_1h_title),
                context.getString(R.string.achievement_time_1h_desc),
                totalTimeSeconds >= 3600,
                totalTimeHours,
                1
        ));

        achievements.add(new AchievementItem("TIME_10H", "⏰",
                context.getString(R.string.achievement_time_10h_title),
                context.getString(R.string.achievement_time_10h_desc),
                totalTimeSeconds >= 36000,
                totalTimeHours,
                10
        ));

        achievements.add(new AchievementItem("TIME_50H", "🕐",
                context.getString(R.string.achievement_time_50h_title),
                context.getString(R.string.achievement_time_50h_desc),
                totalTimeSeconds >= 180000,
                totalTimeHours,
                50
        ));

        // КАТЕГОРИЯ: Рекорд за одну сессию
        achievements.add(new AchievementItem("SESSION_50", "💥",
                context.getString(R.string.achievement_session_50_title),
                context.getString(R.string.achievement_session_50_desc),
                maxSingleSessionReps >= 50,
                maxSingleSessionReps,
                50
        ));

        achievements.add(new AchievementItem("SESSION_100", "🚀",
                context.getString(R.string.achievement_session_100_title),
                context.getString(R.string.achievement_session_100_desc),
                maxSingleSessionReps >= 100,
                maxSingleSessionReps,
                100
        ));

        return achievements;
    }

    public static class AchievementItem {
        public String id;
        public String icon;
        public String title;
        public String description;
        public boolean isUnlocked;
        public int currentProgress;
        public int targetProgress;

        public AchievementItem(String id, String icon, String title, String description,
                               boolean isUnlocked, int currentProgress, int targetProgress) {
            this.id = id;
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.isUnlocked = isUnlocked;
            this.currentProgress = currentProgress;
            this.targetProgress = targetProgress;
        }

        public int getProgressPercentage() {
            if (targetProgress == 0) return 0;
            int percentage = (currentProgress * 100) / targetProgress;
            return Math.min(percentage, 100);
        }
    }
}
