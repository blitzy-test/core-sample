package io.briklabs.sample.payments.data.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentData;

/**
 * Entity class for payment method-specific details, mapping to the payment_data table.
 * <p>
 * This class stores tokenized payment information, payment method identifiers, and associated
 * billing data in structured format. It maintains a relationship to the parent transaction
 * through a foreign key and provides secure storage for payment instrument details while
 * supporting appropriate data masking and security controls.
 * </p>
 * <p>
 * The entity supports flexible payment method data through JSONB fields for payment_details
 * and billing_data, allowing for storage of different payment method types with varying
 * data requirements.
 * </p>
 */
public class PaymentDataEntity extends PaymentEntityBase {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for this payment data record (PRIMARY KEY).
     */
    private UUID paymentDataId;

    /**
     * Associated transaction identifier (FOREIGN KEY).
     */
    private UUID transactionId;

    /**
     * Identifier for the payment method.
     */
    private String paymentMethodId;

    /**
     * Tokenized payment data for secure storage.
     */
    private String paymentToken;

    /**
     * Flexible payment method details stored as JSONB.
     * This field contains method-specific data in a structured format.
     */
    private String paymentDetails;

    /**
     * Creation timestamp.
     */
    private Instant createdAt;

    /**
     * Payment method expiration date (if applicable).
     */
    private LocalDate expiration;

    /**
     * Billing information stored as JSONB.
     * This field contains address and billing contact information.
     */
    private String billingData;

    /**
     * Default constructor for ORM frameworks.
     */
    public PaymentDataEntity() {
        // Default constructor for ORM frameworks
    }

    /**
     * Creates a new payment data entity with required fields.
     *
     * @param transactionId The associated transaction identifier
     * @param paymentMethodId The payment method identifier
     */
    public PaymentDataEntity(UUID transactionId, String paymentMethodId) {
        this.paymentDataId = generateId();
        this.transactionId = transactionId;
        this.paymentMethodId = paymentMethodId;
        this.createdAt = getCurrentTimestamp();
    }

    /**
     * Creates a new payment data entity with all fields.
     *
     * @param paymentDataId The payment data identifier
     * @param transactionId The associated transaction identifier
     * @param paymentMethodId The payment method identifier
     * @param paymentToken The tokenized payment data
     * @param paymentDetails The payment method details as JSON
     * @param createdAt The creation timestamp
     * @param expiration The payment method expiration date
     * @param billingData The billing information as JSON
     */
    public PaymentDataEntity(UUID paymentDataId, UUID transactionId, String paymentMethodId,
                            String paymentToken, String paymentDetails, Instant createdAt,
                            LocalDate expiration, String billingData) {
        this.paymentDataId = paymentDataId;
        this.transactionId = transactionId;
        this.paymentMethodId = paymentMethodId;
        this.paymentToken = paymentToken;
        this.paymentDetails = paymentDetails;
        this.createdAt = createdAt;
        this.expiration = expiration;
        this.billingData = billingData;
    }

    /**
     * Validates the payment data entity.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    @Override
    public void validate() {
        validateId(paymentDataId, "Payment data ID");
        validateId(transactionId, "Transaction ID");
        validateRequiredString(paymentMethodId, "Payment method ID");
        validateTimestamp(createdAt, "Created");
    }

    /**
     * Prepares the entity for persistence operations.
     * <p>
     * This method sets the creation timestamp if it is not already set,
     * and generates a payment data ID if it is not already set.
     * </p>
     */
    @Override
    public void prepareForPersistence() {
        if (paymentDataId == null) {
            paymentDataId = generateId();
        }
        
        if (createdAt == null) {
            createdAt = getCurrentTimestamp();
        }
    }

