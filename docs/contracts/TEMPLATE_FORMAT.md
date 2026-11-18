# Contrato — Formato de templates e catálogos

Define como os estilos de arquitetura e os catálogos são descritos em arquivos ([ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md)). Adicionar um novo estilo de arquitetura deve exigir **apenas** um novo diretório de templates, uma entrada em `styles` e, se precisar, regras novas em `fitness-rules.yaml`.

Tudo fica em `archwright-engine/src/main/resources/archwright/`.

## Estrutura

```text
archwright/
├── catalog/
│   ├── styles.yaml
│   ├── boot-profiles.yaml
│   ├── dependencies.yaml
│   ├── kits.yaml
│   ├── extras.yaml
│   ├── fitness-rules.yaml
│   └── boot-versions.txt          # fallback embutido (ENG-VER), uma versão por linha
├── templates/
│   ├── common/                    # arquivos de todos os estilos
│   │   ├── manifest.yaml
│   │   ├── pom.xml.mustache
│   │   ├── mvnw / mvnw.cmd / maven-wrapper.properties
│   │   └── ...
│   ├── hexagonal-modular/
│   │   ├── manifest.yaml
│   │   └── *.mustache
│   └── hexagonal-layered/
│       ├── manifest.yaml
│       └── *.mustache
└── fitness/
    └── *.java.mustache            # uma classe de teste por grupo de regras
```

## Manifest

Cada estilo tem um `manifest.yaml`; o engine sempre junta `common/manifest.yaml` com o do estilo.

```yaml
style: HEXAGONAL_MODULAR

files:
  - template: Application.java.mustache
    target: src/main/java/{{basePackagePath}}/bootstrap/{{applicationClass}}.java

  - template: docker-compose.yml.mustache
    target: docker-compose.yml
    when: [extra.docker-compose]           # todas as condições precisam ser verdadeiras

  - template: V1__baseline.sql.mustache
    target: src/main/resources/db/migration/V1__baseline.sql
    when: [dep.flyway]

  - template: mvnw
    target: mvnw
    executable: true
    render: false                           # copiado sem Mustache

  - template: mvnw.cmd
    target: mvnw.cmd
    render: false
    lineEnding: CRLF                         # padrão: LF (BR-051)

packages:                                   # vira um package-info.java por item (BR-054)
  - package: ""                              # o próprio pacote base
    description: Raiz do projeto.
  - package: bootstrap
    description: Composition root, sobe o Spring e liga os módulos. Não contém regra de negócio.
  - package: shared.domain
    description: Value objects e tipos de domínio compartilhados. Java puro.

  - forEach: modules                        # repete para cada módulo (variável {{module.*}})
    package: "{{module.packageName}}.application.port.in"
    description: Portas de entrada (use cases e queries) expostas pelo módulo {{module.name}}.
```

