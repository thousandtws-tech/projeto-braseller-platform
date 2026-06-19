variable "subscription_id" {
  description = "Azure subscription ID. Leave null to use ARM_SUBSCRIPTION_ID or the current Azure CLI context."
  type        = string
  default     = null

  validation {
    condition = (
      var.subscription_id == null ||
      (
        can(regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", var.subscription_id)) &&
        var.subscription_id != "00000000-0000-0000-0000-000000000000"
      )
    )
    error_message = "subscription_id must be a real Azure subscription UUID; omit it to use the active Azure CLI subscription."
  }
}

variable "location" {
  description = "Azure region for the resources."
  type        = string
  default     = "brazilsouth"
}

variable "project_name" {
  description = "Short project name used in Azure resource names and tags."
  type        = string
  default     = "brasaller"
}

variable "environment" {
  description = "Environment name, for example prod or hml."
  type        = string
  default     = "prod"
}

variable "resource_group_name" {
  description = "Optional existing or custom resource group name."
  type        = string
  default     = null
}

variable "container_apps_environment_name" {
  description = "Optional custom Azure Container Apps environment name."
  type        = string
  default     = null
}

variable "container_apps_identity_name" {
  description = "Optional custom user assigned identity name for Container Apps."
  type        = string
  default     = null
}

variable "log_analytics_workspace_name" {
  description = "Optional custom Log Analytics workspace name."
  type        = string
  default     = null
}

variable "log_analytics_sku" {
  description = "Log Analytics SKU."
  type        = string
  default     = "PerGB2018"
}

variable "log_analytics_retention_days" {
  description = "Log retention in days."
  type        = number
  default     = 30
}

variable "acr_name" {
  description = "Globally unique Azure Container Registry name. Lowercase letters and numbers only."
  type        = string
  default     = null

  validation {
    condition     = var.acr_name == null || can(regex("^[a-z0-9]{5,50}$", var.acr_name))
    error_message = "acr_name must have 5 to 50 lowercase alphanumeric characters."
  }
}

variable "acr_sku" {
  description = "Azure Container Registry SKU."
  type        = string
  default     = "Basic"
}

variable "image_tag" {
  description = "Container image tag used for all microservices."
  type        = string
  default     = "manual"
}

variable "build_images_with_acr" {
  description = "When true, Terraform runs az acr build for each service before creating/updating Container Apps."
  type        = bool
  default     = true
}

variable "postgres_host" {
  description = "PostgreSQL host, for example the direct Neon host without jdbc:postgresql://."
  type        = string
}

variable "postgres_port" {
  description = "PostgreSQL port."
  type        = number
  default     = 5432
}

variable "postgres_username" {
  description = "PostgreSQL username used by the services."
  type        = string
}

variable "postgres_password" {
  description = "PostgreSQL password used by the services."
  type        = string
  sensitive   = true
}

variable "postgres_sslmode" {
  description = "PostgreSQL sslmode appended to JDBC URLs."
  type        = string
  default     = "require"
}

variable "service_database_names" {
  description = "Optional database name overrides by service name."
  type        = map(string)
  default     = {}
}

variable "service_database_credentials" {
  description = "Optional PostgreSQL connection overrides by service name. Use this when each microservice already has its own database user/password."
  type = map(object({
    host     = optional(string)
    port     = optional(number)
    database = optional(string)
    username = optional(string)
    password = optional(string)
    sslmode  = optional(string)
    jdbc_url = optional(string)
  }))
  default   = {}
  sensitive = true
}

variable "db_pool_min_size" {
  description = "Minimum JDBC pool size per service."
  type        = number
  default     = 1
}

variable "db_pool_max_size" {
  description = "Maximum JDBC pool size per service."
  type        = number
  default     = 4
}

variable "flyway_migrate_at_start" {
  description = "Run Flyway migrations at service startup."
  type        = bool
  default     = true
}

variable "cors_origins" {
  description = "Allowed CORS origins."
  type        = list(string)
  default     = ["http://localhost:3000", "http://localhost:4200"]
}

variable "auth_jwt_issuer" {
  description = "JWT issuer accepted by the platform."
  type        = string
  default     = "brasaller-auth"
}

variable "auth_jwt_audience" {
  description = "JWT audience accepted by the platform."
  type        = string
  default     = "brasaller-platform"
}

variable "auth_jwt_secret" {
  description = "HMAC secret used by auth and downstream services. Use at least 32 bytes."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.auth_jwt_secret) >= 32
    error_message = "auth_jwt_secret must have at least 32 characters."
  }
}

