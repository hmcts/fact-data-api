locals {
  apim_suffix = var.apim_suffix == "" ? var.env : var.apim_suffix
  apim_name   = "cft-api-mgmt-${local.apim_suffix}"
  apim_rg     = "cft-${var.env}-network-rg"
  apim_policy = templatefile("${path.module}/api-mgmt-policy.xml", {
    tenant_id = data.azurerm_client_config.current.tenant_id
    audience  = data.azurerm_key_vault_secret.api_app_registration_id.value
  })
}

data "azurerm_client_config" "current" {}

data "azurerm_key_vault_secret" "api_app_registration_id" {
  name         = "api-app-reg-id"
  key_vault_id = data.azurerm_key_vault.fact_kv.id
}

data "http" "openapi" {
  url = "https://hmcts.github.io/cnp-api-docs/specs/fact-data-api-apim.json"

  request_headers = {
    Accept = "application/json"
  }

  lifecycle {
    postcondition {
      condition     = self.status_code == 200
      error_message = "The published FACT APIM OpenAPI specification could not be downloaded."
    }

    postcondition {
      condition     = can(jsondecode(self.response_body))
      error_message = "The published FACT APIM OpenAPI specification is not valid JSON."
    }
  }
}

module "api_mgmt_product" {
  source = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"

  api_mgmt_name                 = local.apim_name
  api_mgmt_rg                   = local.apim_rg
  name                          = "fact-data-api"
  product_access_control_groups = ["developers"]
  approval_required             = false
  subscription_required         = false

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "api_mgmt" {
  source = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"

  api_mgmt_name = local.apim_name
  api_mgmt_rg   = local.apim_rg
  name          = "fact-data-api"
  revision      = "1"
  product_id    = module.api_mgmt_product.product_id
  display_name  = "FACT Data API"
  path          = "fact"
  protocols     = ["https"]
  service_url = var.env == "prod" ? (
    "https://${var.product}-${var.component}.platform.hmcts.net"
    ) : (
    "https://${var.product}-${var.component}.${var.env}.platform.hmcts.net"
  )
  swagger_url           = data.http.openapi.response_body
  content_format        = "openapi+json"
  subscription_required = false

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "api_mgmt_policy" {
  source = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"

  api_mgmt_name          = local.apim_name
  api_mgmt_rg            = local.apim_rg
  api_name               = module.api_mgmt.name
  api_policy_xml_content = local.apim_policy

  providers = {
    azurerm = azurerm.aks-cftapps
  }

  depends_on = [
    module.api_mgmt
  ]
}
