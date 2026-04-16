package com.example.vigchat.models;

public class ChatRoom {
    private String roomId;
    private String adminId;
    private String joinLink;
    private long createdAt;

    public ChatRoom() {
    }

    public ChatRoom(String roomId, String adminId, String joinLink, long createdAt) {
        this.roomId = roomId;
        this.adminId = adminId;
        this.joinLink = joinLink;
        this.createdAt = createdAt;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getJoinLink() {
        return joinLink;
    }

    public void setJoinLink(String joinLink) {
        this.joinLink = joinLink;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
