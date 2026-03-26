# AgenticSmartCareScheduler — Development Plan

> Coding conventions, module structure, and naming rules are in **CodingConventions.md**.
> Architecture, agents, and dataset details are in **CLAUDE.md**.

---

## Problem Statement

10-30% of outpatient appointments globally result in no-shows. The root cause is **communication-context mismatch**: static SMS reminders ignore the patient's real-time context (driving, in a meeting, asleep), so the outreach channel fails to engage the patient at the moment of contact.

## Solution: Agentic Context-Aware Outreach

A multi-agent system that assesses each patient's real-time context and selects the optimal outreach channel:

| Agent | Role | Key Action |
|---|---|---|
| **PCA** (Patient Context Agent) | Classify C_p state, predict R_p risk | REACHABLE_MOBILE, REACHABLE_STATIONARY, or UNREACHABLE |
| **COA** (Communication Orchestration Agent) | Select and execute outreach channel | IVR call (mobile), SMS deep-link (stationary), callback (unreachable) |
| **RRA** (Resource Reallocation Agent) | Fill predicted no-show slots | Queries waitlist, triggers COA for replacement patients |
| **PSA** (Provider Scheduling Agent) | Optimize provider schedules | Double-booking recommendations for high-risk slots |
| **ACA** (Audit & Compliance Agent) | Ensure HIPAA compliance | Reviews all outreach for consent, frequency limits |

---

## Reproducible Dev Environment Setup

**New laptop → clone → 3 commands → fully running system:**

```bash
# 1. Install all dependencies (Java, Angular, Python)
npm run install:all

# 2. Start infrastructure (Postgres, Airflow)
npm run docker:analytics:up

# 3. Build and start the app + message broker
npm run start

# 4. Seed foundational data (datasets, workflows) via admin API
npm run setup:seed-all
```

### What each command does:

| Command | What it installs/starts |
|---|---|
| `npm run install:all` | Angular (customer + admin portals), Python conda env + pip deps (Airflow, XGBoost, pandas, etc.) |
| `npm run docker:analytics:up` | Postgres (port 5432), Airflow webserver (port 8082), Airflow scheduler, Airflow Postgres (port 5433) |
| `npm run start` | Builds Angular UIs → `mvn install` → Spring Boot (port 8080) + Message Broker (port 8081) concurrently |

### Foundational data seeding (via Admin Dashboard UI):

On first login, the **Admin Dashboard** shows an alert banner:
> ⚠ Foundational data not configured. Seed default datasets, workflow definitions, and engines to get started.

Each alert has a **"Seed Now"** button that calls the corresponding API:

| Alert | Seed Button | API Endpoint |
|---|---|---|
| No datasets registered | Seed Default Datasets | `POST /smart-care/api/admin/v1/analytics/datasets/seed-defaults` |
| No workflow definitions | Seed Default Workflows | `POST /smart-care/api/admin/v1/workflow-definitions/seed-defaults` |
| No workflow engines | Seed Default Engine | `POST /smart-care/api/admin/v1/workflow-engines/seed-defaults` |

Alerts disappear once data is seeded. Message broker seeds sample messages automatically on startup via `DataSeeder.java`.

---

## Kaggle Dataset — Dual Role

The **Medical Appointment No-Show dataset** (110,527 records, Kaggle) serves two purposes:

1. **Train the PCA risk model** — XGBoost learns R_p (no-show probability) from lead time, age, chronic conditions, SMS receipt, day-of-week
2. **Simulate agent decisions** — Each record gets a C_p state based on appointment time, then PCA→COA pipeline runs to answer: *"What outreach channel would the agents have selected for each of these 110K real appointments?"*

This produces the paper's core evidence: channel distribution stats and projected no-show reduction vs SMS-only baseline.

---

## Five Agentic Workflows (Airflow DAGs)

| DAG | Agent Pipeline | What It Does | Paper Section |
|---|---|---|---|
| `patient_outreach_orchestration` | PCA → COA | Assess context, select channel, execute outreach | V. Methodology, VII. Results |
| `smart_appointment_confirmation` | COA | Live IVR/SMS interaction, LLM response parsing, status update | V. Methodology — Section B |
| `waitlist_slot_fulfillment` | PCA → RRA → COA | Predict no-show, rank waitlist candidates, offer slot | VII. Results |
| `provider_schedule_optimization` | PCA → RRA → PSA | Analyze no-show patterns, recommend schedule changes | VII. Results |
| `outreach_compliance_audit` | ACA | HIPAA compliance review, audit summary generation | VII. Discussion |

