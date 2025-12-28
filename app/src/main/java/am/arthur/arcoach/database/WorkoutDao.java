package am.arthur.arcoach.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert
    void insert(Workout workout);

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    List<Workout> getAllWorkouts();

    @Query("SELECT * FROM workouts WHERE date = :date")
    List<Workout> getWorkoutsByDate(String date);

    @Query("SELECT * FROM workouts WHERE exerciseType = :exerciseType ORDER BY timestamp DESC")
    List<Workout> getWorkoutsByType(String exerciseType);

    @Query("SELECT COUNT(*) FROM workouts")
    int getTotalWorkoutsCount();

    @Query("SELECT SUM(totalReps) FROM workouts")
    int getTotalReps();

    @Query("SELECT SUM(timeInSeconds) FROM workouts")
    int getTotalTimeInSeconds();

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC LIMIT 1")
    Workout getLastWorkout();

    @Query("SELECT * FROM workouts WHERE exerciseType = :exerciseType ORDER BY totalReps DESC LIMIT 1")
    Workout getBestWorkoutByType(String exerciseType);

    @Query("SELECT AVG(accuracy) FROM workouts WHERE exerciseType = :exerciseType")
    float getAverageAccuracyByType(String exerciseType);

    @Query("DELETE FROM workouts")
    void deleteAll();

    // тренировки за последние N дней
    @Query("SELECT * FROM workouts WHERE timestamp >= :timestampFrom ORDER BY timestamp DESC")
    List<Workout> getWorkoutsSince(long timestampFrom);

    // количество тренировок за сегодня
    @Query("SELECT COUNT(*) FROM workouts WHERE date = :date")
    int getTodayWorkoutsCount(String date);

    @Query("SELECT * FROM workouts ORDER BY date DESC")
    List<Workout> getAllWorkoutsSync();

    @Query("SELECT SUM(totalReps) FROM workouts WHERE exerciseType = :exerciseType")
    int getTotalRepsByType(String exerciseType);

    @Query("SELECT COUNT(*) FROM workouts WHERE accuracy >= :minAccuracy")
    int getHighAccuracyWorkoutsCount(int minAccuracy);

    @Query("SELECT MAX(totalReps) FROM workouts")
    int getMaxRepsInSingleSession();
}
