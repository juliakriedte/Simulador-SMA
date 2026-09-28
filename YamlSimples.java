import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Leitor minimo de YAML (sem dependencias externas), suficiente para o formato do
 * simulador: mapas aninhados por indentacao, listas ("- item", inclusive
 * "- chave: valor") e valores escalares. Retorna Map, List ou String.
 * Comentarios (#) e a linha "!PARAMETERS" sao ignorados.
 */
public class YamlSimples {

    private static final Pattern CHAVE = Pattern.compile("^[^\\s:#-][^:#]*:(\\s.*)?$");

    private static class Linha {
        final int indent;
        final String texto;
        Linha(int indent, String texto) { this.indent = indent; this.texto = texto; }
        boolean ehItemLista() { return texto.equals("-") || texto.startsWith("- "); }
    }

    private final List<Linha> linhas = new ArrayList<>();
    private int pos = 0;

    public static Object parse(List<String> texto) {
        YamlSimples p = new YamlSimples();
        for (String bruta : texto) {
            String s = removeComentario(bruta.replace("\t", "    "));
            if (s.trim().isEmpty() || s.trim().startsWith("!")) continue;
            int indent = 0;
            while (indent < s.length() && s.charAt(indent) == ' ') indent++;
            p.linhas.add(new Linha(indent, s.trim()));
        }
        if (p.linhas.isEmpty()) return new LinkedHashMap<String, Object>();
        return p.bloco(p.linhas.get(0).indent);
    }

    private static String removeComentario(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '#' && (i == 0 || Character.isWhitespace(s.charAt(i - 1)))) {
                return s.substring(0, i);
            }
        }
        return s;
    }

    private Object bloco(int indent) {
        return linhas.get(pos).ehItemLista() ? lista(indent) : mapa(indent);
    }

    private Map<String, Object> mapa(int indent) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        while (pos < linhas.size()) {
            Linha l = linhas.get(pos);
            if (l.indent != indent || l.ehItemLista()) break;
            if (!CHAVE.matcher(l.texto).matches()) {
                throw new IllegalArgumentException("Linha YAML invalida: \"" + l.texto + "\"");
            }
            int dp = l.texto.indexOf(':');
            String chave = l.texto.substring(0, dp).trim();
            String valor = l.texto.substring(dp + 1).trim();
            pos++;
            if (valor.isEmpty()) {
                Object filho = null;
                if (pos < linhas.size()) {
                    Linha prox = linhas.get(pos);
                    if (prox.indent > indent || (prox.indent == indent && prox.ehItemLista())) {
                        filho = bloco(prox.indent);
                    }
                }
                mapa.put(chave, filho);
            } else {
                mapa.put(chave, valor);
            }
        }
        return mapa;
    }

    private List<Object> lista(int indent) {
        List<Object> lista = new ArrayList<>();
        while (pos < linhas.size()) {
            Linha l = linhas.get(pos);
            if (l.indent != indent || !l.ehItemLista()) break;
            String resto = l.texto.substring(1).trim();
            int espacos = l.texto.length() - 1 - l.texto.substring(1).stripLeading().length();
            int indentConteudo = indent + 1 + espacos;
            if (resto.isEmpty()) {
                pos++;
                lista.add(pos < linhas.size() && linhas.get(pos).indent > indent
                        ? bloco(linhas.get(pos).indent) : null);
            } else if (CHAVE.matcher(resto).matches()) {
                linhas.set(pos, new Linha(indentConteudo, resto));
                lista.add(mapa(indentConteudo));
            } else {
                lista.add(resto);
                pos++;
            }
        }
        return lista;
    }
}
