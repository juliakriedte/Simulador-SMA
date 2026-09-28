import java.io.IOException;
import java.util.Locale;
import java.util.PriorityQueue;

/**
 * Simulador de redes de filas G/G/c/K com topologia arbitraria, carregada de um .yml.
 * Uso: java Simulador [arquivo.yml] [-v]
 */
public class Simulador {

    /** Resultado de uma execucao (uma semente ou uma lista de aleatorios). */
    static class Resultado {
        double tempoGlobal;
        long[] perdas;
        double[][] tempos;
    }

    private final Modelo modelo;
    private final Gerador gerador;
    private final Fila[] filas;
    private final PriorityQueue<Evento> escalonador = new PriorityQueue<>();
    private double relogio = 0.0;
    private long seq = 0;

    public Simulador(Modelo modelo, Gerador gerador) {
        this.modelo = modelo;
        this.gerador = gerador;
        this.filas = new Fila[modelo.filas.size()];
        for (int i = 0; i < filas.length; i++) filas[i] = new Fila(i, modelo.filas.get(i));
    }

    public Resultado executa() {
        for (Fila f : filas) {
            if (f.temChegadaExterna()) agenda(TipoEvento.CHEGADA, f.getPrimeiraChegada(), f.getId(), -1);
        }
        try {
            while (!gerador.acabou() && !escalonador.isEmpty()) {
                Evento e = escalonador.poll();
                if (e.getTipo() == TipoEvento.CHEGADA) trataChegadaExterna(e);
                else trataSaida(e);
            }
        } catch (Gerador.SemAleatorios fim) {
            // lista de rndnumbers acabou: encerra a simulacao
        }
        Resultado r = new Resultado();
        r.tempoGlobal = relogio;
        r.perdas = new long[filas.length];
        r.tempos = new double[filas.length][];
        for (int i = 0; i < filas.length; i++) {
            r.perdas[i] = filas[i].getPerdas();
            r.tempos[i] = filas[i].getTempoAcumuladoEstado().clone();
        }
        return r;
    }

    private void agenda(TipoEvento tipo, double tempo, int fila, int servidor) {
        escalonador.add(new Evento(tipo, tempo, fila, servidor, seq++));
    }

    private void acumulaTempoTodasFilas(double novoRelogio) {
        double delta = novoRelogio - relogio;
        for (Fila f : filas) f.acumulaTempo(delta);
        relogio = novoRelogio;
    }

    private void trataChegadaExterna(Evento evento) {
        acumulaTempoTodasFilas(evento.getTempo());
        Fila fila = filas[evento.getFila()];
        agenda(TipoEvento.CHEGADA, relogio + fila.tempoEntreChegadas(gerador), fila.getId(), -1);
        entraNaFila(fila);
    }

    private void trataSaida(Evento evento) {
        acumulaTempoTodasFilas(evento.getTempo());
        Fila fila = filas[evento.getFila()];
        int servidor = evento.getServidor();

        fila.liberaServidor(servidor);
        fila.sai();

        if (fila.temClienteEsperando()) {
            fila.ocupaServidor(servidor);
            agenda(TipoEvento.SAIDA, relogio + fila.tempoDeAtendimento(gerador), fila.getId(), servidor);
        }

        int destino = decideDestino(fila.getId());
        if (destino != -1) entraNaFila(filas[destino]);
    }

    private void entraNaFila(Fila fila) {
        if (fila.estaCheia()) {
            fila.perdeCliente();
            return;
        }
        fila.entra();
        int livre = fila.indiceServidorLivre();
        if (livre != -1) {
            fila.ocupaServidor(livre);
            agenda(TipoEvento.SAIDA, relogio + fila.tempoDeAtendimento(gerador), fila.getId(), livre);
        }
    }

