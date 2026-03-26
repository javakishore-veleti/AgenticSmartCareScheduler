"""
Appointment Slot Reallocation Simulation DAG

Identifies high-risk appointment slots (R_p > 0.65) and simulates
the RRA (Resource Reallocation Agent) strategy:
  - Waitlist promotion for predicted no-shows
  - Provider schedule optimization
  - Double-booking mitigation for high-risk slots

Outputs slot utilization improvement metrics.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "slot_reallocation_simulation"
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


def simulate_reallocation(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    notify_broker(run_id, "RUNNING")

    # TODO: Load R_p scores, identify high-risk slots, simulate reallocation
    # - Filter slots with R_p > 0.65
    # - Simulate waitlist promotion (fill predicted no-show slots)
    # - Calculate: original utilization vs reallocated utilization
    # - Output: reallocation_metrics.json, slot_utilization.png

    print(f"Simulating slot reallocation on: {dataset_path}")

    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Simulate appointment slot reallocation for high-risk no-shows",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["rra", "reallocation", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    reallocate = PythonOperator(
        task_id="simulate_slot_reallocation",
        python_callable=simulate_reallocation,
    )
