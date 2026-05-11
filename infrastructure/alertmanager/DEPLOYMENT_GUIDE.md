# Alertmanager Deployment Guide

## Files Created

```
infrastructure/alertmanager/
├── alertmanager.yml           # Main configuration file (9.7 KB)
├── .env.example               # Environment variable template (3.9 KB)
├── templates.tmpl             # Message formatting templates (1.4 KB)
├── README.md                  # Setup instructions & troubleshooting (9.4 KB)
├── ARCHITECTURE_SUMMARY.md    # Visual diagrams & examples (9.6 KB)
└── DEPLOYMENT_GUIDE.md        # This file
```

## ✅ Configuration Features

### 1. Severity-Based Routing ✓
- **Critical** → PagerDuty webhook + Slack #incidents (0s group_wait, 4h repeat)
- **Warning** → Slack #alerts (10s group_wait, 1h repeat)
- **Info** → Slack #ops-info (30s group_wait, 24h repeat)

### 2. Service-Aware Routing ✓
- **Database alerts** → Slack #database-alerts
- **Infrastructure alerts** → Slack #infrastructure-alerts
- Critical overrides get both PagerDuty + service channel

### 3. Smart Grouping ✓
- Group by: `environment`, `service`, `severity`, `alertname`
- Multiple InstanceDown alerts → 1 grouped notification
- Related errors grouped together
- Wait 10s to collect related alerts before sending

### 4. Inhibition Rules ✓
- 8 inhibition rules preventing alert fatigue
- InstanceDown suppresses downstream errors
- Node failure suppresses network alerts
- Pod restart suppresses memory alerts
- Critical severity suppresses warnings

### 5. Environment Variables ✓
- Slack webhook URLs (critical, alerts, database, infrastructure, info)
- PagerDuty integration key
- All sensitive values externalized

### 6. Rich Formatting ✓
- Alert templates for consistent formatting
- Includes summary, description, runbook, dashboard links
- Emoji indicators (🚨 critical, ⚠️ warning, ℹ️ info)
- Color coding (red/danger for critical, orange for warning, green for info)

---

## 🚀 Integration Steps

### Step 1: Copy Template to Environment File
```bash
cd infrastructure/alertmanager
cp .env.example .env
```

### Step 2: Create Slack Webhooks

For each channel, follow this process:

1. Go to https://api.slack.com/apps
2. Create new app or select existing
3. Enable "Incoming Webhooks"
4. Click "Add New Webhook to Workspace"
5. Select channel:
   - `#incidents` → `SLACK_WEBHOOK_CRITICAL`
   - `#alerts` → `SLACK_WEBHOOK_ALERTS`
   - `#database-alerts` → `SLACK_WEBHOOK_DATABASE`
   - `#infrastructure-alerts` → `SLACK_WEBHOOK_INFRASTRUCTURE`
   - `#ops-info` → `SLACK_WEBHOOK_OPS_INFO`
6. Copy webhook URL to `.env`

### Step 3: Set Up PagerDuty Integration

1. Navigate to https://your-account.pagerduty.com/integrations
2. Create or select a service
3. Go to "Integrations" tab
4. Find "Events API V2" integration
5. Copy the **Integration Key** to `.env` as `PAGERDUTY_INTEGRATION_KEY`

### Step 4: Populate .env File
```bash
# Edit the file with your values
nano .env

# Verify all values are set
grep -v "^#" .env | grep -v "^$"
```

Example `.env` contents:
```
SLACK_WEBHOOK_CRITICAL=https://hooks.slack.com/services/T00/B00/XXXX
SLACK_WEBHOOK_ALERTS=https://hooks.slack.com/services/T00/B00/YYYY
SLACK_WEBHOOK_DATABASE=https://hooks.slack.com/services/T00/B00/ZZZZ
SLACK_WEBHOOK_INFRASTRUCTURE=https://hooks.slack.com/services/T00/B00/WWWW
SLACK_WEBHOOK_OPS_INFO=https://hooks.slack.com/services/T00/B00/VVVV
PAGERDUTY_INTEGRATION_KEY=abc123def456ghi789jkl012
```

### Step 5: Add to Docker Compose

Update your `docker-compose.yml`:

```yaml
version: '3.8'

services:
  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: alertmanager
    volumes:
      - ./infrastructure/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
      - ./infrastructure/alertmanager/templates.tmpl:/etc/alertmanager/templates/alerts.tmpl:ro
      - alertmanager-storage:/alertmanager
    env_file:
      - ./infrastructure/alertmanager/.env
    ports:
      - "9093:9093"
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
      - '--log.level=info'
    restart: unless-stopped
    networks:
      - monitoring
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:9093/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  alertmanager-storage:
    driver: local

networks:
  monitoring:
    driver: bridge
```

### Step 6: Connect Prometheus

Update Prometheus configuration (`prometheus.yml` or equivalent):

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

rule_files:
  - '/etc/prometheus/rules/*.yml'
```

---

## 🧪 Testing & Validation

### Test 1: Verify Alertmanager Starts
```bash
docker-compose up -d alertmanager
docker ps | grep alertmanager
docker logs alertmanager
```

Expected output:
```
alertmanager started at 127.0.0.1:9093
listening at 127.0.0.1:9093
```

### Test 2: Check Health Endpoint
```bash
curl http://localhost:9093/-/healthy
```

Expected response: HTTP 200

### Test 3: Test Webhook Connectivity
```bash
# Test Slack webhook
curl -X POST $SLACK_WEBHOOK_CRITICAL \
  -H 'Content-Type: application/json' \
  -d '{"text":"✅ Webhook test successful"}'

