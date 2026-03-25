# Coding Conventions — AgenticSmartCareScheduler

## ExecCtx / ReqDto / RespDto Pattern

### Core Rule
All service and facade methods accept a single **ExecCtx** parameter — never raw arguments. ExecCtx wraps a ReqDto and RespDto pair.

### Conservative Approach
Do NOT create a new ExecCtx/ReqDto/RespDto triplet for every use case. Be conservative — reuse existing ones wherever the data context overlaps. Create new ones only when the data shape is genuinely different.

### How It Works

```java
// ExecCtx wraps a ReqDto + RespDto pair
public class AppointmentExecCtx {
    private AppointmentReqDto reqDto;
    private AppointmentRespDto respDto;
    // getters, setters
}
```

**Controller responsibility:**
1. Receives HTTP request (GET params or POST body)
2. Creates ReqDto from the request data
3. Creates a **fresh** RespDto (empty object)
4. Wraps both into ExecCtx
5. Passes ExecCtx to facade/service
6. Returns RespDto from ExecCtx to the caller

```java
@PostMapping("/appointments/classify")
public ResponseEntity<AppointmentRespDto> classify(@RequestBody AppointmentReqDto reqDto) {
    AppointmentRespDto respDto = new AppointmentRespDto();
    AppointmentExecCtx ctx = new AppointmentExecCtx(reqDto, respDto);
    patientContextFacade.classify(ctx);
    return ResponseEntity.ok(ctx.getRespDto());
}
```

**Service/Facade responsibility:**
- Accepts ExecCtx — never raw arguments
- Reads from `ctx.getReqDto()`
- Writes results to `ctx.getRespDto()`
- Can pass the same ExecCtx downstream to other services

```java
public void classify(AppointmentExecCtx ctx) {
    // read input
    String patientId = ctx.getReqDto().getPatientId();
    // do work...
    // write output
    ctx.getRespDto().setContextState(ContextState.REACHABLE_MOBILE);
    ctx.getRespDto().setRiskScore(0.72);
}
```

### When to Create New vs Reuse

**Reuse** when the data context is the same domain — e.g., PCA and COA both operate on appointment data, so they share `AppointmentExecCtx`.

**Create new** only when the data shape is genuinely different — e.g., `AuditExecCtx` has different fields than `AppointmentExecCtx`.

---

## Multi-Module Maven Structure

### Parent POM
- Root `pom.xml` is the parent
- Manages all dependency versions centrally
- Child modules inherit versions, do not declare their own

### Module Naming
- `app-common` — shared interfaces, DTOs, constants, enums
- `app-dao` — data access (JPA, OpenSearch, vector DB)
- `app-core` — business logic, services, facades
- `app-web` — REST API controllers
- `app-wfs` — workflow definitions (Temporal / generic)
- `app-cloud-aws-wfs` — AWS workflow facades (Step Functions, EventBridge)
- `app-cloud-aws-ai` — AWS AI facades (Bedrock, Knowledge Bases)
- `app-cloud-aws-ml` — AWS ML facades (SageMaker if needed)
- `app-cloud-aws-agents` — AWS agent facades (Bedrock Agents, Connect)
- `app-agent-patient` — Patient persona agent (PCA)
- `app-agent-communication` — Communication persona agent (COA)
- `app-agent-provider` — Provider persona agent (PSA)
- `app-agent-resource` — Resource admin persona agent (RRA)
- `app-agent-audit` — Audit persona agent (ACA)

### Package Naming
- Base package: `com.agenticcare`
- Module-specific: `com.agenticcare.common`, `com.agenticcare.dao`, `com.agenticcare.core`, etc.
- Agent-specific: `com.agenticcare.agent.patient`, `com.agenticcare.agent.communication`, etc.

### Dependency Rules
- `app-common` depends on nothing (pure DTOs, interfaces, enums)
- `app-dao` depends on `app-common`
- `app-core` depends on `app-common`, `app-dao`
- `app-cloud-*` modules depend on `app-common`
- `app-agent-*` modules depend on `app-common`, `app-core`, relevant `app-cloud-*`
- `app-wfs` depends on `app-common`, `app-core`, `app-agent-*`
- `app-web` depends on all (top-level assembly, Spring Boot main class lives here)

