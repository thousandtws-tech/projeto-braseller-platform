param(
    [string]$ResourceGroup = "rg-brasaller-tfstate",
    [string]$StorageAccount = "stbrasallerprodtfstate",
    [string]$Container = "tfstate",
    [string]$Location = "brazilsouth"
)

$ErrorActionPreference = "Stop"

$availability = az storage account check-name --name $StorageAccount -o json | ConvertFrom-Json
$existing = az storage account list `
    --query "[?name=='$StorageAccount'] | [0]" `
    -o json |
    ConvertFrom-Json

if (-not $existing) {
    if (-not $availability.nameAvailable) {
        throw "Storage account name '$StorageAccount' is unavailable: $($availability.reason)"
    }

    az group create `
        --name $ResourceGroup `
        --location $Location `
        --tags project=brasaller environment=prod managed_by=terraform-bootstrap `
        --output none

    az storage account create `
        --name $StorageAccount `
        --resource-group $ResourceGroup `
        --location $Location `
        --sku Standard_LRS `
        --kind StorageV2 `
        --min-tls-version TLS1_2 `
        --allow-blob-public-access false `
        --https-only true `
        --output none
}

$accountKey = az storage account keys list `
    --resource-group $ResourceGroup `
    --account-name $StorageAccount `
    --query "[0].value" `
    --output tsv

az storage container create `
    --name $Container `
    --account-name $StorageAccount `
    --account-key $accountKey `
    --public-access off `
    --output none

az storage account blob-service-properties update `
    --resource-group $ResourceGroup `
    --account-name $StorageAccount `
    --enable-versioning true `
    --enable-delete-retention true `
    --delete-retention-days 30 `
    --enable-container-delete-retention true `
    --container-delete-retention-days 30 `
    --output none

Write-Host "Azure Blob backend is ready."
Write-Host "Run: terraform init -migrate-state -force-copy -backend-config=backend.hcl"
