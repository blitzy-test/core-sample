package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface that defines payment refund operations.
 * This service provides the contract for implementing refund functionality,
 * which is essential for returning funds to customers when needed
 * throughout the payment lifecycle.
 */
public interface PaymentRefundService {

    /**
     * Refunds the full amount of a captured payment transaction.
     * This operation moves the transaction from CAPTURED to REFUNDED state.
     *
     * @param transactionId The unique identifier of the transaction to refund
     * @return The updated payment transaction after refund
     * @throws IllegalArgumentException if the transaction ID is invalid
     * @throws IllegalStateException if the transaction is not in CAPTURED state
     */
    PaymentTransaction refundTransaction(UUID transactionId);

    /**
     * Refunds a partial amount of a captured payment transaction.
     * The refunded amount must be greater than zero and less than or equal to
     * the original captured amount.
     *
     * @param transactionId The unique identifier of the transaction to refund
     * @param amount The amount to refund, must be greater than zero and less than or equal to the captured amount
     * @return The updated payment transaction after partial refund
     * @throws IllegalArgumentException if the transaction ID is invalid or the amount is invalid
     * @throws IllegalStateException if the transaction is not in CAPTURED state
     */
    PaymentTransaction refundPartialTransaction(UUID transactionId, BigDecimal amount);

    /**
     * Retrieves the refund status of a transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @return true if the transaction has been refunded, false otherwise
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    boolean isRefunded(UUID transactionId);

    /**
     * Checks if a transaction can be refunded based on its current state.
     * A transaction can typically be refunded only if it is in CAPTURED state.
     *
     * @param transactionId The unique identifier of the transaction
     * @return true if the transaction can be refunded, false otherwise
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    boolean canRefund(UUID transactionId);

    /**
     * Validates a refund operation for a transaction.
     * This method checks if the transaction is in a valid state for refund
     * and if the refund amount is valid.
     *
     * @param transaction The transaction to validate
     * @param refundAmount The amount to refund (null for full refund)
     * @throws IllegalArgumentException if the refund operation is invalid
     * @throws IllegalStateException if the transaction is not in a valid state for refund
     */
    void validateRefundOperation(PaymentTransaction transaction, BigDecimal refundAmount);

    /**
     * Validates a refund amount for a transaction.
     * The amount must be greater than zero and less than or equal to the captured amount.
     *
     * @param transaction The transaction to validate against
     * @param refundAmount The amount to validate
     * @throws IllegalArgumentException if the amount is invalid
     */
    void validateRefundAmount(PaymentTransaction transaction, BigDecimal refundAmount);

    /**
     * Gets the total amount that has been refunded for a transaction.
     * For transactions with multiple partial refunds, this returns the sum of all refunds.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The total refunded amount
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    BigDecimal getTotalRefundedAmount(UUID transactionId);

    /**
     * Gets the remaining amount that can be refunded for a transaction.
     * This is the difference between the captured amount and the total refunded amount.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The remaining amount that can be refunded
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    BigDecimal getRemainingRefundAmount(UUID transactionId);
    
    /**
     * Approves a pending refund request.
     * Some refund operations may require explicit approval before processing.
     *
     * @param transactionId The unique identifier of the transaction
     * @param approverUserId The identifier of the user approving the refund
     * @param notes Optional notes regarding the approval decision
     * @return The updated payment transaction after refund approval
     * @throws IllegalArgumentException if the transaction ID is invalid
     * @throws IllegalStateException if the transaction is not in a state that can be approved for refund
     */
    PaymentTransaction approveRefund(UUID transactionId, String approverUserId, String notes);
    
    /**
     * Rejects a pending refund request.
     *
     * @param transactionId The unique identifier of the transaction
     * @param rejectorUserId The identifier of the user rejecting the refund
     * @param reason The reason for rejection
     * @return The updated payment transaction after refund rejection
     * @throws IllegalArgumentException if the transaction ID is invalid
     * @throws IllegalStateException if the transaction is not in a state that can be rejected for refund
     */
    PaymentTransaction rejectRefund(UUID transactionId, String rejectorUserId, String reason);
}