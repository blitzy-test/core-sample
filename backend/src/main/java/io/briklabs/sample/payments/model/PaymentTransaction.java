package io.briklabs.sample.payments.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core model class representing a payment transaction with all essential transaction details.
 * This class encapsulates transaction identification, financial data, processing status, and associated metadata.
 * It serves as the central entity for payment processing, capturing transaction amounts, currency,
 * merchant information, and lifecycle state.
 */
public class PaymentTransaction {

    /**
     * Unique identifier for the transaction.
     */
    private UUID transactionId;

    /**
     * Identifier of the organization that owns this transaction.
     */
    private UUID organizationId;

    /**
     * Identifier of the account associated with this transaction.
     */
    private UUID accountId;

    /**
     * Current status of the transaction in its lifecycle.
     */
    private PaymentStatus status;

    /**
     * Transaction amount with precision of 4 decimal places.
     */
    private BigDecimal amount;

    /**
     * ISO 4217 currency code (3 characters).
     */
    private String currency;

    /**
     * Timestamp when the transaction was created.
     */
    private Instant createdAt;

    /**
     * Timestamp when the transaction was last updated.
     */
    private Instant updatedAt;

    /**
     * External merchant identifier associated with this transaction.
     */
    private String merchantId;

    /**
     * Type of payment method used for this transaction.
     */
    private PaymentType paymentType;

    /**
     * External reference number for this transaction.
     */
    private String transactionReference;

    /**
     * Human-readable description of the transaction.
     */
    private String description;

    /**
     * Default constructor for serialization frameworks.
     */
    public PaymentTransaction() {
        // Default constructor for serialization frameworks
    }

    /**
     * Creates a new transaction with required fields.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param amount The transaction amount
     * @param currency The currency code (ISO 4217)
     * @param merchantId The merchant identifier
     * @param paymentType The payment method type
     */
    public PaymentTransaction(UUID organizationId, UUID accountId, BigDecimal amount, 
                             String currency, String merchantId, PaymentType paymentType) {
        this.transactionId = UUID.randomUUID();
        this.organizationId = organizationId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.paymentType = paymentType;
        this.status = PaymentStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Creates a new transaction with all fields.
     *
     * @param transactionId The transaction identifier
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param status The transaction status
     * @param amount The transaction amount
     * @param currency The currency code (ISO 4217)
     * @param createdAt The creation timestamp
     * @param updatedAt The last update timestamp
     * @param merchantId The merchant identifier
     * @param paymentType The payment method type
     * @param transactionReference The external reference number
     * @param description The transaction description
     */
    public PaymentTransaction(UUID transactionId, UUID organizationId, UUID accountId, 
                             PaymentStatus status, BigDecimal amount, String currency, 
                             Instant createdAt, Instant updatedAt, String merchantId, 
                             PaymentType paymentType, String transactionReference, 
                             String description) {
        this.transactionId = transactionId;
        this.organizationId = organizationId;
        this.accountId = accountId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.merchantId = merchantId;
        this.paymentType = paymentType;
        this.transactionReference = transactionReference;
        this.description = description;
    }

    /**
     * Updates the transaction status and sets the updated timestamp.
     *
     * @param newStatus The new status to set
     * @throws IllegalStateException if the status transition is not allowed
     */
    public void updateStatus(PaymentStatus newStatus) {
        // Validate that the status transition is allowed
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", status, newStatus));
        }
        
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the transaction is in a final state.
     *
     * @return true if the transaction is in a final state, false otherwise
     */
    public boolean isInFinalState() {
        return status.isFinalState();
    }

    /**
     * Checks if the transaction can be captured.
     *
     * @return true if the transaction can be captured, false otherwise
     */
    public boolean canCapture() {
        return status == PaymentStatus.AUTHORIZED;
    }

    /**
     * Checks if the transaction can be refunded.
     *
     * @return true if the transaction can be refunded, false otherwise
     */
    public boolean canRefund() {
        return status == PaymentStatus.CAPTURED;
    }

    /**
     * Checks if the transaction can be voided.
     *
     * @return true if the transaction can be voided, false otherwise
     */
    public boolean canVoid() {
        return status == PaymentStatus.AUTHORIZED;
    }

    /**
     * Validates the transaction data.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO 4217 code");
        }
        
        if (merchantId == null || merchantId.isEmpty()) {
            throw new IllegalArgumentException("Merchant ID is required");
        }
        
        if (paymentType == null) {
            throw new IllegalArgumentException("Payment type is required");
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
        this.currency = currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
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
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", merchantId='" + merchantId + '\'' +
                ", paymentType=" + paymentType +
                ", transactionReference='" + transactionReference + '\'' +
                '}';
    }
}