DAGs live in `DataManagement/Airflow/dags/<workflow_name>/` (product code, versioned).

### Workflow Definitions (seeded in admin UI)

| Workflow | Agent Pipeline | AWS Services | Agentic AI Tech Stack | Paper Section |
|---|---|---|---|---|
| **Patient Outreach Orchestration** | PCA → COA (Full Pipeline) | Bedrock Agents, Connect, SNS, EventBridge, Lambda | Spring AI ChatClient, Bedrock Claude, @Tool for Connect/SNS | V. Methodology, VII. Results |
| **Smart Appointment Confirmation** | COA | Bedrock Agents, Connect Contact Flows, SNS, Lambda | Spring AI ChatClient, Bedrock Claude, @Tool for response parsing | V. Methodology — Section B |
| **Waitlist Slot Fulfillment** | PCA → RRA → COA (Cross-Agent) | Bedrock Agents, HealthLake (FHIR R4), Step Functions, Connect | Spring AI ChatClient, Bedrock Claude, @Tool for HealthLake/StepFn | VII. Results |
| **Provider Schedule Optimization** | PCA → RRA → PSA | Bedrock Agents, HealthLake (FHIR R4), EventBridge, Lambda | Spring AI ChatClient, Bedrock Claude, @Tool for FHIR queries | VII. Results |
| **Outreach Compliance Audit** | ACA | Bedrock Agents, OpenSearch, S3, CloudWatch | Spring AI ChatClient, Bedrock Claude, OpenSearch vector search | VII. Discussion |

---

## Phase 1: PCA Risk Model — **DONE**

### 1A. Dataset — **DONE**
- ✅ Kaggle Medical Appointment No-Show dataset (110,527 records)
- Saved to `DataManagement/DataSets/`
- No-show rate: 28.5%

### 1B. XGBoost Training — **DONE**
- 5-fold stratified cross-validation
- **Results:** F1=0.4541, AUC=0.6106, Precision=0.6065, Recall=0.5123
- Script: `DataManagement/Analytics/ML/train_risk_model.py`
- Outputs: `confusion_matrix.png`, `risk_model_metrics.json`

### 1C. Agent Decision Simulation on Historical Data — **PENDING**
- Assign C_p to each record based on appointment time:
  - Weekday 7-9 AM, 4-7 PM → REACHABLE_MOBILE → COA selects Voice IVR
  - Weekday 9 AM-4 PM → REACHABLE_STATIONARY → COA selects SMS Deep-Link
  - No SMS_received + R_p > 0.5 → UNREACHABLE → COA selects Callback
- Script to create: `DataManagement/Analytics/ML/agent_decision_simulator.py`
- Outputs: `context_state_distribution.json`, `channel_distribution.png` (Fig. 5)

---

## Phase 2: Platform & Portals — **DONE**

### Spring Boot Multi-Module — **DONE**
- ✅ 23 modules in reactor (app-common, app-dao, app-core, app-domain-admin/customer, 5 agent modules, cloud integrations, message broker, app-web)
- ✅ Spring AI + Bedrock starter dependencies
- ✅ H2 in-memory (dev), Postgres (docker)

### Agent Modules — SCAFFOLDING ONLY
- 5 agent modules (PCA, COA, RRA, PSA, ACA) have POM + dependencies, no Java implementation yet

### Admin Portal — **DONE**
- ✅ Dashboard (6 metric cards, agent pipeline, channel distribution bars)
- ✅ Datasets (seed defaults, ingest, instances)
- ✅ Workflows (Engines CRUD, Definitions with engine mapping, Runs with submit/track)
- ✅ Messages (topics listing, message detail with pagination)
- ✅ Secrets (AWS profile/credentials management)
- Stub pages: Agents, Scenarios, Analytics, Digital Twin, Audit Log

### Customer Portal — **DONE**
- ✅ Dashboard with outreach history, AI context agent, status cards (mock data)

