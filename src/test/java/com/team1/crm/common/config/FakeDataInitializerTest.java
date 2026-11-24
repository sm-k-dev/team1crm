package com.team1.crm.common.config;

import com.team1.crm.auth.*;
import com.team1.crm.auth.security.PasswordEncoderConfig;
import com.team1.crm.customer.*;
import com.team1.crm.loyalty.visit.*;
import com.team1.crm.loyalty.point.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.YearMonth;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FakeDataInitializerTest {
    @Test void generatesFiftyCustomersSixMonthsAndCompatiblePasswordsWithoutRepeating() {
        var members = mock(MemberRepository.class); var stores = mock(StoreRepository.class);
        var customers = mock(CustomerRepository.class); var visits = mock(VisitRepository.class);
        var points = mock(PointHistoryRepository.class);
        when(members.save(any())).thenAnswer(c -> c.getArgument(0));
        when(stores.save(any())).thenAnswer(c -> c.getArgument(0));
        when(customers.save(any())).thenAnswer(c -> c.getArgument(0));
        when(visits.save(any())).thenAnswer(c -> c.getArgument(0));
        var encoder = new PasswordEncoderConfig().passwordEncoder();
        var initializer = new FakeDataInitializer(members, stores, customers, visits, points, encoder, "Demo1234!");
        initializer.run(null);
        verify(customers, times(50)).save(any());
        var visitCaptor = ArgumentCaptor.forClass(Visit.class);
        verify(visits, atLeastOnce()).save(visitCaptor.capture());
        assertEquals(6, visitCaptor.getAllValues().stream().map(v -> YearMonth.from(v.getVisitedAt()))
                .collect(Collectors.toSet()).size());
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(members, times(2)).save(memberCaptor.capture());
        for (var member : memberCaptor.getAllValues()) assertTrue(encoder.matches("Demo1234!", member.getPassword()));
        when(members.existsByEmail("owner.demo@crm.local")).thenReturn(true);
        initializer.run(null);
        verify(customers, times(50)).save(any());
    }
}
