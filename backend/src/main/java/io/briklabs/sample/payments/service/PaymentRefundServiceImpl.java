package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the PaymentRefundService interface that provides the business logic
 * for payment refund operations. This class handles refund requests, validates refund amounts,
 * manages refund approval workflows, and ensures proper event tracking for audit purposes.
 */
public class PaymentRefundServiceImpl implements PaymentRefundService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRefundServiceImpl.class);
    
    private final PaymentTransactionDAO transactionDAO;
    private final PaymentEventDAO eventDAO;
    private final PaymentTransactionService transactionService;
    private final PaymentValidationService validationService;
    private final PaymentLifecycleService lifecycleService;
    
    /**
     * Constructs a new PaymentRefundServiceImpl with required dependencies.
     *
     * @param daoFactory Factory for creating data access objects
     * @param transactionService Service for transaction operations
     * @param validationService Service for validation operations
     * @param lifecycleService Service for lifecycle management
     */
    public PaymentRefundServiceImpl(
            PaymentDAOFactory daoFactory,
            PaymentTransactionService transactionService,
            PaymentValidationService validationService,
            PaymentLifecycleService lifecycleService) {
        this.transactionDAO = daoFactory.getPaymentTransactionDAO();
        this.eventDAO = daoFactory.getPaymentEventDAO();
        this.transactionService = transactionService;
        this.validationService = validationService;
        this.lifecycleService = lifecycleService;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction refundTransaction(UUID transactionId) {
        logger.info("Processing full refund for transaction: {}", transactionId);
        
        // Retrieve the transaction
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Validate the refund operation
        validateRefundOperation(transaction, null);
        
        // Process the refund
        try {
            // Create refund initiated event
            PaymentEvent refundEvent = PaymentEvent.createRefundEvent(
                    transaction,
                    "system",
                    transaction.getAmount().toString(),
                    "Full refund requested"
            );
            eventDAO.create(refundEvent);
            
            // Update transaction status to REFUNDED
            transaction.updateStatus(PaymentStatus.REFUNDED);
            transaction.setUpdatedAt(Instant.now());
            
            // Create status change event
            PaymentEvent statusEvent = PaymentEvent.createStatusChangeEvent(
                    transaction,
                    PaymentStatus.REFUNDED,
                    "system"
            );
            eventDAO.create(statusEvent);
            
            // Update the transaction in the database
            transaction = transactionDAO.update(transaction);
            
            logger.info("Full refund processed successfully for transaction: {}", transactionId);
            return transaction;
        } catch (Exception e) {
            logger.error("Error processing refund for transaction: {}", transactionId, e);
            
            // Create error event
            PaymentEvent errorEvent = PaymentEvent.createErrorEvent(
                    transaction,
                    "system",
                    "REFUND_ERROR",
                    e.getMessage()
            );
            eventDAO.create(errorEvent);
            
            throw new RuntimeException("Failed to process refund: " + e.getMessage(), e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction refundPartialTransaction(UUID transactionId, BigDecimal amount) {
        logger.info("Processing partial refund of {} for transaction: {}", amount, transactionId);
        
        // Retrieve the transaction
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Validate the refund operation and amount
        validateRefundOperation(transaction, amount);
        validateRefundAmount(transaction, amount);
        
        // Check if this would be a full refund
        BigDecimal totalRefunded = getTotalRefundedAmount(transactionId);
        BigDecimal remainingAfterRefund = transaction.getAmount().subtract(totalRefunded).subtract(amount);
        
        if (remainingAfterRefund.compareTo(BigDecimal.ZERO) <= 0) {
            // This is effectively a full refund, so process it as such
            logger.info("Partial refund amount equals or exceeds remaining amount, processing as full refund");
            return refundTransaction(transactionId);
        }
        
        // Process the partial refund
        try {
            // Create refund initiated event with partial amount
            PaymentEvent refundEvent = PaymentEvent.createRefundEvent(
                    transaction,
                    "system",
                    amount.toString(),
                    "Partial refund requested"
            );
            eventDAO.create(refundEvent);
            
            // For partial refunds, we still keep the transaction in CAPTURED state
            // but record the refund amount in the event data
            transaction.setUpdatedAt(Instant.now());
            
            // Update the transaction in the database
            transaction = transactionDAO.update(transaction);
            
            logger.info("Partial refund of {} processed successfully for transaction: {}", 
                    amount, transactionId);
            return transaction;
        } catch (Exception e) {
            logger.error("Error processing partial refund for transaction: {}", transactionId, e);
            
            // Create error event
            PaymentEvent errorEvent = PaymentEvent.createErrorEvent(
                    transaction,
                    "system",
                    "PARTIAL_REFUND_ERROR",
                    e.getMessage()
            );
            eventDAO.create(errorEvent);
            
            throw new RuntimeException("Failed to process partial refund: " + e.getMessage(), e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRefunded(UUID transactionId) {
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        return transaction.getStatus() == PaymentStatus.REFUNDED;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRefund(UUID transactionId) {
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        return transaction.canRefund();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void validateRefundOperation(PaymentTransaction transaction, BigDecimal refundAmount) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        // Check if transaction is in a refundable state
        if (!transaction.canRefund()) {
            throw new IllegalStateException(
                    "Transaction is not in a refundable state. Current status: " + 
                    transaction.getStatus());
        }
        
        // If refund amount is provided, validate it
        if (refundAmount != null) {
            validateRefundAmount(transaction, refundAmount);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void validateRefundAmount(PaymentTransaction transaction, BigDecimal refundAmount) {
        if (refundAmount == null) {
            throw new IllegalArgumentException("Refund amount cannot be null");
        }
        
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }
        
        BigDecimal totalRefunded = getTotalRefundedAmount(transaction.getTransactionId());
        BigDecimal remainingAmount = transaction.getAmount().subtract(totalRefunded);
        
        if (refundAmount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount exceeds remaining refundable amount. " +
                    "Requested: " + refundAmount + ", Available: " + remainingAmount);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getTotalRefundedAmount(UUID transactionId) {
        // Retrieve all refund events for this transaction
        List<PaymentEvent> refundEvents = getRefundEvents(transactionId);
        
        // Sum up all refund amounts
        BigDecimal totalRefunded = BigDecimal.ZERO;
        for (PaymentEvent event : refundEvents) {
            // Extract refund amount from event data
            // Note: In a real implementation, this would parse the JSON event data properly
            String eventData = event.getEventData();
            if (eventData != null && eventData.contains("refundAmount")) {
                try {
                    // Simple parsing for demonstration purposes
                    // In production, use a proper JSON parser
                    String amountStr = eventData.split("refundAmount\":\"")[1].split("\"")[0];
                    BigDecimal refundAmount = new BigDecimal(amountStr);
                    totalRefunded = totalRefunded.add(refundAmount);
                } catch (Exception e) {
                    logger.warn("Failed to parse refund amount from event data: {}", eventData, e);
                }
            }
        }
        
        return totalRefunded;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRemainingRefundAmount(UUID transactionId) {
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        BigDecimal totalRefunded = getTotalRefundedAmount(transactionId);
        return transaction.getAmount().subtract(totalRefunded);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction approveRefund(UUID transactionId, String approverUserId, String notes) {
        logger.info("Approving refund for transaction: {} by user: {}", transactionId, approverUserId);
        
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Check if there are pending refunds to approve
        List<PaymentEvent> pendingRefunds = getPendingRefundEvents(transactionId);
        if (pendingRefunds.isEmpty()) {
            throw new IllegalStateException("No pending refunds found for transaction: " + transactionId);
        }
        
        try {
            // Create refund approval event
            PaymentEvent approvalEvent = new PaymentEvent(
                    transaction.getTransactionId(),
                    "REFUND_APPROVED",
                    approverUserId
            );
            approvalEvent.setEventData("{\"notes\":\"" + (notes != null ? notes : "") + "\"}");
            eventDAO.create(approvalEvent);
            
            // If this is a full refund, update the transaction status
            BigDecimal totalRefunded = getTotalRefundedAmount(transactionId);
            if (totalRefunded.compareTo(transaction.getAmount()) >= 0) {
                transaction.updateStatus(PaymentStatus.REFUNDED);
                transaction.setUpdatedAt(Instant.now());
                
                // Create status change event
                PaymentEvent statusEvent = PaymentEvent.createStatusChangeEvent(
                        transaction,
                        PaymentStatus.REFUNDED,
                        approverUserId
                );
                eventDAO.create(statusEvent);
            }
            
            // Update the transaction in the database
            transaction = transactionDAO.update(transaction);
            
            logger.info("Refund approved successfully for transaction: {}", transactionId);
            return transaction;
        } catch (Exception e) {
            logger.error("Error approving refund for transaction: {}", transactionId, e);
            
            // Create error event
            PaymentEvent errorEvent = PaymentEvent.createErrorEvent(
                    transaction,
                    approverUserId,
                    "REFUND_APPROVAL_ERROR",
                    e.getMessage()
            );
            eventDAO.create(errorEvent);
            
            throw new RuntimeException("Failed to approve refund: " + e.getMessage(), e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction rejectRefund(UUID transactionId, String rejectorUserId, String reason) {
        logger.info("Rejecting refund for transaction: {} by user: {}", transactionId, rejectorUserId);
        
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Check if there are pending refunds to reject
        List<PaymentEvent> pendingRefunds = getPendingRefundEvents(transactionId);
        if (pendingRefunds.isEmpty()) {
            throw new IllegalStateException("No pending refunds found for transaction: " + transactionId);
        }
        
        try {
            // Create refund rejection event
            PaymentEvent rejectionEvent = new PaymentEvent(
                    transaction.getTransactionId(),
                    "REFUND_REJECTED",
                    rejectorUserId
            );
            rejectionEvent.setEventData("{\"reason\":\"" + (reason != null ? reason : "") + "\"}");
            eventDAO.create(rejectionEvent);
            
            // Update the transaction timestamp
            transaction.setUpdatedAt(Instant.now());
            
            // Update the transaction in the database
            transaction = transactionDAO.update(transaction);
            
            logger.info("Refund rejected successfully for transaction: {}", transactionId);
            return transaction;
        } catch (Exception e) {
            logger.error("Error rejecting refund for transaction: {}", transactionId, e);
            
            // Create error event
            PaymentEvent errorEvent = PaymentEvent.createErrorEvent(
                    transaction,
                    rejectorUserId,
                    "REFUND_REJECTION_ERROR",
                    e.getMessage()
            );
            eventDAO.create(errorEvent);
            
            throw new RuntimeException("Failed to reject refund: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieves all refund events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of refund events
     */
    private List<PaymentEvent> getRefundEvents(UUID transactionId) {
        // In a real implementation, this would use the eventDAO to query for events
        // with event type "REFUND_INITIATED"
        // For simplicity, we're returning an empty list here
        return List.of(); // Placeholder
    }
    
    /**
     * Retrieves pending refund events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of pending refund events
     */
    private List<PaymentEvent> getPendingRefundEvents(UUID transactionId) {
        // In a real implementation, this would use the eventDAO to query for events
        // with event type "REFUND_INITIATED" that don't have corresponding
        // "REFUND_APPROVED" or "REFUND_REJECTED" events
        // For simplicity, we're returning an empty list here
        return List.of(); // Placeholder
    }
}