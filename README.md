# StoreCRM — 다중 매장 고객 관리 시스템

소규모 매장의 사장님이 고객과 방문·매출 현황을 관리하고, 고객 분석을 마케팅에 활용하는 팀 프로젝트입니다.
한 사장님이 여러 매장을 소유할 수 있으며, 로그인한 회원의 소유 매장을 기준으로 데이터를 조회하고 관리합니다.
플랫폼 관리자는 사장님 계정과 매장 연결 현황을 관리합니다.

서비스 범위: **ADMIN 플랫폼 관리 / OWNER 다중 매장 CRM / 고객·방문·포인트·쿠폰·분석·AI**

이 문서는 현재 체크아웃된 코드 기준입니다. 다른 기능 브랜치의 작업은 통합 후 반영합니다.

## 주요 기능

| 영역 | 현재 구현 내용 |
| --- | --- |
| 인증 | 회원가입, 일반 로그인·로그아웃, JWT 로그인 API, Google OAuth2 로그인 연결 |
| 사장님·매장 | 소유 매장 조회·선택·등록·수정, 본인 이름 수정, 이름 중복 검사 |
| 고객 | 매장별 등록·상세·수정·삭제, 이름·전화번호 뒤 4자리 검색, 페이지 조회, 전화번호 마스킹 |
| 홈 | 선택 매장의 현재 고객 수·오늘 방문·이번 달 매출 요약과 업무 화면 이동 |
| ADMIN | 사장님 계정 등록·이름 수정, 매장 등록·수정·소유자 변경, 계정·매장 연결 및 월별 신규 등록 집계 |
| 분석·AI | 기간별 매출·방문 지표, RFM 고객 등급, OpenRouter AI 마케팅 문구 생성 및 사용 횟수 제한 |
| 개인정보 | 고객 소프트 삭제, 삭제 3개월 후 주요 정보 제거 배치, 매장 폐업 시 즉시 주요 정보 제거·과거 집계 보존 |

코드의 사장님 역할 enum은 `USER`이며 문서의 OWNER에 해당합니다. 관리자는 `ADMIN`입니다.
Google 로그인 회원도 같은 `member` 테이블에서 관리합니다.

### 고객 삭제와 매장 폐업

- 고객 삭제 시 `deleted_at`을 기록하고 고객 업무 조회에서 제외합니다. 달력 기준 3개월 경과 후 이름을 대체하고 전화번호·생일·메모 등 주요 정보를 제거합니다.
- 매장 폐업 시 매장명은 유지하고 `closed_at`으로 상태를 표시합니다. 해당 매장 고객 주요 정보와 방문 메모 등을 즉시 정리합니다.
- 고객·매장 ID와 거래 관계를 보존해 과거 방문·매출 집계를 유지합니다. 폐업 매장은 과거 통계를 조회할 수 있고 고객 관리·매장 정보 변경은 제한합니다.
- 고객 ID와 거래가 남으므로 완전 익명화를 의미하지 않습니다. 로그·백업 등 별도 저장처는 추가 점검 대상입니다.

## 기술 구성

| 구분 | 기술 |
| --- | --- |
| 언어·빌드 | Java 25, Gradle Wrapper |
| 서버 | Spring Boot 4.0.0, Spring MVC |
| 데이터 | Spring Data JPA, MySQL 8, Bean Validation |
| 화면 | Thymeleaf, Bootstrap, HTML·CSS·JavaScript |
| 인증·인가 | Spring Security, OAuth2 Client, JJWT |
| AI | Spring AI 의존성, OpenRouter API 연동 |
| 테스트 | JUnit 5, Mockito, Spring Boot Test, H2 |
| 배포 관련 | GitHub Actions 설정, AWS EC2·S3 관련 설정 및 스크립트 |

개발·운영 프로필 모두 MySQL과 Thymeleaf를 사용합니다.
`templates/`는 동적 업무 화면, `static/`은 CSS·JavaScript 등 정적 자원입니다.
`static/pages/`의 디자인 미리보기는 실제 업무 화면과 다를 수 있습니다.

## 팀 구성

