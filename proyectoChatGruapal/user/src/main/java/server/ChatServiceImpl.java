package server;

import Chat.*;
import model.*;
import com.zeroc.Ice.Current;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServiceImpl implements ChatService, Subject {
    
    // Almacenamiento usando las clases del modelo existente
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final List<Message> messages = new CopyOnWriteArrayList<>();
    private final Map<String, UserGroup> groups = new ConcurrentHashMap<>();
    private final List<ObserverPrx> observers = new CopyOnWriteArrayList<>();
    
    // Contadores para IDs
    private int userCounter = 0;
    private int messageCounter = 0;
    private int groupCounter = 0;

    // ========== MÉTODOS DE USUARIOS ==========
    
    @Override
    public synchronized UserDTO joinChat(String username, Current current) {
        String userId = "user_" + (++userCounter);
        
        // Crear usuario usando el modelo existente
        User newUser = new User(userId, username);
        newUser.setOnline(true);
        newUser.setConnectedAt(System.currentTimeMillis());
        
        users.put(userId, newUser);
        
        // Crear mensaje de sistema
        Message systemMsg = new Message(
            "msg_" + (++messageCounter),
            "system",
            "System",
            username + " se ha unido al chat",
            MessageType.SYSTEM
        );
        messages.add(systemMsg);
        
        // Notificar a todos los observadores
        notifyObservers();
        
        System.out.println("Usuario unido: " + username + " (ID: " + userId + ")");
        
        // Retornar DTO
        return newUser.toDTO();
    }

    @Override
    public synchronized void leaveChat(String userId, Current current) {
        User user = users.get(userId);
        if (user != null) {
            user.setOnline(false);
            
            // Mensaje de sistema
            Message systemMsg = new Message(
                "msg_" + (++messageCounter),
                "system",
                "System",
                user.getUsername() + " ha salido del chat",
                MessageType.SYSTEM
            );
            messages.add(systemMsg);
            
            // Remover de grupos
            for (UserGroup group : groups.values()) {
                group.removeParticipant(user);
            }
            
            notifyObservers();
            System.out.println("Usuario salió: " + user.getUsername());
        }
    }

    @Override
    public UserDTO[] getUsers(Current current) {
        return users.values().stream()
            .map(User::toDTO)
            .toArray(UserDTO[]::new);
    }

    // ========== MÉTODOS DE MENSAJES ==========

    @Override
    public synchronized void sendMessage(String userId, String content, MessageTypeEnum type, Current current) {
        User user = users.get(userId);
        if (user == null) {
            System.err.println("Usuario no encontrado: " + userId);
            return;
        }

        // Convertir MessageTypeEnum de Ice a MessageType del modelo
        MessageType messageType = MessageType.fromIceEnum(type);

        Message message = new Message(
            "msg_" + (++messageCounter),
            userId,
            user.getUsername(),
            content,
            messageType
        );
        
        messages.add(message);
        notifyObservers();
        
        System.out.println("[" + user.getUsername() + "]: " + content);
    }

    @Override
    public MessageDTO[] getMessages(Current current) {
        return messages.stream()
            .map(Message::toDTO)
            .toArray(MessageDTO[]::new);
    }

    // ========== MÉTODOS DE GRUPOS ==========

    @Override
    public synchronized GroupDTO createGroup(String groupName, String creatorId, Current current) {
        String groupId = "group_" + (++groupCounter);
        
        // Crear grupo usando el modelo existente
        UserGroup newGroup = new UserGroup(groupId, groupName);
        
        // Agregar el creador al grupo
        User creator = users.get(creatorId);
        if (creator != null) {
            newGroup.addParticipant(creator);
            
            // Agregar grupo al mapa de grupos del usuario
            creator.getGroups().put(groupId, newGroup);
            
            // Mensaje del sistema
            Message systemMsg = new Message(
                "msg_" + (++messageCounter),
                "system",
                "System",
                creator.getUsername() + " creó el grupo: " + groupName,
                MessageType.SYSTEM
            );
            messages.add(systemMsg);
        }
        
        groups.put(groupId, newGroup);
        
        notifyObservers();
        System.out.println("Grupo creado: " + groupName + " (ID: " + groupId + ")");
        
        return newGroup.toDTO();
    }

    @Override
    public synchronized void joinGroup(String groupId, String userId, Current current) {
        UserGroup group = groups.get(groupId);
        User user = users.get(userId);
        
        if (group == null || user == null) {
            System.err.println("Grupo o usuario no encontrado");
            return;
        }
        
        // Verificar si ya es miembro
        if (!group.hasParticipant(userId)) {
            group.addParticipant(user);
            user.getGroups().put(groupId, group);
            
            Message systemMsg = new Message(
                "msg_" + (++messageCounter),
                "system",
                "System",
                user.getUsername() + " se unió al grupo: " + group.getName(),
                MessageType.SYSTEM
            );
            messages.add(systemMsg);
            
            notifyObservers();
            System.out.println(user.getUsername() + " se unió al grupo " + group.getName());
        }
    }

    @Override
    public GroupDTO[] getGroups(Current current) {
        return groups.values().stream()
            .map(UserGroup::toDTO)
            .toArray(GroupDTO[]::new);
    }

    // ========== MÉTODOS DE LLAMADAS DE VOZ ==========

    @Override
    public void startVoiceCall(String userId, String targetUserId, Current current) {
        User caller = users.get(userId);
        User target = users.get(targetUserId);
        
        if (caller != null && target != null) {
            Message callMsg = new Message(
                "msg_" + (++messageCounter),
                userId,
                caller.getUsername(),
                "Llamada de voz iniciada con " + target.getUsername(),
                MessageType.VOICECALL
            );
            messages.add(callMsg);
            notifyObservers();
            System.out.println("Llamada iniciada: " + caller.getUsername() + " -> " + target.getUsername());
        }
    }

    @Override
    public void endVoiceCall(String userId, Current current) {
        User user = users.get(userId);
        if (user != null) {
            Message callMsg = new Message(
                "msg_" + (++messageCounter),
                userId,
                user.getUsername(),
                "Llamada de voz finalizada",
                MessageType.VOICECALL
            );
            messages.add(callMsg);
            notifyObservers();
            System.out.println("Llamada finalizada: " + user.getUsername());
        }
    }

    // ========== PATRÓN OBSERVER ==========

    @Override
    public void attachObserver(ObserverPrx obs, Current current) {
        if (!observers.contains(obs)) {
            observers.add(obs);
            System.out.println("Observer agregado. Total: " + observers.size());
        }
    }

    @Override
    public void detachObserver(ObserverPrx obs, Current current) {
        observers.remove(obs);
        System.out.println("Observer removido. Total: " + observers.size());
    }

    private void notifyObservers() {
        // Convertir modelos a DTOs para Ice
        MessageDTO[] msgArray = messages.stream()
            .map(Message::toDTO)
            .toArray(MessageDTO[]::new);
            
        UserDTO[] userArray = users.values().stream()
            .map(User::toDTO)
            .toArray(UserDTO[]::new);
        
        List<ObserverPrx> toRemove = new ArrayList<>();
        
        for (ObserverPrx observer : observers) {
            try {
                observer.updateMessagesAsync(msgArray);
                observer.updateUsersAsync(userArray);
            } catch (Exception e) {
                System.err.println("Error notificando observer: " + e.getMessage());
                toRemove.add(observer);
            }
        }
        
        // Remover observers inválidos
        observers.removeAll(toRemove);
    }

    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Obtener usuario por ID
     */
    public User getUser(String userId) {
        return users.get(userId);
    }
    
    /**
     * Obtener grupo por ID
     */
    public UserGroup getGroup(String groupId) {
        return groups.get(groupId);
    }
    
    /**
     * Obtener todos los mensajes como modelos (no DTOs)
     */
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
    
    /**
     * Obtener todos los usuarios como modelos (no DTOs)
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    /**
     * Obtener mensajes filtrados por tipo
     */
    public List<Message> getMessagesByType(MessageType type) {
        List<Message> filtered = new ArrayList<>();
        for (Message msg : messages) {
            if (msg.getType() == type) {
                filtered.add(msg);
            }
        }
        return filtered;
    }
    
    /**
     * Verificar si un usuario existe y está online
     */
    public boolean isUserOnline(String userId) {
        User user = users.get(userId);
        return user != null && user.isOnline();
    }
}
