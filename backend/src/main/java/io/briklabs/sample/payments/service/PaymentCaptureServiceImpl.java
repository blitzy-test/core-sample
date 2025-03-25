package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the PaymentCaptureService interface that provides
 * the business logic for payment capture operations. This class handles
 * the execution of capture requests, validates capture amounts, manages
 * the capture lifecycle states, and ensures proper event tracking for
 * audit purposes.
 */
@Singleton
public class PaymentCaptureServiceImpl implements PaymentCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCaptureServiceImpl.class);
    
    private final PaymentTransactionService transactionService;
    private final PaymentLifecycleService lifecycleService;
    private final PaymentEventService eventService;
    private final PaymentValidationService validationService;
    private final PaymentTransactionDAO transactionDAO;
    
    // Lock for synchronizing capture operations on the same transaction
    private final Lock captureLock = new ReentrantLock();

    /**
     * Constructs a new PaymentCaptureServiceImpl with required dependencies.
     *
     * @param transactionService Service for transaction operations
     * @param lifecycleService Service for managing transaction lifecycle
     * @param eventService Service for recording transaction events
     * @param validationService Service for validating payment operations
     * @param daoFactory Factory for creating data access objects
     */
    @Inject
    public PaymentCaptureServiceImpl(
            PaymentTransactionService transactionService,
            PaymentLifecycleService lifecycleService,
            PaymentEventService eventService,
            PaymentValidationService validationService,
            PaymentDAOFactory daoFactory) {
        this.transactionService = transactionService;
        this.lifecycleService = lifecycleService;
        this.eventService = eventService;
        this.validationService = validationService;
        this.transactionDAO = daoFactory.getPaymentTransactionDAO();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction captureTransaction(UUID transactionId) {
        logger.info("Initiating full capture for transaction: {}", transactionId);
        
        // Retrieve the transaction
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            logger.error("Transaction not found for capture: {}", transactionId);
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Validate the capture operation
        validateCaptureOperation(transaction, null);
        
        try {
            captureLock.lock();
            
            // Double-check that the transaction is still in a valid state
            if (!canCapture(transactionId)) {
                logger.error("Transaction {} is no longer in a valid state for capture", transactionId);
                throw new IllegalStateException("Transaction is no longer in a valid state for capture: " + transactionId);
            }
            
            // Perform the capture operation
            transaction.updateStatus(PaymentStatus.CAPTURED);
            
            // Record the capture event
            eventService.recordCaptureEvent(
                    transaction.getTransactionId(),
                    transaction.getAmount(),
                    transaction.getStatus(),
                    PaymentStatus.CAPTURED,
                    "Full capture processed",
                    null
            );
            
            // Update the transaction in the database
            transaction = transactionDAO.update(transaction);
            
            logger.info("Full capture completed successfully for transaction: {}", transactionId);
            return transaction;
        } finally {
            captureLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction capturePartialTransaction(UUID transactionId, BigDecimal amount) {
        logger.info("Initiating partial capture for transaction: {} with amount: {}", transactionId, amount);
        
        // Retrieve the transaction
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            logger.error("Transaction not found for partial capture: {}", transactionId);
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Validate the capture operation and amount
        validateCaptureOperation(transaction, amount);
        validateCaptureAmount(transaction, amount);
        
        try {
            captureLock.lock();
            
            // Double-check that the transaction is still in a valid state
            if (!canCapture(transactionId)) {
                logger.error("Transaction {} is no longer in a valid state for partial capture", transactionId);
                throw new IllegalStateException("Transaction is no longer in a valid state for partial capture: " + transactionId);
            }
            
            // Get the current captured amount
            BigDecimal currentCapturedAmount = getTotalCapturedAmount(transactionId);
            
            // Calculate the new total captured amount
            BigDecimal newTotalCaptured = currentCapturedAmount.add(amount);
            
            // Check if this is a full capture (total captured equals authorized amount)
            boolean isFullCapture = newTotalCaptured.compareTo(transaction.getAmount()) == 0;
            
            // Update the transaction status
            PaymentStatus previousStatus = transaction.getStatus();
            if (isFullCapture) {
                transaction.updateStatus(PaymentStatus.CAPTURED);
            } else {
                // For partial captures, we use the same status but track the captured amount separately
                transaction.updateStatus(PaymentStatus.AUTHORIZED);
            }
            
            // Record the capture event with the partial amount
            eventService.recordPartialCaptureEvent(
                    transaction.getTransactionId(),
                    amount,
                    newTotalCaptured,
                    previousStatus,
                    transaction.getStatus(),
                    isFullCapture ? "Full amount captured" : "Partial amount captured",
                    null
            );
            
            // Update the transaction in the database
            transaction = transactionDAO.update(transaction);
            
            // Store the captured amount in the transaction metadata
            // This would typically be handled by a dedicated capture record in a real implementation
            // but for simplicity, we're using the event system to track this
            
            logger.info("Partial capture completed successfully for transaction: {}, amount: {}, total captured: {}", 
                    transactionId, amount, newTotalCaptured);
            return transaction;
        } finally {
            captureLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCaptured(UUID transactionId) {
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        return transaction.getStatus() == PaymentStatus.CAPTURED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canCapture(UUID transactionId) {
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        return transaction.canCapture();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCaptureOperation(PaymentTransaction transaction, BigDecimal captureAmount) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        // Check if the transaction is in a valid state for capture
        if (!transaction.canCapture()) {
            throw new IllegalStateException(
                    String.format("Transaction %s is in %s state which cannot be captured. Only AUTHORIZED transactions can be captured.",
                            transaction.getTransactionId(), transaction.getStatus())
            );
        }
        
        // Check if the payment type supports capture
        if (!transaction.getPaymentType().supportsDelayedCapture()) {
            throw new IllegalStateException(
                    String.format("Payment type %s does not support capture operations",
                            transaction.getPaymentType())
            );
        }
        
        // For partial captures, validate the amount
        if (captureAmount != null) {
            validateCaptureAmount(transaction, captureAmount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateCaptureAmount(PaymentTransaction transaction, BigDecimal captureAmount) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        if (captureAmount == null) {
            throw new IllegalArgumentException("Capture amount cannot be null");
        }
        
        // Amount must be greater than zero
        if (captureAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Capture amount must be greater than zero");
        }
        
        // Get the remaining amount that can be captured
        BigDecimal remainingAmount = getRemainingCaptureAmount(transaction.getTransactionId());
        
        // Amount must not exceed the remaining capturable amount
        if (captureAmount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException(
                    String.format("Capture amount %s exceeds the remaining capturable amount %s",
                            captureAmount, remainingAmount)
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getTotalCapturedAmount(UUID transactionId) {
        // In a real implementation, this would query a dedicated captures table
        // For this implementation, we'll use the event service to calculate the total
        
        // Get all capture events for this transaction
        BigDecimal totalCaptured = eventService.getTotalCapturedAmount(transactionId);
        
        // If no captures have been recorded yet, return zero
        return totalCaptured != null ? totalCaptured : BigDecimal.ZERO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getRemainingCaptureAmount(UUID transactionId) {
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Get the total amount that has been captured so far
        BigDecimal totalCaptured = getTotalCapturedAmount(transactionId);
        
        // Calculate the remaining amount that can be captured
        return transaction.getAmount().subtract(totalCaptured);
    }
}