---

## Naming Conventions

### Classes
- Request DTOs: `*ReqDto` (e.g., `AppointmentReqDto`)
- Response DTOs: `*RespDto` (e.g., `AppointmentRespDto`)
- Execution Context: `*ExecCtx` (e.g., `AppointmentExecCtx`)
- Service interfaces: `I*Service` (e.g., `IRiskScoringService`)
- Service implementations: `*ServiceImpl` (e.g., `RiskScoringServiceImpl`)
- Facade interfaces: `I*Facade` (e.g., `IPatientContextFacade`)
- Facade implementations: `*FacadeImpl` (e.g., `PatientContextFacadeImpl`)
- Entities: `*Entity` (e.g., `AppointmentEntity`)
- Repositories: `*Repository` (e.g., `AppointmentRepository`)
- Controllers: `*Controller` (e.g., `AppointmentController`)
- Agents: `*Agent` (e.g., `PatientContextAgent`)
- Enums: descriptive name (e.g., `ContextState`, `SlotStatus`, `OutreachChannel`)
- Constants: `*Constants` (e.g., `AgentConstants`)

### Methods
- Service/facade methods take ExecCtx, return void — results go into RespDto
- Controller methods return `ResponseEntity<*RespDto>`
- No raw arguments in service/facade method signatures

### Enums (from paper)
```java
public enum ContextState {
    REACHABLE_STATIONARY, REACHABLE_MOBILE, UNREACHABLE
}

public enum SlotStatus {
    CONFIRMED, AT_RISK, VACANT
}

public enum OutreachChannel {
    VOICE_IVR, SMS_DEEPLINK, CALLBACK
}
```

---

## Spring AI Patterns

### Agent Definition
Agents use Spring AI `@Tool` annotations for capabilities that Amazon Bedrock can invoke.

```java
@Component
public class PatientContextAgent {

    @Tool(description = "Classify patient context state based on appointment metadata")
    public void classifyContext(AppointmentExecCtx ctx) {
        // Bedrock LLM call for C_p classification
    }

    @Tool(description = "Score patient no-show risk using XGBoost model")
    public void scoreRisk(AppointmentExecCtx ctx) {
        // XGBoost inference
    }
}
```

### Bedrock Integration
- Use Spring AI `ChatClient` with Amazon Bedrock auto-configuration
- Use `spring-ai-bedrock-starter` dependency
- Structured prompts for C_p classification
- Knowledge Bases for RAG over patient history

### Orchestration
- Use Spring AI `SequentialAgent` for PCA → COA pipeline
- Alternatively, use AWS Step Functions via `app-cloud-aws-wfs`

---

## Python Convention

Python is used ONLY for:
1. ML model training (XGBoost on Kaggle dataset)
2. Model export (ONNX for Java inference if needed)
3. Data analytics and visualization
4. Located in `python/` directory at repo root
5. Uses `requirements.txt` for dependencies
6. Scripts are standalone — no Flask/Django server

---

## API Design

### REST Endpoints
- `POST /api/v1/appointments/classify` — trigger PCA classification
- `POST /api/v1/appointments/outreach` — trigger COA channel selection
- `POST /api/v1/scenarios/run` — trigger end-to-end scenario
- `GET /api/v1/agents/status` — agent health check
- `GET /api/v1/analytics/metrics` — retrieve metrics

### All endpoints follow:
1. Accept ReqDto in body (POST) or build ReqDto from params (GET)
2. Create fresh RespDto
3. Wrap in ExecCtx
4. Call facade
5. Return RespDto

---

## Git & CI/CD

### Branching
- `main` — stable, deployable
- Feature branches for development

### GitHub Actions Workflows
- `bootstrap-infra.yml` — deploy CloudFormation stacks
- `deploy-agents.yml` — build Spring Boot, deploy to Lambda
- `run-scenario.yml` — trigger scenarios
- `extract-analytics.yml` — query results
- `shutdown-infra.yml` — tear down (cost control)

### Commit Messages
Include `Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>` when Claude assists.
