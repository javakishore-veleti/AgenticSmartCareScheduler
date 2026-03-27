"""
Shared utilities for SmartCare Airflow DAGs.
Reads connection details from Airflow Connections (configured in Admin > Connections).
"""
import os
import json
import glob
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


def resolve_dataset_path(dataset_path):
    """
    Resolve dataset path from Spring Boot's storageLocationHint to a path
    accessible inside the Airflow Docker container.

    Spring Boot stores: ~/runtime_data/DataSets/SmartCare-Admin/Datasets-Loaded/<uuid>
    Docker mounts:      ${HOME}/runtime_data → /home/airflow/runtime_data

    Returns the path to the first CSV file found in the resolved directory.
    """
    if not dataset_path:
        return None

    # Translate host path to container path
    # ~/runtime_data/... → /home/airflow/runtime_data/...
    resolved = dataset_path
    if resolved.startswith("~/"):
        resolved = "/home/airflow/" + resolved[2:]
    elif "/runtime_data/" in resolved:
        # Handle absolute host paths like /Users/user/runtime_data/...
        idx = resolved.index("/runtime_data/")
        resolved = "/home/airflow" + resolved[idx:]

    # Find CSV file in the directory
    if os.path.isdir(resolved):
        csvs = glob.glob(os.path.join(resolved, "*.csv"))
        if csvs:
            return csvs[0]
        # Check subdirectories
        csvs = glob.glob(os.path.join(resolved, "**/*.csv"), recursive=True)
        if csvs:
            return csvs[0]
        print(f"WARNING: No CSV found in {resolved}")
        return resolved

    if os.path.isfile(resolved):
        return resolved

    print(f"WARNING: Path not found: {resolved} (original: {dataset_path})")
    return resolved


def resolve_output_dir(output_dir, run_id):
    """Resolve output directory path for container access."""
    if not output_dir:
        return f"/home/airflow/runtime_data/workflow_output/run_{run_id}"
    resolved = output_dir
    if "~/runtime_data" in resolved:
        resolved = resolved.replace("~/runtime_data", "/home/airflow/runtime_data")
    elif "/runtime_data/" in resolved and not resolved.startswith("/home/airflow"):
        idx = resolved.index("/runtime_data/")
        resolved = "/home/airflow" + resolved[idx:]
    os.makedirs(resolved, exist_ok=True)
    return resolved


def get_agents_admin_url():
    """Get agents/admin API base URL for workflow run updates."""
    try:
        conn = BaseHook.get_connection("smartcare_api")
        return f"{conn.schema}://{conn.host}:{conn.port}/smart-care/api/agents/admin/v1"
    except Exception:
        return "http://host.docker.internal:8080/smart-care/api/agents/admin/v1"


def update_run_status(run_id, status, error_message=None):
    """Update workflow run status via agents/admin API."""
    agents_url = get_agents_admin_url()
    try:
        body = {"status": status}
        if error_message:
            body["errorMessage"] = error_message
        requests.put(f"{agents_url}/workflow-runs/{run_id}/status", json=body, timeout=5)
    except Exception as e:
        print(f"API status update failed: {e}")
