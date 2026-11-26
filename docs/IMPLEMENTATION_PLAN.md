# Implementation Plan — Wishka MVP

Status: **All decisions resolved (D-29 postponed to first release). Awaiting your approval and "go" for Phase 0.**
Source of truth for business rules: `.claude/CLAUDE.md` (referred to below as CLAUDE.md).
This plan only orders the work; when the two disagree, CLAUDE.md wins and this plan gets fixed.

**Design (D-19):** UI follows the Claude Design designs you provide (format: see `docs/design/README.md`), produced from `docs/design/designer-prompts.md`
(Package N ↔ Phase N). The frontend part of a phase starts only once its design package is available. If it isn't, I'll
stop and ask rather than invent UI.

Each phase ends with: green `./mvnw verify` + `npm run lint && npm run type-check && npm run test:unit && npm run build`,
updated `.env.example`, updated docs, conventional commits, a summary, and **a "go" from you before the next phase**.

---

## 0. Versions (checked 2026-11-25 against the official registries)

Checked against Maven Central metadata, the npm registry `latest` dist-tag and the Adoptium / Node.js release indexes,
not from memory.

| Component | Version | Notes |
|---|---|---|
| Java (LTS) | **25** | Latest LTS (27 is the newest non-LTS). Local JDK: 25.0.5.1 ✔ |
| Spring Boot | **4.2.0** (GA) | Starter parent; Spring Framework 7 / Spring Security 7 through the BOM |
| Flyway | 13.13.0 | Managed by the Boot BOM; we use the BOM version, not this number |
| Testcontainers | 2.0.5 | Managed by the Boot BOM |
| ArchUnit (junit5) | 1.5.1 | |
| Jsoup | 1.24.1 | |
| Web Push (Java) | `nl.martijndwars:web-push` 5.1.2 | Chosen (D-17) |
| PostgreSQL | **17** | Dev: container `dev-postgres` (`postgres:17-alpine`, `127.0.0.1:5432`, network `dev_network`). Resolved D-1 |
| Vue | **3.5.46** | |
| Vite | **8.3.8** | Needs Node `^20.19 \|\| >=22.12` |
| Tailwind CSS | **4.3.5** | Through `@tailwindcss/vite` 4.3.5 (no PostCSS config needed) |
| vite-plugin-pwa | **2.0.0** | Peer: vite ≤ 8 ✔, workbox 7.4 |
| @vitejs/plugin-vue | 6.0.11 | |
| vue-i18n | 11.4.23 | |
| Pinia | 4.1.0 | |
| Vue Router | 5.5.1 | |
| TypeScript | **6.0.3** (pinned `~6.0`) | `latest` is 7.0.2 (native Go compiler). Not used, per D-16 |
| Vitest | 5.1.2 | |
| Playwright | 1.65.1 | |
| Node.js | **26 LTS** | Pinned with `.nvmrc` + `engines`. TeamCity agent (currently Node 22 via NodeSource) and local machine (currently 20.20.2) get upgraded. Resolved D-15 |

The exact versions get pinned in `pom.xml` / `package.json` in Phase 0, and Phase 0 records them in `docs/adr/0001-stack-versions.md`.

---

## 1. Cross-cutting conventions (apply to every phase)

**API namespaces.** These namespaces are what keeps owner data and giver data apart:

| Prefix | Audience | May contain reservation data? |
|---|---|---|
| `/api/auth/**` | anonymous / auth flows | no |
| `/api/me/**` | **owner** (own items, lists, circles, profile, prefs) | **never** |
| `/api/shared/**` | logged-in giver (lists shared with me via a circle) | yes, giver DTOs |
| `/api/public/**` | anonymous link visitor and guest reservation flow | "reserved" only, no names |
| `/api/reservations/**` | the giver's *own* reservations ("My reservations") | yes, about others' items only |
| `/api/notifications/**`, `/api/push/**` | recipient's own inbox and devices | only events addressed to the recipient |

**Backend package layout (per feature):** `<feature>/{domain, application, web, infrastructure}`. Controllers →
application services → repositories. Each feature exposes a small `api` package (facade service + events + DTOs)
for other features. ArchUnit enforces that rule. DTOs are Java `record`s. Errors use `ProblemDetail` (RFC 9457) with
`code` (e.g. `ITEM_NOT_FOUND`) + `params`. The frontend translates the code.

**i18n gate:** a test fails the build if `messages_pl.properties` ≠ `messages_en.properties` key sets, if
`pl.json` ≠ `en.json` key sets, or if an email template exists for only one locale. ESLint `@intlify/vue-i18n/no-raw-text`
is set to `error`.

**Migration naming:** `V{n}__{phase}_{description}.sql`, e.g. `V3__p1_users.sql`. Applied migrations are never edited.

**Test data:** a fixture builder (`TestData.user()...`) shared by integration tests. Every integration test runs against
Testcontainers PostgreSQL 17, so it never touches your dev DB.

---

## Phase 0 — Scaffolding

**Goal:** an empty but complete skeleton that builds, tests and ships through TeamCity to Nexus, with i18n and
architecture rules in place before any feature code exists.

### Scope
**Infra / repo**
- Monorepo: `/backend`, `/frontend`, `/deploy` (empty placeholder), `/docs` (`adr/`, this plan). No `.teamcity/` (D-36).
- `.editorconfig`, `.gitignore` (incl. `.env`, `/data`), root `README.md` (how to run locally).
- `.env.example` documenting every variable: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`,
  `APP_NAME` (=`Wishka`), `APP_BASE_URL` (dev `http://localhost:5173`, prod `https://wishka.pl`), `IMAGE_STORAGE_PATH`, `GOOGLE_CLIENT_ID/SECRET` (placeholder for Phase 1),
  `TURNSTILE_*`, `VAPID_*` (placeholders, marked "phase N").
- Dev connects to the **existing** containers from `~/Dev/dev-infra` (no duplicates): `dev-postgres` at
  `127.0.0.1:5432` (database `wishka`, created with a documented `psql` one-liner) and `dev-mailpit`
  (SMTP `127.0.0.1:1025`, UI `http://127.0.0.1:8025`). From containers on `dev_network`, use hostnames `dev-postgres` / `dev-mailpit`.
- `docker-compose.dev.yml` is **not** added (no duplicates).

**Backend**
- Maven project (`./mvnw`), Boot 4.2 parent, Java 25 toolchain, `spring-boot-starter-{web,validation,data-jpa,security,
  thymeleaf,mail,actuator,oauth2-client}`, `spring-session-jdbc`, Flyway + `flyway-database-postgresql`, springdoc-openapi.
- `application.yml` + profiles `dev`, `test`, `prod`; every secret comes from env.
- `MessageSource` (`messages`, default `pl`, fallback off), `LocaleResolver` (user → `Accept-Language` → `pl`).
- Global `@RestControllerAdvice` → `ProblemDetail` with `code` + `params`; Bean Validation errors mapped to codes.
- Flyway `V1__p0_baseline.sql` (extensions: `citext`, `pgcrypto`; schema comment).
- Spotless (palantir-java-format) + Maven Enforcer (Java 25, no SNAPSHOTs).
- Testcontainers base class `AbstractIntegrationTest` (`@ServiceConnection` PostgreSQL 17, container reuse locally).
- ArchUnit `ModuleRulesTest`: no package cycles between features; feature X may only use `Y.api..`; controllers don't touch
  repositories; entities never leave `application` (no `@Entity` in `web`). The spoiler rules come in Phase 5/6
  and appear as **disabled placeholders** now.
