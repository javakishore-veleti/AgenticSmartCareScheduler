"""
Shared utilities for SmartCare Airflow DAGs.
Reads connection details from Airflow Connections (configured in Admin > Connections).
"""
import json
import requests
from airflow.hooks.base import BaseHook


def get_broker_url():
    """Get message broker base URL from Airflow Connection 'smartcare_broker'."""
    try:
        conn = BaseHook.get_connection("smartcare_broker")
        return f"{conn.schema}://{conn.host}:{conn.port}/smart-care/api/broker/v1/messages"
    except Exception:
        # Fallback for local dev without connection configured
        return "http://host.docker.internal:8081/smart-care/api/broker/v1/messages"


def get_api_url():
    """Get Spring Boot API base URL from Airflow Connection 'smartcare_api'."""
    try:
        conn = BaseHook.get_connection("smartcare_api")
        return f"{conn.schema}://{conn.host}:{conn.port}/smart-care/api/admin/v1"
    except Exception:
        return "http://host.docker.internal:8080/smart-care/api/admin/v1"


def notify_broker(run_id, dag_id, status):
    """Publish workflow status event to message broker."""
    broker_url = get_broker_url()
    try:
        requests.post(f"{broker_url}/publish", json={
            "queueName": "workflow_run_event",
            "messageKey": f"run.{status.lower()}",
            "payload": json.dumps({"runId": run_id, "dagId": dag_id, "status": status}),
            "ctxData": {"dagId": dag_id, "runId": str(run_id)}
        }, timeout=5)
    except Exception as e:
        print(f"Broker notification failed: {e}")


def update_run_status(run_id, status, error_message=None):
    """Directly update workflow run status via Spring Boot API."""
    api_url = get_api_url()
    try:
        body = {"status": status}
        if error_message:
            body["errorMessage"] = error_message
        requests.put(f"{api_url}/workflow-runs/{run_id}/status", json=body, timeout=5)
    except Exception as e:
        print(f"API status update failed: {e}")
