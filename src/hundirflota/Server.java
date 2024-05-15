package hundirflota;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;

/*
 * CHECKS (TODOs)
 * - Corregir la excepción que salta al meter sólo una letra cómo coordenada ✅
 * - Cambia el programa para que sea cliente servidor mediate TCP ✅
 * - En la comunicación va el resultado del disparo o las coordenadas ✅
 * - Jugando el server contra el usuario que está en el lado del cliente ✅
 * - El servidor guarda un ranking con el nombre y el número de disparos en el
 * que ha sido derrotado (sólo lo garda en el caso de que el usuario desde el
 * lado cliente gane) ✅
 * - Menú inicial con 3 opciones: ✅
 * Jugar
 * Records
 * Salir
 * - Intenta realizar el trabajo de manera óptima y siguiendo los estándares de
 * Java ✅
 * 
 * AVANZADO: Avisa cuándo se hunde un barco ✅
 */

/**
 * HUNDIR LA FLOTA
 */
public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static final String RECORDS_FILE = "ranking.txt";
    private static final int TAMANIO = 10;
    private static final char AGUA_NO_TOCADO = '.';
    private static final char AGUA = 'A';
    private static final char TOCADO = 'X';

    private Map<Character, Integer> shipSizes = new HashMap<>();
    private Map<Character, Integer> shipHits = new HashMap<>();
    private Map<Character, Boolean> shipSunk = new HashMap<>();
    private static Map<Character, String> shipMessages = new HashMap<>();
    private final Map<String, Consumer<Void>> commandMap;

    public Server() {
        // Inicializa el mapa de comandos
        commandMap = new HashMap<>();
        commandMap.put("play", v -> playGame());
        commandMap.put("records", v -> sendRecords());
        commandMap.put("exit", v -> stop());

        initializeShipData();
    }

    private void initializeShipData() {
        shipSizes.put('5', 5);
        shipSizes.put('3', 3);
        shipSizes.put('1', 1);

        for (Character shipType : shipSizes.keySet()) {
            shipHits.put(shipType, 0);
            shipSunk.put(shipType, false);
        }

        shipMessages.put('5', "portaaviones");
        shipMessages.put('3', "crucero");
        shipMessages.put('1', "submarino");
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en el puerto " + port);

        while (true) {
            startClientConnection();
            processClientRequests();
            closeConnections();
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
                String command = message.toString().toLowerCase();
                Consumer<Void> action = commandMap.get(command);
                if (action != null) {
                    action.accept(null);
                } else {
                    out.writeObject("Comando no reconocido: " + command);
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Error en la comunicación con el servidor.");
        }
    }

    private void playGame() {
        GameData gameData = initializeGame();
        String playerName = askPlayerName();

        try {
            out.writeObject("¡Bienvenido a Hundir la Flota, " + playerName + "!");
            initialize(gameData.getMapaUsuario(), gameData.getMapaOrdenador());
            initializeRecordMap(gameData.getMapaOrdenadorParaUsuario());

            startGameLoop(gameData, playerName);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void startGameLoop(GameData gameData, String playerName)
            throws IOException, ClassNotFoundException {
        while (!gameData.isJuegoTerminado()) {
            out.writeObject("MAPA DEL USUARIO:\n");
            printMap(gameData.getMapaUsuario());
            out.writeObject("PUNTOS RESTANTES DEL JUGADOR: " + gameData.getPuntosUsuario());
            out.writeObject("TURNO DEL JUGADOR");

            playerTurn(gameData);

            if (!gameData.isJuegoTerminado())
                computerTurn(gameData);
        }

        if (gameData.getPuntosOrdenador() == 0) {
            out.writeObject("EL VENCEDOR HA SIDO EL JUGADOR");
            updateRecords(playerName, 24 - gameData.getPuntosUsuario());
        } else {
            out.writeObject("EL VENCEDOR HA SIDO EL ORDENADOR");
        }
        stop();
    }

    private void playerTurn(GameData gameData) throws IOException, ClassNotFoundException {
        boolean correctShot = false;
        while (!correctShot) {
            gameData.setTiro(askForCell());

            if (gameData.getTiro()[0] != -1 && gameData.getTiro()[1] != -1) {
                correctShot = evaluateShot(gameData.getMapaOrdenador(), gameData.getTiro());
            }

            if (!correctShot) {
                out.writeObject("TIRO INCORRECTO");
            }
        }

        // Realizar el disparo y actualizar el mapa
        int previousComputerPoints = gameData.getPuntosOrdenador();
        boolean shipSunk = updateMapAndCheckSunk(gameData.getMapaOrdenador(), gameData.getTiro(), gameData);
        char shotType = (previousComputerPoints - gameData.getPuntosOrdenador()) > 0 ? TOCADO : AGUA;
        updateRecordMap(gameData.getMapaOrdenadorParaUsuario(), gameData.getTiro(), shotType);
        out.writeObject("\nREGISTRO DEL MAPA DEL ORDENADOR");
        printMap(gameData.getMapaOrdenadorParaUsuario());

        // Verificar si se ha hundido un barco
        if (shipSunk) {
            char shipType = gameData.getMapaOrdenador()[gameData.getTiro()[0]][gameData.getTiro()[1]];
            notifySinking(shipType, true);
        }

        gameData.setJuegoTerminado((gameData.getPuntosOrdenador() == 0));
    }

    private boolean updateMapAndCheckSunk(char[][] mapaOrdenador, int[] tiro, GameData gameData) {
        int previousComputerPoints = gameData.getPuntosOrdenador();
        gameData.setPuntosOrdenador(
                updateMap(mapaOrdenador, tiro, gameData.getPuntosOrdenador(), true));

        char shotType = (previousComputerPoints - gameData.getPuntosOrdenador()) > 0 ? TOCADO : AGUA;
        updateRecordMap(gameData.getMapaOrdenadorParaUsuario(), tiro, shotType);

        // Verificar si se ha hundido un barco
        char barco = gameData.getMapaOrdenador()[tiro[0]][tiro[1]];
        if (barco != AGUA && barco != AGUA_NO_TOCADO && barco != TOCADO) {
            int[] coordenadasBarco = getShipCoordinates(gameData.getMapaOrdenador(), tiro);
            char tipoBarco = gameData.getMapaOrdenador()[coordenadasBarco[0]][coordenadasBarco[1]];
            if (isShipSunk(gameData.getMapaOrdenador(), coordenadasBarco, tipoBarco)) {
                return true;
            }
        }

        return false;
    }

    private int[] getShipCoordinates(char[][] map, int[] shot) {
        int row = shot[0];
        int col = shot[1];
        char shipType = map[row][col];

        int[] coordinates = new int[2];
        for (int i = 0; i < TAMANIO; i++) {
            for (int j = 0; j < TAMANIO; j++) {
                if (map[i][j] == shipType) {
                    coordinates[0] = i;
                    coordinates[1] = j;
                    return coordinates;
                }
            }
        }

        return coordinates;
    }

    private boolean isShipSunk(char[][] map, int[] coordinates, char shipType) {
        int row = coordinates[0];
        int col = coordinates[1];
        int size = shipSizes.get(shipType);

        if (shipSunk.get(shipType))
            return false;

        if (shipType == '5') {
            return isShipSunk(map, row, col, size, true) || isShipSunk(map, row, col, size, false);
        } else {
            return isShipSunk(map, row, col, size, true) || isShipSunk(map, row, col, size, false);
        }
    }

    private boolean isShipSunk(char[][] map, int row, int col, int size, boolean horizontal) {
        if (horizontal) {
            for (int j = 0; j < size; j++) {
                if (map[row][col + j] != TOCADO)
                    return false;
            }
        } else {
            for (int j = 0; j < size; j++) {
                if (map[row + j][col] != TOCADO)
                    return false;
            }
        }
        return true;
    }

    private void computerTurn(GameData gameData) throws IOException {
        out.writeObject("PUNTOS RESTANTES DEL ORDENADOR: " + gameData.getPuntosOrdenador());
        out.writeObject("TURNO DEL ORDENADOR");
        boolean correctShot = false;
        while (!correctShot) {
            gameData.setTiro(generateRandomShot());
            correctShot = evaluateShot(gameData.getMapaUsuario(), gameData.getTiro());
        }

        gameData.setPuntosUsuario(
                updateMap(gameData.getMapaUsuario(), gameData.getTiro(), gameData.getPuntosUsuario(), false));

        char ship = gameData.getMapaUsuario()[gameData.getTiro()[0]][gameData.getTiro()[1]];
        if (ship != AGUA && ship != AGUA_NO_TOCADO && ship != TOCADO) {
            int[] shipCoordinates = getShipCoordinates(gameData.getMapaUsuario(), gameData.getTiro());
            char shipType = gameData.getMapaUsuario()[shipCoordinates[0]][shipCoordinates[1]];
            if (isShipSunk(gameData.getMapaUsuario(), shipCoordinates, shipType)) {
                notifySinking(shipType, false);
            }
        }

        gameData.setJuegoTerminado((gameData.getPuntosUsuario() == 0));
    }

    private GameData initializeGame() {
        GameData gameData = new GameData();
        gameData.setMapaUsuario(new char[TAMANIO][TAMANIO]);
        gameData.setMapaOrdenador(new char[TAMANIO][TAMANIO]);
        gameData.setMapaOrdenadorParaUsuario(new char[TAMANIO][TAMANIO]);
        gameData.setPuntosUsuario(24);
        gameData.setPuntosOrdenador(24);
        gameData.setJuegoTerminado(false);
        gameData.setTiro(new int[2]);
        return gameData;
    }

    private String askPlayerName() {
        try {
            String playerName;
            do {
                out.writeObject("Introduce tu nombre: ");
                playerName = (String) in.readObject();
            } while (playerName == null || playerName.trim().isEmpty());
            return playerName;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al leer el nombre del jugador. Se usará el nombre por defecto.");
            return "Jugador";
        }
    }

    private void sendRecords() {
        try {
            out.writeObject("\nRanking de jugadores:");

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
        } catch (IOException e) {
            System.err.println("Error sending records to client.");
        }
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

    private static int[] generateRandomShot() {
        return new int[] { randomIndex(), randomIndex() };
    }

    private static void initialize(char[][] m1, char[][] m2) {
        initializeMap(m1);
        initializeMap(m2);
    }

    private static void initializeRecordMap(char[][] map) {
        for (int i = 0; i < TAMANIO; i++)
            for (int j = 0; j < TAMANIO; j++)
                map[i][j] = AGUA_NO_TOCADO;
    }

    private static void initializeMap(char[][] map) {
        for (int i = 0; i < TAMANIO; i++)
            for (int j = 0; j < TAMANIO; j++)
                map[i][j] = AGUA_NO_TOCADO;

        placeShips(map, '5', 2);
        placeShips(map, '3', 3);
        placeShips(map, '1', 5);
    }

    private static void placeShips(char[][] map, char shipType, int quantity) {
        int size = shipType == '5' ? 5 : (shipType == '3' ? 3 : 1);
        Random rand = new Random();

        for (int i = 0; i < quantity; i++) {
            boolean placed = false;
            while (!placed) {
                int row = rand.nextInt(TAMANIO);
                int col = rand.nextInt(TAMANIO);
                boolean horizontal = rand.nextBoolean();

                if (canPlaceShip(map, row, col, size, horizontal)) {
                    for (int j = 0; j < size; j++) {
                        if (horizontal) {
                            map[row][col + j] = shipType;
                        } else {
                            map[row + j][col] = shipType;
                        }
                    }
                    placed = true;
                }
            }
        }
    }

    private static boolean canPlaceShip(char[][] map, int row, int col, int size, boolean horizontal) {
        if (horizontal) {
            if (col + size > TAMANIO)
                return false;
            for (int j = 0; j < size; j++) {
                if (map[row][col + j] != AGUA_NO_TOCADO)
                    return false;
            }
        } else {
            if (row + size > TAMANIO)
                return false;
            for (int j = 0; j < size; j++) {
                if (map[row + j][col] != AGUA_NO_TOCADO)
                    return false;
            }
        }
        return true;
    }

    private static int[] askForCell() throws ClassNotFoundException, IOException {
        out.writeObject("Introduce la casilla (por ejemplo, B4): ");
        String line = (String) in.readObject();
        line = line.toUpperCase();
        int[] coordinates;

        if (line.length() < 2)
            return new int[] { -1, -1 };

        if (line.matches("^[A-Z][0-9]*$")) {
            char letter = line.charAt(0);
            int row = Character.getNumericValue(letter) - Character.getNumericValue('A');
            int col = Integer.parseInt(line.substring(1, line.length()));
            if (row >= 0 && row < TAMANIO && col >= 0 && col <= TAMANIO) {
                coordinates = new int[] { row, col };
            } else
                coordinates = new int[] { -1, -1 };
        } else
            coordinates = new int[] { -1, -1 };

        return coordinates;
    }

    private static boolean evaluateShot(char[][] map, int[] coordinates) {
        int row = coordinates[0];
        int col = coordinates[1];
        return map[row][col] == AGUA_NO_TOCADO || (map[row][col] >= '1' && map[row][col] <= '5');
    }

    private int updateMap(char[][] map, int[] shot, int points, boolean isPlayer) {
        char cell = map[shot[0]][shot[1]];

        if (Character.isDigit(cell)) {
            map[shot[0]][shot[1]] = TOCADO;
            shipHits.put(cell, shipHits.getOrDefault(cell, 0) + 1);

            // Verifica si el barco ha sido hundido
            if (shipHits.get(cell).equals(shipSizes.get(cell))) {
                shipSunk.put(cell, true);
                notifySinking(cell, isPlayer);
            }

            points--;
        } else {
            map[shot[0]][shot[1]] = AGUA;
        }

        return points;
    }

    private void notifySinking(char ship, boolean isPlayer) {
        String shipType = shipMessages.getOrDefault(ship, "barco");

        try {
            if (isPlayer) {
                out.writeObject("\n===============\n ¡Has hundido un " + shipType + " enemigo! \n===============");
            } else {
                out.writeObject("\n===============\n ¡El ordenador ha hundido tu " + shipType + "! \n===============");
            }
        } catch (IOException e) {
            System.err.println("Error al notificar el hundimiento de un barco.");
        }
    }

    private static void updateRecordMap(char[][] map, int[] shot, char shotType) {
        map[shot[0]][shot[1]] = shotType;
    }

    private static void printMap(char[][] map) throws IOException {
        StringBuilder mapString = new StringBuilder();

        char[] letters = new char[TAMANIO];
        for (int i = 0; i < TAMANIO; i++)
            letters[i] = (char) ('A' + i);

        mapString.append("    ");
        for (int i = 0; i < TAMANIO; i++) {
            mapString.append("[" + i + "] ");
        }
        mapString.append("\n");

        for (int i = 0; i < TAMANIO; i++) {
            mapString.append("[" + letters[i] + "]  ");
            for (int j = 0; j < TAMANIO; j++) {
                mapString.append(map[i][j] + "   ");
            }
            mapString.append("\n");
        }

        try {
            out.writeObject(mapString.toString());
        } catch (IOException e) {
            System.err.println("Error al imprimir el mapa.");
        }
    }

    private static int randomIndex() {
        return new Random().nextInt(TAMANIO);
    }

    private void closeConnections() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start(5555);
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor.");
            e.printStackTrace();
        }
    }
}