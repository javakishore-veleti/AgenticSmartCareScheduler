"""
Patient Outreach Orchestration DAG

End-to-end agentic pipeline:
  1. PCA assesses patient context (C_p) and risk (R_p)
  2. COA selects optimal outreach channel based on C_p
  3. Executes live outreach via Connect (IVR) or SNS (SMS)
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status

DAG_ID = "patient_outreach_orchestration"
default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}


def assess_patient_context(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    notify_broker(run_id, DAG_ID, "RUNNING")
    update_run_status(run_id, "RUNNING")
    # TODO: call Spring Boot PCA endpoint
    print("PCA: Assessing patient context states...")


def select_outreach_channel(**kwargs):
    # TODO: call Spring Boot COA endpoint
    print("COA: Selecting outreach channels per C_p state...")


def execute_outreach(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    # TODO: call Connect/SNS via Spring Boot @Tool endpoints
    print("COA: Executing live outreach...")
    notify_broker(run_id, DAG_ID, "COMPLETED")
    update_run_status(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="End-to-end PCA→COA patient outreach pipeline",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["pca", "coa", "outreach", "smartcare"],
         on_failure_callback=on_failure) as dag:

    t1 = PythonOperator(task_id="assess_patient_context", python_callable=assess_patient_context)
    t2 = PythonOperator(task_id="select_outreach_channel", python_callable=select_outreach_channel)
    t3 = PythonOperator(task_id="execute_outreach", python_callable=execute_outreach)

    t1 >> t2 >> t3
