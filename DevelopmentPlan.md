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

## Project Structure — Multi-Module Spring Boot

The application follows a multi-module Maven architecture with a root parent POM and domain-specific modules. Each module has a single responsibility and clear dependency boundaries.

```
AgenticSmartCareScheduler/
├── pom.xml                                    — Parent POM (dependency management, versions)
├── CLAUDE.md
├── DevelopmentPlan.md
├── README.md
├── .gitignore
│
├── app-common/                                — Shared interfaces, DTOs, constants, enums
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/common/
│       ├── constants/
│       │   └── AgentConstants.java            — R_p thresholds, T_v intervals, channel types
│       ├── enums/
│       │   ├── ContextState.java              — REACHABLE_STATIONARY, REACHABLE_MOBILE, UNREACHABLE
│       │   ├── SlotStatus.java                — CONFIRMED, AT_RISK, VACANT
│       │   └── OutreachChannel.java           — VOICE_IVR, SMS_DEEPLINK, CALLBACK
│       ├── dto/
│       │   ├── PatientContextResult.java      — Record: (C_p, R_p, P_p)
│       │   ├── OutreachResult.java            — Record: channel, timestamp, outcome
│       │   ├── SlotEscalationEvent.java       — PSA → RRA event
│       │   └── AuditEntry.java                — ACA audit document
│       └── interfaces/
│           ├── Agent.java                     — Base agent interface
│           └── DigitalTwinState.java          — DT(t) state contract
│
├── app-dao/                                   — Data access: JPA, OpenSearch, vector DB
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/dao/
│       ├── entity/
│       │   ├── AppointmentEntity.java         — appointments index/table
│       │   ├── OutreachEventEntity.java       — outreach_events index
│       │   ├── ProviderScheduleEntity.java    — provider_schedule index
│       │   └── AuditLogEntity.java            — audit_log index (ILM write-once)
│       ├── repository/
│       │   ├── rdbms/                         — JPA repositories (H2/PostgreSQL for local)
│       │   ├── opensearch/                    — OpenSearch repositories (4 indices)
│       │   └── vector/                        — Vector search for patient embeddings
│       └── config/
│           ├── JpaConfig.java
│           ├── OpenSearchConfig.java
│           └── VectorStoreConfig.java
│
├── app-core/                                  — Core business logic, service facades
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/core/
│       ├── service/
│       │   ├── RiskScoringService.java        — XGBoost model inference
│       │   ├── ContextClassificationService.java — C_p classification logic
│       │   ├── ChannelSelectionService.java   — Table I mapping: C_p → channel
│       │   ├── ScheduleMonitorService.java    — T-90min / T-30min escalation logic
│       │   ├── WaitlistReallocationService.java — Waitlist query + slot reservation
│       │   ├── AuditService.java              — Immutable audit logging
│       │   └── DigitalTwinService.java        — DT(t) state management
│       └── facade/
│           ├── PatientContextFacade.java      — Orchestrates PCA sub-services
│           ├── CommunicationFacade.java       — Orchestrates COA sub-services
│           └── CoordinationFacade.java        — End-to-end PCA→COA→PSA→RRA→ACA
│
├── app-web/                                   — REST API controllers
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/web/
│       ├── controller/
│       │   ├── AppointmentController.java     — Ingest appointments, trigger coordination
│       │   ├── AgentStatusController.java     — Agent health, DT(t) state queries
│       │   ├── AnalyticsController.java       — Metrics endpoints for dashboard
│       │   └── ScenarioController.java        — Trigger specific test scenarios
│       └── config/
│           └── WebSecurityConfig.java
│
├── app-wfs/                                   — Workflow definitions (Temporal / AWS)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/wfs/
│       ├── CoordinationWorkflow.java          — PCA→COA→PSA→RRA→ACA sequence
│       ├── EscalationWorkflow.java            — T-90min / T-30min escalation
│       └── ReallocationWorkflow.java          — Waitlist reallocation sequence
│
├── app-cloud-aws-wfs/                         — AWS workflow facades (Step Functions)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/cloud/aws/wfs/
│       ├── StepFunctionsOrchestrator.java     — Start/monitor Step Functions executions
│       └── EventBridgePublisher.java          — Publish/consume EventBridge events
│
├── app-cloud-aws-ai/                          — AWS AI service facades (Bedrock)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/cloud/aws/ai/
│       ├── BedrockChatService.java            — Spring AI ChatClient with Bedrock
│       ├── BedrockKnowledgeBaseService.java   — RAG over patient interaction history
│       └── BedrockEmbeddingService.java       — Vector embeddings for patient context
│
├── app-cloud-aws-ml/                          — AWS ML service facades
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/cloud/aws/ml/
│       ├── SageMakerInferenceService.java     — If model hosted on SageMaker
│       └── ModelRegistryService.java          — Model versioning
│
├── app-cloud-aws-agents/                      — AWS agent orchestration
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/cloud/aws/agents/
│       ├── BedrockAgentService.java           — Amazon Bedrock Agents integration
│       └── ConnectAgentService.java           — Amazon Connect contact flow execution
│
├── app-agent-patient/                         — Patient persona agent (PCA)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/agent/patient/
│       ├── PatientContextAgent.java           — @Tool annotated, Spring AI agent
│       └── PatientContextToolCallbacks.java   — Tool definitions for Bedrock
│
├── app-agent-communication/                   — Communication persona agent (COA)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/agent/communication/
│       ├── CommunicationOrchestrationAgent.java — @Tool annotated
│       └── ChannelSelectionToolCallbacks.java
│
├── app-agent-provider/                        — Provider persona agent (PSA)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/agent/provider/
│       ├── ProviderScheduleAgent.java         — @Tool annotated
│       └── EscalationToolCallbacks.java
│
├── app-agent-resource/                        — Resource admin persona agent (RRA)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/agent/resource/
│       ├── ResourceReallocationAgent.java     — @Tool annotated
│       └── WaitlistToolCallbacks.java
│
├── app-agent-audit/                           — Audit persona agent (ACA)
│   ├── pom.xml
│   └── src/main/java/com/agenticcare/agent/audit/
│       ├── AuditComplianceAgent.java          — @Tool annotated
│       └── AuditToolCallbacks.java
│
├── python/                                    — Python: ML training + analytics
│   ├── requirements.txt
│   ├── data_prep/
│   │   └── prepare_dataset.py                 — Download + clean Kaggle dataset
│   ├── ml/
│   │   ├── train_risk_model.py                — XGBoost training, 5-fold CV
│   │   ├── context_state_simulator.py         — Assign C_p based on time patterns
│   │   └── export_model.py                    — Export to ONNX for Java inference
│   └── analytics/
│       ├── risk_model_evaluation.py           — F1, AUC, ROC curve
│       ├── channel_distribution.py            — C_p + channel breakdown
│       ├── baseline_comparison.py             — Proposed vs SMS-only
│       └── output/                            — Generated charts + metrics
│
├── data/
│   └── .gitkeep
├── models/
│   └── .gitkeep
│
├── infra/
│   ├── cfn-opensearch.yaml
│   ├── cfn-lambda.yaml
│   ├── cfn-eventbridge.yaml
│   └── cfn-stepfunctions.yaml
│
├── .github/workflows/
│   ├── bootstrap-infra.yml
│   ├── deploy-agents.yml
│   ├── run-scenario.yml
│   ├── extract-analytics.yml
│   └── shutdown-infra.yml
│
└── tests/
    ├── java/                                  — JUnit tests per module
    └── python/                                — pytest for ML + analytics
```

