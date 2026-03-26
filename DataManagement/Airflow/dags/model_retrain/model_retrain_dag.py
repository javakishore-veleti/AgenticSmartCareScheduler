"""
XGBoost Model Retrain DAG

Retrains the PCA risk model (XGBoost) on the Medical Appointment
No-Show dataset. Outputs metrics, confusion matrix, and saved model.

Publishes workflow_run_event to message broker on start/complete/fail.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
import json
import requests

DAG_ID = "model_retrain"
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


def retrain_model(**kwargs):
    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")

    notify_broker(run_id, "RUNNING")

    # TODO: actual retraining logic
    # - Load dataset
    # - Feature engineering
    # - XGBoost train with cross-validation
    # - Save metrics, confusion matrix, model pickle

    print(f"Retraining XGBoost model on: {dataset_path}")
    print("Retrain complete (placeholder)")

    notify_broker(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, "FAILED")


with DAG(
    dag_id=DAG_ID,
    default_args=default_args,
    description="Retrain XGBoost no-show risk model and output metrics",
    schedule=None,
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["ml", "smartcare"],
    on_failure_callback=on_failure,
) as dag:

    retrain = PythonOperator(
        task_id="retrain_xgboost_model",
        python_callable=retrain_model,
    )
