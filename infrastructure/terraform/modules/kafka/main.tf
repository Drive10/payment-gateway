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

output "bootstrap_servers" {
  value = aws_msk_cluster.main.bootstrap_brokers_tls
}

output "zookeeper_connect_string" {
  value = aws_msk_cluster.main.zookeeper_connect_string
}

output "arn" {
  value = aws_msk_cluster.main.arn
}

resource "aws_security_group" "kafka" {
  name        = "${var.service_name}-kafka-${var.environment}"
  description = "Security group for MSK Kafka"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
    description = "Kafka from VPC"
  }

  ingress {
    from_port   = 9094
    to_port     = 9094
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
    description = "Kafka TLS from VPC"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.service_name}-kafka-sg-${var.environment}"
  }
}

resource "aws_msk_cluster" "main" {
  cluster_name           = "${var.service_name}-kafka-${var.environment}"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = var.environment == "prod" ? 6 : 3

  broker_node_group_info {
    instance_type   = var.environment == "prod" ? "kafka.m5.large" : "kafka.t3.small"
    az_ids         = [for i, subnet in var.subnet_ids : element(["subnet-0a1b2c3d", "subnet-1a2b3c4d", "subnet-2a3b4c5d"], i)]
    client_subnets = var.subnet_ids

    storage_info {
      ebs_storage_info {
        volume_size = var.environment == "prod" ? 500 : 100
      }
    }
  }

  encryption_info {
    encryption_at_rest_kms_key_arn = aws_kms_key.kafka.arn
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster   = true
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.kafka.name
      }
    }
  }

  client_authentication {
    sasl {
      iam = true
    }
  }

  tags = {
    Name = "${var.service_name}-kafka-${var.environment}"
  }
}

resource "aws_kms_key" "kafka" {
  description             = "KMS key for MSK encryption"
  deletion_window_in_days = 7
  enable_key_rotation    = true

  tags = {
    Name = "${var.service_name}-kafka-kms-${var.environment}"
  }
}

resource "aws_cloudwatch_log_group" "kafka" {
  name              = "/aws/msk/${var.service_name}-${var.environment}"
  retention_in_days = 7
}
