#!/bin/bash
# [mysql-backup.sh] crm DB 를 파일로 백업 (발표 전, 큰 수정 전에 실행)
# 사용법: bash mysql-backup.sh  → /home/ubuntu/crm/backup/crm-날짜.sql
# 복원:   mysql -u crm -p crm < 백업파일.sql
set -e
source /home/ubuntu/crm/crm.env
mkdir -p /home/ubuntu/crm/backup
FILE=/home/ubuntu/crm/backup/crm-$(date +%Y%m%d-%H%M).sql
mysqldump -u "${DB_USERNAME:-crm}" -p"${DB_PASSWORD}" --no-tablespaces crm > "$FILE"
echo "백업 완료: $FILE"
