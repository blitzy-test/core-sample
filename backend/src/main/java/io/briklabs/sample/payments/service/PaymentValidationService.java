package io.briklabs.sample.payments.service;

import java.math.BigDecimal;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentTransaction;

/**
 * Service interface for validating payment operations and data.
 * Provides methods for ensuring data integrity and business rule compliance
 * throughout the payment lifecycle.
 */
public interface PaymentValidationService {
    
    /**
     * Validates a payment transaction for creation.
     * Ensures all required fields are present and valid.
     *
     * @param transaction The payment transaction to validate
     * @return true if the transaction is valid, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateTransactionData(PaymentTransaction transaction);
    
    /**
     * Validates the amount and currency of a payment transaction.
     * Ensures the amount is positive and the currency is a valid ISO 4217 code.
     *
     * @param amount The payment amount to validate
     * @param currency The currency code to validate (3-character ISO 4217 code)
     * @return true if the amount and currency are valid, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateAmountAndCurrency(BigDecimal amount, String currency);
    
    /**
     * Validates a capture amount against the original transaction amount.
     * Ensures the capture amount is positive and does not exceed the available amount.
     *
     * @param transactionId The ID of the transaction being captured
     * @param captureAmount The amount to capture
     * @return true if the capture amount is valid, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateCaptureAmount(UUID transactionId, BigDecimal captureAmount);
    
    /**
     * Validates a refund amount against the captured transaction amount.
     * Ensures the refund amount is positive and does not exceed the captured amount.
     *
     * @param transactionId The ID of the transaction being refunded
     * @param refundAmount The amount to refund
     * @return true if the refund amount is valid, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateRefundAmount(UUID transactionId, BigDecimal refundAmount);
    
    /**
     * Validates a state transition for a payment transaction.
     * Ensures the requested state transition is allowed based on the current state.
     *
     * @param transactionId The ID of the transaction
     * @param currentState The current state of the transaction
     * @param newState The requested new state
     * @return true if the state transition is valid, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateStateTransition(UUID transactionId, String currentState, String newState);
    
    /**
     * Validates that a transaction exists and is in a valid state for processing.
     *
     * @param transactionId The ID of the transaction to validate
     * @return true if the transaction exists and is in a valid state, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateTransactionExists(UUID transactionId);
    
    /**
     * Validates that a transaction is in a specific state.
     *
     * @param transactionId The ID of the transaction to validate
     * @param expectedState The expected state of the transaction
     * @return true if the transaction is in the expected state, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateTransactionState(UUID transactionId, String expectedState);
    
    /**
     * Validates that a transaction is in one of a set of allowed states.
     *
     * @param transactionId The ID of the transaction to validate
     * @param allowedStates Array of allowed states for the transaction
     * @return true if the transaction is in one of the allowed states, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateTransactionState(UUID transactionId, String[] allowedStates);
    
    /**
     * Validates that a transaction belongs to the specified organization and account.
     *
     * @param transactionId The ID of the transaction to validate
     * @param organizationId The organization ID to validate against
     * @param accountId The account ID to validate against
     * @return true if the transaction belongs to the specified organization and account, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateTransactionOwnership(UUID transactionId, UUID organizationId, UUID accountId);
    
    /**
     * Validates a merchant ID format and existence.
     *
     * @param merchantId The merchant ID to validate
     * @return true if the merchant ID is valid, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateMerchantId(String merchantId);
    
    /**
     * Validates a payment method type.
     *
     * @param paymentType The payment method type to validate
     * @return true if the payment method type is valid, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validatePaymentType(String paymentType);
    
    /**
     * Validates payment data for a specific payment method type.
     *
     * @param paymentMethodId The payment method ID
     * @param paymentData The payment data to validate
     * @return true if the payment data is valid for the payment method, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validatePaymentData(String paymentMethodId, String paymentData);
    
    /**
     * Validates that a capture operation is allowed for a transaction.
     * Checks transaction state, previous captures, and available amount.
     *
     * @param transactionId The ID of the transaction to validate for capture
     * @return true if capture is allowed, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateCaptureAllowed(UUID transactionId);
    
    /**
     * Validates that a refund operation is allowed for a transaction.
     * Checks transaction state, previous refunds, and available amount.
     *
     * @param transactionId The ID of the transaction to validate for refund
     * @return true if refund is allowed, false otherwise
     * @throws IllegalArgumentException if validation fails with specific error details
     */
    boolean validateRefundAllowed(UUID transactionId);
}