"""AWS Lambda handler for PSA agent."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "ProviderScheduling", "core"))
from psa_agent import check_slot_status


def lambda_handler(event, context):
    appointment = event.get("appointment", event)
    minutes_before = event.get("minutes_before_appointment", 90)
    result = check_slot_status(appointment, minutes_before)
    return {"statusCode": 200, "psa_result": result}
