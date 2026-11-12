
package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     *      Store 객체를 생성하여 데이터베이스에 저장합니다.
     *
     * @param email 로그인한 회원의 이메일
     * @param name 매장 이름
     * @param category 매장 업종
     * @param address 매장 주소
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

        // 같은 사장님의 영업 매장 이름 중복을 확인한다. 작성자: 김순민 / 작성일: 2026-11-12
        if (storeRepository.existsByOwner_IdAndNameAndClosedAtIsNullAndIdNot(member.getId(), name.strip(), 0L)) {
            throw new IllegalArgumentException("내 다른 영업 매장에서 사용 중인 이름입니다.");
        }
        // 4. 검증된 회원 정보와 매장 정보를 이용하여 Store 객체 생성
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
     *      해당 회원이 소유한 모든 매장 목록을 반환합니다.
     *
     * @param email 로그인한 회원의 이메일
     * @return 해당 회원이 소유한 매장 목록
     */
    @Transactional(readOnly = true)  // 조회 전용 트랜잭션으로 실행
    public List<Store> getMyStores(String email) {

        // 1. 로그인한 회원을 이메일로 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        // 회원 정보가 존재하지 않으면 예외 발생
                        new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
                );
        // 2. 조회한 회원의 ID를 이용하여 소유한 매장 목록 조회
        // 3. 조회된 매장 목록을 List<Store> 형태로 반환
        return storeRepository.findByOwnerId(member.getId());
    }

    /**
     * 로그인한 회원이 소유한 매장을 조회한다.
     * 계정·매장 승인 및 정지 상태 검증은 상태 모델 구현 후 추가해야 한다.
     *
     * @param memberId 인증 정보에서 확인한 로그인 회원의 ID
     * @param storeId 조회할 매장의 ID
     * @return 로그인 회원이 소유한 매장
     * @throws IllegalArgumentException ID가 잘못되었거나 소유 매장을 찾을 수 없는 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Transactional(readOnly = true)
    public Store findOwnedStore(Long memberId, Long storeId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("회원 정보를 확인해 주세요.");
        }

        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("매장 정보를 확인해 주세요.");
        }

        Store store = storeRepository.findByIdAndOwner_Id(storeId, memberId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "접근 가능한 매장을 찾을 수 없습니다."
                        )
                );
        store.requireOpen();
        return store;
    }
}
