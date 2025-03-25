package io.briklabs.sample.payments.data.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity class for fee information related to payment transactions.
 * <p>
 * This entity maps to the payment_fees table and captures fee amounts, fee types,
 * and associated metadata for each transaction. It enables detailed financial reporting,
 * fee analysis, and reconciliation by maintaining a complete record of all charges
 * associated with payment processing.
 * </p>
 * <p>
 * The entity maintains a relationship to the parent transaction through the transaction_id
 * foreign key, implementing the one-to-many relationship from transactions to fees.
 * </p>
 */
public class PaymentFeeEntity extends PaymentEntityBase {

    /**
     * Unique fee identifier (PRIMARY KEY).
     */
    private UUID feeId;

    /**
     * Associated transaction identifier (FOREIGN KEY).
     */
    private UUID transactionId;

    /**
     * Fee classification (e.g., TRANSACTION_FEE, SERVICE_CHARGE, PROCESSING_FEE).
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
     * Fee description.
     */
    private String description;

    /**
     * External fee reference for reconciliation.
     */
    private String feeReference;

    /**
     * Creation timestamp.
     */
    private Instant createdAt;

    /**
     * Default constructor for ORM frameworks.
     */
    public PaymentFeeEntity() {
        // Default constructor for ORM frameworks
    }

    /**
     * Creates a new fee entity with required fields.
     *
     * @param transactionId The associated transaction identifier
     * @param feeType The fee classification
     * @param amount The fee amount
     * @param currency The currency code (ISO 4217)
     */
    public PaymentFeeEntity(UUID transactionId, String feeType, BigDecimal amount, String currency) {
        this.feeId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.feeType = feeType;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new fee entity with all fields.
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
    public PaymentFeeEntity(UUID feeId, UUID transactionId, String feeType, BigDecimal amount,
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
        if (feeId == null) {
            throw new IllegalArgumentException("Fee ID is required");
        }
        
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
        
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp is required");
        }
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentFee domain model object
     */
    public io.briklabs.sample.payments.model.PaymentFee toDomainModel() {
        return new io.briklabs.sample.payments.model.PaymentFee(
            feeId,
            transactionId,
            feeType,
            amount,
            currency,
            description,
            feeReference,
            createdAt
        );
    }

    /**
     * Creates an entity from a domain model object.
     *
     * @param domainModel The domain model object
     * @return A PaymentFeeEntity
     */
    public static PaymentFeeEntity fromDomainModel(
            io.briklabs.sample.payments.model.PaymentFee domainModel) {
        return new PaymentFeeEntity(
            domainModel.getFeeId(),
            domainModel.getTransactionId(),
            domainModel.getFeeType(),
            domainModel.getAmount(),
            domainModel.getCurrency(),
            domainModel.getDescription(),
            domainModel.getFeeReference(),
            domainModel.getCreatedAt()
        );
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
        PaymentFeeEntity that = (PaymentFeeEntity) o;
        return Objects.equals(feeId, that.feeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeId);
    }

    @Override
    public String toString() {
        return "PaymentFeeEntity{" +
                "feeId=" + feeId +
                ", transactionId=" + transactionId +
                ", feeType='" + feeType + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", feeReference='" + feeReference + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}