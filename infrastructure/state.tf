terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.81.0"
    }
  }
}

provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  alias           = "postgres_network"
  subscription_id = var.aks_subscription_id
}

provider "azurerm" {
  features {}
  alias           = "aks-preview"
  subscription_id = "8b6ea922-0862-443e-af15-6056e1c9b9a4"
}

provider "azurerm" {
  features {}
  alias           = "mgmt"
  subscription_id = var.mgmt_subscription_id
}
