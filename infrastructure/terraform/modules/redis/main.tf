variable "environment" {
  type = string
}

variable "service_name" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

output "redis_endpoint" {
  value = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "redis_port" {
  value = aws_elasticache_replication_group.main.port
}

output "redis_security_group_id" {
  value = aws_security_group.redis.id
}

resource "aws_security_group" "redis" {
  name        = "${var.service_name}-redis-${var.environment}"
  description = "Security group for ElastiCache Redis"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    cidr_blocks     = ["10.0.0.0/16"]
    description     = "Redis from VPC"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.service_name}-redis-sg-${var.environment}"
  }
}

resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.service_name}-redis-${var.environment}"
  subnet_ids = var.subnet_ids
}

resource "aws_elasticache_replication_group" "main" {
  identifier = "${var.service_name}-redis-${var.environment}"

  engine               = "redis"
  engine_version       = "7.1"
  node_type            = var.environment == "prod" ? "cache.r6g.large" : "cache.t3.micro"
  number_cache_clusters = var.environment == "prod" ? 3 : 2

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false
  auth_token_enabled         = false

  automatic_failover_enabled = var.environment == "prod"

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis.name
    destination_type = "cloudwatch-logs"
    log_format       = "json"
    log_type         = "engine-log"
  }

  tags = {
    Name = "${var.service_name}-redis-${var.environment}"
  }
}

resource "aws_cloudwatch_log_group" "redis" {
  name              = "/elasticache/${var.service_name}-${var.environment}"
  retention_in_days = 7
}
