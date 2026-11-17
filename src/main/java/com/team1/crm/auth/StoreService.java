
package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * + 매장 등록 및 조회 서비스
 * 역할: 로그인한 회원의 정보를 확인하고 새로운 매장을 등록하거나,
 *      해당 회원이 소유한 매장 목록을 조회하는 비즈니스 로직을 처리합니다.
 *      매장 이름과 포인트 적립률을 검증하고 Repository를 통해
 *      데이터베이스에 매장 정보를 저장하거나 조회합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */


@Service  // 비즈니스 로직을 처리하는 서비스 클래스로 등록
@RequiredArgsConstructor  // final 필드의 생성자를 자동 생성하여 의존성 주입
public class StoreService {
    // 매장 정보를 저장하고 조회하는 Repository
    private final StoreRepository storeRepository;
    // 회원 정보를 이메일로 조회하는 Repository
    private final MemberRepository memberRepository;

    /**
     * + 새로운 매장 등록
     * 역할: 로그인한 회원을 이메일로 조회하고 매장 정보를 검증한 뒤,
     * Store 객체를 생성하여 데이터베이스에 저장합니다.
     *
     * @param email     로그인한 회원의 이메일
     * @param name      매장 이름
     * @param category  매장 업종
     * @param address   매장 주소
     * @param pointRate 포인트 적립률
     * @return 데이터베이스에 저장된 Store 객체
     */
    @Transactional // 매장 등록 작업을 하나의 트랜잭션으로 처리
    public Store registerStore(
            String email,
            String name,
            String category,
            String address,
            int pointRate
    ) {

        // 1. 로그인한 회원을 이메일로 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        // 회원 정보가 존재하지 않으면 예외 발생
                        new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
                );

