package model;

import Chat.UserDTO;
import java.util.HashMap;

public class User {
    private String id;
    private String username;
    private String ipAddress;
    private long connectedAt;
    private boolean isOnline;
    private HashMap<String, UserGroup> groups;

    public User(String id, String username) {
        this.id = id;
        this.username = username;
        this.connectedAt = System.currentTimeMillis();
        this.isOnline = true;
        this.groups = new HashMap<>();
    }

    // Constructor adicional desde UserDTO
    public User(UserDTO dto) {
        this.id = dto.id;
        this.username = dto.username;
        this.isOnline = dto.isOnline;
        this.connectedAt = dto.connectedAt;
        this.groups = new HashMap<>();
    }

    // Convertir a DTO para Ice
    public UserDTO toDTO() {
        return new UserDTO(id, username, isOnline, connectedAt);
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
    
    public HashMap<String, UserGroup> getGroups() { return groups; }
    public void setGroups(HashMap<String, UserGroup> groups) { this.groups = groups; }
}
