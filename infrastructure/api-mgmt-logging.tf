resource "azurerm_api_management_api_diagnostic" "apim_logs" {
  provider                 = azurerm.aks-cftapps
  identifier               = "applicationinsights"
  resource_group_name      = local.apim_rg
  api_management_name      = local.apim_name
  api_name                 = module.api_mgmt.name
  api_management_logger_id = "/subscriptions/${var.cft_subscription_id}/resourceGroups/${local.apim_rg}/providers/Microsoft.ApiManagement/service/${local.apim_name}/loggers/${local.apim_name}-logger"

  sampling_percentage       = 100
  always_log_errors         = true
  log_client_ip             = false
  verbosity                 = "information"
  http_correlation_protocol = "W3C"

  frontend_request {
    headers_to_log = [
      "accept",
      "content-type",
      "origin",
    ]
  }

  frontend_response {
    headers_to_log = [
      "content-length",
      "content-type",
    ]
  }

  backend_request {
    headers_to_log = [
      "accept",
      "content-type",
    ]
  }

  backend_response {
    headers_to_log = [
      "content-length",
      "content-type",
    ]
  }

  depends_on = [
    module.api_mgmt
  ]
}
