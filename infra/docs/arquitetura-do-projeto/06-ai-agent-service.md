# AI Agent Service — Documentação Técnica

## Equipe

| Papel | Nome |
|-------|------|
| Agencia de Desenvolvimento | **Clarituz** |
| CEO | **Jerferson** |
| Engenheiro Full Stack | **Vinicius Moreira** |

---

## Visão Geral

O `ai-agent-service` é um microserviço autônomo de IA integrado à plataforma Brasaller. Ele permite criar **agentes inteligentes** que percebem o contexto do sistema, tomam decisões, executam ferramentas dos demais microserviços e aprendem com o histórico de execuções.

| Atributo | Valor |
|----------|-------|
| **Porta (dev)** | 8086 |
| **Porta (prod)** | 8080 |
| **Framework** | Quarkus 3.35.4 / Java 21 |
| **Banco** | PostgreSQL (Neon) — `ai-agent-service-db` |
| **Migrations** | Flyway |
| **Arquitetura** | Hexagonal (Ports & Adapters) |

---

## Diagrama de Arquitetura do Microserviço

```mermaid
graph TB
    subgraph "ai-agent-service"
        subgraph "interfaces/rest"
            AR[AgentResource\n/ai-agents/**]
        end

        subgraph "application/service"
            AS[AgentService\nCRUD + Orquestração]
            DE[AgentDecisionEngine\nLoop de Decisão]
        end

        subgraph "infrastructure/llm"
            LLM_F[LLMProviderFactory]
            LLM_O[OpenAIProvider]
            LLM_C[ClaudeProvider]
            LLM_N[NoOpProvider]
        end

        subgraph "infrastructure/tool"
            TR[ToolRegistry]
            T1[SendNotificationTool]
            T2[GenerateReportTool]
            T3[QueryBillingTool]
            T4[QueryUserTool]
            T5[CallWebhookTool]
        end

        subgraph "infrastructure/client"
            NC[NotificationServiceClient]
            RC[ReportingServiceClient]
            BC[BillingServiceClient]
            UC[UserServiceClient]
            CC[CoreServiceClient]
        end

        subgraph "infrastructure/persistence"
            DB[(PostgreSQL)]
        end
    end

    AR --> AS
    AS --> DE
    DE --> LLM_F
    LLM_F --> LLM_O
    LLM_F --> LLM_C
    LLM_F --> LLM_N
    DE --> TR
    TR --> T1 & T2 & T3 & T4 & T5
    T1 --> NC
    T2 --> RC
    T3 --> BC
    T4 --> UC
    AS --> DB
    DE --> DB

    style AR fill:#E8A838,color:#000
    style DE fill:#9B59B6,color:#fff
    style LLM_F fill:#3498DB,color:#fff
    style TR fill:#5DAA68,color:#fff
```

---

## Fluxo de Decisão do Agente

```mermaid
sequenceDiagram
    actor U as Usuario
    participant API as AgentResource
    participant SVC as AgentService
    participant DE as AgentDecisionEngine
    participant LLM as LLMProvider
    participant TR as ToolRegistry
    participant TOOL as AgentToolExecutor
    participant DB as PostgreSQL

    U->>API: POST /tenants/{id}/agents/{id}/execute { goalId }
    API->>SVC: executeAgent(cmd)
    SVC->>DB: INSERT agent_executions (RUNNING)
    SVC->>DE: runAsync(execution, agent, goal)
    SVC-->>API: AgentExecution { status: RUNNING }
    API-->>U: 202 Accepted

    loop Ciclo de Decisão (max 20 steps)
        DE->>DB: load memories + contexts + recent decisions
        DE->>LLM: complete(prompt + context + tools)
        LLM-->>DE: LLMResponse { content, confidence }
        DE->>DB: INSERT agent_decisions

        alt decision = EXECUTE_TOOL
            DE->>TR: find(toolName)
            TR-->>DE: AgentToolExecutor
            DE->>TOOL: execute(tenantId, inputJson)
            TOOL-->>DE: ToolResult { success, output }
            DE->>DB: UPDATE agent_actions (COMPLETED/FAILED)
            DE->>DB: INSERT agent_memories (SHORT_TERM)
        else decision = COMPLETE
            DE->>DE: break loop
        end
    end

    DE->>DB: UPDATE agent_executions (COMPLETED)
    DE->>DB: UPDATE agent_goals (COMPLETED)
    DE->>DB: INSERT outbox_events (agent.execution.finished)

    U->>API: GET /tenants/{id}/executions/{execId}
    API-->>U: AgentExecution { status: COMPLETED }
```

---

