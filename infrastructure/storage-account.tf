locals {
  aks_env                 = var.env == "sandbox" ? "sbox" : var.env

  app_aks_network_name    = "cft-${local.aks_env}-vnet"
  app_aks_network_rg_name = "cft-${local.aks_env}-network-rg"

  preview_vnet_name           = "cft-preview-vnet"
  preview_vnet_resource_group = "cft-preview-network-rg"

  standard_subnets = [
    data.azurerm_subnet.app_aks_00_subnet.id,
    data.azurerm_subnet.app_aks_01_subnet.id
  ]

  preview_subnets = var.env == "aat" ? [data.azurerm_subnet.preview_aks_00_subnet.id, data.azurerm_subnet.preview_aks_01_subnet.id] : []
  valid_subnets   = concat(local.standard_subnets, local.preview_subnets)
}

data "azurerm_subnet" "preview_aks_00_subnet" {
  provider             = azurerm.aks-preview
  name                 = "aks-00"
  virtual_network_name = local.preview_vnet_name
  resource_group_name  = local.preview_vnet_resource_group
}

data "azurerm_subnet" "preview_aks_01_subnet" {
  provider             = azurerm.aks-preview
  name                 = "aks-01"
  virtual_network_name = local.preview_vnet_name
  resource_group_name  = local.preview_vnet_resource_group
}

data "azurerm_subnet" "app_aks_00_subnet" {
  provider             = azurerm.aks-infra
  name                 = "aks-00"
  virtual_network_name = local.app_aks_network_name
  resource_group_name  = local.app_aks_network_rg_name
}

data "azurerm_subnet" "app_aks_01_subnet" {
  provider             = azurerm.aks-infra
  name                 = "aks-01"
  virtual_network_name = local.app_aks_network_name
  resource_group_name  = local.app_aks_network_rg_name
}

module "storage_account" {
  source                          = "git@github.com:hmcts/cnp-module-storage-account?ref=4.x"
  env                             = var.env
  storage_account_name            = "${var.product}sa${var.env}"
  resource_group_name             = data.azurerm_resource_group.fact_rg.name
  location                        = var.location
  account_kind                    = "StorageV2"
  account_replication_type        = "ZRS"
  enable_data_protection          = true
  retention_period                = 14
  common_tags                     = var.common_tags

  sa_subnets = local.valid_subnets

  containers = [
    {
      name        = "photos",
      access_type = "private"
    },
    {
      name        = "csv",
      access_type = "private"
    }
  ]

  managed_identity_object_id = data.azurerm_user_assigned_identity.fact_mi.principal_id
  role_assignments = [
    "Storage Blob Data Contributor"
  ]
}

resource "azurerm_key_vault_secret" "storage_account_connection_string_secret" {
  name         = "storage-account-connection-string"
  key_vault_id = data.azurerm_key_vault.fact_kv.id
  value        = module.storage_account.storageaccount_primary_blob_connection_string
  tags = merge(var.common_tags, {
    "source" : "${var.component} Storage account"
  })
  expiration_date = timeadd(timestamp(), "17520h")
}
