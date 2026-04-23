# Distributed Document Search Service — Architecture Design

## 1. Goal and assumptions

This service supports multi-tenant full-text search over millions of documents with sub-second query latency and horizontally scalable indexing. The prototype is intentionally simplified, but the architecture choices are production-oriented: PostgreSQL for durable document metadata, Kafka for asynchronous indexing, Elasticsearch for search, and Redis for caching and rate limiting. The design targets eventual consistency between the source of truth and the search index.

Assumptions for the assignment:
- 10M+ documents across many tenants
- 1000+ concurrent search requests per second
- p95 query latency under 500 ms
- Logical tenant isolation is acceptable for the prototype
- Search freshness within a few seconds is acceptable

## 2. High-level architecture

```
Clients
  |
  v
Spring Boot API
  |-------------------------------> Redis (search cache, rate limit)
  |
  +--> PostgreSQL (source of truth for documents)
  |
  +--> Kafka topic: document-index-events
             |
             v
      Indexer Consumer(s)
             |
             v
      Elasticsearch index
```

### Main components
- **API layer**: Handles CRUD, search, validation, tenant scoping, health checks.
- **PostgreSQL**: Source of truth for document records and tenant ownership.
- **Kafka**: Durable event stream between write path and indexing path.
- **Indexer consumer**: Reads indexing events and upserts/deletes documents in Elasticsearch.
- **Elasticsearch**: Full-text search engine using inverted indexes and relevance scoring.
- **Redis**: Query-result cache and simple per-tenant rate-limiting counters.

## 3. Data flow

### Indexing / write flow
1. Client calls `POST /documents` with `X-Tenant-ID`.
2. API validates request and persists the document in PostgreSQL.
3. API evicts cached search entries for that tenant.
4. API publishes an `UPSERT` event to Kafka.
5. Indexer consumer reads the event and writes the document into Elasticsearch.

### Delete flow
1. Client calls `DELETE /documents/{id}` with `X-Tenant-ID`.
2. API verifies tenant ownership and deletes from PostgreSQL.
3. API evicts tenant cache entries.
4. API publishes a `DELETE` event to Kafka.
5. Indexer consumer deletes the corresponding Elasticsearch document.

### Search flow
1. Client calls `GET /search?q=...` with `X-Tenant-ID`.
2. API applies a Redis-based per-tenant rate limit.
3. API checks Redis for a cached result.
4. On cache miss, API queries Elasticsearch with a `multi_match` query plus `tenantId` filter.
5. Response is cached with a short TTL and returned to the client.

## 4. Database and storage strategy

### PostgreSQL
Chosen as the system of record because it provides strong transactional semantics, straightforward schema evolution, and clean tenant ownership validation. It stores document ID, tenant ID, title, content, and timestamps.

### Elasticsearch
Chosen for full-text search because it offers inverted indexing, analyzers, BM25-style ranking, and horizontal scaling through shards and replicas. The current mapping indexes `tenantId` as a keyword field and `title` and `content` as text fields.

### Redis
Used for two low-latency responsibilities: cached query responses and lightweight rate limiting. This keeps repeated searches fast and protects Elasticsearch from bursty tenants.

## 5. API design

### `POST /documents`
Creates a document for the tenant in the header.

Request body:
```json
{
  "title": "Distributed systems notes",
  "content": "Kafka decouples indexing from writes."
}
```

### `GET /documents/{id}`
Returns a single document only if the tenant owns it.

### `DELETE /documents/{id}`
Deletes a document only if the tenant owns it.

### `GET /search?q={query}&size={n}`
Performs a full-text search scoped to the tenant.

### `GET /health`
Reports dependency health for PostgreSQL, Redis, and Kafka.

## 6. Multi-tenancy and security approach

The prototype uses **header-based tenant scoping** with `X-Tenant-ID`. In production this would come from an authenticated token rather than a trusted client header. Tenant isolation is enforced in two places:
- JPA lookups include `tenantId` for CRUD reads and deletes
- Elasticsearch queries include a `tenantId` filter

This is logical isolation in a shared cluster. For very large or highly regulated tenants, the next step would be dedicated indices or separate clusters.

## 7. Consistency model and trade-offs

The design is **eventually consistent** between PostgreSQL and Elasticsearch. This is a deliberate trade-off:
- **Pros**: low write latency, durable async buffering, better resilience to Elasticsearch slowness, and independent scaling of indexing consumers
- **Cons**: a document may be readable from PostgreSQL before it appears in search results

In this workload that trade-off is reasonable because search systems commonly optimize for throughput and availability over synchronous index updates.

## 8. Caching strategy

- **Search result cache**: keyed by tenant + query hash + page size, TTL around 60 seconds
- **Eviction**: tenant search cache is invalidated on document create and delete
- **Why cache here**: many enterprise search queries repeat, and Redis shields Elasticsearch from identical hot queries

## 9. Message queue usage

Kafka is used to decouple writes from indexing. That provides:
- durable buffering during bursts
- replay capability after failures
- independent scaling of API and indexer tiers
- safer handling of downstream slowness

For a more rigorous production implementation, I would use the **transactional outbox pattern** so the database write and Kafka publication cannot diverge.

## 10. Production hardening roadmap

To make this truly production-ready, the next upgrades would be:
- separate API and indexer deployments
- Kafka DLQ topic and replay tooling
- outbox pattern for DB/Kafka consistency
- JWT auth with tenant-scoped claims
- Elasticsearch aliases, rollover, and reindex jobs
- circuit breakers, retries, and backpressure policies
- metrics, logs, tracing, SLO dashboards, and alerting
- replicas, multi-AZ deployment, backup and restore testing

This architecture demonstrates the right shape for an enterprise-grade distributed search service while remaining small enough to implement and explain in an interview setting.
