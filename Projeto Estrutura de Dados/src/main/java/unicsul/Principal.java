package unicsul;

import unicsul.campus.Campus;
import unicsul.campus.SistemaDeCampi;
import unicsul.web.ServidorWeb;

import java.awt.Desktop;
import java.net.URI;

public class Principal {

    private static final int PORTA_INICIAL = 8080;
    private static final int PORTAS_TESTADAS = 10;

    public static void main(String[] args) {

        System.out.println();
        System.out.println("==============================================");
        System.out.println("     CADASTRO DE ALUNOS - UNICSUL");
        System.out.println("==============================================");

        SistemaDeCampi sistema = new SistemaDeCampi();

        cadastrarAlunosDeExemplo(sistema);
        mostrarResumo(sistema);
        ServidorWeb servidor = ligarServidor(sistema);

        if (servidor == null) {

            System.out.println();
            System.out.println(
                    "Nao foi possivel iniciar o servidor."
            );

            System.out.println(
                    "As portas de "
                            + PORTA_INICIAL
                            + " ate "
                            + (PORTA_INICIAL
                            + PORTAS_TESTADAS
                            - 1)
                            + " estao ocupadas."
            );

            return;
        }

        servidor.iniciar();

        mostrarMensagemInicial(
                servidor.getEndereco()
        );

        if (!temParametro(
                args,
                "--sem-navegador"
        )) {

            abrirNavegador(
                    servidor.getEndereco()
            );
        }
    }

    /**
     * Tenta iniciar na porta 8080.
     * Se estiver ocupada, tenta 8081, 8082...
     */
    private static ServidorWeb ligarServidor(
            SistemaDeCampi sistema) {

        for (
                int i = 0;
                i < PORTAS_TESTADAS;
                i++
        ) {

            int porta =
                    PORTA_INICIAL + i;

            try {

                System.out.println(
                        "Tentando iniciar servidor na porta "
                                + porta
                                + "..."
                );

                return new ServidorWeb(
                        sistema,
                        porta
                );

            } catch (Exception e) {

                System.out.println(
                        "A porta "
                                + porta
                                + " esta ocupada."
                );
            }
        }

        return null;
    }

    private static void mostrarMensagemInicial(
            String endereco) {

        System.out.println();
        System.out.println(
                "===================================================="
        );
        System.out.println(
                " Cadastro de Alunos por Campus - Unicsul"
        );
        System.out.println(
                " Estrutura: Arvore Binaria de Busca"
        );
        System.out.println(
                "===================================================="
        );
        System.out.println(
                " Endereco da tela: "
                        + endereco
        );
        System.out.println(
                " Para encerrar: Ctrl+C"
        );
        System.out.println(
                "===================================================="
        );
        System.out.println();
    }

