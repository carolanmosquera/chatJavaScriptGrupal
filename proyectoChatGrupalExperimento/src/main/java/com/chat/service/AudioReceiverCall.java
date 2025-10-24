package com.chat.service;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

//para llamadas (UDP)

public class AudioReceiverCall {

    //volatile boolean recibiendo: Bandera para controlar el estado de recepción. 
    //volatile asegura que el valor sea leído y escrito correctamente por múltiples hilos.
    private volatile boolean recibiendo;

    //Socket UDP para recibir los paquetes de audio.
    private DatagramSocket socket;

    //Línea de audio para reproducir el sonido recibido.
    private SourceDataLine altavoz;

    //Hilo en el que se ejecutará la recepción de audio.
    private Thread receptionThread;

    //Puerto en el que se escuchará el audio.
    private int puerto;
    
    //Recibe el puerto de escucha y inicializa el estado recibiendo a false.
    public AudioReceiverCall(int puertoEscucha) {
        this.puerto = puertoEscucha;
        this.recibiendo = false;
    }
    
    //metodo para iniciar recepcion
    public void iniciarRecepcion() {

        //Verifica si ya se está recibiendo audio. Si es así, muestra un mensaje y retorna.
        if (recibiendo) {
            System.out.println(" Ya se está recibiendo audio");
            return;
        }
        
        //Si no, establece recibiendo a true y crea un nuevo hilo que ejecutará el método ejecutarRecepcion. 
        recibiendo = true;
        //en el hilo se esta 
        receptionThread = new Thread(this::ejecutarRecepcion);
        //Luego inicia el hilo.
        receptionThread.start();
    }
    
    //metodo que ejecuta el hilo
    private void ejecutarRecepcion() {
        try {
            //se configura el audio
            AudioFormat formato = new AudioFormat(16000.0f, 16, 1, true, false);
            //se obtiene la informacion del audio
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, formato);

            //si el formato no es soportado se cambia a otro
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Línea de audio no soportada. Probando formato alternativo...");
                formato = new AudioFormat(8000.0f, 16, 1, true, false);
                info = new DataLine.Info(SourceDataLine.class, formato);
            }

            //se obtiene conexion con altavoz (Se obtiene la línea de audio (SourceDataLine) )
            altavoz = (SourceDataLine) AudioSystem.getLine(info);
            //se habre altavoz
            altavoz.open(formato);
            //se inicializa altavoz para reproducir el audio
            altavoz.start();

            //se habre un DatagramSocket
            socket = new DatagramSocket(puerto);
            //se crea un arreglo de bytes
            byte[] buffer = new byte[1024];

            System.out.println(" Esperando audio entrante en el puerto " + puerto + "...");

            //en el caso que si se este recibiendo un audio
            //e reciben paquetes UDP y se escriben en la línea de audio para reproducirlos.
            while (recibiendo) {
                //crea un paqute con el Dtagrampacket para recibir el audio
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                //lo recibe en el socket
                socket.receive(paquete);
                //lo escribe para reproducirlo 
                altavoz.write(paquete.getData(), 0, paquete.getLength());
            }

        } catch (Exception e) {
            if (recibiendo) { // Solo mostrar error si no fue una parada intencional
                System.err.println(" ERROR en AudioReceiverCall: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            //En el finally se llama a terminarRecepcion para liberar recursos.
            terminarRecepcion();
        }
    }
    
    public void terminarRecepcion() {
        //Establece recibiendo a false para detener el bucle de recepción.
        recibiendo = false;
        try {
            if (altavoz != null) {
                //Cierra la línea de audio 
                altavoz.stop();
                altavoz.close();
            }
            if (socket != null && !socket.isClosed()) {
                //Cierra el socket.
                socket.close();
            }
            System.out.println("🔇 Recepción de audio finalizada.");
        } catch (Exception e) {
            System.err.println("Error al terminar recepción: " + e.getMessage());
        }
    }
    
    public boolean isRecibiendo() {
        return recibiendo;
    }
}

