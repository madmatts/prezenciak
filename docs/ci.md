# CI: TeamCity (configured in the UI, D-36)

The pipeline is configured **by hand in the TeamCity UI** (no Kotlin DSL in the repo). This page is the source of truth
for what to enter. When a phase changes the pipeline, this page changes in the same commit.

Environment (from `~/Dev/team-city`): TeamCity server `http://localhost:8111`, agent `teamcity-agent-1` (JDK 25 as
`env.JDK_25`, Node 26, Google Chrome, host Docker through `/var/run/docker.sock`, networks `teamcity-net` +
`dev_network`), Nexus `http://nexus:8081` from the agent.

---

## 1. One-time setup

### 1.1 Nexus
1. **Docker (hosted)** repository `docker-private`, HTTP connector port **8082**, deployment policy "Allow redeploy".
   (Port 8082 is already published by the Nexus container.)
2. Optional, for faster npm installs: **npm (proxy)** `npm-proxy` → `https://registry.npmjs.org`, and **npm (group)**
   `npm-group` containing it. Until then, CI uses the public npm registry.

### 1.2 Maven settings (TeamCity → project → Maven Settings)
Extend your uploaded `Nexus Credentials` settings.xml with a mirror so every dependency goes through Nexus:

```xml
<mirrors>
  <mirror>
    <id>nexus</id>
    <mirrorOf>*</mirrorOf>
    <url>http://nexus:8081/repository/maven-public/</url>
  </mirror>
</mirrors>
<servers>
  <!-- add next to the existing nexus-releases / nexus-snapshots entries -->
  <server>
    <id>nexus</id>
    <username>…</username>
    <password>…</password>
  </server>
</servers>
```

### 1.3 VCS root (project level)
- URL `git@github.com:madmatts/prezenciak.git`, authentication: uploaded SSH key (read-only deploy key is enough).
- Default branch `refs/heads/main`, branch specification `+:refs/heads/*`.

### 1.4 Project parameters (project → Parameters)

| Name | Kind | Value |
|---|---|---|
| `env.CI` | Environment variable | `true` |
| `env.NEXUS_USER` | Environment variable | Nexus user allowed to push to `docker-private` |
| `env.NEXUS_PASSWORD` | Environment variable, **Password** | its password |
| `env.GHCR_USER` | Environment variable | `madmatts` |
| `env.GHCR_TOKEN` | Environment variable, **Password** | classic PAT with `write:packages` |
| `registry.nexus` | Configuration parameter | `localhost:8082` |
| `registry.ghcr` | Configuration parameter | `ghcr.io/madmatts` |

> **Why `localhost:8082` and not `nexus:8082`:** `docker build/push` run by the agent are executed by the **host**
> Docker daemon (through the socket). The host can't resolve `nexus`, but it reaches the published port on
> `localhost`, and Docker trusts `localhost` registries over plain HTTP by default.

Build number format (both builds): `0.1.%build.counter%`.

---

## 2. Build configuration `Backend`

**General:** build number format `0.1.%build.counter%`. Artifact paths: none needed.

**Steps:**

1. **Maven: verify** (runner *Maven*)
   - POM file: `backend/pom.xml`, goals: `-B clean verify`
   - JDK: `%env.JDK_25%`, user settings: `Nexus Credentials`
   - Runs unit tests, integration tests (Testcontainers, PostgreSQL 17), ArchUnit, i18n parity, Spotless check.
     Test reports are picked up automatically.
2. **Docker image → Nexus** (runner *Command Line*, working directory `backend`)
   ```bash
   set -e
   IMAGE=%registry.nexus%/wishka-backend
   docker build -t "$IMAGE:%build.number%" -t "$IMAGE:latest" .
   echo "$NEXUS_PASSWORD" | docker login %registry.nexus% -u "$NEXUS_USER" --password-stdin
   docker push "$IMAGE:%build.number%"
   docker push "$IMAGE:latest"
   ```
3. **Docker image → GHCR** (runner *Command Line*, working directory `backend`)
   - **Step condition:** `teamcity.build.branch` *equals* `main`
   ```bash
   set -e
   SRC=%registry.nexus%/wishka-backend:%build.number%
   DST=%registry.ghcr%/wishka-backend
   echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
   docker tag "$SRC" "$DST:%build.number%"
   docker tag "$SRC" "$DST:latest"
   docker push "$DST:%build.number%"
   docker push "$DST:latest"
   ```

**Triggers:** VCS trigger, branch filter `+:*`, trigger rules `+:backend/**` and `+:docs/ci.md`.

**Testcontainers on the containerized agent:** the agent drives the host's Docker, so test containers run as *siblings*
of the agent. Testcontainers detects that it runs inside a container and connects through the agent's default
gateway, so no configuration is needed. If the first build can't reach the PostgreSQL container, add the project
parameter `env.TESTCONTAINERS_HOST_OVERRIDE` with the gateway of the agent's network:
`docker network inspect team-city_teamcity-net -f '{{(index .IPAM.Config 0).Gateway}}'`.

---

## 3. Build configuration `Frontend`

**General:** build number format `0.1.%build.counter%`. Working directory for every step: `frontend`.

**Build features:** *XML report processing*, type **Ant JUnit**, paths `frontend/test-results/*.xml`.

**Steps** (all *Command Line*, working directory `frontend`):

1. **Install** `node --version && npm ci`
2. **Lint** `npm run lint && npm run format:check`
3. **Type-check** `npm run type-check`
4. **Unit tests** `npm run test:unit:ci`
5. **Build** `npm run build-only`
6. **E2E smoke** (Playwright, serves the build with `vite preview`)
   ```bash
   set -e
   npx playwright install chromium
   npm run test:e2e
   ```
7. **Docker image → Nexus**
   ```bash
   set -e
   IMAGE=%registry.nexus%/wishka-frontend
   docker build -t "$IMAGE:%build.number%" -t "$IMAGE:latest" .
   echo "$NEXUS_PASSWORD" | docker login %registry.nexus% -u "$NEXUS_USER" --password-stdin
   docker push "$IMAGE:%build.number%"
   docker push "$IMAGE:latest"
   ```
8. **Docker image → GHCR** (step condition: `teamcity.build.branch` *equals* `main`)
   ```bash
   set -e
   SRC=%registry.nexus%/wishka-frontend:%build.number%
   DST=%registry.ghcr%/wishka-frontend
   echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
   docker tag "$SRC" "$DST:%build.number%"
   docker tag "$SRC" "$DST:latest"
   docker push "$DST:%build.number%"
   docker push "$DST:latest"
   ```

**Triggers:** VCS trigger, branch filter `+:*`, trigger rules `+:frontend/**`, `+:backend/src/main/java/pl/wishka/common/error/ErrorCode.java`
(the frontend test checks every backend error code is translated) and `+:docs/ci.md`.

---

## 4. Verify the pipeline
- Both builds green on `feature/phase-0`; images `wishka-backend:0.1.N` / `wishka-frontend:0.1.N` visible in Nexus
  (`docker-private`).
- After merging to `main`: the same tags plus `latest` in GHCR (`https://github.com/madmatts?tab=packages`), private.