    /**
     * Updates the entity's audit information before an update operation.
     * <p>
     * This method is a no-op for PaymentDataEntity as it doesn't have an updatedAt field.
     * Payment data records are generally immutable after creation for security and audit purposes.
     * </p>
     */
    @Override
    public void prepareForUpdate() {
        // Payment data is generally immutable after creation
        // No update timestamp to set
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
     * @return The payment data ID (primary key)
     */
    @Override
    public UUID getId() {
        return paymentDataId;
    }

    /**
     * Creates a deep copy of the entity.
     *
     * @return A new PaymentDataEntity with the same values
     */
    @Override
    public Object clone() {
        return new PaymentDataEntity(
            this.paymentDataId,
            this.transactionId,
            this.paymentMethodId,
            this.paymentToken,
            this.paymentDetails,
            this.createdAt,
            this.expiration,
            this.billingData
        );
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentData domain model object
     */
    @Override
    public PaymentData toDomainModel() {
        PaymentData model = new PaymentData();
        model.setPaymentDataId(this.paymentDataId);
        model.setTransactionId(this.transactionId);
        model.setPaymentMethodId(this.paymentMethodId);
        model.setPaymentToken(this.paymentToken);
        model.setPaymentDetails(this.paymentDetails);
        model.setExpiration(this.expiration);
        model.setBillingData(this.billingData);
        model.setCreatedAt(this.createdAt != null ? 
                this.createdAt.atZone(java.time.ZoneOffset.UTC).toLocalDateTime() : null);
        return model;
    }

    /**
     * Creates an entity from a domain model object.
     *
     * @param domainModel The domain model object
     * @return A PaymentDataEntity
     */
    public static PaymentDataEntity fromDomainModel(PaymentData domainModel) {
        if (domainModel == null) {
            return null;
        }
        
        return new PaymentDataEntity(
            domainModel.getPaymentDataId(),
            domainModel.getTransactionId(),
            domainModel.getPaymentMethodId(),
            domainModel.getPaymentToken(),
            domainModel.getPaymentDetails(),
            domainModel.getCreatedAt() != null ? 
                domainModel.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : null,
            domainModel.getExpiration(),
            domainModel.getBillingData()
        );
    }

    /**
     * Returns a copy of this entity with sensitive data masked for logging or display.
     * <p>
     * This method creates a new instance with the same identifiers but with sensitive
     * payment data masked to prevent accidental exposure.
     * </p>
     *
     * @return A new PaymentDataEntity with masked sensitive fields
     */
    public PaymentDataEntity getMaskedCopy() {
        PaymentDataEntity masked = new PaymentDataEntity();
        masked.setPaymentDataId(this.paymentDataId);
        masked.setTransactionId(this.transactionId);
        masked.setPaymentMethodId(this.paymentMethodId);
        
        // Mask the payment token if present
        if (this.paymentToken != null && !this.paymentToken.isEmpty()) {
            int length = this.paymentToken.length();
            if (length > 4) {
                masked.setPaymentToken("****" + this.paymentToken.substring(length - 4));
            } else {
                masked.setPaymentToken("****");
            }
        }
        
        // Don't include payment details in masked copy
        masked.setPaymentDetails(null);
        
        masked.setCreatedAt(this.createdAt);
        masked.setExpiration(this.expiration);
        
        // Don't include billing data in masked copy
        masked.setBillingData(null);
        
        return masked;
    }

    /**
     * Checks if the payment method has expired.
     *
     * @return true if the payment method has an expiration date and it has passed, false otherwise
     */
    public boolean isExpired() {
        return expiration != null && expiration.isBefore(LocalDate.now());
    }

    /**
     * Checks if the payment method will expire soon (within 30 days).
     *
     * @return true if the payment method will expire within 30 days, false otherwise
     */
    public boolean isExpiringSoon() {
        if (expiration == null) {
            return false;
        }
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return expiration.isAfter(LocalDate.now()) && expiration.isBefore(thirtyDaysFromNow);
    }

    // Getters and setters

    public UUID getPaymentDataId() {
        return paymentDataId;
    }

    public void setPaymentDataId(UUID paymentDataId) {
        this.paymentDataId = paymentDataId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getExpiration() {
        return expiration;
    }

    public void setExpiration(LocalDate expiration) {
        this.expiration = expiration;
    }

    public String getBillingData() {
        return billingData;
    }

    public void setBillingData(String billingData) {
        this.billingData = billingData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentDataEntity that = (PaymentDataEntity) o;
        return Objects.equals(paymentDataId, that.paymentDataId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentDataId);
    }

    @Override
    public String toString() {
        // Use masked copy for toString to prevent accidental logging of sensitive data
        PaymentDataEntity masked = getMaskedCopy();
        return "PaymentDataEntity{" +
                "paymentDataId=" + masked.paymentDataId +
                ", transactionId=" + masked.transactionId +
                ", paymentMethodId='" + masked.paymentMethodId + '\'' +
                ", paymentToken='" + masked.paymentToken + '\'' +
                ", expiration=" + masked.expiration +
                ", createdAt=" + masked.createdAt +
                '}';
    }
}