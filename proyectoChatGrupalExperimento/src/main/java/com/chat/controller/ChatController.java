package com.chat.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.chat.model.User;
import com.chat.model.UserGroups;
import com.chat.model.AudioMessage;
import com.chat.model.Message;
import com.chat.service.ChatClient;
import com.chat.service.AudioRecorder;
import com.chat.service.AudioSenderCall;
import com.chat.service.AudioPlayer;
import com.chat.service.AudioReceiverCall;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

/*
FXML: Para inyección de componentes de la UI

FXMLLoader: Para cargar archivos FXML

Parent, Scene: Para manejar la escena gráfica

Alert, Button, TextField: Componentes de UI

Stage: Ventana de la aplicación
 */

//esta es la clase que se utiliza para llamar a los servicios y elementos graficos
public class ChatController {
    
    @FXML private Label usernameLabel;
    @FXML private Label userCountLabel;
    @FXML private ListView<Message> messagesListView;
    @FXML private ListView<User> usersListView;
    @FXML private TextArea messageTextArea;
    @FXML private Button sendTextButton;
    @FXML private Button sendAudioButton;
    @FXML private Button logoutButton;

    @FXML private Button callButton;

    @FXML private Button createGroupButton;
    @FXML private ListView<UserGroups> groupsListView;
    @FXML private Button joinGroupButton;
    
   
    private User currentUser;
    private ChatClient chatClient;
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private ObservableList<Message> messages;
    private ObservableList<User> connectedUsers;

    private List<UserGroups> grupos = new ArrayList<>();
    private ObservableList<UserGroups> observableGroups;

