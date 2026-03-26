"""
Patient Outreach Orchestration DAG

End-to-end agentic pipeline:
  1. PCA assesses patient context (C_p) and risk (R_p)
  2. COA selects optimal outreach channel based on C_p
  3. Executes live outreach via Connect (IVR) or SNS (SMS)

This is the core workflow of the multi-agent system.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "patient_outreach_orchestration"
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


def assess_patient_context(**kwargs):
    """PCA: assess C_p and R_p for target patients."""
    conf = kwargs.get("dag_run").conf or {}
    notify(conf.get("runId"), "RUNNING")
    # TODO: call Spring Boot PCA endpoint → Bedrock Claude classifies C_p
    print("PCA: Assessing patient context states...")


def select_outreach_channel(**kwargs):
    """COA: select IVR/SMS/Callback based on C_p."""
    # TODO: call Spring Boot COA endpoint → Bedrock Claude selects channel
    print("COA: Selecting outreach channels per C_p state...")


def execute_outreach(**kwargs):
    """COA: trigger Connect IVR or SNS SMS."""
    conf = kwargs.get("dag_run").conf or {}
    # TODO: call Connect/SNS via Spring Boot @Tool endpoints
    print("COA: Executing live outreach...")
    notify(conf.get("runId"), "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id: notify(run_id, "FAILED")


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="End-to-end PCA→COA patient outreach pipeline",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["pca", "coa", "outreach", "smartcare"],
         on_failure_callback=on_failure) as dag:

    t1 = PythonOperator(task_id="assess_patient_context", python_callable=assess_patient_context)
    t2 = PythonOperator(task_id="select_outreach_channel", python_callable=select_outreach_channel)
    t3 = PythonOperator(task_id="execute_outreach", python_callable=execute_outreach)

    t1 >> t2 >> t3
