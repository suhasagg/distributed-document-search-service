# Distributed Document Search Service

A multi-tenant, event-driven document search prototype designed to demonstrate production-oriented architecture for large-scale search systems.

This project implements:
- PostgreSQL as the source of truth
- Kafka for asynchronous indexing
- Elasticsearch for full-text search
- Redis for caching and rate limiting
- Spring Boot for the API layer

The prototype is intentionally scoped for an assessment, but the architecture is shaped like a production system and includes clear paths for scaling, resilience, and operational hardening.

## 1. Problem Statement

This service is designed for a scenario where a company needs to:
- store and search millions of documents across multiple tenants
- support full-text search with relevance ranking
- return low-latency search responses
- handle multi-tenant isolation safely
- scale horizontally
- demonstrate enterprise-grade design patterns

The system separates:
- transactional persistence
- search indexing
- query caching
- request throttling

This makes the write path reliable and the search path fast.

## 2. Architecture Overview

### High-level architecture

```text
                           +----------------------+
                           |       Clients        |
                           |  UI / API Consumers  |
                           +----------+-----------+
                                      |
                                      v
                         +------------+-------------+
                         |      Spring Boot API     |
                         | CRUD + Search + Health   |
                         +------+---------+---------+
                                |         |
                    Write path  |         | Search path
                                |         |
                                v         v
                    +-----------+--+   +--+------------------+
                    | PostgreSQL   |   | Redis               |
                    | Source of    |   | Query cache +       |
                    | Truth        |   | rate limiting       |
                    +------+-------+   +-----+---------------+
                           |                 |
                           |                 |
                           v                 |
                    +------+-----------------+------+
                    |      Kafka (Index Events)     |
                    | document-index-events topic   |
                    +---------------+---------------+
                                    |
                                    v
                         +----------+-----------+
                         |   KafkaIndexConsumer |
                         |   Async indexer      |
                         +----------+-----------+
                                    |
                                    v
                         +----------+-----------+
                         |    Elasticsearch     |
                         | Full-text index      |
                         | Ranked retrieval     |
                         +----------------------+
```

### Core architectural choices

#### PostgreSQL
Used as the source of truth because it provides strong transactional guarantees and reliable canonical storage.

#### Kafka
Used to decouple document writes from indexing so the write API is not tightly coupled to Elasticsearch availability or performance.

#### Elasticsearch
Used as the search-optimized read model for low-latency full-text search, relevance ranking, fuzzy matching, and highlighting.

#### Redis
Used for:
- caching repeated search results
- rate limiting per tenant

## 3. Key Features

### Functional features
- Create document
- Retrieve document by ID
- Delete document
- Full-text search
- Multi-tenant isolation
- Health check with dependency status

### Platform features
- Async indexing through Kafka
- Redis-backed search cache
- Redis-backed tenant rate limiting
- Elasticsearch relevance ranking
- Fuzzy search
- Highlighted search snippets

### Assessment bonus features included
- Fuzzy search
- Highlighting
- Benchmark script
- Blue-green deployment strategy documentation
- Cost optimization guidance

## 4. Consistency Model

The system intentionally uses a mixed consistency model.

### Strong consistency
- document writes to PostgreSQL
- read-by-ID from PostgreSQL

### Eventual consistency
- document visibility in Elasticsearch search results

### Why this trade-off was chosen

Synchronous writes to both PostgreSQL and Elasticsearch would:
- increase write latency
- tightly couple API success to search cluster health
- reduce resilience

Instead, this design acknowledges writes once durable in PostgreSQL, then propagates indexing asynchronously through Kafka.

## 5. Multi-Tenancy Strategy

Tenant isolation is enforced at multiple layers.

### Tenant identification
The API uses the `X-Tenant-ID` header.

### Isolation enforcement
Tenant scope is applied in:
- document creation
- document retrieval
- document deletion
- Elasticsearch search filter
- Redis cache key namespace
- Redis rate-limit key namespace

### Why this matters
This prevents accidental cross-tenant leakage not only in business logic but also in:
- search results
- cache pollution
- throttling behavior

## 6. API List

### 6.1 Create Document

**Endpoint** 
`POST /documents`

**Headers**
```http
X-Tenant-ID: tenant-a
Content-Type: application/json
```

