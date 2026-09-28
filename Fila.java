import java.util.Arrays;

/** Fila G/G/servidores/capacidade (capacidade < 0 = infinita). */
public class Fila {

    private final int id;
    private final String nome;
    private final int servidores;
    private final int capacidade;

    private final double minChegada, maxChegada, primeiraChegada;
    private final boolean chegadaExterna;
    private final double minAtendimento, maxAtendimento;

    private int clientes = 0;
    private long perdas = 0L;

    private final boolean[] servidorOcupado;
    private double[] tempoAcumuladoEstado;

    public Fila(int id, Modelo.ConfigFila cfg) {
        this.id = id;
        this.nome = cfg.nome;
        this.servidores = cfg.servidores;
        this.capacidade = cfg.capacidade;
        this.chegadaExterna = cfg.chegadaExterna;
        this.minChegada = cfg.minChegada;
        this.maxChegada = cfg.maxChegada;
        this.primeiraChegada = cfg.primeiraChegada;
        this.minAtendimento = cfg.minAtendimento;
        this.maxAtendimento = cfg.maxAtendimento;

        this.servidorOcupado = new boolean[servidores];
        int tam = capacidade >= 0 ? capacidade + 1 : Math.max(servidores, 16) + 1;
        this.tempoAcumuladoEstado = new double[tam];
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public int getServidores() { return servidores; }
    public int getCapacidade() { return capacidade; }
    public boolean temChegadaExterna() { return chegadaExterna; }
    public double getPrimeiraChegada() { return primeiraChegada; }
    public long getPerdas() { return perdas; }
    public double[] getTempoAcumuladoEstado() { return tempoAcumuladoEstado; }

    public boolean estaCheia() { return capacidade >= 0 && clientes >= capacidade; }
    public void entra() { clientes++; }
    public void sai() { clientes--; }
    public void perdeCliente() { perdas++; }

    public void acumulaTempo(double delta) {
        if (clientes >= tempoAcumuladoEstado.length) {
            tempoAcumuladoEstado = Arrays.copyOf(tempoAcumuladoEstado,
                    Math.max(clientes + 1, tempoAcumuladoEstado.length * 2));
        }
        tempoAcumuladoEstado[clientes] += delta;
    }

    public int indiceServidorLivre() {
        for (int i = 0; i < servidores; i++) {
            if (!servidorOcupado[i]) return i;
        }
        return -1;
    }

    public void ocupaServidor(int idx) { servidorOcupado[idx] = true; }
    public void liberaServidor(int idx) { servidorOcupado[idx] = false; }

    public boolean temClienteEsperando() { return clientes >= servidores; }

    public double tempoEntreChegadas(Gerador g) {
        return minChegada + g.proximo() * (maxChegada - minChegada);
    }

    public double tempoDeAtendimento(Gerador g) {
        return minAtendimento + g.proximo() * (maxAtendimento - minAtendimento);
    }
}
