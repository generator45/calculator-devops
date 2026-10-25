#!/usr/bin/env bash
# Smoke-test a running calculator deployment. Mirrors the Jenkins "Verify Health" stage.
#
#   ./scripts/smoke-test.sh [base-url]        # default http://localhost:8080
set -euo pipefail

BASE="${1:-http://localhost:8080}"
failures=0

check_json() {
    local label="$1" url="$2" expected="$3"
    local body
    body=$(curl -fsS "$url")
    if printf '%s' "$body" | grep -q "$expected"; then
        printf '  PASS  %-28s %s\n' "$label" "$body"
    else
        printf '  FAIL  %-28s %s (expected to contain %s)\n' "$label" "$body" "$expected"
        failures=$((failures + 1))
    fi
}

check_status() {
    local label="$1" url="$2" expected="$3"
    local code body
    body=$(curl -s -o /tmp/smoke-body -w '%{http_code}' "$url")
    code="$body"
    if [ "$code" = "$expected" ]; then
        printf '  PASS  %-28s HTTP %s %s\n' "$label" "$code" "$(cat /tmp/smoke-body)"
    else
        printf '  FAIL  %-28s HTTP %s (expected %s)\n' "$label" "$code" "$expected"
        failures=$((failures + 1))
    fi
}

echo "Smoke-testing ${BASE}"

echo "Health:"
check_json "actuator/health"       "${BASE}/actuator/health"                '"status":"UP"'

echo "Operations:"
check_json "add 2+3"               "${BASE}/api/v1/add?a=2&b=3"             '"result":5.0'
check_json "subtract 9-4.5"        "${BASE}/api/v1/subtract?a=9&b=4.5"      '"result":4.5'
check_json "multiply 6*7"          "${BASE}/api/v1/multiply?a=6&b=7"        '"result":42.0'
check_json "divide 10/4"           "${BASE}/api/v1/divide?a=10&b=4"         '"result":2.5'

echo "Error handling (expect HTTP 400):"
check_status "divide by zero"      "${BASE}/api/v1/divide?a=10&b=0"         400
check_status "missing parameter"   "${BASE}/api/v1/add?a=2"                 400
check_status "non-numeric input"   "${BASE}/api/v1/multiply?a=abc&b=2"      400

echo
if [ "$failures" -eq 0 ]; then
    echo "All smoke tests passed."
else
    echo "${failures} smoke test(s) failed."
    exit 1
fi
