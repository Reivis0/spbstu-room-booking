# Grafana Provisioning Guide

This document describes the Infrastructure-as-Code (IaC) setup for Grafana provisioning, ensuring dashboards and datasources are deployed as code rather than through manual UI configuration.

## Directory Structure

```
infrastructure/grafana/
├── provisioning/
│   ├── datasources/
│   │   └── datasources.yml        # Datasource configuration (Prometheus)
│   └── dashboards/
│       └── dashboards.yml         # Dashboard provider configuration
├── dashboards/
│   ├── infrastructure-node-use.json      # Node resource utilization (USE method)
│   ├── services-red-method.json          # Service metrics (RED method)
│   ├── availability-engine.json          # Availability Engine performance
│   ├── booking-api.json                  # Booking API service metrics
│   ├── infrastructure-nats.json          # NATS message broker metrics
│   └── infrastructure-postgresql.json    # PostgreSQL database metrics
├── Dockerfile                    # Grafana container definition
└── README_PROVISIONING.md       # This file
```

## Datasource Configuration

### File: `provisioning/datasources/datasources.yml`

**Purpose**: Defines how Grafana connects to Prometheus for metrics collection.

**Key Settings**:
- **Name**: Prometheus (displayed in Grafana UI)
- **Type**: prometheus (native Prometheus datasource)
- **URL**: http://prometheus:9090 (internal Docker network address)
- **Access**: proxy (Grafana server proxies queries to Prometheus)
- **Default**: true (default datasource for new panels)
- **Scrape Interval**: 15s (matches Prometheus scrape interval)
- **Query Timeout**: 60s (maximum time for metric queries)
- **Editable**: false (prevents accidental UI changes via API)

**Example**:
```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
    jsonData:
      timeInterval: 15s
      queryTimeout: 60s
      scrapeInterval: 15s
```

**Adding New Datasources**:
1. Edit `provisioning/datasources/datasources.yml`
2. Add a new datasource block under `datasources:`
3. Save and restart Grafana: `docker-compose restart grafana`
4. Verify in Grafana UI: Configuration → Data Sources

---

## Dashboard Provisioning Configuration

### File: `provisioning/dashboards/dashboards.yml`

**Purpose**: Tells Grafana where to find and how to manage dashboard JSON files.

**Key Settings**:
- **Name**: observability (provider identifier)
- **Type**: file (load dashboards from filesystem)
- **Update Interval**: 10s (check for new/updated dashboard files)
- **Folder**: Observability (Grafana folder where dashboards appear)
- **Path**: /etc/grafana/provisioning/dashboards (mounted volume path)
- **Disableletion**: false (allows deletion via UI)
- **Editable**: true (allows dashboard modifications via UI)
- **AllowUiUpdates**: true (UI changes persist to disk)

**Example**:
```yaml
apiVersion: 1
providers:
  - name: observability
    type: file
    disableDeletion: false
    editable: true
    allowUiUpdates: true
    updateIntervalSeconds: 10
    folder: Observability
    options:
      path: /etc/grafana/provisioning/dashboards
```

---

## Dashboard Files

### Infrastructure Dashboards

#### 1. `infrastructure-node-use.json`
- **Method**: USE (Utilization, Saturation, Errors)
- **Scope**: Host/Node metrics
- **Metrics**:
  - CPU utilization, load average, saturation
  - Memory usage, swaps, pressure
  - Disk I/O, throughput, latency
  - Network packets/bytes, errors, drops
- **Use Case**: Host-level resource monitoring

#### 2. `services-red-method.json`
- **Method**: RED (Rate, Errors, Duration)
- **Scope**: Service-level metrics
- **Metrics**:
  - Request rate (requests/second)
  - Error rate (5xx responses)
  - Request duration (latency percentiles)
- **Use Case**: Application performance monitoring

#### 3. `availability-engine.json`
- **Scope**: C++ Availability Engine service
- **Metrics**:
  - gRPC call metrics (latency, throughput)
  - Booking availability calculations
  - Cache hit rates
  - Thread pool utilization

