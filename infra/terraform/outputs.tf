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

output "core_realtime_ws_public_url" {
  description = "Value for CORE_REALTIME_WS_PUBLIC_URL in the Next.js runtime."
  value       = "wss://${azurerm_container_app.services["gateway-api"].ingress[0].fqdn}/api/core/connectors/events/ws"
}

output "client_realtime_environment" {
  description = "Runtime environment values to configure on the deployed Next.js client."
  value = {
    GATEWAY_URL                 = "https://${azurerm_container_app.services["gateway-api"].ingress[0].fqdn}"
    CORE_REALTIME_WS_PUBLIC_URL = "wss://${azurerm_container_app.services["gateway-api"].ingress[0].fqdn}/api/core/connectors/events/ws"
  }
}

output "container_app_fqdns" {
  description = "FQDNs generated for each Container App."
  value = {
    for name, app in azurerm_container_app.services :
    name => app.ingress[0].fqdn
  }
}
