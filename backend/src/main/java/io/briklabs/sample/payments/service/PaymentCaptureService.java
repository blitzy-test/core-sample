package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface defining methods for payment capture operations, including full and partial captures.
 * <p>
 * This service provides the contract for implementing capture functionality, which is a critical step
 * in the payment lifecycle that converts authorized funds into actual charges. Capture operations can
 * be performed for the full authorized amount or for partial amounts, allowing for flexible payment
 * processing scenarios.
 * </p>
 */
public interface PaymentCaptureService {

    /**
     * Captures the full amount of an authorized payment transaction.
     * This operation transitions the transaction from AUTHORIZED to CAPTURED status.
     *
     * @param transactionId The unique identifier of the transaction to capture
     * @param userId The identifier of the user initiating the capture
     * @param metadata Additional metadata for the capture operation (optional)
     * @return The updated payment transaction with CAPTURED status
     * @throws IllegalStateException if the transaction is not in AUTHORIZED status
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction captureFullAmount(UUID transactionId, String userId, Map<String, String> metadata);

    /**
     * Captures a partial amount of an authorized payment transaction.
     * This operation transitions the transaction from AUTHORIZED to PARTIALLY_CAPTURED status
     * if the capture amount is less than the authorized amount, or to CAPTURED if this
     * capture completes the full authorized amount.
     *
     * @param transactionId The unique identifier of the transaction to capture
     * @param captureAmount The amount to capture (must be greater than zero and less than or equal to the remaining authorized amount)
     * @param userId The identifier of the user initiating the capture
     * @param metadata Additional metadata for the capture operation (optional)
     * @return The updated payment transaction with PARTIALLY_CAPTURED or CAPTURED status
     * @throws IllegalStateException if the transaction is not in AUTHORIZED or PARTIALLY_CAPTURED status
     * @throws IllegalArgumentException if the transaction ID is invalid or the capture amount is invalid
     */
    PaymentTransaction capturePartialAmount(UUID transactionId, BigDecimal captureAmount, String userId, Map<String, String> metadata);

    /**
     * Retrieves the capture status of a transaction, including total captured amount and remaining amount.
     *
     * @param transactionId The unique identifier of the transaction
     * @return A map containing capture status information, including:
     *         - totalAuthorizedAmount: The original authorized amount
     *         - totalCapturedAmount: The sum of all captured amounts
     *         - remainingAmount: The amount still available for capture
     *         - isFullyCaptured: Boolean indicating if the transaction is fully captured
     *         - captureCount: Number of capture operations performed
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    Map<String, Object> getCaptureStatus(UUID transactionId);

    /**
     * Retrieves the history of capture operations for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return A list of capture events in chronological order
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    List<PaymentEvent> getCaptureHistory(UUID transactionId);

    /**
     * Validates if a transaction can be captured.
     * A transaction can be captured if it is in AUTHORIZED or PARTIALLY_CAPTURED status.
     *
     * @param transactionId The unique identifier of the transaction
     * @return true if the transaction can be captured, false otherwise
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    boolean canCapture(UUID transactionId);

    /**
     * Validates if a specific capture amount is valid for a transaction.
     * The amount is valid if it is greater than zero and less than or equal to
     * the remaining authorized amount.
     *
     * @param transactionId The unique identifier of the transaction
     * @param captureAmount The amount to validate
     * @return true if the capture amount is valid, false otherwise
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    boolean isValidCaptureAmount(UUID transactionId, BigDecimal captureAmount);

    /**
     * Calculates the remaining amount available for capture for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The remaining amount available for capture
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    BigDecimal getRemainingCaptureAmount(UUID transactionId);

    /**
     * Retrieves the total amount captured so far for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The total captured amount
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    BigDecimal getTotalCapturedAmount(UUID transactionId);

    /**
     * Retrieves the most recent capture operation for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return An Optional containing the most recent capture event, or empty if no captures exist
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    Optional<PaymentEvent> getLastCaptureOperation(UUID transactionId);

    /**
     * Checks if a transaction is fully captured.
     * A transaction is fully captured when the total captured amount equals the authorized amount.
     *
     * @param transactionId The unique identifier of the transaction
     * @return true if the transaction is fully captured, false otherwise
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    boolean isFullyCaptured(UUID transactionId);

    /**
     * Validates a capture operation before processing.
     * This performs comprehensive validation including:
     * - Transaction state validation
     * - Amount validation
     * - Business rule compliance
     * - User permission verification
     *
     * @param transactionId The unique identifier of the transaction
     * @param captureAmount The amount to capture (null for full capture)
     * @param userId The identifier of the user initiating the capture
     * @throws IllegalStateException if the transaction cannot be captured
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws SecurityException if the user does not have permission to capture
     */
    void validateCaptureOperation(UUID transactionId, BigDecimal captureAmount, String userId);
}