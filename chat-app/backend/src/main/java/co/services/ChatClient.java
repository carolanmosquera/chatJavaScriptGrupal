package com.chat.service;

import java.io.*;
import java.net.*;
import java.util.List;
import com.chat.model.Message;
import com.chat.model.User;
import com.chat.model.UserGroups;
import com.chat.controller.ChatController;

//esta es la clase que se utiliza para definir el puerto y direccion ip y conexion con el servidor
public class ChatClient {
    //Dirección IP o nombre del host del servidor.
    private String host;
    //Puerto del servidor.
    private int port;
    //Socket para la comunicación con el servidor.
    private Socket socket;
    //Flujo de salida para enviar objetos al servidor.
    private ObjectOutputStream out;
    //Flujo de entrada para recibir objetos del servidor.
    private ObjectInputStream in;
    //Referencia al controlador de la interfaz de usuario.
    private ChatController controller;
    // Usuario actual que está utilizando el cliente.
    private User currentUser;

    //constructor
    public ChatClient(String host, int port, ChatController controller) {
        this.host = host;
        this.port = port;
        this.controller = controller;
    }

    //Método connect: Intenta establecer una conexión con el servidor.
    public void connect() {
        try {
            //Crea un Socket con el host y puerto proporcionados.
            socket = new Socket(host, port);
            //utiliza el socket para inicializar el flujo de entrada y salida
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Enviar información del usuario al servidor
            //Envía el objeto currentUser al servidor para identificarse.
            out.writeObject(currentUser);
            out.flush();

            // Hilo para escuchar mensajes (audios)
            //Inicia un hilo que ejecuta el método listenForMessages para escuchar mensajes entrantes del servidor.
            new Thread(this::listenForMessages).start();

        } catch (IOException e) {
            e.printStackTrace();
            controller.showError("No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    //Método setCurrentUser: Establece el usuario actual.
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    //metodo para enviar mensaje (tipo de mensaje que se va a enviar (contiene enum de tipo))
    public void sendMessage(Message message) {
        try {
            //Envía un mensaje (objeto Message) al servidor a través del flujo de salida.
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            controller.showError("Error al enviar mensaje: " + e.getMessage());
        }
    }

    //Método listenForMessages: Escucha constantemente mensajes entrantes del servidor.
    private void listenForMessages() {
        try {
            while (true) {
                //Lee un objeto del flujo de entrada.
                Object obj = in.readObject();

                //Si el objeto es una instancia de Message, lo procesa con handleIncomingMessage.
                if (obj instanceof Message) {
                    Message message = (Message) obj;
                    handleIncomingMessage(message);

                //Si el objeto es una List, verifica el tipo de elementos en la lista:
                } else if (obj instanceof List) {
                    // Verificar el tipo de lista

                    //Si es una lista de User, actualiza la lista de usuarios en el controlador.
                    List<?> lista = (List<?>) obj;
                    if (!lista.isEmpty()) {
                        if (lista.get(0) instanceof User) {
                            // Es lista de usuarios
                            @SuppressWarnings("unchecked")
                            List<User> users = (List<User>) lista;
                            controller.updateUserList(users);

                            //Si es una lista de UserGroups, actualiza la lista de grupos en el controlador.
                        } else if (lista.get(0) instanceof UserGroups) {
                            // Es lista de grupos
                            @SuppressWarnings("unchecked")
                            List<UserGroups> groups = (List<UserGroups>) lista;
                            controller.updateGroupList(groups);
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("🔌 Cliente desconectado del servidor.");
        }
    }

    /**
     * Maneja diferentes tipos de mensajes entrantes
     */
    //Método handleIncomingMessage: Maneja los mensajes entrantes según su tipo.
    private void handleIncomingMessage(Message message) {
        //se realiza el switch con el enum de message 
        switch (message.getType()) {

            //actualiza la lista de grupo con la nueva informacion
            case JOIN_GROUP:
                // El servidor envía la lista completa de grupos
                if (((Message) message).GetGruposLista() != null) {
                    controller.updateGroupList(((Message) message).GetGruposLista());
                }
                break;
            //actualiza la lista de grupo con la nueva informacion
            case UPDATE_GROUPS:
            if (message.GetGruposLista() != null) {
                controller.updateGroupList(message.GetGruposLista());
            }
            break;
                
            default:
                // Mensajes normales (texto, audio, sistema)
                controller.addMessage(message);
                break;
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
