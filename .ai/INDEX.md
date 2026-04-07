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

1. .ai/core/safety.md     # 🔴 Security (HIGHEST PRIORITY)
2. .ai/core/thinking.md   # 🧠 Decision framework
3. .ai/core/execution.md  # ⚙️ Task protocol

These are NON-NEGOTIABLE.

---

## 🧪 RULES LOADED DEBUG (MANDATORY)

To confirm rules are active, ALWAYS include this at the END of every response:

[RULES_ACTIVE]

If this is missing → rules are NOT applied correctly.

---

## 🧩 Task Type → Rule Selection

| Task | Apply (in order) |
|------|------------------|
| Bug Fix | core → backend/frontend → database |
| New Feature | core → api → backend → testing |
| Frontend | core → frontend |
| Database | core → database → devops |
| Payment | core → payment → backend |
| Security | safety (priority) → backend |
| API Change | core → api → backend |
| DevOps | core → devops |

---

## 🧠 Smart Application Examples

### Example 1: Fix bug in payment service

.ai/core/safety.md
.ai/core/thinking.md
.ai/core/execution.md
.ai/rules/payment.md
.ai/rules/backend.md
services/payment-service/AGENTS.md


---

### Example 2: Add new API endpoint

.ai/core/safety.md
.ai/core/thinking.md
.ai/core/execution.md
.ai/rules/api.md
.ai/rules/backend.md
.ai/rules/testing.md


---

## 📂 File Overview

### Core (ALWAYS APPLY)
| File | Purpose |
|------|--------|
| .ai/core/safety.md | Security, approvals, constraints |
| .ai/core/thinking.md | Decision logic, trade-offs |
| .ai/core/execution.md | Task workflow, validation |

---

### Context (Use When Needed)
| File | Purpose |
|------|--------|
| .ai/context/architecture.md | System design, tech stack |
| .ai/context/services.md | Service relationships |

---

### Rules (Task-Based)
| File | Purpose |
|------|--------|
| .ai/rules/backend.md | Java/Spring patterns |
| .ai/rules/frontend.md | React/TypeScript patterns |
| .ai/rules/database.md | DB design & optimization |
| .ai/rules/devops.md | Docker, CI/CD |
| .ai/rules/payment.md | Payment logic, PCI, idempotency |
| .ai/rules/api.md | REST/GraphQL standards |
| .ai/rules/testing.md | Test coverage, patterns |
| .ai/rules/git.md | Git workflow |

---

## 🏗️ Service-Level Rules

Apply when working on specific service:

- services/*/AGENTS.md

These override generic rules when applicable.

---

## ⚠️ Rule Priority

If conflict occurs:

1. 🔴 Safety (cannot be overridden)
2. 🟠 Architecture / Production constraints
3. 🟡 Code quality
4. 🟢 Task-specific rules

---

## 🧠 Decision Rules

When multiple solutions exist:

- Prefer existing architecture
- Keep solution simple
- Avoid new patterns unless necessary
- Minimize changes

---

## 🚨 Failure Handling

If unclear:

- Do NOT assume
- Do NOT invent missing logic
- Choose safest approach

If risk exists:
→ explicitly mention it

---

## 🧪 Output Requirements

Every response MUST include:

1. ✅ What changed
2. 📂 Files modified
3. 💡 Why this approach
4. ⚠️ Risks (if any)
5. [RULES_ACTIVE]

---

## 🏁 Goal

- Production-ready code
- No regressions
- Consistent architecture
- Strict rule adherence