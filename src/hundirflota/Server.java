package hundirflota;
import java.io.*;
import java.net.*;

public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor iniciado, esperando por cliente...");
            clientSocket = serverSocket.accept();
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            // Manejo del juego
            playGame();

            stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playGame() {
        try {
            // Lógica del juego
            // Envío y recepción de mensajes, por ejemplo:
            out.writeObject("Bienvenido al juego Hundir la Flota!");
            // Implementar la lógica de juego aquí
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start(6666);
    }
}