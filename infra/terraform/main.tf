resource "azurerm_resource_group" "main" {
  name     = local.resource_group_name
  location = var.location
  tags     = local.tags
}

resource "azurerm_log_analytics_workspace" "main" {
  name                = local.log_analytics_name
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = var.log_analytics_sku
  retention_in_days   = var.log_analytics_retention_days
  tags                = local.tags
}

resource "azurerm_container_registry" "main" {
  name                = local.acr_name
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = var.acr_sku
  admin_enabled       = false
  tags                = local.tags
}

resource "azurerm_container_app_environment" "main" {
  name                       = local.aca_environment_name
  location                   = azurerm_resource_group.main.location
  resource_group_name        = azurerm_resource_group.main.name
  logs_destination           = "log-analytics"
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id
  tags                       = local.tags
}

resource "azurerm_user_assigned_identity" "container_apps" {
  name                = local.identity_name
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.tags
}

resource "azurerm_role_assignment" "acr_pull" {
  scope                = azurerm_container_registry.main.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_user_assigned_identity.container_apps.principal_id
}

resource "terraform_data" "acr_build" {
  for_each = local.acr_build_services

  triggers_replace = [
    each.key,
    local.service_image_tags[each.key],
    tostring(var.swagger_ui_enabled)
  ]

  input = {
    registry_name      = azurerm_container_registry.main.name
    image_name         = each.key
    image_tag          = local.service_image_tags[each.key]
    dockerfile         = "../../${each.value.context}/src/main/docker/Dockerfile.jvm"
    context            = "../../${each.value.context}"
    swagger_ui_enabled = tostring(var.swagger_ui_enabled)
  }

  provisioner "local-exec" {
    working_dir = path.module
    interpreter = ["PowerShell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command"]
    command     = "az acr build --registry \"${azurerm_container_registry.main.name}\" --image \"${each.key}:${local.service_image_tags[each.key]}\" --build-arg \"SWAGGER_UI_ENABLED=${tostring(var.swagger_ui_enabled)}\" --file \"../../${each.value.context}/src/main/docker/Dockerfile.jvm\" \"../../${each.value.context}\""
  }
}

resource "azurerm_container_app" "services" {
  for_each = local.services

  name                         = each.key
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"
  tags                         = local.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.container_apps.id]
  }

  registry {
    server   = azurerm_container_registry.main.login_server
    identity = azurerm_user_assigned_identity.container_apps.id
  }

  dynamic "secret" {
    for_each = local.container_app_secret_names[each.key]

    content {
      name  = secret.value
      value = local.secret_values_by_service[each.key][secret.value]
    }
  }

  ingress {
    external_enabled           = each.value.ingress_external
    target_port                = 8080
    transport                  = "auto"
    allow_insecure_connections = false

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas                     = each.value.min_replicas
    max_replicas                     = each.value.max_replicas
    polling_interval_in_seconds      = var.container_app_scale_polling_interval_seconds
    cooldown_period_in_seconds       = var.container_app_scale_cooldown_seconds
    termination_grace_period_seconds = var.container_app_termination_grace_period_seconds

    http_scale_rule {
      name                = "http-concurrency"
      concurrent_requests = tostring(each.value.http_concurrent_requests)
    }

    container {
      name   = each.key
      image  = "${azurerm_container_registry.main.login_server}/${each.key}:${local.service_image_tags[each.key]}"
      cpu    = each.value.cpu
      memory = each.value.memory

      startup_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/q/health/live"
        initial_delay           = 10
        interval_seconds        = 5
        timeout                 = 5
        failure_count_threshold = 24
      }

      liveness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/q/health/live"
        initial_delay           = 60
        interval_seconds        = 30
        timeout                 = 5
        failure_count_threshold = 3
      }

      readiness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/q/health/ready"
        interval_seconds        = 10
        timeout                 = 5
        failure_count_threshold = 6
        success_count_threshold = 1
      }

      dynamic "env" {
        for_each = local.container_app_env_plain[each.key]

        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = local.container_app_env_secret_refs[each.key]

        content {
          name        = env.key
          secret_name = env.value
        }
      }
    }
  }

  depends_on = [
    azurerm_role_assignment.acr_pull,
    terraform_data.acr_build
  ]

  lifecycle {
    # Existing production apps were created before their state was recovered.
    # Preserve runtime configuration and secret values during adoption. New,
    # intentionally managed runtime values are patched by terraform_data below.
    ignore_changes = [
      secret,
      template[0].container[0].env
    ]
  }
}

resource "terraform_data" "core_realtime_secret" {
  triggers_replace = [
    sha256(var.realtime_ticket_secret)
  ]

  provisioner "local-exec" {
    working_dir = path.module
    interpreter = ["PowerShell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command"]
    environment = {
      REALTIME_TICKET_SECRET = var.realtime_ticket_secret
    }
    command = "az containerapp secret set --resource-group \"${azurerm_resource_group.main.name}\" --name \"core-service\" --secrets \"realtime-ticket-secret=$env:REALTIME_TICKET_SECRET\" --output none"
  }

  depends_on = [azurerm_container_app.services["core-service"]]
}

resource "terraform_data" "core_realtime_runtime" {
  triggers_replace = [
    var.core_realtime_http_idle_timeout,
    var.core_realtime_poll_interval,
    tostring(var.core_realtime_batch_size),
    tostring(var.core_realtime_ticket_ttl_seconds),
    tostring(var.core_realtime_retention_days)
  ]

  provisioner "local-exec" {
    working_dir = path.module
    interpreter = ["PowerShell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command"]
    command     = "az containerapp update --resource-group \"${azurerm_resource_group.main.name}\" --name \"core-service\" --set-env-vars \"HTTP_IDLE_TIMEOUT=${var.core_realtime_http_idle_timeout}\" \"REALTIME_POLL_INTERVAL=${var.core_realtime_poll_interval}\" \"REALTIME_BATCH_SIZE=${var.core_realtime_batch_size}\" \"REALTIME_TICKET_TTL_SECONDS=${var.core_realtime_ticket_ttl_seconds}\" \"REALTIME_RETENTION_DAYS=${var.core_realtime_retention_days}\" \"REALTIME_TICKET_SECRET=secretref:realtime-ticket-secret\" --output none"
  }

  depends_on = [terraform_data.core_realtime_secret]
}

resource "terraform_data" "gateway_realtime_runtime" {
  triggers_replace = [
    tostring(var.gateway_realtime_connect_timeout_ms),
    tostring(var.gateway_realtime_read_timeout_ms),
    tostring(var.gateway_realtime_max_connections),
    var.gateway_realtime_auto_ping_interval,
    var.gateway_realtime_tls_configuration_name
  ]

  provisioner "local-exec" {
    working_dir = path.module
    interpreter = ["PowerShell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command"]
    command     = "az containerapp update --resource-group \"${azurerm_resource_group.main.name}\" --name \"gateway-api\" --set-env-vars \"CORE_REALTIME_CONNECT_TIMEOUT_MS=${var.gateway_realtime_connect_timeout_ms}\" \"CORE_REALTIME_READ_TIMEOUT_MS=${var.gateway_realtime_read_timeout_ms}\" \"CORE_REALTIME_TLS_CONFIGURATION_NAME=${var.gateway_realtime_tls_configuration_name}\" \"REALTIME_MAX_CONNECTIONS=${var.gateway_realtime_max_connections}\" \"REALTIME_AUTO_PING_INTERVAL=${var.gateway_realtime_auto_ping_interval}\" --output none"
  }

  depends_on = [azurerm_container_app.services["gateway-api"]]
}
