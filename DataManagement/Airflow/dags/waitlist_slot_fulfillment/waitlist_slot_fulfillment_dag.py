"""
Waitlist Slot Fulfillment DAG

When PCA predicts a likely no-show (R_p > 0.65):
  1. RRA identifies waitlisted patients from HealthLake
  2. Bedrock Claude ranks candidates by urgency and travel feasibility
  3. COA offers the slot to top candidate via preferred channel
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "waitlist_slot_fulfillment"
BROKER_URL = "http://host.docker.internal:8081/smart-care/api/broker/v1/messages"

default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}


def notify(run_id, status):
    try:
        requests.post(f"{BROKER_URL}/publish", json={
            "queueName": "workflow_run_event", "messageKey": f"run.{status.lower()}",
            "payload": json.dumps({"runId": run_id, "dagId": DAG_ID, "status": status}),
            "ctxData": {"dagId": DAG_ID, "runId": str(run_id)}}, timeout=5)
    except Exception as e:
        print(f"Broker notification failed: {e}")


def identify_at_risk_slots(**kwargs):
    """PCA flags slots with R_p > 0.65."""
    conf = kwargs.get("dag_run").conf or {}
    notify(conf.get("runId"), "RUNNING")
    # TODO: query PCA risk scores, filter high-risk
    print("PCA: Identifying at-risk appointment slots...")


def rank_waitlist_candidates(**kwargs):
    """RRA uses Bedrock Claude to rank waitlisted patients."""
    # TODO: query HealthLake waitlist, Bedrock ranks by urgency + feasibility
    print("RRA: Ranking waitlist candidates via Bedrock Claude...")


def offer_slot(**kwargs):
    """COA contacts top candidate to offer the slot."""
    conf = kwargs.get("dag_run").conf or {}
    # TODO: COA triggers outreach to waitlist candidate
    print("COA: Offering slot to top waitlist candidate...")
    notify(conf.get("runId"), "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id: notify(run_id, "FAILED")


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Proactively fill predicted no-show slots from waitlist",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["pca", "rra", "coa", "waitlist", "smartcare"],
         on_failure_callback=on_failure) as dag:

    t1 = PythonOperator(task_id="identify_at_risk_slots", python_callable=identify_at_risk_slots)
    t2 = PythonOperator(task_id="rank_waitlist_candidates", python_callable=rank_waitlist_candidates)
    t3 = PythonOperator(task_id="offer_slot", python_callable=offer_slot)

    t1 >> t2 >> t3
