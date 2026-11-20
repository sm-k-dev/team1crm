/**
 * 고객 검색과 페이지 이동을 JSON API에 연결한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-20
 */
(() => {
    "use strict";

    const directory = document.getElementById("customerDirectory");

    if (!directory) {
        return;
    }

    const searchForm = document.getElementById("customerSearchForm");
    const keywordInput = document.getElementById("keyword");
    const searchButton = document.getElementById("customerSearchButton");
    const resetButton = document.getElementById("customerSearchReset");

    const tableBody = document.getElementById("customerTableBody");
    const customerCount = document.getElementById("customerCount");
    const status = document.getElementById("customerListStatus");

    const pagination = document.getElementById("customerPagination");
    const pageLabel = document.getElementById("customerPageLabel");
    const previousButton = document.getElementById("customerPrevious");
    const nextButton = document.getElementById("customerNext");

    const storeId = directory.dataset.storeId;

    let currentPage = Number(directory.dataset.page);
    let currentKeyword = keywordInput.value.trim();
    let hasPrevious = !previousButton.disabled;
    let hasNext = !nextButton.disabled;
    let loading = false;

    /**
     * 값을 HTML 코드가 아닌 일반 텍스트로 셀에 넣는다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-20
     */
    function appendCell(row, value) {
        const cell = document.createElement("td");
        cell.textContent = value ?? "-";
        row.append(cell);
        return cell;
    }

    /**
     * 고객 목록과 인원수 페이지 표시를 갱신한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-20
     */
    function renderCustomers(data) {
        const fragment = document.createDocumentFragment();

        for (const customer of data.customers) {
            const row = document.createElement("tr");

            const nameCell = document.createElement("td");
            const detailLink = document.createElement("a");

            const detailUrl = new URL (
                `${directory.dataset.customerUrl}/${customer.id}`,
                window.location.origin
            );

            detailUrl.searchParams.set("storeId", data.storeId);

            detailLink.href = detailUrl.href;
            detailLink.textContent = customer.name;

            nameCell.append(detailLink);
            row.append(nameCell);

            appendCell(row, customer.maskedPhone);

            appendCell(
                row,
                customer.birthMonth == null
                    ? "-"
                    : `${customer.birthMonth}월`
            );

            const marketingCell = document.createElement("td");
            const badge = document.createElement("span");

            badge.className = customer.agreeMarketing
                ? "badge bg-success"
                : "badge bg-secondary";

            badge.textContent = customer.agreeMarketing
                ? "동의"
                : "미동의";

            marketingCell.append(badge);
            row.append(marketingCell);

            appendCell(row, customer.createdDate);

            fragment.append(row);
        }

        if (data.customers.length === 0) {
            const row = document.createElement("tr");
            const cell = appendCell(
                row,
                data.keyword
                    ? "검색 결과가 없습니다."
                    : "고객이 없습니다."
            );

            cell.colSpan = 5;
            cell.className = "text-center text-muted";

            fragment.append(row);
        }

        tableBody.replaceChildren(fragment);

        customerCount.textContent = `${data.totalElements}명`;

        pagination.classList.toggle (
            "d-none",
            data.totalPages === 0
        );

        pageLabel.textContent =
            `${data.page + 1} / ${data.totalPages} 페이지`;

        currentPage = data.page;
        currentKeyword = data.keyword;
        hasPrevious = data.hasPrevious;
        hasNext = data.hasNext;
    }

    /**
     * 조회 중 중복 요청을 막고 페이지 버튼 상태를 반영한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-20
     */
    function setLoading(value) {
        loading = value;

        searchButton.disabled = value;
        resetButton.disabled = value;
        previousButton.disabled = value || !hasPrevious;
        nextButton.disabled = value || !hasNext;

        tableBody.setAttribute("aria-busy", String(value));
    }

    /**
     * 고객 API를 호출하고 성공했을 때만 목록과 주소를 갱신한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-20
     */
    async function loadCustomers(page, keyword) {
        if (loading) {
            return;
        }

        setLoading(true);
        status.className = "small text-muted";
        status.textContent = "고객 목록을 불러오는 중입니다.";

        try {
            const url = new URL(
                directory.dataset.apiUrl,
                window.location.origin
            );

            url.searchParams.set("storeId", storeId);
            url.searchParams.set("keyword", keyword);
            url.searchParams.set("page", page);

            // 서버 API로 요청하고 응답을 기다림.
            const response = await fetch(url, {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                },
                credentials: "same-origin",
                cache: "no-store"
            });

            if (response.status === 401 || response.redirected) {
                throw new Error(
                    "로그인이 만료되었을 수 있습니다. 새로고침 후 다시 로그인 해 주세요."
                );
            }

            if (response.status === 403) {
                throw new Error("이 매장의 고객을 조회할 권한이 없습니다.");
            }

            if (response.status === 409) {
                throw new Error("폐업 매장은 고객 관리를 이용할 수 없습니다.");
            }

            if (!response.ok) {
                throw new Error(`고객 목록 조회에 실패했습니다. (${response.status})`);
            }

            const contentType =
                response.headers.get("content-type") ?? "";

            if (!contentType.includes("application/json")) {
                throw new Error(
                    "JSON 응답을 받지 못 했습니다. 로그인 상태를 확인해 주세요."
                );
            }

            // 서버가 보낸 JSON을 JavaScript에서 사용할 객체로 읽는다.
            // fetch는 HTTP 오류 상태에서도 응답을 반환할 수 있으므로 response.ok도 확인해야 한다.
            const data = await response.json();

            renderCustomers(data);

            const pageUrl = new URL(window.location.href);

            pageUrl.searchParams.set("storeId", data.storeId);
            pageUrl.searchParams.set("page", data.page);

            if (data.keyword) {
                pageUrl.searchParams.set("keyword", data.keyword);
            } else {
                pageUrl.searchParams.delete("keyword");
            }

            window.history.replaceState(null, "", pageUrl);

            status.textContent =
                `고객 ${data.totalElements}명을 조회했습니다.`;
        } catch (error) {
            status.className = "small text-danger";
            status.textContent = error instanceof Error
                ? error.message
                : "고객 목록을 불러오지 못했습니다.";
        } finally {
            setLoading(false);
        }
    }

    // 검색할 때는 첫 페이지부터 조회한다.
    searchForm.addEventListener("submit", (event) => {
        event.preventDefault(); // 검색폼이 페이지 전체를 이동 시키는 기본 동작을 막는다.

        loadCustomers(0, keywordInput.value.trim());
    });

    // 초기화하면 검색어를 비우고 첫 페이지를 조회한다.
    resetButton.addEventListener("click", () => {
        keywordInput.value="";
        loadCustomers(0, "");
    });

    // 페이지 이동은 현재 표의 검색 조건을 유지한다.
    previousButton.addEventListener("click", () => {
        if (hasPrevious) {
            loadCustomers(currentPage - 1, currentKeyword);
        }
    });

    nextButton.addEventListener("click", () => {
        if (hasNext) {
            loadCustomers(currentPage + 1, currentKeyword);
        }
    });
})();