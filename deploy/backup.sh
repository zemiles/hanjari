#!/usr/bin/env bash
# PostgreSQL 일일 백업
# pg_dump → 무결성 확인 → Object Storage 업로드 → 서버 보관분 정리 → 결과를 Prometheus 지표로 기록
# 사용법: bash ~/hanjari/backup.sh   (cron이 매일 04:17 KST에 실행)
set -Eeuo pipefail
umask 077
cd "$(dirname "$0")"

BUCKET="${BACKUP_BUCKET:-hanjari-backup}"
PREFIX="${BACKUP_PREFIX:-postgres/daily}"
LOCAL_DIR="${BACKUP_LOCAL_DIR:-$HOME/backups}"
LOCAL_KEEP_DAYS="${BACKUP_LOCAL_KEEP_DAYS:-3}"
OCI="$HOME/oci-cli/bin/oci"
METRIC_DIR="$PWD/node-exporter"

DB="$(grep -E '^POSTGRES_DB=' .env | cut -d= -f2-)"
DB_USER="$(grep -E '^POSTGRES_USER=' .env | cut -d= -f2-)"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="$LOCAL_DIR/hanjari-$STAMP.dump"
OBJECT="$PREFIX/$(basename "$FILE")"

metric() {        # metric <파일 이름> <내용>: node-exporter가 읽는 지표 파일을 통째로 교체
  local tmp
  tmp="$(mktemp "$METRIC_DIR/.$1.XXXXXX")"
  printf '%s\n' "$2" > "$tmp"
  chmod 644 "$tmp"
  mv "$tmp" "$METRIC_DIR/$1"
}

last_run() {      # last_run <1|0>: 마지막 실행 성공 여부 기록
  metric backup_last_run.prom "# HELP hanjari_backup_last_run_success 마지막 백업 실행 성공 여부 (1=성공, 0=실패)
# TYPE hanjari_backup_last_run_success gauge
hanjari_backup_last_run_success $1"
}

on_error() {
  last_run 0
  echo "!!! $(date -u +%FT%TZ) 백업 실패 (${BASH_COMMAND})" >&2
}
trap on_error ERR

install -d -m 700 "$LOCAL_DIR"
install -d -m 755 "$METRIC_DIR"

# 1. 덤프: 운영 중인 DB에서 일관된 스냅샷을 뜬다
echo "==> $(date -u +%FT%TZ) pg_dump 시작 ($DB)"
docker compose exec -T postgres pg_dump -U "$DB_USER" -d "$DB" -Fc > "$FILE"

# 2. 무결성 확인: 목차를 끝까지 읽을 수 있고, 카나리 테이블 데이터가 들어 있는지
docker compose exec -T postgres pg_restore --list < "$FILE" > "$FILE.toc"
grep -q 'TABLE DATA ops backup_canary' "$FILE.toc"
rm -f "$FILE.toc"
SIZE="$(stat -c %s "$FILE")"
SHA="$(sha256sum "$FILE" | cut -d' ' -f1)"
echo "    크기 ${SIZE} bytes, sha256 ${SHA}"

# 3. 업로드: 같은 이름이 있으면 실패 (VM에는 덮어쓰기·삭제 권한이 없다)
"$OCI" --auth instance_principal os object put \
  -bn "$BUCKET" --file "$FILE" --name "$OBJECT" \
  --no-multipart --no-overwrite --metadata "{\"sha256\":\"$SHA\"}" > /dev/null
echo "==> 업로드 완료: $OBJECT"

# 4. 서버 보관분 정리 (Object Storage 쪽은 수명 주기 규칙이 35일 뒤 삭제)
find "$LOCAL_DIR" -maxdepth 1 -name 'hanjari-*.dump' -mtime +"$LOCAL_KEEP_DAYS" -delete

# 5. 결과 기록
last_run 1
metric backup_last_success.prom "# HELP hanjari_backup_last_success_timestamp_seconds 마지막 백업 성공 시각
# TYPE hanjari_backup_last_success_timestamp_seconds gauge
hanjari_backup_last_success_timestamp_seconds $(date +%s)
# HELP hanjari_backup_last_size_bytes 마지막으로 성공한 백업 파일 크기
# TYPE hanjari_backup_last_size_bytes gauge
hanjari_backup_last_size_bytes $SIZE"
echo "==> $(date -u +%FT%TZ) 백업 성공"
