import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Modelo da rede de filas carregado de um arquivo .yml. */
public class Modelo {

    public static class ConfigFila {
        public String nome;
        public int servidores;
        public int capacidade = -1;           // -1 = infinita
        public boolean chegadaExterna = false;
        public double primeiraChegada;
        public double minChegada, maxChegada;
        public double minAtendimento, maxAtendimento;
    }

    public static class ModeloInvalido extends Exception {
        public ModeloInvalido(String msg) { super(msg); }
    }

    public final List<ConfigFila> filas = new ArrayList<>();
    public double[][] roteamento;
    public long[] seeds = null;
    public long aleatoriosPorSemente = 0;
    public double[] rndnumbers = null;

    public static Modelo carrega(String caminho) throws IOException, ModeloInvalido {
        Object raiz;
        try {
            raiz = YamlSimples.parse(Files.readAllLines(Paths.get(caminho)));
        } catch (IllegalArgumentException e) {
            throw new ModeloInvalido(e.getMessage());
        }
        if (!(raiz instanceof Map)) throw new ModeloInvalido("Arquivo YAML vazio ou mal formatado.");
        Modelo m = new Modelo();
        m.monta(asMap(raiz));
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) { return (Map<String, Object>) o; }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) { return (List<Object>) o; }

    private void monta(Map<String, Object> raiz) throws ModeloInvalido {
        List<String> erros = new ArrayList<>();
        Map<String, Integer> indice = new LinkedHashMap<>();

        // ---- queues ----
        Object q = raiz.get("queues");
        if (!(q instanceof Map)) throw new ModeloInvalido("Secao 'queues' ausente.");
        Map<String, Object> mq = asMap(q);
        for (Map.Entry<String, Object> e : mq.entrySet()) {
            ConfigFila f = new ConfigFila();
            f.nome = e.getKey();
            if (!(e.getValue() instanceof Map)) {
                erros.add("Fila " + f.nome + ": sem parametros.");
                continue;
            }
            Map<String, Object> p = asMap(e.getValue());
            Double s = num(p, "servers", f.nome, true, erros);
            if (s != null) {
                f.servidores = s.intValue();
                if (f.servidores < 1) erros.add("Fila " + f.nome + ": 'servers' deve ser >= 1.");
            }
            Double cap = num(p, "capacity", f.nome, false, erros);
            if (cap != null) f.capacidade = cap.intValue();     // ausente = infinita
            Double mn = num(p, "minService", f.nome, true, erros);
            Double mx = num(p, "maxService", f.nome, true, erros);
            if (mn != null && mx != null) {
                f.minAtendimento = mn; f.maxAtendimento = mx;
                if (mn > mx) erros.add("Fila " + f.nome + ": minService > maxService.");
            }
            f.minChegada = valorOuZero(p, "minArrival", f.nome, erros);
            f.maxChegada = valorOuZero(p, "maxArrival", f.nome, erros);
            indice.put(f.nome, filas.size());
            filas.add(f);
        }
        if (filas.isEmpty()) erros.add("Nenhuma fila definida em 'queues'.");

        // ---- arrivals ----
        Object a = raiz.get("arrivals");
        if (!(a instanceof Map) || asMap(a).isEmpty()) {
            erros.add("Secao 'arrivals' ausente: e preciso ao menos uma chegada externa.");
        } else {
            for (Map.Entry<String, Object> e : asMap(a).entrySet()) {
                Integer i = indice.get(e.getKey());
                if (i == null) { erros.add("arrivals: fila '" + e.getKey() + "' nao existe em 'queues'."); continue; }
                ConfigFila f = filas.get(i);
                try {
                    f.primeiraChegada = Double.parseDouble(String.valueOf(e.getValue()).trim());
                    f.chegadaExterna = true;
                } catch (NumberFormatException ex) {
                    erros.add("arrivals: valor invalido para " + e.getKey() + ".");
                    continue;
                }
                Map<String, Object> p = asMap(mq.get(f.nome));
                if (!p.containsKey("minArrival") || !p.containsKey("maxArrival")) {
                    erros.add("Fila " + f.nome + " recebe chegadas externas: faltam 'minArrival' e/ou 'maxArrival'.");
                } else if (f.minChegada > f.maxChegada) {
                    erros.add("Fila " + f.nome + ": minArrival > maxArrival.");
                }
            }
        }

        // ---- network ----
        roteamento = new double[filas.size()][filas.size()];
        Object n = raiz.get("network");
        if (n instanceof List) {
            for (Object item : asList(n)) {
                if (!(item instanceof Map)) { erros.add("network: item invalido."); continue; }
                Map<String, Object> r = asMap(item);
                String src = String.valueOf(r.get("source")), dst = String.valueOf(r.get("target"));
                Integer is = indice.get(src), id = indice.get(dst);
                if (is == null) erros.add("network: source '" + src + "' nao existe.");
                if (id == null) erros.add("network: target '" + dst + "' nao existe.");
                Double pr = num(r, "probability", src + "->" + dst, true, erros);
                if (is != null && id != null && pr != null) {
                    if (pr < 0 || pr > 1) erros.add("network: probabilidade fora de [0,1] em " + src + "->" + dst + ".");
                    roteamento[is][id] += pr;
                }
            }
            for (int i = 0; i < filas.size(); i++) {
                double soma = 0;
                for (double v : roteamento[i]) soma += v;
                if (soma > 1.0 + 1e-9) erros.add("network: probabilidades saindo de " + filas.get(i).nome + " somam " + soma + " (> 1).");
            }
        }

        // ---- aleatorios ----
        Object sd = raiz.get("seeds");
        Object rl = raiz.get("rndnumbers");
        if (sd instanceof List && !asList(sd).isEmpty()) {
            seeds = new long[asList(sd).size()];
            for (int i = 0; i < seeds.length; i++) {
                try { seeds[i] = Long.parseLong(String.valueOf(asList(sd).get(i)).trim()); }
                catch (NumberFormatException ex) { erros.add("seeds: valor invalido '" + asList(sd).get(i) + "'."); }
            }
            try {
                aleatoriosPorSemente = Long.parseLong(String.valueOf(raiz.get("rndnumbersPerSeed")).trim());
                if (aleatoriosPorSemente < 1) erros.add("rndnumbersPerSeed deve ser >= 1.");
            } catch (NumberFormatException ex) {
                erros.add("'seeds' informado, mas 'rndnumbersPerSeed' ausente ou invalido.");
            }
        } else if (rl instanceof List && !asList(rl).isEmpty()) {
            rndnumbers = new double[asList(rl).size()];
            for (int i = 0; i < rndnumbers.length; i++) {
                try { rndnumbers[i] = Double.parseDouble(String.valueOf(asList(rl).get(i)).trim()); }
                catch (NumberFormatException ex) { erros.add("rndnumbers: valor invalido '" + asList(rl).get(i) + "'."); }
            }
        } else {
            erros.add("Informe 'seeds' (com 'rndnumbersPerSeed') ou 'rndnumbers'.");
        }

        if (!erros.isEmpty()) {
            StringBuilder sb = new StringBuilder("Modelo invalido:");
            for (String e : erros) sb.append("\n  - ").append(e);
            throw new ModeloInvalido(sb.toString());
        }
    }

    private static Double num(Map<String, Object> m, String chave, String ctx, boolean obrigatorio, List<String> erros) {
        Object v = m.get(chave);
        if (v == null) {
            if (obrigatorio) erros.add("Fila " + ctx + ": parametro obrigatorio '" + chave + "' ausente.");
            return null;
        }
        try {
            return Double.parseDouble(v.toString().trim());
        } catch (NumberFormatException e) {
            erros.add(ctx + ": valor invalido para '" + chave + "': " + v);
            return null;
        }
    }

    private static double valorOuZero(Map<String, Object> m, String chave, String ctx, List<String> erros) {
        Double d = num(m, chave, ctx, false, erros);
        return d == null ? 0.0 : d;
    }
}
