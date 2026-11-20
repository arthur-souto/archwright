# Regras de negócio e invariantes

Os IDs `BR-*` são compromissos testáveis. Toda especificação em [ARCHITECTURE.md](../architecture/ARCHITECTURE.md) cita as regras que implementa, e todo teste que cobre uma regra cita o ID.

Vocabulário:
- **Pedido** (`ProjectRequest`): o que a interface envia ao motor, sem validação.
- **Especificação** (`ProjectSpec`): o pedido depois de normalizado e validado. Só existe se for válido.
- **Violação**: um problema de entrada, com `code`, campo e parâmetros. Nunca é exceção (BR-071).
- **Aviso** (`warning`): algo permitido, mas que o usuário deve saber (ex.: Java acima do testado).

---

## Identificação do projeto
- **BR-001** `artifactId`: após `trim` e conversão para minúsculas, deve casar com `[a-z][a-z0-9]*([.-][a-z0-9]+)*` e ter de 1 a 64 caracteres. Fora disso: `INVALID_ARTIFACT_ID`. O motor não faz outras correções silenciosas.
- **BR-002** `groupId`: segmentos separados por ponto, cada um casando com `[a-z][a-z0-9_]*`, ao menos um segmento, até 128 caracteres. Fora disso: `INVALID_GROUP_ID`.
- **BR-003** Pacote base: se vier vazio, o padrão é `groupId + "." + artifactId` sem `-` e `.` (ex.: `com.acme` + `pedido-api` → `com.acme.pedidoapi`). Cada segmento deve ser identificador Java válido em minúsculas e **não** pode ser palavra reservada (`new`, `class`, `record`, `var`…). Fora disso: `INVALID_BASE_PACKAGE`. O pacote tem de ter ao menos dois segmentos (`main` ou `app` sozinhos não servem); com um só: `BASE_PACKAGE_TOO_SHORT`. O padrão derivado sempre tem dois ou mais.
- **BR-004** Descrição: opcional, até 255 caracteres, sem quebra de linha. Fora disso: `INVALID_DESCRIPTION`. É escapada para XML e Markdown na saída.
- **BR-005** Classe principal: `PascalCase(artifactId) + "Application"` (ex.: `pedido-api` → `PedidoApiApplication`). Se começar com dígito, recebe o prefixo `App`.

