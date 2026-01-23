package com.atiera.mobilefleetcommandapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<Conversation> items = new ArrayList<>();
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation c);
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Conversation> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation c = items.get(position);
        String name = c.name != null && !c.name.isEmpty() ? c.name : (c.peerId != null ? c.peerId : "Unknown");
        holder.name.setText(name);
        String last = c.lastMessage != null && !c.lastMessage.isEmpty() ? c.lastMessage : "No messages";
        holder.lastMessage.setText(last);
        holder.time.setText(c.lastTime != null ? c.lastTime : "");
        holder.unreadBadge.setVisibility(c.hasUnread ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(c);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, lastMessage, time;
        View unreadBadge;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.convName);
            lastMessage = itemView.findViewById(R.id.convLastMessage);
            time = itemView.findViewById(R.id.convTime);
            unreadBadge = itemView.findViewById(R.id.convUnreadBadge);
        }
    }
}
