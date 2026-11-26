# CLAUDE.md — Wishka

**Wishka** (production domain: `wishka.pl`) is a web app (PWA) where users collect things they
want, group them into wishlists and share those lists with their circles (family, friends) or
via a link, so others can reserve and buy gifts **without the recipient finding out**.

Phase 1 audience: the author and his family. Target: public product on the Polish market.
UI languages: Polish (default) and English from day one.

### Naming and branding
- Product name: **Wishka**. It is a brand name: never translated, same in PL and EN, always
  written "Wishka" (capital W) in UI text, emails, push notifications, PWA manifest
  (`name`, `short_name`) and page titles.
- The product name and domain are **configuration, not literals** scattered in code: use a
  single `app.name` / `APP_NAME` setting and `APP_BASE_URL` (prod: `https://wishka.pl`) for
  every absolute link (emails, push, share links, OAuth redirect URIs).
- Technical names use lowercase `wishka`: Java base package and Maven `groupId` `pl.wishka`,
  artifacts `wishka-backend` / `wishka-frontend`, Docker images `wishka-backend` /
  `wishka-frontend`, DB name `wishka`. (Defaults. Flag in the plan if you see a reason to
  change them.)
- Email sender display name: "Wishka". The sender address is on `wishka.pl` and comes from
  configuration (`MAIL_FROM`); the author supplies the exact address.

---

## 1. Golden rules (never break these)

1. **Spoiler protection.** The owner of an item must never learn from the app that the item
   is reserved, by whom, or that a conflict exists.
   - Owner-facing endpoints (`/api/me/**`) never include reservation data, not even a
     boolean flag. Use separate DTOs for owner and giver views; never reuse one DTO with
     nullable reservation fields.
   - When the logged-in owner opens their own public share link, the backend returns the
     owner view (no reservations).
   - The owner never receives any notification (email, in-app, push) about reservations
     or reservation conflicts on their own items.
   - Every endpoint returning items has a test asserting that the owner's response contains
     no reservation data.
2. **Sharing is one-directional.** A circle belongs to exactly one owner and is that owner's
   private audience. Being in someone's circle gives that person no rights to your lists.
3. **Reservation belongs to the item, not the list.** An item placed on several lists is
   reserved once, everywhere. One active reservation per item (no quantities in MVP).
4. **No hard-coded user-facing text**, in frontend or backend. Every string goes through i18n
   (PL + EN) from the first commit, including emails, push notifications and error messages.
5. **The app never handles money.** (Relevant for future group gifts: it records pledges only.)
6. **Every outbound URL fetch is SSRF-protected** (see §6).

---

## 2. Domain model

| Entity | Notes |
|---|---|
| `User` | email, display name, password hash (nullable for Google-only), auth providers, `locale` (pl/en), notification preferences |
| `Item` | owned by user; name, url, image_path, description, price, currency, priority (`MUST_HAVE`, `NICE_TO_HAVE`), note, created_at; status `ACTIVE`, `BOUGHT_BY_OWNER`, `RECEIVED_AS_GIFT`, `DELETED` (soft delete) |
| `Wishlist` | owned by user; name, optional occasion + date, `share_token` (nullable, revocable, regenerable) |
| `WishlistItem` | many-to-many: a list is a *selection* from the user's item pool. Items in no list are private |
| `Circle` | owned by user, **max 5 per owner**; name |
| `CircleMember` | registered users only; membership created by accepting an invitation |
| `Invitation` | circle_id, email, token, status (`PENDING`, `ACCEPTED`, `DECLINED`, `EXPIRED`), expires_at. Only the circle owner invites. Invitee without an account registers first, then joins |
| `ListShare` | wishlist ↔ circle |
| `Reservation` | item_id (unique among active reservations), user_id (nullable) **or** guest_name + guest_email + guest_locale; status `PENDING`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, `FULFILLED`; confirm/cancel tokens; expires_at |
| `Notification` | in-app notification center entry: recipient, type, payload (i18n key + params), read_at |
| `PushSubscription` | Web Push endpoint + keys per user device |
| `OutboxEvent` | transactional outbox for notification delivery |

Archive view = items with status other than `ACTIVE`.

### Visibility rules
- **Owner:** sees own items, lists, circles; never reservation data.
- **Circle member** viewing a list shared with their circle: sees items and reservation status.
  Sees the reserver's *name* only if the reserver is a member of the same circle; otherwise
  only "reserved".
