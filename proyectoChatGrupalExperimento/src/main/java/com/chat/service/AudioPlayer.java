package com.chat.service;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class AudioPlayer {
    //formato de audio
    private AudioFormat format;
    
    //constructor
    public AudioPlayer() {
        this.format = new AudioFormat(44100, 16, 1, true, true);
    }
    
    //MÉTODO play() - REPRODUCCIÓN SINCRÓNICA
    public void play(byte[] audioData) throws LineUnavailableException {

        //DataLine.Info: Clase que describe una línea de audio
        //informacion de altavocez
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        //SourceDataLine.class: línea de altavocez (salida - altavoces)
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        
        //abren linea con formato 
        line.open(format);
        line.start();
        
        line.write(audioData, 0, audioData.length);
        
        line.drain();
        line.close();
    }
    
    //reproducen audio
    public void playAsync(byte[] audioData) {
        new Thread(() -> {
            try {
                play(audioData);
            } catch (LineUnavailableException e) {
                System.err.println("Error reproduciendo audio: " + e.getMessage());
            }
        }).start();
    }
}
