# Terraform Azure Deploy

Stack Terraform para publicar os microservicos Quarkus do Brasaller no Azure Container Apps.

## O que cria

- Resource Group.
- Azure Container Registry com admin desabilitado.
- Log Analytics Workspace.
- Azure Container Apps Environment.
- User Assigned Managed Identity com permissao `AcrPull`.
- Sete Azure Container Apps:
  - `gateway-api` com ingress externo.
  - `auth-service`, `user-service`, `billing-service`, `core-service`, `notification-service` e `reporting-service` com ingress interno.

O banco PostgreSQL continua externo ao Terraform, seguindo a decisao documentada em `docs/azure-container-apps-deployment.md`: crie os databases no Neon ou em outro PostgreSQL antes do `apply`.

## Pre-requisitos

- Terraform >= 1.6.
- Azure CLI autenticado com `az login`.
- Permissao na subscription para criar Resource Group, ACR, Log Analytics, Managed Identity, Role Assignment e Container Apps.
- Providers Azure registrados:

```powershell
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights
az provider register --namespace Microsoft.ContainerRegistry
az provider register --namespace Microsoft.ManagedIdentity
```

## Uso

No diretorio `infra/terraform`, crie um arquivo `prod.auto.tfvars` baseado em `terraform.tfvars.example` e ajuste:

- `subscription_id`
- `acr_name`
- `image_tag`
- `postgres_host`, `postgres_username`, `postgres_password`
- secrets da plataforma
- URLs do Keycloak, CORS e integracoes externas

Depois execute:

```powershell
cd infra/terraform
terraform init
terraform plan -out brasaller.tfplan
terraform apply brasaller.tfplan
```

Por padrao, `build_images_with_acr = true`; durante o `apply`, o Terraform executa `az acr build` para cada servico usando os Dockerfiles `src/main/docker/Dockerfile.jvm`. Para ambientes com CI/CD, deixe `build_images_with_acr = false`, publique as imagens no ACR com a mesma `image_tag` e rode o `apply`.

## Bancos esperados

Com os defaults, as URLs JDBC geradas apontam para:

- `auth_service`
- `user_service`
- `billing_service`
- `core_service`
- `reporting_service`
- `notification_service`
- `gateway_api`

Para mudar nomes, use `service_database_names`:

```hcl
service_database_names = {
  "gateway-api" = "gateway_api_prod"
}
```

Se cada microservico ja tiver credenciais proprias, use `service_database_credentials`:

```hcl
service_database_credentials = {
  "gateway-api" = {
    host     = "ep-xxxxx.us-east-2.aws.neon.tech"
    database = "gateway_api"
    username = "gateway-api"
    password = "valor-secreto"
    jdbc_url = "jdbc:postgresql://ep-xxxxx.us-east-2.aws.neon.tech/gateway_api?sslmode=require"
  }
}
```

## Secrets e estado

Os secrets configurados nos Container Apps ficam no estado do Terraform. Para producao, use backend remoto criptografado e com acesso restrito, por exemplo Azure Storage com versionamento e RBAC.

## Saidas

Ao final do `apply`, use:

```powershell
terraform output gateway_url
terraform output container_app_fqdns
```

O primeiro endpoint publico esperado e o `gateway-api`; os demais apps ficam acessiveis apenas dentro do Container Apps Environment.

## Alta disponibilidade e escala

O Azure Container Apps ja distribui o trafego entre replicas saudaveis no ingress do proprio ambiente. Esta stack configura esse balanceamento com replicas minimas, autoscale por concorrencia HTTP, probes e desligamento gracioso:

- `gateway-api`: minimo 3 replicas, maximo 20, escala acima de 60 requisicoes concorrentes por replica.
- Microservicos internos: minimo 2 replicas, maximo entre 8 e 10, escala entre 30 e 40 requisicoes concorrentes por replica.
- `startup_probe` evita matar uma revisao enquanto Quarkus/Flyway ainda esta iniciando.
- `readiness_probe` tira replicas nao prontas do trafego.
- `termination_grace_period_seconds = 60` e `GRACEFUL_SHUTDOWN_TIMEOUT = 60S` reduzem perda de requisicoes durante deploy/restart.

Para ajustar capacidade sem editar os arquivos, use overrides no `prod.auto.tfvars`:

```hcl
service_min_replicas = {
  "gateway-api"     = 4
  "billing-service" = 3
}

service_max_replicas = {
  "gateway-api"        = 30
  "reporting-service" = 15
}

service_http_concurrent_requests = {
  "gateway-api"        = 50
  "reporting-service" = 20
}
```

Use thresholds menores para escalar mais cedo quando houver lentidao; use valores maiores quando as replicas estiverem com CPU, memoria e banco folgados. Depois do `apply`, acompanhe `Replicas`, `Requests`, `Response Time`, CPU e memoria no Azure Portal ou Log Analytics antes de subir limites de forma agressiva.
