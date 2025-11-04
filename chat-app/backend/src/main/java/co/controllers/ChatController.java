package co.controllers;

import co.model.Message;
import co.model.User;
import co.model.UserGroups;
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
    
    // Método principal para procesar los mensajes entrantes
    public Message processMessage(WebSocket conn, Message msg) {
        // Asigna el usuario de la conexión si es un mensaje de 'login' (o el primer mensaje)
        if (!connectedUsers.containsKey(conn) && msg.getSender() != null) {
            connectedUsers.put(conn, msg.getSender());
            System.out.println("Usuario " + msg.getSender().getUsername() + " conectado.");
            // Envía la lista de grupos al nuevo usuario
            return createGroupsUpdateMessage(); 
        }

        // Procesa el mensaje basado en su tipo
        switch (msg.getType()) {
            case TEXT:
                // Reenviar el mensaje de texto a todos (por ahora)
                return msg; 
                
            case CREATE_GROUP:
                return handleCreateGroup(msg);
                
            case JOIN_GROUP:
                return handleJoinGroup(msg);
                
            case UPDATE_GROUPS:
                return createGroupsUpdateMessage();

            default:
                // Manejar otros tipos de mensajes o errores
                System.err.println("Tipo de mensaje desconocido: " + msg.getType());
                return null;
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
