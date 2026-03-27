"""
Patient Context Agent (PCA) — Core Logic.
Platform-agnostic. No cloud SDK imports.

Classifies patient context state (C_p) and assesses risk using
multi-step reasoning chain. In production, the reasoning is augmented
by LLM (Bedrock Claude) — the wrapper provides LLM output, this core
processes it.

Input: PatientInput (or dict)
Output: PCAResult
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "Shared", "core"))
from models import PatientInput, PCAResult


def assess_patient(patient, llm_context_override=None):
    """
    Assess a patient's context state and risk.

    Args:
        patient: PatientInput or dict with patient fields
        llm_context_override: optional C_p from LLM (Bedrock Claude).
            If provided, PCA uses LLM's classification. If None, uses rules.

    Returns:
        PCAResult
    """
    if isinstance(patient, dict):
        patient = PatientInput(**{k: v for k, v in patient.items() if k in PatientInput.__init__.__code__.co_varnames})

    reasoning_steps = []
    risk_factors = []

    # Step 1: Engagement history
    if not patient.sms_received:
        reasoning_steps.append(
            "No prior SMS engagement — low digital health literacy or system disengagement.")
        risk_factors.append("no_sms_history")
    else:
        reasoning_steps.append("Patient has SMS engagement history — digital channel viable.")

    # Step 2: Time-based context
    is_weekday = patient.day_of_week < 5
    h = patient.hour

    if llm_context_override:
        c_p = llm_context_override
        reasoning_steps.append(f"LLM classified context as {c_p}.")
    elif is_weekday and (7 <= h <= 9):
        c_p = "REACHABLE_MOBILE"
        reasoning_steps.append("Weekday morning commute (7-9 AM) — patient likely in transit.")
    elif is_weekday and (16 <= h <= 19):
        c_p = "REACHABLE_MOBILE"
        reasoning_steps.append("Weekday evening commute (4-7 PM) — patient likely in transit.")
    elif is_weekday and (9 < h < 16):
        c_p = "REACHABLE_STATIONARY"
        reasoning_steps.append("Weekday work hours (9 AM-4 PM) — patient likely at desk/home.")
    else:
        c_p = "REACHABLE_STATIONARY"
        reasoning_steps.append(f"Off-hours (hour={h}, day={patient.day_of_week}) — defaulting to stationary.")

    # Step 3: Risk factors
    if patient.age >= 65:
        reasoning_steps.append(f"Elderly patient (age={patient.age}) — higher support needs.")
        risk_factors.append("elderly")

    if patient.hypertension or patient.diabetes:
        conditions = []
        if patient.hypertension: conditions.append("hypertension")
        if patient.diabetes: conditions.append("diabetes")
        reasoning_steps.append(f"Chronic conditions: {', '.join(conditions)} — appointment is clinically important.")
        risk_factors.append("chronic_conditions")

    if patient.lead_time_days > 14:
        reasoning_steps.append(f"Long lead time ({patient.lead_time_days} days) — patient may have forgotten.")
        risk_factors.append("long_lead_time")

    # Step 4: Override to UNREACHABLE if strong signals
    if not patient.sms_received and len(risk_factors) >= 2:
        c_p = "UNREACHABLE"
        reasoning_steps.append(
            f"OVERRIDE: {', '.join(risk_factors)} + no SMS → UNREACHABLE. Needs proactive callback.")

    # Step 5: Urgency
    if len(risk_factors) >= 2:
        urgency = "HIGH"
    elif len(risk_factors) >= 1:
        urgency = "MEDIUM"
    else:
        urgency = "ROUTINE"

    # Step 6: Simple risk score (0-1) based on factor count
    risk_score = min(1.0, len(risk_factors) * 0.25 + (0.1 if not patient.sms_received else 0))

    return PCAResult(
        patient_id=patient.patient_id,
        context_state=c_p,
        risk_score=round(risk_score, 3),
        urgency=urgency,
        risk_factors=risk_factors,
        reasoning_steps=reasoning_steps
    )
