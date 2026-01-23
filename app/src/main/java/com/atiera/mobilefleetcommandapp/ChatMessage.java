package com.atiera.mobilefleetcommandapp;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("messageID")
    public String messageID;

    @SerializedName("messages")
    public String body;

    @SerializedName("message_from")
    public String from;

    @SerializedName("message_to")
    public String to;

    @SerializedName("message_created")
    public String createdAt;

    @SerializedName("is_read")
    public int isRead;
}