    /** Sorteia o destino; retorna -1 se o cliente deixa o sistema. */
    private int decideDestino(int origem) {
        double[] linha = modelo.roteamento[origem];
        boolean temRota = false;
        for (double p : linha) if (p > 0) { temRota = true; break; }
        if (!temRota) return -1;               // fila sem saidas: nao consome aleatorio

        double r = gerador.proximo();
        double acumulado = 0.0;
        for (int j = 0; j < linha.length; j++) {
            acumulado += linha[j];
            if (r < acumulado) return j;
        }
        return -1;
    }

    // ------------------------------------------------------------------ saida

    private static String rotulo(Modelo.ConfigFila f) {
        return f.nome + ": G/G/" + f.servidores + (f.capacidade >= 0 ? "/" + f.capacidade : "");
    }

    private static void imprime(Modelo modelo, Resultado r, String titulo) {
        System.out.println("################ " + titulo + " ################");
        System.out.printf(Locale.US, "Tempo global da simulacao: %.4f%n%n", r.tempoGlobal);
        for (int i = 0; i < modelo.filas.size(); i++) {
            System.out.println("=== Fila " + rotulo(modelo.filas.get(i)) + " ===");
            System.out.println("Clientes perdidos: " + r.perdas[i]);
            double[] t = r.tempos[i];
            int ultimo = t.length - 1;
            if (modelo.filas.get(i).capacidade < 0) while (ultimo > 0 && t[ultimo] == 0) ultimo--;
            for (int s = 0; s <= ultimo; s++) {
                System.out.printf(Locale.US, "Estado %d -> tempo acumulado: %.4f | probabilidade: %.4f%%%n",
                        s, t[s], 100.0 * t[s] / r.tempoGlobal);
            }
            System.out.println();
        }
    }

    private static Resultado media(Modelo modelo, java.util.List<Resultado> rs) {
        int nf = modelo.filas.size(), n = rs.size();
        Resultado m = new Resultado();
        m.perdas = new long[nf];
        m.tempos = new double[nf][];
        double[] perdasMedia = new double[nf];
        for (int i = 0; i < nf; i++) {
            int tam = 0;
            for (Resultado r : rs) tam = Math.max(tam, r.tempos[i].length);
            m.tempos[i] = new double[tam];
            for (Resultado r : rs) {
                for (int s = 0; s < r.tempos[i].length; s++) m.tempos[i][s] += r.tempos[i][s] / n;
                perdasMedia[i] += (double) r.perdas[i] / n;
            }
            m.perdas[i] = Math.round(perdasMedia[i]);
        }
        for (Resultado r : rs) m.tempoGlobal += r.tempoGlobal / n;
        return m;
    }

    public static void main(String[] args) {
        String arquivo = "model.yml";
        boolean detalhado = false;
        for (String a : args) {
            if (a.equals("-v")) detalhado = true; else arquivo = a;
        }

        Modelo modelo;
        try {
            modelo = Modelo.carrega(arquivo);
        } catch (IOException e) {
            System.err.println("Nao foi possivel ler o arquivo '" + arquivo + "': " + e.getMessage());
            System.exit(1);
            return;
        } catch (Modelo.ModeloInvalido e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        if (modelo.seeds != null) {
            java.util.List<Resultado> todos = new java.util.ArrayList<>();
            for (long semente : modelo.seeds) {
                Resultado r = new Simulador(modelo, new Gerador(semente, modelo.aleatoriosPorSemente)).executa();
                todos.add(r);
                if (detalhado || modelo.seeds.length == 1) imprime(modelo, r, "Semente " + semente);
            }
            if (modelo.seeds.length > 1) {
                imprime(modelo, media(modelo, todos),
                        "MEDIA de " + todos.size() + " execucoes (" + modelo.aleatoriosPorSemente + " aleatorios cada)");
                System.out.println("(perdas exibidas como media arredondada; use -v para ver cada semente)");
            }
        } else {
            Resultado r = new Simulador(modelo, new Gerador(modelo.rndnumbers)).executa();
            imprime(modelo, r, "Lista de aleatorios (" + modelo.rndnumbers.length + " numeros)");
        }
    }
}
