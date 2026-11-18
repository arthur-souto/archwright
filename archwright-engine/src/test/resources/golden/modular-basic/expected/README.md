# pedido-api

API de pedidos

Projeto gerado pelo [Archwright](https://github.com/arthur-souto/archwright), com a arquitetura montada desde o primeiro commit.

## Resumo

| | |
|---|---|
| Arquitetura | Hexagonal modular |
| Spring Boot | 4.1.2 |
| Java | 25 |
| Kit de dependências | Básico |
| Pacote base | `com.acme.pedidoapi` |

## Estrutura de pacotes

Monólito modular: cada módulo tem a sua própria hexagonal (`domain`, `application`, `adapter`, `config`).

- `bootstrap`: composition root, sobe o Spring e liga os módulos.
- `shared`: shared kernel, todos os módulos dependem dele e ele não depende de nenhum.
- `pedidos`: módulo Pedidos.
- `pagamentos`: módulo Pagamentos.

Cada pacote tem um `package-info.java` explicando a responsabilidade dele.

## Como rodar

Suba o PostgreSQL local (opcional: copie `.env.example` para `.env` e ajuste):

```shell
docker compose up -d
```

```shell
./mvnw spring-boot:run
```

Para compilar e testar:

```shell
./mvnw verify
```

## Fitness functions

Os testes de arquitetura ficam em `src/test/java/com/acme/pedidoapi/architecture` e rodam no `./mvnw verify`.
O build quebrar quando alguém viola a arquitetura é intencional: é isso que mantém a estrutura certa ao longo do tempo.

- Para aceitar as violações que já existem e barrar só as novas, envolva a regra com `FreezingArchRule.freeze(...)`.
- Para desligar um grupo de regras, remova a classe de teste correspondente.
