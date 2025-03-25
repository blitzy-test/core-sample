package io.briklabs.sample.payments.data.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

/**
 * Entity class representing payment transactions in the database.
 * <p>
 * This entity maps to the payment_transaction table and serves as the core data model
 * for payment operations. It contains fields for transaction identifiers, organization
 * and account references, status, amount, currency, timestamps, and metadata.
 * </p>
 * <p>
 * The entity is essential for all payment processing operations and serves as the
 * central reference point for related payment entities (PaymentDataEntity, PaymentFeeEntity,
 * PaymentEventEntity) through one-to-many relationships.
 * </p>
 */
public class PaymentTransactionEntity extends PaymentEntityBase {

    /**
     * Unique transaction identifier (PRIMARY KEY).
     */
    private UUID transactionId;

    /**
     * Owning organization identifier (FOREIGN KEY).
     */
    private UUID organizationId;

    /**
     * Associated account identifier (FOREIGN KEY).
     */
    private UUID accountId;

    /**
     * Current transaction status.
     * Maps to the payment lifecycle states as defined in the state diagram.
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
     * Creation timestamp.
     */
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    private Instant updatedAt;

    /**
     * External merchant identifier.
     */
    private String merchantId;

    /**
     * Payment method type (e.g., CREDIT_CARD, ACH, WIRE_TRANSFER).
     */
    private String paymentType;

    /**
     * External reference number for the transaction.
     */
    private String transactionReference;

    /**
     * Transaction description.
     */
    private String description;

    /**
     * Default constructor for ORM frameworks.
     */
    public PaymentTransactionEntity() {
        // Default constructor for ORM frameworks
    }

    /**
     * Creates a new transaction entity with required fields.
     *
     * @param organizationId The owning organization identifier
     * @param accountId The associated account identifier
     * @param amount The transaction amount
     * @param currency The currency code (ISO 4217)
     * @param merchantId The external merchant identifier
     * @param paymentType The payment method type
     */
    public PaymentTransactionEntity(UUID organizationId, UUID accountId, BigDecimal amount,
                                   String currency, String merchantId, String paymentType) {
        this.transactionId = generateId();
        this.organizationId = organizationId;
        this.accountId = accountId;
        this.status = PaymentStatus.CREATED;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.paymentType = paymentType;
        this.createdAt = getCurrentTimestamp();
        this.updatedAt = this.createdAt;
    }

    /**
     * Creates a new transaction entity with all fields.
     *
     * @param transactionId The transaction identifier
     * @param organizationId The owning organization identifier
     * @param accountId The associated account identifier
     * @param status The transaction status
     * @param amount The transaction amount
     * @param currency The currency code (ISO 4217)
     * @param createdAt The creation timestamp
     * @param updatedAt The last update timestamp
     * @param merchantId The external merchant identifier
     * @param paymentType The payment method type
     * @param transactionReference The external reference number
     * @param description The transaction description
     */
    public PaymentTransactionEntity(UUID transactionId, UUID organizationId, UUID accountId,
                                   PaymentStatus status, BigDecimal amount, String currency,
                                   Instant createdAt, Instant updatedAt, String merchantId,
                                   String paymentType, String transactionReference, String description) {
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
     * Validates the transaction data.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    @Override
    public void validate() {
        validateId(transactionId, "Transaction ID");
        validateId(organizationId, "Organization ID");
        validateId(accountId, "Account ID");
        
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        validateCurrency(currency);
        validateTimestamp(createdAt, "Created");
        validateTimestamp(updatedAt, "Updated");
        validateRequiredString(merchantId, "Merchant ID");
        validateRequiredString(paymentType, "Payment type");
    }

    /**
     * Updates the transaction status.
     * <p>
     * This method updates the status of the transaction and sets the updated timestamp.
     * It also validates that the status transition is allowed according to the payment
     * lifecycle state machine.
     * </p>
     *
     * @param newStatus The new status to set
     * @throws IllegalArgumentException if the status transition is not allowed
     */
    public void updateStatus(PaymentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        // Validate that the status transition is allowed
        if (!isValidStatusTransition(this.status, newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + this.status + " to " + newStatus);
        }
        
        this.status = newStatus;
        this.updatedAt = getCurrentTimestamp();
    }

    /**
     * Checks if a status transition is valid according to the payment lifecycle state machine.
     *
     * @param currentStatus The current status
     * @param newStatus The new status
     * @return true if the transition is valid, false otherwise
     */
    private boolean isValidStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        switch (currentStatus) {
            case CREATED:
                return newStatus == PaymentStatus.PROCESSING;
                
            case PROCESSING:
                return newStatus == PaymentStatus.AUTHORIZED || newStatus == PaymentStatus.FAILED;
                
            case AUTHORIZED:
                return newStatus == PaymentStatus.PARTIALLY_SETTLED || 
                       newStatus == PaymentStatus.FULLY_SETTLED || 
                       newStatus == PaymentStatus.VOIDED || 
                       newStatus == PaymentStatus.EXPIRED;
                
            case PARTIALLY_SETTLED:
                return newStatus == PaymentStatus.FULLY_SETTLED || 
                       newStatus == PaymentStatus.PARTIALLY_REFUNDED;
                
            case FULLY_SETTLED:
                return newStatus == PaymentStatus.PARTIALLY_REFUNDED || 
                       newStatus == PaymentStatus.FULLY_REFUNDED;
                
            case PARTIALLY_REFUNDED:
                return newStatus == PaymentStatus.FULLY_REFUNDED;
                
            case FAILED:
            case VOIDED:
            case EXPIRED:
            case FULLY_REFUNDED:
                // Terminal states - no further transitions allowed
                return false;
                
            default:
                return false;
        }
    }

