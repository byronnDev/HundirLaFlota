package hundirflota;

public class GameData {
    char[][] mapaUsuario;
    char[][] mapaOrdenador;
    char[][] mapaOrdenadorParaUsuario;
    int puntosUsuario;
    int puntosOrdenador;
    boolean juegoTerminado;
    int[] tiro;

    public int[] getTiro() {
        return tiro;
    }

    public void setTiro(int[] tiro) {
        this.tiro = tiro;
    }

    public char[][] getMapaUsuario() {
        return mapaUsuario;
    }

    public void setMapaUsuario(char[][] mapaUsuario) {
        this.mapaUsuario = mapaUsuario;
    }

    public char[][] getMapaOrdenador() {
        return mapaOrdenador;
    }

    public void setMapaOrdenador(char[][] mapaOrdenador) {
        this.mapaOrdenador = mapaOrdenador;
    }

    public char[][] getMapaOrdenadorParaUsuario() {
        return mapaOrdenadorParaUsuario;
    }

    public void setMapaOrdenadorParaUsuario(char[][] mapaOrdenadorParaUsuario) {
        this.mapaOrdenadorParaUsuario = mapaOrdenadorParaUsuario;
    }

    public int getPuntosUsuario() {
        return puntosUsuario;
    }

    public void setPuntosUsuario(int puntosUsuario) {
        this.puntosUsuario = puntosUsuario;
    }

    public int getPuntosOrdenador() {
        return puntosOrdenador;
    }

    public void setPuntosOrdenador(int puntosOrdenador) {
        this.puntosOrdenador = puntosOrdenador;
    }

    public boolean isJuegoTerminado() {
        return juegoTerminado;
    }

    public void setJuegoTerminado(boolean juegoTerminado) {
        this.juegoTerminado = juegoTerminado;
    }

    public GameData() {
    }
}
