"""
Real-Time Outreach Agent DAG (DAG 2)

Agentic AI for ONE patient — receives a single patient record and
orchestrates the full PCA→COA pipeline with LLM reasoning:

  Task 1 (@task.agent pattern - PCA): LLM reasons about patient context
    - Analyzes: age, appointment time, SMS history, chronic conditions
    - Classifies: C_p context state with natural language reasoning
    - Assesses: urgency level and risk factors

  Task 2 (COA Decision): Selects outreach channel based on PCA assessment
    - Maps C_p → channel (IVR/SMS/Callback)
    - Generates channel-specific action plan

  Task 3 (COA Execute): Simulates outreach execution
    - Places IVR call / sends SMS / schedules callback
    - Interprets patient response
    - Determines next action: confirmed, rescheduled, escalate to admin

  Task 4 (Publish): Writes action record, publishes to message broker

This DAG is triggered async by the batch DAG — many instances run in parallel.
"""
import sys
import os
import json
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta
from smartcare_utils import notify_broker

DAG_ID = "realtime_outreach_agent"
default_args = {"owner": "smartcare", "retries": 0, "retry_delay": timedelta(minutes=1)}

# Channel configuration
CHANNEL_MAP = {
    'REACHABLE_MOBILE': {'channel': 'VOICE_IVR', 'label': 'Voice IVR Call', 'service': 'Amazon Connect'},
    'REACHABLE_STATIONARY': {'channel': 'SMS_DEEPLINK', 'label': 'SMS Deep-Link', 'service': 'Amazon SNS'},
    'UNREACHABLE': {'channel': 'CALLBACK', 'label': 'Scheduled Callback', 'service': 'Amazon Connect'}
}


def pca_assess_context(**kwargs):
    """
    PCA Agent: Assess patient context using LLM-style reasoning.

    In production, this would use @task.agent with Bedrock Claude:
        @task.agent(model="bedrock/claude-3", tools=[check_history, assess_risk])
        def assess_patient(patient_data):
            return f"Assess context for patient: {patient_data}"

    For now, simulates LLM reasoning with structured decision logic
    that mirrors what the LLM would produce.
    """
    import random
    random.seed(hash(str(kwargs.get("dag_run").conf.get("patient_id", ""))))

    conf = kwargs.get("dag_run").conf or {}
    patient_id = conf.get("patient_id", "unknown")
    age = conf.get("age", 0)
    hour = conf.get("hour", 12)
    day_of_week = conf.get("day_of_week", 0)
    sms_received = conf.get("sms_received", 0)
    lead_time = conf.get("lead_time_days", 0)
    hypertension = conf.get("hypertension", 0)
    diabetes = conf.get("diabetes", 0)

    print(f"🤖 PCA Agent: Assessing patient {patient_id}")
    print(f"   Profile: age={age}, appointment hour={hour}, day={day_of_week}, "
          f"SMS history={'yes' if sms_received else 'no'}, lead time={lead_time}d")

    # LLM-style reasoning chain (what Bedrock Claude would think)
    reasoning_steps = []
    risk_factors = []

    # Step 1: Check engagement history
    if not sms_received:
        reasoning_steps.append(
            f"Patient has NO prior SMS engagement — this suggests low digital health literacy "
            f"or disengagement from the healthcare system.")
        risk_factors.append("no_sms_history")
    else:
        reasoning_steps.append(f"Patient has responded to SMS before — digital channel viable.")

    # Step 2: Assess time-based context
    is_weekday = day_of_week < 5
    if is_weekday and (7 <= hour <= 9):
        context_reasoning = "Weekday morning (7-9 AM) — patient likely commuting to work. Mobile context."
        c_p = 'REACHABLE_MOBILE'
    elif is_weekday and (16 <= hour <= 19):
        context_reasoning = "Weekday evening (4-7 PM) — patient likely commuting home. Mobile context."
        c_p = 'REACHABLE_MOBILE'
    elif is_weekday and (9 < hour < 16):
        context_reasoning = "Weekday work hours (9 AM-4 PM) — patient likely at desk or work. Stationary context."
        c_p = 'REACHABLE_STATIONARY'
    else:
        context_reasoning = f"Off-hours (hour={hour}, day={day_of_week}) — context uncertain."
        c_p = 'REACHABLE_STATIONARY'
    reasoning_steps.append(context_reasoning)

    # Step 3: Check risk factors
    if age >= 65:
        reasoning_steps.append(f"Elderly patient (age={age}) — may need extra support, consider callback.")
        risk_factors.append("elderly")
    if hypertension or diabetes:
        conditions = []
        if hypertension: conditions.append("hypertension")
        if diabetes: conditions.append("diabetes")
        reasoning_steps.append(f"Chronic conditions: {', '.join(conditions)} — appointment is clinically important.")
        risk_factors.append("chronic_conditions")
    if lead_time > 14:
        reasoning_steps.append(f"Long lead time ({lead_time} days) — patient may have forgotten. Higher urgency.")
        risk_factors.append("long_lead_time")

    # Step 4: Override to UNREACHABLE if strong signals
    if not sms_received and len(risk_factors) >= 2:
        c_p = 'UNREACHABLE'
        reasoning_steps.append(
            f"OVERRIDE: Multiple risk factors ({', '.join(risk_factors)}) + no SMS history → "
            f"classifying as UNREACHABLE. Needs proactive callback, not passive SMS.")

    # Step 5: Determine urgency
    urgency = 'ROUTINE'
    if len(risk_factors) >= 2:
        urgency = 'HIGH'
    elif len(risk_factors) >= 1:
        urgency = 'MEDIUM'

    pca_result = {
        "patient_id": patient_id,
        "context_state": c_p,
        "urgency": urgency,
        "risk_factors": risk_factors,
        "reasoning": " → ".join(reasoning_steps),
        "full_reasoning_steps": reasoning_steps
    }

    print(f"   PCA Decision: C_p={c_p}, urgency={urgency}")
    print(f"   Reasoning: {pca_result['reasoning'][:200]}...")

    # Pass to next task via XCom
    kwargs['ti'].xcom_push(key='pca_result', value=json.dumps(pca_result))


