#!/usr/bin/env bash
# End-to-end demo of the running stack (docker compose up -d --build first).
#   1. OAuth2 Client Credentials token through the gateway
#   2. Security checks (no token / insufficient scope)
#   3. CSV ingestion -> Kafka -> PostgreSQL
#   4. Queries through the gateway and through the resilient client
#
# Usage: scripts/demo.sh [gateway-url]   (default http://localhost:8080)
set -euo pipefail

GATEWAY="${1:-${GATEWAY_URL:-http://localhost:8080}}"
CLIENT_ID="${BANKING_CLIENT_ID:-banking-client}"
CLIENT_SECRET="${BANKING_CLIENT_SECRET:-banking-secret}"

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
json() { if command -v jq > /dev/null; then jq "$@"; else cat; echo; fi; }

token() {
  curl -sf -u "$CLIENT_ID:$CLIENT_SECRET" -d grant_type=client_credentials -d "scope=$1" \
    "$GATEWAY/oauth2/token" | sed -E 's/.*"access_token":"([^"]+)".*/\1/'
}

step "Request without token -> expected 401"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' "$GATEWAY/api/stats"

step "Client Credentials grant (scope data.read ingestion.write)"
TOKEN=$(token "data.read ingestion.write")
echo "access_token: ${TOKEN:0:40}..."
READ_TOKEN=$(token "data.read")

step "Waiting for every route to be registered in Eureka (503 until then)"
for path in /api/ingestion/last-report /api/stats /api/client/stats; do
  for _ in $(seq 1 40); do
    code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $READ_TOKEN" "$GATEWAY$path" || true)
    [ "$code" != "503" ] && [ "$code" != "000" ] && break
    sleep 3
  done
  echo "$path -> HTTP $code"
done

step "Read-only token trying to trigger ingestion -> expected 403"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X POST -H "Authorization: Bearer $READ_TOKEN" \
  "$GATEWAY/api/ingestion/run"

step "Ingest the legacy CSV files (validation + publish to Kafka)"
REPORT=$(curl -sf -X POST -H "Authorization: Bearer $TOKEN" "$GATEWAY/api/ingestion/run")
echo "$REPORT" | json .
field() { echo "$1" | sed -E "s/.*\"$2\":([0-9]+).*/\1/"; }
EXPECTED=$(( $(field "$REPORT" totalAccepted) + $(field "$REPORT" totalRejected) ))

step "Waiting for data-processor-service to consume the $EXPECTED events"
for _ in $(seq 1 30); do
  STATS=$(curl -sf -H "Authorization: Bearer $READ_TOKEN" "$GATEWAY/api/stats" || echo '{}')
  STORED=0
  for f in productosConInteres transacciones movimientosAnuales filasRechazadas; do
    n=$(field "$STATS" "$f"); [[ "$n" =~ ^[0-9]+$ ]] && STORED=$(( STORED + n ))
  done
  [ "$STORED" -ge "$EXPECTED" ] && break
  sleep 2
done
echo "$STATS" | json .

step "Transactions summary"
curl -sf -H "Authorization: Bearer $READ_TOKEN" "$GATEWAY/api/transactions/summary" | json .

step "Account 105 summary (movements + interest products)"
curl -sf -H "Authorization: Bearer $READ_TOKEN" "$GATEWAY/api/accounts/105/summary" | json .

step "Sample of rejected rows of transacciones.csv"
curl -sf -H "Authorization: Bearer $READ_TOKEN" \
  "$GATEWAY/api/stats/rejected?file=transacciones.csv&limit=3" | json .

step "Same data through resilient-client (Circuit Breaker + Retry + Rate Limiter)"
curl -sf -H "Authorization: Bearer $READ_TOKEN" "$GATEWAY/api/client/stats" | json .

step "Circuit breaker state (resilient-client actuator)"
curl -sf "${RESILIENT_CLIENT_URL:-http://localhost:8083}/actuator/circuitbreakers" | json . \
  || echo "(actuator only reachable from the Docker host)"
