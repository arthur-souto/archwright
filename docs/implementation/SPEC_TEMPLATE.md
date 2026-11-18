# Modelo de especificação de implementação

Use este modelo para toda etapa do motor, tipo de domínio ou caso de uso novo, dentro do [`ARCHITECTURE.md`](../architecture/ARCHITECTURE.md). Exemplos completos: [ENG-VAL](../architecture/ARCHITECTURE.md#etapa-1--validar-e-normalizar-eng-val) e [ENG-EXP](../architecture/ARCHITECTURE.md#etapa-6--exportar-eng-exp).

**Regras do modelo**
- Toda regra cita a sua origem com link: `BR-xxx` em [BUSINESS_RULES](../domain/BUSINESS_RULES.md), ADR ou contrato.
- Todo critério de aceite tem **resultado esperado concreto**: valor normalizado, `code` da violação, arquivo criado ou não criado.
- Cada critério vira pelo menos um teste. O id (`ENG-VAL-01 #3`) vai no nome do teste ou num comentário.
- Erro de entrada do usuário → `Violation` com `code` (nunca exceção, BR-071). Bug do Archwright (catálogo ou template quebrado) → `IllegalStateException`.
- Quando implementado: trocar o status para ✅, citar o arquivo e o teste, e corrigir o texto se a implementação divergir.

---

```markdown
### `NomeDaEtapaOuTipo` (`PFX`)
**Status:** ⏳ a implementar | ✅ implementado (`engine/.../NomeDoTipo.java`; testes em `NomeDoTipoTest`).
Regras: **[BR-xxx](../domain/BUSINESS_RULES.md#secao)** (resumo em 3 palavras), [ADR-000x](../adr/...). Contrato: [ENGINE_API](../contracts/ENGINE_API.md).
Depende de: [OutraEtapa](#outra-etapa).

Uma ou duas frases: o que é e por que existe.

**Contrato**

    // assinaturas públicas, retorno, violações possíveis e exceções
    public Resultado operacao(Entrada entrada);   // Violation CODE; bug → IllegalStateException

#### PFX-01 — Título curto da história
> **Como** <usuário | mantenedor>, **quero** <capacidade>, **para** <benefício> (BR-xxx).

| # | Dado | Quando | Então |
|---|---|---|---|
| 1 | estado inicial concreto | ação | resultado esperado concreto |
| 2 | caso de borda (limite exato) | ação | resultado |
| 3 | caso inválido | ação | `code` da violação; o que **não** é gerado/gravado |

**Fora do escopo:** o que fica para depois, e em que fase.
**Pronto quando:** quais testes (unitário, golden, integração, matriz) cobrem quais histórias.
```

**Prefixos de ID em uso:** `ENG-CAT`, `ENG-VER`, `ENG-VAL`, `ENG-BPR`, `ENG-CTX`, `ENG-PLN`, `ENG-RND`, `ENG-EXP`, `ENG-UC` (engine); `AW-FF` (fitness functions do próprio Archwright); `FF-*` (fitness functions geradas). Para a interface: `DSK` (desktop). Para estilos novos: `STY-<estilo>`.
