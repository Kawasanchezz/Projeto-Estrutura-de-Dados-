# Cadastro de Alunos por Campus - Unicsul

Trabalho de Estrutura de Dados: uma Árvore Binária de Busca (implementada à mão) para cada um dos 7 campi,
com interface web servida por um servidor HTTP embutido (`com.sun.net.httpserver`). Não usa bibliotecas externas.

## Executar

Requer JDK 17+ (testado com JDK 26). A partir da pasta do projeto:

```bash
javac -encoding UTF-8 -d target/classes $(find src -name '*.java')
java -cp target/classes unicsul.Principal
```

O navegador abre em `http://localhost:8080` (usa 8081, 8082... se a porta estiver ocupada).
Use `--sem-navegador` para não abrir o navegador. Execute sempre com a pasta do projeto como diretório atual:
o servidor procura a pasta `webapp` a partir dele.

## Testes

```bash
java -cp target/classes unicsul.testes.TesteArvore
```

## Segurança

- O servidor escuta apenas em `127.0.0.1` e rejeita `Host`/`Origin` de fora, o que bloqueia CSRF e DNS rebinding
  vindos de outros sites. Não há autenticação: **não exponha a porta na rede**.
- As respostas enviam CSP restritiva, `X-Content-Type-Options`, `X-Frame-Options` e `Referrer-Policy`.
- Cada campus aceita no máximo 1000 alunos (a árvore não é balanceada e as operações são recursivas).
- Os dados ficam só em memória e os alunos de exemplo são fictícios. Não há secrets, banco de dados nem variáveis de ambiente.
