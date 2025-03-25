package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface that defines payment capture operations.
 * This service provides the contract for implementing capture functionality,
 * which is a critical step in the payment lifecycle that converts
 * authorized funds into actual charges.
 */
public interface PaymentCaptureService {

    /**
     * Captures the full amount of an authorized payment transaction.
     * This operation moves the transaction from AUTHORIZED to CAPTURED state.
     *
     * @param transactionId The unique identifier of the transaction to capture
     * @return The updated payment transaction after capture
     * @throws IllegalArgumentException if the transaction ID is invalid
     * @throws IllegalStateException if the transaction is not in AUTHORIZED state
     */
    PaymentTransaction captureTransaction(UUID transactionId);

    /**
     * Captures a partial amount of an authorized payment transaction.
     * The captured amount must be greater than zero and less than or equal to
     * the original authorized amount.
     *
     * @param transactionId The unique identifier of the transaction to capture
     * @param amount The amount to capture, must be greater than zero and less than or equal to the authorized amount
     * @return The updated payment transaction after partial capture
     * @throws IllegalArgumentException if the transaction ID is invalid or the amount is invalid
     * @throws IllegalStateException if the transaction is not in AUTHORIZED state
     */
    PaymentTransaction capturePartialTransaction(UUID transactionId, BigDecimal amount);

    /**
     * Retrieves the capture status of a transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @return true if the transaction has been captured, false otherwise
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    boolean isCaptured(UUID transactionId);

    /**
     * Checks if a transaction can be captured based on its current state.
     * A transaction can typically be captured only if it is in AUTHORIZED state.
     *
     * @param transactionId The unique identifier of the transaction
     * @return true if the transaction can be captured, false otherwise
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    boolean canCapture(UUID transactionId);

    /**
     * Validates a capture operation for a transaction.
     * This method checks if the transaction is in a valid state for capture
     * and if the capture amount is valid.
     *
     * @param transaction The transaction to validate
     * @param captureAmount The amount to capture (null for full capture)
     * @throws IllegalArgumentException if the capture operation is invalid
     * @throws IllegalStateException if the transaction is not in a valid state for capture
     */
    void validateCaptureOperation(PaymentTransaction transaction, BigDecimal captureAmount);

    /**
     * Validates a capture amount for a transaction.
     * The amount must be greater than zero and less than or equal to the authorized amount.
     *
     * @param transaction The transaction to validate against
     * @param captureAmount The amount to validate
     * @throws IllegalArgumentException if the amount is invalid
     */
    void validateCaptureAmount(PaymentTransaction transaction, BigDecimal captureAmount);

    /**
     * Gets the total amount that has been captured for a transaction.
     * For transactions with multiple partial captures, this returns the sum of all captures.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The total captured amount
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    BigDecimal getTotalCapturedAmount(UUID transactionId);

    /**
     * Gets the remaining amount that can be captured for a transaction.
     * This is the difference between the authorized amount and the total captured amount.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The remaining amount that can be captured
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    BigDecimal getRemainingCaptureAmount(UUID transactionId);
}