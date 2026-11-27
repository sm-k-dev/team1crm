-- T29 방문 재전송 방지. 운영은 prod validate이므로 코드 배포 전에 DBA/배포 담당자가 적용한다.
-- 선택 DB와 customer.id / visit.id 타입(BIGINT)을 확인하고 백업 후 적용한다.
-- 기존 visit 데이터는 변경하지 않으며 요청 키 도입 이후의 방문만 보호한다.
CREATE TABLE IF NOT EXISTS visit_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    visit_id BIGINT NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    earned_points INT NOT NULL,
    discount_amount INT NOT NULL,
    used_points INT NOT NULL,
    paid_amount INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_visit_request_customer_key UNIQUE (customer_id, request_key),
    CONSTRAINT fk_visit_request_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_request_visit FOREIGN KEY (visit_id) REFERENCES visit(id) ON DELETE CASCADE
);
-- IF NOT EXISTS는 기존 테이블의 컬럼·제약 일치를 보장하지 않는다. SHOW CREATE TABLE과 엔티티를 대조한다.
