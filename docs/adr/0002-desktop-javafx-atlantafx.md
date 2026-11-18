# ADR-0002 — App desktop com JavaFX + AtlantaFX, distribuído com jpackage

- **Status:** aceito (2026-11-18).
- **Data:** 2026-11-18
- **Contexto:** o primeiro canal de uso é um executável local, no estilo de um assistente (wizard) de 4 telas: Projeto, Versões, Dependências e Exportar. O protótipo em Swing funcionava, mas tinha aparência datada. O motor é Java ([ADR-0001](0001-motor-independente-e-hexagonal.md)).
- **Decisão:**
  - Interface em **JavaFX** com o tema **AtlantaFX** (temas planos, modo claro e escuro).
  - Distribuição com **`jpackage`**, gerando instalador nativo por sistema (`.msi`/`.exe`, `.dmg`, `.deb`) com o runtime Java embutido. Quem instala não precisa ter JDK.
  - O desktop chama o engine em processo, direto pelas portas de entrada.
- **Alternativas consideradas:**
  - *Swing:* zero dependências, mas visual datado. Descartado.
  - *Tauri ou Electron com interface web:* visual moderno, mas obrigaria o motor Java a rodar como processo separado, com duas tecnologias e uma camada de comunicação. A vantagem de tamanho do Tauri some porque o runtime Java continua necessário. Descartado para a Fase 1.
  - *Compose Multiplatform:* visual excelente, mas exige Kotlin. Descartado para manter uma linguagem só.
- **Consequências:** instalador na faixa de 50–80 MB (runtime embutido). O build precisa gerar um instalador por sistema operacional (job de CI por SO). Uma versão web futura continua possível como outro adapter de entrada sobre o mesmo engine.
- **Quando revisar:** se a versão web passar a ser o canal principal, ou se o tamanho do instalador virar problema real para os usuários.
