# 팀 공통 환경변수 설정 가이드 (Starter)

## 핵심 원칙
- **환경변수 이름은 팀 전체가 통일**하고 실제 값은 각자 로컬 또는 서버에서 관리합니다.
- `.env.example`, `deploy/crm.env.example`에는 **가짜 예시 값만** 넣습니다. 실제 API Key, JWT 비밀값, DB 비밀번호는 절대 커밋하지 않습니다.
- .gitignore 파일에 실제 .env 및 로컬 설정 파일, .gradle/, build/, .idea/ 같은 개인 PC의 빌드·IDE 생성 파일은 제외
- **현재 Starter는 UI 미리보기 단계**입니다. `application.properties`에서 DB/JPA 자동 구성을 제외했고 `application-dev.properties`에서 Spring AI 모델을 비활성화했습니다. 따라서 DB/AI/JWT/S3 변수는 **향후 구현을 위한 이름 예약**이며 지금 값을 넣어도 기능이 활성화되지는 않습니다.

## IntelliJ 설정 (권장: 로컬 개발)
1. `Run` → `Edit Configurations...` → **Application** → `CrmApplication` 선택.
2. `Environment variables`에서 `SPRING_PROFILES_ACTIVE=dev;SERVER_PORT=8082` 설정 (IntelliJ 편집기에서는 항목별 입력 가능).
3. 나중에 기능을 연결할 때 `.env.example`에 정의된 **같은 변수명**으로 본인의 실제 값을 입력합니다. 비밀값을 Git에 저장하거나 단체 채팅으로 보내지 않습니다.
4. `Run`으로 실행합니다. 내장 Tomcat 사용 프로젝트이므로 **별도의 Tomcat Server 구성에 넣는 것이 아닙니다.**
5. IDE 실행 설정을 Git에 공유하지 않습니다. 팀 공통 설정은 이 문서와 `.env.example`을 기준으로 합니다.

> `.env.example`을 `.env`로 복사하는 것만으로는 Spring Boot가 자동으로 `.env`를 읽지 않습니다. `.env` 로더를 별도로 도입하지 않았다면 IntelliJ 환경변수 또는 운영체제 환경변수로 설정하세요.

## 환경변수 목록
| 변수 | 용도 | 현재 UI Starter에서 사용 여부 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring 프로필 (`dev`/`prod`) | 예 (Spring 기본 기능) |
| `SERVER_PORT` | HTTP 포트 (`8082`) | 예 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 추후 MySQL 연결 | 아니오 |
| `JWT_SECRET` | 추후 JWT 서명 | 아니오 |
| `OPENROUTER_API_KEY`, `OPENROUTER_MODEL` | 추후 AI 연결 | 아니오 |
| `APP_STORAGE`, `S3_BUCKET`, `AWS_REGION` | 추후 파일 저장 | 아니오 |

    SPRING_PROFILES_ACTIVE=dev
    SERVER_PORT=8082
    DB_URL=jdbc:mysql://localhost:3306/crm
    DB_USERNAME=crm
    DB_PASSWORD=본인_DB_비밀번호

## 추후 기능 연결 시 (그때 적용할 예시)
DB/JPA 자동 구성 제외 설정을 제거하고 DB가 준비된 다음, **dev/prod 별** 설정을 다음처럼 연결합니다.

```properties
# Example ONLY; do not enable until DB/JPA integration is implemented
# application-dev.properties에 다음을 추가하면 됩니다.
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

AI 연동도 사용 모델과 Spring AI 2.x 설정을 검증한 뒤에 `OPENROUTER_API_KEY`를 참조하도록 구성해야 합니다. 현재 모델 비활성화 설정을 무작정 해제하지 마세요.

## 운영 배포
- `deploy/crm.env.example`을 참고해 **서버 외부**에 실제 환경 파일을 만들고 접근 권한을 제한합니다 (예: Linux `chmod 600`).
- 실행 프로세스(systemd 등)에 필요한 환경변수만 주입하고, 실제 비밀값은 Git 및 배포 ZIP에 포함하지 않습니다.
- 운영 환경에서는 인증/인가·DB·스토리지·AI 등 미구현 기능을 완성하고 검증한 뒤 배포해야 합니다. `prod` 프로필을 선택하는 것만으로 배포 가능한 앱이 되지는 않습니다.

## 커밋 전 점검
- `git status`와 `git diff --cached`에서 비밀번호·토큰·키 문자열이 없는지 확인합니다.
- 이미 추적 중인 파일은 `.gitignore` 추가만으로 Git 추적이 중단되지 않습니다. 실제 키가 커밋됐다면 즉시 키를 폐기/재발급하고 Git 이력 노출을 별도로 처리해야 합니다.
