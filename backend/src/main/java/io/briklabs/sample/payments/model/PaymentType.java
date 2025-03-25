package io.briklabs.sample.payments.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumeration of supported payment method types in the payment processing system.
 * This enum defines the valid payment method classifications with associated validation
 * rules and processing requirements. It enables type-safe references to payment methods
 * throughout the application and supports method-specific processing logic.
 */
public enum PaymentType {
    
    /**
     * Credit card payment method (Visa, Mastercard, Amex, etc.)
     * Requires card number, expiration date, and CVV for processing.
     */
    CREDIT_CARD("Credit Card", "Payment using a credit card", true, true, true),
    
    /**
     * Debit card payment method
     * Requires card number, expiration date, and may require PIN for processing.
     */
    DEBIT_CARD("Debit Card", "Payment using a debit card", true, true, false),
    
    /**
     * Bank transfer payment method (ACH, wire transfer, etc.)
     * Requires bank account and routing information.
     */
    BANK_TRANSFER("Bank Transfer", "Payment using a bank transfer", false, false, true),
    
    /**
     * Digital wallet payment method (PayPal, Apple Pay, Google Pay, etc.)
     * Requires wallet-specific authentication and tokenization.
     */
    DIGITAL_WALLET("Digital Wallet", "Payment using a digital wallet", false, true, false);
    
    private final String displayName;
    private final String description;
    private final boolean requiresCardData;
    private final boolean supportsTokenization;
    private final boolean supportsDelayedCapture;
    
    /**
     * List of payment types that require card verification.
     */
    public static final List<PaymentType> CARD_VERIFICATION_REQUIRED = 
            Collections.unmodifiableList(Arrays.asList(CREDIT_CARD, DEBIT_CARD));
    
    /**
     * List of payment types that support tokenization for recurring payments.
     */
    public static final List<PaymentType> TOKENIZATION_SUPPORTED = 
            Collections.unmodifiableList(Arrays.asList(CREDIT_CARD, DEBIT_CARD, DIGITAL_WALLET));
    
    /**
     * List of payment types that support delayed capture (auth/capture flow).
     */
    public static final List<PaymentType> DELAYED_CAPTURE_SUPPORTED = 
            Collections.unmodifiableList(Arrays.asList(CREDIT_CARD, BANK_TRANSFER));
    
    /**
     * Constructor for PaymentType enum.
     * 
     * @param displayName The user-friendly name for display in UI
     * @param description Detailed description of the payment method
     * @param requiresCardData Whether this payment type requires card data
     * @param supportsTokenization Whether this payment type supports tokenization
     * @param supportsDelayedCapture Whether this payment type supports delayed capture
     */
    PaymentType(String displayName, String description, boolean requiresCardData, 
                boolean supportsTokenization, boolean supportsDelayedCapture) {
        this.displayName = displayName;
        this.description = description;
        this.requiresCardData = requiresCardData;
        this.supportsTokenization = supportsTokenization;
        this.supportsDelayedCapture = supportsDelayedCapture;
    }
    
    /**
     * Gets the user-friendly display name for this payment type.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the detailed description of this payment type.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this payment type requires card data (number, expiration, CVV).
     * 
     * @return true if card data is required, false otherwise
     */
    public boolean requiresCardData() {
        return requiresCardData;
    }
    
    /**
     * Checks if this payment type supports tokenization for recurring payments.
     * 
     * @return true if tokenization is supported, false otherwise
     */
    public boolean supportsTokenization() {
        return supportsTokenization;
    }
    
    /**
     * Checks if this payment type supports delayed capture (auth/capture flow).
     * 
     * @return true if delayed capture is supported, false otherwise
     */
    public boolean supportsDelayedCapture() {
        return supportsDelayedCapture;
    }
    
    /**
     * Checks if the payment type requires additional verification.
     * 
     * @return true if additional verification is required, false otherwise
     */
    public boolean requiresAdditionalVerification() {
        return CARD_VERIFICATION_REQUIRED.contains(this);
    }
    
    /**
     * Checks if the payment type can be used for recurring payments.
     * 
     * @return true if the payment type can be used for recurring payments, false otherwise
     */
    public boolean canUseForRecurringPayments() {
        return TOKENIZATION_SUPPORTED.contains(this);
    }
    
    /**
     * Checks if the payment type supports partial captures.
     * 
     * @return true if partial captures are supported, false otherwise
     */
    public boolean supportsPartialCapture() {
        return DELAYED_CAPTURE_SUPPORTED.contains(this);
    }
    
    /**
     * Validates if the provided payment type is valid for a specific transaction amount.
     * Some payment types may have minimum or maximum amount restrictions.
     * 
     * @param amount The transaction amount to validate
     * @return true if the payment type is valid for the amount, false otherwise
     */
    public boolean isValidForAmount(double amount) {
        // Implementation of payment type specific amount validation rules
        switch (this) {
            case CREDIT_CARD:
                // Credit cards typically have no minimum amount restrictions
                return amount > 0;
            case DEBIT_CARD:
                // Debit cards typically have no minimum amount restrictions
                return amount > 0;
            case BANK_TRANSFER:
                // Bank transfers might have minimum amount requirements
                return amount >= 1.0;
            case DIGITAL_WALLET:
                // Digital wallets might have their own restrictions
                return amount > 0 && amount <= 10000.0;
            default:
                return false;
        }
    }
    
    /**
     * Determines if the payment type is considered a card-based payment method.
     * 
     * @return true if this is a card-based payment method, false otherwise
     */
    public boolean isCardBased() {
        return this == CREDIT_CARD || this == DEBIT_CARD;
    }
    
    /**
     * Determines if the payment type is considered a bank-based payment method.
     * 
     * @return true if this is a bank-based payment method, false otherwise
     */
    public boolean isBankBased() {
        return this == BANK_TRANSFER;
    }
    
    /**
     * Determines if the payment type is considered a wallet-based payment method.
     * 
     * @return true if this is a wallet-based payment method, false otherwise
     */
    public boolean isWalletBased() {
        return this == DIGITAL_WALLET;
    }
    
    /**
     * Finds a PaymentType by its string representation (case-insensitive).
     * 
     * @param typeString The string representation of the payment type
     * @return The matching PaymentType or null if not found
     */
    public static PaymentType fromString(String typeString) {
        if (typeString == null || typeString.isEmpty()) {
            return null;
        }
        
        try {
            return PaymentType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}