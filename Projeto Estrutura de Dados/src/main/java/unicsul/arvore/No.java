package unicsul.arvore;

/**
 * No da arvore binaria de busca.
 *
 * O nome e guardado duas vezes: em "nome" fica o texto como o usuario digitou,
 * usado na exibicao, e em "chave" fica a versao sem acento e em minusculas, usada
 * em todas as comparacoes.
 *
 * Os atributos tem visibilidade de pacote porque a classe ArvoreBinariaBusca
 * precisa alterar os ponteiros esquerda e direita durante a insercao e a remocao.
 * O nome e a chave nao sao final porque a remocao de um no com dois filhos precisa
 * substituir os dados do no sem desmontar a arvore inteira.
 */
public class No {

    String nome;
    String chave;
    No esquerda;
    No direita;

    No(String nome) {
        this.nome = nome;
        this.chave = ArvoreBinariaBusca.padronizar(nome);
        this.esquerda = null;
        this.direita = null;
    }
}
