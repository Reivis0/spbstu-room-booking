FROM grafana/grafana:10.4.1

# Copy provisioning configurations
COPY provisioning/ /etc/grafana/provisioning/

# Copy default dashboards
COPY dashboards/ /var/lib/grafana/dashboards/

# Ensure proper permissions
USER root
RUN chown -R 472:0 /var/lib/grafana/dashboards
USER 472
