package com.chat.service;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioSenderCall {
    //bandera de enviado
    private volatile boolean enviando;
    //Socket UDP para enviar los paquetes.
    private DatagramSocket socket;
    //Línea de audio para capturar el sonido del micrófono.
    private TargetDataLine microfono;
    //Hilo que manejará el envío de audio.
    private Thread sendingThread;
    //Dirección IP
    private String ipDestino;
    //puerto del destino
    private int puertoDestino;
    
    //cuando lo invocan recibe una ip y un puerto
    public AudioSenderCall(String ipDestino, int puertoDestino) {
        this.ipDestino = ipDestino;
        this.puertoDestino = puertoDestino;
        this.enviando = false;
    }
    
    public void iniciarEnvio() {
        //Si ya se está enviando audio, muestra un mensaje y retorna.
        if (enviando) {
            System.out.println(" Ya se está enviando audio");
            return;
        }
        //en el caso que no este ocpado
        //Establece la bandera enviando a true.
        enviando = true;
        //Crea un nuevo hilo que ejecutará el método ejecutarEnvio.
        sendingThread = new Thread(this::ejecutarEnvio);
        //inicializa hilo
        sendingThread.start();
    }
    
    //metodo que envia audio 
    private void ejecutarEnvio() {
        try {

            System.out.println(" === INICIANDO ENVÍO DE AUDIO ===");
            System.out.println(" Destino: " + ipDestino + ":" + puertoDestino);

            //Define el formato de audio
            AudioFormat formato = new AudioFormat(16000.0f, 16, 1, true, false);
            //Prepara la información para obtener la línea de captura (micrófono) con el formato especificado.
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);

            //en el caso que ese formato no sea soportado lo manda a otro 
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Línea de audio no soportada. Probando formato alternativo...");
                formato = new AudioFormat(8000.0f, 16, 1, true, false);
                info = new DataLine.Info(TargetDataLine.class, formato);
            }

            //obtiene la informacion del microfono (captura (micrófono) del sistema)
            microfono = (TargetDataLine) AudioSystem.getLine(info);
            //Abre la línea con el formato.
            microfono.open(formato);
            //Inicia la captura de audio.
            microfono.start();

            //crea un socket UDP 
            socket = new DatagramSocket();
            //crea un arreglo de bytes
            byte[] buffer = new byte[1024];

            System.out.println(" Enviando audio...");

            //si se esta enviando
            while (enviando) {
                //Lee datos del micrófono al buffer, obtiene los bytes del microfono
                int bytesRead = microfono.read(buffer, 0, buffer.length);
                //si hay informaion capturada (si alguien esta habando)
                if (bytesRead > 0) {
                    //crea un paquete para almacenar la informacion del micrifno
                    DatagramPacket paquete = new DatagramPacket(buffer, bytesRead, InetAddress.getByName(ipDestino), puertoDestino);
                    //lo manda al socket (al cliente)
                    socket.send(paquete);
                }
                
                // Pequeña pausa para no saturar
                Thread.sleep(10);
            }

        } catch (Exception e) {
            if (enviando) { // Solo mostrar error si no fue una parada intencional
                System.err.println("❌ ERROR en AudioSenderCall: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            terminarEnvio();
        }
    }
    //metodo para dejar de grabar
    public void terminarEnvio() {
        enviando = false;
        try {
            if (microfono != null) {
                //cierra microfono
                microfono.stop();
                microfono.close();
            }
            if (socket != null && !socket.isClosed()) {
                //cierra socket
                socket.close();
            }
            System.out.println("🔇 Envío de audio finalizado.");
        } catch (Exception e) {
            System.err.println("Error al terminar envío: " + e.getMessage());
        }
    }
    
    public boolean isEnviando() {
        return enviando;
    }
}

