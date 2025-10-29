package co.model;

public class User implements java.io.Serializable{
    private String username;
    private String id;
    
    public User(String username, String id) {
        this.username = username;
        this.id = id;
    }
    
    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    @Override
    public String toString() {
        return username;
    }
}
