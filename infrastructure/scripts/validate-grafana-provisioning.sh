#!/bin/bash
# Grafana Provisioning Validation Script
# Validates IaC configuration for Grafana provisioning

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GRAFANA_DIR="$PROJECT_ROOT/infrastructure/grafana"

PASS=0
FAIL=0

print_header() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  $1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

print_pass() {
    echo "  ✓ $1"
    ((PASS++))
}

print_fail() {
    echo "  ✗ $1"
    ((FAIL++))
}

# Check provisioning directory structure
print_header "Checking Directory Structure"

if [ -d "$GRAFANA_DIR/provisioning/datasources" ]; then
    print_pass "Datasources directory exists"
else
    print_fail "Datasources directory missing"
fi

if [ -d "$GRAFANA_DIR/provisioning/dashboards" ]; then
    print_pass "Dashboards directory exists"
else
    print_fail "Dashboards directory missing"
fi

if [ -d "$GRAFANA_DIR/dashboards" ]; then
    print_pass "Dashboards folder exists"
else
    print_fail "Dashboards folder missing"
fi

# Check provisioning configuration files
print_header "Validating Provisioning Configuration Files"

if [ -f "$GRAFANA_DIR/provisioning/datasources/datasources.yml" ]; then
    print_pass "datasources.yml exists"
    
    # Check for required fields
    if grep -q "apiVersion: 1" "$GRAFANA_DIR/provisioning/datasources/datasources.yml"; then
        print_pass "datasources.yml has apiVersion: 1"
    else
        print_fail "datasources.yml missing apiVersion"
    fi
    
    if grep -q "name: Prometheus" "$GRAFANA_DIR/provisioning/datasources/datasources.yml"; then
        print_pass "Prometheus datasource configured"
    else
        print_fail "Prometheus datasource not configured"
    fi
    
    if grep -q "url: http://prometheus:9090" "$GRAFANA_DIR/provisioning/datasources/datasources.yml"; then
        print_pass "Prometheus URL configured correctly"
    else
        print_fail "Prometheus URL not configured correctly"
    fi
    
    if grep -q "isDefault: true" "$GRAFANA_DIR/provisioning/datasources/datasources.yml"; then
        print_pass "Prometheus set as default datasource"
    else
        print_fail "Prometheus not set as default datasource"
    fi
    
    if grep -q "queryTimeout: 60s" "$GRAFANA_DIR/provisioning/datasources/datasources.yml"; then
        print_pass "Query timeout configured (60s)"
    else
        print_fail "Query timeout not configured"
    fi
else
    print_fail "datasources.yml missing"
fi

if [ -f "$GRAFANA_DIR/provisioning/dashboards/dashboards.yml" ]; then
    print_pass "dashboards.yml exists"
    
    if grep -q "apiVersion: 1" "$GRAFANA_DIR/provisioning/dashboards/dashboards.yml"; then
        print_pass "dashboards.yml has apiVersion: 1"
    else
        print_fail "dashboards.yml missing apiVersion"
    fi
    
    if grep -q "folder: Observability" "$GRAFANA_DIR/provisioning/dashboards/dashboards.yml"; then
        print_pass "Dashboard folder set to 'Observability'"
    else
        print_fail "Dashboard folder not set correctly"
    fi
    
    if grep -q "updateIntervalSeconds: 10" "$GRAFANA_DIR/provisioning/dashboards/dashboards.yml"; then
        print_pass "Update interval set to 10s"
    else
        print_fail "Update interval not set correctly"
    fi
    
    if grep -q "allowUiUpdates: true" "$GRAFANA_DIR/provisioning/dashboards/dashboards.yml"; then
        print_pass "UI updates enabled for dashboards"
    else
        print_fail "UI updates not enabled"
    fi
else
    print_fail "dashboards.yml missing"
fi

# Check dashboard JSON files
print_header "Validating Dashboard JSON Files"

REQUIRED_DASHBOARDS=(
    "infrastructure-node-use.json"
    "services-red-method.json"
    "availability-engine.json"
    "booking-api.json"
    "infrastructure-nats.json"
    "infrastructure-postgresql.json"
)

for dashboard in "${REQUIRED_DASHBOARDS[@]}"; do
    DASHBOARD_FILE="$GRAFANA_DIR/dashboards/$dashboard"
    
    if [ -f "$DASHBOARD_FILE" ]; then
        if jq empty "$DASHBOARD_FILE" 2>/dev/null; then
            print_pass "$dashboard exists and is valid JSON"
        else
            print_fail "$dashboard is invalid JSON"
        fi
    else
        print_fail "$dashboard missing"
    fi
done

# Check docker-compose configuration
print_header "Validating docker-compose.yml Configuration"

if grep -q "image: grafana/grafana:10.4.1" "$PROJECT_ROOT/docker-compose.yml"; then
    print_pass "Grafana image version 10.4.1 specified"
else
    print_fail "Grafana image version mismatch"
fi

if grep -q "GF_PATHS_PROVISIONING=/etc/grafana/provisioning" "$PROJECT_ROOT/docker-compose.yml"; then
    print_pass "GF_PATHS_PROVISIONING environment variable set"
else
    print_fail "GF_PATHS_PROVISIONING not configured"
fi

if grep -q "GF_SECURITY_ADMIN_PASSWORD=admin" "$PROJECT_ROOT/docker-compose.yml"; then
    print_pass "Admin password environment variable set"
else
    print_fail "Admin password not configured"
fi

if grep -q "./infrastructure/grafana/provisioning:/etc/grafana/provisioning:ro" "$PROJECT_ROOT/docker-compose.yml"; then
    print_pass "Provisioning volume mounted correctly"
else
    print_fail "Provisioning volume not mounted"
fi

if grep -q "./infrastructure/grafana/dashboards:/etc/grafana/provisioning/dashboards:ro" "$PROJECT_ROOT/docker-compose.yml"; then
    print_pass "Dashboards volume mounted correctly"
else
    print_fail "Dashboards volume not mounted"
fi

# Check documentation
print_header "Checking Documentation"

if [ -f "$GRAFANA_DIR/README_PROVISIONING.md" ]; then
    print_pass "Provisioning documentation exists"
    
    if grep -q "Adding New Dashboards" "$GRAFANA_DIR/README_PROVISIONING.md"; then
        print_pass "Documentation includes dashboard addition guide"
    else
        print_fail "Documentation missing dashboard guide"
    fi
    
    if grep -q "Updating Datasources" "$GRAFANA_DIR/README_PROVISIONING.md"; then
        print_pass "Documentation includes datasource update guide"
    else
        print_fail "Documentation missing datasource guide"
    fi
    
    if grep -q "Grafana API" "$GRAFANA_DIR/README_PROVISIONING.md"; then
        print_pass "Documentation includes API examples"
    else
        print_fail "Documentation missing API examples"
    fi
else
    print_fail "Provisioning documentation missing"
fi

# Summary
print_header "Validation Summary"

echo ""
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
echo ""

if [ $FAIL -eq 0 ]; then
    echo "  ✓ All validations passed! Grafana provisioning IaC is properly configured."
    exit 0
else
    echo "  ✗ Some validations failed. Please review the output above."
    exit 1
fi
