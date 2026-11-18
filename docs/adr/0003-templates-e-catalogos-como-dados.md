# ADR-0003 — Templates e catálogos como arquivos de dados, não como código

- **Status:** proposto (2026-11-18). Aceitar antes de iniciar a Fase 1, item [ENG-PLN](../architecture/ARCHITECTURE.md#etapa-4--planejar-arquivos-eng-pln).
- **Data:** 2026-11-18
- **Contexto:** no protótipo, o `pom.xml`, os `package-info.java` e a matriz de compatibilidade de versões eram strings dentro do código Java. Mudar um template exigia editar Java, e contribuições externas ficavam difíceis. O Archwright vai crescer em duas direções: novas versões do Spring Boot (todo semestre) e novas arquiteturas.
- **Decisão:**
  1. **Templates** ficam em arquivos, um diretório por estilo de arquitetura, renderizados com **Mustache** (biblioteca **JMustache**: um único jar, sem dependências).
  2. Cada estilo tem um **`manifest.yaml`** que lista os arquivos a gerar, as condições de inclusão e a repetição por módulo ([formato](../contracts/TEMPLATE_FORMAT.md)).
  3. **Catálogos** também são dados em YAML: perfis de versão do Spring Boot (`boot-profiles.yaml`), kits de dependências (`kits.yaml`), dependências (`dependencies.yaml`) e regras de fitness functions (`fitness-rules.yaml`). São lidos com **SnakeYAML**.
  4. Os arquivos ficam embutidos no jar do engine. A Fase 4 do [roadmap](../implementation/ROADMAP.md) avalia permitir uma pasta externa que sobrescreva os embutidos.
  5. Mustache não tem lógica (só `{{var}}`, seções e listas). **Toda decisão fica no engine**, que entrega ao template um contexto já resolvido.
- **Alternativas consideradas:**
  - *Pebble ou FreeMarker:* `if/else` e filtros mais expressivos, mas facilitam colocar regra de negócio no template, onde ela não é testada. Descartado.
  - *Manifest em JSON:* sem dependência extra, mas pior para editar à mão (sem comentários). Descartado.
  - *Manifest em código Java:* tipado, mas volta ao problema do protótipo. Descartado.
  - *Maven Archetype:* nativo, mas a lógica condicional (Boot 2/3/4, dependências opcionais) fica muito difícil no Velocity. Descartado.
- **Consequências:** atualizar uma versão do Spring Boot ou o texto de um `package-info` vira uma mudança só em dados, revisável por qualquer pessoa. Em troca, erros de template só aparecem em tempo de execução: por isso todo template e catálogo é validado no build ([ENG-CAT](../architecture/ARCHITECTURE.md#catálogos-eng-cat)) e coberto por testes de saída ([TESTING](../operations/TESTING.md)).
- **Quando revisar:** se os templates precisarem de lógica que o contexto pré-calculado não consiga expressar.
