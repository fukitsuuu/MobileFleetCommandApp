package com.atiera.mobilefleetcommandapp;

import com.google.gson.annotations.SerializedName;

public class Conversation {
    @SerializedName("id")
    public String peerId;

    @SerializedName("name")
    public String name;

    @SerializedName("display")
    public String display;

    @SerializedName("role")
    public String role;

    @SerializedName("last_message")
    public String lastMessage;

    @SerializedName("last_time")
    public String lastTime;

    @SerializedName("last_time_raw")
    public String lastTimeRaw;

    @SerializedName("last_message_from")
    public String lastMessageFrom;

    @SerializedName("has_unread")
    public boolean hasUnread;

    @SerializedName("unread_count")
    public int unreadCount;
}