### Coding Conventions

**Request/Response Pattern:**
- All service methods accept `ExecCtx` (Execution Context) — never raw arguments
- `ExecCtx` internally contains `ReqDto` and `RespDto`
- API controllers create `ReqDto` from HTTP GET parameters or POST body
- API controllers create a fresh `RespDto` object
- API controllers wrap both into `ExecCtx` and pass to service facades

Each use case has its own context-specific triplet: `ExecCtx`, `ReqDto`, `RespDto`. These are reusable across services that operate on the same domain context.

```java
// PCA domain
public class PatientClassifyReqDto { ... }
public class PatientClassifyRespDto { ... }
public class PatientClassifyExecCtx {
    private PatientClassifyReqDto reqDto;
    private PatientClassifyRespDto respDto;
}

// COA domain
public class OutreachSelectReqDto { ... }
public class OutreachSelectRespDto { ... }
public class OutreachSelectExecCtx {
    private OutreachSelectReqDto reqDto;
    private OutreachSelectRespDto respDto;
}

// Controller creates ReqDto + fresh RespDto, wraps in ExecCtx
@PostMapping("/classify")
public ResponseEntity<PatientClassifyRespDto> classify(@RequestBody PatientClassifyReqDto reqDto) {
    PatientClassifyRespDto respDto = new PatientClassifyRespDto();
    PatientClassifyExecCtx ctx = new PatientClassifyExecCtx(reqDto, respDto);
    patientContextFacade.classify(ctx);
    return ResponseEntity.ok(ctx.getRespDto());
}

// All service/facade methods take ExecCtx — never raw arguments
public void classify(PatientClassifyExecCtx ctx) { ... }
```

