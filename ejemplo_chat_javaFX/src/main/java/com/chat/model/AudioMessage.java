package com.chat.model;

public class AudioMessage extends Message implements java.io.Serializable {
    //bytes para el audio de mensaje
    private byte[] audioData;
    //duracion de mensaje de audio
    private int duration; // en segundos
    
    //constructor
    public AudioMessage(User sender, byte[] audioData, int duration) {
        super(sender, " Mensaje de audio", MessageType.AUDIO);
        this.audioData = audioData;
        this.duration = duration;
    }
    
    // Getters y Setters
    public byte[] getAudioData() { return audioData; }
    public void setAudioData(byte[] audioData) { this.audioData = audioData; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}