- `GET /actuator/health`, `GET /api/public/ping` (returns a localized message to prove i18n wiring).

**Frontend**
- Vite 8 + Vue 3.5 + TS + Pinia + Vue Router + vue-i18n + Tailwind 4 (`@tailwindcss/vite`).
- `src/locales/pl.json`, `en.json`, locale bootstrap (profile → browser → `pl`), `Intl` formatters for money/date
  (default currency PLN).
- `src/api/http.ts`: fetch wrapper with `credentials: 'include'`, CSRF header from the `XSRF-TOKEN` cookie, ProblemDetail
  → translated toast via a global `useToast` store.
- ESLint (flat config, vue + ts + vue-i18n plugin), Prettier, Vitest + Vue Test Utils, Playwright installed (one smoke test).
- Vite dev proxy `/api`, `/oauth2`, `/login` → `localhost:8080`, so the session cookie is same-origin.

**TeamCity** (project and the `Backend` / `Frontend` build configurations already created in the UI; steps added in
Phase 0. Configured **in the TeamCity UI** per D-36. The exact steps and parameters are documented in `docs/ci.md`, so you apply them yourself)
- `Backend`: `./mvnw -B verify` (JDK `env.JDK_25`), test reports, then build image `wishka-backend`.
  `Frontend`: `npm ci`, lint, type-check, test:unit, build, then build image `wishka-frontend` (nginx + `dist`).
  Each pushes its image to Nexus `localhost:8082/wishka-*:%build.number%` on every build, and on `main` also to
  `ghcr.io/madmatts/wishka-*:%build.number%` + `:latest` (D-28). VCS trigger on all branches.
- Secrets as TeamCity Password parameters: `env.GHCR_USER`, `env.GHCR_TOKEN`, Nexus credentials. Never in the repo.
- Maven: `settings.xml` mirror → `http://nexus:8081/repository/maven-public/` (agent is on `teamcity-net`). Credentials
  come from the TeamCity "Maven Settings" upload (`nexus-releases` server id), never from the repo. npm: per D-14.
- Docker images: hosted Nexus repo `docker-private` on port 8082 (to be created, D-14). **Push to `localhost:8082`,
  not `nexus:8082`**: the agent drives the *host* Docker daemon through the socket, and the host can't resolve `nexus`.
  `localhost` registries are trusted as insecure by default.
- JDK: build uses `env.JDK_25` from the agent image.
- `docs/ci.md`: the agent has `/var/run/docker.sock` mounted and runs privileged, so Testcontainers works. Containers it starts
  are siblings on the host, so set `TESTCONTAINERS_HOST_OVERRIDE` to the host gateway (or attach them to `dev_network`). This gets
  verified with the first green build. Also documents Node/JDK versions required on the agent.

### Flyway migrations
- `V1__p0_baseline.sql`

### API endpoints
- `GET /actuator/health`, `GET /api/public/ping`, `GET /v3/api-docs` (dev only)

### i18n keys
- `common.*` (app name, generic errors), `errors.VALIDATION_FAILED`, `errors.INTERNAL`, `errors.NOT_FOUND`.

### Tests
- Context loads against Testcontainers PG17 with Flyway applied.
- `ProblemDetailAdviceTest` (code + params, localized `detail`).
- `I18nKeyParityTest` (backend), `locales.spec.ts` (frontend parity), `ModuleRulesTest`.
- Vitest: `http.ts` CSRF header + error mapping. Playwright smoke: the app loads in PL and switches to EN.

### Acceptance criteria (manual)
1. `./mvnw verify` passes locally. `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` starts against your Postgres,
   and `flyway_schema_history` shows V1.
2. `npm run dev` → the page shows a Polish placeholder. Switching to EN changes the text and nothing stays hard-coded.
3. `curl -H 'Accept-Language: en' localhost:8080/api/public/ping` returns English, `pl` returns Polish.
4. TeamCity runs all three builds green; images appear in Nexus.
5. Removing one key from `en.json` makes the frontend build fail.

### Risks / open questions
- Testcontainers networking from the containerized agent (see `docs/ci.md`).
- Requires the D-14 Nexus repositories and the D-15 agent upgrade before the TeamCity part can go green.

---

## Phase 1 — Auth and notification foundation

**Goal:** people can create accounts and log in with email/password or Google. The outbox + email pipeline exists and
delivers localized emails to Mailpit.

### Scope
**Backend**
- `User` (email `citext` unique, display_name, password_hash nullable, locale, email_verified_at, created_at),
  `UserAuthProvider` (provider, subject).
- Spring Security: form-less JSON login (`POST /api/auth/login`), Spring Session JDBC, cookie `HttpOnly; Secure (prod);
  SameSite=Lax`, `CookieCsrfTokenRepository` (SPA pattern), 401/403 as ProblemDetail, session fixation protection,
  Argon2/BCrypt password encoder, login throttling: per-IP in memory, per-email in DB (D-10).
- Registration → `EmailVerificationToken` (hashed, 24 h) → verification email. Unverified users can log in but see a
  "verify your email" banner, **cannot share/reserve/invite**, and may create at most **5 items** (D-6).
- Password reset: request (always returns 202, no account enumeration) → token (hashed, 1 h) → confirm.
- Google OAuth2 login: links to an existing account when the Google email is verified and matches (D-5; after linking, Google alone is enough to log in), otherwise creates a user
  (locale from `Accept-Language`). Success redirects to frontend.
