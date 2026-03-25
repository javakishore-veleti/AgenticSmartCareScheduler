# AgenticSmartCareScheduler

Cloud-native, multi-agent AI orchestration framework for context-aware healthcare appointment coordination. Built on AWS.

## Problem

10–30% of outpatient appointments globally result in no-shows due to **communication-context mismatch** — patients receive outreach through channels they cannot interact with at the moment of contact.

## Solution

Five specialized AI agents coordinate through AWS Step Functions to infer patient context, select the optimal outreach channel, monitor provider schedules, reallocate at-risk slots, and maintain compliance audit trails.

## Architecture

| Agent | AWS Service | Function |
|---|---|---|
| Patient Context Agent (PCA) | Amazon Bedrock, Lambda | Risk scoring + context classification |
| Communication Orchestration Agent (COA) | Amazon Connect, SNS | Adaptive channel selection |
| Provider Schedule Agent (PSA) | Amazon OpenSearch, EventBridge | Schedule monitoring + escalation |
| Resource Reallocation Agent (RRA) | Amazon OpenSearch, Step Functions | Waitlist reallocation |
| Audit & Compliance Agent (ACA) | Amazon OpenSearch (ILM) | Immutable audit logging |

## Tech Stack

- **AI/ML**: Amazon Bedrock Ecosystem, XGBoost, Vector Search
- **Compute**: AWS Lambda (Python 3.12)
- **Communication**: Amazon Connect, Amazon SNS
- **Data**: Amazon OpenSearch, AWS HealthLake
- **Orchestration**: AWS Step Functions, Amazon EventBridge
- **IaC**: AWS CloudFormation, GitHub Actions

## Author

Aruna Kishore Veleti, Senior Member, IEEE
