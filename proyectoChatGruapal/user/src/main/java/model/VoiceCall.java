package model;

public class VoiceCall {
    String callId;
    String callerId;
    String receiverId;
    long startTime;
    boolean isActive;
    
    public VoiceCall(String callId, String callerId, String receiverId) {
        this.callId = callId;
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Verifica si un usuario es participante de esta llamada
     */
    public boolean isParticipant(String userId) {
        return callerId.equals(userId) || receiverId.equals(userId);
    }
    
    /**
     * Obtiene el ID del otro participante
     */
    public String getOtherParticipant(String userId) {
        if (callerId.equals(userId)) {
            return receiverId;
        } else if (receiverId.equals(userId)) {
            return callerId;
        }
        return null;
    }

    
}
