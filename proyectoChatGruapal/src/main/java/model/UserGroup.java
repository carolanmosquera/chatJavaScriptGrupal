package model;

import java.util.HashSet;
import java.util.Set;

public class UserGroup {

    private String username;
    private Set<User> participants = new HashSet<>();
    
    public UserGroup(String username, Set<User> participants) {
        this.username = username;
        this.participants = participants;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public Set<User> getParticipants() {
        return participants;
    }
    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }
    
}
