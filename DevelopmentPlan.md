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

### Workflow Run Detail Page — Dynamic Output Renderer
Each workflow definition declares an **output schema** that tells the UI how to render results:
- **Output schema per workflow**: JSON schema defining what sections (accordions) to display
- **Accordion-based layout**: each section has a title, data source path, and render type
- **Render types**: key-value pairs, table (with optional pagination), chart (bar/line/pie), log viewer, JSON tree
- **Dynamic rendering engine**: Angular component reads the schema + output data and builds the UI at runtime
- **Pagination**: configurable per section (e.g., outreach events table paginated, summary stats not)
- **Example**: Patient Outreach Orchestration output might have:
  - Accordion 1: "Run Summary" — key-value (total patients, duration, success rate)
  - Accordion 2: "Context State Distribution" — bar chart (REACHABLE_MOBILE/STATIONARY/UNREACHABLE counts)
  - Accordion 3: "Channel Selection" — pie chart (IVR/SMS/Callback split)
  - Accordion 4: "Patient-Level Results" — paginated table (patientId, C_p, channel, outcome)
  - Accordion 5: "Execution Log" — log viewer (timestamped agent actions)

### API Namespace Convention

| Path | Purpose | Who calls it |
|---|---|---|
| `/api/admin/v1/...` | Admin portal BFF | Admin Angular UI |
| `/api/customer/v1/...` | Customer/patient BFF | Customer Angular UI, SMS deep-links |
| `/api/provider/v1/...` | Provider BFF | Provider mobile app |
| `/api/agents/admin/v1/...` | Agents → admin domain | Airflow DAGs (workflow status updates) |
| `/api/agents/customer/v1/...` | Agents → customer domain | Airflow DAGs (outreach action records) |

### Simulation Framing (for Paper)

Each Kaggle appointment record is treated as an incoming event — as if triggered by a patient booking or provider scheduling system. The originating mechanism (mobile app, EHR, provider portal) is outside paper scope. The paper focuses on what happens AFTER the event arrives: PCA assesses, COA decides, system acts.

**Patient signal APIs** (implemented):
- `GET /api/customer/v1/appointment-signals/respond?p={patientId}&a={appointmentId}&r={response}` — SMS deep-link click (on_my_way, late, cancel)
- `POST /api/provider/v1/appointment-signals/patient-arrived` — provider marks arrival
- `POST /api/provider/v1/appointment-signals/patient-noshow` — provider marks no-show
- `POST /api/provider/v1/appointment-signals/open-slot` — provider releases slot

**PCA guard logic** (before outreach):
- Check signals: if PATIENT_ON_MY_WAY or PATIENT_CONFIRMED or PROVIDER_PATIENT_ARRIVED → SKIP outreach
- If PATIENT_CANCEL → release slot to RRA agent
- If no signal + high risk → PROCEED with outreach

---

## AWS Deployment Architecture (Design Notes)

### Dual Environment: Local Airflow vs AWS

The system supports two runtime environments for the real-time outreach workflow:

| Component | Local (Airflow) | AWS (Step Functions) |
|---|---|---|
| Orchestrator | Apache Airflow DAGs | AWS Step Functions state machine |
| Agent compute | Python tasks in Airflow | AWS Lambda functions |
| LLM reasoning | Simulated (placeholder) | Amazon Bedrock (Claude) |
| Communication | Simulated responses | Amazon Connect (IVR, SMS, Callback) |
| Event bus | Message Broker (H2/Postgres) | Amazon EventBridge + SQS |
| Dataset storage | Local filesystem | Amazon S3 |
| Spring Boot integration | Direct HTTP (localhost) | SQS polling (Spring Boot polls AWS queues) |

### Admin UI: Workflow Engine Selection

When user selects "Real-Time Outreach Simulation" and picks **AWS Step Functions** as the engine (instead of local Airflow):
- UI shows S3 dataset location (pre-filled from GH Actions config)
- Radio button: "Create new folder in bucket" or "Reuse existing pre-defined folder"
- On submit: Spring Boot triggers AWS Step Functions execution (not Airflow)

### AWS Deployment Flow

**Step 1: GitHub Actions create AWS infrastructure**
GH Actions naming: `AWS-001-`, `AWS-002-`, etc. in logical build-up order.

