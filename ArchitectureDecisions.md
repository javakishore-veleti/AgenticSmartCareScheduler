# Architecture Decisions — AgenticSmartCareScheduler

> This file captures all architecture and infrastructure decisions.
> Read by Claude Code at session startup alongside CLAUDE.md and CodingConventions.md.

---

## Database Strategy

### Default: H2 (Zero Config)
- H2 in-memory database is the **default** for all local development
- No setup required — just run the Spring Boot app
- Configured in `application.yml`

### Postgres: Profile-Activated Only
- Postgres driver is included but **only activated** with `-Dspring.profiles.active=local-postgres`
- Configured in `application-local-postgres.yml`
- Uses **pgVector extension** for vector search in local development
- Docker Compose: `DevOps/Local/Postgres/docker-compose.yml`

### MongoDB
- Used where document-oriented storage is more appropriate than relational
- Local: `DevOps/Local/MongoDB/docker-compose.yml`
- MongoDB Atlas Vector Search for local vector DB alternative

### Vector Database Strategy
| Environment | Vector DB | Purpose |
|---|---|---|
| Default (H2) | In-memory simple similarity | Dev/test, no external deps |
| Local Postgres | pgVector extension | Vector search with SQL |
| Local MongoDB | MongoDB Atlas Vector Search | Document + vector combined |
| Local Standalone | Qdrant or Milvus via Docker | Dedicated vector DB |
| AWS | Amazon OpenSearch (k-NN) | Production vector search |
| AWS | Amazon Bedrock Knowledge Bases | Managed RAG |

---

## Observability Stack

### Spring Boot
- **Micrometer** for metrics (counters, gauges, timers per agent)
- **OpenTelemetry** for distributed tracing across agents
- Auto-instrumented via `spring-boot-starter-actuator` + `micrometer-tracing-bridge-otel`

### Local Observability (DevOps/Local/)
| Tool | Purpose | Docker Compose |
|---|---|---|
| Prometheus | Metrics collection | `DevOps/Local/Prometheus/docker-compose.yml` |
| Grafana | Metrics dashboards | `DevOps/Local/Grafana/docker-compose.yml` |
| Jaeger | Distributed tracing | `DevOps/Local/Jaeger/docker-compose.yml` |
| Kibana | Log analysis (with OpenSearch) | `DevOps/Local/Kibana/docker-compose.yml` |

### AWS Observability
- Amazon CloudWatch Metrics + Logs
- AWS X-Ray for distributed tracing
- Amazon OpenSearch Dashboards (replaces Kibana)

---

## Messaging & Streaming

### Local
- **Kafka**: `DevOps/Local/Kafka/docker-compose.yml` — inter-agent event streaming
- **Redis**: `DevOps/Local/Redis/docker-compose.yml` — caching, session, pub/sub

### AWS
- **Amazon EventBridge** — event-driven agent communication
- **Amazon ElastiCache (Redis)** — if caching needed
- **Amazon MSK** — if Kafka needed in production

---

## Workflow Engine

### Default: Embedded Temporal
- Temporal runs **embedded** in the Spring Boot process by default
- No external dependency for local development
- Sufficient for development and testing

### External Temporal: Feature Toggle
```yaml
# application.yml
feature:
  toggles:
    workflow:
      engines:
        temporal:
          enabled: false  # default: embedded
```
- When `feature.toggles.workflow.engines.temporal.enabled: true` → connects to external Temporal server
- Local external: `DevOps/Local/Temporal/docker-compose.yml`
- AWS: Temporal on EKS or AWS Step Functions as alternative

### AWS Workflows
- **AWS Step Functions** for serverless orchestration
- **app-cloud-aws-wfs** module for Step Functions integration
- Can coexist with Temporal — Step Functions for cloud-native flows, Temporal for complex sagas

---

## UI — Angular Delivered by Spring Boot

### No Separate Portal Server
- Angular apps are **built and served by Spring Boot** — no separate Node.js server
- Two Angular apps, two different web contexts:

| Portal | Web Context | Path | Purpose |
|---|---|---|---|
| Customer Portal | `/portal/customer/**` | Patient-facing: appointment status, confirmation, reschedule |
| Admin Portal | `/portal/admin/**` | Provider/admin: dashboards, agent status, analytics, scenarios |

### Build Process
- Angular apps live in `app-web/src/main/frontend/customer/` and `app-web/src/main/frontend/admin/`
- Maven build runs `ng build` and copies output to `src/main/resources/static/portal/customer/` and `static/portal/admin/`
- Spring Boot serves static files — no separate deployment

### Spring Boot Web Config
```java
// Serve Angular routes — forward to index.html for SPA routing
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // /portal/customer/** → customer/index.html
    // /portal/admin/** → admin/index.html
}
```

---

## API Documentation

### Swagger / OpenAPI
- **springdoc-openapi** with Swagger UI enabled
- Available at `/swagger-ui.html`
- All REST endpoints documented with `@Operation`, `@ApiResponse` annotations
- Grouped by agent: PCA APIs, COA APIs, Scenario APIs, Analytics APIs

---

## Amazon Bedrock Integration

### Module: app-cloud-aws-bedrock
Dedicated module for all Bedrock interactions — separate from generic AI module.

```
app-cloud-aws-bedrock/
  src/main/java/com/agenticcare/cloud/aws/bedrock/
    config/
      BedrockClientConfig.java          — Spring AI Bedrock auto-config
    chat/
      BedrockChatService.java           — ChatClient for C_p classification prompts
      PromptTemplateService.java        — Manages structured prompts for agents
    knowledgebase/
      BedrockKnowledgeBaseService.java  — RAG: retrieve patient interaction history
      KnowledgeBaseIngestionService.java — Ingest patient data into KB
    embedding/
      BedrockEmbeddingService.java      — Generate vector embeddings for patients
    agents/
      BedrockAgentService.java          — Amazon Bedrock Agents (if using managed agents)
```