## Fluxo de Memória

```mermaid
graph LR
    subgraph "Memória Curta (SHORT_TERM)"
        ST1[last_action_1]
        ST2[last_action_2]
        ST3[pause_reason]
        ST_EXP[TTL: 3600s → auto-delete]
    end

    subgraph "Memória Longa (LONG_TERM)"
        LT1[tenant_profile]
        LT2[user_preferences]
        LT3[ml_connector_status]
        LT_PERM[Sem TTL → permanente]
    end

    subgraph "Contexto (agent_contexts)"
        CTX1[AGENT: pause_reason]
        CTX2[TENANT: billing_status]
        CTX3[SERVICE: last_sync]
    end

    DE[AgentDecisionEngine] -->|salva após cada ação| ST1
    DE -->|carrega ao iniciar| LT1
    DE -->|carrega ao iniciar| CTX1
    SCH[Scheduler 10min] -->|deleteExpired| ST_EXP

    style ST_EXP fill:#E74C3C,color:#fff
    style LT_PERM fill:#5DAA68,color:#fff
```

---

## Diagrama de Entidades (ERD)

```mermaid
erDiagram
    agents {
        varchar id PK
        varchar tenant_id
        varchar name
        varchar agent_type
        jsonb capabilities
        varchar status
        timestamp created_at
    }

    agent_goals {
        varchar id PK
        varchar tenant_id
        varchar agent_id FK
        varchar title
        jsonb objective
        int priority
        varchar status
        timestamp deadline
        timestamp created_at
    }

    agent_executions {
        varchar id PK
        varchar tenant_id
        varchar agent_id FK
        varchar goal_id FK
        varchar triggered_by
        varchar status
        int total_actions
        int success_actions
        int failed_actions
        timestamp started_at
        timestamp finished_at
    }

    agent_memories {
        varchar id PK
        varchar tenant_id
        varchar agent_id FK
        varchar memory_type
        varchar memory_key
        text memory_value
        int ttl_seconds
        timestamp expires_at
    }

    agent_decisions {
        varchar id PK
        varchar tenant_id
        varchar agent_id FK
        varchar execution_id FK
        varchar decision
        double confidence
        varchar selected_tool
        text reasoning
        timestamp created_at
    }

    agent_actions {
        varchar id PK
        varchar tenant_id
        varchar agent_id FK
        varchar execution_id FK
        varchar tool_name
        varchar status
        long duration_ms
        text output_json
        timestamp created_at
    }

    agent_feedbacks {
        varchar id PK
        varchar tenant_id
        varchar agent_id FK
        varchar execution_id FK
        varchar feedback_type
        int score
        text comment
        timestamp created_at
    }

    agent_contexts {
        varchar id PK
        varchar tenant_id
        varchar agent_id FK
        varchar context_type
        varchar context_key
        text context_value
        timestamp updated_at
    }

    outbox_events {
        varchar id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        text payload_json
        timestamp created_at
        timestamp processed_at
    }

    agents ||--o{ agent_goals : "agent_id"
    agents ||--o{ agent_executions : "agent_id"
    agents ||--o{ agent_memories : "agent_id"
    agents ||--o{ agent_decisions : "agent_id"
    agents ||--o{ agent_actions : "agent_id"
    agents ||--o{ agent_feedbacks : "agent_id"
    agents ||--o{ agent_contexts : "agent_id"
    agent_goals ||--o{ agent_executions : "goal_id"
    agent_executions ||--o{ agent_decisions : "execution_id"
    agent_executions ||--o{ agent_actions : "execution_id"
    agent_executions ||--o{ agent_feedbacks : "execution_id"
```

---

## Diagrama de Eventos (Outbox)

```mermaid
graph LR
    subgraph "Produtores"
        DE[AgentDecisionEngine]
    end

    subgraph "outbox_events (PostgreSQL)"
        OB[(PENDING)]
    end

    subgraph "Dispatcher (30s)"
        DISP[Scheduler]
    end

    subgraph "Eventos Publicados"
        E1[agent.execution.started]
        E2[agent.execution.finished]
        E3[agent.execution.failed]
        E4[agent.action.created]
        E5[agent.decision.created]
        E6[agent.feedback.received]
    end

    DE -->|INSERT| OB
    DISP -->|SELECT + mark processed| OB
    OB --> E1 & E2 & E3 & E4 & E5 & E6

    style OB fill:#336791,color:#fff
    style DISP fill:#E8A838,color:#000
```

---

## Ferramentas e LLM Providers

