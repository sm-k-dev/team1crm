# Starter 화면 ↔ Java Controller ↔ Thymeleaf ↔ 정적 HTML 매핑

> 기준: **첨부된 `CRM_Starter(2).zip`의 `src/main/` 실제 파일 및 Controller 소스**를 대조한 결과입니다. `build/`는 생성 결과물이므로 매핑 기준에서 제외했습니다. 이 문서는 파일/어노테이션 확인 결과이며, 서버를 실행한 통합 테스트 결과는 아닙니다.

## 경로를 한눈에 보는 표

- **Java Controller 경로**: `src/main/java/com/team1/crm/` 이하 상대 경로
- **Thymeleaf 경로**: `src/main/resources/templates/` 이하 상대 경로
- **정적 HTML 경로**: `src/main/resources/static/pages/` 이하 상대 경로 (예외: 최상위 `static/index.html`)
- 표의 `return`은 Controller의 뷰 이름이며, `templates/{return}.html`과 연결됩니다.
- 연결된 Controller에는 `@Profile("dev")`가 있으므로 **dev 프로필에서만 등록**됩니다.

| 화면 | Java Controller 파일 | 메서드 | 실제 GET URL | `return` | Thymeleaf HTML | 정적 HTML | 상태 |
|---|---|---|---|---|---|---|---|
| 홈 | `home/HomeController.java` | `home()` | `/`, `/index.html` | `"home/index"` | `templates/home/index.html` | `static/pages/home/index.html` | 연결됨 |
| 관리자 메인 | `admin/AdminViewController.java` | `index()` | `/admin` | `"admin/index"` | `templates/admin/index.html` | `static/pages/admin/index.html` | 연결됨 |
| 관리자 대시보드 | `admin/AdminViewController.java` | `dashboard()` | `/admin/dashboard` | `"admin/dashboard"` | `templates/admin/dashboard.html` | `static/pages/admin/dashboard.html` | 연결됨 |
| 사장님 계정 관리 | `admin/AdminViewController.java` | `owners()` | `/admin/owners` | `"admin/owners"` | `templates/admin/owners.html` | `static/pages/admin/owners.html` | 연결됨 |
| 매장 관리 | `admin/AdminViewController.java` | `stores()` | `/admin/stores` | `"admin/stores"` | `templates/admin/stores.html` | `static/pages/admin/stores.html` | 연결됨 |
| 분석 대시보드 | `analytics/DashboardViewController.java` | `dashboard()` | `/analytics/dashboard` | `"analytics/dashboard"` | `templates/analytics/dashboard.html` | `static/pages/analytics/dashboard.html` | 연결됨 |
| 로그인 | `auth/MemberViewController.java` | `login()` | `/login` | `"auth/login"` | `templates/auth/login.html` | `static/pages/auth/login.html` | 연결됨 |
| 회원가입 | `auth/MemberViewController.java` | `signup()` | `/signup` | `"auth/signup"` | `templates/auth/signup.html` | `static/pages/auth/signup.html` | 연결됨 |
| 고객 목록 | `customer/CustomerViewController.java` | `list()` | `/customers` | `"customer/list"` | `templates/customer/list.html` | `static/pages/customer/list.html` | 연결됨 |
| 매장 설정 | `auth/StoreSettingsViewController.java` | `없음` | 없음 | 없음 | `templates/auth/store-settings.html` | `static/pages/auth/store-settings.html` | 미연결: Controller TODO |
| 고객 상세 | `customer/CustomerViewController.java` | `없음` | 없음 | 없음 | `templates/customer/detail.html` | `static/pages/customer/detail.html` | 미연결: ID/업무 로직 필요 |
| 오류 페이지 | 없음 | `없음` | 없음 | 없음 | `templates/error.html` | `static/pages/error.html` | 미연결: 정적/템플릿 파일만 존재 |

### 추가 정적 진입 페이지

| 정적 URL | 실제 파일 | 설명 |
|---|---|---|
| `/index.html` | `src/main/resources/static/index.html` | 별도의 정적 시작 페이지가 존재함. 동시에 `HomeController`가 `/index.html`을 매핑하므로 **dev에서는 Controller가 우선 처리**함 |

### Thymeleaf Fragment (독립 페이지로 연결하지 않음)

| 파일 | Java Controller | 상태 |
|---|---|---|
| `templates/loyalty/_visit.html` | 없음 | 방문 기능에서 포함할 조각 템플릿; 미연결 |
| `templates/loyalty/_point.html` | 없음 | 포인트 기능에서 포함할 조각 템플릿; 미연결 |
| `templates/loyalty/_coupon.html` | 없음 | 쿠폰 기능에서 포함할 조각 템플릿; 미연결 |

## 현재 Controller 중 화면 연결과 무관한 TODO 파일

다음 파일은 ZIP에 있지만 **Controller 어노테이션 및 GET 매핑이 없는 스켈레톤**입니다. 화면 URL로 오해하지 마세요.

| Java 파일 | 현재 상태 |
|---|---|
| `auth/MemberApiController.java` | TODO, API 미구현 |
| `auth/StoreApiController.java` | TODO, API 미구현 |
| `customer/CustomerApiController.java` | TODO, API 미구현 |
| `analytics/ai/AiApiController.java` | TODO, API 미구현 |
| `infrastructure/storage/FileApiController.java` | TODO, API 미구현 |
| `auth/StoreSettingsViewController.java` | TODO, 화면 미연결 |

## 상태 해석과 주의사항

1. **연결됨**은 `@GetMapping`과 `return`으로 Thymeleaf 템플릿을 찾을 수 있도록 코드가 작성됐다는 뜻입니다. 로그인, DB 조회, CRUD, 권한 검증이 구현됐다는 뜻은 아닙니다.
2. **미연결** 화면은 템플릿/정적 HTML 파일이 있어도 대응하는 MVC GET 매핑이 확인되지 않았다는 뜻입니다. 실제 로직이 필요한 상세/설정 화면은 임의로 연결하지 않았습니다.
3. 정적 미리보기는 `/pages/...` URL로 접근하고, Thymeleaf 렌더링은 Controller URL(`/customers` 등)로 접근합니다. 둘은 다른 경로입니다.
4. `application.properties`에서 기본 프로필은 `dev`, 포트는 `8082`입니다. `application-dev.properties`에서 Thymeleaf는 활성화되어 있습니다. 실제 실행은 다른 설정·의존성에 영향을 받을 수 있습니다.
5. 기존 문서의 “서버 기능 미구현”은 **업무 기능 기준**으로는 여전히 맞지만, 현재 ZIP에는 위 9개 화면의 **MVC 라우팅 코드가 존재**하므로 둘을 구분해 표시했습니다.
