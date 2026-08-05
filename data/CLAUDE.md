# CLAUDE.md — Pizza Configurator Microservice Platform

This file gives Claude Code the context needed to build, extend, and operate this project.
Read it fully before writing code. When a decision isn't covered here, prefer the
simplest option that keeps services independently deployable.

## 1. Project Summary

A distributed, decoupled microservice platform for a pizza restaurant chain. Customers
configure pizzas via a UI (buttons) or free-text comments. Free-text comments are parsed
by an AI layer into a structured proposal, which a **deterministic Rule Service**
validates before anything is written to the order. The AI has zero business-rule
knowledge — it only translates language into structured intent.

Reference docs (put these in `/docs` if provided): the original assignment
("Konzeption eines Pizza-Konfigurators") and the extended technical specification.
This CLAUDE.md is the source of truth if they conflict with older drafts.

## 2. Tech Stack (fixed — do not substitute without asking)

| Concern            | Choice                                                        |
|---------------------|----------------------------------------------------------------|
| Language / runtime  | Java 21, Spring Boot 3.x                                       |
| Build tool          | Apache Maven (multi-module reactor build)                      |
| API style           | REST/JSON (synchronous, internal + external) — see §7 for why  |
| Messaging           | Apache Kafka (async, fire-and-forget events)                   |
| Relational storage  | PostgreSQL (one schema/database per service)                   |
| Cache               | Redis (cart/session staging only, pre-checkout)                |
| AI parsing          | OpenAI API, called only by `ai-parser-service`                 |
| Frontend            | React (or Vue) — separate repo/module, not covered in depth here |
| Containerization    | Docker (multi-stage builds, distroless/JRE-slim runtime image) |
| Orchestration       | Kubernetes (manifests in `/k8s`, Kustomize overlays per env)   |
| CI/CD               | GitHub Actions, **one workflow per service**, path-filtered    |
| Observability       | Spring Boot Actuator, Micrometer → Prometheus, Grafana, ELK (or Loki), OpenTelemetry tracing |
| Auth                | JWT / OAuth2 (Spring Security Resource Server)                 |

## 3. Repository Layout

Use a **Maven multi-module monorepo**. Each service is independently buildable,
testable, containerizable, and deployable — the monorepo is for developer convenience
only; it must not create compile-time coupling between services beyond the shared
contracts module.

```
pizza-configurator/
├── pom.xml                      # parent POM (packaging=pom), dependency mgmt only
├── docker-compose.yml           # local dev: all services + postgres + kafka + redis
├── .github/
│   └── workflows/
│       ├── catalog-service.yml
│       ├── rule-service.yml
│       ├── pricing-service.yml
│       ├── ai-parser-service.yml
│       ├── order-service.yml
│       ├── kitchen-service.yml
│       ├── admin-service.yml
│       ├── notification-service.yml
│       └── gateway.yml
├── k8s/
│   ├── base/                    # plain manifests per service (Deployment, Service, ConfigMap)
│   └── overlays/
│       ├── local/                # kind/k3s, low resource requests
│       ├── hetzner/               # k3s on Hetzner Cloud VM(s) — see §13
│       └── production/
├── common-contracts/            # shared DTOs / event schemas ONLY — no business logic
├── gateway/                     # Spring Cloud Gateway
├── catalog-service/
├── rule-service/
├── pricing-service/
├── ai-parser-service/
├── order-service/
├── kitchen-service/
├── admin-service/
├── notification-service/
└── docs/
```

Each `*-service/` module has its own:
```
<service>/
├── pom.xml
├── Dockerfile
├── src/main/java/com/pizzaconfig/<service>/
│   ├── <Service>Application.java
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/                  # entities
│   ├── dto/
│   ├── config/
│   └── client/                  # REST/Kafka clients to other services
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/            # Flyway migrations, Vxxx__description.sql
└── src/test/java/...
```

`common-contracts` should hold only: event payload records (e.g. `OrderSubmittedEvent`,
`OrderStatusChangedEvent`), and OpenAPI-generated client stubs if used. If a change to
`common-contracts` would force two services to redeploy together, that's a smell —
prefer duplicating a small DTO over introducing coupling.

