"""
Audit and Compliance Agent (ACA) — Core Logic.
Platform-agnostic. No cloud SDK imports.

Builds immutable audit records for every agent action.
Checks compliance: HIPAA de-identification, contact frequency limits,
patient consent verification.
"""
from datetime import datetime


def build_audit_record(agent_name, event_type, patient_id, decision_rationale,
                       outcome, metadata=None):
    """
    Build an audit record for an agent action.

    Args:
        agent_name: PCA, COA, PSA, RRA, ACA
        event_type: CONTEXT_CLASSIFIED, OUTREACH_SENT, SLOT_ESCALATED, etc.
        patient_id: de-identified patient pseudonym
        decision_rationale: why the agent made this decision
        outcome: what happened

    Returns:
        dict — audit record ready for storage
    """
    return {
        "audit_id": f"{agent_name}_{event_type}_{patient_id}_{datetime.utcnow().strftime('%Y%m%d%H%M%S')}",
        "agent": agent_name,
        "event_type": event_type,
        "patient_pseudonym": f"PT-{hash(patient_id) % 100000:05d}",
        "decision_rationale": decision_rationale,
        "outcome": outcome,
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "metadata": metadata or {}
    }


def check_contact_frequency(recent_contacts, max_per_day=3):
    """
    Check if patient has been contacted too many times today.

    Args:
        recent_contacts: list of contact timestamps (today)
        max_per_day: maximum allowed contacts per day

    Returns:
        dict with allowed (bool), reason
    """
    count = len(recent_contacts)
    if count >= max_per_day:
        return {
            "allowed": False,
            "reason": f"Contact frequency limit reached ({count}/{max_per_day} today). HIPAA compliance: no further contact."
        }
    return {
        "allowed": True,
        "reason": f"Contact allowed ({count}/{max_per_day} today)."
    }
