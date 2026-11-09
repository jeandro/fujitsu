# Andamento — Migração MVC → Hexagonal (fujitsu)

Fluxo: negócio → arquitetura → contrato → backend ‖ frontend → testes → dba → DoD.

Lead acionou o squad em 06/11/2026 para **fechar DoD**, não para recomeçar. Painel: canvas `migracao-mvc-hexagonal`.

| Fase | Entregável | Status | Dono agora |
|------|------------|--------|------------|
| 1 | Inventário estado atual | concluído | — |
| 2 | RN-xx / CA-xx (docs/spec/) | concluído — RN-01..22, CA-01..17, E-01..13 | — |
| 3 | Portas + ADRs + ArchUnit | concluído — ADR-0002..0006 | Helena (reauditoria) |
| 4 | openapi.yaml | concluído | — |
| 5 | Flyway + PostgreSQL | ajuste — **dois V004** | Gabriel |
| 6 | Backend hexagonal | ajuste — 46/46 testes; flyway-core/OTel/outbox | Diego |
| 7 | Frontend Angular | em_andamento vs `docs/spec/telas.md` | Eva + Clara |
| 8 | JaCoCo ≥98%, PIT ≥90%, contrato | em_andamento (piso ainda não é gate) | Felipe |
| 9 | Docker Compose + CI/CD | pendente (arquivos ausentes) | Gabriel + Lead |
| 10 | DoD e entrega | pendente | Lead |

Congelado: PA-02 retenção 24 h; PA-05 compensação lógica; ADR-0005 sem cache.

Bloqueios: Flyway V004 duplicado; sem `docker-compose.yml`; sem `.github/workflows`; JaCoCo sem `check`; PIT ausente.