def coa_select_and_execute(**kwargs):
    """
    COA Agent: Select channel and execute outreach.

    In production with @task.llm_branch:
        @task.llm_branch(model="bedrock/claude-3")
        def select_channel(pca_assessment):
            return f"Based on {pca_assessment}, select IVR, SMS, or Callback"

    Then @task.agent for execution:
        @task.agent(model="bedrock/claude-3", tools=[place_call, send_sms, schedule_callback])
        def execute_outreach(channel, patient):
            return f"Execute {channel} outreach for {patient}"
    """
    import random

    conf = kwargs.get("dag_run").conf or {}
    patient_id = conf.get("patient_id", "unknown")
    random.seed(hash(patient_id))

    pca_json = kwargs['ti'].xcom_pull(key='pca_result', task_ids='pca_assess_context')
    pca = json.loads(pca_json)

    c_p = pca['context_state']
    urgency = pca['urgency']
    channel_info = CHANNEL_MAP[c_p]

    print(f"🤖 COA Agent: Acting on patient {patient_id}")
    print(f"   PCA says: C_p={c_p}, urgency={urgency}")
    print(f"   COA selects: {channel_info['label']} via {channel_info['service']}")

    # COA reasoning
    coa_reasoning = {
        'REACHABLE_MOBILE': (
            f"Patient is in mobile context → Voice IVR selected. "
            f"Interactive voice call allows immediate confirmation while patient is alert. "
            f"Using Amazon Connect contact flow with appointment details."
        ),
        'REACHABLE_STATIONARY': (
            f"Patient is stationary → SMS Deep-Link selected. "
            f"Rich SMS with tap-to-confirm link is optimal for desk/home context. "
            f"Using Amazon SNS with branded deep-link to patient portal."
        ),
        'UNREACHABLE': (
            f"Patient is unreachable via standard channels → Scheduled Callback selected. "
            f"A human-assisted callback at a suitable time gives best chance of engagement. "
            f"{'HIGH PRIORITY: ' if urgency == 'HIGH' else ''}Queuing for next available slot."
        )
    }[c_p]

    # Simulate outreach execution and patient response
    channel = channel_info['channel']
    response_rates = {
        'VOICE_IVR': {'confirmed': 0.60, 'rescheduled': 0.15, 'no_answer': 0.20, 'cancelled': 0.05},
        'SMS_DEEPLINK': {'confirmed': 0.55, 'rescheduled': 0.20, 'no_answer': 0.20, 'cancelled': 0.05},
        'CALLBACK': {'confirmed': 0.35, 'rescheduled': 0.15, 'no_answer': 0.40, 'cancelled': 0.10}
    }
    rates = response_rates[channel]
    response = random.choices(list(rates.keys()), weights=list(rates.values()), k=1)[0]

    # Determine action based on response
    needs_admin = False
    action_details = {
        'confirmed': f"Patient confirmed appointment via {channel_info['label']}. Slot secured.",
        'rescheduled': f"Patient requested reschedule via {channel_info['label']}. Slot released to waitlist. RRA agent notified.",
        'no_answer': f"No response to {channel_info['label']}. "
                     + (f"ESCALATED TO ADMIN: High-urgency patient needs manual follow-up."
                        if urgency in ['HIGH', 'MEDIUM']
                        else f"Queued for retry in 2 hours."),
        'cancelled': f"Patient cancelled via {channel_info['label']}. Slot released. RRA agent offering to waitlist candidates."
    }[response]

    if response == 'no_answer' and urgency in ['HIGH', 'MEDIUM']:
        needs_admin = True
    if response == 'cancelled':
        needs_admin = True  # Admin should know about cancellations

    coa_result = {
        "patient_id": patient_id,
        "channel": channel,
        "channel_label": channel_info['label'],
        "channel_service": channel_info['service'],
        "coa_reasoning": coa_reasoning,
        "patient_response": response,
        "action_taken": action_details,
        "needs_admin_attention": needs_admin,
        "pca_context": c_p,
        "pca_urgency": urgency,
        "pca_reasoning": pca['reasoning'],
        "pca_risk_factors": pca['risk_factors']
    }

    print(f"   Response: {response} → {action_details[:100]}...")
    if needs_admin:
        print(f"   ⚠️  ADMIN ALERT: This patient needs human follow-up")

    kwargs['ti'].xcom_push(key='coa_result', value=json.dumps(coa_result))


