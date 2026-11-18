# ADR-0001 — Motor independente de interface, organizado em arquitetura hexagonal

- **Status:** aceito (2026-11-18).
- **Data:** 2026-11-18
- **Contexto:** o Archwright terá primeiro um app desktop, mas já se discute CLI, uma possível versão web e outras arquiteturas além da hexagonal. Um protótipo anterior (`GeradorHex.java`) misturava interface Swing, regras de versão e templates no mesmo arquivo, o que tornava qualquer mudança arriscada. Além disso, uma ferramenta que gera projetos hexagonais com fitness functions perde credibilidade se ela mesma não seguir essas regras.
- **Decisão:**
  1. O projeto é um build Maven com dois módulos: `archwright-engine` (biblioteca Java pura) e `archwright-desktop` (JavaFX, ver [ADR-0002](0002-desktop-javafx-atlantafx.md)). O desktop depende do engine; o engine não depende de nenhuma interface.
  2. O engine é organizado em arquitetura hexagonal: `domain` (regras puras), `application` (portas e casos de uso) e `adapter` (Maven Central, templates, renderização, ZIP, disco). A interface só conversa com `application.port.in`.
  3. O engine não usa Spring nem container de injeção. A montagem dos objetos fica num único ponto (`config/ArchwrightEngine`), com construtores explícitos.
  4. O próprio Archwright roda as mesmas fitness functions que gera (*dogfooding*).
- **Alternativas consideradas:**
  - *Um módulo só, com pacotes separados:* mais simples no início, mas nada impede a interface de importar um adapter. Descartado.
  - *Spring no engine:* facilitaria a injeção, mas aumenta o tamanho do executável e o tempo de inicialização do desktop sem ganho real para um pipeline linear. Descartado.
  - *Engine como CLI separado chamado pelo desktop:* exigiria protocolo entre processos. Descartado enquanto tudo for Java.
- **Consequências:** qualquer nova interface (CLI, API web, plugin de IDE) é um novo adapter de entrada, sem tocar no engine. O engine pode ser publicado no Maven Central como biblioteca. Exige disciplina de fronteiras, garantida por ArchUnit no build.
- **Quando revisar:** se surgir uma interface que não rode na JVM e precise do engine em outro processo.