```
AWS-001-Validate-VPC.yml           — validates VPC ID from secrets
AWS-002-Create-S3-Bucket.yml       — dataset storage bucket + folders
AWS-003-Create-SNS-Topics.yml      — appointment events, outreach results, admin alerts
AWS-004-Create-SQS-Queues.yml      — queues for Spring Boot to poll from laptop
AWS-005-Create-EventBridge.yml     — event bus + rules → route to SQS + Lambda
AWS-006-Create-OpenSearch.yml      — digital twin DT(t) with 4 indices
AWS-007-Create-Bedrock-Agent.yml   — PCA agent with Claude foundation model
AWS-008-Create-Lambda-Agents.yml   — 5 Lambda functions (PCA, COA, PSA, RRA, ACA)
AWS-009-Create-StepFunctions.yml   — orchestrator state machine (PCA→COA→PSA→RRA→ACA)
AWS-010-Create-Connect.yml         — Connect instance + contact flows (IVR, SMS, callback)
AWS-011-Create-HealthLake.yml      — FHIR R4 appointment data
AWS-012-Generate-Env-File.yml      — generates .env.aws-integration with all ARNs/URLs
AWS-099-Deploy-All.yml             — wrapper with checkboxes for selective deployment
AWS-099-Destroy-All.yml            — teardown wrapper with checkboxes
```

Matching destroy actions for each create:
```
AWS-001-Destroy-... through AWS-011-Destroy-...
```

**Step 2: Generate local config file**
`AWS-012-Generate-Env-File.yml` outputs `.env.aws-integration` (git-ignored) containing:
- All SNS topic ARNs, SQS queue URLs, S3 bucket name + dataset prefix
- OpenSearch endpoint, Bedrock agent ID, Step Functions state machine ARN
- Connect instance ID, Lambda function ARNs

**Step 3: Start Spring Boot with AWS profile**
```bash
# One-time: run mvn command to read .env.aws-integration and create application-aws-integration.yml
mvn exec:java -Dexec.mainClass="com.agenticcare.config.AwsConfigGenerator"

# Then start with AWS profile
npm run start:aws-integration
# → Spring Boot starts with spring.profiles.active=aws-integration
# → Polls SQS queues for events from Step Functions/Lambda
# → Can trigger Step Functions executions via AWS SDK
```

### Real-Time Outreach Flow on AWS

```
1. User clicks "Submit Run" in Admin UI
   → selects "Real-Time Outreach Simulation"
   → selects "AWS Step Functions" as engine
   → selects S3 dataset location (pre-filled or new folder)
   → clicks Submit

2. Spring Boot (on laptop):
   → WfEngineFacadeFactory.getFacade("AWS_STEP_FUNCTIONS")
   → AwsStepFunctionsEngineFacade.triggerRun()
   → AWS SDK: StartExecution(stateMachineArn, input={datasetS3Path, runId})

3. AWS Step Functions orchestrates:
   → Step 1: Lambda reads CSV from S3, iterates rows
   → Step 2: For each patient, invokes PCA Lambda
     → PCA Lambda calls Amazon Bedrock (Claude) for context reasoning
     → Classifies C_p, assesses risk
   → Step 3: Invokes COA Lambda
     → COA Lambda calls Amazon Connect (place IVR call / send SMS / schedule callback)
     → Records patient response
   → Step 4: Publishes result to EventBridge
     → EventBridge routes to SQS (for Spring Boot) + SNS (for notifications)

4. Spring Boot (on laptop) polls SQS:
   → Receives per-patient action results
   → Saves to agentic_outreach_action DB table
   → Updates workflow run status
   → Admin UI shows results in real-time
```

### Apache Airflow's Role with AWS

For the AWS environment, Airflow acts as the **batch dispatcher** (same as local):
- Airflow reads dataset from S3 (instead of local filesystem)
- For each patient, Airflow triggers AWS Step Functions (instead of agent DAG)
- AWS Step Functions handles the per-patient agentic pipeline (Bedrock, Connect, Lambda)
- Results flow back via SQS to Spring Boot

This means:
- Airflow DAG 1 (batch) dispatches to AWS Step Functions (not Airflow DAG 2)
- AWS Step Functions replaces Airflow DAG 2 for per-patient agent execution
- The agentic AI runs on AWS (Bedrock + Lambda), not in Airflow Python tasks

### GitHub Secrets Required

