package hundirflota;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Scanner scanner;

    public void connectToServer(String host, int port) throws IOException {
        // Establish the socket connection to the server
        socket = new Socket(host, port);
        // Initialize the ObjectOutputStream and ObjectInputStream
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        scanner = new Scanner(System.in);
    }

    public void start() throws IOException {
        boolean running = true;

        while (running) {
            System.out.println("\nMENU:");
            System.out.println("1. Jugar");
            System.out.println("2. Records");
            System.out.println("3. Salir");
            int choice;
            do {
                System.out.print("Elige una opción: ");
                while (!scanner.hasNextInt()) {
                    System.out.print("Opción no válida, intenta de nuevo: ");
                    scanner.next();
                }
                choice = scanner.nextInt();
                scanner.nextLine();
            } while (choice < 1 || choice > 3);

            switch (choice) {
                case 1:
                    // Recibir mensajes del servidor
                    new Thread(this::receiveMessages).start();
                    playGame();
                    break;
                case 2:
                    showRecords();
                    break;
                case 3:
                    System.out.println("Saliendo del juego...");
                    sendMessage("exit");
                    running = false;
                    break;
                default:
                    System.out.println("Opción no válida, por favor elige de nuevo.");
            }
        }
        stop();
    }

    private void playGame() throws IOException {
        sendMessage("play");
        while (true) {
            try {
                String casilla = scanner.nextLine();
                out.writeObject(casilla);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

    }

    private void showRecords() throws IOException {
        sendMessage("records");
        try {
            Object message;
            // Read the records sent by the server
            while ((message = in.readObject()) != null) {
                System.out.println(message);
                if (message.toString().equals("END_OF_RECORDS")) {
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Error en la comunicación con el servidor.");
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

    private void sendMessage(String message) throws IOException {
        if (out != null) {
            out.writeObject(message);
        } else {
            System.out.println("Connection to server not established.");
        }
    }

    private void stop() throws IOException {
        if (in != null)
            in.close();
        if (out != null)
            out.close();
        if (socket != null)
            socket.close();
        if (scanner != null)
            scanner.close();
    }

    // public static void main(String[] args) {
    // String host = "localhost";
    // int port = 6666;
    // Client client = new Client();
    // try {
    // client.connectToServer(host, port);
    // client.start();
    // } catch (IOException e) {
    // System.out.println("Failed to connect to the server: " + e.getMessage());
    // e.printStackTrace();
    // }
    // }
}