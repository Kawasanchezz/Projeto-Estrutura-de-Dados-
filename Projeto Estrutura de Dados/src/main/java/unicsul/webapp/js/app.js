let campi = [];

// Sem handlers inline no HTML: a Content-Security-Policy do servidor so permite scripts de app.js.
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-tela]").forEach(function (botao) {
        botao.addEventListener("click", function () {
            mostrarTela(botao.dataset.tela, botao);
        });
    });

    document.querySelectorAll("[data-limpar]").forEach(function (botao) {
        botao.addEventListener("click", function () {
            limparCampo(botao.dataset.limpar);
        });
    });

    document.getElementById("formCadastro").addEventListener("submit", cadastrarAluno);
    document.getElementById("formBusca").addEventListener("submit", localizarAluno);
    document.getElementById("formListagem").addEventListener("submit", listarAlunos);
    document.getElementById("formExclusao").addEventListener("submit", excluirAluno);

    carregarCampi();
});

/*
 * Chama a API e sempre devolve um objeto. Falhas de rede ou resposta que nao
 * seja JSON viram { ok: false, encontrado: false, mensagem } para as telas
 * tratarem como qualquer outro erro, sem promise rejeitada.
 */
async function chamarApi(endereco, opcoes) {
    try {
        const resposta = await fetch(endereco, opcoes);
        return await resposta.json();
    } catch (erro) {
        return {
            ok: false,
            encontrado: false,
            mensagem: "Nao foi possivel falar com o programa em Java. "
                + "Verifique se a janela dele continua aberta."
        };
    }
}

function mostrarTela(nome, botao) {
    const telas = document.getElementsByClassName("tela");
    for (let i = 0; i < telas.length; i++) {
        telas[i].classList.remove("ativa");
    }
    document.getElementById("tela-" + nome).classList.add("ativa");

    const botoes = document.getElementsByClassName("botao-menu");
    for (let i = 0; i < botoes.length; i++) {
        botoes[i].classList.remove("ativo");
    }
    botao.classList.add("ativo");
}

async function carregarCampi() {
    const dados = await chamarApi("/api/campi");

    if (!Array.isArray(dados)) {
        mostrarMensagem("msgCadastro", "erro", dados.mensagem);
        return;
    }

    campi = dados;
    preencherCampi("campusCadastro", false);
    preencherCampi("campusListagem", true);
    atualizarTotalGeral();
}

function preencherCampi(idDoSelect, mostrarTotal) {
    const selecao = document.getElementById(idDoSelect);
    const escolhidoAntes = selecao.value;

    selecao.innerHTML = "";

    if (!mostrarTotal) {
        selecao.innerHTML = "<option value=''>Escolha o campus</option>";
    }

    for (let i = 0; i < campi.length; i++) {
        const opcao = document.createElement("option");
        opcao.value = campi[i].codigo;
        if (mostrarTotal) {
            opcao.textContent = campi[i].nome + " (" + campi[i].total + " alunos)";
        } else {
            opcao.textContent = campi[i].nome;
        }
        selecao.appendChild(opcao);
    }

    if (escolhidoAntes !== "") {
        selecao.value = escolhidoAntes;
    }
}

function atualizarTotalGeral() {
    let total = 0;
    for (let i = 0; i < campi.length; i++) {
        total = total + campi[i].total;
    }
    document.getElementById("totalGeral").textContent = total;
}

function limparCampo(idDoCampo) {
    const campo = document.getElementById(idDoCampo);
    campo.value = "";
    campo.focus();
}

async function cadastrarAluno(evento) {
    evento.preventDefault();

    const nome = document.getElementById("nomeCadastro").value;
    const campus = document.getElementById("campusCadastro").value;

    if (nome.trim() === "") {
        mostrarMensagem("msgCadastro", "erro", "Digite o nome do aluno.");
        return;
    }
    if (campus === "") {
        mostrarMensagem("msgCadastro", "erro", "Escolha o campus.");
        return;
    }

    const endereco = "/api/cadastrar?nome=" + encodeURIComponent(nome)
                   + "&campus=" + encodeURIComponent(campus);

    const dados = await chamarApi(endereco, { method: "POST" });

    if (dados.ok) {
        mostrarMensagem("msgCadastro", "sucesso", dados.mensagem
            + " A arvore desse campus agora tem " + dados.total
            + " aluno(s) e altura " + dados.altura + ".",
            "Aluno cadastrado!");
        document.getElementById("nomeCadastro").value = "";
        carregarCampi();
    } else {
        mostrarMensagem("msgCadastro", "erro", dados.mensagem);
    }
}

