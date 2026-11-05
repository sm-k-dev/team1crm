> **문서 기준 (2026-11-04):** [공식 SaaS 설계](../README.md) · [업무 분류](../README.md) · [팀 가이드](../README.md). 배포 전이며 ADMIN은 김순민의 B 개발 업무에 포함됩니다.

> **SaaS 공식 문서 기준(v1):** [아키텍처 목차](../README.md) · [화면/담당 작업계약](../README.md). ADMIN 담당은 김순민, OWNER 인증은 황현우이며 결제·구독·요금제는 제외합니다. 기존 초기 초안과 충돌할 경우 공식 설계를 우선합니다.

# 1조(5명) 팀 프로젝트 준비 번들 안내

> **2026-11-04 기준.** 이 ZIP은 선생님 CRM 스타터 + Modern SaaS 디자인 제안본 + 팀 협업 규칙 초안 + 1조 역할 및 업무 관리 템플릿을 묶은 검토용 파일입니다. 팀원 합의 전 제안 규칙을 확정 규칙으로 취급하지 않습니다.
>
> **현재 실제 배정:** 김순민(팀장+B 고객관리), 배수환(부팀장·인프라), 황현우(A 인증·매장), 이재준(C 방문·포인트·쿠폰), 한상희(D 분석·RFM·AI).
>
> `docs/team-proposals/project-management/README.md`에서 팀별 업무일지와 Google Sheets용 CSV를 확인하세요. 아래 역할·담당자 표는 1조 실제 5인 배정 기준으로 수정했습니다.

---

# 📊 소상공인 단골 고객 관리(CRM)

5인 팀 프로젝트 · Spring Boot 3.4 · Spring Security · JWT · 구글 로그인 · JPA · MySQL 8 · 타임리프 · Spring AI(OpenRouter 무료 모델) · AWS EC2/S3 · GitHub Actions

## 실행 (내 PC)

1. MySQL 8 에서 한 번 실행
```sql
CREATE DATABASE crm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'crm'@'localhost' IDENTIFIED BY 'crm1234';
GRANT ALL PRIVILEGES ON crm.* TO 'crm'@'localhost';
```
2. IntelliJ 에서 `CrmApplication` 실행 → http://localhost:8082
3. 테스트 계정: `admin@test.com` / `user1@test.com` / `user2@test.com` (비밀번호 모두 `1234`)

## 환경 변수

| 환경 변수 | 용도 | 없으면 |
|---|---|---|
| `OPENROUTER_API_KEY`, `OPENROUTER_MODEL` | AI 재방문 문구 (`:free` 모델만) | AI 버튼이 안내 문구 표시 |
| `APP_FAKE_DATA` → properties `app.fake-data` | 가짜 고객 50명·6개월 방문 생성 (user2 매장) | 기본 true |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) | 실행할 때마다 임시 키 |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL 계정 | `crm` / `crm1234` |

### IntelliJ 에서 환경 변수 넣는 방법

1. 오른쪽 위 실행 목록 → **Edit Configurations...** (또는 Run → Edit Configurations)
2. 왼쪽 Spring Boot 아래 `CrmApplication` 선택
3. 오른쪽 위 **Modify options** (Alt+M) → **Operating System** 묶음의 **Environment variables** 체크
4. 새로 생긴 **Environment variables** 칸에 `이름=값` 을 세미콜론(`;`)으로 이어서 입력
   예) `OPENROUTER_API_KEY=sk-or-v1-...;OPENROUTER_MODEL=모델이름:free;JWT_SECRET=32자이상문자열`
5. 구글 로그인을 켤 때는 **Active profiles** 칸에 `google` 입력
6. **Apply** → **OK** → ▶ 실행

환경 변수를 하나도 넣지 않아도 서버는 실행되고, AI 버튼만 "잠시 사용할 수 없습니다" 로 응답합니다.

## 화면

`/` 홈(매장 등록·요약) · `/customers` 고객 목록·등록 · `/customers/{id}` 고객 상세(방문·포인트·쿠폰) · `/dashboard` 분석 대시보드(차트·RFM·AI)

## API

| 메서드 | 주소 | 설명 | 담당 |
|---|---|---|---|
| POST | `/api/members/signup` · `/api/members/login` | 회원가입 · 로그인(세션 + JWT 토큰) | 황현우 |
| POST | `/api/store` | 매장 등록·수정 {name, category, address, pointRate} | 황현우 |
| POST | `/api/customers` | 고객 등록 {name, phone, birthMonth, memo, agreeMarketing} | 김순민 |
| PUT | `/api/customers/{id}` | 고객 수정 | 김순민 |
| DELETE | `/api/customers/{id}` | 고객 삭제 (방문·포인트·쿠폰 함께 삭제) | 김순민 |
| POST | `/api/customers/{id}/visits` | 방문 등록 {amount, usePoint, menuMemo} → {earned} | 이재준 |
| POST | `/api/customers/{id}/coupons` | 쿠폰 발급 {title, discount, expiresAt} | 이재준 |
| POST | `/api/coupons/{id}/use` | 쿠폰 사용 | 이재준 |
| POST | `/api/ai/marketing` | AI 문구 {grade: VIP|단골|일반|이탈 위험} → {messages, remaining} | 한상희 |

AI 응답 코드: 200 성공 / 400 입력 오류 / 401 로그인 필요 / 429 하루 5회 초과 / 503 AI 일시 오류

## 역할

