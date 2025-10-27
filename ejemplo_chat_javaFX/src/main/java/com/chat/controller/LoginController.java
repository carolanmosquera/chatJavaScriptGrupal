package com.chat.controller;

// Si los imports dan error, IGNÓRALOS por ahora y ejecuta igual
//el login controller es el que controla javafx 
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.chat.model.User;

import java.io.IOException;

public class LoginController {
    
    /*
    @FXML: Anotación que conecta variables Java con componentes FXML

    usernameField: Campo de texto donde usuario ingresa nombre

    loginButton: Botón para iniciar sesión

    Conexión con FXML: Estos nombres deben coincidir con fx:id en el archivo FXML
     */
    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    
    /*
    Anotación @FXML: Indica que este método es manejador de eventos FXML.
    Obtención de texto:

    getText(): Obtiene texto del campo

    trim(): Elimina espacios en blanco al inicio/fin
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        
        if (username.isEmpty()) {
            showAlert("Error", "Por favor ingresa un nombre de usuario");
            return;
        }
        
        if (username.length() < 3) {
            showAlert("Error", "El nombre de usuario debe tener al menos 3 caracteres");
            return;
        }
        
        try {
            // Crear usuario
            User user = new User(username, java.util.UUID.randomUUID().toString());
            
            // Cargar vista del chat
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/view/chat-view.fxml"));
            Parent root = loader.load();
            
            // Pasar usuario al controlador del chat
            ChatController chatController = loader.getController();
            chatController.setCurrentUser(user);
            chatController.initializeChat();
            
            // Mostrar ventana del chat
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Chat Grupal - " + username);
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo cargar la interfaz del chat");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error inesperado: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
    