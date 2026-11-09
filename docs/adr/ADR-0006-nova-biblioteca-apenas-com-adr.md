# ADR-0006: Nova biblioteca/dependência somente com ADR e aprovação explícita do humano

Status: aceito
Data: 2026-11-05

## Contexto

O `AGENTS.md` define uma pilha obrigatória (Java 25, Spring Boot 4, PostgreSQL 17,
Flyway, Resilience4j, OpenTelemetry, Micrometer/Actuator, ArchUnit, JSpecify) e
proíbe introduzir biblioteca, framework ou padrão fora dela sem justificar em ADR
e sem aprovação explícita do humano. Até hoje essa proibição vive apenas no
contrato do squad (`AGENTS.md`), sem um ADR próprio que a torne citável em PRs e
revisões de arquitetura.

Sem um registro explícito, dependências entram por conveniência ("uma anotação
pronta", "um utilitário de JSON", "um cliente HTTP alternativo"), acumulando
surface de CVE, licenças não revisadas e acoplamento desnecessário ao núcleo
(Lei #1 do squad: `domain` sem frameworks).

## Decisão

1. **Proibir qualquer nova biblioteca (Maven/Gradle), framework ou padrão de
   projeto fora da lista obrigatória do `AGENTS.md` sem:**
   - ADR próprio (força, alternativa descartada, custo da indireção), e
   - aprovação explícita do humano (`squad-lead` ou responsável designado).

2. O ADR deve ser revisado e aceito **antes** da dependência ser adicionada ao
   `pom.xml`/`build.gradle` e antes de qualquer código importá-la.

3. Uma dependência já presente na pilha obrigatória pode ser usada livremente,
   respeitando as regras de camada (ex.: Resilience4j apenas em
   `adapter.out.client`/`config`; nunca em `domain`/`application`).

4. A exceção é registrada no ADR com janela de uso e data de revisão; sem ADR, a
   adição é bloqueio de merge (gate de CI/revisão de arquitetura).

## Consequências

Fica fácil:
- Rastrear por que cada dependência existe.
- Bloquear dependências por impulso.
- Manter o núcleo puro (domínio sem frameworks).

Fica difícil:
- Atrito para adicionar uma utilidade pequena; exige abertura de ADR e espera
  por aprovação. Custo aceito porque o custo de uma dependência desnecessária se
  paga por toda a vida do projeto.

## Números (força registrada)

- Zero dependências novas sem ADR aprovado.
- Zero import de biblioteca não-listada em `domain` ou `application`.

## Alternativas descartadas

- **Permitir adicionar bibliotecas por conveniência, revisando depois.** Rejeitada:
  dependências raramente são removidas; o acoplamento permanece e o núcleo fica
  menos puro. O custo de não ter a regra excede o atrito do ADR.
- **Listar todas as bibliotecas permitidas neste ADR de forma exaustiva.** Rejeitada:
  a pilha obrigatória já está no `AGENTS.md`; este ADR trata o processo de
  exceção, não o catálogo.

## Referências

- `AGENTS.md` — pilha obrigatória e lei de não introduzir biblioteca sem ADR.
- `docs/adr/ADR-0002` — pilha obrigatória adotada e camadas.
- `backend/src/test/java/com/factory/events/ArchitectureTest.java` — regras
  ArchUnit que travam frameworks no núcleo.
