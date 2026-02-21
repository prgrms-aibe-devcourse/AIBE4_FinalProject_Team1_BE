#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# .env 로드
# ============================================================
ENV_FILE="$(dirname "$0")/../.env"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a
else
  echo "[ERROR] .env 파일을 찾을 수 없습니다: $ENV_FILE"
  exit 1
fi

# ============================================================
# SSM 터널 설정값 (필수 환경변수 검증)
# ============================================================
EC2_ID="${EC2_ID:?EC2_ID가 .env에 없습니다}"
RDS_ENDPOINT="${RDS_ENDPOINT:?RDS_ENDPOINT가 .env에 없습니다}"
LOCAL_DB_PORT="15432"
AWS_REGION="ap-northeast-2"
COMPOSE_FILE="docker-compose.yml"

# ============================================================
# 필수 도구 체크
# ============================================================
require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[ERROR] '$cmd' 명령을 찾을 수 없습니다."
    echo "        AWS CLI / Session Manager Plugin / Docker 설치를 확인하세요."
    exit 1
  fi
}

require_cmd aws
require_cmd session-manager-plugin
require_cmd docker

# AWS CLI 옵션
AWS_ARGS=(--region "$AWS_REGION")
if [[ -n "${AWS_PROFILE:-}" ]]; then
  AWS_ARGS+=(--profile "$AWS_PROFILE")
fi

# ============================================================
# 터널 에러 디버깅
# ============================================================
LOG_DIR="./.ssm-logs"
mkdir -p "$LOG_DIR"

RDS_LOG="$LOG_DIR/ssm-rds.log"

: > "$RDS_LOG"

# ============================================================
# 터널 포트 오픈 대기 함수
# ============================================================
wait_for_port_open_log() {
  local name="$1"
  local logfile="$2"
  local port="$3"
  local pid="$4"
  local timeout="${5:-60}"

  local start
  start="$(date +%s)"

  while true; do
    if grep -q "Port ${port} opened" "$logfile" 2>/dev/null; then
      echo "  [$name] localhost:${port} 터널 열림"
      return 0
    fi

    if ! kill -0 "$pid" 2>/dev/null; then
      echo ""
      echo "  [$name] SSM 세션이 비정상 종료됨. 로그:"
      tail -n 20 "$logfile" || true
      return 1
    fi

    local now
    now="$(date +%s)"
    if (( now - start >= timeout )); then
      echo ""
      echo "  [$name] ${timeout}초 타임아웃. 로그:"
      tail -n 20 "$logfile" || true
      return 1
    fi

    sleep 0.3
  done
}

# ============================================================
# 종료 시 SSM 터널 자동 정리
# ============================================================
cleanup() {
  echo ""
  echo "[cleanup] SSM 터널 종료 중..."
  [[ -n "${PID_RDS:-}" ]] && kill "$PID_RDS" 2>/dev/null || true
  echo "[cleanup] 완료"
}
trap cleanup EXIT INT TERM

# ============================================================
# 실행
# ============================================================
echo ""
echo "========================================="
echo "  로컬 개발 환경 시작 (SSM + Docker)"
echo "========================================="
echo ""

echo "[1/3] RDS 터널 시작 (localhost:${LOCAL_DB_PORT} → RDS:5432)"
aws "${AWS_ARGS[@]}" ssm start-session \
  --target "$EC2_ID" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"$RDS_ENDPOINT\"],\"portNumber\":[\"5432\"],\"localPortNumber\":[\"$LOCAL_DB_PORT\"]}" \
  </dev/null >"$RDS_LOG" 2>&1 &
PID_RDS=$!

echo "[2/3] 터널 오픈 대기 (최대 60초)..."
wait_for_port_open_log "RDS" "$RDS_LOG" "$LOCAL_DB_PORT" "$PID_RDS" 60

echo ""
echo "[3/3] docker compose up --build"
echo ""
docker compose -f "$COMPOSE_FILE" up --build