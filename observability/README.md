# Observability

Stack local de observabilidade para os microservices Brasaller.

## Prometheus

Configurado em `prometheus.yml` para raspar `/q/metrics` dos servicos pela rede interna do Docker Compose:

- `gateway-api:8080`
- `core-service:8080`
- `billing-service:8080`
- `notification-service:8080`
- `user-service:8080`
- `auth-service:8080`

## Grafana

O Grafana usa provisioning declarativo:

- Datasource: `grafana/provisioning/datasources/prometheus.yml`
- Dashboard provider: `grafana/provisioning/dashboards/brasaller.yml`
- Dashboard: `grafana/dashboards/brasaller/microservices-overview.json`

URL local: `http://localhost:3001`

Credenciais padrao:

- Usuario: `admin`
- Senha: `admin`

Altere `GRAFANA_ADMIN_USER` e `GRAFANA_ADMIN_PASSWORD` no `.env.example` ou no seu `.env` local antes de subir o ambiente.
