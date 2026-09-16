import java.util.PriorityQueue;

public class Simulador {

    static long a = 1103515245L;
    static long c = 12345L;
    static long m = 2147483648L;
    static long previous = 42L;
    static long count = 100000;

    static double nextRandom() {
        previous = (a * previous + c) % m;
        count--;
        return (double) previous / (double) m;
    }

    static double relogio = 0.0;
    static Fila[] filas;
    static double[][] roteamento;
    static PriorityQueue<Evento> escalonador = new PriorityQueue<>();

    public static void main(String[] args) {

        Fila fila1 = new Fila(
                0,
                2,
                3,
                1.0, 5.0,
                true,
                2.5,
                4.0, 5.0
        );

        Fila fila2 = new Fila(
                1,
                1,
                5,
                1.0, 3.0
        );

        filas = new Fila[] { fila1, fila2 };

        roteamento = new double[][] {
                { 0.0, 1.0 },
                { 0.0, 0.0 }
        };

        for (Fila f : filas) {
            if (f.temChegadaExterna()) {
                escalonador.add(new Evento(TipoEvento.CHEGADA, f.getPrimeiraChegada(), f.getId(), -1));
            }
        }

        while (count > 0 && !escalonador.isEmpty()) {
            Evento evento = escalonador.poll();

            if (evento.getTipo() == TipoEvento.CHEGADA) {
                trataChegadaExterna(evento);
            } else {
                trataSaida(evento);
            }
        }

        imprimeResultados();
    }

    private static void acumulaTempoTodasFilas(double novoRelogio) {
        double delta = novoRelogio - relogio;
        for (Fila f : filas) {
            f.acumulaTempo(delta);
        }
        relogio = novoRelogio;
    }

    private static void trataChegadaExterna(Evento evento) {
        acumulaTempoTodasFilas(evento.getTempo());

        Fila fila = filas[evento.getFila()];

        double proxima = relogio + fila.tempoEntreChegadas();
        escalonador.add(new Evento(TipoEvento.CHEGADA, proxima, fila.getId(), -1));

        entraNaFila(fila);
    }

    private static void trataSaida(Evento evento) {
        acumulaTempoTodasFilas(evento.getTempo());

        Fila fila = filas[evento.getFila()];
        int servidor = evento.getServidor();

        fila.liberaServidor(servidor);
        fila.sai();

        if (fila.temClienteEsperando()) {
            double tempoSaida = relogio + fila.tempoDeAtendimento();
            fila.ocupaServidor(servidor, tempoSaida);
            escalonador.add(new Evento(TipoEvento.SAIDA, tempoSaida, fila.getId(), servidor));
        }

        int destino = decideDestino(fila.getId());
        if (destino != -1) {
            entraNaFila(filas[destino]);
        }
    }

    private static void entraNaFila(Fila fila) {
        if (fila.estaCheia()) {
            fila.perdeCliente();
            return;
        }

        fila.entra();

        int livre = fila.indiceServidorLivre();
        if (livre != -1) {
            double tempoSaida = relogio + fila.tempoDeAtendimento();
            fila.ocupaServidor(livre, tempoSaida);
            escalonador.add(new Evento(TipoEvento.SAIDA, tempoSaida, fila.getId(), livre));
        }
    }

    private static int decideDestino(int origem) {
        double r = nextRandom();
        double acumulado = 0.0;
        for (int j = 0; j < filas.length; j++) {
            acumulado += roteamento[origem][j];
            if (r < acumulado) {
                return j;
            }
        }
        return -1;
    }

    private static void imprimeResultados() {
        for (Fila fila : filas) {
            System.out.println("=== Fila " + (fila.getId() + 1) + ": G/G/" + fila.getServidores()
                    + "/" + fila.getCapacidade() + " ===");
            System.out.println("Tempo global da simulacao: " + relogio);
            System.out.println("Clientes perdidos: " + fila.getPerdas());

            double[] tempos = fila.getTempoAcumuladoEstado();
            for (int i = 0; i < tempos.length; i++) {
                double prob = tempos[i] / relogio;
                System.out.println("Estado " + i + " -> tempo acumulado: " + tempos[i]
                        + " | probabilidade: " + prob);
            }
            System.out.println();
        }
    }
}