
package com.team1.crm.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * + 매장 정보 데이터베이스 접근 Repository
 * 역할: Spring Data JPA를 이용하여 매장 정보를 저장, 조회,
 *      수정, 삭제하는 데이터베이스 접근 기능을 제공합니다.
 *      회원 ID를 기준으로 해당 회원이 소유한 매장 목록을 조회합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */

public interface StoreRepository extends JpaRepository<Store, Long> {
    /** 같은 소유자의 다른 영업 매장 이름 중복을 확인한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    boolean existsByOwner_IdAndNameAndClosedAtIsNullAndIdNot(Long ownerId, String name, Long id);
    /** 폐업·고객 쓰기를 직렬화하기 위해 매장을 잠근다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Store s where s.id = :id")
    Optional<Store> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    /** 이미 로딩한 엔티티와 별개로 DB의 최신 폐업 여부를 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    boolean existsByIdAndClosedAtIsNotNull(Long id);
    // JpaRepository<Store, Long>
    // Store : 데이터베이스의 매장 정보를 나타내는 Entity 클래스
    // Long  : Store Entity의 기본키(PK) 자료형
    // JpaRepository를 상속하여 save(), findById(), findAll(),
    // deleteById() 등의 기본 CRUD 메서드를 사용할 수 있습니다.

    /**
     * + 회원별 매장 목록 조회
     * 역할: 전달받은 회원 ID를 기준으로 해당 회원이
     *      소유한 모든 매장을 조회하여 List로 반환합니다.
     *
     * @param memberId 매장 소유자의 회원 ID
     * @return 해당 회원이 소유한 매장 목록
     */
    List<Store> findByOwnerId(Long memberId);

    /**
     * 매장 ID와 소유 회원 ID가 모두 일치하는 매장을 조회한다.
     *
     * @param storeId 조회할 매장의 ID
     * @param memberId 로그인한 회원의 ID
     * @return 조건에 맞는 매장. 일치하는 매장이 없으면 빈 Optional
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    Optional<Store> findByIdAndOwner_Id(Long storeId, Long memberId);

    /**
     * + 동일 소유자의 매장 중복 등록 확인
     * 역할: 같은 회원이 동일한 이름과 주소의 매장을
     *       이미 등록했는지 확인합니다.
     *
     * @param memberId 매장 소유자의 회원 ID
     * @param name 매장 이름
     * @param address 매장 주소
     * @return 중복 매장이 있으면 true, 없으면 false
     */
    boolean existsByOwner_IdAndNameAndAddress(
            Long memberId,
            String name,
            String address
    );

}
