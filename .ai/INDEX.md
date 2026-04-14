# 🧠 AI Rules Index (Routing & Execution Guide)

> Smart rule usage system for AI agents — START HERE

---

## ⚠️ IMPORTANT NOTE

- All rule files are preloaded via `opencode.json`
- This file does NOT load files
- It defines **how to interpret and prioritize rules**

---

## 🚀 Quick Start (MANDATORY)

For EVERY task, ALWAYS prioritize:

1. `.ai/core/safety.md` - 🔴 Security (HIGHEST)
2. `.ai/core/diff-rules.md` - 📝 Diff output (STRICT)
3. `.ai/core/thinking.md` - 🧠 Decision framework
4. `.ai/core/payment-domain.md` - 💳 Payment reality
5. `.ai/core/execution.md` - ⚙️ Task protocol

These are NON-NEGOTIABLE.

---

## 🎭 Agent System

When working on tasks, use agent role:

| Agent | Scope | Focus |
|-------|-------|-------|
| backend | payment-service, order-service | idempotency, async flow, webhooks |
| qa | all services | break system, edge cases |
| security | all services | signature validation, replay prevention |
| design | all services | event-driven, Kafka architecture |
| product | frontend | real UX, no fake states |

---

## 📝 Output Rules (STRICT)

**MUST follow:**
- `.ai/core/diff-rules.md` - Only unified diff output
- `.ai/rules/enforce-diff-only.md` - Reject non-diff

If unable to produce valid diff:
→ REJECT: invalid diff

---

## 🧪 RULES LOADED DEBUG (MANDATORY)

To confirm rules are active, ALWAYS include at END of every response:

```
[RULES_ACTIVE]
```

If this is missing → rules are NOT applied correctly.

---

## 🧩 Task Type → Rule Selection

| Task | Apply (in order) |
|------|------------------|
| Bug Fix | safety → diff-rules → thinking → execution → backend → payment-domain |
| New Feature | safety → diff-rules → thinking → api → backend → testing |
| Payment | safety → diff-rules → payment-domain → backend → qa |
| Frontend | safety → diff-rules → thinking → frontend → product |
| Database | safety → diff-rules → database → devops |
| Security | safety → security → diff-rules → backend |
| Refactor | safety → diff-rules → design → backend |

---

## 📂 File Overview

### Core (ALWAYS APPLY)
| File | Purpose |
|------|---------|
| `.ai/core/safety.md` | Security, approvals, constraints |
| `.ai/core/thinking.md` | Decision logic, trade-offs |
| `.ai/core/execution.md` | Task workflow, validation |
| `.ai/core/diff-rules.md` | Strict unified diff output |
| `.ai/core/merge-strategy.md` | Priority-based conflict resolution |
| `.ai/core/payment-domain.md` | Async payment flow reality |

### Context (Use When Needed)
| File | Purpose |
|------|---------|
| `.ai/context/architecture.md` | System design, tech stack |
| `.ai/context/services.md` | Service relationships |

### Agents (Role-Based)
| File | Purpose |
|------|---------|
| `.ai/agents/backend.md` | Backend agent responsibilities |
| `.ai/agents/qa.md` | QA agent - break system |
| `.ai/agents/security.md` | Security agent - validation |
| `.ai/agents/design.md` | Design agent - architecture |
| `.ai/agents/product.md` | Product agent - UX reality |

### Rules (Task-Based)
| File | Purpose |
|------|---------|
| `.ai/rules/backend.md` | Java/Spring patterns |
| `.ai/rules/frontend.md` | React/TypeScript patterns |
| `.ai/rules/database.md` | DB design & optimization |
| `.ai/rules/devops.md` | Docker, CI/CD |
| `.ai/rules/payment.md` | Payment logic, PCI, idempotency |
| `.ai/rules/api.md` | REST/GraphQL standards |
| `.ai/rules/testing.md` | Test coverage, patterns |
| `.ai/rules/git.md` | Git workflow |
| `.ai/rules/enforce-diff-only.md` | Reject non-diff output |

---

## 🧠 Decision Rules

When multiple solutions exist:

- Prefer existing architecture
- Keep solution simple
- Avoid new patterns unless necessary
- Minimize changes
- **Never assume sync success for payments** - always async

---

## 🚨 Failure Handling

If unclear:
- Do NOT assume
- Do NOT invent missing logic
- Choose safest approach
- Ask user for clarification

If risk exists:
→ explicitly mention it

---

## 🧪 Output Requirements

Every response MUST include:

1. ✅ What changed
2. 📂 Files modified
3. 💡 Why this approach
4. ⚠️ Risks (if any)
5. `[RULES_ACTIVE]`

---

## 🏁 Goal

- Production-ready code
- No regressions
- Consistent architecture
- Strict rule adherence
- **Real payment flow (async, webhook-driven)**