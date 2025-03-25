package io.briklabs.sample.payments.data.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentFee;
import io.briklabs.sample.payments.model.PaymentFee.FeeType;

/**
 * Entity class for fee information related to payment transactions, mapping to the payment_fees table.
 * <p>
 * This entity captures fee amounts, fee types, and associated metadata for each transaction.
 * It enables detailed financial reporting, fee analysis, and reconciliation by maintaining
 * a complete record of all charges associated with payment processing, including transaction
 * fees, service charges, and other costs.
 * </p>
 * <p>
 * The entity maintains a relationship to the parent transaction through a foreign key and
 * provides comprehensive fee tracking capabilities for financial operations.
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
     * Fee classification (e.g., PROCESSING, SERVICE, INTERNATIONAL).
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
     * External fee reference identifier.
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
        this.feeId = generateId();
        this.transactionId = transactionId;
        this.feeType = feeType;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = getCurrentTimestamp();
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
    @Override
    public void validate() {
        validateId(feeId, "Fee ID");
        validateId(transactionId, "Transaction ID");
        validateRequiredString(feeType, "Fee type");
        
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        
        validateCurrency(currency);
        validateTimestamp(createdAt, "Created");
    }

    /**
     * Prepares the entity for persistence operations.
     * <p>
     * This method sets the creation timestamp if it is not already set,
     * and generates a fee ID if it is not already set.
     * </p>
     */
    @Override
    public void prepareForPersistence() {
        if (feeId == null) {
            feeId = generateId();
        }
        
        if (createdAt == null) {
            createdAt = getCurrentTimestamp();
        }
    }

    /**
     * Updates the entity's audit information before an update operation.
     * <p>
     * For fee entities, this method does nothing as fees are not updated
     * after creation (they are immutable records).
     * </p>
     */
    @Override
    public void prepareForUpdate() {
        // Fees are generally immutable after creation
        // No update logic required
    }

    /**
     * Checks if this entity is new (not yet persisted).
     *
     * @return true if the entity is new, false otherwise
     */
    @Override
    public boolean isNew() {
        return createdAt == null;
    }

    /**
     * Gets the primary key of this entity.
     *
     * @return The fee ID (primary key)
     */
    @Override
    public UUID getId() {
        return feeId;
    }

    /**
     * Creates a deep copy of the entity.
     *
     * @return A new PaymentFeeEntity with the same values
     */
    @Override
    public Object clone() {
        return new PaymentFeeEntity(
            this.feeId,
            this.transactionId,
            this.feeType,
            this.amount,
            this.currency,
            this.description,
            this.feeReference,
            this.createdAt
        );
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentFee domain model object
     */
    @Override
    public PaymentFee toDomainModel() {
        return new PaymentFee(
            feeId,
            transactionId,
            mapStringToFeeType(feeType),
            amount,
            currency,
            description,
            feeReference,
            createdAt
        );
    }

    /**
     * Maps a string fee type to the corresponding FeeType enum value.
     *
     * @param feeTypeStr The fee type string
     * @return The corresponding FeeType enum value
     */
    private FeeType mapStringToFeeType(String feeTypeStr) {
        try {
            return FeeType.valueOf(feeTypeStr);
        } catch (IllegalArgumentException e) {
            // Default to OTHER if the fee type is not recognized
            return FeeType.OTHER;
        }
    }

    /**
     * Creates an entity from a domain model object.
     *
     * @param domainModel The domain model object
     * @return A PaymentFeeEntity
     */
    public static PaymentFeeEntity fromDomainModel(PaymentFee domainModel) {
        return new PaymentFeeEntity(
            domainModel.getFeeId(),
            domainModel.getTransactionId(),
            domainModel.getFeeType().name(),
            domainModel.getAmount(),
            domainModel.getCurrency(),
            domainModel.getDescription(),
            domainModel.getFeeReference(),
            domainModel.getCreatedAt()
        );
    }

    /**
     * Creates a processing fee entity for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param amount The fee amount
     * @param currency The currency code
     * @param description The fee description
     * @return A new PaymentFeeEntity representing a processing fee
     */
    public static PaymentFeeEntity createProcessingFee(UUID transactionId, BigDecimal amount, 
                                                     String currency, String description) {
        PaymentFeeEntity fee = new PaymentFeeEntity();
        fee.setFeeId(UUID.randomUUID());
        fee.setTransactionId(transactionId);
        fee.setFeeType(FeeType.PROCESSING.name());
        fee.setAmount(amount);
        fee.setCurrency(currency);
        fee.setDescription(description);
        fee.setCreatedAt(Instant.now());
        return fee;
    }

    /**
     * Creates a service fee entity for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param amount The fee amount
     * @param currency The currency code
     * @param description The fee description
     * @return A new PaymentFeeEntity representing a service fee
     */
    public static PaymentFeeEntity createServiceFee(UUID transactionId, BigDecimal amount, 
                                                  String currency, String description) {
        PaymentFeeEntity fee = new PaymentFeeEntity();
        fee.setFeeId(UUID.randomUUID());
        fee.setTransactionId(transactionId);
        fee.setFeeType(FeeType.SERVICE.name());
        fee.setAmount(amount);
        fee.setCurrency(currency);
        fee.setDescription(description);
        fee.setCreatedAt(Instant.now());
        return fee;
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