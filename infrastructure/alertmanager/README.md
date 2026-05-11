# Alertmanager Configuration Guide

## Overview

This Alertmanager configuration provides intelligent alert routing and grouping to prevent alert fatigue while ensuring critical incidents receive immediate attention.

## Routing Architecture

```
root (4h repeat)
│
├─ [severity=critical] → pagerduty-critical + slack-critical
│  ├─ [service=database] → slack-database (1h repeat)
│  └─ [service=infrastructure] → slack-infrastructure (1h repeat)
│
├─ [service=database, severity=warning|info] → slack-database (1h repeat)
│
├─ [service=infrastructure, severity=warning|info] → slack-infrastructure (1h repeat)
│
├─ [severity=warning] → slack-alerts (1h repeat)
│
└─ [severity=info|notice] → slack-info (24h repeat)
```

## Grouping Strategy

Alerts are grouped by: `environment`, `service`, `severity`, `alertname`

This ensures:
- Multiple `InstanceDown` alerts for the same service → 1 grouped notification
- `HighErrorRate` + `HighLatency` for same service → 1 grouped notification
- Node exporter failures grouped separately from application failures

### Timing Parameters

| Phase | Interval | Purpose |
|-------|----------|---------|
| **group_wait** | 10s | Wait for related alerts before first notification |
| **group_interval** | 5m | Check for updates and merge new alerts into group |
| **repeat_interval** | Varies by severity | Resend grouped alerts if still firing |

**Repeat Intervals:**
- Critical: **4h** - Ensures on-call engineer stays aware
- Warning: **1h** - Prevents alert fatigue, maintains visibility
- Info: **24h** - Informational only, minimal disturbance

## Receiver Configuration

### 1. **PagerDuty (Critical)**
- **Channel:** `pagerduty-critical`
- **Trigger:** `severity=critical`
- **Behavior:** Immediate incident creation, on-call notification
- **Includes:** Service, environment, cluster, runbook link
- **Recovery:** Alert resolved notifications

### 2. **Slack #incidents (Critical)**
- **Channel:** `#incidents`
- **Trigger:** `severity=critical`
- **Behavior:** Parallel to PagerDuty, human-readable summary
- **Includes:** Status, environment, service, cluster, description, runbook, dashboard links
- **Color:** Red (danger)

### 3. **Slack #alerts (Warning)**
- **Channel:** `#alerts`
- **Trigger:** `severity=warning`
- **Behavior:** Batch notifications, not routed by service
- **Includes:** Service, environment, severity, description, links
- **Color:** Orange (warning)

### 4. **Slack #database-alerts (Database)**
- **Channel:** `#database-alerts`
- **Trigger:** `service=database` (any severity except critical)
- **Behavior:** Isolated channel for database team
- **Includes:** Database instance, severity, description, dashboard
- **Color:** Dynamic (red if firing, green if resolved)

### 5. **Slack #infrastructure-alerts (Infrastructure)**
- **Channel:** `#infrastructure-alerts`
- **Trigger:** `service=infrastructure` (any severity except critical)
- **Behavior:** Isolated channel for infrastructure team
- **Includes:** Node, severity, description, dashboard
- **Color:** Dynamic (red if firing, green if resolved)

### 6. **Slack #ops-info (Info)**
- **Channel:** `#ops-info`
- **Trigger:** `severity=info|notice`
- **Behavior:** Informational only, low urgency
- **Includes:** Service, environment, description, runbook
- **Color:** Green (good)
- **Repeat:** 24h (minimal disturbance)

## Inhibition Rules

Inhibit rules suppress less critical alerts when higher-priority ones are active:

| Source | Target | Condition | Reason |
|--------|--------|-----------|--------|
| `InstanceDown` (critical) | `HighLatency` | same service/env | Instance down causes latency |
| `InstanceDown` (critical) | `HighErrorRate` | same service/env | Instance down causes errors |
| `InstanceDown` (critical) | `DiskSpaceRunningOut` | same node/env | Instance down => no disk I/O |
| Any critical | Any warning | same environment | Critical incidents overshadow warnings |
| Any warning/critical | Info | same service | Warning/critical overshadow info |
| `InstanceDown` | `ConnectionTimeout` | same instance | Down instance = timeout |
| `PodRestarting` | `HighMemoryUsage` | same pod/ns | Restart resets memory metrics |
| `NodeUnreachable` | `NetworkErrors` | same node | Unreachable node causes network errors |

## Setup Instructions

### 1. Create Slack Webhooks

