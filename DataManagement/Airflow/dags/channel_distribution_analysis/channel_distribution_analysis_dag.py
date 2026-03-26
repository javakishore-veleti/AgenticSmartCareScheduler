"""
Channel Distribution Analysis DAG

Computes outreach channel distribution across C_p states:
  - REACHABLE_MOBILE → Voice IVR
  - REACHABLE_STATIONARY → SMS Deep-Link
  - UNREACHABLE → Scheduled Callback

Demonstrates the COA (Communication Orchestration Agent) decision logic.
Generates the channel distribution chart (Fig. 5) for the IEEE paper.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "channel_distribution_analysis"
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


def analyze_channels(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    notify_broker(run_id, "RUNNING")

    # TODO: Load C_p assignments, compute channel distribution
    # - Map C_p → channel using Table I from paper
    # - Aggregate: IVR count, SMS count, Callback count per C_p
    # - Generate channel_distribution.png (matplotlib)
    # - Output: channel_distribution_stats.json, channel_distribution.png

    print(f"Analyzing channel distribution on: {dataset_path}")

    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Compute IVR/SMS/Callback distribution across patient context states",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["coa", "channel-selection", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    analyze = PythonOperator(
        task_id="analyze_channel_distribution",
        python_callable=analyze_channels,
    )
