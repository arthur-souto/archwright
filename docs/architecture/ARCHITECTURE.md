# Arquitetura do Archwright

## Decisão
Um build Maven com dois módulos: **`archwright-engine`**, uma biblioteca Java pura que valida, planeja, renderiza e exporta projetos, e **`archwright-desktop`**, o app JavaFX que só coleta escolhas e chama o engine ([ADR-0001](../adr/0001-motor-independente-e-hexagonal.md), [ADR-0002](../adr/0002-desktop-javafx-atlantafx.md)). O engine segue a mesma arquitetura hexagonal que gera e roda as mesmas fitness functions.

## Visão geral

```text
┌──────────────── archwright-desktop (JavaFX) ────────────────┐
│ 1. Projeto  →  2. Versões  →  3. Dependências  →  4. Exportar │
└──────────────────────────────┬───────────────────────────────┘
                               │ ProjectRequest + ExportTarget
┌──────────────── archwright-engine ──────────────▼──────────────┐
│ 1 Validar → 2 BootProfile → 3 Contexto → 4 Planejar → 5 Renderizar │
│                                                    │            │
│                                       6 Exportar: ZIP | PASTA   │
└───▲──────────────▲──────────────▲──────────────▲────────────────┘
    │ versões      │ kits/deps    │ fitness rules │ templates
    └──────── catálogos (YAML + templates Mustache, embutidos) ──┘
```

### Diagrama da Fase 1

![Gerador Hexagonal — Fase 1: app desktop, motor, catálogos e projeto gerado](images/visao-geral-fase-1.png)

Como ler:

