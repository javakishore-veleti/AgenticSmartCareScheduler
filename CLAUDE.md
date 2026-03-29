# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## IMPORTANT — Read These Files First
Before scanning the codebase, read these files for full context:
1. **CLAUDE.md** (this file) — build commands, architecture overview, key patterns
2. **CodingConventions.md** — ExecCtx/ReqDto/RespDto pattern, module structure, naming, Spring AI patterns
3. **ArchitectureDecisions.md** — databases, observability, DevOps/Local, Angular portals, Bedrock, EKS, profiles
4. **DevelopmentPlan.md** — 5-phase plan, scenarios, outputs, current task statuses

## Build & Run Commands

All commands are defined in `package.json` and run from the repo root.

### Quick Start (new machine)
```bash
npm run install:all        # Angular UIs + Python conda env (smartcare-analytics)
npm run docker:analytics:up # Postgres (5432) + Airflow (8082)
npm run start              # Build UIs → mvn install → Spring Boot (8080) + Message Broker (8081)
npm run setup:seed-all     # Seed datasets, workflows, engines via admin API
```

### Build
```bash
npm run build:java         # mvn clean install -DskipTests
npm run build:ui           # ng build both portals → copies to static/portal/{customer,admin}/
npm run build:all          # build:ui then build:java
npm run clean              # mvn clean + rm static/portal
```

### Run
```bash
npm run start              # Full: build UIs + mvn install + Spring Boot + Message Broker (concurrently)
npm run start:spring       # Spring Boot only (port 8080) — mvn -pl app-web spring-boot:run
npm run start:broker       # Message Broker only (port 8081) — mvn -pl app-extensions/app-message-broker spring-boot:run
npm run dev                # Spring Boot + both Angular dev servers (4200/4201) — hot reload
npm run dev:admin          # Spring Boot + admin portal dev server only
npm run start:aws-integration  # Spring Boot with aws-integration profile (polls SQS)
```

### Test
```bash
npm run test:java          # mvn test (all Java modules)
npm run test:analytics     # pytest on DataManagement/ (requires conda env smartcare-analytics)
mvn test -pl app-core      # Single module test
mvn test -pl app-core -Dtest=RiskScoringServiceImplTest  # Single test class
```

### Lint
```bash
npm run lint:ui            # ng lint both portals
npm run lint:admin         # ng lint admin only
npm run lint:customer      # ng lint customer only
```

### Analytics / ML
```bash
npm run analytics:train    # XGBoost risk model training
npm run analytics:context  # Context state simulation on Kaggle dataset
```

### Docker (local infrastructure)
```bash
npm run docker:analytics:up    # Postgres + Airflow stack
npm run docker:analytics:down
npm run docker:up              # All DevOps/Local services (Postgres, Kafka, Redis, OpenSearch, etc.)
npm run docker:down
```

## Project Context

IEEE ICTS4eHealth 2026 conference paper implementation: a multi-agent AI framework that reduces healthcare appointment no-shows through context-aware communication channel selection.

**Core insight**: Static SMS reminders fail because they ignore patient context. Five agents assess real-time context and route outreach to the optimal channel (IVR / SMS deep-link / callback).

## Architecture

### Two Runtime Layers

**Java (Spring Boot 3.4, Java 17)** — Admin/customer portals, REST APIs, workflow management, DB, SQS polling.

**Python 3.12** — ML model training (XGBoost), Airflow DAGs (batch dispatch), AWS Lambda agents (Bedrock/Connect/SNS calls).

### Maven Multi-Module Structure (parent POM)

| Module | Role |
|---|---|
| `app-common` | Shared DTOs, enums, interfaces (depends on nothing) |
| `app-dao` | JPA entities, repositories (H2/Postgres) |
| `app-core` | Business logic, service facades |
| `app-domain-admin` | Admin-specific domain logic |
| `app-domain-customer` | Customer/patient domain logic |
| `app-wfs` | Workflow definitions (Temporal / generic) |
| `app-cloud-integration` | AWS cloud facades (Step Functions, SQS, Bedrock, etc.) |
| `app-agents` | 5 agent modules (PCA, COA, PSA, RRA, ACA) — scaffolding only in Java |
| `app-extensions/app-message-broker` | Standalone message broker (port 8081) |
| `app-web` | REST controllers + Angular portals (Spring Boot main class) |

Dependency flow: `app-common` ← `app-dao` ← `app-core` ← `app-domain-*` ← `app-web` (top-level assembly).

### Five Agents

| Agent | Function | Key Rule |
|---|---|---|
| **PCA** (Patient Context Agent) | Risk score R_p + context state C_p | XGBoost + Bedrock |
| **COA** (Communication Orchestration Agent) | Channel selection based on C_p | Stationary→SMS, Mobile→IVR, Unreachable→Callback |
| **PSA** (Provider Schedule Agent) | Escalation at T-90min / T-30min | |
| **RRA** (Resource Reallocation Agent) | Waitlist slot filling | |
| **ACA** (Audit & Compliance Agent) | HIPAA-compliant logging | |

Agent core logic lives in `DataManagement/SmartCareAgents/<AgentName>/core/` (Python, platform-agnostic). AWS Lambda wrappers in `DataManagement/SmartCareAgents/AWS/<AgentName>/wrapper/handler.py`.

### Angular Portals (served by Spring Boot)

Two Angular apps built into `app-web/src/main/resources/static/portal/`:
- **Admin Portal** (`/portal/admin/`) — dashboards, agent status, workflows, analytics
- **Customer Portal** (`/portal/customer/`) — patient-facing appointment status

Source: `app-web/src/main/frontend/{admin-portal,customer-portal}/`

### Key Patterns

- **ExecCtx/ReqDto/RespDto**: All service/facade methods accept a single ExecCtx wrapping a ReqDto + RespDto pair. Controllers create ExecCtx, pass to facade, return RespDto. See CodingConventions.md.
- **Service/facade methods return void** — results written to `ctx.getRespDto()`.
- **Spring profiles**: default (H2), `local-postgres`, `local-mongo`, `local-full`, `aws-dev`, `aws-prod`, `aws-integration`.

### AWS Deployment

- 14 GitHub Actions (`AWS-001-*` through `AWS-099-*`) for infrastructure create/destroy (manual dispatch)
- 10 CloudFormation templates in `infra/`
- AWS Lambda agents are Python 3.12 (not Java — cold start requirements)
- Spring Boot polls SQS queues when running with `aws-integration` profile

### Airflow DAGs

5 workflow DAGs in `DataManagement/Airflow/dags/`:
- `patient_outreach_orchestration` (PCA → COA)
- `smart_appointment_confirmation` (COA)
- `waitlist_slot_fulfillment` (PCA → RRA → COA)
- `provider_schedule_optimization` (PCA → RRA → PSA)
- `outreach_compliance_audit` (ACA)

### API Namespaces

- `/smart-care/api/admin/v1/...` — Admin portal BFF
- `/smart-care/api/customer/v1/...` — Customer portal BFF
- `/smart-care/api/provider/v1/...` — Provider BFF
- `/smart-care/api/agents/admin/v1/...` — Agents → admin domain
- Swagger UI at `/swagger-ui.html`

## Dataset

Kaggle Medical Appointment No-Show dataset (110,527 records, ~20% no-show rate) in `DataManagement/DataSets/`. Used for both XGBoost training and agent decision simulation.

## Author
Aruna Kishore Veleti, Senior Member, IEEE
