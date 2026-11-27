# SaaS 화면·API 계약 — 2026-11-27 현재 구현

작성자: 김순민. 사장님은 실제 enum의 USER이며 OWNER는 업무상 명칭이다.
승인·반려·정지·재활성화 API는 현재 범위에 포함하지 않는다.

## JSON API

| Method | 경로 | 입력·응답 / 권한 |
|---|---|---|
| POST | `/api/members/signup` | 회원가입 DTO → 가입 결과. 공개 가입은 USER 고정. 웹 CSRF 적용 |
| POST | `/api/members/login` | 이메일·비밀번호 → JWT. CSRF 검사 제외 |
| GET | `/api/members/me` | 인증 회원의 id,email |
| GET | `/api/stores` | 인증 회원의 소유 매장 목록 |
| POST | `/api/stores` | name,category,address,pointRate → 201 storeId |
| GET | `/api/customers` | storeId,keyword,page(0부터) → 마스킹된 고객 페이지. 소유 매장만 |
| POST | `/api/customers/{customerId}/visits` | storeId + JSON requestKey(UUID 필수),amount,visitedAt,menuMemo,couponId,usedPoints → 201 VisitResult |
| POST | `/api/customers/{customerId}/coupons` | storeId + JSON title,discount,expiresAt → 201 couponId |
| POST | `/api/ai/marketing-suggestions` | storeId,month(yyyy-MM),grade 요청 파라미터 → suggestions,remaining,example |

고객 등록·수정·삭제와 분석 조회는 아래 MVC 화면 경로를 사용한다.
독립적인 points/use, coupons/redeem API는 제공하지 않으며 방문 등록 안에서 함께 처리한다.
AI의 등급별 인원은 클라이언트 값을 신뢰하지 않고 서버가 계산한다.

### 방문 재전송 처리

- JSON API와 MVC 방문 Form은 UUID `requestKey`가 필수다. 같은 고객·키·내용을 재전송하면 최초 방문 ID·할인·사용 포인트·적립·실결제 결과를 반환한다. JSON은 첫 요청과 재시도 모두 201이며, 방문·매출·적립은 한 번만 반영한다.
- 성공한 키로 금액·시각·쿠폰·사용 포인트·메모를 바꾸면 409, 키 누락·형식 오류는 400이다. 새 키는 같은 내용이어도 새 방문이다. 사용 포인트 null과 0은 같으며, 방문 시각 생략도 같은 요청으로 인식한다.
- MVC는 고객 상세 화면에서 생성한 키를 숨김 입력으로 보내고 등록 후 새 화면에서 새 키를 받는다. API 호출자도 같은 방문 재시도에는 키를 유지해야 한다. 실패한 트랜잭션은 키를 소비하지 않아 수정 후 재시도할 수 있다.
- 매장 잠금·요청 기록 locking read와 `(customer_id, request_key)` UNIQUE로 동시 요청을 제어한다. 성공 기록·방문·쿠폰·포인트는 함께 commit·rollback된다. 현재 소유권·CSRF 검사는 유지하며 폐업·삭제 고객의 재시도도 거절한다.
- `visit_request`에는 원문 요청 대신 SHA-256 지문과 최초 결과를 저장한다. 완전한 익명화를 의미하지 않는다. 고객·방문 물리 삭제 시 CASCADE하며, 성공 기록을 지우면 과거 키의 중복 방지도 사라지므로 임의 정리하지 않는다.
- **배포 전** [추가 테이블 SQL](../sql/visit_request_idempotency.sql)을 검증·적용하고 컬럼·FK·UNIQUE를 대조한다. `IF NOT EXISTS`는 기존 구조를 갱신하지 않는다. prod는 validate이므로 화면·서버·외부 API 호출 측을 함께 갱신해야 한다. 이전 화면은 새로 열어야 하며 과거 방문의 중복 제거는 하지 않는다.

검증 결과와 운영 미확인 범위는 [기존 검증 기록](../verification/20261127_제출전_정리.md#계획-테스트-및-t29-후속-검증)에 정리한다.

## MVC 화면 및 Form

| Method | 경로 | 역할 |
|---|---|---|
| GET | `/` | 로그인 회원의 매장별 오늘 방문·이번 달 매출 |
| GET/POST | `/customers` | storeId로 고객 목록 조회 / Form 등록 |
| GET | `/customers/{customerId}` | 소유 매장 고객 상세 + 방문·포인트·쿠폰 |
| GET/POST | `/customers/{customerId}/edit` | 고객 수정 Form 조회 / 저장 |
| POST | `/customers/{customerId}/delete` | 소프트 삭제 |
| POST | `/customers/{customerId}/visits` | 방문·결제·포인트·쿠폰 사용 Form |
| POST | `/customers/{customerId}/coupons` | 쿠폰 발급 Form |
| GET | `/analytics/dashboard` | 소유 매장 분석·RFM 화면 |
| GET | `/admin`, `/admin/stores`, `/admin/owners` | 관리자 화면. 등록·수정은 각 Controller의 Form 경로 사용 |

## 공통 인가·오류

- dev/prod 업무 화면은 동일하게 인증·역할·소유권을 검사한다.
- storeId는 요청으로 받되 인증된 회원의 소유 매장인지 서버에서 검증한다.
- 다른 소유자의 매장은 403, 없는 매장은 404, 타 매장/삭제/없는 고객은 404.
- 입력 및 잔액 등 업무 검증 오류는 400, 폐업 매장 쓰기는 409.
- JSON 방문·쿠폰 업무 오류는 message 응답을 제공한다. 전역 응답 envelope는 아직 통일하지 않았다.
- Bearer JWT 비공개 API는 stateless/CSRF 제외, 일반 웹·세션 요청은 CSRF 보호를 유지한다.
- RFM·시연 데이터·운영 설정의 상세 기준은 [통합 수정 문서](14_요구사항_통합_수정_20261123.md)를 따른다.
