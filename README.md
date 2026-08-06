# Pizza Configurator

A microservices platform for configuring, validating, and tracking pizza orders —
customer ordering, a live kitchen display, and an admin CMS, all behind a single
API gateway.

## Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Identity & auth](#identity--auth)
- [Key workflows](#key-workflows)
- [Data & messaging](#data--messaging)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [MVP & feature timeline](#mvp--feature-timeline)
- [Deployment](#deployment)
- [Known limitations](#known-limitations)

## Overview

A customer builds a pizza from a base recipe (size, dough, default ingredients,
allowed extras), optionally adds a free-text comment ("no basil, extra crispy"),
and places an order. A rule engine validates every configuration automatically;
anything it can't confidently resolve is routed to kitchen staff for a manual
review, whose resolution the customer then confirms. Admins manage the pizza
menu (including photos), ingredient/size/dough prices, and a handful of global
rule thresholds — all at runtime, without a redeploy.

Three account types share one login flow: **customers** (self-registration),
**staff** (kitchen display access), and **admin** (menu/price/rule/account
management) — staff and admin accounts are created by an existing admin, not
self-registered.

## Architecture

```
Browser ──▶ gateway (:8080) ──▶ catalog-service (:8081)
                             ├─▶ pricing-service (:8083)
                             ├─▶ rule-service    (:8082)
                             ├─▶ order-service    (:8085)
                             ├─▶ ai-parser-service (:8084)
                             ├─▶ kitchen-service   (:8086, SSE)
                             └─▶ admin-service     (:8087)
```

The gateway is the only thing a browser ever talks to. It verifies the caller's
JWT, stamps a customer/staff identity onto the request (`X-Customer-Id` /
`X-Staff-Id`), then routes to whichever service owns that data. Backend
services also call each other directly for cross-cutting logic — e.g.
rule-service reads a pizza's allowed extras live from catalog-service, so a
brand-new pizza gets correct validation with zero rule-service changes.

No service re-verifies the JWT itself; every internal call trusts the gateway
boundary. There is no shared database — each service that needs one owns its
own Postgres schema, and nothing is joined across service boundaries at the
SQL level (see [Data & messaging](#data--messaging)).

## Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| `gateway` | 8080 | — | Sole JWT issuer/verifier, routing, CORS, orchestrates the free-text validate call |
| `catalog-service` | 8081 | `catalog_db` | Pizzas, default ingredients, allowed extras, images (admin CRUD + public read) |
| `pricing-service` | 8083 | `price_db` | Price items for sizes/dough/ingredients, full CRUD |
| `rule-service` | 8082 | `rules_db` | Validation engine — 7 rules (3 DB-backed thresholds, 4 hardcoded) |
| `order-service` | 8085 | `order_db` | Orders, pending reviews, customer accounts, staff/admin accounts |
| `ai-parser-service` | 8084 | — | Parses free-text comments into structured change proposals (DeepSeek API) |
| `kitchen-service` | 8086 | — (Redis) | Kitchen display board, Redis-backed for horizontal scaling, SSE to staff |
| `notification-service` | 8088 | — | Consumes order events, sends the pickup-ready notification |
| `admin-service` | 8087 | `admin_db` (audit log only) | Authenticated, audited proxy for price/rule/staff-account changes |
| `common-contracts` | — | — | Shared Kafka event DTOs only — the one place services intentionally share code |

## Identity & auth

One JWT (HMAC-signed, minted only by the gateway), three scopes:

- **`customer`** — real accounts (email + bcrypt password in order-service).
  Login is required before reaching the ordering flow at all.
- **`staff`** — kitchen display access. Accounts are created by an admin, not
  self-registered.
- **`admin`** — menu/price/rule/staff management. Same admin-created model as
  staff.

There is no public "register as staff/admin" endpoint — an admin creates those
accounts from the admin panel, which generates a one-time temporary password
(shown once) and forces a password change on first login.

A fresh deployment seeds exactly one admin account so it isn't locked out:

```
email:    admin@pizzaconfig.local
password: ChangeMe123!
```

You'll be forced to set a new password on first login.

## Key workflows

**Ordering.** Customer configures a pizza → optionally adds a free-text
comment → the gateway orchestrates ai-parser-service (parse) then
rule-service (validate) → `APPROVED` places the order directly;
`INVALID` shows recommendations (other pizzas that would work); an
unparseable/ambiguous comment creates a **pending review** instead.

**Manual review.** A pending review shows up on the kitchen display for staff,
who resolve it into a concrete configuration — validated by rule-service
before the customer ever sees it — and the customer confirms that resolution
to place the order. No second validation happens on the customer side, since
staff's resolution was already checked.

**Kitchen board.** Staff advance a ticket through `PLACED → PROCESSING →
READY_FOR_COLLECTION`. Only the transition into `READY_FOR_COLLECTION`
publishes an event back to order-service and notification-service — that's
the one status change anything downstream needs to react to.

**Admin CMS.** Add/edit/delete pizzas (with ingredients, allowed extras, and a
photo), add/edit/delete price items, edit the 3 global rule thresholds, and
manage staff/admin accounts — all live, no redeploy.

## Data & messaging

**PostgreSQL** — one database per service that needs one (`catalog_db`,
`price_db`, `rules_db`, `order_db`, `admin_db`). No cross-database foreign
keys; where one service's row references another service's entity (e.g. an
order item's `base_pizza_id`), it's a plain string with no referential
integrity — the "join" happens in application code after a REST call, not in
SQL.

**Kafka** (KRaft mode) — three topics, all fire-and-forget:

| Topic | Producer | Consumer(s) | Purpose |
|---|---|---|---|
| `order-submitted` | order-service | notification-service | Order confirmation |
| `order-status-changed` | kitchen-service | order-service, notification-service | Syncs status on the ready-for-collection transition only |
| `rule-config-updated` | rule-service | rule-service (other replicas) | Cache invalidation when an admin edits a threshold |

**Redis** — used only by kitchen-service, to share board state and fan out SSE
updates across its own replicas.

## Getting started

Prerequisites: Docker and Docker Compose.

```bash
# ai-parser-service needs an OpenAI-compatible API key (OpenAI or DeepSeek)
export OPENAI_API_KEY="sk-..."
export OPENAI_BASE_URL="https://api.deepseek.com"   # omit for real OpenAI
export OPENAI_MODEL="deepseek-chat"                  # omit for real OpenAI

docker compose up -d --build
```

Then:
- Frontend: http://localhost:3001
- Gateway: http://localhost:8080
- Log in as `admin@pizzaconfig.local` / `ChangeMe123!` (forced password
  change on first login) to reach the admin panel, or register a new
  customer account from the ordering page.

Building without Docker requires Java 21 and Maven (or use the included
wrapper: `./mvnw` / `mvnw.cmd`).

## Project structure

```
common-contracts/     Shared Kafka event DTOs
catalog-service/      Pizzas, ingredients, extras, images
pricing-service/      Price items
rule-service/         Validation rules
order-service/        Orders, pending reviews, customers, staff accounts
gateway/              API gateway, JWT auth, routing
ai-parser-service/    Free-text comment parsing
kitchen-service/      Kitchen display board
notification-service/ Order-ready notifications
admin-service/        Audited price/rule/staff-account proxy
frontend/             React SPA (customer, kitchen, admin)
k8s/                  Kustomize base + overlays (local, hetzner, production)
docker-compose.yml    Local orchestration for all services + Postgres/Kafka/Redis
data/CLAUDE.md        Original architecture spec this project was built from
```

## MVP & feature timeline

The MVP scope (data/CLAUDE.md) was: catalog + pricing + rule + order services,
a deterministic rule engine, and free-text parsing via an AI layer with zero
business-rule authority of its own. Each service's own Flyway migration
history is the most reliable record of how it grew past that baseline:

- **catalog-service**: `V1` core pizza/ingredient/extras schema → `V2` split
  out per-pizza default ingredients as their own structure → `V3` added admin
  photo upload (pizza images).
- **order-service**: `V1` core orders schema → `V2` added a customer phone
  number → `V3` added **pending reviews** (the manual-review workflow for
  free-text comments the rule engine can't confidently resolve) → `V4`
  replaced anonymous ordering with real **customer accounts** (email + bcrypt
  password) → `V5` added real **staff/admin accounts**, replacing the
  gateway's original hardcoded demo credentials with order-service-backed
  login and an admin-managed account model.
- **admin-service**: started as the canonical owner of prices/rules, then was
  refactored into a thin **audited proxy** in front of pricing-service and
  rule-service (each keeps owning its own data) — `admin_db` now stores only
  the audit log of who changed what, when.
- **pricing-service** / **rule-service**: single-migration schemas; grew in
  behavior (rule-service's 7 validation rules, admin-editable thresholds)
  without further schema changes.

**Deployment** was the most recent addition, built out incrementally against
a real shared server rather than a clean box — see below for what that
actually involved and what broke along the way.

## Deployment

Docker Compose is for local development (see [Getting started](#getting-started)).

For a real cluster, `k8s/base` holds Kustomize manifests (ConfigMaps,
Deployments, Services, StatefulSets for Postgres/Kafka/Redis, and a PVC for
catalog-service's pizza images) with environment-specific overlays under
`k8s/overlays/{local,hetzner,production}`. GitHub Actions CI is path-filtered
per service (plus one for `frontend/`), so a change to one microservice
doesn't rebuild the whole fleet; each workflow builds, tests, pushes to GHCR,
then does a rolling `kubectl set image` update.

### Live deployment (hetzner overlay)

Running at **https://pizza.204.168.156.164.sslip.io** — a single-node k3s
cluster on a shared Hetzner box that also hosts unrelated projects, so several
choices here are specifically about coexisting safely rather than what a
dedicated box would do:

- **Ingress**: host nginx keeps ports 80/443 (fronting the other projects
  already on that box); Traefik (k3s's ingress controller) runs behind it on
  fixed NodePorts, reached via one extra nginx server block + a Let's Encrypt
  cert for the `sslip.io` subdomain — no DNS setup needed, `sslip.io` resolves
  `<name>.<ip>.sslip.io` straight to the embedded IP.
- **Ingress routing**: `/v1/**` and `/actuator/**` go to `gateway`; everything
  else goes to `frontend`. The frontend image is built with
  `VITE_API_BASE_URL=""` so it calls the API same-origin through that same
  ingress domain (see `frontend/Dockerfile`).
- **CORS**: `gateway`'s `CORS_ALLOWED_ORIGINS` must include whatever origin
  the frontend is actually served from — browsers send an `Origin` header on
  same-origin POSTs too, not just cross-origin ones, so this isn't optional
  even when frontend and API share a domain. Set per-overlay (see
  `k8s/overlays/hetzner/kustomization.yaml`); base only defaults to local-dev
  ports.
- **Sizing**: CPU limits are generous (CPU is compressible, doesn't need to
  fit within real headroom) but memory requests are trimmed hard and JVM
  heaps capped via `JAVA_TOOL_OPTIONS` — this box's real free memory is far
  below what Kubernetes reports as "allocatable" once the other projects'
  containers are accounted for. A 4GB swap file is a deliberate safety net
  against transient pressure, not routine capacity.
- **Credentials**: `Secret` objects are **not** part of the kustomize-managed
  resources — they used to be checked-in placeholders, but that meant every
  `kubectl apply -k` silently reverted real rotated credentials back to
  `changeme`, which broke this live deployment twice before being fixed.
  Create them once per cluster before the first apply:

  ```bash
  kubectl create secret generic postgres-credentials \
    --from-literal=POSTGRES_PASSWORD=<...>
  kubectl create secret generic gateway-jwt-secret \
    --from-literal=JWT_SIGNING_SECRET=<...>
  kubectl create secret generic ai-parser-service-openai-credentials \
    --from-literal=OPENAI_API_KEY=<...>
  # for each of: catalog-service, pricing-service, rule-service, order-service, admin-service
  kubectl create secret generic <service>-db-credentials \
    --from-literal=DB_USERNAME=postgres --from-literal=DB_PASSWORD=<...>
  ```

  `kubectl apply -k` is then safe to re-run indefinitely without touching them.
- **CI's cluster access** (`KUBE_CONFIG_B64` in GitHub Actions) is a scoped
  `ServiceAccount` + `Role`, not cluster-admin — limited to `get/list/patch`
  on `Deployments` inside this app's own namespace only. A leaked token can
  restart this app's pods; it can't touch the cluster, the node, or any other
  project sharing the box. The Kubernetes API server has to be reachable from
  GitHub's hosted runners, so this scoping is the actual security boundary,
  not network restriction.
- **Bootstrapping a brand-new environment**: create the namespace and the
  Secrets above, `kubectl apply -k k8s/overlays/<env>` once by hand (CI's
  `kubectl set image` only works on a Deployment that already exists), then
  set `KUBE_CONFIG_B64` — from then on, pushes to `main` deploy automatically.

## Known limitations

- **Rule engine scope.** 4 of rule-service's 7 rules are hardcoded Java, not
  admin-configurable (the 3 global thresholds are). Making all of them fully
  dynamic would need a rule-authoring engine — a deliberately separate,
  larger effort than the CRUD work done so far.
- **Image storage.** Pizza photos live on a local disk volume in
  catalog-service — fine at one replica, but a multi-replica deployment would
  need shared object storage instead.
- **No automated tests.** Everything has been verified manually via curl and
  browser testing; there's no regression safety net yet for future changes.
- **JWT signing secret and all other credentials** default to development
  placeholders in `docker-compose.yml` and `k8s/base`'s ConfigMaps — see
  [Deployment](#deployment) for how real clusters supply real values without
  ever committing them.
