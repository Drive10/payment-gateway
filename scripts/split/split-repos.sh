#!/bin/bash
# split-repos.sh - Split monorepo into multiple repositories

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "This script will guide you through splitting the PayFlow monorepo."
echo ""
echo "IMPORTANT: Backup your repository before running this script!"
echo ""
echo "Target Repositories:"
echo "1. payflow-payment-core (auth, order, payment, notification, analytics, audit + libs/common)"
echo "2. payflow-api-gateway"
echo "3. payflow-simulator"
echo "4. payflow-payment-page"
echo "5. payflow-dashboard"
echo ""

read -p "Continue? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Aborted."
    exit 0
fi

echo ""
echo "Step 1: Cloning repositories..."
echo ""

# Clone each repository (you need to create these first on GitHub)
REPOS=(
    "git@github.com:Drive10/payflow-payment-core.git"
    "git@github.com:Drive10/payflow-api-gateway.git"
    "git@github.com:Drive10/payflow-simulator.git"
    "git@github.com:Drive10/payflow-payment-page.git"
    "git@github.com:Drive10/payflow-dashboard.git"
)

for repo in "${REPOS[@]}"; do
    reponame=$(basename -s .git "$repo")
    if [ -d "$reponame" ]; then
        echo "$reponame already exists, skipping clone"
    else
        git clone "$repo" "$reponame" || echo "Failed to clone $repo - create it first"
    fi
done

echo ""
echo "Step 2: Filtering and pushing content..."
echo ""

# payflow-payment-core: services + libs/common
echo "Creating payflow-payment-core..."
cd payflow-payment-core
git filter-repo --path services/auth-service/ \
    --path services/order-service/ \
    --path services/payment-service/ \
    --path services/notification-service/ \
    --path services/analytics-service/ \
    --path services/audit-service/ \
    --path libs/common/ \
    --force

echo "Pushing to payflow-payment-core..."
git remote add origin git@github.com:Drive10/payflow-payment-core.git || true
git push --force --all origin
git push --force --tags origin

# Return to root
cd ..

# payflow-api-gateway
echo "Creating payflow-api-gateway..."
cd payflow-api-gateway
git filter-repo --path services/api-gateway/ --force

echo "Pushing to payflow-api-gateway..."
git remote add origin git@github.com:Drive10/payflow-api-gateway.git || true
git push --force --all origin
git push --force --tags origin

cd ..

# payflow-simulator
echo "Creating payflow-simulator..."
cd payflow-simulator
git filter-repo --path services/simulator-service/ --force

echo "Pushing to payflow-simulator..."
git remote add origin git@github.com:Drive10/payflow-simulator.git || true
git push --force --all origin
git push --force --tags origin

cd ..

# payflow-payment-page
echo "Creating payflow-payment-page..."
cd payflow-payment-page
git filter-repo --path web/payment-page/ --force

echo "Pushing to payflow-payment-page..."
git remote add origin git@github.com:Drive10/payflow-payment-page.git || true

cd ..

# payflow-dashboard
echo "Creating payflow-dashboard..."
cd payflow-dashboard
git filter-repo --path web/dashboard/ --force

echo "Pushing to payflow-dashboard..."
git remote add origin git@github.com:Drive10/payflow-dashboard.git || true
git push --force --all origin
git push --force --tags origin

cd ..

echo ""
echo "Done! Each repository now contains its independent service."
echo ""
echo "Next steps:"
echo "1. Update each repository's pom.xml to remove parent reference"
echo "2. Add CI/CD workflows to each repository"
echo "3. Set up Dependabot for each repository"
echo "4. Update documentation with new repository URLs"