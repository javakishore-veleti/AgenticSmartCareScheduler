"""
AWS Lambda handler for PCA agent.
Thin wrapper: parse Lambda event → call core pca_agent → Bedrock for LLM → return.
"""
import json
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "PatientContext", "core"))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "Shared", "core"))

import boto3
from pca_agent import assess_patient
from signal_checker import check_signals


bedrock = boto3.client("bedrock-runtime")
BEDROCK_MODEL_ID = os.environ.get("BEDROCK_MODEL_ID", "anthropic.claude-3-haiku-20240307-v1:0")


def invoke_bedrock_for_context(patient_data):
    """Call Bedrock Claude to classify patient context (optional LLM augmentation)."""
    try:
        prompt = (
            f"You are a Patient Context Agent. Classify this patient's reachability context.\n"
            f"Patient: age={patient_data.get('age')}, appointment hour={patient_data.get('hour')}, "
            f"day_of_week={patient_data.get('day_of_week')}, SMS history={'yes' if patient_data.get('sms_received') else 'no'}, "
            f"lead_time={patient_data.get('lead_time_days')} days.\n"
            f"Respond with exactly one of: REACHABLE_MOBILE, REACHABLE_STATIONARY, UNREACHABLE"
        )
        response = bedrock.invoke_model(
            modelId=BEDROCK_MODEL_ID,
            body=json.dumps({"anthropic_version": "bedrock-2023-05-31", "max_tokens": 50,
                             "messages": [{"role": "user", "content": prompt}]}),
            contentType="application/json"
        )
        result = json.loads(response["body"].read())
        text = result.get("content", [{}])[0].get("text", "").strip()
        if text in ("REACHABLE_MOBILE", "REACHABLE_STATIONARY", "UNREACHABLE"):
            return text
    except Exception as e:
        print(f"Bedrock call failed (falling back to rules): {e}")
    return None


def lambda_handler(event, context):
    """AWS Lambda entry point for PCA agent."""
    patient_data = event.get("patient", event)

    # Check signals before assessment
    signals = event.get("signals", [])
    signal_result = check_signals(signals)
    if not signal_result.should_outreach:
        return {
            "statusCode": 200,
            "action": "SKIPPED",
            "reason": signal_result.reason,
            "patient_id": patient_data.get("patient_id")
        }

    # Optional: LLM augmentation
    llm_context = invoke_bedrock_for_context(patient_data)

    # Core PCA assessment
    result = assess_patient(patient_data, llm_context_override=llm_context)

    return {
        "statusCode": 200,
        "pca_result": result.to_dict()
    }
