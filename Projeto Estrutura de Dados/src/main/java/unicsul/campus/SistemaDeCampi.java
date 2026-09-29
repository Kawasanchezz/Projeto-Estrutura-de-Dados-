package unicsul.campus;

import unicsul.arvore.ArvoreBinariaBusca;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Junta as 7 arvores binarias, uma por campus, e aplica as regras do trabalho:
 * cadastrar, localizar, excluir e listar alunos.
 *
 * Os metodos sao synchronized porque o servidor atende varios pedidos ao mesmo
 * tempo, em threads diferentes, e as arvores nao podem ser alteradas por duas
 * threads de uma vez.
 */
public class SistemaDeCampi {

    private static final int TAMANHO_MINIMO = 2;
    private static final int TAMANHO_MAXIMO = 80;

    // Limite de seguranca: uma arvore sem balanceamento pode virar uma lista, e as
    // operacoes recursivas estouram a pilha em arvores muito profundas.
    private static final int MAXIMO_POR_CAMPUS = 1000;

    private final Map<Campus, ArvoreBinariaBusca> arvores = new EnumMap<>(Campus.class);

    public SistemaDeCampi() {
        for (Campus campus : Campus.values()) {
            arvores.put(campus, new ArvoreBinariaBusca());
        }
    }

    /**
     * Cadastra o aluno no campus escolhido.
     * Devolve null quando deu certo ou a mensagem de erro quando o nome e
     * invalido ou o aluno ja esta matriculado em algum campus.
     */
    public synchronized String cadastrar(String nome, Campus campus) {
        String erro = validarNome(nome);
        if (erro != null) {
            return erro;
        }
        if (campus == null) {
            return "Selecione um dos 7 campi.";
        }

        String nomeLimpo = limpar(nome);

        // Regra principal do trabalho: o nome nao pode existir em nenhuma das 7 arvores.
        Campus campusExistente = localizarCampus(nomeLimpo);
        if (campusExistente != null) {
            String nomeCadastrado = arvores.get(campusExistente).buscar(nomeLimpo);
            return "O aluno " + nomeCadastrado + " já está cadastrado no campus "
                    + campusExistente.getNome() + ".";
        }

        if (arvores.get(campus).tamanho() >= MAXIMO_POR_CAMPUS) {
            return "O campus " + campus.getNome() + " atingiu o limite de "
                    + MAXIMO_POR_CAMPUS + " alunos.";
        }

        arvores.get(campus).inserir(nomeLimpo);
        return null;
    }

    /** Devolve null quando o nome pode ser usado ou a mensagem do erro encontrado. */
    public String validarNome(String nome) {
        String nomeLimpo = limpar(nome);

        if (nomeLimpo.isEmpty()) {
            return "Informe o nome do aluno.";
        }
        if (nomeLimpo.length() < TAMANHO_MINIMO) {
            return "O nome deve ter pelo menos " + TAMANHO_MINIMO + " caracteres.";
        }
        if (nomeLimpo.length() > TAMANHO_MAXIMO) {
            return "O nome deve ter no máximo " + TAMANHO_MAXIMO + " caracteres.";
        }

        for (int i = 0; i < nomeLimpo.length(); i++) {
            char letra = nomeLimpo.charAt(i);
            boolean aceita = Character.isLetter(letra) || letra == ' '
                    || letra == '-' || letra == '.' || letra == '\'';
            if (!aceita) {
                return "O nome deve conter apenas letras, espaços, hífen, apóstrofo ou ponto.";
            }
        }

        return null;
    }

    /**
     * Procura o aluno nas 7 arvores, na ordem dos campi, e para assim que
     * encontra. Devolve o campus do aluno ou null se ele nao existir.
     */
    public synchronized Campus localizarCampus(String nome) {
        for (Campus campus : Campus.values()) {
            if (arvores.get(campus).buscar(nome) != null) {
                return campus;
            }
        }
        return null;
    }

    public synchronized String nomeCadastrado(String nome) {
        Campus campus = localizarCampus(nome);
        if (campus == null) {
            return null;
        }
        return arvores.get(campus).buscar(nome);
    }

    /**
     * Exclui o aluno do campus onde ele estiver matriculado.
     * Assim como a localizacao, a busca e feita nas 7 arvores: quem exclui nao
     * precisa saber de antemao em qual campus o nome esta.
     *
     * Devolve o campus de onde o aluno foi removido, ou null se o nome nao
     * existir em nenhum dos 7 campi.
     */
    public synchronized Campus excluir(String nome) {
        Campus campus = localizarCampus(nome);
        if (campus == null) {
            return null;
        }
        arvores.get(campus).remover(nome);
        return campus;
    }

    /** Alunos do campus em ordem alfabetica, pelo percurso em ordem da arvore. */
    public synchronized List<String> listarAlunos(Campus campus) {
        return arvores.get(campus).listarEmOrdem();
    }

    public synchronized String desenharArvore(Campus campus) {
        return arvores.get(campus).desenhar();
    }

    public synchronized int totalDoCampus(Campus campus) {
        return arvores.get(campus).tamanho();
    }

    public synchronized int alturaDaArvore(Campus campus) {
        return arvores.get(campus).altura();
    }

    public synchronized int totalDeAlunos() {
        int total = 0;
        for (Campus campus : Campus.values()) {
            total = total + arvores.get(campus).tamanho();
        }
        return total;
    }

    /** Os 7 campi, na ordem usada para montar os seletores da tela. */
    public List<Campus> getCampi() {
        List<Campus> lista = new ArrayList<>();
        for (Campus campus : Campus.values()) {
            lista.add(campus);
        }
        return lista;
    }

    private String limpar(String nome) {
        if (nome == null) {
            return "";
        }
        return nome.trim().replaceAll("\\s+", " ");
    }
}
