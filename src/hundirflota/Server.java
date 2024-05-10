package hundirflota;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

/**
 * HUNDIR LA FLOTA
 * 
 * Se trata de un juego clásico en el que nos enfrentamos al ordenador. Cada
 * usuario dispone de un mapa de 10x10
 * casillas donde se colocan barcos (dos de tamaño 5, tres de tamaño 3, y cinco
 * de tamaño 1).
 * Jugador y ordenador irán diciendo posiciones en el mapa, y el jugador opuesto
 * debe indicar si la "bomba"
 * ha caído en el agua o ha tocado algún barco.
 * 
 * Para simplificar el juego, en lugar de verificar si un barco completo se ha
 * hundido y cuántos barcos quedan a flote,
 * contabilizamos el número total de casillas correspondientes a un barco que no
 * han sido "hundidas". Se parte con
 * 24 puntos (5+5+3+3+3+1+1+1+1+1)
 * 
 * Inspirado en el código original de Manuel Jesús Gallego Vela.
 */
public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private static ObjectOutputStream out;
    private ObjectInputStream in;

    // CONSTANTES, que nos sirven para representar algunos valores
    final static char AGUA_NO_TOCADO = '.';
    final static char AGUA = 'A';
    final static char TOCADO = 'X';

    // TAMAÑO DEL TABLERO
    final static int TAMANIO = 10;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor iniciado, esperando por cliente...");
            clientSocket = serverSocket.accept();
            System.out.println("Cliente conectado.");

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            out.writeObject("Bienvenido al juego Hundir la Flota!");

            // Manejo del juego
            playGame();

            stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playGame() {
        // Lógica del juego
        // Mapa del usuario y del ordenador
        char[][] mapaUsuario = new char[TAMANIO][TAMANIO];
        char[][] mapaOrdenador = new char[TAMANIO][TAMANIO];
        // Este tercer mapa nos sirve para anotar y visualizar
        // las tiradas que hacemos sobre el mapa del ordenador
        char[][] mapaOrdenadorParaUsuario = new char[TAMANIO][TAMANIO];
        // Puntos con los que comienzan las partidas
        int puntosUsuario = 24;
        int puntosOrdenador = 24;
        // Lleva el control del programa.
        // Si no quedan barcos a flote del jugador o el ordenador, lo ponemos a true
        boolean juegoTerminado = false;
        // Indica si el tiro es correcto, para volver a realizar otro
        try {
            boolean tiroCorrecto = false;
            // Posiciones de la tirada
            int[] tiro = new int[2];
            // Inicializamos los mapas, colocando los barcos
            inicializacion(mapaUsuario, mapaOrdenador);
            // Inicializamos el mapa de registro a AGUA_NO_TOCADO
            inicializaMapaRegistro(mapaOrdenadorParaUsuario);
            // Mientras queden barcos a flote
            while (!juegoTerminado) {
                // Al principio del turno, pintamos el mapa del usuario
                out.writeObject("MAPA DEL USUARIO:\n");
                imprimirMapa(mapaUsuario);
                out.writeObject("PUNTOS RESTANTES DEL JUGADOR: " + puntosUsuario);
                out.writeObject("TURNO DEL JUGADOR");

                // Comenzamos con la tirada del usuario
                tiroCorrecto = false;
                while (!tiroCorrecto) {
                    tiro = pedirCasilla();
                    // Verificamos si el tiro es correcto o no
                    if (tiro[0] != -1 && tiro[1] != -1) {
                        // Puede ser INCORRECTO porque ya haya tirado
                        // sobre esas coordenadas
                        tiroCorrecto = evaluarTiro(mapaOrdenador, tiro);
                        if (!tiroCorrecto)
                            out.writeObject("TIRO INCORRECTO");
                    } else {
                        out.writeObject("TIRO INCORRECTO");
                    }
                    // De no serlo, el jugador debe volver a tirar
                }

                // Actualizamos mapa del ordenador y los puntos
                int puntosOrdenadorAnterior = puntosOrdenador;
                puntosOrdenador = actualizarMapa(mapaOrdenador, tiro, puntosOrdenador);
                // Actualizamos nuestro mapa de registro y lo enviamos al cliente.
                // Sabemos si la tirada ha sido AGUA O TOCADO si el número de puntos se ha
                // decrementado.
                char tipoTiro = (puntosOrdenadorAnterior - puntosOrdenador) > 0 ? TOCADO : AGUA;
                actualizarMapaRegistro(mapaOrdenadorParaUsuario, tiro, tipoTiro);
                out.writeObject("\nREGISTRO DEL MAPA DEL ORDENADOR");
                out.writeObject(mapaOrdenadorParaUsuario);

                // El juego termina si el n�mero de puntos llega a 0
                juegoTerminado = (puntosOrdenador == 0);

                // Si no ha ganado el jugador, le toca a la máquina
                if (!juegoTerminado) {
                    out.writeObject("PUNTOS RESTANTES DEL ORDENADOR: " + puntosOrdenador);
                    out.writeObject("TURNO DEL ORDENADOR");
                    tiroCorrecto = false;
                    // Seguimos los mismos parámetros de comprobación que en la tirada del usuario
                    while (!tiroCorrecto) {
                        tiro = generaDisparoAleatorio();
                        tiroCorrecto = evaluarTiro(mapaUsuario, tiro);
                    }

                    // Actualizamos mapa
                    puntosUsuario = actualizarMapa(mapaUsuario, tiro, puntosUsuario);
                    // El juego termina si el número de puntos llega a 0
                    juegoTerminado = (puntosUsuario == 0);
                }
            } // FIN DE LA PARTIDA. Alguien ha ganado
            if (puntosOrdenador == 0) {
                out.writeObject("EL VENCEDOR HA SIDO EL JUGADOR");
            } else
                out.writeObject("EL VENCEDOR HA SIDO EL ORDENADOR");
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

    /*
     * Método que sirve para que el ordenador pueda hacer un disparo
     */
    private static int[] generaDisparoAleatorio() {
        return new int[] { aleatorio(), aleatorio() };
    }

    /*
     * Método que aglutina la inicialización de ambos mapas
     */
    public static void inicializacion(char[][] m1, char[][] m2) {
        inicializaMapa(m1);
        inicializaMapa(m2);
    }

    /*
     * Método que inicializa el mapa que mostramos al usuario
     * con las tiradas que ha hecho sobre el mapa del ordenador.
     */
    private static void inicializaMapaRegistro(char[][] mapa) {
        // Inicializamos el mapa entero a AGUA_NO_TOCADO
        for (int i = 0; i < TAMANIO; i++)
            for (int j = 0; j < TAMANIO; j++)
                mapa[i][j] = AGUA_NO_TOCADO;
    }

    /*
     * Método que inicializa un mapa de juego, colocando
     * los barcos sobre el mismo.
     */
    private static void inicializaMapa(char[][] mapa) {
        // Inicializamos el mapa entero a AGUA_NO_TOCADO
        for (int i = 0; i < TAMANIO; i++)
            for (int j = 0; j < TAMANIO; j++)
                mapa[i][j] = AGUA_NO_TOCADO;

        // 2 portaaviones (5 casillas)
        // 3 buques (3 casillas)
        // 5 lanchas (1 casilla)
        int[] barcos = { 5, 5, 3, 3, 3, 1, 1, 1, 1, 1 };

        // Posible dirección de colocación del barco
        char[] direccion = { 'V', 'H' };

        // Para cada barco
        for (int b : barcos) {
            // Intentamos tantas veces sea necesarias para colocar el barco en el mapa.
            // Vamos de mayor tamaño a menor, para que sea menos
            // dificultoso encontrar un hueco
            boolean colocado = false;
            while (!colocado) {
                // Obtenemos una posición y dirección aleatorias
                int fila = aleatorio();
                int columna = aleatorio();
                char direcc = direccion[aleatorio() % 2];

                // �Cabe el barco en la posici�n indicada?
                if (direcc == 'V') {
                    if (fila + b <= (TAMANIO - 1)) {
                        // comprobamos que no hay otro barco que se solape
                        boolean otro = false;
                        for (int i = fila; (i <= fila + b) && !otro; i++) {
                            if (mapa[i][columna] != AGUA_NO_TOCADO)
                                otro = true;
                        }
                        // Si no hay otro barco, lo colocamos
                        if (!otro) {
                            for (int i = fila; i < fila + b; i++) {
                                mapa[i][columna] = Integer.toString(b).charAt(0);
                            }
                            colocado = true;
                        }
                    }
                } else { // direcc == 'H'
                    if (columna + b <= (TAMANIO - 1)) {
                        // comprobamos que no hay otro barco que se solape
                        boolean otro = false;
                        for (int j = columna; (j <= columna + b) && !otro; j++) {
                            if (mapa[fila][j] != AGUA_NO_TOCADO)
                                otro = true;
                        }
                        // Si no hay otro barco, lo colocamos
                        if (!otro) {
                            for (int j = columna; j < columna + b; j++) {
                                mapa[fila][j] = Integer.toString(b).charAt(0);
                            }
                            colocado = true;
                        }
                    }
                }

            }
        }

    }

    /*
     * M�todo que nos devuelve un n�mero aleatorio
     */
    private static int aleatorio() {
        Random r = new Random(System.currentTimeMillis());
        return r.nextInt(TAMANIO);
    }

    /*
     * M�todo que imprime un mapa, con una fila y columna de encabezados
     */
    public static void imprimirMapa(char[][] mapa) {

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * M�todo mediante el cual el usuario introduce una casilla
     */
    @SuppressWarnings("resource")
    private static int[] pedirCasilla() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Introduzca la casilla (por ejemplo B4): ");
        String linea = sc.nextLine();
        linea = linea.toUpperCase();
        int[] t;

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
     * M�todo que nos permite evaluar si un tiro es CORRECTO (AGUA o TOCADO)
     * o se trata de una casilla por la que ya hemos pasado antes.
     */
    public static boolean evaluarTiro(char[][] mapa, int[] t) {
        int fila = t[0];
        int columna = t[1];
        return mapa[fila][columna] == AGUA_NO_TOCADO || (mapa[fila][columna] >= '1' && mapa[fila][columna] <= '5');

    }

    /*
     * M�todo que actualiza el mapa, con un determinado tiro.
     * Devolvemos el n�mero de puntos restantes.
     */
    private static int actualizarMapa(char[][] mapa, int[] t, int puntos) {
        int fila = t[0];
        int columna = t[1];

        if (mapa[fila][columna] == AGUA_NO_TOCADO) {
            mapa[fila][columna] = AGUA;
            System.out.println("AGUA");
        } else {
            mapa[fila][columna] = TOCADO;
            System.out.println("HAS ALCANZADO A ALG�N BARCO");
            --puntos;
        }

        return puntos;

    }

    /*
     * M�todo que actualiza el mapa de registro
     */
    private static void actualizarMapaRegistro(char[][] mapa, int[] t, char valor) {
        int fila = t[0];
        int columna = t[1];

        mapa[fila][columna] = valor;
    }

}
