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

variable "allocated_storage" {
  type    = number
  default = 20
}

output "database_address" {
  value     = aws_db_instance.main.address
  sensitive = true
}

output "database_arn" {
  value = aws_db_instance.main.arn
}

output "database_name" {
  value     = aws_db_instance.main.db_name
  sensitive = true
}

output "database_endpoint" {
  value = aws_db_instance.main.endpoint
}

output "database_port" {
  value = aws_db_instance.main.port
}

output "database_username" {
  value     = aws_db_instance.main.username
  sensitive = true
}

resource "random_password" "postgres" {
  length  = 32
  special = true
}

resource "aws_db_subnet_group" "main" {
  name       = "${var.service_name}-${var.environment}"
  subnet_ids = var.subnet_ids

  tags = {
    Name = "${var.service_name}-db-subnet-${var.environment}"
  }
}

resource "aws_security_group" "database" {
  name        = "${var.service_name}-postgres-${var.environment}"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    cidr_blocks     = ["10.0.0.0/16"]
    description     = "PostgreSQL from VPC"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.service_name}-postgres-sg-${var.environment}"
  }
}

resource "aws_db_instance" "main" {
  identifier = "${var.service_name}-postgres-${var.environment}"

  engine               = "postgres"
  engine_version       = "16.2"
  instance_class       = var.environment == "prod" ? "db.r6g.xlarge" : "db.t3.medium"
  allocated_storage    = var.allocated_storage
  max_allocated_storage = var.allocated_storage * 3

  db_name  = "postgres"
  username = "postgres"
  password = random_password.postgres.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.database.id]

  storage_encrypted   = true
  storage_type        = "gp3"

  backup_retention_period = var.environment == "prod" ? 7 : 1
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"

  enabled_cloudwatch_logs_exports = ["postgresql"]

  skip_final_snapshot = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "${var.service_name}-final-snapshot-${var.environment}" : null

  tags = {
    Name = "${var.service_name}-postgres-${var.environment}"
  }
}
