package hundirflota;

import java.io.*;
import java.net.*;

public class Client {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            // Juego
            playGame();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playGame() {
        try {
            // Leer mensaje del servidor
            String message = (String) in.readObject();
            System.out.println("Servidor dice: " + message);

            // Implementar la interacción con el servidor aquí
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.startConnection("127.0.0.1", 6666);
    }
}