**Request body**
```json
{
  "title": "Distributed systems notes",
  "content": "Kafka Elasticsearch Redis scaling"
}
```

**Example curl**
```bash
curl -X POST http://localhost:8080/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-a" \
  -d '{
    "title": "Distributed systems notes",
    "content": "Kafka Elasticsearch Redis scaling"
  }'
```

**Example response**
```json
{
  "id": "15eb4261-5d29-4c6d-83cc-a7ceaf291e9b",
  "tenantId": "tenant-a",
  "title": "Distributed systems notes",
  "content": "Kafka Elasticsearch Redis scaling",
  "createdAt": "2026-04-21T15:17:20.133021930Z",
  "updatedAt": "2026-04-21T15:17:20.133021930Z"
}
```

### 6.2 Search Documents

**Endpoint** 
`GET /search?q={query}&size={size}`

**Headers**
```http
X-Tenant-ID: tenant-a
```

**Example curl**
```bash
curl "http://localhost:8080/search?q=distributed&size=10" \
  -H "X-Tenant-ID: tenant-a"
```

**Example response**
```json
{
  "tenantId": "tenant-a",
  "query": "distributed",
  "total": 1,
  "results": [
    {
      "id": "15eb4261-5d29-4c6d-83cc-a7ceaf291e9b",
      "tenantId": "tenant-a",
      "title": "Distributed systems notes",
      "snippet": "Kafka Elasticsearch Redis scaling",
      "score": 0.5753642
    }
  ],
  "cacheHit": false
}
```

**Notes**
- `cacheHit` shows whether the response came from Redis
- tenant filter is always applied
- fuzzy search is enabled
- highlighting may appear in the `snippet` field

### 6.3 Get Document by ID

**Endpoint** 
`GET /documents/{id}`

**Headers**
```http
X-Tenant-ID: tenant-a
```

**Example curl**
```bash
curl http://localhost:8080/documents/15eb4261-5d29-4c6d-83cc-a7ceaf291e9b \
  -H "X-Tenant-ID: tenant-a"
```

**Example response**
```json
{
  "id": "15eb4261-5d29-4c6d-83cc-a7ceaf291e9b",
  "tenantId": "tenant-a",
  "title": "Distributed systems notes",
  "content": "Kafka Elasticsearch Redis scaling",
  "createdAt": "2026-04-21T15:17:20.133021Z",
  "updatedAt": "2026-04-21T15:17:20.133021Z"
}
```

### 6.4 Delete Document

**Endpoint** 
`DELETE /documents/{id}`

**Headers**
```http
X-Tenant-ID: tenant-a
```

**Example curl**
```bash
curl -X DELETE http://localhost:8080/documents/15eb4261-5d29-4c6d-83cc-a7ceaf291e9b \
  -H "X-Tenant-ID: tenant-a" -i
```

**Example response**
```http
HTTP/1.1 204 No Content
```

### 6.5 Health Check

**Endpoint** 
`GET /health`

**Example curl**
```bash
curl http://localhost:8080/health
```

**Example response**
```json
{
  "status": "UP",
  "dependencies": {
    "kafka": "UP",
    "redis": "UP",
    "postgres": "UP"
  }
}
```

## 7. Project Structure

```text
.
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── README.md
├── benchmarks/
│   └── k6-search-smoke.js
├── docs/
│   ├── architecture-design.md
│   ├── production-readiness-analysis.md
│   ├── enterprise-experience-showcase.md
│   └── interview-walkthrough-notes.md
└── src/
    ├── main/java/com/example/search/
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── messaging/
    │   ├── config/
    │   ├── dto/
    │   └── domain/
    └── main/resources/
        └── application.yml
```

## 8. How the Code Works

### Controllers

#### DocumentController
Handles:
- create document
- get document by ID
- delete document

This controller is intentionally thin and delegates business logic to `DocumentService`.

#### SearchController
Handles:
- search endpoint only

This separation keeps responsibilities clean and avoids endpoint ambiguity.

### Services

#### DocumentService
Responsible for:
- building and persisting documents
- cache eviction on writes and deletes
- publishing Kafka indexing events

Important detail:
The service ensures that the document write is visible before the indexing event is published, which fixes the transaction-visibility race discovered during debugging.

