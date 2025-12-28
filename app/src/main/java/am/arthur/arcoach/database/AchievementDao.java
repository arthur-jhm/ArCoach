package am.arthur.arcoach.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AchievementDao {

    @Insert
    void insert(Achievement achievement);

    @Update
    void update(Achievement achievement);

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedTimestamp DESC")
    List<Achievement> getUnlockedAchievements();

    @Query("SELECT * FROM achievements")
    List<Achievement> getAllAchievements();

    @Query("SELECT * FROM achievements WHERE achievementType = :type")
    Achievement getAchievementByType(String type);

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    int getUnlockedCount();

    @Query("SELECT COUNT(*) FROM achievements")
    int getTotalCount();

    @Query("DELETE FROM achievements")
    void deleteAll();
}
