package com.saad.moviessaad.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.saad.moviessaad.R;
import com.saad.moviessaad.model.ChatMessage;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_USER = 1;
    private static final int TYPE_BOT = 2;
    private static final int TYPE_TYPING = 3;

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (msg.isTyping()) return TYPE_TYPING;
        return msg.isUser() ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == TYPE_TYPING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_typing_indicator, parent, false);
            return new TypingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(message.getContent());
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).tvMessage.setText(message.getContent());
        } else if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).startAnimation();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_user);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        BotViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_bot);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        View dot1, dot2, dot3;
        TypingViewHolder(View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        void startAnimation() {
            Animation anim1 = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.typing_dot);
            Animation anim2 = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.typing_dot);
            Animation anim3 = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.typing_dot);

            anim2.setStartOffset(133);
            anim3.setStartOffset(266);

            dot1.startAnimation(anim1);
            dot2.startAnimation(anim2);
            dot3.startAnimation(anim3);
        }
    }
}
