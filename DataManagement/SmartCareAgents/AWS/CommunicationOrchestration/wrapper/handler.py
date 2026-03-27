"""
AWS Lambda handler for COA agent.
Thin wrapper: parse event → call core coa_agent → Connect/SNS boto3 → return.
"""
import json
import os
import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "CommunicationOrchestration", "core"))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "Shared", "core"))

import boto3
from coa_agent import select_channel, determine_action

connect = boto3.client("connect")
sns = boto3.client("sns")
CONNECT_INSTANCE_ID = os.environ.get("CONNECT_INSTANCE_ID", "")
SNS_TOPIC_ARN = os.environ.get("OUTREACH_SNS_TOPIC_ARN", "")


def execute_outreach(channel, patient_data):
    """Execute outreach via AWS Connect or SNS. Returns simulated response for now."""
    patient_id = patient_data.get("patient_id", "unknown")

    if channel == "VOICE_IVR" and CONNECT_INSTANCE_ID:
        try:
            # In production: connect.start_outbound_voice_contact(...)
            print(f"COA: Would place IVR call to {patient_id} via Connect {CONNECT_INSTANCE_ID}")
        except Exception as e:
            print(f"Connect call failed: {e}")

    elif channel == "SMS_DEEPLINK" and SNS_TOPIC_ARN:
        try:
            sns.publish(
                TopicArn=SNS_TOPIC_ARN,
                Message=json.dumps({"patient_id": patient_id, "type": "appointment_reminder"}),
                Subject="SmartCare Appointment Reminder"
            )
            print(f"COA: SMS sent to {patient_id} via SNS")
        except Exception as e:
            print(f"SNS publish failed: {e}")

    # Simulated response (in production, this comes from Connect callback/SMS reply)
    import random
    random.seed(hash(patient_id))
    rates = {
        "VOICE_IVR": {"confirmed": 0.60, "rescheduled": 0.15, "no_answer": 0.20, "cancelled": 0.05},
        "SMS_DEEPLINK": {"confirmed": 0.55, "rescheduled": 0.20, "no_answer": 0.20, "cancelled": 0.05},
        "CALLBACK": {"confirmed": 0.35, "rescheduled": 0.15, "no_answer": 0.40, "cancelled": 0.10}
    }
    r = rates.get(channel, rates["SMS_DEEPLINK"])
    return random.choices(list(r.keys()), weights=list(r.values()), k=1)[0]


def lambda_handler(event, context):
    """AWS Lambda entry point for COA agent."""
    pca_result = event.get("pca_result", {})
    patient_data = event.get("patient", event)

    channel_info = select_channel(pca_result)
    response = execute_outreach(channel_info["channel"], patient_data)
    coa_result = determine_action(pca_result, channel_info, outreach_response=response)

    return {
        "statusCode": 200,
        "coa_result": coa_result.to_dict()
    }
