"""
Smart Appointment Confirmation DAG

COA agent conducts live patient interaction:
  1. Place IVR call or send SMS deep-link
  2. Interpret patient response (confirm/reschedule/cancel) via Bedrock Claude
  3. Update appointment status
  4. Escalate non-responders to callback queue
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "smart_appointment_confirmation"
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


def initiate_contact(**kwargs):
    """Place IVR call via Connect or send SMS via SNS."""
    conf = kwargs.get("dag_run").conf or {}
    notify(conf.get("runId"), "RUNNING")
    # TODO: call Connect contact flow or SNS publish
    print("COA: Initiating patient contact...")


def interpret_response(**kwargs):
    """Use Bedrock Claude to interpret patient response."""
    # TODO: Bedrock Claude parses voice/text response → confirm/reschedule/cancel
    print("COA: Interpreting patient response via Bedrock Claude...")


def update_appointment(**kwargs):
    """Update appointment status and escalate if needed."""
    conf = kwargs.get("dag_run").conf or {}
    # TODO: update HealthLake FHIR appointment, escalate non-responders
    print("COA: Updating appointment status...")
    notify(conf.get("runId"), "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id: notify(run_id, "FAILED")


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Live appointment confirmation via IVR/SMS with LLM response parsing",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["coa", "confirmation", "smartcare"],
         on_failure_callback=on_failure) as dag:

    t1 = PythonOperator(task_id="initiate_contact", python_callable=initiate_contact)
    t2 = PythonOperator(task_id="interpret_response", python_callable=interpret_response)
    t3 = PythonOperator(task_id="update_appointment", python_callable=update_appointment)

    t1 >> t2 >> t3
