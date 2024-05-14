package hundirflota;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * HUNDIR LA FLOTA
 */
public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static final String RECORDS_FILE = "ranking.txt";
    private static String nombreJugador = "Jugador"; // Nombre del jugador por defecto

    // CONSTANTES
    final static char AGUA_NO_TOCADO = '.';
    final static char AGUA = 'A';
    final static char TOCADO = 'X';

    // TAMAÑO DEL TABLERO
    final static int TAMANIO = 10;

    // Mapa de barcos, impactos y hundimiento
    private Map<Character, Integer> shipSizes = new HashMap<>();
    private Map<Character, Integer> shipHits = new HashMap<>();
    private Map<Character, Boolean> shipSunk = new HashMap<>();

    public Server() {
        shipSizes.put('5', 5); // Two ships of size 5
        shipSizes.put('3', 3); // Three ships of size 3
        shipSizes.put('1', 1); // Five ships of size 1
        shipHits.put('5', 0);
        shipHits.put('3', 0);
        shipHits.put('1', 0);
        shipSunk.put('5', false);
        shipSunk.put('3', false);
        shipSunk.put('1', false);
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en el puerto " + port);

        while (true) {
            startClientConnection();

            try {
                processClientRequests();
            } catch (IOException e) {
                System.out.println("Cliente desconectado.");
            } finally {
                in.close();
                out.close();
                clientSocket.close();
            }
        }
    }

    private void startClientConnection() throws IOException {
        System.out.println("Esperando conexión del cliente...");
        clientSocket = serverSocket.accept();
        System.out.println("Cliente conectado.");

        out = new ObjectOutputStream(clientSocket.getOutputStream());
        in = new ObjectInputStream(clientSocket.getInputStream());
    }

    private void processClientRequests() throws IOException {
        try {
            Object message;
            while ((message = in.readObject()) != null) {
                String command = message.toString();
                if ("play".equalsIgnoreCase(command)) {
                    playGame();
                } else if ("records".equalsIgnoreCase(command)) {
                    sendRecords();
                } else if ("exit".equalsIgnoreCase(command)) {
                    stop();
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Error en la comunicación con el servidor.");
        }
    }

    private void playGame() {
        GameData gameData = new GameData();
        gameData.setMapaUsuario(new char[TAMANIO][TAMANIO]);
        gameData.setMapaOrdenador(new char[TAMANIO][TAMANIO]);
        gameData.setMapaOrdenadorParaUsuario(new char[TAMANIO][TAMANIO]);
        gameData.setPuntosUsuario(24);
        gameData.setPuntosOrdenador(24);
        gameData.setJuegoTerminado(false);

        nombreJugador = pedirNombreUsuario();

        try {
            int[] tiro = new int[2];

            out.writeObject("¡Bienvenido a Hundir la Flota, " + nombreJugador + "!");
            inicializacion(gameData.getMapaUsuario(), gameData.getMapaOrdenador());
            inicializaMapaRegistro(gameData.getMapaOrdenadorParaUsuario());

            startGameLoop(gameData, tiro);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void startGameLoop(GameData gameData, int[] tiro)
            throws IOException, ClassNotFoundException {
        boolean tiroCorrecto = false;
        while (!gameData.isJuegoTerminado()) {
            out.writeObject("MAPA DEL USUARIO:\n");
            imprimirMapa(gameData.getMapaUsuario());
            out.writeObject("PUNTOS RESTANTES DEL JUGADOR: " + gameData.getPuntosUsuario());
            out.writeObject("TURNO DEL JUGADOR");

            tiroCorrecto = false;
            while (!tiroCorrecto) {
                tiro = pedirCasilla();

                if (tiro[0] != -1 && tiro[1] != -1) {
                    tiroCorrecto = evaluarTiro(gameData.getMapaOrdenador(), tiro);
                }

                if (!tiroCorrecto) {
                    out.writeObject("TIRO INCORRECTO");
                }
            }

            int puntosOrdenadorAnterior = gameData.getPuntosOrdenador();
            gameData.setPuntosOrdenador(actualizarMapa(gameData.mapaOrdenador, tiro, gameData.getPuntosOrdenador()));
            char tipoTiro = (puntosOrdenadorAnterior - gameData.getPuntosOrdenador()) > 0 ? TOCADO : AGUA;
            actualizarMapaRegistro(gameData.getMapaOrdenadorParaUsuario(), tiro, tipoTiro);
            out.writeObject("\nREGISTRO DEL MAPA DEL ORDENADOR");
            imprimirMapa(gameData.getMapaOrdenadorParaUsuario());

            // Comprueba si se ha hundido un barco del ordenador
            char barcoHundido = verificarHundimiento(gameData.getMapaOrdenador(), tiro);
            if (isBarcoHundido(barcoHundido)) {
                out.writeObject("¡Barco de tamaño " + shipSizes.get(barcoHundido) + " hundido!");
            }

            // El juego termina si el número de puntos llega a 0
            gameData.setJuegoTerminado((gameData.getPuntosOrdenador() == 0));

            // Si no ha ganado el jugador, le toca a la máquina
            if (!gameData.isJuegoTerminado()) {
                out.writeObject("PUNTOS RESTANTES DEL ORDENADOR: " + gameData.getPuntosOrdenador());
                out.writeObject("TURNO DEL ORDENADOR");
                tiroCorrecto = false;
                while (!tiroCorrecto) {
                    tiro = generaDisparoAleatorio();
                    tiroCorrecto = evaluarTiro(gameData.getMapaUsuario(), tiro);
                }

                gameData.setPuntosUsuario(actualizarMapa(gameData.getMapaUsuario(), tiro, gameData.getPuntosUsuario()));

                // Comprueba si se ha hundido un barco del usuario
                barcoHundido = verificarHundimiento(gameData.getMapaUsuario(), tiro);
                if (!isBarcoHundido(barcoHundido)) {
                    out.writeObject(
                            "¡El ordenador ha hundido un barco de tamaño " + shipSizes.get(barcoHundido) + "!");
                }

                gameData.setJuegoTerminado((gameData.getPuntosUsuario() == 0));
            }
        }

        if (gameData.getPuntosOrdenador() == 0) {
            out.writeObject("EL VENCEDOR HA SIDO EL JUGADOR");
            updateRecords(nombreJugador, 24 - gameData.getPuntosUsuario());
        } else {
            out.writeObject("EL VENCEDOR HA SIDO EL ORDENADOR");
        }
    }

    private boolean isBarcoHundido(char barcoHundido) {
        return barcoHundido == '\0';
    }

    private String pedirNombreUsuario() {
        try {
            out.writeObject("Introduce tu nombre: ");
            return (String) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al leer el nombre del jugador. Se usará el nombre por defecto.");
            return "Jugador";
        }
    }

    private void sendRecords() throws IOException {
        File file = new File(RECORDS_FILE);
        if (!file.exists()) {
            out.writeObject("No records found.");
            out.writeObject("END_OF_RECORDS");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            sendLinesOfBufferReader(reader);
        } catch (FileNotFoundException e) {
            out.writeObject("Records file not found.");
        } catch (IOException e) {
            out.writeObject("Error reading records file.");
        }

        out.writeObject("END_OF_RECORDS");
    }

    private void sendLinesOfBufferReader(BufferedReader reader) throws IOException {
        String record;
        while ((record = reader.readLine()) != null) {
            out.writeObject(record);
        }
    }

    private void updateRecords(String playerName, int shots) {
        try (PrintWriter out = new PrintWriter(new FileWriter(RECORDS_FILE, true))) {
            out.println(playerName + " - " + shots);
        } catch (IOException e) {
            System.out.println("Error al escribir en el archivo de records.");
        }
    }

    public void showRecords() {
        System.out.println("Ranking de jugadores:");
        try (BufferedReader reader = new BufferedReader(new FileReader(RECORDS_FILE))) {
            showLinesOfBufferReader(reader);
        } catch (IOException e) {
            System.out.println("Error al leer el archivo de records.");
        }
    }

    private void showLinesOfBufferReader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
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
        try {
            server.start(6666);
        } catch (SocketException e) {
            System.out.println("El cliente se ha desconectado");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int[] generaDisparoAleatorio() {
        return new int[] { aleatorio(), aleatorio() };
    }

    public static void inicializacion(char[][] m1, char[][] m2) {
        inicializaMapa(m1);
        inicializaMapa(m2);
    }

    private static void inicializaMapaRegistro(char[][] mapa) {
        for (int i = 0; i < TAMANIO; i++)
            for (int j = 0; j < TAMANIO; j++)
                mapa[i][j] = AGUA_NO_TOCADO;
    }

    private static void inicializaMapa(char[][] mapa) {
        for (int i = 0; i < TAMANIO; i++)
            for (int j = 0; j < TAMANIO; j++)
                mapa[i][j] = AGUA_NO_TOCADO;

        // Coloca los barcos en el mapa
        colocarBarcos(mapa, '5', 2); // Dos barcos de tamaño 5
        colocarBarcos(mapa, '3', 3); // Tres barcos de tamaño 3
        colocarBarcos(mapa, '1', 5); // Cinco barcos de tamaño 1
    }

    /*
     * Método que coloca los barcos en el mapa.
     * 
     * @param mapa El mapa donde se van a colocar los barcos
     * 
     * @param tipoBarco El carácter que representa el tipo de barco
     * 
     * @param cantidad La cantidad de barcos de ese tipo
     */
    private static void colocarBarcos(char[][] mapa, char tipoBarco, int cantidad) {
        int tamano = tipoBarco == '5' ? 5 : (tipoBarco == '3' ? 3 : 1);
        Random rand = new Random();

        for (int i = 0; i < cantidad; i++) {
            boolean colocado = false;
            while (!colocado) {
                int fila = rand.nextInt(TAMANIO);
                int columna = rand.nextInt(TAMANIO);
                boolean horizontal = rand.nextBoolean();

                if (puedeColocarBarco(mapa, fila, columna, tamano, horizontal)) {
                    for (int j = 0; j < tamano; j++) {
                        if (horizontal) {
                            mapa[fila][columna + j] = tipoBarco;
                        } else {
                            mapa[fila + j][columna] = tipoBarco;
                        }
                    }
                    colocado = true;
                }
            }
        }
    }

    /*
     * Método que verifica si se puede colocar un barco en una posición dada.
     */
    private static boolean puedeColocarBarco(char[][] mapa, int fila, int columna, int tamano, boolean horizontal) {
        if (horizontal) {
            if (columna + tamano > TAMANIO)
                return false;
            for (int j = 0; j < tamano; j++) {
                if (mapa[fila][columna + j] != AGUA_NO_TOCADO)
                    return false;
            }
        } else {
            if (fila + tamano > TAMANIO)
                return false;
            for (int j = 0; j < tamano; j++) {
                if (mapa[fila + j][columna] != AGUA_NO_TOCADO)
                    return false;
            }
        }
        return true;
    }

    /*
     * Método que pide una casilla al cliente.
     */
    private static int[] pedirCasilla() throws ClassNotFoundException, IOException {
        out.writeObject("Introduce la casilla (por ejemplo, B4): ");
        String linea = (String) in.readObject();
        linea = linea.toUpperCase();
        int[] t;

        // Comprobamos que la longitud de la cadena sea mayor que 2
        if (linea.length() < 2)
            return new int[] { -1, -1 }; // Si la longitud es menor que 2, devolvemos -1

        // Comprobamos que lo introducido por el usaurio es correcto mediante una
        // expresi�n regular
        if (linea.matches("^[A-Z][0-9]*$")) {

            // Obtenemos la letra.
            // Suponemos que, como mucho, usaremos una letra del abecedario
            char letra = linea.charAt(0);
            // El n�mero de fila es VALOR_NUMERICO(LETRA) - VALOR_NUMERICO(A).
            int fila = Character.getNumericValue(letra) - Character.getNumericValue('A');
            // Para la columna, tan solo tenemos que procesar el n�mero
            int columna = Integer.parseInt(linea.substring(1, linea.length()));
            // Si las coordenadas est�n dentro del tama�o del tablero, las devolvemos
            if (fila >= 0 && fila < TAMANIO && columna >= 0 && columna <= TAMANIO) {
                t = new int[] { fila, columna };
            } else // En otro caso, devolvemos -1, para que vuelva a solicitar el tiro
                t = new int[] { -1, -1 };
        } else
            t = new int[] { -1, -1 };

        return t;
    }

    /*
     * Método que evalúa si un disparo es válido.
     */
    public static boolean evaluarTiro(char[][] mapa, int[] t) {
        int fila = t[0];
        int columna = t[1];
        return mapa[fila][columna] == AGUA_NO_TOCADO || (mapa[fila][columna] >= '1' && mapa[fila][columna] <= '5');
    }

    /*
     * Método que actualiza el mapa después de un disparo.
     */
    private int actualizarMapa(char[][] mapa, int[] tiro, int puntos) {
        char casilla = mapa[tiro[0]][tiro[1]];
        if (Character.isDigit(casilla)) {
            mapa[tiro[0]][tiro[1]] = TOCADO;
            shipHits.put(casilla, shipHits.get(casilla) + 1);
            puntos--;
        } else {
            mapa[tiro[0]][tiro[1]] = AGUA;
        }
        return puntos;
    }

    /*
     * Método que actualiza el mapa de registro.
     */
    private static void actualizarMapaRegistro(char[][] mapaRegistro, int[] tiro, char tipoTiro) {
        mapaRegistro[tiro[0]][tiro[1]] = tipoTiro;
    }

    /*
     * Método que imprime un mapa.
     */
    private static void imprimirMapa(char[][] mapa) throws IOException {
        // Create a string to store the map
        StringBuilder map = new StringBuilder();

        // Calculamos las letras seg�n el tama�o
        char[] letras = new char[TAMANIO];
        for (int i = 0; i < TAMANIO; i++)
            letras[i] = (char) ('A' + i);

        // Append the header row to the map string
        map.append("    ");
        for (int i = 0; i < TAMANIO; i++) {
            map.append("[" + i + "] ");
        }
        map.append("\n");

        // Append the rest of the rows to the map string
        for (int i = 0; i < TAMANIO; i++) {
            map.append("[" + letras[i] + "]  ");
            for (int j = 0; j < TAMANIO; j++) {
                map.append(mapa[i][j] + "   ");
            }
            map.append("\n");
        }

        // Send the map string to the user
        try {
            out.writeObject(map.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Método que verifica si un barco se ha hundido.
     */
    private char verificarHundimiento(char[][] mapa, int[] tiro) {
        char barco = mapa[tiro[0]][tiro[1]];
        if (Character.isDigit(barco)) {
            int hits = shipHits.get(barco);
            if (hits == shipSizes.get(barco)) {
                shipSunk.put(barco, true);
                return barco;
            }
        }
        return '\0';
    }

    /*
     * Método que genera un índice aleatorio.
     */
    private static int aleatorio() {
        return new Random().nextInt(TAMANIO);
    }
}