    private static void abrirNavegador(
            String endereco) {

        try {

            if (Desktop.isDesktopSupported()) {

                Desktop.getDesktop().browse(
                        new URI(endereco)
                );

            } else {

                System.out.println(
                        "Abra o navegador e acesse:"
                );

                System.out.println(
                        endereco
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Nao foi possivel abrir o navegador automaticamente."
            );

            System.out.println(
                    "Acesse:"
            );

            System.out.println(
                    endereco
            );
        }
    }

    private static boolean temParametro(
            String[] args,
            String procurado) {

        if (args == null) {
            return false;
        }

        for (String argumento : args) {

            if (argumento != null
                    && argumento.equalsIgnoreCase(
                    procurado
            )) {

                return true;
            }
        }

        return false;
    }

    /** Cadastra 10 alunos de exemplo em cada campus. */
    private static void cadastrarAlunosDeExemplo(
            SistemaDeCampi sistema) {

        String[][] exemplos = {

                {
                        "analia-franco",

                        "Mariana Lopes",
                        "Gustavo Prado",
                        "Rafael Nunes",
                        "Beatriz Coelho",
                        "Otávio Ramires",
                        "Camila Dorneles",
                        "Felipe Andrade",
                        "Débora Nascimento",
                        "Igor Barbosa",
                        "Priscila Lemos"
                },

                {
                        "guarulhos",

                        "Lucas Ferreira",
                        "Daniela Rocha",
                        "Thiago Menezes",
                        "Amanda Vieira",
                        "Renato Salgado",
                        "Vitor Cavalcante",
                        "Simone Farias",
                        "Bruno Teixeira",
                        "Cristina Monteiro",
                        "Fábio Carvalho"
                },

                {
                        "liberdade",

                        "Yuri Tanaka",
                        "Helena Kimura",
                        "Paulo Nakamura",
                        "Alice Sato",
                        "Kenji Watanabe",
                        "Fernanda Kato",
                        "Rogério Matsuda",
                        "Vanessa Ishikawa",
                        "Gabriel Okamoto",
                        "Sônia Yamamoto"
                },

                {
                        "paulista",

                        "Marcos Antunes",
                        "Fernanda Britto",
                        "Vinícius Prado",
                        "Isabela Cardoso",
                        "Diego Marchetti",
                        "Larissa Fontes",
                        "Roberto Aguiar",
                        "Adriana Ferraz",
                        "Henrique Vidal",
                        "Natália Siqueira"
                },

                {
                        "sao-miguel",

                        "Juliana Peixoto",
                        "André Bastos",
                        "Tatiane Moraes",
                        "Wellington Cruz",
                        "Ricardo Fontoura",
                        "Camila Xavier",
                        "Ederson Pires",
                        "Luana Rezende",
                        "Anderson Brito",
                        "Débora Cunha"
                },

                {
                        "santo-amaro",

                        "Patrícia Duarte",
                        "Eduardo Galvão",
                        "Sabrina Teles",
                        "Márcio Bezerra",
                        "Aline Quaresma",
                        "Cláudia Ramos",
                        "Wagner Correia",
                        "Juliano Prates",
                        "Tânia Espósito",
                        "Bianca Novais"
                },

                {
                        "villa-lobos",

                        "Leandro Pacheco",
                        "Bruna Vasconcelos",
                        "Rodrigo Sampaio",
                        "Elisa Moretti",
                        "Caio Vergara",
                        "Marina Uchoa",
                        "Fabiano Dutra",
                        "Yasmin Rocha",
                        "Otacílio Bandeira",
                        "Karen Salles"
                }
        };

        int quantidade =
                0;

        System.out.println();
        System.out.println(
                "Cadastrando alunos..."
        );

        for (String[] linha : exemplos) {

            String codigoCampus =
                    linha[0];

            Campus campus =
                    Campus.procurarPorCodigo(
                            codigoCampus
                    );

            if (campus == null) {

                System.out.println(
                        "ERRO: Campus nao encontrado: "
                                + codigoCampus
                );

                continue;
            }

            System.out.println();
            System.out.println(
                    "Campus: "
                            + campus.getNome()
            );

            for (
                    int i = 1;
                    i < linha.length;
                    i++
            ) {

                String nome =
                        linha[i];

                String erro =
                        sistema.cadastrar(
                                nome,
                                campus
                        );

                if (erro == null) {

                    quantidade++;

                    System.out.println(
                            "  + "
                                    + nome
                    );

                } else {

                    System.out.println(
                            "  ERRO ao cadastrar "
                                    + nome
                                    + ": "
                                    + erro
                    );
                }
            }
        }

        System.out.println();
        System.out.println(
                "=============================================="
        );
        System.out.println(
                "TOTAL DE ALUNOS CADASTRADOS: "
                        + quantidade
        );
        System.out.println(
                "=============================================="
        );
    }

    private static void mostrarResumo(
            SistemaDeCampi sistema) {

        System.out.println();
        System.out.println(
                "========== RESUMO DOS CAMPI =========="
        );

        for (Campus campus :
                sistema.getCampi()) {

            System.out.println(
                    campus.getNome()
                            + " -> "
                            + sistema.totalDoCampus(
                            campus
                    )
                            + " alunos"
            );
        }

        System.out.println(
                "======================================"
        );
        System.out.println();
    }
}
