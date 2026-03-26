"""
Context State (C_p) Simulation DAG

Assigns context states to patient records based on appointment time,
SMS receipt, and risk score. Produces channel distribution stats.

Publishes workflow_run_event to message broker on start/complete/fail.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "cp_simulation"
BROKER_URL = "http://host.docker.internal:8081/smart-care/api/broker/v1/messages"
SPRING_URL = "http://host.docker.internal:8080/smart-care/api/admin/v1/workflow-runs"

default_args = {
    "owner": "smartcare",
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}


def notify_broker(run_id, status, **kwargs):
    """Publish workflow status event to message broker."""
    try:
        requests.post(f"{BROKER_URL}/publish", json={
            "queueName": "workflow_run_event",
            "messageKey": f"run.{status.lower()}",
            "payload": json.dumps({"runId": run_id, "dagId": DAG_ID, "status": status}),
            "ctxData": {"dagId": DAG_ID, "runId": str(run_id)}
        }, timeout=5)
    except Exception as e:
        print(f"Broker notification failed: {e}")


def run_simulation(**kwargs):
    """Execute C_p simulation on dataset."""
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    # Notify RUNNING
    notify_broker(run_id, "RUNNING")

    # TODO: actual simulation logic
    # - Load dataset from dataset_path
    # - Assign C_p states based on appointment time
    # - Calculate channel distribution
    # - Write results to output path

    print(f"Running C_p simulation on: {dataset_path}")
    print("Simulation complete (placeholder)")

    # Notify COMPLETED
    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    """Handle DAG failure."""
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Assign context states (C_p) to patient records and compute channel distribution",
    schedule=None,  # triggered externally
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["analytics", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    simulate = PythonOperator(
        task_id="run_cp_simulation",
        python_callable=run_simulation,
    )
