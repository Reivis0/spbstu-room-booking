# Alertmanager Routing & Grouping - Architecture Summary

## 📊 Alert Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PROMETHEUS FIRING ALERTS                         │
│              (with labels: severity, service, env, etc.)            │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │  ALERTMANAGER INGESTION │
                    │  - Parse labels        │
                    │  - Check inhibitions   │
                    │  - Group alerts        │
                    └────────────┬────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
          ▼                      ▼                      ▼
    ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
    │ GROUPING     │      │ GROUPING     │      │ GROUPING     │
    │ wait: 10s    │      │ wait: 10s    │      │ wait: 30s    │
    │ interval: 5m │      │ interval: 5m │      │ interval: 10m│
    └──────────────┘      └──────────────┘      └──────────────┘
          │                      │                      │
    ┌─────▼──────┐         ┌─────▼──────┐        ┌─────▼──────┐
    │ severity   │         │ severity   │        │ severity   │
    │ = critical │         │ = warning  │        │ = info     │
    └─────┬──────┘         └─────┬──────┘        └─────┬──────┘
          │                      │                     │
   ┌──────┴─────────┐            │                     │
   │                │            │                     │
   ▼                ▼            ▼                     ▼
┌──────────┐  ┌──────────┐  ┌──────────┐        ┌──────────┐
│ PagerDuty│  │Slack     │  │Slack     │        │Slack     │
│ Incident │  │#alerts   │  │#database │        │#ops-info │
│ Creation │  │(Warning) │  │(Database)│        │(Info)    │
└────┬─────┘  └──────────┘  └──────────┘        └──────────┘
     │
   ┌─┴──────────────┐
   ▼                ▼
┌─────────┐  ┌─────────────┐
│Slack    │  │Slack        │
│#incidents│  │#infrastructure
│(Critical)│  │(Infrastructure)
└─────────┘  └─────────────┘
```

## 🎯 Grouping Examples

### Example 1: Multiple Instance Failures (Same Service)

**Incoming Alerts:**
```
InstanceDown{service=api, environment=prod, severity=critical}
InstanceDown{service=api, environment=prod, severity=critical}
HighLatency{service=api, environment=prod, severity=warning}
HighErrorRate{service=api, environment=prod, severity=warning}
```

**Grouping Result:**
- Group key: `api:prod:critical`
- Members: InstanceDown (2), HighLatency (suppressed), HighErrorRate (suppressed)

**Notifications:**
1. ✅ PagerDuty: 1 incident (all 2 InstanceDown alerts) + links + runbook
2. ✅ Slack #incidents: 1 message with 2 InstanceDown details
3. ⏭️ Repeat after 4h if still firing
4. 🚫 HighLatency/HighErrorRate alerts suppressed (instance down = root cause)

---

### Example 2: Database Issues (Different Alert Type)

**Incoming Alerts:**
```
PostgreSQL_MaxConnections{service=database, environment=prod, severity=warning}
PostgreSQL_SlowQueries{service=database, environment=prod, severity=warning}
PostgreSQL_BackupFailed{service=database, environment=prod, severity=warning}
```

**Grouping Result:**
- Group key: `database:prod:warning`
- All 3 alerts in single group (same service/env/severity)

**Notifications:**
1. ✅ Slack #database-alerts: 1 message with all 3 database alerts
2. 📊 Includes database instance, metrics, dashboard link
3. ⏭️ Repeat after 1h if still firing
4. 🔕 Not sent to general #alerts (dedicated database channel)

---

### Example 3: Informational Updates (Low Priority)

**Incoming Alerts:**
```
CacheHitRateImproving{service=api, environment=prod, severity=info}
RequestsIncreasing{service=api, environment=prod, severity=info}
DiskUsageNormal{service=infrastructure, environment=prod, severity=info}
```

**Grouping Result:**
- Group 1: `api:prod:info` (2 alerts)
- Group 2: `infrastructure:prod:info` (1 alert)
- Wait 30s to collect more info alerts

**Notifications:**
1. ✅ Slack #ops-info: 2 messages (batched by service)
2. 📝 Informational only, no escalation
3. ⏭️ Repeat after 24h (minimal disturbance)

---

## 🛑 Inhibition Rules in Action

### Scenario: Node Failure

**Timeline:**
```
T=0:  NodeUnreachable fires
      └─ Triggers network error inhibition

T=1:  NetworkErrors attempts to fire
      └─ SUPPRESSED (NodeUnreachable active, same node)
      └─ Root cause = node is down, not network config

T=5:  NodeUnreachable resolves
      └─ Inhibition lifted
      └─ NetworkErrors can now fire if still present
```

### Scenario: Instance Down with Cascading Failures

**Timeline:**
```
T=0:  InstanceDown fires
      └─ Triggers 3 inhibition rules

T=2:  HighLatency fires
      └─ SUPPRESSED (InstanceDown active, same service)
      └─ Reason: dead instance = always high latency

