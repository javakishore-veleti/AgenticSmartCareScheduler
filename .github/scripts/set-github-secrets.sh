#!/usr/bin/env bash
# ------------------------------------------------------------------
# set-github-secrets.sh
#
# Reads secret values from environment variables on the developer's
# laptop and pushes them to the GitHub repo via `gh secret set`.
#
# Prerequisites:
#   - gh CLI installed and authenticated (`gh auth login`)
#   - Environment variables exported before running this script
#
# Usage:
#   export AGENTIC_SCS_CLAIMS_PROC_AWS_ACCESS_KEY_ID="..."
#   export AGENTIC_SCS_AWS_SECRET_ACCESS_KEY="..."
#   ...
#   npm run github:repo:secrets:push
# ------------------------------------------------------------------
set -euo pipefail

# --- Required secrets (always needed) ---
REQUIRED_SECRETS=(
  AGENTIC_SCS_CLAIMS_PROC_AWS_ACCESS_KEY_ID
  AGENTIC_SCS_AWS_SECRET_ACCESS_KEY
  AGENTIC_SCS_AES_ENCRYPTION_KEY
  AGENTIC_SCS_ENCRYPTION_KEY
  AGENTIC_SCS_JWT_SIGNING_KEY
  AGENTIC_SCS_OPENSEARCH_MASTER_PASSWORD
  AGENTIC_SCS_RDS_MASTER_PASSWORD
  AGENTIC_SCS_REDIS_AUTH_TOKEN
)

# --- Optional secrets (only needed when using an existing VPC/region) ---
# If you select "create_new" in AWS-001, the VPC ID is created by
# CloudFormation and passed to downstream workflows automatically.
# AGENTIC_SCS_AWS_REGION defaults to us-east-1 if not set.
# Set these only if you have a pre-existing VPC you want to reuse.
OPTIONAL_SECRETS=(
  AGENTIC_SCS_AWS_REGION
  AGENTIC_SCS_AWS_VPC_ID
)

# Verify gh CLI is available
if ! command -v gh &>/dev/null; then
  echo "ERROR: gh CLI not found. Install from https://cli.github.com/"
  exit 1
fi

# Verify gh is authenticated
if ! gh auth status &>/dev/null; then
  echo "ERROR: gh CLI not authenticated. Run: gh auth login"
  exit 1
fi

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)
if [ -z "$REPO" ]; then
  echo "ERROR: Not in a GitHub repo or remote not configured."
  exit 1
fi

echo "Pushing secrets to: $REPO"
echo ""

MISSING=()
SET_COUNT=0

echo "=== Required Secrets ==="
for SECRET_NAME in "${REQUIRED_SECRETS[@]}"; do
  VALUE="${!SECRET_NAME:-}"
  if [ -z "$VALUE" ]; then
    MISSING+=("$SECRET_NAME")
    echo "SKIP  $SECRET_NAME  (env var not set)"
  else
    echo -n "$VALUE" | gh secret set "$SECRET_NAME" --repo "$REPO"
    echo "SET   $SECRET_NAME"
    ((SET_COUNT++))
  fi
done

echo ""
echo "=== Optional Secrets (only for 'use_existing' VPC mode) ==="
OPTIONAL_SKIPPED=0
for SECRET_NAME in "${OPTIONAL_SECRETS[@]}"; do
  VALUE="${!SECRET_NAME:-}"
  if [ -z "$VALUE" ]; then
    ((OPTIONAL_SKIPPED++))
    echo "SKIP  $SECRET_NAME  (not set — OK if using 'create_new' VPC mode)"
  else
    echo -n "$VALUE" | gh secret set "$SECRET_NAME" --repo "$REPO"
    echo "SET   $SECRET_NAME"
    ((SET_COUNT++))
  fi
done

echo ""
echo "---"
echo "Done: $SET_COUNT secrets set."

if [ ${#MISSING[@]} -gt 0 ]; then
  echo ""
  echo "REQUIRED secrets missing (export these and re-run):"
  for m in "${MISSING[@]}"; do
    echo "  export $m=\"...\""
  done
  exit 1
fi

if [ $OPTIONAL_SKIPPED -gt 0 ]; then
  echo ""
  echo "Note: Optional secrets were skipped. This is fine if you plan to"
  echo "select 'create_new' in the AWS-001 VPC workflow, which creates a"
  echo "VPC via CloudFormation and passes the ID to downstream services."
fi
