# Estratégia de testes

O Archwright tem duas responsabilidades a testar: o **próprio código** (motor e interface) e os **projetos que ele gera**. A segunda é a mais importante: um gerador que produz um projeto que não compila não serve para nada (BR-044).

## Pirâmide

| Nível | O que cobre | Ferramentas | Rede/disco |
|---|---|---|---|
| Domínio | nomes, validação, perfis, determinismo | JUnit 5, AssertJ | nenhum |
| Aplicação | casos de uso com portas falsas (*fakes*) | JUnit 5 | nenhum |
| Adapters | YAML real, Mustache real, exportação ZIP/pasta, parser do Maven Central | JUnit 5, `@TempDir` | disco temporário; **nunca rede** |
| Golden | saída completa do motor comparada byte a byte | JUnit 5 | disco temporário |
| Arquitetura | AW-FF do próprio Archwright | ArchUnit | nenhum |
| Matriz de compilação | projetos gerados compilam e passam nas próprias fitness functions | CI + Maven | rede (Maven Central) |
| Desktop | fluxo do wizard | manual na Fase 2; TestFX avaliado depois | — |

Convenções:
- Testes de domínio e aplicação não usam arquivos nem rede.
- Nenhum teste automatizado acessa o Maven Central, exceto a matriz de compilação. O engine é criado com `.offline(true)` e o parser usa um `maven-metadata.xml` de exemplo em `src/test/resources`.
- Todo teste cita o id do critério (`ENG-EXP-02 #3`) no nome ou num comentário.
- Relógio injetado (`Clock`) onde houver tempo (cache de versões).

## Golden files

Para cada estilo há pedidos de referência em `archwright-engine/src/test/resources/golden/<caso>/request.json`, com o projeto esperado ao lado (`expected/`). O `GoldenProjectTest` gera o projeto e compara arquivo a arquivo.

Casos iniciais:

| Caso | Estilo | Boot / Java | Kit | Extras | Fitness |
|---|---|---|---|---|---|
| `modular-basic` | modular, módulos `pedidos`, `pagamentos` | 4.1 / 25 | `BASIC` | docker-compose, github-actions | sim |
| `modular-none` | modular, módulo `core` | 3.5 / 21 | `NONE` | — | sim |
| `layered-basic` | camadas, agregado `paciente` | 4.1 / 25 | `BASIC` | docker-compose | sim |
| `legacy-boot2` | modular, módulo `core` | 2.7 / 8 | `BASIC` + `springdoc` | — | sim |
| `no-fitness` | camadas | 4.1 / 25 | `NONE` | — | não |

Existem os cinco casos: `modular-basic`, `modular-none` (Boot 3.5.16), `legacy-boot2` (2.7.18), `layered-basic` (agregado `paciente`, 4.1.2) e `no-fitness` (camadas, sem agregados, NONE, 4.1.2). O `request.json` usa os mesmos campos do `ProjectRequest`. Os arquivos de `expected/` ficam marcados como `-text` no `.gitattributes`, para o Git nunca converter fim de linha.

Mudança intencional na saída: rodar `./mvnw -pl archwright-engine test -Dtest=GoldenProjectTest -Dgolden.update=true`, revisar o *diff* dos arquivos esperados no Git e versionar junto com a mudança de template. Diff inesperado em golden é sempre investigado antes de atualizar.

## Matriz de compilação

Job de CI separado (`generated-projects.yml`) que garante o BR-044:
1. Para cada linha do `boot-profiles.yaml` (último patch embutido), cada estilo e cada kit (`NONE`, `BASIC`), com fitness functions ligadas, gera o projeto com o engine.
2. Roda `./mvnw -B verify` dentro de cada projeto gerado, com o Java padrão do perfil (BR-022).
3. Linha `4.1` também roda com `javaMaxTested`.

Quando roda: em todo PR que altere `catalog/`, `templates/` ou `fitness/`, e toda noite na `main`. Falha na matriz bloqueia o merge.

Os valores marcados com ⚠️ nos [perfis iniciais](../architecture/ARCHITECTURE.md#perfis-de-versão-iniciais) só perdem a marcação depois de passar por aqui.

## Aceitação mínima por fase

| Fase | Teste obrigatório |
|---|---|
| 0 | `./mvnw verify` verde com `EngineArchitectureTest` |
| 1 | todos os `ENG-*`; golden dos 5 casos; matriz de compilação verde |
| 2 | `DesktopArchitectureTest`; roteiro manual do wizard gerando ZIP e pasta nos dois estilos |
| 3 | instalador de cada SO instala e gera projeto numa máquina sem JDK |

## Matriz de falhas

| Cenário | Esperado |
|---|---|
| Sem internet ao abrir o app | versões do cache ou embutidas; aviso `VERSIONS_OFFLINE`; geração funciona |
| Maven Central lento | desiste em 8 s e usa o fallback |
| Disco cheio durante exportação de pasta | pasta temporária removida; destino intacto |
| Disco cheio durante exportação de ZIP | nenhum `.zip` parcial no destino |
| Pasta de destino já com projeto | `TARGET_NOT_EMPTY`; nada alterado |
| Template com variável inexistente (bug) | falha no build do Archwright (golden/renderer), nunca na mão do usuário |
| Catálogo inconsistente (bug) | `CatalogConsistencyTest` falha no build |
| Nova versão do Spring Boot de linha desconhecida | não aparece na lista até o catálogo ser atualizado |
