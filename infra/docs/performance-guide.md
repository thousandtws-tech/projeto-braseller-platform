# Guia de Performance e Resiliencia

Este guia descreve a configuracao local dos microservices Quarkus para alta concorrencia, baixa latencia e crescimento horizontal futuro sem Kubernetes, Docker Swarm ou cloud.

## Guardrails de Runtime

Todos os services usam os mesmos controles basicos:

- `HTTP_IDLE_TIMEOUT=30S`: evita conexoes ociosas segurando recursos.
- `HTTP_MAX_BODY_SIZE=2M`: limita payloads de entrada. Aumente por endpoint/servico apenas quando houver caso real.
- `GRACEFUL_SHUTDOWN_TIMEOUT=30S`: permite encerrar requisicoes em andamento antes do processo morrer.
- `APP_MAX_WORKER_THREADS=32`: limite finito para o executor de trabalho bloqueante do Quarkus. O valor e maior que `DB_POOL_MAX_SIZE=16`, mas ainda conservador para nao esconder saturacao do banco.
- `DB_POOL_MIN_SIZE=2` e `DB_POOL_MAX_SIZE=16`: suficiente para desenvolvimento e instancias pequenas. Aumentar pool sem medir costuma transferir o gargalo para o Postgres.
- `DB_ACQUISITION_TIMEOUT=5S`: falha rapido quando o pool esta esgotado, em vez de criar fila infinita.
- `DB_BACKGROUND_VALIDATION_INTERVAL=2M`, `DB_IDLE_REMOVAL_INTERVAL=5M` e `DB_MAX_LIFETIME=30M`: reciclam conexoes e reduzem chance de conexao morta ficar no pool.

Logs JSON ficam habilitados em `prod` por `LOG_JSON=true`. Prometheus exposto em `/q/metrics`; readiness e liveness em `/q/health/ready` e `/q/health/live`.

## Integracoes Externas

- Gateway REST client: `GATEWAY_DOWNSTREAM_CONNECT_TIMEOUT_MS=2000`, `GATEWAY_DOWNSTREAM_READ_TIMEOUT_MS=30000`.
- Notification -> Reporting REST client: `REPORTING_SERVICE_CONNECT_TIMEOUT_MS=2000`, `REPORTING_SERVICE_READ_TIMEOUT_MS=10000`.
- Auth HTTP clients: `AUTH_HTTP_CONNECT_TIMEOUT_MS=3000`, `AUTH_HTTP_REQUEST_TIMEOUT_MS=8000`.
- Mercado Livre HTTP client: `MERCADOLIVRE_CONNECT_TIMEOUT_MS=5000`, `MERCADOLIVRE_REQUEST_TIMEOUT_MS=15000`.

Timeouts curtos protegem workers. Se uma integracao precisar de mais tempo, prefira job consultavel por status com Quarkus Scheduler e persistencia no Postgres.

## Teste de Carga com hey

Instale o `hey` e suba o ambiente local:

```powershell
docker compose --env-file .env.example up --build
```

Os SLOs iniciais por rota estao em `docs/slo.md`. O script versionado executa os cenarios principais e salva o resultado em `.reports/performance`:

```powershell
.\scripts\performance\run-hey.ps1 -BaseUrl http://localhost:8080 -Jwt "<JWT>" -TenantId "<TENANT_ID>"
```

Faca aquecimento antes da medicao:

```powershell
hey -z 30s -c 20 http://localhost:8080/q/health/ready
```

Teste endpoint publico leve:

```powershell
hey -z 60s -c 100 http://localhost:8080/q/health/ready
```

Teste uma rota autenticada substituindo o token:

```powershell
hey -z 60s -c 50 -H "Authorization: Bearer <JWT>" http://localhost:8080/api/core/context
```

Teste Reporting com paginacao:

```powershell
hey -z 60s -c 50 -H "Authorization: Bearer <JWT>" "http://localhost:8080/api/reports/tenants/<TENANT_ID>/entries?size=50"
```

Teste enfileiramento de sincronizacao:

```powershell
hey -z 60s -c 20 -m POST -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" -d "{ \"since\": \"2026-05-01T00:00:00Z\" }" http://localhost:8080/api/core/connectors/sandbox/sync-all
```

## Como Medir

No output do `hey`, acompanhe:

