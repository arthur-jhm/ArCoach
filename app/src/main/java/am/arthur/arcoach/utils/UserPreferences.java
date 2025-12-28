package am.arthur.arcoach.utils;

import android.content.Context;
import android.content.SharedPreferences;

import am.arthur.arcoach.R;

public class UserPreferences {

    private static final String PREF_NAME = "ArCoachPreferences";

    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_BIRTH_DATE = "birth_date";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_WEIGHT = "weight";

    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_REMINDER_TIME = "reminder_time";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_BACK_CAMERA = "back_camera";



    private SharedPreferences preferences;

    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setUserName(String name) {
        preferences.edit().putString(KEY_USER_NAME, name).apply();
    }
    public String getUserName(String defaultName) {
        return preferences.getString(KEY_USER_NAME, defaultName);
    }

    public void setUserAvatar(String avatar) {
        preferences.edit().putString(KEY_USER_AVATAR, avatar).apply();
    }
    public String getUserAvatar(String defaultAvatar) {
        return preferences.getString(KEY_USER_AVATAR, defaultAvatar);
    }

    // Дата рождения (формат: YYYY-MM-DD)
    public void setBirthDate(String birthDate) {
        preferences.edit().putString(KEY_BIRTH_DATE, birthDate).apply();
    }
    public String getBirthDate() {
        return preferences.getString(KEY_BIRTH_DATE, "");
    }

    // Рост (см)
    public void setHeight(int height) {
        preferences.edit().putInt(KEY_HEIGHT, height).apply();
    }
    public int getHeight() {
        return preferences.getInt(KEY_HEIGHT, 0);
    }

    // Вес (кг)
    public void setWeight(float weight) {
        preferences.edit().putFloat(KEY_WEIGHT, weight).apply();
    }
    public float getWeight() {
        return preferences.getFloat(KEY_WEIGHT, 0f);
    }

    // Вычисление возраста
    public int getAge() {
        String birthDate = getBirthDate();
        if (birthDate.isEmpty()) return 0;

        try {
            String[] parts = birthDate.split("-");
            int birthYear = Integer.parseInt(parts[0]);

            java.util.Calendar today = java.util.Calendar.getInstance();
            int currentYear = today.get(java.util.Calendar.YEAR);

            return currentYear - birthYear;
        } catch (Exception e) {
            return 0;
        }
    }

    // Вычисление BMI (Body Mass Index)
    public float getBMI() {
        int height = getHeight();
        float weight = getWeight();

        if (height == 0 || weight == 0) return 0f;

        float heightInMeters = height / 100f;
        return weight / (heightInMeters * heightInMeters);
    }

    // Категория BMI
    public String getBMICategory(Context context) {
        float bmi = getBMI();

        if (bmi == 0) return "";
        if (bmi < 18.5) return context.getString(R.string.bmi_underweight);
        if (bmi < 25) return context.getString(R.string.bmi_normal);
        if (bmi < 30) return context.getString(R.string.bmi_overweight);
        return context.getString(R.string.bmi_obese);
    }


    // Голосовые подсказки
    public void setVoiceEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply();
    }
    public boolean isVoiceEnabled() {
        return preferences.getBoolean(KEY_VOICE_ENABLED, true);
    }

    // Вибрация
    public void setVibrationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }
    public boolean isVibrationEnabled() {
        return preferences.getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    // Уведомления
    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }
    public boolean isNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    // Громкость (0-100)
    public void setVolume(int volume) {
        preferences.edit().putInt(KEY_VOLUME, volume).apply();
    }
    public int getVolume() {
        return preferences.getInt(KEY_VOLUME, 80);
    }

    // Время напоминания (формат: HH:mm)
    public void setReminderTime(String time) {
        preferences.edit().putString(KEY_REMINDER_TIME, time).apply();
    }
    public String getReminderTime() {
        return preferences.getString(KEY_REMINDER_TIME, "18:00");
    }

    // Напоминания включены
    public void setReminderEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }
    public boolean isReminderEnabled() {
        return preferences.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    // Выбор камеры
    public void setBackCameraSelected(boolean isBack) {
        preferences.edit().putBoolean(KEY_BACK_CAMERA, isBack).apply();
    }
    public boolean isBackCameraSelected() {
        return preferences.getBoolean(KEY_BACK_CAMERA, true);
    }

    /**
     * Получить прямой доступ к SharedPreferences
     * (для дополнительных настроек в других классах)
     */
    public SharedPreferences getSharedPreferences() {
        return preferences;
    }
}