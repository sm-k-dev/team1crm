-- 고객 소프트 삭제 및 주요 정보 제거 정책용 MySQL 8 마이그레이션.
-- 작성자: 김순민 / 작성일: 2026-11-11
-- 자동 실행하지 않는다. 백업 후 SHOW COLUMNS FROM customer로 현 스키마를 확인한다.
-- deleted_at/anonymized_at이 이미 있으면 해당 ADD COLUMN 문장은 제외한다.
ALTER TABLE customer ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE customer ADD COLUMN anonymized_at DATETIME(6) NULL;
ALTER TABLE customer MODIFY COLUMN phone VARCHAR(13) NULL;
-- (store_id, phone) 유니크 제약은 유지한다. MySQL은 여러 NULL을 허용한다.