- **Link visitor (anonymous):** sees items and "reserved" (no names).
- Circle members of the same circle can see each other (needed for future group gifts).

### Reservation flows
- **Logged-in user:** reservation is `CONFIRMED` immediately.
- **Guest (from link):** provides name + email, passes Cloudflare Turnstile → `PENDING`
  (expires after 24 h) → email with confirm link → `CONFIRMED`. The item is blocked
  while `PENDING`. The confirmation email also contains a cancel link. Expired pending
  reservations are released by a scheduled job.
- Rate limiting per IP and per email on guest endpoints. No one-click reservation without
  confirmation for guests.

### Conflicts (never notify the owner)
1. **Giver ↔ reserver:** a giver wants an already reserved item → "ask the reserver to
   release it or buy it together" → notification to the reserver (email + in-app + push).
2. **Owner changed the item:** owner marks as bought, deletes, or edits a reserved item →
   notification to the reserver.

---

## 3. Notifications (core feature, not an add-on)

Notify the people concerned as quickly and precisely as possible.

- Channels: **email**, **in-app notification center**, **Web Push** (VAPID). All in MVP.
- Architecture: domain services publish domain events → stored in `OutboxEvent` in the same
  transaction → a scheduled dispatcher delivers them to the channels with retries and
  idempotency. Channel adapters: `EmailChannel`, `InAppChannel`, `WebPushChannel`.
- Per-user preferences: which event types arrive on which channel.
- Content is rendered in the **recipient's locale** (user locale, or guest_locale for guests).
- Event types (MVP): circle invitation received / accepted, guest reservation confirmation
  request, reservation confirmed / cancelled / expired (to reserver), conflict request
  (to reserver), reserved item changed / bought / deleted (to reserver), new item added
  to a list shared with you (to circle members, respecting preferences).
- iOS: Web Push works only for the PWA installed on the home screen. The UI must guide
  the user to install it before asking for push permission.
- Dev: all email goes to Mailpit.

---

## 4. Tech stack

**Backend:** Java (current LTS), Spring Boot (current stable), Maven, Spring Web,
Spring Security (session-based auth: HttpOnly + SameSite cookie with CSRF protection,
Spring Session JDBC, no JWT in localStorage), OAuth2 client (Google), Spring Data JPA,
PostgreSQL 17, Flyway, Thymeleaf (email templates per locale), `MessageSource` for i18n,
Jsoup (scraping), a Java Web Push library (VAPID), Bean Validation.
Testing: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), ArchUnit for module boundaries.

**Frontend:** Vue 3 (Composition API, `<script setup>`, TypeScript), Vite, Pinia, Vue Router,
vue-i18n, Tailwind CSS, vite-plugin-pwa (service worker, manifest, push handling).
Testing: Vitest + Vue Test Utils; Playwright for key E2E flows.

Before choosing exact versions, check the latest stable releases; do not rely on memory.

**No Redis in MVP.** Background work uses the DB outbox + `@Scheduled`.

---

## 5. Repository layout (monorepo)

```
/backend                 Spring Boot app (base package pl.wishka)
  src/main/java/pl/wishka/
    common/              errors, i18n, security utils, SSRF-safe HTTP client
    auth/                registration, login, Google OAuth, email verification, password reset
    user/                profile, locale, notification preferences, account deletion
    item/                item pool, statuses, archive, image storage
    scraping/            ProductExtractor SPI + per-shop extractors + generic fallback
    wishlist/            lists, list-item selection, share tokens
    circle/              circles, members, invitations
    sharing/             list ↔ circle shares, public link view
    reservation/         reservations, guest flow, conflicts, expiry job
    notification/        outbox, dispatcher, channels, notification center, push subscriptions
/frontend                Vue PWA
  src/locales/pl.json, en.json
/deploy                  docker-compose.prod.yml, nginx config, backup scripts
/docs                    IMPLEMENTATION_PLAN.md, ADRs, scraper-report.md
.teamcity/               Kotlin DSL build config (if used)
```

Package by feature. Modules talk through services and domain events, never through
another module's repositories. Enforce with ArchUnit.

---

## 6. Product data scraping

- Interface `ProductExtractor { boolean supports(URI); ProductData extract(URI); }`
  with per-domain implementations and a `GenericExtractor` fallback:
  JSON-LD `schema.org/Product` → Open Graph → `<title>`/meta.
