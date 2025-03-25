package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface defining methods for managing payment state transitions throughout the payment lifecycle.
 * This service provides the contract for implementing lifecycle management that ensures payments
 * follow the correct state transition paths according to business rules.
 */
public interface PaymentLifecycleService {

    /**
     * Executes a state transition for a payment transaction, validating the transition
     * and recording the appropriate events.
     *
     * @param transaction The payment transaction to update
     * @param newStatus The new status to transition to
     * @param userId The ID of the user initiating the transition
     * @param metadata Additional metadata to record with the transition event
     * @return The updated transaction with the new status
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException if the transition is not valid
     */
    PaymentTransaction executeStateTransition(PaymentTransaction transaction, PaymentStatus newStatus, 
                                            String userId, Map<String, String> metadata);

    /**
     * Checks if a state transition is valid for a given transaction.
     *
     * @param transaction The payment transaction to check
     * @param newStatus The new status to transition to
     * @return true if the transition is valid, false otherwise
     */
    boolean isValidStateTransition(PaymentTransaction transaction, PaymentStatus newStatus);

    /**
     * Checks if a state transition is valid between two states.
     *
     * @param currentStatus The current payment status
     * @param newStatus The new status to transition to
     * @return true if the transition is valid, false otherwise
     */
    boolean isValidStateTransition(PaymentStatus currentStatus, PaymentStatus newStatus);

    /**
     * Gets a list of all valid next states for a given transaction.
     *
     * @param transaction The payment transaction to check
     * @return A list of valid next states
     * @throws IllegalArgumentException if the transaction is null
     */
    List<PaymentStatus> getValidNextStates(PaymentTransaction transaction);

    /**
     * Gets a list of all valid next states for a given status.
     *
     * @param currentStatus The current payment status
     * @return A list of valid next states
     * @throws IllegalArgumentException if the current status is null
     */
    List<PaymentStatus> getValidNextStates(PaymentStatus currentStatus);

    /**
     * Checks if a transaction is in a final state (no further transitions possible).
     *
     * @param transaction The payment transaction to check
     * @return true if the transaction is in a final state, false otherwise
     * @throws IllegalArgumentException if the transaction is null
     */
    boolean isInFinalState(PaymentTransaction transaction);

    /**
     * Checks if a transaction can be captured based on its current state.
     *
     * @param transaction The payment transaction to check
     * @return true if the transaction can be captured, false otherwise
     * @throws IllegalArgumentException if the transaction is null
     */
    boolean canCapture(PaymentTransaction transaction);

    /**
     * Checks if a transaction can be refunded based on its current state.
     *
     * @param transaction The payment transaction to check
     * @return true if the transaction can be refunded, false otherwise
     * @throws IllegalArgumentException if the transaction is null
     */
    boolean canRefund(PaymentTransaction transaction);

    /**
     * Checks if a transaction can be voided based on its current state.
     *
     * @param transaction The payment transaction to check
     * @return true if the transaction can be voided, false otherwise
     * @throws IllegalArgumentException if the transaction is null
     */
    boolean canVoid(PaymentTransaction transaction);

    /**
     * Initiates processing for a transaction, transitioning it to the PROCESSING state.
     *
     * @param transaction The payment transaction to process
     * @param userId The ID of the user initiating the processing
     * @return The updated transaction with the PROCESSING status
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException if the transaction is not in a valid state for processing
     */
    PaymentTransaction initiateProcessing(PaymentTransaction transaction, String userId);

    /**
     * Marks a transaction as authorized, transitioning it to the AUTHORIZED state.
     *
     * @param transaction The payment transaction to authorize
     * @param userId The ID of the user authorizing the transaction
     * @param authorizationCode Optional authorization code from the payment processor
     * @return The updated transaction with the AUTHORIZED status
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException if the transaction is not in a valid state for authorization
     */
    PaymentTransaction markAsAuthorized(PaymentTransaction transaction, String userId, String authorizationCode);

