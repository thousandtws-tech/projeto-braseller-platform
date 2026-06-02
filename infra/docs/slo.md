# SLOs de Performance por Rota

Estes SLOs sao metas iniciais para ambiente local/servidor pequeno usando Docker Compose. Ajuste depois de medir em hardware real e com dados representativos.

| Rota | Tipo | Concorrencia de referencia | Throughput minimo | p95 alvo | p99 alvo | Erro 5xx maximo |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `GET /q/health/ready` | readiness | 100 | 500 req/s | 100 ms | 250 ms | 0.1% |
| `GET /api/core/context` | autenticada leve | 50 | 150 req/s | 250 ms | 750 ms | 0.5% |
| `GET /api/reports/tenants/{tenantId}/entries?size=50` | leitura paginada | 50 | 80 req/s | 500 ms | 1200 ms | 0.5% |
| `POST /api/core/connectors/{connector}/sync-all` | job assincrono | 20 | 30 req/s | 400 ms | 1000 ms | 0.5% |
| `GET /api/core/connectors/sync-jobs/{jobId}` | consulta de job | 50 | 120 req/s | 300 ms | 800 ms | 0.5% |
| `POST /api/reports/tenants/{tenantId}/dre/calculations` | job DRE assincrono | 20 | 25 req/s | 400 ms | 1000 ms | 0.5% |

## Regras de Leitura

- `p95` e `p99` devem ser medidos depois de um warm-up de pelo menos 30 segundos.
- Um SLO so vale quando o banco nao esta usando dados artificiais minusculos; carregue massa representativa antes de aprovar.
- Para rotas que exigem JWT, use token real de um tenant de teste e nunca cole token de producao no terminal compartilhado.
- Rotas mutantes devem ser executadas com tenant isolado para nao poluir dados de validacao funcional.

## Comando Padrao

Use o script versionado:

```powershell
.\scripts\performance\run-hey.ps1 -BaseUrl http://localhost:8080 -Jwt "<JWT>" -TenantId "<TENANT_ID>"
```

Para incluir rotas que criam jobs:

```powershell
.\scripts\performance\run-hey.ps1 -BaseUrl http://localhost:8080 -Jwt "<JWT>" -TenantId "<TENANT_ID>" -IncludeMutating
```
