"""
Patient Outreach Orchestration DAG

End-to-end PCA→COA pipeline on historical appointment data:
  Task 1 (PCA): Load dataset, predict R_p, classify C_p context states
  Task 2 (COA): Select outreach channel per C_p state
  Task 3 (Report): Stats, chart, baseline comparison, save results
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker, update_run_status, resolve_dataset_path, resolve_output_dir

DAG_ID = "patient_outreach_orchestration"
default_args = {"owner": "smartcare", "retries": 1, "retry_delay": timedelta(minutes=2)}


def assess_patient_context(**kwargs):
    """PCA: Load dataset, predict R_p, classify C_p."""
    import pandas as pd
    import numpy as np
    from sklearn.model_selection import StratifiedKFold, cross_val_predict
    from xgboost import XGBClassifier

    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    dataset_path = conf.get("datasetPath", "")
    output_dir = resolve_output_dir(conf.get("outputDir"), run_id)

    notify_broker(run_id, DAG_ID, "RUNNING")
    update_run_status(run_id, "RUNNING")

    # Resolve dataset path (translates host path to container path, finds CSV)
    csv_file = resolve_dataset_path(dataset_path)
    if not csv_file or not os.path.isfile(csv_file):
        raise FileNotFoundError(f"Dataset not found. datasetPath={dataset_path}, resolved={csv_file}")

    print(f"PCA: Loading {csv_file}")
    df = pd.read_csv(csv_file)
    df['ScheduledDay'] = pd.to_datetime(df['ScheduledDay'])
    df['AppointmentDay'] = pd.to_datetime(df['AppointmentDay'])
    df['LeadTimeDays'] = (df['AppointmentDay'] - df['ScheduledDay']).dt.days
    df['DayOfWeek'] = df['AppointmentDay'].dt.dayofweek
    df['Hour'] = df['ScheduledDay'].dt.hour
    df['NoShow'] = (df['No-show'] == 'Yes').astype(int)
    df = df[df['LeadTimeDays'] >= 0]

    features = ['Age', 'Scholarship', 'Hipertension', 'Diabetes', 'Alcoholism',
                'Handcap', 'SMS_received', 'LeadTimeDays', 'DayOfWeek', 'Hour']
    X = df[features].fillna(0)
    y = df['NoShow']

    print(f"PCA: {len(df)} records, no-show rate: {y.mean()*100:.1f}%")
    print("PCA: Training XGBoost for R_p...")
    model = XGBClassifier(n_estimators=200, max_depth=6, learning_rate=0.1,
                          subsample=0.8, colsample_bytree=0.8, eval_metric='logloss', random_state=42)
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    df['R_p'] = cross_val_predict(model, X, y, cv=cv, method='predict_proba')[:, 1]

    print("PCA: Classifying C_p states...")
    def classify_cp(row):
        if row['SMS_received'] == 0 and row['R_p'] > 0.5:
            return 'UNREACHABLE'
        is_wd = row['DayOfWeek'] < 5
        h = row['Hour']
        if is_wd and (7 <= h <= 9 or 16 <= h <= 19):
            return 'REACHABLE_MOBILE'
        if is_wd and 9 < h < 16:
            return 'REACHABLE_STATIONARY'
        return 'REACHABLE_MOBILE' if row['R_p'] > 0.4 else 'REACHABLE_STATIONARY'

    df['C_p'] = df.apply(classify_cp, axis=1)

    os.makedirs(output_dir, exist_ok=True)
    df.to_parquet(os.path.join(output_dir, "pca_output.parquet"), index=False)
    print(f"PCA: Saved to {output_dir}/pca_output.parquet")


def select_outreach_channel(**kwargs):
    """COA: Map C_p → channel."""
    import pandas as pd

    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    output_dir = resolve_output_dir(conf.get("outputDir"), run_id)

    df = pd.read_parquet(os.path.join(output_dir, "pca_output.parquet"))
    df['Channel'] = df['C_p'].map({
        'REACHABLE_MOBILE': 'VOICE_IVR',
        'REACHABLE_STATIONARY': 'SMS_DEEPLINK',
        'UNREACHABLE': 'CALLBACK'
    })
    print("COA: Channel distribution:")
    for ch, n in df['Channel'].value_counts().items():
        print(f"  {ch}: {n:,} ({n/len(df)*100:.1f}%)")

    df.to_parquet(os.path.join(output_dir, "coa_output.parquet"), index=False)


def generate_report(**kwargs):
    """Report: stats, chart, baseline comparison, notify."""
    import json
    import pandas as pd
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt

    conf = kwargs.get("dag_run").conf or {}
    run_id = conf.get("runId")
    output_dir = resolve_output_dir(conf.get("outputDir"), run_id)

    df = pd.read_parquet(os.path.join(output_dir, "coa_output.parquet"))
    n = len(df)
    states = ['REACHABLE_MOBILE', 'REACHABLE_STATIONARY', 'UNREACHABLE']
    channels = ['VOICE_IVR', 'SMS_DEEPLINK', 'CALLBACK']

    sms_reach = {'REACHABLE_MOBILE': 0.40, 'REACHABLE_STATIONARY': 0.70, 'UNREACHABLE': 0.20}
    proposed_reach = {'REACHABLE_MOBILE': 0.75, 'REACHABLE_STATIONARY': 0.80, 'UNREACHABLE': 0.55}
    bl = sum(len(df[df['C_p'] == s]) * sms_reach[s] for s in states) / n * 100
    pr = sum(len(df[df['C_p'] == s]) * proposed_reach[s] for s in states) / n * 100

    results = {
        "run_id": run_id, "dag_id": DAG_ID, "total_records": n,
        "no_show_rate": round(df['NoShow'].mean() * 100, 1),
        "context_state_distribution": {
            s: {"count": int(len(df[df['C_p'] == s])),
                "percentage": round(len(df[df['C_p'] == s]) / n * 100, 1)} for s in states},
        "channel_distribution": {
            c: {"count": int(len(df[df['Channel'] == c])),
                "percentage": round(len(df[df['Channel'] == c]) / n * 100, 1)} for c in channels},
        "noshow_rate_by_context": {
            s: round(df[df['C_p'] == s]['NoShow'].mean() * 100, 1) for s in states},
        "baseline_comparison": {
            "sms_only_reachability": round(bl, 1),
            "proposed_reachability": round(pr, 1),
            "improvement_pp": round(pr - bl, 1)}
    }

    with open(os.path.join(output_dir, "results.json"), 'w') as f:
        json.dump(results, f, indent=2)

    # Chart
    fig, axes = plt.subplots(1, 3, figsize=(16, 5))
    s_labels = ['Mobile', 'Stationary', 'Unreachable']
    s_counts = [len(df[df['C_p'] == s]) for s in states]
    clrs = ['#4f46e5', '#16a34a', '#ea580c']

    for ax, labels, counts, title in [
        (axes[0], s_labels, s_counts, 'Context States (C_p)'),
        (axes[1], ['IVR', 'SMS', 'Callback'],
         [len(df[df['Channel'] == c]) for c in channels], 'COA Channels')]:
        bars = ax.bar(labels, counts, color=clrs)
        ax.set_title(title, fontweight='bold')
        for b, c in zip(bars, counts):
            ax.text(b.get_x() + b.get_width()/2, b.get_height() + 200,
                    f'{c:,}\n({c/n*100:.1f}%)', ha='center', fontsize=9)

    bars = axes[2].bar(['SMS-Only', 'Proposed'], [bl, pr], color=['#94a3b8', '#4f46e5'])
    axes[2].set_title('Reachability', fontweight='bold')
    axes[2].set_ylim(0, 100)
    for b, v in zip(bars, [bl, pr]):
        axes[2].text(b.get_x() + b.get_width()/2, b.get_height() + 1,
                     f'{v:.1f}%', ha='center', fontweight='bold')

    plt.suptitle(f'PCA→COA Simulation — Run #{run_id}', fontweight='bold', y=1.02)
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "channel_distribution.png"), dpi=200, bbox_inches='tight')
    plt.close()

    print(f"Report: Results and chart saved to {output_dir}")
    notify_broker(run_id, DAG_ID, "COMPLETED")
    update_run_status(run_id, "COMPLETED")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    run_id = conf.get("runId")
    if run_id:
        notify_broker(run_id, DAG_ID, "FAILED")
        update_run_status(run_id, "FAILED", str(context.get("exception", "")))


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="PCA→COA outreach simulation on appointment dataset",
         start_date=datetime(2026, 1, 1), catchup=False,
         tags=["pca", "coa", "outreach", "smartcare"],
         on_failure_callback=on_failure) as dag:

    t1 = PythonOperator(task_id="assess_patient_context", python_callable=assess_patient_context)
    t2 = PythonOperator(task_id="select_outreach_channel", python_callable=select_outreach_channel)
    t3 = PythonOperator(task_id="generate_report", python_callable=generate_report)

    t1 >> t2 >> t3
