# ADR 0001: Stack and versions

- Status: accepted
- Date: 2026-11-27
- Context: CLAUDE.md §4 asks for the current stable versions, checked against the registries rather than recalled
  from memory. The decisions are recorded in `docs/IMPLEMENTATION_PLAN.md` (D-1…D-38).

## Decision

Versions pinned in Phase 0 (checked against Maven Central, the npm registry and the Adoptium / Node.js indexes on
2026-11-25/27):

| Area | Choice | Pinned in |
|---|---|---|
| Java | 25 (current LTS) | `backend/pom.xml` (`java.version`), Maven Enforcer |
| Spring Boot | 4.2.0 (BOM manages Spring 7, Security 7, Hibernate, Flyway 12.11, Testcontainers 2, Jackson 3) | `backend/pom.xml` parent |
| springdoc-openapi | 3.2.1 (API docs only, no UI) | `backend/pom.xml` |
| ArchUnit | 1.5.1 | `backend/pom.xml` |
| Spotless | 3.12.0 with palantir-java-format | `backend/pom.xml` |
| PostgreSQL | 17 (dev `dev-postgres`, tests `postgres:17-alpine`, prod 17) | `TestcontainersConfiguration` |
| Node.js | 26 LTS | `frontend/.nvmrc`, `engines` |
| Vue | 3.5.46, Vue Router 5.5, Pinia 4.1, vue-i18n 11.4 | `frontend/package.json` |
| Vite | 8.4 | `frontend/package.json` |
| TypeScript | ~6.0 (6.0.3). **Not 7.x** (D-16) | `frontend/package.json` |
| Tailwind CSS | 4.3 through `@tailwindcss/vite` | `frontend/package.json` |
| Vitest / Playwright | 5.1 / 1.65 | `frontend/package.json` |
| vite-plugin-pwa | 2.0 (added in Phase 7) | n/a yet |
| Web Push | `nl.martijndwars:web-push` 5.1.2 (added in Phase 7, D-17) | n/a yet |

Notes:
- **Flyway** comes from the Spring Boot BOM (12.11), not the latest Flyway release (13.x). Overriding the BOM for
  Flyway isn't worth the compatibility risk. It gets revisited with the next Boot upgrade.
- **No Lombok** (D-32): records for DTOs, explicit constructors.
- **Jackson 3** (`tools.jackson`) is the Boot 4 default, so inject `JsonMapper`.
- The frontend was scaffolded with `create-vue` (oxlint + ESLint + Prettier). ESLint adds
  `@intlify/eslint-plugin-vue-i18n` (`no-raw-text`, `no-missing-keys`) to enforce golden rule 4.

## Consequences
- Upgrades are deliberate: bump the version here and in the build file in the same commit, then run the full test
  suite.
- TypeScript 7 gets reconsidered once `vue-tsc` officially supports it.
