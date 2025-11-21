package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import model.*;

public class WriteThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private ChatClient client;

    public WriteThread(Socket socket, ChatClient client) {
        this.socket = socket;
        this.client = client;

        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            System.out.println("Error getting output stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void run() {

        Scanner scanner = new Scanner(System.in);

        // here its were the user register and enter its name
        System.out.print("\nEnter your name: ");
        String userName = scanner.nextLine();

        // with the name a object user its created
        User newUser = new User(userName);
        newUser.setUsername(userName);
        // client its only using astring to reference User
        client.setUserName(newUser.getUsername());

        // the name of the user its being sended to the server
        writer.println(userName);

        String text;

        do {
            // this is the method that is used by ther user to send messages
            System.out.print("[" + userName + "]: ");

            // method of identification of type mesage
            // switch (text) {
            // case value:

            // break;

            // default:
            // break;
            // }

            text = scanner.nextLine();
            writer.println(text);

        } while (!text.equals("bye"));

        try {
            socket.close();
        } catch (IOException ex) {

            System.out.println("Error writing to server: " + ex.getMessage());
        }
    }
}
