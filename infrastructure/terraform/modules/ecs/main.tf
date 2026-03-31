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

output "cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "cluster_arn" {
  value = aws_ecs_cluster.main.arn
}

output "cluster_id" {
  value = aws_ecs_cluster.main.id
}

resource "aws_ecs_cluster" "main" {
  name = "${var.service_name}-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "${var.service_name}-ecs-${var.environment}"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name = aws_ecs_cluster.main.name

  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    base              = 1
    weight            = 100
    capacity_provider = "FARGATE"
  }
}

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${var.service_name}-${var.environment}"
  retention_in_days = 7

  tags = {
    Name = "${var.service_name}-logs-${var.environment}"
  }
}