    /**
     * Prepares the entity for persistence operations.
     * <p>
     * This method sets creation and update timestamps if they are not already set,
     * and generates a transaction ID if it is not already set.
     * </p>
     */
    @Override
    public void prepareForPersistence() {
        if (transactionId == null) {
            transactionId = generateId();
        }
        
        Instant now = getCurrentTimestamp();
        
        if (createdAt == null) {
            createdAt = now;
        }
        
        if (updatedAt == null) {
            updatedAt = now;
        }
        
        if (status == null) {
            status = PaymentStatus.CREATED;
        }
    }

    /**
     * Updates the entity's audit information before an update operation.
     * <p>
     * This method updates the updatedAt timestamp to the current time.
     * </p>
     */
    @Override
    public void prepareForUpdate() {
        updatedAt = getCurrentTimestamp();
    }

    /**
     * Checks if this entity is new (not yet persisted).
     *
     * @return true if the entity is new, false otherwise
     */
    @Override
    public boolean isNew() {
        return createdAt == null || updatedAt == null;
    }

    /**
     * Gets the primary key of this entity.
     *
     * @return The transaction ID (primary key)
     */
    @Override
    public UUID getId() {
        return transactionId;
    }

    /**
     * Creates a deep copy of the entity.
     *
     * @return A new PaymentTransactionEntity with the same values
     */
    @Override
    public Object clone() {
        return new PaymentTransactionEntity(
            this.transactionId,
            this.organizationId,
            this.accountId,
            this.status,
            this.amount,
            this.currency,
            this.createdAt,
            this.updatedAt,
            this.merchantId,
            this.paymentType,
            this.transactionReference,
            this.description
        );
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentTransaction domain model object
     */
    @Override
    public PaymentTransaction toDomainModel() {
        return new PaymentTransaction(
            transactionId,
            organizationId,
            accountId,
            status,
            amount,
            currency,
            createdAt,
            updatedAt,
            merchantId,
            paymentType,
            transactionReference,
            description
        );
    }

    /**
     * Creates an entity from a domain model object.
     *
     * @param domainModel The domain model object
     * @return A PaymentTransactionEntity
     */
    public static PaymentTransactionEntity fromDomainModel(PaymentTransaction domainModel) {
        return new PaymentTransactionEntity(
            domainModel.getTransactionId(),
            domainModel.getOrganizationId(),
            domainModel.getAccountId(),
            domainModel.getStatus(),
            domainModel.getAmount(),
            domainModel.getCurrency(),
            domainModel.getCreatedAt(),
            domainModel.getUpdatedAt(),
            domainModel.getMerchantId(),
            domainModel.getPaymentType(),
            domainModel.getTransactionReference(),
            domainModel.getDescription()
        );
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
        PaymentTransactionEntity that = (PaymentTransactionEntity) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "PaymentTransactionEntity{" +
                "transactionId=" + transactionId +
                ", organizationId=" + organizationId +
                ", accountId=" + accountId +
                ", status=" + status +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", merchantId='" + merchantId + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", transactionReference='" + transactionReference + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}