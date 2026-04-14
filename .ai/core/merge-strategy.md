# Merge Strategy

Priority order:
1. security
2. qa
3. backend
4. design
5. product

Rules:
- Higher priority overrides lower
- Never duplicate logic
- Preserve stricter validation
- Resolve conflicts deterministically

If conflict unclear:
→ mark as CONFLICT