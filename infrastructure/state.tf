terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "5.2.0"
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