    //llamadas
    private AudioSenderCall audioSenderCall;
    private AudioReceiverCall audioReceiverCall;
    private boolean enLlamada = false;
    private String ipDestinoLlamada = "localhost"; // Cambiar según necesidad
    private int puertoEnvio = 5000;
    private int puertoRecepcion = 5001;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    //inicializa chat
    public void initializeChat() {
        try {
            // Inicializar listas observables
            messages = FXCollections.observableArrayList();
            connectedUsers = FXCollections.observableArrayList();
            observableGroups = FXCollections.observableArrayList();
            
            // Configurar ListViews
            messagesListView.setItems(messages);
            usersListView.setItems(connectedUsers);
            groupsListView.setItems(observableGroups);
            
            // Configurar cómo mostrar los mensajes
            messagesListView.setCellFactory(param -> new ListCell<Message>() {
                @Override
                protected void updateItem(Message message, boolean empty) {
                    super.updateItem(message, empty);
                    if (empty || message == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(message.toString());
                        
                        // Estilo diferente para mensajes de audio
                        if (message.getType() == Message.MessageType.AUDIO) {
                            setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                        } else if (message.getType() == Message.MessageType.SYSTEM) {
                            setStyle("-fx-text-fill: #FF9800; -fx-font-style: italic;");
                        } else {
                            setStyle("-fx-text-fill: #000000;");
                        }
                    }
                }
            });
            
            // Configurar cómo mostrar usuarios
            usersListView.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getUsername());
                        setGraphic(null);
                    }
                }
            });

            //configuracion para mostrar grupos 
            groupsListView.setCellFactory(param -> new ListCell<UserGroups>() {
                @Override
                protected void updateItem(UserGroups group, boolean empty) {
                    super.updateItem(group, empty);
                    if (empty || group == null) {
                        setText(null);
                    } else {
                        setText(group.getNombreGrupo() + " (" + group.getListaUsuarios().size() + " miembros)");
                    }
                }
            });

            //listener para detectar cuando el usuario selecciona un grupo y mostrar sus miembros en una ventana informativa:
            groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldGroup, newGroup) -> {
            if (newGroup != null) {
                StringBuilder miembros = new StringBuilder();
                for (User u : newGroup.getListaUsuarios()) {
                    miembros.append("• ").append(u.getUsername()).append("\n");
                }

                    showInfo("Grupo: " + newGroup.getNombreGrupo() + "\n\nMiembros:\n" + miembros);
                }
            });

            // Actualizar UI
            usernameLabel.setText(currentUser.getUsername());
            userCountLabel.setText("Usuarios: 1");
            
            // Inicializar servicios de audio
            audioRecorder = new AudioRecorder();
            audioPlayer = new AudioPlayer();
            
            // Conectar al servidor de chat (en otro hilo para no bloquear la UI)
            new Thread(() -> {
                try {
                    chatClient = new ChatClient("localhost", 8080, this);
                    chatClient.setCurrentUser(currentUser);
                    chatClient.connect();
                } catch (Exception e) {
                    showError("Error conectando al servidor: " + e.getMessage());
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error al inicializar el chat: " + e.getMessage());
        }
    }
    
    //manda texto, es el que maneja texto
    @FXML
    private void handleSendText() {
        String text = messageTextArea.getText().trim();
        if (!text.isEmpty()) {
            Message message = new Message(currentUser, text, Message.MessageType.TEXT);
            if (chatClient != null) {
                chatClient.sendMessage(message);
            }
            messageTextArea.clear();
        }
    }
    
    //manda audio, es el que maneja audio
    @FXML
    private void handleSendAudio() {
        new Thread(() -> {
            try {
                javafx.application.Platform.runLater(() -> {
                    sendAudioButton.setDisable(true);
                    sendAudioButton.setText("🎙️ Grabando...");
                });
                
                // Grabar audio por 5 segundos
                byte[] audioData = audioRecorder.record(5000);
                
                // Enviar mensaje de audio
                int duration = 5; // durationMillis / 1000
                AudioMessage audioMessage = new AudioMessage(currentUser, audioData, duration); 

                if (chatClient != null) {
                    chatClient.sendMessage(audioMessage);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error al grabar audio: " + e.getMessage());
            } finally {
                javafx.application.Platform.runLater(() -> {
                    sendAudioButton.setDisable(false);
                    sendAudioButton.setText("🎤 Audio");
                });
            }
        }).start();
    }
    

    //maneja crear grupo, maneja la creacion de grupos
   // ... (código existente)

/**
 * Maneja crear grupo - Versión mejorada
 */
@FXML
private void handleCreateGroupButton() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Crear grupo");
    dialog.setHeaderText("Nuevo grupo de chat");
    dialog.setContentText("Ingrese el nombre del grupo:");

    dialog.showAndWait().ifPresent(nombreGrupo -> {
        if (nombreGrupo.trim().isEmpty()) {
            showError("Debe ingresar un nombre de grupo.");
            return;
        }

        // Selección de usuarios
        List<User> seleccionados = new ArrayList<>(usersListView.getSelectionModel().getSelectedItems());

        if (seleccionados.isEmpty()) {
            showError("Debe seleccionar al menos un usuario para crear el grupo.");
            return;
        }

        // Crear nuevo grupo
        UserGroups nuevoGrupo = new UserGroups(nombreGrupo.trim());
        nuevoGrupo.addUser(currentUser); // el creador se incluye automáticamente
        
        // Agregar usuarios seleccionados
        for (User u : seleccionados) {
            // Evitar duplicados
            boolean yaExiste = nuevoGrupo.getListaUsuarios().stream()
                .anyMatch(user -> user.getId().equals(u.getId()));
            if (!yaExiste) {
                nuevoGrupo.addUser(u);
            }
        }

        // Enviar al servidor - EL SERVIDOR SE ENCARGA DE SINCRONIZAR
        Message groupMessage = new Message(currentUser, "Crear grupo: " + nombreGrupo, Message.MessageType.CREATE_GROUP);
        groupMessage.setGroup(nuevoGrupo);
        
        if (chatClient != null) {
            chatClient.sendMessage(groupMessage);
            showInfo("Solicitando creación del grupo '" + nombreGrupo + "'...");
        } else {
            showError(" No hay conexión con el servidor");
        }
    });
}

//unirse a grupo
//maneja unirse a grupo
    @FXML
    private void handleJoinGroupButton() {
    UserGroups selectedGroup = groupsListView.getSelectionModel().getSelectedItem();

        if (selectedGroup == null) {
            showError("Debe seleccionar un grupo para unirse.");
            return;
        }

        // Verificar si el usuario ya está dentro del grupo
        for (User u : selectedGroup.getListaUsuarios()) {
            if (u.getId().equals(currentUser.getId())) {
                showInfo("Ya eres miembro del grupo '" + selectedGroup.getNombreGrupo() + "'.");
                return;
            }
        }

        // Agregar al usuario al grupo
        selectedGroup.addUser(currentUser);

        //envias a servidor
        Message groupMessage = new Message(currentUser, selectedGroup.getNombreGrupo(), Message.MessageType.JOIN_GROUP);
        groupMessage.setGroup(selectedGroup);
        chatClient.sendMessage(groupMessage);
        
        //updateGroupList((List<UserGroups>) groupsListView);

        showInfo("Te has unido exitosamente al grupo '" + selectedGroup.getNombreGrupo() + "'.");
        StringBuilder miembros = new StringBuilder();
        
        for (User u : selectedGroup.getListaUsuarios()) {
            miembros.append("• ").append(u.getUsername()).append("\n");
        }
        showInfo("Miembros actuales del grupo '" + selectedGroup.getNombreGrupo() + "':\n\n" + miembros);

        // Actualizar visualmente
        groupsListView.refresh();
    }


/**
 * Actualiza la lista de grupos - Versión mejorada
 */
public void updateGroupList(List<UserGroups> groups) {
    javafx.application.Platform.runLater(() -> {
        System.out.println(" Actualizando lista de grupos: " + groups.size() + " grupos recibidos");
        
        // Actualizar la lista observable
        observableGroups.setAll(groups);
        
        // También actualizar la lista local para referencia
        grupos.clear();
        grupos.addAll(groups);
        groupsListView.refresh();
        
        // Log para debugging
        if (!groups.isEmpty()) {
            System.out.println("📋 Grupos actualizados:");
            for (UserGroups grupo : groups) {
                System.out.println("   - " + grupo.getNombreGrupo() + " (" + grupo.getListaUsuarios().size() + " miembros)");
            }
        }
    });
}

// ... (resto del código existente)

    //maneja llamada 
     @FXML
    private void handleCall() {
        if (!enLlamada) {
            iniciarLlamada();
        } else {
            terminarLlamada();
        }
    }

     /**
     * Inicia una llamada de voz con otro usuario
     */
    private void iniciarLlamada() {
        try {
            // Diálogo para seleccionar usuario para llamar
            User usuarioSeleccionado = seleccionarUsuarioParaLlamar();
            if (usuarioSeleccionado == null) {
                showInfo("Selecciona un usuario de la lista para llamar");
                return;
            }
            
            // Diálogo para configurar llamada
            if (!configurarLlamada()) {
                return;
            }
            
            // Mostrar confirmación
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Iniciar Llamada");
            confirmacion.setHeaderText("Llamar a: " + usuarioSeleccionado.getUsername());
            confirmacion.setContentText("¿Iniciar llamada de voz?");
            
            Optional<ButtonType> resultado = confirmacion.showAndWait();
            if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                
                // Iniciar recepción primero
                audioReceiverCall = new AudioReceiverCall(puertoRecepcion);
                audioReceiverCall.iniciarRecepcion();
                
                // Iniciar envío
                audioSenderCall = new AudioSenderCall(ipDestinoLlamada, puertoEnvio);
                audioSenderCall.iniciarEnvio();
                
                enLlamada = true;
                actualizarBotonLlamada();
                
                // Enviar mensaje de sistema sobre la llamada
                Message mensajeLlamada = new Message(
                    currentUser, 
                    "🔊 " + currentUser.getUsername() + " inició una llamada de voz", 
                    Message.MessageType.SYSTEM
                );
                chatClient.sendMessage(mensajeLlamada);
                
                showInfo(" Llamada iniciada con " + usuarioSeleccionado.getUsername() + 
                        "\n Enviando a: " + ipDestinoLlamada + ":" + puertoEnvio +
                        "\n Escuchando en: puerto " + puertoRecepcion);
            }
            
        } catch (Exception e) {
            showError("Error al iniciar llamada: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Termina la llamada actual
     */
    private void terminarLlamada() {
        try {
            if (audioSenderCall != null) {
                audioSenderCall.terminarEnvio();
            }
            if (audioReceiverCall != null) {
                audioReceiverCall.terminarRecepcion();
            }
            
            enLlamada = false;
            actualizarBotonLlamada();
            
            // Enviar mensaje de fin de llamada
            Message mensajeFinLlamada = new Message(
                currentUser, 
                "🔇 " + currentUser.getUsername() + " terminó la llamada de voz", 
                Message.MessageType.SYSTEM
            );
            chatClient.sendMessage(mensajeFinLlamada);
            
            showInfo(" Llamada terminada");
            
        } catch (Exception e) {
            showError("Error al terminar llamada: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Selecciona un usuario de la lista para llamar
     */
    private User seleccionarUsuarioParaLlamar() {
        User usuarioSeleccionado = usersListView.getSelectionModel().getSelectedItem();
        
        if (usuarioSeleccionado == null) {
            // Mostrar diálogo de selección si no hay selección previa
            ChoiceDialog<User> dialog = new ChoiceDialog<>(null, connectedUsers);
            dialog.setTitle("Seleccionar Usuario");
            dialog.setHeaderText("Llamar a usuario");
            dialog.setContentText("Selecciona el usuario para llamar:");
            
            Optional<User> resultado = dialog.showAndWait();
            if (resultado.isPresent()) {
                usuarioSeleccionado = resultado.get();
            }
        }
        
        return usuarioSeleccionado;
    }
    
    /**
     * Configura los parámetros de la llamada
     */
    private boolean configurarLlamada() {
        try {
            // Diálogo para IP destino
            TextInputDialog ipDialog = new TextInputDialog(ipDestinoLlamada);
            ipDialog.setTitle("Configurar Llamada");
            ipDialog.setHeaderText("Configuración de Llamada");
            ipDialog.setContentText("IP del destinatario:");
            
            Optional<String> ipResult = ipDialog.showAndWait();
            if (ipResult.isPresent() && !ipResult.get().trim().isEmpty()) {
                ipDestinoLlamada = ipResult.get().trim();
            } else {
                return false;
            }
            
            // Diálogo para puerto de envío
            TextInputDialog puertoEnvioDialog = new TextInputDialog(String.valueOf(puertoEnvio));
            puertoEnvioDialog.setTitle("Configurar Llamada");
            puertoEnvioDialog.setHeaderText("Puerto de Envío");
            puertoEnvioDialog.setContentText("Puerto donde enviar audio:");
            
            Optional<String> puertoEnvioResult = puertoEnvioDialog.showAndWait();
            if (puertoEnvioResult.isPresent()) {
                puertoEnvio = Integer.parseInt(puertoEnvioResult.get());
            } else {
                return false;
            }
            
            // Diálogo para puerto de recepción
            TextInputDialog puertoRecepcionDialog = new TextInputDialog(String.valueOf(puertoRecepcion));
            puertoRecepcionDialog.setTitle("Configurar Llamada");
            puertoRecepcionDialog.setHeaderText("Puerto de Recepción");
            puertoRecepcionDialog.setContentText("Puerto donde escuchar audio:");
            
            Optional<String> puertoRecepcionResult = puertoRecepcionDialog.showAndWait();
            if (puertoRecepcionResult.isPresent()) {
                puertoRecepcion = Integer.parseInt(puertoRecepcionResult.get());
            } else {
                return false;
            }
            
            return true;
            
        } catch (NumberFormatException e) {
            showError("Los puertos deben ser números válidos");
            return false;
        } catch (Exception e) {
            showError("Error en configuración: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Actualiza la apariencia del botón de llamada según el estado
     */
    private void actualizarBotonLlamada() {
        if (enLlamada) {
            callButton.setText("📞 Colgar");
            callButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
        } else {
            callButton.setText("📞 Llamar");
            callButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        }
    }
    
    /**
     * Maneja la desconexión - termina llamadas activas
     */
    @FXML
    private void handleLogout() {
        try {
            // Terminar llamada si está activa
            if (enLlamada) {
                terminarLlamada();
            }
            
            if (chatClient != null) {
                chatClient.disconnect();
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //muestra informacion
    private void showInfo(String msg) {
    javafx.application.Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    });
    }
    
    // Métodos llamados por el ChatClient
    public void addMessage(Message message) {
        javafx.application.Platform.runLater(() -> {
            messages.add(message);
            messagesListView.scrollTo(messages.size() - 1);
            
            // Si es mensaje de audio, reproducirlo
           if (message.getType() == Message.MessageType.AUDIO) {
    try {
        // CAMBIO: Intentamos castear a AudioMessage para obtener los bytes.
        if (message instanceof AudioMessage audioMsg) {
            
            // Usamos el player asíncrono para reproducir en un hilo separado
            // y no congelar la UI de JavaFX.
            audioPlayer.playAsync(audioMsg.getAudioData());
            
            System.out.println("REPRODUCIENDO Mensaje de audio de: " + audioMsg.getSender().getUsername());
        } else {
            // Esto sucede si el mensaje de audio no fue enviado como AudioMessage.
            System.err.println("Error: Mensaje de audio recibido sin datos adjuntos.");
        }
    } catch (Exception e) {
        // Usamos showError para notificar errores en la reproducción de audio
        showError("Error al intentar reproducir audio: " + e.getMessage());
        e.printStackTrace();
    }
    }
        });
    }
    
    //actuliza lista de usuarios
    public void updateUserList(List<User> users) {
        javafx.application.Platform.runLater(() -> {
            connectedUsers.setAll(users);
            userCountLabel.setText("Usuarios: " + users.size());
        });
    }
    
    public void showError(String error) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(error);
            alert.showAndWait();
        });
    }
}
