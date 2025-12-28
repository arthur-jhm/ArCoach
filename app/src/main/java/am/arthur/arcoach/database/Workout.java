package am.arthur.arcoach.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workouts")
public class Workout {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String exerciseType;
    private int totalReps;
    private int goodReps;
    private int timeInSeconds;
    private int accuracy;
    private long timestamp;
    private String date; // дата в формате "2025-12-28"

    public Workout(String exerciseType, int totalReps, int goodReps, int timeInSeconds, int accuracy, long timestamp, String date) {
        this.exerciseType = exerciseType;
        this.totalReps = totalReps;
        this.goodReps = goodReps;
        this.timeInSeconds = timeInSeconds;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.date = date;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getExerciseType() {
        return exerciseType;
    }
    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public int getTotalReps() {
        return totalReps;
    }
    public void setTotalReps(int totalReps) {
        this.totalReps = totalReps;
    }

    public int getGoodReps() {
        return goodReps;
    }
    public void setGoodReps(int goodReps) {
        this.goodReps = goodReps;
    }

    public int getTimeInSeconds() {
        return timeInSeconds;
    }
    public void setTimeInSeconds(int timeInSeconds) {
        this.timeInSeconds = timeInSeconds;
    }

    public int getAccuracy() {
        return accuracy;
    }
    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
}
