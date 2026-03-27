"""AWS Lambda handler for RRA agent."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "ResourceReallocation", "core"))
from rra_agent import decide_reallocation


def lambda_handler(event, context):
    escalation = event.get("escalation", {})
    waitlist = event.get("waitlist", [])
    result = decide_reallocation(escalation, waitlist)
    return {"statusCode": 200, "rra_result": result}
