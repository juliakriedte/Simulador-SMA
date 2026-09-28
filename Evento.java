public class Evento implements Comparable<Evento> {

    private final TipoEvento tipo;
    private final double tempo;
    private final int fila;
    private final int servidor;
    private final long seq; // desempate deterministico para eventos no mesmo instante

    public Evento(TipoEvento tipo, double tempo, int fila, int servidor, long seq) {
        this.tipo = tipo;
        this.tempo = tempo;
        this.fila = fila;
        this.servidor = servidor;
        this.seq = seq;
    }

    public TipoEvento getTipo() { return tipo; }
    public double getTempo() { return tempo; }
    public int getFila() { return fila; }
    public int getServidor() { return servidor; }

    @Override
    public int compareTo(Evento outro) {
        int c = Double.compare(this.tempo, outro.tempo);
        return c != 0 ? c : Long.compare(this.seq, outro.seq);
    }
}