| 역할 | 맡는 일 | 브랜치 |
|---|---|---|
| 👑 김순민 (팀장) | 홈(요약 카드), 공통 레이아웃, 일정·머지 | `feature/layout` |
| 🛠️ 배수환 (부팀장) | GitHub Actions CI/CD, EC2·S3·MySQL, 가짜 데이터 생성기, 테스트, README | `feature/cicd` |
| 황현우 (회원·매장) | 회원가입·로그인, JWT, 구글 로그인, 매장 등록 | `feature/member` |
| 김순민 (고객관리) | 고객 등록·수정·삭제·검색, 전화번호 마스킹 | `feature/customer` |
| 이재준 (방문·포인트) | 방문 등록, 포인트 적립·사용, 쿠폰 발급·사용 | `feature/visit` |
| 한상희 (분석·AI) | 분석 SQL·대시보드 차트·RFM 등급, **AI 재방문 문구** | `feature/analytics` |

## 테스트

`./gradlew clean build` → `AiMarketingParseTest`(AI 문구 파싱·RFM 등급 규칙), `FreeModelGuardTest`, `AiUsageLimiterTest`, 앱 기동 테스트(가짜 데이터 생성 포함)
(테스트는 H2 MySQL 호환 모드라 MySQL 없이 GitHub Actions 에서 실행됩니다)

## 배포

`docs/devops-common-guide.html` 을 따릅니다. 이 프로젝트의 이름은 서비스 `crm`, DB `crm` 입니다.

## 📄 파일명·문서 버전 규칙

소스코드는 고정 파일명 + Git/PR로 관리하고, 별도 보관하는 문서 개정판은 `파일명_YYYYMMDD_vN.확장자`를 사용합니다. 날짜가 바뀌어도 동일 산출물의 버전은 연속 증가합니다. `README.md`, `docs/TEAM_GUIDE.md`, `docs/TODO_BY_ROLE.md`는 파일명을 고정합니다. 자세한 기준은 [`문서 네이밍·버전 관리 규칙`](../README.md)을 참조하세요.


## 모듈형 패키지 구조 (리팩터링 v1)

- 상세 패키지/담당: [`docs/architecture/01_프로젝트_패키지_구조.md`](../README.md)
- 도메인 간 호출: [`02_도메인_간_연동_규칙.md`](../README.md)
- 화면·디자인: [`03_Thymeleaf_화면_구조.md`](../README.md)
- **주의:** Spring Boot 4/JDK 25 빌드·실행 검증 전. [`검증 체크리스트`](../README.md) 참조.


## 모듈형 리팩터링 v1 문서·업무관리
- 패키지 및 소유권: `docs/architecture/01_프로젝트_패키지_구조.md`
- 도메인 연동: `docs/architecture/02_도메인_간_연동_규칙.md`
- 최신 업무관리: `docs/team-proposals/project-management/templates/CRM_1조_업무관리_모듈형_v1.xlsx`
- 이전 v9 엑셀은 이전 기준 기록이며, v1을 현재 작업 기준으로 사용한다.


## v1 패키지 및 빌드 기준
- Java 패키지: `com.team1.crm` (화면상 회사/팀 브랜드명과 별도)
- Gradle Wrapper: 9.2.1 / JDK: 25 / Spring Boot: 4.0.0
- 상세: [`docs/architecture/05_패키지명_및_빌드_검증_v1.md`](../README.md)


## [SaaS 공식 설계 v1 업데이트 · 2026-11-04]
- **ADMIN 및 고객·홈·통합 담당: 김순민**, OWNER 가입·인증: 황현우, loyalty: 이재준, analytics: 한상희, 인프라: 배수환.
- 확정 범위: ADMIN 매장·OWNER 관리 + OWNER 직접 가입 + 매장별 CRM. 결제·구독·요금제 제외.
- 공식 기준: `docs/architecture/00_SaaS_공식설계_v1.md`, `04_SaaS_ERD.md`, `05_SaaS_API.md`, `08_SaaS_구현_점검_v1.md`.
- 기존 문서의 USER/ADMIN만 있는 역할표 또는 ADMIN 제외 문구는 **이 확정 결정으로 대체**합니다. 기존 내용은 변경 이력으로 보존합니다.
- 목표 설계와 구현 완료를 구분하며, 코드/DB/API는 별도 작업으로 검증합니다.

## SaaS v1 화면·업무관리 갱신 (2026-11-04)
- ADMIN 담당: 김순민. `admin.AdminViewController`와 `templates/admin/{index,stores,owners}.html` 추가 (현재 조회 기능).
- OWNER 내 매장 정보: `auth.StoreSettingsViewController`, `templates/auth/store-settings.html` 추가 (조회 기능).
- 공통 `fragments/layout.html`에 ADMIN 전용 메뉴 및 매장 정보 메뉴 연결.
- 업무관리 엑셀은 **기존 `패키지_담당`, `팀원_역할`, `공통_작업` 시트 자체를 수정**하고, `SaaS_API_화면`, `SaaS_개발백로그`도 갱신.
- `docs/architecture/06_화면_Java_매핑.md`에서 구현/미구현 상태 확인.
- **미구현**: ADMIN 직접 등록·승인·중지, OWNER 상태 모델/마이그레이션, 로열티 Fragment 독립화, 테넌트 격리 테스트.
- JDK 25 빌드 검증은 수행되지 않았음. 코드 생성과 동작 검증을 혼동하지 말 것.


### 업무관리 엑셀 v1 보정 — ADMIN 드롭다운
- `드롭다운_목록`: 김순민 B/팀장 업무 선택 목록에 ADMIN 매장 등록·승인·정지, OWNER 관리, API/UI 및 테스트 항목 추가
- `일지_김순민_B`, `일지_김순민_팀장`: C4:C503 업무 구분 드롭다운과 담당 설명 업데이트
- 기존 업무 기록, 타 팀원 시트, SaaS 설계 문서는 유지
