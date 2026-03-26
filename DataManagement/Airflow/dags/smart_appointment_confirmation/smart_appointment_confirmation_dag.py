"""
Smart Appointment Confirmation DAG
COA agent: IVR/SMS interaction, LLM response parsing, status update.
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status

DAG_ID = "smart_appointment_confirmation"
default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}


def initiate_contact(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    notify_broker(conf.get("runId"), DAG_ID, "RUNNING")
    update_run_status(conf.get("runId"), "RUNNING")
    print("COA: Initiating patient contact...")


def interpret_response(**kwargs):
    print("COA: Interpreting patient response via Bedrock Claude...")


def update_appointment(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    print("COA: Updating appointment status...")
    notify_broker(conf.get("runId"), DAG_ID, "COMPLETED")
    update_run_status(conf.get("runId"), "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Live appointment confirmation via IVR/SMS with LLM response parsing",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["coa", "confirmation", "smartcare"],
         on_failure_callback=on_failure) as dag:
    t1 = PythonOperator(task_id="initiate_contact", python_callable=initiate_contact)
    t2 = PythonOperator(task_id="interpret_response", python_callable=interpret_response)
    t3 = PythonOperator(task_id="update_appointment", python_callable=update_appointment)
    t1 >> t2 >> t3
