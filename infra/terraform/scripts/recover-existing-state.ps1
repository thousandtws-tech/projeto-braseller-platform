param(
    [string]$ResourceGroup = "rg-brasaller-prod",
    [string]$SubscriptionId = ""
)

$ErrorActionPreference = "Stop"

function Invoke-TerraformImport {
    param(
        [Parameter(Mandatory = $true)][string]$Address,
        [Parameter(Mandatory = $true)][string]$ResourceId
    )

    $known = @()
    if (Test-Path "terraform.tfstate") {
        $known = & terraform state list
    }
    if ($known -contains $Address) {
        Write-Host "State already contains $Address"
        return
    }

    Write-Host "Importing $Address"
    $escapedAddress = $Address -replace '"', '\"'
    & terraform import $escapedAddress $ResourceId
    if ($LASTEXITCODE -ne 0) {
        throw "Could not import $Address"
    }
}

if ($SubscriptionId) {
    az account set --subscription $SubscriptionId
}

$account = az account show --query "{id:id,state:state}" -o json | ConvertFrom-Json
if ($account.state -ne "Enabled") {
    throw "The active Azure subscription is not enabled."
}

$subscription = $account.id
$resourceGroupId = "/subscriptions/$subscription/resourceGroups/$ResourceGroup"
$acr = az acr list --resource-group $ResourceGroup --query "[0].{id:id,name:name}" -o json | ConvertFrom-Json
$workspace = az monitor log-analytics workspace list --resource-group $ResourceGroup --query "[0].{id:id,name:name}" -o json | ConvertFrom-Json
$environment = az containerapp env list --resource-group $ResourceGroup --query "[0].{id:id,name:name}" -o json | ConvertFrom-Json
$identityName = "id-brasaller-prod-apps"
$identity = az identity show --resource-group $ResourceGroup --name $identityName -o json |
    ConvertFrom-Json
$identity.id = $identity.id -replace "/resourcegroups/", "/resourceGroups/"
$roleAssignments = az role assignment list --scope $acr.id -o json | ConvertFrom-Json
$acrPullAssignment = $roleAssignments |
    Where-Object {
        $_.roleDefinitionName -eq "AcrPull" -and
        $_.principalId -eq $identity.principalId
    } |
    Select-Object -First 1
$acrPull = $acrPullAssignment.id

if (-not $acr -or -not $workspace -or -not $environment -or -not $identity -or -not $acrPull) {
    $missing = @()
    if (-not $acr) { $missing += "container registry" }
    if (-not $workspace) { $missing += "log analytics workspace" }
    if (-not $environment) { $missing += "container apps environment" }
    if (-not $identity) { $missing += "managed identity" }
    if (-not $acrPull) { $missing += "AcrPull role assignment" }
    throw "Required Azure resources were not discovered: $($missing -join ', ')."
}

Invoke-TerraformImport "azurerm_resource_group.main" $resourceGroupId
Invoke-TerraformImport "azurerm_log_analytics_workspace.main" $workspace.id
Invoke-TerraformImport "azurerm_container_registry.main" $acr.id
Invoke-TerraformImport "azurerm_container_app_environment.main" $environment.id
Invoke-TerraformImport "azurerm_user_assigned_identity.container_apps" $identity.id
Invoke-TerraformImport "azurerm_role_assignment.acr_pull" $acrPull

$services = @(
    "auth-service",
    "user-service",
    "billing-service",
    "core-service",
    "reporting-service",
    "notification-service",
    "gateway-api"
)

foreach ($service in $services) {
    $id = az containerapp show --resource-group $ResourceGroup --name $service --query id -o tsv
    if (-not $id) {
        throw "Container App '$service' was not found."
    }
    $id = $id -replace "/containerapps/", "/containerApps/"
    Invoke-TerraformImport "azurerm_container_app.services[`"$service`"]" $id
}

Write-Host ""
Write-Host "State recovery completed."
Write-Host "Run: terraform plan -refresh-only"
Write-Host "Then run a normal plan and review every non-realtime change."