# Verify message appears in #incidents channel
```

### Test 4: Send Test Alert
```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {
      "alertname": "TestAlert",
      "severity": "critical",
      "service": "api",
      "environment": "production"
    },
    "annotations": {
      "summary": "This is a test critical alert",
      "description": "Testing Alertmanager routing to #incidents",
      "runbook_url": "https://example.com/runbooks/test-alert",
      "dashboard_url": "https://grafana.example.com/d/test"
    }
  }]'
```

Expected behavior:
- ✅ Alert appears in Alertmanager UI (http://localhost:9093)
- ✅ Message in Slack #incidents channel
- ✅ Incident created in PagerDuty (check dashboard)
- ✅ Color-coded red (danger)
- ✅ Includes all annotations (summary, description, links)

### Test 5: Test Grouping (Multiple Alerts)
```bash
# Send 2 InstanceDown alerts for same service
for i in {1..2}; do
  curl -X POST http://localhost:9093/api/v1/alerts \
    -H 'Content-Type: application/json' \
    -d '[{
      "labels": {
        "alertname": "InstanceDown",
        "severity": "critical",
        "service": "api",
        "environment": "production",
        "instance": "api-'$i'.prod:9090"
      },
      "annotations": {
        "summary": "Instance api-'$i' is down",
        "description": "Cannot reach api-'$i'.prod:9090"
      }
    }]'
  sleep 0.5
done
```

Expected behavior:
- ✅ Both alerts grouped into 1 Slack message
- ✅ Shows 2 firing alerts in group
- ✅ Single PagerDuty incident (updated)

### Test 6: Test Inhibition Rules
```bash
# Send InstanceDown alert
curl -X POST http://localhost:9093/api/v1/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {
      "alertname": "InstanceDown",
      "severity": "critical",
      "service": "api",
      "environment": "production",
      "instance": "api-1.prod:9090"
    },
    "annotations": {"summary": "Instance down"}
  }]'

# Wait 2 seconds, then send HighLatency (should be suppressed)
sleep 2

curl -X POST http://localhost:9093/api/v1/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {
      "alertname": "HighLatency",
      "severity": "warning",
      "service": "api",
      "environment": "production",
      "instance": "api-1.prod:9090"
    },
    "annotations": {"summary": "High latency"}
  }]'
```

Expected behavior:
- ✅ InstanceDown shows in Slack
- ✅ HighLatency NOT sent to Slack (inhibited)
- ✅ Check Alertmanager UI: shows both alerts but HighLatency marked as suppressed

### Test 7: Test Info Alerts (Low Priority)
```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {
      "alertname": "CacheHitRateImproving",
      "severity": "info",
      "service": "api",
      "environment": "production"
    },
    "annotations": {"summary": "Cache efficiency improved"}
  }]'
```

Expected behavior:
- ✅ Message in Slack #ops-info (not #incidents)
- ✅ Green color (good)
- ✅ Will repeat after 24h (not 4h)

---

## 📊 Monitoring Alertmanager

Add Prometheus scrape job to collect Alertmanager metrics:

```yaml
scrape_configs:
  - job_name: 'alertmanager'
    static_configs:
      - targets: ['alertmanager:9093']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'alertmanager'
```

Key metrics to monitor:
```
alertmanager_alerts{state="active"}                      # Current firing alerts
alertmanager_alerts_received_total                       # Total alerts received
alertmanager_notifications_total{integration="slack"}   # Notifications sent
alertmanager_notifications_failed_total                  # Failed notifications
alertmanager_notification_latency_seconds               # Delivery latency
alertmanager_config_last_reload_successful              # Config valid (1=yes, 0=no)
```

---

## 🔧 Troubleshooting

### Issue: Alerts not reaching Slack

**Check 1: Alertmanager logs**
```bash
docker logs alertmanager | grep -i slack
```

**Check 2: Webhook URL validity**
```bash
curl -v -X POST $SLACK_WEBHOOK_CRITICAL -H 'Content-Type: application/json' -d '{"text":"test"}'
```

**Check 3: Alertmanager configuration**
```bash
curl http://localhost:9093/api/v1/alerts
```

### Issue: Alerts reaching wrong channel

**Check:** Routing rules in alertmanager.yml
- Verify alert labels match route matchers
- Check `continue` flag behavior
- Review route order (first match wins)

### Issue: Alerts not grouped

**Check:** Grouping configuration
- Verify `group_by` labels match alert labels
- Check `group_wait` (10s default)
- Review `/api/v1/alerts` to see alert labels

### Issue: Inhibition rules not working

**Check:** Inhibition rule configuration
- Verify source/target matchers syntax
- Check `equal` labels are correct
- Ensure source alert has `severity: critical`

---

## 📝 Maintenance Checklist

**Weekly:**
- [ ] Review Slack notification volume
- [ ] Check for alert fatigue patterns
- [ ] Verify PagerDuty incident response time

**Monthly:**
- [ ] Review repeat intervals effectiveness
- [ ] Analyze inhibition rule performance
- [ ] Update runbook links if needed

**Quarterly:**
- [ ] Audit receiver connectivity
- [ ] Review grouping strategy
- [ ] Update alert routing based on incidents

**Annually:**
- [ ] Review and optimize all grouping rules
- [ ] Audit webhook endpoints
- [ ] Plan for Alertmanager version upgrades

---

## 📚 Additional Resources

- [Alertmanager Documentation](https://prometheus.io/docs/alerting/latest/overview/)
- [Routing Configuration](https://prometheus.io/docs/alerting/latest/configuration/#route)
- [Inhibition Rules](https://prometheus.io/docs/alerting/latest/configuration/#inhibit_rule)
- [Slack Integration](https://prometheus.io/docs/alerting/latest/configuration/#slack_config)
- [PagerDuty Integration](https://prometheus.io/docs/alerting/latest/configuration/#pagerduty_config)