## 4. Services & Responsibilities

| Service | Port (local) | Responsibility | Owns DB |
|---|---|---|---|
| `gateway` | 8080 | Ingress, SSL termination, RBAC, rate limiting. Public `/v1/customer/*`, locked `/v1/admin/*` | — |
| `catalog-service` | 8081 | Standard pizzas, sizes, dough types, ingredient catalog | `catalog_db` |
| `rule-service` | 8082 | Deterministic validation of every configuration change (see §6) | `rules_db` |
| `pricing-service` | 8083 | Ingredient/size/dough prices, surcharges, price calculation | `price_db` |
| `ai-parser-service` | 8084 | Headless NLP: free-text comment → structured mutation proposal. Zero business-rule knowledge | — (stateless) |
| `order-service` | 8085 | Transactional order ledger (ACID), publishes `OrderSubmitted` / `OrderStatusChanged` | `order_db` |
| `kitchen-service` | 8086 | Kitchen board state machine (PLACED → PROCESSING → READY_FOR_COLLECTION), pushes to KDS via SSE/WebSocket | — (reads order events) |
| `admin-service` | 8087 | Price/rule editing endpoints for staff, triggers rule-cache invalidation | `admin_db` |
| `notification-service` | 8088 | Consumes `OrderStatusChanged`, texts pickup token to customer | — |

Frontend talks only to `gateway`. Services never call each other's databases directly —
cross-service reads go through REST calls or, for eventual-consistency cases, Kafka
events.

## 5. Order Lifecycle (implement this exact flow)

1. Customer edits config via buttons **or** free-text comment.
2. Frontend → Gateway → `rule-service` (`POST /v1/rules/validate`) for **every** change,
   button-driven or AI-driven — there is no bypass path for manual clicks.
3. If the change came from free text: Gateway → `ai-parser-service` first, which returns
   a structured proposal; that proposal is what gets validated in step 2.