#### 4. `booking-api.json`
- **Scope**: Java Spring Boot API service
- **Metrics**:
  - HTTP endpoint latency
  - Request throughput
  - Error rates by endpoint
  - JVM memory, GC pauses

#### 5. `infrastructure-nats.json`
- **Scope**: NATS JetStream message broker
- **Metrics**:
  - Message publish rate
  - Subscription count
  - Storage usage
  - Connection health

#### 6. `infrastructure-postgresql.json`
- **Scope**: PostgreSQL database
- **Metrics**:
  - Connection pool usage
  - Query latency
  - Index scans vs seq scans
  - Transaction rates
  - Replication lag

---

## Adding New Dashboards

### Process

1. **Create Dashboard in Grafana UI**:
   - Log in to Grafana (http://localhost:3001, admin/admin)
   - Create new dashboard with desired panels
   - Configure variables and templating

2. **Export Dashboard JSON**:
   - Dashboard menu → Share → Export (JSON Model)
   - Save to `infrastructure/grafana/dashboards/my-dashboard.json`

3. **Enable Provisioning**:
   - Dashboard automatically appears in "Observability" folder
   - Changes persist if `allowUiUpdates: true`

4. **Git Workflow**:
   ```bash
   git add infrastructure/grafana/dashboards/my-dashboard.json
   git commit -m "Add my-dashboard for monitoring feature X"
   git push origin feature/monitoring
   ```

### Dashboard JSON Best Practices

```json
{
  "dashboard": {
    "title": "My Dashboard",
    "description": "Clear description of dashboard purpose",
    "tags": ["service-name", "monitoring"],
    "timezone": "UTC",
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "templating": {
      "list": [
        {
          "name": "environment",
          "type": "query",
          "datasource": "Prometheus",
          "query": "label_values(up, environment)"
        },
        {
          "name": "service",
          "type": "query",
          "datasource": "Prometheus",
          "query": "label_values(up{environment=\"$environment\"}, job)"
        }
      ]
    }
  }
}
```

---

## Updating Datasources

### Via Provisioning File

1. Edit `provisioning/datasources/datasources.yml`
2. Add/modify datasource configuration
3. Restart Grafana: `docker-compose restart grafana`

### Examples

**Adding Prometheus with authentication**:
```yaml
datasources:
  - name: Prometheus Prod
    type: prometheus
    url: https://prometheus.prod.internal
    access: proxy
    isDefault: false
    secureJsonData:
      basicAuthPassword: ${PROMETHEUS_PASSWORD}
    jsonData:
      basicAuthUser: prometheus
      timeInterval: 30s
```

**Adding Loki for logs**:
```yaml
datasources:
  - name: Loki
    type: loki
    url: http://loki:3100
    access: proxy
    isDefault: false
```

---

## Grafana API for Dashboard Management

### Programmatic Dashboard Upload

```bash
# Export current dashboard
DASHBOARD_JSON=$(curl -s http://localhost:3001/api/dashboards/db/infrastructure-node-use)

# Create new dashboard from JSON
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GRAFANA_API_TOKEN}" \
  -d '{
    "dashboard": '"${DASHBOARD_JSON}"',
    "overwrite": true
  }' \
  http://localhost:3001/api/dashboards/db
```

### Provisioning via API Token

1. Generate API token in Grafana (Configuration → API Tokens)
2. Export `GRAFANA_API_TOKEN="glc_xxx"`
3. Use in scripts for automation

### Useful Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/datasources` | GET | List all datasources |
| `/api/datasources` | POST | Create datasource |
| `/api/dashboards/db` | GET | Get dashboard by slug |
| `/api/dashboards/db` | POST | Create/update dashboard |
| `/api/dashboards/uid/:uid` | DELETE | Delete dashboard |
| `/api/provisioning/dashboards/reload` | POST | Reload provisioned dashboards |
| `/api/provisioning/datasources/reload` | POST | Reload provisioned datasources |

---

## Git Workflow for Dashboard Updates

### Local Development

```bash
# 1. Start development environment
make up

# 2. Access Grafana
open http://localhost:3001

# 3. Modify dashboards in UI
# 4. Export updated dashboards
curl http://localhost:3001/api/dashboards/db/my-dashboard | jq '.dashboard' > \
  infrastructure/grafana/dashboards/my-dashboard.json

# 5. Commit changes
git add infrastructure/grafana/dashboards/my-dashboard.json
git commit -m "Update my-dashboard with improved visualizations"

# 6. Push to feature branch
git push origin feature/dashboard-improvements
```

### CI/CD Integration

Add to `.github/workflows/deploy.yml`:
```yaml
- name: Validate Dashboard JSON
  run: |
    for file in infrastructure/grafana/dashboards/*.json; do
      jq empty "$file" || exit 1
    done

- name: Deploy Dashboards
  run: |
    docker-compose restart grafana
    sleep 10
    curl -f http://localhost:3001/api/datasources || exit 1
```

---

## Validation Checklist

- ✅ **Prometheus datasource configured**: Verify in Grafana UI
  ```bash
  curl http://localhost:3001/api/datasources | jq '.[] | {name, url}'
  ```

- ✅ **Dashboards load on startup**: Check dashboard folder
  ```bash
  curl http://localhost:3001/api/dashboards/db/infrastructure-node-use
  ```

- ✅ **Dashboard JSON validation**: Verify all files are valid JSON
  ```bash
  find infrastructure/grafana/dashboards -name "*.json" -exec jq empty {} \;
  ```

- ✅ **Variables properly scoped**: Check templating in dashboard JSON
  - `$environment`, `$service`, `$instance` variables available
  - Label query supports dynamic filtering

- ✅ **Provisioning config syntax**: Validate YAML files
  ```bash
  yamllint infrastructure/grafana/provisioning/
  ```

---

## Troubleshooting

### Dashboards Not Loading

**Symptoms**: Dashboard folder empty, provisioned dashboards missing

**Solutions**:
1. Check Grafana logs: `docker-compose logs grafana | tail -50`
2. Verify file permissions: `ls -la infrastructure/grafana/dashboards/`
3. Check volume mounts: `docker-compose ps` → verify volumes
4. Reload provisioning: `curl -X POST http://localhost:3001/api/provisioning/dashboards/reload`

### Datasource Errors

**Symptoms**: "No data" in panels, datasource connection failed

**Solutions**:
1. Verify Prometheus is running: `docker-compose ps prometheus`
2. Test connection: `curl http://prometheus:9090/-/healthy`
3. Check datasource config: `curl http://localhost:3001/api/datasources`
4. Review YAML syntax in `provisioning/datasources/datasources.yml`

### JSON Format Errors

**Symptoms**: Dashboard doesn't load, JSON validation errors

**Solutions**:
```bash
# Validate all dashboards
for file in infrastructure/grafana/dashboards/*.json; do
  echo "Checking $file..."
  jq . "$file" > /dev/null || echo "ERROR: $file is invalid"
done

# Pretty-print to diagnose
jq . infrastructure/grafana/dashboards/my-dashboard.json
```

---

## References

- [Grafana Provisioning Documentation](https://grafana.com/docs/grafana/latest/administration/provisioning/)
- [Grafana Datasource Configuration](https://grafana.com/docs/grafana/latest/datasources/prometheus/)
- [Dashboard JSON Model](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/manage-dashboards/#dashboard-json-model)
- [Grafana API Documentation](https://grafana.com/docs/grafana/latest/developers/http_api/)

---

## Related Files

- **Docker Compose**: `docker-compose.yml` (Grafana service definition)
- **Prometheus Config**: `infrastructure/prometheus/prometheus.yml`
- **Environment Variables**: `.env.example`
- **Dockerfile**: `infrastructure/grafana/Dockerfile`

---

**Last Updated**: 2024
**Maintained by**: DevOps Team
