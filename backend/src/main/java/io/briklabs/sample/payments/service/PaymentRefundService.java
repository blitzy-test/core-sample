package io.briklabs.sample.payments.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentTransaction;

/**
 * Service interface for payment refund operations.
 * 
 * This service provides methods for processing full and partial refunds
 * for payment transactions. It handles the validation, execution, and
 * status tracking of refund operations throughout the payment lifecycle.
 */
public interface PaymentRefundService {
    
    /**
     * Process a full refund for a payment transaction.
     * 
     * @param transactionId The unique identifier of the transaction to refund
     * @param reason Optional reason for the refund
     * @param userId The ID of the user initiating the refund
     * @return The updated transaction with refund information
     * @throws IllegalArgumentException if the transaction cannot be refunded
     */
    PaymentTransaction processFullRefund(UUID transactionId, String reason, String userId);
    
    /**
     * Process a partial refund for a payment transaction.
     * 
     * @param transactionId The unique identifier of the transaction to refund
     * @param amount The amount to refund, must be greater than zero and less than or equal to the remaining refundable amount
     * @param reason Optional reason for the refund
     * @param userId The ID of the user initiating the refund
     * @return The updated transaction with refund information
     * @throws IllegalArgumentException if the amount is invalid or the transaction cannot be refunded
     */
    PaymentTransaction processPartialRefund(UUID transactionId, BigDecimal amount, String reason, String userId);
    
    /**
     * Verify if a transaction can be refunded.
     * 
     * @param transactionId The unique identifier of the transaction to check
     * @return true if the transaction can be refunded, false otherwise
     */
    boolean canRefund(UUID transactionId);
    
    /**
     * Get the maximum refundable amount for a transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @return The maximum amount that can be refunded
     * @throws IllegalArgumentException if the transaction does not exist or is not in a refundable state
     */
    BigDecimal getRefundableAmount(UUID transactionId);
    
    /**
     * Validate a refund amount for a specific transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @param amount The refund amount to validate
     * @return true if the amount is valid for refund, false otherwise
     */
    boolean validateRefundAmount(UUID transactionId, BigDecimal amount);
    
    /**
     * Get the refund history for a transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @return A list of refund events for the transaction
     */
    List<PaymentEvent> getRefundHistory(UUID transactionId);
    
    /**
     * Check the status of a refund operation.
     * 
     * @param refundId The unique identifier of the refund operation
     * @return The current status of the refund operation
     * @throws IllegalArgumentException if the refund operation does not exist
     */
    String getRefundStatus(UUID refundId);
    
    /**
     * Approve a refund that requires explicit approval.
     * 
     * @param refundId The unique identifier of the refund operation
     * @param approverId The ID of the user approving the refund
     * @param notes Optional approval notes
     * @return The updated transaction with approved refund
     * @throws IllegalArgumentException if the refund cannot be approved
     */
    PaymentTransaction approveRefund(UUID refundId, String approverId, String notes);
    
    /**
     * Reject a refund that requires explicit approval.
     * 
     * @param refundId The unique identifier of the refund operation
     * @param rejecterId The ID of the user rejecting the refund
     * @param reason Required reason for rejection
     * @return The updated transaction with rejected refund
     * @throws IllegalArgumentException if the refund cannot be rejected or reason is not provided
     */
    PaymentTransaction rejectRefund(UUID refundId, String rejecterId, String reason);
    
    /**
     * Check if a refund requires approval based on business rules.
     * 
     * @param transactionId The unique identifier of the transaction
     * @param amount The proposed refund amount
     * @return true if the refund requires approval, false if it can be processed immediately
     */
    boolean refundRequiresApproval(UUID transactionId, BigDecimal amount);
}