# AI Tool Usage Note

This submission was developed with AI assistance for acceleration, not blind generation.

AI tools were used to:
- accelerate initial code scaffolding
- generate draft documentation structure
- refine API examples and repo organization
- help articulate trade-offs and production-hardening steps

I reviewed and curated the final architecture, technology choices, trade-offs, and documentation.  
The important decisions — especially the use of PostgreSQL as source of truth, Kafka for asynchronous indexing, Elasticsearch as a derived search model, Redis for cache/rate limiting, and eventual consistency with outbox as the next production step — were selected intentionally.

I treated AI as a force multiplier for speed, while keeping ownership of system design judgment and final deliverables.
