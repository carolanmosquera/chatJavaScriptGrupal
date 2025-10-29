package co;

import co.services.ChatServer;
import java.io.IOException;

public class Main {
    // Puerto para el servidor WebSocket
    private static final int WS_PORT = 8887; 

    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer(WS_PORT);
            server.start(); // Inicia el hilo del servidor
            System.out.println("Servidor de Chat inicializado y escuchando en ws://localhost:" + WS_PORT);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("No se pudo iniciar el servidor de chat.");
        }
    }
}
