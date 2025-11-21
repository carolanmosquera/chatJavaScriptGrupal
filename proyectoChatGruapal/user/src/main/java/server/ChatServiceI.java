package server;

public class ChatServiceI implements ChatService {

    private final Map<String, UserDTO> users = new HashMap<>();
    private final List<MessageDTO> messages = new ArrayList<>();

    @Override
    public UserDTO joinChat(String username, Current current) {
        UserDTO user = new UserDTO(UUID.randomUUID().toString(), username);
        users.put(user.id, user);
        return user;
    }

    @Override
    public void sendMessage(String userId, String content, MessageTypeEnum type, Current current) {
        MessageDTO msg = new MessageDTO(userId, content, type, System.currentTimeMillis());
        messages.add(msg);
    }

    @Override
    public MessageDTO[] getMessages(Current current) {
        return messages.toArray(new MessageDTO[0]);
    }

    @Override
    public UserDTO[] getUsers(Current current) {
        return users.values().toArray(new UserDTO[0]);
    }

    // Y así con todos los métodos restantes
}
