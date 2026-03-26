"""
Patient Context Classification DAG

Classifies each patient into a context state (C_p):
  - REACHABLE_MOBILE: commuting hours (7-9 AM, 4-7 PM weekday)
  - REACHABLE_STATIONARY: work hours (9 AM-4 PM weekday)
  - UNREACHABLE: no SMS received + R_p > 0.5

Uses appointment time, SMS receipt history, and R_p score.
Core input for the COA channel selection logic.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "patient_context_classification"
BROKER_URL = "http://host.docker.internal:8081/smart-care/api/broker/v1/messages"

default_args = {
    "owner": "smartcare",
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}


def notify_broker(run_id, status):
    try:
        requests.post(f"{BROKER_URL}/publish", json={
            "queueName": "workflow_run_event",
            "messageKey": f"run.{status.lower()}",
            "payload": json.dumps({"runId": run_id, "dagId": DAG_ID, "status": status}),
            "ctxData": {"dagId": DAG_ID, "runId": str(run_id)}
        }, timeout=5)
    except Exception as e:
        print(f"Broker notification failed: {e}")


def classify_context(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    notify_broker(run_id, "RUNNING")

    # TODO: Load dataset + R_p scores, assign C_p states
    # - Weekday 7-9 AM, 4-7 PM → REACHABLE_MOBILE
    # - Weekday 9 AM-4 PM → REACHABLE_STATIONARY
    # - No SMS_received + R_p > 0.5 → UNREACHABLE
    # - Output: C_p distribution stats, per-patient C_p assignments

    print(f"Classifying patient context on: {dataset_path}")

    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Classify patients into context states (C_p) for channel selection",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["pca", "context-state", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    classify = PythonOperator(
        task_id="classify_patient_context",
        python_callable=classify_context,
    )