## Estilo de arquitetura
- **BR-010** Estilos disponíveis na Fase 1: `HEXAGONAL_MODULAR` (monólito modular, estilo [Hexbank](https://github.com/arthur-souto/Hexbank)) e `HEXAGONAL_LAYERED` (camadas `domain`/`application`/`infrastructure`, estilo [Liva](https://github.com/arthur-souto/liva)). Cada estilo corresponde a um diretório de templates com manifest próprio ([formato](../contracts/TEMPLATE_FORMAT.md)). Estilo desconhecido: `UNKNOWN_ARCHITECTURE_STYLE`.
- **BR-011** `HEXAGONAL_MODULAR` exige de 1 a 20 módulos. Cada nome segue as mesmas regras de segmento de pacote do BR-003, é único depois de normalizado e não pode ser `shared`, `bootstrap` nem `architecture`. Violações: `MODULES_REQUIRED`, `TOO_MANY_MODULES`, `INVALID_MODULE_NAME`, `DUPLICATED_MODULE_NAME`, `RESERVED_MODULE_NAME`. A interface sugere `core` como primeiro módulo.
- **BR-012** `HEXAGONAL_LAYERED` aceita de 0 a 20 agregados (subpacotes de `domain.model`), com as mesmas regras de nome do BR-011, exceto as palavras reservadas.

## Versões
- **BR-020** Spring Boot suportado: versão estável (`x.y.z`, sem `-M`, `-RC` ou `-SNAPSHOT`), a partir de `2.7.0`, **cuja linha `x.y` exista no catálogo de perfis** (`boot-profiles.yaml`). Patch novo de uma linha conhecida é aceito automaticamente; linha nova exige atualizar o catálogo. Fora disso: `UNSUPPORTED_BOOT_VERSION`.
- **BR-021** Java: deve estar entre as versões conhecidas (8, 11, 17 e de 21 em diante; fora disso, `UNSUPPORTED_JAVA_VERSION`) e ser **maior ou igual** ao `javaMin` do perfil. Abaixo do mínimo: `INCOMPATIBLE_JAVA_VERSION`. Acima do `javaMaxTested` do perfil: permitido, com o aviso `JAVA_ABOVE_TESTED`.
- **BR-022** Padrões: Spring Boot = versão estável mais recente disponível; Java = maior LTS entre `javaMin` e `javaMaxTested` do perfil.
- **BR-023** A lista de versões do Spring Boot vem, nesta ordem: (1) cache local com menos de 24 h, sem acessar a rede; (2) Maven Central; (3) cache local mais antigo, se o Maven Central não responder; (4) lista embutida no engine. A resposta informa a origem usada (`REMOTE`, `CACHE`, `STALE_CACHE`, `EMBEDDED`); fora de `REMOTE` e `CACHE`, a interface mostra o aviso `VERSIONS_OFFLINE`. A falta de internet nunca impede a geração.
- **BR-024** Linhas fora de suporte gratuito (hoje, `2.7.x`) são marcadas `legacy` no catálogo e geram o aviso `LEGACY_BOOT_LINE`.

## Dependências e kits
- **BR-030** Cada dependência do catálogo tem `id`, rótulo, faixa de versões do Spring Boot em que está disponível e as coordenadas Maven por perfil. Dependência fora da faixa da versão escolhida: `DEPENDENCY_NOT_AVAILABLE`. Id desconhecido: `UNKNOWN_DEPENDENCY`.
- **BR-031** Kits da Fase 1:
  - `NONE` (Nenhuma): nenhuma dependência além do starter base e do starter de testes.
  - `BASIC` (Básico): `web`, `validation`, `actuator`, `jpa` (inclui o driver PostgreSQL), `flyway`, `lombok`.
- **BR-032** O kit é só um ponto de partida: o pedido carrega a lista final de dependências, e o id do kit escolhido não altera a geração além de ir para o README gerado. O id deve ser de um kit conhecido (estratégia registrada em `Kits`); fora disso: `UNKNOWN_KIT`.
- **BR-033** Dependências implícitas, adicionadas pelo motor com o aviso `DEPENDENCY_ADDED` (citando qual e por quê): `flyway` → `jpa`; `springdoc` → `web`.
- **BR-034** Extras não são dependências Maven:
  - `docker-compose`: só com `jpa`. Sem `jpa`: `EXTRA_REQUIRES_DEPENDENCY`.
  - `github-actions`: sem pré-requisito.
  - Id desconhecido: `UNKNOWN_EXTRA`.
- **BR-035** O Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) é **sempre** gerado, a partir de arquivos embutidos no engine. Gerar um projeto nunca depende de Maven instalado na máquina.

## Fitness functions
- **BR-040** As fitness functions vêm ligadas por padrão em todos os kits; o pedido pode desligá-las explicitamente ([ADR-0004](../adr/0004-fitness-functions-por-padrao.md)).
- **BR-041** O conjunto de regras é escolhido por estilo de arquitetura, dependências e versão do Spring Boot (pacotes `javax` × `jakarta`), conforme `fitness-rules.yaml`.
- **BR-042** Cada regra tem um id estável (`FF-xxx`), e o teste gerado o exibe no nome (`@DisplayName` ou `.as(...)`), para que a falha no build diga qual regra foi violada.
- **BR-043** Com fitness functions ligadas, o projeto gerado tem `archunit.properties` com `archRule.failOnEmptyShould=false` e a versão do ArchUnit como propriedade no `pom.xml`.
- **BR-044** Todo projeto gerado com qualquer combinação válida de estilo, kit e linha do Spring Boot passa no próprio `mvn verify`, incluindo as fitness functions. Isso é garantido pela matriz de CI do Archwright ([TESTING](../operations/TESTING.md#matriz-de-compilação)).

## Geração
- **BR-050** Determinismo: o mesmo pedido, na mesma versão do engine, gera exatamente os mesmos bytes (ordem de arquivos, conteúdo e metadados do ZIP). Nenhum conteúdo gerado contém data, hora ou valor aleatório.
- **BR-051** Arquivos de texto em UTF-8 com quebra de linha LF; a única exceção é `mvnw.cmd`, em CRLF.
- **BR-052** Todo caminho gerado é relativo à raiz do projeto, sem `..`, sem caminho absoluto e sem separador `\`. Plano que viole isso: erro de template, `INVALID_TEMPLATE_PATH` (bug, não erro do usuário).
- **BR-053** Geração é tudo ou nada: o projeto é renderizado inteiro em memória antes de qualquer gravação. Se algo falhar, nada é gravado.
- **BR-054** Todo pacote criado tem um `package-info.java` com a descrição da sua responsabilidade. Não existem diretórios vazios.
- **BR-055** Na Fase 1 o motor não gera classes de negócio de exemplo (use cases, controllers, entidades). Gera apenas a classe principal, `package-info`, configuração, migration inicial (com Flyway), testes de arquitetura e a infraestrutura de erros: exceções de domínio padrão e, com `web`, o `GlobalExceptionHandler` ([contrato](../contracts/GENERATED_PROJECT.md#exceções-padrão-e-tratamento-de-erros)).

## Exportação
- **BR-060** Formatos: `ZIP` e `FOLDER`.
- **BR-061** `ZIP`: grava `<destino>/<artifactId>.zip`, com todas as entradas dentro da pasta raiz `<artifactId>/`, como o Spring Initializr. Se o arquivo já existir: `TARGET_EXISTS`, a menos que o pedido traga `overwrite=true` (confirmação explícita da interface). `mvnw` recebe permissão de execução (modo Unix `755`).
- **BR-062** `FOLDER`: grava em `<destino>/<artifactId>/`. A pasta não pode existir com conteúdo: `TARGET_NOT_EMPTY`. **Nunca sobrescreve.** Grava primeiro numa pasta temporária irmã e depois renomeia; se falhar, remove a temporária. `mvnw` recebe permissão de execução quando o sistema de arquivos suporta POSIX.
- **BR-063** O destino deve existir e permitir escrita: `DESTINATION_NOT_FOUND` ou `DESTINATION_NOT_WRITABLE`.
- **BR-064** O resultado informa o caminho absoluto gerado, a quantidade de arquivos, o tamanho total e os avisos.

## Erros, avisos e privacidade
- **BR-070** A validação coleta **todas** as violações de uma vez (não para na primeira) e devolve cada uma com `code`, campo e parâmetros. A interface traduz o `code` em mensagem.
- **BR-071** Erro de entrada do usuário nunca vira exceção: o motor devolve um resultado de falha. Exceções ficam para bugs e falhas de E/S inesperadas.
- **BR-072** O motor só acessa a rede para ler o `maven-metadata.xml` do Maven Central. Não envia nenhum dado do usuário nem tem telemetria.

## Decisões em aberto
- Quais versões exatas do SpringDoc e o `javaMaxTested` de cada linha do Spring Boot: preencher no catálogo durante a Fase 1, validando com a matriz de CI ([ARCHITECTURE](../architecture/ARCHITECTURE.md#perfis-de-versão-iniciais)).
- Suporte a Gradle e Kotlin: fora da Fase 1.
- Se o kit `BASIC` deve incluir `springdoc`: hoje não.
