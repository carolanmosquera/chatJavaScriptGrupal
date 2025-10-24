package com.chat.service;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecorder {
    //formato de microfono
    private AudioFormat format;
    
    
    public AudioRecorder() {
        this.format = new AudioFormat(44100, 16, 1, true, true);
    }
    
    //MÉTODO record() - CAPTURA DE AUDIO
    public byte[] record(int durationMillis) throws LineUnavailableException, InterruptedException {
        //informacion del microfno
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        //linea del microfono
        //(TargetDataLine): Cast al tipo específico de línea de entrada
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        
        //abre linea de microfono
        line.open(format);
        //inicializa linea de microfono
        line.start();
        
        //ByteArrayOutputStream: Stream que almacena datos en un array de bytes en memoria
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //array de bites para microfono
        byte[] buffer = new byte[4096];
        //CONTROL DE TIEMPO DE GRABACIÓN
        long startTime = System.currentTimeMillis();

        //va a grabar el audio dependiendo del tiempo de durationMillis
        while (System.currentTimeMillis() - startTime < durationMillis) {
            //line.read(buffer, 0, buffer.length): Lee datos del micrófono al buffer
            /*
            buffer: Donde almacenar los datos leídos

            0: Offset de inicio en el buffer

            buffer.length: Máximo de bytes a leer
             */
            int bytesRead = line.read(buffer, 0, buffer.length);

            //baos.write(buffer, 0, bytesRead): Ecribe solo los bytes realmente leídos
            baos.write(buffer, 0, bytesRead);
        }
        
        line.stop();
        line.close();
        
        return baos.toByteArray();
    }
}