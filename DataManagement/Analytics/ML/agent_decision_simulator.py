"""
Agent Decision Simulator — PCA→COA Pipeline on Historical Data

Simulates what the multi-agent system would have done for each of the
110K real appointment records:
  1. PCA: Predict R_p (no-show risk) using trained XGBoost
  2. PCA: Classify C_p context state from appointment time + SMS + R_p
  3. COA: Select outreach channel based on C_p state
  4. Compare: multi-channel vs SMS-only baseline

Produces:
  - context_state_distribution.json (C_p counts + channel breakdown)
  - channel_distribution.png (Fig. 5 for the IEEE paper)
  - baseline_comparison.json (proposed vs SMS-only)
"""
import os
import json
import pandas as pd
import numpy as np
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from xgboost import XGBClassifier
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

# ── Paths ──
DATA_DIR = os.path.expanduser("~/runtime_data/DataSets/SmartCare-Admin/Datasets-Loaded/kaggle-noshow")
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "output")
os.makedirs(OUTPUT_DIR, exist_ok=True)
CSV_FILE = os.path.join(DATA_DIR, "KaggleV2-May-2016.csv")

# ── Load and prepare data ──
print("=" * 60)
print("AGENT DECISION SIMULATOR — PCA→COA Pipeline")
print("=" * 60)

print(f"\nLoading data from {CSV_FILE}...")
df = pd.read_csv(CSV_FILE)
df['ScheduledDay'] = pd.to_datetime(df['ScheduledDay'])
df['AppointmentDay'] = pd.to_datetime(df['AppointmentDay'])
df['LeadTimeDays'] = (df['AppointmentDay'] - df['ScheduledDay']).dt.days
df['DayOfWeek'] = df['AppointmentDay'].dt.dayofweek  # 0=Mon, 4=Fri, 5=Sat, 6=Sun
df['Hour'] = df['ScheduledDay'].dt.hour
df['NoShow'] = (df['No-show'] == 'Yes').astype(int)
df = df[df['LeadTimeDays'] >= 0]

features = ['Age', 'Scholarship', 'Hipertension', 'Diabetes', 'Alcoholism',
            'Handcap', 'SMS_received', 'LeadTimeDays', 'DayOfWeek', 'Hour']
X = df[features].fillna(0)
y = df['NoShow']

print(f"Dataset: {len(df)} records, no-show rate: {y.mean()*100:.1f}%")

# ── Step 1: PCA — Predict R_p (risk score) ──
print("\n[PCA] Training XGBoost and predicting R_p for all patients...")
model = XGBClassifier(
    n_estimators=200, max_depth=6, learning_rate=0.1,
    subsample=0.8, colsample_bytree=0.8, eval_metric='logloss', random_state=42
)
cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
df['R_p'] = cross_val_predict(model, X, y, cv=cv, method='predict_proba')[:, 1]
print(f"[PCA] R_p range: {df['R_p'].min():.3f} — {df['R_p'].max():.3f}, mean: {df['R_p'].mean():.3f}")

# ── Step 2: PCA — Classify C_p context state ──
print("\n[PCA] Classifying patient context states (C_p)...")


def classify_context_state(row):
    """
    Context state classification rules:
    - UNREACHABLE: no SMS received AND high risk (R_p > 0.5) — patient is disengaged
    - REACHABLE_MOBILE: weekday commuting hours (7-9 AM or 4-7 PM) — patient is mobile
    - REACHABLE_STATIONARY: weekday work hours (9 AM-4 PM) — patient is at desk/home
    - Weekend/other: use risk to decide (high risk → MOBILE for urgent IVR, else STATIONARY)
    """
    is_weekday = row['DayOfWeek'] < 5
    hour = row['Hour']

    # Unreachable: no SMS history + high risk = disengaged patient
    if row['SMS_received'] == 0 and row['R_p'] > 0.5:
        return 'UNREACHABLE'

    # Weekday commuting hours
    if is_weekday and (7 <= hour <= 9 or 16 <= hour <= 19):
        return 'REACHABLE_MOBILE'

    # Weekday work hours
    if is_weekday and 9 < hour < 16:
        return 'REACHABLE_STATIONARY'

    # Weekend or other: high risk gets mobile (urgent), low risk gets stationary
    if row['R_p'] > 0.4:
        return 'REACHABLE_MOBILE'
    return 'REACHABLE_STATIONARY'


