package co.services;

import co.controllers.ChatController;
import co.model.Message;
import co.model.User;
import co.model.UserGroups;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends WebSocketServer {

    // Set para rastrear las conexiones abiertas
    private final Set<WebSocket> connections;
    // Utilidad para serializar/deserializar objetos Java a JSON
    private final Gson gson = new Gson();
    // Controlador que maneja la lógica de chat (grupos, usuarios, etc.)
    private final ChatController controller; 

    public ChatServer(int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        this.connections = Collections.synchronizedSet(new HashSet<>());
        // Inicializa el controlador de lógica
        this.controller = new ChatController(); 
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("Nueva conexión: " + conn.getRemoteSocketAddress());
        // Aquí podrías enviar la lista de grupos al nuevo usuario
        // sendToAll(controller.getGroupsUpdateMessage()); 
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Conexión cerrada: " + conn.getRemoteSocketAddress());
        // Aquí podrías notificar a los demás usuarios sobre la desconexión
    }

    @Override
    public void onMessage(WebSocket conn, String messageJson) {
        System.out.println("Mensaje recibido de " + conn.getRemoteSocketAddress() + ": " + messageJson);
        
        // 1. Deserializar el JSON a un objeto Message de Java
        Message incomingMessage = gson.fromJson(messageJson, Message.class);
        
        // 2. Procesar el mensaje usando el controlador
        Message response = controller.processMessage(conn, incomingMessage);

        // 3. Enviar la respuesta(s) apropiada(s)
        if (response != null) {
             broadcast(response); // o un método más específico como sendToGroup(response)
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error en la conexión " + (conn != null ? conn.getRemoteSocketAddress() : "desconocida") + ":" + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Servidor de Chat iniciado en el puerto " + getPort());
        setConnectionLostTimeout(0); // Deshabilita el timeout para testear
    }
    
    // Método para enviar un objeto Message serializado a JSON a todos los clientes
    public void broadcast(Message message) {
        String json = gson.toJson(message);
        for (WebSocket conn : connections) {
            conn.send(json);
        }
    }
}
