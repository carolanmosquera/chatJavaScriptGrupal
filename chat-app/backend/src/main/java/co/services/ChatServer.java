package com.chat.service;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.chat.model.Message;
import com.chat.model.User;
import com.chat.model.UserGroups;

public class ChatServer {
    //Puerto en el que el servidor escuchará las conexiones.
    private static final int PORT = 8080;
    //Socket del servidor que acepta conexiones de clientes.
    private ServerSocket serverSocket;
    //ExecutorService para manejar hilos de clientes (un hilo por cliente).
    private ExecutorService pool;
    //lista de clientes conectados (Lista de manejadores de clientes conectados.)
    private List<ClientHandler> clients;
    //Lista de usuarios conectados.
    private List<User> connectedUsers;
    //Lista de grupos de chat.
    private List<UserGroups> grupos; // Lista centralizada de grupos

    public ChatServer() {
        //Inicializa las listas con CopyOnWriteArrayList para seguridad en entornos multihilo.
        this.clients = new CopyOnWriteArrayList<>();
        this.connectedUsers = new CopyOnWriteArrayList<>();
        this.grupos = new CopyOnWriteArrayList<>(); // Inicializar lista de grupos
        //Crea un ExecutorService de tipo CachedThreadPool que crea hilos según sea necesario.
        this.pool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            //Crea el ServerSocket en el puerto especificado.
            serverSocket = new ServerSocket(PORT);
            System.out.println(" Servidor de chat iniciado en puerto " + PORT);
            
            //Bucle infinito que acepta conexiones de clientes.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                //Por cada cliente, crea un ClientHandler, lo añade a la lista y lo ejecuta en un hilo.
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                pool.execute(clientHandler);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Agrega un nuevo grupo y notifica a todos los clientes
     */
    public synchronized void addGroup(UserGroups nuevoGrupo) {
        // Verificar si el grupo ya existe
        //Verifica si el grupo ya existe (por nombre, ignorando mayúsculas/minúsculas).
        boolean grupoExiste = grupos.stream().anyMatch(g -> g.getNombreGrupo().equalsIgnoreCase(nuevoGrupo.getNombreGrupo()));
        
        //Si no existe, lo añade y notifica a todos los clientes.
        if (!grupoExiste) {
            grupos.add(nuevoGrupo);

            System.out.println(" Nuevo grupo creado: " + nuevoGrupo.getNombreGrupo() + " por " + nuevoGrupo.getListaUsuarios().get(0).getUsername());
            
            // Notificar a TODOS los clientes sobre el nuevo grupo
            broadcastGroupsToAll();
        } else {
            System.out.println(" Grupo ya existe: " + nuevoGrupo.getNombreGrupo());
        }
    }

    /**
     * Actualiza un grupo existente cuando un usuario se une
     */
    public synchronized void updateGroup(UserGroups grupoActualizado) {
        //Busca un grupo por nombre
        for (int i = 0; i < grupos.size(); i++) {
            UserGroups grupoExistente = grupos.get(i);
            // lo actualiza con la nueva información
            if (grupoExistente.getNombreGrupo().equals(grupoActualizado.getNombreGrupo())) {
                // Reemplazar el grupo existente con el actualizado
                grupos.set(i, grupoActualizado);

                System.out.println(" Grupo actualizado: " + grupoActualizado.getNombreGrupo() + " - Miembros: " + grupoActualizado.getListaUsuarios().size());
                
                // Notificar a TODOS los clientes
                broadcastGroupsToAll();
                return;
            }
        }
    }

    /**
     * Envía la lista actualizada de grupos a TODOS los clientes conectados
     */
    public void broadcastGroupsToAll() {
        // Crear copia de la lista de grupos para evitar problemas de concurrencia
        //grupos es la variable que se esta actualizando
        List<UserGroups> gruposCopia = new ArrayList<>(grupos);
        
        //Crea un mensaje de tipo UPDATE_GROUPS con la lista actual de grupos.
        Message gruposMessage = new Message(null, "Actualización de grupos", Message.MessageType.UPDATE_GROUPS);
        //utiliza el mensaje para enviar la nueva lista de grupos 
        gruposMessage.setGroupList((ArrayList) gruposCopia);
        
        System.out.println(" Transmitiendo " + gruposCopia.size() + " grupos a " + clients.size() + " clientes");
        
        //Envía este mensaje a todos los clientes.
        for (ClientHandler client : clients) {
            try {
                client.sendMessage(gruposMessage);
            } catch (Exception e) {
                System.err.println(" Error enviando grupos a cliente: " + e.getMessage());
            }
        }
    }

    /**
     * Envía la lista de grupos solo a un cliente específico (cuando se conecta)
     */
    //Envía la lista actual de grupos a un cliente específico (cuando se conecta).
    public void sendGroupsToClient(ClientHandler client) {
        //se obtiene todos los grupos actuales
        List<UserGroups> gruposCopia = new ArrayList<>(grupos);
        //se crea el mensaje
        Message gruposMessage = new Message(null, "Lista de grupos inicial", Message.MessageType.JOIN_GROUP);
        gruposMessage.setGroupList((ArrayList) gruposCopia);
        
        try {
            //se le envia
            client.sendMessage(gruposMessage);
            System.out.println("Enviando " + gruposCopia.size() + " grupos a " + client.getUser().getUsername());
        } catch (Exception e) {
            System.err.println("Error enviando grupos a " + client.getUser().getUsername() + ": " + e.getMessage());
        }
    }

    //Transmite un mensaje a todos los clientes excepto al remitente.
    public void broadcastMessage(Message message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    //agrega un nuevo usuario y le notifica a todos
    public void addUser(User user) {
        connectedUsers.add(user);
        broadcastUserList();
    }

    //elimina un usuario y le notifica a todos
    public void removeUser(User user) {
        connectedUsers.removeIf(u -> u.getId().equals(user.getId()));
        broadcastUserList();
    }

    //remueve un cliente 
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    //metodo para actulizar lista de usuarios a todos los clientes
    private void broadcastUserList() {
        List<User> usuariosCopia = new ArrayList<>(connectedUsers);
        for (ClientHandler client : clients) {
            client.sendUserList(usuariosCopia);
        }
    }

    //inicializa el servidor
    public static void main(String[] args) {
        new ChatServer().start();
    }

 
    //clase interna para manejar clientes individuakes
    //Maneja la comunicación con un cliente específico.
    private class ClientHandler implements Runnable {

        private Socket socket;
        private ChatServer server;
         //Flujo de salida para enviar objetos al servidor.
        private ObjectOutputStream out;
        //Flujo de entrada para recibir objetos del servidor.
        private ObjectInputStream in;
        private User user;

        //define el socket y el servidor 
        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        public Socket getSocket() {
            return socket;
        }

        public User getUser() {
            return user;
        }

        @Override
        public void run() {
            try {
                //utiliza el socket para inicializar el flujo de entrada y salida
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // lee el usuario del cliente para enviarlo al servidor
                user = (User) in.readObject();
                //agrega el usuario al servidor
                server.addUser(user);

                System.out.println("👤 Usuario conectado: " + user.getUsername());

                //  ENVIAR GRUPOS AL CLIENTE CUANDO SE CONECTA
                server.sendGroupsToClient(this);

                //Bucle para recibir mensajes y manejarlos.
                // Escuchar mensajes
                while (true) {
                    //lee el mensaje 
                    Message message = (Message) in.readObject();
                    //lo maneja
                    handleMessage(message);
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println(" Usuario desconectado: " + (user != null ? user.getUsername() : "Desconocido"));
            } finally {
                //cuando ya se desconecta
                try {
                    if (user != null) {
                        server.removeUser(user);
                    }
                    server.removeClient(this);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //etodo handle para manejar los mensajes
        //segun tipo de enum
        private void handleMessage(Message message) {
            switch (message.getType()) {
                case CREATE_GROUP:
                    //los mensajes de tipo crear grupo ya llevan un grupo internamente
                    handleCreateGroup(message);
                    break;
                    
                case JOIN_GROUP:
                    //los mesnajes de unirse al grupo llevan un grupo y la informacion del usuario que se quiere unir
                    handleJoinGroup(message);
                    break;
                    
                default:
                    // Mensajes normales de texto/audio
                    server.broadcastMessage(message, this);
                    break;
            }
        }

        /**
         * Maneja la creación de un nuevo grupo
         */
        private void handleCreateGroup(Message message) {
            //se obtiene el grupo nuevo del mensaje
            UserGroups nuevoGrupo = message.getGroup();
            //si no esta vacio lo agrega al servidor
            if (nuevoGrupo != null) {
                server.addGroup(nuevoGrupo);
                
                // se crea un mensaje al sistema para notificar
                Message systemMessage = new Message(
                    message.getSender(),
                    " " + message.getSender().getUsername() + " creó el grupo '" + nuevoGrupo.getNombreGrupo() + "'",
                    Message.MessageType.SYSTEM
                );
                //se le envia el mensaje del sistema al servudor
                server.broadcastMessage(systemMessage, this);
            }
        }

        /**
         * Maneja cuando un usuario se une a un grupo
         */
        private void handleJoinGroup(Message message) {
            //extrae el grupo del mensjaw
            UserGroups grupoParaUnirse = message.getGroup();
            //extrae el usuario que se quiere unnir
            User usuario = message.getSender();
            
            if (grupoParaUnirse != null && usuario != null) {
                // Buscar el grupo en la lista del servidor
                for (UserGroups grupoExistente : server.grupos) {
                    if (grupoExistente.getNombreGrupo().equals(grupoParaUnirse.getNombreGrupo())) {
                        
                        // Verificar si el usuario ya está en el grupo
                        boolean yaEsMiembro = grupoExistente.getListaUsuarios().stream().anyMatch(u -> u.getId().equals(usuario.getId()));
                      
                        //si no esta
                        if (!yaEsMiembro) {
                            // Agregar usuario al grupo
                            grupoExistente.addUser(usuario);

                            System.out.println(" " + usuario.getUsername() + " se unió al grupo " + grupoExistente.getNombreGrupo());
                            
                            // Actualizar el grupo en el servidor
                            server.updateGroup(grupoExistente);
                            
                            // Enviar mensaje de sistema
                            Message systemMessage = new Message(
                                usuario,
                                " " + usuario.getUsername() + " se unió al grupo '" + grupoExistente.getNombreGrupo() + "'",
                                Message.MessageType.SYSTEM
                            );
                            server.broadcastMessage(systemMessage, this);
                        } else {
                            System.out.println(" " + usuario.getUsername() + " ya está en el grupo " + grupoExistente.getNombreGrupo());
                        }
                        break;
                    }
                }
            }
        }

        //metodo para enviar un mensaje al servior
        public void sendMessage(Message message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println(" Error enviando mensaje a " + (user != null ? user.getUsername() : "cliente"));
            }
        }

        //metodo para enviar lista de usuarios
        public void sendUserList(List<User> users) {
            try {
                out.writeObject(users);
                out.flush();
            } catch (IOException e) {
                System.err.println(" Error enviando lista de usuarios a " + (user != null ? user.getUsername() : "cliente"));
            }
        }
    }
}
