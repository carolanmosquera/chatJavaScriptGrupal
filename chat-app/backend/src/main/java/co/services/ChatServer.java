package co.services;

import co.controllers.ChatController;
import co.model.Message;
import co.model.User;
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
        super(new InetSocketAddress(port));
        this.connections = Collections.synchronizedSet(new HashSet<>());
        this.controller = new ChatController();

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc,
                                context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime
                                .parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, typeOfSrc,
                                context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString(),
                                DateTimeFormatter.ISO_LOCAL_DATE))
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
        controller.handleDisconnection(conn);
        System.out.println("Conexión cerrada: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String messageJson) {
        System.out.println("Mensaje recibido de " + conn.getRemoteSocketAddress() + ": " + messageJson);
        try {
            Message incomingMessage = gson.fromJson(messageJson, Message.class);
            Message response = controller.processMessage(conn, incomingMessage);

            if (response != null) {
                // ✅ CORRECCIÓN: Pasar también la conexión del remitente
                sendToRelevantUsers(response, incomingMessage, conn);
            }
        } catch (Exception e) {
            System.err.println("❌ Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ CORREGIDO: Recibir la conexión del remitente
    private void sendToRelevantUsers(Message response, Message originalMessage, WebSocket senderConn) {
        String json = gson.toJson(response);

        switch (response.getType()) {
            case TEXT:
                // ✅ CORRECCIÓN: Obtener el usuario remitente de la conexión
                User sender = controller.getUserForConnection(senderConn);
                if (sender != null) {
                    handleTextMessage(response, originalMessage, sender.getUsername(), json);
                } else {
                    System.err.println("❌ No se pudo obtener el usuario remitente");
                }
                break;

            case UPDATE_GROUPS:
            case UPDATE_USERS:
                System.out.println("📢 Enviando actualización a todos los usuarios");
                broadcast(json);
                break;

            case CREATE_GROUP:
            case JOIN_GROUP:
                broadcast(json);
                break;

            default:
                System.out.println("⚠️ Tipo no manejado: " + response.getType());
                break;
        }
    }

    private void handleTextMessage(Message response, Message originalMessage, String senderUsername, String json) {
        System.out.println("💬 Procesando mensaje de " + senderUsername);

        // ✅ Si el mensaje pertenece a un grupo
        if (originalMessage.getGroup() != null && originalMessage.getGroup().getNombreGrupo() != null) {
            String groupName = originalMessage.getGroup().getNombreGrupo();
            System.out.println("👥 Mensaje de grupo en: " + groupName);
            sendToGroup(groupName, json, senderUsername); // enviar a todos menos el remitente
            sendToUser(senderUsername, json); // también al remitente
            return;
        }

        // ✅ Si el mensaje tiene destinatario directo (mensajes privados)
        String targetUser = originalMessage.getTarget();
        if (targetUser != null) {
            System.out.println("📨 Mensaje privado de " + senderUsername + " para " + targetUser);
            sendToUser(senderUsername, json);
            sendToUser(targetUser, json);
            return;
        }

        // ✅ Si no hay destino claro (fallback)
        System.out.println("⚠️ No se pudo determinar destinatario, enviando solo al remitente");
        sendToUser(senderUsername, json);
    }

    // ✅ MEJORADO: Enviar a usuario específico con logs
    private void sendToUser(String username, String message) {
        boolean sent = false;
        System.out.println("TODOS LOS USERS: "+connections);
        for (WebSocket conn : connections) {
            User user = controller.getUserForConnection(conn);
            if (user != null && user.getUsername().equals(username)) {
                if (conn.isOpen()) {
                    conn.send(message);
                    sent = true;
                    System.out.println("✅ Mensaje enviado a usuario: " + username);
                }
            }
        }
        if (!sent) {
            System.out.println("⚠️ Usuario " + username + " no encontrado o desconectado");
        }
    }

    // ✅ MEJORADO: Enviar a grupo excluyendo al remitente
    private void sendToGroup(String groupName, String message, String excludeUser) {
        int sentCount = 0;
        for (WebSocket conn : connections) {
            User user = controller.getUserForConnection(conn);
            if (user != null && !user.getUsername().equals(excludeUser)) {
                if (controller.isUserInGroup(user.getUsername(), groupName)) {
                    if (conn.isOpen()) {
                        conn.send(message);
                        sentCount++;
                    }
                }
            }
        }
        System.out.println("👥 Mensaje enviado a " + sentCount + " miembros del grupo: " + groupName);
    }

    public void broadcast(String message) {
        int sentCount = 0;
        for (WebSocket conn : connections) {
            if (conn.isOpen()) {
                conn.send(message);
                sentCount++;
            }
        }
        System.out.println("📢 Broadcast enviado a " + sentCount + " conexiones");
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
}