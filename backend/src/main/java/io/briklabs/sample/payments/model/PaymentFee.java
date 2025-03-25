package io.briklabs.sample.payments.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Model class representing fee information associated with payment transactions.
 * This class tracks individual fees with amount, currency, fee type, and description attributes.
 * It enables comprehensive fee management for financial reporting, reconciliation, and analysis.
 */
public class PaymentFee {

    /**
     * Unique identifier for the fee.
     */
    private UUID feeId;

    /**
     * Identifier of the transaction this fee is associated with.
     */
    private UUID transactionId;

    /**
     * Classification of the fee (e.g., PROCESSING, INTERCHANGE, SERVICE).
     */
    private String feeType;

    /**
     * Fee amount with precision of 4 decimal places.
     */
    private BigDecimal amount;

    /**
     * ISO 4217 currency code (3 characters).
     */
    private String currency;

    /**
     * Human-readable description of the fee.
     */
    private String description;

    /**
     * External reference identifier for the fee.
     */
    private String feeReference;

    /**
     * Timestamp when the fee was created.
     */
    private Instant createdAt;

    /**
     * Default constructor for serialization frameworks.
     */
    public PaymentFee() {
        // Default constructor for serialization frameworks
    }

    /**
     * Creates a new fee with required fields.
     *
     * @param transactionId The associated transaction identifier
     * @param feeType The fee classification
     * @param amount The fee amount
     * @param currency The currency code (ISO 4217)
     */
    public PaymentFee(UUID transactionId, String feeType, BigDecimal amount, String currency) {
        this.feeId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.feeType = feeType;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new fee with all fields.
     *
     * @param feeId The fee identifier
     * @param transactionId The associated transaction identifier
     * @param feeType The fee classification
     * @param amount The fee amount
     * @param currency The currency code (ISO 4217)
     * @param description The fee description
     * @param feeReference The external fee reference
     * @param createdAt The creation timestamp
     */
    public PaymentFee(UUID feeId, UUID transactionId, String feeType, BigDecimal amount,
                     String currency, String description, String feeReference, Instant createdAt) {
        this.feeId = feeId;
        this.transactionId = transactionId;
        this.feeType = feeType;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.feeReference = feeReference;
        this.createdAt = createdAt;
    }

    /**
     * Validates the fee data.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (feeType == null || feeType.isEmpty()) {
            throw new IllegalArgumentException("Fee type is required");
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO 4217 code");
        }
    }

    /**
     * Common fee types used in the system.
     */
    public static final class FeeTypes {
        public static final String PROCESSING = "PROCESSING";
        public static final String INTERCHANGE = "INTERCHANGE";
        public static final String SERVICE = "SERVICE";
        public static final String GATEWAY = "GATEWAY";
        public static final String ASSESSMENT = "ASSESSMENT";
        public static final String FOREIGN_EXCHANGE = "FOREIGN_EXCHANGE";
        public static final String CHARGEBACK = "CHARGEBACK";
        public static final String OTHER = "OTHER";
        
        private FeeTypes() {
            // Private constructor to prevent instantiation
        }
    }

    // Getters and setters

    public UUID getFeeId() {
        return feeId;
    }

    public void setFeeId(UUID feeId) {
        this.feeId = feeId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFeeReference() {
        return feeReference;
    }

    public void setFeeReference(String feeReference) {
        this.feeReference = feeReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentFee that = (PaymentFee) o;
        return Objects.equals(feeId, that.feeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeId);
    }

    @Override
    public String toString() {
        return "PaymentFee{" +
                "feeId=" + feeId +
                ", transactionId=" + transactionId +
                ", feeType='" + feeType + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", feeReference='" + feeReference + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}