"""
Resource Reallocation Agent (RRA) — Core Logic.
Platform-agnostic. No cloud SDK imports.

Receives escalated slots from PSA, ranks waitlist candidates,
triggers COA outreach to top candidate.
"""


def rank_waitlist_candidates(waitlist, slot_time, slot_provider):
    """
    Rank waitlist candidates for a given slot.

    Args:
        waitlist: list of dicts with patient_id, urgency, distance_minutes, preference
        slot_time: appointment time string
        slot_provider: provider name

    Returns:
        list of candidates sorted by priority (best first)
    """
    def score(candidate):
        s = 0
        if candidate.get("urgency") == "HIGH": s += 100
        elif candidate.get("urgency") == "MEDIUM": s += 50
        if candidate.get("distance_minutes", 999) < 30: s += 30
        if candidate.get("sms_received"): s += 10
        return s

    ranked = sorted(waitlist, key=score, reverse=True)
    return ranked


def decide_reallocation(escalation, waitlist):
    """
    Decide whether to reallocate a slot.

    Args:
        escalation: dict from PSA (action, reason, urgency)
        waitlist: list of candidate dicts

    Returns:
        dict with action, candidate, reason
    """
    if not waitlist:
        return {
            "action": "NO_CANDIDATES",
            "candidate": None,
            "reason": "Waitlist is empty. Slot remains unconfirmed."
        }

    ranked = rank_waitlist_candidates(waitlist, None, None)
    top = ranked[0]

    return {
        "action": "OFFER_SLOT",
        "candidate": top,
        "reason": f"Offering slot to {top.get('patient_id')} (urgency: {top.get('urgency', 'unknown')}). Triggering COA outreach."
    }
