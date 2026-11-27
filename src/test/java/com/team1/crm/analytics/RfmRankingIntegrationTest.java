package com.team1.crm.analytics;

import com.team1.crm.auth.*;
import com.team1.crm.customer.Customer;
import com.team1.crm.loyalty.visit.Visit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

/** T28: 실제 native NTILE SQL과 ID 동점 정렬. H2 결과이며 MySQL 재검증과 구분. 김순민, 2026-11-27. */
@DataJpaTest(properties={"spring.datasource.url=jdbc:h2:mem:rfm-ranking;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never"})
@ActiveProfiles("test")
class RfmRankingIntegrationTest {
    @Autowired EntityManager em;
    @Autowired AnalyticsRepository repository;
    final LocalDateTime start=LocalDateTime.of(2026,6,1,0,0),end=LocalDateTime.of(2026,12,1,0,0);
    Store store() {
        Member m=new Member("ranking@test.invalid","unused","순위검증",Role.USER);em.persist(m);
        Store s=new Store(m,"카페","카페",null,5);em.persist(s);return s;
    }
    Customer customer(Store s,int i) {
        Customer c=new Customer(s,"고객"+i,String.format("010-9000-%04d",i),null,null,false,null);em.persist(c);return c;
    }
    @Test void tenDistinctCustomersHaveFiveTilesForAllAxes() {
        Store s=store();
        for(int i=0;i<10;i++){
            Customer c=customer(s,i);
            for(int j=0;j<10-i;j++)em.persist(new Visit(c,end.minusDays(i+1).minusMinutes(j),100*(10-i),0,null));
        }
        em.flush();em.clear();
        var rows=repository.summarizeRfm(s.getId(),start,end);
        assertEquals(10,rows.size());
        for(int i=0;i<10;i++){
            var row=rows.get(i);int expected=5-i/2;
            assertEquals("고객"+i,row.getCustomerName());
            assertEquals(expected,row.getRecencyScore());assertEquals(expected,row.getFrequencyScore());assertEquals(expected,row.getMonetaryScore());
            assertEquals(10-i,row.getVisitCount());assertEquals(100L*(10-i)*(10-i),row.getTotalAmount());
        }
    }
    @Test void tiesUseCustomerIdConsistentlyOnRepeatedQuery() {
        Store s=store();
        for(int i=0;i<10;i++)em.persist(new Visit(customer(s,i),end.minusDays(1),1000,0,null));
        em.flush();em.clear();
        for(int pass=0;pass<2;pass++){
            var rows=repository.summarizeRfm(s.getId(),start,end);
            assertEquals(IntStream.range(0,10).mapToObj(i->"고객"+i).toList(),rows.stream().map(AnalyticsRepository.RfmTotal::getCustomerName).toList());
            for(int i=0;i<10;i++){
                assertEquals(5-i/2,rows.get(i).getRecencyScore());assertEquals(5-i/2,rows.get(i).getFrequencyScore());assertEquals(5-i/2,rows.get(i).getMonetaryScore());
            }
        }
    }
    @Test void fewerThanFiveCustomersUseOnlyOccupiedTiles() {
        Store s=store();for(int i=0;i<3;i++)em.persist(new Visit(customer(s,i),end.minusDays(1),1000,0,null));
        em.flush();
        assertEquals(List.of(5,4,3),repository.summarizeRfm(s.getId(),start,end).stream().map(AnalyticsRepository.RfmTotal::getRecencyScore).toList());
    }
}
