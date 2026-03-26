# AgenticSmartCareScheduler — Development Plan

> Coding conventions, module structure, and naming rules are in **CodingConventions.md**.
> Architecture, agents, and dataset details are in **CLAUDE.md**.

---

## Mar 31, 2026 — Paper Submission Sprint (12-16 hours)

### Goal
Submit IEEE ICTS4eHealth 2026 paper (V07) with real implementation evidence: XGBoost metrics, UI screenshots, channel distribution stats, and working code repository.

### Paper Figures Strategy
| Figure | Source | What it shows | Paper section |
|---|---|---|---|
| Fig. 1 | Architecture diagram (already created) | Full system architecture with AWS services | IV. System Architecture |
| Fig. 2 | Customer Portal screenshot | Patient dashboard: outreach history, AI context agent, status cards | VII. Results — demonstrates patient-facing capability |
| Fig. 3 | Admin Portal screenshot | Agent pipeline, channel distribution bars, 6 metric cards | VII. Results — demonstrates operational dashboard |
| Fig. 4 | XGBoost ROC curve / confusion matrix | Real ML model performance | VI. Evaluation |
| Fig. 5 | Channel distribution chart | IVR / SMS / callback split across C_p states | VII. Results — core contribution visualization |

> **Why screenshots help:** Reviewers see a working system, not just a paper architecture. The admin dashboard directly visualizes the agent pipeline (PCA→COA→PSA→RRA→ACA) and channel distribution — the core claims of the paper.

### Activity Plan (12-16 hours)

| # | Activity | Time | Output | Paper Impact | Status |
|---|---|---|---|---|---|
| **DAY 1 — Implementation (8 hrs)** | | | | | |
| 1 | XGBoost model training on Kaggle dataset | 1.5 hr | F1=0.4541, AUC=0.6106, confusion matrix PNG | Replace qualitative claims with real numbers | **DONE** |
| 2 | Context state (C_p) simulation on 110K records | 1 hr | C_p distribution stats + channel selection breakdown | Fig. 5 — channel distribution chart | PENDING |
| 3 | Wire admin dashboard to real data (or realistic mock) | 1.5 hr | Admin portal showing real metrics, channel bars | Fig. 3 — admin screenshot | **DONE** (mock data, visually complete) |
| 4 | Wire customer dashboard to realistic patient scenarios | 1 hr | Customer portal showing outreach history, context agent | Fig. 2 — customer screenshot | **DONE** (mock data, visually complete) |
| 5 | Generate analytics charts (Python matplotlib) | 1 hr | ROC curve, confusion matrix, channel distribution PNG | Fig. 4, Fig. 5 | **PARTIAL** (confusion matrix done, channel dist pending) |
| 6 | Take high-quality screenshots of both portals | 0.5 hr | PNG files for paper | Fig. 2, Fig. 3 | PENDING |
| 7 | Commit all code, push, verify GitHub Actions CI passes | 0.5 hr | Green build, public repo link | Paper credibility | **DONE** |
| **DAY 2 — Paper Finalization (4-8 hrs)** | | | | | |
| 8 | Update V06 → V07 with real metrics from XGBoost | 1 hr | Replace "evidence-based" with actual F1, AUC | Abstract, VI, VII | PENDING |
| 9 | Embed Fig. 2-5 into paper, update figure references | 1 hr | Paper with 5 figures | Throughout | PENDING |
| 10 | Update Results/Discussion with real channel distribution | 1 hr | Table IV with real numbers instead of capabilities-only | VII | PENDING |
| 11 | Convert to IEEE double-column LaTeX/Word template | 2 hr | Properly formatted IEEE submission | Final format | PENDING |
| 12 | Final review: page count (≤7), references, figures fit | 1 hr | Submission-ready PDF | — | PENDING |
| 13 | Submit via EDAS | 0.5 hr | Confirmation email | Done | PENDING |

### Priority Order (if time is short)
If only 12 hours available, drop in this order:
1. ~~Wire customer dashboard to real data~~ — use existing mock UI, take screenshot as-is
2. ~~Generate analytics charts~~ — use table format in paper instead of chart figures
3. ~~Convert to LaTeX~~ — submit Word doc (EDAS accepts both)

**Minimum viable submission (8 hrs):**
- XGBoost model → real F1/AUC (Activity 1)
- C_p distribution stats (Activity 2)
- Screenshots of existing portals (Activity 6)
- Update paper V07 with real numbers (Activity 8-10)
- Submit Word doc via EDAS (Activity 13)

### Key Dates
| Date | Milestone |
|---|---|
| Mar 25-29 | Implementation sprint (Activities 1-7) |
| Mar 29-30 | Paper finalization (Activities 8-12) |
| Mar 31 | Submit via EDAS (FIRM deadline) |
| Apr 20 | Notification of acceptance |
| Jun 23-26 | Conference in Vilamoura, Portugal |

---

---

## Technology Stack

