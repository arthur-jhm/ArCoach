package am.arthur.arcoach.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import am.arthur.arcoach.R;
import am.arthur.arcoach.adapters.AvatarAdapter;
import am.arthur.arcoach.utils.UserPreferences;

public class EditProfileActivity extends BaseActivity {

    private TextView tvCurrentAvatar;
    private EditText etUserName;
    private Button btnSelectDate;
    private EditText etHeight;
    private EditText etWeight;
    private RecyclerView rvAvatars;
    private Button btnSave;
    private Button btnCancel;

    private UserPreferences userPreferences;
    private AvatarAdapter avatarAdapter;
    private String selectedAvatar;
    private String selectedBirthDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        setupBackButton(R.string.edit_profile);

        userPreferences = new UserPreferences(this);

        initViews();
        loadCurrentProfile();
        setupAvatarGrid();
        setupClickListeners();
    }

    private void initViews() {
        tvCurrentAvatar = findViewById(R.id.tv_current_avatar);
        etUserName = findViewById(R.id.et_user_name);
        btnSelectDate = findViewById(R.id.btn_select_date);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        rvAvatars = findViewById(R.id.rv_avatars);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void loadCurrentProfile() {
        String currentName = userPreferences.getUserName(getString(R.string.user_name));
        String currentAvatar = userPreferences.getUserAvatar("💪");
        String currentBirthDate = userPreferences.getBirthDate();
        int currentHeight = userPreferences.getHeight();
        float currentWeight = userPreferences.getWeight();

        etUserName.setText(currentName);
        tvCurrentAvatar.setText(currentAvatar);
        selectedAvatar = currentAvatar;
        selectedBirthDate = currentBirthDate;

        if (!currentBirthDate.isEmpty()) {
            btnSelectDate.setText(currentBirthDate);
        }

        if (currentHeight > 0) {
            etHeight.setText(String.valueOf(currentHeight));
        }

        if (currentWeight > 0) {
            etWeight.setText(String.format("%.1f", currentWeight));
        }
    }

    private void setupAvatarGrid() {
        List<String> avatars = new ArrayList<>();
        avatars.add("💪");
        avatars.add("🏋️");
        avatars.add("🤸");
        avatars.add("🏃");
        avatars.add("🚴");
        avatars.add("🧘");
        avatars.add("⚡");
        avatars.add("🔥");
        avatars.add("🎯");
        avatars.add("🏆");
        avatars.add("⭐");
        avatars.add("🚀");
        avatars.add("👤");
        avatars.add("😎");
        avatars.add("🦸");
        avatars.add("🥇");

        avatarAdapter = new AvatarAdapter(avatars, selectedAvatar, new AvatarAdapter.OnAvatarClickListener() {
            @Override
            public void onAvatarClick(String avatar) {
                selectedAvatar = avatar;
                tvCurrentAvatar.setText(avatar);
            }
        });

        rvAvatars.setLayoutManager(new GridLayoutManager(this, 4));
        rvAvatars.setAdapter(avatarAdapter);
    }

    private void setupClickListeners() {
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        if (!selectedBirthDate.isEmpty()) {
            try {
                String[] parts = selectedBirthDate.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1;
                day = Integer.parseInt(parts[2]);
            } catch (Exception e) {
                // текущая дата
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // формат YYYY-MM-DD
                        selectedBirthDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        btnSelectDate.setText(selectedBirthDate);
                    }
                },
                year, month, day
        );

        // минимум 10 лет назад, максимум 100 лет назад
        calendar.add(Calendar.YEAR, -10);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        calendar.add(Calendar.YEAR, -90); // -10 + (-90) = -100
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private void saveProfile() {
        String newName = etUserName.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (newName.isEmpty()) {
            etUserName.setError(getString(R.string.name_cannot_be_empty));
            return;
        }

        // основные данные
        userPreferences.setUserName(newName);
        userPreferences.setUserAvatar(selectedAvatar);

        // дата рождения
        if (!selectedBirthDate.isEmpty()) {
            userPreferences.setBirthDate(selectedBirthDate);
        }

        // рост
        if (!heightStr.isEmpty()) {
            try {
                int height = Integer.parseInt(heightStr);
                if (height >= 50 && height <= 250) {
                    userPreferences.setHeight(height);
                } else {
                    etHeight.setError(getString(R.string.invalid_height));
                    return;
                }
            } catch (NumberFormatException e) {
                etHeight.setError(getString(R.string.invalid_number));
                return;
            }
        } else {
            userPreferences.setHeight(0);
        }

        // вес
        if (!weightStr.isEmpty()) {
            try {
                float weight = Float.parseFloat(weightStr.replace(",", "."));
                if (weight >= 20 && weight <= 300) {
                    userPreferences.setWeight(weight);
                } else {
                    etWeight.setError(getString(R.string.invalid_weight));
                    return;
                }
            } catch (NumberFormatException e) {
                etWeight.setError(getString(R.string.invalid_number));
                return;
            }
        } else {
            userPreferences.setWeight(0);
        }

        Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
