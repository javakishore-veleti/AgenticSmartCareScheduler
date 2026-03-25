# Workflow Definitions

Each workflow is a self-contained Python module with its own `__init__.py`.
The folder name becomes the DAG ID in Apache Airflow.

## Structure
```
WFs/
├── kaggle-noshow-train/         ← WF-unique-name (becomes DAG ID)
│   ├── __init__.py
│   ├── dag.py                   ← Airflow DAG definition
│   ├── tasks/
│   │   ├── download_dataset.py
│   │   ├── feature_engineering.py
│   │   ├── train_xgboost.py
│   │   └── evaluate_model.py
│   └── requirements.txt         ← WF-specific deps if any
├── context-state-simulation/
│   ├── dag.py
│   └── tasks/
└── ...
```

## Local: Apache Airflow (Docker)
- DAGs root path: each WF folder is mounted as a DAG
- DevOps/Local/Airflow/docker-compose.yml

## AWS: EMR / Step Functions
- WF code uploaded to S3
- EMR runs the workflow

## Admin Portal
- Administration → Workflow Engines → Definitions + Instances
- Select workflow + target engine → submit
