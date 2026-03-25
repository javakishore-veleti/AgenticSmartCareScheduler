# DataManagement

## Structure
```
DataManagement/
├── Ingestion/          — Dataset download, parsing, loading scripts
├── Analytics/
│   ├── AI/             — AI-related analytics (Bedrock, LLM)
│   ├── ML/             — ML model training (XGBoost, etc.)
│   └── output/         — Generated charts, metrics, reports
└── Ops/
    ├── MLOps/          — Model versioning, deployment, monitoring
    └── AIOps/          — AI agent operations, health monitoring
```

## Dataset Management
Datasets are managed through the **Admin Portal → Analytics → Datasets** interface.
- Backend API: `/smart-care/api/admin/v1/analytics/datasets/`
- Database: `smartcare_admin_db` schema, tables: `datasets_master`, `dataset_instance`
- See ArchitectureDecisions.md for full design.
