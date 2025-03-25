package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing payment state transitions throughout the payment lifecycle.
 * This service ensures payments follow the correct state transition paths according to business rules,
 * validates transition requests, and maintains the integrity of the payment lifecycle.
 */
public interface PaymentLifecycleService {

    /**
     * Executes a state transition for a payment transaction.
     *
     * @param transaction The transaction to update
     * @param newStatus The new status to apply
     * @param userId The identifier of the user initiating the transition
     * @param metadata Additional context for the transition (optional)
     * @return The updated transaction with the new status
     * @throws IllegalStateException if the transition is not allowed
     * @throws IllegalArgumentException if any parameters are invalid
     */
    PaymentTransaction executeStateTransition(PaymentTransaction transaction, PaymentStatus newStatus, 
                                             String userId, Map<String, String> metadata);

    /**
     * Validates whether a state transition is allowed for a transaction.
     *
     * @param transaction The transaction to check
     * @param newStatus The target status
     * @return true if the transition is valid, false otherwise
     */
    boolean isValidStateTransition(PaymentTransaction transaction, PaymentStatus newStatus);

    /**
     * Validates whether a state transition is allowed between two statuses.
     *
     * @param currentStatus The current status
     * @param newStatus The target status
     * @return true if the transition is valid, false otherwise
     */
    boolean isValidStateTransition(PaymentStatus currentStatus, PaymentStatus newStatus);

    /**
     * Gets all valid next states for a transaction based on its current state.
     *
     * @param transaction The transaction to check
     * @return A list of valid states that the transaction can transition to
     */
    List<PaymentStatus> getValidNextStates(PaymentTransaction transaction);

    /**
     * Gets all valid next states for a given status.
     *
     * @param currentStatus The current status
     * @return A list of valid states that can follow the current status
     */
    List<PaymentStatus> getValidNextStates(PaymentStatus currentStatus);

    /**
     * Checks if a transaction is in a final state.
     *
     * @param transaction The transaction to check
     * @return true if the transaction is in a final state, false otherwise
     */
    boolean isInFinalState(PaymentTransaction transaction);

    /**
     * Checks if a transaction can be captured.
     *
     * @param transaction The transaction to check
     * @return true if the transaction can be captured, false otherwise
     */
    boolean canCapture(PaymentTransaction transaction);

    /**
     * Checks if a transaction can be refunded.
     *
     * @param transaction The transaction to check
     * @return true if the transaction can be refunded, false otherwise
     */
    boolean canRefund(PaymentTransaction transaction);

    /**
     * Checks if a transaction can be voided.
     *
     * @param transaction The transaction to check
     * @return true if the transaction can be voided, false otherwise
     */
    boolean canVoid(PaymentTransaction transaction);

    /**
     * Initiates the processing of a newly created transaction.
     *
     * @param transaction The transaction to process
     * @param userId The identifier of the user initiating the processing
     * @return The updated transaction with processing status
     * @throws IllegalStateException if the transaction is not in a valid state for processing
     */
    PaymentTransaction initiateProcessing(PaymentTransaction transaction, String userId);

    /**
     * Marks a transaction as authorized.
     *
     * @param transaction The transaction to authorize
     * @param userId The identifier of the user authorizing the transaction
     * @param authorizationCode The authorization code from the payment processor
     * @return The updated transaction with authorized status
     * @throws IllegalStateException if the transaction is not in a valid state for authorization
     */
    PaymentTransaction markAsAuthorized(PaymentTransaction transaction, String userId, String authorizationCode);

    /**
     * Marks a transaction as captured.
     *
     * @param transaction The transaction to capture
     * @param userId The identifier of the user capturing the transaction
     * @param captureAmount The amount to capture (for partial captures)
     * @param captureReference The reference code for the capture operation
     * @return The updated transaction with captured status
     * @throws IllegalStateException if the transaction is not in a valid state for capture
     */
    PaymentTransaction markAsCaptured(PaymentTransaction transaction, String userId, 
                                     String captureAmount, String captureReference);

    /**
     * Marks a transaction as refunded.
     *
     * @param transaction The transaction to refund
     * @param userId The identifier of the user refunding the transaction
     * @param refundAmount The amount to refund (for partial refunds)
     * @param refundReason The reason for the refund
     * @param refundReference The reference code for the refund operation
     * @return The updated transaction with refunded status
     * @throws IllegalStateException if the transaction is not in a valid state for refund
     */
    PaymentTransaction markAsRefunded(PaymentTransaction transaction, String userId, 
                                     String refundAmount, String refundReason, String refundReference);

    /**
     * Marks a transaction as voided.
     *
     * @param transaction The transaction to void
     * @param userId The identifier of the user voiding the transaction
     * @param voidReason The reason for voiding the transaction
     * @return The updated transaction with voided status
     * @throws IllegalStateException if the transaction is not in a valid state for voiding
     */
    PaymentTransaction markAsVoided(PaymentTransaction transaction, String userId, String voidReason);

    /**
     * Marks a transaction as failed.
     *
     * @param transaction The transaction that failed
     * @param userId The identifier of the user or system reporting the failure
     * @param errorCode The error code
     * @param errorMessage The error message
     * @return The updated transaction with failed status
     */
    PaymentTransaction markAsFailed(PaymentTransaction transaction, String userId, 
                                   String errorCode, String errorMessage);

    /**
     * Gets the current lifecycle status of a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The current status of the transaction
     * @throws IllegalArgumentException if the transaction does not exist
     */
    PaymentStatus getCurrentStatus(UUID transactionId);

    /**
     * Gets the complete lifecycle history of a transaction.
     *
     * @param transactionId The transaction identifier
     * @return A chronologically ordered list of events representing the transaction lifecycle
     * @throws IllegalArgumentException if the transaction does not exist
     */
    List<PaymentEvent> getLifecycleHistory(UUID transactionId);

    /**
     * Verifies that a transaction is in the expected state.
     *
     * @param transaction The transaction to verify
     * @param expectedStatus The expected status
     * @throws IllegalStateException if the transaction is not in the expected state
     */
    void verifyTransactionState(PaymentTransaction transaction, PaymentStatus expectedStatus);

    /**
     * Validates a transaction's current state against a set of allowed states.
     *
     * @param transaction The transaction to validate
     * @param allowedStatuses The set of allowed statuses
     * @return true if the transaction is in one of the allowed states, false otherwise
     */
    boolean validateTransactionState(PaymentTransaction transaction, PaymentStatus... allowedStatuses);

    /**
     * Gets a summary of the transaction's lifecycle including current status,
     * time spent in each state, and key lifecycle events.
     *
     * @param transactionId The transaction identifier
     * @return A map containing lifecycle summary information
     * @throws IllegalArgumentException if the transaction does not exist
     */
    Map<String, Object> getLifecycleSummary(UUID transactionId);
}