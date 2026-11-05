# Thymeleaf 목표 화면 구조 및 정적 미리보기

## 개발 목표: `src/main/resources/templates/`

Thymeleaf의 기본 템플릿 위치는 `classpath:/templates/`입니다. Controller가 뷰 이름(예: `customer/list`)을 반환하면 `templates/customer/list.html`을 렌더링합니다.

- `templates/{admin,auth,home,customer,analytics}/`: 서버 렌더링 전환 대상 화면
- `templates/loyalty/`: 방문·포인트·쿠폰 fragment (기존 파일 유지)
- `static/css/`, `static/js/`: CSS 및 JS
- `static/pages/`: 현재 독립 실행 가능한 정적 미리보기 **복사본**. 실제 Thymeleaf 운영 경로가 아닙니다.

현재 프로젝트는 Controller 및 모델 연동이 완료되지 않은 **Starter**입니다. `templates/`에 HTML이 존재해도 Controller가 없으면 서버 렌더링 URL이 동작하지 않습니다. 정적 페이지와 템플릿은 이후 기능 구현 중 중복 관리하지 말고 템플릿을 정본으로 통합해야 합니다.

## 메뉴 점검
모든 정적 미리보기 페이지에 `/css/modern-saas.css`와 `/js/modern-saas.js`를 연결했습니다. 이 경로는 Spring Boot 서버 루트(`/`)에서 제공될 때 유효합니다. HTML 파일을 `file://`로 직접 열면 절대경로가 동작하지 않습니다.

## 다중 매장 UI
OWNER는 매장 선택 UI가 필요하며, 서버가 선택된 매장에 대한 소유권을 매 요청 검증해야 합니다. 현재 메뉴는 시각적 스켈레톤이며 매장 선택 및 권한별 노출은 아직 구현되지 않았습니다.
