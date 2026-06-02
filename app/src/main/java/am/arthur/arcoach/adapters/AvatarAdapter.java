package am.arthur.arcoach.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import am.arthur.arcoach.R;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final List<String> avatars;
    private String selectedAvatar;
    private final OnAvatarClickListener listener;

    public interface OnAvatarClickListener {
        void onAvatarClick(String avatar);
    }

    public AvatarAdapter(List<String> avatars, String selectedAvatar, OnAvatarClickListener listener) {
        this.avatars = avatars;
        this.selectedAvatar = selectedAvatar;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        String avatar = avatars.get(position);
        holder.bind(avatar);
    }

    @Override
    public int getItemCount() {
        return avatars.size();
    }

    class AvatarViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        CardView cardView;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            cardView = itemView.findViewById(R.id.card_avatar);
        }

        public void bind(String avatar) {
            tvAvatar.setText(avatar);

            if (avatar.equals(selectedAvatar)) {
                cardView.setCardBackgroundColor(Color.parseColor("#2196F3"));
            } else {
                cardView.setCardBackgroundColor(Color.WHITE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedAvatar = avatar;
                    notifyDataSetChanged();
                    listener.onAvatarClick(avatar);
                }
            });
        }
    }
}
