group "default" {
  targets = ["api-gateway","auth-service","payment-service","ledger-service","risk-service","settlement-service","notification-service"]
}

variable "REGISTRY" {
  default = "ghcr.io"
}
variable "IMAGE_OWNER" {
  default = "your-user"
}
variable "VERSION" {
  default = "latest"
}

target "common" {
  platforms = ["linux/amd64"]
  args = {
    MAVEN_OPTS = "-Dmaven.test.skip=true"
  }
}

target "api-gateway" {
  inherits = ["common"]
  context = "../services/api-gateway"
  tags = ["${REGISTRY}/${IMAGE_OWNER}/api-gateway:${VERSION}"]
}

target "auth-service" {
  inherits = ["common"]
  context = "../services/auth-service"
  tags = ["${REGISTRY}/${IMAGE_OWNER}/auth-service:${VERSION}"]
}

target "payment-service" {
  inherits = ["common"]
  context = "../services/payment-service"
  tags = ["${REGISTRY}/${IMAGE_OWNER}/payment-service:${VERSION}"]
}

target "ledger-service" {
  inherits = ["common"]
  context = "../services/ledger-service"
  tags = ["${REGISTRY}/${IMAGE_OWNER}/ledger-service:${VERSION}"]
}

target "risk-service" {
  inherits = ["common"]
  context = "../services/risk-service"
  tags = ["${REGISTRY}/${IMAGE_OWNER}/risk-service:${VERSION}"]
}

target "settlement-service" {
  inherits = ["common"]
  context = "../services/settlement-service"
  tags = ["${REGISTRY}/${IMAGE_OWNER}/settlement-service:${VERSION}"]
}

target "notification-service" {
  inherits = ["common"]
  context = "../services/notification-service"
  tags = ["${REGISTRY}/${IMAGE_OWNER}/notification-service:${VERSION}"]
}