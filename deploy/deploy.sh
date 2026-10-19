#!/usr/bin/env bash
# Blue-Green + 점진 전환(카나리) 무중단 배포
# 사용법: bash ~/hanjari/deploy.sh <이미지 태그(커밋 SHA)>
set -euo pipefail

TAG="${1:?이미지 태그(커밋 SHA)를 넘겨주세요}"
cd "$(dirname "$0")"

STAGES="${STAGES:-10 25 50 75 100}"              # 새 버전으로 보낼 트래픽 비율(%) 단계
STAGE_SECONDS="${STAGE_SECONDS:-75}"             # 단계마다 지켜보는 시간
MAX_NEW_ERRORS="${MAX_NEW_ERRORS:-0}"            # 단계마다 허용하는 새 버전 5xx 증가 건수
READY_TIMEOUT="${READY_TIMEOUT:-180}"            # 새 버전 기동 대기 최대 시간
READY_CHECKS="${READY_CHECKS:-5}"                # readiness 연속 UP 횟수(2초 간격)
DRAIN_QUIET_SECONDS="${DRAIN_QUIET_SECONDS:-15}" # 처리 중 요청 0이 이만큼 이어지면 종료
DRAIN_MAX_SECONDS="${DRAIN_MAX_SECONDS:-120}"    # 드레이닝 최대 대기 시간

DOMAIN="$(grep -E '^DOMAIN=' .env | cut -d= -f2-)"
ACTIVE="$(cat .active-slot 2> /dev/null || echo legacy)"
if [ "$ACTIVE" = "blue" ]; then NEXT="green"; NEXT_PORT=8082; else NEXT="blue"; NEXT_PORT=8081; fi
if [ "$ACTIVE" = "legacy" ]; then OLD_ADDR="api:8080"; else OLD_ADDR="api-$ACTIVE:8080"; fi
NEW_ADDR="api-$NEXT:8080"
echo "==> $ACTIVE($OLD_ADDR) → $NEXT($NEW_ADDR), 태그 $TAG"

caddy_reload() {
  docker compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile > /dev/null
}

route_only() {    # route_only <주소>: 한 곳으로만 보냄
  echo "reverse_proxy $1" > caddy/upstream.caddy
  caddy_reload
}

route_split() {   # route_split <새 버전 비율%>: 이전/새 버전에 가중치로 나눔
  cat > caddy/upstream.caddy << EOF
reverse_proxy $OLD_ADDR $NEW_ADDR {
	lb_policy weighted_round_robin $((100 - $1)) $1
	lb_try_duration 2s
}
EOF
  caddy_reload
}

inflight() {      # inflight <주소>: Caddy가 그 주소로 보내서 아직 처리 중인 요청 수
  docker compose exec -T caddy wget -qO- http://127.0.0.1:2019/reverse_proxy/upstreams 2> /dev/null \
    | python3 -c 'import json, sys; print(sum(u["num_requests"] for u in json.load(sys.stdin) if u["address"] == sys.argv[1]))' "$1" 2> /dev/null
}

drain() {         # drain <주소>: 처리 중 요청 0이 일정 시간 이어질 때까지 대기
  local quiet=0 failed=0 n
  for _ in $(seq 1 "$DRAIN_MAX_SECONDS"); do
    if ! n="$(inflight "$1")"; then
      n=1
      failed=$((failed + 1))
    fi
    if [ "$n" -eq 0 ]; then quiet=$((quiet + 1)); else quiet=0; fi
    if [ "$quiet" -ge "$DRAIN_QUIET_SECONDS" ]; then
      echo "    $1 드레이닝 완료 (처리 중 요청 0이 ${DRAIN_QUIET_SECONDS}초 유지)"
      return 0
    fi
    sleep 1
  done
  echo "    경고: $1 요청이 ${DRAIN_MAX_SECONDS}초 안에 비지 않음 (관리 API 조회 실패 ${failed}회) → graceful shutdown에 맡김"
}

server_errors() { # server_errors <포트>: 그 슬롯이 지금까지 낸 5xx 응답 수
  curl -fsS "http://127.0.0.1:$1/actuator/prometheus" \
    | awk '/^http_server_requests_seconds_count\{/ && /outcome="SERVER_ERROR"/ { s += $NF } END { printf "%d", s }'
}

ready() {         # 새 슬롯 readiness(앱 + DB + Redis)가 UP인지
  curl -fsS "http://127.0.0.1:${NEXT_PORT}/actuator/health/readiness" 2> /dev/null | grep -q '"UP"'
}

