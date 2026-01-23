package com.atiera.mobilefleetcommandapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "peer_id";
    public static final String EXTRA_PEER_NAME = "peer_name";

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";

    private String peerId;
    private String peerName;
    private String username;
    private String driverID;

    private RecyclerView messagesList;
    private ProgressBar progress;
    private EditText messageInput;
    private ImageButton sendButton;
    private MessageAdapter adapter;

    // Auto-update polling
    private Handler pollHandler;
    private Runnable pollRunnable;
    private static final long POLL_INTERVAL = 3000; // 3 seconds
    private String lastMessageTimestamp = "";
    private boolean isPolling = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.topbar_bg));
        getWindow().getDecorView().setSystemUiVisibility(
            getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );
        setContentView(R.layout.activity_conversation);

        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        peerName = getIntent().getStringExtra(EXTRA_PEER_NAME);
        if (peerId == null) peerId = "";
        if (peerName == null) peerName = peerId;

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        username = prefs.getString(KEY_USERNAME, "");
        driverID = prefs.getString("driverID", "");

        TextView peerNameView = findViewById(R.id.peerName);
        peerNameView.setText(peerName);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> finish());

        messagesList = findViewById(R.id.messagesList);
        progress = findViewById(R.id.progress);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        messagesList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter();
        adapter.setMyId(driverID);
        messagesList.setAdapter(adapter);

        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        loadHistory();
        markAsRead();
        
        // Initialize polling for new messages
        pollHandler = new Handler(Looper.getMainLooper());
        startPolling();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPolling) {
            startPolling();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    private void startPolling() {
        if (isPolling || username == null || username.isEmpty() || peerId.isEmpty()) {
            return;
        }
        isPolling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    checkForNewMessages();
                    pollHandler.postDelayed(this, POLL_INTERVAL);
                }
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    private void stopPolling() {
        isPolling = false;
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void checkForNewMessages() {
        if (username == null || username.isEmpty() || peerId.isEmpty()) {
            return;
        }
        
        // Use last message timestamp or empty string for first poll
        String since = lastMessageTimestamp != null ? lastMessageTimestamp : "";
        
        ApiClient.get().getNewMessages("new_messages", username, peerId, since)
            .enqueue(new Callback<DriverMessagesResponses.NewMessagesResponse>() {
                @Override
                public void onResponse(Call<DriverMessagesResponses.NewMessagesResponse> call,
                                       Response<DriverMessagesResponses.NewMessagesResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        List<ChatMessage> newMessages = response.body().items;
                        if (newMessages != null && !newMessages.isEmpty()) {
                            // Check if user is at bottom before adding messages
                            boolean wasAtBottom = isScrolledToBottom();
                            
                            // Append new messages
                            adapter.appendItems(newMessages);
                            
                            // Update last timestamp
                            ChatMessage lastMsg = newMessages.get(newMessages.size() - 1);
                            if (lastMsg.createdAt != null && !lastMsg.createdAt.isEmpty()) {
                                lastMessageTimestamp = lastMsg.createdAt;
                            }
                            
                            // Auto-scroll if user was at bottom
                            if (wasAtBottom) {
                                scrollToBottom();
                            }
                            
                            // Mark new messages as read
                            markAsRead();
                        }
                    }
                }

                @Override
                public void onFailure(Call<DriverMessagesResponses.NewMessagesResponse> call, Throwable t) {
                    // Silently fail - polling will retry
                    Log.d("ConversationActivity", "Poll failed: " + t.getMessage());
                }
            });
    }

    private boolean isScrolledToBottom() {
        if (messagesList == null || adapter == null || adapter.getItemCount() == 0) {
            return true;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
        if (layoutManager == null) {
            return true;
        }
        int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
        int totalItems = adapter.getItemCount();
        return lastVisiblePosition >= totalItems - 1;
    }

    private void loadHistory() {
        if (username == null || username.isEmpty() || peerId.isEmpty()) {
            return;
        }
        progress.setVisibility(View.VISIBLE);
        ApiClient.get().getMessageHistory("history", username, peerId)
            .enqueue(new Callback<DriverMessagesResponses.HistoryResponse>() {
                @Override
                public void onResponse(Call<DriverMessagesResponses.HistoryResponse> call,
                                       Response<DriverMessagesResponses.HistoryResponse> response) {
                    progress.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        List<ChatMessage> list = response.body().items;
                        adapter.setItems(list);
                        
                        // Update last message timestamp for polling
                        if (list != null && !list.isEmpty()) {
                            ChatMessage lastMsg = list.get(list.size() - 1);
                            if (lastMsg.createdAt != null && !lastMsg.createdAt.isEmpty()) {
                                lastMessageTimestamp = lastMsg.createdAt;
                            }
                        }
                        
                        scrollToBottom();
                    }
                }

                @Override
                public void onFailure(Call<DriverMessagesResponses.HistoryResponse> call, Throwable t) {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(ConversationActivity.this, "Could not load messages.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void markAsRead() {
        if (username == null || username.isEmpty() || peerId.isEmpty()) return;
        ApiClient.get().markMessagesAsRead("mark_as_read", username, peerId)
            .enqueue(new Callback<DriverMessagesResponses.MarkReadResponse>() {
                @Override
                public void onResponse(Call<DriverMessagesResponses.MarkReadResponse> call,
                                       Response<DriverMessagesResponses.MarkReadResponse> response) { }

                @Override
                public void onFailure(Call<DriverMessagesResponses.MarkReadResponse> call, Throwable t) { }
            });
    }

    private void sendMessage() {
        String text = messageInput.getText() != null ? messageInput.getText().toString().trim() : "";
        if (text.isEmpty() || username == null || username.isEmpty() || peerId.isEmpty()) {
            return;
        }
        messageInput.setText("");
        ApiClient.get().sendDriverMessage("send", username, peerId, text)
            .enqueue(new Callback<DriverMessagesResponses.SendMessageResponse>() {
                @Override
                public void onResponse(Call<DriverMessagesResponses.SendMessageResponse> call,
                                       Response<DriverMessagesResponses.SendMessageResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success && response.body().message != null) {
                        ChatMessage sentMsg = response.body().message;
                        adapter.addMessage(sentMsg);
                        
                        // Update last message timestamp
                        if (sentMsg.createdAt != null && !sentMsg.createdAt.isEmpty()) {
                            lastMessageTimestamp = sentMsg.createdAt;
                        }
                        
                        scrollToBottom();
                    } else {
                        Toast.makeText(ConversationActivity.this, "Failed to send.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DriverMessagesResponses.SendMessageResponse> call, Throwable t) {
                    Toast.makeText(ConversationActivity.this, "Could not send message.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            messagesList.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }
}
