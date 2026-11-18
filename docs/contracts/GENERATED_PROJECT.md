# Contrato — O que o projeto gerado contém

Este é o contrato de saída do Archwright: o que um usuário recebe ao gerar um projeto. Qualquer mudança aqui altera o comportamento visível e precisa atualizar os testes *golden* ([TESTING](../operations/TESTING.md#golden-files)).

Exemplos usam `groupId=com.acme`, `artifactId=pedido-api`, pacote `com.acme.pedidoapi`.

## Arquivos comuns a todos os estilos

| Arquivo | Quando | Conteúdo |
|---|---|---|
| `pom.xml` | sempre | parent `spring-boot-starter-parent:<boot>`, `java.version`, dependências do perfil, propriedades fixadas, plugin Spring Boot; com Lombok, `annotationProcessorPaths` no compiler plugin e exclusão no plugin Spring Boot |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | sempre (BR-035) | Maven Wrapper em modo `only-script` |
| `.gitignore` | sempre | padrão Maven + IntelliJ, Eclipse, VS Code, macOS, `.env` |
| `README.md` | sempre | estilo, versões, kit, árvore de pacotes, como rodar, lista de fitness functions e como congelar uma regra |
| `src/main/resources/application.yaml` | sempre | nome da aplicação e porta; com `jpa`: datasource por variável de ambiente, `open-in-view: false`, `ddl-auto: validate` (com Flyway) ou `none`; com `actuator`: expõe só `health` |
| `src/main/resources/db/migration/V1__baseline.sql` | `flyway` | comentário, sem DDL |
| `docker-compose.yml`, `.env.example` | extra `docker-compose` | PostgreSQL 16 com healthcheck e volume nomeado |
| `.github/workflows/ci.yml` | extra `github-actions` | `actions/setup-java` (Temurin, Java escolhido, cache Maven) + `./mvnw -B verify` |
| `src/test/resources/archunit.properties` | fitness ligada | `archRule.failOnEmptyShould=false` (BR-043) |
| `src/test/java/<base>/architecture/*Test.java` | fitness ligada | ver [Fitness functions](#fitness-functions) |

Nenhum arquivo contém data, hora, usuário da máquina ou caminho absoluto (BR-050).

**Projeto sem banco de dados:** sem `jpa` (por exemplo, kit `NONE`) o projeto não tem driver, `datasource`, configuração JPA, `V1__baseline.sql`, `docker-compose.yml` nem `.env.example`; o extra `docker-compose` exige `jpa` (BR-034) e `flyway` implica `jpa` (BR-033). Os `package-info` de persistência continuam sendo gerados, porque o esqueleto de pacotes é o mesmo em todo projeto.

## `HEXAGONAL_MODULAR`

Estilo do [Hexbank](https://github.com/arthur-souto/Hexbank): monólito modular, cada módulo com hexagonal completa.

```text
com.acme.pedidoapi
├── bootstrap/                      PedidoApiApplication (scanBasePackages + @AutoConfigurationPackage)
├── shared/                         shared kernel: todos dependem, não depende de ninguém
│   ├── domain/ (+ exceptions/)
│   ├── application/port/out/
│   └── adapter/in/http/, adapter/out/persistence/
└── <módulo>/                       um por módulo informado
    ├── domain/ (+ exceptions/)     Java puro
    ├── application/
    │   ├── port/in/ (+ command/, view/)
    │   ├── port/out/
    │   └── usecase/
    ├── adapter/
    │   ├── in/http/ (+ request/)
    │   └── out/persistence/ (+ entity/, jpa/)
    └── config/                     registra os use cases como @Bean
```

**`package-info.java` gerados** (todos, inclusive os intermediários, BR-054):
- Raiz e `bootstrap`: 2.
- `shared`: `shared`, `domain`, `domain.exceptions`, `application`, `application.port`, `application.port.out`, `adapter`, `adapter.in`, `adapter.in.http`, `adapter.out`, `adapter.out.persistence` = 11.
- Por módulo: `<m>`, `domain`, `domain.exceptions`, `application`, `application.port`, `application.port.in`, `application.port.in.command`, `application.port.in.view`, `application.port.out`, `application.usecase`, `adapter`, `adapter.in`, `adapter.in.http`, `adapter.in.http.request`, `adapter.out`, `adapter.out.persistence`, `adapter.out.persistence.entity`, `adapter.out.persistence.jpa`, `config` = 19.

Total com N módulos: `13 + 19 × N`.

## `HEXAGONAL_LAYERED`

Estilo do [Liva](https://github.com/arthur-souto/liva): uma hexagonal só, sem módulos. Os nomes de pacote são em inglês (o Liva usa `persistencia`; aqui vira `persistence`).

```text
com.acme.pedidoapi
├── PedidoApiApplication            @SpringBootApplication
├── domain/
│   └── model/ (+ <agregado>/)      Java puro
├── application/
│   ├── exception/
│   ├── port/in/
│   ├── port/out/
│   └── usecase/
└── infrastructure/
    ├── adapter/in/web/
    ├── adapter/out/persistence/ (+ entity/, jpa/)
    └── config/
```

**`package-info.java` gerados:** raiz, `domain`, `domain.model`, `application`, `application.exception`, `application.port`, `application.port.in`, `application.port.out`, `application.usecase`, `infrastructure`, `infrastructure.adapter`, `infrastructure.adapter.in`, `infrastructure.adapter.in.web`, `infrastructure.adapter.out`, `infrastructure.adapter.out.persistence`, `infrastructure.adapter.out.persistence.entity`, `infrastructure.adapter.out.persistence.jpa`, `infrastructure.config` = 18, mais 1 por agregado.

## Fitness functions

Uma classe de teste por grupo, no pacote `<base>.architecture`. Cada regra mostra o seu id na mensagem de falha (BR-042). Os padrões de pacote abaixo valem para os dois estilos; onde diferem, a tabela indica.

| Classe | Id | Regra | Entra quando |
|---|---|---|---|
| `HexagonalLayersTest` | FF-LAY-01 | Dependências só no sentido adapter/infrastructure → application → domain | sempre |
| | FF-LAY-02 | Sem ciclos entre pacotes de primeiro nível | sempre |
| `DomainPurityTest` | FF-DOM-01 | `..domain..` não depende de Spring, `javax/jakarta.persistence`, Hibernate, Lombok nem Jackson | sempre |
| `PortsTest` | FF-PRT-01 | Tipos diretamente em `..port.in` e `..port.out` são interfaces | sempre |
| | FF-PRT-02 | Só classes de `..application.usecase..` implementam interfaces de `..port.in..` | sempre |
| `AdaptersTest` | FF-ADP-01 | `..adapter.in..` não depende de `..adapter.out..` | `web` |
| | FF-ADP-02 | `@RestController`/`@Controller` só em `..adapter.in..` | `web` |
| | FF-ADP-03 | Controllers não dependem de `..application.usecase..` nem de repositórios Spring Data (falam com `port.in`) | `web` |
| `PersistenceTest` | FF-PER-01 | `@Entity` só em `..adapter.out.persistence.entity..` | `jpa` |
| | FF-PER-02 | Subtipos de `Repository` do Spring Data só em `..adapter.out.persistence.jpa..` | `jpa` |
| `ModuleBoundariesTest` | FF-MOD-01 | Um módulo só usa `application.port.in` e `domain` de outro módulo | estilo modular |
| | FF-MOD-02 | `shared` não depende de nenhum módulo | estilo modular |
| | FF-MOD-03 | Sem ciclos entre módulos | estilo modular |
| `CodingRulesTest` | FF-COD-01 | Sem `@Autowired` em campo (injeção por construtor) | sempre |
| | FF-COD-02 | Classes de `..domain..` e `..application..` não têm `@Service`, `@Component` nem `@Repository` (beans registrados em `config`) | sempre |
| | FF-COD-03 | Sem uso de `System.out` / `System.err` | sempre |

Diferenças por versão do Spring Boot: FF-DOM-01 e FF-PER-01 usam `javax.persistence` no 2.7 e `jakarta.persistence` no 3.x e 4.x (`persistence.namespace` do contexto). No Java 8 e 11, *records* não existem, e nenhuma regra os exige.

O README gerado explica como congelar uma regra num projeto existente (`FreezingArchRule.freeze(...)`) e como desligar uma classe inteira (removê-la), deixando claro que o build quebrar por violação de arquitetura é intencional ([ADR-0004](../adr/0004-fitness-functions-por-padrao.md)).

## O que **não** é gerado (Fase 1)
- Classes de negócio de exemplo: use cases, controllers, entidades, DTOs (BR-055).
- `@SpringBootTest` de contexto: com `jpa` ele exige banco no ar e quebraria o primeiro `mvn verify`. Entra junto com Testcontainers numa fase futura.
- Configuração de segurança: com `security`, o Spring Boot usa os padrões dele (usuário gerado no log).
