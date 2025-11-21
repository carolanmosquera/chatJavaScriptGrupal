package model;

import java.util.HashMap;

public class User {

    private String username;
    private String ipAddress;
    private long connectedAt;
    private boolean isOnline;

    // the use of hashMap is to vinculate the user with the gropus through the
    // id(String)
    private HashMap<String, UserGroup> groups;

    public User(String username) {

        this.username = username;
        this.connectedAt = System.currentTimeMillis();
        this.isOnline = true;
        this.groups = new HashMap<>();

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(long connectedAt) {
        this.connectedAt = connectedAt;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
