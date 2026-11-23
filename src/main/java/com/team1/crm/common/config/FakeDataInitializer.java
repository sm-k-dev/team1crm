package com.team1.crm.common.config;

import com.team1.crm.auth.*;
import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerRepository;
import com.team1.crm.loyalty.visit.*;
import com.team1.crm.loyalty.point.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

/** 명시적으로 켠 개발 환경에서만 가상 고객 50명과 6개월 거래를 생성한다. 김순민, 2026-11-23 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class FakeDataInitializer implements ApplicationRunner {
    private final MemberRepository members;
    private final StoreRepository stores;
    private final CustomerRepository customers;
    private final VisitRepository visits;
    private final PointHistoryRepository points;
    private final PasswordEncoder encoder;
    private final String password;

    public FakeDataInitializer(MemberRepository members, StoreRepository stores,
            CustomerRepository customers, VisitRepository visits, PointHistoryRepository points,
            PasswordEncoder encoder, @Value("${app.demo.password:}") String password) {
        this.members = members; this.stores = stores; this.customers = customers;
        this.visits = visits; this.points = points; this.encoder = encoder; this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        PasswordPolicy.validate(password);
        // 전용 계정이 있으면 기존 고객·이력·비밀번호를 수정하지 않는다.
        if (members.existsByEmail("owner.demo@crm.local")) return;
        if (members.existsByEmail("admin.demo@crm.local")) {
            throw new IllegalStateException("시연 계정 이메일이 이미 사용 중입니다. 기존 데이터를 확인해 주세요.");
        }
        members.save(new Member("admin.demo@crm.local", encoder.encode(password), "시연관리자", Role.ADMIN));
        Member owner = members.save(new Member("owner.demo@crm.local", encoder.encode(password), "시연사장님", Role.USER));
        Store primary = stores.save(new Store(owner, "시연 카페", "카페", "가상 주소 A", 5));
        Store secondary = stores.save(new Store(owner, "시연 두번째 매장", "카페", "가상 주소 B", 5));
        YearMonth month = YearMonth.now(ZoneId.of("Asia/Seoul"));
        for (int i = 0; i < 50; i++) {
            Customer customer = customers.save(new Customer(i < 40 ? primary : secondary,
                    "가상고객" + (i + 1), "010-0000-" + String.format("%04d", i + 1),
                    i % 12 + 1, "시연용 가상 데이터", i % 2 == 0,
                    month.minusMonths(5).atDay(1).atStartOfDay()));
            for (int offset = 5; offset >= 0; offset--) {
                if (i % 5 == 0 && offset < 2) continue;
                LocalDate date = month.minusMonths(offset).atDay(1 + i % 20);
                if (date.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) continue;
                int amount = 5000 + i * 500;
                Visit visit = visits.save(new Visit(customer, date.atTime(9 + i % 12, 0), amount, 0, "가상 메뉴"));
                points.save(new PointHistory(customer, visit, PointType.EARN, amount / 20, visit.getVisitedAt()));
            }
        }
    }
}
