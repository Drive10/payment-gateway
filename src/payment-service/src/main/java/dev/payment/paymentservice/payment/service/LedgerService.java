package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.enums.LedgerAccountType;
import dev.payment.paymentservice.payment.domain.enums.LedgerEntryType;
import dev.payment.paymentservice.payment.domain.enums.LedgerTransactionType;
import dev.payment.paymentservice.payment.domain.ledger.LedgerAccount;
import dev.payment.paymentservice.payment.domain.ledger.LedgerEntry;
import dev.payment.paymentservice.payment.repository.LedgerAccountRepository;
import dev.payment.paymentservice.payment.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerService(LedgerAccountRepository accountRepository, LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Creates default ledger accounts for a merchant
     */
    @Transactional
    public void createMerchantAccounts(UUID merchantId, String currency) {
        // Create merchant wallet account
        if (accountRepository.findByMerchantIdAndAccountType(merchantId, LedgerAccountType.MERCHANT_WALLET).isEmpty()) {
            LedgerAccount wallet = new LedgerAccount(LedgerAccountType.MERCHANT_WALLET, merchantId, currency);
            accountRepository.save(wallet);
        }

        // Create merchant receivable account
        if (accountRepository.findByMerchantIdAndAccountType(merchantId, LedgerAccountType.MERCHANT_RECEIVABLE).isEmpty()) {
            LedgerAccount receivable = new LedgerAccount(LedgerAccountType.MERCHANT_RECEIVABLE, merchantId, currency);
            accountRepository.save(receivable);
        }
    }

    /**
     * Records a payment in the ledger (double-entry)
     * Debit: Customer Escrow
     * Credit: Merchant Receivable (net)
     * Credit: Platform Fee
     * Credit: Gateway Fee
     */
    @Transactional
    public void recordPayment(UUID paymentId, String paymentIdStr, UUID merchantId, 
                              BigDecimal grossAmount, BigDecimal platformFee, 
                              BigDecimal gatewayFee, String currency) {
        List<LedgerEntry> entries = new ArrayList<>();
        BigDecimal netAmount = grossAmount.subtract(platformFee).subtract(gatewayFee);

        // 1. Debit Customer Escrow (money received from customer)
        LedgerAccount customerEscrow = getOrCreatePlatformAccount(LedgerAccountType.CUSTOMER_ESCROW, currency);
        LedgerEntry debitEscrow = createEntry(
                UUID.randomUUID().toString(),
                paymentIdStr,
                LedgerTransactionType.PAYMENT,
                customerEscrow.getId(),
                LedgerEntryType.DEBIT,
                grossAmount,
                customerEscrow.getBalance().add(grossAmount),
                "PAYMENT", paymentId,
                "Payment received from customer: " + grossAmount
        );
        entries.add(debitEscrow);
        customerEscrow.setBalance(customerEscrow.getBalance().add(grossAmount));

        // 2. Credit Merchant Receivable (net amount owed to merchant)
        LedgerAccount merchantReceivable = getOrCreateMerchantAccount(merchantId, LedgerAccountType.MERCHANT_RECEIVABLE, currency);
        LedgerEntry creditReceivable = createEntry(
                UUID.randomUUID().toString(),
                paymentIdStr,
                LedgerTransactionType.PAYMENT,
                merchantReceivable.getId(),
                LedgerEntryType.CREDIT,
                netAmount,
                merchantReceivable.getBalance().add(netAmount),
                "PAYMENT", paymentId,
                "Merchant receivable for payment: " + netAmount
        );
        entries.add(creditReceivable);
        merchantReceivable.setBalance(merchantReceivable.getBalance().add(netAmount));

        // 3. Credit Platform Fee
        LedgerAccount platformFeeAccount = null;
        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            platformFeeAccount = getOrCreatePlatformAccount(LedgerAccountType.PLATFORM_FEE, currency);
            LedgerEntry creditFee = createEntry(
                    UUID.randomUUID().toString(),
                    paymentIdStr,
                    LedgerTransactionType.FEE,
                    platformFeeAccount.getId(),
                    LedgerEntryType.CREDIT,
                    platformFee,
                    platformFeeAccount.getBalance().add(platformFee),
                    "PAYMENT", paymentId,
                    "Platform fee: " + platformFee
            );
            entries.add(creditFee);
            platformFeeAccount.setBalance(platformFeeAccount.getBalance().add(platformFee));
        }

        // 4. Credit Gateway Fee
        LedgerAccount gatewayFeeAccount = null;
        if (gatewayFee.compareTo(BigDecimal.ZERO) > 0) {
            gatewayFeeAccount = getOrCreatePlatformAccount(LedgerAccountType.PLATFORM_GATEWAY_FEE, currency);
            LedgerEntry creditGatewayFee = createEntry(
                    UUID.randomUUID().toString(),
                    paymentIdStr,
                    LedgerTransactionType.FEE,
                    gatewayFeeAccount.getId(),
                    LedgerEntryType.CREDIT,
                    gatewayFee,
                    gatewayFeeAccount.getBalance().add(gatewayFee),
                    "PAYMENT", paymentId,
                    "Gateway fee: " + gatewayFee
            );
            entries.add(creditGatewayFee);
            gatewayFeeAccount.setBalance(gatewayFeeAccount.getBalance().add(gatewayFee));
        }

        // Save all entries and update account balances
        entryRepository.saveAll(entries);
        accountRepository.save(customerEscrow);
        accountRepository.save(merchantReceivable);
        if (platformFeeAccount != null) {
            accountRepository.save(platformFeeAccount);
        }
        if (gatewayFeeAccount != null) {
            accountRepository.save(gatewayFeeAccount);
        }
    }

    /**
     * Records a refund in the ledger (reverses payment entries)
     */
    @Transactional
    public void recordRefund(UUID refundId, String refundIdStr, UUID paymentId, String paymentIdStr,
                             UUID merchantId, BigDecimal refundAmount, 
                             BigDecimal refundedFee, String currency) {
        List<LedgerEntry> entries = new ArrayList<>();
        BigDecimal netRefund = refundAmount.subtract(refundedFee);

        // 1. Debit Merchant Receivable (reduce amount owed to merchant)
        LedgerAccount merchantReceivable = getOrCreateMerchantAccount(merchantId, LedgerAccountType.MERCHANT_RECEIVABLE, currency);
        LedgerEntry debitReceivable = createEntry(
                UUID.randomUUID().toString(),
                refundIdStr,
                LedgerTransactionType.REFUND,
                merchantReceivable.getId(),
                LedgerEntryType.DEBIT,
                netRefund,
                merchantReceivable.getBalance().subtract(netRefund),
                "REFUND", refundId,
                "Refund to customer: " + netRefund
        );
        entries.add(debitReceivable);
        merchantReceivable.setBalance(merchantReceivable.getBalance().subtract(netRefund));

        // 2. Credit Customer Escrow (return money to customer)
        LedgerAccount customerEscrow = getOrCreatePlatformAccount(LedgerAccountType.CUSTOMER_ESCROW, currency);
        LedgerEntry creditEscrow = createEntry(
                UUID.randomUUID().toString(),
                refundIdStr,
                LedgerTransactionType.REFUND,
                customerEscrow.getId(),
                LedgerEntryType.CREDIT,
                refundAmount,
                customerEscrow.getBalance().subtract(refundAmount),
                "REFUND", refundId,
                "Refund to customer: " + refundAmount
        );
        entries.add(creditEscrow);
        customerEscrow.setBalance(customerEscrow.getBalance().subtract(refundAmount));

        // 3. Debit Platform Fee (reverse fee income)
        LedgerAccount platformFeeAccount = null;
        if (refundedFee.compareTo(BigDecimal.ZERO) > 0) {
            platformFeeAccount = getOrCreatePlatformAccount(LedgerAccountType.PLATFORM_FEE, currency);
            LedgerEntry debitFee = createEntry(
                    UUID.randomUUID().toString(),
                    refundIdStr,
                    LedgerTransactionType.FEE,
                    platformFeeAccount.getId(),
                    LedgerEntryType.DEBIT,
                    refundedFee,
                    platformFeeAccount.getBalance().subtract(refundedFee),
                    "REFUND", refundId,
                    "Reversed platform fee: " + refundedFee
            );
            entries.add(debitFee);
            platformFeeAccount.setBalance(platformFeeAccount.getBalance().subtract(refundedFee));
        }

        entryRepository.saveAll(entries);
        accountRepository.save(merchantReceivable);
        accountRepository.save(customerEscrow);
        if (platformFeeAccount != null) {
            accountRepository.save(platformFeeAccount);
        }
    }

    /**
     * Records a settlement (transfers from receivables to merchant wallet)
     */
    @Transactional
    public void recordSettlement(UUID settlementId, String settlementIdStr, UUID merchantId,
                                 BigDecimal amount, String currency) {
        List<LedgerEntry> entries = new ArrayList<>();

        // 1. Debit Merchant Receivable
        LedgerAccount merchantReceivable = getOrCreateMerchantAccount(merchantId, LedgerAccountType.MERCHANT_RECEIVABLE, currency);
        LedgerEntry debitReceivable = createEntry(
                UUID.randomUUID().toString(),
                settlementIdStr,
                LedgerTransactionType.SETTLEMENT,
                merchantReceivable.getId(),
                LedgerEntryType.DEBIT,
                amount,
                merchantReceivable.getBalance().subtract(amount),
                "SETTLEMENT", settlementId,
                "Settlement to merchant wallet: " + amount
        );
        entries.add(debitReceivable);
        merchantReceivable.setBalance(merchantReceivable.getBalance().subtract(amount));

        // 2. Credit Merchant Wallet
        LedgerAccount merchantWallet = getOrCreateMerchantAccount(merchantId, LedgerAccountType.MERCHANT_WALLET, currency);
        LedgerEntry creditWallet = createEntry(
                UUID.randomUUID().toString(),
                settlementIdStr,
                LedgerTransactionType.SETTLEMENT,
                merchantWallet.getId(),
                LedgerEntryType.CREDIT,
                amount,
                merchantWallet.getBalance().add(amount),
                "SETTLEMENT", settlementId,
                "Settlement received: " + amount
        );
        entries.add(creditWallet);
        merchantWallet.setBalance(merchantWallet.getBalance().add(amount));

        entryRepository.saveAll(entries);
        accountRepository.save(merchantReceivable);
        accountRepository.save(merchantWallet);
    }

    /**
     * Gets merchant balance (available for payout)
     */
    public BigDecimal getMerchantBalance(UUID merchantId) {
        return accountRepository.findByMerchantIdAndAccountType(merchantId, LedgerAccountType.MERCHANT_WALLET)
                .map(LedgerAccount::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Gets merchant receivable (pending settlement)
     */
    public BigDecimal getMerchantReceivable(UUID merchantId) {
        return accountRepository.findByMerchantIdAndAccountType(merchantId, LedgerAccountType.MERCHANT_RECEIVABLE)
                .map(LedgerAccount::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    private LedgerAccount getOrCreateMerchantAccount(UUID merchantId, LedgerAccountType type, String currency) {
        return accountRepository.findByMerchantIdAndAccountType(merchantId, type)
                .orElseGet(() -> {
                    LedgerAccount account = new LedgerAccount(type, merchantId, currency);
                    return accountRepository.save(account);
                });
    }

    private LedgerAccount getOrCreatePlatformAccount(LedgerAccountType type, String currency) {
        return accountRepository.findByAccountTypeAndMerchantIdIsNull(type)
                .orElseGet(() -> {
                    LedgerAccount account = new LedgerAccount(type, null, currency);
                    return accountRepository.save(account);
                });
    }

    private LedgerEntry createEntry(String entryId, String transactionId, LedgerTransactionType transactionType,
                                     UUID accountId, LedgerEntryType entryType, BigDecimal amount,
                                     BigDecimal balanceAfter, String referenceType, UUID referenceId, String description) {
        return new LedgerEntry(entryId, transactionId, transactionType, accountId, 
                               entryType, amount, balanceAfter, referenceType, referenceId, description);
    }
}