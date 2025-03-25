package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentType;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.exception.ConcurrencyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.Currency;

/**
 * Implementation of the PaymentTransactionService interface that handles
 * core payment transaction operations. This class contains the business logic
 * for creating, processing, and retrieving payment transactions.
 */
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentTransactionServiceImpl.class);
    
    private final PaymentTransactionDAO transactionDAO;
    private final PaymentEventDAO eventDAO;
    private final PaymentValidationService validationService;
    private final PaymentLifecycleService lifecycleService;
    
    /**
     * Constructs a new PaymentTransactionServiceImpl with the required dependencies.
     * 
     * @param daoFactory The factory for creating data access objects
     * @param validationService The service for validating payment data
     * @param lifecycleService The service for managing payment lifecycle
     */
    public PaymentTransactionServiceImpl(
            PaymentDAOFactory daoFactory,
            PaymentValidationService validationService,
            PaymentLifecycleService lifecycleService) {
        this.transactionDAO = daoFactory.getPaymentTransactionDAO();
        this.eventDAO = daoFactory.getPaymentEventDAO();
        this.validationService = validationService;
        this.lifecycleService = lifecycleService;
        
        logger.info("PaymentTransactionService initialized with HikariCP connection pool");
    }
    
    @Override
    public PaymentTransaction createTransaction(
            UUID organizationId,
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantId,
            PaymentType paymentType,
            String description,
            String transactionReference) {
        
        logger.debug("Creating new payment transaction for organization: {}, account: {}, amount: {} {}", 
                organizationId, accountId, amount, currency);
        
        // Validate input parameters
        validateCreateTransactionParams(organizationId, accountId, amount, currency, merchantId, paymentType);
        
        // Create new transaction with CREATED status
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setOrganizationId(organizationId);
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setMerchantId(merchantId);
        transaction.setPaymentType(paymentType);
        transaction.setStatus(PaymentStatus.CREATED);
        transaction.setDescription(description);
        transaction.setTransactionReference(transactionReference);
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());
        
        // Validate the transaction
        validationService.validateTransaction(transaction);
        
        // Persist the transaction
        PaymentTransaction createdTransaction = transactionDAO.create(transaction);
        
        // Record creation event
        eventDAO.recordTransactionEvent(
                createdTransaction.getTransactionId(),
                "TRANSACTION_CREATED",
                null,
                PaymentStatus.CREATED,
                "Transaction created",
                null);
        
        logger.info("Created payment transaction with ID: {}", createdTransaction.getTransactionId());
        
        return createdTransaction;
    }
    
    @Override
    public PaymentTransaction getTransactionById(UUID transactionId) {
        logger.debug("Retrieving transaction by ID: {}", transactionId);
        
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        PaymentTransaction transaction = transactionDAO.findById(transactionId);
        
        if (transaction == null) {
            logger.warn("Transaction not found with ID: {}", transactionId);
            throw new ResourceNotFoundException("Transaction not found", "transaction_id", transactionId.toString());
        }
        
        return transaction;
    }
    
    @Override
    public List<PaymentTransaction> getTransactionsByOrganization(
            UUID organizationId,
            int limit,
            int offset) {
        
        logger.debug("Retrieving transactions for organization: {}, limit: {}, offset: {}", 
                organizationId, limit, offset);
        
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        return transactionDAO.findByOrganization(organizationId, limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> getTransactionsByAccount(
            UUID organizationId,
            UUID accountId,
            int limit,
            int offset) {
        
        logger.debug("Retrieving transactions for organization: {}, account: {}, limit: {}, offset: {}", 
                organizationId, accountId, limit, offset);
        
        if (organizationId == null || accountId == null) {
            throw new IllegalArgumentException("Organization ID and Account ID cannot be null");
        }
        
        return transactionDAO.findByAccount(organizationId, accountId, limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> getTransactionsByStatus(
            UUID organizationId,
            UUID accountId,
            PaymentStatus status,
            int limit,
            int offset) {
        
        logger.debug("Retrieving transactions for organization: {}, account: {}, status: {}, limit: {}, offset: {}", 
                organizationId, accountId, status, limit, offset);
        
        if (organizationId == null || status == null) {
            throw new IllegalArgumentException("Organization ID and Status cannot be null");
        }
        
        return transactionDAO.findByStatus(organizationId, accountId, status, limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> getTransactionsByDateRange(
            UUID organizationId,
            UUID accountId,
            Instant startDate,
            Instant endDate,
            int limit,
            int offset) {
        
        logger.debug("Retrieving transactions for organization: {}, account: {}, date range: {} to {}, limit: {}, offset: {}", 
                organizationId, accountId, startDate, endDate, limit, offset);
        
        if (organizationId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Organization ID, Start Date, and End Date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        return transactionDAO.findByDateRange(organizationId, accountId, startDate, endDate, limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> getTransactionsByAmountRange(
            UUID organizationId,
            UUID accountId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String currency,
            int limit,
            int offset) {
        
        logger.debug("Retrieving transactions for organization: {}, account: {}, amount range: {} to {} {}, limit: {}, offset: {}", 
                organizationId, accountId, minAmount, maxAmount, currency, limit, offset);
        
        if (organizationId == null || minAmount == null || maxAmount == null || currency == null) {
            throw new IllegalArgumentException("Organization ID, Min Amount, Max Amount, and Currency cannot be null");
        }
        
        if (minAmount.compareTo(BigDecimal.ZERO) < 0 || maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount values cannot be negative");
        }
        
        if (minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Minimum amount must be less than or equal to maximum amount");
        }
        
        // Validate currency code
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currency);
        }
        
        return transactionDAO.findByAmountRange(organizationId, accountId, minAmount, maxAmount, currency, limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> getTransactionsByMerchant(
            UUID organizationId,
            UUID accountId,
            String merchantId,
            int limit,
            int offset) {
        
        logger.debug("Retrieving transactions for organization: {}, account: {}, merchant: {}, limit: {}, offset: {}", 
                organizationId, accountId, merchantId, limit, offset);
        
        if (organizationId == null || merchantId == null || merchantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID and Merchant ID cannot be null or empty");
        }
        
        return transactionDAO.findByMerchant(organizationId, accountId, merchantId, limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> queryTransactions(PaymentFilterParams filterParams) {
        logger.debug("Querying transactions with filter params: {}", filterParams);
        
        if (filterParams == null) {
            throw new IllegalArgumentException("Filter parameters cannot be null");
        }
        
        // Validate filter parameters
        validationService.validateFilterParams(filterParams);
        
        return transactionDAO.findByFilterParams(filterParams);
    }
    
    @Override
    public long countTransactions(PaymentFilterParams filterParams) {
        logger.debug("Counting transactions with filter params: {}", filterParams);
        
        if (filterParams == null) {
            throw new IllegalArgumentException("Filter parameters cannot be null");
        }
        
        // Validate filter parameters
        validationService.validateFilterParams(filterParams);
        
        return transactionDAO.countByFilterParams(filterParams);
    }
    
    @Override
    public PaymentTransaction updateTransactionStatus(UUID transactionId, PaymentStatus newStatus) {
        logger.debug("Updating transaction status: {} -> {}", transactionId, newStatus);
        
        if (transactionId == null || newStatus == null) {
            throw new IllegalArgumentException("Transaction ID and new status cannot be null");
        }
        
        // Get current transaction
        PaymentTransaction transaction = getTransactionById(transactionId);
        PaymentStatus currentStatus = transaction.getStatus();
        
        // Validate status transition
        if (!lifecycleService.isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
        
        // Update status
        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(Instant.now());
        
        // Persist updated transaction
        PaymentTransaction updatedTransaction = transactionDAO.update(transaction);
        
        // Record status change event
        eventDAO.recordTransactionEvent(
                transactionId,
                "STATUS_CHANGED",
                currentStatus,
                newStatus,
                String.format("Status changed from %s to %s", currentStatus, newStatus),
                null);
        
        logger.info("Updated transaction status: {} from {} to {}", 
                transactionId, currentStatus, newStatus);
        
        return updatedTransaction;
    }
    
    @Override
    public PaymentTransaction processTransaction(UUID transactionId) {
        logger.debug("Processing transaction: {}", transactionId);
        
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (transaction.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException(
                    String.format("Cannot process transaction in %s state", transaction.getStatus()));
        }
        
        // Update to PROCESSING status
        return updateTransactionStatus(transactionId, PaymentStatus.PROCESSING);
    }
    
    @Override
    public PaymentTransaction authorizeTransaction(UUID transactionId) {
        logger.debug("Authorizing transaction: {}", transactionId);
        
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (transaction.getStatus() != PaymentStatus.PROCESSING) {
            throw new IllegalStateException(
                    String.format("Cannot authorize transaction in %s state", transaction.getStatus()));
        }
        
        // Update to AUTHORIZED status
        return updateTransactionStatus(transactionId, PaymentStatus.AUTHORIZED);
    }
    
    @Override
    public PaymentTransaction captureTransaction(UUID transactionId) {
        logger.debug("Capturing transaction: {}", transactionId);
        
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (transaction.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                    String.format("Cannot capture transaction in %s state", transaction.getStatus()));
        }
        
        // Update to CAPTURED status
        return updateTransactionStatus(transactionId, PaymentStatus.CAPTURED);
    }
    
    @Override
    public PaymentTransaction capturePartialTransaction(UUID transactionId, BigDecimal amount) {
        logger.debug("Capturing partial transaction: {} with amount: {}", transactionId, amount);
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Capture amount must be positive");
        }
        
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (transaction.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                    String.format("Cannot capture transaction in %s state", transaction.getStatus()));
        }
        
        if (amount.compareTo(transaction.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Capture amount cannot exceed the authorized amount");
        }
        
        // For partial captures, we need to track the captured amount
        // This would typically involve additional data structures and logic
        // For this implementation, we'll record the partial capture as an event
        // and update the transaction status
        
        // Record partial capture event with amount information
        eventDAO.recordTransactionEvent(
                transactionId,
                "PARTIAL_CAPTURE",
                PaymentStatus.AUTHORIZED,
                PaymentStatus.PARTIALLY_CAPTURED,
                String.format("Partial capture of %s %s", amount, transaction.getCurrency()),
                amount);
        
        // Update to PARTIALLY_CAPTURED status if it's a partial amount, otherwise CAPTURED
        PaymentStatus newStatus = amount.compareTo(transaction.getAmount()) < 0 
                ? PaymentStatus.PARTIALLY_CAPTURED 
                : PaymentStatus.CAPTURED;
        
        return updateTransactionStatus(transactionId, newStatus);
    }
    
    @Override
    public PaymentTransaction refundTransaction(UUID transactionId) {
        logger.debug("Refunding transaction: {}", transactionId);
        
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (transaction.getStatus() != PaymentStatus.CAPTURED && 
            transaction.getStatus() != PaymentStatus.PARTIALLY_CAPTURED) {
            throw new IllegalStateException(
                    String.format("Cannot refund transaction in %s state", transaction.getStatus()));
        }
        
        // Update to REFUNDED status
        return updateTransactionStatus(transactionId, PaymentStatus.REFUNDED);
    }
    
    @Override
    public PaymentTransaction refundPartialTransaction(UUID transactionId, BigDecimal amount) {
        logger.debug("Refunding partial transaction: {} with amount: {}", transactionId, amount);
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (transaction.getStatus() != PaymentStatus.CAPTURED && 
            transaction.getStatus() != PaymentStatus.PARTIALLY_CAPTURED) {
            throw new IllegalStateException(
                    String.format("Cannot refund transaction in %s state", transaction.getStatus()));
        }
        
        if (amount.compareTo(transaction.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount cannot exceed the captured amount");
        }
        
        // For partial refunds, we need to track the refunded amount
        // This would typically involve additional data structures and logic
        // For this implementation, we'll record the partial refund as an event
        // and update the transaction status
        
        // Record partial refund event with amount information
        eventDAO.recordTransactionEvent(
                transactionId,
                "PARTIAL_REFUND",
                transaction.getStatus(),
                PaymentStatus.PARTIALLY_REFUNDED,
                String.format("Partial refund of %s %s", amount, transaction.getCurrency()),
                amount);
        
        // Update to PARTIALLY_REFUNDED status if it's a partial amount, otherwise REFUNDED
        PaymentStatus newStatus = amount.compareTo(transaction.getAmount()) < 0 
                ? PaymentStatus.PARTIALLY_REFUNDED 
                : PaymentStatus.REFUNDED;
        
        return updateTransactionStatus(transactionId, newStatus);
    }
    
    @Override
    public PaymentTransaction voidTransaction(UUID transactionId) {
        logger.debug("Voiding transaction: {}", transactionId);
        
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (transaction.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                    String.format("Cannot void transaction in %s state", transaction.getStatus()));
        }
        
        // Update to VOIDED status
        return updateTransactionStatus(transactionId, PaymentStatus.VOIDED);
    }
    
    @Override
    public PaymentTransaction failTransaction(UUID transactionId, String errorReason) {
        logger.debug("Marking transaction as failed: {} with reason: {}", transactionId, errorReason);
        
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        // Get current transaction
        PaymentTransaction transaction = getTransactionById(transactionId);
        PaymentStatus currentStatus = transaction.getStatus();
        
        // Only allow failing transactions that are not in a final state
        if (PaymentStatus.isFinalStatus(currentStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot fail transaction in final state: %s", currentStatus));
        }
        
        // Update to FAILED status
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setUpdatedAt(Instant.now());
        
        // Persist updated transaction
        PaymentTransaction updatedTransaction = transactionDAO.update(transaction);
        
        // Record failure event
        eventDAO.recordTransactionEvent(
                transactionId,
                "TRANSACTION_FAILED",
                currentStatus,
                PaymentStatus.FAILED,
                errorReason != null ? errorReason : "Transaction failed",
                null);
        
        logger.info("Marked transaction as failed: {} from {} to FAILED, reason: {}", 
                transactionId, currentStatus, errorReason);
        
        return updatedTransaction;
    }
    
    @Override
    public void validateTransaction(PaymentTransaction transaction) {
        logger.debug("Validating transaction: {}", transaction);
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        validationService.validateTransaction(transaction);
    }
    
    @Override
    public boolean canProcessTransaction(UUID transactionId) {
        logger.debug("Checking if transaction can be processed: {}", transactionId);
        
        try {
            PaymentTransaction transaction = getTransactionById(transactionId);
            return transaction.getStatus() == PaymentStatus.CREATED;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public boolean canCaptureTransaction(UUID transactionId) {
        logger.debug("Checking if transaction can be captured: {}", transactionId);
        
        try {
            PaymentTransaction transaction = getTransactionById(transactionId);
            return transaction.getStatus() == PaymentStatus.AUTHORIZED;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public boolean canRefundTransaction(UUID transactionId) {
        logger.debug("Checking if transaction can be refunded: {}", transactionId);
        
        try {
            PaymentTransaction transaction = getTransactionById(transactionId);
            return transaction.getStatus() == PaymentStatus.CAPTURED || 
                   transaction.getStatus() == PaymentStatus.PARTIALLY_CAPTURED;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public boolean canVoidTransaction(UUID transactionId) {
        logger.debug("Checking if transaction can be voided: {}", transactionId);
        
        try {
            PaymentTransaction transaction = getTransactionById(transactionId);
            return transaction.getStatus() == PaymentStatus.AUTHORIZED;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Validates the parameters for creating a transaction.
     * 
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param amount The transaction amount
     * @param currency The currency code
     * @param merchantId The merchant identifier
     * @param paymentType The payment method type
     * @throws IllegalArgumentException if any parameters are invalid
     */
    private void validateCreateTransactionParams(
            UUID organizationId,
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantId,
            PaymentType paymentType) {
        
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        
        // Validate currency code
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currency);
        }
        
        if (merchantId == null || merchantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Merchant ID cannot be null or empty");
        }
        
        if (paymentType == null) {
            throw new IllegalArgumentException("Payment type cannot be null");
        }
    }
}