    /**
     * Marks a transaction as captured, transitioning it to the CAPTURED state.
     *
     * @param transaction The payment transaction to capture
     * @param userId The ID of the user capturing the transaction
     * @param captureAmount Optional amount to capture (for partial captures)
     * @param captureReference Optional reference for the capture operation
     * @return The updated transaction with the CAPTURED status
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException if the transaction is not in a valid state for capture
     */
    PaymentTransaction markAsCaptured(PaymentTransaction transaction, String userId, 
                                    String captureAmount, String captureReference);

    /**
     * Marks a transaction as refunded, transitioning it to the REFUNDED state.
     *
     * @param transaction The payment transaction to refund
     * @param userId The ID of the user refunding the transaction
     * @param refundAmount Optional amount to refund (for partial refunds)
     * @param refundReason Optional reason for the refund
     * @param refundReference Optional reference for the refund operation
     * @return The updated transaction with the REFUNDED status
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException if the transaction is not in a valid state for refund
     */
    PaymentTransaction markAsRefunded(PaymentTransaction transaction, String userId, 
                                    String refundAmount, String refundReason, String refundReference);

    /**
     * Marks a transaction as voided, transitioning it to the VOIDED state.
     *
     * @param transaction The payment transaction to void
     * @param userId The ID of the user voiding the transaction
     * @param voidReason Optional reason for voiding the transaction
     * @return The updated transaction with the VOIDED status
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException if the transaction is not in a valid state for voiding
     */
    PaymentTransaction markAsVoided(PaymentTransaction transaction, String userId, String voidReason);

    /**
     * Marks a transaction as failed, transitioning it to the FAILED state.
     *
     * @param transaction The payment transaction that failed
     * @param userId The ID of the user recording the failure
     * @param errorCode Optional error code describing the failure
     * @param errorMessage Optional error message with details about the failure
     * @return The updated transaction with the FAILED status
     * @throws IllegalArgumentException if any parameters are invalid
     */
    PaymentTransaction markAsFailed(PaymentTransaction transaction, String userId, 
                                  String errorCode, String errorMessage);

    /**
     * Gets the current status of a transaction by its ID.
     *
     * @param transactionId The ID of the transaction to check
     * @return The current payment status
     * @throws IllegalArgumentException if the transaction ID is invalid or not found
     */
    PaymentStatus getCurrentStatus(UUID transactionId);

    /**
     * Gets the complete lifecycle history of a transaction as a chronological list of events.
     *
     * @param transactionId The ID of the transaction to get history for
     * @return A list of payment events in chronological order
     * @throws IllegalArgumentException if the transaction ID is invalid or not found
     */
    List<PaymentEvent> getLifecycleHistory(UUID transactionId);

    /**
     * Verifies that a transaction is in the expected state, throwing an exception if not.
     *
     * @param transaction The payment transaction to verify
     * @param expectedStatus The expected status of the transaction
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException if the transaction is not in the expected state
     */
    void verifyTransactionState(PaymentTransaction transaction, PaymentStatus expectedStatus);

    /**
     * Validates that a transaction is in one of the allowed states.
     *
     * @param transaction The payment transaction to validate
     * @param allowedStatuses One or more allowed statuses
     * @return true if the transaction is in one of the allowed states, false otherwise
     * @throws IllegalArgumentException if any parameters are invalid
     */
    boolean validateTransactionState(PaymentTransaction transaction, PaymentStatus... allowedStatuses);

    /**
     * Gets a comprehensive summary of a transaction's lifecycle, including time spent in each state,
     * key events, and current capabilities.
     *
     * @param transactionId The ID of the transaction to summarize
     * @return A map containing the lifecycle summary data
     * @throws IllegalArgumentException if the transaction ID is invalid or not found
     */
    Map<String, Object> getLifecycleSummary(UUID transactionId);
}