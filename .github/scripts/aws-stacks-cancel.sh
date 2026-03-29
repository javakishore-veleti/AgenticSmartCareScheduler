#!/usr/bin/env bash
# ------------------------------------------------------------------
# aws-stacks-cancel.sh
#
# Lists all smartcare-* CloudFormation stacks that are in progress
# and offers to cancel or delete them.
#
# Prerequisites:
#   - AWS CLI configured (aws configure or env vars)
#
# Usage:
#   npm run aws:stacks:cancel
# ------------------------------------------------------------------
set -euo pipefail

REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}"
PREFIX="smartcare-"

echo "Region: $REGION"
echo "Checking for in-progress smartcare-* stacks..."
echo ""

# Find stacks in any in-progress state
IN_PROGRESS=$(aws cloudformation list-stacks \
  --region "$REGION" \
  --stack-status-filter \
    CREATE_IN_PROGRESS \
    UPDATE_IN_PROGRESS \
    DELETE_IN_PROGRESS \
    UPDATE_ROLLBACK_IN_PROGRESS \
    UPDATE_COMPLETE_CLEANUP_IN_PROGRESS \
    ROLLBACK_IN_PROGRESS \
  --query "StackSummaries[?starts_with(StackName, '${PREFIX}')].[StackName,StackStatus]" \
  --output text 2>/dev/null || true)

if [ -z "$IN_PROGRESS" ]; then
  echo "No in-progress smartcare-* stacks found."
  echo ""
  echo "Current stacks:"
  aws cloudformation list-stacks \
    --region "$REGION" \
    --stack-status-filter \
      CREATE_COMPLETE \
      UPDATE_COMPLETE \
      ROLLBACK_COMPLETE \
      UPDATE_ROLLBACK_COMPLETE \
      CREATE_FAILED \
    --query "StackSummaries[?starts_with(StackName, '${PREFIX}')].[StackName,StackStatus]" \
    --output table 2>/dev/null || echo "  (none)"
  exit 0
fi

echo "In-progress stacks:"
echo "---"
echo "$IN_PROGRESS" | while read -r name status; do
  echo "  $name  ($status)"
done
echo "---"
echo ""

read -rp "Cancel/delete ALL in-progress stacks above? [y/N] " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 0
fi

echo ""
echo "$IN_PROGRESS" | while read -r name status; do
  case "$status" in
    UPDATE_IN_PROGRESS|UPDATE_ROLLBACK_IN_PROGRESS|UPDATE_COMPLETE_CLEANUP_IN_PROGRESS)
      echo "Cancelling update: $name ..."
      aws cloudformation cancel-update-stack --stack-name "$name" --region "$REGION" 2>/dev/null || \
        echo "  cancel-update failed — attempting delete"
      ;;
    CREATE_IN_PROGRESS|ROLLBACK_IN_PROGRESS)
      echo "Deleting (cannot cancel create): $name ..."
      aws cloudformation delete-stack --stack-name "$name" --region "$REGION" 2>/dev/null || \
        echo "  delete failed for $name"
      ;;
    DELETE_IN_PROGRESS)
      echo "Already deleting: $name — skipping"
      ;;
    *)
      echo "Unknown state $status for $name — skipping"
      ;;
  esac
done

echo ""
echo "Done. Run again in a minute to verify stacks are resolved."
