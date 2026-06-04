# Deploy de Microservices no Azure

Guia de referência para buildar e fazer redeploy de microservices no Azure Container Apps.

---

## Configuração

| Variável | Valor |
|---|---|
| ACR | `acrbrasallerprod` |
| Resource Group | `rg-brasaller-prod` |
| Image Tag | `prod` |

**Serviços disponíveis:**
`auth-service` · `user-service` · `billing-service` · `core-service` · `reporting-service` · `notification-service` · `gateway-api`

---

## Pré-requisitos

### 1. Login no Azure CLI
```powershell
az login
```

### 2. Login no Azure Container Registry
```powershell
az acr login --name acrbrasallerprod
```

> Execute estes dois passos uma vez por sessão. O token do ACR expira em ~3 horas.

---

## Deploy de um serviço específico

Substitua `<service>` pelo nome do serviço (ex: `reporting-service`).

### Passo 1 — Build da imagem Docker
```powershell
docker build `
  -f <service>/src/main/docker/Dockerfile.jvm `
  -t acrbrasallerprod.azurecr.io/<service>:prod `
  --build-arg SWAGGER_UI_ENABLED=false `
  <service>
```

### Passo 2 — Push para o ACR
```powershell
docker push acrbrasallerprod.azurecr.io/<service>:prod
```

### Passo 3 — Redeploy no Container Apps
```powershell
az containerapp update `
  --name <service> `
  --resource-group rg-brasaller-prod `
  --image acrbrasallerprod.azurecr.io/<service>:prod
```

---

## Deploy de múltiplos serviços

Para buildar e fazer deploy de dois ou mais serviços em sequência:

```powershell
# Defina os serviços que deseja atualizar
$services = @("reporting-service", "core-service")

$acr = "acrbrasallerprod"
$rg  = "rg-brasaller-prod"
$tag = "prod"

foreach ($svc in $services) {
    Write-Host "`n=== $svc ===" -ForegroundColor Cyan

    docker build `
        -f "$svc/src/main/docker/Dockerfile.jvm" `
        -t "$acr.azurecr.io/${svc}:$tag" `
        --build-arg SWAGGER_UI_ENABLED=false `
        $svc

    docker push "$acr.azurecr.io/${svc}:$tag"

    az containerapp update `
        --name $svc `
        --resource-group $rg `
        --image "$acr.azurecr.io/${svc}:$tag"

    Write-Host "Deploy de $svc concluido." -ForegroundColor Green
}
```

---

## Deploy completo (todos os serviços via Terraform)

Use apenas quando a infraestrutura precisar ser recriada ou quando variáveis de ambiente mudarem.

```powershell
cd infra/terraform

# Primeiro build — construir todas as imagens manualmente (ACR Tasks desabilitado)
$services = @("auth-service","user-service","billing-service","core-service","reporting-service","notification-service","gateway-api")
$acr = "acrbrasallerprod"

az acr login --name $acr

foreach ($svc in $services) {
    $dockerfile = if ($svc -eq "gateway-api") { "gateway-api/src/main/docker/Dockerfile.jvm" } else { "$svc/src/main/docker/Dockerfile.jvm" }
    docker build -f "../../$dockerfile" -t "$acr.azurecr.io/${svc}:prod" --build-arg SWAGGER_UI_ENABLED=false "../../$svc"
    docker push "$acr.azurecr.io/${svc}:prod"
}

# Aplicar infraestrutura (build_images_with_acr=false pois imagens já foram enviadas)
terraform apply
```

> **Nota:** `build_images_with_acr = false` está definido em `prod.auto.tfvars` porque o ACR Tasks não está habilitado nessa subscription. As imagens devem sempre ser buildadas localmente e enviadas via `docker push`.

---

## Verificar status após deploy

```powershell
# Ver revisões ativas de um serviço
az containerapp revision list `
  --name <service> `
  --resource-group rg-brasaller-prod `
  --output table

# Ver logs em tempo real
az containerapp logs show `
  --name <service> `
  --resource-group rg-brasaller-prod `
  --follow
```

---

## Troubleshooting

| Problema | Causa | Solução |
|---|---|---|
| `TasksOperationsNotAllowed` no `az acr build` | ACR Tasks não habilitado na subscription | Usar `docker build` + `docker push` local |
| `Premature end of Content-Length` no Maven | Falha de rede no download de dependência | Retentar o `docker build` |
| Container App não sobe após update | Erro na aplicação / health check falha | Ver logs com `az containerapp logs show` |
| Token expirado no ACR | Sessão de login expirou | Executar `az acr login --name acrbrasallerprod` novamente |
