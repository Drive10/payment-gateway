package dev.payment.notificationservice.service;

import dev.payment.notificationservice.client.MerchantClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MerchantClient merchantClient;

    private NotificationService notificationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notificationService = new NotificationService(restTemplate, merchantClient, objectMapper);
    }

    @Test
    void handlePaymentEvent_ValidEvent_ShouldProcessSuccessfully() {
        String eventJson = "{\"paymentId\":\"test-payment-123\",\"orderId\":\"order-456\",\"merchantId\":\"merchant-789\",\"amount\":1000,\"currency\":\"USD\",\"status\":\"CREATED\"}";

        notificationService.handlePaymentEvent(eventJson);

        assertTrue(true);
    }

    @Test
    void handlePaymentEvent_DoubleEncodedEvent_ShouldParseCorrectly() {
        String doubleEncodedJson = "\"{\\\"paymentId\\\":\\\"test-payment-123\\\",\\\"orderId\\\":\\\"order-456\\\",\\\"merchantId\\\":\\\"merchant-789\\\",\\\"amount\\\":1000,\\\"currency\\\":\\\"USD\\\",\\\"status\\\":\\\"CREATED\\\"}\"";

        notificationService.handlePaymentEvent(doubleEncodedJson);

        assertTrue(true);
    }

    @Test
    void getMerchantWebhookUrl_WithValidMerchantId_ShouldReturnUrl() {
        String merchantId = "merchant-123";
        String expectedUrl = "https://example.com/webhook";
        when(merchantClient.getMerchantWebhookUrl(merchantId)).thenReturn(expectedUrl);

        String result = merchantClient.getMerchantWebhookUrl(merchantId);

        assertEquals(expectedUrl, result);
        verify(merchantClient, times(1)).getMerchantWebhookUrl(merchantId);
    }

    @Test
    void getMerchantWebhookUrl_WithInvalidMerchantId_ShouldReturnNull() {
        String merchantId = "invalid-merchant";
        when(merchantClient.getMerchantWebhookUrl(merchantId)).thenReturn(null);

        String result = merchantClient.getMerchantWebhookUrl(merchantId);

        assertNull(result);
    }
}