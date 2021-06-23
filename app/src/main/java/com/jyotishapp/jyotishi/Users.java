package com.jyotishapp.jyotishi;

public class Users {
    String Name, Age, UserId, NotificationKey, lastMessage, newMessage;
    long timestamp;
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setNewMessage(String newMessage) {
        this.newMessage = newMessage;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNewMessage() {
        return newMessage;
    }

    public String getNotificationKey() {
        return NotificationKey;
    }

    public void setNotificationKey(String notificationKey) {
        NotificationKey = notificationKey;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getAge() {
        return Age;
    }

    public void setAge(String Age) {
        this.Age = Age;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getName() {
        return Name;
    }

}
