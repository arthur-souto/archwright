# ADR-0004 — Fitness functions geradas e ligadas por padrão

- **Status:** aceito (2026-11-18).
- **Data:** 2026-11-18
- **Contexto:** gerar a estrutura de pacotes certa é fácil; mantê-la certa depois de meses de desenvolvimento é o problema real. Sem verificação automática, um controller que chama um repositório direto ou uma entidade JPA dentro do domínio passam despercebidos em code review. O diferencial do Archwright em relação a outros geradores é entregar a arquitetura **e** a garantia de que ela se mantém.
- **Decisão:**
  1. Todo projeto gerado recebe **fitness functions de arquitetura** escritas com **ArchUnit**, num pacote `architecture` de testes, uma classe por grupo de regras ([catálogo](../contracts/GENERATED_PROJECT.md#fitness-functions)).
  2. As regras ficam **ligadas por padrão em todos os kits**, inclusive no kit "Nenhuma". O usuário pode desligá-las explicitamente.
  3. O engine escolhe as regras conforme o estilo de arquitetura e as dependências (ex.: regras de persistência só entram com JPA) e conforme a versão do Spring Boot (`javax` × `jakarta`).
  4. Projetos novos geram `archunit.properties` com `archRule.failOnEmptyShould=false`, para que camadas ainda vazias não quebrem o build.
  5. As regras rodam no `mvn verify`, portanto também no CI gerado.
- **Alternativas consideradas:**
  - *ArchUnit como dependência opcional comum:* a maioria das pessoas não marcaria. Descartado.
  - *Spring Modulith:* ótimo para fronteiras de módulo, mas não cobre regras de camadas hexagonais nem funciona com Boot 2.7. Pode virar complemento no futuro.
  - *jMolecules + ArchUnit:* exige anotar as classes do usuário. Pode virar opção futura.
- **Consequências:** o build do projeto gerado falha quando a arquitetura é violada. Isso é intencional, e precisa estar claro no README gerado, com instrução de como desligar ou congelar uma regra (`FreezingArchRule`).
- **Quando revisar:** quando entrar um estilo de arquitetura cujas regras o ArchUnit não consiga expressar.
