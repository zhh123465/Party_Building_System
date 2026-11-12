#!/usr/bin/env bash
# 党员发展智能管理系统：数据库 + 材料文件备份
# 用法：bash scripts/backup.sh
# 建议 cron：0 2 * * * cd /root/develop/Party_Building_System && bash scripts/backup.sh >> /var/log/party-backup.log 2>&1
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$ROOT/.env" ] && set -a && source "$ROOT/.env" && set +a

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-party_build}"
DB_USER="${DB_USER:-party}"
DB_PASSWORD="${DB_PASSWORD:-party_dev_2026}"
FILE_ROOT="${FILE_ROOT:-$ROOT/backend/data/files}"
BACKUP_DIR="${BACKUP_DIR:-/data/party-backup}"
RETAIN_DAYS="${RETAIN_DAYS:-28}"

STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR/db" "$BACKUP_DIR/files"

DB_FILE="$BACKUP_DIR/db/party_build-$STAMP.sql.gz"
FILES_FILE="$BACKUP_DIR/files/party-files-$STAMP.tar.gz"

echo "[$(date '+%F %T')] backup start"

# 数据库：单事务一致性导出
MYSQL_PWD="$DB_PASSWORD" mysqldump \
  -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
  --single-transaction --no-tablespaces --routines --triggers --default-character-set=utf8mb4 \
  "$DB_NAME" | gzip > "$DB_FILE"

# 材料文件目录
if [ -d "$FILE_ROOT" ]; then
  tar -czf "$FILES_FILE" -C "$(dirname "$FILE_ROOT")" "$(basename "$FILE_ROOT")"
else
  echo "WARN: FILE_ROOT 不存在：$FILE_ROOT" >&2
fi

# 校验值
sha256sum "$DB_FILE" > "$DB_FILE.sha256"
[ -f "$FILES_FILE" ] && sha256sum "$FILES_FILE" > "$FILES_FILE.sha256"

# 清理过期备份
find "$BACKUP_DIR/db" -name '*.sql.gz*' -mtime +"$RETAIN_DAYS" -delete
find "$BACKUP_DIR/files" -name '*.tar.gz*' -mtime +"$RETAIN_DAYS" -delete

echo "[$(date '+%F %T')] backup done"
echo "  DB:    $DB_FILE"
echo "  Files: $FILES_FILE"
echo "  保留天数：$RETAIN_DAYS"