For each channel (#incidents, #alerts, #database-alerts, #infrastructure-alerts, #ops-info):

1. Go to https://api.slack.com/apps
2. Select or create your Slack app
3. Enable **Incoming Webhooks**
4. Click **Add New Webhook to Workspace**
5. Select the channel and authorize
6. Copy the webhook URL

### 2. Set Up PagerDuty Integration

1. Navigate to PagerDuty: https://subdomain.pagerduty.com/integrations
2. Create or select a service
3. Go to **Integrations** tab
4. Find **Events API V2** or **Webhooks** integration
5. Copy the **Integration Key** (Service Key)

### 3. Configure Environment Variables

Copy `.env.example` to `.env` and fill in:

```bash
SLACK_WEBHOOK_CRITICAL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
SLACK_WEBHOOK_ALERTS=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
SLACK_WEBHOOK_DATABASE=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
SLACK_WEBHOOK_INFRASTRUCTURE=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
SLACK_WEBHOOK_OPS_INFO=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

PAGERDUTY_INTEGRATION_KEY=abc123def456ghi789jkl012
```

### 4. Reference in Docker Compose

```yaml
alertmanager:
  image: prom/alertmanager:latest
  volumes:
    - ./infrastructure/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    - ./infrastructure/alertmanager/templates.tmpl:/etc/alertmanager/templates/alerts.tmpl
    - alertmanager-storage:/alertmanager
  env_file:
    - ./infrastructure/alertmanager/.env
  ports:
    - "9093:9093"
  command:
    - '--config.file=/etc/alertmanager/alertmanager.yml'
    - '--storage.path=/alertmanager'
  networks:
    - monitoring
```

## Alert Label Requirements

For proper routing, ensure Prometheus alerts include these labels:

```yaml
labels:
  severity: "critical|warning|info"
  service: "database|infrastructure|api|frontend"
  environment: "production|staging|development"
  cluster: "us-east|eu-west"  # optional
  instance: "hostname:port"
```

And annotations:

```yaml
annotations:
  summary: "Short description"
  description: "Detailed explanation"
  runbook_url: "https://wiki.example.com/runbooks/alert-name"
  dashboard_url: "https://grafana.example.com/d/dashboard-uid"
```

## Alert Examples

### Example 1: Multiple InstanceDown alerts (same service)

**Firing:**
- `InstanceDown` on api-1.prod
- `InstanceDown` on api-2.prod
- `HighLatency` on api-prod
- `HighErrorRate` on api-prod

**Result:**
- ✅ Grouped into 1 notification (same service/environment)
- ✅ Sent to `#incidents` AND PagerDuty simultaneously (0s group_wait)
- ✅ `HighLatency` and `HighErrorRate` suppressed (inhibition rule)
- ✅ Repeat after 4h if still firing

### Example 2: Database warning

**Firing:**
- `PostgreSQL_MaxConnections` 85% (service=database, severity=warning)

**Result:**
- ✅ Routed to `#database-alerts` only
- ✅ Database team notified in isolated channel
- ✅ Repeat after 1h if still firing

### Example 3: Multiple info alerts

**Firing:**
- `CacheHitRateHigh` (info)
- `RequestsIncreasing` (info)

**Result:**
- ✅ Grouped into 1 notification to `#ops-info`
- ✅ 30s wait (allows grouping of multiple info alerts)
- ✅ Repeat after 24h (low urgency, minimal disturbance)

## Troubleshooting

### Alerts not being received

1. **Check Alertmanager UI:** http://alertmanager:9093
2. **Verify webhook URLs:** Test with curl:
   ```bash
   curl -X POST $SLACK_WEBHOOK_ALERTS \
     -H 'Content-Type: application/json' \
     -d '{"text":"Test message"}'
   ```
3. **Check logs:**
   ```bash
   docker logs alertmanager-container-id
   ```

### Duplicate notifications

- Ensure `group_wait` is appropriate (currently 10s)
- Check for multiple receivers triggering on same alert
- Review route `continue` flags (set to prevent cascading)

### Alert fatigue

- Increase `repeat_interval` for specific severity
- Add inhibition rules for related alerts
- Group by more labels (e.g., add `pod` for k8s)
- Consider alert thresholds in Prometheus

### Alerts resolved not showing

- Ensure `send_resolved: true` in receiver config
- Check `resolve_timeout` in global config (5m default)
- Verify alert's `StartsAt` and `EndsAt` timestamps

## Best Practices

1. **Always include runbook and dashboard URLs** in annotations
2. **Use consistent label naming** across all Prometheus rules
3. **Test inhibition rules** to prevent alert storms
4. **Monitor Alertmanager metrics** for delivery failures
5. **Regularly review** grouping effectiveness (low group_wait = fast, high = efficient)
6. **Keep templates consistent** across teams
7. **Document custom routing** for new services
8. **Use environment variable validation** before deployment
9. **Test webhook connectivity** before production
10. **Review repeat intervals** monthly based on incident volume

## Monitoring Alertmanager

Add these Prometheus scrape configs:

```yaml
scrape_configs:
  - job_name: 'alertmanager'
    static_configs:
      - targets: ['alertmanager:9093']
```

Key metrics to monitor:
- `alertmanager_alerts{state="active"}` - Current firing alerts
- `alertmanager_notifications_total` - Total notifications sent
- `alertmanager_notifications_failed_total` - Failed notifications
- `alertmanager_notification_latency_seconds` - Delivery latency
