# Add federated identity credentials to Azure AD applications
data "azuread_application" "fact_admin_frontend_app" {
  display_name = "fact-admin-frontend-non-prod"
}

data "azuread_application" "fact_data_api_app" {
  display_name = "fact-data-api-non-prod"
}

resource "azuread_application_federated_identity_credential" "fact_admin_frontend" {
  count = var.env == "prod" ? 0 : 1

  application_id = data.azuread_application.fact_admin_frontend_app.id
  display_name   = data.azurerm_user_assigned_identity.fact_mi.name
  audiences      = ["api://AzureADTokenExchange"]
  issuer         = "https://login.microsoftonline.com/${data.azurerm_user_assigned_identity.fact_mi.tenant_id}/v2.0"
  subject        = data.azurerm_user_assigned_identity.fact_mi.principal_id
  description    = "${data.azurerm_user_assigned_identity.fact_mi.name} used by FaCT for S2S Auth"
}

resource "azuread_application_federated_identity_credential" "fact_data_api" {
  count = var.env == "prod" ? 0 : 1

  application_id = data.azuread_application.fact_data_api_app.id
  display_name   = data.azurerm_user_assigned_identity.fact_mi.name
  audiences      = ["api://AzureADTokenExchange"]
  issuer         = "https://login.microsoftonline.com/${data.azurerm_user_assigned_identity.fact_mi.tenant_id}/v2.0"
  subject        = data.azurerm_user_assigned_identity.fact_mi.principal_id
  description    = "${data.azurerm_user_assigned_identity.fact_mi.name} used by FaCT for S2S Auth"
}
