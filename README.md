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

## GitHub Secrets Setup

The AWS deployment workflows require secrets configured in the GitHub repository. A helper script reads values from environment variables on your machine and pushes them via `gh secret set`.

```bash
# 1. Export required secrets
export AGENTIC_SCS_CLAIMS_PROC_AWS_ACCESS_KEY_ID="..."
export AGENTIC_SCS_AWS_SECRET_ACCESS_KEY="..."
export AGENTIC_SCS_AES_ENCRYPTION_KEY="..."
export AGENTIC_SCS_ENCRYPTION_KEY="..."
export AGENTIC_SCS_JWT_SIGNING_KEY="..."
export AGENTIC_SCS_OPENSEARCH_MASTER_PASSWORD="..."
export AGENTIC_SCS_RDS_MASTER_PASSWORD="..."
export AGENTIC_SCS_REDIS_AUTH_TOKEN="..."

# 2. Optional — only if using an existing VPC (select "use_existing" in AWS-001)
#    If you select "create_new", the VPC is created by CloudFormation and
#    the ID is passed to downstream workflows automatically.
export AGENTIC_SCS_AWS_REGION="us-east-1"
export AGENTIC_SCS_AWS_VPC_ID="vpc-..."

# 3. Push to GitHub (requires gh CLI authenticated)
npm run github:repo:secrets:push
```

The script exits with an error if any required secrets are missing, and notes which optional ones were skipped.

## Author

Aruna Kishore Veleti, Senior Member, IEEE
