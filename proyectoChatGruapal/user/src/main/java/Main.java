import com.zeroc.Ice.*;
import server.ChatServiceImpl;

public class Main {
    public static void main(String[] args) {
        int status = 0;
        Communicator communicator = null;
        
        try {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("  Inicializando Chat Server con ZeroC Ice");
            System.out.println("=".repeat(50));
            
            // Crear propiedades de configuración
            Properties properties = Util.createProperties();
            
            // Configurar adaptadores
            properties.setProperty("Ice.Warn.Connections", "1");
            properties.setProperty("Ice.Trace.Network", "0");
            
            InitializationData initData = new InitializationData();
            initData.properties = properties;
            
            // Inicializar comunicador Ice
            communicator = Util.initialize(args, initData);
            
            System.out.println("\n✓ Comunicador Ice inicializado");
            
            // Crear adaptador de objetos con endpoints TCP y WebSocket
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                "ChatAdapter", 
                "tcp -h localhost -p 10000 : ws -h localhost -p 10001"
            );
            
            System.out.println("✓ Adaptador de objetos creado");
            
            // Crear e implementar el servicio de chat
            ChatServiceImpl chatService = new ChatServiceImpl();
            
            // Agregar el servicio al adaptador con el identificador
            adapter.add(chatService, Util.stringToIdentity("ChatService"));
            
            System.out.println("✓ Servicio de chat registrado");
            
            // Activar el adaptador
            adapter.activate();
            
            System.out.println("✓ Adaptador activado");
            
            // Mostrar información de conexión
            System.out.println("\n" + "=".repeat(50));
            System.out.println("  SERVIDOR INICIADO CORRECTAMENTE");
            System.out.println("=".repeat(50));
            System.out.println("\n📡 Endpoints disponibles:");
            System.out.println("   • TCP:       tcp -h localhost -p 10000");
            System.out.println("   • WebSocket: ws -h localhost -p 10001");
            System.out.println("\n💡 Proxy para clientes:");
            System.out.println("   ChatService:tcp -h localhost -p 10000");
            System.out.println("   ChatService:ws -h localhost -p 10001");
            System.out.println("\n" + "=".repeat(50));
            System.out.println("  Esperando conexiones...");
            System.out.println("  Presiona Ctrl+C para detener el servidor");
            System.out.println("=".repeat(50) + "\n");
            
            // Configurar shutdown hook para limpieza
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n\n🛑 Deteniendo servidor...");
                if (communicator != null) {
                    try {
                        communicator.destroy();
                        System.out.println("✓ Servidor detenido correctamente");
                    } catch (Exception e) {
                        System.err.println("⚠ Error al detener el servidor: " + e.getMessage());
                    }
                }
            }));
            
            // Esperar hasta que se apague el servidor
            communicator.waitForShutdown();
            
        } catch (LocalException e) {
            System.err.println("\n❌ Error local de Ice: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } catch (Exception e) {
            System.err.println("\n❌ Error general en el servidor: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } finally {
            if (communicator != null) {
                try {
                    communicator.destroy();
                } catch (Exception e) {
                    System.err.println("⚠ Error destruyendo comunicador: " + e.getMessage());
                    status = 1;
                }
            }
        }
        
        System.exit(status);
    }
}
