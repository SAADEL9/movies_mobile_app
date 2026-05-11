package com.saad.moviessaad.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.saad.moviessaad.R;
import com.saad.moviessaad.api.ApiConstants;
import com.saad.moviessaad.model.WatchedItem;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WatchedAdapter extends RecyclerView.Adapter<WatchedAdapter.WatchedViewHolder> {

    private List<WatchedItem> items = new ArrayList<>();
    private final OnWatchedActionListener listener;

    public interface OnWatchedActionListener {
        void onMovieClick(WatchedItem item);
    }

    public WatchedAdapter(OnWatchedActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<WatchedItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public WatchedItem getItemAt(int position) {
        return items.get(position);
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void insertItem(int position, WatchedItem item) {
        items.add(position, item);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public WatchedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watched_card, parent, false);
        return new WatchedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchedViewHolder holder, int position) {
        WatchedItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WatchedViewHolder extends RecyclerView.ViewHolder {
        private final ImageView poster;
        private final TextView title;
        private final TextView rating;
        private final TextView watchedDate;

        public WatchedViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.movie_poster);
            title = itemView.findViewById(R.id.movie_title);
            rating = itemView.findViewById(R.id.movie_rating);
            watchedDate = itemView.findViewById(R.id.watched_date);
        }

        public void bind(WatchedItem item, OnWatchedActionListener listener) {
            title.setText(item.getTitle());
            rating.setText(String.format(Locale.US, "%.1f", item.getRating()));
            
            String dateStr = item.getWatchedAt();
            if (dateStr != null) {
                try {
                    // Supabase timestamp format: 2024-05-10T12:00:00.000Z
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                    Date date = inputFormat.parse(dateStr);
                    if (date != null) {
                        watchedDate.setText("Watched on " + outputFormat.format(date));
                    }
                } catch (ParseException e) {
                    watchedDate.setText(dateStr);
                }
            }

            Glide.with(itemView.getContext())
                    .load(ApiConstants.IMAGE_BASE_URL + item.getPosterPath())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(poster);

            itemView.setOnClickListener(v -> listener.onMovieClick(item));
        }
    }
}
