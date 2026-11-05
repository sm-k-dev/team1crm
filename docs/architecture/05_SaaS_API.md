# SaaS API 계약 초안 v1

> **목표 계약 (미구현 경로 포함)**. 표준 경로 제안. **현재 구현 API와 일치한다고 가정하지 않는다.** 기존 컨트롤러 경로와 대조한 후 담당자 PR로 적용한다.

| Method | 경로 | 권한 | 요청 핵심 | 응답 핵심 | 담당 |
|---|---|---|---|---|---|
| POST | `/api/members/signup` | PUBLIC | email,password,storeName,... | memberId,storeId,status=PENDING | 황현우 |
| POST | `/api/members/login` | PUBLIC | email,password | accessToken,role,status | 황현우 |
| GET | `/api/members/me` | OWNER/ADMIN | - | id,role,status,storeId? | 황현우 |
| GET | `/api/admin/summary` | ADMIN | - | totalStores,pending,active,suspended | 김순민 |
| GET | `/api/admin/stores` | ADMIN | page,status,keyword | paged store summary | 김순민 |
| POST | `/api/admin/stores` | ADMIN | store info,ownerEmail or invite | storeId,ownerId,status | 김순민+황현우 |
| PATCH | `/api/admin/stores/{id}/status` | ADMIN | status | id,status | 김순민 |
| GET | `/api/admin/owners` | ADMIN | page,keyword,status | paged owners | 김순민 |
| PATCH | `/api/admin/owners/{id}/status` | ADMIN | status | id,status | 김순민 |
| GET | `/api/stores/me` | OWNER | - | store profile | 황현우 |
| GET/POST | `/api/customers` | OWNER | query / customer form | store-scoped result | 김순민 |
| GET/PATCH/DELETE | `/api/customers/{id}` | OWNER | customer update | own-store customer | 김순민 |
| POST | `/api/customers/{id}/visits` | OWNER | amount,visitedAt | visitId,earnedPoints | 이재준 |
| POST | `/api/customers/{id}/points/use` | OWNER | points | balance,historyId | 이재준 |
| POST | `/api/customers/{id}/coupons` | OWNER | title,discount,expiresAt | couponId | 이재준 |
| POST | `/api/coupons/{id}/redeem` | OWNER | - | usedAt | 이재준 |
| GET | `/api/analytics/summary` | OWNER | range | aggregated revenue/visits | 한상희 |
| GET | `/api/analytics/rfm` | OWNER | range | segments/counts | 한상희 |
| POST | `/api/ai/marketing-suggestions` | OWNER | aggregate metrics only | 3 suggestions | 한상희 |

### 공통 계약
- OWNER는 인증된 OWNER의 소유 매장 목록과 요청 storeId를 대조하여 접근을 허용; 소유하지 않은 storeId 거부 (403).
- ADMIN 생성·상태변경은 서버 역할 검사 및 감사 기록; 공개 가입에서 ADMIN 생성 불가.
- 계정 상태 `PENDING`/`SUSPENDED`는 보호 API 접근 불가. 역할별 로그인 후 리다이렉트.
- 응답 오류 예시 `{ "code": "FORBIDDEN_STORE", "message": "접근 권한이 없습니다." }`; 400/401/403/404/409 통일.
- 경로·DTO·권한·오류코드가 변경되면 이 문서를 먼저/동시에 갱신하고 관련 팀원 승인.

현재 경로의 차이는 [구현 격차](../IMPLEMENTATION_GAP.md)를 참조하세요.


## 다중 매장 소유권 원칙 (확정)
- OWNER 1명은 매장 0..N개를 소유할 수 있다. ADMIN의 전체 매장 접근과 구분한다.
- OWNER는 매장 목록 조회 후 활성 매장을 선택한다. 서버는 모든 요청에서 해당 매장의 소유권을 검증한다.
- 매장별 고객·방문·포인트·쿠폰 데이터는 서로 섞이지 않아야 한다.
- 기존 단일 매장 API 응답(`storeId`)과 `/api/stores/me`는 다중 매장용 목록 및 선택 매장 계약으로 재설계해야 하며, 아직 구현된 것으로 간주하지 않는다.

### 다중 매장 API 제안 (미구현)
| GET | `/api/stores/mine` | OWNER | - | 소유 매장 목록 |
| GET | `/api/stores/{storeId}` | OWNER | 소유권 확인 | 선택 매장 정보 |
| GET | `/api/stores/{storeId}/customers` | OWNER | 소유권 확인 | 해당 매장 고객 목록 |

기존 `/api/customers` 등 단일 매장 경로는 팀에서 호환성/전환 정책 결정 후 교체한다.
