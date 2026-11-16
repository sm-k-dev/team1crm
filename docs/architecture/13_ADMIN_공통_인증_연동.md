# ADMIN 생성 계정의 공통 인증 정책 연동

작성자: 김순민 / 작성일: 2026-11-16

## 구현 범위

- develop PR #25의 PasswordPolicy·비밀번호 재설정·닉네임 중복 확인 기능을 fast-forward로 가져왔다.
- ADMIN 초기 비밀번호도 일반 가입·재설정과 같은 PasswordPolicy.validate를 사용한다. Form 검증은 SharedPasswordValidator에서 공통 정책을 호출하며, Service 직접 호출도 다시 검사한다.
- 비밀번호는 8~20자, 영문·숫자·특수문자 각각 포함, 공백 금지이다. 화면 안내·최대 길이와 오류 표시를 맞췄다. 기존 계정의 비밀번호를 일괄 변경하지 않는다.
- ADMIN 이메일은 EmailAddress.normalize로 저장하고, MemberRepository의 정규화 조회를 재사용해 Google 계정까지 중복으로 거절한다.
- ADMIN 생성 회원은 provider 없는 USER이며 기존 PasswordResetService의 대상이다. 별도의 ADMIN 전용 재설정 경로·메일 발송 코드를 만들지 않는다.
- 이름은 기존 ADMIN 이름 정책을 유지한다. 가입 화면의 닉네임 길이와 전체 회원 이름 정책 통일은 별도 협의한다.
- 아이디 찾기 및 가입 이메일 인증은 이번 코드에서 구현 완료로 확인하지 않았다. 재설정 메일의 이메일 소유 확인과 가입 이메일 인증은 다른 기능이다. B-012 전체 완료와 구분한다.

## 실행 설정

- MAIL_USERNAME: 메일 발신 계정. MAIL_APP_PASSWORD: 발신 Gmail의 앱 비밀번호. Google OAuth Client Secret과 다른 값이다.
- APP_BASE_URL: 이메일에 넣을 실제 서비스 주소. dev는 localhost:8082 기본값, prod는 필수 환경변수이다. 팀원에게 링크를 보낼 때 localhost가 수신자 자신의 PC를 가리킨다는 점에 유의한다.
- MAIL_USERNAME·MAIL_APP_PASSWORD는 공통 설정에서 참조한다. 값을 실제 실행 환경에 제공하고 비밀값을 저장소에 넣지 않는다.
- password_reset_tokens 테이블이 필요하다. dev는 ddl-auto=update, prod는 validate이므로 운영 스키마는 인프라 담당과 먼저 적용한다. 회원 ID 타입·외래키와 엔티티 정의를 대조한다.
- 이번 작업에서 실제 SMTP 발송·실 DB 스키마 변경은 하지 않았다.

## 검증 및 다음 확인

- AdminAuthPolicyTest 12건: Form/Service 공통 비밀번호 조건, Google 이메일 변형 중복3종, ADMIN 생성 계정의 재설정 발급·변경·링크 재사용 거부, Google 전용 계정 발급 제외를 H2와 실제 Service로 확인했다.
- 테스트는 SMTP 의존성을 mock으로 제공한다. 테스트 트랜잭션을 롤백하므로 afterCommit 메일 발송은 실행하지 않는다.
- 전체 테스트 180건 통과(실패·오류 0). 실제 Google·SMTP·브라우저·EC2 통합 검증 완료를 뜻하지 않는다.
- 수동 확인: ADMIN 초기 비밀번호 오류 시 필드 안내·비밀번호 비움 → 정상 생성 → 해당 이메일의 재설정 메일 요청 → 수신 링크로 변경 → 새 비밀번호 로그인 성공·이전 비밀번호 실패·링크 재사용 실패.
- Google 전용 계정에는 비밀번호 재설정 링크를 보내지 않으며 일반 계정으로 전환하지 않는다.