- **App desktop → motor:** o wizard monta um `ProjectRequest` (mais o `ExportTarget`) e o entrega ao motor. O motor não conhece a interface.
- **Erro volta para o wizard:** se a etapa 1 devolver violações, elas retornam à tela correspondente ([ENG-VAL](#etapa-1--validar-e-normalizar-eng-val)); nada é renderizado nem gravado.
- **Catálogos → motor:** versões (Maven Central + fallback), estilos, dependências, extras, regras de fitness functions e templates alimentam o motor como dados (kits e regras de estilo são estratégias em código) ([ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md)). No diagrama a seta sai das versões para o `BootProfile` (etapa 2); kits, regras e templates são lidos nas etapas 1, 4 e 5.
- **Exportação:** na etapa 6 o formato decide o adapter. `ZipExporter` gera um `.zip` igual ao do Initializr; `FolderExporter` grava numa pasta vazia, usando diretório temporário e `move` ao final ([ENG-EXP](#etapa-6--exportar-eng-exp)).
- **Projeto gerado:** `src/main` hexagonal com `package-info`, fitness functions ArchUnit em `src/test/.../architecture` e `mvn verify` + CI, de modo que as regras rodam a cada push/PR ([GENERATED_PROJECT](../contracts/GENERATED_PROJECT.md)).

## Módulos Maven

| Módulo | Conteúdo | Depende de |
|---|---|---|
| `archwright-engine` | domínio, casos de uso, catálogos, templates, exportadores | JDK, SnakeYAML, JMustache |
| `archwright-desktop` | telas JavaFX + AtlantaFX, empacotamento `jpackage` | `archwright-engine` (somente `application.port.in` e `config`) |

Os dois módulos usam **Java 25** e são compilados com `--release 25`. Os projetos gerados podem usar qualquer Java suportado (BR-021); a versão do Archwright não limita a do projeto gerado.

## Pacotes do engine

```text
io.github.arthursouto.archwright.engine
├── domain/
│   ├── model/        # ProjectSpec, ProjectIdentity, ArtifactId, GroupId, BasePackage, Description, ModuleName,
│   │                 # Architecture, ArchitectureStyle, BootVersion, JavaVersion, BootProfile, Catalog,
│   │                 # DependencyId, DependencyDefinition, ExtraId, ExtraDefinition, KitId,
│   │                 # FilePlan, PlannedFile, GeneratedProject, GeneratedFile
│   ├── style/        # ArchitectureStyleStrategy, HexagonalModularStyle, HexagonalLayeredStyle, ArchitectureStyles
│   ├── kit/          # KitStrategy, NoneKit, BasicKit, Kits
│   ├── validation/   # ProjectDraft, Violation, Warning, ViolationCode, WarningCode, ValidationResult, SpecValidator
│   │                 # (regras BR-001 a BR-034; um validador interno por assunto)
│   └── naming/       # Names, PackageSegments (puros)
├── application/
│   ├── port/in/      # GenerateProjectUseCase, PreviewProjectQuery, CatalogQuery + DTOs (ProjectRequest, ExportTarget, ...)
│   ├── port/out/     # BootVersionSource, VersionCache, CatalogRepository, TemplateRepository,
│   │                 # TemplateRenderer, ProjectExporter
│   └── usecase/      # GenerateProject, PreviewProject, BrowseCatalog, ProfileResolver,
│                     # ContextBuilder, FilePlanner, ProjectRenderer
├── adapter/
│   └── out/
│       ├── version/  # MavenCentralBootVersionSource, FileVersionCache, EmbeddedBootVersions
│       ├── catalog/  # YamlCatalogRepository (classpath)
│       ├── template/ # ClasspathTemplateRepository, MustacheTemplateRenderer
│       └── export/   # ZipProjectExporter, FolderProjectExporter
└── config/           # ArchwrightEngine: monta tudo e expõe as portas de entrada
```

Recursos (não são código):

```text
archwright-engine/src/main/resources/archwright/
├── catalog/        # styles.yaml, boot-profiles.yaml, dependencies.yaml, extras.yaml, fitness-rules.yaml, boot-versions.txt
├── templates/
│   ├── common/             # pom.xml, application.yaml, .gitignore, README.md, mvnw, docker-compose.yml, ci.yml
│   ├── hexagonal-modular/  # manifest.yaml + arquivos do estilo
│   └── hexagonal-layered/  # manifest.yaml + arquivos do estilo
└── fitness/        # templates das classes de teste ArchUnit, uma por grupo de regras
```

## Regras de dependência (fitness functions do próprio engine)

| Id | Regra |
|---|---|
| AW-FF-01 | `adapter → application → domain`, nunca ao contrário; `config` pode ver todos. |
| AW-FF-02 | `domain` não depende de SnakeYAML, JMustache, `java.io.File`, `java.nio.file`, `java.net` nem `java.util.zip`: o domínio não faz E/S. |
| AW-FF-03 | `application` não depende de SnakeYAML nem JMustache (só de portas). |
| AW-FF-04 | Tipos em `port.in` e `port.out` são interfaces, records ou enums. |
| AW-FF-05 | O módulo `archwright-desktop` só importa `..engine.application.port.in..` e `..engine.config..`. |
| AW-FF-06 | Sem ciclos entre pacotes. |

Ficam em `archwright-engine/src/test/java/.../architecture/EngineArchitectureTest.java` e `archwright-desktop/src/test/java/.../architecture/DesktopArchitectureTest.java`.

## Como ler as especificações
Cada etapa abaixo segue o [modelo de especificação](../implementation/SPEC_TEMPLATE.md): status, regras de origem, contrato e critérios de aceite em tabela *Dado / Quando / Então*. Cada linha da tabela vira ao menos um teste, que cita o id (`ENG-VAL-01 #3`) no nome ou num comentário. Prefixos em uso:

| Prefixo | Assunto |
|---|---|
| `ENG-CAT` | carregamento e validação dos catálogos |
| `ENG-VER` | lista de versões do Spring Boot |
| `ENG-VAL` | etapa 1: validar e normalizar |
| `ENG-BPR` | etapa 2: resolver o `BootProfile` |
| `ENG-CTX` | etapa 3: montar contexto |
| `ENG-PLN` | etapa 4: planejar arquivos e escolher fitness functions |
| `ENG-RND` | etapa 5: renderizar |
| `ENG-EXP` | etapa 6: exportar |
| `ENG-UC` | casos de uso (portas de entrada) |

---

## Catálogos (`ENG-CAT`)
**Status:** ✅ implementado para `styles`, `boot-profiles`, `dependencies` e `extras`. Pendente para o passo 10 do roadmap: `fitness-rules.yaml` e a linha #5.
Regras: [BR-020](../domain/BUSINESS_RULES.md#versões), [BR-030](../domain/BUSINESS_RULES.md#dependências-e-kits), [BR-031](../domain/BUSINESS_RULES.md#dependências-e-kits), [BR-041](../domain/BUSINESS_RULES.md#fitness-functions), [ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md). Formato: [TEMPLATE_FORMAT](../contracts/TEMPLATE_FORMAT.md#catálogos).

Lê os YAML do classpath uma vez, valida a consistência entre eles e entrega objetos imutáveis. Estilos e kits têm regras em código ([ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md), item 6); o catálogo só confere que o que elas citam existe nos YAML. Um catálogo inválido é bug do Archwright: falha na inicialização do engine e no build (teste dedicado), nunca na mão do usuário.

**Contrato**

    public interface CatalogRepository {
        List<BootProfile> bootProfiles();        // ordenados da linha mais nova para a mais antiga
        List<DependencyDefinition> dependencies();
        List<ExtraDefinition> extras();
        List<FitnessRuleDefinition> fitnessRules();
        List<ArchitectureStyleDefinition> styles();
    }
    // YamlCatalogRepository lança IllegalStateException("catalog: <arquivo>: <problema>") se inválido

#### ENG-CAT-01 — Catálogos consistentes
> **Como** mantenedor, **quero** que um catálogo quebrado falhe o build, **para** nunca distribuir uma versão que gere projetos errados.

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | catálogos oficiais do repositório | engine inicia | carrega sem erro; 2 estilos, ≥ 9 perfis; `Kits.standard()` com `NONE` e `BASIC` |
| 2 | kit (estratégia) referencia dependência `foo` que não existe em `dependencies.yaml` | engine inicia | `IllegalStateException` citando o kit e `foo` |
| 3 | dois perfis com a mesma linha `3.5` | carregar | exceção citando `boot-profiles.yaml` e `3.5` |
| 4 | perfil com `artifacts` de uma dependência que não existe em `dependencies.yaml` (a disponibilidade por versão é definida pelas chaves de `artifacts`) | carregar | exceção citando o perfil e a dependência |
| 5 | regra de fitness function referencia template inexistente | carregar | exceção citando a regra e o caminho (passo 10 do roadmap) |
| 6 | perfil com `javaMin` > `javaMaxTested` | carregar | exceção citando a linha |
| 7 | estilo em `styles.yaml` sem estratégia registrada em `ArchitectureStyles` | engine inicia | `IllegalStateException` citando o estilo (`CatalogVerifier`) |

Também verificados: arquivo ausente, vazio ou malformado, chave YAML duplicada, `line` sem aspas (YAML leria `3.10` como número), campo obrigatório ausente (cita a entrada), id duplicado, `implies` e `requires` com dependência inexistente, estilo desconhecido ou ausente. Os perfis saem ordenados pela linha, numericamente.

**Pronto quando:** `CatalogConsistencyTest` (catálogos quebrados num diretório temporário, lidos pelo classpath), `OfficialCatalogTest` (#1) e `CatalogVerifierTest` (#2) cobrem #1 a #4, #6 e #7. O `boot-profiles.yaml` já traz as coordenadas Maven, mas só as chaves de `artifacts` são lidas até o [ENG-BPR](#etapa-2--resolver-o-bootprofile-eng-bpr); os valores ⚠️ continuam pendentes da matriz de compilação.

---

## Versões do Spring Boot (`ENG-VER`)
**Status:** ⏳ a implementar.
Regras: [BR-020](../domain/BUSINESS_RULES.md#versões), [BR-022](../domain/BUSINESS_RULES.md#versões), [BR-023](../domain/BUSINESS_RULES.md#versões), [BR-072](../domain/BUSINESS_RULES.md#erros-avisos-e-privacidade).

Monta a lista de versões oferecida na tela 2, com fallback em cadeia.

**Contrato**

    public interface BootVersionSource { List<String> fetch() throws IOException; } // Maven Central, timeout total 8 s
    public interface VersionCache {
        Optional<CachedVersions> read();            // versões + instante da gravação
        void write(List<String> versions, Instant at);
    }
    // cache em <home do usuário>/.archwright/cache/boot-versions.json

Filtro aplicado sobre qualquer origem: só `x.y.z` estáveis, `>= 2.7.0`, cuja linha exista no catálogo; ordenadas da mais nova para a mais antiga.

#### ENG-VER-01 — Lista de versões com fallback
> **Como** usuário, **quero** ver as versões atuais do Spring Boot mesmo sem internet, **para** nunca ficar impedido de gerar (BR-023).

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | Maven Central responde `4.1.2, 4.1.1, 4.2.0-M1, 2.6.9` | listar | `[4.1.2, 4.1.1]`, origem `REMOTE`; cache gravado |
| 2 | Maven Central fora; cache gravado há 3 h | listar | versões do cache, origem `CACHE` |
| 3 | Maven Central fora; cache gravado há 3 dias | listar | versões do cache, origem `STALE_CACHE` |
| 4 | Maven Central fora; sem cache | listar | lista embutida (`boot-versions.txt`), origem `EMBEDDED` |
| 5 | Maven Central demora 30 s | listar | desiste em 8 s e cai no item #2, #3 ou #4 |
| 6 | Maven Central devolve `5.0.0` e não existe perfil `5.0` | listar | `5.0.0` não aparece |
| 7 | arquivo de cache corrompido | listar | ignora o cache, segue a cadeia, regrava se o remoto responder |
| 8 | cache fresco (< 24 h) | listar | **não** chama o Maven Central |

**Fora do escopo:** atualizar o catálogo de perfis remotamente (perfis só mudam com nova versão do Archwright).
**Pronto quando:** `BootVersionsTest` (fakes de `BootVersionSource` e `VersionCache`, relógio fixo) cobre #1 a #8; `MavenCentralBootVersionSourceTest` cobre o parser do XML com arquivo de exemplo.

---

## Etapa 1 — Validar e normalizar (`ENG-VAL`)
**Status:** ✅ implementado (`SpecValidatorTest` e um teste por validador).
Regras: [BR-001 a BR-005](../domain/BUSINESS_RULES.md#identificação-do-projeto), [BR-010 a BR-012](../domain/BUSINESS_RULES.md#estilo-de-arquitetura), [BR-020 a BR-024](../domain/BUSINESS_RULES.md#versões), [BR-030 a BR-034](../domain/BUSINESS_RULES.md#dependências-e-kits), [BR-070](../domain/BUSINESS_RULES.md#erros-avisos-e-privacidade).
Depende de: [catálogos](#catálogos-eng-cat).

Transforma o `ProjectDraft` (strings cruas, com nulos já normalizados) num `ProjectSpec` (tipos de domínio válidos) ou numa lista com **todas** as violações. O caso de uso converte o `ProjectRequest` (`application.port.in`) em `ProjectDraft`, porque o domínio não pode depender da aplicação (AW-FF-01). Lógica pura, no `domain`, sem E/S. A verificação do destino de exportação (BR-061 a BR-063) **não** é feita aqui: é do exportador ([ENG-EXP](#etapa-6--exportar-eng-exp)).

**Contrato**

    public sealed interface ValidationResult {
        record Valid(ProjectSpec spec, List<Warning> warnings) implements ValidationResult {}
        record Invalid(List<Violation> violations) implements ValidationResult {}
    }
    public SpecValidator(Catalog catalog, ArchitectureStyles styles, Kits kits);
    public ValidationResult SpecValidator.validate(ProjectDraft draft);
    public record Violation(ViolationCode code, String field, Map<String, String> params) {}

#### ENG-VAL-01 — Identificação do projeto
> **Como** usuário, **quero** saber exatamente o que está errado nos nomes, **para** corrigir tudo de uma vez (BR-001 a BR-005, BR-070).

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | `artifactId=" Pedido-API "`, `groupId="com.acme"`, pacote vazio | validar | válido; `artifactId=pedido-api`, pacote `com.acme.pedidoapi`, classe `PedidoApiApplication` |
| 2 | `artifactId="pedido_api"` | validar | `INVALID_ARTIFACT_ID` em `artifactId` |
| 3 | `artifactId` com 65 caracteres válidos | validar | `INVALID_ARTIFACT_ID` |
| 4 | `groupId="com..acme"` | validar | `INVALID_GROUP_ID` |
| 5 | pacote `"com.acme.new"` | validar | `INVALID_BASE_PACKAGE` com `params.segment=new` |
| 6 | `artifactId="1app"` (inválido) e `groupId=""` | validar | **duas** violações, na ordem dos campos |
| 7 | `artifactId="3d-viewer"` (começa com dígito) | validar | `INVALID_ARTIFACT_ID` |
| 8 | descrição com quebra de linha | validar | `INVALID_DESCRIPTION` |
| 9 | descrição `"API <beta> & cia"` | validar | válido; escapar é responsabilidade do contexto ([ENG-CTX-01 #4](#etapa-3--montar-contexto-eng-ctx)) |

#### ENG-VAL-02 — Estilo e módulos
> **Como** usuário, **quero** escolher a arquitetura e os módulos sem gerar pacotes inválidos (BR-010 a BR-012).

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | `HEXAGONAL_MODULAR`, módulos `["Pedidos", " pagamentos "]` | validar | válido; módulos `pedidos`, `pagamentos` (ordem preservada) |
| 2 | `HEXAGONAL_MODULAR`, módulos `[]` | validar | `MODULES_REQUIRED` |
| 3 | `HEXAGONAL_MODULAR`, módulos `["shared"]` | validar | `RESERVED_MODULE_NAME` com `params.name=shared` |
| 4 | módulos `["pedidos", "PEDIDOS"]` | validar | `DUPLICATED_MODULE_NAME` |
| 5 | 21 módulos | validar | `TOO_MANY_MODULES` |
| 6 | `HEXAGONAL_LAYERED`, agregados `[]` | validar | válido |
| 7 | `HEXAGONAL_LAYERED`, agregados `["shared"]` | validar | válido (palavra reservada só vale para o estilo modular) |
| 8 | estilo `"ONION"` | validar | `UNKNOWN_ARCHITECTURE_STYLE` |

#### ENG-VAL-03 — Versões
> **Como** usuário, **quero** que o gerador recuse combinações que não compilam (BR-020 a BR-024).

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | Boot `3.5.3`, Java `21` | validar | válido, sem avisos |
| 2 | Boot `3.2.0`, Java `11` | validar | `INCOMPATIBLE_JAVA_VERSION` com `params.min=17` |
| 3 | Boot `2.7.18`, Java `8` | validar | válido, aviso `LEGACY_BOOT_LINE` |
| 4 | Boot `4.1.2`, Java acima do `javaMaxTested` da linha 4.1 | validar | válido, aviso `JAVA_ABOVE_TESTED` |
| 5 | Boot `3.5.0-RC1` | validar | `UNSUPPORTED_BOOT_VERSION` |
| 6 | Boot `2.6.15` | validar | `UNSUPPORTED_BOOT_VERSION` |
| 7 | Boot `9.0.0` (linha fora do catálogo) | validar | `UNSUPPORTED_BOOT_VERSION` |
| 8 | Java `10` | validar | `UNSUPPORTED_JAVA_VERSION` |

#### ENG-VAL-04 — Dependências, kits e extras
> **Como** usuário, **quero** partir de um kit e ajustar, sem montar combinações quebradas (BR-030 a BR-034).

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | kit `BASIC`, dependências exatamente as do kit | validar | válido; 6 dependências |
| 2 | kit `NONE`, dependências `[]` | validar | válido; 0 dependências |
| 3 | dependências `["flyway"]` | validar | válido; `jpa` incluída; aviso `DEPENDENCY_ADDED` com `params.added=jpa`, `params.by=flyway` |
| 4 | dependências `["springdoc"]` | validar | `web` incluída; aviso `DEPENDENCY_ADDED` |
| 5 | dependências `["kafka"]` (não existe) | validar | `UNKNOWN_DEPENDENCY` |
| 6 | dependência existente, fora da faixa da versão escolhida | validar | `DEPENDENCY_NOT_AVAILABLE` com `params.boot` |
| 7 | extra `docker-compose` sem `jpa` | validar | `EXTRA_REQUIRES_DEPENDENCY` com `params.requires=jpa` |
| 8 | dependências `["web", "web"]` | validar | válido; `web` uma vez só |
| 9 | kit `"PREMIUM"` (fora do registro `Kits`) ou vazio | validar | `UNKNOWN_KIT` com `params.value` |

**Pronto quando:** `SpecValidatorTest` (domínio puro, catálogo montado em memória) cobre ENG-VAL-01 a 04.

---

## Etapa 2 — Resolver o `BootProfile` (`ENG-BPR`)
**Status:** ⏳ a implementar.
Regras: [BR-020](../domain/BUSINESS_RULES.md#versões), [BR-030](../domain/BUSINESS_RULES.md#dependências-e-kits), [BR-041](../domain/BUSINESS_RULES.md#fitness-functions).

Toda diferença entre gerações do Spring Boot mora aqui, em dados (`boot-profiles.yaml`), e nunca nos templates. O `BootProfile` responde: qual o Java mínimo e o máximo testado, se usa `javax` ou `jakarta`, qual artefato Maven corresponde a cada dependência do catálogo, quais starters de teste usar e quais versões fixar fora do BOM do Spring Boot.

**Campos**

| Campo | Tipo | Regra |
|---|---|---|
| `line` | `String` | `x.y`, único no catálogo |
| `javaMin` / `javaMaxTested` | `int` | `javaMin <= javaMaxTested` |
| `legacy` | `boolean` | gera aviso `LEGACY_BOOT_LINE` |
| `persistenceNamespace` | `javax` \| `jakarta` | usado pelas fitness functions de persistência |
| `artifacts` | `Map<DependencyId, List<MavenCoordinate>>` | dependência → artefatos (com escopo e versão quando não gerenciada pelo Boot) |
| `testArtifacts` | `TestStrategy` | `SINGLE_STARTER` (2.x/3.x) ou `PER_TECHNOLOGY` (4.x) |
| `pinnedVersions` | `Map<String, String>` | propriedades do `pom.xml` (ex.: `springdoc.version`, `archunit.version`) |

### Perfis de versão iniciais
Valores de partida para o `boot-profiles.yaml`. Os marcados com ⚠️ precisam ser confirmados pela [matriz de compilação](../operations/TESTING.md#matriz-de-compilação) antes da primeira release.

| Linha | Java mín. | Java máx. testado | Namespace | Web | Flyway | `flyway-database-postgresql` | Testes | SpringDoc |
|---|---|---|---|---|---|---|---|---|
| 2.7 (legacy) | 8 | 21 ⚠️ | `javax` | `spring-boot-starter-web` | `flyway-core` | não | starter único | `springdoc-openapi-ui` 1.8.0 |
| 3.0 | 17 | 21 ⚠️ | `jakarta` | `spring-boot-starter-web` | `flyway-core` | não | starter único | 2.1.0 ⚠️ |
| 3.1 | 17 | 21 ⚠️ | `jakarta` | `spring-boot-starter-web` | `flyway-core` | não | starter único | 2.2.0 ⚠️ |
| 3.2 | 17 | 21 ⚠️ | `jakarta` | `spring-boot-starter-web` | `flyway-core` | não | starter único | 2.5.0 ⚠️ |
| 3.3 | 17 | 22 ⚠️ | `jakarta` | `spring-boot-starter-web` | `flyway-core` | sim | starter único | 2.6.0 ⚠️ |
| 3.4 | 17 | 23 ⚠️ | `jakarta` | `spring-boot-starter-web` | `flyway-core` | sim | starter único | 2.8.x ⚠️ |
| 3.5 | 17 | 24 ⚠️ | `jakarta` | `spring-boot-starter-web` | `flyway-core` | sim | starter único | 2.8.x ⚠️ |
| 4.0 | 17 | 25 ⚠️ | `jakarta` | `spring-boot-starter-webmvc` | `spring-boot-starter-flyway` | sim | por tecnologia | 3.0.x ⚠️ |
| 4.1 | 17 | 25 | `jakarta` | `spring-boot-starter-webmvc` | `spring-boot-starter-flyway` | sim | por tecnologia | 3.2.0 |

A linha 4.1 com Java 25 é a combinação usada pelo Hexbank e pelo Liva, por isso não tem ⚠️. ArchUnit: `1.5.1` em todas as linhas (versão mais recente estável; ⚠️ confirmar suporte a Java acima de 25).

#### ENG-BPR-01 — Perfil por versão
| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | Boot `3.5.3` | resolver | perfil `3.5` |
| 2 | Boot `4.1.2`, dependência `web` | resolver artefatos | `org.springframework.boot:spring-boot-starter-webmvc` |
| 3 | Boot `3.5.3`, dependência `web` | resolver artefatos | `org.springframework.boot:spring-boot-starter-web` |
| 4 | Boot `3.2.12`, dependência `flyway` | resolver artefatos | só `flyway-core` |
| 5 | Boot `3.3.0`, dependência `flyway` | resolver artefatos | `flyway-core` + `flyway-database-postgresql` |
| 6 | Boot `4.1.2`, dependências `web`, `jpa` | resolver testes | `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` |
| 7 | Boot `4.1.2`, sem dependências | resolver testes | `spring-boot-starter-test` |
| 8 | Boot `3.5.3`, `security` | resolver testes | `spring-boot-starter-test` + `spring-security-test` |
| 9 | Boot `2.7.18`, `springdoc` | resolver artefatos | `org.springdoc:springdoc-openapi-ui:${springdoc.version}`, propriedade `1.8.0` |

**Pronto quando:** `ProfileResolverTest` cobre #1 a #9 com o catálogo oficial.

---

## Etapa 3 — Montar contexto (`ENG-CTX`)
**Status:** ⏳ a implementar.
Regras: [BR-004](../domain/BUSINESS_RULES.md#identificação-do-projeto), [BR-005](../domain/BUSINESS_RULES.md#identificação-do-projeto), [BR-050](../domain/BUSINESS_RULES.md#geração), [ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md) (decisão no motor, nunca no template).

Junta `ProjectSpec` + `BootProfile` num mapa somente-leitura com tudo que os templates usam, já calculado e escapado. A lista completa de variáveis é contrato: [TEMPLATE_FORMAT — Contexto](../contracts/TEMPLATE_FORMAT.md#contexto-disponível-nos-templates).

#### ENG-CTX-01 — Contexto completo e seguro
| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | pacote `com.acme.pedidoapi` | montar | `basePackage=com.acme.pedidoapi`, `basePackagePath=com/acme/pedidoapi` |
| 2 | Boot 4.1, kit `BASIC` | montar | `dependencies` ordenadas por `groupId:artifactId`, já com os artefatos do perfil; `has.jpa=true`, `has.security=false` |
| 3 | Java `8` | montar | `javaVersionProperty=1.8`; Java `21` → `21` |
| 4 | descrição `API <beta> & cia` | montar | `descriptionXml=API &lt;beta&gt; &amp; cia`; `descriptionMarkdown` sem alteração de `<`/`>` mas com `\` antes de `*` e `_` |
| 5 | módulos `pedidos`, `pagamentos` | montar | `modules` = lista de `{name, packageName, packagePath, displayName}` na ordem do pedido |
| 6 | dois contextos do mesmo pedido | comparar | iguais (determinismo, BR-050) |

**Pronto quando:** `ContextBuilderTest` cobre #1 a #6.

---

## Etapa 4 — Planejar arquivos (`ENG-PLN`)
**Status:** ⏳ a implementar. Depende do aceite do [ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md).
Regras: [BR-035](../domain/BUSINESS_RULES.md#dependências-e-kits), [BR-040 a BR-043](../domain/BUSINESS_RULES.md#fitness-functions), [BR-052](../domain/BUSINESS_RULES.md#geração), [BR-054](../domain/BUSINESS_RULES.md#geração), [BR-055](../domain/BUSINESS_RULES.md#geração).

Lê o `manifest.yaml` do estilo e o `common`, avalia as condições (`when`) contra o contexto, expande as entradas repetidas por módulo (`forEach`) e acrescenta as classes de fitness functions escolhidas. O resultado é uma lista **ordenada por caminho de destino** de `PlannedFile(template, target, executable, lineEnding)`. Não lê o conteúdo dos templates.

#### ENG-PLN-01 — Plano por estilo
| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | `HEXAGONAL_MODULAR`, módulos `pedidos`, `pagamentos`, kit `NONE`, fitness ligada | planejar | contém `bootstrap/<App>.java`, os `package-info` de `shared` e de cada módulo (lista em [GENERATED_PROJECT](../contracts/GENERATED_PROJECT.md#hexagonal_modular)), `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` |
| 2 | mesmo pedido com um módulo a mais | planejar | +19 `package-info` (um por pacote do módulo, incluindo os intermediários) |
| 3 | `HEXAGONAL_LAYERED`, agregados `paciente` | planejar | contém `domain/model/paciente/package-info.java`; não contém `bootstrap/` |
| 4 | sem `flyway` | planejar | não contém `db/migration/V1__baseline.sql` |
| 5 | extra `docker-compose` | planejar | contém `docker-compose.yml` e `.env.example` |
| 6 | qualquer pedido | planejar | todos os caminhos relativos, sem `..`, com `/` (BR-052) |
| 7 | template com caminho `../fora.txt` (manifest de teste) | planejar | `IllegalStateException` `INVALID_TEMPLATE_PATH` |
| 8 | qualquer pedido | planejar | cada diretório Java gerado tem exatamente um `package-info.java` (BR-054) |

#### ENG-PLN-02 — Escolha das fitness functions
> **Como** usuário, **quero** receber só as regras que fazem sentido para o meu projeto, **para** o build não falhar por regras irrelevantes (BR-041).

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | `HEXAGONAL_MODULAR`, kit `NONE` | planejar | `HexagonalLayersTest`, `DomainPurityTest`, `PortsTest`, `CodingRulesTest`, `ModuleBoundariesTest`; sem `AdaptersTest` e sem `PersistenceTest` |
| 2 | `HEXAGONAL_LAYERED`, kit `BASIC` | planejar | `HexagonalLayersTest`, `DomainPurityTest`, `PortsTest`, `AdaptersTest`, `PersistenceTest`, `CodingRulesTest`; sem `ModuleBoundariesTest` |
| 3 | fitness desligada | planejar | nenhuma classe em `architecture/`, sem `archunit.properties`, sem dependência ArchUnit no contexto |
| 4 | fitness ligada | planejar | `src/test/resources/archunit.properties` presente (BR-043) |

**Pronto quando:** `FilePlannerTest` cobre ENG-PLN-01 e 02, comparando o plano com listas esperadas em arquivo (`src/test/resources/expected-plans/*.txt`).

---

## Etapa 5 — Renderizar (`ENG-RND`)
**Status:** ⏳ a implementar.
Regras: [BR-050](../domain/BUSINESS_RULES.md#geração), [BR-051](../domain/BUSINESS_RULES.md#geração), [BR-053](../domain/BUSINESS_RULES.md#geração).

Aplica o Mustache a cada `PlannedFile` e devolve um `GeneratedProject` em memória. Arquivos marcados como binários (ex.: futuras imagens) são copiados sem renderização. Nada é gravado em disco nesta etapa.

**Contrato**

    public interface TemplateRenderer { String render(String template, Map<String, Object> context); }
    // variável ausente no contexto → IllegalStateException (bug de template), nunca string vazia silenciosa

#### ENG-RND-01 — Renderização determinística
| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | mesmo pedido renderizado duas vezes | comparar bytes | idênticos (BR-050) |
| 2 | template com `\r\n` | renderizar | saída com `\n` (BR-051) |
| 3 | `mvnw.cmd` | renderizar | saída com `\r\n` |
| 4 | template usa `{{naoExiste}}` | renderizar | `IllegalStateException` citando arquivo e variável |
| 5 | pedido de referência (golden) | renderizar | igual, byte a byte, ao projeto esperado em `src/test/resources/golden/` |

**Pronto quando:** `ProjectRendererTest` cobre #1 a #4; `GoldenProjectTest` cobre #5 para cada estilo.

---

## Etapa 6 — Exportar (`ENG-EXP`)
**Status:** ⏳ a implementar.
Regras: [BR-053](../domain/BUSINESS_RULES.md#geração), [BR-060 a BR-064](../domain/BUSINESS_RULES.md#exportação).

Dois adapters de saída para a mesma porta. O caso de uso chama `check` **antes** de renderizar, para falhar rápido, e `write` só depois de tudo renderizado.

**Contrato**

    public interface ProjectExporter {
        ExportFormat format();
        List<Violation> check(ExportTarget target, String artifactId);          // destino, existência, escrita
        ExportResult write(GeneratedProject project, ExportTarget target) throws IOException;
    }
    public record ExportTarget(ExportFormat format, Path destination, boolean overwrite) {}

#### ENG-EXP-01 — ZIP igual ao Initializr
| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | destino `/tmp/x`, `artifactId=pedido-api` | exportar ZIP | cria `/tmp/x/pedido-api.zip`; todas as entradas começam com `pedido-api/` |
| 2 | ZIP gerado | inspecionar | entrada `pedido-api/mvnw` com modo Unix `755`; demais `644` |
| 3 | mesmo pedido exportado duas vezes | comparar arquivos | bytes idênticos (datas fixas nas entradas, BR-050) |
| 4 | `pedido-api.zip` já existe, `overwrite=false` | `check` | `TARGET_EXISTS` |
| 5 | `pedido-api.zip` já existe, `overwrite=true` | exportar | substitui o arquivo |
| 6 | destino inexistente | `check` | `DESTINATION_NOT_FOUND` |
| 7 | falha de E/S no meio da gravação | exportar | nenhum `.zip` parcial fica no destino (grava em temporário e renomeia) |

#### ENG-EXP-02 — Pasta, sem nunca sobrescrever
| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | destino `/tmp/x`, `/tmp/x/pedido-api` não existe | exportar pasta | cria `/tmp/x/pedido-api/` com todos os arquivos |
| 2 | `/tmp/x/pedido-api` existe e está vazia | exportar pasta | usa a pasta |
| 3 | `/tmp/x/pedido-api` existe com 1 arquivo | `check` | `TARGET_NOT_EMPTY`; nada é alterado |
| 4 | sistema POSIX | exportar | `mvnw` executável |
| 5 | falha de E/S no meio da gravação | exportar | pasta temporária removida; `/tmp/x/pedido-api` não existe |
| 6 | destino sem permissão de escrita | `check` | `DESTINATION_NOT_WRITABLE` |

**Pronto quando:** `ZipProjectExporterTest` e `FolderProjectExporterTest` (com `@TempDir`) cobrem ENG-EXP-01 e 02.

---

## Casos de uso (`ENG-UC`)
Contratos completos: [ENGINE_API](../contracts/ENGINE_API.md).

#### ENG-UC-01 — Gerar projeto
**Status:** ⏳ a implementar. Classe: `application/usecase/GenerateProject`.

Ordem: validar (etapa 1) → se inválido, devolve `Failure` → resolver perfil (2) → `exporter.check` (falha rápida) → contexto (3) → plano (4) → renderizar (5) → `exporter.write` (6) → `Success`.

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | pedido válido, destino válido, ZIP | gerar | `Success` com caminho do ZIP, nº de arquivos, avisos da validação |
| 2 | pedido com 2 violações e destino inválido | gerar | `Failure` com as 2 violações de entrada **e** a de destino; nada renderizado |
| 3 | template quebrado (bug) | gerar | exceção propagada; nada gravado (BR-053) |
| 4 | pedido válido, pasta já ocupada | gerar | `Failure` com `TARGET_NOT_EMPTY`; nada renderizado |

#### ENG-UC-02 — Pré-visualizar projeto
**Status:** ⏳ a implementar. Classe: `application/usecase/PreviewProject`.

Executa as etapas 1 a 4 e devolve a árvore de caminhos, as dependências resolvidas (com coordenadas Maven) e as fitness functions escolhidas, sem renderizar nem gravar. Alimenta o resumo da tela 4 do desktop.

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | pedido válido | pré-visualizar | mesma lista de caminhos que o `GenerateProject` grava |
| 2 | pedido inválido | pré-visualizar | `Failure` com as violações |

#### ENG-UC-03 — Consultar catálogo
**Status:** ⏳ a implementar. Classe: `application/usecase/BrowseCatalog`.

Alimenta as telas 1 a 3: estilos, versões do Spring Boot ([ENG-VER](#versões-do-spring-boot-eng-ver)), versões Java válidas para uma versão do Boot (com padrão e marcação LTS), kits, dependências disponíveis para uma versão do Boot e fitness functions aplicáveis.

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | Boot `3.5.3` | Java disponíveis | `17, 21, 22, 23, 24` + as acima do testado marcadas `aboveTested`; padrão `21` |
| 2 | Boot `2.7.18` | Java disponíveis | começa em `8`; padrão `21` |
| 3 | — | kits | `NONE` e `BASIC`, com a lista de dependências de cada um |
| 4 | Boot `4.1.2` | dependências | todas as do catálogo cuja faixa inclua `4.1.2` |

**Pronto quando:** `GenerateProjectTest`, `PreviewProjectTest` e `BrowseCatalogTest` (portas de saída com fakes) cobrem os casos acima; `EngineEndToEndTest` gera um ZIP real com o catálogo oficial.