| 담당 | 이름 | 주요 영역 |
| --- | --- | --- |
| 팀장·B | 김순민 | 고객·ADMIN, 공통 UI·홈, 기능 통합 및 문서 |
| 부팀장 | 배수환 | 인프라·보안·배포 |
| A | 황현우 | 인증·회원·OWNER 매장 |
| C | 이재준 | 방문·포인트·쿠폰 |
| D | 한상희 | 분석·RFM·AI |

## 로컬 실행

### 1. 준비

JDK 25와 MySQL 8을 준비하고 IDE·Gradle의 Java 설정을 맞춥니다.
접속할 DB 이름과 계정 권한을 확인합니다. 기존 DB는 아래 마이그레이션 안내를 먼저 확인합니다.

### 2. 환경변수

IntelliJ 실행 설정 또는 실행 프로세스의 환경변수에 입력합니다.
`.env.example`은 참고 파일이며 Spring Boot가 `.env`를 자동으로 읽는 구성은 아닙니다.

| 변수 | 설명 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 로컬 개발은 `dev`. 미지정 시 기본값도 `dev` |
| `SERVER_PORT` | 서버 포트. 예: `8082` |
| `DB_URL` | 예: `jdbc:mysql://localhost:3306/crm?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `DB_USERNAME`, `DB_PASSWORD` | 선택한 DB에 접근 가능한 MySQL 계정·비밀번호 |
| `JWT_SECRET` | 현재 구현은 UTF-8 바이트를 사용. 최소 32바이트 이상의 무작위 JWT 서명 키 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google에서 함께 발급한 OAuth ID·Secret. 현재 공통 설정에서 참조하므로 실행 환경에 제공 |
| `OPENROUTER_API_KEY`, `OPENROUTER_MODEL` | AI 기능 사용 시 설정하는 키·모델 |
| `CUSTOMER_PRIVACY_ENABLED` | 3개월 경과 고객 정보 제거 배치 활성화. 기본 `false` |
| `CUSTOMER_PRIVACY_CRON` | 기본 `0 0 3 * * *`, 한국 시간 매일 03시 |

개발 프로필의 `DB_URL` 기본값은 로컬 `crm`입니다. EC2 등 다른 DB는 실제 DB 이름을 포함한 URL을 명시합니다.
`dev`는 `ddl-auto=update`로 연결한 DB의 스키마 변경을 시도하므로 공유 DB는 팀 적용 절차를 따릅니다.
Google OAuth Secret과 JWT 키는 용도가 다르므로 별도로 관리하고 실제 비밀값은 Git에 커밋하지 않습니다.

로컬 Google 로그인의 기본 콜백 주소는 아래와 같습니다. Google Cloud에 등록한 리디렉션 URI와 실제 접속 주소·포트를 맞춥니다.

```text
http://localhost:8082/login/oauth2/code/google
```

### 3. 시작

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

또는 IDE에서 `com.team1.crm.CrmApplication`을 실행합니다.
`SERVER_PORT=8082` 기준 접속 주소는 `http://localhost:8082/`, 로그인 화면은 `/login`입니다.
SQL 초기화는 자동 실행하지 않습니다. 더미 데이터가 필요하면 [시드 SQL](docs/sql/seed_dummy.sql)을 검토한 뒤 개발 DB에 적용합니다.

## DB 변경·운영 설정

기존 DB는 현재 상태를 조회한 뒤 필요한 변경만 적용합니다. 이미 있는 컬럼에 `ADD COLUMN`을 다시 실행하면 중복 컬럼 오류가 발생합니다.

| 안내 파일 | 확인 사항 |
| --- | --- |
| [고객 개인정보 처리](docs/sql/customer_privacy_migration.sql) | `deleted_at`, `anonymized_at`, `phone` NULL 허용 |
| [다중 매장 소유](docs/sql/store_multi_owner_migration.sql) | `store.member_id` 단일 UNIQUE 제거, 일반 인덱스·외래키 유지 |
| [매장 폐업](docs/sql/store_closure_migration.sql) | `store.closed_at` |

