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

## Deployment

Docker Compose is for local development. `k8s/base` holds Kustomize manifests
(ConfigMaps, Secrets as placeholders, Deployments, Services, and a PVC for
catalog-service's pizza images) with environment-specific overlays under
`k8s/overlays/{local,hetzner,production}`. GitHub Actions CI is path-filtered
per service, so a change to one microservice doesn't rebuild the whole fleet.

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
- **JWT signing secret** defaults to a development placeholder — rotate it
  (`JWT_SIGNING_SECRET`) before any real deployment.
