# AgenticSmartCareScheduler

## IMPORTANT — Read These Files First
Before scanning the codebase, read these files for full context:
1. **CLAUDE.md** (this file) — architecture, agents, dataset, AWS stack
2. **CodingConventions.md** — ExecCtx/ReqDto/RespDto pattern, module structure, naming, Spring AI patterns
3. **ArchitectureDecisions.md** — databases, observability, DevOps/Local, Angular portals, Bedrock, EKS, profiles
4. **DevelopmentPlan.md** — 5-phase plan, scenarios, outputs

These three files contain everything needed to understand and contribute to this project without scanning the full codebase.

## Project Context

This is the implementation repo for a cloud-native, multi-agent AI orchestration framework that addresses healthcare appointment no-shows through context-aware communication. The framework is described in an IEEE ICTS4eHealth 2026 conference paper.

## Business Problem

- 10–30% of outpatient appointments globally result in no-shows
- Root cause identified as **communication-context mismatch**: outreach channel interaction demands exceed patient's capacity at the time of contact (e.g., patient in transit receives portal authentication request)
- Existing SMS reminders achieve only marginal reductions because channel selection is static

## Architecture — Five Agents

### Three Personas
- **Patient**: characterized by (C_p, R_p, P_p) — context state, risk score, communication preference
- **Provider**: characterized by (S_v, T_v) — slot status, escalation threshold
- **Resource Administrator**: characterized by (W, f_r(Δt)) — waitlist queue, reallocation function

### Context States
- C_p ∈ {reachable-stationary, reachable-mobile, unreachable}

### Five Agents (AWS Lambda)
1. **PCA (Patient Context Agent)** — Amazon Bedrock + XGBoost: risk score R_p, context state C_p at T-24h, T-4h, T-90min
2. **COA (Communication Orchestration Agent)** — Amazon Connect + SNS: adaptive channel selection (voice IVR / SMS / callback) based on C_p
3. **PSA (Provider Schedule Agent)** — Amazon OpenSearch + EventBridge: monitors confirmations, escalates at T-90min and T-30min
4. **RRA (Resource Reallocation Agent)** — Amazon OpenSearch + Step Functions: queries waitlist, triggers COA outreach, reserves slots
5. **ACA (Audit & Compliance Agent)** — Amazon OpenSearch ILM: immutable logging, de-identification

### AWS Technology Stack
| Capability | AWS Service |
|---|---|
| Foundation Models & Knowledge | Amazon Bedrock Ecosystem, Bedrock Knowledge Bases |
| Agentic Compute | AWS Lambda (Python 3.12) |
| Intelligent Communication | Amazon Connect, Amazon SNS |
| Real-Time Data & Digital Twin | Amazon OpenSearch Service (+ Vector Search) |
| Healthcare Data Integration | AWS HealthLake, Amazon EventBridge |
| Agent Orchestration | AWS Step Functions, Amazon EventBridge |
| Infrastructure | AWS CloudFormation |

### Digital Twin
DT(t) = {P(t), V(t), W(t), E(t)} — patient states, provider schedules, waitlist, event log — hosted on Amazon OpenSearch

### Channel Selection Logic (Table I from paper)
| Channel | Visual | Manual | Cognitive | Stationary | Mobile | Unreachable |
|---|---|---|---|---|---|---|
| Patient portal | High | High | High | ✓ | ✗ | ✗ |
| SMS deep-link | Medium | Medium | Low | ✓ | Partial | ✗ |
| Voice IVR (single-keypress) | None | Minimal | Low | ✓ | ✓ | ✗ |
| Callback scheduling | None | None | None | ✓ | ✓ | ✓ (deferred) |

### COA Channel Selection Rules
- Reachable-stationary (R_p > 0.5): SMS deep-link via Amazon SNS
- Reachable-mobile: Amazon Connect voice IVR (single keypress)
- Unreachable: Amazon Connect callback scheduling

### PSA Escalation Rules
- T-90min: unconfirmed with R_p > 0.65 → escalate to RRA
- T-30min: all unconfirmed → escalate regardless of R_p

## Dataset

- **Medical Appointment No-Show dataset** (Kaggle): https://www.kaggle.com/datasets/joniarroba/noshowappointments
- 110,527 records, ~20% no-show rate, 14 columns
- Features: PatientId, AppointmentDay, ScheduledDay, Age, Gender, Neighbourhood, Scholarship, Hypertension, Diabetes, Alcoholism, Handicap, SMS_received, No-show

## Implementation Scope (5 hours)

### Focus: PCA + COA Pipeline
1. **PCA Risk Model**: XGBoost on Kaggle dataset → real F1, AUC, precision, recall
2. **PCA Context Classifier**: Amazon Bedrock for C_p inference
3. **COA Channel Selector**: Route patients to IVR/SMS/callback based on C_p
4. **End-to-End Simulation**: Run PCA→COA on full 110K dataset → channel distribution, outreach metrics
5. **AWS Deployment**: Lambda + EventBridge via GitHub Actions + CloudFormation
6. **Analytics**: Results visualization

### Tech Stack
- Python 3.12 (primary)
- Java, Go (supporting services if needed)
- Angular/React (dashboard if time permits)
- GitHub Actions (CI/CD, infra bootstrap/shutdown)
- AWS CloudFormation (IaC)

## Author
Aruna Kishore Veleti, Senior Member, IEEE
Independent Researcher, Monroe, North Carolina, USA

## Related Paper Repo
IEEE-ICTS4eHealth-2026-CallForPapers (same parent directory)