variable "internal_service_token" {
  description = "Shared token for internal service-to-service endpoints."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.internal_service_token) >= 32
    error_message = "internal_service_token must have at least 32 characters."
  }
}

variable "billing_webhook_token" {
  description = "Token used to authorize billing webhooks."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.billing_webhook_token) >= 32
    error_message = "billing_webhook_token must have at least 32 characters."
  }
}

variable "connector_token_encryption_key" {
  description = "Secret used by core-service to encrypt marketplace connector tokens."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.connector_token_encryption_key) >= 32
    error_message = "connector_token_encryption_key must have at least 32 characters."
  }
}

variable "realtime_ticket_secret" {
  description = "HMAC secret used by core-service to issue short-lived WebSocket tickets."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.realtime_ticket_secret) >= 32
    error_message = "realtime_ticket_secret must have at least 32 characters."
  }
}

variable "core_realtime_http_idle_timeout" {
  description = "Quarkus HTTP idle timeout for long-lived SSE and WebSocket connections."
  type        = string
  default     = "5M"
}

variable "core_realtime_poll_interval" {
  description = "Interval used by core-service to poll the durable realtime event log."
  type        = string
  default     = "750ms"
}

variable "core_realtime_batch_size" {
  description = "Maximum number of durable connector events read per realtime poll."
  type        = number
  default     = 500

  validation {
    condition     = var.core_realtime_batch_size >= 1 && var.core_realtime_batch_size <= 1000
    error_message = "core_realtime_batch_size must be between 1 and 1000."
  }
}

variable "core_realtime_ticket_ttl_seconds" {
  description = "Lifetime of signed WebSocket connection tickets."
  type        = number
  default     = 45

  validation {
    condition     = var.core_realtime_ticket_ttl_seconds >= 15 && var.core_realtime_ticket_ttl_seconds <= 300
    error_message = "core_realtime_ticket_ttl_seconds must be between 15 and 300."
  }
}

variable "core_realtime_retention_days" {
  description = "Retention window for replayable connector realtime events."
  type        = number
  default     = 7

  validation {
    condition     = var.core_realtime_retention_days >= 1
    error_message = "core_realtime_retention_days must be greater than or equal to 1."
  }
}

variable "auth_access_token_ttl_seconds" {
  description = "Access token TTL issued by auth-service."
  type        = number
  default     = 900
}

variable "keycloak_base_url" {
  description = "Internal or server-side Keycloak base URL."
  type        = string
  default     = "https://keycloak-production-96de.up.railway.app"
}

variable "keycloak_public_base_url" {
  description = "Public Keycloak base URL used in browser redirects."
  type        = string
  default     = "https://keycloak-production-96de.up.railway.app"
}

variable "keycloak_realm" {
  description = "Keycloak realm."
  type        = string
  default     = "brasaller"
}

variable "keycloak_client_id" {
  description = "Keycloak confidential client ID."
  type        = string
  default     = "auth-service"
}

variable "keycloak_client_secret" {
  description = "Keycloak client secret."
  type        = string
  sensitive   = true
}

variable "keycloak_redirect_uri" {
  description = "OAuth callback URL registered in Keycloak."
  type        = string
  default     = "https://app.seudominio.com.br/auth/callback"
}

variable "keycloak_scope" {
  description = "OIDC scopes requested from Keycloak."
  type        = string
  default     = "openid email profile"
}

variable "keycloak_require_email_verified" {
  description = "Require verified email for Keycloak identities."
  type        = bool
  default     = true
}

variable "keycloak_admin_username" {
  description = "Keycloak admin username used by auth-service when needed."
  type        = string
  default     = "admin"
}

variable "keycloak_admin_password" {
  description = "Keycloak admin password used by auth-service when needed."
  type        = string
  sensitive   = true
}

variable "auth_http_connect_timeout_ms" {
  description = "Auth service downstream connect timeout."
  type        = number
  default     = 3000
}

variable "auth_http_request_timeout_ms" {
  description = "Auth service downstream request timeout."
  type        = number
  default     = 8000
}

variable "mercadolivre_api_base_url" {
  description = "Mercado Livre API base URL."
  type        = string
  default     = "https://api.mercadolibre.com"
}

variable "mercadolivre_client_id" {
  description = "Mercado Livre OAuth client ID."
  type        = string
  default     = ""
}

