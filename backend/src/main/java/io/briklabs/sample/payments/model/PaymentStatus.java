package io.briklabs.sample.payments.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration representing all possible states in the payment transaction lifecycle.
 * This enum defines valid status values for payment transactions with appropriate
 * state transition rules and validation logic.
 */
public enum PaymentStatus {
    /**
     * Initial state when a transaction is first created but not yet processed.
     */
    CREATED("Created", "Transaction has been created but not yet processed"),
    
    /**
     * Transaction is being processed by the payment system.
     */
    PROCESSING("Processing", "Transaction is being processed"),
    
    /**
     * Transaction has been authorized but funds have not yet been captured.
     */
    AUTHORIZED("Authorized", "Transaction has been authorized but not yet captured"),
    
    /**
     * Funds have been successfully captured from the payment method.
     */
    CAPTURED("Captured", "Funds have been captured successfully"),
    
    /**
     * Transaction has been refunded (partially or fully).
     */
    REFUNDED("Refunded", "Transaction has been refunded"),
    
    /**
     * Transaction has failed during processing.
     */
    FAILED("Failed", "Transaction processing has failed"),
    
    /**
     * Transaction has been voided (canceled before capture).
     */
    VOIDED("Voided", "Transaction has been voided");
    
    /**
     * Set of status values that represent final states in the transaction lifecycle.
     * Transactions in these states cannot transition to any other state.
     */
    public static final Set<PaymentStatus> FINAL_STATES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(CAPTURED, REFUNDED, FAILED, VOIDED)));
    
    /**
     * Set of status values that represent active states in the transaction lifecycle.
     * Transactions in these states can still be processed further.
     */
    public static final Set<PaymentStatus> ACTIVE_STATES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(CREATED, PROCESSING, AUTHORIZED)));
    
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for PaymentStatus enum.
     * 
     * @param displayName Human-readable name for UI display
     * @param description Detailed description of the status
     */
    PaymentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Gets the human-readable display name for this status.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the detailed description of this status.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this status is a final state in the transaction lifecycle.
     * 
     * @return true if this is a final state, false otherwise
     */
    public boolean isFinalState() {
        return FINAL_STATES.contains(this);
    }
    
    /**
     * Checks if this status is an active state in the transaction lifecycle.
     * 
     * @return true if this is an active state, false otherwise
     */
    public boolean isActiveState() {
        return ACTIVE_STATES.contains(this);
    }
    
    /**
     * Validates whether a transition from the current status to the target status is allowed.
     * 
     * @param targetStatus The status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(PaymentStatus targetStatus) {
        if (this == targetStatus) {
            return true; // Same state transition is always allowed
        }
        
        if (this.isFinalState()) {
            return false; // Cannot transition from a final state
        }
        
        switch (this) {
            case CREATED:
                // From CREATED, can only go to PROCESSING or FAILED
                return targetStatus == PROCESSING || targetStatus == FAILED;
                
            case PROCESSING:
                // From PROCESSING, can go to AUTHORIZED, FAILED, or back to CREATED (if retry needed)
                return targetStatus == AUTHORIZED || targetStatus == FAILED || targetStatus == CREATED;
                
            case AUTHORIZED:
                // From AUTHORIZED, can go to CAPTURED, VOIDED, or FAILED
                return targetStatus == CAPTURED || targetStatus == VOIDED || targetStatus == FAILED;
                
            case CAPTURED:
                // From CAPTURED, can only go to REFUNDED (partial or full refund)
                return targetStatus == REFUNDED;
                
            default:
                return false; // Any other transition is invalid
        }
    }
    
    /**
     * Validates a state transition and throws an exception if it's invalid.
     * 
     * @param currentStatus The current status
     * @param targetStatus The target status to transition to
     * @throws IllegalStateException if the transition is not allowed
     */
    public static void validateTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", 
                            currentStatus.name(), targetStatus.name()));
        }
    }
    
    /**
     * Checks if the status represents a successful transaction.
     * 
     * @return true if this status indicates success, false otherwise
     */
    public boolean isSuccessful() {
        return this == AUTHORIZED || this == CAPTURED;
    }
    
    /**
     * Checks if the status represents a failed transaction.
     * 
     * @return true if this status indicates failure, false otherwise
     */
    public boolean isFailed() {
        return this == FAILED;
    }
    
    /**
     * Checks if the status allows for a capture operation.
     * 
     * @return true if capture is allowed, false otherwise
     */
    public boolean canCapture() {
        return this == AUTHORIZED;
    }
    
    /**
     * Checks if the status allows for a void operation.
     * 
     * @return true if void is allowed, false otherwise
     */
    public boolean canVoid() {
        return this == AUTHORIZED;
    }
    
    /**
     * Checks if the status allows for a refund operation.
     * 
     * @return true if refund is allowed, false otherwise
     */
    public boolean canRefund() {
        return this == CAPTURED;
    }
}