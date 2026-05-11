package com.saad.moviessaad.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.saad.moviessaad.R;
import com.saad.moviessaad.model.CastMember;
import java.util.ArrayList;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.CastViewHolder> {

    private List<CastMember> castList = new ArrayList<>();
    private Context context;

    public CastAdapter(Context context) {
        this.context = context;
    }

    public void setCastList(List<CastMember> castList) {
        this.castList = castList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cast, parent, false);
        return new CastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CastViewHolder holder, int position) {
        CastMember member = castList.get(position);
        holder.name.setText(member.getName());
        holder.character.setText(member.getCharacter());

        String profilePath = member.getProfilePath();
        Glide.with(context)
                .load("https://image.tmdb.org/t/p/w185" + profilePath)
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return castList.size();
    }

    static class CastViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        TextView character;

        public CastViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cast_image);
            name = itemView.findViewById(R.id.cast_name);
            character = itemView.findViewById(R.id.cast_character);
        }
    }
}
