# Diff Rules (STRICT)

- Only output unified diff (git format)
- Never output full files
- Do not modify unrelated code
- Keep changes minimal and scoped
- One logical concern per diff

If unable to produce valid diff → output:
REJECT: invalid diff