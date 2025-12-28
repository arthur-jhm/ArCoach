package am.arthur.arcoach.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import am.arthur.arcoach.R;
import am.arthur.arcoach.database.Workout;
import am.arthur.arcoach.database.WorkoutRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private TextView tvTotalWorkouts;
    private TextView tvTotalReps;
    private TextView tvTotalTime;
    private TextView tvStreak;

    private LinearLayout layoutSquatsBlock;
    private LinearLayout layoutPushupsBlock;
    private LinearLayout layoutJumpingJacksBlock;
    private LinearLayout layoutPlankBlock;
    private TextView tvExerciseDetailsPlaceholder;

    private TextView tvSquatsSessions;
    private TextView tvSquatsTotal;
    private TextView tvSquatsBest;
    private TextView tvSquatsAccuracy;
    private TextView tvPushupsSessions;
    private TextView tvPushupsTotal;
    private TextView tvPushupsBest;
    private TextView tvPushupsAccuracy;
    private TextView tvJumpingJacksSessions;
    private TextView tvJumpingJacksTotal;
    private TextView tvJumpingJacksBest;
    private TextView tvJumpingJacksAccuracy;
    private TextView tvPlankSessions;
    private TextView tvPlankTotal;
    private TextView tvPlankBest;
    private TextView tvPlankAccuracy;

    private BarChart barChart;
    private LineChart lineChart;
    private PieChart pieChart;

    private WorkoutRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        repository = new WorkoutRepository(requireContext());

        initViews(view);
        loadStatistics();
        loadCharts();
        loadExerciseDetails();

        return view;
    }

    private void initViews(View view) {
        tvTotalWorkouts = view.findViewById(R.id.tv_total_workouts);
        tvTotalReps = view.findViewById(R.id.tv_total_reps);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        tvStreak = view.findViewById(R.id.tv_streak);

        layoutSquatsBlock = view.findViewById(R.id.layout_squats_block);
        layoutPushupsBlock = view.findViewById(R.id.layout_pushups_block);
        layoutJumpingJacksBlock = view.findViewById(R.id.layout_jumping_jacks_block);
        layoutPlankBlock = view.findViewById(R.id.layout_plank_block);
        tvExerciseDetailsPlaceholder = view.findViewById(R.id.tv_exercise_details_placeholder);

        tvSquatsSessions = view.findViewById(R.id.tv_squats_sessions);
        tvSquatsTotal = view.findViewById(R.id.tv_squats_total);
        tvSquatsBest = view.findViewById(R.id.tv_squats_best);
        tvSquatsAccuracy = view.findViewById(R.id.tv_squats_accuracy);
        tvPushupsSessions = view.findViewById(R.id.tv_pushups_sessions);
        tvPushupsTotal = view.findViewById(R.id.tv_pushups_total);
        tvPushupsBest = view.findViewById(R.id.tv_pushups_best);
        tvPushupsAccuracy = view.findViewById(R.id.tv_pushups_accuracy);
        tvJumpingJacksSessions = view.findViewById(R.id.tv_jumping_jacks_sessions);
        tvJumpingJacksTotal = view.findViewById(R.id.tv_jumping_jacks_total);
        tvJumpingJacksBest = view.findViewById(R.id.tv_jumping_jacks_best);
        tvJumpingJacksAccuracy = view.findViewById(R.id.tv_jumping_jacks_accuracy);
        tvPlankSessions = view.findViewById(R.id.tv_plank_sessions);
        tvPlankTotal = view.findViewById(R.id.tv_plank_total);
        tvPlankBest = view.findViewById(R.id.tv_plank_best);
        tvPlankAccuracy = view.findViewById(R.id.tv_plank_accuracy);

        barChart = view.findViewById(R.id.bar_chart);
        lineChart = view.findViewById(R.id.line_chart);
        pieChart = view.findViewById(R.id.pie_chart);
    }

    private void loadStatistics() {
        repository.getStatistics(new WorkoutRepository.OnStatisticsLoadedListener() {
            @Override
            public void onLoaded(int totalWorkouts, int totalReps, int totalTime, int streak) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        tvTotalWorkouts.setText(String.valueOf(totalWorkouts));
                        tvTotalReps.setText(String.valueOf(totalReps));

                        int minutes = totalTime / 60;
                        tvTotalTime.setText(getString(R.string.total_time_value, minutes));

                        tvStreak.setText(getString(R.string.streak_value, streak));
                    }
                });
            }
        });
    }

    private void loadCharts() {
        // График за 7 дней
        repository.getWorkoutsForChart(7, new WorkoutRepository.OnChartDataLoadedListener() {
            @Override
            public void onLoaded(List<Workout> workouts) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        setupBarChart(workouts);
                        setupLineChart(workouts);
                    }
                });
            }
        });

        // Круговая диаграмма
        repository.getExerciseTypeStats(new WorkoutRepository.OnExerciseTypeStatsLoadedListener() {
            @Override
            public void onLoaded(int squatsCount, int pushupsCount, int jumpingJacksCount, int plankCount) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        setupPieChart(squatsCount, pushupsCount, jumpingJacksCount, plankCount);
                    }
                });
            }
        });
    }

    private void loadExerciseDetails() {
        repository.getWorkoutsByType("SQUATS", new WorkoutRepository.OnWorkoutsLoadedListener() {
            @Override
            public void onLoaded(List<Workout> workouts) {
                StatisticsFragment.this.bindExerciseBlock(
                        workouts, layoutSquatsBlock, "SQUATS",
                        tvSquatsSessions, tvSquatsTotal, tvSquatsBest, tvSquatsAccuracy);
            }
        });

        repository.getWorkoutsByType("PUSHUPS", new WorkoutRepository.OnWorkoutsLoadedListener() {
            @Override
            public void onLoaded(List<Workout> workouts) {
                StatisticsFragment.this.bindExerciseBlock(
                        workouts, layoutPushupsBlock, "PUSHUPS",
                        tvPushupsSessions, tvPushupsTotal, tvPushupsBest, tvPushupsAccuracy);
            }
        });

        repository.getWorkoutsByType("JUMPING_JACKS", new WorkoutRepository.OnWorkoutsLoadedListener() {
            @Override
            public void onLoaded(List<Workout> workouts) {
                StatisticsFragment.this.bindExerciseBlock(
                        workouts, layoutJumpingJacksBlock, "JUMPING_JACKS",
                        tvJumpingJacksSessions, tvJumpingJacksTotal, tvJumpingJacksBest, tvJumpingJacksAccuracy);
            }
        });

        repository.getWorkoutsByType("PLANK", new WorkoutRepository.OnWorkoutsLoadedListener() {
            @Override
            public void onLoaded(List<Workout> workouts) {
                StatisticsFragment.this.bindExerciseBlock(
                        workouts, layoutPlankBlock, "PLANK",
                        tvPlankSessions, tvPlankTotal, tvPlankBest, tvPlankAccuracy);
            }
        });
    }

    private void bindExerciseBlock(List<Workout> workouts, LinearLayout container,
                                   String exerciseType,
                                   TextView tvSessions, TextView tvTotal, TextView tvBest, TextView tvAccuracy) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (workouts.isEmpty()) {
                container.setVisibility(View.GONE);
                checkExercisePlaceholder();
                return;
            }

            boolean isPlank = "PLANK".equals(exerciseType);
            int total = 0, best = 0;
            float avgAccuracy = 0;
            for (Workout w : workouts) {
                total += w.getTotalReps();
                if (w.getTotalReps() > best) best = w.getTotalReps();
                avgAccuracy += w.getAccuracy();
            }
            avgAccuracy /= workouts.size();

            tvSessions.setText(getString(R.string.stat_sessions, workouts.size()));
            tvTotal.setText(isPlank
                    ? getString(R.string.stat_total_seconds, total)
                    : getString(R.string.stat_total_reps, total));
            tvBest.setText(isPlank
                    ? getString(R.string.stat_best_seconds, best)
                    : getString(R.string.stat_best_reps, best));
            tvAccuracy.setText(getString(R.string.stat_avg_accuracy, (int) avgAccuracy));

            container.setBackgroundResource(R.drawable.ripple_white);
            container.setClickable(true);
            container.setFocusable(true);
            container.setOnClickListener(v ->
                    BottomSheetExerciseDetailFragment
                            .newInstance(exerciseType)
                            .show(getChildFragmentManager(), "exercise_detail"));

            container.setVisibility(View.VISIBLE);
            checkExercisePlaceholder();
        });
    }

    private void checkExercisePlaceholder() {
        boolean anyVisible = layoutSquatsBlock.getVisibility() == View.VISIBLE
                || layoutPushupsBlock.getVisibility() == View.VISIBLE
                || layoutJumpingJacksBlock.getVisibility() == View.VISIBLE
                || layoutPlankBlock.getVisibility() == View.VISIBLE;
        tvExerciseDetailsPlaceholder.setVisibility(anyVisible ? View.GONE : View.VISIBLE);
    }

    private void setupBarChart(List<Workout> workouts) {
        Map<String, Integer> dailyReps = new HashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        List<String> last7Days = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String date = sdf.format(calendar.getTime());
            last7Days.add(date);
            dailyReps.put(date, 0);
        }

        for (Workout workout : workouts) {
            calendar.setTimeInMillis(workout.getTimestamp());
            String date = sdf.format(calendar.getTime());

            if (dailyReps.containsKey(date)) {
                dailyReps.put(date, dailyReps.get(date) + workout.getTotalReps());
            }
        }

        // entries для графика
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < last7Days.size(); i++) {
            String date = last7Days.get(i);
            entries.add(new BarEntry(i, dailyReps.get(date)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Reps");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.animateY(1000);

        // Настройка осей
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(last7Days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);

        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.invalidate();
    }

    private void setupLineChart(List<Workout> workouts) {
        ArrayList<Entry> entries = new ArrayList<>();

        // последние 10 тренировок для графика точности
        int count = Math.min(workouts.size(), 10);
        for (int i = count - 1; i >= 0; i--) {
            Workout workout = workouts.get(i);
            entries.add(new Entry(count - 1 - i, workout.getAccuracy()));
        }

        if (entries.isEmpty()) {
            lineChart.setNoDataText("No data available");
            lineChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Accuracy %");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#2196F3"));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);

        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);

        // Настройка осей
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setGranularity(10f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.invalidate();
    }

    private void setupPieChart(int squatsCount, int pushupsCount, int jumpingJacksCount, int plankCount) {
        if (squatsCount == 0 && pushupsCount == 0 && jumpingJacksCount == 0 && plankCount == 0) {
            pieChart.setNoDataText("No data available");
            pieChart.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (squatsCount > 0)       { entries.add(new PieEntry(squatsCount, "Squats"));        colors.add(Color.parseColor("#5FDE63")); }
        if (pushupsCount > 0)      { entries.add(new PieEntry(pushupsCount, "Push-ups"));     colors.add(Color.parseColor("#FF836A")); }
        if (jumpingJacksCount > 0) { entries.add(new PieEntry(jumpingJacksCount, "Jumping")); colors.add(Color.parseColor("#FFB22F")); }
        if (plankCount > 0)        { entries.add(new PieEntry(plankCount, "Plank"));          colors.add(Color.parseColor("#D769F0")); }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);

        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setCenterText("Total\nReps");
        pieChart.setCenterTextSize(16f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
        loadCharts();
        loadExerciseDetails();
    }
}
