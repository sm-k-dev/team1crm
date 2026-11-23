# SaaS 화면·API 계약 — 2026-11-23 현재 구현

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
| POST | `/api/customers/{customerId}/visits` | storeId + JSON amount,visitedAt,menuMemo,couponId,usedPoints → 201 VisitResult |
| POST | `/api/customers/{customerId}/coupons` | storeId + JSON title,discount,expiresAt → 201 couponId |
| POST | `/api/ai/marketing-suggestions` | storeId,month(yyyy-MM),grade 요청 파라미터 → suggestions,remaining,example |

고객 등록·수정·삭제와 분석 조회는 아래 MVC 화면 경로를 사용한다.
독립적인 points/use, coupons/redeem API는 제공하지 않으며 방문 등록 안에서 함께 처리한다.
AI의 등급별 인원은 클라이언트 값을 신뢰하지 않고 서버가 계산한다.

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