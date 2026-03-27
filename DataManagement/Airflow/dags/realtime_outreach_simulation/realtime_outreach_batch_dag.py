"""
Real-Time Outreach Batch DAG (DAG 1)

Traditional loop: reads dataset CSV, iterates rows, triggers the
agent DAG (DAG 2) async for each patient. Fire-and-forget —
doesn't wait for agent DAG to complete before moving to next patient.

Each patient is urgent (appointment approaching), so parallel execution
is critical.
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.trigger_dagrun import TriggerDagRunOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status, resolve_dataset_path, resolve_output_dir

DAG_ID = "realtime_outreach_batch"
AGENT_DAG_ID = "realtime_outreach_agent"
SAMPLE_SIZE = 30  # Process 30 patients for demo (production: all rows)

default_args = {"owner": "smartcare", "retries": 0, "retry_delay": timedelta(minutes=1)}


def load_and_dispatch(**kwargs):
    """Read CSV, sample patients, trigger agent DAG for each one async."""
    import pandas as pd
    import json
    import requests

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

    print(f"Batch: Loading {csv_file}")
    df = pd.read_csv(csv_file)
    df['ScheduledDay'] = pd.to_datetime(df['ScheduledDay'])
    df['AppointmentDay'] = pd.to_datetime(df['AppointmentDay'])
    df['LeadTimeDays'] = (df['AppointmentDay'] - df['ScheduledDay']).dt.days
    df['DayOfWeek'] = df['AppointmentDay'].dt.dayofweek
    df['Hour'] = df['ScheduledDay'].dt.hour
    df['NoShow'] = (df['No-show'] == 'Yes').astype(int)
    df = df[df['LeadTimeDays'] >= 0]

    # Sample patients
    sample = df.sample(n=min(SAMPLE_SIZE, len(df)), random_state=42)
    print(f"Batch: Dispatching {len(sample)} patients to agent DAG (async)")

    # Save batch manifest
    os.makedirs(output_dir, exist_ok=True)
    manifest = {
        "batch_run_id": run_id,
        "total_in_dataset": len(df),
        "patients_dispatched": len(sample),
        "patient_ids": []
    }

    # Trigger agent DAG for each patient — fire and forget
    from airflow.api.client.local_client import Client
    client = Client(None, None)

    for idx, (_, row) in enumerate(sample.iterrows()):
        patient_id = f"P-{1001 + idx}"
        patient_data = {
            "patient_id": patient_id,
            "age": int(row['Age']),
            "gender": row.get('Gender', 'Unknown'),
            "appointment_day": str(row['AppointmentDay'])[:10],
            "scheduled_day": str(row['ScheduledDay'])[:19],
            "lead_time_days": int(row['LeadTimeDays']),
            "day_of_week": int(row['DayOfWeek']),
            "hour": int(row['Hour']),
            "sms_received": int(row['SMS_received']),
            "scholarship": int(row['Scholarship']),
            "hypertension": int(row['Hipertension']),
            "diabetes": int(row['Diabetes']),
            "alcoholism": int(row['Alcoholism']),
            "handicap": int(row['Handcap']),
            "actual_noshow": bool(row['NoShow']),
            "batch_run_id": run_id,
            "output_dir": output_dir
        }

        # Trigger agent DAG async
        try:
            client.trigger_dag(
                dag_id=AGENT_DAG_ID,
                run_id=f"batch_{run_id}_patient_{patient_id}_{datetime.now().strftime('%Y%m%d%H%M%S%f')}",
                conf=patient_data,
                replace_microseconds=False
            )
            print(f"  Dispatched {patient_id} (age={patient_data['age']}, hour={patient_data['hour']})")
            manifest["patient_ids"].append(patient_id)
        except Exception as e:
            print(f"  Failed to dispatch {patient_id}: {e}")

    # Save manifest
    with open(os.path.join(output_dir, "batch_manifest.json"), 'w') as f:
        import json
        json.dump(manifest, f, indent=2)

    print(f"Batch: All {len(manifest['patient_ids'])} patients dispatched. Agent DAGs running in parallel.")

    # Batch DAG completes — individual agents report their own results
    notify_broker(run_id, DAG_ID, "COMPLETED")
    update_run_status(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Batch dispatcher: reads CSV, triggers agent DAG per patient (async)",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["batch", "dispatcher", "smartcare"],
         on_failure_callback=on_failure) as dag:

    dispatch = PythonOperator(
        task_id="load_and_dispatch",
        python_callable=load_and_dispatch
    )
