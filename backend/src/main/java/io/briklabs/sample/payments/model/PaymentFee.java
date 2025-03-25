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
     * Enum representing the standardized fee types for payment transactions.
     * These categories allow for consistent classification and reporting of fees.
     */
    public enum FeeType {
        /**
         * Processing fee charged by the payment processor.
         */
        PROCESSING,
        
        /**
         * Fee for currency conversion when transaction currency differs from settlement currency.
         */
        CURRENCY_CONVERSION,
        
        /**
         * Fee charged for international transactions.
         */
        INTERNATIONAL,
        
        /**
         * Fee charged for specific payment methods (e.g., premium cards).
         */
        PAYMENT_METHOD,
        
        /**
         * Service fee charged by the platform.
         */
        SERVICE,
        
        /**
         * Fee for additional services or features.
         */
        ADDITIONAL_SERVICE,
        
        /**
         * Tax applied to the transaction or other fees.
         */
        TAX,
        
        /**
         * Miscellaneous fees that don't fit other categories.
         */
        OTHER
    }

    /**
     * Unique identifier for the fee.
     */
    private UUID feeId;
    
    /**
     * Identifier of the transaction this fee is associated with.
     */
    private UUID transactionId;
    
    /**
     * Type of fee for classification and reporting.
     */
    private FeeType feeType;
    
    /**
     * Amount of the fee.
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
     * Creates a new payment fee with required fields.
     *
     * @param transactionId The transaction identifier
     * @param feeType The type of fee
     * @param amount The fee amount
     * @param currency The ISO 4217 currency code
     */
    public PaymentFee(UUID transactionId, FeeType feeType, BigDecimal amount, String currency) {
        this.feeId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.feeType = feeType;
        this.amount = amount;
        this.currency = validateCurrency(currency);
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new payment fee with all fields.
     *
     * @param feeId The fee identifier
     * @param transactionId The transaction identifier
     * @param feeType The type of fee
     * @param amount The fee amount
     * @param currency The ISO 4217 currency code
     * @param description The fee description
     * @param feeReference The external fee reference
     * @param createdAt The creation timestamp
     */
    public PaymentFee(UUID feeId, UUID transactionId, FeeType feeType, BigDecimal amount,
                     String currency, String description, String feeReference, Instant createdAt) {
        this.feeId = feeId;
        this.transactionId = transactionId;
        this.feeType = feeType;
        this.amount = amount;
        this.currency = validateCurrency(currency);
        this.description = description;
        this.feeReference = feeReference;
        this.createdAt = createdAt;
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
     * Creates a processing fee for a transaction.
     *
     * @param transaction The transaction to associate the fee with
     * @param amount The fee amount
     * @param description The fee description
     * @return A new PaymentFee representing the processing fee
     */
    public static PaymentFee createProcessingFee(PaymentTransaction transaction, BigDecimal amount, String description) {
        PaymentFee fee = new PaymentFee();
        fee.setFeeId(UUID.randomUUID());
        fee.setTransactionId(transaction.getTransactionId());
        fee.setFeeType(FeeType.PROCESSING);
        fee.setAmount(amount);
        fee.setCurrency(transaction.getCurrency());
        fee.setDescription(description);
        fee.setCreatedAt(Instant.now());
        return fee;
    }

    /**
     * Creates a service fee for a transaction.
     *
     * @param transaction The transaction to associate the fee with
     * @param amount The fee amount
     * @param description The fee description
     * @return A new PaymentFee representing the service fee
     */
    public static PaymentFee createServiceFee(PaymentTransaction transaction, BigDecimal amount, String description) {
        PaymentFee fee = new PaymentFee();
        fee.setFeeId(UUID.randomUUID());
        fee.setTransactionId(transaction.getTransactionId());
        fee.setFeeType(FeeType.SERVICE);
        fee.setAmount(amount);
        fee.setCurrency(transaction.getCurrency());
        fee.setDescription(description);
        fee.setCreatedAt(Instant.now());
        return fee;
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
        
        if (feeType == null) {
            throw new IllegalArgumentException("Fee type is required");
        }
        
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO 4217 code");
        }
        
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at timestamp is required");
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

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
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
        this.currency = validateCurrency(currency);
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
                ", feeType=" + feeType +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", feeReference='" + feeReference + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}