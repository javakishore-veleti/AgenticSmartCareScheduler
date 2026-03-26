"""
No-Show Risk Scoring DAG

Runs XGBoost model on patient appointment dataset to predict no-show
risk probability (R_p) for each patient. Outputs risk scores, F1, AUC,
precision, recall, and confusion matrix.

Maps to PCA (Patient Context Agent) pipeline stage.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "noshow_risk_scoring"
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


def score_risk(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    notify_broker(run_id, "RUNNING")

    # TODO: Load dataset, run XGBoost prediction, output R_p scores
    # - Load KaggleV2-May-2016.csv from dataset_path
    # - Feature engineering (LeadTimeDays, DayOfWeek, Hour, etc.)
    # - 5-fold stratified cross-validation
    # - Output: risk_model_metrics.json, confusion_matrix.png, per-patient R_p scores

    print(f"Scoring no-show risk on: {dataset_path}")

    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Predict no-show risk (R_p) using XGBoost on appointment dataset",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["pca", "risk-scoring", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    score = PythonOperator(
        task_id="run_risk_scoring",
        python_callable=score_risk,
    )
