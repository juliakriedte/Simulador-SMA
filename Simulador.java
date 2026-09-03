public class Simulador {
    static final int CAPACIDADE = 1;
    static long k = 5;

    static long a = 1103515245L;
    static long c = 12345L;
    static long m = 2147483648L;
    static long previous = 42L;

    static final double CHEGADA_MIN = 3.0;
    static final double CHEGADA_MAX = 5.0;

    static final double ATENDIMENTO_MIN = 4.0;
    static final double ATENDIMENTO_MAX = 5.0;

    static long numClientes = 0;
    static double relogio = 0.0;

    static boolean[] servidorOcupado = new boolean[CAPACIDADE];
    static double[] proximaSaidaServidor = new double[CAPACIDADE];

    static double proximaChegada = 3.0;
    static long perdas = 0;
    static double[] tempoAcumuladoEstado = new double[(int) k + 1];
    static long count = 100000;

    public static void main(String[] args) {
        for (int i = 0; i < CAPACIDADE; i++) {
            proximaSaidaServidor[i] = Double.MAX_VALUE;
        }

        while (count > 0) {
            Evento evento = nextEvent();

            if (evento == Evento.CHEGADA) {
                chegada();
            } else if (evento == Evento.SAIDA) {
                saida();
            }
        }

        System.out.println("G/G/" + CAPACIDADE + "/" + k);
        System.out.println("Tempo global da simulacao: " + relogio);
        System.out.println("Clientes perdidos: " + perdas);
        for (int i = 0; i <= k; i++) {
            double prob = tempoAcumuladoEstado[i] / relogio;
            System.out.println("Estado " + i + " -> tempo acumulado: " + tempoAcumuladoEstado[i] + " | probabilidade: " + prob);
        }
    }

    private static double nextRandom() {
        previous = ((a * previous + c) % m);
        count--;
        return (double) previous / (double) m;
    }

    private static double tempoEntreChegadas() {
        return CHEGADA_MIN + nextRandom() * (CHEGADA_MAX - CHEGADA_MIN);
    }

    private static double tempoDeAtendimento() {
        return ATENDIMENTO_MIN + nextRandom() * (ATENDIMENTO_MAX - ATENDIMENTO_MIN);
    }

    private static int indiceServidorLivre() {
        for (int i = 0; i < CAPACIDADE; i++) {
            if (!servidorOcupado[i]) {
                return i;
            }
        }
        return -1;
    }

    private static int indiceMenorSaida() {
        int idx = -1;
        double menor = Double.MAX_VALUE;
        for (int i = 0; i < CAPACIDADE; i++) {
            if (proximaSaidaServidor[i] < menor) {
                menor = proximaSaidaServidor[i];
                idx = i;
            }
        }
        return idx;
    }

    private static Evento nextEvent() {
        int idxSaida = indiceMenorSaida();
        double tempoSaida = (idxSaida == -1) ? Double.MAX_VALUE : proximaSaidaServidor[idxSaida];

        if (proximaChegada <= tempoSaida) {
            return Evento.CHEGADA;
        } else {
            return Evento.SAIDA;
        }
    }

    private static void chegada() {
        double delta = proximaChegada - relogio;
        tempoAcumuladoEstado[(int) numClientes] += delta;
        relogio = proximaChegada;

        if (numClientes < k) {
            numClientes++;

            int livre = indiceServidorLivre();
            if (livre != -1) {
                servidorOcupado[livre] = true;
                proximaSaidaServidor[livre] = relogio + tempoDeAtendimento();
            }

        } else {
            perdas++;
        }

        proximaChegada = relogio + tempoEntreChegadas();
    }

    private static void saida() {
        int servidor = indiceMenorSaida();

        double delta = proximaSaidaServidor[servidor] - relogio;
        tempoAcumuladoEstado[(int) numClientes] += delta;
        relogio = proximaSaidaServidor[servidor];

        numClientes--;

        if (numClientes >= CAPACIDADE) {
            proximaSaidaServidor[servidor] = relogio + tempoDeAtendimento();
        } else {
            servidorOcupado[servidor] = false;
            proximaSaidaServidor[servidor] = Double.MAX_VALUE;
        }
    }
}