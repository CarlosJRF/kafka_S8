#!/usr/bin/env bash
# Shows the Resilience4j circuit breaker of resilient-client going
# CLOSED -> OPEN -> HALF_OPEN -> CLOSED while data-processor-service is stopped and restarted.
# Run on the Docker host after scripts/demo.sh (so there is a cached answer to fall back to).
set -euo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:8080}"
ACTUATOR="${RESILIENT_CLIENT_URL:-http://localhost:8083}/actuator"

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
cb_state() { curl -s "$ACTUATOR/circuitbreakers" | sed -E 's/.*"state":"([A-Z_]+)".*/\1/'; }
state() { echo "circuit breaker: $(cb_state)"; }
call() {
  curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY/api/client/stats" \
    | sed -E 's/.*"source":"([a-z]+)".*"error":(null|"[^"]{0,80}).*/source=\1 error=\2/'
  echo
}

TOKEN=$(curl -sf -u "${BANKING_CLIENT_ID:-banking-client}:${BANKING_CLIENT_SECRET:-banking-secret}" \
  -d grant_type=client_credentials -d scope=data.read "$GATEWAY/oauth2/token" \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')

step "Downstream healthy"
call; state

step "Stopping data-processor-service"
docker compose stop data-processor-service > /dev/null 2>&1
for i in 1 2 3 4 5 6; do echo -n "call $i: "; call; done
state

step "Starting data-processor-service again"
docker compose up -d --wait data-processor-service > /dev/null 2>&1
echo "healthy; the OPEN circuit lets trial calls through after 15s (HALF_OPEN)"
for _ in $(seq 1 30); do
  sleep 3
  echo -n "$(cb_state): "; call
  [ "$(cb_state)" = "CLOSED" ] && break
done
state

step "Circuit breaker transitions"
curl -s "$ACTUATOR/circuitbreakerevents/dataProcessor" \
  | grep -oE '"stateTransition":"[^"]+"' | sed -E 's/"stateTransition":"(.*)"/  \1/'
