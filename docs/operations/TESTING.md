# Estratégia de testes

O Archwright tem duas responsabilidades a testar: o **próprio código** (motor e interface) e os **projetos que ele gera**. A segunda é a mais importante: um gerador que produz um projeto que não compila não serve para nada (BR-044).

## Pirâmide

| Nível | O que cobre | Ferramentas | Rede/disco |
|---|---|---|---|
| Domínio | nomes, validação, perfis, determinismo | JUnit 5, AssertJ | nenhum |
| Aplicação | casos de uso com portas falsas (*fakes*) | JUnit 5 | nenhum |
| Adapters | YAML real, Mustache real, exportação ZIP/pasta, parser do Maven Central | JUnit 5, `@TempDir` | disco temporário; **nunca rede** |
| Golden | saída completa do motor comparada byte a byte | JUnit 5 | disco temporário |
| Arquitetura | AW-FF do próprio Archwright | ArchUnit | nenhum |
| Matriz de compilação | projetos gerados compilam e passam nas próprias fitness functions | CI + Maven | rede (Maven Central) |
| Desktop | `Messages` (todo código do engine tem texto), `FieldErrors`, `WizardStep`, `ProjectForm`, `UserPreferences` e o formulário contra o engine real (ZIP e pasta); sem toolkit gráfico | JUnit 5, AssertJ, `@TempDir` | disco temporário; **nunca rede** |
| Desktop (telas) | `WizardViewTest` dirige o `WizardView` real (cliques, digitação, as quatro telas, engine real offline, `Dialogs` de teste, falhas injetadas); roda com o toolkit JavaFX e é **pulado** quando não há tela (`Assumptions`) | JUnit 5, AssertJ | disco temporário; **nunca rede** |

Convenções:
- Testes de domínio e aplicação não usam arquivos nem rede.
- Nenhum teste automatizado acessa o Maven Central, exceto a matriz de compilação. O engine é criado com `.offline(true)` e o parser usa um `maven-metadata.xml` de exemplo em `src/test/resources`.
- Todo teste cita o id do critério (`ENG-EXP-02 #3`) no nome ou num comentário.
- Relógio injetado (`Clock`) onde houver tempo (cache de versões).

## Golden files

Para cada estilo há pedidos de referência em `archwright-engine/src/test/resources/golden/<caso>/request.json`, com o projeto esperado ao lado (`expected/`). O `GoldenProjectTest` gera o projeto e compara arquivo a arquivo.

Casos iniciais:

| Caso | Estilo | Boot / Java | Kit | Extras | Fitness |
|---|---|---|---|---|---|
| `modular-basic` | modular, módulos `pedidos`, `pagamentos` | 4.1 / 25 | `BASIC` | docker-compose, github-actions | sim |
| `modular-none` | modular, módulo `core` | 3.5 / 21 | `NONE` | — | sim |
| `layered-basic` | camadas, agregado `paciente` | 4.1 / 25 | `BASIC` | docker-compose | sim |
| `legacy-boot2` | modular, módulo `core` | 2.7 / 8 | `BASIC` + `springdoc` | — | sim |
| `no-fitness` | camadas | 4.1 / 25 | `NONE` | — | não |

Existem os cinco casos: `modular-basic`, `modular-none` (Boot 3.5.16), `legacy-boot2` (2.7.18), `layered-basic` (agregado `paciente`, 4.1.2) e `no-fitness` (camadas, sem agregados, NONE, 4.1.2). O `request.json` usa os mesmos campos do `ProjectRequest`. Os arquivos de `expected/` ficam marcados como `-text` no `.gitattributes`, para o Git nunca converter fim de linha.

Mudança intencional na saída: rodar `./mvnw -pl archwright-engine test -Dtest=GoldenProjectTest -Dgolden.update=true`, revisar o *diff* dos arquivos esperados no Git e versionar junto com a mudança de template. Diff inesperado em golden é sempre investigado antes de atualizar.

## Matriz de compilação

Job de CI separado (`generated-projects.yml`) que garante o BR-044:
1. O job `generate` roda `CompilationMatrixTest` com `-Dmatrix.output`, que gera um projeto por caso (via `ArchwrightEngine` offline, só pela API pública) e um `matrix.json`. Para cada linha do Spring Boot (último patch embutido), cada estilo e cada kit (`NONE`, `BASIC`), com fitness functions ligadas, são 52 casos hoje.
2. O job `verify` roda uma execução por caso (`fail-fast: false`), com o JDK do caso, e faz `./mvnw -B verify` no projeto gerado.
3. Java por linha: o padrão do perfil (BR-022), o maior testado (`javaMaxTested`, quando difere do padrão: 3.3 a 3.5 rodam em 22, 23 e 24) e, na 2.7, também o Java 8 (compilado no JDK 21).
4. Os casos `BASIC` acrescentam `springdoc` e `security` às dependências do kit e os dois extras, para exercitar as versões fixadas de SpringDoc e o `docker-compose`/CI gerados.

