# Repository Splitting Guide

This document provides commands and scripts to split the PayFlow monorepo into independent repositories.

## Current Structure

```
payflow/ (monorepo)
├── libs/common/
├── services/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── notification-service/
│   ├── analytics-service/
│   ├── audit-service/
│   └── simulator-service/
├── web/
│   ├── payment-page/
│   └── dashboard/
├── docs/
└── .github/
```

## Target Repositories

1. **payflow-payment-core** - Contains: auth-service, order-service, payment-service, notification-service, analytics-service, audit-service, libs/common
2. **payflow-api-gateway** - Contains: api-gateway
3. **payflow-simulator** - Contains: simulator-service
4. **payflow-payment-page** - Contains: web/payment-page
5. **payflow-dashboard** - Contains: web/dashboard

## Step 1: Prepare Each Service

Each service needs a standalone pom.xml. Run the following to generate:

```bash
# Generate standalone poms
./scripts/split/generate-poms.sh
```

## Step 2: Update Dockerfiles

Update each service's Dockerfile to build independently:

```bash
# Update Dockerfiles
./scripts/split/update-dockerfiles.sh
```

## Step 3: Split Repositories

Run the git filter-branch commands:

```bash
# Split each repository
./scripts/split/split-repos.sh
```

## Step 4: Update CI/CD

Each new repository will need its own GitHub Actions workflow. Copy the template:

```bash
# Copy workflow template
cp -r .github/workflows payflow-payment-core/
cp -r .github/workflows payflow-api-gateway/
# etc.
```

## Verification

After splitting, verify each repository:

```bash
# Verify build
cd payflow-payment-core
mvn clean verify

# Verify tests
mvn test
```

## Rollback Plan

If something goes wrong:

1. Keep the original monorepo as backup
2. Test splitting on a fork first
3. Use tags to mark versions before splitting