#!/bin/bash
set -u

# NOTE: 100% CLAUDE Generated

# Polls the SBOM scan endpoint at a fixed interval and records latency/failures
# as CSV, to evaluate availability during rolling updates.
#
# usage: ./probe-availability.sh [-u url] [-f sbom_file] [-i interval_seconds] [-o out.csv] [-t timeout_seconds]

URL=${URL:-"https://141.76.44.125:31646/sbom"}
SBOM_FILE=${SBOM_FILE:-"sample/python3.4-sbom.zip"}
INTERVAL=${INTERVAL:-2}
TIMEOUT=${TIMEOUT:-60}
OUT_FILE=${OUT_FILE:-"probe-availability.csv"}

while getopts "u:f:i:o:t:h" opt; do
    case $opt in
        u) URL=$OPTARG ;;
        f) SBOM_FILE=$OPTARG ;;
        i) INTERVAL=$OPTARG ;;
        o) OUT_FILE=$OPTARG ;;
        t) TIMEOUT=$OPTARG ;;
        h) grep '^# ' "$0" | cut -c3-; exit 0 ;;
        *) exit 1 ;;
    esac
done

if [ ! -f "$SBOM_FILE" ]; then
    echo "[ERROR] SBOM file not found: $SBOM_FILE" >&2
    exit 1
fi

if [ ! -f "$OUT_FILE" ]; then
    echo "timestamp,elapsed_s,http_code,response_time_s,success,curl_exit,error" > "$OUT_FILE"
fi

START=$(date +%s)

function probe {
    local body metrics http_code response_time curl_exit success error
    body=$(mktemp)

    metrics=$(curl -k -s -o "$body" \
        -w "%{http_code} %{time_total}" \
        --max-time "$TIMEOUT" \
        -F "file=@$SBOM_FILE" \
        "$URL" 2>/dev/null)
    curl_exit=$?

    read -r http_code response_time <<< "$metrics"
    http_code=${http_code:-0}
    response_time=${response_time:-0}

    if [ "$curl_exit" -ne 0 ]; then
        success=0
        error="curl_error_$curl_exit"
    elif [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        success=1
        error=""
    else
        success=0
        error=$(tr '\n' ' ' < "$body" | tr -d '"' | cut -c1-200)
    fi

    rm -f "$body"

    local now elapsed
    now=$(date +%s)
    elapsed=$((now - START))
    printf '%s,%s,%s,%s,%s,%s,"%s"\n' \
        "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$elapsed" "$http_code" "$response_time" \
        "$success" "$curl_exit" "$error"
}

echo "[INFO] probing $URL every ${INTERVAL}s -> $OUT_FILE (ctrl-c to stop)"
trap 'echo; echo "[INFO] stopped, results in $OUT_FILE"; exit 0' INT

while true; do
    line=$(probe)
    echo "$line" >> "$OUT_FILE"
    echo "$line"
    sleep "$INTERVAL"
done
