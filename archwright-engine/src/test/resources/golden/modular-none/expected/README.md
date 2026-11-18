# catalogo

Projeto gerado pelo [Archwright](https://github.com/arthur-souto/archwright), com a arquitetura montada desde o primeiro commit.

## Resumo

| | |
|---|---|
| Arquitetura | Hexagonal modular |
| Spring Boot | 3.5.16 |
| Java | 21 |
| Kit de dependências | Nenhuma |
| Pacote base | `br.com.exemplo.catalogo` |

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

Os testes de arquitetura ficam em `src/test/java/br/com/exemplo/catalogo/architecture` e rodam no `./mvnw verify`.
O build quebrar quando alguém viola a arquitetura é intencional: é isso que mantém a estrutura certa ao longo do tempo.

- Para aceitar as violações que já existem e barrar só as novas, envolva a regra com `FreezingArchRule.freeze(...)`.
- Para desligar um grupo de regras, remova a classe de teste correspondente.
