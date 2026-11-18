# Contrato — API Java do engine

É a única superfície que o desktop (e qualquer interface futura) pode usar ([AW-FF-05](../architecture/ARCHITECTURE.md#regras-de-dependência-fitness-functions-do-próprio-engine)). Fica em `io.github.arthursouto.archwright.engine.application.port.in`, mais a fábrica em `...engine.config`.

**Versionamento:** a API segue SemVer a partir da `1.0.0`. Adicionar campo opcional, código de violação ou aviso é *minor*; remover ou mudar significado é *major*. Até a `1.0.0`, pode mudar entre fases do [roadmap](../implementation/ROADMAP.md).

**Concorrência:** o engine é *stateless* e seguro para uso por várias threads. Todos os métodos são bloqueantes. `bootVersions()` pode acessar a rede (até 8 s), então interfaces gráficas devem chamá-lo fora da thread de UI.

## Criação

    ArchwrightEngine engine = ArchwrightEngine.create();             // padrões: cache em ~/.archwright/cache
    ArchwrightEngine engine = ArchwrightEngine.builder()
            .cacheDirectory(Path.of("/tmp/aw-cache"))                // testes e instalações portáteis
            .offline(true)                                           // nunca acessa a rede (CI, testes)
            .build();

    GenerateProjectUseCase generate = engine.generateProject();
    PreviewProjectQuery    preview  = engine.previewProject();
    CatalogQuery           catalog  = engine.catalog();

## Pedido

    public record ProjectRequest(
            String artifactId,             // BR-001
            String groupId,                // BR-002
            String basePackage,            // BR-003; null ou "" = padrão
            String description,            // BR-004; pode ser null
            String architectureStyle,      // BR-010: "HEXAGONAL_MODULAR" | "HEXAGONAL_LAYERED"
            List<String> modules,          // BR-011/012: módulos (modular) ou agregados (layered)
            String bootVersion,            // BR-020
            String javaVersion,            // BR-021: "8", "11", "17", "21", "25"...
            String kit,                    // BR-031/032: "NONE" | "BASIC" (informativo)
            List<String> dependencies,     // BR-030: ids do catálogo
            List<String> extras,           // BR-034: "docker-compose", "github-actions"
            boolean fitnessFunctions) {}   // BR-040: true por padrão na interface

    public record ExportTarget(ExportFormat format, Path destination, boolean overwrite) {}
    public enum ExportFormat { ZIP, FOLDER }

Campos de texto nulos são tratados como vazios. Listas nulas, como vazias.

## Gerar

    public interface GenerateProjectUseCase {
        GenerationResult generate(ProjectRequest request, ExportTarget target);
    }

    public sealed interface GenerationResult {
        record Success(Path output, int fileCount, long totalBytes, List<Warning> warnings) implements GenerationResult {}
        record Failure(List<Violation> violations) implements GenerationResult {}
    }

- `output`: caminho absoluto do `.zip` ou da pasta criada (BR-064).
- Em `Failure`, nada foi gravado (BR-053).
- Exceções só em bugs ou E/S inesperada (BR-071). A interface deve mostrar uma mensagem genérica e o detalhe técnico num painel de log.

## Pré-visualizar

    public interface PreviewProjectQuery {
        PreviewResult preview(ProjectRequest request);
    }

    public sealed interface PreviewResult {
        record Preview(List<String> paths,                         // ordenados, relativos à raiz do projeto
                       List<ResolvedDependency> dependencies,      // coordenadas Maven finais
                       List<FitnessRuleSummary> fitnessRules,
                       List<Warning> warnings) implements PreviewResult {}
        record Failure(List<Violation> violations) implements PreviewResult {}
    }

    public record ResolvedDependency(String id, String groupId, String artifactId, String version, String scope) {}
    // version == null quando gerenciada pelo BOM do Spring Boot
    public record FitnessRuleSummary(String id, String testClass, String description) {}

## Catálogo

    public interface CatalogQuery {
        List<ArchitectureStyleOption> architectureStyles();
        BootVersions bootVersions();                                   // ENG-VER
        List<JavaVersionOption> javaVersions(String bootVersion);      // vazio se a versão não for suportada
        List<KitOption> kits();
        List<DependencyOption> dependencies(String bootVersion);
        List<ExtraOption> extras();
        List<FitnessRuleSummary> fitnessRules(String style, List<String> dependencies, String bootVersion);
        String suggestBasePackage(String groupId, String artifactId); // BR-003; "" se não der para sugerir
    }

    public record ArchitectureStyleOption(String id, String label, String description,
                                          String modulesLabel, boolean modulesRequired) {}
    // modulesLabel: "Módulos" (modular) | "Agregados" (layered)
    // montado a partir de duas fontes: styles.yaml (id, label, description, modulesLabel)
    // e a estratégia do estilo (modulesRequired)

    public record BootVersions(List<BootVersionOption> versions, VersionOrigin origin, Instant fetchedAt) {}
    // fetchedAt: gravação do cache ou consulta remota; null quando origin == EMBEDDED
    public record BootVersionOption(String version, String line, boolean legacy, boolean latestOfLine) {}
    public enum VersionOrigin { REMOTE, CACHE, STALE_CACHE, EMBEDDED }

    public record JavaVersionOption(String version, boolean lts, boolean aboveTested, boolean isDefault) {}
    public record KitOption(String id, String label, List<String> dependencies) {}
    // vem da estratégia do kit (id, label, dependências padrão); a interface traduz o rótulo por id
    public record DependencyOption(String id, String label, String description, String group,
                                   List<String> implies) {}           // implies: BR-033
    public record ExtraOption(String id, String label, List<String> requires) {}

## Violações

    public record Violation(String code, String field, Map<String, String> params) {}

| `code` | `field` | `params` | Mensagem sugerida (pt-BR) |
|---|---|---|---|
| `INVALID_ARTIFACT_ID` | `artifactId` | — | Use letras minúsculas, números, `-` ou `.`, começando com letra (até 64). |
| `INVALID_GROUP_ID` | `groupId` | — | Use segmentos em minúsculas separados por ponto, como `com.empresa`. |
| `INVALID_BASE_PACKAGE` | `basePackage` | `segment` | O trecho `{segment}` não pode ser usado como nome de pacote Java. |
| `INVALID_DESCRIPTION` | `description` | `max` | Descrição deve ter uma linha só, até `{max}` caracteres. |
| `UNKNOWN_ARCHITECTURE_STYLE` | `architectureStyle` | `value` | Estilo de arquitetura desconhecido. |
| `MODULES_REQUIRED` | `modules` | — | Informe ao menos um módulo. |
| `TOO_MANY_MODULES` | `modules` | `max` | No máximo `{max}` módulos. |
| `INVALID_MODULE_NAME` | `modules` | `name` | `{name}` não é um nome de pacote válido. |
| `DUPLICATED_MODULE_NAME` | `modules` | `name` | O módulo `{name}` aparece mais de uma vez. |
| `RESERVED_MODULE_NAME` | `modules` | `name` | `{name}` é reservado pelo gerador. |
| `UNSUPPORTED_BOOT_VERSION` | `bootVersion` | `value` | Versão do Spring Boot não suportada. |
| `UNSUPPORTED_JAVA_VERSION` | `javaVersion` | `value` | Versão do Java não suportada. |
| `INCOMPATIBLE_JAVA_VERSION` | `javaVersion` | `min`, `boot` | Spring Boot `{boot}` exige Java `{min}` ou superior. |
| `UNKNOWN_KIT` | `kit` | `value` | Kit de dependências desconhecido. |
| `UNKNOWN_DEPENDENCY` | `dependencies` | `id` | Dependência `{id}` desconhecida. |
| `DEPENDENCY_NOT_AVAILABLE` | `dependencies` | `id`, `boot` | `{id}` não está disponível no Spring Boot `{boot}`. |
| `UNKNOWN_EXTRA` | `extras` | `id` | Extra `{id}` desconhecido. |
| `EXTRA_REQUIRES_DEPENDENCY` | `extras` | `id`, `requires` | `{id}` precisa da dependência `{requires}`. |
| `DESTINATION_NOT_FOUND` | `destination` | `path` | A pasta de destino não existe. |
| `DESTINATION_NOT_WRITABLE` | `destination` | `path` | Sem permissão de escrita na pasta de destino. |
| `TARGET_EXISTS` | `destination` | `path` | Já existe um arquivo com esse nome. Deseja substituir? |
| `TARGET_NOT_EMPTY` | `destination` | `path` | A pasta `{path}` já existe e não está vazia. |

## Avisos

    public record Warning(String code, Map<String, String> params) {}

| `code` | `params` | Mensagem sugerida (pt-BR) |
|---|---|---|
| `LEGACY_BOOT_LINE` | `line` | Spring Boot `{line}` está fora do suporte gratuito. |
| `JAVA_ABOVE_TESTED` | `java`, `maxTested` | Java `{java}` ainda não foi testado com esta versão do Spring Boot (testado até `{maxTested}`). |
| `DEPENDENCY_ADDED` | `added`, `by` | `{added}` foi incluída porque `{by}` precisa dela. |
| `VERSIONS_OFFLINE` | `origin` | Sem conexão: usando lista de versões salva (`{origin}`). |

## Exemplo de uso (desktop)

    var request = new ProjectRequest("pedido-api", "com.acme", null, "API de pedidos",
            "HEXAGONAL_MODULAR", List.of("pedidos", "pagamentos"),
            "4.1.2", "25", "BASIC",
            List.of("web", "validation", "actuator", "jpa", "flyway", "lombok"),
            List.of("docker-compose", "github-actions"), true);

    switch (engine.generateProject().generate(request,
            new ExportTarget(ExportFormat.ZIP, Path.of(System.getProperty("user.home"), "Downloads"), false))) {
        case GenerationResult.Success s -> mostrarSucesso(s.output(), s.warnings());
        case GenerationResult.Failure f -> destacarCampos(f.violations());
    }
