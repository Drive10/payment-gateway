path "secret/data/microservice/*" {
  capabilities = ["read", "list"]
}

path "secret/data/database/*" {
  capabilities = ["read", "list"]
}

path "secret/data/jwt/*" {
  capabilities = ["read"]
}

path "secret/data/api-keys/*" {
  capabilities = ["read"]
}

path "database/creds/*" {
  capabilities = ["read"]
}

path "aws/creds/*" {
  capabilities = ["read"]
}

path "sys/health" {
  capabilities = ["read"]
}