# 🏭 Desafio Técnico - Sistema de Gestão e Auditoria de Eventos de Fábrica

Solução Fullstack desenvolvida para recepção, validação, impedimento de duplicidade e consulta de eventos enviados por equipamentos industriais, cumprindo integralmente os requisitos definidos no documento de referência do desafio técnico.

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
2. **Idempotência e Tratamento de Concorrência:** O sistema valida a existência prévia do ID do evento tanto na camada de serviço (`existsByEventId`) quanto através da restrição única (`unique = true`) na base de dados, convertendo disputas simultâneas (*race conditions*) de forma segura em HTTP `409 Conflict`.
3. **Regra de Auditoria (`QUALITY`):** Conforme as regras de negócio, eventos do tipo `PRODUCTION` e `MAINTENANCE` são apenas persistidos, enquanto eventos do tipo `QUALITY` geram automaticamente um registo detalhado de auditoria na tabela correspondente.

---

## 🗄️ Esquema do Banco de Dados & Tabela de Auditoria

A aplicação utiliza o banco de dados H2 em memória (configuração leve permitida pelo PDF de referência), estruturado com duas tabelas principais:

### 1. Tabela `tb_factory_events` (Armazenamento de Eventos)
* `id` (Long, Primary Key, Auto Increment)
* `event_id` (String, Unique, Not Null) — Identificador único enviado pelo equipamento (ex: `EVT-1001`).
* `event_type` (Enum, Not Null) — Tipo do evento (`PRODUCTION`, `MAINTENANCE` ou `QUALITY`).
* `description` (String, Not Null, até 1000 caracteres) — Descrição detalhada do ocorrido na fábrica.
* `timestamp` (LocalDateTime, Not Null) — Data e hora exatas da ocorrência do evento.

### 2. Tabela `tb_audit_logs` (Tabela de Auditoria para Eventos `QUALITY`)
* `id` (Long, Primary Key, Auto Increment)
* `event_id` (String, Not Null) — Referência ao ID do evento gerador.
* `log_message` (String, Not Null, até 2000 caracteres) — Mensagem detalhada de auditoria registada pelo sistema.
* `created_at` (LocalDateTime, Not Null) — Momento exato da gravação do log de auditoria.

---

## 🗄️ Acesso ao Banco de Dados em Memória (H2 Console)

Enquanto o backend estiver em execução, você pode aceder diretamente à consola web do H2 para inspecionar as tabelas `tb_factory_events` e `tb_audit_logs`:

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
npm install
npm start
```
* Aceda à interface web no navegador através de: `http://localhost:4200`.

---

## 🧪 Testes Automatizados e Cobertura (JaCoCo)

Para executar a suíte completa de testes unitários e de integração do backend:
```bash
cd backend
mvn test
```
* O relatório HTML detalhado de cobertura de código gerado pelo JaCoCo fica disponível em: `backend/target/site/jacoco/index.html`.

---

## 📋 Matriz de Atendimento aos Requisitos do PDF de Referência

| Requisito do Desafio Técnico (`Teste_Tecnico_Fullstack_v5.pdf`) | Status | Implementação na Solução |
| :--- | :---: | :--- |
| **Endpoint de recebimento de eventos** | ✅ Atendido | `POST /api/events` validando payloads via Bean Validation. |
| **Impedimento de processamento duplicado** | ✅ Atendido | Verificação prévia e tratamento de restrição de unicidade retornando HTTP `409 Conflict`. |
| **Tratamento diferenciado para tipos de evento** | ✅ Atendido | Suporte a `PRODUCTION`, `MAINTENANCE` e geração automática de auditoria para `QUALITY`. |
| **Códigos HTTP adequados** | ✅ Atendido | Retornos corretos para `201 Created`, `200 OK`, `400 Bad Request`, `405 Method Not Allowed`, `415 Unsupported Media Type` e `409 Conflict`. |
| **Persistência simples em memória** | ✅ Atendido | Utilização configurada do banco H2 em memória. |
| **Interface Frontend para Cadastro e Consulta** | ✅ Atendido | Desenvolvido em Angular 17+ com componentes *standalone*, formulários reativos e listagem em tabela. |
| **Mensagens de sucesso e erro na UI** | ✅ Atendido | Feedback visual claro na tela para operações bem-sucedidas, duplicidades e falhas de conexão. |
| **Testes automatizados e Cobertura** | ✅ Atendido | Testes unitários com JUnit 5/Mockito e validação de cobertura via JaCoCo. |

---

## 🔮 Possíveis Melhorias Futuras

1. **Segurança Avançada:** Implementação de autenticação via Spring Security com tokens JWT nos endpoints REST.
2. **Mensageria Assíncrona:** Integração com RabbitMQ ou Apache Kafka para processamento em larga escala de eventos de fábrica.
3. **Paginação e Filtros na Consulta:** Adição de paginação (`Pageable`) e filtros por período ou tipo no endpoint de listagem de eventos.
4. **Persistência Robusta:** Migração opcional do banco H2 em memória para PostgreSQL ou MySQL utilizando perfis de configuração do Spring (*Spring Profiles*).