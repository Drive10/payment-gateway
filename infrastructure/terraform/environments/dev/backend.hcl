# Terraform Backend Configuration for Dev Environment
# Run: terraform init -backend-config=backend.hcl

bucket = "payment-gateway-terraform-state"
key    = "payment-gateway/dev/terraform.tfstate"
region = "us-east-1"

# Enable state encryption
encrypt = true

# Enable state versioning
versioning = true
