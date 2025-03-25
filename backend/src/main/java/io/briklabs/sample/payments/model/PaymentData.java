package io.briklabs.sample.payments.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Primary identifier for the payment data
    private UUID paymentDataId;
    
    // Reference to the parent transaction
    private UUID transactionId;
    
    // Payment method identification
    private String paymentMethodId;
    
    // Tokenized payment data for secure storage
    private String paymentToken;
    
    // Flexible payment method details stored as JSON
    private String paymentDetails;
    
    // Payment method expiration date (if applicable)
    private LocalDate expiration;
    
    // Billing information stored as JSON
    private String billingData;
    
    // Creation timestamp
    private LocalDateTime createdAt;

    /**
     * Default constructor for serialization frameworks.
     */
    public PaymentData() {
    }

    /**
     * Creates a new payment data record with required fields.
     *
     * @param paymentDataId Unique identifier for this payment data record
     * @param transactionId Associated transaction identifier
     * @param paymentMethodId Identifier for the payment method
     */
    public PaymentData(UUID paymentDataId, UUID transactionId, String paymentMethodId) {
        this.paymentDataId = paymentDataId;
        this.transactionId = transactionId;
        this.paymentMethodId = paymentMethodId;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Creates a new payment data record with all fields.
     *
     * @param paymentDataId Unique identifier for this payment data record
     * @param transactionId Associated transaction identifier
     * @param paymentMethodId Identifier for the payment method
     * @param paymentToken Tokenized payment data (if applicable)
     * @param paymentDetails JSON string containing payment method details
     * @param expiration Payment method expiration date (if applicable)
     * @param billingData JSON string containing billing information
     */
    public PaymentData(UUID paymentDataId, UUID transactionId, String paymentMethodId,
                      String paymentToken, String paymentDetails, LocalDate expiration,
                      String billingData) {
        this.paymentDataId = paymentDataId;
        this.transactionId = transactionId;
        this.paymentMethodId = paymentMethodId;
        this.paymentToken = paymentToken;
        this.paymentDetails = paymentDetails;
        this.expiration = expiration;
        this.billingData = billingData;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Masks sensitive payment data for display or logging purposes.
     * This method returns a copy of the payment data with sensitive fields masked.
     *
     * @return A new PaymentData instance with masked sensitive fields
     */
    public PaymentData getMaskedCopy() {
        PaymentData masked = new PaymentData();
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
        
        // Payment details would be masked at the JSON level before setting
        masked.setPaymentDetails(null);
        
        masked.setExpiration(this.expiration);
        
        // Billing data would be masked at the JSON level before setting
        masked.setBillingData(null);
        
        masked.setCreatedAt(this.createdAt);
        
        return masked;
    }

    /**
     * Checks if this payment method has expired.
     *
     * @return true if the payment method has an expiration date and it has passed, false otherwise
     */
    public boolean isExpired() {
        return expiration != null && expiration.isBefore(LocalDate.now());
    }

    /**
     * Checks if this payment method will expire soon (within 30 days).
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
        // Use masked copy for toString to prevent accidental logging of sensitive data
        PaymentData masked = getMaskedCopy();
        return "PaymentData{" +
                "paymentDataId=" + masked.paymentDataId +
                ", transactionId=" + masked.transactionId +
                ", paymentMethodId='" + masked.paymentMethodId + '\'' +
                ", paymentToken='" + masked.paymentToken + '\'' +
                ", expiration=" + masked.expiration +
                ", createdAt=" + masked.createdAt +
                '}';
    }
}