"""
Signal checker — guard logic before outreach.
Platform-agnostic. Receives signals as a list of dicts.

Signals that SKIP outreach:
  PATIENT_ON_MY_WAY, PATIENT_CONFIRMED, PROVIDER_PATIENT_ARRIVED, KIOSK_CHECK_IN

Signals that CANCEL and release slot:
  PATIENT_CANCEL

No signal + high risk → PROCEED with outreach.
"""
from .models import SignalCheckResult

SKIP_SIGNALS = {
    "PATIENT_ON_MY_WAY",
    "PATIENT_CONFIRMED",
    "PROVIDER_PATIENT_ARRIVED",
    "KIOSK_CHECK_IN"
}

CANCEL_SIGNALS = {
    "PATIENT_CANCEL"
}


def check_signals(signals):
    """
    Check patient signals to determine if outreach should proceed.

    Args:
        signals: list of dicts with at least 'signal_type' key,
                 ordered by most recent first.

    Returns:
        SignalCheckResult
    """
    if not signals:
        return SignalCheckResult(
            should_outreach=True,
            reason="No signals received — patient status unknown, proceed with outreach."
        )

    latest = signals[0]
    signal_type = latest.get("signal_type", "")

    if signal_type in SKIP_SIGNALS:
        return SignalCheckResult(
            should_outreach=False,
            reason=f"Patient already {signal_type.replace('_', ' ').lower()}. Skipping outreach to avoid unnecessary contact.",
            latest_signal_type=signal_type
        )

    if signal_type in CANCEL_SIGNALS:
        return SignalCheckResult(
            should_outreach=False,
            reason="Patient cancelled. Slot released to waitlist. RRA agent should take over.",
            latest_signal_type=signal_type
        )

    if signal_type == "PATIENT_RUNNING_LATE":
        return SignalCheckResult(
            should_outreach=False,
            reason="Patient is running late but acknowledged. No outreach needed — provider notified.",
            latest_signal_type=signal_type
        )

    if signal_type == "OUTREACH_SENT":
        return SignalCheckResult(
            should_outreach=False,
            reason="Outreach already sent for this appointment. Waiting for response before retry.",
            latest_signal_type=signal_type
        )

    return SignalCheckResult(
        should_outreach=True,
        reason=f"Latest signal: {signal_type} — does not prevent outreach.",
        latest_signal_type=signal_type
    )
