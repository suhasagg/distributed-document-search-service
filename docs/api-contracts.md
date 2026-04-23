# API Contracts

This file gives concrete request/response examples for the implemented endpoints.

## Common header

All business endpoints require:

```http
X-Tenant-ID: <tenant-id>
```

For the prototype, tenant identity comes from a trusted header.  
In production, this should be derived from an authenticated identity token or gateway-injected claim.

---

## POST /documents

Creates a new document for the tenant.

### Request
```http
POST /documents
Content-Type: application/json
X-Tenant-ID: tenant-a
```

```json
{
  "title": "Distributed systems notes",
  "content": "Kafka decouples writes from indexing and Elasticsearch serves search."
}
```

### Response
```http
201 Created
```

```json
{
  "id": "2e8c4dbe-7b31-4c27-a9b6-d2aa6d7b8df2",
  "tenantId": "tenant-a",
  "title": "Distributed systems notes",
  "content": "Kafka decouples writes from indexing and Elasticsearch serves search.",
  "createdAt": "2026-04-21T14:03:11.093Z",
  "updatedAt": "2026-04-21T14:03:11.093Z"
}
```

---

## GET /documents/{id}

Returns document details if the document belongs to the tenant.

### Request
```http
GET /documents/2e8c4dbe-7b31-4c27-a9b6-d2aa6d7b8df2
X-Tenant-ID: tenant-a
```

### Response
```http
200 OK
```

```json
{
  "id": "2e8c4dbe-7b31-4c27-a9b6-d2aa6d7b8df2",
  "tenantId": "tenant-a",
  "title": "Distributed systems notes",
  "content": "Kafka decouples writes from indexing and Elasticsearch serves search.",
  "createdAt": "2026-04-21T14:03:11.093Z",
  "updatedAt": "2026-04-21T14:03:11.093Z"
}
```

### Not found response
```http
404 Not Found
```

```json
{
  "message": "Document not found"
}
```

---

## GET /search?q={query}&size={n}

Searches documents within the tenant.

### Request
```http
GET /search?q=distributed&size=10
X-Tenant-ID: tenant-a
```

### Cache miss response
```http
200 OK
```

```json
{
  "tenantId": "tenant-a",
  "query": "distributed",
  "total": 1,
  "results": [
    {
      "id": "2e8c4dbe-7b31-4c27-a9b6-d2aa6d7b8df2",
      "tenantId": "tenant-a",
      "title": "Distributed systems notes",
      "snippet": "Kafka decouples writes from indexing and Elasticsearch serves search.",
      "score": 1.4271
    }
  ],
  "cacheHit": false
}
```

### Cache hit response
Same structure, but:

```json
{
  "cacheHit": true
}
```

### Rate limit exceeded response
```http
429 Too Many Requests
```

```json
{
  "message": "Tenant search rate limit exceeded"
}
```

---

## DELETE /documents/{id}

Deletes a tenant-owned document.

### Request
```http
DELETE /documents/2e8c4dbe-7b31-4c27-a9b6-d2aa6d7b8df2
X-Tenant-ID: tenant-a
```

### Response
```http
204 No Content
```

---

## GET /health

Returns service health with key dependency status.

### Request
```http
GET /health
```

### Response
```json
{
  "status": "UP",
  "dependencies": {
    "postgres": "UP",
    "redis": "UP",
    "kafka": "UP"
  }
}
```

If one dependency is down, the response becomes:

```json
{
  "status": "DEGRADED",
  "dependencies": {
    "postgres": "UP",
    "redis": "DOWN",
    "kafka": "UP"
  }
}
```