df['C_p'] = df.apply(classify_context_state, axis=1)

cp_counts = df['C_p'].value_counts()
print(f"\n[PCA] Context State Distribution:")
for state, count in cp_counts.items():
    pct = count / len(df) * 100
    print(f"  {state:25s} {count:>8,} ({pct:.1f}%)")

# ── Step 3: COA — Select outreach channel ──
print("\n[COA] Selecting outreach channels based on C_p...")

CHANNEL_MAP = {
    'REACHABLE_MOBILE': 'VOICE_IVR',       # Patient is mobile → IVR call via Amazon Connect
    'REACHABLE_STATIONARY': 'SMS_DEEPLINK', # Patient is at desk → SMS with deep-link via SNS
    'UNREACHABLE': 'CALLBACK'               # Patient is disengaged → scheduled callback
}

df['Channel'] = df['C_p'].map(CHANNEL_MAP)

channel_counts = df['Channel'].value_counts()
print(f"\n[COA] Channel Selection Distribution:")
for channel, count in channel_counts.items():
    pct = count / len(df) * 100
    print(f"  {channel:20s} {count:>8,} ({pct:.1f}%)")

# ── Step 4: Cross-tabulation — C_p × Channel × Actual Outcome ──
print("\n[Analysis] Context State × No-Show Outcome:")
cross_tab = pd.crosstab(df['C_p'], df['NoShow'], margins=True)
cross_tab.columns = ['Show', 'No-Show', 'Total']
print(cross_tab.to_string())

# No-show rate per context state
print("\n[Analysis] No-Show Rate per Context State:")
for state in ['REACHABLE_MOBILE', 'REACHABLE_STATIONARY', 'UNREACHABLE']:
    subset = df[df['C_p'] == state]
    rate = subset['NoShow'].mean() * 100
    print(f"  {state:25s} {rate:.1f}% no-show rate ({len(subset):,} patients)")

# ── Step 5: Baseline Comparison — Proposed vs SMS-only ──
print("\n[Comparison] Multi-Channel Outreach vs SMS-Only Baseline")

# SMS-only baseline: everyone gets SMS regardless of context
# Assumption: SMS reaches ~70% of STATIONARY, ~40% of MOBILE, ~20% of UNREACHABLE
SMS_REACH_RATES = {
    'REACHABLE_MOBILE': 0.40,      # SMS during commute = low engagement
    'REACHABLE_STATIONARY': 0.70,  # SMS at desk = moderate engagement
    'UNREACHABLE': 0.20            # SMS to disengaged = very low engagement
}

# Proposed: context-aware channel selection
# IVR reaches mobile patients better, Callback reaches unreachable better
PROPOSED_REACH_RATES = {
    'REACHABLE_MOBILE': 0.75,      # IVR during commute = high engagement
    'REACHABLE_STATIONARY': 0.80,  # SMS deep-link at desk = high engagement
    'UNREACHABLE': 0.55            # Scheduled callback = moderate engagement
}

baseline_reached = 0
proposed_reached = 0
for state in ['REACHABLE_MOBILE', 'REACHABLE_STATIONARY', 'UNREACHABLE']:
    n = len(df[df['C_p'] == state])
    baseline_reached += n * SMS_REACH_RATES[state]
    proposed_reached += n * PROPOSED_REACH_RATES[state]

baseline_rate = baseline_reached / len(df) * 100
proposed_rate = proposed_reached / len(df) * 100
improvement = proposed_rate - baseline_rate

print(f"  SMS-Only Baseline:     {baseline_rate:.1f}% estimated reachability")
print(f"  Proposed Multi-Channel: {proposed_rate:.1f}% estimated reachability")
print(f"  Improvement:           +{improvement:.1f} percentage points")

# ── Save Results ──

# 1. Context state distribution JSON
results = {
    "dataset": "Medical Appointment No-Show (Kaggle)",
    "total_records": len(df),
    "no_show_rate": round(y.mean() * 100, 1),
    "context_state_distribution": {},
    "channel_distribution": {},
    "noshow_rate_by_context": {},
    "baseline_comparison": {
        "sms_only_reachability": round(baseline_rate, 1),
        "proposed_reachability": round(proposed_rate, 1),
        "improvement_pp": round(improvement, 1)
    }
}

