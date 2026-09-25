locals {
  mgmt_network_name    = "cft-ptl-vnet"
  mgmt_network_rg_name = "cft-ptl-network-rg"

  preview_vnet_name           = "cft-preview-vnet"
  preview_vnet_resource_group = "cft-preview-network-rg"
  aks_env                     = var.env == "sandbox" ? "sbox" : var.env

  app_aks_network_name    = "cft-${local.aks_env}-vnet"
  app_aks_network_rg_name = "cft-${local.aks_env}-network-rg"

  standard_subnets = [
    data.azurerm_subnet.jenkins_subnet.id,
    data.azurerm_subnet.jenkins_aks_00.id,
    data.azurerm_subnet.jenkins_aks_01.id,
    data.azurerm_subnet.app_aks_00_subnet.id,
    data.azurerm_subnet.app_aks_01_subnet.id
  ]

  preview_subnets = var.env == "aat" ? [data.azurerm_subnet.aks-00-preview[0].id, data.azurerm_subnet.aks-01-preview[0].id] : []
  valid_subnets   = concat(local.standard_subnets, local.preview_subnets)
}

data "azurerm_virtual_network" "aks_preview_vnet" {
  count = var.env == "aat" ? 1 : 0

  provider            = azurerm.aks-preview
  name                = "cft-preview-vnet"
  resource_group_name = "cft-preview-network-rg"
}

data "azurerm_subnet" "aks-00-preview" {
  count = var.env == "aat" ? 1 : 0

  provider             = azurerm.aks-preview
  name                 = "aks-00"
  virtual_network_name = data.azurerm_virtual_network.aks_preview_vnet[0].name
  resource_group_name  = data.azurerm_virtual_network.aks_preview_vnet[0].resource_group_name
}

data "azurerm_subnet" "aks-01-preview" {
  count = var.env == "aat" ? 1 : 0

  provider             = azurerm.aks-preview
  name                 = "aks-01"
  virtual_network_name = data.azurerm_virtual_network.aks_preview_vnet[0].name
  resource_group_name  = data.azurerm_virtual_network.aks_preview_vnet[0].resource_group_name
}

data "azurerm_subnet" "jenkins_subnet" {
  provider             = azurerm.mgmt
  name                 = "iaas"
  virtual_network_name = local.mgmt_network_name
  resource_group_name  = local.mgmt_network_rg_name
}

data "azurerm_subnet" "jenkins_aks_00" {
  provider             = azurerm.mgmt
  name                 = "aks-00"
  virtual_network_name = local.mgmt_network_name
  resource_group_name  = local.mgmt_network_rg_name
}

data "azurerm_subnet" "jenkins_aks_01" {
  provider             = azurerm.mgmt
  name                 = "aks-01"
  virtual_network_name = local.mgmt_network_name
  resource_group_name  = local.mgmt_network_rg_name
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
  default_action                  = "Deny"
  allow_nested_items_to_be_public = "false"
  public_network_access_enabled   = false
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
    },
    {
      name        = "temp",
      access_type = "container"
    },
    {
      name        = "temp",
      access_type = "container"
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