#### SearchService
Responsible for:
- applying rate limit checks
- checking Redis cache
- calling Elasticsearch on cache miss
- caching non-empty results

#### ElasticsearchIndexService
Encapsulates:
- index creation
- upsert into Elasticsearch
- delete from Elasticsearch
- full-text search query execution
- fuzzy matching
- highlighting

#### CacheService
Handles:
- Redis read and write for cached search results
- tenant-scoped cache eviction

#### IndexerWorker / KafkaIndexConsumer
Consumes Kafka events and updates Elasticsearch asynchronously.

#### IndexingEventPublisher
Publishes UPSERT and DELETE events to Kafka.

## 9. Local Setup and Run Instructions

### Prerequisites
- Docker
- Docker Compose
- Java 21 and Maven only if running outside Docker
- Optional: k6 for load testing

### Run with Docker Compose
```bash
docker compose up --build
```

### To reset state
```bash
docker compose down -v
docker compose up --build
```

## 10. Full Demo and Testing Steps

### 10.1 Start clean
```bash
docker compose down -v
docker compose up --build
```

### 10.2 Health check
```bash
curl http://localhost:8080/health
```

### 10.3 Verify Kafka topic
```bash
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic document-index-events
```

Expected:
```text
PartitionCount:1
```

### 10.4 Create a tenant-a document
```bash
curl -X POST http://localhost:8080/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-a" \
  -d '{
    "title": "Distributed systems notes",
    "content": "Kafka Elasticsearch Redis scaling"
  }'
```

Save the returned id:
```bash
DOC_A=<paste-id>
echo $DOC_A
```

### 10.5 Verify Postgres source of truth
```bash
docker compose exec postgres psql -U postgres -d docsearch \
  -c "select id, tenant_id, title from documents where id='$DOC_A';"
```

### 10.6 Verify Elasticsearch indexing
```bash
sleep 1
curl -s "http://localhost:9200/documents/_doc/$DOC_A?pretty"
```

Expected:
```text
"found": true
```

### 10.7 Search tenant-a

First call:
```bash
curl "http://localhost:8080/search?q=distributed&size=10" \
  -H "X-Tenant-ID: tenant-a"
```

Expected:
- `total = 1`
- `cacheHit = false`

Second call:
```bash
curl "http://localhost:8080/search?q=distributed&size=10" \
  -H "X-Tenant-ID: tenant-a"
```

Expected:
- `total = 1`
- `cacheHit = true`

### 10.8 Verify Redis cache key
```bash
docker compose exec redis redis-cli KEYS 'search:*'
```

### 10.9 Fetch by ID
```bash
curl http://localhost:8080/documents/$DOC_A \
  -H "X-Tenant-ID: tenant-a"
```

### 10.10 Create tenant-b document
```bash
curl -X POST http://localhost:8080/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-b" \
  -d '{
    "title": "Tenant B private document",
    "content": "orionblue belongs only to tenant b"
  }'
```

Save returned id:
```bash
DOC_B=<paste-id>
```

### 10.11 Verify tenant isolation

Search from tenant-a:
```bash
curl "http://localhost:8080/search?q=orionblue&size=10" \
  -H "X-Tenant-ID: tenant-a"
```

Expected:
- `total = 0`

Search from tenant-b:
```bash
curl "http://localhost:8080/search?q=orionblue&size=10" \
  -H "X-Tenant-ID: tenant-b"
```

Expected:
- `total = 1`

Search tenant-b again to show cache:
```bash
curl "http://localhost:8080/search?q=orionblue&size=10" \
  -H "X-Tenant-ID: tenant-b"
```

Expected:
- `cacheHit = true`

### 10.12 Delete document
```bash
curl -X DELETE http://localhost:8080/documents/$DOC_A \
  -H "X-Tenant-ID: tenant-a" -i
```

### 10.13 Verify deletion
```bash
curl http://localhost:8080/documents/$DOC_A \
  -H "X-Tenant-ID: tenant-a"
```

Expected:
```text
404
```

## 11. Bonus Features Implemented

### 11.1 Fuzzy Search

What it does

The system supports fuzzy matching using Elasticsearch’s fuzziness: AUTO, allowing approximate matches for user queries.

Why it matters

In real-world systems:

Users frequently make typos
Queries may be incomplete or misspelled
Exact-match search reduces recall

Fuzzy search improves:

