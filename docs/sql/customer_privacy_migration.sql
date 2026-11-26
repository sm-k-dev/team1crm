-- 고객 소프트 삭제 및 주요 정보 제거 정책용 MySQL 8 마이그레이션.
-- 작성자: 김순민 / 작성일: 2026-11-11
-- 자동 실행하지 않는다. 백업 후 대상 DB를 명시적으로 선택하고 컬럼 형식을 확인한다.
-- 2026-11-26: 이미 있는 컬럼에는 ADD를 재실행하지 않는다. 실행 중 다른 DDL은 수행하지 않는다.
-- 기존 컬럼의 타입 불일치는 자동 교정하지 않으므로 실행 전후 SHOW COLUMNS로 대조한다.
SET @crm_privacy_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'deleted_at'),
    'DO 0', 'ALTER TABLE customer ADD COLUMN deleted_at DATETIME(6) NULL');
PREPARE crm_privacy_statement FROM @crm_privacy_ddl;
EXECUTE crm_privacy_statement;
DEALLOCATE PREPARE crm_privacy_statement;

SET @crm_privacy_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'anonymized_at'),
    'DO 0', 'ALTER TABLE customer ADD COLUMN anonymized_at DATETIME(6) NULL');
PREPARE crm_privacy_statement FROM @crm_privacy_ddl;
EXECUTE crm_privacy_statement;
DEALLOCATE PREPARE crm_privacy_statement;

SET @crm_privacy_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer'
             AND COLUMN_NAME = 'phone' AND IS_NULLABLE = 'YES'),
    'DO 0', 'ALTER TABLE customer MODIFY COLUMN phone VARCHAR(13) NULL');
PREPARE crm_privacy_statement FROM @crm_privacy_ddl;
EXECUTE crm_privacy_statement;
DEALLOCATE PREPARE crm_privacy_statement;
-- (store_id, phone) 유니크 제약은 유지한다. MySQL은 여러 NULL을 허용한다.
