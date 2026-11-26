# Thymeleaf 업무 화면과 정적 미리보기

2026-11-26 현재 소스 기준입니다. 화면 경로는 [화면 매핑](06_화면_Java_매핑.md)을 참고합니다.

## 실제 업무 화면

Controller가 뷰 이름을 반환하면 `classpath:/templates/`의 해당 HTML을 렌더링합니다.

- `templates/admin/`, `auth/`, `home/`, `customer/`, `analytics/`: 관리자·인증·매장·고객·분석 업무 화면.
- `templates/loyalty/`: 고객 상세 화면에 포함되는 방문·포인트·쿠폰 fragment. 독립 GET 페이지가 아닙니다.
- `templates/fragments/navigation.html`: 공통 메뉴와 매장 선택·권한별 화면 구성.
- `static/css/`, `static/js/`: 실제 화면에서도 사용하는 정적 자원.

고객 목록·상세·수정, 매장 설정과 분석 등에는 Controller와 모델 연동 코드가 있습니다. 주요 업무 화면은 dev/prod에서 제공하며, 코드 연결과 실제 DB·브라우저·보안 필터 검증은 구분합니다.

## 정적 미리보기

`static/pages/`는 디자인 참고용 복사본이며 실제 업무 화면의 정본은 `templates/`입니다. `/pages/...` 경로는 정적 자원 경로이고, `/customers` 등 MVC 경로와 다릅니다. 미리보기 접근도 현재 보안 설정의 영향을 받습니다.

CSS·JavaScript의 `/css/...`, `/js/...` 절대경로는 서버 루트에서 제공되는 것을 전제로 합니다. 파일을 직접 열면 자원 연결이 달라질 수 있습니다.

## 다중 매장과 권한

사장님 역할의 실제 enum은 USER입니다. 소유 매장 목록에서 매장을 선택하며 서버는 요청 storeId의 소유권을 확인합니다. 관리자 메뉴와 사장님 메뉴는 구분합니다. 화면에서 메뉴를 숨기는 것만으로 인가를 대신하지 않습니다. 폐업 매장은 과거 통계 조회와 업무 변경 제한 정책을 따릅니다.
