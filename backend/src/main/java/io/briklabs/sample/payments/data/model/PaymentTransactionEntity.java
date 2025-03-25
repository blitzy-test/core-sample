package io.briklabs.sample.payments.data.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentType;

/**
 * Entity class representing payment transactions in the database.
 * <p>
 * This entity maps to the payment_transaction table and serves as the core data model
 * for payment operations. It contains fields for transaction identifiers, organization
 * and account references, status, amount, currency, timestamps, and metadata.
 * </p>
 * <p>
 * The entity maintains relationships with related payment entities (PaymentDataEntity,
 * PaymentFeeEntity, PaymentEventEntity) through one-to-many associations.
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
     * Payment method type.
     */
    private PaymentType paymentType;

    /**
     * External reference number.
     */
    private String transactionReference;

    /**
     * Transaction description.
     */
    private String description;

    /**
     * Payment method details associated with this transaction.
     * One transaction can have multiple payment data records (for complex or multi-method payments).
     */
    private List<PaymentDataEntity> paymentData = new ArrayList<>();

    /**
     * Fee records associated with this transaction.
     * One transaction can have multiple fee records.
     */
    private List<PaymentFeeEntity> fees = new ArrayList<>();

    /**
     * Event records associated with this transaction.
     * One transaction generates multiple event records throughout its lifecycle.
     */
    private List<PaymentEventEntity> events = new ArrayList<>();

    /**
     * Default constructor for ORM frameworks.
     */
    public PaymentTransactionEntity() {
        // Default constructor for ORM frameworks
    }

    /**
     * Creates a new transaction entity with required fields.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param amount The transaction amount
     * @param currency The currency code (ISO 4217)
     * @param merchantId The merchant identifier
     * @param paymentType The payment method type
     */
    public PaymentTransactionEntity(UUID organizationId, UUID accountId, BigDecimal amount, 
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
     * Creates a new transaction entity with all fields.
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
    public PaymentTransactionEntity(UUID transactionId, UUID organizationId, UUID accountId, 
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
        PaymentStatus.validateTransition(this.status, newStatus);
        
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Adds a payment data entity to this transaction.
     *
     * @param paymentData The payment data entity to add
     */
    public void addPaymentData(PaymentDataEntity paymentData) {
        if (paymentData != null) {
            this.paymentData.add(paymentData);
            paymentData.setTransactionId(this.transactionId);
        }
    }

    /**
     * Adds a fee entity to this transaction.
     *
     * @param fee The fee entity to add
     */
    public void addFee(PaymentFeeEntity fee) {
        if (fee != null) {
            this.fees.add(fee);
            fee.setTransactionId(this.transactionId);
        }
    }

    /**
     * Adds an event entity to this transaction.
     *
     * @param event The event entity to add
     */
    public void addEvent(PaymentEventEntity event) {
        if (event != null) {
            this.events.add(event);
            event.setTransactionId(this.transactionId);
        }
    }

    /**
     * Records a status change event for this transaction.
     *
     * @param previousStatus The previous status
     * @param newStatus The new status
     * @param createdBy The user or system that created the event
     * @param correlationId Optional correlation ID for tracking related events
     * @return The created event entity
     */
    public PaymentEventEntity recordStatusChangeEvent(PaymentStatus previousStatus, 
                                                     PaymentStatus newStatus,
                                                     String createdBy,
                                                     UUID correlationId) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(this.transactionId);
        event.setEventType("STATUS_CHANGE");
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        event.setCorrelationId(correlationId);
        
        this.events.add(event);
        return event;
    }

    /**
     * Checks if the transaction is in a final state.
     *
     * @return true if the transaction is in a final state, false otherwise
     */
    public boolean isInFinalState() {
        return status != null && status.isFinalState();
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
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
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
        
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated timestamp is required");
        }
        
        if (merchantId == null || merchantId.isEmpty()) {
            throw new IllegalArgumentException("Merchant ID is required");
        }
        
        if (paymentType == null) {
            throw new IllegalArgumentException("Payment type is required");
        }
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentTransaction domain model object
     */
    public io.briklabs.sample.payments.model.PaymentTransaction toDomainModel() {
        return new io.briklabs.sample.payments.model.PaymentTransaction(
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
    public static PaymentTransactionEntity fromDomainModel(
            io.briklabs.sample.payments.model.PaymentTransaction domainModel) {
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

    public List<PaymentDataEntity> getPaymentData() {
        return paymentData;
    }

    public void setPaymentData(List<PaymentDataEntity> paymentData) {
        this.paymentData = paymentData != null ? paymentData : new ArrayList<>();
    }

    public List<PaymentFeeEntity> getFees() {
        return fees;
    }

    public void setFees(List<PaymentFeeEntity> fees) {
        this.fees = fees != null ? fees : new ArrayList<>();
    }

    public List<PaymentEventEntity> getEvents() {
        return events;
    }

    public void setEvents(List<PaymentEventEntity> events) {
        this.events = events != null ? events : new ArrayList<>();
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
                ", paymentType=" + paymentType +
                ", transactionReference='" + transactionReference + '\'' +
                ", paymentData.size=" + (paymentData != null ? paymentData.size() : 0) +
                ", fees.size=" + (fees != null ? fees.size() : 0) +
                ", events.size=" + (events != null ? events.size() : 0) +
                '}';
    }
}