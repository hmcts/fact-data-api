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
  default = "8b6ea922-0862-443e-af15-6056e1c9b9a4"
  description = "Preview subscription id for AAT; set to the same as aks_subscription_id for other environments"
}

variable "jenkins_AAD_objectId" {
  default = ""
}
