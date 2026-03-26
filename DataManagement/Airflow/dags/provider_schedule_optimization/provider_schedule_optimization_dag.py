"""
Provider Schedule Optimization DAG
PCA→RRA→PSA: analyze no-show patterns, recommend schedule changes.
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status

DAG_ID = "provider_schedule_optimization"
default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}


def analyze_noshow_patterns(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    notify_broker(conf.get("runId"), DAG_ID, "RUNNING")
    update_run_status(conf.get("runId"), "RUNNING")
    print("RRA: Analyzing no-show patterns across providers...")


def generate_recommendations(**kwargs):
    print("RRA: Generating schedule recommendations via Bedrock Claude...")


def notify_providers(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    print("PSA: Notifying providers of schedule recommendations...")
    notify_broker(conf.get("runId"), DAG_ID, "COMPLETED")
    update_run_status(conf.get("runId"), "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Optimize provider schedules based on predicted no-show patterns",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["rra", "psa", "scheduling", "smartcare"],
         on_failure_callback=on_failure) as dag:
    t1 = PythonOperator(task_id="analyze_noshow_patterns", python_callable=analyze_noshow_patterns)
    t2 = PythonOperator(task_id="generate_recommendations", python_callable=generate_recommendations)
    t3 = PythonOperator(task_id="notify_providers", python_callable=notify_providers)
    t1 >> t2 >> t3
