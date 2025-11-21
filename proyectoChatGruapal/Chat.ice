module Chat {
    
    // DTO para mensajes
    struct MessageDTO {
        string id;
        string senderId;
        string senderName;
        string content;
        long timestamp;
        int type; // 0=TEXT, 1=SYSTEM
    };
    
    // DTO para usuarios
    struct UserDTO {
        string id;
        string username;
        bool isOnline;
    };
    
    // Secuencias
    sequence<MessageDTO> MessageList;
    sequence<UserDTO> UserList;
    
    // Observer para actualizaciones en tiempo real
    interface Observer {
        void updateMessages(MessageList messages);
        void updateUsers(UserList users);
    };
    
    // Subject para patrón Observer
    interface Subject {
        void attachObserver(Observer* obs);
        void detachObserver(Observer* obs);
    };
    
    // Servicio principal del chat
    interface ChatService {
        UserDTO joinChat(string username);
        void sendMessage(string userId, string content);
        MessageList getMessages();
        UserList getUsers();
        void leaveChat(string userId);
    };
};