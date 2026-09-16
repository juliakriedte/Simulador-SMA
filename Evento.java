public class Evento implements Comparable<Evento> {

    private final TipoEvento tipo;
    private final double tempo;
    private final int fila;
    private final int servidor;

    public Evento(TipoEvento tipo, double tempo, int fila, int servidor) {
        this.tipo = tipo;
        this.tempo = tempo;
        this.fila = fila;
        this.servidor = servidor;
    }

    public TipoEvento getTipo() { return tipo; }
    public double getTempo() { return tempo; }
    public int getFila() { return fila; }
    public int getServidor() { return servidor; }

    @Override
    public int compareTo(Evento outro) {
        return Double.compare(this.tempo, outro.tempo);
    }
}