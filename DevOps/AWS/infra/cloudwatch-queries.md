# CloudWatch Log Queries — SmartCare Agents

Reference queries for troubleshooting and analytics using OpenSearch PPL, OpenSearch SQL, and CloudWatch Logs Insights.

## Log Group Classes

| Log Group | Class | Query Support | Retention |
|---|---|---|---|
| `/aws/lambda/smartcare-pca-*` | Standard | Logs Insights + PPL + SQL | 14 days |
| `/aws/lambda/smartcare-coa-*` | Standard | Logs Insights + PPL + SQL | 14 days |
| `/aws/lambda/smartcare-psa-*` | Standard | Logs Insights + PPL + SQL | 14 days |
| `/aws/lambda/smartcare-rra-*` | Standard | Logs Insights + PPL + SQL | 14 days |
| `/aws/lambda/smartcare-aca-*` | Infrequent Access | Logs Insights + PPL + SQL | 365 days |
| `/smartcare/audit/outreach-decisions-*` | Infrequent Access | Logs Insights + PPL + SQL | 7 years |
| `/smartcare/audit/data-protection-findings-*` | Infrequent Access | Logs Insights + PPL + SQL | 7 years |
| `/aws/states/smartcare-outreach-*` | Standard | Logs Insights + PPL + SQL | 14 days |

---

## OpenSearch PPL Queries

### Channel distribution by context state (using field index)

```ppl
source = [`aws:fieldIndex`="context_state"]
| stats count() by context_state, channel
| sort -count()
```

### High-risk patients that were not reached

```ppl
source = [`aws:fieldIndex`="action", `action` = "OUTREACH_FAILED"]
| where risk_score > 0.65
| fields patient_id, context_state, risk_score, channel, @timestamp
| sort -@timestamp
| head 50
```

### PCA agent latency breakdown

```ppl
source = [lg:`/aws/lambda/smartcare-pca-dev`]
| where status = "completed"
| stats avg(duration_ms) as avg_ms, p99(duration_ms) as p99_ms, count() as total by context_state
```

### COA channel selection over time (trendline)

```ppl
source = [`aws:fieldIndex`="channel"]
| stats count() as outreach_count by channel, span(@timestamp, 1h) as hour
| sort hour
```

### Outreach decisions for a specific patient

```ppl
source = [`aws:fieldIndex`="patient_id", `patient_id` = "29872499824296"]
| fields @timestamp, agent, context_state, risk_score, channel, action
| sort @timestamp
```

### Join PCA and COA logs for end-to-end trace

```ppl
source = [lg:`/aws/lambda/smartcare-pca-dev`]
| where appointment_id IS NOT NULL
| LEFT JOIN left=pca, right=coa ON pca.appointment_id = coa.appointment_id
  [lg:`/aws/lambda/smartcare-coa-dev`]
| fields pca.context_state, pca.risk_score, coa.channel, coa.action, coa.@timestamp
| head 100
```

### DLQ analysis — failed outreach events

```ppl
source = [`aws:fieldIndex`="status", `status` = "FAILED"]
| stats count() by agent, channel
| sort -count()
```

---

## OpenSearch SQL Queries

### Channel distribution summary

```sql
SELECT context_state, channel, COUNT(*) as count
FROM `/aws/lambda/smartcare-coa-dev`
WHERE action = 'OUTREACH_EXECUTED'
GROUP BY context_state, channel
ORDER BY count DESC
```

### Patients with risk score above threshold

```sql
SELECT patient_id, risk_score, context_state, channel
FROM `/aws/lambda/smartcare-pca-dev`
WHERE risk_score > 0.65
ORDER BY risk_score DESC
LIMIT 100
```

### Average outreach response time by channel

```sql
SELECT channel,
       AVG(response_time_ms) as avg_response_ms,
       COUNT(*) as total
FROM `/aws/lambda/smartcare-coa-dev`
WHERE action = 'OUTREACH_EXECUTED'
GROUP BY channel
```

---

## CloudWatch Logs Insights Queries

### Error summary across all agents (last 1 hour)

```
fields @timestamp, @message, agent, @logStream
| filter @message like /ERROR/
| stats count() by agent
| sort count desc
```

### Step Functions execution failures with context

```
fields @timestamp, execution_arn, error, cause
| filter ispresent(error)
| sort @timestamp desc
| limit 20
```

### Data protection findings — PHI detected in logs

```
fields @timestamp, @logStream, policyName, dataIdentifierArn, findingType
| filter findingType = "SENSITIVE_DATA"
| stats count() by dataIdentifierArn
| sort count desc
```

### Cold start analysis for PCA Lambda

```
fields @timestamp, @duration, @billedDuration, @initDuration
| filter ispresent(@initDuration)
| stats count() as cold_starts, avg(@initDuration) as avg_init_ms, max(@initDuration) as max_init_ms
```

---

## CLI Examples

### Run a PPL query via AWS CLI

```bash
aws logs start-query \
  --log-group-names "/aws/lambda/smartcare-pca-dev" "/aws/lambda/smartcare-coa-dev" \
  --start-time $(date -d '1 hour ago' +%s) \
  --end-time $(date +%s) \
  --query-language PPL \
  --query-string 'source = [`aws:fieldIndex`="context_state"] | stats count() by context_state, channel'

# Get results (use query-id from above)
aws logs get-query-results --query-id "QUERY_ID"
```

### Run a SQL query via AWS CLI

```bash
aws logs start-query \
  --log-group-names "/aws/lambda/smartcare-coa-dev" \
  --start-time $(date -d '1 hour ago' +%s) \
  --end-time $(date +%s) \
  --query-language SQL \
  --query-string "SELECT channel, COUNT(*) as count FROM \`/aws/lambda/smartcare-coa-dev\` GROUP BY channel"
```

### Unmask PHI for authorized investigation

```bash
# Requires logs:Unmask permission — restricted to compliance officers
aws logs get-log-events \
  --log-group-name "/smartcare/audit/outreach-decisions-dev" \
  --log-stream-name "STREAM_NAME" \
  --unmask
```
