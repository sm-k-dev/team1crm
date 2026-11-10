# Team1 CRM SaaS 공식 설계 v1

> 확정: 2026-11-04 / 상태: **설계 확정, 구현·테스트 미완료** / 기본 패키지 `com.team1.crm`

## 범위와 역할
- **ADMIN (김순민)**: 플랫폼 전체 매장 등록·목록·승인·정지, OWNER 계정 생성·연결·상태 조회/변경. ADMIN 공개 가입 금지.
- **OWNER (황현우 인증 담당)**: 직접 회원가입 + 매장 등록 신청, 승인 후 자기 매장의 CRM 이용. OWNER 1명은 매장 0..N개를 소유하며, 소유 매장 목록에서 활성 매장을 선택한다.
- **고객**: CRM에 등록되는 데이터 주체이며 로그인 계정이 아님.
- **제외**: 결제, 구독, 요금제, 청구, 결제 게이트웨이.

## 가입 경로
1. OWNER 직접 가입 → member(role=OWNER, status=PENDING) 및 store(status=PENDING) 생성 → ADMIN 승인 → ACTIVE → 로그인/CRM 이용.
2. ADMIN 매장 등록 → 매장 및 OWNER 연결(기존 OWNER 연결 또는 초대) → 계정 설정 링크/초기 비밀번호 재설정 → ACTIVE.
3. 정지(SUSPENDED) 시 해당 OWNER의 CRM 접근 차단. 회원가입·승인 정책은 API에서 서버 측 검증.

## 테넌트 격리
- 공용 MySQL 8, `store.id`를 테넌트 키로 사용. customer.store_id, visit.customer_id, point_history.customer_id, coupon.customer_id 관계를 통해 소유 매장 검증.
- OWNER API는 요청의 storeId를 신뢰하지 않고 **인증된 사용자 → 소유 Store 목록 중 선택된 Store**로 서버에서 결정.
- 고객 ID·방문 ID·쿠폰 ID를 바꾸어 타 매장에 접근하면 403 또는 404. 검색/CSV/집계/AI도 동일.
- ADMIN과 OWNER는 역할 및 계정/매장 상태 모두 확인. 권한 변경은 감사 로그 권장.

## 데이터 및 운영
- 실습용 가짜 고객 50명 및 6개월 방문 기록 사용. 실제 개인정보 금지.
- AI에는 고객 이름·전화번호·이메일을 전달하지 않고 집계된 수치만 전송.
- 실제 개발 코드의 `Role.USER`, Member/Store 상태 필드 부재 등은 **마이그레이션 작업**이며 이 문서만으로 구현되지 않음.
- [README](../../README.md)와 [역할별 미완료 작업](../TODO_BY_ROLE.md)은 이 공식 설계를 기준으로 확인한다.

> **구현 주의:** Reference에 있던 ADMIN 매장/OWNER 생성·수정 로직은 이 Starter에서는 TODO 스켈레톤입니다. 승인/정지 상태 모델 및 권한 정책도 미구현입니다. 담당별 작업은 [역할별 미완료 작업](../TODO_BY_ROLE.md)을 확인하세요.


## 다중 매장 소유권 원칙 (확정)
- OWNER 1명은 매장 0..N개를 소유할 수 있다. ADMIN의 전체 매장 접근과 구분한다.
- OWNER는 매장 목록 조회 후 활성 매장을 선택한다. 서버는 모든 요청에서 해당 매장의 소유권을 검증한다.
- 매장별 고객·방문·포인트·쿠폰 데이터는 서로 섞이지 않아야 한다.
- 기존 단일 매장 API 응답(`storeId`)과 `/api/stores/me`는 다중 매장용 목록 및 선택 매장 계약으로 재설계해야 하며, 아직 구현된 것으로 간주하지 않는다.
