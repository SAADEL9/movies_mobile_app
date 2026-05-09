package com.saad.moviessaad.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.saad.moviessaad.R;
import com.saad.moviessaad.api.ApiConstants;
import com.saad.moviessaad.model.WatchlistItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.WatchlistViewHolder> {

    public interface OnWatchlistActionListener {
        void onMovieClick(WatchlistItem item);
        void onRemoveClick(WatchlistItem item);
    }

    private final List<WatchlistItem> items = new ArrayList<>();
    private final OnWatchlistActionListener listener;

    public WatchlistAdapter(OnWatchlistActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WatchlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watchlist_card, parent, false);
        return new WatchlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchlistViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public WatchlistItem getItemAt(int position) {
        return items.get(position);
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void insertItem(int position, WatchlistItem item) {
        items.add(position, item);
        notifyItemInserted(position);
    }

    public void setItems(List<WatchlistItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    class WatchlistViewHolder extends RecyclerView.ViewHolder {
        private final ImageView poster;
        private final TextView title;
        private final TextView rating;
        private final ImageView heart;
        private final ImageView watchedIcon;

        WatchlistViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.watchlist_poster);
            title = itemView.findViewById(R.id.watchlist_title);
            rating = itemView.findViewById(R.id.watchlist_rating);
            heart = itemView.findViewById(R.id.watchlist_heart);
            watchedIcon = itemView.findViewById(R.id.watchlist_watched_icon);
        }

        void bind(WatchlistItem item) {
            title.setText(item.getTitle());
            rating.setText(String.format(Locale.getDefault(), "⭐ %.1f", item.getRating()));
            
            if (watchedIcon != null) {
                watchedIcon.setVisibility(item.isWatched() ? View.VISIBLE : View.GONE);
            }

            Glide.with(itemView.getContext())
                    .load(ApiConstants.IMAGE_BASE_URL + item.getPosterPath())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(android.R.color.darker_gray)
                    .into(poster);

            itemView.setOnClickListener(v -> listener.onMovieClick(item));
            heart.setOnClickListener(v -> listener.onRemoveClick(item));
        }
    }
}
