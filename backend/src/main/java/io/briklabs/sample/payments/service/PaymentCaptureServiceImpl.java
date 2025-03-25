package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * Implementation of the PaymentCaptureService interface that provides the business logic
 * for payment capture operations. This class handles the execution of capture requests,
 * validates capture amounts, manages the capture lifecycle states, and ensures proper
 * event tracking for audit purposes.
 */
public class PaymentCaptureServiceImpl implements PaymentCaptureService {

    private static final Logger logger = Logger.getLogger(PaymentCaptureServiceImpl.class.getName());
    
    private final PaymentTransactionService transactionService;
    private final PaymentEventService eventService;
    private final PaymentValidationService validationService;
    private final PaymentLifecycleService lifecycleService;

    /**
     * Constructs a new PaymentCaptureServiceImpl with required dependencies.
     *
     * @param transactionService Service for transaction operations
     * @param eventService Service for event tracking
     * @param validationService Service for validation operations
     * @param lifecycleService Service for lifecycle management
     */
    public PaymentCaptureServiceImpl(
            PaymentTransactionService transactionService,
            PaymentEventService eventService,
            PaymentValidationService validationService,
            PaymentLifecycleService lifecycleService) {
        this.transactionService = transactionService;
        this.eventService = eventService;
        this.validationService = validationService;
        this.lifecycleService = lifecycleService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction captureFullAmount(UUID transactionId, String userId, Map<String, String> metadata) {
        logger.info("Initiating full capture for transaction: " + transactionId);
        
        // Validate the capture operation
        validateCaptureOperation(transactionId, null, userId);
        
        // Retrieve the transaction
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        
        // Verify transaction is in a state that allows capture
        lifecycleService.verifyTransactionState(transaction, PaymentStatus.AUTHORIZED);
        
        // Prepare metadata if null
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("captureType", "FULL");
        metadata.put("captureAmount", transaction.getAmount().toString());
        
        // Execute state transition to CAPTURED
        PaymentTransaction updatedTransaction = lifecycleService.executeStateTransition(
                transaction, PaymentStatus.CAPTURED, userId, metadata);
        
        // Record capture event
        eventService.recordCaptureEvent(
                updatedTransaction, 
                userId, 
                transaction.getAmount().toString(), 
                false);
        
        logger.info("Full capture completed successfully for transaction: " + transactionId);
        return updatedTransaction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction capturePartialAmount(UUID transactionId, BigDecimal captureAmount, 
                                                  String userId, Map<String, String> metadata) {
        logger.info("Initiating partial capture for transaction: " + transactionId + 
                    " with amount: " + captureAmount);
        
        // Validate the capture operation
        validateCaptureOperation(transactionId, captureAmount, userId);
        
        // Retrieve the transaction
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        
        // Verify transaction is in a state that allows capture
        if (!transaction.getStatus().equals(PaymentStatus.AUTHORIZED) && 
            !transaction.getStatus().equals(PaymentStatus.PARTIALLY_CAPTURED)) {
            throw new IllegalStateException("Transaction must be in AUTHORIZED or PARTIALLY_CAPTURED status to perform a capture");
        }
        
        // Validate capture amount
        validationService.validateCaptureAmount(transaction, captureAmount);
        
        // Calculate total captured amount including this capture
        BigDecimal totalCapturedAmount = getTotalCapturedAmount(transactionId).add(captureAmount);
        
        // Determine if this will be a full capture or partial capture
        boolean isFullCapture = totalCapturedAmount.compareTo(transaction.getAmount()) >= 0;
        PaymentStatus newStatus = isFullCapture ? PaymentStatus.CAPTURED : PaymentStatus.PARTIALLY_CAPTURED;
        
        // Prepare metadata if null
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("captureType", isFullCapture ? "FULL" : "PARTIAL");
        metadata.put("captureAmount", captureAmount.toString());
        metadata.put("totalCapturedAmount", totalCapturedAmount.toString());
        metadata.put("remainingAmount", transaction.getAmount().subtract(totalCapturedAmount).toString());
        
        // Execute state transition
        PaymentTransaction updatedTransaction = lifecycleService.executeStateTransition(
                transaction, newStatus, userId, metadata);
        
        // Record capture event
        eventService.recordCaptureEvent(
                updatedTransaction, 
                userId, 
                captureAmount.toString(), 
                !isFullCapture);
        
        logger.info("Partial capture completed successfully for transaction: " + transactionId + 
                    " with amount: " + captureAmount + ", new status: " + newStatus);
        return updatedTransaction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getCaptureStatus(UUID transactionId) {
        logger.info("Retrieving capture status for transaction: " + transactionId);
        
        // Validate transaction exists
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        BigDecimal totalAuthorizedAmount = transaction.getAmount();
        BigDecimal totalCapturedAmount = getTotalCapturedAmount(transactionId);
        BigDecimal remainingAmount = totalAuthorizedAmount.subtract(totalCapturedAmount);
        
        // Get capture count from events
        int captureCount = eventService.countEventsByTransactionIdAndType(transactionId, "CAPTURE");
        
        // Build and return status map
        Map<String, Object> status = new HashMap<>();
        status.put("transactionId", transactionId.toString());
        status.put("currentStatus", transaction.getStatus().toString());
        status.put("totalAuthorizedAmount", totalAuthorizedAmount);
        status.put("totalCapturedAmount", totalCapturedAmount);
        status.put("remainingAmount", remainingAmount);
        status.put("isFullyCaptured", isFullyCaptured(transactionId));
        status.put("captureCount", captureCount);
        status.put("currency", transaction.getCurrency());
        
        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentEvent> getCaptureHistory(UUID transactionId) {
        logger.info("Retrieving capture history for transaction: " + transactionId);
        
        // Validate transaction exists
        if (!transactionService.getTransactionById(transactionId).isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Retrieve all capture events for this transaction
        return eventService.getEventsByTransactionIdAndType(transactionId, "CAPTURE");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canCapture(UUID transactionId) {
        logger.info("Checking if transaction can be captured: " + transactionId);
        
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        
        // Check if transaction is in a state that allows capture
        return transaction.getStatus().equals(PaymentStatus.AUTHORIZED) || 
               transaction.getStatus().equals(PaymentStatus.PARTIALLY_CAPTURED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidCaptureAmount(UUID transactionId, BigDecimal captureAmount) {
        logger.info("Validating capture amount for transaction: " + transactionId + 
                    " with amount: " + captureAmount);
        
        // Basic validation - amount must be positive
        if (captureAmount == null || captureAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Retrieve the transaction
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        
        // Calculate remaining amount available for capture
        BigDecimal remainingAmount = getRemainingCaptureAmount(transactionId);
        
        // Capture amount must be less than or equal to remaining amount
        return captureAmount.compareTo(remainingAmount) <= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRemainingCaptureAmount(UUID transactionId) {
        logger.info("Calculating remaining capture amount for transaction: " + transactionId);
        
        // Retrieve the transaction
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        BigDecimal totalAuthorizedAmount = transaction.getAmount();
        BigDecimal totalCapturedAmount = getTotalCapturedAmount(transactionId);
        
        return totalAuthorizedAmount.subtract(totalCapturedAmount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getTotalCapturedAmount(UUID transactionId) {
        logger.info("Calculating total captured amount for transaction: " + transactionId);
        
        // Retrieve all capture events for this transaction
        List<PaymentEvent> captureEvents = eventService.getEventsByTransactionIdAndType(transactionId, "CAPTURE");
        
        // Sum up all capture amounts from events
        BigDecimal totalCaptured = BigDecimal.ZERO;
        for (PaymentEvent event : captureEvents) {
            String eventData = event.getEventData();
            // Extract capture amount from event data
            // This is a simplified approach - in a real implementation, proper JSON parsing would be used
            if (eventData != null && eventData.contains("captureAmount")) {
                try {
                    // Simple parsing assuming format like {"captureAmount":"123.45",...}
                    int start = eventData.indexOf("captureAmount") + "captureAmount".length() + 3;
                    int end = eventData.indexOf("\"", start);
                    String amountStr = eventData.substring(start, end);
                    BigDecimal amount = new BigDecimal(amountStr);
                    totalCaptured = totalCaptured.add(amount);
                } catch (Exception e) {
                    logger.warning("Error parsing capture amount from event data: " + e.getMessage());
                }
            }
        }
        
        return totalCaptured;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PaymentEvent> getLastCaptureOperation(UUID transactionId) {
        logger.info("Retrieving last capture operation for transaction: " + transactionId);
        
        // Retrieve the most recent capture event
        return Optional.ofNullable(eventService.getMostRecentEventByType(transactionId, "CAPTURE"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFullyCaptured(UUID transactionId) {
        logger.info("Checking if transaction is fully captured: " + transactionId);
        
        // Retrieve the transaction
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        
        // Check if status is CAPTURED (which means fully captured)
        if (transaction.getStatus().equals(PaymentStatus.CAPTURED)) {
            return true;
        }
        
        // For transactions in other states, compare amounts
        BigDecimal totalAuthorizedAmount = transaction.getAmount();
        BigDecimal totalCapturedAmount = getTotalCapturedAmount(transactionId);
        
        // Transaction is fully captured if total captured amount equals or exceeds authorized amount
        // (should never exceed in practice, but checking >= for robustness)
        return totalCapturedAmount.compareTo(totalAuthorizedAmount) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCaptureOperation(UUID transactionId, BigDecimal captureAmount, String userId) {
        logger.info("Validating capture operation for transaction: " + transactionId);
        
        // Validate transaction exists
        Optional<PaymentTransaction> optionalTransaction = transactionService.getTransactionById(transactionId);
        if (!optionalTransaction.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = optionalTransaction.get();
        
        // Validate transaction state
        if (!transaction.getStatus().equals(PaymentStatus.AUTHORIZED) && 
            !transaction.getStatus().equals(PaymentStatus.PARTIALLY_CAPTURED)) {
            throw new IllegalStateException("Transaction must be in AUTHORIZED or PARTIALLY_CAPTURED status to perform a capture");
        }
        
        // For partial captures, validate the capture amount
        if (captureAmount != null) {
            // Amount must be positive
            if (captureAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Capture amount must be greater than zero");
            }
            
            // Amount must not exceed remaining amount
            BigDecimal remainingAmount = getRemainingCaptureAmount(transactionId);
            if (captureAmount.compareTo(remainingAmount) > 0) {
                throw new IllegalArgumentException("Capture amount exceeds remaining authorized amount. " +
                        "Requested: " + captureAmount + ", Available: " + remainingAmount);
            }
        }
        
        // Validate user permissions (simplified - in a real implementation, this would check against Access Rights module)
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required for capture operations");
        }
        
        // Additional business rule validations could be added here
        logger.info("Capture operation validation successful for transaction: " + transactionId);
    }
}