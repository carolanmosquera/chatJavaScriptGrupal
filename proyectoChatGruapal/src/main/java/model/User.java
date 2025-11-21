package model;

public class User {
    private String id;
    private String username;
    private String ipAddress;
    private long connectedAt;
    private boolean isOnline;

    public User(String id, String username, String ipAddress) {
        this.id = id;
        this.username = username;
        this.ipAddress = ipAddress;
        this.connectedAt = System.currentTimeMillis();
        this.isOnline = true;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public long getConnectedAt() { return connectedAt; }
    public void setConnectedAt(long connectedAt) { this.connectedAt = connectedAt; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}
