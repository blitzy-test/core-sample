package io.briklabs.sample.payments.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentType;

/**
 * Implementation of the PaymentValidationService interface that provides
 * comprehensive validation for payment operations.
 * <p>
 * This class implements various validation rules including amount validation,
 * currency validation, state transition validation, and business rule enforcement.
 * It's critical for maintaining data integrity and business rule compliance.
 * </p>
 */
public class PaymentValidationServiceImpl implements PaymentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentValidationServiceImpl.class);
    
    // Regular expression for validating ISO 4217 currency codes (3 uppercase letters)
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");
    
    // Regular expression for validating merchant IDs (alphanumeric with hyphens, 5-64 chars)
    private static final Pattern MERCHANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]{5,64}$");
    
    // Maximum allowed decimal places for monetary amounts
    private static final int MAX_DECIMAL_PLACES = 4;
    
    // Minimum transaction amount (in the smallest unit of any currency)
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    
    // Maximum transaction amount (arbitrary large value for validation)
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("999999999.9999");
    
    private final PaymentTransactionDAO transactionDAO;
    
    /**
     * Constructs a new PaymentValidationServiceImpl with the necessary dependencies.
     * 
     * @param daoFactory The factory for creating data access objects
     */
    public PaymentValidationServiceImpl(PaymentDAOFactory daoFactory) {
        this.transactionDAO = daoFactory.getPaymentTransactionDAO();
    }
    
    @Override
    public boolean validateTransactionData(PaymentTransaction transaction) {
        if (transaction == null) {
            logger.error("Transaction validation failed: transaction is null");
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        // Validate required fields
        if (transaction.getTransactionId() == null) {
            logger.error("Transaction validation failed: missing transaction ID");
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (transaction.getOrganizationId() == null) {
            logger.error("Transaction validation failed: missing organization ID for transaction {}", 
                    transaction.getTransactionId());
            throw new IllegalArgumentException("Organization ID is required");
        }
        
        if (transaction.getAccountId() == null) {
            logger.error("Transaction validation failed: missing account ID for transaction {}", 
                    transaction.getTransactionId());
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (transaction.getStatus() == null) {
            logger.error("Transaction validation failed: missing status for transaction {}", 
                    transaction.getTransactionId());
            throw new IllegalArgumentException("Transaction status is required");
        }
        
        // Validate amount and currency
        if (transaction.getAmount() == null) {
            logger.error("Transaction validation failed: missing amount for transaction {}", 
                    transaction.getTransactionId());
            throw new IllegalArgumentException("Transaction amount is required");
        }
        
        if (transaction.getCurrency() == null || transaction.getCurrency().isEmpty()) {
            logger.error("Transaction validation failed: missing currency for transaction {}", 
                    transaction.getTransactionId());
            throw new IllegalArgumentException("Transaction currency is required");
        }
        
        // Validate merchant and payment type
        if (transaction.getMerchantId() == null || transaction.getMerchantId().isEmpty()) {
            logger.error("Transaction validation failed: missing merchant ID for transaction {}", 
                    transaction.getTransactionId());
            throw new IllegalArgumentException("Merchant ID is required");
        }
        
        if (transaction.getPaymentType() == null || transaction.getPaymentType().isEmpty()) {
            logger.error("Transaction validation failed: missing payment type for transaction {}", 
                    transaction.getTransactionId());
            throw new IllegalArgumentException("Payment type is required");
        }
        
        // Validate amount and currency in detail
        validateAmountAndCurrency(transaction.getAmount(), transaction.getCurrency());
        
        // Validate merchant ID format
        validateMerchantId(transaction.getMerchantId());
        
        // Validate payment type
        validatePaymentType(transaction.getPaymentType());
        
        // All validations passed
        return true;
    }
    
    @Override
    public boolean validateAmountAndCurrency(BigDecimal amount, String currency) {
        // Validate amount
        if (amount == null) {
            logger.error("Amount validation failed: amount is null");
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        // Check if amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Amount validation failed: amount must be positive, got {}", amount);
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Check if amount is within allowed range
        if (amount.compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
            logger.error("Amount validation failed: amount {} is below minimum {}", amount, MIN_TRANSACTION_AMOUNT);
            throw new IllegalArgumentException("Amount is below minimum allowed value");
        }
        
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            logger.error("Amount validation failed: amount {} exceeds maximum {}", amount, MAX_TRANSACTION_AMOUNT);
            throw new IllegalArgumentException("Amount exceeds maximum allowed value");
        }
        
        // Check decimal places
        int decimalPlaces = amount.scale() < 0 ? 0 : amount.scale();
        if (decimalPlaces > MAX_DECIMAL_PLACES) {
            logger.error("Amount validation failed: amount {} has too many decimal places (max {})", 
                    amount, MAX_DECIMAL_PLACES);
            throw new IllegalArgumentException("Amount has too many decimal places (max " + MAX_DECIMAL_PLACES + ")");
        }
        
        // Validate currency
        if (currency == null || currency.isEmpty()) {
            logger.error("Currency validation failed: currency is null or empty");
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        
        // Check currency format (ISO 4217: 3 uppercase letters)
        if (!CURRENCY_CODE_PATTERN.matcher(currency).matches()) {
            logger.error("Currency validation failed: invalid currency format {}", currency);
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO 4217 code");
        }
        
        // All validations passed
        return true;
    }
    
    @Override
    public boolean validateCaptureAmount(UUID transactionId, BigDecimal captureAmount) {
        if (transactionId == null) {
            logger.error("Capture amount validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (captureAmount == null) {
            logger.error("Capture amount validation failed: capture amount is null for transaction {}", transactionId);
            throw new IllegalArgumentException("Capture amount cannot be null");
        }
        
        // Check if capture amount is positive
        if (captureAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Capture amount validation failed: amount must be positive, got {} for transaction {}", 
                    captureAmount, transactionId);
            throw new IllegalArgumentException("Capture amount must be positive");
        }
        
        try {
            // Retrieve the transaction to validate against
            Optional<PaymentTransaction> transactionOpt = transactionDAO.findById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.error("Capture amount validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Check if transaction is in a state that allows capture
            if (transaction.getStatus() != PaymentStatus.AUTHORIZED) {
                logger.error("Capture amount validation failed: transaction {} is in state {}, must be AUTHORIZED", 
                        transactionId, transaction.getStatus());
                throw new IllegalArgumentException("Transaction must be in AUTHORIZED state for capture");
            }
            
            // Check if capture amount is less than or equal to the original transaction amount
            if (captureAmount.compareTo(transaction.getAmount()) > 0) {
                logger.error("Capture amount validation failed: capture amount {} exceeds transaction amount {} for transaction {}", 
                        captureAmount, transaction.getAmount(), transactionId);
                throw new IllegalArgumentException("Capture amount cannot exceed the original transaction amount");
            }
            
            // All validations passed
            return true;
            
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Capture amount validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate capture amount due to database error", e);
        }
    }
    
    @Override
    public boolean validateRefundAmount(UUID transactionId, BigDecimal refundAmount) {
        if (transactionId == null) {
            logger.error("Refund amount validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (refundAmount == null) {
            logger.error("Refund amount validation failed: refund amount is null for transaction {}", transactionId);
            throw new IllegalArgumentException("Refund amount cannot be null");
        }
        
        // Check if refund amount is positive
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Refund amount validation failed: amount must be positive, got {} for transaction {}", 
                    refundAmount, transactionId);
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        
        try {
            // Retrieve the transaction to validate against
            Optional<PaymentTransaction> transactionOpt = transactionDAO.findById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.error("Refund amount validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Check if transaction is in a state that allows refund
            if (transaction.getStatus() != PaymentStatus.CAPTURED) {
                logger.error("Refund amount validation failed: transaction {} is in state {}, must be CAPTURED", 
                        transactionId, transaction.getStatus());
                throw new IllegalArgumentException("Transaction must be in CAPTURED state for refund");
            }
            
            // Check if refund amount is less than or equal to the original transaction amount
            if (refundAmount.compareTo(transaction.getAmount()) > 0) {
                logger.error("Refund amount validation failed: refund amount {} exceeds transaction amount {} for transaction {}", 
                        refundAmount, transaction.getAmount(), transactionId);
                throw new IllegalArgumentException("Refund amount cannot exceed the original transaction amount");
            }
            
            // TODO: In a real implementation, we would check against already refunded amounts
            // to ensure the total refunded amount doesn't exceed the captured amount
            
            // All validations passed
            return true;
            
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Refund amount validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate refund amount due to database error", e);
        }
    }
    
    @Override
    public boolean validateStateTransition(UUID transactionId, String currentState, String newState) {
        if (transactionId == null) {
            logger.error("State transition validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (currentState == null || currentState.isEmpty()) {
            logger.error("State transition validation failed: current state is null or empty for transaction {}", 
                    transactionId);
            throw new IllegalArgumentException("Current state cannot be null or empty");
        }
        
        if (newState == null || newState.isEmpty()) {
            logger.error("State transition validation failed: new state is null or empty for transaction {}", 
                    transactionId);
            throw new IllegalArgumentException("New state cannot be null or empty");
        }
        
        try {
            // Convert string states to enum values
            PaymentStatus currentStatusEnum = PaymentStatus.valueOf(currentState);
            PaymentStatus newStatusEnum = PaymentStatus.valueOf(newState);
            
            // Check if the transition is allowed
            if (!currentStatusEnum.canTransitionTo(newStatusEnum)) {
                logger.error("State transition validation failed: invalid transition from {} to {} for transaction {}", 
                        currentState, newState, transactionId);
                throw new IllegalArgumentException("Invalid state transition from " + currentState + " to " + newState);
            }
            
            // All validations passed
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("State transition validation failed: invalid state value for transaction {}", transactionId, e);
            throw new IllegalArgumentException("Invalid state value: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateTransactionExists(UUID transactionId) {
        if (transactionId == null) {
            logger.error("Transaction existence validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        try {
            boolean exists = transactionDAO.exists(transactionId);
            
            if (!exists) {
                logger.error("Transaction existence validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            // Transaction exists
            return true;
            
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Transaction existence validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate transaction existence due to database error", e);
        }
    }
    
    @Override
    public boolean validateTransactionState(UUID transactionId, String expectedState) {
        if (transactionId == null) {
            logger.error("Transaction state validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (expectedState == null || expectedState.isEmpty()) {
            logger.error("Transaction state validation failed: expected state is null or empty for transaction {}", 
                    transactionId);
            throw new IllegalArgumentException("Expected state cannot be null or empty");
        }
        
        try {
            // Convert string state to enum value
            PaymentStatus expectedStatusEnum = PaymentStatus.valueOf(expectedState);
            
            // Retrieve the transaction to validate state
            Optional<PaymentTransaction> transactionOpt = transactionDAO.findById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.error("Transaction state validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Check if transaction is in the expected state
            if (transaction.getStatus() != expectedStatusEnum) {
                logger.error("Transaction state validation failed: transaction {} is in state {}, expected {}", 
                        transactionId, transaction.getStatus(), expectedStatusEnum);
                throw new IllegalArgumentException("Transaction is in state " + transaction.getStatus() + 
                        ", expected " + expectedStatusEnum);
            }
            
            // Transaction is in the expected state
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("Transaction state validation failed: invalid state value for transaction {}", transactionId, e);
            throw new IllegalArgumentException("Invalid state value: " + e.getMessage());
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Transaction state validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate transaction state due to database error", e);
        }
    }
    
    @Override
    public boolean validateTransactionState(UUID transactionId, String[] allowedStates) {
        if (transactionId == null) {
            logger.error("Transaction state validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (allowedStates == null || allowedStates.length == 0) {
            logger.error("Transaction state validation failed: allowed states array is null or empty for transaction {}", 
                    transactionId);
            throw new IllegalArgumentException("Allowed states cannot be null or empty");
        }
        
        try {
            // Convert string states to enum values
            Set<PaymentStatus> allowedStatusEnums = new HashSet<>();
            for (String state : allowedStates) {
                if (state != null && !state.isEmpty()) {
                    allowedStatusEnums.add(PaymentStatus.valueOf(state));
                }
            }
            
            if (allowedStatusEnums.isEmpty()) {
                logger.error("Transaction state validation failed: no valid allowed states for transaction {}", 
                        transactionId);
                throw new IllegalArgumentException("No valid allowed states provided");
            }
            
            // Retrieve the transaction to validate state
            Optional<PaymentTransaction> transactionOpt = transactionDAO.findById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.error("Transaction state validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Check if transaction is in one of the allowed states
            if (!allowedStatusEnums.contains(transaction.getStatus())) {
                logger.error("Transaction state validation failed: transaction {} is in state {}, allowed states are {}", 
                        transactionId, transaction.getStatus(), Arrays.toString(allowedStates));
                throw new IllegalArgumentException("Transaction is in state " + transaction.getStatus() + 
                        ", which is not among the allowed states: " + Arrays.toString(allowedStates));
            }
            
            // Transaction is in one of the allowed states
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("Transaction state validation failed: invalid state value for transaction {}", transactionId, e);
            throw new IllegalArgumentException("Invalid state value: " + e.getMessage());
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Transaction state validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate transaction state due to database error", e);
        }
    }
    
    @Override
    public boolean validateTransactionOwnership(UUID transactionId, UUID organizationId, UUID accountId) {
        if (transactionId == null) {
            logger.error("Transaction ownership validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (organizationId == null) {
            logger.error("Transaction ownership validation failed: organization ID is null for transaction {}", 
                    transactionId);
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (accountId == null) {
            logger.error("Transaction ownership validation failed: account ID is null for transaction {}", 
                    transactionId);
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        try {
            // Retrieve the transaction to validate ownership
            Optional<PaymentTransaction> transactionOpt = transactionDAO.findById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.error("Transaction ownership validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Check if transaction belongs to the specified organization
            if (!transaction.getOrganizationId().equals(organizationId)) {
                logger.error("Transaction ownership validation failed: transaction {} belongs to organization {}, not {}", 
                        transactionId, transaction.getOrganizationId(), organizationId);
                throw new IllegalArgumentException("Transaction does not belong to the specified organization");
            }
            
            // Check if transaction belongs to the specified account
            if (!transaction.getAccountId().equals(accountId)) {
                logger.error("Transaction ownership validation failed: transaction {} belongs to account {}, not {}", 
                        transactionId, transaction.getAccountId(), accountId);
                throw new IllegalArgumentException("Transaction does not belong to the specified account");
            }
            
            // Transaction belongs to the specified organization and account
            return true;
            
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Transaction ownership validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate transaction ownership due to database error", e);
        }
    }
    
    @Override
    public boolean validateMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isEmpty()) {
            logger.error("Merchant ID validation failed: merchant ID is null or empty");
            throw new IllegalArgumentException("Merchant ID cannot be null or empty");
        }
        
        // Check merchant ID format
        if (!MERCHANT_ID_PATTERN.matcher(merchantId).matches()) {
            logger.error("Merchant ID validation failed: invalid merchant ID format {}", merchantId);
            throw new IllegalArgumentException("Merchant ID must be alphanumeric with hyphens, 5-64 characters");
        }
        
        // In a real implementation, we might check if the merchant ID exists in a merchant database
        
        // All validations passed
        return true;
    }
    
    @Override
    public boolean validatePaymentType(String paymentType) {
        if (paymentType == null || paymentType.isEmpty()) {
            logger.error("Payment type validation failed: payment type is null or empty");
            throw new IllegalArgumentException("Payment type cannot be null or empty");
        }
        
        try {
            // Check if the payment type is a valid enum value
            PaymentType.valueOf(paymentType);
            
            // All validations passed
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("Payment type validation failed: invalid payment type {}", paymentType);
            throw new IllegalArgumentException("Invalid payment type: " + paymentType);
        }
    }
    
    @Override
    public boolean validatePaymentData(String paymentMethodId, String paymentData) {
        if (paymentMethodId == null || paymentMethodId.isEmpty()) {
            logger.error("Payment data validation failed: payment method ID is null or empty");
            throw new IllegalArgumentException("Payment method ID cannot be null or empty");
        }
        
        // Basic validation for payment data presence
        if (paymentData == null || paymentData.isEmpty()) {
            logger.error("Payment data validation failed: payment data is null or empty for method {}", paymentMethodId);
            throw new IllegalArgumentException("Payment data cannot be null or empty");
        }
        
        // In a real implementation, we would validate the payment data format based on the payment method type
        // For example, credit card data would have different validation rules than bank transfer data
        
        // All validations passed
        return true;
    }
    
    @Override
    public boolean validateCaptureAllowed(UUID transactionId) {
        if (transactionId == null) {
            logger.error("Capture allowed validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        try {
            // Retrieve the transaction to validate
            Optional<PaymentTransaction> transactionOpt = transactionDAO.findById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.error("Capture allowed validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Check if transaction is in a state that allows capture
            if (transaction.getStatus() != PaymentStatus.AUTHORIZED) {
                logger.error("Capture allowed validation failed: transaction {} is in state {}, must be AUTHORIZED", 
                        transactionId, transaction.getStatus());
                throw new IllegalArgumentException("Transaction must be in AUTHORIZED state for capture");
            }
            
            // Check if the payment type supports capture
            PaymentType paymentTypeEnum = PaymentType.valueOf(transaction.getPaymentType());
            if (!paymentTypeEnum.supportsDelayedCapture()) {
                logger.error("Capture allowed validation failed: payment type {} does not support capture for transaction {}", 
                        paymentTypeEnum, transactionId);
                throw new IllegalArgumentException("Payment type does not support capture operations");
            }
            
            // All validations passed
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("Capture allowed validation failed: validation error for transaction {}", transactionId, e);
            throw new IllegalArgumentException("Capture not allowed: " + e.getMessage());
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Capture allowed validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate capture allowed due to database error", e);
        }
    }
    
    @Override
    public boolean validateRefundAllowed(UUID transactionId) {
        if (transactionId == null) {
            logger.error("Refund allowed validation failed: transaction ID is null");
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        try {
            // Retrieve the transaction to validate
            Optional<PaymentTransaction> transactionOpt = transactionDAO.findById(transactionId);
            
            if (!transactionOpt.isPresent()) {
                logger.error("Refund allowed validation failed: transaction {} not found", transactionId);
                throw new IllegalArgumentException("Transaction not found");
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Check if transaction is in a state that allows refund
            if (transaction.getStatus() != PaymentStatus.CAPTURED) {
                logger.error("Refund allowed validation failed: transaction {} is in state {}, must be CAPTURED", 
                        transactionId, transaction.getStatus());
                throw new IllegalArgumentException("Transaction must be in CAPTURED state for refund");
            }
            
            // In a real implementation, we would check if there are any funds available for refund
            // by comparing the original amount with already refunded amounts
            
            // All validations passed
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("Refund allowed validation failed: validation error for transaction {}", transactionId, e);
            throw new IllegalArgumentException("Refund not allowed: " + e.getMessage());
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Refund allowed validation failed: database error for transaction {}", transactionId, e);
            throw new IllegalStateException("Unable to validate refund allowed due to database error", e);
        }
    }
}