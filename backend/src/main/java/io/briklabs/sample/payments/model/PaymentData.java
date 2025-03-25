package io.briklabs.sample.payments.model;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Model class representing payment method details for a transaction.
 * This class stores payment instrument information including tokenized data,
 * payment method identification, and associated billing information.
 * It supports different payment methods through a flexible structure,
 * enabling secure storage of sensitive payment data.
 */
public class PaymentData {

    /**
     * Unique identifier for the payment data record.
     */
    private UUID paymentDataId;

    /**
     * Associated transaction identifier.
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
     * Flexible payment method details stored as JSON.
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
     * Associated billing information stored as JSON.
     * This can include billing address, contact information, etc.
     */
    private String billingData;

    /**
     * Default constructor for serialization frameworks.
     */
    public PaymentData() {
        // Default constructor for serialization frameworks
    }

    /**
     * Creates a new payment data record with required fields.
     *
     * @param transactionId The associated transaction identifier
     * @param paymentMethodId The payment method identifier
     */
    public PaymentData(UUID transactionId, String paymentMethodId) {
        this.paymentDataId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.paymentMethodId = paymentMethodId;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new payment data record with all fields.
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
    public PaymentData(UUID paymentDataId, UUID transactionId, String paymentMethodId,
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
     * Validates the payment data.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (paymentMethodId == null || paymentMethodId.isEmpty()) {
            throw new IllegalArgumentException("Payment method ID is required");
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
     * This method returns a copy of the payment data with sensitive information masked.
     *
     * @return A new PaymentData instance with masked sensitive data
     */
    public PaymentData maskedCopy() {
        PaymentData masked = new PaymentData();
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
        PaymentData that = (PaymentData) o;
        return Objects.equals(paymentDataId, that.paymentDataId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentDataId);
    }

    @Override
    public String toString() {
        // Use maskedCopy to ensure sensitive data isn't accidentally logged
        PaymentData masked = this.maskedCopy();
        return "PaymentData{" +
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