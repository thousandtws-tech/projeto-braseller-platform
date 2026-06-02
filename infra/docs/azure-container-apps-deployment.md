# Hospedagem no Azure Container Apps

Projeto: BraSeller Sistema Modular

Objetivo: definir a estrategia profissional para hospedar os microservicos do projeto no Azure Container Apps, usando NeonDB como PostgreSQL gerenciado, Azure Container Registry, secrets seguros, observabilidade inicial e processamento interno com recursos do proprio Quarkus, sem broker no MVP.

## 1. Veredito Tecnico

Recomendacao aprovada:

- Usar Azure Container Apps para os microservicos.
- Usar NeonDB para PostgreSQL gerenciado.
- Usar Azure Container Registry para imagens Docker.
- Usar Azure Monitor e Log Analytics no inicio.
- Evitar AKS agora.
- Remover broker de eventos do MVP de producao.
- Usar Quarkus com REST interno, Scheduler, jobs locais e persistencia no PostgreSQL para os fluxos operacionais iniciais.

Por que Azure Container Apps faz sentido:

- Reduz operacao de Kubernetes.
- Aceita containers Docker dos microservicos atuais.
- Permite ingress externo apenas para o gateway e ingress interno para os demais servicos.
- Integra com Log Analytics, secrets, escalabilidade e revisoes.
- E suficiente para um SaaS inicial com microservicos Quarkus.

## 2. Arquitetura Alvo

```text
Internet
   |
   v
Azure Container Apps - gateway-api - ingress externo HTTPS
   |
   +--> auth-service          - ingress interno
   +--> user-service          - ingress interno
   +--> billing-service       - ingress interno
   +--> core-service          - ingress interno
   +--> notification-service  - ingress interno
   +--> reporting-service     - ingress interno
   |
   +--> Keycloak ou provedor auth gerenciado

NeonDB - PostgreSQL gerenciado
   +--> gateway_api
   +--> auth_service
   +--> user_service
   +--> billing_service
   +--> core_service
   +--> notification_service
   +--> reporting_service
   +--> keycloak, se Keycloak for hospedado pelo projeto

Azure Container Registry
   +--> brasaller/gateway-api
   +--> brasaller/auth-service
   +--> brasaller/user-service
   +--> brasaller/billing-service
   +--> brasaller/core-service
   +--> brasaller/notification-service
   +--> brasaller/reporting-service

Azure Monitor / Log Analytics
   +--> logs e metricas dos containers

Processamento interno do MVP
   +--> Quarkus REST Client para chamadas internas
   +--> Quarkus Scheduler para jobs recorrentes
   +--> tabelas de job/outbox no PostgreSQL quando precisar de retry/idempotencia
   +--> sem broker externo no primeiro momento
```

## 3. Mapeamento dos Servicos

| Servico | Azure Container App | Ingress | Porta | Min replicas | Max replicas | Observacao |
| --- | --- | --- | --- | --- | --- | --- |
| `gateway-api` | `gateway-api` | Externo | 8080 | 1 | 3 | Unico ponto publico `/api`. |
| `auth-service` | `auth-service` | Interno | 8080 | 1 | 2 | Emite JWT interno e integra com Keycloak. |
| `user-service` | `user-service` | Interno | 8080 | 1 | 2 | Tenants, usuarios e papeis. |
| `billing-service` | `billing-service` | Interno | 8080 | 1 | 2 | Planos, trial, assinatura e webhooks. |
| `core-service` | `core-service` | Interno | 8080 | 1 | 3 | Conectores de marketplace e sincronizacao. |
| `notification-service` | `notification-service` | Interno | 8080 | 1 | 2 | Notificacoes e jobs Quarkus Scheduler. |
| `reporting-service` | `reporting-service` | Interno | 8080 | 1 | 3 | Dashboard, relatorios e exportacao. |

Observacao importante: o projeto atual tambem usa Keycloak. O `auth-service` nao substitui o Keycloak; ele depende dele. Em producao, escolher uma das opcoes:

1. Hospedar Keycloak tambem em Azure Container Apps, com banco PostgreSQL proprio.
2. Usar Keycloak gerenciado/externo.
3. Substituir por Azure AD B2C, Auth0, Clerk ou outro provedor, exigindo ajuste no `auth-service`.

