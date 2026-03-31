variable "environment" {
  type = string
}

variable "service_name" {
  type = string
}

variable "account_id" {
  type = string
}

output "ecs_task_execution_role_arn" {
  value = aws_iam_role.ecs_task_execution.arn
}

output "ecs_task_role_arn" {
  value = aws_iam_role.ecs_task.arn
}

output "ecr_repository_urls" {
  value = {
    api_gateway        = aws_ecr_repository.api_gateway.url
    auth_service       = aws_ecr_repository.auth_service.url
    order_service      = aws_ecr_repository.order_service.url
    payment_service    = aws_ecr_repository.payment_service.url
    notification_service = aws_ecr_repository.notification_service.url
    webhook_service    = aws_ecr_repository.webhook_service.url
    simulator_service  = aws_ecr_repository.simulator_service.url
    settlement_service  = aws_ecr_repository.settlement_service.url
    risk_service       = aws_ecr_repository.risk_service.url
    analytics_service  = aws_ecr_repository.analytics_service.url
  }
}

resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.service_name}-ecs-task-execution-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "${var.service_name}-ecs-task-execution-${var.environment}"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task" {
  name = "${var.service_name}-ecs-task-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "${var.service_name}-ecs-task-${var.environment}"
  }
}

resource "aws_iam_policy" "ecs_task" {
  name        = "${var.service_name}-ecs-task-policy-${var.environment}"
  description = "IAM policy for ECS task role"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "ssm:GetParameters",
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
        ]
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "${var.service_name}-ecs-task-policy-${var.environment}"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_task" {
  role       = aws_iam_role.ecs_task.name
  policy_arn = aws_iam_policy.ecs_task.arn
}

resource "aws_ecr_repository" "api_gateway" {
  name                 = "${var.service_name}/api-gateway"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "auth_service" {
  name                 = "${var.service_name}/auth-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "order_service" {
  name                 = "${var.service_name}/order-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "payment_service" {
  name                 = "${var.service_name}/payment-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "notification_service" {
  name                 = "${var.service_name}/notification-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "webhook_service" {
  name                 = "${var.service_name}/webhook-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "simulator_service" {
  name                 = "${var.service_name}/simulator-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "settlement_service" {
  name                 = "${var.service_name}/settlement-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "risk_service" {
  name                 = "${var.service_name}/risk-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "analytics_service" {
  name                 = "${var.service_name}/analytics-service"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}