User experience
Search recall
Robustness to input errors

Implementation
Implemented using multi_match query with fuzziness
Applied to both:
title (boosted)
content

```bash
curl "http://localhost:8080/search?q=distrbuted&size=10" \
  -H "X-Tenant-ID: tenant-a"
```
Expected:

Matches "distributed" despite typo

Trade-offs
Slight increase in query cost
Can introduce irrelevant matches if overused

Production improvement
Enable fuzziness only for short queries
Disable for long queries to reduce cost

### 11.2 Highlighting
What it does

Search responses include highlighted snippets showing matched text.

Why it matters

Improves result explainability
Helps users quickly understand relevance
Common feature in enterprise search systems (Google, Elastic, etc.)

Implementation
Elasticsearch highlight API used
Fields:
title
content

```bash
curl "http://localhost:8080/search?q=redis&size=10" \
  -H "X-Tenant-ID: tenant-a"
```
The `snippet` field may contain highlighted matched fragments.

Trade-offs
Slightly higher response size
Minor latency increase
Production improvement
Limit highlight size
Use fragment size tuning

### 11.3 Benchmark Script
Included:
```text
benchmarks/k6-search-smoke.js
```

Run:
```bash
k6 run benchmarks/k6-search-smoke.js
```

Or with environment variables:
```bash
BASE_URL=http://localhost:8080 TENANT=tenant-a QUERY=distributed k6 run benchmarks/k6-search-smoke.js
```

Benchmark interpretation:
If the benchmark uses one tenant and exceeds the configured per-tenant limit, some responses will be throttled. That is expected and validates rate limiting.

## 11.4 Benchmark Notes

A k6 run showed that successful requests were very fast, but a high failure rate can appear when the test intentionally exceeds the configured per-tenant rate limit.

This is not only a performance signal but also proof that the per-tenant protection logic is working.



### 11.4 Blue-Green Deployment Strategy
What it is

A zero-downtime deployment strategy using two environments:

Blue (current live)
Green (new version)
Flow
Deploy new version as green
Run validation tests:
health check
API tests
indexing + search validation
Shift traffic gradually
If issue occurs → instant rollback to blue
Why it matters
Zero downtime deployments
Safe rollback
Production stability
Production implementation

Using:

Kubernetes + Service routing
or Load balancer switching
Advanced upgrade
Canary deployment (partial traffic)
Feature flags for gradual rollout

### 11.5 Cost Optimization Strategy
Key principle

Scale each component independently, not uniformly.

API Layer
Stateless → horizontal scaling
Auto-scale based on CPU + latency
Use spot instances for non-critical workloads
PostgreSQL
Keep only canonical data
Avoid using as search engine
Add read replicas only if needed
Elasticsearch
Most expensive component → optimize carefully

Optimizations:

Right shard count (avoid over-sharding)
Increase refresh interval for heavy indexing
Use hot/warm architecture
Archive old data
Redis
Use short TTLs
Cache only high-value queries
Avoid caching empty results
Kafka
Partition count based on throughput
Avoid over-provisioning brokers
Use managed Kafka if ops overhead is high
Cloud-level optimization
Use autoscaling groups
Use reserved instances for steady load
Use spot instances for indexing jobs

### 11.7 Rate Limiting (Abuse Protection)
What it does

Prevents a single tenant from overwhelming the system.

Implementation
Redis-based counters
Per-tenant request window
Why it matters
Protects shared infrastructure
Ensures fairness across tenants
Observed behavior
High load → throttling triggered
System remains stable
11.8 Cache Strategy Optimization
Current strategy

Cache key:

search:{tenant}:{queryHash}:{size}
Cache only non-empty results
TTL-based expiration
Why not cache empty results

Because of eventual consistency:

A document may be indexed shortly after a query
Cached empty result would hide it
Production improvements
Cache invalidation on write/delete
Query normalization
Adaptive TTL based on query popularity

### 11.9 Real-world Production Enhancements (Future Scope)

These are not implemented but clearly designed for:

Reliability
Dead-letter queue for Kafka
Retry with exponential backoff
Idempotent consumers
Observability
Prometheus metrics
Grafana dashboards
Distributed tracing
Security
JWT authentication
Tenant-level authorization policies
Data correctness
Outbox pattern for reliable event publishing

