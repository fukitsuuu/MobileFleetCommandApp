package com.atiera.mobilefleetcommandapp;

import java.util.List;

public class DriverMessagesResponses {

    public static class ConversationsResponse {
        public boolean success;
        public String error;
        public List<Conversation> conversations;
    }

    public static class HistoryResponse {
        public boolean success;
        public String error;
        public List<ChatMessage> items;
    }

    public static class SendMessageResponse {
        public boolean success;
        public String error;
        public ChatMessage message;
    }

    public static class NewMessagesResponse {
        public boolean success;
        public String error;
        public List<ChatMessage> items;
    }

    public static class MarkReadResponse {
        public boolean success;
        public String error;
        public int updated;
    }

    public static class UnreadCountResponse {
        public boolean success;
        public String error;
        public int count;
    }
}
