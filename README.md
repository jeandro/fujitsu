# 🏭 Desafio Técnico - Sistema de Gestão e Auditoria de Eventos de Fábrica

Solução Fullstack desenvolvida para recepção, validação, impedimento de duplicidade e consulta de eventos enviados por equipamentos industriais, implementando os requisitos funcionais do documento de referência do desafio técnico.

---

## 🏗️ Arquitetura e Fluxo da Aplicação

```text
[ Equipamento / Front-end ]
          │
          ▼  HTTP POST /api/events
┌────────────────────────────────────────────────────────┐
│                   EventController                      │ (Validação DTO & OpenAPI)
└───────────┬────────────────────────────────────────────┘
            │
            ▼
┌────────────────────────────────────────────────────────┐
│                     EventService                       │ (Regra de Negócio & Idempotência)
└───────────┬────────────────────────────────┬───────────┘
            │                                │
            ▼ (Salva Evento)                 ▼ (Se QUALITY: Salva Auditoria)
┌───────────────────────┐        ┌───────────────────────┐
│   EventRepository     │        │    AuditRepository    │
└───────────┬───────────┘        └───────────┬───────────┘
            │                                │
            └────────────────┬───────────────┘
                             ▼
                    [ H2 Database (Memória) ]
```

### **Decisões de Design e Fluxo de Negócio:**
1. **Camadas Claras (Controller → Service → Repository):** Separação de responsabilidades para facilitar a manutenção e testes.
2. **Idempotência e Tratamento de Concorrência:** O sistema valida a existência prévia do ID do evento tanto na camada de serviço (`existsByEventId`) quanto através da restrição única (constraint `uk_events_event_id`) na base de dados, convertendo disputas simultâneas (*race conditions*) de forma segura em HTTP `409 Conflict`.
3. **Regra de Auditoria (`QUALITY`):** Conforme as regras de negócio, eventos do tipo `PRODUCTION` e `MAINTENANCE` são apenas persistidos, enquanto eventos do tipo `QUALITY` geram automaticamente um registo detalhado de auditoria na tabela correspondente.

---

## 🗄️ Esquema do Banco de Dados & Tabela de Auditoria

A aplicação utiliza o banco de dados H2 em memória (configuração leve permitida pelo PDF de referência), estruturado com duas tabelas principais:

### 1. Tabela `tb_factory_events` (Armazenamento de Eventos)
* `id` (Long, Primary Key, Auto Increment)
* `event_id` (String, Unique, Not Null) — Identificador único enviado pelo equipamento (ex: `EVT-1001`).
* `event_type` (Enum, Not Null) — Tipo do evento (`PRODUCTION`, `MAINTENANCE` ou `QUALITY`).
* `description` (String, Not Null, até 1000 caracteres) — Descrição detalhada do ocorrido na fábrica.
* `timestamp` (Instant, Not Null) — Data e hora exatas da ocorrência do evento.

### 2. Tabela `tb_audit_logs` (Tabela de Auditoria para Eventos `QUALITY`)
* `id` (Long, Primary Key, Auto Increment)
* `event_id` (String, Not Null) — Referência ao ID do evento gerador.
* `log_message` (String, Not Null, até 2000 caracteres) — Mensagem detalhada de auditoria registada pelo sistema.
* `created_at` (Instant, Not Null) — Momento exato da gravação do log de auditoria.

---

## 🗄️ Acesso ao Banco de Dados em Memória (H2 Console)