variable "mercadolivre_client_secret" {
  description = "Mercado Livre OAuth client secret."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "mercadolivre_redirect_uri" {
  description = "Mercado Livre OAuth redirect URI."
  type        = string
  default     = ""
}

variable "mercadolivre_connect_timeout_ms" {
  description = "Mercado Livre API connect timeout."
  type        = number
  default     = 5000
}

variable "mercadolivre_request_timeout_ms" {
  description = "Mercado Livre API request timeout."
  type        = number
  default     = 15000
}

variable "mercadolivre_refresh_skew_seconds" {
  description = "Mercado Livre refresh skew in seconds."
  type        = number
  default     = 300
}

variable "shopee_partner_id" {
  description = "Shopee Open Platform partner ID (app ID)."
  type        = number
  default     = 0
}

variable "shopee_partner_key" {
  description = "Shopee Open Platform partner key (app secret)."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "shopee_connect_timeout_ms" {
  description = "Shopee API connect timeout in milliseconds."
  type        = number
  default     = 5000
}

variable "shopee_request_timeout_ms" {
  description = "Shopee API request timeout in milliseconds."
  type        = number
  default     = 15000
}

variable "shopee_retry_max_attempts" {
  description = "Max retry attempts on Shopee API 429 responses."
  type        = number
  default     = 3
}

variable "amazon_client_id" {
  description = "Amazon SP-API LWA client ID."
  type        = string
  default     = "not-configured"
}

variable "amazon_client_secret" {
  description = "Amazon SP-API LWA client secret."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "amazon_aws_access_key" {
  description = "AWS IAM access key for Amazon SP-API SigV4 signing."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "amazon_aws_secret_key" {
  description = "AWS IAM secret key for Amazon SP-API SigV4 signing."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "amazon_connect_timeout_ms" {
  description = "Amazon SP-API connect timeout in milliseconds."
  type        = number
  default     = 5000
}

variable "amazon_request_timeout_ms" {
  description = "Amazon SP-API request timeout in milliseconds."
  type        = number
  default     = 15000
}

variable "amazon_retry_max_attempts" {
  description = "Max retry attempts on Amazon SP-API 429 responses."
  type        = number
  default     = 3
}

variable "mercadolivre_retry_max_attempts" {
  description = "Max retry attempts on Mercado Livre API 429 responses."
  type        = number
  default     = 3
}

variable "mercadolivre_retry_initial_delay_ms" {
  description = "Initial backoff delay in milliseconds for Mercado Livre API retries."
  type        = number
  default     = 1000
}

variable "mercadolivre_retry_max_delay_ms" {
  description = "Maximum backoff delay in milliseconds for Mercado Livre API retries."
  type        = number
  default     = 30000
}

variable "cloudinary_cloud_name" {
  description = "Cloudinary cloud name used by reporting-service."
  type        = string
  default     = "not-configured"
}

variable "cloudinary_api_key" {
  description = "Cloudinary API key used by reporting-service."
  type        = string
  default     = "not-configured"
}

variable "cloudinary_api_secret" {
  description = "Cloudinary API secret used by reporting-service."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "cloudinary_expense_folder" {
  description = "Cloudinary folder for expense attachments."
  type        = string
  default     = "brasaller/despesas"
}

variable "cloudinary_resource_type" {
  description = "Cloudinary resource type."
  type        = string
  default     = "auto"
}

variable "clicksign_base_url" {
  description = "Clicksign API base URL."
  type        = string
  default     = "https://sandbox.clicksign.com/api/v3"
}

variable "clicksign_access_token" {
  description = "Clicksign access token."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "clicksign_webhook_secret" {
  description = "Clicksign webhook secret."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "clicksign_default_deadline_days" {
  description = "Default Clicksign deadline in days."
  type        = number
  default     = 7
}

variable "notification_mail_from" {
  description = "Default sender e-mail for notification-service."
  type        = string
  default     = "no-reply@braseller.local"
}

variable "notification_ml_payment_release_lookahead_days" {
  description = "Lookahead window for Mercado Livre payment release alerts."
  type        = number
  default     = 2
}

variable "notification_monthly_closing_cron" {
  description = "Monthly closing notification cron expression."
  type        = string
  default     = "0 0 8 1 * ?"
}

variable "notification_ml_payment_release_cron" {
  description = "Mercado Livre payment release notification cron expression."
  type        = string
  default     = "0 0/30 * * * ?"
}

variable "notification_weekly_accountant_report_cron" {
  description = "Weekly accountant report cron expression."
  type        = string
  default     = "0 0 8 ? * MON"
}

variable "smtp_host" {
  description = "SMTP host used by notification-service."
  type        = string
  default     = "localhost"
}

variable "smtp_port" {
  description = "SMTP port used by notification-service."
  type        = number
  default     = 1025
}

variable "smtp_username" {
  description = "SMTP username used by notification-service."
  type        = string
  default     = ""
}

variable "smtp_password" {
  description = "SMTP password used by notification-service."
  type        = string
  sensitive   = true
  default     = "not-configured"
}

variable "smtp_mock" {
  description = "Use Quarkus mailer mock mode."
  type        = bool
  default     = false
}

variable "email_verification_hash_secret" {
  description = "Secret used to hash email verification codes in user-service."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.email_verification_hash_secret) >= 32
    error_message = "email_verification_hash_secret must have at least 32 characters."
  }
}

variable "notification_service_connect_timeout_ms" {
  description = "Core service connect timeout for notification-service."
  type        = number
  default     = 2000
}

variable "notification_service_read_timeout_ms" {
  description = "Core service read timeout for notification-service."
  type        = number
  default     = 10000
}

variable "reporting_service_connect_timeout_ms" {
  description = "Connect timeout for reporting-service REST clients."
  type        = number
  default     = 2000
}

variable "reporting_service_read_timeout_ms" {
  description = "Read timeout for reporting-service REST clients."
  type        = number
  default     = 10000
}

variable "messaging_outbox_dispatch_every" {
  description = "Outbox dispatcher interval."
  type        = string
  default     = "5s"
}

variable "messaging_outbox_batch_size" {
  description = "Outbox batch size."
  type        = number
  default     = 50
}

variable "messaging_outbox_max_attempts" {
  description = "Outbox max attempts."
  type        = number
  default     = 10
}

variable "messaging_outbox_retry_delay_seconds" {
  description = "Outbox retry delay in seconds."
  type        = number
  default     = 30
}

variable "messaging_outbox_in_flight_timeout_seconds" {
  description = "Outbox in-flight timeout in seconds."
  type        = number
  default     = 300
}

variable "log_json" {
  description = "Enable JSON logs."
  type        = bool
  default     = true
}

variable "http_access_log_enabled" {
  description = "Enable HTTP access logs."
  type        = bool
  default     = true
}

variable "swagger_ui_enabled" {
  description = "Include/enable Swagger UI in service images."
  type        = bool
  default     = true
}

variable "service_min_replicas" {
  description = "Optional minimum replica overrides by Container App service name."
  type        = map(number)
  default     = {}

  validation {
    condition     = alltrue([for replicas in values(var.service_min_replicas) : replicas >= 0])
    error_message = "service_min_replicas values must be greater than or equal to 0."
  }
}

variable "service_max_replicas" {
  description = "Optional maximum replica overrides by Container App service name."
  type        = map(number)
  default     = {}

  validation {
    condition     = alltrue([for replicas in values(var.service_max_replicas) : replicas >= 1])
    error_message = "service_max_replicas values must be greater than or equal to 1."
  }
}

variable "service_http_concurrent_requests" {
  description = "HTTP concurrent request threshold used by Azure Container Apps autoscaling, by service name."
  type        = map(number)
  default     = {}

  validation {
    condition     = alltrue([for requests in values(var.service_http_concurrent_requests) : requests >= 1])
    error_message = "service_http_concurrent_requests values must be greater than or equal to 1."
  }
}

variable "container_app_scale_polling_interval_seconds" {
  description = "Azure Container Apps autoscaler polling interval in seconds."
  type        = number
  default     = 15
}

variable "container_app_scale_cooldown_seconds" {
  description = "Azure Container Apps autoscaler cooldown period in seconds."
  type        = number
  default     = 60
}

variable "container_app_termination_grace_period_seconds" {
  description = "Seconds Azure Container Apps waits after SIGTERM before forcibly killing a replica."
  type        = number
  default     = 60
}

variable "http_idle_timeout" {
  description = "Quarkus HTTP idle timeout passed to all services."
  type        = string
  default     = "30S"
}

variable "http_max_body_size" {
  description = "Quarkus HTTP max body size passed to all services."
  type        = string
  default     = "2M"
}

variable "graceful_shutdown_timeout" {
  description = "Graceful shutdown timeout passed to Quarkus."
  type        = string
  default     = "60S"
}

variable "app_max_worker_threads" {
  description = "Quarkus max worker threads per service."
  type        = number
  default     = 32
}

variable "gateway_downstream_connect_timeout_ms" {
  description = "Gateway connect timeout for internal microservice calls."
  type        = number
  default     = 2000
}

variable "gateway_downstream_read_timeout_ms" {
  description = "Gateway read timeout for internal microservice calls."
  type        = number
  default     = 30000
}

variable "extra_env" {
  description = "Extra plain environment variables by service name."
  type        = map(map(string))
  default     = {}
}

variable "tags" {
  description = "Extra Azure tags."
  type        = map(string)
  default     = {}
}
