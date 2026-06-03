-- agents
CREATE INDEX idx_agents_tenant_id           ON agents(tenant_id);
CREATE INDEX idx_agents_tenant_status       ON agents(tenant_id, status);

-- agent_goals
CREATE INDEX idx_goals_agent_id             ON agent_goals(agent_id);
CREATE INDEX idx_goals_tenant_status        ON agent_goals(tenant_id, status);
CREATE INDEX idx_goals_tenant_priority      ON agent_goals(tenant_id, priority DESC);

-- agent_executions
CREATE INDEX idx_executions_agent_id        ON agent_executions(agent_id);
CREATE INDEX idx_executions_tenant_status   ON agent_executions(tenant_id, status);
CREATE INDEX idx_executions_tenant_created  ON agent_executions(tenant_id, created_at DESC);
CREATE INDEX idx_executions_goal_id         ON agent_executions(goal_id);

-- agent_memories
CREATE INDEX idx_memories_agent_tenant      ON agent_memories(agent_id, tenant_id);
CREATE INDEX idx_memories_expires           ON agent_memories(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_memories_type              ON agent_memories(memory_type);

-- agent_decisions
CREATE INDEX idx_decisions_execution_id     ON agent_decisions(execution_id);
CREATE INDEX idx_decisions_agent_created    ON agent_decisions(agent_id, created_at DESC);

-- agent_actions
CREATE INDEX idx_actions_execution_id       ON agent_actions(execution_id);
CREATE INDEX idx_actions_agent_created      ON agent_actions(agent_id, created_at DESC);
CREATE INDEX idx_actions_status             ON agent_actions(status);

-- agent_feedbacks
CREATE INDEX idx_feedbacks_agent_id         ON agent_feedbacks(agent_id);
CREATE INDEX idx_feedbacks_execution_id     ON agent_feedbacks(execution_id);

-- agent_contexts
CREATE INDEX idx_contexts_agent_tenant      ON agent_contexts(agent_id, tenant_id);

-- outbox_events
CREATE INDEX idx_outbox_pending             ON outbox_events(created_at) WHERE processed_at IS NULL AND failed_at IS NULL;
CREATE INDEX idx_outbox_aggregate           ON outbox_events(aggregate_type, aggregate_id);
