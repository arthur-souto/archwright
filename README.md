# Archwright

**Gerador de projetos Spring Boot com a arquitetura certa desde o primeiro commit, e fitness functions que mantêm ela certa.**

*Architecture-first project generator for Spring Boot — hexagonal today, more styles soon. Fitness functions included.*

> 🚧 **Em construção.** Estamos na [Fase 0](docs/implementation/ROADMAP.md#fase-0--fundação). Ainda não há release.

## O que é

O Spring Initializr gera um projeto vazio: a estrutura de pacotes e as regras de arquitetura ficam por sua conta. O Archwright faz o mesmo papel (você escolhe versões e dependências e recebe um ZIP ou uma pasta), mas o projeto já sai:

- **com a arquitetura montada:** pacotes de domínio, aplicação (portas e casos de uso) e adapters, cada um com um `package-info.java` explicando a responsabilidade dele;
- **com fitness functions:** testes ArchUnit que quebram o build quando alguém viola a arquitetura (um controller acessando repositório, JPA dentro do domínio, um módulo usando a parte interna de outro);
- **compilando de primeira:** toda combinação de versão do Spring Boot, estilo e kit é gerada e testada no CI do Archwright.

## Estilos de arquitetura

| Estilo | Para quê | Status |
|---|---|---|
| Hexagonal modular | monólito modular: vários módulos, cada um com hexagonal própria, shared kernel e bootstrap | Fase 1 |
| Hexagonal em camadas | uma hexagonal só: `domain`, `application`, `infrastructure` | Fase 1 |
| Clean, Onion, camadas tradicionais | — | [Fase 5](docs/implementation/ROADMAP.md#fase-5--novos-estilos-de-arquitetura) |

## Como vai funcionar

Um app desktop (Windows, macOS, Linux, sem precisar de Java instalado) com 4 passos:

1. **Projeto:** nome, group, pacote e estilo de arquitetura.
2. **Versões:** Spring Boot (do 2.7 ao mais recente, lista direto do Maven Central) e Java, só em combinações válidas.
3. **Dependências:** kit Básico (Web, Validation, Actuator, JPA + PostgreSQL, Flyway, Lombok) ou Nenhuma, com ajuste fino, e as fitness functions.
4. **Exportar:** ZIP ou pasta, em qualquer diretório.

## Documentação

| Documento | Conteúdo |
|---|---|
| [Roadmap](docs/implementation/ROADMAP.md) | fases, critérios de pronto e próxima tarefa |
| [Regras de negócio](docs/domain/BUSINESS_RULES.md) | regras `BR-*` testáveis |
| [Arquitetura](docs/architecture/ARCHITECTURE.md) | módulos, pacotes e a especificação de cada etapa do motor |
| [API do engine](docs/contracts/ENGINE_API.md) | contrato Java usado pelas interfaces |
| [Formato de templates](docs/contracts/TEMPLATE_FORMAT.md) | manifest, contexto e catálogos |
| [Projeto gerado](docs/contracts/GENERATED_PROJECT.md) | o que cada estilo gera e o catálogo de fitness functions |
| [Testes](docs/operations/TESTING.md) | pirâmide, golden files e matriz de compilação |
| [ADRs](docs/adr/) | decisões de arquitetura |

## Inspiração

A estrutura dos estilos vem de dois projetos reais: [Hexbank](https://github.com/arthur-souto/Hexbank) (monólito modular hexagonal) e [Liva](https://github.com/arthur-souto/liva) (hexagonal em camadas).

## Licença

Apache 2.0 (arquivo `LICENSE` a adicionar na Fase 0).

*Spring e Spring Boot são marcas registradas da Broadcom Inc. O Archwright é um projeto independente, sem vínculo com o projeto Spring.*
