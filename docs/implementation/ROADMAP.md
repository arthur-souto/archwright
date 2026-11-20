# Roadmap de implementação — ordem recomendada

Cada fase depende da anterior, e os critérios de pronto são verificáveis. **Não são estimativas de duração.** Em toda fase: atualizar testes, README, specs (status ⏳ → ✅) e ADRs.

Foco atual: **motor primeiro, interface depois.**

## Fase 0 — Fundação
- [x] Documentação inicial: ADRs 0001 a 0004, regras de negócio, arquitetura, contratos, roadmap e estratégia de testes.
- [x] Licença Apache 2.0 (`LICENSE` na raiz).
- [x] Build Maven multi-módulo: `pom.xml` agregador + `archwright-engine` + `archwright-desktop` (vazio por enquanto), Java 25, Maven Wrapper.
- [x] Pacotes do engine ([estrutura](../architecture/ARCHITECTURE.md#pacotes-do-engine)). O próprio engine não usa `package-info.java` (decisão do autor); o BR-054 vale só para o projeto gerado.
- [x] `EngineArchitectureTest` com AW-FF-01 a 04 e 06 ([regras](../architecture/ARCHITECTURE.md#regras-de-dependência-fitness-functions-do-próprio-engine)), com `archunit.properties` (`archRule.failOnEmptyShould=false`).
- [x] CI no GitHub Actions: `./mvnw -B verify` em Ubuntu.
- [x] Dependabot (Maven e GitHub Actions): só dependências estáveis e recentes, com alerta de vulnerabilidades.

**Pronto quando:** `./mvnw verify` passa localmente e no CI, com as fitness functions do próprio engine rodando.

## Fase 1 — Motor
Ordem pensada para que cada passo seja testável sozinho, do domínio para as bordas.

1. [x] **Nomes e validação** ([ENG-VAL](../architecture/ARCHITECTURE.md#etapa-1--validar-e-normalizar-eng-val)): `Names`, tipos de valor (`ArtifactId`, `GroupId`, `BasePackage`, `ModuleName`, `BootVersion`, `JavaVersion`), `Violation`, `Warning`, `SpecValidator`. Catálogo montado em memória nos testes.
2. [x] **Aceitar o [ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md)** (aceito, com Strategy para estilo e kit) e implementar os **catálogos YAML** ([ENG-CAT](../architecture/ARCHITECTURE.md#catálogos-eng-cat)) com os [perfis iniciais](../architecture/ARCHITECTURE.md#perfis-de-versão-iniciais). O `fitness-rules.yaml` e a regra ENG-CAT-01 #5 entram no passo 10.
3. [x] **BootProfile** ([ENG-BPR](../architecture/ARCHITECTURE.md#etapa-2--resolver-o-bootprofile-eng-bpr)).
4. [x] **Versões do Spring Boot** com cache e fallback ([ENG-VER](../architecture/ARCHITECTURE.md#versões-do-spring-boot-eng-ver)).
5. [x] **Contexto** ([ENG-CTX](../architecture/ARCHITECTURE.md#etapa-3--montar-contexto-eng-ctx)).
6. [x] **Templates `common` + `hexagonal-modular`**, portando o protótipo `GeradorHex.java` e o Hexbank, e o **planner** ([ENG-PLN-01](../architecture/ARCHITECTURE.md#etapa-4--planejar-arquivos-eng-pln)).
7. [x] **Renderer** + primeiro teste *golden* ([ENG-RND](../architecture/ARCHITECTURE.md#etapa-5--renderizar-eng-rnd)).
8. [x] **Exportadores ZIP e pasta** ([ENG-EXP](../architecture/ARCHITECTURE.md#etapa-6--exportar-eng-exp)).
9. [x] **Casos de uso** e fábrica `ArchwrightEngine` ([ENG-UC](../architecture/ARCHITECTURE.md#casos-de-uso-eng-uc), [ENGINE_API](../contracts/ENGINE_API.md)) + `EngineEndToEndTest`.
10. [x] **Templates das fitness functions** e seleção ([ENG-PLN-02](../architecture/ARCHITECTURE.md#etapa-4--planejar-arquivos-eng-pln), [catálogo](../contracts/GENERATED_PROJECT.md#fitness-functions)).
11. [x] **Estilo `hexagonal-layered`** (portando o Liva) + golden.
12. [x] **Matriz de compilação** no CI ([TESTING](../operations/TESTING.md#matriz-de-compilação)) e remoção dos ⚠️ dos perfis.
    - [x] Gerador dos casos (`CompilationMatrixTest`) e workflow `generated-projects.yml`.
    - [x] Primeira execução verde no GitHub (inclui Java 22 a 24) e ⚠️ removidos de `ARCHITECTURE.md`.
13. [x] **Exceções padrão e `GlobalExceptionHandler`** nos dois estilos ([contrato](../contracts/GENERATED_PROJECT.md#exceções-padrão-e-tratamento-de-erros)): `DomainException`, `ResourceNotFoundException`, `ConflictException`, `BusinessRuleException` e, com `web`, o handler (`ProblemDetail` no Boot 3/4, `ApiError` no 2.7). O estilo em camadas ganhou `domain.exceptions` (19 `package-info`) e os pacotes de exceções dos dois estilos passaram a se chamar `exceptions`. O handler traz `errors` com campo e mensagem na validação, e o `ApiError` do 2.7 segue o formato do `ProblemDetail`.
    - [x] Matriz de compilação verde no GitHub com os arquivos novos (53 jobs, 2026-11-20; um job caiu por rede ao baixar o Maven e passou ao reexecutar). Localmente, `legacy-boot2` com Lombok só compila no JDK 21, não no 25.
14. [x] **Pacotes de persistência só com `jpa`**: `when` passou a valer em `packages` do manifest; sem `jpa` não saem `persistence`, `entity` nem `jpa` ([contrato](../contracts/GENERATED_PROJECT.md#arquivos-comuns-a-todos-os-estilos)).

**Pronto quando:** todos os testes `ENG-*` passam; golden dos dois estilos estável; a matriz de compilação gera e roda `./mvnw verify` com sucesso em todas as combinações (linha do Boot × estilo × kit) com fitness functions ligadas (BR-044).

## Fase 2 — App desktop
- [x] Módulo `archwright-desktop` com JavaFX + AtlantaFX, tema claro/escuro.
- [x] Wizard: (1) Projeto, (2) Versões, (3) Dependências + fitness functions, (4) Exportar com resumo da pré-visualização (ENG-UC-02).
- [x] Validação enquanto o usuário digita, destacando o campo de cada `Violation`; textos das mensagens em `messages_pt_BR.properties` (preparado para `en`).
- [x] Busca de versões fora da thread de UI, com indicador de origem (`REMOTE`/`CACHE`/`EMBEDDED`).
- [x] Escolha de destino (`DirectoryChooser`), formato ZIP ou pasta, confirmação de sobrescrita do ZIP (`TARGET_EXISTS`) e botão "abrir pasta" ao final.
- [x] Lembrar as últimas escolhas (`java.util.prefs`).
- [x] Descrição visível em cada campo e árvore de pacotes ao vivo no canto direito (muda com pacote base, módulos e estilo), com detalhe do pacote (propósito, subpacotes, classes, linhas), artifacts Maven, kits, extras e regras na tela de dependências.
- [x] `DesktopArchitectureTest` (AW-FF-05).
- [x] Redesenho da interface (branch `ui/redesign`): barra lateral com a logo (Início, Novo projeto, Documentação, Histórico, Sobre e tema), tela de início com números, gráficos de uso, modelos de início rápido, projetos recentes e dicas, histórico de projetos gerados (reutilizar, abrir pasta, remover), documentação com links, paleta quente da logo (âmbar e grafite) e cantos de 3 px, no estilo do Jira.
- [ ] Roteiro manual do desktop ([TESTING](../operations/TESTING.md#roteiro-manual-do-desktop)): falta ver o seletor de pastas, a confirmação e o "Abrir pasta" nativos (a lógica já é testada com `Dialogs` de teste) e repetir no Windows e no Linux.

**Pronto quando:** dá para gerar ZIP e pasta pelos dois estilos só pela interface, e o projeto gerado passa no `./mvnw verify`.

## Fase 3 — Distribuição
- [x] Logo (girafa de mosaico hexagonal, `archwright-desktop/packaging/archwright.svg`) com ícones `.icns`, `.ico` e `.png`, e guia de instalação em `INSTALL.md`.
- [x] Perfil Maven `installer` com `jpackage`: `.dmg` gerado, instalado e aberto no macOS (sem assinatura).
- [x] `jpackage` no CI para Windows, macOS e Linux: workflow `installers.yml` roda nos quatro alvos (macOS arm64 e Intel, Windows, Linux) e gera `.dmg`, `.msi` e `.deb`.
- [ ] Instalar e abrir o `.msi` (Windows) e o `.deb` (Linux) numa máquina de verdade.
- [x] GitHub Releases com instaladores e notas de versão: o job `release` publica um pré-lançamento a partir de uma tag `v*`. [v0.1.0](https://github.com/arthur-souto/archwright/releases/tag/v0.1.0) saiu com os quatro instaladores, e a [v0.2.0](https://github.com/arthur-souto/archwright/releases/tag/v0.2.0) trouxe a interface nova.
- [ ] Publicar `archwright-engine` no Maven Central (`io.github.arthursouto`).
- [ ] Política de versão (SemVer), incluindo o número do instalador (hoje fixo em `1.0.0` nos arquivos `jpackage-<so>.txt`, por causa do macOS), e atualização dos perfis a cada nova linha do Spring Boot.
- [ ] Assinatura e notarização dos instaladores (Apple Developer no macOS, certificado de código no Windows).

**Pronto quando:** um usuário sem JDK instala pelo GitHub Releases e gera um projeto.

## Fase 4 — Extensibilidade
- [ ] Pasta externa de templates que sobrescreve os embutidos.
- [ ] CLI (outro adapter de entrada sobre o mesmo engine).
- [ ] Adicionar um módulo a um projeto já gerado.

## Fase 5 — Novos estilos de arquitetura
- [ ] Clean Architecture.
- [ ] Onion.
- [ ] Camadas tradicionais (controller/service/repository), com fitness functions próprias.
- [ ] Avaliar Spring Modulith e jMolecules como complementos de fitness functions.

## Fase 6 — Projetos existentes e outros builds
- [ ] Gerar só as fitness functions para um projeto existente, com `FreezingArchRule` (violações atuais registradas, só novas quebram o build).
- [ ] Gradle e Kotlin.

## Próxima tarefa concreta
**Fase 3, em andamento.** Na ordem:
1. Terminar o roteiro manual do desktop ([TESTING](../operations/TESTING.md#roteiro-manual-do-desktop)): seletor de pastas, confirmação e "Abrir pasta" nativos, e instalar o `.msi` e o `.deb`.
2. Definir a política de versão e o número do instalador.
3. Publicar o engine no Maven Central.
