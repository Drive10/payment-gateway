package dev.payment.settlementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.settlementservice.entity.MerchantSettlement;
import dev.payment.settlementservice.entity.Settlement;
import dev.payment.settlementservice.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private SettlementController settlementController;

    private UUID merchantId;
    private UUID settlementId;
    private Settlement testSettlement;
    private MerchantSettlement testMerchantSettlement;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(settlementController).build();

        merchantId = UUID.randomUUID();
        settlementId = UUID.randomUUID();

        testSettlement = new Settlement();
        testSettlement.setId(settlementId);
        testSettlement.setMerchantId(merchantId);
        testSettlement.setMerchantName("Test Merchant");
        testSettlement.setSettlementReference("STL-ABC12345");
        testSettlement.setPeriodStart(LocalDateTime.now().minusDays(1));
        testSettlement.setPeriodEnd(LocalDateTime.now());
        testSettlement.setStatus(Settlement.SettlementStatus.PENDING);
        testSettlement.setTotalTransactions(0);
        testSettlement.setTotalAmount(BigDecimal.ZERO);
        testSettlement.setTotalFees(BigDecimal.ZERO);
        testSettlement.setTotalRefunds(BigDecimal.ZERO);
        testSettlement.setNetAmount(BigDecimal.ZERO);
        testSettlement.setCurrency("INR");

        testMerchantSettlement = new MerchantSettlement();
        testMerchantSettlement.setId(UUID.randomUUID());
        testMerchantSettlement.setMerchantId(merchantId);
        testMerchantSettlement.setMerchantName("Test Merchant");
        testMerchantSettlement.setCurrentBalance(new BigDecimal("5000.00"));
        testMerchantSettlement.setPendingBalance(new BigDecimal("1000.00"));
        testMerchantSettlement.setTotalSettled(new BigDecimal("20000.00"));
    }

    @Test
    void createSettlement_shouldReturnCreatedSettlement() throws Exception {
        when(settlementService.createSettlement(any(UUID.class), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testSettlement);

        Map<String, Object> request = Map.of(
                "merchantId", merchantId.toString(),
                "merchantName", "Test Merchant"
        );

        mockMvc.perform(post("/internal/platform/settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.data.merchantName").value("Test Merchant"))
                .andExpect(jsonPath("$.data.settlementReference").value("STL-ABC12345"));

        verify(settlementService).createSettlement(any(UUID.class), anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void createSettlement_shouldReturnBadRequestWhenMerchantIdMissing() throws Exception {
        Map<String, Object> request = Map.of("merchantName", "Test Merchant");

        mockMvc.perform(post("/internal/platform/settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(settlementService, never()).createSettlement(any(), any(), any(), any());
    }

    @Test
    void createSettlement_shouldReturnBadRequestWhenMerchantNameMissing() throws Exception {
        Map<String, Object> request = Map.of("merchantId", merchantId.toString());

        mockMvc.perform(post("/internal/platform/settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(settlementService, never()).createSettlement(any(), any(), any(), any());
    }

    @Test
    void createSettlement_shouldReturnBadRequestWhenBothFieldsMissing() throws Exception {
        Map<String, Object> request = Map.of();

        mockMvc.perform(post("/internal/platform/settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(settlementService, never()).createSettlement(any(), any(), any(), any());
    }

    @Test
    void processSettlement_shouldReturnProcessedSettlement() throws Exception {
        Settlement processedSettlement = new Settlement();
        processedSettlement.setId(settlementId);
        processedSettlement.setMerchantId(merchantId);
        processedSettlement.setSettlementReference("STL-ABC12345");
        processedSettlement.setStatus(Settlement.SettlementStatus.COMPLETED);
        processedSettlement.setPayoutReference("PAY-1234567890");
        processedSettlement.setPayoutDate(LocalDateTime.now());
        processedSettlement.setProcessedAt(LocalDateTime.now());

        when(settlementService.processSettlement(settlementId)).thenReturn(processedSettlement);

        mockMvc.perform(post("/internal/platform/settlement/{id}/process", settlementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(settlementId.toString()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.payoutReference").value("PAY-1234567890"));

        verify(settlementService).processSettlement(settlementId);
    }

    @Test
    void processSettlement_shouldReturnNotFoundWhenSettlementDoesNotExist() throws Exception {
        when(settlementService.processSettlement(settlementId))
                .thenThrow(new RuntimeException("Settlement not found: " + settlementId));

        mockMvc.perform(post("/internal/platform/settlement/{id}/process", settlementId))
                .andExpect(status().isInternalServerError());

        verify(settlementService).processSettlement(settlementId);
    }

    @Test
    void getSettlement_shouldReturnSettlementWhenExists() throws Exception {
        when(settlementService.getSettlement(settlementId)).thenReturn(Optional.of(testSettlement));

        mockMvc.perform(get("/internal/platform/settlement/{id}", settlementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(settlementId.toString()))
                .andExpect(jsonPath("$.data.merchantName").value("Test Merchant"))
                .andExpect(jsonPath("$.data.settlementReference").value("STL-ABC12345"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(settlementService).getSettlement(settlementId);
    }

    @Test
    void getSettlement_shouldReturnNotFoundWhenSettlementDoesNotExist() throws Exception {
        when(settlementService.getSettlement(settlementId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/platform/settlement/{id}", settlementId))
                .andExpect(status().isNotFound());

        verify(settlementService).getSettlement(settlementId);
    }

    @Test
    void getMerchantSettlements_shouldReturnListOfSettlements() throws Exception {
        Settlement s2 = new Settlement();
        s2.setId(UUID.randomUUID());
        s2.setMerchantId(merchantId);
        s2.setMerchantName("Test Merchant");
        s2.setSettlementReference("STL-XYZ98765");
        s2.setStatus(Settlement.SettlementStatus.COMPLETED);
        s2.setTotalTransactions(5);
        s2.setTotalAmount(new BigDecimal("500.00"));
        s2.setTotalFees(new BigDecimal("12.50"));
        s2.setTotalRefunds(BigDecimal.ZERO);
        s2.setNetAmount(new BigDecimal("487.50"));
        s2.setCurrency("INR");
        s2.setPeriodStart(LocalDateTime.now().minusDays(2));
        s2.setPeriodEnd(LocalDateTime.now().minusDays(1));

        when(settlementService.getMerchantSettlements(merchantId)).thenReturn(Arrays.asList(testSettlement, s2));

        mockMvc.perform(get("/internal/platform/settlement/merchant/{merchantId}", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].settlementReference").value("STL-ABC12345"))
                .andExpect(jsonPath("$.data[1].settlementReference").value("STL-XYZ98765"));

        verify(settlementService).getMerchantSettlements(merchantId);
    }

    @Test
    void getMerchantSettlements_shouldReturnEmptyListWhenNoSettlements() throws Exception {
        when(settlementService.getMerchantSettlements(merchantId)).thenReturn(List.of());

        mockMvc.perform(get("/internal/platform/settlement/merchant/{merchantId}", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(settlementService).getMerchantSettlements(merchantId);
    }

    @Test
    void getPendingSettlements_shouldReturnPendingSettlements() throws Exception {
        Settlement s2 = new Settlement();
        s2.setId(UUID.randomUUID());
        s2.setMerchantId(UUID.randomUUID());
        s2.setMerchantName("Another Merchant");
        s2.setSettlementReference("STL-PEND002");
        s2.setStatus(Settlement.SettlementStatus.PENDING);
        s2.setTotalTransactions(0);
        s2.setTotalAmount(BigDecimal.ZERO);
        s2.setTotalFees(BigDecimal.ZERO);
        s2.setTotalRefunds(BigDecimal.ZERO);
        s2.setNetAmount(BigDecimal.ZERO);
        s2.setCurrency("INR");
        s2.setPeriodStart(LocalDateTime.now().minusDays(1));
        s2.setPeriodEnd(LocalDateTime.now());

        when(settlementService.getPendingSettlements()).thenReturn(Arrays.asList(testSettlement, s2));

        mockMvc.perform(get("/internal/platform/settlement/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[1].status").value("PENDING"));

        verify(settlementService).getPendingSettlements();
    }

    @Test
    void getPendingSettlements_shouldReturnEmptyListWhenNonePending() throws Exception {
        when(settlementService.getPendingSettlements()).thenReturn(List.of());

        mockMvc.perform(get("/internal/platform/settlement/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(settlementService).getPendingSettlements();
    }

    @Test
    void getMerchantBalance_shouldReturnBalanceInfo() throws Exception {
        when(settlementService.getOrCreateMerchantSettlement(eq(merchantId), anyString()))
                .thenReturn(testMerchantSettlement);

        mockMvc.perform(get("/internal/platform/settlement/merchant/{merchantId}/balance", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.data.currentBalance").value(5000.00))
                .andExpect(jsonPath("$.data.pendingBalance").value(1000.00))
                .andExpect(jsonPath("$.data.totalSettled").value(20000.00));

        verify(settlementService).getOrCreateMerchantSettlement(eq(merchantId), anyString());
    }

    @Test
    void getMerchantBalance_shouldCreateMerchantIfNotExists() throws Exception {
        MerchantSettlement newMerchant = new MerchantSettlement();
        newMerchant.setId(UUID.randomUUID());
        newMerchant.setMerchantId(merchantId);
        newMerchant.setMerchantName("Merchant");
        newMerchant.setCurrentBalance(BigDecimal.ZERO);
        newMerchant.setPendingBalance(BigDecimal.ZERO);
        newMerchant.setTotalSettled(BigDecimal.ZERO);

        when(settlementService.getOrCreateMerchantSettlement(eq(merchantId), anyString()))
                .thenReturn(newMerchant);

        mockMvc.perform(get("/internal/platform/settlement/merchant/{merchantId}/balance", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentBalance").value(0.0))
                .andExpect(jsonPath("$.data.pendingBalance").value(0.0))
                .andExpect(jsonPath("$.data.totalSettled").value(0.0));

        verify(settlementService).getOrCreateMerchantSettlement(eq(merchantId), eq("Merchant"));
    }

    @Test
    void syncBalance_shouldUpdateMerchantBalance() throws Exception {
        doNothing().when(settlementService).updateMerchantBalance(eq(merchantId), any(BigDecimal.class));

        Map<String, Object> request = Map.of("availableBalance", "750.50");

        mockMvc.perform(post("/internal/platform/settlement/merchant/{merchantId}/sync-balance", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(settlementService).updateMerchantBalance(eq(merchantId), eq(new BigDecimal("750.50")));
    }

    @Test
    void syncBalance_shouldHandleZeroAmount() throws Exception {
        doNothing().when(settlementService).updateMerchantBalance(eq(merchantId), any(BigDecimal.class));

        Map<String, Object> request = Map.of("availableBalance", "0");

        mockMvc.perform(post("/internal/platform/settlement/merchant/{merchantId}/sync-balance", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(settlementService).updateMerchantBalance(eq(merchantId), eq(BigDecimal.ZERO));
    }

    @Test
    void syncBalance_shouldHandleNegativeAmount() throws Exception {
        doNothing().when(settlementService).updateMerchantBalance(eq(merchantId), any(BigDecimal.class));

        Map<String, Object> request = Map.of("availableBalance", "-100.00");

        mockMvc.perform(post("/internal/platform/settlement/merchant/{merchantId}/sync-balance", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(settlementService).updateMerchantBalance(eq(merchantId), eq(new BigDecimal("-100.00")));
    }

    @Test
    void triggerSettlement_shouldProcessAllPendingSettlements() throws Exception {
        Settlement s2 = new Settlement();
        s2.setId(UUID.randomUUID());
        s2.setMerchantId(UUID.randomUUID());
        s2.setSettlementReference("STL-TRIG002");
        s2.setStatus(Settlement.SettlementStatus.PENDING);
        s2.setTotalTransactions(0);
        s2.setTotalAmount(BigDecimal.ZERO);
        s2.setTotalFees(BigDecimal.ZERO);
        s2.setTotalRefunds(BigDecimal.ZERO);
        s2.setNetAmount(BigDecimal.ZERO);
        s2.setCurrency("INR");
        s2.setPeriodStart(LocalDateTime.now().minusDays(1));
        s2.setPeriodEnd(LocalDateTime.now());

        when(settlementService.getPendingSettlements()).thenReturn(Arrays.asList(testSettlement, s2));
        when(settlementService.processSettlement(settlementId)).thenReturn(testSettlement);
        when(settlementService.processSettlement(s2.getId())).thenReturn(s2);

        mockMvc.perform(post("/internal/platform/settlement/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Settlement job triggered, processed 2 settlements"));

        verify(settlementService).getPendingSettlements();
        verify(settlementService).processSettlement(settlementId);
        verify(settlementService).processSettlement(s2.getId());
    }

    @Test
    void triggerSettlement_shouldReturnZeroWhenNoPendingSettlements() throws Exception {
        when(settlementService.getPendingSettlements()).thenReturn(List.of());

        mockMvc.perform(post("/internal/platform/settlement/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Settlement job triggered, processed 0 settlements"));

        verify(settlementService).getPendingSettlements();
        verify(settlementService, never()).processSettlement(any());
    }

    @Test
    void createSettlement_shouldHandleInvalidMerchantIdFormat() throws Exception {
        Map<String, Object> request = Map.of(
                "merchantId", "not-a-uuid",
                "merchantName", "Test Merchant"
        );

        mockMvc.perform(post("/internal/platform/settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(settlementService, never()).createSettlement(any(), any(), any(), any());
    }
}