| Secret | Description |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM access key |
| `AWS_SECRET_ACCESS_KEY` | IAM secret key |
| `AWS_REGION` | e.g. us-east-1 |
| `AWS_VPC_ID` | VPC for Connect/OpenSearch |
| `AWS_ACCOUNT_ID` | For ARN construction |

### Spring Boot AWS Integration Profile

`application-aws-integration.yml` (generated, git-ignored):
```yaml
smartcare:
  aws:
    region: us-east-1
    s3:
      dataset-bucket: smartcare-datasets-{account-id}
      dataset-prefix: datasets/
    sqs:
      workflow-events-queue-url: https://sqs.us-east-1.amazonaws.com/...
      outreach-results-queue-url: https://sqs.us-east-1.amazonaws.com/...
      admin-alerts-queue-url: https://sqs.us-east-1.amazonaws.com/...
    step-functions:
      state-machine-arn: arn:aws:states:us-east-1:...:stateMachine:smartcare-outreach
    connect:
      instance-id: ...
    bedrock:
      agent-id: ...
    opensearch:
      endpoint: https://...es.amazonaws.com
```

### npm Scripts for AWS Integration

```json
"start:aws-integration": "SPRING_PROFILES_ACTIVE=aws-integration mvn -pl app-web spring-boot:run"
"aws:generate-config": "mvn exec:java -Dexec.mainClass=com.agenticcare.config.AwsConfigGenerator"
```

### Technology Decision: Python Lambda (NOT Java)

Java Lambda cold starts are 5-10 seconds — too slow for per-patient real-time outreach. All 5 agent Lambda functions are **Python 3.12** using boto3 for Bedrock/Connect/SNS. Same language as Airflow DAGs — logic can be shared.

| Component | Language | Where |
|---|---|---|
| Spring Boot (admin portal, workflow mgmt, DB, SQS polling) | Java | Laptop / EC2 / ECS |
| Lambda agents (PCA, COA, PSA, RRA, ACA) | Python 3.12 | AWS Lambda |
| Airflow DAGs (batch dispatcher) | Python | Docker (local) / MWAA (AWS) |
| Bedrock + Connect + SNS calls | Python (boto3) | Inside Lambda |

Lambda code lives in: `infra/lambda/pca/`, `infra/lambda/coa/`, `infra/lambda/psa/`, `infra/lambda/rra/`, `infra/lambda/aca/`
Each folder has: `handler.py`, `requirements.txt`, and shares common utils.

---

## AWS Deployment — Task Breakdown with Status

### Phase A: GitHub Actions — Infrastructure (CloudFormation)

| # | Task | GH Action | CloudFormation | Status | Notes |
|---|---|---|---|---|---|
| A1 | Validate VPC from secrets | `AWS-001-Validate-VPC.yml` | N/A (CLI check) | **DONE** (YAML created) | Checks `AWS_VPC_ID` secret, prints subnets |
| A2 | Create S3 bucket for datasets | `AWS-002-Create-S3-Bucket.yml` | `infra/cfn-s3-bucket.yaml` | PENDING | Bucket: `smartcare-datasets-{account}`, prefix: `datasets/` |
| A3 | Create SNS topics | `AWS-003-Create-SNS-Topics.yml` | `infra/cfn-sns-topics.yaml` | **DONE** (YAML created, CFN pending) | Topics: appointment_event, outreach_result, admin_alert |
| A4 | Create SQS queues | `AWS-004-Create-SQS-Queues.yml` | `infra/cfn-sqs-queues.yaml` | **DONE** (YAML created, CFN pending) | Queues Spring Boot polls: workflow_events, outreach_results, admin_alerts |
| A5 | Create EventBridge bus + rules | `AWS-005-Create-EventBridge.yml` | `infra/cfn-eventbridge.yaml` | **DONE** (YAML created, CFN pending) | Routes events to SQS + Lambda |
| A6 | Create OpenSearch domain | `AWS-006-Create-OpenSearch.yml` | `infra/cfn-opensearch.yaml` | **DONE** (YAML created, CFN pending) | 4 indices: appointments, outreach_events, provider_schedule, audit_log |
| A7 | Create Bedrock agent (PCA) | `AWS-007-Create-Bedrock-Agent.yml` | `infra/cfn-bedrock-agent.yaml` | **DONE** (YAML created, CFN pending) | Claude foundation model for C_p classification |
| A8 | Create Lambda functions (5 agents) | `AWS-008-Create-Lambda-Agents.yml` | `infra/cfn-lambda-agents.yaml` | **DONE** (YAML created, CFN pending) | Python 3.12 functions: PCA, COA, PSA, RRA, ACA |
| A9 | Create Step Functions state machine | `AWS-009-Create-StepFunctions.yml` | `infra/cfn-step-functions.yaml` | **DONE** (YAML created, CFN pending) | Orchestrator: PCA→COA→PSA→RRA→ACA |
| A10 | Create Connect instance | `AWS-010-Create-Connect.yml` | `infra/cfn-connect.yaml` | **DONE** (YAML created, CFN pending) | Contact flows: IVR, SMS, callback |
| A11 | Create HealthLake datastore | `AWS-011-Create-HealthLake.yml` | `infra/cfn-healthlake.yaml` | PENDING | FHIR R4 appointment data |
| A12 | Generate .env.aws-integration | `AWS-012-Generate-Env-File.yml` | N/A (reads stack outputs) | PENDING | Writes all ARNs, URLs, IDs to .env file |
| A13 | Deploy-All wrapper | `AWS-099-Deploy-All.yml` | N/A (calls A1-A12) | PENDING | Checkbox inputs for selective deployment |
| A14 | Destroy-All wrapper | `AWS-099-Destroy-All.yml` | N/A (deletes stacks) | PENDING | Checkbox inputs for selective teardown |

