package unicsul.testes;

import unicsul.arvore.ArvoreBinariaBusca;
import unicsul.campus.Campus;
import unicsul.campus.SistemaDeCampi;

import java.util.List;

/**
 * Testes da arvore binaria e das regras do sistema. Cada verificacao mostra OK ou
 * FALHA na tela. Nao usa JUnit para o projeto continuar sem bibliotecas de fora.
 *
 * Para executar: java -cp target/classes unicsul.testes.TesteArvore
 */
public class TesteArvore {

    private static int testesOk = 0;
    private static int testesFalha = 0;

    public static void main(String[] args) {
        System.out.println("--- Testes da arvore binaria de busca ---");
        testarInsercao();
        testarPercursoEmOrdem();
        testarBusca();
        testarNomeRepetido();
        testarAltura();
        testarRemocaoDeFolha();
        testarRemocaoDeNoComUmFilho();
        testarRemocaoDeNoComDoisFilhos();
        testarRemocaoDeNomeInexistente();

        System.out.println();
        System.out.println("--- Testes das regras do sistema ---");
        testarCadastro();
        testarAlunoEmDoisCampi();
        testarLocalizarAluno();
        testarListagemDoCampus();
        testarValidacaoDoNome();
        testarExclusaoDeAluno();
        testarExclusaoDeAlunoInexistente();
        testarLimiteDeAlunosPorCampus();

        System.out.println();
        System.out.println("Testes OK: " + testesOk + "   Testes com falha: " + testesFalha);
    }

    private static void testarInsercao() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();

        verificar("arvore nova esta vazia", arvore.estaVazia());

        arvore.inserir("Mariana Lopes");
        arvore.inserir("Ana Souza");
        arvore.inserir("Zuleica Prado");

