package am.arthur.arcoach.adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import am.arthur.arcoach.R;
import am.arthur.arcoach.utils.AchievementManager;

public class AchievementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;
    private final List<Object> items;

    public AchievementAdapter(List<Object> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_achievement_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            ((AchievementViewHolder) holder).bind(
                    (AchievementManager.AchievementItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryTitle = itemView.findViewById(R.id.tv_category_title);
        }

        void bind(String title) {
            tvCategoryTitle.setText(title);
        }
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvProgress;
        ProgressBar progressBar;
        CardView cardView;
        View lockOverlay;

        AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon        = itemView.findViewById(R.id.tv_achievement_icon);
            tvTitle       = itemView.findViewById(R.id.tv_achievement_title);
            tvDescription = itemView.findViewById(R.id.tv_achievement_description);
            tvProgress    = itemView.findViewById(R.id.tv_achievement_progress);
            progressBar   = itemView.findViewById(R.id.progress_bar);
            cardView      = itemView.findViewById(R.id.card_achievement);
            lockOverlay   = itemView.findViewById(R.id.lock_overlay);
        }

        @SuppressLint("SetTextI18n")
        void bind(AchievementManager.AchievementItem achievement) {
            tvIcon.setText(achievement.icon);
            tvTitle.setText(achievement.title);
            tvDescription.setText(achievement.description);

            if (achievement.isUnlocked) {
                cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                lockOverlay.setVisibility(View.GONE);
                tvProgress.setText(itemView.getContext().getString(R.string.achievement_unlocked));
                tvProgress.setTextColor(Color.parseColor("#4CAF50"));
                progressBar.setProgress(100);
                progressBar.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            } else {
                cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                lockOverlay.setVisibility(View.VISIBLE);

                int percentage = achievement.getProgressPercentage();
                tvProgress.setText(achievement.currentProgress + " / "
                        + achievement.targetProgress + " (" + percentage + "%)");
                tvProgress.setTextColor(Color.parseColor("#666666"));
                progressBar.setProgress(percentage);
                progressBar.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
            }
        }
    }
}

