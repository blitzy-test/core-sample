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
 */
public class PaymentDataEntity extends PaymentEntityBase {

    /**
     * Unique identifier for the payment data record (PRIMARY KEY).
     */
    private UUID paymentDataId;

    /**
     * Associated transaction identifier (FOREIGN KEY).
     */
    private UUID transactionId;

    /**
     * Payment method identifier used for reference and tracking.
     */
    private String paymentMethodId;

    /**
     * Tokenized payment data for secure storage of sensitive information.
     */
    private String paymentToken;

    /**
     * Flexible payment method details stored as JSONB.
     * This can include method-specific attributes that vary by payment type.
     */
    private String paymentDetails;

    /**
     * Timestamp when the payment data was created.
     */
    private Instant createdAt;

    /**
     * Expiration date for the payment method, if applicable.
     */
    private LocalDate expiration;

    /**
     * Associated billing information stored as JSONB.
     * This can include billing address, contact information, etc.
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
        this.paymentDataId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.paymentMethodId = paymentMethodId;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new payment data entity with all fields.
     *
     * @param paymentDataId The payment data identifier
     * @param transactionId The associated transaction identifier
     * @param paymentMethodId The payment method identifier
     * @param paymentToken The tokenized payment data
     * @param paymentDetails The payment method details as JSONB
     * @param createdAt The creation timestamp
     * @param expiration The payment method expiration date
     * @param billingData The billing information as JSONB
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
    public void validate() {
        if (paymentDataId == null) {
            throw new IllegalArgumentException("Payment data ID is required");
        }
        
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (paymentMethodId == null || paymentMethodId.isEmpty()) {
            throw new IllegalArgumentException("Payment method ID is required");
        }
        
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp is required");
        }
        
        // If expiration date is provided, ensure it's not in the past
        if (expiration != null && expiration.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
    }

    /**
     * Checks if this payment method has expired.
     *
     * @return true if the payment method has expired, false otherwise or if no expiration is set
     */
    public boolean isExpired() {
        return expiration != null && expiration.isBefore(LocalDate.now());
    }

    /**
     * Masks sensitive payment data for display or logging purposes.
     * This method returns a copy of the payment data entity with sensitive information masked.
     *
     * @return A new PaymentDataEntity instance with masked sensitive data
     */
    public PaymentDataEntity maskedCopy() {
        PaymentDataEntity masked = new PaymentDataEntity();
        masked.paymentDataId = this.paymentDataId;
        masked.transactionId = this.transactionId;
        masked.paymentMethodId = this.paymentMethodId;
        masked.createdAt = this.createdAt;
        masked.expiration = this.expiration;
        
        // Mask the payment token if present
        if (this.paymentToken != null && !this.paymentToken.isEmpty()) {
            // Keep only the last 4 characters, mask the rest
            int length = this.paymentToken.length();
            if (length > 4) {
                masked.paymentToken = "****" + this.paymentToken.substring(length - 4);
            } else {
                masked.paymentToken = "****";
            }
        }
        
        // For payment details and billing data, we would need to parse the JSON
        // and selectively mask fields. For simplicity, we're just indicating
        // that these fields contain data but not showing the actual content.
        if (this.paymentDetails != null && !this.paymentDetails.isEmpty()) {
            masked.paymentDetails = "{...}";
        }
        
        if (this.billingData != null && !this.billingData.isEmpty()) {
            masked.billingData = "{...}";
        }
        
        return masked;
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentData domain model object
     */
    public PaymentData toDomainModel() {
        return new PaymentData(
            paymentDataId,
            transactionId,
            paymentMethodId,
            paymentToken,
            paymentDetails,
            createdAt,
            expiration,
            billingData
        );
    }

    /**
     * Creates an entity from a domain model object.
     *
     * @param domainModel The domain model object
     * @return A PaymentDataEntity
     */
    public static PaymentDataEntity fromDomainModel(PaymentData domainModel) {
        return new PaymentDataEntity(
            domainModel.getPaymentDataId(),
            domainModel.getTransactionId(),
            domainModel.getPaymentMethodId(),
            domainModel.getPaymentToken(),
            domainModel.getPaymentDetails(),
            domainModel.getCreatedAt(),
            domainModel.getExpiration(),
            domainModel.getBillingData()
        );
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
        // Use maskedCopy to ensure sensitive data isn't accidentally logged
        PaymentDataEntity masked = this.maskedCopy();
        return "PaymentDataEntity{" +
                "paymentDataId=" + masked.paymentDataId +
                ", transactionId=" + masked.transactionId +
                ", paymentMethodId='" + masked.paymentMethodId + '\'' +
                ", paymentToken='" + masked.paymentToken + '\'' +
                ", paymentDetails='" + masked.paymentDetails + '\'' +
                ", createdAt=" + masked.createdAt +
                ", expiration=" + masked.expiration +
                ", billingData='" + masked.billingData + '\'' +
                '}';
    }
}