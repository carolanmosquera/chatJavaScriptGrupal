package model;

public enum MessageType {
    TEXT(0),        
    SYSTEM(1),      
    VOICE_CALL(4);  

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MessageType fromInt(int value) {
        for (MessageType type : MessageType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return TEXT; 
    }
}