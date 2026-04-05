# Payment Gateway - Jenkins CI/CD

## Overview
Local Jenkins setup with GitHub integration and Docker for the payment gateway project.

## Prerequisites

- **Java 17+** - Already installed ✓
- **Docker Desktop** - Must be running
- **Git** - Already installed ✓
- **Maven** - For local builds

## Quick Start

```bash
# 1. Setup Jenkins
./jenkins/setup-jenkins.sh

# 2. Setup credentials
./jenkins/setup-credentials.sh

# 3. Run pipeline
# Go to Jenkins UI > New Item > Pipeline
# Point to: Jenkinsfile in jenkins/ directory
```

## Architecture

```
GitHub Repository
       ↓
Jenkins (localhost:8080)
       ↓
   Build + Test
       ↓
   Docker Build
       ↓
   Push to Registry (localhost:5000)
       ↓
   Deploy via Docker Compose
       ↓
   Health Check + Smoke Tests
```

## Jenkins Credentials Setup

Go to: **Jenkins > Manage Jenkins > Credentials > System > Global credentials**

| Credential ID | Type | Value |
|--------------|------|-------|
| `github-credentials` | Username/Password | Your GitHub username + PAT |
| `docker-registry` | Username/Password | admin/admin |
| `db-password` | Secret text | payment_dev_pass |
| `redis-password` | Secret text | redis_dev_pass |
| `jwt-secret` | Secret text | (generated random) |

## Required Jenkins Plugins

Install these via: **Manage Jenkins > Plugins > Available**

1. **Docker Pipeline** - Docker build/push
2. **Git** - Git integration
3. **GitHub Branch Source** - GitHub webhooks
4. **Pipeline** - Jenkinsfile support
5. **Credentials Binding** - Secret management
6. **JUnit** - Test results

## Pipeline Stages

1. **Checkout** - Pull from GitHub
2. **Load Environment** - Get secrets from Jenkins credentials
3. **Build** - Maven build
4. **Unit Tests** - Run tests
5. **Security Scan** - Check for hardcoded secrets
6. **Start Infrastructure** - Docker compose up infra
7. **Build Docker Images** - Build all services
8. **Push to Registry** - Push to local registry
9. **Deploy Services** - Deploy via compose
10. **Health Check** - Verify all services healthy
11. **Smoke Test** - Test endpoints

## Managing Jenkins

```bash
# Start Jenkins
./jenkins/setup-jenkins.sh

# Stop Jenkins
kill $(cat ~/jenkins_home/jenkins.pid)

# View logs
tail -f ~/jenkins_home/jenkins.log

# Check status
curl http://localhost:8080/login
```

## Troubleshooting

### Jenkins won't start
```bash
# Check if port is in use
lsof -i :8080

# Kill existing process
kill $(lsof -t -i:8080)

# Restart
./jenkins/setup-jenkins.sh
```

### Docker not responding
```bash
# Check Docker
docker info

# Restart Docker Desktop
# (via menu bar icon)
```

### Pipeline fails at build
```bash
# Check Maven
mvn -v

# Clean and rebuild
mvn clean package -DskipTests
```
