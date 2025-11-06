package co.services;

import co.controllers.ChatController;
import co.model.Message;
import com.google.gson.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends WebSocketServer {

    private final Set<WebSocket> connections;
    private final Gson gson;
    private final ChatController controller;

   

    public ChatServer(int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        this.connections = Collections.synchronizedSet(new HashSet<>());
        this.controller = new ChatController();

        this.controller.setServer(this);

        // Registrar adaptadores de tiempo
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, 
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, 
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDate.class, 
                    (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, 
                    (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                        LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .create();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("Nueva conexión: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        controller.userDisconnected(conn); // ✅ Notificar al controlador
        System.out.println("Conexión cerrada: " + conn.getRemoteSocketAddress());
    }

      @Override
    public void onMessage(WebSocket conn, String messageJson) {
        System.out.println("📨 Mensaje recibido de " + conn.getRemoteSocketAddress() + ": " + messageJson);
        try {
            Message incomingMessage = gson.fromJson(messageJson, Message.class);
            Message response = controller.processMessage(conn, incomingMessage);
            
            // ✅ El controlador ahora maneja el broadcast internamente
            if (response != null) {
                System.out.println("✅ Mensaje procesado: " + response.getType());
            }
        } catch (Exception e) {
            System.err.println("❌ Error procesando mensaje: " + e.getMessage());
        }
    }

        // ✅ NUEVO: Método para que el controlador pueda hacer broadcast
    public void broadcastMessage(Message message) {
        if (message != null) {
            String jsonResponse = gson.toJson(message);
            broadcast(jsonResponse);
            System.out.println("📤 Mensaje broadcast a " + connections.size() + " clientes: " + message.getType());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error en la conexión " + 
            (conn != null ? conn.getRemoteSocketAddress() : "desconocida") + ": " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Servidor de Chat iniciado en el puerto " + getPort());
        setConnectionLostTimeout(0);
    }

     public void broadcast(String message) {
        for (WebSocket conn : connections) {
            if (conn.isOpen()) {
                conn.send(message);
            }
        }
    }


}
