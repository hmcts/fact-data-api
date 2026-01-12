# MI resource group
data "azurerm_resource_group" "mi_resource_group" {
  name = "managed-identities-${var.env}-rg"
}

# FaCT managed identity
data "azurerm_user_assigned_identity" "fact_mi" {
  name                = "${var.product}-${var.env}-mi"
  resource_group_name = data.azurerm_resource_group.mi_resource_group.name
}

# FaCT resource group
data "azurerm_resource_group" "fact_rg" {
  name = "${var.product}-${var.env}"
}

# Key vault data source
data "azurerm_key_vault" "fact_kv" {
  name                = "${var.product}-kv-${var.env}"
  resource_group_name = data.azurerm_resource_group.fact_rg.name
}

# Postgres Database
module "postgresql" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source    = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  product   = var.product
  component = var.component
  location  = var.location
  env       = var.env
  pgsql_databases = [
    {
      name : "fact"
    }
  ]
  common_tags          = var.common_tags
  business_area        = "cft"
  pgsql_version        = "16"
  admin_user_object_id = var.jenkins_AAD_objectId
  pgsql_server_configuration = [{
     name  = "azure.extensions"
     value = "pgcrypto,cube,earthdistance"
  }, {
    "name" : "backslash_quote",
    "value" : "on"
  }]
}

# Store postgres secrets
locals {
  flexible_secret_prefix = "${var.component}-POSTGRES"

  flexible_secrets = [
    {
      name_suffix = "PASS"
      value       = module.postgresql.password
    },
    {
      name_suffix = "HOST"
      value       = module.postgresql.fqdn
    },
    {
      name_suffix = "USER"
      value       = module.postgresql.username
    },
    {
      name_suffix = "PORT"
      value       = "5432"
    },
    {
      name_suffix = "DATABASE"
      value       = "fact"
    }
  ]

}

resource "azurerm_key_vault_secret" "flexible_secret" {
  for_each     = { for secret in local.flexible_secrets : secret.name_suffix => secret }
  key_vault_id = data.azurerm_key_vault.fact_kv.id
  name         = "${local.flexible_secret_prefix}-${each.value.name_suffix}"
  value        = each.value.value
  tags = merge(var.common_tags, {
    "source" : "${var.component} PostgreSQL"
  })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "17520h")
}
