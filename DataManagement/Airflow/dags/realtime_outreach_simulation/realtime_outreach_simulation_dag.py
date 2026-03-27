"""
Real-Time Outreach Simulation DAG

Simulates live agentic orchestration on individual patient records:
  1. Sample patients from dataset
  2. For each patient: PCA assesses → COA decides → Action executed
  3. Generate action log with outcomes (confirmed, rescheduled, escalated)
  4. Surface admin alerts for human-in-the-loop cases

Uses Airflow AI SDK pattern (fallback to standard Python when AI SDK not available).
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status, resolve_dataset_path, resolve_output_dir

DAG_ID = "realtime_outreach_simulation"
default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}

# Number of patients to simulate in detail (keeps runtime reasonable)
SAMPLE_SIZE = 50


def simulate_outreach(**kwargs):
    """
    Simulate real-time PCA→COA→Action pipeline on individual patients.
    Each patient gets a full agent decision chain with reasoning.
    """
    import json
    import pandas as pd
    import numpy as np
    from sklearn.model_selection import StratifiedKFold, cross_val_predict
    from xgboost import XGBClassifier
    import random

    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")
    output_dir = resolve_output_dir(conf.get("outputDir"), run_id)

    notify_broker(run_id, DAG_ID, "RUNNING")
    update_run_status(run_id, "RUNNING")

    # Load dataset
    csv_file = resolve_dataset_path(dataset_path) if dataset_path else None
    if not csv_file or not os.path.isfile(csv_file):
        fallback_base = "/home/airflow/runtime_data/DataSets/SmartCare-Admin/Datasets-Loaded"
        if os.path.isdir(fallback_base):
            import glob
            csvs = glob.glob(os.path.join(fallback_base, "**/*.csv"), recursive=True)
            if csvs:
                csv_file = csvs[0]
    if not csv_file:
        raise FileNotFoundError(f"No dataset found: {dataset_path}")

    print(f"Loading dataset: {csv_file}")
    df = pd.read_csv(csv_file)
    df['ScheduledDay'] = pd.to_datetime(df['ScheduledDay'])
    df['AppointmentDay'] = pd.to_datetime(df['AppointmentDay'])
    df['LeadTimeDays'] = (df['AppointmentDay'] - df['ScheduledDay']).dt.days
    df['DayOfWeek'] = df['AppointmentDay'].dt.dayofweek
    df['Hour'] = df['ScheduledDay'].dt.hour
    df['NoShow'] = (df['No-show'] == 'Yes').astype(int)
    df = df[df['LeadTimeDays'] >= 0]

    features = ['Age', 'Scholarship', 'Hipertension', 'Diabetes', 'Alcoholism',
                'Handcap', 'SMS_received', 'LeadTimeDays', 'DayOfWeek', 'Hour']
    X = df[features].fillna(0)
    y = df['NoShow']

    # Train model for R_p
    print("Training XGBoost for R_p scores...")
    model = XGBClassifier(n_estimators=200, max_depth=6, learning_rate=0.1,
                          subsample=0.8, colsample_bytree=0.8, eval_metric='logloss', random_state=42)
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    df['R_p'] = cross_val_predict(model, X, y, cv=cv, method='predict_proba')[:, 1]

    # Sample patients for detailed simulation
    sample = df.sample(n=min(SAMPLE_SIZE, len(df)), random_state=42).copy()
    random.seed(42)

    # Generate fake names for readability
    first_names = ['Maria', 'James', 'Aisha', 'Robert', 'Sofia', 'Carlos', 'Elena', 'David',
                   'Priya', 'Michael', 'Ana', 'Luis', 'Fatima', 'John', 'Mei', 'Ahmed',
                   'Rachel', 'Pedro', 'Yuki', 'Thomas', 'Zara', 'Diego', 'Olivia', 'Wei']
    last_names = ['Santos', 'Chen', 'Patel', 'Williams', 'Garcia', 'Kim', 'Johnson', 'Silva',
                  'Martinez', 'Lee', 'Brown', 'Rodriguez', 'Ahmed', 'Taylor', 'Nakamura', 'Wilson']

    # Channel parameters
    channel_map = {
        'REACHABLE_MOBILE': 'VOICE_IVR',
        'REACHABLE_STATIONARY': 'SMS_DEEPLINK',
        'UNREACHABLE': 'CALLBACK'
    }

    # Simulate response rates per channel
    response_rates = {
        'VOICE_IVR': {'confirmed': 0.60, 'rescheduled': 0.15, 'no_answer': 0.20, 'cancelled': 0.05},
        'SMS_DEEPLINK': {'confirmed': 0.55, 'rescheduled': 0.20, 'no_answer': 0.20, 'cancelled': 0.05},
        'CALLBACK': {'confirmed': 0.35, 'rescheduled': 0.15, 'no_answer': 0.40, 'cancelled': 0.10}
    }

    action_log = []
    admin_alerts = []

    for idx, (_, patient) in enumerate(sample.iterrows()):
        name = f"{random.choice(first_names)} {random.choice(last_names)}"
        patient_id = f"P-{1001 + idx}"

        # PCA: Classify context
        is_weekday = patient['DayOfWeek'] < 5
        hour = patient['Hour']
        r_p = patient['R_p']

        if patient['SMS_received'] == 0 and r_p > 0.5:
            c_p = 'UNREACHABLE'
            pca_reasoning = (f"No SMS engagement history and high risk score (R_p={r_p:.2f}). "
                           f"Patient is likely disengaged from the healthcare system.")
        elif is_weekday and (7 <= hour <= 9 or 16 <= hour <= 19):
            c_p = 'REACHABLE_MOBILE'
            pca_reasoning = (f"Weekday {'morning' if hour <= 9 else 'evening'} commuting hours (hour={hour}). "
                           f"Patient is likely in transit. R_p={r_p:.2f}.")
        elif is_weekday and 9 < hour < 16:
            c_p = 'REACHABLE_STATIONARY'
            pca_reasoning = f"Weekday work hours (hour={hour}). Patient likely at desk or home. R_p={r_p:.2f}."
        elif r_p > 0.4:
            c_p = 'REACHABLE_MOBILE'
            pca_reasoning = f"Weekend/off-hours with elevated risk (R_p={r_p:.2f}). Treating as mobile for urgent outreach."
        else:
            c_p = 'REACHABLE_STATIONARY'
            pca_reasoning = f"Weekend/off-hours with low risk (R_p={r_p:.2f}). Standard SMS outreach appropriate."

        # COA: Select channel
        channel = channel_map[c_p]
        channel_labels = {'VOICE_IVR': 'Voice IVR Call', 'SMS_DEEPLINK': 'SMS Deep-Link', 'CALLBACK': 'Scheduled Callback'}
        coa_reasoning = {
            'VOICE_IVR': f"Mobile context → Voice IVR selected via Amazon Connect. Interactive call allows immediate confirmation.",
            'SMS_DEEPLINK': f"Stationary context → SMS with deep-link selected via Amazon SNS. Patient can tap to confirm/reschedule.",
            'CALLBACK': f"Unreachable context → Scheduled callback selected. Will attempt at next available window."
        }[channel]

        # Simulate patient response
        rates = response_rates[channel]
        outcome = random.choices(
            list(rates.keys()),
            weights=list(rates.values()),
            k=1
        )[0]

        # Determine action
        actual_noshow = bool(patient['NoShow'])
        action_taken = {
            'confirmed': f"Appointment confirmed. {'(Patient actually showed up)' if not actual_noshow else '(Despite confirmation, patient was a no-show)'}",
            'rescheduled': f"Patient requested reschedule. Slot opened for waitlist candidate.",
            'no_answer': f"No response after outreach attempt. {'Escalated to admin for manual follow-up.' if r_p > 0.5 else 'Queued for retry.'}",
            'cancelled': f"Patient cancelled appointment. Slot released to waitlist."
        }[outcome]

        entry = {
            "patient_id": patient_id,
            "patient_name": name,
            "age": int(patient['Age']),
            "appointment_day": str(patient['AppointmentDay'].date()) if hasattr(patient['AppointmentDay'], 'date') else str(patient['AppointmentDay'])[:10],
            "lead_time_days": int(patient['LeadTimeDays']),
            "sms_received": bool(patient['SMS_received']),
            "risk_score": round(r_p, 3),
            "actual_noshow": actual_noshow,
            "pca_context_state": c_p,
            "pca_reasoning": pca_reasoning,
            "coa_channel": channel,
            "coa_channel_label": channel_labels[channel],
            "coa_reasoning": coa_reasoning,
            "patient_response": outcome,
            "action_taken": action_taken
        }
        action_log.append(entry)

        # Admin alerts for cases needing human attention
        if outcome == 'no_answer' and r_p > 0.5:
            admin_alerts.append({
                "alert_type": "MANUAL_FOLLOWUP",
                "severity": "HIGH",
                "patient_id": patient_id,
                "patient_name": name,
                "reason": f"High-risk patient (R_p={r_p:.2f}) unreachable after {channel_labels[channel]}. Needs manual follow-up.",
                "recommended_action": "Phone call by admin staff or assign to care coordinator"
            })
        if outcome == 'cancelled':
            admin_alerts.append({
                "alert_type": "SLOT_AVAILABLE",
                "severity": "MEDIUM",
                "patient_id": patient_id,
                "patient_name": name,
                "reason": f"Patient cancelled. Slot on {entry['appointment_day']} is now open.",
                "recommended_action": "Offer slot to next waitlist candidate via RRA agent"
            })

    # Compute action summary
    outcomes = [e['patient_response'] for e in action_log]
    action_summary = {
        "total_patients_contacted": len(action_log),
        "confirmed": outcomes.count('confirmed'),
        "rescheduled": outcomes.count('rescheduled'),
        "no_answer": outcomes.count('no_answer'),
        "cancelled": outcomes.count('cancelled'),
        "admin_alerts_generated": len(admin_alerts),
        "confirmation_rate": round(outcomes.count('confirmed') / len(outcomes) * 100, 1),
        "escalation_rate": round(outcomes.count('no_answer') / len(outcomes) * 100, 1)
    }

    # Narrative
    narrative = {
        "problem_statement": (
            f"This simulation demonstrates how the multi-agent system would handle real-time outreach "
            f"for {len(action_log)} patients, showing the complete decision chain from context assessment "
            f"to outreach execution and patient response handling."
        ),
        "what_agents_did": (
            f"PCA assessed each patient's context individually — considering appointment time, SMS history, "
            f"and risk score to classify reachability. COA then selected the optimal channel: "
            f"{outcomes.count('confirmed') + outcomes.count('rescheduled')} patients were successfully engaged, "
            f"{outcomes.count('no_answer')} required follow-up, and {outcomes.count('cancelled')} cancelled."
        ),
        "key_finding": (
            f"The system achieved a {action_summary['confirmation_rate']}% confirmation rate across all channels. "
            f"{len(admin_alerts)} cases were escalated to admin staff for human intervention — "
            f"demonstrating the human-in-the-loop design where AI handles routine outreach and "
            f"surfaces complex cases to clinical staff."
        ),
        "value_proposition": (
            f"Of {len(action_log)} patients contacted, {action_summary['confirmed']} confirmed their appointments "
            f"and {action_summary['rescheduled']} rescheduled — freeing {action_summary['cancelled'] + action_summary['rescheduled']} "
            f"slots for waitlist patients. Admin staff only needed to handle {len(admin_alerts)} escalations "
            f"instead of manually contacting all {len(action_log)} patients."
        ),
        "clinical_impact": (
            f"This agentic approach reduces admin workload by ~{round((1 - len(admin_alerts)/len(action_log)) * 100)}% — "
            f"agents autonomously handled {len(action_log) - len(admin_alerts)} out of {len(action_log)} outreach tasks. "
            f"Staff time is focused on the {len(admin_alerts)} high-priority cases that genuinely need human judgment."
        )
    }

    # Build results
    results = {
        "run_id": run_id,
        "dag_id": DAG_ID,
        "simulation_type": "realtime_outreach",
        "total_records_in_dataset": len(df),
        "patients_simulated": len(action_log),
        "narrative": narrative,
        "action_summary": action_summary,
        "admin_alerts": admin_alerts,
        "action_log": action_log,
        "channel_breakdown": {
            ch: len([e for e in action_log if e['coa_channel'] == ch])
            for ch in ['VOICE_IVR', 'SMS_DEEPLINK', 'CALLBACK']
        }
    }

    os.makedirs(output_dir, exist_ok=True)
    with open(os.path.join(output_dir, "results.json"), 'w') as f:
        json.dump(results, f, indent=2)

    print(f"Simulation complete: {len(action_log)} patients, {action_summary['confirmed']} confirmed, "
          f"{len(admin_alerts)} admin alerts")

    notify_broker(run_id, DAG_ID, "COMPLETED")
    update_run_status(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Real-time PCA→COA outreach simulation with per-patient agent reasoning and admin alerts",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["pca", "coa", "realtime", "agentic", "smartcare"],
         on_failure_callback=on_failure) as dag:

    simulate = PythonOperator(
        task_id="simulate_realtime_outreach",
        python_callable=simulate_outreach
    )
