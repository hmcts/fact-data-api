variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "aks_subscription_id" {
  default = ""
}

variable "mgmt_subscription_id" {
  default = ""
}

variable "aks_preview_subscription_id" {
  default = ""
}

variable "jenkins_AAD_objectId" {
  default = ""
}