- `PATCH /api/me` (display name, locale).
- **Notification foundation (`notification` module):**
  - `OutboxEvent` (id, type, recipient ref (user id or guest email + locale), payload jsonb, dedup_key unique,
    status, attempts, next_attempt_at, created_at, processed_at).
  - `NotificationPublisher.publish(event)` writes in the caller's transaction (`MANDATORY` propagation).
  - `OutboxDispatcher` `@Scheduled` (fixed delay ~2 s), `SELECT … FOR UPDATE SKIP LOCKED` batch, exponential backoff,
    max attempts → `FAILED`. Idempotency: per (event, channel) delivery row `NotificationDelivery` unique.
  - `NotificationChannel` SPI; `EmailChannel` (Thymeleaf `templates/email/{type}_{locale}.html` + subject from
    `MessageSource`, rendered in **recipient's locale**). `InAppChannel` and `WebPushChannel` come in Phase 7.
  - Account emails (verification, password reset) are **mandatory** events: they skip preferences.

**Frontend**
- Views: Register, Login (with "Continue with Google"), VerifyEmail (token from URL), ForgotPassword, ResetPassword,
  Profile (display name, language).
- `useAuth` store (Pinia: session user), router guards (`requiresAuth`, `guestOnly`).
- **App shell** (D-19): responsive layout (desktop sidebar / mobile bottom nav, top bar, toasts, verify banner) built from
  the **design Package 1** screens, together with the design tokens → Tailwind 4 `@theme`.

### Flyway migrations
- `V2__p1_spring_session.sql` (Spring Session JDBC schema, copied from the jar, so not auto-initialized)
- `V3__p1_users.sql` (users, user_auth_providers, email_verification_tokens, password_reset_tokens)
- `V4__p1_outbox.sql` (outbox_events, notification_deliveries)

### API endpoints
- `GET /api/auth/csrf` (initializes the token cookie)
- `POST /api/auth/register`, `POST /api/auth/verify-email`, `POST /api/auth/verify-email/resend`
- `POST /api/auth/login`, `POST /api/auth/logout`
- `POST /api/auth/password-reset/request`, `POST /api/auth/password-reset/confirm`
- `GET /oauth2/authorization/google`, `GET /login/oauth2/code/google` (Spring)
- `GET /api/me`, `PATCH /api/me`

### i18n keys
- `auth.*` (forms, validation, banners), `profile.*`, `errors.EMAIL_TAKEN`, `errors.INVALID_CREDENTIALS`,
  `errors.TOKEN_INVALID_OR_EXPIRED`, `errors.EMAIL_NOT_VERIFIED`, `errors.TOO_MANY_REQUESTS`;
  backend `email.verify.*`, `email.passwordReset.*`, `email.common.footer`.

### Tests
- Registration → outbox row in same tx → dispatcher sends → mail arrives (GreenMail in tests) in PL and EN.
- Rollback of registration → no outbox row. Dispatcher retry/backoff, idempotency (two dispatcher runs → one email).
- Login/logout, CSRF required on POST, session cookie flags, password reset no-enumeration, expired tokens.
- OAuth: `oauth2Login()` mock → new user / linked user.
- ArchUnit: only `notification` touches outbox tables.

### Acceptance criteria (manual)
1. Register in PL → Mailpit shows a **Polish** verification email. Register with UI in EN → English email.
2. The verification link opens the app, the banner disappears.
3. Logging in sets an `HttpOnly` session cookie. A POST without the CSRF header gets 403 with a translated toast.
4. "Forgot password" for an unknown email shows the same message as for a known one. Reset works for a known one.
5. "Continue with Google" works (needs Google client credentials in `.env`).
6. Changing the language in Profile persists after logout/login.

### Risks / open questions
- Google OAuth redirect URIs for dev (`http://localhost:5173` via proxy) and prod. You need to create the OAuth client.
- Google OAuth client must be created by you (dev + prod redirect URIs).

---

## Phase 2 — Item pool and scraping

**Goal:** paste a shop URL → see a pre-filled preview → edit → save an item with a locally stored image. Manage
statuses and the archive.

### Step 2a — Scraping spike (first, before any extractor code)
- Throwaway CLI/test harness (`backend/src/test/.../ScrapingSpike`, `@Tag("spike")`, excluded from CI) fetches 3–5
  real product URLs per shop: **Allegro, Amazon.pl, x-kom, Komputronik, Media Expert, Rossmann**. Each fetch goes through the
  same SSRF-safe client and realistic headers.
- Record per shop in `docs/scraper-report.md`: HTTP status, blocking (403/captcha/JS challenge), presence of JSON-LD
  `Product`, OG tags, price/currency/image/name availability, needed headers, recommended strategy (generic / per-shop
  / not supported / needs headless → ADR).
- **Stop and show you the report** before 2b (a checkpoint inside the phase).

### Step 2b — Implementation
**Backend**
- `common.http.SafeHttpClient`: http/https only, ports 80/443 only (D-18), DNS resolve → reject loopback, private (10/8,
  172.16/12, 192.168/16), link-local (169.254/16 incl. metadata), CGNAT 100.64/10, multicast, `0.0.0.0/8`, IPv6
  `::1`, `fc00::/7`, `fe80::/10`, IPv4-mapped IPv6. **Connect to the validated IP** (pinning, so no DNS rebinding),
  re-validate on every redirect, max 3 redirects, total budget ~5 s, max body 2 MB (HTML) / 5 MB (image), per-user
  rate limit.
- `scraping`: `ProductExtractor` SPI, `GenericExtractor` (JSON-LD Product → OpenGraph → `<title>`/meta),
  per-shop extractors **only where the report justifies them**, `ProductData` record. Prices are normalized
  (`1 299,00 zł` → 1299.00 PLN).
- `item`: `Item` entity per CLAUDE.md (+ `updated_at`, `version`), statuses, soft delete (`DELETED` hidden everywhere
  except "archive"? see D-8), archive = status ≠ ACTIVE.
- Images: `ImageService` downloads through the SafeHttpClient, validates the real type by magic bytes (JPEG/PNG/WebP/GIF→
  re-encode), resizes to max 800 px with WebP output, stores at `${IMAGE_STORAGE_PATH}/{uuid}.webp`, strips EXIF. Manual
  upload is also allowed (same pipeline). Serving: random UUID names, no auth, `Cache-Control: immutable` (D-9).
- Preview flow: `POST /api/me/items/preview` returns data + a temporary image id. Saving binds it. A cleanup job removes
  orphaned temp images after 24 h.
- `Item` **does not know about reservations** (module boundary). Status changes / edits / deletes publish
  `ItemChanged`, `ItemStatusChanged`, `ItemDeleted` application events; Phase 6 consumes them.

**Frontend**
- Items pool view (card grid desktop / list mobile), filters (active / archive), add-item flow: URL → preview →
  edit form → save. Manual entry without a URL. Edit, change status, delete (with confirm).
- Price/currency formatting by locale. Priority badge.

### Flyway migrations
- `V5__p2_items.sql` (items, item_images or image columns, indexes on owner+status)

### API endpoints
- `POST /api/me/items/preview` `{url}` → `ItemPreviewDto`
- `POST /api/me/images` (multipart manual upload)
- `GET /api/me/items?status=ACTIVE|ARCHIVE`, `POST /api/me/items`, `GET/PUT/DELETE /api/me/items/{id}`
- `PATCH /api/me/items/{id}/status` `{status}`
- `GET /api/images/{uuid}.webp`

### i18n keys
- `items.*` (list, form, statuses, priorities, archive), `scraping.*` (preview, partial data notice),
  `errors.URL_NOT_ALLOWED`, `errors.SCRAPE_FAILED`, `errors.SCRAPE_TIMEOUT`, `errors.IMAGE_INVALID`,
  `errors.ITEM_NOT_FOUND`, `errors.RATE_LIMITED`.

### Tests
- **SSRF suite**: `127.0.0.1`, `localhost`, `[::1]`, `169.254.169.254`, `10.x`, decimal/octal IP forms, DNS name
  resolving to private IP, redirect from public → private, >3 redirects, `file://`, `gopher://`, oversized body,
  slow-loris timeout. Uses WireMock + a custom DNS resolver stub.
- Extractor tests on **saved HTML fixtures** from the spike (no live network in CI).
- Image pipeline: wrong content type, polyglot file, huge dimensions (decompression bomb guard).
- Item CRUD authorization: user A cannot read/edit user B's items (404, not 403).
- Owner spoiler baseline test registered for every item endpoint (passes trivially now; becomes meaningful in Phase 6).

### Acceptance criteria (manual)
1. `docs/scraper-report.md` exists and you've reviewed it.
2. Pasting a product URL from each supported shop pre-fills name/price/image (or shows a clear "fill in manually" notice
   for blocked shops).
3. Pasting `http://127.0.0.1:8080/actuator` or `http://169.254.169.254/` shows a translated "URL not allowed" error.
4. The image lives in `IMAGE_STORAGE_PATH` as WebP. The item card doesn't hotlink (check the Network tab).
5. Marking an item "bought by me" moves it to the archive. Restoring it returns it to the pool.

### Risks / open questions
- Allegro/Amazon/Media Expert likely block plain HTTP (Cloudflare/Akamai). A headless browser needs an ADR + container (CLAUDE.md §6).
  Per D-11 those shops become manual entry only in MVP (no headless browser, no Allegro API yet).
- Legal/ToS of scraping for a public product: revisit before public launch.

---

## Phase 3 — Wishlists

**Goal:** lists are named selections from the pool, with an optional occasion/date and a revocable share token.

### Scope
- Backend: `Wishlist`, `WishlistItem` (unique (wishlist_id, item_id), position). Add/remove/reorder items. An item can
  be on many lists. Share token = 32 random bytes base64url (stored as-is so the owner can copy it again, D-12), generate / revoke / regenerate
  (regenerate = revoke + new). Deleting a list doesn't delete items. Items with status ≠ ACTIVE are hidden from list
  views for givers (D-8). The owner sees them in the archive, including `DELETED` with Restore.
- Frontend: lists overview, list detail (owner view), "add from pool" picker, inline "new item → add to this list",
  share-link panel (copy, revoke, regenerate, with a warning that old links stop working).

### Flyway migrations
- `V6__p3_wishlists.sql` (wishlists, wishlist_items)

### API endpoints
- `GET/POST /api/me/wishlists`, `GET/PUT/DELETE /api/me/wishlists/{id}`
- `PUT /api/me/wishlists/{id}/items` (ordered id list), `POST/DELETE /api/me/wishlists/{id}/items/{itemId}`
- `POST /api/me/wishlists/{id}/share-link` (generate or regenerate), `DELETE /api/me/wishlists/{id}/share-link`

### i18n keys
- `wishlists.*` (form, occasion types, empty states, share link panel), `errors.WISHLIST_NOT_FOUND`.

### Tests
- Ownership/authorization, item on multiple lists, revoke → old token 404 (public endpoint arrives in Phase 5. Tested
  then; here the token rotation is tested at service level).
- Owner spoiler test registered for every new endpoint.

### Acceptance criteria (manual)
1. Create "Birthday 2027" with a date, add 3 items from the pool, reorder them.
2. The same item appears on two lists. Editing it once changes it on both.
3. Generate a link, regenerate it: the URL changes. Revoke: the panel shows "no link".

### Risks / open questions
- Occasion = enum `BIRTHDAY, NAME_DAY, CHRISTMAS, WEDDING, BABY, OTHER` + optional free-text label (D-13). The list date is
  also the default reservation due date (D-37).

---

## Phase 4 — Circles and invitations

**Goal:** owners build private audiences of registered users through email invitations.

### Scope
- Backend: `Circle` (max 5 per owner, enforced in service + DB-level check via trigger or count lock), `CircleMember`,
  `Invitation` (token hashed, 7-day expiry (D-4), statuses). Only the owner invites. Inviting an existing member returns a
  "already a member" error. Re-inviting a pending email reuses/refreshes the invitation. Invitation expiry job.
- Accept flow: link → `GET /api/public/invitations/{token}` (circle name + inviter display name only) → if logged in
  accept/decline with **any** logged-in account (D-3: we invite a person, not an address. The token is a single-use bearer secret) → if no account, register (email prefilled)
  → after verification, auto-return to the accept screen.
- Members: the owner can remove members. A member can leave a circle (D-4). Members of the same circle can see each other
  (`GET /api/shared/circles/{id}/members`).
- Notifications (through Phase 1 outbox, email now, in-app/push added in Phase 7): `CIRCLE_INVITATION_RECEIVED` (to
  invitee email, in invitee's locale if a user, otherwise the inviter's locale or `pl`), `CIRCLE_INVITATION_ACCEPTED` (to owner).
- Frontend: Circles view (owner), circle detail with members + pending invitations (revoke/resend), "Circles I'm in"
  view, invitation landing page.

### Flyway migrations
- `V7__p4_circles.sql` (circles, circle_members, invitations)

### API endpoints
- `GET/POST /api/me/circles`, `GET/PUT/DELETE /api/me/circles/{id}`
- `GET /api/me/circles/{id}/members`, `DELETE /api/me/circles/{id}/members/{userId}`
- `POST /api/me/circles/{id}/invitations`, `DELETE /api/me/circles/{id}/invitations/{invId}`,
  `POST /api/me/circles/{id}/invitations/{invId}/resend`
- `GET /api/public/invitations/{token}`, `POST /api/invitations/{token}/accept`, `POST /api/invitations/{token}/decline`
- `GET /api/shared/circles` (circles I'm a member of), `GET /api/shared/circles/{id}/members`,
  `DELETE /api/shared/circles/{id}/membership` (leave)

### i18n keys
- `circles.*`, `invitations.*`, `email.invitation.*`, `email.invitationAccepted.*`, `errors.CIRCLE_LIMIT_REACHED`,
  `errors.ALREADY_MEMBER`, `errors.INVITATION_INVALID`, `errors.CANNOT_JOIN_OWN_CIRCLE`.

### Tests
- 6th circle rejected (including the concurrent creation race). One-directional: a member of my circle gets 404 on my
  `/api/me/**` resources. Invitation token single-use/expiry. Register-from-invitation flow end-to-end.
- Outbox: invitation email in invitee locale (PL/EN), accepted email to owner.

### Acceptance criteria (manual)
1. Create 5 circles. The 6th shows a translated limit error.
2. Invite an email with no account → Mailpit shows the invitation → register from it → after verifying, you're a member.
3. Invite an existing user → they accept → the owner gets "invitation accepted" in Mailpit.
4. Decline / expired / revoked links show proper messages.

### Risks / open questions
- A forwarded invitation link lets whoever opens it first join (accepted trade-off of D-3). Mitigated by single use, 7-day
  expiry, the owner seeing "invited X, joined as Y" plus an "accepted" notification, and member removal.

---

## Phase 5 — Sharing and giver views

**Goal:** owners share lists with circles. Givers see shared lists. Anyone with the link sees the public view. Owner/giver
separation becomes a hard rule here.

### Scope
- Backend: `ListShare` (wishlist_id, circle_id unique). Owner sets the set of circles per list. Access check:
  viewer is a member of ≥1 circle the list is shared with **and** not the owner.
- `sharing` module builds read models:
  - `OwnerListView` / `OwnerItemView` (in `sharing.web.owner` / `wishlist.web` packages): **no reservation types**.
  - `GiverListView` / `GiverItemView` (in `sharing.web.giver`): has `reservation` sub-object. Filled in Phase 6.
    For now it's always "available", but the DTO shape is final.
  - `PublicListView` / `PublicItemView`: has `reserved: boolean`, never names.
- `GET /api/public/lists/{token}`: **if the session user is the list owner → returns `OwnerListView` with
  `viewerIsOwner: true`** (a separate response type via a sealed interface + Jackson type info). The frontend renders the owner
  page with a hint "this is how your list looks… (no reservation info shown)".
- A giver who is also the owner of a *different* list sees normal giver views. Owner opening `/shared/lists/{ownId}` → 404.
- Frontend: "Share with circles" panel on the list. "Shared with me" view (lists grouped by owner), giver list view,
  public list view (no login required, language from browser, switcher).
- ArchUnit (enabled now): classes in `..web.owner..` and every controller mapped under `/api/me/**` must not depend on
  `reservation..` or any `*Giver*`/`*Public*` DTO.

### Flyway migrations
- `V8__p5_list_shares.sql`

### API endpoints
- `PUT /api/me/wishlists/{id}/shares` `{circleIds[]}`, `GET /api/me/wishlists/{id}/shares`
- `GET /api/shared/lists`, `GET /api/shared/lists/{id}`
- `GET /api/public/lists/{token}`

### i18n keys
- `sharing.*`, `giver.*`, `public.*`, `errors.LIST_NOT_FOUND` (also used for "not shared with you", so nothing leaks).

### Tests
- Access matrix (owner / circle member / member of other circle / random user / anonymous) × (shared/unshared,
  token valid/revoked).
- Owner opening own token → owner DTO (assert JSON has no `reserved`, `reservation*` keys).
- Removing a circle share / removing a member revokes access immediately.
- Spoiler harness (see §Spoiler protection) gets wired here. It runs with the reservation table empty until Phase 6.

### Acceptance criteria (manual)
1. Share a list with "Family". A family member sees it under "Shared with me". A non-member gets "not found".
2. Open the public link in a private window: items visible, language switchable, no login needed.
3. Open your own public link while logged in: you see your owner view with the hint, not the giver view.
4. Unshare → the member loses access on refresh.

### Risks / open questions
- None open (D-7 resolved: no reserver identity in any giver/public view).

---

## Phase 6 — Reservations and conflicts

**Goal:** givers reserve items without the owner ever finding out. Guests reserve through a confirmed email flow. Conflicts
reach only the reserver.

### Scope
**Backend (`reservation` module)**
- `Reservation` per CLAUDE.md + `fulfilled_at`, `cancelled_reason`. **Partial unique index**
  `ON reservations(item_id) WHERE status IN ('PENDING','CONFIRMED')`. That guarantees one active reservation per item
  across all lists (golden rule 3). Concurrency: the unique-violation → `RESERVATION_TAKEN`.
- **Reservations never write to the `items` row** (no `updated_at`/`version` bump). This is a spoiler requirement.
- Logged-in reserve: allowed only through a list the giver can see (circle or token). Owner can't reserve own item
  (409 is fine here: the owner calling a giver endpoint on their own item is a 404, same as "not visible").
- Guest reserve (`/api/public/lists/{token}/items/{itemId}/reservation`): name, email, locale, Turnstile token
  (server-side verify, dev uses Cloudflare test keys) → `PENDING`, `expires_at = now + 24h` → outbox
  `GUEST_RESERVATION_CONFIRMATION_REQUEST` (confirm + cancel links, guest locale). The item is blocked while pending.
- Confirm / cancel by token (hashed, single-use for confirm. Cancel stays valid until the reservation ends).
- Rate limiting (D-10: IP in memory, email in DB): per IP and per email on guest endpoints (e.g. 5/h per email, 20/h per IP), plus Turnstile.
- Expiry job `@Scheduled` every 5 min: `PENDING` & `expires_at < now` → `EXPIRED` + `RESERVATION_EXPIRED` to the reserver.
- **Due date (D-37):** every reservation has `due_at`. Chosen by the reserver. Otherwise the occasion date of the list
  it was reserved through (if set and in the future). Otherwise now + 7 days. The reserver can extend it to a specific date
  (guests via the manage link in their email). The reserver gets notified of the due date and reminded before it
  (`RESERVATION_DUE_SOON`). No action by `due_at` → reservation ends, the item is released, and the reserver gets
  `RESERVATION_EXPIRED`. Same job as above.
- Giver cancels own reservation. Giver marks it "Bought" → `FULFILLED` (no expiry, stays blocked). Owner marks item
  `RECEIVED_AS_GIFT` → a `CONFIRMED` reservation silently becomes `FULFILLED` (D-20).
- Giver/public views show only `available` / `reserved` / `reservedByYou`. **No reserver identity anywhere** (D-7).
- **Conflicts:**
  1. Giver ↔ reserver: `POST …/conflict-request` (optional message, length-limited) → `RESERVATION_CONFLICT_REQUEST`
     to the reserver (email now, in-app + push in Phase 7). Rate-limited per (requester, item).
  2. Owner changed the item: `reservation` listens to `ItemChanged / ItemStatusChanged / ItemDeleted` from `item`
     (published in the owner's transaction) and writes outbox events `RESERVED_ITEM_CHANGED / _BOUGHT / _DELETED`
     **to the reserver only**. Owner bought/deleted → reservation `CANCELLED` (reason). "Changed" fires only for
     name, URL, price or a substantial description change. Note/image/priority edits are silent (D-20).
  The conflict-request email shows the requester's display name + email (they consent by sending) with
  `Reply-To` = requester, so the reserver makes the first move (D-7).
- Guest reserving with the list owner's email → generic `ITEM_NOT_AVAILABLE` (D-35).
- `My reservations` for givers: `GET /api/reservations` (items reserved by me, across owners). Guests use their links.
- **Outbox recipient guard** (see Spoiler section): every reservation-category event is checked against the item owner
  before it's stored and again before delivery.

**Frontend**
- Giver list view: Reserve / Cancel / "Already reserved — ask the reserver" button (logged-in givers only, D-21), "reserved" / "reserved by you".
- Guest reservation modal (name, email, Turnstile widget) → "check your email" state. Confirm and cancel landing pages.
- "My reservations" view with item changes highlighted.

### Flyway migrations
- `V9__p6_reservations.sql` (reservations + partial unique index, token hash indexes)
- `V10__p6_rate_limit.sql` (per-email counters, D-10)

### API endpoints
- `POST /api/shared/lists/{listId}/items/{itemId}/reservation`, `DELETE` same
- `POST /api/shared/lists/{listId}/items/{itemId}/conflict-request`
- `POST /api/public/lists/{token}/items/{itemId}/reservation` (guest) (no guest conflict requests, D-21)
- `POST /api/public/reservations/confirm` `{token}`, `POST /api/public/reservations/cancel` `{token}`
- `GET /api/reservations`, `POST /api/reservations/{id}/fulfilled`, `DELETE /api/reservations/{id}`

### i18n keys
- `reservations.*`, `conflicts.*`, `guest.*`, `email.guestConfirm.*`, `email.reservationConfirmed.*`,
  `email.reservationCancelled.*`, `email.reservationExpired.*`, `email.conflictRequest.*`, `email.reservedItemChanged.*`,
  `email.reservedItemBought.*`, `email.reservedItemDeleted.*`, `errors.RESERVATION_TAKEN`, `errors.CAPTCHA_FAILED`,
  `errors.RESERVATION_TOKEN_INVALID`.

### Tests
- Race: 20 parallel reservations of the same item → exactly 1 succeeds. Same item via two lists → second fails.
- Guest flow: pending blocks others. Confirm works once. Cancel link. Expiry job releases. Turnstile fail/mock.
  Rate limits hit → 429 with ProblemDetail.
- Identity test: no giver or public response ever contains a reserver's name/email (canary strings), D-7.
- Conflicts reach the reserver in their locale. **The owner gets nothing.**
- **Full spoiler regression suite across ALL item-returning endpoints and all notification channels** (see dedicated section).

### Acceptance criteria (manual)
1. As family member B, reserve an item on A's shared list → B sees "reserved by you". C (same circle) sees only "reserved", no name.
2. As A (owner), open the list, the item, the pool, the archive and your own public link: **no hint anywhere**. In Mailpit
   there's nothing addressed to A.
3. In a private window via the link: reserve as guest → Mailpit shows a confirmation email in the guest's chosen language
   with confirm + cancel links → after confirm, the item shows "reserved" (no name).
4. Leave a guest reservation unconfirmed. After 24 h (or by setting `expires_at` in the DB and waiting ≤5 min), it's released.
5. C clicks "ask the reserver" → B gets an email. A gets nothing.
6. A edits the price of the reserved item → B gets "item changed". A marks it bought → B gets "owner bought it".

### Risks / open questions
- Turnstile site key / secret per environment.

---

## Phase 7 — In-app notifications, Web Push and PWA

**Goal:** all MVP events reach people through all three channels with per-type preferences, and the app installs as a PWA.

### Scope
**Backend**
- `Notification` (recipient, type, payload: i18n key + params jsonb, read_at). `InAppChannel` writes them.
- `PushSubscription` (user, endpoint unique, p256dh, auth, user_agent, created_at, last_success_at). `WebPushChannel`
  with VAPID, payload text resolved in the recipient's locale, deletes subscription on 404/410.
- `NotificationPreference` (user, event_type, channel, enabled). Defaults: all on. Account emails are mandatory.
- All MVP event types wired to all 3 channels (`CIRCLE_INVITATION_RECEIVED/ACCEPTED`,
  `GUEST_RESERVATION_CONFIRMATION_REQUEST` (email-only, guests have no in-app/push), `RESERVATION_CONFIRMED/CANCELLED/
  EXPIRED`, `RESERVATION_CONFLICT_REQUEST`, `RESERVED_ITEM_CHANGED/BOUGHT/DELETED`, `SHARED_LIST_ITEM_ADDED` (to circle
  members of lists the item was added to. One notification per list, sent ~15 min after the last addition, D-22).
**Frontend**
- Notification center (bell with unread count, list, mark read / all read, deep links).
- Preferences matrix (event type × channel).
- `vite-plugin-pwa` (`injectManifest` strategy: custom SW for `push` + `notificationclick`), manifest PL/EN
  (single manifest, name "Wishka", Polish description, D-23), icons, offline app-shell. **API responses are never cached by the SW**.
- Push opt-in UX: on iOS Safari not installed → install instructions (Share → Add to Home Screen), ask only when
  standalone. Permission is requested only after a user gesture.
- Clipboard paste button ("Paste link") on mobile home. Android **Web Share Target** (`share_target` in manifest →
  `/items/new?url=…`).
- Responsive polish: desktop sidebar + card grid, mobile bottom nav + large touch targets, a11y pass (labels, focus,
  contrast, axe check in Playwright).

### Flyway migrations
- `V11__p7_notifications.sql` (notifications, push_subscriptions, notification_preferences)

### API endpoints
- `GET /api/notifications?page`, `GET /api/notifications/unread-count`, `POST /api/notifications/{id}/read`,
  `POST /api/notifications/read-all`
- `GET /api/me/notification-preferences`, `PUT /api/me/notification-preferences`
- `GET /api/push/vapid-public-key`, `POST /api/push/subscriptions`, `DELETE /api/push/subscriptions`

### i18n keys
- `notifications.*` (one title/body pair per event type), `preferences.*`, `pwa.*` (install guidance iOS/Android),
  `push.*` (backend payload texts).

### Tests
- Preference matrix honoured per channel. Mandatory emails ignore preferences.
- Push: mocked push service (WireMock) receives an encrypted payload. 410 → subscription removed.
- In-app notifications are only visible to their recipient.
- Spoiler suite extended: owner's notification center, push mock and inbox stay empty for every reservation/conflict
  scenario.
- Playwright: install banner logic (mocked `display-mode`), share target route.

### Acceptance criteria (manual)
1. Desktop Chrome: allow push → any event addressed to you (e.g. a conflict request) shows a system notification. Clicking opens the right page.
2. Android Chrome: install the app, share a link from a shop app → the add-item form opens pre-filled.
3. iPhone: Safari shows install guidance. After adding to home screen, push can be enabled and arrives.
4. Turn off "conflict request" push in preferences → only email + in-app arrive.
5. Resize the browser: sidebar ↔ bottom nav switch. Keyboard navigation works with visible focus.

### Risks / open questions
- iOS push needs HTTPS: tested through a Cloudflare Tunnel to the dev machine (D-24).
- None open.

---

## Phase 8 — Privacy and production

**Goal:** GDPR minimum and a reproducible production deployment on the VPS.

### Scope
- **Account deletion** (`DELETE /api/me`, re-auth with password or recent login): hard delete of personal data, the
  user's items/lists/circles/memberships/invitations/push subscriptions/notifications/sessions. Reservations they
  made: anonymised (`user_id` null) and **kept active** (D-25). If the deleted user owned items with active
  reservations, those reservers get a "list no longer exists" notification (D-25).
- **Guest data cleanup job:** guest_name/email nulled 30 days (D-26) after reservation reaches
  `EXPIRED/CANCELLED/FULFILLED`. Old outbox rows/deliveries purged (they contain emails) after 30 days.
- Privacy policy page (PL/EN, static Vue route, text supplied/approved by you).
- `deploy/docker-compose.prod.yml`: `nginx` (frontend static + `/api` proxy + `/api/images`), `backend`, `postgres:17`,
  `certbot` (Let's Encrypt, webroot renewal), `backup` (daily `pg_dump -Fc`, 7 daily / 4 weekly / 6 monthly retention,
  plus image volume tar) kept on the VPS only (no offsite copy in MVP, D-27).
- nginx: HTTP→HTTPS, HSTS, CSP (Turnstile + self), `Permissions-Policy`, gzip/brotli, SPA fallback, `client_max_body_size`.
- Backend prod hardening: `Secure` cookies, forwarded headers, actuator only `health` on internal port, structured
  JSON logs, no stack traces in ProblemDetail.
- `docs/deployment.md` runbook: VPS prerequisites, DNS, first deploy, `docker login ghcr.io` with a read-only
  (`read:packages`) token and `docker compose pull` of `ghcr.io/madmatts/wishka-*:${APP_VERSION}` (D-28), GHCR retention
  (keep the last N versions), env setup, cert issuance, restore-from-backup drill, upgrade procedure.
- `docs/email-domain-checklist.md`: provider choice (D-29), SPF, DKIM, DMARC (`p=none` → `quarantine`), return-path,
  testing with mail-tester.

### Flyway migrations
- `V12__p8_privacy.sql` (cascade/anonymisation helpers, indexes for cleanup jobs)

### API endpoints
- `DELETE /api/me` (no data export in MVP, D-30)

### i18n keys
- `privacy.*`, `account.delete.*`, `email.accountDeleted.*`.

### Tests
- Deletion removes all PII (query each table for the user's email/name afterwards). Reservations anonymised per D-25.
- Cleanup job respects the grace period. Restore drill documented and executed once against a scratch DB.

### Acceptance criteria (manual)
1. Delete a test account → login fails, Mailpit gets a confirmation, DB has no rows with that email.
2. `docker compose -f deploy/docker-compose.prod.yml up -d` on the VPS serves HTTPS with a valid cert, `/api` works.
3. A backup file appears daily. Following the runbook restore works.
4. mail-tester.com score ≥ 9/10 for a verification email from the prod domain.

### Risks / open questions
- No offsite backup (D-27, accepted). Email provider still to be chosen (D-29) before going live.

---

## Spoiler protection test strategy

Golden rule 1 is enforced by **four independent layers**. A spoiler leak would have to get past all four.

### Layer 1 — Structure (compile time / ArchUnit)
- Owner DTOs live in `..web.owner..` packages and controllers under `/api/me/**`. ArchUnit rules:
  - classes in `..web.owner..` and any `@RestController` whose `@RequestMapping` starts with `/api/me` must not
    depend on `..reservation..` or on any `*Giver*` / `*Public*` DTO;
  - `item`, `wishlist`, `circle`, `user` modules must not depend on `reservation` (reservation depends on them, never
    the reverse);
  - no DTO class may have a field typed with a reservation type unless it lives in `..web.giver..` / `..web.publicview..`
    / `reservation.web..`.
- Owner and giver views are **separate record types**, never one record with nullable fields (CLAUDE.md rule).
- The public endpoint returns a sealed `PublicListResponse permits OwnerListView, PublicListView`. The owner branch is
  built by the same assembler as `/api/me/wishlists/{id}`.
- Frontend: separate TS types (`OwnerItem` vs `GiverItem`) and separate components. Owner components can't import giver types
  (ESLint `no-restricted-imports` by folder).

### Layer 2 — Reservation-invariance tests (API level, the main safety net)
`SpoilerProtectionIT` (Testcontainers):
1. Build a fixture: owner O with items on several lists, shared with circles and with a link. Use a deterministic clock.
2. **Snapshot** every owner-facing response (headers minus `Date`/`Set-Cookie`, plus body).
3. Apply every reservation-side mutation: user reservation, guest PENDING, guest CONFIRMED, cancel, expiry, fulfilled,
   conflict request type 1, and owner edits that trigger type 2. Reserver names/emails are **canary strings**
   (`ZZ_CANARY_RESERVER`, `canary-reserver@example.test`).
4. Re-run all owner requests. Assert:
   - **byte-identical bodies and same status codes** to the snapshot (catches flags, counts, ordering, `updatedAt`, ETags);
   - no canary string and no forbidden JSON key (`reserv*`, `reserved`, `conflict*`, `giver*`) anywhere in the body.
5. Owner **write** actions on reserved vs unreserved items (edit, status change, delete, remove from list, unshare,
   revoke link) must return identical status/body shapes, so no "this item is reserved" errors.

**Coverage enforcement:** the endpoint list isn't hand-maintained. The test reads
`RequestMappingHandlerMapping` and requires every GET under `/api/me/**`, `/api/public/**` (as owner), `/api/notifications/**`
to be either exercised by the suite or explicitly listed in an allowlist with a justification. A new endpoint without a spoiler
test **fails the build**. That covers the CLAUDE.md requirement "every endpoint returning items has a test".

### Layer 3 — Notification guard
- `OutboxRecipientGuard`: every event of category `RESERVATION` / `CONFLICT` carries `itemId`. On publish **and**
  again on dispatch, if the resolved recipient is the item owner (by user id **or** by email, which also catches a guest
  using the owner's email), the event is dropped and logged as a security warning. The test suite configures the guard to throw.
- Tests for each event type × each channel: owner's GreenMail inbox empty, `notifications` table empty for owner,
  WireMock push endpoint received nothing for owner's subscriptions.
- `SHARED_LIST_ITEM_ADDED` never goes to the owner (they aren't a member of their own circle, and this gets asserted anyway).

### Layer 4 — E2E and transport
- Playwright scenario: B reserves, C sends a conflict request, A edits/buys. Then A's UI (pool, list, archive, own
  public link, notification center) is checked for canary text, and Mailpit API is queried for mail to A.
- HTTP: `Cache-Control: no-store` on all `/api/**`, `Vary: Cookie`. The service worker doesn't cache `/api/**` (a family
  sharing one tablet must not see a cached giver view after switching accounts). Logout clears Pinia stores.
- Residual risk: an owner who opens their own public link **logged out** (or from another account) could see
  "reserved". Mitigated by the public-link interstitial (D-31).

---

## Decisions (resolved 2026-11-26)

All decisions below are resolved unless marked ⏸ (postponed).

**Environment** (resolved 2026-11-26)
- **D-1 PostgreSQL** ✅ Use `dev-postgres` (`postgres:17-alpine`, `127.0.0.1:5432`, `dev_network`) from `~/Dev/dev-infra`.
- **D-2 Mailpit** ✅ Use `dev-mailpit` (SMTP `127.0.0.1:1025`, UI `http://127.0.0.1:8025`, `dev_network`).
- **D-14 Nexus** ✅ (taken from `~/Dev/team-city`) Maven through `maven-public`, credentials from the TeamCity-uploaded
  `settings.xml` (`nexus-releases` id). Backend JARs aren't deployed, only Docker images. **To create in Nexus before the
  Phase 0 CI step:** `docker-private` (docker hosted, HTTP 8082) and `npm-proxy` (npm proxy → registry.npmjs.org) +
  `npm-group`. Until `npm-proxy` exists, npm uses the public registry.
- **D-15 Node** ✅ Node 26 LTS everywhere. Agent: change `setup_22.x` → `setup_26.x` in `~/Dev/team-city/Dockerfile.agent`
  and rebuild. Local: upgrade from 20.20.2 (e.g. `nvm install 26`).
- **D-16 TypeScript** ✅ Pin TypeScript `~6.0` (6.0.3). No TS 7.
- **D-28 Getting images to the VPS** ✅ GHCR (private). TeamCity pushes from `main` with a `write:packages` token
  (created). The VPS pulls with a separate `read:packages` token. MinIO rejected (Community Edition archived Feb 2026,
  and it isn't a registry). Owner: `madmatts` → `ghcr.io/madmatts/wishka-*`.
- **D-36 TeamCity configuration source** ✅ TeamCity UI. Claude writes the step-by-step configuration (steps, parameters,
  triggers, Docker/GHCR login) into `docs/ci.md`, and you apply it in the UI. No Kotlin DSL in the repo.

**Code conventions**
- **D-32 Lombok** ✅ No Lombok. Modern Java instead: records for DTOs/events/value objects, explicit constructors
  for injection, hand-written accessors on JPA entities (no generated `equals`/`hashCode` on entities).
- **D-33 Project naming** ✅ Resolved in CLAUDE.md: brand **Wishka**, domain `wishka.pl`, `groupId`/base package
  `pl.wishka`, artifacts and images `wishka-backend` / `wishka-frontend`, DB `wishka`. Brand and domain come only from
  `APP_NAME` / `APP_BASE_URL` (i18n strings use a `{appName}` parameter). Sender from `MAIL_FROM` (address still to be
  supplied, needed by Phase 8; dev uses `no-reply@wishka.pl` towards Mailpit). No reason found to change the defaults.
- **D-34 Module boundaries** ✅ ArchUnit rules + custom outbox (as CLAUDE.md). Packages stay Modulith-compatible
  (one top-level package per feature, a public `api` subpackage, everything else internal), so Spring Modulith
  verification can be adopted later with a small change.

**Business rules**
- **D-3 Invitation acceptance** ✅ We invite a *person*, not an address. Anyone holding the (single-use, 7-day) link
  can accept with whatever account they use. The owner sees "invited `x`, joined as `y`" and gets the "accepted"
  notification. The owner can't accept an invitation to their own circle.
- **D-4 Invitations & membership** ✅ (1) Invitations expire after 7 days, and the owner can resend. (2) The owner can
  remove members (silently). (3) Members can leave. (4) Deleting a circle removes its list shares. Existing
  reservations stay, but every reservation gets a hard due date, see **D-37**.
- **D-5 Google linking** ✅ Auto-link when the Google-verified email matches. Afterwards Google alone is enough to log in
  (password optional, see D-38).
- **D-6 Unverified accounts** ✅ Can log in and create up to **5 items** and lists. Can't share, invite or reserve until
  the email is verified.
- **D-7 Reserver identity** ✅ Never shown to other givers or link visitors (only "reserved" / "reserved by you"). Contact
  goes through "Ask the reserver": Wishka emails the reserver (with the requester's name + email and Reply-To), and the
  reserver's email is exposed only if they reply. CLAUDE.md visibility rules updated accordingly.
- **D-8 Archived / deleted items** ✅ Givers and link visitors see only `ACTIVE` items. A reserver of a hidden item sees it
  in "My reservations" with the reason. The owner's archive includes `DELETED` items with Restore.
- **D-9 Image access** ✅ Served by unguessable random UUID name without auth (`Cache-Control: immutable`).
- **D-10 Rate limiting** ✅ Per-IP limits in memory (Bucket4j, reset on restart). Per-email limits counted in the DB.
- **D-11 Blocked shops** ✅ Shops the spike shows as blocked are **manual entry only** in MVP (link + manual name/price/photo).
  A headless browser or the Allegro API is reconsidered after MVP, and only with an ADR.
- **D-12 Share token storage** ✅ Share tokens stored as-is (re-copyable). All one-time tokens (verify, reset, invite,
  reservation confirm/cancel/manage) stored hashed.
- **D-13 Occasion** ✅ Enum (`BIRTHDAY, NAME_DAY, CHRISTMAS, WEDDING, BABY, OTHER`) + optional free-text label.
- **D-37 Reservation due date** ✅ Reserver sets it. Otherwise the list's occasion date (if set). Otherwise 7 days. The reserver is
  notified and reminded, and can extend to a chosen date. No action by the due date → reservation cancelled/released and
  the reserver is notified. Never visible to the owner.
- **D-38 Password after Google linking** ✅ The password keeps working alongside Google (no lock-in if Google access is lost).
  The profile allows setting or removing a password, but the last login method can't be removed.
- **D-20 Reservation end states** ✅ Reserver "Bought" → `FULFILLED`. Owner bought/deleted → `CANCELLED` + reserver notified.
  Owner "received as gift" → `CONFIRMED` silently becomes `FULFILLED`. "Item changed" only for name/URL/price/substantial
  description edits.
- **D-21 Guest conflict requests** ✅ Not in MVP. Only logged-in givers can "Ask the reserver".
- **D-22 New-item notifications** ✅ Bundled: one per list, ~15 min after the last addition.
- **D-23 PWA manifest** ✅ Single manifest: name/short_name "Wishka", Polish description. Per-locale manifests maybe later.
- **D-24 iPhone push testing** ✅ Cloudflare Tunnel to the dev machine during Phase 7.
- **D-25 Account deletion** ✅ (a) Reservations made by a deleted user stay active, anonymised. (b) When an owner deletes
  their account, holders of active reservations on their items are notified that the list no longer exists.
- **D-26 Guest data retention** ✅ Guest name/email deleted 30 days after the reservation ends.
- **D-27 Offsite backups** ✅ None in MVP: daily `pg_dump` + image archive stay on the VPS only. Accepted risk: losing the VPS
  loses the backups. Revisit before public launch.
- **D-29 Transactional email provider** ⏸ Postponed until the first release. Recommendation on record: keep OVH Zimbra
  for human mail and use a transactional provider for app mail from `no-reply@wishka.pl` (provider DKIM + SPF include
  at OVH). Until then all mail goes to Mailpit. The app only needs SMTP host/port/credentials + `MAIL_FROM` in `.env`.
- **D-30 Data export** ✅ Postponed to the GDPR iteration before public launch.
- **D-31 Owner opening own link logged out** ✅ Public links show an interstitial "If this is your list, log in so you
  don't spoil the surprise" (Log in / Continue), remembered per device after dismissal.
- **D-35 Owner reserving via link** ✅ Blocked with the generic "item not available" error. No email is sent to the owner.

**Plan structure**
- **D-19 UI from Claude Design** ✅ You provide designs per phase, made in Claude Design (light theme only in MVP).
  Hand-off format in `docs/design/README.md`. Prompts for the designer are in
  `docs/design/designer-prompts.md` (shared brief + Packages 1–8). The app shell and design tokens come with Package 1
  (Phase 1). Phase order unchanged.
- **D-17 Web Push library** ✅ `nl.martijndwars:web-push` (you've used it successfully before).
- **D-18 Ports for scraping** ✅ Only 80/443. Other ports are rejected as `URL_NOT_ALLOWED`.
