package co.controllers;

import co.model.Message;
import co.model.User;
import co.model.UserGroups;
import co.services.ChatServer;

import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatController {
    
    // Mapa para asociar una conexión WebSocket con un usuario
    private final Map<WebSocket, User> connectedUsers;
    // Lista para almacenar los grupos de chat activos
    private final List<UserGroups> availableGroups;

    public ChatController() {
        this.connectedUsers = new ConcurrentHashMap<>();
        this.availableGroups = new CopyOnWriteArrayList<>();
        
        // Grupo por defecto: "General"
        UserGroups general = new UserGroups("General");
        availableGroups.add(general);
    }

    public void setServer(ChatServer server) {
        this.server = server;
    }
    
    // Método principal para procesar los mensajes entrantes
        public Message processMessage(WebSocket conn, Message msg) {
        if (!connectedUsers.containsKey(conn) && msg.getSender() != null) {
            User user = msg.getSender();
            connectedUsers.put(conn, user);
            System.out.println("✅ Usuario " + user.getUsername() + " conectado desde: " + 
                conn.getRemoteSocketAddress());
            
            // Notificar a todos sobre el nuevo usuario
            Message joinMsg = new Message(user, user.getUsername() + " se ha unido al chat", Message.MessageType.TEXT);
            broadcastMessage(joinMsg);
            
            return createGroupsUpdateMessage(); 
        }

        switch (msg.getType()) {
            case TEXT:
                System.out.println(" Mensaje de " + msg.getSender().getUsername() + ": " + msg.getContent());
                broadcastMessage(msg); //  Enviar a TODOS los clientes
                return msg;
                
            case CREATE_GROUP:
                Message groupMsg = handleCreateGroup(msg);
                if (groupMsg != null) {
                    broadcastMessage(groupMsg);
                }
                return groupMsg;
                
            case JOIN_GROUP:
                Message joinMsg = handleJoinGroup(msg);
                if (joinMsg != null) {
                    broadcastMessage(joinMsg);
                }
                return joinMsg;
                
            case UPDATE_GROUPS:
                return createGroupsUpdateMessage();

            default:
                System.err.println(" Tipo de mensaje desconocido: " + msg.getType());
                return null;
        }
    }

    //  NUEVO: Método para enviar mensajes a TODOS los clientes
    private void broadcastMessage(Message message) {
        if (server != null) {
            server.broadcastMessage(message);
        }
    }

    //  NUEVO: Manejar desconexión de usuarios
    public void userDisconnected(WebSocket conn) {
        User user = connectedUsers.remove(conn);
        if (user != null) {
            System.out.println("❌ Usuario desconectado: " + user.getUsername());
            // Notificar a todos sobre la desconexión
            Message leaveMsg = new Message(user, user.getUsername() + " ha abandonado el chat", Message.MessageType.TEXT);
            broadcastMessage(leaveMsg);
        }
    }
    
    private Message handleCreateGroup(Message msg) {
        String groupName = msg.getContent();
        if (groupName != null && !groupName.trim().isEmpty()) {
            UserGroups newGroup = new UserGroups(groupName);
            availableGroups.add(newGroup);
            System.out.println("Nuevo grupo creado: " + groupName);
            // Notifica a todos los clientes sobre el nuevo grupo
            return createGroupsUpdateMessage(); 
        }
        return null;
    }

    private Message handleJoinGroup(Message msg) {
        String groupName = msg.getContent();
        User sender = msg.getSender();
        
        // Busca el grupo
        UserGroups group = availableGroups.stream()
                .filter(g -> g.getNombreGrupo().equals(groupName))
                .findFirst()
                .orElse(null);

        if (group != null && sender != null) {
            group.addUser(sender);
            System.out.println("Usuario " + sender.getUsername() + " se unió a " + groupName);
            // Notifica a todos los clientes que la lista de grupos ha cambiado
            return createGroupsUpdateMessage(); 
        }
        return null;
    }

    // Crea un mensaje especial con la lista de todos los grupos disponibles
    private Message createGroupsUpdateMessage() {
        Message updateMsg = new Message(null, "Lista de grupos actualizada", Message.MessageType.UPDATE_GROUPS);
        updateMsg.setGroupList(new ArrayList<>(availableGroups)); // ✅ CORRECCIÓN
        return updateMsg;
    }
}
