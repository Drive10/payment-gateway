package dev.payment.ledgerservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.ledgerservice.dto.request.CreateAccountRequest;
import dev.payment.ledgerservice.dto.request.PostJournalRequest;
import dev.payment.ledgerservice.dto.response.AccountResponse;
import dev.payment.ledgerservice.dto.response.JournalResponse;
import dev.payment.ledgerservice.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {
    private final LedgerService ledgerService;
    public LedgerController(LedgerService ledgerService) { this.ledgerService = ledgerService; }

    @PostMapping("/accounts")
    public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.success(ledgerService.createAccount(request));
    }

    @PostMapping("/journals")
    public ApiResponse<JournalResponse> postJournal(@Valid @RequestBody PostJournalRequest request) {
        return ApiResponse.success(ledgerService.postJournal(request));
    }

    @GetMapping("/accounts")
    public ApiResponse<List<AccountResponse>> getAccounts() {
        return ApiResponse.success(ledgerService.getAccounts());
    }
}
