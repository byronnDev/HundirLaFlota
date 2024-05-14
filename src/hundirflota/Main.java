package hundirflota;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        Server server = new Server(); // Create a server instance
        Client client = new Client(); // Create a client instance

        new Thread() {
            public void run() {
                try {
                    server.start(port);
                } catch (IOException e) {
                    System.err.println("Error starting the server: " + e.getMessage());
                } // Start the server thread
            }
        }.start();

        try {
            client.connectToServer(host, port);
            client.start();
        } catch (IOException e) {
            System.out.println("Failed to connect to the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