        // 2. 매장 이름이 null이거나 공백인지 확인
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("매장 이름을 입력해주세요.");
        }

        // 3. 포인트 적립률이 0~20% 범위인지 확인
        if (pointRate < 0 || pointRate > 20) {
            throw new IllegalArgumentException("적립률은 0~20% 사이여야 합니다.");
        }

        // 4. 매장 이름과 주소의 앞뒤 공백 제거
        String trimmedName = name.trim();
        String trimmedAddress =
                address == null ? null : address.trim();

        // 5. 동일 소유자의 매장 중복 등록 검사
        if (storeRepository.existsByOwner_IdAndNameAndAddress(
                member.getId(),
                trimmedName,
                trimmedAddress
        )) {
            throw new IllegalArgumentException(
                    "이미 등록된 매장입니다."
            );
        }
        //      기존     ->     변경
        // name.trim() ->	trimmedName
        // address ->	trimmedAddress            이름과 주소값의 공백을 날려 검사 -> 글자로 중복 판단
        // 6. 검증된 회원 정보와 매장 정보를 이용하여 Store 객체 생성
        Store store = new Store(
                member, // 매장 소유 회원
                name.trim(), // 매장 이름의 앞뒤 공백 제거
                category, // 매장 업종
                address, // 매장 주소
                pointRate // 포인트 적립률
        );

        // 5. StoreRepository를 이용하여 매장 정보를 DB에 저장
        // 저장된 Store 객체를 반환
        return storeRepository.save(store);
    }

    /**
     * + 로그인한 회원의 매장 목록 조회
     * 역할: 회원 이메일을 이용하여 회원 정보를 조회한 뒤,
     *       해당 회원이 소유한 모든 매장 목록을 반환합니다.
     *
     * @param email 로그인한 회원의 이메일
     * @return 해당 회원이 소유한 매장 목록
     *
     * 작성자: 황현우
     * 일시: 2026-11-09
     */
    @Transactional(readOnly = true)
    public List<Store> getMyStores(String email) {

        // 1. 로그인한 회원을 이메일로 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );

        // 2. 조회한 회원의 ID를 이용하여 소유한 매장 목록 조회
        // 3. 조회된 매장 목록을 List<Store> 형태로 반환
        return storeRepository.findByOwnerId(member.getId());
    }



    /**
     * + 로그인 회원의 소유 매장 조회 및 접근 권한 검증
     * <p>
     * 역할: 로그인한 회원의 ID와 요청한 매장 ID를 비교하여
     * 해당 회원이 소유한 매장에만 접근하도록 제한합니다.
     * <p>
     * [기존 기능]
     * - 로그인 회원 ID와 매장 ID의 유효성을 검사합니다.
     * - 매장 ID와 소유자 ID가 모두 일치하는 매장을 조회합니다.
     * - 본인 소유 매장이 아니면 접근을 거부합니다.
     * <p>
     * [2026-11-16 개선 사항]
     * - 기존에는 조회 실패 시 IllegalArgumentException으로 처리했습니다.
     * - 매장 자체가 존재하지 않는 경우 404 Not Found를 반환합니다.
     * - 매장은 존재하지만 다른 회원 소유인 경우 403 Forbidden을 반환합니다.
     * - 정상적인 본인 매장 접근은 기존과 동일하게 유지합니다.
     * <p>
     * 계정·매장 승인 및 정지 상태 검증은 상태 모델 구현 후 추가해야 합니다.
     *
     * @param memberId 인증 정보에서 확인한 로그인 회원의 ID
     * @param storeId  조회할 매장의 ID
     * @return 로그인 회원이 소유한 Store 객체
     * @throws IllegalArgumentException 회원 ID 또는 매장 ID가 유효하지 않은 경우
     * @throws ResponseStatusException 매장이 없으면 404, 다른 회원 소유이면 403
     *
     * 최초 작성자: 김순민
     * 최초 작성일: 2026-11-10
     * 수정자: 황현우
     * 수정일: 2026-11-16
     * 수정 내용: 매장 접근 오류를 HTTP 403과 404로 구분
     */
    @Transactional(readOnly = true)
    public Store findOwnedStore(Long memberId, Long storeId) {

        // 1. 회원 ID 검증
        // 회원 ID가 없거나 0 이하라면 잘못된 입력값으로 판단
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException(
                    "회원 정보를 확인해 주세요."
            );
        }

        // 2. 매장 ID 검증
        // 매장 ID가 없거나 0 이하라면 잘못된 입력값으로 판단
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException(
                    "매장 정보를 확인해 주세요."
            );
        }

        // 3. 본인 소유 매장 조회
        // 매장 ID와 회원 ID가 모두 일치해야 조회에 성공
        return storeRepository.findByIdAndOwner_Id(storeId, memberId)
                .orElseThrow(() -> {

                    // [2026-11-16 / 황현우]
                    // 4. 매장이 실제로 존재하는지 추가 확인
                    // 매장 자체가 없다면 404 Not Found 반환
                    if (!storeRepository.existsById(storeId)) {
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "매장을 찾을 수 없습니다."
                        );
                    }

                    // [2026-11-16 / 황현우]
                    // 5. 매장은 존재하지만 조회한 회원의 소유가 아님
                    // 접근 권한이 없으므로 403 Forbidden 반환
                    return new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "다른 회원의 매장에 접근할 권한이 없습니다."
                    );
                });
    }

    /**
     * 신규 거래(방문 등록·쿠폰 발급)를 하기 전에 매장 행을 잠그고 영업 중인지 확인한다.
     * 폐업 처리와 같은 매장 행을 잠그므로 폐업과 신규 거래가 동시에 실행되지 않는다.
     * 호출한 쓰기 트랜잭션에 합류하며, 잠금은 그 트랜잭션이 끝날 때 풀린다.
     * 요청 앞부분에서 이미 읽어 둔 매장 엔티티는 낡은 값일 수 있어서, DB의 최신 폐업 여부를 한 번 더 확인한다.
     *
     * @throws ResponseStatusException 매장이 없으면 404, 폐업 매장이면 409
     *
     * 작성자: 이재준
     * 작성일: 2026-11-17
     * 근거: docs/architecture/10_매장_폐업_정책.md (C 담당 신규 거래는 매장 잠금과 폐업 차단 적용)
     */
    @Transactional
    public void lockOpenStore(Long storeId) {
        Store store = storeRepository.findLockedById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."));
        store.requireOpen();
        if (storeRepository.existsByIdAndClosedAtIsNotNull(storeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "폐업 매장은 변경할 수 없습니다.");
        }
    }
}