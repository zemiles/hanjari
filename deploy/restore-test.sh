#!/usr/bin/env bash
# 복구 리허설: Object Storage의 백업을 내려받아 임시 PostgreSQL에 복원하고 검증 (운영 DB는 건드리지 않음)
# 사용법: bash ~/hanjari/restore-test.sh [객체 이름]   (생략하면 가장 최근 백업, cron이 매주 일요일 05:17 KST에 실행)
set -Eeuo pipefail
umask 077
cd "$(dirname "$0")"

BUCKET="${BACKUP_BUCKET:-hanjari-backup}"
PREFIX="${BACKUP_PREFIX:-postgres/daily}"
OCI="$HOME/oci-cli/bin/oci"
METRIC_DIR="$PWD/node-exporter"
NAME="hanjari-restore-test"
WORK="$(mktemp -d)"

cleanup() {
  docker rm -f "$NAME" > /dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT
trap 'echo "!!! $(date -u +%FT%TZ) 복구 리허설 실패 (${BASH_COMMAND})" >&2' ERR

oci() { "$OCI" --auth instance_principal "$@"; }

START="$(date +%s)"
install -d -m 755 "$METRIC_DIR"

# 1. 대상 백업 고르기 (이름이 시각 순서라 가장 큰 이름이 최신)
OBJECT="${1:-$(oci os object list -bn "$BUCKET" --prefix "$PREFIX/" --all --fields name \
  --query 'max_by(data, &name).name' --raw-output)}"
echo "==> $(date -u +%FT%TZ) 대상 백업: $OBJECT"

# 2. 내려받기 + sha256 대조 (업로드할 때 메타데이터에 남긴 값과 비교)
oci os object get -bn "$BUCKET" --name "$OBJECT" --file "$WORK/restore.dump" > /dev/null
EXPECTED="$(oci os object head -bn "$BUCKET" --name "$OBJECT" --query '"opc-meta-sha256"' --raw-output 2> /dev/null || true)"
ACTUAL="$(sha256sum "$WORK/restore.dump" | cut -d' ' -f1)"
if [ -n "$EXPECTED" ] && [ "$EXPECTED" != "null" ]; then
  [ "$EXPECTED" = "$ACTUAL" ] || { echo "!!! sha256 불일치: 기대 $EXPECTED, 실제 $ACTUAL" >&2; exit 1; }
  echo "    sha256 일치"
else
  echo "    경고: 객체에 sha256 메타데이터가 없어 대조를 건너뜀"
fi

# 3. 임시 DB에 복원 (운영과 분리된 일회용 컨테이너)
docker rm -f "$NAME" > /dev/null 2>&1 || true
docker run -d --name "$NAME" -e POSTGRES_PASSWORD=restore-test postgres:18 > /dev/null
until docker exec "$NAME" pg_isready -h 127.0.0.1 -U postgres > /dev/null 2>&1; do sleep 1; done
RESTORE_START="$(date +%s%N)"
docker exec -i "$NAME" pg_restore -U postgres -d postgres --no-owner --no-privileges --exit-on-error < "$WORK/restore.dump"
RESTORE_MS=$(( ($(date +%s%N) - RESTORE_START) / 1000000 ))
RESTORE_SECONDS="$(awk -v ms="$RESTORE_MS" 'BEGIN { printf "%.3f", ms / 1000 }')"

# 4. 검증: 카나리 레코드가 살아 있는지 + 테이블별 행 수
docker exec "$NAME" psql -U postgres -d postgres -v ON_ERROR_STOP=1 -At \
  -c "select note from ops.backup_canary where id = 1" | grep -q '복구 리허설용 카나리'
echo "    카나리 확인"
docker exec -i "$NAME" psql -U postgres -d postgres -v ON_ERROR_STOP=1 << 'SQL'
select table_schema || '.' || table_name as "테이블",
       (xpath('/row/c/text()',
              query_to_xml(format('select count(*) as c from %I.%I', table_schema, table_name), false, true, '')))[1]::text::bigint as "행 수"
from information_schema.tables
where table_type = 'BASE TABLE' and table_schema not in ('pg_catalog', 'information_schema')
order by 1;
SQL

# 5. 결과 기록
TOTAL=$(( $(date +%s) - START ))
tmp="$(mktemp "$METRIC_DIR/.restore_test.prom.XXXXXX")"
cat > "$tmp" << PROM
# HELP hanjari_restore_test_last_success_timestamp_seconds 마지막 복구 리허설 성공 시각
# TYPE hanjari_restore_test_last_success_timestamp_seconds gauge
hanjari_restore_test_last_success_timestamp_seconds $(date +%s)
# HELP hanjari_restore_test_restore_seconds 마지막 복구 리허설의 pg_restore 소요 시간
# TYPE hanjari_restore_test_restore_seconds gauge
hanjari_restore_test_restore_seconds $RESTORE_SECONDS
# HELP hanjari_restore_test_total_seconds 마지막 복구 리허설 전체 소요 시간 (내려받기 포함)
# TYPE hanjari_restore_test_total_seconds gauge
hanjari_restore_test_total_seconds $TOTAL
PROM
chmod 644 "$tmp"
mv "$tmp" "$METRIC_DIR/restore_test.prom"
echo "==> $(date -u +%FT%TZ) 복구 리허설 성공: 복원 ${RESTORE_MS}ms, 전체 ${TOTAL}초"