| Layer | Technology |
|---|---|
| Core Framework | Spring Boot 3.x + Spring AI |
| AI/LLM | Amazon Bedrock Ecosystem, Bedrock Knowledge Bases, Spring AI ChatClient |
| ML Training | Python 3.12, XGBoost, scikit-learn |
| Communication | Amazon Connect, Amazon SNS |
| Data Platform | Amazon OpenSearch (4 indices + vector search) |
| Healthcare Integration | AWS HealthLake (FHIR R4) |
| Orchestration | AWS Step Functions, Amazon EventBridge |
| Compute | AWS Lambda (Spring Boot via GraalVM native or SnapStart) |
| IaC | AWS CloudFormation |
| CI/CD | GitHub Actions |
| Analytics | Python (matplotlib, pandas) |

---

## Phase 1: Python ML Foundation (1 hour) — **DONE**

### 1A. Dataset — **DONE**
- ✅ Kaggle Medical Appointment No-Show dataset (110,527 records)
- Saved to `DataManagement/DataSets/`
- EDA script: no-show rate (28.5%), feature distributions, lead time analysis

### 1B. XGBoost Risk Model — **DONE**
- Features: lead time, age, prior no-show count, chronic conditions, SMS receipt, day-of-week
- 5-fold stratified cross-validation
- **Output:** F1=0.4541, AUC=0.6106, Precision=0.6065, Recall=0.5123
- Confusion matrix PNG → `DataManagement/Analytics/output/confusion_matrix.png`
- Metrics JSON → `DataManagement/Analytics/output/risk_model_metrics.json`
- Training script → `DataManagement/Analytics/ML/train_risk_model.py`

### 1C. Context State Simulation — PENDING
- Assign C_p to each record based on appointment time:
  - Weekday 7-9 AM, 4-7 PM → REACHABLE_MOBILE
  - Weekday 9 AM-4 PM → REACHABLE_STATIONARY
  - No SMS_received + R_p > 0.5 → UNREACHABLE
- **Output:** C_p distribution stats, channel selection distribution
- Script exists: `DataManagement/Analytics/ML/context_state_simulator.py`

---

## Phase 2: Spring Boot Multi-Module Setup (1.5 hours) — **PARTIAL**

### 2A. Maven Multi-Module Scaffolding — **DONE**
- ✅ Root `pom.xml` with 23 modules in reactor
- ✅ All modules created: app-common, app-dao, app-core, app-domain-admin, app-domain-customer, app-wfs, app-agents (5 agents), app-cloud-integration (aws-wfs, aws-ai, aws-bedrock, aws-ml, aws-agents), app-extensions (message-broker), app-web
- ✅ Spring AI + Bedrock starter dependencies
- ✅ Application profiles: default (H2)

### 2B. app-common — **DONE**
- ✅ Enums: `ContextState`, `SlotStatus`, `OutreachChannel`
- ✅ Constants, DTOs, Interfaces defined

### 2C. app-agent-patient (PCA) — SCAFFOLDING ONLY
- Module exists with POM and dependencies on app-common, app-core, app-cloud-aws-bedrock
- ❌ No Java implementation — source directories empty

### 2D. app-agent-communication (COA) — SCAFFOLDING ONLY
- Module exists with POM
- ❌ No Java implementation — source directories empty

### 2E. app-core + app-dao — **DONE**
- ✅ DatasetService with seed/ingest pipeline
- ✅ SecuritySettingsService for AWS credential management
- ✅ JPA repositories for datasets, instances, security settings

### 2F. app-web — **DONE**
- ✅ REST endpoints: AdminDatasetController, AdminSecuritySettingsController, AdminAwsProfilesController
- ✅ Both Angular portals (admin + customer) built and served as static assets
- ✅ Spring Boot 3.4.6 on port 8080

---

## Phase 3: AWS Infrastructure (1 hour) — **PARTIAL**

### 3A. CloudFormation Templates — NOT STARTED
```
infra/    — directory exists but empty (no CloudFormation templates yet)
```

### 3B. GitHub Actions Workflows — **DONE (CI only)**
```
.github/workflows/
  ci-build.yml               — ✅ 3-job CI: build-java, build-customer-portal, build-admin-portal
  bootstrap-infra.yml        — ❌ not created
  deploy-agents.yml          — ❌ not created
  run-scenario.yml           — ❌ not created
  extract-analytics.yml      — ❌ not created
  shutdown-infra.yml         — ❌ not created
```

### 3C. Bootstrap Sequence — NOT STARTED

---

## Phase 4: Scenario Execution (1 hour) — NOT STARTED

### Three Focused Scenarios — PENDING (requires agent implementations)

**Scenario 1: High-Risk Mobile Patient**
- Filter: R_p > 0.65, appointment 4-6 PM weekday
- Expected: C_p = REACHABLE_MOBILE → COA selects VOICE_IVR

