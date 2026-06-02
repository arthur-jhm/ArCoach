package am.arthur.arcoach.database;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutRepository {

    private final WorkoutDao workoutDao;
    private final AchievementDao achievementDao;
    private final ExecutorService executorService;

    public WorkoutRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        workoutDao = database.workoutDao();
        achievementDao = database.achievementDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insertWorkout(Workout workout, OnWorkoutInsertedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                workoutDao.insert(workout);
                if (listener != null) {
                    listener.onInserted();
                }
            }
        });
    }

    public void getAllWorkouts(OnWorkoutsLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<Workout> workouts = workoutDao.getAllWorkouts();
                if (listener != null) {
                    listener.onLoaded(workouts);
                }
            }
        });
    }

    public void getStatistics(OnStatisticsLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                int totalWorkouts = workoutDao.getTotalWorkoutsCount();
                int totalReps = workoutDao.getTotalReps();
                int totalTime = workoutDao.getTotalTimeInSeconds();

                // streak (дни подряд)
                int streak = calculateStreak();

                if (listener != null) {
                    listener.onLoaded(totalWorkouts, totalReps, totalTime, streak);
                }
            }
        });
    }

    public void getTodayStatistics(OnTodayStatsLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String today = getCurrentDate();
                int todayWorkouts = workoutDao.getTodayWorkoutsCount(today);

                if (listener != null) {
                    listener.onLoaded(todayWorkouts);
                }
            }
        });
    }

    // streak (дни подряд)
    private int calculateStreak() {
        List<Workout> workouts = workoutDao.getAllWorkouts();
        if (workouts.isEmpty()) return 0;

        int streak = 1;
        String previousDate = workouts.get(0).getDate();

        for (int i = 1; i < workouts.size(); i++) {
            String currentDate = workouts.get(i).getDate();

            // Идут ли даты подряд
            if (isConsecutiveDay(currentDate, previousDate)) {
                streak++;
                previousDate = currentDate;
            } else {
                break;
            }
        }

        return streak;
    }

    private boolean isConsecutiveDay(String date1, String date2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);

            long diff = Math.abs(d2.getTime() - d1.getTime());
            long daysDiff = diff / (24 * 60 * 60 * 1000);

            return daysDiff == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void getBestWorkout(String exerciseType, OnBestWorkoutLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Workout best = workoutDao.getBestWorkoutByType(exerciseType);
                if (listener != null) {
                    listener.onLoaded(best);
                }
            }
        });
    }


    public void clearAllData(OnDataClearedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                workoutDao.deleteAll();
                achievementDao.deleteAll();
                if (listener != null) {
                    listener.onCleared();
                }
            }
        });
    }

    // Interfaces для callback'ов
    public interface OnWorkoutInsertedListener {
        void onInserted();
    }

    public interface OnWorkoutsLoadedListener {
        void onLoaded(List<Workout> workouts);
    }

    public interface OnStatisticsLoadedListener {
        void onLoaded(int totalWorkouts, int totalReps, int totalTime, int streak);
    }

    public interface OnTodayStatsLoadedListener {
        void onLoaded(int todayWorkouts);
    }

    public interface OnBestWorkoutLoadedListener {
        void onLoaded(Workout workout);
    }

    public interface OnAverageAccuracyLoadedListener {
        void onLoaded(float avgAccuracy);
    }

    public interface OnLastWorkoutLoadedListener {
        void onLoaded(Workout workout);
    }

    public interface OnDataClearedListener {
        void onCleared();
    }

    // Тренировки за последние N дней с группировкой по дням
    public void getWorkoutsForChart(int days, OnChartDataLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                long timestampFrom = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
                List<Workout> workouts = workoutDao.getWorkoutsSince(timestampFrom);

                if (listener != null) {
                    listener.onLoaded(workouts);
                }
            }
        });
    }

    // Статистику по типам упражнений
    public void getExerciseTypeStats(OnExerciseTypeStatsLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<Workout> allWorkouts = workoutDao.getAllWorkouts();

                int squatsCount = 0;
                int pushupsCount = 0;
                int jumpingJacksCount = 0;
                int plankCount = 0;

                for (Workout workout : allWorkouts) {
                    switch (workout.getExerciseType()) {
                        case "SQUATS":        squatsCount       += workout.getTotalReps(); break;
                        case "PUSHUPS":       pushupsCount      += workout.getTotalReps(); break;
                        case "JUMPING_JACKS": jumpingJacksCount += workout.getTotalReps(); break;
                        case "PLANK":         plankCount        += workout.getTotalReps(); break;
                    }
                }

                if (listener != null) {
                    listener.onLoaded(squatsCount, pushupsCount, jumpingJacksCount, plankCount);
                }
            }
        });
    }

    public void getAchievementStats(OnAchievementStatsLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                int totalWorkouts = workoutDao.getTotalWorkoutsCount();
                int totalReps = workoutDao.getTotalReps();
                int totalTime = workoutDao.getTotalTimeInSeconds();
                int streak = calculateStreak();
                int squatReps = workoutDao.getTotalRepsByType("SQUATS");
                int pushupReps = workoutDao.getTotalRepsByType("PUSHUPS");
                int jumpingJackReps = workoutDao.getTotalRepsByType("JUMPING_JACKS");
                int plankSeconds = workoutDao.getTotalRepsByType("PLANK");
                int highAccuracySessions = workoutDao.getHighAccuracyWorkoutsCount(80);
                int maxSingleSessionReps = workoutDao.getMaxRepsInSingleSession();

                if (listener != null) {
                    listener.onLoaded(totalWorkouts, totalReps, totalTime, streak,
                            squatReps, pushupReps, jumpingJackReps, plankSeconds,
                            highAccuracySessions, maxSingleSessionReps);
                }
            }
        });
    }

    public interface OnAchievementStatsLoadedListener {
        void onLoaded(int totalWorkouts, int totalReps, int totalTime, int streak,
                      int squatReps, int pushupReps, int jumpingJackReps, int plankSeconds,
                      int highAccuracySessions, int maxSingleSessionReps);
    }

    public interface OnChartDataLoadedListener {
        void onLoaded(List<Workout> workouts);
    }

    public interface OnExerciseTypeStatsLoadedListener {
        void onLoaded(int squatsCount, int pushupsCount, int jumpingJacksCount, int plankCount);
    }

    // Тренировки по типу
    public void getWorkoutsByType(String exerciseType, OnWorkoutsLoadedListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<Workout> workouts = workoutDao.getWorkoutsByType(exerciseType);
                if (listener != null) {
                    listener.onLoaded(workouts);
                }
            }
        });
    }

    /**
     * Получить все тренировки синхронно (для экспорта)
     */
    public List<Workout> getAllWorkoutsSync() {
        return workoutDao.getAllWorkoutsSync();
    }

    /**
     * Вставить тренировку синхронно (для импорта)
     */
    public void insertWorkoutSync(Workout workout) {
        workoutDao.insert(workout);
    }

    /**
     * Удалить ВСЕ тренировки (для полной замены при синхронизации)
     */
    public void deleteAllWorkoutsSync() {
        workoutDao.deleteAll();
    }
}