**Regras do manifest**
- `target` e `package` são templates Mustache avaliados com o mesmo [contexto](#contexto-disponível-nos-templates).
- `when` aceita apenas nomes de **flags** do contexto, opcionalmente negados com `!` (ex.: `!dep.jpa`). Não existe `or` nem comparação: se precisar, o engine cria uma flag nova ([ADR-0003](../adr/0003-templates-e-catalogos-como-dados.md) item 5).
- `forEach` aceita `modules` (estilo modular) ou `aggregates` (estilo em camadas).
- O caminho final passa pela verificação do BR-052; se houver violação, é erro de template.
- Dois itens com o mesmo `target` final: erro de template.

## Contexto disponível nos templates

Montado pela [etapa 3](../architecture/ARCHITECTURE.md#etapa-3--montar-contexto-eng-ctx). Qualquer variável fora desta lista é erro de renderização ([ENG-RND-01 #4](../architecture/ARCHITECTURE.md#etapa-5--renderizar-eng-rnd)).

| Variável | Exemplo | Observação |
|---|---|---|
| `artifactId`, `groupId` | `pedido-api`, `com.acme` | |
| `basePackage`, `basePackagePath` | `com.acme.pedidoapi`, `com/acme/pedidoapi` | |
| `applicationClass` | `PedidoApiApplication` | BR-005 |
| `descriptionXml`, `descriptionMarkdown`, `hasDescription` | | já escapados |
| `style.id`, `style.label` | `HEXAGONAL_MODULAR`, `Hexagonal modular` | |
| `boot.version`, `boot.line` | `4.1.2`, `4.1` | |
| `java.version`, `java.property` | `25`, `25` (`1.8` para Java 8) | |
| `kit.id`, `kit.label` | `BASIC`, `Básico` | |
| `dependencies` | lista de `{groupId, artifactId, version, scope, optional}` | ordenada; `version` só quando não gerenciada |
| `testDependencies` | mesma estrutura | |
| `properties` | lista de `{name, value}` | ex.: `archunit.version` |
| `annotationProcessors` | lista de `{groupId, artifactId, versionProperty}` | ex.: Lombok |
| `modules`, `aggregates` | lista de `{name, packageName, packagePath, displayName}` | |
| `persistence.namespace` | `jakarta` | `javax` no Boot 2.7 |
| `database.name` | `pedido_api_db` | só com `dep.jpa` |
| `fitness.rules` | lista de `{id, description}` | para o README |
| **Flags** | | usadas em `when` e em seções `{{#flag}}` |
| `dep.<id>` | `dep.jpa`, `dep.web`… | uma por dependência do catálogo |
| `extra.<id>` | `extra.docker-compose` | |
| `fitness.enabled` | | BR-040 |
| `style.modular`, `style.layered` | | |
| `boot.v2`, `boot.v3`, `boot.v4` | | geração do Spring Boot |
| `tests.perTechnology` | | Boot 4.x |

## Catálogos

### `styles.yaml`
```yaml
- id: HEXAGONAL_MODULAR
  label: Hexagonal modular
  description: Monólito modular. Cada módulo tem domain, application e adapter próprios, com shared kernel e bootstrap.
  templateDir: hexagonal-modular
  modulesLabel: Módulos
  modulesRequired: true
  reservedModuleNames: [shared, bootstrap, architecture]
```

### `boot-profiles.yaml`
```yaml
- line: "4.1"
  javaMin: 17
  javaMaxTested: 25
  legacy: false
  persistenceNamespace: jakarta
  tests: PER_TECHNOLOGY
  pinnedVersions:
    springdoc.version: 3.2.0
    archunit.version: 1.4.1
  artifacts:
    web:       [{ g: org.springframework.boot, a: spring-boot-starter-webmvc }]
    flyway:    [{ g: org.springframework.boot, a: spring-boot-starter-flyway },
                { g: org.flywaydb, a: flyway-database-postgresql }]
    jpa:       [{ g: org.springframework.boot, a: spring-boot-starter-data-jpa },
                { g: org.postgresql, a: postgresql, scope: runtime }]
    springdoc: [{ g: org.springdoc, a: springdoc-openapi-starter-webmvc-ui, v: "${springdoc.version}" }]
  testArtifacts:
    web: [{ g: org.springframework.boot, a: spring-boot-starter-webmvc-test }]
    jpa: [{ g: org.springframework.boot, a: spring-boot-starter-data-jpa-test }]
```
Dependência do catálogo sem entrada em `artifacts` num perfil = não disponível naquela linha (`DEPENDENCY_NOT_AVAILABLE`).

### `dependencies.yaml`
```yaml
- id: flyway
  label: Flyway
  description: Migrations versionadas do banco.
  group: Dados
  implies: [jpa]          # BR-033
```

### `kits.yaml`
```yaml
- id: NONE
  label: Nenhuma
  dependencies: []
- id: BASIC
  label: Básico
  dependencies: [web, validation, actuator, jpa, flyway, lombok]
```

### `extras.yaml`
```yaml
- id: docker-compose
  label: docker-compose.yml com PostgreSQL
  requires: [jpa]
- id: github-actions
  label: GitHub Actions (CI)
  requires: []
```

### `fitness-rules.yaml`
```yaml
- testClass: PersistenceTest
  template: PersistenceTest.java.mustache
  when: [dep.jpa]
  rules:
    - id: FF-PER-01
      description: "@Entity só no pacote de entidades de persistência."
    - id: FF-PER-02
      description: Repositórios Spring Data só no pacote jpa de persistência.
```
O catálogo completo das regras está em [GENERATED_PROJECT — Fitness functions](GENERATED_PROJECT.md#fitness-functions).
