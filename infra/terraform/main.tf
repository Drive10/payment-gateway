terraform {
  required_version = ">= 1.7.0"
  required_providers { aws = { source = "hashicorp/aws", version = "~> 5.0" } }
}
provider "aws" { region = var.region }
variable "region" { type = string, default = "us-east-1" }
# Add modules: vpc, eks, rds, msk, elasticache
