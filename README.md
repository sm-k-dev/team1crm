# CRM Team Starter — 팀 배포용

**JDK 25 / Spring Boot 4.0.0** 기반 프로젝트 구조 및 프론트 정적 디자인 미리보기입니다.

- Java 패키지/클래스 이름만 보존; 구현 로직은 TODO로 비워둠
- Thymeleaf 미사용; `src/main/resources/static/pages/` 아래 정적 HTML 화면
- CSS, Bootstrap 디자인, 메뉴 이동 유지; 로그인·저장·조회·AI·DB 등 기능은 미구현
- UI는 화면 이동 확인용이며 서버 인증·API·DB 기능은 제공하지 않음
- `/index.html` 또는 `/pages/...` 경로로 접근
- 빌드: `./gradlew bootRun` (JDK 25 필요)
- 기본 프로필은 `dev`이며 `SERVER_PORT`, `DB_USERNAME`, `DB_PASSWORD`와 접속 가능한 MySQL이 필요합니다. `DB_URL`은 개발 환경에서만 로컬 기본값을 제공합니다.
- 운영은 `SPRING_PROFILES_ACTIVE=prod`로 선택하며 `DB_URL`도 필수입니다. 운영 DB 스키마는 미리 준비해야 합니다 (`ddl-auto=validate`).

## 구조 검증

`docs/architecture/01_프로젝트_패키지_구조.md`의 패키지와 실제 디렉터리 구조가 일치합니다. `docs/STARTER_CLASS_INVENTORY.md` 참조.

## 주의

`docs/architecture/03_Thymeleaf_화면_구조.md` 등 기존 설계 문서의 Thymeleaf 및 API 구현 설명은 **원본 Reference 기준**입니다. Starter의 현재 동작을 의미하지 않습니다. 팀 합의 후 문서를 갱신하세요.

## 이번 추가 사항
- `src/main/resources/templates/loyalty/`의 방문·포인트·쿠폰 Thymeleaf fragment **스켈레톤** 3개. 정적 페이지에 자동 반영되지 않으며 서버 기능은 미구현입니다. [연결 안내](docs/architecture/03_Thymeleaf_화면_구조.md).
- [Git 커밋·브랜치·PR 규칙](docs/GIT_COMMIT_CONVENTION.md) — 팀 검토용.
- [현재 구현 격차](docs/IMPLEMENTATION_GAP.md)와 [변경 기록](docs/CHANGELOG_BUNDLE.md).


## 2026-11-05 정정
- OWNER 1명은 여러 STORE를 소유할 수 있습니다 (1:N). 상세 설계: `docs/architecture/04_SaaS_ERD.md`.
- `templates/`에 Thymeleaf 전환용 화면을 배치했습니다. `static/pages/`는 스타일 확인용 미리보기 복사본입니다.
- 모든 정적 화면에 누락된 `/css/modern-saas.css`, `/js/modern-saas.js` 링크를 복구했습니다.
- Thymeleaf Controller, 매장 선택 및 매장별 인가는 **미구현**입니다.

## 환경변수 (팀 공통)

[설정 가이드](docs/ENVIRONMENT_SETUP.md) 및 [예시 변수 목록](.env.example)을 참조하세요. 실제 비밀값은 Git에 올리지 않습니다.
