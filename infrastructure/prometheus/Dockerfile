FROM prom/prometheus:v2.50.1

COPY prometheus.yml /etc/prometheus/prometheus.yml
COPY recording_rules.yml /etc/prometheus/recording_rules.yml

USER nobody