```mermaid
graph TB
    subgraph "LLM Providers"
        FAC[LLMProviderFactory\nconfig: ai.llm.provider]
        OAI[OpenAIProvider\ngpt-4o-mini]
        CLD[ClaudeProvider\nclaude-haiku-4-5]
        NOP[NoOpProvider\nfallback/dev]
    end

    subgraph "Tool Registry"
        TR[ToolRegistry\nCDI Instance injection]
        T1[send_notification\nNotification Service]
        T2[generate_report\nReporting Service]
        T3[query_billing\nBilling Service]
        T4[query_user\nUser Service]
        T5[call_webhook\nHTTP externo]
    end

    FAC -->|provider=openai| OAI
    FAC -->|provider=claude| CLD
    FAC -->|not available fallback| NOP
    TR --> T1 & T2 & T3 & T4 & T5

    style FAC fill:#3498DB,color:#fff
    style TR fill:#5DAA68,color:#fff
    style NOP fill:#95A5A6,color:#fff
```

---

## Integração com Microserviços

```mermaid
graph LR
    AI[ai-agent-service]

    AI -->|POST /notifications/events/agent-action| NS[notification-service]
    AI -->|GET /reports/internal/tenants/{id}/summary| RS[reporting-service]
    AI -->|GET /billing/tenants/{id}/subscription| BS[billing-service]
    AI -->|GET /users/tenants/{id}/members| US[user-service]
    AI -->|GET /core/connectors| CS[core-service]

    GW[gateway-api] -->|proxy| AI

    style AI fill:#9B59B6,color:#fff
    style GW fill:#E8A838,color:#000
```

---

## REST Endpoints

| Método | Path | Descrição | Auth |
|--------|------|-----------|------|
| `GET` | `/ai-agents` | Status do serviço | — |
| `POST` | `/ai-agents/tenants/{id}/agents` | Criar agente | JWT ADMIN |
| `GET` | `/ai-agents/tenants/{id}/agents` | Listar agentes | JWT |
| `GET` | `/ai-agents/tenants/{id}/agents/{agentId}` | Consultar agente | JWT |
| `POST` | `/ai-agents/tenants/{id}/agents/{agentId}/pause` | Pausar agente | JWT ADMIN |
| `POST` | `/ai-agents/tenants/{id}/agents/{agentId}/resume` | Retomar agente | JWT ADMIN |
| `POST` | `/ai-agents/tenants/{id}/agents/{agentId}/goals` | Criar objetivo | JWT |
| `GET` | `/ai-agents/tenants/{id}/agents/{agentId}/goals` | Listar objetivos | JWT |
| `POST` | `/ai-agents/tenants/{id}/agents/{agentId}/execute` | Executar agente | JWT |
| `GET` | `/ai-agents/tenants/{id}/agents/{agentId}/executions` | Histórico execuções | JWT |
| `GET` | `/ai-agents/tenants/{id}/executions/{execId}` | Status execução | JWT |
| `GET` | `/ai-agents/tenants/{id}/agents/{agentId}/memory` | Consultar memória | JWT |
| `POST` | `/ai-agents/tenants/{id}/agents/{agentId}/memory` | Armazenar memória | JWT |
| `GET` | `/ai-agents/tenants/{id}/executions/{execId}/decisions` | Decisões tomadas | JWT |
| `POST` | `/ai-agents/tenants/{id}/agents/{agentId}/actions` | Ação manual | JWT ADMIN |
| `POST` | `/ai-agents/tenants/{id}/agents/{agentId}/feedback` | Registrar feedback | JWT |
| `GET` | `/ai-agents/tenants/{id}/agents/{agentId}/feedback` | Listar feedbacks | JWT |

---

## Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `HTTP_PORT` | Porta HTTP | `8086` |
| `DB_JDBC_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/ai-agent-service` |
| `DB_USERNAME` | Usuário do banco | `ai-agent-service` |
| `DB_PASSWORD` | Senha do banco | — |
| `AI_JWT_SECRET` | Segredo JWT (min 32 chars) | — |
| `AI_JWT_ISSUER` | Issuer esperado no JWT | `brasaller-auth` |
| `AI_INTERNAL_TOKEN` | Token service-to-service | — |
| `AI_LLM_PROVIDER` | Provider ativo: `noop`, `openai`, `claude` | `noop` |
| `AI_LLM_OPENAI_API_KEY` | Chave OpenAI | — |
| `AI_LLM_OPENAI_MODEL` | Modelo OpenAI | `gpt-4o-mini` |
| `AI_LLM_CLAUDE_API_KEY` | Chave Anthropic | — |
| `AI_LLM_CLAUDE_MODEL` | Modelo Claude | `claude-haiku-4-5-20251001` |
| `AUTH_SERVICE_URL` | URL auth-service | `http://auth-service:8080` |
| `USER_SERVICE_URL` | URL user-service | `http://user-service:8080` |
| `CORE_SERVICE_URL` | URL core-service | `http://core-service:8080` |
| `BILLING_SERVICE_URL` | URL billing-service | `http://billing-service:8080` |
| `NOTIFICATION_SERVICE_URL` | URL notification-service | `http://notification-service:8080` |
| `REPORTING_SERVICE_URL` | URL reporting-service | `http://reporting-service:8080` |

