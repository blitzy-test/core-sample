package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentType;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of the PaymentValidationService interface that provides
 * comprehensive validation for payment operations. This class implements
 * various validation rules including amount validation, currency validation,
 * state transition validation, and business rule enforcement.
 */
@Singleton
public class PaymentValidationServiceImpl implements PaymentValidationService {

    private static final Logger LOGGER = Logger.getLogger(PaymentValidationServiceImpl.class.getName());
    
    // Constants for validation
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Pattern MERCHANT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final int CURRENCY_CODE_LENGTH = 3;
    
    // Set of valid ISO 4217 currency codes (common ones)
    private static final Set<String> COMMON_CURRENCIES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CNY", "INR")));

    /**
     * Default constructor for dependency injection.
     */
    @Inject
    public PaymentValidationServiceImpl() {
        // Default constructor for dependency injection
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateTransactionData(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        // Validate required fields
        if (transaction.getTransactionId() == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }

        if (transaction.getOrganizationId() == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }

        if (transaction.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID is required");
        }

        // Validate amount and currency
        validateAmount(transaction.getAmount(), transaction.getCurrency());
        validateCurrency(transaction.getCurrency());

        // Validate merchant ID
        if (transaction.getMerchantId() == null || transaction.getMerchantId().isEmpty()) {
            throw new IllegalArgumentException("Merchant ID is required");
        }

        if (!MERCHANT_ID_PATTERN.matcher(transaction.getMerchantId()).matches()) {
            throw new IllegalArgumentException("Merchant ID format is invalid");
        }

        // Validate payment type
        if (transaction.getPaymentType() == null) {
            throw new IllegalArgumentException("Payment type is required");
        }

        // Validate payment type is valid for the transaction amount
        if (!transaction.getPaymentType().isValidForAmount(transaction.getAmount().doubleValue())) {
            throw new IllegalArgumentException(
                    String.format("Payment type %s is not valid for amount %s %s",
                            transaction.getPaymentType().name(),
                            transaction.getCurrency(),
                            transaction.getAmount().toString()));
        }

        // Validate status
        if (transaction.getStatus() == null) {
            throw new IllegalArgumentException("Transaction status is required");
        }

        // Validate timestamps
        if (transaction.getCreatedAt() == null) {
            throw new IllegalArgumentException("Created timestamp is required");
        }

        if (transaction.getUpdatedAt() == null) {
            throw new IllegalArgumentException("Updated timestamp is required");
        }

        // Additional validation for specific payment types
        if (transaction.getPaymentType().requiresCardData() && transaction.getPaymentType().isCardBased()) {
            // Card-based payment methods require additional validation
            // This would typically be handled by the PaymentData validation
            LOGGER.fine("Card-based payment method detected, additional validation may be required");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Validate amount precision based on currency
        int maxFractionDigits = getMaxFractionDigitsForCurrency(currency);
        if (amount.scale() > maxFractionDigits) {
            throw new IllegalArgumentException(
                    String.format("Amount has too many decimal places for currency %s (max: %d)",
                            currency, maxFractionDigits));
        }

        // Check for reasonable amount limits to prevent errors
        if (amount.compareTo(new BigDecimal("1000000000")) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum allowed value");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCurrency(String currency) {
        if (currency == null || currency.isEmpty()) {
            throw new IllegalArgumentException("Currency code is required");
        }

        if (currency.length() != CURRENCY_CODE_LENGTH) {
            throw new IllegalArgumentException("Currency code must be exactly 3 characters");
        }

        // Check if it's a valid ISO 4217 currency code
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            // If not in Java's Currency class, check our common currencies list
            if (!COMMON_CURRENCIES.contains(currency)) {
                throw new IllegalArgumentException("Invalid currency code: " + currency);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current status cannot be null");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s",
                            currentStatus.name(), newStatus.name()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCanCapture(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (!transaction.canCapture()) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be captured. Current status: %s",
                            transaction.getTransactionId(), transaction.getStatus()));
        }

        // Check if payment type supports capture
        if (!transaction.getPaymentType().supportsDelayedCapture()) {
            throw new IllegalStateException(
                    String.format("Payment type %s does not support capture operations",
                            transaction.getPaymentType().name()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCanRefund(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (!transaction.canRefund()) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be refunded. Current status: %s",
                            transaction.getTransactionId(), transaction.getStatus()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCanVoid(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (!transaction.canVoid()) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be voided. Current status: %s",
                            transaction.getTransactionId(), transaction.getStatus()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCaptureAmount(PaymentTransaction transaction, BigDecimal captureAmount) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (captureAmount == null) {
            throw new IllegalArgumentException("Capture amount cannot be null");
        }

        // Validate the transaction can be captured
        validateCanCapture(transaction);

        // Validate capture amount is positive
        if (captureAmount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Capture amount must be greater than zero");
        }

        // Validate capture amount does not exceed original transaction amount
        if (captureAmount.compareTo(transaction.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    String.format("Capture amount %s exceeds original transaction amount %s",
                            captureAmount, transaction.getAmount()));
        }

        // Validate amount precision based on currency
        int maxFractionDigits = getMaxFractionDigitsForCurrency(transaction.getCurrency());
        if (captureAmount.scale() > maxFractionDigits) {
            throw new IllegalArgumentException(
                    String.format("Capture amount has too many decimal places for currency %s (max: %d)",
                            transaction.getCurrency(), maxFractionDigits));
        }

        // Check if payment type supports partial capture if amount is less than original
        if (captureAmount.compareTo(transaction.getAmount()) < 0 && 
                !transaction.getPaymentType().supportsPartialCapture()) {
            throw new IllegalArgumentException(
                    String.format("Payment type %s does not support partial capture",
                            transaction.getPaymentType().name()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateRefundAmount(PaymentTransaction transaction, BigDecimal refundAmount) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (refundAmount == null) {
            throw new IllegalArgumentException("Refund amount cannot be null");
        }

        // Validate the transaction can be refunded
        validateCanRefund(transaction);

        // Validate refund amount is positive
        if (refundAmount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        // Validate refund amount does not exceed original transaction amount
        if (refundAmount.compareTo(transaction.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    String.format("Refund amount %s exceeds original transaction amount %s",
                            refundAmount, transaction.getAmount()));
        }

        // Validate amount precision based on currency
        int maxFractionDigits = getMaxFractionDigitsForCurrency(transaction.getCurrency());
        if (refundAmount.scale() > maxFractionDigits) {
            throw new IllegalArgumentException(
                    String.format("Refund amount has too many decimal places for currency %s (max: %d)",
                            transaction.getCurrency(), maxFractionDigits));
        }

        // Note: Additional validation for already refunded amounts would typically be
        // performed here, but would require access to previous refund records
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateOrganizationAccess(UUID organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }

        // In a real implementation, this would check against the Access Rights module
        // to verify the current user has access to the specified organization
        
        // For now, we'll just validate the UUID format
        if (organizationId.toString().length() != 36) {
            throw new IllegalArgumentException("Invalid organization ID format");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateAccountAccess(UUID organizationId, UUID accountId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }

        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        // Validate organization access first
        validateOrganizationAccess(organizationId);

        // In a real implementation, this would check if the account belongs to the organization
        // and if the current user has access to it through the Access Rights module
        
        // For now, we'll just validate the UUID format
        if (accountId.toString().length() != 36) {
            throw new IllegalArgumentException("Invalid account ID format");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateMerchantId(UUID organizationId, String merchantId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }

        if (merchantId == null || merchantId.isEmpty()) {
            throw new IllegalArgumentException("Merchant ID cannot be null or empty");
        }

        // Validate organization access first
        validateOrganizationAccess(organizationId);

        // Validate merchant ID format
        if (!MERCHANT_ID_PATTERN.matcher(merchantId).matches()) {
            throw new IllegalArgumentException("Merchant ID format is invalid");
        }

        // In a real implementation, this would check if the merchant belongs to the organization
        // For now, we'll just validate the format
    }

    /**
     * Helper method to determine the maximum number of fraction digits allowed for a currency.
     * 
     * @param currencyCode The ISO 4217 currency code
     * @return The maximum number of fraction digits allowed
     */
    private int getMaxFractionDigitsForCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isEmpty()) {
            return 2; // Default to 2 decimal places
        }

        try {
            Currency currency = Currency.getInstance(currencyCode);
            return currency.getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            // For currencies not supported by Java's Currency class
            if ("BTC".equals(currencyCode)) {
                return 8; // Bitcoin has 8 decimal places
            }
            
            // Default to 2 decimal places for most currencies
            return 2;
        }
    }
}