package unicsul.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import unicsul.campus.Campus;
import unicsul.campus.SistemaDeCampi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP local que entrega a interface web e a API do sistema.
 *
 * Escuta somente em 127.0.0.1. Como as rotas que alteram dados (cadastrar e excluir)
 * nao exigem login, cada pedido passa por {@link #proteger}, que rejeita Host e
 * Origin de fora do proprio servidor. Isso impede que outro site aberto no navegador
 * envie pedidos ao sistema (CSRF) ou o alcance por DNS rebinding.
 */
public class ServidorWeb {

    private static final String POLITICA_DE_CONTEUDO =
            "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
                    + "object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'";

    private final SistemaDeCampi sistema;
    private final HttpServer servidor;
    private final int porta;
    private final File pastaWeb;
    private final Set<String> hostsPermitidos;
    private final Set<String> origensPermitidas;

    public ServidorWeb(SistemaDeCampi sistema, int porta) throws IOException {

        this.sistema = sistema;
        this.porta = porta;
        this.pastaWeb = encontrarPastaWeb();
        this.hostsPermitidos = Set.of("localhost:" + porta, "127.0.0.1:" + porta);
        this.origensPermitidas = Set.of(
                "http://localhost:" + porta,
                "http://127.0.0.1:" + porta
        );

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", porta),
                0
        );

        // Varias threads para um cliente lento nao travar os demais; o estado e protegido por synchronized.
        servidor.setExecutor(Executors.newFixedThreadPool(4));

        servidor.createContext("/", proteger(this::entregarArquivo));

        servidor.createContext("/api/campi", proteger(this::listarCampi));
        servidor.createContext("/api/cadastrar", proteger(this::cadastrarAluno));
        servidor.createContext("/api/buscar", proteger(this::localizarAluno));
        servidor.createContext("/api/excluir", proteger(this::excluirAluno));
        servidor.createContext("/api/listar", proteger(this::listarAlunos));
    }

    public void iniciar() {

        if (pastaWeb == null) {
            System.out.println("AVISO: pasta webapp nao encontrada; a interface nao sera exibida.");
        }

        servidor.start();

        System.out.println();
        System.out.println("==============================================");
        System.out.println("          SERVIDOR WEB INICIADO");
        System.out.println("==============================================");
        System.out.println("Endereco: " + getEndereco());
        System.out.println("==============================================");
        System.out.println();
    }

    public String getEndereco() {
        return "http://localhost:" + porta;
    }

    /** Aplica a checagem de Host/Origin e trata qualquer erro inesperado sem vazar detalhes ao cliente. */
    private HttpHandler proteger(HttpHandler destino) {

        return troca -> {
            try {
                String host = troca.getRequestHeaders().getFirst("Host");
                String origem = troca.getRequestHeaders().getFirst("Origin");

                if (host == null || !hostsPermitidos.contains(host.toLowerCase())
                        || (origem != null && !origensPermitidas.contains(origem.toLowerCase()))) {

                    responderTexto(troca, 403, "Acesso negado.");
                    return;
                }

                destino.handle(troca);

            } catch (Exception e) {
                responderErroInterno(troca, e);
            }
        };
    }

    private void entregarArquivo(HttpExchange troca) throws IOException {

        if (!troca.getRequestMethod().equalsIgnoreCase("GET")) {
            responderTexto(troca, 405, "Este endereco aceita apenas GET.");
            return;
        }

        if (pastaWeb == null) {
            responderTexto(troca, 500, "Interface web nao encontrada.");
            return;
        }

        String caminho = troca.getRequestURI().getPath();

        if (caminho == null || caminho.isEmpty() || caminho.equals("/")) {
            caminho = "/index.html";
        }

        // O caminho vem decodificado; barra invertida e byte nulo nunca fazem parte de um endereco valido.
        if (caminho.contains("..") || caminho.contains("\\") || caminho.contains("\0")) {
            responderTexto(troca, 400, "Endereco invalido.");
            return;
        }

        File arquivo = new File(pastaWeb, caminho.substring(1)).getCanonicalFile();

        // Defesa contra path traversal e symlinks que apontem para fora da webapp.
        if (!arquivo.toPath().startsWith(pastaWeb.toPath())) {
            responderTexto(troca, 403, "Acesso negado.");
            return;
        }

        if (!arquivo.isFile()) {
            responderTexto(troca, 404, "Arquivo nao encontrado.");
            return;
        }

        responder(
                troca,
                200,
                tipoDoArquivo(caminho),
                Files.readAllBytes(arquivo.toPath())
        );
    }

    /**
     * Localiza a pasta "webapp" uma unica vez, ao criar o servidor. A busca sobe
     * pelos diretorios pai e desce pelas subpastas do diretorio atual, mas so aceita
     * uma pasta chamada "webapp" que contenha index.html. Nunca serve uma pasta
     * qualquer que apenas tenha um index.html, para nao expor arquivos de outros projetos.
     */
    private static File encontrarPastaWeb() {

        File atual = new File(System.getProperty("user.dir"));

        for (int nivel = 0; nivel < 10 && atual != null; nivel++) {
            File candidata = procurarWebapp(atual, 8);
            if (candidata != null) {
                return candidata;
            }
            atual = atual.getParentFile();
        }

        return null;
    }

    private static File procurarWebapp(File pasta, int profundidade) {

        if (profundidade < 0 || !pasta.isDirectory()) {
            return null;
        }

        if (pasta.getName().equals("webapp")
                && new File(pasta, "index.html").isFile()) {
            try {
                return pasta.getCanonicalFile();
            } catch (IOException e) {
                return null;
            }
        }

        File[] filhos = pasta.listFiles(File::isDirectory);
        if (filhos == null) {
            return null;
        }

        for (File filho : filhos) {
            String nome = filho.getName();
            if (nome.startsWith(".") || nome.equals("node_modules")) {
                continue;
            }

            File encontrada = procurarWebapp(filho, profundidade - 1);
            if (encontrada != null) {
                return encontrada;
            }
        }

        return null;
    }

    private void listarCampi(
            HttpExchange troca) throws IOException {

        try {

            if (!troca.getRequestMethod()
                    .equalsIgnoreCase("GET")) {

                responderJson(
                        troca,
                        405,
                        respostaDeErro(
                                "Este endereco aceita apenas GET."
                        )
                );

                return;
            }

            StringBuilder json =
                    new StringBuilder("[");

            List<Campus> campi =
                    sistema.getCampi();

            for (int i = 0;
                 i < campi.size();
                 i++) {

                Campus campus = campi.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"codigo\":\"")
                        .append(
                                escapar(
                                        campus.getCodigo()
                                )
                        )
                        .append("\",")

                        .append("\"nome\":\"")
                        .append(
                                escapar(
                                        campus.getNome()
                                )
                        )
                        .append("\",")

                        .append("\"total\":")
                        .append(
                                sistema.totalDoCampus(
                                        campus
                                )
                        )

                        .append("}");
            }

            json.append("]");

            responderJson(
                    troca,
                    200,
                    json.toString()
            );

        } catch (Exception e) {

            responderErroInterno(
                    troca,
                    e
            );
        }
    }

    private void cadastrarAluno(
            HttpExchange troca) throws IOException {

        try {

            if (!troca.getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                responderJson(
                        troca,
                        405,
                        respostaDeErro(
                                "Este endereco aceita apenas POST."
                        )
                );

                return;
            }

            Map<String, String> parametros =
                    lerParametros(troca);

            String nome =
                    parametros.get("nome");

            String codigo =
                    parametros.get("campus");

            if (nome == null
                    || nome.trim().isEmpty()) {

                responderJson(
                        troca,
                        200,
                        respostaDeErro(
                                "Digite o nome do aluno."
                        )
                );

                return;
            }

            Campus campus =
                    Campus.procurarPorCodigo(
                            codigo
                    );

            if (campus == null) {

                responderJson(
                        troca,
                        200,
                        respostaDeErro(
                                "Selecione um dos 7 campi."
                        )
                );

                return;
            }

            String erro =
                    sistema.cadastrar(
                            nome,
                            campus
                    );

            if (erro != null) {

                responderJson(
                        troca,
                        200,
                        respostaDeErro(erro)
                );

                return;
            }

            String mensagem =
                    "Aluno "
                            + nome.trim()
                            + " cadastrado no campus "
                            + campus.getNome()
                            + ".";

            String json =
                    "{"
                            + "\"ok\":true,"
                            + "\"mensagem\":\""
                            + escapar(mensagem)
                            + "\","
                            + "\"total\":"
                            + sistema.totalDoCampus(campus)
                            + ","
                            + "\"altura\":"
                            + sistema.alturaDaArvore(campus)
                            + "}";

            responderJson(
                    troca,
                    200,
                    json
            );

        } catch (Exception e) {

            responderErroInterno(
                    troca,
                    e
            );
        }
    }

    private void localizarAluno(
            HttpExchange troca) throws IOException {

        try {

            if (!troca.getRequestMethod()
                    .equalsIgnoreCase("GET")) {

                responderJson(
                        troca,
                        405,
                        respostaDeErro(
                                "Este endereco aceita apenas GET."
                        )
                );

                return;
            }

            String nome =
                    lerParametros(troca)
                            .get("nome");

            String erro =
                    sistema.validarNome(nome);

            if (erro != null) {

                responderJson(
                        troca,
                        200,
                        respostaDeErro(erro)
                );

                return;
            }

            Campus campus =
                    sistema.localizarCampus(nome);

            if (campus == null) {

                responderJson(
                        troca,
                        200,
                        "{\"encontrado\":false,"
                                + "\"mensagem\":\"Aluno nao localizado.\"}"
                );

                return;
            }

            String nomeCadastrado =
                    sistema.nomeCadastrado(nome);

            String json =
                    "{"
                            + "\"encontrado\":true,"
                            + "\"nome\":\""
                            + escapar(nomeCadastrado)
                            + "\","
                            + "\"campus\":\""
                            + escapar(campus.getNome())
                            + "\""
                            + "}";

            responderJson(
                    troca,
                    200,
                    json
            );

        } catch (Exception e) {

            responderErroInterno(
                    troca,
                    e
            );
        }
    }

    private void excluirAluno(
            HttpExchange troca) throws IOException {

        try {

            if (!troca.getRequestMethod()
                    .equalsIgnoreCase("DELETE")) {

                responderJson(
                        troca,
                        405,
                        respostaDeErro(
                                "Este endereco aceita apenas DELETE."
                        )
                );

                return;
            }

            String nome =
                    lerParametros(troca)
                            .get("nome");

            String erro =
                    sistema.validarNome(nome);

            if (erro != null) {

                responderJson(
                        troca,
                        200,
                        respostaDeErro(erro)
                );

                return;
            }

            Campus campus =
                    sistema.excluir(nome);

            if (campus == null) {

                responderJson(
                        troca,
                        200,
                        respostaDeErro(
                                "Nenhum aluno chamado \""
                                        + nome.trim()
                                        + "\" foi encontrado nos 7 campi."
                        )
                );

                return;
            }

            String mensagem =
                    "Aluno "
                            + nome.trim()
                            + " removido do campus "
                            + campus.getNome()
                            + ".";

            String json =
                    "{"
                            + "\"ok\":true,"
                            + "\"mensagem\":\""
                            + escapar(mensagem)
                            + "\","
                            + "\"total\":"
                            + sistema.totalDoCampus(campus)
                            + ","
                            + "\"altura\":"
                            + sistema.alturaDaArvore(campus)
                            + "}";

            responderJson(
                    troca,
                    200,
                    json
            );

        } catch (Exception e) {

            responderErroInterno(
                    troca,
                    e
            );
        }
    }

    private void listarAlunos(
            HttpExchange troca) throws IOException {

        try {

            if (!troca.getRequestMethod()
                    .equalsIgnoreCase("GET")) {

                responderJson(
                        troca,
                        405,
                        respostaDeErro(
                                "Este endereco aceita apenas GET."
                        )
                );

                return;
            }

            String codigo =
                    lerParametros(troca)
                            .get("campus");

            Campus campus =
                    Campus.procurarPorCodigo(codigo);

            if (campus == null) {

                responderJson(
                        troca,
                        200,
                        respostaDeErro(
                                "Selecione um dos 7 campi."
                        )
                );

                return;
            }

            List<String> alunos =
                    sistema.listarAlunos(campus);

            StringBuilder lista =
                    new StringBuilder("[");

            for (int i = 0;
                 i < alunos.size();
                 i++) {

                if (i > 0) {
                    lista.append(",");
                }

                lista.append("\"")
                        .append(
                                escapar(
                                        alunos.get(i)
                                )
                        )
                        .append("\"");
            }

            lista.append("]");

            String arvore =
                    sistema.desenharArvore(campus);

            String json =
                    "{"
                            + "\"ok\":true,"
                            + "\"campus\":\""
                            + escapar(campus.getNome())
                            + "\","
                            + "\"total\":"
                            + alunos.size()
                            + ","
                            + "\"altura\":"
                            + sistema.alturaDaArvore(campus)
                            + ","
                            + "\"alunos\":"
                            + lista
                            + ","
                            + "\"arvore\":\""
                            + escapar(arvore)
                            + "\""
                            + "}";

            responderJson(
                    troca,
                    200,
                    json
            );

        } catch (Exception e) {

            responderErroInterno(
                    troca,
                    e
            );
        }
    }

    private Map<String, String> lerParametros(
            HttpExchange troca) {

        Map<String, String> parametros =
                new HashMap<>();

        String consulta =
                troca.getRequestURI()
                        .getRawQuery();

        if (consulta == null
                || consulta.isEmpty()) {

            return parametros;
        }

        String[] partes =
                consulta.split("&");

        for (String parte : partes) {

            int igual =
                    parte.indexOf('=');

            if (igual <= 0) {
                continue;
            }

            String chave =
                    URLDecoder.decode(
                            parte.substring(
                                    0,
                                    igual
                            ),
                            StandardCharsets.UTF_8
                    );

            String valor =
                    URLDecoder.decode(
                            parte.substring(
                                    igual + 1
                            ),
                            StandardCharsets.UTF_8
                    );

            parametros.put(
                    chave,
                    valor
            );
        }

        return parametros;
    }

    private String respostaDeErro(
            String mensagem) {

        return "{"
                + "\"ok\":false,"
                + "\"encontrado\":false,"
                + "\"mensagem\":\""
                + escapar(mensagem)
                + "\""
                + "}";
    }

    private String escapar(String texto) {

        if (texto == null) {
            return "";
        }

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void responderJson(
            HttpExchange troca,
            int codigo,
            String json)
            throws IOException {

        responder(
                troca,
                codigo,
                "application/json; charset=utf-8",
                json.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    private void responderTexto(
            HttpExchange troca,
            int codigo,
            String texto)
            throws IOException {

        responder(
                troca,
                codigo,
                "text/plain; charset=utf-8",
                texto.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    private void responder(
            HttpExchange troca,
            int codigo,
            String tipo,
            byte[] conteudo)
            throws IOException {

        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        troca.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        troca.getResponseHeaders().set("X-Frame-Options", "DENY");
        troca.getResponseHeaders().set("Content-Security-Policy", POLITICA_DE_CONTEUDO);

        // Resposta a HEAD nao tem corpo; o tamanho -1 evita erro no HttpServer.
        if (troca.getRequestMethod().equalsIgnoreCase("HEAD")) {
            troca.sendResponseHeaders(codigo, -1);
            troca.close();
            return;
        }

        troca.sendResponseHeaders(
                codigo,
                conteudo.length
        );

        try (OutputStream saida =
                     troca.getResponseBody()) {

            saida.write(conteudo);
        }
    }

    private String tipoDoArquivo(
            String caminho) {

        String extensao =
                caminho.toLowerCase();

        if (extensao.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }

        if (extensao.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }

        if (extensao.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }

        if (extensao.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }

        if (extensao.endsWith(".png")) {
            return "image/png";
        }

        if (extensao.endsWith(".jpg")
                || extensao.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (extensao.endsWith(".gif")) {
            return "image/gif";
        }

        if (extensao.endsWith(".svg")) {
            return "image/svg+xml";
        }

        if (extensao.endsWith(".ico")) {
            return "image/x-icon";
        }

        return "application/octet-stream";
    }

    private void responderErroInterno(
            HttpExchange troca,
            Exception e)
            throws IOException {

        System.out.println();
        System.out.println("ERRO NO SERVIDOR:");
        e.printStackTrace();

        try {

            responderJson(
                    troca,
                    500,
                    respostaDeErro(
                            "Erro interno no servidor."
                    )
            );

        } catch (Exception ignorado) {
            // conexão pode já ter sido encerrada
        }
    }
}