Com o backend iniciado no perfil `dev` (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`), você pode aceder diretamente à consola web do H2 para inspecionar as tabelas `tb_factory_events` e `tb_audit_logs`:

1. Abra no navegador: `http://localhost:8080/h2-console`
2. Configure os parâmetros de ligação:
   * **JDBC URL:** `jdbc:h2:mem:eventsdb`
   * **User Name:** `sa`
   * **Password:** *(deixe em branco)*
3. Clique em **Connect** para visualizar e executar consultas SQL nas tabelas.

---

## 📖 Documentação Interativa da API (Swagger / OpenAPI)

A API conta com documentação interativa integrada através do Springdoc OpenAPI:
* **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
* **Especificação OpenAPI (JSON):** `http://localhost:8080/v3/api-docs`

---

## 🚀 Como Executar a Aplicação

### 1️⃣ Executando o Backend (Java Spring Boot)
No terminal, navegue até a pasta `backend/` e execute:
```bash
cd backend
mvn spring-boot:run
```
* O servidor será iniciado em `http://localhost:8080`.

### 2️⃣ Executando o Frontend (Angular 17+)
No terminal, navegue até a pasta `frontend/` e execute:
```bash
cd frontend
npm ci
npm start
```
* Aceda à interface web no navegador através de: `http://localhost:4200` ou do Endereço proposto no console!.

---

## 🧪 Testes Automatizados e Cobertura (JaCoCo)

Para executar a suíte completa de testes unitários e de integração do backend:
```bash
cd backend
mvn test
```
* O relatório HTML detalhado de cobertura de código gerado pelo JaCoCo fica disponível em: `backend/target/site/jacoco/index.html`.

---

## Testes do frontend e validação executada

Na pasta `frontend`, execute `npm ci` e `npm test`. É necessário Chrome ou Edge; a configuração procura o executável nos diretórios usuais do Windows. Para cobertura: `npm test -- --code-coverage`. Detalhes e alternativas em [EXECUCAO.md](EXECUCAO.md).

Validação de 22/10/2026: 16 testes backend aprovados, 8 testes frontend aprovados, build Angular aprovado e 21 cenários HTTP aprovados. Cobertura de linhas: backend 84,13%; frontend 94,11% dos arquivos instrumentados pela suíte. Os testes Angular foram executados com um navegador Chromium conectado ao Karma em modo manual (`KARMA_MANUAL=1`, `ng test --watch=false --code-coverage`); o processo terminou com código zero. Esses resultados não equivalem a cobertura funcional completa.

### Decisões de contrato

- ID normalizado com remoção de espaços nas extremidades; duplicidade retorna 409, sem novo registro ou auditoria.
- Descrição obrigatória com até 1000 caracteres; ID com até 100.
- Datas com offset são convertidas para `Instant`. O exemplo do PDF sem offset é interpretado em `America/Sao_Paulo`; a resposta usa UTC e a interface exibe o fuso do navegador.
- A data do cadastro manual é o instante de envio. O PDF não exige um campo de edição da data na tela.
- `MAINTENANCE` é uma extensão preservada da implementação; apenas `PRODUCTION` e `QUALITY` são explicitamente exigidos no PDF.
- H2 em memória perde os dados quando a aplicação é encerrada, comportamento compatível com o enunciado.

## 📋 Matriz de Atendimento aos Requisitos do PDF de Referência

| Requisito do Desafio Técnico (`Teste_Tecnico_Fullstack_v5.pdf`) | Status | Implementação na Solução |
| :--- | :---: | :--- |
| **Endpoint de recebimento de eventos** | ✅ Atendido | `POST /api/events` validando payloads via Bean Validation. |
| **Impedimento de processamento duplicado** | ✅ Atendido | Verificação prévia e tratamento de restrição de unicidade retornando HTTP `409 Conflict`. |
| **Tratamento diferenciado para tipos de evento** | ✅ Atendido | Suporte a `PRODUCTION`, `MAINTENANCE` e geração automática de auditoria para `QUALITY`. |
| **Códigos HTTP adequados** | ✅ Atendido | Retornos corretos para `201 Created`, `200 OK`, `400 Bad Request`, `405 Method Not Allowed`, `415 Unsupported Media Type` e `409 Conflict`. |
| **Persistência simples em memória** | ✅ Atendido | Utilização configurada do banco H2 em memória. |
| **Interface Frontend para Cadastro e Consulta** | ✅ Atendido | Desenvolvido em Angular 17+ com componentes *standalone*, formulários template-driven (`FormsModule` / `ngForm`) e listagem em tabela. |
| **Mensagens de sucesso e erro na UI** | ✅ Atendido | Feedback visual claro na tela para operações bem-sucedidas, duplicidades e falhas de conexão. |
| **Testes automatizados e Cobertura** | ✅ Atendido | Testes unitários com JUnit 5/Mockito e validação de cobertura via JaCoCo. |

---

## 🔮 Possíveis Melhorias Futuras

1. **Segurança Avançada:** Implementação de autenticação via Spring Security com tokens JWT nos endpoints REST.
2. **Mensageria Assíncrona:** Integração com RabbitMQ ou Apache Kafka para processamento em larga escala de eventos de fábrica.
3. **Paginação e Filtros na Consulta:** Adição de paginação (`Pageable`) e filtros por período ou tipo no endpoint de listagem de eventos.
4. **Persistência Robusta:** Migração opcional do banco H2 em memória para PostgreSQL ou MySQL utilizando perfis de configuração do Spring (*Spring Profiles*).

## Guia de execução atualizado

Consulte [EXECUCAO.md](EXECUCAO.md) para instalação, comandos, contrato, testes e limitações. Em caso de divergência com exemplos anteriores, prevalece esse guia: descrição de até 1000 caracteres; console H2 apenas no perfil dev; API pelo proxy do Angular.


