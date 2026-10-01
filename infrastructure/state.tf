terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "5.8.0"
    }
    http = {
      source  = "hashicorp/http"
      version = "3.5.0"
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
  alias           = "aks-cftapps"
  subscription_id = var.cft_subscription_id
}
