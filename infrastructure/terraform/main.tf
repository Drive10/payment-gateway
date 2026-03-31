module "vpc" {
  source = "./modules/vpc"

  environment         = var.environment
  service_name        = var.service_name
  vpc_cidr           = var.vpc_cidr
  availability_zones  = var.availability_zones
  public_subnet_cidrs = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  database_subnet_cidrs = var.database_subnet_cidrs
}

module "ecs_cluster" {
  source = "./modules/ecs"

  environment  = var.environment
  service_name = var.service_name
  vpc_id      = module.vpc.vpc_id
  subnet_ids  = module.vpc.private_subnet_ids

  depends_on = [module.vpc]
}

module "rds_postgres" {
  source = "./modules/rds"

  environment  = var.environment
  service_name = var.service_name
  vpc_id      = module.vpc.vpc_id
  subnet_ids  = module.vpc.database_subnet_ids
  allocated_storage = var.environment == "prod" ? 100 : 20

  depends_on = [module.vpc]
}

module "redis_cluster" {
  source = "./modules/redis"

  environment  = var.environment
  service_name = var.service_name
  vpc_id      = module.vpc.vpc_id
  subnet_ids  = module.vpc.private_subnet_ids

  depends_on = [module.vpc]
}

module "msk_kafka" {
  source = "./modules/kafka"

  environment  = var.environment
  service_name = var.service_name
  vpc_id      = module.vpc.vpc_id
  subnet_ids  = module.vpc.private_subnet_ids

  depends_on = [module.vpc]
}

module "alb" {
  source = "./modules/alb"

  environment  = var.environment
  service_name = var.service_name
  vpc_id      = module.vpc.vpc_id
  subnet_ids  = module.vpc.public_subnet_ids

  depends_on = [module.vpc]
}

module "iam" {
  source = "./modules/iam"

  environment  = var.environment
  service_name = var.service_name
  account_id   = local.account_id
}
