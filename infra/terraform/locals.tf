locals {
  resource_prefix = "${var.project_name}-${var.environment}"

  resource_group_name  = coalesce(var.resource_group_name, "rg-${local.resource_prefix}")
  log_analytics_name   = coalesce(var.log_analytics_workspace_name, "log-${local.resource_prefix}")
  aca_environment_name = coalesce(var.container_apps_environment_name, "cae-${local.resource_prefix}")
  acr_name             = coalesce(var.acr_name, lower(replace("acr${var.project_name}${var.environment}", "-", "")))
  identity_name        = coalesce(var.container_apps_identity_name, "id-${local.resource_prefix}-apps")

  default_tags = {
    project     = var.project_name
    environment = var.environment
    managed_by  = "terraform"
  }

  tags = merge(local.default_tags, var.tags)

  default_service_database_names = {
    "auth-service"         = "auth_service"
    "user-service"         = "user_service"
    "billing-service"      = "billing_service"
    "core-service"         = "core_service"
    "reporting-service"    = "reporting_service"
    "notification-service" = "notification_service"
    "gateway-api"          = "gateway_api"
  }

  service_database_names = merge(local.default_service_database_names, var.service_database_names)

  default_service_scaling = {
    "auth-service" = {
      min_replicas             = 2
      max_replicas             = 8
      http_concurrent_requests = 40
    }
    "user-service" = {
      min_replicas             = 2
      max_replicas             = 8
      http_concurrent_requests = 40
    }
    "billing-service" = {
      min_replicas             = 2
      max_replicas             = 8
      http_concurrent_requests = 30
    }
    "core-service" = {
      min_replicas             = 2
      max_replicas             = 10
      http_concurrent_requests = 30
    }
    "reporting-service" = {
      min_replicas             = 2
      max_replicas             = 10
      http_concurrent_requests = 30
    }
    "notification-service" = {
      min_replicas             = 2
      max_replicas             = 8
      http_concurrent_requests = 30
    }
    "gateway-api" = {
      min_replicas             = 3
      max_replicas             = 20
      http_concurrent_requests = 60
    }
  }

  services = {
    "auth-service" = {
      context                  = "auth-service"
      database                 = local.service_database_names["auth-service"]
      ingress_external         = false
      min_replicas             = lookup(var.service_min_replicas, "auth-service", local.default_service_scaling["auth-service"].min_replicas)
      max_replicas             = lookup(var.service_max_replicas, "auth-service", local.default_service_scaling["auth-service"].max_replicas)
      http_concurrent_requests = lookup(var.service_http_concurrent_requests, "auth-service", local.default_service_scaling["auth-service"].http_concurrent_requests)
      cpu                      = 0.5
      memory                   = "1Gi"
    }
    "user-service" = {
      context                  = "user-service"
      database                 = local.service_database_names["user-service"]
      ingress_external         = false
      min_replicas             = lookup(var.service_min_replicas, "user-service", local.default_service_scaling["user-service"].min_replicas)
      max_replicas             = lookup(var.service_max_replicas, "user-service", local.default_service_scaling["user-service"].max_replicas)
      http_concurrent_requests = lookup(var.service_http_concurrent_requests, "user-service", local.default_service_scaling["user-service"].http_concurrent_requests)
      cpu                      = 0.5
      memory                   = "1Gi"
    }
    "billing-service" = {
      context                  = "billing-service"
      database                 = local.service_database_names["billing-service"]
      ingress_external         = false
      min_replicas             = lookup(var.service_min_replicas, "billing-service", local.default_service_scaling["billing-service"].min_replicas)
      max_replicas             = lookup(var.service_max_replicas, "billing-service", local.default_service_scaling["billing-service"].max_replicas)
      http_concurrent_requests = lookup(var.service_http_concurrent_requests, "billing-service", local.default_service_scaling["billing-service"].http_concurrent_requests)
      cpu                      = 0.5
      memory                   = "1Gi"
    }
    "core-service" = {
      context                  = "core-service"
      database                 = local.service_database_names["core-service"]
      ingress_external         = false
      min_replicas             = lookup(var.service_min_replicas, "core-service", local.default_service_scaling["core-service"].min_replicas)
      max_replicas             = lookup(var.service_max_replicas, "core-service", local.default_service_scaling["core-service"].max_replicas)
      http_concurrent_requests = lookup(var.service_http_concurrent_requests, "core-service", local.default_service_scaling["core-service"].http_concurrent_requests)
      cpu                      = 1.0
      memory                   = "2Gi"
    }
    "reporting-service" = {
      context                  = "reporting-service"
      database                 = local.service_database_names["reporting-service"]
      ingress_external         = false
      min_replicas             = lookup(var.service_min_replicas, "reporting-service", local.default_service_scaling["reporting-service"].min_replicas)
      max_replicas             = lookup(var.service_max_replicas, "reporting-service", local.default_service_scaling["reporting-service"].max_replicas)
      http_concurrent_requests = lookup(var.service_http_concurrent_requests, "reporting-service", local.default_service_scaling["reporting-service"].http_concurrent_requests)
      cpu                      = 1.0
      memory                   = "2Gi"
    }
    "notification-service" = {
      context                  = "notification-service"
      database                 = local.service_database_names["notification-service"]
      ingress_external         = false
      min_replicas             = lookup(var.service_min_replicas, "notification-service", local.default_service_scaling["notification-service"].min_replicas)
      max_replicas             = lookup(var.service_max_replicas, "notification-service", local.default_service_scaling["notification-service"].max_replicas)
      http_concurrent_requests = lookup(var.service_http_concurrent_requests, "notification-service", local.default_service_scaling["notification-service"].http_concurrent_requests)
      cpu                      = 0.5
      memory                   = "1Gi"
    }
    "gateway-api" = {
      context                  = "gateway-api"
      database                 = local.service_database_names["gateway-api"]
      ingress_external         = true
      min_replicas             = lookup(var.service_min_replicas, "gateway-api", local.default_service_scaling["gateway-api"].min_replicas)
      max_replicas             = lookup(var.service_max_replicas, "gateway-api", local.default_service_scaling["gateway-api"].max_replicas)
      http_concurrent_requests = lookup(var.service_http_concurrent_requests, "gateway-api", local.default_service_scaling["gateway-api"].http_concurrent_requests)
      cpu                      = 1.0
      memory                   = "2Gi"
    }
  }

  internal_service_urls = {
    "auth-service"         = "https://auth-service.internal.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io"
    "user-service"         = "https://user-service.internal.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io"
    "billing-service"      = "https://billing-service.internal.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io"
    "core-service"         = "https://core-service.internal.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io"
    "reporting-service"    = "https://reporting-service.internal.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io"
    "notification-service" = "https://notification-service.internal.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io"
  }

  common_env_plain = {
    HTTP_PORT                 = "8080"
    HTTP_IDLE_TIMEOUT         = var.http_idle_timeout
    HTTP_MAX_BODY_SIZE        = var.http_max_body_size
    DB_KIND                   = "postgresql"
    DB_POOL_MIN_SIZE          = tostring(var.db_pool_min_size)
    DB_POOL_MAX_SIZE          = tostring(var.db_pool_max_size)
    FLYWAY_MIGRATE_AT_START   = tostring(var.flyway_migrate_at_start)
    CORS_ORIGINS              = join(",", var.cors_origins)
    AUTH_JWT_ISSUER           = var.auth_jwt_issuer
    AUTH_JWT_AUDIENCE         = var.auth_jwt_audience
    LOG_JSON                  = tostring(var.log_json)
    HTTP_ACCESS_LOG_ENABLED   = tostring(var.http_access_log_enabled)
    SWAGGER_UI_ENABLED        = tostring(var.swagger_ui_enabled)
    GRACEFUL_SHUTDOWN_TIMEOUT = var.graceful_shutdown_timeout
    APP_MAX_WORKER_THREADS    = tostring(var.app_max_worker_threads)
  }

  common_env_secret_refs = {
    DB_PASSWORD            = "db-password"
    AUTH_JWT_SECRET        = "auth-jwt-secret"
    INTERNAL_SERVICE_TOKEN = "internal-service-token"
  }

  service_env_plain = {
    "auth-service" = {
      USER_SERVICE_URL                = local.internal_service_urls["user-service"]
      AUTH_ACCESS_TOKEN_TTL_SECONDS   = tostring(var.auth_access_token_ttl_seconds)
      KEYCLOAK_BASE_URL               = var.keycloak_base_url
      KEYCLOAK_PUBLIC_BASE_URL        = var.keycloak_public_base_url
      KEYCLOAK_REALM                  = var.keycloak_realm
      KEYCLOAK_CLIENT_ID              = var.keycloak_client_id
      KEYCLOAK_REDIRECT_URI           = var.keycloak_redirect_uri
      KEYCLOAK_SCOPE                  = var.keycloak_scope
      KEYCLOAK_REQUIRE_EMAIL_VERIFIED = tostring(var.keycloak_require_email_verified)
      KEYCLOAK_ADMIN_USERNAME         = var.keycloak_admin_username
      AUTH_HTTP_CONNECT_TIMEOUT_MS    = tostring(var.auth_http_connect_timeout_ms)
      AUTH_HTTP_REQUEST_TIMEOUT_MS    = tostring(var.auth_http_request_timeout_ms)
    }
    "user-service" = {
      KEYCLOAK_ADMIN_URL      = var.keycloak_base_url
      KEYCLOAK_ADMIN_REALM    = var.keycloak_realm
      KEYCLOAK_ADMIN_USERNAME = var.keycloak_admin_username
    }
    "billing-service" = {}
    "core-service" = {
      NOTIFICATION_SERVICE_URL                   = local.internal_service_urls["notification-service"]
      REPORTING_SERVICE_URL                      = local.internal_service_urls["reporting-service"]
      MERCADOLIVRE_API_BASE_URL                  = var.mercadolivre_api_base_url
      MERCADOLIVRE_CLIENT_ID                     = var.mercadolivre_client_id
      MERCADOLIVRE_REDIRECT_URI                  = var.mercadolivre_redirect_uri
      MERCADOLIVRE_CONNECT_TIMEOUT_MS            = tostring(var.mercadolivre_connect_timeout_ms)
      MERCADOLIVRE_REQUEST_TIMEOUT_MS            = tostring(var.mercadolivre_request_timeout_ms)
      MERCADOLIVRE_REFRESH_SKEW_SECONDS          = tostring(var.mercadolivre_refresh_skew_seconds)
      MERCADOLIVRE_RETRY_MAX_ATTEMPTS            = tostring(var.mercadolivre_retry_max_attempts)
      MERCADOLIVRE_RETRY_INITIAL_DELAY_MS        = tostring(var.mercadolivre_retry_initial_delay_ms)
      MERCADOLIVRE_RETRY_MAX_DELAY_MS            = tostring(var.mercadolivre_retry_max_delay_ms)
      SHOPEE_PARTNER_ID                          = tostring(var.shopee_partner_id)
      SHOPEE_CONNECT_TIMEOUT_MS                  = tostring(var.shopee_connect_timeout_ms)
      SHOPEE_REQUEST_TIMEOUT_MS                  = tostring(var.shopee_request_timeout_ms)
      SHOPEE_RETRY_MAX_ATTEMPTS                  = tostring(var.shopee_retry_max_attempts)
      AMAZON_CLIENT_ID                           = var.amazon_client_id
      AMAZON_CONNECT_TIMEOUT_MS                  = tostring(var.amazon_connect_timeout_ms)
      AMAZON_REQUEST_TIMEOUT_MS                  = tostring(var.amazon_request_timeout_ms)
      AMAZON_RETRY_MAX_ATTEMPTS                  = tostring(var.amazon_retry_max_attempts)
      NOTIFICATION_SERVICE_CONNECT_TIMEOUT_MS    = tostring(var.notification_service_connect_timeout_ms)
      NOTIFICATION_SERVICE_READ_TIMEOUT_MS       = tostring(var.notification_service_read_timeout_ms)
      REPORTING_SERVICE_CONNECT_TIMEOUT_MS       = tostring(var.reporting_service_connect_timeout_ms)
      REPORTING_SERVICE_READ_TIMEOUT_MS          = tostring(var.reporting_service_read_timeout_ms)
      MESSAGING_OUTBOX_DISPATCH_EVERY            = var.messaging_outbox_dispatch_every
      MESSAGING_OUTBOX_BATCH_SIZE                = tostring(var.messaging_outbox_batch_size)
      MESSAGING_OUTBOX_MAX_ATTEMPTS              = tostring(var.messaging_outbox_max_attempts)
      MESSAGING_OUTBOX_RETRY_DELAY_SECONDS       = tostring(var.messaging_outbox_retry_delay_seconds)
      MESSAGING_OUTBOX_IN_FLIGHT_TIMEOUT_SECONDS = tostring(var.messaging_outbox_in_flight_timeout_seconds)
    }
    "reporting-service" = {
      CLOUDINARY_CLOUD_NAME                      = var.cloudinary_cloud_name
      CLOUDINARY_API_KEY                         = var.cloudinary_api_key
      CLOUDINARY_EXPENSE_FOLDER                  = var.cloudinary_expense_folder
      CLOUDINARY_RESOURCE_TYPE                   = var.cloudinary_resource_type
      CLICKSIGN_BASE_URL                         = var.clicksign_base_url
      CLICKSIGN_DEFAULT_DEADLINE_DAYS            = tostring(var.clicksign_default_deadline_days)
      MESSAGING_OUTBOX_DISPATCH_EVERY            = var.messaging_outbox_dispatch_every
      MESSAGING_OUTBOX_BATCH_SIZE                = tostring(var.messaging_outbox_batch_size)
      MESSAGING_OUTBOX_MAX_ATTEMPTS              = tostring(var.messaging_outbox_max_attempts)
      MESSAGING_OUTBOX_RETRY_DELAY_SECONDS       = tostring(var.messaging_outbox_retry_delay_seconds)
      MESSAGING_OUTBOX_IN_FLIGHT_TIMEOUT_SECONDS = tostring(var.messaging_outbox_in_flight_timeout_seconds)
    }
    "notification-service" = {
      REPORTING_SERVICE_URL                          = local.internal_service_urls["reporting-service"]
      NOTIFICATION_MAIL_FROM                         = var.notification_mail_from
      NOTIFICATION_ML_PAYMENT_RELEASE_LOOKAHEAD_DAYS = tostring(var.notification_ml_payment_release_lookahead_days)
      NOTIFICATION_MONTHLY_CLOSING_CRON              = var.notification_monthly_closing_cron
      NOTIFICATION_ML_PAYMENT_RELEASE_CRON           = var.notification_ml_payment_release_cron
      NOTIFICATION_WEEKLY_ACCOUNTANT_REPORT_CRON     = var.notification_weekly_accountant_report_cron
      REPORTING_SERVICE_CONNECT_TIMEOUT_MS           = tostring(var.reporting_service_connect_timeout_ms)
      REPORTING_SERVICE_READ_TIMEOUT_MS              = tostring(var.reporting_service_read_timeout_ms)
      SMTP_HOST                                      = var.smtp_host
      SMTP_PORT                                      = tostring(var.smtp_port)
      SMTP_USERNAME                                  = var.smtp_username
      SMTP_MOCK                                      = tostring(var.smtp_mock)
    }
    "gateway-api" = {
      AUTH_SERVICE_URL         = local.internal_service_urls["auth-service"]
      USER_SERVICE_URL         = local.internal_service_urls["user-service"]
      CORE_SERVICE_URL         = local.internal_service_urls["core-service"]
      BILLING_SERVICE_URL      = local.internal_service_urls["billing-service"]
      NOTIFICATION_SERVICE_URL = local.internal_service_urls["notification-service"]
      REPORTING_SERVICE_URL    = local.internal_service_urls["reporting-service"]

      AUTH_SERVICE_OPENAPI_URL         = "/api/auth/q/openapi"
      USER_SERVICE_OPENAPI_URL         = "/api/users/q/openapi"
      CORE_SERVICE_OPENAPI_URL         = "/api/core/q/openapi"
      BILLING_SERVICE_OPENAPI_URL      = "/api/billing/q/openapi"
      NOTIFICATION_SERVICE_OPENAPI_URL = "/api/notifications/q/openapi"
      REPORTING_SERVICE_OPENAPI_URL    = "/api/reports/q/openapi"

      GATEWAY_DOWNSTREAM_CONNECT_TIMEOUT_MS = tostring(var.gateway_downstream_connect_timeout_ms)
      GATEWAY_DOWNSTREAM_READ_TIMEOUT_MS    = tostring(var.gateway_downstream_read_timeout_ms)
      SWAGGER_UI_ENABLED                    = "true"
    }
  }

  service_env_secret_refs = {
    "auth-service" = {
      KEYCLOAK_CLIENT_SECRET  = "keycloak-client-secret"
      KEYCLOAK_ADMIN_PASSWORD = "keycloak-admin-password"
    }
    "user-service" = {
      KEYCLOAK_ADMIN_PASSWORD = "keycloak-admin-password"
    }
    "billing-service" = {
      BILLING_WEBHOOK_TOKEN = "billing-webhook-token"
    }
    "core-service" = {
      CONNECTOR_TOKEN_ENCRYPTION_KEY = "connector-token-encryption-key"
      MERCADOLIVRE_CLIENT_SECRET     = "mercadolivre-client-secret"
      SHOPEE_PARTNER_KEY             = "shopee-partner-key"
      AMAZON_CLIENT_SECRET           = "amazon-client-secret"
      AMAZON_AWS_ACCESS_KEY          = "amazon-aws-access-key"
      AMAZON_AWS_SECRET_KEY          = "amazon-aws-secret-key"
    }
    "reporting-service" = {
      CLOUDINARY_API_SECRET    = "cloudinary-api-secret"
      CLICKSIGN_ACCESS_TOKEN   = "clicksign-access-token"
      CLICKSIGN_WEBHOOK_SECRET = "clicksign-webhook-secret"
    }
    "notification-service" = {
      SMTP_PASSWORD = "smtp-password"
    }
    "gateway-api" = {
      BILLING_WEBHOOK_TOKEN = "billing-webhook-token"
    }
  }

  container_app_env_plain = {
    for service_name, service in local.services :
    service_name => merge(
      local.common_env_plain,
      {
        DB_USERNAME = local.service_database_config[service_name].username
        DB_JDBC_URL = coalesce(local.service_database_config[service_name].jdbc_url, "jdbc:postgresql://${local.service_database_config[service_name].host}:${local.service_database_config[service_name].port}/${local.service_database_config[service_name].database}?sslmode=${local.service_database_config[service_name].sslmode}")
      },
      local.service_env_plain[service_name],
      lookup(var.extra_env, service_name, {})
    )
  }

  container_app_env_secret_refs = {
    for service_name, _ in local.services :
    service_name => merge(
      local.common_env_secret_refs,
      local.service_env_secret_refs[service_name]
    )
  }

  container_app_secret_names = {
    for service_name, refs in local.container_app_env_secret_refs :
    service_name => toset(distinct(values(refs)))
  }

  service_database_config = {
    for service_name, service in local.services :
    service_name => {
      host     = coalesce(try(var.service_database_credentials[service_name].host, null), var.postgres_host)
      port     = coalesce(try(var.service_database_credentials[service_name].port, null), var.postgres_port)
      database = coalesce(try(var.service_database_credentials[service_name].database, null), service.database)
      username = coalesce(try(var.service_database_credentials[service_name].username, null), var.postgres_username)
      password = coalesce(try(var.service_database_credentials[service_name].password, null), var.postgres_password)
      sslmode  = coalesce(try(var.service_database_credentials[service_name].sslmode, null), var.postgres_sslmode)
      jdbc_url = try(var.service_database_credentials[service_name].jdbc_url, null)
    }
  }

  secret_values_by_service = {
    for service_name, _ in local.services :
    service_name => {
      "db-password"                    = local.service_database_config[service_name].password
      "auth-jwt-secret"                = var.auth_jwt_secret
      "internal-service-token"         = var.internal_service_token
      "billing-webhook-token"          = var.billing_webhook_token
      "connector-token-encryption-key" = var.connector_token_encryption_key
      "keycloak-client-secret"         = var.keycloak_client_secret
      "keycloak-admin-password"        = var.keycloak_admin_password
      "mercadolivre-client-secret"     = var.mercadolivre_client_secret
      "shopee-partner-key"             = var.shopee_partner_key
      "amazon-client-secret"           = var.amazon_client_secret
      "amazon-aws-access-key"          = var.amazon_aws_access_key
      "amazon-aws-secret-key"          = var.amazon_aws_secret_key
      "cloudinary-api-secret"          = var.cloudinary_api_secret
      "clicksign-access-token"         = var.clicksign_access_token
      "clicksign-webhook-secret"       = var.clicksign_webhook_secret
      "smtp-password"                  = var.smtp_password
    }
  }
}
