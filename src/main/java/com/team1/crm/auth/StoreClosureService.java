package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 소유 매장을 폐업·읽기 전용 상태로 바꾸고 주요 고객 정보를 제거한다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Service
@RequiredArgsConstructor
public class StoreClosureService {
    private final StoreRepository stores;
    private final StoreClosureRepository closure;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    /** 상태 기록과 정보 제거를 하나의 트랜잭션으로 실행한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Transactional
    public void close(Long memberId, Long storeId) {
        Store store = stores.findLockedById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."));
        // 앞서 같은 요청에서 조회했더라도 잠금을 획득한 뒤 최신 소유자·상태를 다시 읽는다.
        entityManager.refresh(store, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!store.getOwner().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "소유 매장만 폐업 처리할 수 있습니다.");
        }
        if (store.isClosed()) return;
        var at = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        store.close(at);
        closure.removeCustomerData(storeId, at);
        closure.clearVisitMemos(storeId);
        closure.clearCouponTitles(storeId);
    }
}