4. `rule-service` outcome is one of three states — implement all three, not just pass/fail:
   - `APPROVED` → `pricing-service` recalculates total → customer confirms → `order-service`.
   - `INVALID` → `rule-service` calls `pricing-service` + `catalog-service` internally to
     build one or more `recommendations` (alternative valid configurations) → returned to
     customer, no employee involved.
   - `MANUAL_REVIEW` → queued for staff approve/modify/reject before the order can proceed.
     (This exists because free-text requests can be ambiguous even after AI parsing —
     don't collapse this into a binary approve/reject.)
5. Kitchen-only free-text instructions (e.g. "bake extra crispy") **never** go through
   the AI Parser or Rule Service — store them verbatim in `custom_notes` and display
   as-is on the KDS.
6. On order creation, `order-service` publishes `OrderSubmitted` to Kafka.
7. `kitchen-service` consumes it, renders the board. Staff move a ticket to
   `READY_FOR_COLLECTION` → `kitchen-service` publishes `OrderStatusChanged` →
   `notification-service` consumes it and texts the customer their `pickup_security_token`.
8. The KDS view must **never** show price, raw comment text, or audio — only
   `display_number`, base selection, structural modifications, and `custom_notes`.

## 6. Business Rules (implement exactly, in `rule-service`, as pure functions over the change array)

1. **Cheese cap** — max 2 additional cheese varieties per pizza.
2. **Hawaii invariant** — if base item is HAWAII, pineapple may be removed but not
   added beyond quantity 1.
3. **Gluten-free constraint** — GF dough is a **whitelist** of sizes `[S, M]`, sourced
   from the same dynamic config the admin UI edits — do not hardcode an "L is invalid"
   blacklist, since sizes may expand later.
4. **Double cheese exclusion** — if any cheese addition has `quantity == 2`, block any
   other secondary extra-cheese topping.
5. **Binary separation** — anchovies (Sardellen) and vegan cheese alternatives are
   mutually exclusive in the same modification payload.
6. **Oven thermal limit** — `Σ addition.quantity` across all custom ingredients ≤ 10
   per pizza (configurable; see §8).

Run these sequentially against the incoming change array. Each rule returns either
nothing (pass) or a `FailedRule{rule_id, message_de}`. Collect all failures before
deciding `INVALID` vs `MANUAL_REVIEW` — don't short-circuit on the first failure if the
UI needs to show every violation at once.

## 7. Interface & Communication Decisions

- **REST/JSON everywhere**, including internal calls (`rule-service` → `catalog-service`
  / `pricing-service`). This is a deliberate scope decision for clarity; gRPC is the
  documented upgrade path for lower internal latency in production — leave a comment
  where these internal clients are defined noting that.
- **Kafka** for `OrderSubmitted` and `OrderStatusChanged` only. Everything else is
  synchronous REST.
- **SSE or WebSocket** from `kitchen-service` to the KDS frontend for live board
  updates — do not make the KDS poll.
- Notification consumption must be **idempotent** — dedupe on `order_id + status` in
  case Kafka redelivers, so a customer is never texted twice for the same transition.

## 8. Admin Endpoints (don't skip these — they were a gap in an earlier draft)

`admin-service` must expose, at minimum:
```
GET    /v1/admin/prices                     list ingredient/size/dough prices
PUT    /v1/admin/prices/{itemId}             update a single price
GET    /v1/admin/rules                       list current rule thresholds
PUT    /v1/admin/rules/{ruleId}              update a rule parameter (e.g. cheese cap, oven limit)
```
Any `PUT` to `/v1/admin/rules/*` must publish a `RuleConfigUpdated` Kafka event (or
Redis pub/sub message) so every running `rule-service` pod invalidates its in-memory
cache immediately — a rule change must never require a restart, and must propagate to
**all** replicas, not just the pod that received the write.

## 9. Data Model — Minimum Viable Schema

`order-service` (PostgreSQL, Flyway-managed):
```sql
CREATE TABLE orders (
    order_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_number        VARCHAR(10)  NOT NULL,      -- daily counter, e.g. #042
    status                VARCHAR(32)  NOT NULL,       -- PLACED | PROCESSING | READY_FOR_COLLECTION
    total_price            DECIMAL(10,2) NOT NULL,
    custom_notes           TEXT,                        -- bypasses validation, verbatim on KDS
    pickup_security_token  VARCHAR(64)  NOT NULL,
    created_at             TIMESTAMPTZ  DEFAULT now()
);

CREATE TABLE order_items (
    item_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID REFERENCES orders(order_id) ON DELETE CASCADE,
    base_pizza_id   VARCHAR(64)  NOT NULL,
    chosen_size     VARCHAR(5)   NOT NULL,
    chosen_dough    VARCHAR(32)  NOT NULL,
    modifications   JSONB        NOT NULL,  -- {"additions":[{"id":"x","qty":1}],"removals":["y"]}
    subtotal        DECIMAL(10,2) NOT NULL
);
```
The five most important attributes on `orders` for a grader/demo: `order_id`,
`display_number`, `status`, `total_price`, `custom_notes`.

No cross-service foreign keys, ever. If `catalog-service` needs to change independently
of `order-service`, that's the point — orders snapshot `base_price` at checkout so
catalog price changes never retroactively alter a placed order.

## 10. Local Development

```bash
# Build everything
mvn -T 1C clean install

# Run one service locally against local infra
docker compose up -d postgres kafka redis
cd order-service && mvn spring-boot:run

# Or run the whole stack locally
docker compose up --build
```
`docker-compose.yml` must define one Postgres instance with **separate databases**
per service (not one shared schema), one Kafka broker, and one Redis instance, plus
all 9 services with `depends_on` + healthchecks.

## 11. Dockerfiles

One per service, multi-stage:
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY <service>/pom.xml <service>/
RUN mvn -q -pl <service> -am dependency:go-offline
COPY <service> <service>/
RUN mvn -q -pl <service> -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/<service>/target/*.jar app.jar
EXPOSE 808X
ENTRYPOINT ["java", "-jar", "app.jar"]
```
Expose Actuator on the same port under `/actuator/health`, `/actuator/prometheus`.

## 12. Kubernetes

- One `Deployment` + `Service` + `ConfigMap` per microservice under `k8s/base/`.
- Use Kustomize overlays (`k8s/overlays/local`, `k8s/overlays/production`) for
  environment-specific replica counts / resource limits — don't fork the base manifests.
- Liveness probe → `/actuator/health/liveness`, readiness probe →
  `/actuator/health/readiness` (requires `management.endpoint.health.probes.enabled=true`).
- Secrets (DB creds, OpenAI API key, JWT signing key) via `Secret` objects, never baked
  into images or ConfigMaps.
- Ingress or the Gateway service is the only externally exposed entry point.

See §13 for the concrete deployment target (Hetzner Cloud, k3s) and a third overlay
(`k8s/overlays/hetzner`) sized for it.

## 13. Deployment Target — Hetzner Cloud (k3s)

The cluster runs on **Hetzner Cloud VMs** using **k3s** (not a managed K8s offering —
we own the control plane). Current setup: **2× CX22, no load balancer.** This is a
capacity decision, not an HA decision — see 13.4 for what that means honestly.

**Prerequisites — set these up in this order before touching 13.1:**
1. Hetzner Cloud account + a **Project** (everything below lives in one project).
2. An **SSH key** added to the Hetzner project.
3. A **Hetzner Private Network** (e.g. `10.0.0.0/16`) created in the project.
4. Two **Hetzner Cloud Firewalls** — Node A's public rule set and Node B's SSH-only
   rule set (exact rules in 13.3) — created and ready to attach.
5. Provision **Node A** and **Node B** (both CX22, Ubuntu 24.04, same region),
   attaching each to the private network and its firewall at creation time. Note each
   node's private IP from the console.
6. Confirm SSH access to both nodes before running any k3s command.
7. A domain/subdomain with DNS access, **A record pointed at Node A's public IP**
   (needed before 13.6's TLS step will work).
8. A container registry (GitHub Container Registry is easiest alongside GitHub
   Actions) with push credentials stored as repo secrets — needed before §14.
9. An OpenAI API key for `ai-parser-service`, ready to store as a Kubernetes `Secret`
   once the cluster exists.

`KUBE_CONFIG_B64` (§13.7) is the one secret you *can't* generate yet — it depends on
Node A's k3s server already being bootstrapped, so that comes after 13.2, not before.

### 13.1 Server sizing (current: 2 nodes)

- **Node A** — CX22 (2 vCPU / 4GB), runs the k3s **server** (control plane) and
  **Traefik ingress**. This is the node your DNS points at.
- **Node B** — CX22 (2 vCPU / 4GB), joins as a k3s **agent** (worker only). Pure
  extra capacity for pods — no ingress, no control-plane role.
- Both nodes: **Ubuntu 24.04**, same Hetzner region, same Hetzner project.
- With 9 Spring Boot services + Postgres + Kafka + Redis spread across 8GB total RAM,
  tune JVM heaps explicitly (`-Xmx256m` or similar per service) rather than letting
  defaults claim unbounded chunks — don't wait for an OOMKilled pod to find this out.
- A sensible starting split: put **Kafka + Postgres** (the stateful, memory-hungry
  pair) on **Node B** via `nodeSelector`, and let the 9 Spring Boot services schedule
  wherever the scheduler puts them across both nodes. Keeps your heaviest fixed-cost
  processes off the node that also has to answer the Kubernetes API and route ingress.

### 13.2 Cluster bootstrap (2 nodes: 1 server + 1 agent)

```bash
# On Node A (control plane + ingress), as root
curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644

# Get the join token (needed for Node B)
cat /var/lib/rancher/k3s/server/node-token

# On Node B, as root — join as a worker via Node A's PRIVATE network IP
curl -sfL https://get.k3s.io | K3S_URL=https://<node-A-private-ip>:6443 \
  K3S_TOKEN=<token-from-above> sh -

# Back on Node A, verify both nodes are Ready
kubectl get nodes -o wide
```
Set up the **Hetzner Private Network** between the two nodes *before* joining (see
13.3), and use Node A's private IP — not its public IP — for `K3S_URL`, so control-plane
traffic between the nodes never touches the public internet.

Pin ingress-relevant workloads to Node A explicitly, since k3s's default Traefik
Deployment could otherwise land on either node — and if it lands on Node B, your DNS
record (pointed at Node A) stops working:
```yaml
# values/patch for the Traefik deployment, or a nodeSelector on your gateway Ingress-backing pods
nodeSelector:
  kubernetes.io/hostname: node-a
```

### 13.3 Networking & firewall (2 nodes)

Use a **Hetzner Cloud Firewall**, applied differently per node:
```
Node A (control plane + ingress) — public-facing:
  22/tcp    — SSH, restrict to your IP
  6443/tcp  — Kubernetes API, restrict to your IP / CI runner IP
  80/tcp    — HTTP (ingress)
  443/tcp   — HTTPS (ingress)

Node B (worker only) — should NOT need any public inbound ports beyond SSH:
  22/tcp    — SSH, restrict to your IP
Outbound: allow all, both nodes
```
Put both nodes on a **Hetzner Private Network** (free) and let all node-to-node
traffic — the k3s agent connecting to the server, Kafka/Postgres reachability, any
service-to-service calls that cross nodes — stay on that private network. Node B's
public IP, if it has one at all, should end up doing nothing.

### 13.4 Ingress — and the tradeoff of skipping a load balancer

**No Hetzner Cloud Load Balancer for now** — with only 2 nodes it wouldn't be doing
its actual job (spreading traffic across multiple *interchangeable* ingress-capable
nodes and failing over between them). Instead:

- DNS A record → **Node A's public IP**, directly.
- Traefik (bundled with k3s), pinned to Node A per 13.2, handles routing to `gateway`.

**Be explicit with yourself about what this buys and doesn't buy.** Node B gives you
real horizontal capacity — more room for pods, and if Node B dies, its pods
reschedule onto Node A (assuming Node A has spare capacity) and the cluster keeps
serving traffic. But **Node A is still a single point of failure for both the control
plane and ingress** — if Node A goes down, the whole cluster's API and all incoming
traffic go down with it, node count notwithstanding. That's an accepted tradeoff at
this stage, not an oversight. The upgrade path when you want to remove that single
point of failure is: 3 control-plane nodes (odd number, for etcd quorum) + a Hetzner
Cloud Load Balancer in front of whichever nodes run ingress + the
`hcloud-cloud-controller-manager` so `Service{type: LoadBalancer}` provisions it
automatically. Revisit this section when that becomes worth the extra ~€20/mo.

### 13.5 Storage

- k3s's default `local-path-provisioner` is fine for Postgres/Kafka **only if you accept
  that data lives on the node's local disk** — good enough for a demo/assignment, not
  for real HA.
- For durable storage independent of the node, install the
  [Hetzner CSI driver](https://github.com/hetznercloud/csi-driver) and attach
  **Hetzner Cloud Volumes** as `PersistentVolumeClaim`s — do this before you care about
  surviving a node rebuild.

### 13.6 TLS

Install **cert-manager** + Let's Encrypt for automatic HTTPS on the `gateway` Ingress:
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
```
Point a `ClusterIssuer` at Let's Encrypt's HTTP-01 challenge (works fine on a single
public IP) rather than DNS-01 unless you already manage DNS via an API.

### 13.7 Deploying from GitHub Actions to a Hetzner-hosted cluster

Unlike EKS/GKE, there's no managed cloud auth — the pipeline needs the cluster's
kubeconfig directly:
```yaml
      - name: Configure kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.KUBE_CONFIG_B64 }}" | base64 -d > ~/.kube/config
      - name: Deploy
        run: kubectl set image deployment/order-service order-service=$REGISTRY/order-service:${{ github.sha }}
```
Generate the secret once with `cat /etc/rancher/k3s/k3s.yaml | base64 -w0`, **replace
the `server:` field's `127.0.0.1` with Node A's public IP** (the k3s server node)
before encoding, and store the result as the `KUBE_CONFIG_B64` GitHub Actions secret
(repo or org level, so all 9 service workflows can reuse it).

### 13.8 k8s/overlays/hetzner

Add a third overlay alongside `local` and `production` (see §12) with Hetzner-specific
values: `storageClassName: hcloud-volumes` (once the CSI driver is installed),
`ingressClassName: traefik`, and resource requests/limits sized to a CX22 (2 vCPU/4GB)
— don't reuse the `production` overlay's numbers blind. Since Node A also runs the
control plane and ingress, leave it more headroom than Node B when setting requests/limits
for the pods you expect the scheduler to place there (see the Kafka/Postgres split in 13.1).

## 14. CI/CD — one GitHub Actions workflow per service, path-filtered

Each `.github/workflows/<service>.yml` should look like:
```yaml
name: order-service
on:
  push:
    branches: [main]
    paths: ['order-service/**', 'common-contracts/**']
  pull_request:
    paths: ['order-service/**', 'common-contracts/**']
jobs:
  build-test-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: mvn -pl order-service -am test
      - run: mvn -pl order-service -am package -DskipTests
      - name: Build & push image
        run: |
          docker build -t $REGISTRY/order-service:${{ github.sha }} order-service
          docker push $REGISTRY/order-service:${{ github.sha }}
      - name: Deploy (rolling update)
        run: kubectl set image deployment/order-service order-service=$REGISTRY/order-service:${{ github.sha }}
```
The path filter is what gives each service its independence: a change to
`pricing-service/` never triggers `order-service`'s pipeline, and vice versa. Only
changes to `common-contracts/` should fan out to every service's pipeline.

## 15. Observability

- Every service: Spring Boot Actuator + Micrometer registry → Prometheus scrape endpoint.
- Structured JSON logging (Logback + logstash encoder) shipped to ELK/Loki — include
  `order_id` / `trace_id` in the MDC wherever a request touches an order.
- Distributed tracing via OpenTelemetry — propagate trace context across the REST hops
  in the Sync Loopback flow (`AI Parser → Rule Service → Catalog/Pricing`), since that's
  the path most likely to need debugging (rule rejections, ambiguous AI parses).
- Grafana dashboards: one per service minimum (latency, error rate, JVM), plus one
  cross-cutting dashboard for the order funnel (placed → approved/invalid/manual-review
  → ready).

## 16. Testing Conventions

- Unit tests for every rule in `rule-service` — one test class per rule, table-driven
  where possible (valid case, boundary case, violation case).
- `@SpringBootTest` + Testcontainers (Postgres, Kafka) for integration tests — don't
  mock the database in integration tests.
- Contract tests between `order-service` and `notification-service` around the
  `OrderStatusChanged` event schema (Spring Cloud Contract or a simple JSON schema
  check is fine).

## 17. Suggested Build Order (for incremental delivery)

1. `common-contracts` (event/DTO shapes only)
2. `catalog-service` + `pricing-service` (no dependencies on anything else)
3. `rule-service` (depends on catalog + pricing via REST)
4. `order-service`
5. `gateway` (wire up routing to the above four — enough for a manual-click-only demo)
6. `ai-parser-service` (adds free-text path)
7. `kitchen-service` + `notification-service` (adds the async event side)
8. `admin-service`
9. Dockerize everything, `docker-compose.yml` for local integration
10. Kubernetes manifests + CI/CD workflows
11. Observability wiring last, once the happy path works end-to-end

## 18. Things to Ask Before Assuming

- Real payment processing is explicitly **out of scope** per the assignment — don't add it.
- Frontend framework (React vs Vue) — default to React unless told otherwise.
- Deployment target is **Hetzner Cloud, 2-node k3s, no load balancer** (see §13) —
  1 node runs control-plane + ingress, the other is worker capacity. Don't add a
  3rd control-plane node or a Hetzner Load Balancer unless asked — that's a deliberate
  future upgrade, not an oversight.