## 4. Estrategia de Banco de Dados - NeonDB

Para comecar de forma simples:

- Um projeto NeonDB.
- Uma branch de producao e uma branch de homologacao, se o plano permitir.
- Um database por microservico.
- Usuario/role unico administrativo para MVP, se o cliente quiser simplificar.
- Depois evoluir para usuario por servico com permissoes minimas.

Databases sugeridos:

```text
gateway_api
auth_service
user_service
billing_service
core_service
notification_service
reporting_service
keycloak, se Keycloak for mantido dentro do projeto
```

Recomendacoes para NeonDB:

- Usar `sslmode=require` em todas as URLs JDBC.
- Usar host direto, sem `-pooler`, enquanto `FLYWAY_MIGRATE_AT_START=true` estiver habilitado.
- Reduzir o pool JDBC de cada microservico no MVP para evitar excesso de conexoes.
- Quando as migracoes forem separadas em job proprio, avaliar host pooled (`-pooler`) para runtime e host direto para migracoes.
- Criar bancos e roles no painel/SQL editor do Neon antes do deploy.

Exemplo de JDBC por servico:

```text
jdbc:postgresql://ep-xxxxx.us-east-2.aws.neon.tech:5432/core_service?sslmode=require
```

## 5. Estrategia Sem Broker no MVP

Decisao atual:

- Nao usar broker de eventos agora.
- Nao usar Azure Service Bus agora.
- Nao usar RabbitMQ agora.
- Usar o proprio Quarkus e o PostgreSQL para coordenar o MVP.

Fluxos recomendados:

| Necessidade | Solucao no MVP |
| --- | --- |
| Core avisar venda nova | Chamada REST interna para `notification-service` ou persistencia de evento/job em tabela. |
| Reporting materializar lancamento | Chamada REST interna protegida por `X-Internal-Token` para `/reports/internal/entries`. |
| Jobs automaticos | `@Scheduled` do Quarkus em `notification-service` e/ou `reporting-service`. |
| Reprocessamento simples | Tabela de job/outbox no PostgreSQL com status `PENDING`, `PROCESSING`, `DONE`, `FAILED`. |
| Idempotencia | Chave unica por evento/pedido/job no banco. |
| Alertas | Azure Monitor + logs estruturados no inicio. |

Padrao de implementacao recomendado:

- Para operacoes imediatas e simples, usar REST interno entre servicos.
- Para operacoes demoradas, gravar job no banco e processar com Quarkus Scheduler.
- Para eventos criticos, usar tabela outbox antes de chamar outro servico.
- Manter interfaces/ports na aplicacao para permitir trocar por Service Bus no futuro, se o volume crescer.

Quando voltar a considerar broker:

- muitos tenants simultaneos;
- alto volume de pedidos;
- necessidade de consumo por multiplos servicos;
- DLQ e retentativas mais robustas;
- processamento distribuido real.

## 6. Variaveis e Secrets por Ambiente

Secrets nunca devem ser commitados. No Azure Container Apps, usar secrets do proprio Container App ou Azure Key Vault.

Secrets globais:

```text
AUTH_JWT_SECRET
INTERNAL_SERVICE_TOKEN
BILLING_WEBHOOK_TOKEN
MERCADOLIVRE_CLIENT_SECRET
KEYCLOAK_CLIENT_SECRET
```

Configuracoes globais:

```text
AUTH_JWT_ISSUER=brasaller-auth
AUTH_JWT_AUDIENCE=brasaller-platform
LOG_JSON=true
HTTP_ACCESS_LOG_ENABLED=true
FLYWAY_MIGRATE_AT_START=true
CORS_ORIGINS=https://app.seudominio.com.br,https://api.seudominio.com.br
```

URLs internas no Container Apps:

```text
AUTH_SERVICE_URL=http://auth-service:8080
USER_SERVICE_URL=http://user-service:8080
CORE_SERVICE_URL=http://core-service:8080
BILLING_SERVICE_URL=http://billing-service:8080
NOTIFICATION_SERVICE_URL=http://notification-service:8080
REPORTING_SERVICE_URL=http://reporting-service:8080
```

JDBC por servico:

