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
    // ✅ NUEVO: Lista de todos los usuarios registrados
    private final List<User> allUsers;

    public ChatController() {
        this.connectedUsers = new ConcurrentHashMap<>();
        this.availableGroups = new CopyOnWriteArrayList<>();
        this.allUsers = new CopyOnWriteArrayList<>(); // ✅ Inicializar lista de usuarios

        // Grupo por defecto: "General"
        UserGroups general = new UserGroups("General");
        availableGroups.add(general);
    }

    // Método principal para procesar los mensajes entrantes
    public Message processMessage(WebSocket conn, Message msg) {

        System.out.println("CONEXIÓN DE USER!!!!!!!!!!!");
        System.out.println(conn.getRemoteSocketAddress());
        // ✅ CORRECCIÓN: Verificar que el tipo no sea null
        if (msg.getType() == null) {
            System.err.println("❌ Error: Tipo de mensaje es null. Mensaje: " + msg);
            return null;
        }

        // Asigna el usuario de la conexión si es un mensaje de 'login' (o el primer
        // mensaje)
        if (!connectedUsers.containsKey(conn) && msg.getSender() != null) {
            System.out.println("Entre al método!!");
            return handleUserConnection(conn, msg.getSender()); // ✅ Usar nuevo método
        }

        // ✅ CORRECCIÓN: Switch seguro
        switch (msg.getType()) {
            case TEXT:
                System.out.println("💬 Mensaje de texto de " + msg.getSender().getUsername() + ": " + msg.getContent());
                // ✅ CORRECCIÓN IMPORTANTE: No reenviar el mensaje original
                Message notification = new Message(msg.getSender(), msg.getContent(), Message.MessageType.TEXT);
                notification.setGroup(msg.getGroup()); // Preservar info de grupo si existe
                return notification;

            case CREATE_GROUP:
                return handleCreateGroup(msg);

            case JOIN_GROUP:
                return handleJoinGroup(msg);

            case UPDATE_GROUPS:
                return createGroupsUpdateMessage();

            case UPDATE_USERS:
                return createUsersUpdateMessage(); // ✅ Ahora existe

            default:
                System.err.println("❌ Tipo de mensaje desconocido: " + msg.getType());
                return null;
        }
    }

    // ✅ NUEVO MÉTODO: Manejar conexión de usuario
    private Message handleUserConnection(WebSocket conn, User user) {
        connectedUsers.put(conn, user);

        // Agregar a lista global de usuarios si no existe
        if (!userExists(user.getUsername())) {
            allUsers.add(user);
        }

        System.out.println("✅ Usuario " + user.getUsername() + " conectado.");
        System.out.println("📊 Usuarios conectados: " + getConnectedUsernames());

        // Enviar actualización de usuarios a todos
        return createUsersUpdateMessage();
    }

    // ✅ NUEVO MÉTODO: Crear mensaje de actualización de usuarios
    private Message createUsersUpdateMessage() {
        Message updateMsg = new Message(null, "Lista de usuarios actualizada", Message.MessageType.UPDATE_USERS);

        // Enviar lista de usuarios en el contenido (como string separado por comas)
        List<String> usernames = getConnectedUsernames();
        updateMsg.setContent(String.join(",", usernames));

        return updateMsg;
    }

    // ✅ NUEVO MÉTODO: Verificar si usuario existe
    private boolean userExists(String username) {
        return allUsers.stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    // ✅ NUEVO MÉTODO: Obtener nombres de usuarios conectados
    private List<String> getConnectedUsernames() {
        List<String> usernames = new ArrayList<>();
        for (User user : connectedUsers.values()) {
            usernames.add(user.getUsername());
        }
        return usernames;
    }

    private Message handleCreateGroup(Message msg) {
        String groupName = msg.getContent();
        if (groupName != null && !groupName.trim().isEmpty()) {
            UserGroups newGroup = new UserGroups(groupName);
            availableGroups.add(newGroup);
            System.out.println("Nuevo grupo creado: " + groupName);
            return createGroupsUpdateMessage();
        }
        return null;
    }

    private Message handleJoinGroup(Message msg) {
        String groupName = msg.getContent();
        User sender = msg.getSender();

        UserGroups group = availableGroups.stream()
                .filter(g -> g.getNombreGrupo().equals(groupName))
                .findFirst()
                .orElse(null);

        if (group != null && sender != null) {
            group.addUser(sender);
            System.out.println("Usuario " + sender.getUsername() + " se unió a " + groupName);
            return createGroupsUpdateMessage();
        }
        return null;
    }

    // Crea un mensaje especial con la lista de todos los grupos disponibles
    private Message createGroupsUpdateMessage() {
        Message updateMsg = new Message(null, "Lista de grupos actualizada", Message.MessageType.UPDATE_GROUPS);
        updateMsg.setGroupList(new ArrayList<>(availableGroups));
        return updateMsg;
    }

    // ✅ NUEVO: Obtener usuario de una conexión WebSocket
    public User getUserForConnection(WebSocket conn) {
        return connectedUsers.get(conn);
    }

    // ✅ NUEVO: Verificar si usuario está en grupo
    public boolean isUserInGroup(String username, String groupName) {
        for (UserGroups group : availableGroups) {
            if (group.getNombreGrupo().equals(groupName)) {
                return group.getListaUsuarios().stream()
                        .anyMatch(user -> user.getUsername().equals(username));
            }
        }
        return false;
    }

    // ✅ NUEVO: Manejar desconexión
    public void handleDisconnection(WebSocket conn) {
        User disconnectedUser = connectedUsers.remove(conn);
        if (disconnectedUser != null) {
            System.out.println("Usuario desconectado: " + disconnectedUser.getUsername());
        }
    }

    // ✅ NUEVO: Obtener lista de usuarios para API REST
    public List<String> getAllUsernames() {
        List<String> usernames = new ArrayList<>();
        for (User user : allUsers) {
            usernames.add(user.getUsername());
        }
        return usernames;
    }

    // ✅ NUEVO: Obtener lista de grupos
    public List<UserGroups> getAvailableGroups() {
        return new ArrayList<>(availableGroups);
    }
}