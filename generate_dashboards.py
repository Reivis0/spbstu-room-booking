import json

def get_base_dashboard(title):
    return {
        "annotations": {"list": [{"builtIn": 1, "datasource": {"type": "grafana", "uid": "-- Grafana --"}, "enable": True, "hide": True, "name": "Annotations & Alerts", "type": "dashboard"}]},
        "editable": True,
        "fiscalYearStartMonth": 0,
        "graphTooltip": 0,
        "links": [],
        "liveNow": False,
        "panels": [],
        "schemaVersion": 39,
        "tags": [],
        "templating": {"list": []},
        "time": {"from": "now-1h", "to": "now"},
        "title": title
    }

def create_timeseries_panel(id, title, expr, gridPos, unit=None):
    panel = {
        "datasource": {"type": "prometheus", "uid": "Prometheus"},
        "fieldConfig": {
            "defaults": {
                "color": {"mode": "palette-classic"},
                "custom": {"drawStyle": "line", "fillOpacity": 10, "lineWidth": 1},
                "thresholds": {"mode": "absolute", "steps": [{"color": "green", "value": None}]}
            },
            "overrides": []
        },
        "gridPos": gridPos,
        "id": id,
        "options": {"legend": {"displayMode": "list", "placement": "bottom", "showLegend": True}, "tooltip": {"mode": "single", "sort": "none"}},
        "targets": [{"datasource": {"type": "prometheus", "uid": "Prometheus"}, "expr": expr, "refId": "A"}],
        "title": title,
        "type": "timeseries"
    }
    if unit:
        panel["fieldConfig"]["defaults"]["unit"] = unit
    return panel

# NATS Dashboard
nats = get_base_dashboard("NATS Infrastructure")
nats["panels"].append(create_timeseries_panel(1, "Server CPU", "nats_server_cpu", {"h": 8, "w": 12, "x": 0, "y": 0}, "percentunit"))
nats["panels"].append(create_timeseries_panel(2, "Server Memory", "nats_server_mem", {"h": 8, "w": 12, "x": 12, "y": 0}, "bytes"))
nats["panels"].append(create_timeseries_panel(3, "In Messages Rate", "rate(nats_server_in_msgs[1m])", {"h": 8, "w": 12, "x": 0, "y": 8}, "pps"))
nats["panels"].append(create_timeseries_panel(4, "Out Messages Rate", "rate(nats_server_out_msgs[1m])", {"h": 8, "w": 12, "x": 12, "y": 8}, "pps"))

with open('infrastructure/grafana/dashboards/infrastructure-nats.json', 'w') as f:
    json.dump(nats, f, indent=2)

# PostgreSQL Dashboard
pg = get_base_dashboard("PostgreSQL Infrastructure")
pg["panels"].append(create_timeseries_panel(1, "Active Connections", "pg_stat_activity_count", {"h": 8, "w": 12, "x": 0, "y": 0}))
pg["panels"].append(create_timeseries_panel(2, "Database Size", "pg_database_size_bytes", {"h": 8, "w": 12, "x": 12, "y": 0}, "bytes"))
pg["panels"].append(create_timeseries_panel(3, "Transactions Rate", "sum(rate(pg_stat_database_xact_commit[1m])) + sum(rate(pg_stat_database_xact_rollback[1m]))", {"h": 8, "w": 12, "x": 0, "y": 8}, "tps"))
pg["panels"].append(create_timeseries_panel(4, "Cache Hit Ratio", "sum(rate(pg_stat_database_blks_hit[1m])) / (sum(rate(pg_stat_database_blks_hit[1m])) + sum(rate(pg_stat_database_blks_read[1m]))) * 100", {"h": 8, "w": 12, "x": 12, "y": 8}, "percent"))

with open('infrastructure/grafana/dashboards/infrastructure-postgresql.json', 'w') as f:
    json.dump(pg, f, indent=2)

# Availability Engine
ae = get_base_dashboard("Availability Engine")
ae["panels"].append(create_timeseries_panel(1, "NATS processed rate", "rate(nats_messages_processed_total[1m])", {"h": 8, "w": 12, "x": 0, "y": 0}))
ae["panels"].append(create_timeseries_panel(2, "NATS error rate", "rate(nats_messages_failed_total[1m])", {"h": 8, "w": 12, "x": 12, "y": 0}))
ae["panels"].append(create_timeseries_panel(3, "Cache hit rate", "rate(cache_hits_total[5m]) / (rate(cache_hits_total[5m]) + rate(cache_misses_total[5m])) * 100", {"h": 8, "w": 12, "x": 0, "y": 8}, "percent"))

with open('infrastructure/grafana/dashboards/availability-engine.json', 'w') as f:
    json.dump(ae, f, indent=2)

# Booking API
ba = get_base_dashboard("Booking API")
ba["panels"].append(create_timeseries_panel(1, "RPS", "sum(rate(http_server_requests_seconds_count{application=\"booking-api\"}[1m]))", {"h": 8, "w": 12, "x": 0, "y": 0}))
ba["panels"].append(create_timeseries_panel(2, "Error rate 5xx", "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[1m])) / sum(rate(http_server_requests_seconds_count[1m])) * 100", {"h": 8, "w": 12, "x": 12, "y": 0}, "percent"))
ba["panels"].append(create_timeseries_panel(3, "JVM Heap", "jvm_memory_used_bytes{area=\"heap\", application=\"booking-api\"}", {"h": 8, "w": 12, "x": 0, "y": 8}, "bytes"))

with open('infrastructure/grafana/dashboards/booking-api.json', 'w') as f:
    json.dump(ba, f, indent=2)
