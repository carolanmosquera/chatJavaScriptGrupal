package model;

import Chat.MessageTypeEnum;

public enum MessageType {
    TEXT(0),        
    SYSTEM(1),
    AUDIO(2),      
    VOICECALL(3);  

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

    // Convertir desde Ice MessageTypeEnum
    public static MessageType fromIceEnum(MessageTypeEnum iceEnum) {
        switch (iceEnum) {
            case TEXT: return TEXT;
            case SYSTEM: return SYSTEM;
            case AUDIO: return AUDIO;
            case VOICECALL: return VOICECALL;
            default: return TEXT;
        }
    }

    // Convertir a Ice MessageTypeEnum
    public MessageTypeEnum toIceEnum() {
        switch (this) {
            case TEXT: return MessageTypeEnum.TEXT;
            case SYSTEM: return MessageTypeEnum.SYSTEM;
            case AUDIO: return MessageTypeEnum.AUDIO;
            case VOICECALL: return MessageTypeEnum.VOICECALL;
            default: return MessageTypeEnum.TEXT;
        }
    }
}