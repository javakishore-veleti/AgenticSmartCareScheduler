"""
Communication Orchestration Agent (COA) — Core Logic.
Platform-agnostic. No cloud SDK imports.

Selects outreach channel based on PCA's context classification,
executes outreach (via wrapper), and determines next action.

Input: PCAResult + optional outreach_response from wrapper
Output: COAResult
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "Shared", "core"))
from models import PCAResult, COAResult

CHANNEL_MAP = {
    "REACHABLE_MOBILE": {
        "channel": "VOICE_IVR",
        "label": "Voice IVR Call",
        "service": "Amazon Connect",
        "reasoning": "Mobile context — interactive voice call allows immediate confirmation while patient is alert."
    },
    "REACHABLE_STATIONARY": {
        "channel": "SMS_DEEPLINK",
        "label": "SMS Deep-Link",
        "service": "Amazon SNS",
        "reasoning": "Stationary context — rich SMS with tap-to-confirm link is optimal for desk/home."
    },
    "UNREACHABLE": {
        "channel": "CALLBACK",
        "label": "Scheduled Callback",
        "service": "Amazon Connect",
        "reasoning": "Unreachable via standard channels — human-assisted callback gives best engagement chance."
    }
}


def select_channel(pca_result):
    """
    Select outreach channel based on PCA result.

    Args:
        pca_result: PCAResult or dict

    Returns:
        dict with channel, label, service, reasoning
    """
    if isinstance(pca_result, dict):
        c_p = pca_result.get("context_state", "REACHABLE_STATIONARY")
    else:
        c_p = pca_result.context_state

    return CHANNEL_MAP.get(c_p, CHANNEL_MAP["REACHABLE_STATIONARY"])


def determine_action(pca_result, channel_info, outreach_response=None):
    """
    Determine action after outreach attempt.

    Args:
        pca_result: PCAResult or dict
        channel_info: dict from select_channel
        outreach_response: str — confirmed, rescheduled, no_answer, cancelled
            If None, outreach hasn't been executed yet (just planning).

    Returns:
        COAResult
    """
    if isinstance(pca_result, dict):
        patient_id = pca_result.get("patient_id", "unknown")
        urgency = pca_result.get("urgency", "ROUTINE")
    else:
        patient_id = pca_result.patient_id
        urgency = pca_result.urgency

    channel = channel_info["channel"]
    label = channel_info["label"]

    if outreach_response is None:
        return COAResult(
            patient_id=patient_id,
            channel=channel,
            channel_label=label,
            reasoning=channel_info["reasoning"],
            patient_response="pending",
            action_taken=f"Outreach planned via {label}. Awaiting execution.",
            action_status="PENDING",
            needs_admin=False
        )

    needs_admin = False
    if outreach_response == "confirmed":
        action_taken = f"Patient confirmed appointment via {label}. Slot secured."
        action_status = "ACTION_TAKEN"
    elif outreach_response == "rescheduled":
        action_taken = f"Patient requested reschedule via {label}. Slot released to waitlist."
        action_status = "ACTION_TAKEN"
    elif outreach_response == "no_answer":
        if urgency in ("HIGH", "MEDIUM"):
            action_taken = f"No response to {label}. ESCALATED: high-urgency patient needs manual follow-up."
            action_status = "ESCALATED_TO_ADMIN"
            needs_admin = True
        else:
            action_taken = f"No response to {label}. Queued for retry in 2 hours."
            action_status = "ACTION_TAKEN"
    elif outreach_response == "cancelled":
        action_taken = f"Patient cancelled via {label}. Slot released. RRA agent offering to waitlist."
        action_status = "ACTION_TAKEN"
        needs_admin = True
    else:
        action_taken = f"Unknown response: {outreach_response}"
        action_status = "ACTION_TAKEN"

    return COAResult(
        patient_id=patient_id,
        channel=channel,
        channel_label=label,
        reasoning=channel_info["reasoning"],
        patient_response=outreach_response,
        action_taken=action_taken,
        action_status=action_status,
        needs_admin=needs_admin
    )