- Target shops: Allegro, Amazon (.pl), x-kom, Komputronik, Media Expert, Rossmann
  (more later). Some shops actively block bots. **Before writing extractors, run a spike**
  against real product URLs from each shop and record the results in `docs/scraper-report.md`
  (what works, what is blocked, which fields are available). A headless browser is
  introduced only if the report justifies it, as a separate container.
- Scraping only pre-fills the form; the user can always complete or override data manually.
- **SSRF protection (mandatory):** http/https only; resolve DNS and reject private, loopback,
  link-local and metadata ranges (also after every redirect); max 3 redirects; connect/read
  timeouts (~5 s total); max response size; per-user rate limit.
- Images: download the thumbnail, validate the content type, resize, store locally
  (volume `/data/images`). Never hotlink.

---

## 7. i18n

- Languages: `pl` (default), `en`. Every new text means a key in **both** dictionaries in the
  same commit.
- The brand name "Wishka" is not translated. Reference it through the `app.name` setting or
  an interpolation parameter, not a literal in each string.
- Frontend: vue-i18n, keys namespaced by feature (`items.form.name`). Locale from the user
  profile, otherwise from the browser.
- Backend: `messages_pl.properties` / `messages_en.properties`, email templates
  per locale, push payload texts resolved in the recipient's locale.
- The API returns machine-readable error codes (`ITEM_NOT_FOUND`, `RESERVATION_TAKEN`) plus
  params; the frontend translates them. Use Problem Details (RFC 9457) as the error format.
- Format money, dates and numbers by locale. Default currency is PLN.

---

## 8. UI / UX

- Responsive PWA. Desktop: sidebar navigation, card grid, wider forms. Mobile: single column,
  bottom navigation, large touch targets.
- Adding an item: paste a URL → preview of the scraped data → edit → save.
  On mobile, a "paste link from clipboard" button on the home screen. Android also gets
  Web Share Target (not supported on iOS).
- Accessible: semantic HTML, labels, focus states, sufficient contrast.

---

## 9. Environments

**Dev** (author's machine, Docker): existing containers TeamCity server, TeamCity agent
(Maven, JDK, Node.js), PostgreSQL 17, Mailpit, Nexus.
- Connect to the existing PostgreSQL and Mailpit through environment variables
  (`.env.example` documents all of them). Do not start duplicates unless asked.
- Nexus: Maven proxy/artifacts and Docker image registry.
- TeamCity pipeline: backend build + tests, frontend lint + tests + build, Docker images
  pushed to Nexus. Testcontainers requires Docker access on the agent; document it.

**Prod:** VPS with Docker Compose, served at `https://wishka.pl` (`www` redirects to the
apex domain): `nginx` (TLS via Let's Encrypt, serves the frontend, proxies `/api`),
`backend`, `postgres`, plus a daily `pg_dump` backup with retention.
Secrets only via env/`.env` (never committed). Transactional email provider on the
`wishka.pl` domain (SPF, DKIM, DMARC). Environment-specific values (`APP_BASE_URL`,
`APP_NAME`, `MAIL_FROM`) live in configuration, never in code.

---

## 10. Privacy (minimum in MVP)

- Account deletion (hard delete of personal data, anonymised reservations).
- Guest personal data deleted when the reservation ends (expired, cancelled, fulfilled)
  plus a grace period.
- Privacy policy page (PL/EN). Full GDPR scope comes before public launch.

---

## 11. Working agreements

- Work phase by phase according to `docs/IMPLEMENTATION_PLAN.md`. Do not start a new phase
  before the current one builds, passes tests and has been accepted.
- Every phase ends with: green tests, updated `.env.example`, updated docs, a short summary
  of what was done and what is next.
- Database changes only through new Flyway migrations; never edit applied ones.
- Prefer simple, explicit code over clever abstractions. No new infrastructure
  (Redis, message broker, headless browser) without a written ADR in `/docs`.
- Commit messages in English, conventional commits (`feat:`, `fix:`, `chore:`).
- When unsure about a business rule, ask. Don't guess.

---

## 12. Roadmap (out of MVP scope; do not build yet)

- **Iteration 2: group gifts.** A circle member starts a group gift for an item, invites other
  members of the same circle, members propose contribution amounts and vote; with no proposals
  the price is split equally. Pledge tracking only, no payments.
- **Iteration 3:** public launch (full GDPR, terms), affiliate links, periodic price refresh
  and price history, reminders before occasions.
- **Later:** item groups ("list within a list", e.g. a book series where single books can be
  reserved individually).