        verificar("arvore com 3 alunos", arvore.tamanho() == 3);
        verificar("arvore nao esta mais vazia", !arvore.estaVazia());
    }

    private static void testarPercursoEmOrdem() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();
        arvore.inserir("Mariana Lopes");
        arvore.inserir("Ana Souza");
        arvore.inserir("Zuleica Prado");
        arvore.inserir("Carlos Dias");

        List<String> lista = arvore.listarEmOrdem();
        List<String> esperado = List.of("Ana Souza", "Carlos Dias", "Mariana Lopes", "Zuleica Prado");

        verificar("percurso em ordem devolve os nomes em ordem alfabetica",
                lista.equals(esperado));
    }

    private static void testarBusca() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();
        arvore.inserir("Vinícius Prado");
        arvore.inserir("Ana Souza");

        verificar("encontra um aluno cadastrado",
                arvore.buscar("Ana Souza") != null);
        verificar("encontra mesmo digitando sem acento e em maiusculas",
                arvore.buscar("VINICIUS PRADO") != null);
        verificar("devolve o nome do jeito que foi cadastrado",
                "Vinícius Prado".equals(arvore.buscar("vinicius prado")));
        verificar("nao encontra aluno que nao existe",
                arvore.buscar("Bruno Lima") == null);
    }

    private static void testarNomeRepetido() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();

        verificar("primeira insercao e aceita", arvore.inserir("Ana Souza"));
        verificar("insercao repetida e recusada", !arvore.inserir("ANA   SOUZA"));
        verificar("arvore continua com 1 aluno", arvore.tamanho() == 1);
    }

    private static void testarAltura() {
        // Nomes inseridos em ordem alfabetica: pior caso, a arvore vira uma lista.
        ArvoreBinariaBusca piorCaso = new ArvoreBinariaBusca();
        piorCaso.inserir("Ana");
        piorCaso.inserir("Bruno");
        piorCaso.inserir("Carlos");
        verificar("pior caso: altura igual ao numero de alunos", piorCaso.altura() == 3);

        // Mesmo conteudo, inserido em outra ordem: arvore equilibrada.
        ArvoreBinariaBusca equilibrada = new ArvoreBinariaBusca();
        equilibrada.inserir("Bruno");
        equilibrada.inserir("Ana");
        equilibrada.inserir("Carlos");
        verificar("caso equilibrado: altura menor", equilibrada.altura() == 2);
    }

    private static void testarRemocaoDeFolha() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();
        arvore.inserir("Mariana Lopes");
        arvore.inserir("Ana Souza");
        arvore.inserir("Zuleica Prado");

        verificar("remocao de folha e aceita", arvore.remover("ana souza"));
        verificar("folha removida some da listagem",
                List.of("Mariana Lopes", "Zuleica Prado").equals(arvore.listarEmOrdem()));
        verificar("tamanho diminui apos remocao", arvore.tamanho() == 2);
    }

    private static void testarRemocaoDeNoComUmFilho() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();
        arvore.inserir("Mariana Lopes");
        arvore.inserir("Ana Souza");
        arvore.inserir("Carlos Dias"); // filho direito de Ana Souza

        verificar("remocao de no com um filho e aceita", arvore.remover("Ana Souza"));
        verificar("o filho unico assume o lugar do no removido",
                List.of("Carlos Dias", "Mariana Lopes").equals(arvore.listarEmOrdem()));
    }

    private static void testarRemocaoDeNoComDoisFilhos() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();
        arvore.inserir("Mariana Lopes");
        arvore.inserir("Ana Souza");
        arvore.inserir("Zuleica Prado");
        arvore.inserir("Carlos Dias");    // filho direito de Ana Souza
        arvore.inserir("Otávio Ramires"); // filho esquerdo de Zuleica Prado

        // A raiz "Mariana Lopes" tem dois filhos: deve ser substituida pelo
        // sucessor em ordem (o menor nome maior que ela), que e "Otávio Ramires".
        verificar("remocao da raiz com dois filhos e aceita", arvore.remover("Mariana Lopes"));
        verificar("a arvore continua ordenada apos a substituicao pelo sucessor",
                List.of("Ana Souza", "Carlos Dias", "Otávio Ramires", "Zuleica Prado")
                        .equals(arvore.listarEmOrdem()));
        verificar("tamanho reflete a remocao", arvore.tamanho() == 4);
    }

    private static void testarRemocaoDeNomeInexistente() {
        ArvoreBinariaBusca arvore = new ArvoreBinariaBusca();
        arvore.inserir("Ana Souza");

        verificar("remocao de nome inexistente e recusada", !arvore.remover("Bruno Lima"));
        verificar("arvore permanece intacta", arvore.tamanho() == 1);
    }

    private static void testarCadastro() {
        SistemaDeCampi sistema = new SistemaDeCampi();

        String erro = sistema.cadastrar("Ana Souza", Campus.PAULISTA);

        verificar("cadastro valido nao devolve erro", erro == null);
        verificar("campus Paulista ficou com 1 aluno",
                sistema.totalDoCampus(Campus.PAULISTA) == 1);
        verificar("total geral do sistema e 1", sistema.totalDeAlunos() == 1);
    }

    private static void testarAlunoEmDoisCampi() {
        SistemaDeCampi sistema = new SistemaDeCampi();
        sistema.cadastrar("Ana Souza", Campus.PAULISTA);

        String erro = sistema.cadastrar("ANA SOUZA", Campus.GUARULHOS);

        verificar("aluno repetido em outro campus e recusado", erro != null);
        verificar("a mensagem diz em qual campus ele ja esta",
                erro != null && erro.contains("Paulista"));
        verificar("o aluno nao foi cadastrado em Guarulhos",
                sistema.totalDoCampus(Campus.GUARULHOS) == 0);
    }

    private static void testarLocalizarAluno() {
        SistemaDeCampi sistema = new SistemaDeCampi();
        sistema.cadastrar("Ana Souza", Campus.PAULISTA);
        sistema.cadastrar("Bruno Lima", Campus.VILLA_LOBOS);

        verificar("localiza o aluno no campus certo",
                sistema.localizarCampus("bruno lima") == Campus.VILLA_LOBOS);
        verificar("aluno que nao existe devolve null",
                sistema.localizarCampus("Carlos Dias") == null);
    }

    private static void testarListagemDoCampus() {
        SistemaDeCampi sistema = new SistemaDeCampi();
        sistema.cadastrar("Mariana Lopes", Campus.SAO_MIGUEL);
        sistema.cadastrar("Ana Souza", Campus.SAO_MIGUEL);
        sistema.cadastrar("Bruno Lima", Campus.LIBERDADE);

        List<String> lista = sistema.listarAlunos(Campus.SAO_MIGUEL);

        verificar("listagem do campus vem em ordem alfabetica",
                lista.equals(List.of("Ana Souza", "Mariana Lopes")));
        verificar("cada campus tem a sua propria arvore",
                sistema.listarAlunos(Campus.LIBERDADE).equals(List.of("Bruno Lima")));
    }

    private static void testarValidacaoDoNome() {
        SistemaDeCampi sistema = new SistemaDeCampi();

        verificar("nome vazio e recusado", sistema.validarNome("   ") != null);
        verificar("nome com uma letra so e recusado", sistema.validarNome("A") != null);
        verificar("nome com numero e recusado", sistema.validarNome("Aluno 123") != null);
        verificar("nome normal e aceito", sistema.validarNome("Ana Souza") == null);
        verificar("nome com acento e aceito", sistema.validarNome("José D'Ávila") == null);
    }

    private static void testarExclusaoDeAluno() {
        SistemaDeCampi sistema = new SistemaDeCampi();
        sistema.cadastrar("Ana Souza", Campus.PAULISTA);
        sistema.cadastrar("Bruno Lima", Campus.VILLA_LOBOS);

        Campus campusExcluido = sistema.excluir("ana souza");

        verificar("exclusao informa de onde o aluno saiu", campusExcluido == Campus.PAULISTA);
        verificar("campus fica sem o aluno excluido", sistema.totalDoCampus(Campus.PAULISTA) == 0);
        verificar("aluno excluido nao e mais localizavel",
                sistema.localizarCampus("Ana Souza") == null);
        verificar("outro campus nao e afetado", sistema.totalDoCampus(Campus.VILLA_LOBOS) == 1);
        verificar("total geral reflete a exclusao", sistema.totalDeAlunos() == 1);
    }

    private static void testarExclusaoDeAlunoInexistente() {
        SistemaDeCampi sistema = new SistemaDeCampi();

        verificar("exclusao de aluno inexistente devolve null",
                sistema.excluir("Fulano de Tal") == null);
    }

    private static void testarLimiteDeAlunosPorCampus() {
        SistemaDeCampi sistema = new SistemaDeCampi();
        String erro = null;
        int cadastrados = 0;

        // Nomes em ordem alfabetica: pior caso, a arvore vira uma lista encadeada.
        for (int i = 0; i < 1100 && erro == null; i++) {
            String nome = "Aluno " + (char) ('a' + i / 676) + (char) ('a' + i / 26 % 26) + (char) ('a' + i % 26);
            erro = sistema.cadastrar(nome, Campus.PAULISTA);
            if (erro == null) {
                cadastrados++;
            }
        }

        verificar("campus recusa cadastro acima do limite", erro != null && cadastrados == 1000);
        verificar("outro campus continua aceitando", sistema.cadastrar("Ana Souza", Campus.LIBERDADE) == null);
    }

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            testesOk++;
            System.out.println("  OK    - " + descricao);
        } else {
            testesFalha++;
            System.out.println("  FALHA - " + descricao);
        }
    }
}
