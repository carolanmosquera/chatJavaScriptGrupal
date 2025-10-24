package com.chat;

// Si los imports de JavaFX dan error, EJECUTA igual desde terminal
// El problema es del IDE, no del código
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatApplication extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Cargar el FXML - usa esta ruta que es más confiable
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/view/login-view.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("Chat Grupal - Login");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        // Iniciar servidor en un hilo separado
        new Thread(() -> {
            try {
                //inicializas la clase de ChatServer que contiene la logica de mensajes
                com.chat.service.ChatServer server = new com.chat.service.ChatServer();
                server.start();
            } catch (Exception e) {
                System.err.println("Error iniciando servidor: " + e.getMessage());
            }
        }).start();
        
        // Esperar un poco para que el servidor inicie
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Lanzar aplicación JavaFX
        System.out.println("Iniciando aplicación JavaFX...");
        launch(args);
    }
}
