# Git Rules

> Git workflow and conventions for PayFlow

---

## 1. Branch Strategy

### Branch Types

| Type | Purpose | Base | Merge |
|------|---------|------|-------|
| main | Production | - | PR only |
| develop | Integration | main | PR only |
| feature/* | New features | develop | PR |
| bugfix/* | Bug fixes | develop | PR |
| hotfix/* | Production fixes | main | PR |
| refactor/* | Code refactoring | develop | PR |
| docs/* | Documentation | develop | PR |

### Naming Format

```
{type}/{ticket-id}-{short-description}

Examples:
feature/PF-123-add-stripe-provider
bugfix/PF-456-fix-payment-timeout
hotfix/PF-789-security-patch-cve
refactor/PF-101-optimize-database-queries
docs/PF-202-update-api-documentation
```

---

## 2. Commit Messages

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

| Type | Description |
|------|-------------|
| feat | New feature |
| fix | Bug fix |
| docs | Documentation |
| style | Formatting (no code change) |
| refactor | Code refactoring |
| test | Tests |
| chore | Maintenance |
| perf | Performance |
| ci | CI/CD |
| revert | Revert previous commit |

### Examples

```
feat(payment): add Stripe payment provider

fix(auth): resolve JWT token expiration issue

docs(readme): update installation instructions

perf(api): optimize database query performance

refactor(service): extract payment validation logic
```

### Rules

- Subject line: max 50 characters
- Body: wrap at 72 characters
- Use imperative mood: "add" not "added"
- Reference issues: "Fixes #123"

---

## 3. Pull Request

### PR Title Format

```
[TYPE] Brief description

Examples:
[FEATURE] Add Stripe payment provider
[BUGFIX] Fix payment timeout issue
[REFACTOR] Optimize database queries
```

### PR Description Template

```markdown
## Summary
Brief description of changes

## Related Issues
- Fixes #123

## Testing
- [ ] Unit tests added
- [ ] Integration tests passed
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] No security vulnerabilities
- [ ] Documentation updated
- [ ] No breaking changes
```

### Review Requirements

- Min 1 approval required
- Security changes: 2 approvals
- Breaking changes: Team lead approval
- All CI checks passing

---

## 4. Merging Strategy

### Squash vs Rebase

| Branch | Strategy |
|--------|----------|
| feature/* | Squash commits |
| bugfix/* | Squash commits |
| hotfix/* | Squash commits |
| main | NEVER rebase |
| develop | Rebase before merge |

### Merge Steps

```bash
# 1. Update branch
git fetch origin
git rebase origin/develop

# 2. Run tests
mvn test

# 3. Squash commits
git rebase -i HEAD~3
# Keep first commit, squash others

# 4. Push and create PR
git push --force-with-lease
gh pr create
```

---

## 5. Pre-commit Checklist

```bash
# Run before every commit
✅ mvn spotless:check    # Java formatting
✅ mvn test             # All tests pass
✅ npm run lint         # Frontend lint (if changed)
✅ git diff --cached   # Review changes
✅ No secrets in code  # Security check
```

---

## 6. Hooks

### Commit Message Hook

```bash
# .git/hooks/commit-msg
#!/bin/sh
msg=$(cat "$1")
if ! echo "$msg" | grep -E '^(feat|fix|docs|style|refactor|test|chore|perf|ci|revert)' > /dev/null; then
    echo "Invalid commit message format"
    exit 1
fi
```

### Pre-commit Hook

```bash
# .git/hooks/pre-commit
#!/bin/sh
mvn spotless:check || exit 1
mvn test -q || exit 1
```

---

## Quick Reference

| Action | Command |
|--------|---------|
| Create feature branch | `git checkout -b feature/xyz` |
| Update branch | `git fetch && git rebase main` |
| Commit | `git commit -m "type(scope): message"` |
| Squash commits | `git rebase -i HEAD~n` |
| Create PR | `gh pr create --title "type: description"` |
| Force push (safe) | `git push --force-with-lease` |