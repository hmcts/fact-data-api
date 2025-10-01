module "storage_account" {
  source                        = "git@github.com:hmcts/cnp-module-storage-account?ref=4.x"
  env                           = var.env
  storage_account_name          = "${var.product}sa${var.env}"
  resource_group_name           = data.azurerm_resource_group.fact_rg.name
  location                      = var.location
  account_kind                  = "BlobStorage"
  account_replication_type      = "ZRS"
  public_network_access_enabled = true
  common_tags                   = var.common_tags
  containers = [
    {
      name        = "photos",
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
  key_vault_id = data.azurerm_key_vault.fact_key_vault.id
  value        = module.storage_account.storageaccount_primary_blob_connection_string
  tags = merge(var.common_tags, {
    "source" : "${var.component} Storage account"
  })
  expiration_date = timeadd(timestamp(), "17520h")
}