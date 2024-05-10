package hundirflota;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Scanner scanner;

    public void start(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            scanner = new Scanner(System.in);

            // Recibir mensajes del servidor
            new Thread(this::receiveMessages).start();

            // Interactuar con el servidor
            try {
                playGame();
            } catch (SocketException e) {
                System.out.println("Desconectado");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            Object message;
            while ((message = in.readObject()) != null) {
                System.out.println(message.toString());
            }
        } catch (EOFException e) {
            System.out.println("Desconectado");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void playGame() throws SocketException {
        while (true) {
            try {
                System.out.print("Introduzca la casilla (por ejemplo B4): ");
                String casilla = scanner.nextLine();
                out.writeObject(casilla);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start("127.0.0.1", 6666);
    }
}