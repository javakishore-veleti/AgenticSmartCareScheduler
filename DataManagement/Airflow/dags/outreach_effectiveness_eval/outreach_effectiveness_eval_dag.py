"""
Outreach Effectiveness Evaluation DAG

Compares the proposed multi-agent, context-aware outreach strategy
against an SMS-only baseline:
  - Baseline: all patients get SMS regardless of context
  - Proposed: channel selected based on C_p state (IVR/SMS/Callback)

Measures reachability improvement, channel appropriateness,
and estimated confirmation rate uplift.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "outreach_effectiveness_eval"
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


def evaluate_effectiveness(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    notify_broker(run_id, "RUNNING")

    # TODO: Load C_p + channel assignments, compare strategies
    # - SMS-only baseline: assume all patients get SMS
    # - Proposed: context-aware channel selection
    # - Metrics: reachability rate, channel match rate, estimated confirmation uplift
    # - Output: baseline_comparison.json, baseline_comparison.png

    print(f"Evaluating outreach effectiveness on: {dataset_path}")

    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Compare context-aware outreach vs SMS-only baseline",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["evaluation", "baseline", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    evaluate = PythonOperator(
        task_id="evaluate_outreach_effectiveness",
        python_callable=evaluate_effectiveness,
    )
