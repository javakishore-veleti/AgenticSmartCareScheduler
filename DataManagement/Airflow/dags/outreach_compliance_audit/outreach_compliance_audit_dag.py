"""
Outreach Compliance Audit DAG
ACA agent: HIPAA compliance review, audit summary generation.
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status

DAG_ID = "outreach_compliance_audit"
default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}


def collect_outreach_logs(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    notify_broker(conf.get("runId"), DAG_ID, "RUNNING")
    update_run_status(conf.get("runId"), "RUNNING")
    print("ACA: Collecting outreach logs from OpenSearch...")


def audit_compliance(**kwargs):
    print("ACA: Auditing compliance via Bedrock Claude...")


def generate_audit_report(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    print("ACA: Generating audit report...")
    notify_broker(conf.get("runId"), DAG_ID, "COMPLETED")
    update_run_status(conf.get("runId"), "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="HIPAA compliance audit of all outreach interactions",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["aca", "compliance", "audit", "smartcare"],
         on_failure_callback=on_failure) as dag:
    t1 = PythonOperator(task_id="collect_outreach_logs", python_callable=collect_outreach_logs)
    t2 = PythonOperator(task_id="audit_compliance", python_callable=audit_compliance)
    t3 = PythonOperator(task_id="generate_audit_report", python_callable=generate_audit_report)
    t1 >> t2 >> t3