async function localizarAluno(evento) {
    evento.preventDefault();

    const nome = document.getElementById("nomeBusca").value;
    if (nome.trim() === "") {
        mostrarMensagem("msgBusca", "erro", "Digite o nome do aluno.");
        return;
    }

    const dados = await chamarApi("/api/buscar?nome=" + encodeURIComponent(nome));

    if (dados.encontrado) {
        mostrarMensagem("msgBusca", "sucesso",
            "Aluno encontrado: " + dados.nome + " - campus " + dados.campus + ".",
            "Aluno localizado!");
    } else {
        mostrarMensagem("msgBusca", "aviso", dados.mensagem);
    }
}

async function listarAlunos(evento) {
    evento.preventDefault();

    const campus = document.getElementById("campusListagem").value;
    if (campus === "") {
        mostrarMensagem("msgListagem", "erro", "Escolha o campus.");
        return;
    }

    const dados = await chamarApi("/api/listar?campus=" + encodeURIComponent(campus));

    if (!dados.ok) {
        mostrarMensagem("msgListagem", "erro", dados.mensagem);
        return;
    }

    document.getElementById("msgListagem").innerHTML = "";
    document.getElementById("tituloListagem").textContent = "Campus " + dados.campus;
    document.getElementById("totalCampus").textContent = dados.total;
    document.getElementById("alturaCampus").textContent = dados.altura;

    const lista = document.getElementById("listaAlunos");
    lista.innerHTML = "";

    if (dados.total === 0) {
        mostrarMensagem("msgListagem", "aviso", "Esse campus ainda nao tem alunos cadastrados.");
    } else {
        for (let i = 0; i < dados.alunos.length; i++) {
            const item = document.createElement("li");
            item.textContent = dados.alunos[i];
            lista.appendChild(item);
        }
    }

    mostrarArvore(dados.arvore, dados.campus);
    document.getElementById("resultadoListagem").hidden = false;
}

/*
 * O Java devolve a arvore deitada em texto: primeiro a subarvore direita,
 * depois a raiz e por ultimo a esquerda. A quantidade de espacos informa
 * a profundidade. Esta funcao recupera a estrutura exata desses dados.
 */
function lerArvore(texto) {
    if (!texto || texto.trim() === "" || texto.trim() === "(arvore vazia)") {
        return null;
    }

    const linhas = texto.split(/\r?\n/)
        .filter(function (linha) { return linha.trim() !== ""; })
        .map(function (linha) {
            const espacos = linha.match(/^ */)[0].length;
            return {
                profundidade: Math.floor(espacos / 4),
                nome: linha.trim().replace(/^\+--\s*/, "")
            };
        });

    function montar(inicio, fim, profundidade) {
        if (inicio >= fim) {
            return null;
        }

        let indiceDaRaiz = -1;
        for (let i = inicio; i < fim; i++) {
            if (linhas[i].profundidade === profundidade) {
                indiceDaRaiz = i;
                break;
            }
        }

        if (indiceDaRaiz === -1) {
            return null;
        }

        return {
            nome: linhas[indiceDaRaiz].nome,
            direita: montar(inicio, indiceDaRaiz, profundidade + 1),
            esquerda: montar(indiceDaRaiz + 1, fim, profundidade + 1)
        };
    }

    return montar(0, linhas.length, 0);
}