**Scenario 2: Low-Risk Stationary Patient**
- Filter: R_p < 0.3, appointment 10 AM-2 PM weekday
- Expected: C_p = REACHABLE_STATIONARY → COA selects SMS_DEEPLINK

**Scenario 3: Unreachable Patient**
- Filter: no SMS_received, R_p > 0.5
- Expected: C_p = UNREACHABLE → COA selects CALLBACK

### Execution — NOT STARTED
### Metrics — NOT STARTED

---

## Phase 5: Analytics & Paper Output (30 min) — **PARTIAL**

### Scripts
```
DataManagement/Analytics/ML/
  train_risk_model.py           — ✅ DONE: F1, AUC, confusion matrix
  context_state_simulator.py    — ✅ EXISTS: C_p breakdown, channel stats (needs run)
  channel_distribution.py       — ❌ not created
  baseline_comparison.py        — ❌ not created
  scenario_results.py           — ❌ not created
```

### Outputs → Feed back into paper V07
- ✅ `DataManagement/Analytics/output/risk_model_metrics.json` — DONE
- ✅ `DataManagement/Analytics/output/confusion_matrix.png` — DONE
- ❌ `channel_distribution.png` — PENDING
- ❌ `baseline_comparison.png` — PENDING
- ❌ `scenario_summary.md` — PENDING

---

## Success Criteria

After 5 hours:
1. ✅ Real F1 and AUC from XGBoost on 110K dataset — **DONE** (F1=0.4541, AUC=0.6106)
2. ❌ Real C_p distribution across the dataset — PENDING
3. ❌ Real channel selection stats (IVR / SMS / callback split) — PENDING
4. ❌ Baseline comparison with real numbers — PENDING
5. ❌ Deployed AWS infrastructure (OpenSearch, Lambda, Step Functions) — NOT STARTED
6. ✅ GitHub Actions CI/CD pipeline evidence — **DONE**
7. ❌ All outputs ready to update paper from V06 → V07 — IN PROGRESS

---

## TODO — Next Session

### Admin Portal — Messages — **DONE**
- ✅ Left nav: Messages → Topics listing with pending/processing/completed/failed counts
- ✅ Click topic → messages table with sequence, key, status, payload, context, timestamps
- ✅ Pagination on message detail page
- ✅ Message broker on port 8081 with DataSeeder (7 user_profile_event messages)
- ✅ `npm run start` launches both app (8080) and broker (8081) concurrently

### Admin Portal — Workflow Engines — **DONE**
- ✅ Workflow Engines CRUD (Airflow, EMR, Databricks, Step Functions)
- ✅ Workflow Definitions with engine mapping (many-to-many)
- ✅ Workflow Runs with submit, status tracking, dataset instance selection
- ✅ Sidebar: Workflows section (Engines, Definitions, Runs)
- ✅ Docker Compose for Apache Airflow (port 8082)
- ✅ DAGs in `DataManagement/Airflow/dags/` (product code, not infra)
- ✅ Dockerfile for EKS/MWAA deployment

### Product Workflow Definitions

| Workflow | Agent Pipeline | AWS Services | Agentic AI Tech Stack | Paper Section |
|---|---|---|---|---|
| **Patient Outreach Orchestration** | PCA → COA (Full Pipeline) | Bedrock Agents, Connect, SNS, EventBridge, Lambda | Spring AI ChatClient, Bedrock Claude, @Tool for Connect/SNS | V. Methodology, VII. Results |
| **Smart Appointment Confirmation** | COA (Communication Orchestration Agent) | Bedrock Agents, Connect Contact Flows, SNS, Lambda | Spring AI ChatClient, Bedrock Claude, @Tool for response parsing | V. Methodology — Section B |
| **Waitlist Slot Fulfillment** | PCA → RRA → COA (Cross-Agent) | Bedrock Agents, HealthLake (FHIR R4), Step Functions, Connect | Spring AI ChatClient, Bedrock Claude, @Tool for HealthLake/StepFn | VII. Results |
| **Provider Schedule Optimization** | PCA → RRA → PSA (Provider Scheduling) | Bedrock Agents, HealthLake (FHIR R4), EventBridge, Lambda | Spring AI ChatClient, Bedrock Claude, @Tool for FHIR queries | VII. Results |
| **Outreach Compliance Audit** | ACA (Audit & Compliance Agent) | Bedrock Agents, OpenSearch, S3, CloudWatch | Spring AI ChatClient, Bedrock Claude, OpenSearch vector search | VII. Discussion |

### Admin Portal — Stub Pages to Implement — PENDING
- Agents page (currently "coming soon")
- Analytics page (currently "coming soon")
- Audit Log page (currently "coming soon")
- Digital Twin page (currently "coming soon")
- Scenarios page (currently "coming soon")

### Paper V07 — PENDING
- Update with real XGBoost metrics (F1=0.4541, AUC=0.6106)
- Take screenshots of admin + customer portals
- Generate confusion matrix figure for paper
- Run C_p context state simulation for channel distribution stats
