# AgenticSmartCareScheduler — Development Plan

## Technology Choices

### Core Framework: Spring Boot 3.x + Spring AI
- **Spring AI** provides native agentic AI support: @Tool annotations, ChatClient, ToolCallback, A2A Protocol
- **Spring AI Amazon Bedrock Starter** for foundation model integration (Converse API, Knowledge Bases)
- **Spring AI Vector Store** for patient context embeddings (backed by Amazon OpenSearch)
- Enterprise-grade, production-ready, aligns with AWS ecosystem

### AWS Services
| Service | Purpose |
|---|---|
| Amazon Bedrock | Foundation models for PCA context classification (C_p) |
| Amazon Bedrock Knowledge Bases | RAG over patient interaction history |
| Amazon Connect | Multi-channel outreach: voice IVR, SMS, callback |
| Amazon SNS | SMS deep-link delivery |
| Amazon OpenSearch | Digital twin DT(t) — 4 indices + vector search |
| AWS Step Functions | Agent orchestration state machine |
| Amazon EventBridge | Event-driven inter-agent communication |
| AWS Lambda | Serverless agent execution |
| AWS HealthLake | FHIR R4 data normalization |
| AWS CloudFormation | Infrastructure-as-code |

### Supporting Tech
- **Python 3.12**: XGBoost model training, data analysis, analytics scripts
- **GitHub Actions**: CI/CD workflows (bootstrap infra, deploy, run scenarios, extract analytics, shutdown)

---

## Development Phases

### Phase 1: Data & ML Foundation (1 hour)
**Goal:** Train the PCA risk model and produce real metrics.

#### 1A. Dataset Setup
- Download Medical Appointment No-Show dataset from Kaggle (110,527 records)
- Load into `data/` directory
- Python script for EDA: no-show rate distribution, feature analysis, lead time patterns

#### 1B. PCA Risk Model (Python + XGBoost)
- Feature engineering: scheduling lead time, age, prior no-show count, chronic conditions, SMS receipt, day-of-week, time-of-day
- Train XGBoost classifier with 5-fold stratified cross-validation
- Produce real metrics: F1, AUC, precision, recall, confusion matrix
- Save model to `models/pca_risk_model.pkl`
- **Output for paper:** Real F1 and AUC numbers to replace qualitative claims

#### 1C. Context State Simulation
- Python script that assigns C_p states based on appointment time patterns:
  - Weekday 7-9 AM, 4-7 PM → reachable-mobile (commute hours)
  - Weekday 9 AM-4 PM → reachable-stationary (work/home)
  - No SMS_received + high risk → unreachable
- Produce C_p distribution across the 110K dataset
- **Output for paper:** Real channel distribution statistics

---

### Phase 2: Spring Boot Agentic Framework (1.5 hours)
**Goal:** Build the PCA and COA as Spring AI agents with Bedrock integration.

#### 2A. Spring Boot Project Setup
- Spring Boot 3.x with Spring AI starter
- Dependencies: spring-ai-bedrock, spring-ai-opensearch, spring-boot-starter-web
- Application profiles: local, aws-dev

#### 2B. PCA Agent (Spring AI + Bedrock)
```
src/main/java/com/agenticcare/pca/
  PatientContextAgent.java        — @Tool annotated methods
  RiskScoringService.java         — XGBoost model inference (calls Python model via REST or ONNX)
  ContextClassificationService.java — Amazon Bedrock ChatClient for C_p inference
  PatientContextResult.java       — Record: (C_p, R_p, P_p)
```
- Spring AI ChatClient configured with Amazon Bedrock
- Structured prompt for C_p classification using patient metadata
- @Tool annotation for risk scoring and context classification
- Publishes PatientContextResult to EventBridge

#### 2C. COA Agent (Spring AI + Amazon Connect)
```
src/main/java/com/agenticcare/coa/
  CommunicationOrchestrationAgent.java  — @Tool annotated methods
  ChannelSelectionService.java           — C_p → channel mapping (Table I logic)
  ConnectOutreachService.java            — Amazon Connect API integration
  OutreachResult.java                    — Record: channel, timestamp, outcome
```
- Receives PatientContextResult from PCA
- Applies channel selection rules from Table I
- Calls Amazon Connect API for IVR / SNS for SMS / Connect callback queue
- Logs OutreachResult to OpenSearch outreach_events index

#### 2D. Agent Orchestration
```
src/main/java/com/agenticcare/orchestrator/
  AgentOrchestrator.java          — Spring AI SequentialAgent pattern
  DigitalTwinService.java         — DT(t) state management on OpenSearch
  EventBridgePublisher.java       — Event publishing
```
- SequentialAgent: PCA → COA (for this focused implementation)
- DT(t) updated after each agent action

---

### Phase 3: AWS Infrastructure (1 hour)
**Goal:** Deploy via GitHub Actions to AWS.

#### 3A. CloudFormation Templates
```
infra/
  cfn-opensearch.yaml        — OpenSearch domain with 4 indices
  cfn-lambda-pca.yaml        — PCA Lambda function
  cfn-lambda-coa.yaml        — COA Lambda function
  cfn-eventbridge.yaml       — Event bus and rules
  cfn-stepfunctions.yaml     — PCA→COA state machine
  cfn-connect.yaml           — Connect instance + 3 contact flows (if possible in CFN)
```