function mostrarArvore(textoDaArvore, nomeDoCampus) {
    const espaco = document.getElementById("espacoArvore");
    const botaoDiminuir = document.getElementById("diminuirArvore");
    const botaoRestaurar = document.getElementById("restaurarArvore");
    const botaoAumentar = document.getElementById("aumentarArvore");
    espaco.innerHTML = "";

    const raiz = lerArvore(textoDaArvore);
    if (raiz === null) {
        const vazio = document.createElement("p");
        vazio.className = "arvore-vazia";
        vazio.textContent = "Este campus ainda nao possui nos na arvore.";
        espaco.appendChild(vazio);
        return;
    }

    const larguraMinimaDoNo = 112;
    const larguraMaximaDoNo = 250;
    const espacoEntreSubarvores = 24;
    // Reserva uma faixa visivel quando existe apenas um filho. Isso impede
    // que um filho unico pareca estar diretamente abaixo do pai.
    const espacoDoFilhoAusente = 116;
    const espacoVertical = 86;
    const alturaDoNo = 44;
    const margem = 32;
    let maiorProfundidade = 0;

    /*
     * Mede cada subarvore separadamente. Dessa forma um ramo pequeno ocupa
     * apenas o espaco de que precisa, sem herdar a largura da arvore inteira.
     */
    function medir(no) {
        if (no === null) {
            return null;
        }

        medir(no.esquerda);
        medir(no.direita);

        no.largura = Math.min(larguraMaximaDoNo,
            Math.max(larguraMinimaDoNo, no.nome.length * 7.4 + 32));

        if (no.esquerda === null && no.direita === null) {
            no.larguraSubarvore = no.largura;
            no.inicioEsquerda = 0;
            no.inicioDireita = 0;
            return no;
        }

        const larguraEsquerda = no.esquerda === null
            ? espacoDoFilhoAusente : no.esquerda.larguraSubarvore;
        const larguraDireita = no.direita === null
            ? espacoDoFilhoAusente : no.direita.larguraSubarvore;
        const larguraDosFilhos = larguraEsquerda
            + espacoEntreSubarvores + larguraDireita;

        no.larguraSubarvore = Math.max(no.largura, larguraDosFilhos);
        const inicioDosFilhos = (no.larguraSubarvore - larguraDosFilhos) / 2;
        no.inicioEsquerda = inicioDosFilhos;
        no.inicioDireita = inicioDosFilhos + larguraEsquerda + espacoEntreSubarvores;
        return no;
    }

    function posicionar(no, inicioX, profundidade) {
        if (no === null) {
            return;
        }

        no.x = margem + inicioX + no.larguraSubarvore / 2;
        no.y = margem + profundidade * espacoVertical;
        no.profundidade = profundidade;
        maiorProfundidade = Math.max(maiorProfundidade, profundidade);

        if (no.esquerda !== null) {
            posicionar(no.esquerda, inicioX + no.inicioEsquerda, profundidade + 1);
        }
        if (no.direita !== null) {
            posicionar(no.direita, inicioX + no.inicioDireita, profundidade + 1);
        }
    }

    medir(raiz);
    posicionar(raiz, 0, 0);

    const largura = Math.max(320, raiz.larguraSubarvore + margem * 2);
    const altura = margem * 2 + maiorProfundidade * espacoVertical + alturaDoNo;
    const svgNS = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(svgNS, "svg");
    svg.classList.add("arvore-svg");
    svg.setAttribute("width", "100%");
    svg.setAttribute("height", "100%");
    svg.setAttribute("viewBox", "0 0 " + largura + " " + altura);
    svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", "Arvore binaria de alunos do campus " + nomeDoCampus);

    // As conexoes sao desenhadas antes dos nos para permanecerem ao fundo.
    function desenharLigacoes(no) {
        if (no === null) {
            return;
        }

        [
            { no: no.esquerda, rotulo: "esq." },
            { no: no.direita, rotulo: "dir." }
        ].forEach(function (ramo) {
            const filho = ramo.no;
            if (filho === null) {
                return;
            }

            const inicioY = no.y + alturaDoNo;
            const fimY = filho.y;
            const caminho = document.createElementNS(svgNS, "path");
            caminho.classList.add("arvore-ligacao");
            caminho.setAttribute("d", "M " + no.x + " " + inicioY
                + " L " + filho.x + " " + fimY);
            svg.appendChild(caminho);

            const direcao = document.createElementNS(svgNS, "text");
            direcao.classList.add("arvore-direcao");
            direcao.setAttribute("x", no.x + (filho.x - no.x) * 0.35);
            direcao.setAttribute("y", inicioY + (fimY - inicioY) * 0.38);
            direcao.setAttribute("text-anchor", "middle");
            direcao.textContent = ramo.rotulo;
            svg.appendChild(direcao);
        });

        desenharLigacoes(no.esquerda);
        desenharLigacoes(no.direita);
    }

    function desenharNos(no) {
        if (no === null) {
            return;
        }

        const grupo = document.createElementNS(svgNS, "g");
        grupo.classList.add("arvore-no");
        if (no.profundidade === 0) {
            grupo.classList.add("raiz");
        }

        const titulo = document.createElementNS(svgNS, "title");
        titulo.textContent = no.nome;

        const retangulo = document.createElementNS(svgNS, "rect");
        retangulo.setAttribute("x", no.x - no.largura / 2);
        retangulo.setAttribute("y", no.y);
        retangulo.setAttribute("width", no.largura);
        retangulo.setAttribute("height", alturaDoNo);
        retangulo.setAttribute("rx", 9);

        const texto = document.createElementNS(svgNS, "text");
        texto.setAttribute("x", no.x);
        texto.setAttribute("y", no.y + alturaDoNo / 2 + 5);
        texto.setAttribute("text-anchor", "middle");
        texto.textContent = no.nome;

        grupo.appendChild(titulo);
        grupo.appendChild(retangulo);
        grupo.appendChild(texto);
        svg.appendChild(grupo);

        desenharNos(no.esquerda);
        desenharNos(no.direita);
    }

    desenharLigacoes(raiz);
    desenharNos(raiz);
    espaco.appendChild(svg);

    configurarNavegacaoDaArvore(svg, largura, altura, raiz,
        botaoDiminuir, botaoRestaurar, botaoAumentar);
}

