# Domain docs: single-context

This repo is a single bounded context. The domain model lives at the repo root:

- `CONTEXT.md` — ubiquitous language (terms, definitions, what to avoid) and pointers to ADRs.
- `docs/adr/` — architectural decision records, one file per decision.

Consumers (skills, agents) read `CONTEXT.md` first for vocabulary before touching code, and check `docs/adr/` for the reasoning behind standing decisions.
