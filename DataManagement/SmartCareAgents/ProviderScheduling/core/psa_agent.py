"""
Provider Schedule Agent (PSA) — Core Logic.
Platform-agnostic. No cloud SDK imports.

Monitors confirmation status of appointments.
At T-90min: unconfirmed + R_p > 0.65 → escalate to RRA.
At T-30min: all unconfirmed → escalate to RRA.
"""


def check_slot_status(appointment, current_time_minutes_before):
    """
    Check if a slot needs escalation.

    Args:
        appointment: dict with confirmed (bool), risk_score (float)
        current_time_minutes_before: minutes before appointment time

    Returns:
        dict with action, reason
    """
    confirmed = appointment.get("confirmed", False)
    risk_score = appointment.get("risk_score", 0)
    patient_id = appointment.get("patient_id", "unknown")

    if confirmed:
        return {"action": "NONE", "reason": "Patient confirmed. No action needed."}

    if current_time_minutes_before <= 30:
        return {
            "action": "ESCALATE_TO_RRA",
            "reason": f"T-30min: Patient {patient_id} unconfirmed. Escalating to RRA for waitlist reallocation.",
            "urgency": "HIGH"
        }

    if current_time_minutes_before <= 90 and risk_score > 0.65:
        return {
            "action": "ESCALATE_TO_RRA",
            "reason": f"T-90min: Patient {patient_id} unconfirmed, R_p={risk_score:.2f} > 0.65. Escalating to RRA.",
            "urgency": "MEDIUM"
        }

    return {
        "action": "MONITOR",
        "reason": f"Patient {patient_id} unconfirmed but {current_time_minutes_before}min remaining. Monitoring."
    }