```text
DB_JDBC_URL=jdbc:postgresql://<postgres-host>:5432/<database>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<secret>
```

## 7. Passo a Passo de Provisionamento

### 7.1. Pre-requisitos

Na maquina de deploy:

```powershell
az login
az account set --subscription "<SUBSCRIPTION_ID>"
az upgrade
az extension add --name containerapp --upgrade
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights
az provider register --namespace Microsoft.ContainerRegistry
```

### 7.2. Variaveis do deploy

```powershell
$LOCATION = "brazilsouth"
$RESOURCE_GROUP = "rg-brasaller-prod"
$ACR_NAME = "acrbrasallerprod"
$ACA_ENV = "cae-brasaller-prod"
$LOG_WORKSPACE = "log-brasaller-prod"
$NEON_HOST = "ep-xxxxx.us-east-2.aws.neon.tech"
$NEON_USERNAME = "neondb_owner"
```

### 7.3. Criar Resource Group

```powershell
az group create `
  --name $RESOURCE_GROUP `
  --location $LOCATION
```

### 7.4. Criar Azure Container Registry

```powershell
az acr create `
  --resource-group $RESOURCE_GROUP `
  --name $ACR_NAME `
  --sku Basic `
  --admin-enabled true
```

Para producao madura, preferir managed identity em vez de admin user do ACR.

### 7.5. Criar Log Analytics

```powershell
az monitor log-analytics workspace create `
  --resource-group $RESOURCE_GROUP `
  --workspace-name $LOG_WORKSPACE `
  --location $LOCATION
```

### 7.6. Criar ambiente Azure Container Apps

```powershell
$WORKSPACE_ID = az monitor log-analytics workspace show `
  --resource-group $RESOURCE_GROUP `
  --workspace-name $LOG_WORKSPACE `
  --query customerId `
  --output tsv

$WORKSPACE_KEY = az monitor log-analytics workspace get-shared-keys `
  --resource-group $RESOURCE_GROUP `
  --workspace-name $LOG_WORKSPACE `
  --query primarySharedKey `
  --output tsv

az containerapp env create `
  --name $ACA_ENV `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION `
  --logs-workspace-id $WORKSPACE_ID `
  --logs-workspace-key $WORKSPACE_KEY
```

### 7.7. Criar projeto e databases no NeonDB

No painel do Neon:

1. Criar projeto `brasaller-prod`.
2. Criar branch `prod`.
3. Copiar a connection string direta, sem `-pooler`, para uso inicial com Flyway.
4. Criar os databases abaixo no SQL Editor.
5. Guardar host, usuario e senha como secrets do deploy.

SQL sugerido:

```sql
CREATE DATABASE gateway_api;
CREATE DATABASE auth_service;
CREATE DATABASE user_service;
CREATE DATABASE billing_service;
CREATE DATABASE core_service;
CREATE DATABASE notification_service;
CREATE DATABASE reporting_service;
```

Se o Keycloak for mantido no projeto, criar tambem:

```sql
CREATE DATABASE keycloak;
```

Para MVP, o mesmo role pode ser usado por todos os bancos. Em producao madura, criar um role por servico e limitar permissoes.

### 7.8. Validar connection string Neon

Formato esperado para cada microservico:

```text
jdbc:postgresql://$NEON_HOST:5432/<database>?sslmode=require
```

Exemplo:

```text
jdbc:postgresql://ep-xxxxx.us-east-2.aws.neon.tech:5432/reporting_service?sslmode=require
```

Importante: enquanto as migrations Flyway rodam no startup dos microservicos, preferir host direto do Neon. O host pooled com `-pooler` e melhor para muitas conexoes concorrentes, mas deve ser usado com cuidado em ferramentas de migracao.

### 7.9. Build e push das imagens

```powershell
$TAG = "prod-$(Get-Date -Format yyyyMMddHHmm)"

$SERVICES = @(
  "gateway-api",
  "auth-service",
  "user-service",
  "billing-service",
  "core-service",
  "notification-service",
  "reporting-service"
)

