# Load Tests — k6

Testes de carga para o Brasaller Sistema Modular usando [k6](https://k6.io/).
Todos os testes passam pelo `gateway-api` — ponto de entrada único do sistema.

## Pré-requisitos

```bash
# macOS
brew install k6

# Windows (winget)
winget install k6

# Docker (sem instalar)
docker run --rm -i grafana/k6 run - <scenarios/01_smoke.js
```

## Variáveis de ambiente

| Variável        | Padrão                                                          | Descrição                        |
|-----------------|-----------------------------------------------------------------|----------------------------------|
| `BASE_URL`      | `https://gateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io` | URL do gateway |
| `TEST_EMAIL`    | `loadtest@brasaller.com`                                        | E-mail do usuário de teste       |
| `TEST_PASSWORD` | `LoadTest@123`                                                  | Senha do usuário de teste        |

```bash
# Sobrescrever para apontar para staging/local
k6 run -e BASE_URL=http://localhost:8080 scenarios/01_smoke.js
```

> **Importante:** Crie o usuário `loadtest@brasaller.com` no ambiente alvo antes de rodar os testes que dependem de login.

## Cenários

| Arquivo                        | Objetivo                                           | VUs máx | Duração |
|--------------------------------|----------------------------------------------------|---------|---------|
| `scenarios/01_smoke.js`        | Sanidade básica — todos os endpoints essenciais    | 1       | 1 min   |
| `scenarios/02_load.js`         | Carga típica de produção — fluxo login/refresh     | 50      | ~12 min |
| `scenarios/03_stress.js`       | Encontrar ponto de ruptura — carga crescente       | 200     | ~20 min |
| `scenarios/04_circuit_breaker.js` | Validar Circuit Breaker — spike + recovery      | 30      | ~5 min  |
| `scenarios/05_gateway_routing.js` | Throughput puro do gateway — sem Keycloak       | 100     | ~8 min  |

## Executar

```bash
cd load-tests/k6

# Smoke (sempre rodar primeiro)
k6 run scenarios/01_smoke.js

# Load test com saída em JSON para análise
mkdir -p results
k6 run --out json=results/load.json scenarios/02_load.js

# Stress test
k6 run --out json=results/stress.json scenarios/03_stress.js

# Verificar circuit breaker (NÃO rodar em produção com usuários reais)
k6 run scenarios/04_circuit_breaker.js

# Gateway isolado
k6 run scenarios/05_gateway_routing.js
```

## Estrutura

```
k6/
├── config.js              # BASE_URL, credenciais, thresholds padrão
├── helpers/
│   └── auth.js            # login(), refresh(), logout(), bearerHeaders()
├── scenarios/
│   ├── 01_smoke.js
│   ├── 02_load.js
│   ├── 03_stress.js
│   ├── 04_circuit_breaker.js
│   └── 05_gateway_routing.js
└── results/               # arquivos JSON gerados (gitignore)
```

## Métricas relevantes

Após rodar qualquer cenário, observe no output:

- `http_req_duration` — latência geral (p50, p90, p95, p99)
- `http_req_duration{name:auth_login}` — latência específica do login (envolve Keycloak)
- `http_req_failed` — taxa de erros HTTP
- `circuit_breaker_trips` — contador de respostas com CB aberto (cenário 04)
- `login_latency_ms` — trend detalhada de login (cenário 03)

## Thresholds padrão (`config.js`)

```
p(95) < 2000ms  — 95% das requests completam em menos de 2s
p(99) < 5000ms  — 99% completam em menos de 5s
error rate < 5% — menos de 5% de falhas HTTP
```