ready_stable() {  # readiness가 READY_CHECKS번 연속 UP인지 (2초 간격)
  for _ in $(seq 1 "$READY_CHECKS"); do
    ready || return 1
    sleep 2
  done
}

abort_before_traffic() {
  docker compose logs --tail=100 "api-$NEXT"
  docker compose stop "api-$NEXT"
  echo "!!! $1 → 전환하지 않음 (기존 버전 계속 서비스)"
  exit 1
}

rollback() {
  echo "!!! $1 → 이전 버전으로 되돌립니다"
  route_only "$OLD_ADDR"
  drain "$NEW_ADDR"
  docker compose logs --tail=100 "api-$NEXT"
  docker compose stop "api-$NEXT"
  exit 1
}

# 0. 준비: 처음 전환할 때는 기존 단일 api 컨테이너를 가리킨다
mkdir -p caddy
install -d -m 755 node-exporter            # 백업 지표 파일 (node-exporter가 읽음)
install -d -m 700 "$HOME/backups"          # 서버 보관 백업과 로그

# 백업 cron 등록 (서버 시각은 UTC): 매일 04:17 KST 백업, 매주 일요일 05:17 KST 복구 리허설
CRON_BACKUP="17 19 * * * bash $PWD/backup.sh >> $HOME/backups/backup.log 2>&1"
CRON_RESTORE="17 20 * * 6 bash $PWD/restore-test.sh >> $HOME/backups/restore-test.log 2>&1"
{ crontab -l 2> /dev/null | grep -v -e '/backup.sh' -e '/restore-test.sh' || true; echo "$CRON_BACKUP"; echo "$CRON_RESTORE"; } | crontab -
[ -f caddy/upstream.caddy ] || echo "reverse_proxy $OLD_ADDR" > caddy/upstream.caddy
sed -i '/^API_IMAGE_TAG=/d' .env

# 1. api를 제외한 서비스 기동 (설정이 바뀐 것만 다시 만든다)
INFRA="$(docker compose config --services | grep -v '^api-')"
docker compose up -d $INFRA
docker compose exec -T prometheus wget -qO- --post-data='' http://localhost:9090/-/reload > /dev/null || true

# 2. 쉬는 슬롯에 새 버전 기동 → 상태 확인 (이 단계까지 사용자 트래픽은 0%)
VAR="API_${NEXT^^}_TAG"
sed -i "/^${VAR}=/d" .env
echo "${VAR}=${TAG}" >> .env
docker compose pull "api-$NEXT"
docker compose up -d --no-deps --force-recreate "api-$NEXT"
waited=0
until ready; do
  [ "$waited" -lt "$READY_TIMEOUT" ] || abort_before_traffic "새 버전이 ${READY_TIMEOUT}초 안에 준비되지 않음"
  sleep 2
  waited=$((waited + 2))
done
ready_stable || abort_before_traffic "새 버전 상태가 안정적이지 않음"
echo "==> $NEXT 준비 완료 (${waited}초, readiness ${READY_CHECKS}회 연속 UP)"

# 3. 점진 전환: 단계마다 지켜보고, 문제가 있으면 즉시 되돌림
for pct in $STAGES; do
  before="$(server_errors "$NEXT_PORT" || echo 0)"
  route_split "$pct"
  echo "==> 새 버전 ${pct}% (${STAGE_SECONDS}초 관찰)"
  sleep "$STAGE_SECONDS"
  ready_stable || rollback "새 버전 readiness 불안정"
  after="$(server_errors "$NEXT_PORT" || echo 999999)"
  [ $((after - before)) -le "$MAX_NEW_ERRORS" ] || rollback "새 버전 5xx $((after - before))건 증가"
done
curl -fsS --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}/actuator/health" | grep -q '"UP"' \
  || rollback "Caddy 경유 확인 실패"

# 4. 이전 버전 드레이닝: 새 요청은 이미 0%, 처리 중인 요청이 끝날 때까지 대기
echo "==> $OLD_ADDR 드레이닝"
drain "$OLD_ADDR"
echo "$NEXT" > .active-slot
if [ "$ACTIVE" = "legacy" ]; then
  docker compose up -d --remove-orphans $INFRA
else
  docker compose stop "api-$ACTIVE"
fi
route_only "$NEW_ADDR"
docker image prune -f > /dev/null
echo "==> 배포 성공: $TAG ($ACTIVE → $NEXT)"
