
package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    // 1. 새로운 매장 등록
    @Transactional
    public Store registerStore(
            String email,
            String name,
            String category,
            String address,
            int pointRate
    ) {

        // 로그인한 회원을 이메일로 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
                );

        // 매장 이름 검증
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("매장 이름을 입력해주세요.");
        }

        // 포인트 적립률 검증
        if (pointRate < 0 || pointRate > 20) {
            throw new IllegalArgumentException("적립률은 0~20% 사이여야 합니다.");
        }

        // Store 객체 생성
        Store store = new Store(
                member,
                name.trim(),
                category,
                address,
                pointRate
        );

        // DB에 매장 저장
        return storeRepository.save(store);
    }

    // 2. 로그인한 회원의 매장 목록 조회
    @Transactional(readOnly = true)
    public List<Store> getMyStores(String email) {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
                );

        return storeRepository.findByOwnerId(member.getId());
    }
}
