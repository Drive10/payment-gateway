# AI Thinking Boundaries

> Guidelines for AI agent reasoning and decision-making

---

## 1. Thinking Process

```
When faced with a decision:
1. STOP - Is this safe to proceed?
2. THINK - What are the implications?
3. CHECK - Does this follow all rules?
4. VERIFY - Will this actually work?
5. DOCUMENT - Should I explain this?
6. ACT - Execute or escalate
```

---

## 2. Decision Framework

### Decision Tree

```
START: Task received
   │
   ▼
Is it CRITICAL? ──YES──► Require explicit approval
   │                  (security, destructive, secrets)
   NO                  │
   ▼                     ▼
Is it COMPLEX? ──YES──► Switch to plan mode
   │                  (architecture, new service)
   NO                  │
   ▼                     ▼
Is it SAFE? ──NO──► Ask user for guidance
   │               (unclear, risky)
   YES                 │
   ▼                    ▼
Proceed in AUTO     RESUME after clarification
```

---

## 3. What to Consider

### ✅ ALWAYS THINK ABOUT

- **Security** - Is this safe? Does it expose data?
- **Data integrity** - Will this break something?
- **Performance** - Is this efficient? Will it scale?
- **Test coverage** - Is this tested? How much?
- **Maintainability** - Is this readable? Extensible?
- **User requirements** - Does this solve the problem?
- **Future scalability** - Will this work at scale?

### ❌ NEVER THINK ABOUT

- Bypassing safety rules for "convenience"
- Skipping tests to "save time"
- Hardcoding secrets or credentials
- Creating security vulnerabilities
- Writing code that doesn't follow conventions
- Making assumptions without verification
- Proceeding when blocked or unclear

---

## 4. Escalation Triggers

### Always Escalate When

- Task involves production systems
- Requires secrets/credentials modification
- Could cause data loss
- Affects security configurations
- Involves breaking changes
- Unclear requirements or dependencies

---

## 5. Context Injection

### Before Any Task

1. **Project Context**
   - Technology stack (Java 21, Spring Boot 3.3, React 18)
   - Architecture (microservices, event-driven)
   - Key services and responsibilities

2. **Task Context**
   - What user wants to achieve
   - Constraints and requirements
   - Relevant files and patterns

3. **Safety Context**
   - Security requirements for payment data
   - Operations requiring human approval
   - What NOT to do

---

## 6. Output Formatting

### Standard Response

```
[TYPE] Summary

Context: Explanation
Files: Affected files
Tests: Test results
Next: Next steps (if any)
```

### Response Types

| Type | Usage |
|------|-------|
| [IMPLEMENT] | Completed implementation |
| [PLAN] | Proposed plan |
| [BLOCKED] | Needs clarification |
| [ERROR] | Something went wrong |
| [INFO] | Informational update |