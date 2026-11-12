-- 폐업 상태를 기록한다. 작성자: 김순민 / 작성일: 2026-11-11
-- 자동 실행용이 아니다. 고객 개인정보 마이그레이션을 먼저 확인한다.
-- customer.phone NULL 허용 및 deleted_at/anonymized_at 컬럼이 선행 조건이다.
SHOW COLUMNS FROM store LIKE 'closed_at';
SHOW COLUMNS FROM customer WHERE Field IN ('phone', 'deleted_at', 'anonymized_at');
-- closed_at이 없을 때만 아래 한 문장의 주석을 해제하여 실행한다.
-- ALTER TABLE store ADD COLUMN closed_at DATETIME(6) NULL;
-- 기존 매장의 closed_at=NULL은 영업 중이다. 매장명·고객·거래 행은 변경하지 않는다.
