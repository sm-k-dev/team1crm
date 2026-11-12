/* 매장 관리 화면의 등록 요청을 처리한다. 기존 황현우 작성 코드 재사용.
 * 화면 이동: 김순민 / 작성일: 2026-11-12 */
/*
     * + 매장 등록 JavaScript
     * 역할: 매장 등록 폼의 입력값을 검사하고
     *       Fetch API를 통해 서버에 매장 등록 요청을 전송합니다.
     *       서버 응답에 따라 성공 또는 오류 메시지를 표시합니다.
     */
    document.addEventListener("DOMContentLoaded", function () {

        // 1. 매장 등록 폼 가져오기
        const storeForm = document.getElementById("storeForm");

        // 로그인하지 않아 폼이 없으면 실행하지 않음
        if (!storeForm) {
            return;
        }

        // 2. 필요한 HTML 요소 가져오기
        const storeMsg = document.getElementById("storeMsg");
        const submitBtn = document.getElementById("storeSubmitBtn");

        // 3. 매장 등록 버튼을 누르면 실행
        storeForm.addEventListener("submit", async function (event) {

            // 브라우저의 기본 폼 제출 방지
            event.preventDefault();

            // 4. 사용자가 입력한 데이터 가져오기
            const storeData = {
                name: document.getElementById("sName").value.trim(),
                category: document.getElementById("sCategory").value.trim(),
                address: document.getElementById("sAddress").value.trim(),
                pointRate: Number(document.getElementById("sRate").value)
            };

            // 5. 입력값 검사
            // 매장 이름이 비어 있으면 등록 중단
            if (!storeData.name) {
                showMessage("매장 이름을 입력해주세요.", "danger");
                return;
            }

            // 적립률이 정수가 아니거나 0~20 범위를 벗어나면 등록 중단
            if (!Number.isInteger(storeData.pointRate) ||
                storeData.pointRate < 0 ||
                storeData.pointRate > 20) {

                showMessage("적립률은 0~20 사이의 정수로 입력해주세요.", "danger");
                return;
            }

            // 중복 클릭 방지를 위해 등록 버튼 비활성화
            submitBtn.disabled = true;
            submitBtn.textContent = "등록 중...";

            try {

                // 6. Spring Boot API 호출

                // HTML에서 CSRF 토큰 가져오기
                const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

                // 요청 헤더 구성
                const headers = {
                    "Content-Type": "application/json"
                };

                // CSRF 토큰이 있으면 요청 헤더에 추가
                if (csrfToken && csrfHeader) {
                    headers[csrfHeader] = csrfToken;
                }

                // 매장 등록 API 호출
                // 입력한 매장 정보를 JSON으로 변환하여 POST 요청
                const response = await fetch("/api/stores", {
                    method: "POST",
                    headers: headers,
                    credentials: "same-origin",
                    body: JSON.stringify(storeData)
                });

                // 7. 서버 응답 읽기
                const responseText = await response.text();

                // 서버 응답 데이터를 저장할 객체
                let result = {};

                // JSON 응답이면 객체로 변환하고, 아니면 메시지로 저장
                try {
                    result = JSON.parse(responseText);
                } catch (e) {
                    result = { message: responseText };
                }

                // 8. 등록 실패
                if (!response.ok) {

                    // 인증되지 않은 경우
                    if (response.status === 401) {
                        showMessage("로그인이 필요합니다.", "danger");

                        // 접근 권한 또는 보안 검증에 실패한 경우
                    } else if (response.status === 403) {
                        showMessage("매장 등록 권한이 없거나 보안 검증에 실패했습니다.", "danger");

                        // 그 외 오류가 발생한 경우
                    } else {
                        showMessage(
                            result.message || "매장 등록에 실패했습니다.",
                            "danger"
                        );
                    }

                    return;
                }

                // 9. 등록 성공
                showMessage("매장이 정상적으로 등록되었습니다.", "success");

                // 입력 폼 초기화
                storeForm.reset();

                // 내 매장 정보 페이지로 이동
                window.location.href = "/store-settings";

            } catch (error) {

                // 서버 통신 오류 발생 시 콘솔에 기록
                console.error("매장 등록 오류:", error);

                // 사용자에게 오류 메시지 표시
                showMessage(
                    "서버와 통신하는 중 오류가 발생했습니다.",
                    "danger"
                );

            } finally {

                // 등록 성공 또는 실패 후 버튼 상태 복구
                submitBtn.disabled = false;
                submitBtn.innerHTML =
                    '<i class="bi bi-check-circle me-1"></i> 등록';
            }
        });

        /*
         * + 매장 등록 결과 메시지 출력
         * 역할: 성공 또는 실패 메시지에 맞는 Bootstrap 스타일을 적용하고,
         *       사용자에게 결과를 표시합니다.
         */
        // 10. 성공 / 오류 메시지 출력
        function showMessage(message, type) {

            storeMsg.className = "alert alert-" + type;
            storeMsg.textContent = message;
        }

    });
