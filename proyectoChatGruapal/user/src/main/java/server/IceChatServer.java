package server;

public class IceChatServer {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "ChatAdapter",
                    "ws -p 10000" // WebSocket
            );

            ChatServiceI servant = new ChatServiceI();
            adapter.add(servant, Util.stringToIdentity("ChatService"));
            adapter.activate();

            System.out.println("Servidor Ice listo en WebSocket");
            communicator.waitForShutdown();
        }
    }
}
