# Interview Walkthrough Notes — Distributed Document Search Service

## 1. Thirty-second opener

I designed the service around a classic separation of concerns: PostgreSQL as the source of truth, Kafka as the durable asynchronous handoff, Elasticsearch as the search-optimized read model, and Redis as the low-latency cache and rate-limiting layer. The key design choice is eventual consistency, which lets the write path stay fast while the indexing tier scales independently.

## 2. How to walk through the architecture

Start with the write path first because it explains the system shape. A document create request lands at the Spring Boot API, which persists the document in PostgreSQL, evicts any stale cached search entries for that tenant, and publishes an indexing event to Kafka. An indexer consumer reads that event and writes the searchable representation into Elasticsearch.

Then explain the read path. Search requests first go through rate limiting, then Redis cache lookup, and only on a miss do they hit Elasticsearch. Results are scoped by tenant and then cached briefly. This lets the system absorb hot queries without hammering Elasticsearch.

## 3. Why each technology was chosen

### PostgreSQL
I used PostgreSQL because it is a reliable transactional store and a clean source of truth for ownership and metadata. It is simpler and safer than trying to make Elasticsearch the primary store.

### Kafka
Kafka replaces the in-process queue because it is durable, replayable, and scalable. It also gives operational flexibility: I can scale the API tier and indexing tier independently, and temporary Elasticsearch issues do not have to take down writes.

### Elasticsearch
Elasticsearch is the natural fit for full-text search over millions of documents. It gives analyzers, inverted indexes, relevance scoring, and horizontal scale.

### Redis
Redis is used where ultra-low latency matters: repeated searches and simple tenant rate limiting. It is cheap insurance for p95 search latency.

## 4. Multi-tenancy explanation

The prototype uses logical tenant isolation in a shared cluster. Every business endpoint requires `X-Tenant-ID`, and tenant checks are applied consistently in both the relational store and the search layer. In production, that tenant identity should come from JWT claims or a gateway-injected identity rather than a raw client header.

If the interviewer asks how to scale tenant isolation further, say that small and medium tenants can share infrastructure efficiently, while very large or regulated tenants may move to dedicated indices or even isolated clusters.

## 5. Consistency trade-off explanation

The most important trade-off is eventual consistency. A user may successfully create a document and briefly not see it in search until the Kafka consumer updates Elasticsearch. I would frame that as intentional: synchronous indexing increases write latency and couples the API too tightly to the search engine. For enterprise search, a short freshness delay is usually acceptable.

If pressed on correctness, mention that the stronger production pattern is a transactional outbox. That avoids the DB/Kafka dual-write gap and lets events be published reliably after commit.

## 6. Scalability story

For scale, each tier can grow independently:
- stateless API instances scale horizontally behind a load balancer
- Kafka partitions scale indexing throughput
- Elasticsearch scales with shards and replicas
- Redis handles hot-query offload
- PostgreSQL can add read replicas and stronger partitioning strategies if needed

For 100x growth, I would introduce dedicated indexer deployments, partition topics based on tenant or document ID, use index lifecycle management in Elasticsearch, and adopt stronger capacity planning around shard sizing and cache hit rate.

## 7. Failure handling story

If Elasticsearch is slow or temporarily unavailable, writes still succeed because Kafka buffers indexing work. Consumers can retry, and failures can go to a DLQ instead of dropping events. If Redis is unavailable, the system can degrade gracefully by serving uncached searches. If Kafka is down, I would fail writes fast or route through an outbox fallback depending on durability requirements.

## 8. Security and operations story

For security, the next production step is JWT authentication with tenant-scoped authorization, TLS in transit, encryption at rest, and audit logging. For operations, I would add Prometheus metrics, structured logs, OpenTelemetry tracing, health and readiness probes, blue-green or canary deployment, and restore-tested backups.

## 9. Common interviewer follow-ups

### Why not just use PostgreSQL full-text search?
For a small system, Postgres FTS would be perfectly reasonable. For millions of documents, richer ranking, analyzers, and search scale, Elasticsearch is the better fit.

### Why not index synchronously?
Because that increases user-facing write latency and makes the API path depend on Elasticsearch health. Kafka lets indexing lag slightly without degrading write throughput.

### What is the biggest production gap still left?
The biggest real production gap is the DB/Kafka dual-write issue. The right answer is to add a transactional outbox and a relay publisher.

### How would you achieve 99.95% availability?
Run multi-AZ, keep services stateless where possible, use Kafka replication, deploy Elasticsearch with replicas, use managed Postgres or HA failover, define SLOs, and build good alerting plus zero-downtime deployment practices.

## 10. Two-minute closing summary

The design is intentionally pragmatic: keep PostgreSQL as the source of truth, use Kafka to decouple writes from indexing, use Elasticsearch for search, and Redis to keep hot reads fast. That gives a clear path from prototype to production. The biggest deliberate trade-off is eventual consistency, and the biggest next step for production correctness is the outbox pattern. Overall, the architecture is simple enough to explain quickly but realistic enough to demonstrate enterprise-scale design judgment.