foreach ($service in $SERVICES) {
  az acr build `
    --registry $ACR_NAME `
    --image "$service:$TAG" `
    --file "$service/src/main/docker/Dockerfile.jvm" `
    $service
}
```

### 7.10. Criar Container Apps

Ordem recomendada:

1. `auth-service`
2. `user-service`
3. `billing-service`
4. `core-service`
5. `reporting-service`
6. `notification-service`
7. `gateway-api`

Somente `gateway-api` deve ter ingress externo.

Exemplo para servico interno:

```powershell
az containerapp create `
  --name user-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ACA_ENV `
  --image "$ACR_NAME.azurecr.io/user-service:$TAG" `
  --registry-server "$ACR_NAME.azurecr.io" `
  --target-port 8080 `
  --ingress internal `
  --min-replicas 1 `
  --max-replicas 2 `
  --env-vars `
    LOG_JSON=true `
    HTTP_ACCESS_LOG_ENABLED=true `
    AUTH_JWT_ISSUER=brasaller-auth `
    AUTH_JWT_AUDIENCE=brasaller-platform `
    DB_USERNAME=$NEON_USERNAME `
    DB_POOL_MIN_SIZE=1 `
    DB_POOL_MAX_SIZE=4 `
    DB_JDBC_URL="jdbc:postgresql://$NEON_HOST:5432/user_service?sslmode=require"
```

Exemplo para gateway publico:

```powershell
az containerapp create `
  --name gateway-api `
  --resource-group $RESOURCE_GROUP `
  --environment $ACA_ENV `
  --image "$ACR_NAME.azurecr.io/gateway-api:$TAG" `
  --registry-server "$ACR_NAME.azurecr.io" `
  --target-port 8080 `
  --ingress external `
  --min-replicas 1 `
  --max-replicas 3 `
  --env-vars `
    LOG_JSON=true `
    HTTP_ACCESS_LOG_ENABLED=true `
    AUTH_SERVICE_URL=http://auth-service:8080 `
    USER_SERVICE_URL=http://user-service:8080 `
    CORE_SERVICE_URL=http://core-service:8080 `
    BILLING_SERVICE_URL=http://billing-service:8080 `
    NOTIFICATION_SERVICE_URL=http://notification-service:8080 `
    REPORTING_SERVICE_URL=http://reporting-service:8080 `
    DB_USERNAME=$NEON_USERNAME `
    DB_POOL_MIN_SIZE=1 `
    DB_POOL_MAX_SIZE=4 `
    DB_JDBC_URL="jdbc:postgresql://$NEON_HOST:5432/gateway_api?sslmode=require"
```

Para secrets, usar:

```powershell
az containerapp secret set `
  --name gateway-api `
  --resource-group $RESOURCE_GROUP `
  --secrets `
    db-password="<valor>" `
    auth-jwt-secret="<valor>" `
    internal-service-token="<valor>" `
    billing-webhook-token="<valor>"

az containerapp update `
  --name gateway-api `
  --resource-group $RESOURCE_GROUP `
  --set-env-vars `
    DB_PASSWORD=secretref:db-password `
    AUTH_JWT_SECRET=secretref:auth-jwt-secret `
    INTERNAL_SERVICE_TOKEN=secretref:internal-service-token `
    BILLING_WEBHOOK_TOKEN=secretref:billing-webhook-token
```

## 8. Script Template

Foi criado um script inicial em:

```text
scripts/azure/deploy-container-apps.ps1
```

Ele centraliza:

- criacao de Resource Group;
- ACR;
- Log Analytics;
- Container Apps Environment;
- build das imagens no ACR;
- criacao/atualizacao dos sete Container Apps;
- configuracao de ingress externo/interno;
- env vars principais.
- pool JDBC reduzido para NeonDB;
- configuracao das URLs internas entre `core-service`, `notification-service` e `reporting-service`.

Antes de executar, revise:

- subscription;
- nomes dos recursos;
- host PostgreSQL;
- connection string/host NeonDB;
- secrets;
- URLs de Keycloak;
- decisao de manter MVP sem broker externo.

## 9. DNS e Dominios

Recomendacao:

```text
api.seudominio.com.br       -> gateway-api
app.seudominio.com.br       -> frontend
auth.seudominio.com.br      -> Keycloak ou provedor auth
```

No Azure Container Apps:

