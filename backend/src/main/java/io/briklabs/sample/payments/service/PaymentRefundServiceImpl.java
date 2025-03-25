package io.briklabs.sample.payments.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

/**
 * Implementation of the PaymentRefundService interface that provides the business logic
 * for payment refund operations. This class handles refund requests, validates refund amounts,
 * manages refund approval workflows, and ensures proper event tracking for audit purposes.
 */
public class PaymentRefundServiceImpl implements PaymentRefundService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRefundServiceImpl.class);
    
    private final PaymentTransactionService transactionService;
    private final PaymentEventService eventService;
    private final PaymentLifecycleService lifecycleService;
    private final PaymentValidationService validationService;
    
    // Threshold amount that requires approval for refunds
    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("1000.00");
    
    // Map to store pending refund requests that require approval
    private final Map<UUID, RefundRequest> pendingRefunds = new HashMap<>();
    
    /**
     * Constructor with required dependencies.
     * 
     * @param transactionService Service for transaction operations
     * @param eventService Service for event tracking
     * @param lifecycleService Service for managing transaction lifecycle
     * @param validationService Service for validation operations
     */
    public PaymentRefundServiceImpl(
            PaymentTransactionService transactionService,
            PaymentEventService eventService,
            PaymentLifecycleService lifecycleService,
            PaymentValidationService validationService) {
        this.transactionService = transactionService;
        this.eventService = eventService;
        this.lifecycleService = lifecycleService;
        this.validationService = validationService;
    }
    
    /**
     * Process a full refund for a payment transaction.
     * 
     * @param transactionId The unique identifier of the transaction to refund
     * @param reason Optional reason for the refund
     * @param userId The ID of the user initiating the refund
     * @return The updated transaction with refund information
     * @throws IllegalArgumentException if the transaction cannot be refunded
     */
    @Override
    public PaymentTransaction processFullRefund(UUID transactionId, String reason, String userId) {
        logger.info("Processing full refund for transaction: {}", transactionId);
        
        // Get the transaction
        PaymentTransaction transaction = getAndValidateTransaction(transactionId);
        
        // Get the full refundable amount
        BigDecimal refundAmount = getRefundableAmount(transactionId);
        
        // Check if refund requires approval
        if (refundRequiresApproval(transactionId, refundAmount)) {
            logger.info("Full refund requires approval for transaction: {}", transactionId);
            return createPendingRefund(transaction, refundAmount, reason, userId, true);
        }
        
        // Process the refund
        return executeRefund(transaction, refundAmount, reason, userId, true);
    }
    
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
    @Override
    public PaymentTransaction processPartialRefund(UUID transactionId, BigDecimal amount, String reason, String userId) {
        logger.info("Processing partial refund of {} for transaction: {}", amount, transactionId);
        
        // Get the transaction
        PaymentTransaction transaction = getAndValidateTransaction(transactionId);
        
        // Validate the refund amount
        if (!validateRefundAmount(transactionId, amount)) {
            throw new IllegalArgumentException("Invalid refund amount: " + amount);
        }
        
        // Check if refund requires approval
        if (refundRequiresApproval(transactionId, amount)) {
            logger.info("Partial refund requires approval for transaction: {}", transactionId);
            return createPendingRefund(transaction, amount, reason, userId, false);
        }
        
        // Process the refund
        return executeRefund(transaction, amount, reason, userId, false);
    }
    
    /**
     * Verify if a transaction can be refunded.
     * 
     * @param transactionId The unique identifier of the transaction to check
     * @return true if the transaction can be refunded, false otherwise
     */
    @Override
    public boolean canRefund(UUID transactionId) {
        try {
            Optional<PaymentTransaction> transactionOpt = transactionService.getTransactionById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.warn("Transaction not found for refund check: {}", transactionId);
                return false;
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            return lifecycleService.canRefund(transaction);
        } catch (Exception e) {
            logger.error("Error checking if transaction can be refunded: {}", transactionId, e);
            return false;
        }
    }
    
    /**
     * Get the maximum refundable amount for a transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @return The maximum amount that can be refunded
     * @throws IllegalArgumentException if the transaction does not exist or is not in a refundable state
     */
    @Override
    public BigDecimal getRefundableAmount(UUID transactionId) {
        PaymentTransaction transaction = getAndValidateTransaction(transactionId);
        
        // Get the total amount that has already been refunded
        BigDecimal refundedAmount = getAlreadyRefundedAmount(transactionId);
        
        // Calculate the remaining refundable amount
        BigDecimal refundableAmount = transaction.getAmount().subtract(refundedAmount);
        
        if (refundableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction has already been fully refunded: " + transactionId);
        }
        
        return refundableAmount;
    }
    
    /**
     * Validate a refund amount for a specific transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @param amount The refund amount to validate
     * @return true if the amount is valid for refund, false otherwise
     */
    @Override
    public boolean validateRefundAmount(UUID transactionId, BigDecimal amount) {
        try {
            // Amount must be positive
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Invalid refund amount (must be positive): {}", amount);
                return false;
            }
            
            // Get the maximum refundable amount
            BigDecimal refundableAmount = getRefundableAmount(transactionId);
            
            // Amount must not exceed the refundable amount
            if (amount.compareTo(refundableAmount) > 0) {
                logger.warn("Refund amount exceeds refundable amount: {} > {}", amount, refundableAmount);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error validating refund amount for transaction: {}", transactionId, e);
            return false;
        }
    }
    
    /**
     * Get the refund history for a transaction.
     * 
     * @param transactionId The unique identifier of the transaction
     * @return A list of refund events for the transaction
     */
    @Override
    public List<PaymentEvent> getRefundHistory(UUID transactionId) {
        List<PaymentEvent> allEvents = eventService.getEventsByTransactionId(transactionId);
        
        // Filter for refund events only
        return allEvents.stream()
                .filter(event -> "REFUND".equals(event.getEventType()))
                .collect(Collectors.toList());
    }
    
    /**
     * Check the status of a refund operation.
     * 
     * @param refundId The unique identifier of the refund operation
     * @return The current status of the refund operation
     * @throws IllegalArgumentException if the refund operation does not exist
     */
    @Override
    public String getRefundStatus(UUID refundId) {
        if (!pendingRefunds.containsKey(refundId)) {
            // Check if this is a completed refund by looking up events
            List<PaymentEvent> events = eventService.getEventsByCorrelationId(refundId);
            if (!events.isEmpty()) {
                return "COMPLETED";
            }
            throw new IllegalArgumentException("Refund operation not found: " + refundId);
        }
        
        RefundRequest request = pendingRefunds.get(refundId);
        return request.getStatus();
    }
    
    /**
     * Approve a refund that requires explicit approval.
     * 
     * @param refundId The unique identifier of the refund operation
     * @param approverId The ID of the user approving the refund
     * @param notes Optional approval notes
     * @return The updated transaction with approved refund
     * @throws IllegalArgumentException if the refund cannot be approved
     */
    @Override
    public PaymentTransaction approveRefund(UUID refundId, String approverId, String notes) {
        logger.info("Approving refund: {}", refundId);
        
        if (!pendingRefunds.containsKey(refundId)) {
            throw new IllegalArgumentException("Pending refund not found: " + refundId);
        }
        
        RefundRequest request = pendingRefunds.get(refundId);
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Refund is not in PENDING status: " + refundId);
        }
        
        // Update the request status
        request.setStatus("APPROVED");
        request.setApproverId(approverId);
        request.setApprovalNotes(notes);
        
        // Process the approved refund
        PaymentTransaction transaction = getAndValidateTransaction(request.getTransactionId());
        PaymentTransaction result = executeRefund(
                transaction, 
                request.getAmount(), 
                request.getReason(), 
                request.getUserId(), 
                request.isFullRefund());
        
        // Record approval event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("refundId", refundId.toString());
        metadata.put("approverId", approverId);
        metadata.put("notes", notes != null ? notes : "");
        metadata.put("amount", request.getAmount().toString());
        
        eventService.recordCustomEvent(
                transaction, 
                "REFUND_APPROVED", 
                approverId, 
                metadata);
        
        // Remove from pending refunds
        pendingRefunds.remove(refundId);
        
        return result;
    }
    
    /**
     * Reject a refund that requires explicit approval.
     * 
     * @param refundId The unique identifier of the refund operation
     * @param rejecterId The ID of the user rejecting the refund
     * @param reason Required reason for rejection
     * @return The updated transaction with rejected refund
     * @throws IllegalArgumentException if the refund cannot be rejected or reason is not provided
     */
    @Override
    public PaymentTransaction rejectRefund(UUID refundId, String rejecterId, String reason) {
        logger.info("Rejecting refund: {}", refundId);
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        
        if (!pendingRefunds.containsKey(refundId)) {
            throw new IllegalArgumentException("Pending refund not found: " + refundId);
        }
        
        RefundRequest request = pendingRefunds.get(refundId);
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Refund is not in PENDING status: " + refundId);
        }
        
        // Update the request status
        request.setStatus("REJECTED");
        
        // Get the transaction
        PaymentTransaction transaction = getAndValidateTransaction(request.getTransactionId());
        
        // Record rejection event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("refundId", refundId.toString());
        metadata.put("rejecterId", rejecterId);
        metadata.put("reason", reason);
        metadata.put("amount", request.getAmount().toString());
        
        eventService.recordCustomEvent(
                transaction, 
                "REFUND_REJECTED", 
                rejecterId, 
                metadata);
        
        // Remove from pending refunds
        pendingRefunds.remove(refundId);
        
        return transaction;
    }
    
    /**
     * Check if a refund requires approval based on business rules.
     * 
     * @param transactionId The unique identifier of the transaction
     * @param amount The proposed refund amount
     * @return true if the refund requires approval, false if it can be processed immediately
     */
    @Override
    public boolean refundRequiresApproval(UUID transactionId, BigDecimal amount) {
        // Refunds over the threshold amount require approval
        if (amount.compareTo(APPROVAL_THRESHOLD) >= 0) {
            return true;
        }
        
        // Additional business rules could be implemented here
        // For example, checking if the transaction is older than X days
        
        return false;
    }
    
    /**
     * Helper method to get and validate a transaction for refund operations.
     * 
     * @param transactionId The transaction ID
     * @return The validated transaction
     * @throws IllegalArgumentException if the transaction is invalid for refund
     */
    private PaymentTransaction getAndValidateTransaction(UUID transactionId) {
        Optional<PaymentTransaction> transactionOpt = transactionService.getTransactionById(transactionId);
        
        if (!transactionOpt.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = transactionOpt.get();
        
        // Validate that the transaction can be refunded
        if (!lifecycleService.canRefund(transaction)) {
            throw new IllegalArgumentException("Transaction cannot be refunded: " + transactionId);
        }
        
        return transaction;
    }
    
    /**
     * Helper method to calculate the already refunded amount for a transaction.
     * 
     * @param transactionId The transaction ID
     * @return The total amount already refunded
     */
    private BigDecimal getAlreadyRefundedAmount(UUID transactionId) {
        List<PaymentEvent> refundEvents = getRefundHistory(transactionId);
        
        // Sum up all refund amounts from events
        return refundEvents.stream()
                .map(event -> {
                    try {
                        String eventData = event.getEventData();
                        // Extract amount from event data JSON
                        // This is a simplified approach - in a real implementation,
                        // proper JSON parsing would be used
                        if (eventData != null && eventData.contains("\"amount\":")) {
                            int start = eventData.indexOf("\"amount\":") + 9;
                            int end = eventData.indexOf(",", start);
                            if (end == -1) {
                                end = eventData.indexOf("}", start);
                            }
                            String amountStr = eventData.substring(start, end).trim();
                            // Remove quotes if present
                            amountStr = amountStr.replace("\"", "");
                            return new BigDecimal(amountStr);
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing refund amount from event: {}", event.getEventId(), e);
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Helper method to create a pending refund request that requires approval.
     * 
     * @param transaction The transaction to refund
     * @param amount The refund amount
     * @param reason The refund reason
     * @param userId The user ID initiating the refund
     * @param isFullRefund Whether this is a full refund
     * @return The transaction with pending refund status
     */
    private PaymentTransaction createPendingRefund(
            PaymentTransaction transaction, 
            BigDecimal amount, 
            String reason, 
            String userId,
            boolean isFullRefund) {
        
        // Create a unique ID for this refund request
        UUID refundId = UUID.randomUUID();
        
        // Create and store the pending refund request
        RefundRequest request = new RefundRequest(
                refundId,
                transaction.getTransactionId(),
                amount,
                reason,
                userId,
                isFullRefund);
        
        pendingRefunds.put(refundId, request);
        
        // Record the pending refund event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("refundId", refundId.toString());
        metadata.put("amount", amount.toString());
        metadata.put("reason", reason != null ? reason : "");
        metadata.put("isFullRefund", String.valueOf(isFullRefund));
        
        eventService.recordCustomEvent(
                transaction, 
                "REFUND_PENDING_APPROVAL", 
                userId, 
                metadata);
        
        return transaction;
    }
    
    /**
     * Helper method to execute a refund operation.
     * 
     * @param transaction The transaction to refund
     * @param amount The refund amount
     * @param reason The refund reason
     * @param userId The user ID initiating the refund
     * @param isFullRefund Whether this is a full refund
     * @return The updated transaction
     */
    private PaymentTransaction executeRefund(
            PaymentTransaction transaction, 
            BigDecimal amount, 
            String reason, 
            String userId,
            boolean isFullRefund) {
        
        // Determine the new status based on whether this is a full or partial refund
        PaymentStatus newStatus;
        
        if (isFullRefund || amount.compareTo(transaction.getAmount()) >= 0) {
            // Full refund
            newStatus = PaymentStatus.REFUNDED;
        } else {
            // Partial refund
            newStatus = PaymentStatus.PARTIALLY_REFUNDED;
        }
        
        // Update the transaction status through the lifecycle service
        Map<String, String> metadata = new HashMap<>();
        metadata.put("refundAmount", amount.toString());
        metadata.put("reason", reason != null ? reason : "");
        metadata.put("isFullRefund", String.valueOf(isFullRefund));
        
        PaymentTransaction updatedTransaction = lifecycleService.executeStateTransition(
                transaction, 
                newStatus, 
                userId, 
                metadata);
        
        // Record the refund event
        eventService.recordRefundEvent(
                updatedTransaction, 
                userId, 
                amount.toString(), 
                reason, 
                !isFullRefund);
        
        logger.info("Refund processed successfully for transaction: {}, amount: {}, isFullRefund: {}", 
                transaction.getTransactionId(), amount, isFullRefund);
        
        return updatedTransaction;
    }
    
    /**
     * Inner class to represent a pending refund request.
     */
    private static class RefundRequest {
        private final UUID refundId;
        private final UUID transactionId;
        private final BigDecimal amount;
        private final String reason;
        private final String userId;
        private final boolean isFullRefund;
        private String status;
        private String approverId;
        private String approvalNotes;
        
        public RefundRequest(UUID refundId, UUID transactionId, BigDecimal amount, 
                            String reason, String userId, boolean isFullRefund) {
            this.refundId = refundId;
            this.transactionId = transactionId;
            this.amount = amount;
            this.reason = reason;
            this.userId = userId;
            this.isFullRefund = isFullRefund;
            this.status = "PENDING";
        }
        
        public UUID getRefundId() {
            return refundId;
        }
        
        public UUID getTransactionId() {
            return transactionId;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public boolean isFullRefund() {
            return isFullRefund;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getApproverId() {
            return approverId;
        }
        
        public void setApproverId(String approverId) {
            this.approverId = approverId;
        }
        
        public String getApprovalNotes() {
            return approvalNotes;
        }
        
        public void setApprovalNotes(String approvalNotes) {
            this.approvalNotes = approvalNotes;
        }
    }
}