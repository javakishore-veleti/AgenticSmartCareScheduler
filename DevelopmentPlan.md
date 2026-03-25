# AgenticSmartCareScheduler — Development Plan

> Coding conventions, module structure, and naming rules are in **CodingConventions.md**.
> Architecture, agents, and dataset details are in **CLAUDE.md**.

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

## Phase 1: Python ML Foundation (1 hour)

### 1A. Dataset
- Download Kaggle Medical Appointment No-Show dataset (110,527 records)
- Save to `python/data/`
- EDA script: no-show rate, feature distributions, lead time analysis

### 1B. XGBoost Risk Model
- Features: lead time, age, prior no-show count, chronic conditions, SMS receipt, day-of-week
- 5-fold stratified cross-validation
- **Output:** F1, AUC, precision, recall, confusion matrix → `python/analytics/output/`
- Save model: `python/models/pca_risk_model.pkl`
- Export ONNX: `python/models/pca_risk_model.onnx` (for Java inference)

### 1C. Context State Simulation
- Assign C_p to each record based on appointment time:
  - Weekday 7-9 AM, 4-7 PM → REACHABLE_MOBILE
  - Weekday 9 AM-4 PM → REACHABLE_STATIONARY
  - No SMS_received + R_p > 0.5 → UNREACHABLE
- **Output:** C_p distribution stats, channel selection distribution

---

## Phase 2: Spring Boot Multi-Module Setup (1.5 hours)

### 2A. Maven Multi-Module Scaffolding
- Root `pom.xml` with dependency management
- Create all modules listed in CodingConventions.md
- Spring AI + Bedrock starter dependencies
- Application profiles: `local`, `aws-dev`

### 2B. app-common
- Enums: `ContextState`, `SlotStatus`, `OutreachChannel`
- Constants: `AgentConstants` (R_p thresholds, T_v intervals)
- DTOs: conservative set of ExecCtx/ReqDto/RespDto — create as needed
- Interfaces: `Agent`, `DigitalTwinState`

### 2C. app-agent-patient (PCA)
- `PatientContextAgent` with `@Tool` annotations
- Calls Amazon Bedrock via Spring AI ChatClient for C_p classification
- Loads ONNX model for R_p scoring (or calls Python service)
- Publishes results to EventBridge

### 2D. app-agent-communication (COA)
- `CommunicationOrchestrationAgent` with `@Tool` annotations
- Channel selection logic from Table I (C_p → channel mapping)
- Amazon Connect integration for IVR/callback
- Amazon SNS for SMS deep-link
- Logs to OpenSearch outreach_events index

### 2E. app-core + app-dao
- Service facades for PCA and COA
- OpenSearch repository for 4 indices
- Digital twin DT(t) state management

### 2F. app-web
- REST endpoints: `/api/v1/appointments/classify`, `/api/v1/appointments/outreach`, `/api/v1/scenarios/run`
- All follow ExecCtx pattern from CodingConventions.md

---

## Phase 3: AWS Infrastructure (1 hour)

### 3A. CloudFormation Templates
```
infra/
  cfn-opensearch.yaml         — OpenSearch domain, 4 indices
  cfn-eventbridge.yaml        — Event bus + rules
  cfn-stepfunctions.yaml      — PCA→COA state machine
  cfn-lambda.yaml             — Lambda functions for agents
```

### 3B. GitHub Actions Workflows
```
.github/workflows/
  bootstrap-infra.yml         — Deploy CloudFormation stacks
  deploy-agents.yml           — Build Spring Boot, deploy to Lambda
  run-scenario.yml            — Trigger PCA→COA scenario
  extract-analytics.yml       — Query OpenSearch, generate metrics
  shutdown-infra.yml          — Tear down all stacks (cost control)
```

### 3C. Bootstrap Sequence
1. `bootstrap-infra.yml` → OpenSearch, EventBridge, Step Functions
2. `deploy-agents.yml` → build JAR, deploy PCA + COA as Lambda
3. Ready for scenarios

---

## Phase 4: Scenario Execution (1 hour)

### Three Focused Scenarios

**Scenario 1: High-Risk Mobile Patient**
- Filter: R_p > 0.65, appointment 4-6 PM weekday
- Expected: C_p = REACHABLE_MOBILE → COA selects VOICE_IVR

**Scenario 2: Low-Risk Stationary Patient**
- Filter: R_p < 0.3, appointment 10 AM-2 PM weekday
- Expected: C_p = REACHABLE_STATIONARY → COA selects SMS_DEEPLINK

**Scenario 3: Unreachable Patient**
- Filter: no SMS_received, R_p > 0.5
- Expected: C_p = UNREACHABLE → COA selects CALLBACK

### Execution
- GitHub Action triggers Step Functions
- PCA processes batch → EventBridge → COA selects channels → OpenSearch logs
- All events captured in DT(t)

### Metrics
- Channel distribution by C_p state
- R_p distribution across no-show / show
- Comparison: proposed framework vs SMS-only baseline

---

## Phase 5: Analytics & Paper Output (30 min)

### Scripts
```
python/analytics/
  risk_model_evaluation.py     — F1, AUC, ROC curve, confusion matrix
  channel_distribution.py      — C_p breakdown, channel selection stats
  baseline_comparison.py       — Proposed vs SMS-only
  scenario_results.py          — Per-scenario summary
```

### Outputs → Feed back into paper V07
- `python/analytics/output/risk_model_metrics.json`
- `python/analytics/output/channel_distribution.png`
- `python/analytics/output/baseline_comparison.png`
- `python/analytics/output/scenario_summary.md`

---

## Success Criteria

After 5 hours:
1. Real F1 and AUC from XGBoost on 110K dataset
2. Real C_p distribution across the dataset
3. Real channel selection stats (IVR / SMS / callback split)
4. Baseline comparison with real numbers
5. Deployed AWS infrastructure (OpenSearch, Lambda, Step Functions)
6. GitHub Actions CI/CD pipeline evidence
7. All outputs ready to update paper from V06 → V07
