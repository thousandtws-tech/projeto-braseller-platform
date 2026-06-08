locals {
  client_webapp_name = coalesce(var.client_webapp_name, "app-${local.resource_prefix}-client")
}

resource "azurerm_service_plan" "client" {
  count = var.deploy_client ? 1 : 0

  name                = "asp-${local.resource_prefix}-client"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  os_type             = "Linux"
  sku_name            = var.client_app_service_sku_name
  tags                = local.tags
}

resource "azurerm_linux_web_app" "client" {
  count = var.deploy_client ? 1 : 0

  name                = local.client_webapp_name
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  service_plan_id     = azurerm_service_plan.client[0].id
  https_only          = true
  tags                = local.tags

  identity {
    type = "SystemAssigned"
  }

  site_config {
    always_on = true

    application_stack {
      docker_image_name   = "brasaller-client:${var.client_image_tag}"
      docker_registry_url = "https://${azurerm_container_registry.main.login_server}"
    }
  }

  app_settings = {
    GATEWAY_URL                = var.client_gateway_url
    NEXT_TELEMETRY_DISABLED    = "1"
    WEBSITES_PORT              = "3000"
    DOCKER_ENABLE_CI           = "true"
    acrUseManagedIdentityCreds = "true"
  }

  logs {
    http_logs {
      file_system {
        retention_in_days = 7
        retention_in_mb   = 35
      }
    }
    application_logs {
      file_system_level = "Warning"
    }
  }
}

resource "azurerm_role_assignment" "client_acr_pull" {
  count = var.deploy_client ? 1 : 0

  scope                = azurerm_container_registry.main.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_linux_web_app.client[0].identity[0].principal_id
}
