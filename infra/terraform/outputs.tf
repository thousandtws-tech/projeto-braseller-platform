output "resource_group_name" {
  description = "Azure resource group name."
  value       = azurerm_resource_group.main.name
}

output "container_registry_login_server" {
  description = "Azure Container Registry login server."
  value       = azurerm_container_registry.main.login_server
}

output "container_apps_environment_name" {
  description = "Azure Container Apps environment name."
  value       = azurerm_container_app_environment.main.name
}

output "container_apps_environment_default_domain" {
  description = "Default Container Apps environment domain."
  value       = azurerm_container_app_environment.main.default_domain
}

output "gateway_fqdn" {
  description = "Public FQDN for gateway-api."
  value       = azurerm_container_app.services["gateway-api"].ingress[0].fqdn
}

output "gateway_url" {
  description = "Public HTTPS URL for gateway-api."
  value       = "https://${azurerm_container_app.services["gateway-api"].ingress[0].fqdn}"
}

output "container_app_fqdns" {
  description = "FQDNs generated for each Container App."
  value = {
    for name, app in azurerm_container_app.services :
    name => app.ingress[0].fqdn
  }
}

output "client_webapp_name" {
  description = "Azure Web App name for the Next.js client."
  value       = var.deploy_client ? azurerm_linux_web_app.client[0].name : null
}

output "client_webapp_url" {
  description = "Default HTTPS URL for the Next.js client."
  value       = var.deploy_client ? "https://${azurerm_linux_web_app.client[0].default_hostname}" : null
}