- Configurar custom domain para `gateway-api`.
- Validar DNS conforme instrucoes do Azure.
- Usar certificado gerenciado quando disponivel.

Redirects OAuth importantes:

```text
MERCADOLIVRE_REDIRECT_URI=https://api.seudominio.com.br/integrations/mercado-livre/callback
KEYCLOAK_REDIRECT_URI=https://app.seudominio.com.br/auth/callback
```

## 10. Observabilidade Inicial

Inicio:

- Azure Monitor.
- Log Analytics.
- Logs JSON habilitados.
- Health checks `/q/health`.
- Metrics `/q/metrics`.

Depois:

- Grafana Cloud ou Grafana self-hosted.
- Dashboards por servico.
- Alertas de erro HTTP, latencia, consumo de CPU/memoria, falhas de jobs e fila/DLQ.

Alertas minimos:

- `gateway-api` indisponivel.
- Erro 5xx acima do normal.
- NeonDB com conexoes, storage ou compute proximos do limite do plano.
- Container reiniciando repetidamente.
- Jobs internos com status `FAILED`.
- Falha de autenticacao com Mercado Livre.

## 11. Pipeline CI/CD Recomendado

GitHub Actions:

1. Rodar testes Maven.
2. Build das imagens.
3. Push no ACR.
4. Deploy/update no Azure Container Apps.
5. Health check do gateway.
6. Smoke tests:
   - `GET /api`
   - `GET /api/core/connectors`
   - `GET /api/reports/...` com JWT de teste em homologacao.

Ambientes:

```text
develop -> homologacao
main    -> producao
```

Protecoes:

- Deploy de producao com aprovacao manual.
- Secrets no GitHub Actions, Azure Key Vault ou federated credentials.
- Tag por commit SHA.

## 12. Ajustes Necessarios no Projeto

Para hospedagem seria:

- Confirmar que todos os microservicos expoem porta `8080` no container.
- Manter `/q/health` habilitado.
- Usar `LOG_JSON=true`.
- Substituir URLs `localhost` por env vars.
- Confirmar CORS de producao.
- Remover dependencias de Docker Compose em runtime.
- Definir estrategia final de Keycloak.
- Manter comunicacao interna por REST/Quarkus Scheduler no MVP.

Para banco:

- MVP: `FLYWAY_MIGRATE_AT_START=true` com min replicas baixo e host direto do Neon.
- Producao madura: migracoes via Container App Job antes do rollout.
- Runtime com alta concorrencia: avaliar connection string pooled do Neon apos separar migracoes.

## 13. Checklist de Producao

Antes de abrir para cliente final:

- [ ] Dominios configurados.
- [ ] HTTPS validado.
- [ ] Secrets fora do repositorio.
- [ ] PostgreSQL com backup automatico.
- [ ] NeonDB com projeto/branch de producao configurado.
- [ ] Connection string direta validada para Flyway.
- [ ] Pool JDBC reduzido no MVP.
- [ ] Containers com logs JSON.
- [ ] Health checks respondendo.
- [ ] Gateway externo funcionando.
- [ ] Servicos internos sem ingress publico.
- [ ] CORS com dominio real do frontend.
- [ ] Mercado Livre OAuth com redirect URI final.
- [ ] Keycloak/provedor auth com redirect URI final.
- [ ] Plano de rollback.
- [ ] Alertas basicos criados.
- [ ] Custos monitorados por budget.
- [ ] Broker externo ausente do ambiente de producao.

## 14. Fontes Oficiais

- Azure Container Apps quickstart: https://learn.microsoft.com/en-us/azure/container-apps/get-started
- Ingress no Azure Container Apps: https://learn.microsoft.com/en-us/azure/container-apps/ingress-overview
- Variaveis e secrets no Container Apps: https://learn.microsoft.com/en-us/azure/container-apps/environment-variables
- Managed identity no Container Apps: https://learn.microsoft.com/en-us/azure/container-apps/managed-identity
- Neon connect from applications: https://neon.com/docs/connect/connect-from-any-app
- Neon connection pooling: https://neon.com/docs/connect/connection-pooling
- Quarkus Scheduler: https://quarkus.io/guides/scheduler
- Quarkus REST Client: https://quarkus.io/guides/rest-client
