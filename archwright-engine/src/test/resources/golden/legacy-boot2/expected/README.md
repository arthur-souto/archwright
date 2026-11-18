# legado

Sistema legado em Spring Boot 2.7 & Java 8

Projeto gerado pelo [Archwright](https://github.com/arthur-souto/archwright), com a arquitetura montada desde o primeiro commit.

## Resumo

| | |
|---|---|
| Arquitetura | Hexagonal modular |
| Spring Boot | 2.7.18 |
| Java | 8 |
| Kit de dependências | Básico |
| Pacote base | `com.acme.legado` |

## Estrutura de pacotes

Monólito modular: cada módulo tem a sua própria hexagonal (`domain`, `application`, `adapter`, `config`).

- `bootstrap`: composition root, sobe o Spring e liga os módulos.
- `shared`: shared kernel, todos os módulos dependem dele e ele não depende de nenhum.
- `core`: módulo Core.

Cada pacote tem um `package-info.java` explicando a responsabilidade dele.

## Como rodar

```shell
./mvnw spring-boot:run
```

Para compilar e testar:

```shell
./mvnw verify
```

## Fitness functions

Os testes de arquitetura ficam em `src/test/java/com/acme/legado/architecture` e rodam no `./mvnw verify`.
O build quebrar quando alguém viola a arquitetura é intencional: é isso que mantém a estrutura certa ao longo do tempo.

Regras ativas neste projeto:

- `FF-LAY-01` (HexagonalLayersTest): Dependências só no sentido adapter/infrastructure → application → domain.
- `FF-LAY-02` (HexagonalLayersTest): Sem ciclos entre pacotes de primeiro nível.
- `FF-DOM-01` (DomainPurityTest): O domínio não depende de Spring, JPA, Hibernate, Lombok nem Jackson.
- `FF-PRT-01` (PortsTest): Tipos diretamente em port.in e port.out são interfaces.
- `FF-PRT-02` (PortsTest): Só casos de uso implementam portas de entrada.
- `FF-ADP-01` (AdaptersTest): Adapters de entrada não dependem de adapters de saída.
- `FF-ADP-02` (AdaptersTest): Controllers só em adapter.in.
- `FF-ADP-03` (AdaptersTest): Controllers falam com port.in, não com casos de uso nem repositórios.
- `FF-PER-01` (PersistenceTest): @Entity só no pacote de entidades de persistência.
- `FF-PER-02` (PersistenceTest): Repositórios Spring Data só no pacote jpa de persistência.
- `FF-MOD-01` (ModuleBoundariesTest): Um módulo só usa application.port.in e domain de outro módulo.
- `FF-MOD-02` (ModuleBoundariesTest): O shared kernel não depende de nenhum módulo.
- `FF-MOD-03` (ModuleBoundariesTest): Sem ciclos entre módulos.
- `FF-COD-01` (CodingRulesTest): Sem @Autowired em campo; injeção por construtor.
- `FF-COD-02` (CodingRulesTest): Domínio e aplicação não têm @Service, @Component nem @Repository.
- `FF-COD-03` (CodingRulesTest): Sem uso de System.out nem System.err.

- Para aceitar as violações que já existem e barrar só as novas, envolva a regra com `FreezingArchRule.freeze(...)`.
- Para desligar um grupo de regras, remova a classe de teste correspondente.