function configurarNavegacaoDaArvore(svg, larguraTotal, alturaTotal, raiz,
                                    botaoDiminuir, botaoRestaurar, botaoAumentar) {
    const zoomMinimo = 0.7;
    const zoomMaximo = 3;
    const passoDoZoom = 0.25;
    let zoom = 1;
    let centroX = larguraTotal / 2;
    let centroY = alturaTotal / 2;
    let animacao = null;
    let arrastando = false;
    let ponteiroX = 0;
    let ponteiroY = 0;
    let centroInicialX = 0;
    let centroInicialY = 0;

    function limitar(valor, minimo, maximo) {
        return Math.min(maximo, Math.max(minimo, valor));
    }

    function aplicarVisualizacao() {
        const larguraVisivel = larguraTotal / zoom;
        const alturaVisivel = alturaTotal / zoom;
        const sobraX = Math.max(0, (larguraVisivel - larguraTotal) / 2);
        const sobraY = Math.max(0, (alturaVisivel - alturaTotal) / 2);
        const minimoX = larguraVisivel / 2 - sobraX;
        const maximoX = larguraTotal - larguraVisivel / 2 + sobraX;
        const minimoY = alturaVisivel / 2 - sobraY;
        const maximoY = alturaTotal - alturaVisivel / 2 + sobraY;

        centroX = limitar(centroX, Math.min(minimoX, maximoX), Math.max(minimoX, maximoX));
        centroY = limitar(centroY, Math.min(minimoY, maximoY), Math.max(minimoY, maximoY));

        svg.setAttribute("viewBox",
            (centroX - larguraVisivel / 2) + " "
            + (centroY - alturaVisivel / 2) + " "
            + larguraVisivel + " " + alturaVisivel);

        botaoRestaurar.textContent = Math.round(zoom * 100) + "%";
        botaoDiminuir.disabled = zoom <= zoomMinimo;
        botaoAumentar.disabled = zoom >= zoomMaximo;
    }

    function animarZoom(novoZoom, novoCentroX, novoCentroY) {
        if (animacao !== null) {
            window.cancelAnimationFrame(animacao);
        }

        const zoomInicial = zoom;
        const xInicial = centroX;
        const yInicial = centroY;
        const zoomFinal = limitar(novoZoom, zoomMinimo, zoomMaximo);
        const xFinal = novoCentroX === undefined ? centroX : novoCentroX;
        const yFinal = novoCentroY === undefined ? centroY : novoCentroY;
        const inicio = performance.now();
        const duracao = 260;

        function quadro(agora) {
            const progresso = Math.min(1, (agora - inicio) / duracao);
            const suave = 1 - Math.pow(1 - progresso, 3);
            zoom = zoomInicial + (zoomFinal - zoomInicial) * suave;
            centroX = xInicial + (xFinal - xInicial) * suave;
            centroY = yInicial + (yFinal - yInicial) * suave;
            aplicarVisualizacao();

            if (progresso < 1) {
                animacao = window.requestAnimationFrame(quadro);
            } else {
                animacao = null;
            }
        }

        animacao = window.requestAnimationFrame(quadro);
    }

    botaoDiminuir.onclick = function () {
        animarZoom(zoom - passoDoZoom);
    };

    botaoAumentar.onclick = function () {
        animarZoom(zoom + passoDoZoom, raiz.x, Math.max(raiz.y, alturaTotal / 3));
    };

    botaoRestaurar.onclick = function () {
        animarZoom(1, larguraTotal / 2, alturaTotal / 2);
    };

    svg.onwheel = function (evento) {
        evento.preventDefault();
        const direcao = evento.deltaY < 0 ? passoDoZoom : -passoDoZoom;
        animarZoom(zoom + direcao);
    };

    svg.onpointerdown = function (evento) {
        if (evento.button !== 0) {
            return;
        }
        arrastando = true;
        ponteiroX = evento.clientX;
        ponteiroY = evento.clientY;
        centroInicialX = centroX;
        centroInicialY = centroY;
        svg.classList.add("arrastando");
        svg.setPointerCapture(evento.pointerId);
    };

    svg.onpointermove = function (evento) {
        if (!arrastando) {
            return;
        }

        const larguraVisivel = larguraTotal / zoom;
        const alturaVisivel = alturaTotal / zoom;
        centroX = centroInicialX
            - (evento.clientX - ponteiroX) * larguraVisivel / svg.clientWidth;
        centroY = centroInicialY
            - (evento.clientY - ponteiroY) * alturaVisivel / svg.clientHeight;
        aplicarVisualizacao();
    };

    function pararArraste(evento) {
        if (!arrastando) {
            return;
        }
        arrastando = false;
        svg.classList.remove("arrastando");
        if (svg.hasPointerCapture(evento.pointerId)) {
            svg.releasePointerCapture(evento.pointerId);
        }
    }

    svg.onpointerup = pararArraste;
    svg.onpointercancel = pararArraste;
    aplicarVisualizacao();
}