#### 3B. GitHub Actions Workflows
```
.github/workflows/
  bootstrap-infra.yml        — Deploy CloudFormation stacks
  deploy-agents.yml          — Build Spring Boot, deploy to Lambda
  run-scenario.yml           — Trigger end-to-end PCA→COA scenario
  extract-analytics.yml      — Query OpenSearch, generate metrics
  shutdown-infra.yml         — Tear down all stacks (cost control)
```

#### 3C. Bootstrap Sequence
1. `bootstrap-infra.yml` → creates OpenSearch, EventBridge, Step Functions
2. `deploy-agents.yml` → builds JAR, deploys PCA + COA as Lambda
3. Ready for scenario execution

---

### Phase 4: End-to-End Scenario Execution (1 hour)
**Goal:** Run focused scenarios and extract real metrics.

#### 4A. Focused Scenarios (not all 110K — pick representative subsets)

**Scenario 1: High-Risk Mobile Patient**
- Filter: R_p > 0.65, appointment 4-6 PM weekday
- Expected: PCA classifies C_p = reachable-mobile, COA selects voice IVR
- Measure: classification accuracy, channel selection distribution

**Scenario 2: Low-Risk Stationary Patient**
- Filter: R_p < 0.3, appointment 10 AM-2 PM weekday
- Expected: PCA classifies C_p = reachable-stationary, COA selects SMS
- Measure: classification accuracy, channel selection distribution

**Scenario 3: Unreachable Patient**
- Filter: no SMS_received, R_p > 0.5, no prior response history
- Expected: PCA classifies C_p = unreachable, COA schedules callback
- Measure: classification accuracy, channel selection distribution

#### 4B. Execution
- `run-scenario.yml` GitHub Action triggers Step Functions state machine
- PCA processes batch of appointments → publishes to EventBridge
- COA receives events → selects channels → logs to OpenSearch
- All events captured in DT(t)

#### 4C. Metrics Collection
- Query OpenSearch for: channel distribution by C_p, R_p distribution, escalation counts
- Compare against baseline: "what if all patients received SMS only?"
- **Output for paper:** Real numbers replacing qualitative claims

---

### Phase 5: Analytics & Insights (30 min)
**Goal:** Generate visualizations and metrics for the paper.

#### 5A. Analytics Scripts (Python)
```
analytics/
  risk_model_evaluation.py    — F1, AUC, ROC curve, confusion matrix
  channel_distribution.py     — C_p state distribution, channel selection breakdown
  baseline_comparison.py      — Proposed vs SMS-only metrics
  scenario_results.py         — Per-scenario outcome summary
```

#### 5B. Expected Outputs
- `analytics/output/risk_model_metrics.json` — F1, AUC, precision, recall
- `analytics/output/channel_distribution.png` — pie/bar chart of IVR/SMS/callback split
- `analytics/output/baseline_comparison.png` — proposed vs SMS-only
- `analytics/output/scenario_summary.md` — narrative per scenario

---

## Project Structure

```
AgenticSmartCareScheduler/
├── CLAUDE.md                          — Context for Claude Code
├── DevelopmentPlan.md                 — This file
├── README.md                          — Project overview
├── .gitignore
├── src/
│   ├── main/java/com/agenticcare/
│   │   ├── AgenticSmartCareApplication.java
│   │   ├── pca/
│   │   │   ├── PatientContextAgent.java
│   │   │   ├── RiskScoringService.java
│   │   │   ├── ContextClassificationService.java
│   │   │   └── PatientContextResult.java
│   │   ├── coa/
│   │   │   ├── CommunicationOrchestrationAgent.java
│   │   │   ├── ChannelSelectionService.java
│   │   │   ├── ConnectOutreachService.java
│   │   │   └── OutreachResult.java
│   │   ├── orchestrator/
│   │   │   ├── AgentOrchestrator.java
│   │   │   ├── DigitalTwinService.java
│   │   │   └── EventBridgePublisher.java
│   │   └── config/
│   │       ├── BedrockConfig.java
│   │       ├── OpenSearchConfig.java
│   │       └── ConnectConfig.java
│   └── main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── application-aws.yml
├── data/
│   └── .gitkeep                       — Kaggle dataset downloaded here
├── models/
│   └── .gitkeep                       — Trained XGBoost model saved here
├── analytics/
│   ├── risk_model_evaluation.py
│   ├── channel_distribution.py
│   ├── baseline_comparison.py
│   └── output/
├── infra/
│   ├── cfn-opensearch.yaml
│   ├── cfn-lambda.yaml
│   ├── cfn-eventbridge.yaml
│   └── cfn-stepfunctions.yaml
├── .github/workflows/
│   ├── bootstrap-infra.yml
│   ├── deploy-agents.yml
│   ├── run-scenario.yml
│   ├── extract-analytics.yml
│   └── shutdown-infra.yml
├── tests/
│   ├── test_pca_risk_model.py
│   └── test_channel_selection.py
├── pom.xml                            — Maven build
└── Dockerfile                         — For Lambda deployment
```

---

## Success Criteria

After 5 hours, we should have:

1. **Real F1 and AUC** from XGBoost on the 110K dataset — numbers we can cite in the paper
2. **Real C_p distribution** — what percentage of patients are mobile/stationary/unreachable
3. **Real channel selection stats** — how the COA distributes across IVR/SMS/callback
4. **Baseline comparison** — proposed framework vs SMS-only, with real numbers
5. **Deployed AWS infrastructure** — screenshots of Step Functions, OpenSearch, Lambda
6. **GitHub Actions evidence** — CI/CD pipeline running the scenario

These outputs feed back into the paper as V07 — replacing qualitative claims with measured results.
