package am.arthur.arcoach.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "achievements")
public class Achievement {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String achievementType; // "FIRST_10_SQUATS", "FIRST_50_PUSHUPS", "WEEK_STREAK" и т.д.
    private String title;
    private String description;
    private long unlockedTimestamp;
    private boolean isUnlocked;

    public Achievement(String achievementType, String title, String description, long unlockedTimestamp, boolean isUnlocked) {
        this.achievementType = achievementType;
        this.title = title;
        this.description = description;
        this.unlockedTimestamp = unlockedTimestamp;
        this.isUnlocked = isUnlocked;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getAchievementType() {
        return achievementType;
    }
    public void setAchievementType(String achievementType) {
        this.achievementType = achievementType;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public long getUnlockedTimestamp() {
        return unlockedTimestamp;
    }
    public void setUnlockedTimestamp(long unlockedTimestamp) {
        this.unlockedTimestamp = unlockedTimestamp;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }
    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }
}
