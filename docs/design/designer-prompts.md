# Wishka — Designer prompts

How to use: paste **Prompt 0 (shared brief)** first, then one package prompt at a time, in order.
The packages follow the implementation phases, so design can stay one step ahead of development.
Each package prompt is self-contained, but it assumes the designer already has Prompt 0.

Delivery order needed by development: **Package 1 before Phase 1 starts**, then each next package before its phase.

---

## Prompt 0 — Shared brief (paste first, once)

> You are designing **Wishka** (wishka.pl), a responsive Progressive Web App for wishlists. People collect things
> they'd like to receive (pasted from online shops or entered by hand), group them into wishlists for occasions
> (birthday, name day, Christmas…), and share those lists with their private "circles" (family, friends) or through a
> public link. Others can then **reserve** a gift so nobody buys the same thing, **without the recipient ever
> finding out**.
>
> **Audience:** Polish families first, of all ages, including grandparents on phones. Then a public product on the
> Polish market. Warm, friendly, trustworthy, calm. Not childish and not "e-commerce loud".
>
> **Brand:** The name is "Wishka", always with a capital W, never translated. Please propose a logo mark / app icon
> (works at 48 px and as a maskable PWA icon), a primary color, accent, neutrals, and semantic colors (success, warning,
> danger, info). All text/background pairs must meet **WCAG 2.2 AA** contrast.
>
> **Languages:** Polish is the default, English the second. Design every screen with **real Polish copy** and check
> that English fits too (Polish strings are often 20–30 % longer). No lorem ipsum.
> Money looks like `1 299,00 zł` in Polish and `PLN 1,299.00` in English. Dates look like `24.12.2027` / `24 Dec 2027`.
>
> **Platforms and frames:** mobile **390 × 844** (primary) and desktop **1440 × 900**. Tablet behavior can be
> described in notes. Mobile: single column, **bottom navigation**, touch targets ≥ 44 × 44 px, important actions within
> thumb reach. Desktop: **left sidebar navigation**, card grids, wider forms (max ~640 px).
> **Light theme only** for MVP. Still name the color tokens by role (`surface`, `text-muted`, `primary`…), not by value,
> so a dark theme can be added later without redesign.
>
> **Implementation target:** Vue 3 + **Tailwind CSS 4**. Please deliver:
> - Design tokens as **CSS custom properties** for color, spacing (4 px base), radius, shadow and typography, named so
>   they map 1:1 to Tailwind 4 `@theme` variables (e.g. `--color-primary-600`, `--radius-lg`, `--spacing`). Keep them in
>   one shared stylesheet that every screen uses, with no one-off hard-coded values in screens.
> - Build screens as real HTML/CSS (flex/grid, semantic elements, real `<label>`s), so the layout translates directly to Vue
>   components. One screen or state per page/frame, named `P{package}-{screen}-{state}-{mobile|desktop}`
>   (e.g. `P1-login-error-mobile`).
> - A text style scale (one open-source font family with full Polish diacritics: ą ć ę ł ń ó ś ź ż, available on
>   Google Fonts or self-hostable).
> - **Components with variants and states** (default, hover, focus-visible, active, disabled, loading, error), shown on
>   a component overview page: buttons, inputs, textarea, select, date picker, checkbox, toggle, radio, tabs, chips/badges,
>   cards, modal/bottom sheet (bottom sheet on mobile, modal on desktop), toast, banner, empty state, skeleton
>   loaders, avatar, menu/dropdown, icon set (one consistent open-source set).
> - For every screen: **empty, loading, error and success states** where they apply.
> - Visible **focus states** for keyboard users, and form labels always visible (no placeholder-only labels).
>
> **The most important product rule — "spoiler protection":** the person who owns an item must never be able to
> tell from the app that it's reserved, by whom, or that anyone is discussing it. Concretely:
> - Screens the **owner** sees (their items, their lists, their own public link) must contain **no reservation
>   affordance at all**: no "reserved" badge, no greyed-out cards, no counters, no icons, no different ordering, no
>   empty space where a badge would go. Owner item cards and giver item cards are **two different components**.
> - Screens a **giver** sees (someone else's shared list) show each item as *available*, *reserved* or *reserved by you*.
>   They never show *who* reserved it.
>
> Keep everything simple and obvious: one primary action per screen, short labels, generous spacing.

---

## Package 1 — Foundations, app shell, accounts and emails (needed for Phase 1)

> Using the Wishka brief, design:
> 1. **Foundations page:** color/typography/spacing/radius/shadow variables, icon set, logo + app icon
>    (incl. maskable and monochrome variants), and the full component library listed in the brief.
> 2. **App shell** (mobile + desktop): desktop left sidebar (logo, nav: *Moje rzeczy* (My items), *Moje listy* (My lists),
>    *Kręgi* (Circles), *Udostępnione mi* (Shared with me), *Moje rezerwacje* (My reservations), bottom: profile),
>    mobile bottom navigation with 4–5 destinations (propose which go into an overflow/"More"), a top bar with
>    **notification bell + unread badge** and avatar menu, a global **toast** area (success/error/info), and a
>    persistent dismissible **banner** "Potwierdź swój adres e-mail" with a "Wyślij ponownie" (resend) action.
> 3. **Auth screens:** Register (name, email, password with strength hint, consent to the privacy policy),
>    Log in (email/password + "Kontynuuj z Google" button following Google's branding guidelines, "Nie pamiętasz hasła?"),
>    Verify email (states: *check your inbox*, *verified*, *link expired → resend*),
>    Forgot password (always shows the same confirmation message), Reset password (new password + confirm, expired-link state).
> 4. **Profile:** display name, email (read-only), language switch (Polski / English), sign-in methods
>    (password: set / change / remove; Google: linked / not linked). Show that the **last remaining sign-in method can't
>    be removed**. Entry point to "Usuń konto" (delete account, designed in Package 8).
> 5. **Email templates** (600 px wide, mobile-friendly, works without images, Polish copy): one base layout (logo,
>    content, single primary button, plain-text fallback link, footer with "why you got this email") and variants:
>    email verification, password reset. More variants are listed in later packages and should reuse this layout.
>
> Note: a user who hasn't verified their email can create **at most 5 items** and can't share, invite or reserve.
> Design the friendly message shown when they hit one of those limits.

---

## Package 2 — Item pool and adding items (needed for Phase 2)

> Using the Wishka brief and components, design the **"Moje rzeczy" (My items)** area. These are the owner's own screens,
> so **no reservation indicators anywhere**.
> 1. **Item pool:** card grid on desktop, list/cards on mobile. Each card: photo (or placeholder), name, price + currency,
>    shop domain, priority badge (*Muszę mieć* / Must have, *Fajnie byłoby mieć* / Nice to have), which lists it's on.
>    Filters/tabs: *Aktywne* (Active) / *Archiwum* (Archive). Empty state that teaches "paste a link to add your first wish".
> 2. **Add item flow:** (a) paste URL field + on mobile a big **"Wklej link ze schowka"** (Paste link from clipboard)
>    button on the home screen; (b) loading state while we fetch the product (may take up to ~5 s);
>    (c) **preview + edit form** pre-filled with name, price, currency, photo, description, with everything editable;
>    (d) states: *all data found*, *partial data found* ("fill in the rest"), *this shop doesn't allow automatic import, please fill in
>    manually* (some big shops block us), *link not allowed / invalid*; (e) **manual entry** without a link;
>    (f) photo: use the fetched one, upload your own, or none. Fields: name*, link, price, currency (default PLN),
>    priority, description, private note (only the owner sees it), "add to lists" multi-select.
> 3. **Item detail / edit** screen and the **status menu**: *Kupiłem/am sam/a* (bought it myself), *Dostałem/am w prezencie*
>    (received as a gift), *Usuń* (delete, with confirmation). Status changes move the item to the Archive.
> 4. **Archive:** items with a status label (bought myself / received / deleted) and a **Restore** action.
> 5. Android "share to Wishka" landing: the add-item flow opened with a link already filled in.

---

## Package 3 — Wishlists (needed for Phase 3)

> Design the owner's **"Moje listy" (My lists)**. These are owner screens, so **no reservation indicators**.
> 1. **Lists overview:** cards with list name, occasion icon + label, date (and "in 23 days"), item count, sharing
>    status (shared with which circles / has a public link / private).
> 2. **Create/edit list:** name*, occasion (*Urodziny, Imieniny, Boże Narodzenie, Ślub, Narodziny dziecka, Inna*) with
>    an icon each, optional custom label, optional date.
> 3. **List detail (owner view):** items in a chosen order with drag-to-reorder (plus an accessible alternative:
>    move up/down), "add from my items" picker (searchable, multi-select, shows items already on the list),
>    "new item" shortcut, remove from list (the item stays in My items).
> 4. **Share panel** (bottom sheet on mobile): public link section (*create link*, copy, **regenerate** with a warning
>    that the old link stops working, **revoke**) and a placeholder area for "share with circles" (Package 5).

---

## Package 4 — Circles and invitations (needed for Phase 4)

> Design **"Kręgi" (Circles)**. A circle is the owner's private audience (e.g. "Rodzina", "Przyjaciele"),
> **max 5 per person**.
> 1. **My circles:** list of circles with member count and a visible "3 / 5 circles" indicator. The disabled create state
>    explains the limit.
> 2. **Circle detail:** members (avatar, display name), pending invitations with *resend* / *revoke* and expiry
>    ("wygasa za 5 dni"). An accepted invitation can show "zaproszono: tata@onet.pl, dołączył jako: Tadeusz (tata.firma@gmail.com)",
>    because people may join with a different email than the one invited. Remove member (confirmation; the member isn't notified).
>    Rename / delete circle.
> 3. **Invite modal:** email field (one or several), short explanation of what the invitee will see.
> 4. **"Kręgi, do których należę"** (circles I'm in): circle name, owner, other members, **leave circle** action.
> 5. **Invitation landing page** (opened from the email): "*Anna* zaprasza Cię do kręgu *Rodzina*" with states:
>    logged out (log in / create account), logged in (accept / decline), expired, already used, it's your own circle.
> 6. **Email variants** (base layout from Package 1): invitation received, invitation accepted (to the owner).

---

## Package 5 — Sharing and the giver's view (needed for Phase 5)

> 1. **Share with circles** (completing the share panel from Package 3): toggle per circle.
> 2. **"Udostępnione mi" (Shared with me):** lists from other people, grouped by person, with occasion and date.
> 3. **Giver list view** (someone else's list): header with the owner's name, occasion and date. Item cards **for givers**
>    (a separate component from the owner card) with three states: **available** (primary "Rezerwuję" button),
>    **reserved** (by someone else: show only "Zarezerwowane" and a secondary "Zapytaj rezerwującego" (ask the reserver)
>    action; **never the reserver's name**), **reserved by you** ("Zarezerwowane przez Ciebie" + due date + manage).
>    Item detail for givers: full description, open in shop link, price.
> 4. **Public list view** (anyone with the link, not logged in): same giver layout, but without "ask the reserver".
>    Language switch PL/EN, a light "Załóż konto w Wishka" invitation, "reserve as guest" entry (Package 6).
> 5. **Owner opens their own public link while logged in:** they see their **owner view** with a calm info banner
>    "Tak widzą Twoją listę inni — informacje o rezerwacjach są przed Tobą ukryte" (no reservation info appears).
> 6. **Interstitial on public links** (shown once per device, remembered after dismissal): "Jeśli to Twoja lista, zaloguj się, żeby nie
>    zepsuć sobie niespodzianki" with *Log in* / *Continue*.

---

## Package 6 — Reservations (needed for Phase 6)

> 1. **Reserve (logged in):** bottom sheet/modal with a **due date** ("do kiedy kupisz prezent?"). Default = the list's
>    occasion date if it has one, otherwise 7 days, and the user can change it. Short explanation: "If you don't mark it as bought
>    or extend it by this date, the reservation will be released."
> 2. **Reserve as guest** (from the public link): name, email, due date, Cloudflare Turnstile widget area,
>    consent line, then a "**Sprawdź skrzynkę**" (check your inbox) state: the reservation is held for 24 h until confirmed.
> 3. **Guest landing pages** (from email links): reservation **confirmed**, **cancelled**, link expired, and a
>    **manage reservation** page (extend due date, mark as bought, cancel).
> 4. **Ask the reserver** modal: optional short message + a clear notice "Twoje imię i e-mail zostaną przekazane
>    osobie, która zarezerwowała ten prezent" (your name and email will be shared with the reserver). Success state.
> 5. **"Moje rezerwacje" (My reservations):** grouped by person/occasion. Each: item, list, due date, status
>    (*active*, *bought*, *ended*), actions **Kupione** (bought), **Przedłuż** (extend, date picker), **Anuluj** (cancel).
>    Highlighted change states: "item changed by the owner (price: 199 → 249 zł)", "owner already has it",
>    "item removed", "reservation expired". Due-soon highlight (1 day left).
> 6. **Email variants:** guest confirmation request (confirm + cancel buttons), reservation confirmed, cancelled,
>    expired, due soon (tomorrow) with *Bought / Extend / Release* buttons, "someone asks about your reservation"
>    (shows requester's name, message, "reply to this email" hint), item changed / owner already has it / item removed.

---

## Package 7 — Notifications, push and PWA (needed for Phase 7)

> 1. **Notification center:** bell dropdown on desktop, full screen on mobile. Unread/read styles, grouped by day,
>    "mark all as read", each notification deep-links to its screen. Empty state.
> 2. **Notification preferences:** a matrix of event types × channels (*E-mail*, *W aplikacji* (in-app), *Push*) with
>    toggles. Design a usable **mobile** version (e.g. per-event expandable rows). Event types: circle invitation received,
>    invitation accepted, reservation confirmed / cancelled / expired / due soon, someone asks about your reservation,
>    reserved item changed / bought by owner / removed, new items on a list shared with you.
>    Account emails (verification, password reset) aren't configurable, so show that as a note.
> 3. **Push opt-in:** a soft pre-permission card ("Chcesz dostawać powiadomienia?") shown before the browser prompt.
>    On **iPhone** (Safari, not installed) it's an **"Add Wishka to your home screen" guide** with 3 illustrated steps
>    (Share icon → "Do ekranu początkowego" → open from the icon), because iOS only allows push for installed apps.
> 4. **Android install** prompt (custom "Zainstaluj aplikację" card).
> 5. **PWA assets:** app icon set (192, 512, maskable, monochrome), splash/theme colors, push notification icon + badge
>    (monochrome, 96 px).
> 6. Review and polish of all earlier screens for desktop/mobile consistency.

---

## Package 8 — Privacy and account (needed for Phase 8)

> 1. **Privacy policy page** (PL/EN): a readable long-form text layout with a table of contents. Use placeholder text clearly marked
>    as placeholder, since the real text will be supplied.
> 2. **Delete account flow:** explanation of what is deleted (items, lists, circles, notifications) and what happens to
>    reservations you made (kept anonymously so nobody buys a duplicate), re-enter password or re-login with Google,
>    type-to-confirm, final "account deleted" screen.
> 3. **Email variant:** account deleted confirmation.
> 4. **Error pages:** 404, 500 / offline (PWA offline shell), "link no longer valid".
