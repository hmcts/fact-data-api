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

variable "jenkins_AAD_objectId" {
  default = ""
}

variable "cft_subscription_id" {
  type        = string
  description = "Subscription containing the shared CFT API Management instance."
  default     = ""
}

variable "apim_suffix" {
  type        = string
  description = "Optional shared API Management name suffix override."
  default     = ""
}
