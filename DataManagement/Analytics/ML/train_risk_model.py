"""
PCA Risk Model — XGBoost No-Show Prediction
Trains on the Medical Appointment No-Show dataset and produces real metrics.
"""
import os
import json
import pandas as pd
import numpy as np
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.metrics import (classification_report, confusion_matrix,
                             roc_auc_score, f1_score, precision_score, recall_score)
from xgboost import XGBClassifier
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

# Paths
DATA_DIR = os.path.expanduser("~/runtime_data/DataSets/SmartCare-Admin/Datasets-Loaded/kaggle-noshow")
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "output")
os.makedirs(OUTPUT_DIR, exist_ok=True)

CSV_FILE = os.path.join(DATA_DIR, "KaggleV2-May-2016.csv")

print(f"Loading data from {CSV_FILE}...")
df = pd.read_csv(CSV_FILE)
print(f"Loaded {len(df)} records, {df.shape[1]} columns")

# Feature engineering
df['ScheduledDay'] = pd.to_datetime(df['ScheduledDay'])
df['AppointmentDay'] = pd.to_datetime(df['AppointmentDay'])
df['LeadTimeDays'] = (df['AppointmentDay'] - df['ScheduledDay']).dt.days
df['DayOfWeek'] = df['AppointmentDay'].dt.dayofweek
df['Hour'] = df['ScheduledDay'].dt.hour
df['NoShow'] = (df['No-show'] == 'Yes').astype(int)

# Features
features = ['Age', 'Scholarship', 'Hipertension', 'Diabetes', 'Alcoholism',
            'Handcap', 'SMS_received', 'LeadTimeDays', 'DayOfWeek', 'Hour']

# Clean data
df = df[df['LeadTimeDays'] >= 0]  # Remove negative lead times
X = df[features].fillna(0)
y = df['NoShow']

print(f"\nDataset: {len(X)} records after cleaning")
print(f"No-show rate: {y.mean()*100:.1f}%")
print(f"Features: {features}")

# XGBoost with 5-fold stratified CV
print("\nTraining XGBoost with 5-fold stratified cross-validation...")
model = XGBClassifier(
    n_estimators=200,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    eval_metric='logloss',
    random_state=42
)

cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
y_pred = cross_val_predict(model, X, y, cv=cv, method='predict')
y_pred_proba = cross_val_predict(model, X, y, cv=cv, method='predict_proba')[:, 1]

# Metrics
f1 = f1_score(y, y_pred, average='macro')
auc = roc_auc_score(y, y_pred_proba)
precision = precision_score(y, y_pred, average='macro')
recall = recall_score(y, y_pred, average='macro')

print(f"\n{'='*50}")
print(f"RESULTS (5-fold Stratified CV)")
print(f"{'='*50}")
print(f"F1 Score (macro):  {f1:.4f}")
print(f"AUC-ROC:           {auc:.4f}")
print(f"Precision (macro): {precision:.4f}")
print(f"Recall (macro):    {recall:.4f}")
print(f"\nClassification Report:")
print(classification_report(y, y_pred, target_names=['Show', 'No-Show']))

# Confusion matrix
cm = confusion_matrix(y, y_pred)
print(f"Confusion Matrix:\n{cm}")

# Save metrics to JSON
metrics = {
    "model": "XGBoost",
    "dataset": "Medical Appointment No-Show (Kaggle)",
    "records": len(X),
    "no_show_rate": round(y.mean() * 100, 1),
    "cv_folds": 5,
    "f1_macro": round(f1, 4),
    "auc_roc": round(auc, 4),
    "precision_macro": round(precision, 4),
    "recall_macro": round(recall, 4),
    "confusion_matrix": cm.tolist(),
    "features": features
}

metrics_path = os.path.join(OUTPUT_DIR, "risk_model_metrics.json")
with open(metrics_path, 'w') as f:
    json.dump(metrics, f, indent=2)
print(f"\nMetrics saved to {metrics_path}")

# Plot confusion matrix
fig, ax = plt.subplots(figsize=(6, 5))
im = ax.imshow(cm, cmap='Blues')
ax.set_xticks([0, 1]); ax.set_yticks([0, 1])
ax.set_xticklabels(['Show', 'No-Show']); ax.set_yticklabels(['Show', 'No-Show'])
ax.set_xlabel('Predicted'); ax.set_ylabel('Actual')
ax.set_title(f'XGBoost No-Show Prediction\nF1={f1:.3f} | AUC={auc:.3f}')
for i in range(2):
    for j in range(2):
        ax.text(j, i, f'{cm[i,j]:,}', ha='center', va='center', fontsize=14,
                color='white' if cm[i,j] > cm.max()/2 else 'black')
plt.colorbar(im)
plt.tight_layout()
cm_path = os.path.join(OUTPUT_DIR, "confusion_matrix.png")
plt.savefig(cm_path, dpi=200)
print(f"Confusion matrix saved to {cm_path}")

# Train final model on all data for feature importance
model.fit(X, y)
importance = dict(zip(features, model.feature_importances_))
print(f"\nFeature Importance:")
for feat, imp in sorted(importance.items(), key=lambda x: -x[1]):
    print(f"  {feat:20s} {imp:.4f}")

print(f"\nDone! All outputs in {OUTPUT_DIR}")
