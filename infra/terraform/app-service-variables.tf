variable "deploy_client" {
  description = "When false, skip creating App Service resources for the Next.js client."
  type        = bool
  default     = true
}

variable "client_webapp_name" {
  description = "Optional custom Azure Web App name for the client. Generated from project/environment if null."
  type        = string
  default     = null
}

variable "client_app_service_sku_name" {
  description = "App Service Plan SKU for the Next.js client (e.g. B2, P1v3)."
  type        = string
  default     = "B2"
}

variable "client_image_tag" {
  description = "Container image tag for the Next.js client Web App."
  type        = string
  default     = "latest"
}

variable "client_gateway_url" {
  description = "Backend gateway URL injected into the Next.js client as GATEWAY_URL."
  type        = string
  default     = ""
}
