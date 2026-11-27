# Wishka

Wishlists for family and friends: collect things you'd like, group them into lists, share them with your circles or a
link, and let others reserve gifts **without the recipient finding out**. Production: <https://wishka.pl>.

- Business rules and working agreements: [`.claude/CLAUDE.md`](.claude/CLAUDE.md)
- Phased plan and decisions: [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md)
- CI (TeamCity) setup: [`docs/ci.md`](docs/ci.md) · Design hand-off: [`docs/design/README.md`](docs/design/README.md)

## Repository

```
backend/    Spring Boot 4.2, Java 25, Maven (package pl.wishka)
frontend/   Vue 3.5 + TypeScript + Vite 8 + Tailwind 4 PWA
deploy/     production compose, nginx, backups (Phase 8)
docs/       plan, ADRs, design hand-off, CI
```

## Prerequisites

- JDK 25, Node.js 26 (`nvm use` reads `frontend/.nvmrc`), Docker (for Testcontainers).
- The existing dev containers: `dev-postgres` (PostgreSQL 17, database/user `wishka`) and `dev-mailpit`
  (SMTP `127.0.0.1:1025`, UI <http://127.0.0.1:8025>). This repo does not start its own copies.

## Run locally

```bash
cp .env.example .env.dev          # then set DB_PASSWORD etc. (.env.dev is git-ignored)

# Backend on :8080 (profile "dev" reads ../.env.dev; Flyway migrates the wishka database on start)
cd backend && ./mvnw spring-boot:run

# Frontend on :5173 (proxies /api to :8080)
cd frontend && npm install && npm run dev
```

Check: <http://localhost:5173> shows Wishka with "Serwer działa". `curl -H 'Accept-Language: en'
localhost:8080/api/public/ping` answers in English.

No local database at hand? `TestWishkaApplication` (backend test sources) starts the backend against a throwaway
Testcontainers PostgreSQL.

## Tests and checks

| Where | Command | What |
|---|---|---|
| backend | `./mvnw test` | unit tests, i18n key parity, ArchUnit module rules |
| backend | `./mvnw verify` | + integration tests (`*IT`, PostgreSQL 17 via Testcontainers) + Spotless check |
| backend | `./mvnw spotless:apply` | format Java code (palantir-java-format) |
| frontend | `npm run lint` | oxlint + ESLint, incl. "no raw text" and "missing i18n key" rules |
| frontend | `npm run type-check` | vue-tsc |
| frontend | `npm run test:unit` | Vitest (incl. PL/EN parity, backend error codes translated) |
| frontend | `npm run test:e2e` | Playwright smoke (first run: `npx playwright install chromium`) |
| frontend | `npm run build` | type-check + production build |

## i18n rule of thumb

Every user-facing text is a key in **both** `pl` and `en` (backend `i18n/messages_*.properties`, frontend
`src/locales/*.json`) in the same commit. The brand name is never written into translations: backend texts use
`{appName}`, frontend texts use `@:app.name`. The build fails otherwise.
