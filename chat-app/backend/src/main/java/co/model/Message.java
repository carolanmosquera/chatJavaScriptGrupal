package co.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

//esta clase se llama cada vez que se quiera enviar cualquier cosa
public class Message implements java.io.Serializable{
    //id del mesaje
    private String id;
    //usario que mando el mesnaje
    private User sender;
    //contenido del mensaje
    private String content;
    //tipo de mensaje
    private MessageType type;
    //fecha del mensaje
    private LocalDateTime timestamp;
    //grupos
    private UserGroups userGroups;
    //lista de grupos
    private List<UserGroups> gruposLista;
    
    //enum que indica tipo de mensaje
    public enum MessageType {
        TEXT, CREATE_GROUP, UPDATE_GROUPS, JOIN_GROUP
    }

    //constructor 
    public Message(User sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor vacío para GSON
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", 
            timestamp.toLocalTime(), 
            sender != null ? sender.getUsername() : "Unknown", 
            content);
    }

    public void setGroup(UserGroups nuevoGrupo) {
        this.userGroups = nuevoGrupo;
    }
    
    public UserGroups getGroup() {
        return userGroups;
    }

    public List<UserGroups> getGruposLista() {
        return gruposLista;
    }

    public void setGroupList(List<UserGroups> gruposLista) {
        this.gruposLista = gruposLista;
    }

    // ✅ CORRECCIÓN: Método para mejor compatibilidad con JSON
    public String toJson() {
        return String.format(
            "{\"sender\":{\"username\":\"%s\",\"id\":\"%s\"},\"content\":\"%s\",\"type\":\"%s\",\"timestamp\":\"%s\"}",
            sender != null ? escapeJson(sender.getUsername()) : "",
            sender != null ? escapeJson(sender.getId()) : "",
            content != null ? escapeJson(content) : "",
            type != null ? type.name() : "TEXT",
            timestamp != null ? timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : ""
        );
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}