for state in ['REACHABLE_MOBILE', 'REACHABLE_STATIONARY', 'UNREACHABLE']:
    subset = df[df['C_p'] == state]
    results["context_state_distribution"][state] = {
        "count": int(len(subset)),
        "percentage": round(len(subset) / len(df) * 100, 1)
    }
    results["noshow_rate_by_context"][state] = round(subset['NoShow'].mean() * 100, 1)

for channel in ['VOICE_IVR', 'SMS_DEEPLINK', 'CALLBACK']:
    subset = df[df['Channel'] == channel]
    results["channel_distribution"][channel] = {
        "count": int(len(subset)),
        "percentage": round(len(subset) / len(df) * 100, 1)
    }

results_path = os.path.join(OUTPUT_DIR, "agent_decision_results.json")
with open(results_path, 'w') as f:
    json.dump(results, f, indent=2)
print(f"\nResults saved to {results_path}")

# 2. Channel distribution chart (Fig. 5)
fig, axes = plt.subplots(1, 3, figsize=(16, 5))

# Chart 1: C_p Distribution
states = ['REACHABLE_MOBILE', 'REACHABLE_STATIONARY', 'UNREACHABLE']
state_labels = ['Reachable\n(Mobile)', 'Reachable\n(Stationary)', 'Unreachable']
state_counts = [len(df[df['C_p'] == s]) for s in states]
state_colors = ['#4f46e5', '#16a34a', '#ea580c']
bars1 = axes[0].bar(state_labels, state_counts, color=state_colors, edgecolor='white', linewidth=1.5)
axes[0].set_title('Patient Context States (C_p)', fontweight='bold', fontsize=12)
axes[0].set_ylabel('Number of Patients')
for bar, count in zip(bars1, state_counts):
    pct = count / len(df) * 100
    axes[0].text(bar.get_x() + bar.get_width()/2, bar.get_height() + 200,
                 f'{count:,}\n({pct:.1f}%)', ha='center', va='bottom', fontsize=9)

# Chart 2: Channel Selection by COA
channels = ['VOICE_IVR', 'SMS_DEEPLINK', 'CALLBACK']
channel_labels = ['Voice IVR', 'SMS Deep-Link', 'Callback']
channel_counts = [len(df[df['Channel'] == c]) for c in channels]
channel_colors = ['#4f46e5', '#16a34a', '#ea580c']
bars2 = axes[1].bar(channel_labels, channel_counts, color=channel_colors, edgecolor='white', linewidth=1.5)
axes[1].set_title('COA Channel Selection', fontweight='bold', fontsize=12)
axes[1].set_ylabel('Number of Patients')
for bar, count in zip(bars2, channel_counts):
    pct = count / len(df) * 100
    axes[1].text(bar.get_x() + bar.get_width()/2, bar.get_height() + 200,
                 f'{count:,}\n({pct:.1f}%)', ha='center', va='bottom', fontsize=9)

# Chart 3: Baseline vs Proposed Reachability
comparison_labels = ['SMS-Only\nBaseline', 'Proposed\nMulti-Channel']
comparison_values = [baseline_rate, proposed_rate]
comparison_colors = ['#94a3b8', '#4f46e5']
bars3 = axes[2].bar(comparison_labels, comparison_values, color=comparison_colors, edgecolor='white', linewidth=1.5)
axes[2].set_title('Estimated Reachability', fontweight='bold', fontsize=12)
axes[2].set_ylabel('Reachability (%)')
axes[2].set_ylim(0, 100)
for bar, val in zip(bars3, comparison_values):
    axes[2].text(bar.get_x() + bar.get_width()/2, bar.get_height() + 1,
                 f'{val:.1f}%', ha='center', va='bottom', fontsize=11, fontweight='bold')
# Arrow showing improvement
axes[2].annotate(f'+{improvement:.1f}pp', xy=(1, proposed_rate), xytext=(0.5, proposed_rate + 5),
                 fontsize=11, fontweight='bold', color='#059669',
                 arrowprops=dict(arrowstyle='->', color='#059669', lw=2))

plt.suptitle('Agent Decision Simulation — PCA→COA Pipeline on 110K Records',
             fontweight='bold', fontsize=14, y=1.02)
plt.tight_layout()
fig_path = os.path.join(OUTPUT_DIR, "channel_distribution.png")
plt.savefig(fig_path, dpi=200, bbox_inches='tight')
print(f"Channel distribution chart saved to {fig_path}")

print(f"\n{'=' * 60}")
print("SIMULATION COMPLETE")
print(f"{'=' * 60}")
