locals {
  role_admin = "Role.Fact.Admin"
  role_prl   = "Role.Fact.Prl"
  role_c100  = "Role.Fact.C100"

  # Maps each APIM operation id (from the published OpenAPI spec) to the AAD app roles
  # allowed to call it. Role.Fact.Admin is included everywhere so admins retain full access.
  apim_operation_roles = {
    getCourtDetailsById     = [local.role_admin, local.role_prl]
    getCourtDetailsById_1   = [local.role_admin, local.role_prl]
    getContactDetails       = [local.role_admin, local.role_prl]
    getCourtDetailsBySlug   = [local.role_admin, local.role_prl, local.role_c100]
    getCourtDetailsBySlug_1 = [local.role_admin, local.role_prl]
    getCourtsByPostcode     = [local.role_admin, local.role_prl, local.role_c100]
  }
}

resource "azurerm_api_management_api_operation_policy" "apim_operation_role_policy" {
  for_each = local.apim_operation_roles

  provider            = azurerm.aks-cftapps
  api_name            = module.api_mgmt.name
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  operation_id        = each.key

  xml_content = templatefile("${path.module}/api-mgmt-operation-policy.xml", {
    tenant_id = data.azurerm_client_config.current.tenant_id
    audience  = data.azurerm_key_vault_secret.api_app_registration_id.value
    roles     = each.value
  })

  depends_on = [
    module.api_mgmt,
    module.api_mgmt_policy
  ]
}
