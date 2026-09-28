public class Gerador {

    public static class SemAleatorios extends RuntimeException {
        public SemAleatorios() { super("lista de numeros aleatorios esgotada"); }
    }

    private static final long A = 1103515245L;
    private static final long C = 12345L;
    private static final long M = 2147483648L;

    private final double[] lista; 
    private int indice = 0;
    private long previous;
    private long restantes;

    public Gerador(long semente, long limite) {
        this.lista = null;
        this.previous = semente;
        this.restantes = limite;
    }

    public Gerador(double[] lista) {
        this.lista = lista;
        this.restantes = lista.length;
    }

    public double proximo() {
        if (lista != null) {
            if (indice >= lista.length) throw new SemAleatorios();
            restantes--;
            return lista[indice++];
        }
        previous = (A * previous + C) % M;
        restantes--;
        return (double) previous / (double) M;
    }

    public boolean acabou() { return restantes <= 0; }
}