### Phase B: CloudFormation Templates

| # | Task | File | Status | Notes |
|---|---|---|---|---|
| B1 | S3 bucket template | `infra/cfn-s3-bucket.yaml` | PENDING | Bucket with lifecycle, versioning, dataset prefix |
| B2 | SNS topics template | `infra/cfn-sns-topics.yaml` | PENDING | 3 topics with output ARNs |
| B3 | SQS queues template | `infra/cfn-sqs-queues.yaml` | PENDING | 3 queues + DLQs, SNS subscriptions |
| B4 | EventBridge template | `infra/cfn-eventbridge.yaml` | PENDING | Custom bus, rules for appointment events → SQS + Lambda |
| B5 | OpenSearch template | `infra/cfn-opensearch.yaml` | PENDING | t3.small.search domain, 4 index templates |
| B6 | Bedrock agent template | `infra/cfn-bedrock-agent.yaml` | PENDING | Agent with Claude model, knowledge base for patient history |
| B7 | Lambda agents template | `infra/cfn-lambda-agents.yaml` | PENDING | 5 functions, IAM roles, env vars, layers |
| B8 | Step Functions template | `infra/cfn-step-functions.yaml` | PENDING | State machine definition: PCA→COA→PSA→RRA→ACA |
| B9 | Connect template | `infra/cfn-connect.yaml` | PENDING | Instance + 3 contact flows (IVR, SMS, callback) |
| B10 | HealthLake template | `infra/cfn-healthlake.yaml` | PENDING | FHIR R4 datastore |

### Phase C: Lambda Function Code (Python)

| # | Task | Path | Status | Notes |
|---|---|---|---|---|
| C1 | Shared Lambda utils | `infra/lambda/shared/` | PENDING | boto3 helpers for Bedrock, Connect, SNS, SQS, OpenSearch |
| C2 | PCA Lambda handler | `infra/lambda/pca/handler.py` | PENDING | Calls Bedrock Claude for C_p classification, XGBoost for R_p |
| C3 | COA Lambda handler | `infra/lambda/coa/handler.py` | PENDING | Calls Connect (IVR/SMS/callback), publishes result to EventBridge |
| C4 | PSA Lambda handler | `infra/lambda/psa/handler.py` | PENDING | Queries OpenSearch for unconfirmed slots, escalates to RRA |
| C5 | RRA Lambda handler | `infra/lambda/rra/handler.py` | PENDING | Queries waitlist from HealthLake, triggers COA for top candidate |
| C6 | ACA Lambda handler | `infra/lambda/aca/handler.py` | PENDING | Writes immutable audit to OpenSearch with ILM |
| C7 | Lambda requirements.txt | `infra/lambda/*/requirements.txt` | PENDING | boto3, xgboost (PCA only), requests |

### Phase D: Spring Boot AWS Integration