T=3:  HighErrorRate fires
      └─ SUPPRESSED (InstanceDown active, same service)
      └─ Reason: dead instance = always high errors

T=5:  DiskSpaceRunningOut fires
      └─ SUPPRESSED (InstanceDown active, same node)
      └─ Reason: dead instance = no disk I/O possible

T=15: InstanceDown resolves
      └─ All suppressions lifted
      └─ If errors still present → fires independently
```

---

## ⏱️ Timing Strategy Explained

| Component | Value | Rationale |
|-----------|-------|-----------|
| **group_wait: 10s** | Collect related alerts | Allows multiple instance failures to batch together before notification |
| **group_interval: 5m** | Check for new alerts | Merges new alerts into existing group every 5 minutes |
| **repeat_interval: 4h (critical)** | Resend grouped critical | On-call engineers stay aware; not too frequent (prevents burnout) |
| **repeat_interval: 1h (warning)** | Resend grouped warning | Balance visibility + fatigue; enough context to act |
| **repeat_interval: 24h (info)** | Resend grouped info | Minimal disturbance; daily check for trends |

---

## 📡 Receiver Configuration Summary

| Receiver | Channel | Severity | Trigger | Repeat |
|----------|---------|----------|---------|--------|
| **pagerduty-critical** | PagerDuty API | Critical | `severity=critical` | 4h |
| **slack-critical** | #incidents | Critical | `severity=critical` | 4h |
| **slack-alerts** | #alerts | Warning | `severity=warning` | 1h |
| **slack-database** | #database-alerts | Any | `service=database` | 1h |
| **slack-infrastructure** | #infrastructure-alerts | Any | `service=infrastructure` | 1h |
| **slack-info** | #ops-info | Info | `severity=info\|notice` | 24h |

---

## 🔄 Alert Lifecycle

```
1. ALERT FIRES (Prometheus)
   ├─ Assigned labels: severity, service, environment, alertname
   └─ Assigned annotations: summary, description, runbook_url, dashboard_url

2. ALERTMANAGER RECEIVES
   ├─ Evaluates inhibition rules
   ├─ Applies grouping
   └─ Starts group_wait timer (10s)

3. GROUP WAIT (10s)
   ├─ Collects related alerts
   ├─ If new alert matches group → stays grouped
   └─ At 10s: Ready to send

4. ROUTING DECISION
   ├─ Matches routes in order
   ├─ First matching route handles alert
   └─ `continue: true` allows multiple receivers

5. NOTIFICATION SEND (Route-specific)
   ├─ Format message using templates
   ├─ Send to all configured receivers
   └─ Record send time + status

6. GROUP INTERVAL (5m)
   ├─ Check for new alerts
   ├─ Merge new alerts into group if matching
   ├─ Resend notification if changed
   └─ Repeat every 5m

7. REPEAT INTERVAL
   ├─ After 4h (critical) / 1h (warning) / 24h (info)
   ├─ Resend same group (prevent forgetting)
   └─ Reset on new alerts

8. ALERT RESOLVES (Prometheus)
   ├─ Prometheus sends resolve message
   ├─ Alertmanager groups resolve notifications
   ├─ Sends "resolved" status to receivers
   └─ Removes from active group
```

---

## 🚀 Quick Start Commands

**Deploy Alertmanager:**
```bash
# Create .env file from template
cp infrastructure/alertmanager/.env.example infrastructure/alertmanager/.env

# Edit with your webhook URLs
nano infrastructure/alertmanager/.env

# Start with docker-compose
docker-compose up -d alertmanager

# Verify it's running
curl http://localhost:9093/-/healthy
```

**Test Alert:**
```bash
# Send test alert to Alertmanager
curl -X POST http://localhost:9093/api/v1/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {"alertname":"TestAlert","severity":"critical","service":"test"},
    "annotations": {"summary":"Test alert","description":"This is a test"}
  }]'
```

**Verify Slack Delivery:**
- Check #incidents channel for test alert
- Verify PagerDuty incident was created
- Confirm grouping worked (multiple alerts = 1 notification)

---

## 📋 Checklist for Production Deployment

- [ ] All Slack webhook URLs populated in `.env`
- [ ] PagerDuty integration key configured
- [ ] Tested Slack webhook connectivity with curl
- [ ] Tested PagerDuty incident creation
- [ ] Verified inhibition rules suppress expected alerts
- [ ] Tested alert grouping with 2+ related alerts
- [ ] Confirmed repeat intervals suit team's schedule
- [ ] Added runbook/dashboard URLs to alert rules
- [ ] Created runbook procedures for each critical alert
- [ ] Team trained on routing + inhibition logic
- [ ] Monitoring enabled for Alertmanager metrics
- [ ] On-call rotation configured in PagerDuty
- [ ] Tested resolution flow (firing → resolved)
- [ ] Verified logs for delivery failures
- [ ] Set up dashboard for alert trends
