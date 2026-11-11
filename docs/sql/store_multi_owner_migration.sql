-- 사장님 1명당 매장 0..N개를 허용하기 위한 기존 MySQL 스키마 변경 안내.
-- 작성자: 김순민 / 작성일: 2026-11-11
-- 자동 실행용이 아니다. 적용할 DB를 선택하고 현재 스키마와 백업을 먼저 확인한다.
-- 개발 DB 검증 대상은 crm이었다. 다른 환경에서는 해당 DB를 선택한다.
-- 아래 조회는 읽기 전용이다. DDL은 확인 후 필요한 문장만 주석을 해제해 실행한다.

SELECT DATABASE() AS selected_database;
SHOW CREATE TABLE store;
SHOW INDEX FROM store;

-- 1. SHOW INDEX에서 다음 조건을 모두 만족하는 기존 인덱스를 확인한다.
--    Non_unique=0, 해당 Key_name의 전체 행이 1개, Column_name=member_id.
--    다른 컬럼의 유니크 인덱스나 복합 인덱스는 이 절차로 제거하지 않는다.
-- 2. member_id를 첫 컬럼으로 갖는 일반 인덱스(Non_unique=1)가 없으면 먼저 추가한다.
--    idx_store_owner 이름이 이미 있으면 기존 정의를 확인하고 중복 생성하지 않는다.
--    외래키를 지원할 인덱스가 필요하므로 유니크 인덱스 제거보다 먼저 실행한다.
-- CREATE INDEX idx_store_owner ON store (member_id);

-- 3. 확인한 member_id 단일 컬럼 유니크 인덱스를 제거한다.
--    아래 이름은 2026-11-11 로컬 DB에서 실제 확인한 이름이며 환경마다 다를 수 있다.
--    해당 유니크 인덱스가 이미 없으면 실행하지 않는다.
-- ALTER TABLE store DROP INDEX UK2a7h1b2fiymx7vm90mlj2im1q;

-- 4. 적용 후 재조회하여 일반 인덱스 유지·단일 member_id 유니크 제거·외래키 보존을 확인한다.
SHOW INDEX FROM store;
SHOW CREATE TABLE store;
-- 기존 사장님에게 테스트 매장을 추가 등록하여 최종 검증한다.
-- DDL은 MySQL에서 암묵적 COMMIT을 일으킬 수 있으므로 ROLLBACK으로 복구한다고 가정하지 않는다.
-- 이 변경은 매장 행·회원 행·소유 관계를 삭제하지 않는다.
