package io.briklabs.sample.payments.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Core model class representing a payment transaction with all essential transaction details.
 * This class encapsulates transaction identification, financial data, processing status, and
 * associated metadata. It serves as the central entity for payment processing, capturing
 * transaction amounts, currency, merchant information, and lifecycle state.
 */
public class PaymentTransaction {

    /**
     * Enum representing the possible states of a payment transaction throughout its lifecycle.
     * The transaction progresses through these states as it moves from creation to completion.
     */
    public enum PaymentStatus {
        /**
         * Initial state when a transaction is first created but not yet processed.
         */
        PENDING,
        
        /**
         * Transaction has been processed and funds have been authorized but not captured.
         */
        AUTHORIZED,
        
        /**
         * Transaction has been fully captured and funds have been transferred.
         */
        CAPTURED,
        
        /**
         * Transaction has been partially captured, with remaining funds still authorized.
         */
        PARTIALLY_CAPTURED,
        
        /**
         * Transaction has been fully refunded after being captured.
         */
        REFUNDED,
        
        /**
         * Transaction has been partially refunded, with some funds still captured.
         */
        PARTIALLY_REFUNDED,
        
        /**
         * Transaction has been voided before capture, releasing the authorization.
         */
        VOIDED,
        
        /**
         * Transaction has failed during processing due to an error.
         */
        FAILED,
        
        /**
         * Transaction has been declined by the payment processor.
         */
        DECLINED,
        
        /**
         * Transaction is currently being processed asynchronously.
         */
        PROCESSING
    }

    // Primary identifier for the transaction
    private UUID transactionId;
    
    // Organizational hierarchy identifiers
    private UUID organizationId;
    private UUID accountId;
    
    // Transaction status in the payment lifecycle
    private PaymentStatus status;
    
    // Financial information
    private BigDecimal amount;
    private String currency;
    
    // Merchant information
    private String merchantId;
    private String paymentType;
    
    // Reference information
    private String transactionReference;
    private String description;
    
    // Audit timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Default constructor for serialization frameworks.
     */
    public PaymentTransaction() {
    }

    /**
     * Creates a new payment transaction with required fields.
     *
     * @param transactionId Unique identifier for the transaction
     * @param organizationId Organization that owns this transaction
     * @param accountId Account associated with this transaction
     * @param status Current status of the transaction
     * @param amount Transaction amount
     * @param currency ISO 4217 currency code (3 characters)
     * @param merchantId External merchant identifier
     * @param paymentType Payment method type
     */
    public PaymentTransaction(UUID transactionId, UUID organizationId, UUID accountId,
                             PaymentStatus status, BigDecimal amount, String currency,
                             String merchantId, String paymentType) {
        this.transactionId = transactionId;
        this.organizationId = organizationId;
        this.accountId = accountId;
        this.status = status;
        this.amount = amount;
        this.currency = validateCurrency(currency);
        this.merchantId = merchantId;
        this.paymentType = paymentType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Validates that the currency code is a valid 3-character ISO 4217 code.
     *
     * @param currency The currency code to validate
     * @return The validated currency code
     * @throws IllegalArgumentException if the currency code is invalid
     */
    private String validateCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO 4217 code");
        }
        return currency.toUpperCase();
    }

    /**
     * Updates the transaction status and sets the updated timestamp.
     *
     * @param newStatus The new status to set
     */
    public void updateStatus(PaymentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if the transaction can transition to the specified status based on its current status.
     * Implements the payment transaction state machine rules.
     *
     * @param newStatus The status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(PaymentStatus newStatus) {
        switch (this.status) {
            case PENDING:
                // Pending can transition to authorized, declined, failed, or processing
                return newStatus == PaymentStatus.AUTHORIZED || 
                       newStatus == PaymentStatus.DECLINED || 
                       newStatus == PaymentStatus.FAILED ||
                       newStatus == PaymentStatus.PROCESSING;
                
            case PROCESSING:
                // Processing can transition to authorized, declined, or failed
                return newStatus == PaymentStatus.AUTHORIZED || 
                       newStatus == PaymentStatus.DECLINED || 
                       newStatus == PaymentStatus.FAILED;
                
            case AUTHORIZED:
                // Authorized can transition to captured, partially_captured, voided, or failed
                return newStatus == PaymentStatus.CAPTURED || 
                       newStatus == PaymentStatus.PARTIALLY_CAPTURED || 
                       newStatus == PaymentStatus.VOIDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case PARTIALLY_CAPTURED:
                // Partially captured can transition to captured, partially_refunded, or failed
                return newStatus == PaymentStatus.CAPTURED || 
                       newStatus == PaymentStatus.PARTIALLY_REFUNDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case CAPTURED:
                // Captured can transition to refunded, partially_refunded, or failed
                return newStatus == PaymentStatus.REFUNDED || 
                       newStatus == PaymentStatus.PARTIALLY_REFUNDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case PARTIALLY_REFUNDED:
                // Partially refunded can transition to refunded or failed
                return newStatus == PaymentStatus.REFUNDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case REFUNDED:
            case VOIDED:
            case DECLINED:
            case FAILED:
                // Terminal states - no further transitions allowed
                return false;
                
            default:
                return false;
        }
    }

    // Getters and setters

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = validateCurrency(currency);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "transactionId=" + transactionId +
                ", organizationId=" + organizationId +
                ", accountId=" + accountId +
                ", status=" + status +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", transactionReference='" + transactionReference + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}