-- Agentes autonomos
CREATE TABLE agents (
    id           VARCHAR(36)   PRIMARY KEY,
    tenant_id    VARCHAR(36)   NOT NULL,
    name         VARCHAR(160)  NOT NULL,
    description  TEXT,
    agent_type   VARCHAR(80)   NOT NULL,
    capabilities JSONB         NOT NULL DEFAULT '{}',
    status       VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Objetivos dos agentes
CREATE TABLE agent_goals (
    id           VARCHAR(36)  PRIMARY KEY,
    tenant_id    VARCHAR(36)  NOT NULL,
    agent_id     VARCHAR(36)  NOT NULL REFERENCES agents(id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    objective    JSONB        NOT NULL DEFAULT '{}',
    priority     INTEGER      NOT NULL DEFAULT 5,
    status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    deadline     TIMESTAMP,
    result       TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Execucoes de agentes
CREATE TABLE agent_executions (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    agent_id        VARCHAR(36)  NOT NULL REFERENCES agents(id),
    goal_id         VARCHAR(36)  NOT NULL REFERENCES agent_goals(id),
    triggered_by    VARCHAR(160),
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    total_actions   INTEGER      NOT NULL DEFAULT 0,
    success_actions INTEGER      NOT NULL DEFAULT 0,
    failed_actions  INTEGER      NOT NULL DEFAULT 0,
    summary         TEXT,
    error           TEXT,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Memoria persistente dos agentes
CREATE TABLE agent_memories (
    id           VARCHAR(36)  PRIMARY KEY,
    tenant_id    VARCHAR(36)  NOT NULL,
    agent_id     VARCHAR(36)  NOT NULL REFERENCES agents(id),
    memory_type  VARCHAR(32)  NOT NULL DEFAULT 'SHORT_TERM',
    memory_key   VARCHAR(255) NOT NULL,
    memory_value TEXT         NOT NULL,
    ttl_seconds  INTEGER,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP,
    UNIQUE (agent_id, tenant_id, memory_key)
);

-- Ferramentas disponiveis (catalogo)
CREATE TABLE agent_tools (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description TEXT,
    tool_type   VARCHAR(80)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    config_json JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Decisoes tomadas pelos agentes
CREATE TABLE agent_decisions (
    id             VARCHAR(36)  PRIMARY KEY,
    tenant_id      VARCHAR(36)  NOT NULL,
    agent_id       VARCHAR(36)  NOT NULL REFERENCES agents(id),
    execution_id   VARCHAR(36)  REFERENCES agent_executions(id),
    goal_id        VARCHAR(36)  REFERENCES agent_goals(id),
    context_json   TEXT,
    reasoning      TEXT,
    decision       VARCHAR(80)  NOT NULL,
    confidence     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    selected_tool  VARCHAR(80),
    tool_input_json TEXT,
    outcome        TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Acoes executadas pelos agentes
CREATE TABLE agent_actions (
    id           VARCHAR(36)  PRIMARY KEY,
    tenant_id    VARCHAR(36)  NOT NULL,
    agent_id     VARCHAR(36)  NOT NULL REFERENCES agents(id),
    execution_id VARCHAR(36)  REFERENCES agent_executions(id),
    task_id      VARCHAR(36),
    action_type  VARCHAR(80)  NOT NULL,
    tool_name    VARCHAR(80),
    input_json   TEXT,
    output_json  TEXT,
    status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    error        TEXT,
    duration_ms  BIGINT       NOT NULL DEFAULT 0,
    started_at   TIMESTAMP,
    finished_at  TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Feedbacks sobre desempenho dos agentes
CREATE TABLE agent_feedbacks (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    agent_id      VARCHAR(36)  NOT NULL REFERENCES agents(id),
    execution_id  VARCHAR(36)  REFERENCES agent_executions(id),
    feedback_type VARCHAR(32)  NOT NULL DEFAULT 'NEUTRAL',
    score         INTEGER      NOT NULL DEFAULT 5,
    comment       TEXT,
    metadata_json JSONB        NOT NULL DEFAULT '{}',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Contexto persistente por agente/tenant
CREATE TABLE agent_contexts (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    agent_id      VARCHAR(36)  NOT NULL REFERENCES agents(id),
    context_type  VARCHAR(32)  NOT NULL,
    context_key   VARCHAR(255) NOT NULL,
    context_value TEXT,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (agent_id, tenant_id, context_type, context_key)
);

-- Outbox de eventos assincronos
CREATE TABLE outbox_events (
    id             VARCHAR(36)  PRIMARY KEY,
    aggregate_type VARCHAR(80)  NOT NULL,
    aggregate_id   VARCHAR(36)  NOT NULL,
    event_type     VARCHAR(160) NOT NULL,
    payload_json   TEXT,
    error          TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at   TIMESTAMP,
    failed_at      TIMESTAMP
);

-- Dados iniciais de ferramentas
INSERT INTO agent_tools (id, name, description, tool_type, enabled) VALUES
    (gen_random_uuid()::text, 'send_notification', 'Envia notificacao para o usuario', 'INTERNAL', TRUE),
    (gen_random_uuid()::text, 'generate_report', 'Gera relatorio financeiro', 'INTERNAL', TRUE),
    (gen_random_uuid()::text, 'query_billing', 'Consulta dados de assinatura', 'INTERNAL', TRUE),
    (gen_random_uuid()::text, 'query_user', 'Consulta dados de usuarios', 'INTERNAL', TRUE),
    (gen_random_uuid()::text, 'call_webhook', 'Chama webhook HTTP externo', 'EXTERNAL', TRUE);
