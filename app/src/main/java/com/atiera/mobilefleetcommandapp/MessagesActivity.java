package com.atiera.mobilefleetcommandapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesActivity extends AppCompatActivity {

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";

    private RecyclerView conversationsList;
    private ProgressBar progress;
    private TextView emptyMessages;
    private TextView headerSubtitle;
    private ConversationAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.topbar_bg));
        getWindow().getDecorView().setSystemUiVisibility(
            getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        setContentView(R.layout.activity_messages);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> finish());

        conversationsList = findViewById(R.id.conversationsList);
        progress = findViewById(R.id.messagesProgress);
        emptyMessages = findViewById(R.id.emptyMessages);
        headerSubtitle = findViewById(R.id.headerSubtitle);

        conversationsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter();
        adapter.setOnConversationClickListener(c -> {
            Intent i = new Intent(MessagesActivity.this, ConversationActivity.class);
            i.putExtra(ConversationActivity.EXTRA_PEER_ID, c.peerId);
            i.putExtra(ConversationActivity.EXTRA_PEER_NAME, c.name != null ? c.name : c.peerId);
            startActivity(i);
        });
        conversationsList.setAdapter(adapter);

        fetchConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchConversations();
    }

    private void fetchConversations() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String username = prefs.getString(KEY_USERNAME, "");
        if (username == null || username.isEmpty()) {
            showEmpty("Please log in to view messages.");
            return;
        }

        progress.setVisibility(View.VISIBLE);
        emptyMessages.setVisibility(View.GONE);
        conversationsList.setVisibility(View.GONE);

        ApiClient.get().getDriverConversations("conversations_list", username)
            .enqueue(new Callback<DriverMessagesResponses.ConversationsResponse>() {
                @Override
                public void onResponse(Call<DriverMessagesResponses.ConversationsResponse> call,
                                       Response<DriverMessagesResponses.ConversationsResponse> response) {
                    progress.setVisibility(View.GONE);
                    if (!response.isSuccessful() || response.body() == null) {
                        showEmpty("Could not load messages.");
                        return;
                    }
                    DriverMessagesResponses.ConversationsResponse body = response.body();
                    if (!body.success) {
                        String err = body.error != null ? body.error : "Unknown error";
                        if ("DRIVER_NOT_FOUND".equals(err)) {
                            showEmpty("Driver account not found.");
                        } else {
                            showEmpty("Could not load messages.");
                        }
                        Log.e("MessagesActivity", "API error: " + err);
                        return;
                    }
                    List<Conversation> list = body.conversations;
                    if (list == null || list.isEmpty()) {
                        headerSubtitle.setText("Your messages will appear here");
                        showEmpty("No messages yet.");
                        return;
                    }
                    headerSubtitle.setText(list.size() + " conversation(s)");
                    adapter.setItems(list);
                    emptyMessages.setVisibility(View.GONE);
                    conversationsList.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<DriverMessagesResponses.ConversationsResponse> call, Throwable t) {
                    progress.setVisibility(View.GONE);
                    showEmpty("Could not load messages. Check your connection.");
                    Log.e("MessagesActivity", "Fetch failed", t);
                }
            });
    }

    private void showEmpty(String msg) {
        emptyMessages.setText(msg);
        emptyMessages.setVisibility(View.VISIBLE);
        conversationsList.setVisibility(View.GONE);
    }
}
