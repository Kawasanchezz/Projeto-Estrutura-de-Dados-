package unicsul.campus;

/**
 * Os 7 campi da universidade. O codigo e a versao sem acento e sem espacos do
 * nome, usada nos enderecos da API e nas opcoes do formulario.
 */
public enum Campus {

    ANALIA_FRANCO("Anália Franco", "analia-franco"),
    GUARULHOS("Guarulhos", "guarulhos"),
    LIBERDADE("Liberdade", "liberdade"),
    PAULISTA("Paulista", "paulista"),
    SAO_MIGUEL("São Miguel", "sao-miguel"),
    SANTO_AMARO("Santo Amaro", "santo-amaro"),
    VILLA_LOBOS("Villa Lobos", "villa-lobos");

    private final String nome;
    private final String codigo;

    Campus(String nome, String codigo) {
        this.nome = nome;
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigo() {
        return codigo;
    }

    /** Devolve o campus com esse codigo, ou null se o codigo nao existir. */
    public static Campus procurarPorCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }

        for (Campus campus : values()) {
            if (campus.codigo.equalsIgnoreCase(codigo.trim())) {
                return campus;
            }
        }
        return null;
    }
}
