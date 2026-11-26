-- 미사용 이미지 테이블 정리 (2026-11-24)
-- 먼저 Image 엔티티가 제거된 버전을 배포하고 정상 기동을 확인한다.
-- 기존 버전은 dev/update에서 테이블을 재생성하거나 prod/validate에서 기동 실패할 수 있다.
-- Workbench에서 대상 EC2의 crm 또는 matzip 스키마를 선택한 뒤 실행한다.
-- 데이터·외래키·트리거가 있으면 삭제하지 않는다. DDL은 롤백되지 않는다.

SET @crm_cleanup_schema = DATABASE();
SET @crm_cleanup_image_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @crm_cleanup_schema
      AND TABLE_NAME = 'image' AND TABLE_TYPE = 'BASE TABLE'
);
SET @crm_cleanup_read_count = IF(
    @crm_cleanup_schema IN ('crm', 'matzip') AND @crm_cleanup_image_exists = 1,
    'SELECT COUNT(*) INTO @crm_cleanup_image_rows FROM `image`',
    'SET @crm_cleanup_image_rows = NULL'
);
PREPARE crm_cleanup_statement FROM @crm_cleanup_read_count;
EXECUTE crm_cleanup_statement;
DEALLOCATE PREPARE crm_cleanup_statement;

SET @crm_cleanup_foreign_keys = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE (TABLE_SCHEMA = @crm_cleanup_schema AND TABLE_NAME = 'image' AND REFERENCED_TABLE_NAME IS NOT NULL)
       OR (REFERENCED_TABLE_SCHEMA = @crm_cleanup_schema AND REFERENCED_TABLE_NAME = 'image')
);
SET @crm_cleanup_triggers = (
    SELECT COUNT(*) FROM information_schema.TRIGGERS
    WHERE EVENT_OBJECT_SCHEMA = @crm_cleanup_schema AND EVENT_OBJECT_TABLE = 'image'
);

SELECT @crm_cleanup_schema AS selected_database,
       @crm_cleanup_image_exists AS image_table_exists,
       @crm_cleanup_image_rows AS image_rows,
       @crm_cleanup_foreign_keys AS related_foreign_keys,
       @crm_cleanup_triggers AS image_triggers;

SET @crm_cleanup_drop = IF(
    @crm_cleanup_schema IN ('crm', 'matzip')
    AND @crm_cleanup_image_exists = 1 AND @crm_cleanup_image_rows = 0
    AND @crm_cleanup_foreign_keys = 0 AND @crm_cleanup_triggers = 0,
    'DROP TABLE `image`',
    'SELECT ''삭제하지 않았습니다. 선택 DB·테이블 존재·데이터·외래키·트리거를 확인하세요.'' AS cleanup_result'
);
PREPARE crm_cleanup_statement FROM @crm_cleanup_drop;
EXECUTE crm_cleanup_statement;
DEALLOCATE PREPARE crm_cleanup_statement;

SELECT TABLE_SCHEMA, TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = @crm_cleanup_schema AND TABLE_NAME = 'image';
