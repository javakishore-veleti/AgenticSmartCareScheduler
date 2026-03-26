"""
Waitlist Slot Fulfillment DAG
PCA→RRA→COA: predict no-show, rank waitlist, offer slot.
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status

DAG_ID = "waitlist_slot_fulfillment"
default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}


def identify_at_risk_slots(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    notify_broker(conf.get("runId"), DAG_ID, "RUNNING")
    update_run_status(conf.get("runId"), "RUNNING")
    print("PCA: Identifying at-risk appointment slots...")


def rank_waitlist_candidates(**kwargs):
    print("RRA: Ranking waitlist candidates via Bedrock Claude...")


def offer_slot(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    print("COA: Offering slot to top waitlist candidate...")
    notify_broker(conf.get("runId"), DAG_ID, "COMPLETED")
    update_run_status(conf.get("runId"), "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Proactively fill predicted no-show slots from waitlist",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["pca", "rra", "coa", "waitlist", "smartcare"],
         on_failure_callback=on_failure) as dag:
    t1 = PythonOperator(task_id="identify_at_risk_slots", python_callable=identify_at_risk_slots)
    t2 = PythonOperator(task_id="rank_waitlist_candidates", python_callable=rank_waitlist_candidates)
    t3 = PythonOperator(task_id="offer_slot", python_callable=offer_slot)
    t1 >> t2 >> t3
