package com.team1.crm.admin;

import com.team1.crm.auth.Member;
import org.springframework.data.repository.Repository;

/** 관리자 계정 등록의 중복 확인과 저장을 담당한다. 작성자: 김순민 / 작성일: 2026-11-11 */
public interface AdminOwnerRepository extends Repository<Member, Long> {
    /** 이메일 중복을 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    boolean existsByEmail(String email);
    /** 기존 DB의 이름 유니크 제약에 맞춰 중복을 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    boolean existsByNickname(String nickname);
    /** 자기 계정을 제외한 이름 중복을 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    /** 수정할 계정을 조회한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    java.util.Optional<Member> findById(Long id);
    /** DB 제약 위반을 트랜잭션 안에서 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    Member saveAndFlush(Member member);
}