Quando roda: em todo PR que altere `catalog/`, `templates/` ou `fitness/`, e toda noite na `main`. Falha na matriz bloqueia o merge.

Os valores dos [perfis](../architecture/ARCHITECTURE.md#perfis-de-versão-iniciais) só ficam sem marcação de pendência depois de passar por aqui; a primeira rodada completa no GitHub (53 jobs) passou em 2026-11-18, depois de reexecutar dois jobs que falharam por rede (download do Maven e do parent POM).

Localmente: `./mvnw -pl archwright-engine test -Dtest=CompilationMatrixTest -Dmatrix.output=/tmp/matrix` gera os casos; cada um tem `matrix-app/` pronto para `./mvnw verify`. O `CompilationMatrixTest` também roda no `./mvnw verify` normal (sem `matrix.output`) e confere a cobertura e que todo caso gera com sucesso.

Achado da primeira rodada: no Boot 2.7 e 3.0 (maven-compiler-plugin 3.10.1) o javac não gera `package-info.class`, então um projeto recém-gerado tinha as três camadas vazias e o `FF-LAY-01` falhava. Corrigido com `withOptionalLayers(true)` no template.

## Roteiro manual do desktop

A lógica das telas é coberta por `WizardViewTest` (48 casos: detalhe do pacote, kit, artifacts, extras e regras, árvore de pacotes ao vivo, descrições dos campos, validação ao digitar, bloqueio do Avançar, navegação, versões e falha na busca, kits, extras, estilos, fitness functions, geração de ZIP e pasta nos dois estilos, falha de destino, queda inesperada, sobrescrita confirmada e recusada, seletor de pastas, "Abrir pasta", preferências e tema). Isso foi repetido 5 vezes seguidas sem falha. Sobra ao roteiro manual o que só o sistema operacional mostra, e cada rodada termina rodando `./mvnw verify` no projeto gerado.

| # | Passo | Esperado | Última verificação |
|---|---|---|---|
| 1 | Gerar pela interface: modular + ZIP e camadas (com agregados) + pasta, `./mvnw verify` no resultado | 16 e 13 testes verdes | macOS, 2026-11-18 |
| 2 | Temas claro e escuro | legíveis; a escolha persiste ao reabrir | macOS, 2026-11-18 (capturas offscreen) |
| 3 | "Escolher..." no destino | o seletor de pastas nativo abre no destino atual | pendente |
| 4 | Gerar de novo o mesmo ZIP | a janela de confirmação nativa aparece; "Cancelar" não grava e "Substituir" grava | pendente |
| 5 | "Abrir pasta" após gerar | o gerenciador de arquivos abre na pasta certa | pendente |
| 6 | Repetir 1 a 5 no Windows e no Linux | mesmo resultado | pendente |

Em ambiente sem tela (CI no Linux), `WizardViewTest` é pulado; rodá-lo no CI exige um display virtual (`xvfb`) e as bibliotecas GTK, ainda não configurados.

## Aceitação mínima por fase

| Fase | Teste obrigatório |
|---|---|
| 0 | `./mvnw verify` verde com `EngineArchitectureTest` |
| 1 | todos os `ENG-*`; golden dos 5 casos; matriz de compilação verde |
| 2 | `DesktopArchitectureTest`; roteiro manual do wizard gerando ZIP e pasta nos dois estilos e rodando `./mvnw verify` no resultado |
| 3 | instalador de cada SO instala e gera projeto numa máquina sem JDK |

## Matriz de falhas

| Cenário | Esperado |
|---|---|
| Sem internet ao abrir o app | versões do cache ou embutidas; aviso `VERSIONS_OFFLINE`; geração funciona |
| Maven Central lento | desiste em 8 s e usa o fallback |
| Disco cheio durante exportação de pasta | pasta temporária removida; destino intacto |
| Disco cheio durante exportação de ZIP | nenhum `.zip` parcial no destino |
| Pasta de destino já com projeto | `TARGET_NOT_EMPTY`; nada alterado |
| Template com variável inexistente (bug) | falha no build do Archwright (golden/renderer), nunca na mão do usuário |
| Catálogo inconsistente (bug) | `CatalogConsistencyTest` falha no build |
| Nova versão do Spring Boot de linha desconhecida | não aparece na lista até o catálogo ser atualizado |
