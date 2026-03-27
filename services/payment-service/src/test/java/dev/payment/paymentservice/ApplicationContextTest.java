package dev.payment.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestSupportConfig.class, TestLedgerSupportConfig.class})
class ApplicationContextTest {

    @Test
    void contextLoads() {
    }

}
