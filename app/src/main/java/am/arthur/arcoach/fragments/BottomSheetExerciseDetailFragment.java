package am.arthur.arcoach.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import am.arthur.arcoach.R;
import am.arthur.arcoach.database.Workout;
import am.arthur.arcoach.database.WorkoutRepository;

public class BottomSheetExerciseDetailFragment extends BottomSheetDialogFragment {

    private static final String ARG_EXERCISE_TYPE = "exercise_type";

    public static BottomSheetExerciseDetailFragment newInstance(String exerciseType) {
        BottomSheetExerciseDetailFragment fragment = new BottomSheetExerciseDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE_TYPE, exerciseType);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_exercise_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String exerciseType = getArguments() != null
                ? getArguments().getString(ARG_EXERCISE_TYPE, "")
                : "";
        boolean isPlank = "PLANK".equals(exerciseType);

        view.post(() -> {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if (dialog == null) return;
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });

        int accentColor = accentColor(exerciseType);
        view.findViewById(R.id.sheet_header).setBackgroundColor(accentColor);

        TextView tvTitle = view.findViewById(R.id.tv_sheet_title);
        tvTitle.setText(exerciseName(exerciseType));

        ImageButton btnClose = view.findViewById(R.id.btn_close_sheet);
        btnClose.setOnClickListener(v -> dismiss());

        TextView tvRepsLabel = view.findViewById(R.id.tv_reps_chart_label);
        tvRepsLabel.setText(isPlank
                ? getString(R.string.sheet_plank_chart_label)
                : getString(R.string.sheet_reps_chart_label));

        WorkoutRepository repository = new WorkoutRepository(requireContext());
        repository.getWorkoutsByType(exerciseType, workouts -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                bindStats(view, workouts, isPlank);
                buildRepsChart(view, workouts, accentColor, isPlank);
                buildAccuracyChart(view, workouts, accentColor);
            });
        });
    }

    private void bindStats(View view, List<Workout> workouts, boolean isPlank) {
        TextView tvSessions = view.findViewById(R.id.tv_sheet_sessions);
        TextView tvTotal    = view.findViewById(R.id.tv_sheet_total);
        TextView tvBest     = view.findViewById(R.id.tv_sheet_best);
        TextView tvAccuracy = view.findViewById(R.id.tv_sheet_accuracy);

        if (workouts.isEmpty()) {
            tvSessions.setText(R.string.no_exercise_data);
            tvTotal.setVisibility(View.GONE);
            tvBest.setVisibility(View.GONE);
            tvAccuracy.setVisibility(View.GONE);
            return;
        }

        int total = 0, best = 0;
        float avgAcc = 0;
        for (Workout w : workouts) {
            total += w.getTotalReps();
            if (w.getTotalReps() > best) best = w.getTotalReps();
            avgAcc += w.getAccuracy();
        }
        avgAcc /= workouts.size();

        tvSessions.setText(getString(R.string.stat_sessions, workouts.size()));
        tvTotal.setText(isPlank
                ? getString(R.string.stat_total_seconds, total)
                : getString(R.string.stat_total_reps, total));
        tvBest.setText(isPlank
                ? getString(R.string.stat_best_seconds, best)
                : getString(R.string.stat_best_reps, best));
        tvAccuracy.setText(getString(R.string.stat_avg_accuracy, (int) avgAcc));
    }

    private void buildRepsChart(View view, List<Workout> workouts, int color, boolean isPlank) {
        LineChart chart = view.findViewById(R.id.sheet_line_chart_reps);
        if (workouts.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_exercise_data));
            chart.invalidate();
            return;
        }
        int count = Math.min(workouts.size(), 10);
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(i, workouts.get(workouts.size() - count + i).getTotalReps()));
        }
        String unit = isPlank ? "sec" : "reps";
        setupLineChart(chart, entries, color, unit, false);
    }

    private void buildAccuracyChart(View view, List<Workout> workouts, int color) {
        LineChart chart = view.findViewById(R.id.sheet_line_chart_accuracy);
        if (workouts.isEmpty()) {
            chart.setNoDataText(getString(R.string.no_exercise_data));
            chart.invalidate();
            return;
        }
        int count = Math.min(workouts.size(), 10);
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(i, workouts.get(workouts.size() - count + i).getAccuracy()));
        }
        setupLineChart(chart, entries, color, "%", true);
    }

    private void setupLineChart(LineChart chart, ArrayList<Entry> entries,
                                int color, String unit, boolean isAccuracy) {
        LineDataSet dataSet = new LineDataSet(entries, unit);
        dataSet.setColor(color);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chart.setData(new LineData(dataSet));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(entries.size(), 5), false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        if (isAccuracy) leftAxis.setAxisMaximum(100f);
        leftAxis.setGranularity(1f);

        chart.getAxisRight().setEnabled(false);
        chart.animateX(800);
        chart.invalidate();
    }

    private int accentColor(String exerciseType) {
        switch (exerciseType) {
            case "SQUATS":        return Color.parseColor("#4CAF50");
            case "PUSHUPS":       return Color.parseColor("#FF5722");
            case "JUMPING_JACKS": return Color.parseColor("#FFB22F");
            case "PLANK":         return Color.parseColor("#D769F0");
            default:              return Color.parseColor("#2196F3");
        }
    }

    private String exerciseName(String exerciseType) {
        switch (exerciseType) {
            case "SQUATS":        return getString(R.string.exercise_squats_label);
            case "PUSHUPS":       return getString(R.string.exercise_pushups_label);
            case "JUMPING_JACKS": return getString(R.string.exercise_jumping_jacks_label);
            case "PLANK":         return getString(R.string.exercise_plank_label);
            default:              return exerciseType;
        }
    }
}