def publish_action(**kwargs):
    """Post action record to Spring Boot API and publish to message broker."""
    import requests
    from smartcare_utils import get_api_url, get_broker_url

    conf = kwargs.get("dag_run").conf or {}
    patient_id = conf.get("patient_id", "unknown")
    batch_run_id = conf.get("batch_run_id")
    dag_run_id = kwargs.get("dag_run").run_id if kwargs.get("dag_run") else "unknown"

    coa_json = kwargs['ti'].xcom_pull(key='coa_result', task_ids='coa_select_and_execute')
    coa = json.loads(coa_json)

    # Determine action status
    needs_admin = coa["needs_admin_attention"]
    if coa["patient_response"] in ['confirmed', 'rescheduled']:
        action_status = "ACTION_TAKEN"
    elif needs_admin:
        action_status = "ESCALATED_TO_ADMIN"
    else:
        action_status = "ACTION_TAKEN"

    # Build full detail JSON
    action_detail = {
        "patient_profile": {
            "age": conf.get("age"),
            "appointment_day": conf.get("appointment_day"),
            "lead_time_days": conf.get("lead_time_days"),
            "sms_history": bool(conf.get("sms_received")),
            "chronic_conditions": {
                "hypertension": bool(conf.get("hypertension")),
                "diabetes": bool(conf.get("diabetes"))
            },
            "actual_noshow": conf.get("actual_noshow")
        },
        "pca_assessment": {
            "context_state": coa["pca_context"],
            "urgency": coa["pca_urgency"],
            "risk_factors": coa["pca_risk_factors"],
            "reasoning": coa["pca_reasoning"]
        },
        "coa_decision": {
            "channel": coa["channel"],
            "channel_label": coa["channel_label"],
            "service": coa["channel_service"],
            "reasoning": coa["coa_reasoning"]
        },
        "outcome": {
            "patient_response": coa["patient_response"],
            "action_taken": coa["action_taken"],
            "escalated_to_admin": needs_admin
        }
    }

    # Composite key: {batchRunId}_{engineInstanceId}_{patientId} (max 100 chars)
    action_key = f"{batch_run_id}_{dag_run_id[:40]}_{patient_id}"
    if len(action_key) > 100:
        action_key = action_key[:100]

    # POST to Spring Boot API
    api_url = get_api_url()
    try:
        agents_url = api_url.replace("/admin/v1", "/agents/customer/v1")
        resp = requests.post(f"{agents_url}/outreach-actions", json={
            "actionKey": action_key,
            "workflowRunId": batch_run_id,
            "workflowEngineType": "AIRFLOW",
            "engineInstanceId": dag_run_id,
            "patientId": patient_id,
            "contextState": coa["pca_context"],
            "channelSelected": coa["channel"],
            "patientResponse": coa["patient_response"],
            "actionStatus": action_status,
            "actionDetailJson": json.dumps(action_detail)
        }, timeout=10)
        print(f"   API response: {resp.status_code}")
    except Exception as e:
        print(f"   API post failed: {e}")

    # Publish to message broker
    notify_broker(batch_run_id or patient_id, DAG_ID, "ACTION_COMPLETED")

    # Publish admin alert if needed
    if needs_admin:
        try:
            broker_url = get_broker_url()
            requests.post(f"{broker_url}/publish", json={
                "queueName": "admin_alert_event",
                "messageKey": f"alert.{coa['patient_response']}",
                "payload": json.dumps({
                    "patient_id": patient_id,
                    "urgency": coa["pca_urgency"],
                    "reason": coa["action_taken"],
                    "channel_tried": coa["channel_label"]
                }),
                "ctxData": {"dagId": DAG_ID, "patientId": patient_id}
            }, timeout=5)
        except Exception as e:
            print(f"   Admin alert publish failed: {e}")

    print(f"✅ Agent completed for {patient_id}: {coa['patient_response']} via {coa['channel_label']} [{action_status}]")


def on_failure(context):
    conf = context.get("dag_run").conf or {}
    patient_id = conf.get("patient_id", "unknown")
    print(f"❌ Agent FAILED for patient {patient_id}: {context.get('exception')}")


with DAG(dag_id=DAG_ID, default_args=default_args, schedule=None,
         description="Agentic AI: PCA→COA outreach for ONE patient with LLM reasoning",
         start_date=datetime(2026, 1, 1), catchup=False,
         max_active_runs=10,  # up to 10 patients processed in parallel
         tags=["agent", "pca", "coa", "realtime", "smartcare"],
         on_failure_callback=on_failure) as dag:

    t1 = PythonOperator(task_id="pca_assess_context", python_callable=pca_assess_context)
    t2 = PythonOperator(task_id="coa_select_and_execute", python_callable=coa_select_and_execute)
    t3 = PythonOperator(task_id="publish_action", python_callable=publish_action)

    t1 >> t2 >> t3
