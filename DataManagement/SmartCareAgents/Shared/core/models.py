"""
Shared data models for SmartCare agents.
Platform-agnostic — no cloud SDK imports.
"""


class PatientInput:
    """Input to PCA agent — one patient's appointment data."""
    def __init__(self, patient_id, age, gender=None, appointment_day=None,
                 scheduled_day=None, lead_time_days=0, day_of_week=0, hour=12,
                 sms_received=0, scholarship=0, hypertension=0, diabetes=0,
                 alcoholism=0, handicap=0, neighbourhood=None):
        self.patient_id = patient_id
        self.age = age
        self.gender = gender
        self.appointment_day = appointment_day
        self.scheduled_day = scheduled_day
        self.lead_time_days = lead_time_days
        self.day_of_week = day_of_week
        self.hour = hour
        self.sms_received = sms_received
        self.scholarship = scholarship
        self.hypertension = hypertension
        self.diabetes = diabetes
        self.alcoholism = alcoholism
        self.handicap = handicap
        self.neighbourhood = neighbourhood

    def to_dict(self):
        return self.__dict__


class PCAResult:
    """Output from PCA agent."""
    def __init__(self, patient_id, context_state, risk_score, urgency,
                 risk_factors, reasoning_steps):
        self.patient_id = patient_id
        self.context_state = context_state  # REACHABLE_MOBILE, REACHABLE_STATIONARY, UNREACHABLE
        self.risk_score = risk_score        # R_p in [0,1]
        self.urgency = urgency              # HIGH, MEDIUM, ROUTINE
        self.risk_factors = risk_factors    # list of strings
        self.reasoning_steps = reasoning_steps  # list of reasoning strings

    def to_dict(self):
        return self.__dict__


class COAResult:
    """Output from COA agent."""
    def __init__(self, patient_id, channel, channel_label, reasoning,
                 patient_response, action_taken, action_status, needs_admin):
        self.patient_id = patient_id
        self.channel = channel              # VOICE_IVR, SMS_DEEPLINK, CALLBACK
        self.channel_label = channel_label  # human-readable
        self.reasoning = reasoning
        self.patient_response = patient_response  # confirmed, rescheduled, no_answer, cancelled
        self.action_taken = action_taken
        self.action_status = action_status  # ACTION_TAKEN, SKIPPED, ESCALATED_TO_ADMIN
        self.needs_admin = needs_admin

    def to_dict(self):
        return self.__dict__


class SignalCheckResult:
    """Result of checking patient signals before outreach."""
    def __init__(self, should_outreach, reason, latest_signal_type=None):
        self.should_outreach = should_outreach
        self.reason = reason
        self.latest_signal_type = latest_signal_type

    def to_dict(self):
        return self.__dict__
