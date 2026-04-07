# Execution Protocol

> Task execution workflow for AI agents working on PayFlow

---

## 1. Task Analysis Phase

### Input Analysis

When receiving a task:
1. **Understand the goal** - What does the user want?
2. **Identify scope** - What files/services are affected?
3. **Check dependencies** - What else needs to change?
4. **Assess complexity** - Simple fix or major feature?

### Information Gathering

```markdown
Required context check:
- [ ] Architecture overview reviewed
- [ ] Service relationships understood
- [ ] Existing patterns identified
- [ ] Relevant documentation read
- [ ] Dependencies mapped
```

---

## 2. Planning Phase

### Task Breakdown

```
Complex Task → Decompose into subtasks
├── Subtask 1: [description]
├── Subtask 2: [description]
├── Subtask 3: [description]
└── Subtask 4: [description]
```

### Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking changes | High | Test thoroughly, document |
| Database migration | High | Backup, test on staging |
| Security impact | Critical | Review with security rules |
| Performance impact | Medium | Benchmark before/after |

---

## 3. Implementation Phase

### Execution Order

```
1. Read relevant files first
2. Understand existing patterns
3. Implement changes
4. Add/update tests
5. Update documentation
6. Verify build passes
7. Run linters
```

### Verification Checklist

```bash
✅ mvn spotless:check     # Java format
✅ mvn test               # All tests pass
✅ npm run lint           # Frontend lint
✅ npm run build          # Frontend build
✅ docker compose up -d   # Local run works
✅ No secrets in code    # Security check
```

---

## 4. Output Phase

### Standard Response Format

```
[TYPE] Short summary

Context: What was done
Files: Affected files
Tests: Test results
Next: Suggested next steps (if any)
```

### Example Outputs

```
[IMPLEMENT] Added payment validation

Context: Created PaymentValidationService with card
formatting and Luhn validation
Files: services/payment-service/src/main/java/.../PaymentValidationService.java
       services/payment-service/src/test/java/.../PaymentValidationServiceTest.java
Tests: 12 tests passing
Next: Ready for review
```

```
[BLOCKED] Clarification needed

Context: Task requires database migration
Questions:
  1. Should we migrate existing data?
  2. What's the rollback strategy?
Please clarify before proceeding.
```

---

## 5. Task Completion

### Done Criteria

- [ ] All tests passing
- [ ] Lint checks passing
- [ ] Documentation updated (if needed)
- [ ] Build verified
- [ ] Changes committed (if approved)

### Handoff

- Explain what was done
- Note any follow-ups needed
- Update relevant docs

---

## Quick Reference

| Phase | Key Action |
|-------|------------|
| Analysis | Understand goal, scope, dependencies |
| Planning | Break down, assess risks |
| Implementation | Execute in order, verify |
| Output | Standard format response |
| Completion | Verify all checks pass |