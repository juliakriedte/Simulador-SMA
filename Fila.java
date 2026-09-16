import java.util.Arrays;

public class Fila {

    private final int id;
    private final int servidores;
    private final int capacidade;

    private final boolean chegadaExterna;
    private final double minChegada;
    private final double maxChegada;
    private final double primeiraChegada;

    private final double minAtendimento;
    private final double maxAtendimento;

    private int clientes;
    private long perdas;

    private final boolean[] servidorOcupado;
    private final double[] proximaSaidaServidor;
    private final double[] tempoAcumuladoEstado;

    public Fila(int id, int servidores, int capacidade, double minAtendimento, double maxAtendimento) {
        this(id, servidores, capacidade, 0, 0, false, 0, minAtendimento, maxAtendimento);
    }

    public Fila(int id, int servidores, int capacidade,
                double minChegada, double maxChegada, boolean chegadaExterna, double primeiraChegada,
                double minAtendimento, double maxAtendimento) {
        this.id = id;
        this.servidores = servidores;
        this.capacidade = capacidade;
        this.chegadaExterna = chegadaExterna;
        this.minChegada = minChegada;
        this.maxChegada = maxChegada;
        this.primeiraChegada = primeiraChegada;
        this.minAtendimento = minAtendimento;
        this.maxAtendimento = maxAtendimento;

        this.clientes = 0;
        this.perdas = 0L;

        this.servidorOcupado = new boolean[servidores];
        this.proximaSaidaServidor = new double[servidores];
        Arrays.fill(this.proximaSaidaServidor, Double.MAX_VALUE);

        this.tempoAcumuladoEstado = new double[capacidade + 1];
    }

    public int getId() { return id; }
    public int getServidores() { return servidores; }
    public int getCapacidade() { return capacidade; }
    public boolean temChegadaExterna() { return chegadaExterna; }
    public double getPrimeiraChegada() { return primeiraChegada; }
    public int getClientes() { return clientes; }
    public long getPerdas() { return perdas; }
    public double[] getTempoAcumuladoEstado() { return tempoAcumuladoEstado; }

    public boolean estaCheia() { return clientes >= capacidade; }
    public void entra() { clientes++; }
    public void sai() { clientes--; }
    public void perdeCliente() { perdas++; }
    public void acumulaTempo(double delta) { tempoAcumuladoEstado[clientes] += delta; }

    public int indiceServidorLivre() {
        for (int i = 0; i < servidores; i++) {
            if (!servidorOcupado[i]) return i;
        }
        return -1;
    }

    public void ocupaServidor(int idx, double tempoDeSaida) {
        servidorOcupado[idx] = true;
        proximaSaidaServidor[idx] = tempoDeSaida;
    }

    public void liberaServidor(int idx) {
        servidorOcupado[idx] = false;
        proximaSaidaServidor[idx] = Double.MAX_VALUE;
    }

    public boolean temClienteEsperando() { return clientes >= servidores; }

    public double tempoEntreChegadas() {
        return minChegada + Simulador.nextRandom() * (maxChegada - minChegada);
    }

    public double tempoDeAtendimento() {
        return minAtendimento + Simulador.nextRandom() * (maxAtendimento - minAtendimento);
    }
}