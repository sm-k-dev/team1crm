# 개발 / 운영 환경 설정

## 설정 구조
- `application.properties`: 공통 설정. 프로필 미지정 시 `dev` 사용.
- `application-dev.properties`: 로컬 MySQL 주소 기본값, 테이블 자동 변경(`update`), 화면 캐시 비활성화.
- `application-prod.properties`: DB 주소 필수, 스키마 검증(`validate`), 화면 캐시 활성화, 오류 응답의 내부 정보 비공개.
- Spring AI 모델은 두 프로필 모두 비활성화되어 있습니다. JWT/AI/S3 변수는 아직 기능에 연결되지 않았습니다.

DB/JPA 자동 구성은 활성화되어 있으므로 개발 환경에도 접속 가능한 MySQL이 필요합니다.

## 한 곳에서 환경 선택
실행 환경의 `SPRING_PROFILES_ACTIVE`를 `dev` 또는 `prod`로 지정하고 재시작합니다. 소스 수정이나 재빌드는 필요 없습니다.

| 환경변수 | 개발 | 운영 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (기본값) | `prod` 명시 |
| `SERVER_PORT` | 필수, 예: `8082` | 필수, 예: `8082` |
| `DB_URL` | 미지정 시 로컬 `crm` DB | 필수 |
| `DB_USERNAME` | 필수 | 필수 |
| `DB_PASSWORD` | 필수 | 필수 |

프로필 선택은 DB 계정을 자동 교체하지 않습니다. 개발 PC와 운영 서버에 각각 해당 환경의 DB 값을 설정하세요. 실행 인자의 `--spring.profiles.active` 등으로 프로필을 중복 지정하지 마세요.

## IntelliJ 로컬 실행
1. `Run` → `Edit Configurations...` → `CrmApplication`을 선택합니다.
2. `Environment variables`에 `.env.example`의 변수명으로 본인의 값을 입력합니다.
3. `SPRING_PROFILES_ACTIVE=dev`와 개발용 DB 연결 정보를 확인하고 실행합니다.

`.env` 파일은 자동으로 로드되지 않습니다. IDE 또는 운영체제 환경변수로 주입하세요. 실제 비밀값과 개인 IDE 설정은 Git에 올리지 않습니다.

## 운영 서버 (systemd)
1. `deploy/crm.env.example`을 서버의 `/home/ubuntu/crm/crm.env`로 복사합니다.
2. 이 파일 한 곳에 `SPRING_PROFILES_ACTIVE=prod`, 포트, 실제 운영 DB 주소와 계정을 설정합니다. `chmod 600 /home/ubuntu/crm/crm.env`로 권한을 제한하고 서비스 사용자 `ubuntu`가 읽을 수 있도록 소유자를 설정합니다.
3. 운영 DB 스키마를 미리 준비합니다. `validate`는 테이블을 생성하거나 변경하지 않습니다.
4. `deploy/crm.service`를 `/etc/systemd/system/crm.service`에 설치하고 JAR를 `/home/ubuntu/crm/crm.jar`에 배치합니다.
5. 서비스 파일 설치·수정 시 `sudo systemctl daemon-reload` 후 `sudo systemctl restart crm`을 실행합니다. `crm.env` 값만 바꾼 경우에는 재시작만 하면 됩니다.
6. `sudo systemctl status crm`과 `sudo journalctl -u crm -n 100 --no-pager`로 프로필과 기동 결과를 확인합니다.

`crm.service`는 `EnvironmentFile`로 환경 파일을 읽으며 프로필을 별도로 지정하지 않습니다. 실제 환경 파일과 비밀값은 Git이나 배포 ZIP에 포함하지 않습니다.

## 현재 구현 범위
주요 화면 Controller와 미리보기 보안 설정은 `@Profile("dev")`이므로 운영에서는 등록되지 않습니다. 운영용 인증/인가 및 업무 기능은 아직 미완성입니다. 운영 배포 전 해당 기능 구현과 DB 연결·화면·인증 검증이 필요합니다.