| # | Task | Status | Notes |
|---|---|---|---|
| D1 | `application-aws-integration.yml` | PENDING | AWS profile config: SQS URLs, Step Functions ARN, S3 bucket, etc. |
| D2 | `AwsConfigGenerator.java` | PENDING | Reads `.env.aws-integration`, generates `application-aws-integration.yml` |
| D3 | SQS consumer for workflow events | PENDING | Replaces HTTP broker polling — polls SQS for Step Functions results |
| D4 | SQS consumer for outreach results | PENDING | Receives per-patient action results, saves to `agentic_outreach_action` table |
| D5 | SQS consumer for admin alerts | PENDING | Surfaces admin alerts in dashboard |
| D6 | `AwsStepFunctionsEngineFacade` — real impl | PENDING | Currently stub — needs AWS SDK `StartExecution` call |
| D7 | `npm run start:aws-integration` script | PENDING | Starts Spring Boot with `aws-integration` profile |

### Phase E: Admin UI Updates for AWS

| # | Task | Status | Notes |
|---|---|---|---|
| E1 | Workflow engine dropdown shows AWS Step Functions | PENDING | Already seeded — just needs real AWS engine registered |
| E2 | Dataset ingest to S3 | PENDING | "Ingest to AWS S3" option with S3 bucket location pre-filled |
| E3 | S3 dataset location radio buttons | PENDING | "Create new folder" or "Reuse existing pre-defined folder" |
| E4 | Workflow runs: show AWS execution link | PENDING | Link to AWS Step Functions console for the execution |
| E5 | Real-time results from SQS | PENDING | UI polls for new agentic actions as they arrive from SQS |

### Phase F: Destroy Actions

| # | Task | GH Action | Status |
|---|---|---|---|
| F1 | Destroy Connect | `AWS-010-Destroy-Connect.yml` | PENDING |
| F2 | Destroy Step Functions | `AWS-009-Destroy-StepFunctions.yml` | PENDING |
| F3 | Destroy Lambda Agents | `AWS-008-Destroy-Lambda-Agents.yml` | PENDING |
| F4 | Destroy Bedrock Agent | `AWS-007-Destroy-Bedrock-Agent.yml` | PENDING |
| F5 | Destroy OpenSearch | `AWS-006-Destroy-OpenSearch.yml` | PENDING |
| F6 | Destroy EventBridge | `AWS-005-Destroy-EventBridge.yml` | PENDING |
| F7 | Destroy SQS Queues | `AWS-004-Destroy-SQS-Queues.yml` | PENDING |
| F8 | Destroy SNS Topics | `AWS-003-Destroy-SNS-Topics.yml` | PENDING |
| F9 | Destroy S3 Bucket | `AWS-002-Destroy-S3-Bucket.yml` | PENDING |
| F10 | Destroy-All wrapper | `AWS-099-Destroy-All.yml` | PENDING |

### GitHub Secrets Required

| Secret | Description | Example |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | IAM access key | AKIA... |
| `AWS_SECRET_ACCESS_KEY` | IAM secret key | ... |
| `AWS_REGION` | Deployment region | us-east-1 |
| `AWS_VPC_ID` | VPC for Connect/OpenSearch | vpc-0abc... |
| `AWS_ACCOUNT_ID` | For ARN construction | 123456789012 |

### Key Design Decisions

1. **Python Lambda, NOT Java** — cold start <1s vs 5-10s. Real-time patient outreach needs speed.
2. **SQS for laptop→AWS integration** — Spring Boot polls SQS, works from localhost without VPN/ngrok.
3. **Airflow dispatches to Step Functions** — Airflow DAG 1 (batch) iterates CSV from S3, triggers Step Functions per patient. Step Functions replaces Airflow DAG 2.
4. **Step Functions → Lambda → Bedrock/Connect** — each step invokes a Lambda, Lambda calls Bedrock for reasoning or Connect for outreach.
5. **EventBridge routes results to SQS** — Lambda publishes to EventBridge, rules route to SQS queues, Spring Boot polls and saves to DB.
6. **`.env.aws-integration` generated by GH Action** — git-ignored, contains all ARNs/URLs. Spring Boot reads via `application-aws-integration.yml`.
7. **Guard logic in PCA Lambda** — before outreach, PCA queries latest patient signals (check-in, SMS response, provider events). If patient confirmed/arriving → SKIP outreach.
