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
4. [ ] **Versões do Spring Boot** com cache e fallback ([ENG-VER](../architecture/ARCHITECTURE.md#versões-do-spring-boot-eng-ver)).
5. [ ] **Contexto** ([ENG-CTX](../architecture/ARCHITECTURE.md#etapa-3--montar-contexto-eng-ctx)).
6. [ ] **Templates `common` + `hexagonal-modular`**, portando o protótipo `GeradorHex.java` e o Hexbank, e o **planner** ([ENG-PLN-01](../architecture/ARCHITECTURE.md#etapa-4--planejar-arquivos-eng-pln)).
7. [ ] **Renderer** + primeiro teste *golden* ([ENG-RND](../architecture/ARCHITECTURE.md#etapa-5--renderizar-eng-rnd)).
8. [ ] **Exportadores ZIP e pasta** ([ENG-EXP](../architecture/ARCHITECTURE.md#etapa-6--exportar-eng-exp)).
9. [ ] **Casos de uso** e fábrica `ArchwrightEngine` ([ENG-UC](../architecture/ARCHITECTURE.md#casos-de-uso-eng-uc), [ENGINE_API](../contracts/ENGINE_API.md)) + `EngineEndToEndTest`.
10. [ ] **Templates das fitness functions** e seleção ([ENG-PLN-02](../architecture/ARCHITECTURE.md#etapa-4--planejar-arquivos-eng-pln), [catálogo](../contracts/GENERATED_PROJECT.md#fitness-functions)).
11. [ ] **Estilo `hexagonal-layered`** (portando o Liva) + golden.
12. [ ] **Matriz de compilação** no CI ([TESTING](../operations/TESTING.md#matriz-de-compilação)) e remoção dos ⚠️ dos perfis.

**Pronto quando:** todos os testes `ENG-*` passam; golden dos dois estilos estável; a matriz de compilação gera e roda `./mvnw verify` com sucesso em todas as combinações (linha do Boot × estilo × kit) com fitness functions ligadas (BR-044).

## Fase 2 — App desktop
- [ ] Módulo `archwright-desktop` com JavaFX + AtlantaFX, tema claro/escuro.
- [ ] Wizard: (1) Projeto, (2) Versões, (3) Dependências + fitness functions, (4) Exportar com resumo da pré-visualização (ENG-UC-02).
- [ ] Validação enquanto o usuário digita, destacando o campo de cada `Violation`; textos das mensagens em `messages_pt_BR.properties` (preparado para `en`).
- [ ] Busca de versões fora da thread de UI, com indicador de origem (`REMOTE`/`CACHE`/`EMBEDDED`).
- [ ] Escolha de destino (`DirectoryChooser`), formato ZIP ou pasta, confirmação de sobrescrita do ZIP (`TARGET_EXISTS`) e botão "abrir pasta" ao final.
- [ ] Lembrar as últimas escolhas (`java.util.prefs`).
- [ ] `DesktopArchitectureTest` (AW-FF-05).

**Pronto quando:** dá para gerar ZIP e pasta pelos dois estilos só pela interface, e o projeto gerado passa no `./mvnw verify`.

## Fase 3 — Distribuição
- [ ] `jpackage` no CI para Windows, macOS e Linux.
- [ ] GitHub Releases com instaladores e notas de versão.
- [ ] Publicar `archwright-engine` no Maven Central (`io.github.arthursouto`).
- [ ] Política de versão (SemVer) e atualização dos perfis a cada nova linha do Spring Boot.

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
**Fase 0.** Na ordem:
1. Revisar e versionar esta documentação.
2. Adicionar `LICENSE` (Apache 2.0) pelo GitHub.
3. Criar o build multi-módulo, os pacotes do engine com `package-info` e o `EngineArchitectureTest`.
4. CI com `./mvnw -B verify`.