async function excluirAluno(evento) {
    evento.preventDefault();

    const nome = document.getElementById("nomeExclusao").value;
    if (nome.trim() === "") {
        mostrarMensagem("msgExclusao", "erro", "Digite o nome do aluno.");
        return;
    }

    // Exclusao e uma acao que nao tem como desfazer, entao confirma antes.
    const confirmado = window.confirm(
        "Excluir \"" + nome.trim() + "\" de onde ele estiver matriculado?");
    if (!confirmado) {
        return;
    }

    const dados = await chamarApi("/api/excluir?nome=" + encodeURIComponent(nome),
        { method: "DELETE" });

    if (dados.ok) {
        mostrarMensagem("msgExclusao", "sucesso", dados.mensagem
            + " A arvore desse campus agora tem " + dados.total
            + " aluno(s) e altura " + dados.altura + ".",
            "Aluno removido!");
        document.getElementById("nomeExclusao").value = "";
        carregarCampi();
    } else {
        mostrarMensagem("msgExclusao", "aviso", dados.mensagem);
    }
}

function mostrarMensagem(idDaDiv, tipo, texto, titulo) {
    const div = document.getElementById(idDaDiv);
    div.innerHTML = "";

    const mensagem = document.createElement("div");
    mensagem.className = "mensagem " + tipo;
    mensagem.setAttribute("role", "alert");

    if (tipo === "sucesso") {
        mensagem.innerHTML = `
            <svg class="mensagem-icone" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" aria-hidden="true">
                <path d="M13 16h-1v-4h1m0-4h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z"
                      stroke-width="2"
                      stroke-linecap="round" stroke-linejoin="round"></path>
            </svg>
            <div class="mensagem-conteudo">
                <p class="mensagem-titulo"></p>
                <p class="mensagem-texto"></p>
            </div>`;

        mensagem.querySelector(".mensagem-titulo").textContent = titulo || "Operacao concluida!";
        mensagem.querySelector(".mensagem-texto").textContent = texto;
    } else {
        mensagem.textContent = texto;
    }

    div.appendChild(mensagem);
}
