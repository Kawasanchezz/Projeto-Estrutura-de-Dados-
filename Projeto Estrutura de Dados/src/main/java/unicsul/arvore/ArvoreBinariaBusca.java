package unicsul.arvore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Arvore Binaria de Busca (ABB) de nomes de alunos.
 *
 * A arvore foi implementada do zero, com nos e ponteiros, sem usar TreeMap,
 * TreeSet ou Collections.sort.
 *
 * Propriedade da ABB: para qualquer no, os nomes da subarvore da esquerda sao
 * menores e os da direita sao maiores. Por isso o percurso em ordem devolve os
 * nomes ja em ordem alfabetica.
 *
 * Complexidade (n = quantidade de alunos): inserir, buscar e remover sao O(log n)
 * no caso medio e O(n) no pior caso, quando os nomes sao inseridos em ordem
 * alfabetica e a arvore vira uma lista. O percurso em ordem e sempre O(n).
 */
public class ArvoreBinariaBusca {

    private No raiz;

    public boolean inserir(String nome) {
        if (buscar(nome) != null) {
            return false;
        }
        raiz = inserir(raiz, new No(nome));
        return true;
    }

    private No inserir(No no, No novo) {
        if (no == null) {
            return novo;
        }

        int comparacao = novo.chave.compareTo(no.chave);
        if (comparacao < 0) {
            no.esquerda = inserir(no.esquerda, novo);
        } else {
            no.direita = inserir(no.direita, novo);
        }
        return no;
    }

    public String buscar(String nome) {
        return buscar(raiz, padronizar(nome));
    }

    private String buscar(No no, String chave) {
        if (no == null) {
            return null;
        }

        int comparacao = chave.compareTo(no.chave);
        if (comparacao == 0) {
            return no.nome;
        } else if (comparacao < 0) {
            return buscar(no.esquerda, chave);
        } else {
            return buscar(no.direita, chave);
        }
    }

    /**
     * Remove um aluno da arvore, se ele existir.
     *
     * Tem tres casos possiveis para o no encontrado:
     *   - folha (sem filhos): e removido, o lugar dele vira null;
     *   - um filho: o no e substituido pelo proprio filho;
     *   - dois filhos: nao da para remover o no sem furar a arvore, entao o
     *     programa copia para ele o menor nome da subarvore da direita (o
     *     sucessor em ordem, ou seja, o proximo nome em ordem alfabetica) e
     *     remove esse sucessor do lugar onde ele estava. Como o sucessor e o
     *     menor da subarvore da direita, ele nunca tem filho a esquerda, entao
     *     a remocao dele sempre cai em um dos dois primeiros casos.
     *
     * @return true se o aluno existia e foi removido, false caso contrario
     */
    public boolean remover(String nome) {
        if (buscar(nome) == null) {
            return false;
        }
        raiz = remover(raiz, padronizar(nome));
        return true;
    }

    private No remover(No no, String chave) {
        if (no == null) {
            return null;
        }

        int comparacao = chave.compareTo(no.chave);
        if (comparacao < 0) {
            no.esquerda = remover(no.esquerda, chave);
        } else if (comparacao > 0) {
            no.direita = remover(no.direita, chave);
        } else {
            if (no.esquerda == null) {
                return no.direita;
            }
            if (no.direita == null) {
                return no.esquerda;
            }
            No sucessor = menorNo(no.direita);
            no.nome = sucessor.nome;
            no.chave = sucessor.chave;
            no.direita = remover(no.direita, sucessor.chave);
        }
        return no;
    }

    private No menorNo(No no) {
        while (no.esquerda != null) {
            no = no.esquerda;
        }
        return no;
    }

    /**
     * Percurso em ordem: esquerda, raiz, direita.
     * A lista sai em ordem alfabetica sem nenhuma ordenacao posterior.
     */
    public List<String> listarEmOrdem() {
        List<String> nomes = new ArrayList<>();
        emOrdem(raiz, nomes);
        return nomes;
    }

    private void emOrdem(No no, List<String> nomes) {
        if (no == null) {
            return;
        }
        emOrdem(no.esquerda, nomes);
        nomes.add(no.nome);
        emOrdem(no.direita, nomes);
    }

    public int tamanho() {
        return tamanho(raiz);
    }

    private int tamanho(No no) {
        if (no == null) {
            return 0;
        }
        return 1 + tamanho(no.esquerda) + tamanho(no.direita);
    }

    public int altura() {
        return altura(raiz);
    }

    private int altura(No no) {
        if (no == null) {
            return 0;
        }
        return 1 + Math.max(altura(no.esquerda), altura(no.direita));
    }

    public boolean estaVazia() {
        return raiz == null;
    }

    /**
     * Desenho da arvore em texto, mostrado na tela.
     * A arvore aparece deitada: a raiz na primeira coluna, a subarvore da direita
     * acima e a da esquerda abaixo.
     */
    public String desenhar() {
        if (raiz == null) {
            return "(arvore vazia)";
        }

        StringBuilder desenho = new StringBuilder();
        desenhar(raiz.direita, "    ", desenho);
        desenho.append(raiz.nome).append("\n");
        desenhar(raiz.esquerda, "    ", desenho);
        return desenho.toString();
    }

    private void desenhar(No no, String espacos, StringBuilder desenho) {
        if (no == null) {
            return;
        }
        desenhar(no.direita, espacos + "    ", desenho);
        desenho.append(espacos).append("+-- ").append(no.nome).append("\n");
        desenhar(no.esquerda, espacos + "    ", desenho);
    }

    /**
     * Padroniza o nome antes de comparar: remove espacos repetidos, tira os
     * acentos com o Normalizer e passa para minusculas. Assim "José da Silva",
     * "JOSE DA SILVA" e "  josé  da silva " sao o mesmo aluno.
     */
    public static String padronizar(String texto) {
        if (texto == null) {
            return "";
        }

        String limpo = texto.trim().replaceAll("\\s+", " ");
        String semAcento = Normalizer.normalize(limpo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase();
    }
}
