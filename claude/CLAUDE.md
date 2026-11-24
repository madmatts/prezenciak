# Project Overview

Aplikacja fullstackowa oparta na architekturze klient-serwer:
- **Backend:** Java 25, Spring Boot 4.x, Spring Data JPA, Spring Security, PostgreSQL, Flyway, Maven.
- **Frontend:** Vue 3 (Composition API, `<script setup>`), TypeScript, Vite, Pinia, Vue Router, Tailwind CSS.

## Repository Structure

```text
/
├── backend/                  # Aplikacja Spring Boot
│   ├── src/main/java/...     # Kod źródłowy (domain, application, infrastructure, web)
│   ├── src/main/resources/   # Konfiguracja (application.yml) i migracje DB
│   └── src/test/java/...     # Testy jednostkowe i integracyjne (Testcontainers)
├── frontend/                 # Aplikacja Vue 3 + TypeScript
│   ├── src/api/              # Klienty HTTP (Axios/Fetch) i wygenerowane typy OpenAPI
│   ├── src/components/       # Komponenty wielokrotnego użytku
│   ├── src/composables/      # Logika biznesowa wielokrotnego użytku (use*.ts)
│   ├── src/stores/           # Stan globalny (Pinia)
│   ├── src/views/            # Widoki powiązane z routingiem
│   └── src/types/            # Interfejsy i typy TypeScript
└── docker-compose.yml        # Lokalna baza danych i usługi pomocnicze
```

---

## Build, Test & Run Commands

### Backend (`/backend`)
- **Uruchomienie lokalne:** `./mvnw spring-boot:run` *(lub `./gradlew bootRun`)*
- **Budowanie projektu:** `./mvnw clean package -DskipTests`
- **Testy jednostkowe:** `./mvnw test`
- **Testy integracyjne:** `./mvnw verify`
- **Formatowanie / Linter:** `./mvnw spotless:apply` *(jeśli skonfigurowane)*

### Frontend (`/frontend`)
- **Instalacja zależności:** `npm install`
- **Serwer deweloperski:** `npm run dev`
- **Budowanie produkcyjne:** `npm run build`
- **Type-checking:** `npm run type-check` *(lub `vue-tsc --noEmit`)*
- **Linter & Formatter:** `npm run lint` / `npm run format`
- **Testy jednostkowe (Vitest):** `npm run test:unit`

---

## Backend Guidelines (Spring Boot)

1. **Architektura i pakiety:**
   - Stosuj podział warstwowy lub per-feature (`controller` -> `service` -> `repository`).
   - Nigdy nie zwracaj encji JPA (`@Entity`) bezpośrednio w kontrolerach REST. Zawsze mapuj je na obiekty DTO (najlepiej jako Java `record`).
2. **Kontrolery i walidacja:**
   - Używaj `@Valid` na obiektach wejściowych w kontrolerach.
   - Obsługuj wyjątki globalnie za pomocą `@RestControllerAdvice` zwracając standardowy format błędu (np. RFC 7807 `ProblemDetail`).
3. **Baza danych i transakcje:**
   - Adnotację `@Transactional` umieszczaj na poziomie metod w warstwie serwisowej (dla operacji tylko do odczytu używaj `@Transactional(readOnly = true)`).
   - Wszelkie zmiany schematu bazy danych wprowadzaj wyłącznie przez skrypty migracji (Flyway/Liquibase), nigdy przez `ddl-auto: update`.
4. **Dobre praktyki Java:**
   - Preferuj wstrzykiwanie zależności przez konstruktor (`@RequiredArgsConstructor` z Lomboka).
   - Unikaj logiki biznesowej w kontrolerach i repozytoriach.

---

## Frontend Guidelines (Vue 3 + TypeScript)

1. **Komponenty i składnia:**
   - Zawsze używaj **Composition API** oraz składni `<script setup lang="ts">`.
   - Zachowaj kolejność sekcji w plikach `.vue`: `<script setup>`, `<template>`, `<style scoped>`.
   - Definiuj `props` i `emits` za pomocą czystego TypeScriptu (`defineProps<{ ... }>()`).
2. **Zarządzanie stanem i logiką:**
   - Wydzielaj powtarzalną logikę i wywołania API do **Composables** (`src/composables/useFeature.ts`).
   - Pinia (`src/stores/`) służy wyłącznie do stanu współdzielonego między wieloma widokami (np. sesja użytkownika, powiadomienia). Lokalny stan trzymaj w komponentach lub composables.
3. **TypeScript:**
   - Unikaj typu `any`. Używaj `unknown` lub dokładnych interfejsów.
   - Typy odpowiedzi z API trzymaj w `src/types/` lub generuj ze specyfikacji OpenAPI/Swagger backendu.
4. **Szablony i style:**
   - Utrzymuj szablony `<template>` maksymalnie proste – złożone warunki przenoś do `computed`.

---

## Fullstack Contract & Workflow

- **Komunikacja API:** Endpointy backendowe wystawiają dokumentację OpenAPI pod adresem `/v3/api-docs`. Przy zmianie kontraktów DTO w Springu należy zaktualizować odpowiadające im typy TypeScript na frontendzie.
- **Obsługa błędów:** Frontend oczekuje ustandaryzowanych odpowiedzi błędów HTTP z backendu i wyświetla je użytkownikowi za pomocą globalnego systemu powiadomień (toastów).
- **Weryfikacja przed zakończeniem zadania:**
  - Po zmianach w backendzie uruchom kompilację i testy (`./mvnw test`).
  - Po zmianach we frontendzie uruchom sprawdzanie typów (`npm run type-check`) oraz linter (`npm run lint`).