---

## Exemplos de Payloads JSON

### Criar Agente

```json
POST /ai-agents/tenants/{tenantId}/agents
{
  "name": "Agente Financeiro Brasaller",
  "description": "Monitora vendas, analisa DRE e envia alertas automaticos",
  "agentType": "FINANCIAL_ANALYST",
  "capabilities": "{\"can_read_reports\": true, \"can_send_notifications\": true}"
}
```

### Criar Objetivo

```json
POST /ai-agents/tenants/{tenantId}/agents/{agentId}/goals
{
  "title": "Analisar performance do mes de Junho/2026",
  "description": "Consultar vendas, calcular margem e notificar o vendedor",
  "objective": "{\"period\": \"2026-06\", \"actions\": [\"generate_report\", \"send_notification\"]}",
  "priority": 8,
  "deadlineEpochSeconds": 1751328000
}
```

### Executar Agente

```json
POST /ai-agents/tenants/{tenantId}/agents/{agentId}/execute
{
  "goalId": "550e8400-e29b-41d4-a716-446655440000"
}

// Resposta 202:
{
  "id": "exec-uuid",
  "agentId": "agent-uuid",
  "goalId": "goal-uuid",
  "status": "RUNNING",
  "triggeredBy": "user-uuid",
  "startedAt": "2026-06-03T14:00:00Z"
}
```

### Registrar Memória

```json
POST /ai-agents/tenants/{tenantId}/agents/{agentId}/memory
{
  "memoryType": "LONG_TERM",
  "memoryKey": "preferred_notification_channel",
  "memoryValue": "email",
  "ttlSeconds": null
}
```

### Registrar Feedback

```json
POST /ai-agents/tenants/{tenantId}/agents/{agentId}/feedback
{
  "executionId": "exec-uuid",
  "feedbackType": "POSITIVE",
  "score": 9,
  "comment": "Agente identificou corretamente a queda nas vendas e notificou a tempo",
  "metadataJson": "{\"category\": \"accuracy\"}"
}
```

### Consultar Execução

```json
GET /ai-agents/tenants/{tenantId}/executions/{executionId}

// Resposta:
{
  "id": "exec-uuid",
  "agentId": "agent-uuid",
  "goalId": "goal-uuid",
  "status": "COMPLETED",
  "totalActions": 3,
  "successActions": 3,
  "failedActions": 0,
  "summary": "Execution finished: 3 total, 3 success, 0 failed",
  "startedAt": "2026-06-03T14:00:00Z",
  "finishedAt": "2026-06-03T14:00:12Z"
}
```

---

## Riscos Técnicos e Mitigações

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| LLM indisponível | Execuções falham | `NoOpLLMProvider` como fallback; retry configurável |
| Loop infinito de agente | CPU/custo LLM | Limite de 20 ações por execução (`MAX_ACTIONS_PER_EXECUTION`) |
| Memória curta sem TTL | Banco cheio | Scheduler de 10 min limpa expiradas; SHORT_TERM padrão 3600s |
| Token LLM exposto em logs | Segurança | Logs sem conteúdo de chaves; variáveis de ambiente obrigatórias |
| Tenant isolation quebrado | Dados cruzados | `tenant_id` em todas as queries; `TenantAuthorizationService` valida JWT |
| Outbox não processado | Eventos perdidos | Scheduler de 30s; `failed_at` para diagnóstico |
| Ferramenta de webhook abusada | SSRF | Validação de URL; header `X-Agent-Tenant-Id` para auditoria |

---

## Roadmap

- [ ] Integração com Gemini (Google)
- [ ] Agentes multi-step com memória de grafo (Neo4j ou pgvector)
- [ ] Streaming de respostas LLM via SSE
- [ ] Painel de monitoramento de agentes no frontend
- [ ] Rate limiting por tenant (máx N execuções simultâneas)
- [ ] Embedding de documentos para RAG (Retrieval-Augmented Generation)
- [ ] Integração com Kafka para eventos real-time
- [ ] Agent-to-Agent communication (multi-agent workflows)
