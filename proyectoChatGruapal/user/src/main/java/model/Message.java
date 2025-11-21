package model;

public class Message {
    private String id;
    private String senderId;
    private String senderName;
    private String content;
    private MessageType type;

    public Message(String id, String senderId, String senderName, String content, MessageType type) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        // this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // public long getTimestamp() { return timestamp; }
    // public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

   
}