**Domain-Specific ExecCtx Triplets:**

| Use Case | ExecCtx | ReqDto | RespDto |
|---|---|---|---|
| PCA classification | PatientClassifyExecCtx | PatientClassifyReqDto | PatientClassifyRespDto |
| PCA risk scoring | RiskScoreExecCtx | RiskScoreReqDto | RiskScoreRespDto |
| COA channel selection | OutreachSelectExecCtx | OutreachSelectReqDto | OutreachSelectRespDto |
| PSA escalation | EscalationExecCtx | EscalationReqDto | EscalationRespDto |
| RRA reallocation | ReallocationExecCtx | ReallocationReqDto | ReallocationRespDto |
| ACA audit logging | AuditLogExecCtx | AuditLogReqDto | AuditLogRespDto |
| End-to-end coordination | CoordinationExecCtx | CoordinationReqDto | CoordinationRespDto |

ExecCtx classes are reusable — for example, `PatientClassifyExecCtx` is used by both the PCA agent and any service that needs to classify patient context.

**Naming Conventions:**
- Request DTOs: `*ReqDto` (context-specific)
- Response DTOs: `*RespDto` (context-specific)
- Execution Context: `*ExecCtx` (domain-specific, contains ReqDto + RespDto)
- Service interfaces: `I*Service`
- Facade interfaces: `I*Facade`

### Module Dependency Graph

```
app-common          ← no dependencies (pure DTOs, interfaces, enums)
    ↑
app-dao             ← depends on app-common
    ↑
app-core            ← depends on app-common, app-dao
    ↑
app-cloud-aws-ai    ← depends on app-common
app-cloud-aws-ml    ← depends on app-common
app-cloud-aws-wfs   ← depends on app-common
app-cloud-aws-agents ← depends on app-common, app-cloud-aws-ai
    ↑
app-agent-patient   ← depends on app-common, app-core, app-cloud-aws-ai
app-agent-communication ← depends on app-common, app-core, app-cloud-aws-agents
app-agent-provider  ← depends on app-common, app-core, app-dao
app-agent-resource  ← depends on app-common, app-core, app-dao
app-agent-audit     ← depends on app-common, app-core, app-dao
    ↑
app-wfs             ← depends on app-common, app-core, app-agent-*
app-cloud-aws-wfs   ← depends on app-wfs
    ↑
app-web             ← depends on all (top-level assembly)
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
