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
  for_each = var.build_images_with_acr ? local.services : {}

  triggers_replace = [
    each.key,
    var.image_tag,
    tostring(var.swagger_ui_enabled)
  ]

  input = {
    registry_name      = azurerm_container_registry.main.name
    image_name         = each.key
    image_tag          = var.image_tag
    dockerfile         = "../../${each.value.context}/src/main/docker/Dockerfile.jvm"
    context            = "../../${each.value.context}"
    swagger_ui_enabled = tostring(var.swagger_ui_enabled)
  }

  provisioner "local-exec" {
    working_dir = path.module
    interpreter = ["PowerShell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command"]
    command     = "az acr build --registry \"${azurerm_container_registry.main.name}\" --image \"${each.key}:${var.image_tag}\" --build-arg \"SWAGGER_UI_ENABLED=${tostring(var.swagger_ui_enabled)}\" --file \"../../${each.value.context}/src/main/docker/Dockerfile.jvm\" \"../../${each.value.context}\""
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
      image  = "${azurerm_container_registry.main.login_server}/${each.key}:${var.image_tag}"
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
}