- `Requests/sec`: throughput sustentado.
- `Average`: latencia media.
- `95%` e `99%`: p95 e p99. Esses percentis sao mais importantes que a media.
- `Status code distribution`: erros 5xx indicam saturacao ou bug; 4xx podem indicar token/payload incorreto.

No Prometheus/Grafana, acompanhe:

- Taxa de requisicoes HTTP por servico.
- Latencia maxima e soma/contagem de `http_server_requests`.
- Erros por status HTTP.
- `agroal`/datasource: conexoes ativas, disponiveis e tempo aguardando conexao.
- JVM: heap, GC, threads.
- Jobs internos: volume de `connector_sync_jobs`, `dre_calculation_jobs` e eventos pendentes em `messaging_outbox_events`.

## Revisao de Gargalos

- Chamadas JDBC sao bloqueantes. Os resources sincronicos do Quarkus devem permanecer no worker pool; evite mover JDBC para event loop.
- Exports PDF/XLSX/CSV podem consumir CPU e memoria proporcional ao numero de linhas. Manter filtros por periodo e paginacao nos endpoints interativos.
- `ReportFilter` e `ExpenseFilter` limitam pagina em 100 itens. Isso evita respostas enormes e protege o banco.
- Consultas de listagem usam `LIMIT/OFFSET`. Para bases grandes, migrar telas de alta cardinalidade para keyset pagination por `(sale_date, id)`.
- Busca textual com `LIKE '%termo%'` em `report_entries` pode ficar cara em alto volume. Avaliar indice trigram/FTS no Postgres quando houver volume real.
- Indices compostos em `report_entries` e `expense_entries` cobrem os filtros reais por tenant, periodo, status, meio de pagamento, release date e ordenacao de paginacao.
- Jobs de sincronizacao e DRE ficam em tabelas de controle (`connector_sync_jobs` e `dre_calculation_jobs`). Isso evita manter requisicoes HTTP abertas durante processamento pesado.
- Eventos internos passam pelo `messaging_outbox_events`: o request grava o evento no banco e um scheduler publica por REST interno ou executa o job local com retry controlado.
- Cache sugerido: preferencias de notificacao por tenant e catalogo de conectores. Use TTL curto e invalide em update; nao cacheie tokens nem dados financeiros mutaveis sem controle de consistencia.
- Evitar N+1: manter agregacoes no SQL, como ja ocorre no Reporting. Ao adicionar detalhes por pedido, preferir consulta agregada por lote.

## Checklist de Validacao

- Health: `/q/health/live` e `/q/health/ready` respondem 200 em todos os services.
- Metrics: `/q/metrics` exporta HTTP, JVM e datasource.
- Logs: em `prod`, saida JSON e com `x-request-id` propagado pelo gateway quando informado.
- Carga leve: `hey -c 20` sem 5xx e p95 estavel.
- Carga alvo: `hey -c 50` ou `-c 100` sem crescimento continuo de p99.
- Banco: conexoes ativas abaixo de `DB_POOL_MAX_SIZE`; tempo de aquisicao nao cresce continuamente.
- Jobs internos: filas voltam a zero apos pico; eventos no outbox nao ficam presos em `FAILED` ou `DEAD`.
- Shutdown: `docker compose stop <service>` encerra sem matar requisicoes em andamento antes de `GRACEFUL_SHUTDOWN_TIMEOUT`.
- Payload: requests acima de `HTTP_MAX_BODY_SIZE` falham rapidamente.
- SQL: rode `scripts/database/explain-reporting-hot-paths.sql` no Postgres com massa real e compare `actual time`, `rows` e `Buffers`.
- Segredos: rode `.\scripts\security\validate-prod-secrets.ps1 -EnvFile .env.production` antes de qualquer subida fora de desenvolvimento.

## Proximos Passos Enterprise

- Definir SLO por rota: throughput minimo, p95/p99 maximo e taxa de erro.
- Adicionar dashboards Grafana por servico com HTTP, JVM, DB, outbox e jobs internos.
- Criar testes de carga versionados por jornada critica.
- Adicionar cache local com TTL para preferencias pouco mutaveis.
- Migrar buscas textuais caras para indices especificos quando o volume justificar.
- Separar exports pesados em jobs assincronos com download posterior.
