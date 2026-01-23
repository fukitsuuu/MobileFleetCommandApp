package com.atiera.mobilefleetcommandapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;

    private final List<ChatMessage> items = new ArrayList<>();
    private String myId;

    public void setMyId(String driverID) {
        this.myId = driverID;
    }

    public void setItems(List<ChatMessage> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void appendItems(List<ChatMessage> list) {
        if (list != null && !list.isEmpty()) {
            int start = items.size();
            items.addAll(list);
            notifyItemRangeInserted(start, list.size());
        }
    }

    public void addMessage(ChatMessage m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = items.get(position);
        boolean fromMe = myId != null && myId.equals(m.from);
        return fromMe ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage m = items.get(position);
        String text = m.body != null ? m.body : "";
        if (holder instanceof SentHolder) {
            ((SentHolder) holder).bubble.setText(text);
        } else {
            ((ReceivedHolder) holder).bubble.setText(text);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView bubble;
        SentHolder(View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.messageBubble);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView bubble;
        ReceivedHolder(View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.messageBubble);
        }
    }
}
