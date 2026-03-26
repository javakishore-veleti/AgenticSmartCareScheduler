"""
Data Quality Check DAG

Validates dataset integrity: null checks, range validation,
duplicate detection, schema conformance.

Publishes workflow_run_event to message broker on start/complete/fail.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "data_quality_check"
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


def check_quality(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    notify_broker(run_id, "RUNNING")

    # TODO: actual quality checks
    # - Load dataset
    # - Null percentage per column
    # - Value range validation (age, lead time)
    # - Duplicate row detection
    # - Schema conformance check
    # - Write quality report JSON

    print(f"Running data quality checks on: {dataset_path}")
    print("Quality check complete (placeholder)")

    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Validate dataset integrity and produce quality report",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["data-quality", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    quality = PythonOperator(
        task_id="run_quality_checks",
        python_callable=check_quality,
    )