### Message Broker — **DONE**
- ✅ Port 8081, auto-seeds user_profile_event messages
- ✅ Topics listing, message detail, publish/consume/acknowledge API

### Workflow Engine — **DONE**
- ✅ Workflow Engines, Definitions (5 outreach workflows), Runs
- ✅ Docker Compose for Airflow (port 8082)
- ✅ 5 DAGs in `DataManagement/Airflow/dags/`
- ✅ Dockerfile for EKS/MWAA deployment

### CI/CD — **DONE**
- ✅ GitHub Actions: build-java, build-customer-portal, build-admin-portal

---

## Phase 3: Paper Evidence Generation — **IN PROGRESS**

### Activity Timeline (Mar 25-31)

| # | Activity | Time | Status |
|---|---|---|---|
| 1 | Create + run agent_decision_simulator.py (C_p + channel distribution) | 1.5 hr | **NEXT** |
| 2 | Generate channel_distribution.png (Fig. 5) | 0.5 hr | PENDING |
| 3 | Generate baseline comparison (multi-channel vs SMS-only) | 1 hr | PENDING |
| 4 | Take portal screenshots (Fig. 2, Fig. 3) | 0.5 hr | PENDING |
| 5 | Update paper V06 → V07 with real metrics + figures | 2 hr | PENDING |
| 6 | IEEE template formatting | 2 hr | PENDING |
| 7 | Final review + submit via EDAS | 1 hr | PENDING |

**Minimum viable submission (~5 hrs):** Activities 1, 2, 4, 5, 7

### Key Dates
| Date | Milestone |
|---|---|
| Mar 25-27 | Agent simulation + charts (Activities 1-3) |
| Mar 28-29 | Paper V07 with real metrics and figures (Activities 4-5) |
| Mar 30 | IEEE formatting and final review (Activity 6) |
| **Mar 31** | **Submit via EDAS (FIRM deadline)** |
| Apr 20 | Notification of acceptance |
| Jun 23-26 | Conference in Vilamoura, Portugal |

---

## Technology Stack

| Layer | Technology | Outreach Role |
|---|---|---|
| Core Framework | Spring Boot 3.x + Spring AI | Agent service endpoints |
| AI/LLM | Amazon Bedrock Agents, Bedrock Claude, Knowledge Bases | PCA context classification, COA channel reasoning |
| Communication | Amazon Connect, Amazon SNS | COA outreach execution (IVR, SMS) |
| Data Platform | Amazon OpenSearch (vector search) | Digital twin, outreach event logs |
| Healthcare | AWS HealthLake (FHIR R4) | RRA waitlist/schedule queries |
| Orchestration | AWS Step Functions, EventBridge | Agent pipeline coordination |
| Workflow Engine | Apache Airflow | 5 agentic outreach DAGs |
| Compute | AWS Lambda (SnapStart) | Serverless agent execution |
| IaC | AWS CloudFormation | Infrastructure templates |
| CI/CD | GitHub Actions | 3-job build pipeline |

---

## Success Criteria for Paper Submission

| # | Criterion | Status |
|---|---|---|
| 1 | Real F1 and AUC from PCA risk model on 110K records | **DONE** (F1=0.4541, AUC=0.6106) |
| 2 | Real C_p distribution (what agents would classify) | PENDING |
| 3 | Real channel selection stats (IVR / SMS / callback split) | PENDING |
| 4 | Channel distribution visualization (Fig. 5) | PENDING |
| 5 | Portal screenshots (Fig. 2, Fig. 3) | PENDING |
| 6 | GitHub Actions CI/CD evidence | **DONE** |
| 7 | Paper V07 with all real numbers and figures | PENDING |
| 8 | Submitted via EDAS by Mar 31 | PENDING |

---

## Post-Submission Backlog

Not required for paper, can be completed after submission:

- Agent Java implementations (PCA, COA, RRA, PSA, ACA)
- AWS CloudFormation templates
- Deployment workflows (bootstrap-infra, deploy-agents, run-scenario, shutdown-infra)
- Admin portal stub pages (Agents, Scenarios, Analytics, Digital Twin, Audit Log)
- Live scenario execution (3 outreach scenarios against deployed agents)
- End-to-end AWS deployment (Lambda + Connect + SNS + OpenSearch)
