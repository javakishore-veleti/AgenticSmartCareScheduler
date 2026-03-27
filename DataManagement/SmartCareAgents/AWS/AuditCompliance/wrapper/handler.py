"""AWS Lambda handler for ACA agent."""
import json
import os
import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "AuditCompliance", "core"))

import boto3
from aca_agent import build_audit_record, check_contact_frequency

opensearch_endpoint = os.environ.get("OPENSEARCH_ENDPOINT", "")


def lambda_handler(event, context):
    agent_name = event.get("agent_name", "UNKNOWN")
    event_type = event.get("event_type", "UNKNOWN")
    patient_id = event.get("patient_id", "unknown")
    decision = event.get("decision_rationale", "")
    outcome = event.get("outcome", "")

    audit = build_audit_record(agent_name, event_type, patient_id, decision, outcome)

    # Write to OpenSearch (if configured)
    if opensearch_endpoint:
        try:
            import requests
            from requests_aws4auth import AWS4Auth
            credentials = boto3.Session().get_credentials()
            auth = AWS4Auth(credentials.access_key, credentials.secret_key,
                           os.environ.get("AWS_REGION", "us-east-1"), "es",
                           session_token=credentials.token)
            url = f"{opensearch_endpoint}/audit_log/_doc/{audit['audit_id']}"
            requests.put(url, auth=auth, json=audit, headers={"Content-Type": "application/json"})
        except Exception as e:
            print(f"OpenSearch write failed: {e}")

    return {"statusCode": 200, "audit_record": audit}
