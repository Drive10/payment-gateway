#!/bin/bash

# Git hooks setup for PayFlow
# Run this script once to install git hooks

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIT_DIR="$(git rev-parse --show-toplevel)/.git"

echo "Installing Git hooks..."

# Copy hooks to .git/hooks
cp "$HOOKS_DIR/hooks/pre-commit" "$GIT_DIR/hooks/"
cp "$HOOKS_DIR/hooks/commit-msg" "$GIT_DIR/hooks/"

# Make executable
chmod +x "$GIT_DIR/hooks/pre-commit"
chmod +x "$GIT_DIR/hooks/commit-msg"

echo "Git hooks installed successfully!"
echo ""
echo "Hooks installed:"
echo "  - pre-commit: Runs lint/format checks before commit"
echo "  - commit-msg: Validates commit message format"
echo ""
echo "Commit message format: type(scope): description"
echo "  Example: feat(payment): add Stripe provider"