운영은 `SPRING_PROFILES_ACTIVE=prod`와 `DB_URL` 등 환경변수를 지정합니다.
`prod`는 `ddl-auto=validate`이므로 DB 스키마를 미리 준비해야 합니다.
일부 업무 Controller에 `@Profile("dev")`가 남아 있으므로 운영 프로필의 화면 제공·권한 검증 후 배포해야 합니다.

3개월 경과 개인정보 제거 배치는 스키마·처리 대상 확인 후 명시적으로 활성화합니다.
폐업 시 즉시 정보 제거는 배치 활성화 여부와 별개입니다.
상세 설정: [환경설정 안내](docs/ENVIRONMENT_SETUP.md), [개발·운영 환경](docs/개발_운영_환경.md), [서비스 실행 설정](deploy/crm.service).

## 테스트·빌드

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat bootJar
```

macOS / Linux:

```bash
./gradlew test
./gradlew bootJar
```

실행 JAR은 `build/libs/crm.jar`입니다. 동일한 환경변수를 제공한 상태에서 실행합니다.

```bash
java -jar build/libs/crm.jar
```

테스트는 입력 검증·전화번호 정규화·중복 검사, 매장 격리, 고객 삭제·정보 제거와 이력 보존,
ADMIN 처리, 홈 집계, 폐업·본인 정보 수정, 역할별 화면 렌더링, AI 사용 제한 등을 다룹니다.
H2 자동 테스트와 실제 MySQL·브라우저 검증은 범위가 다릅니다.
결과는 `build/reports/tests/test/index.html`에서 확인합니다.

## 프로젝트 구조

```text
src/main/java/com/team1/crm/
  auth/             회원·인증·매장·본인 정보
  customer/         고객 CRUD·검색·개인정보 처리
  admin/            플랫폼 계정·매장 관리·집계
  home/             사장님 홈 요약
  loyalty/          방문·포인트·쿠폰
  analytics/        지표·RFM·AI
  common/           공통 설정·DTO
  infrastructure/   파일·이미지 관련 구성
src/main/resources/
  templates/        Thymeleaf 업무 화면
  static/           정적 자원 및 디자인 참고 화면
src/test/           자동 테스트
docs/               설계·정책·검증 기록·SQL
deploy/             배포 설정·스크립트
```

## 통합·검증이 남은 범위

- 방문·포인트·쿠폰 서비스와 API는 담당 브랜치에서 통합했습니다. 실제 거래 호출·동시성·롤백과 고객 상세 화면 연동은 별도 검증 대상입니다.
- 아이디 찾기·비밀번호 재설정·비밀번호 조합·이메일 인증 추가 요구와 ADMIN 공통 인증 정책 연동은 담당자 작업 및 통합 확인 대상입니다.
- 승인·반려·정지·재활성화는 범위·상태 모델을 추가 협의합니다.
- 운영 프로필, 실보안 필터·CSRF, 폐업과 신규 거래의 동시 처리, 배포 스모크 테스트를 별도로 확인합니다.
- AI 결과는 외부 모델의 가용성·요청 제한에 영향을 받습니다.

## 설계·검증 문서

- [공식 설계](docs/architecture/00_SaaS_공식설계_v1.md) / [ERD](docs/architecture/04_SaaS_ERD.md) / [API 설계](docs/architecture/05_SaaS_API.md)
- [고객 삭제 정책·수동 검증](docs/architecture/07_고객_삭제_정책.md)
- [ADMIN 관리 검증](docs/architecture/08_ADMIN_매장_관리_검증.md) / [홈 집계](docs/architecture/09_홈_요약_연동.md)
- [폐업 정책·검증](docs/architecture/10_매장_폐업_정책.md) / [본인 정보·매장 수정](docs/architecture/11_본인_정보와_매장_수정.md)
- [Git 커밋 규칙 초안](docs/team-proposals/common/04_GIT_커밋_규칙.md)

설계의 목표 범위와 실제 구현·검증 범위는 구분합니다. 과거 Starter·Reference 문서에는 초기 설명이 남아 있을 수 있습니다.
