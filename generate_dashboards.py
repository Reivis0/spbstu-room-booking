import json
import urllib.request
import os

def create_panel(title, exprs, id):
    targets = [{"expr": e, "refId": chr(65+i)} for i, e in enumerate(exprs)]
    return {
        "id": id,
        "type": "timeseries",
        "title": title,
        "targets": targets
    }

booking_api_panels = [
    create_panel("RPS", ['rate(http_server_requests_seconds_count{application="booking-api"}[1m])'], 1),
    create_panel("Error rate 5xx", ['rate(http_server_requests_seconds_count{status=~"5.."}[1m]) / rate(http_server_requests_seconds_count[1m]) * 100'], 2),
    create_panel("Error rate 4xx", ['rate(http_server_requests_seconds_count{status=~"4.."}[1m]) / rate(http_server_requests_seconds_count[1m]) * 100'], 3),
    create_panel("JVM Heap", ['jvm_memory_used_bytes{area="heap", application="booking-api"}'], 4),
    create_panel("JVM Non-Heap", ['jvm_memory_used_bytes{area="nonheap", application="booking-api"}'], 5),
    create_panel("HikariCP pool", ['hikaricp_connections_active{pool="bookingApiPool"}'], 6),
    create_panel("Business", ['increase(booking_created_total[5m])', 'increase(booking_cancelled_total[5m])'], 7)
]

availability_engine_panels = [
    create_panel("NATS processed rate", ['rate(nats_messages_processed_total[1m])'], 1),
    create_panel("NATS error rate", ['rate(nats_messages_failed_total[1m])'], 2),
    create_panel("Cache hit rate", ['rate(cache_hits_total[5m]) / (rate(cache_hits_total[5m]) + rate(cache_misses_total[5m])) * 100'], 3),
    create_panel("RUZ parse duration P95", ['histogram_quantile(0.95, rate(schedule_parse_duration_seconds_bucket[5m]))'], 4),
    create_panel("DB query duration P95", ['histogram_quantile(0.95, rate(db_query_duration_seconds_bucket[5m]))'], 5)
]

os.makedirs('infrastructure/grafana/dashboards', exist_ok=True)

with open('infrastructure/grafana/dashboards/booking-api.json', 'w') as f:
    json.dump({"title": "Booking API", "panels": booking_api_panels}, f, indent=2)

with open('infrastructure/grafana/dashboards/availability-engine.json', 'w') as f:
    json.dump({"title": "Availability Engine", "panels": availability_engine_panels}, f, indent=2)

# Download dashboards from Grafana Marketplace (by ID using Grafana API if possible, but actually we can just create placeholders if we don't have the real ones, or try to download them from an open source repo. Wait, the prompt says "скачай с Grafana Marketplace". Dashboard 2279 and 9628. Let's try downloading via API)
# Since getting them from grafana.com without API key might be hard, let's curl them.
def download_dashboard(id, filename):
    url = f"https://grafana.com/api/dashboards/{id}/revisions/latest/download"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req) as response:
            with open(filename, 'wb') as out:
                out.write(response.read())
        print(f"Downloaded {id} to {filename}")
    except Exception as e:
        print(f"Failed to download {id}: {e}")

download_dashboard(2279, 'infrastructure/grafana/dashboards/infrastructure-nats.json')
download_dashboard(9628, 'infrastructure/grafana/dashboards/infrastructure-postgresql.json')