### How Agents Use Bedrock
| Agent | Bedrock Capability | What For |
|---|---|---|
| PCA | ChatClient (foundation models) | Classify patient context state C_p from structured prompt |
| PCA | Knowledge Bases (RAG) | Retrieve similar patient interaction patterns |
| PCA | Embeddings | Vector embeddings for patient context similarity |
| COA | ChatClient | Natural language understanding of patient voice/text responses |
| ACA | ChatClient | Generate human-readable audit rationale |

### Spring AI Configuration
```yaml
# application-aws.yml
spring:
  ai:
    bedrock:
      aws:
        region: us-east-1
      chat:
        enabled: true
      embedding:
        enabled: true
      knowledge-base:
        enabled: true
```

---

## DevOps / Local Development

### Directory Structure
```
DevOps/
  Local/
    docker-all-up.sh                    — Start all docker-compose files
    docker-all-down.sh                  — Stop all
    docker-all-status.sh                — Status check
    Postgres/
      docker-compose.yml                — PostgreSQL 16 + pgVector
    MongoDB/
      docker-compose.yml                — MongoDB 7 + Atlas Vector
    Redis/
      docker-compose.yml                — Redis 7
    Kafka/
      docker-compose.yml                — Kafka + Zookeeper (or KRaft)
    Prometheus/
      docker-compose.yml                — Prometheus
      prometheus.yml                    — Scrape config for Spring Boot
    Grafana/
      docker-compose.yml                — Grafana
      dashboards/                       — Pre-built agent dashboards
    Jaeger/
      docker-compose.yml                — Jaeger all-in-one
    Kibana/
      docker-compose.yml                — Kibana (connects to OpenSearch)
    OpenSearch/
      docker-compose.yml                — OpenSearch (local equivalent of AWS)
    Temporal/
      docker-compose.yml                — Temporal server + UI
    VectorDB/
      docker-compose.yml                — Qdrant or Milvus for dedicated vector search
```

### Shell Scripts
```bash
# docker-all-up.sh
#!/bin/bash
for dir in Postgres MongoDB Redis Kafka OpenSearch Prometheus Grafana Jaeger Kibana Temporal VectorDB; do
  echo "Starting $dir..."
  docker-compose -f DevOps/Local/$dir/docker-compose.yml up -d
done
echo "All services started."

# docker-all-down.sh — same pattern with 'down'
# docker-all-status.sh — same pattern with 'ps'
```

---

## AWS Deployment (EKS)

### Separate Pods per Component
| Pod | Contains | Scaling |
|---|---|---|
| app-web | Spring Boot (REST APIs + Angular portals) | Horizontal (2+ replicas) |
| app-agent-patient | PCA Lambda or EKS pod | Event-driven |
| app-agent-communication | COA Lambda or EKS pod | Event-driven |
| app-agent-provider | PSA Lambda or EKS pod | Event-driven |
| app-agent-resource | RRA Lambda or EKS pod | Event-driven |
| app-agent-audit | ACA Lambda or EKS pod | Event-driven |
| temporal-worker | Temporal workflow worker | Auto-scale |

### AWS Native Services Replace Local Docker
| Local (Docker) | AWS (EKS) |
|---|---|
| PostgreSQL + pgVector | Amazon RDS PostgreSQL + pgVector OR Amazon Aurora |
| MongoDB | Amazon DocumentDB |
| Redis | Amazon ElastiCache |
| Kafka | Amazon MSK |
| OpenSearch | Amazon OpenSearch Service |
| Prometheus/Grafana | Amazon CloudWatch + Managed Grafana |
| Jaeger | AWS X-Ray |
| Temporal | Self-hosted on EKS or AWS Step Functions |
| Qdrant/Milvus | Amazon OpenSearch k-NN + Bedrock Knowledge Bases |

---

## Spring Profiles Summary

| Profile | Activated By | What Changes |
|---|---|---|
| (default) | Nothing | H2 database, embedded Temporal, in-memory vector |
| `local-postgres` | `-Dspring.profiles.active=local-postgres` | PostgreSQL + pgVector |
| `local-mongo` | `-Dspring.profiles.active=local-mongo` | MongoDB |
| `local-full` | `-Dspring.profiles.active=local-full` | All Docker services (Postgres, Mongo, Kafka, Redis, etc.) |
| `aws-dev` | `-Dspring.profiles.active=aws-dev` | AWS services (Bedrock, OpenSearch, Connect, etc.) |
| `aws-prod` | `-Dspring.profiles.active=aws-prod` | Production AWS config |

---

## Module List (Updated)

```
app-common                     — Shared interfaces, DTOs, constants, enums
app-dao                        — Data access: JPA (H2/Postgres), MongoDB, OpenSearch, vector DB
app-core                       — Business logic, service facades
app-web                        — REST controllers + Angular portals (customer + admin)
app-wfs                        — Workflow definitions (Temporal / generic)
app-cloud-aws-wfs              — AWS Step Functions + EventBridge facades
app-cloud-aws-ai               — Generic AWS AI facades
app-cloud-aws-bedrock           — Amazon Bedrock: chat, KB, embeddings, agents
app-cloud-aws-ml               — AWS ML facades (SageMaker if needed)
app-cloud-aws-agents           — AWS agent facades (Connect, Bedrock Agents)
app-agent-patient              — PCA: Patient Context Agent
app-agent-communication        — COA: Communication Orchestration Agent
app-agent-provider             — PSA: Provider Schedule Agent
app-agent-resource             — RRA: Resource Reallocation Agent
app-agent-audit                — ACA: Audit & Compliance Agent
```
