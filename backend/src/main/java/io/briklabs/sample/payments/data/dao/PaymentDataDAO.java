package io.briklabs.sample.payments.data.dao;

import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.SecurityException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.model.PaymentData;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for payment method data access operations, handling storage and retrieval
 * of payment instrument details.
 * <p>
 * This DAO manages all interactions with payment method information, including tokenized
 * card data, digital wallet identifiers, and billing information. It supports secure
 * storage patterns for sensitive payment data with appropriate masking and encryption,
 * while enabling retrieval by transaction ID and payment method type.
 * </p>
 */
public interface PaymentDataDAO extends PaymentDAO<PaymentData, UUID> {
    
    /**
     * Retrieves all payment data associated with a specific transaction.
     *
     * @param transactionId the unique identifier of the transaction
     * @return a list of payment data records associated with the transaction
     * @throws ResourceNotFoundException if the transaction does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByTransactionId(UUID transactionId) 
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException;
    
    /**
     * Retrieves payment data by payment method identifier.
     *
     * @param paymentMethodId the unique identifier of the payment method
     * @return an Optional containing the payment data if found, or empty if not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Optional<PaymentData> findByPaymentMethodId(String paymentMethodId)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Retrieves payment data by transaction ID and payment method type.
     *
     * @param transactionId the unique identifier of the transaction
     * @param paymentType the type of payment method
     * @return a list of payment data records matching the criteria
     * @throws ResourceNotFoundException if the transaction does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByTransactionIdAndPaymentType(UUID transactionId, String paymentType)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException;
    
    /**
     * Stores a new payment method with secure handling of sensitive data.
     *
     * @param paymentData the payment data to store
     * @return the stored payment data with generated IDs and tokenized information
     * @throws ValidationException if the payment data fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     * @throws SecurityException if secure storage operations fail
     */
    PaymentData secureStore(PaymentData paymentData)
            throws ValidationException, ConnectionException, QueryExecutionException, 
                   TransactionException, SecurityException;
    
    /**
     * Retrieves payment data with full access to sensitive information.
     * This method should only be used by authorized services with appropriate permissions.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @return the payment data with unmasked sensitive information
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws SecurityException if the caller lacks sufficient permissions
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    PaymentData retrieveSensitiveData(UUID paymentDataId)
            throws ResourceNotFoundException, SecurityException, ConnectionException, QueryExecutionException;
    
    /**
     * Updates the payment token for an existing payment method.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param newToken the new payment token
     * @return the updated payment data
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ValidationException if the new token fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     * @throws SecurityException if secure storage operations fail
     */
    PaymentData updatePaymentToken(UUID paymentDataId, String newToken)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException, SecurityException;
    
    /**
     * Updates the expiration date for a payment method.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param expirationMonth the new expiration month (1-12)
     * @param expirationYear the new expiration year (4-digit format)
     * @return the updated payment data
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ValidationException if the expiration date is invalid
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentData updateExpiration(UUID paymentDataId, int expirationMonth, int expirationYear)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException;
    
    /**
     * Updates the billing information associated with a payment method.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param billingData JSON representation of billing information
     * @return the updated payment data
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ValidationException if the billing data fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentData updateBillingData(UUID paymentDataId, String billingData)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException;
    
    /**
     * Retrieves payment data with masked sensitive information based on user role.
     * Different masking levels are applied depending on the user's role.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param userRole the role of the requesting user
     * @return the payment data with role-appropriate masking applied
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    PaymentData retrieveWithRoleBasedMasking(UUID paymentDataId, String userRole)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException;
    
    /**
     * Searches for payment data across multiple transactions based on filter criteria.
     *
     * @param filterParams the parameters to filter the search results
     * @return a list of payment data records matching the search criteria
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> searchPaymentData(PaymentFilterParams filterParams)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Marks a payment method as expired or invalid.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param reason the reason for invalidation
     * @return the updated payment data with invalid status
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentData invalidatePaymentMethod(UUID paymentDataId, String reason)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException, TransactionException;
    
    /**
     * Securely deletes sensitive payment data while maintaining transaction records.
     * This method replaces sensitive data with redaction markers rather than completely
     * removing the record, ensuring audit trail integrity.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @return true if the sensitive data was successfully deleted
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws SecurityException if the secure deletion operation fails
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    boolean secureDelete(UUID paymentDataId)
            throws ResourceNotFoundException, SecurityException, ConnectionException,
                   QueryExecutionException, TransactionException;
    
    /**
     * Finds payment data records with expiration dates in the specified range.
     *
     * @param startDate the start date of the expiration range (inclusive)
     * @param endDate the end date of the expiration range (inclusive)
     * @return a list of payment data records with expiration dates in the range
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByExpirationRange(LocalDate startDate, LocalDate endDate)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Finds payment data records that are expiring soon.
     *
     * @param monthsThreshold the number of months from now to consider "expiring soon"
     * @return a list of payment data records expiring within the threshold
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findExpiringSoon(int monthsThreshold)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Finds payment data records by payment type.
     *
     * @param paymentType the payment type to search for
     * @return a list of payment data records with the specified payment type
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByPaymentType(String paymentType)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Counts payment data records by payment type.
     *
     * @param paymentType the payment type to count
     * @return the count of payment data records with the specified payment type
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    long countByPaymentType(String paymentType)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Checks if a payment method is valid and not expired.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @return true if the payment method is valid and not expired, false otherwise
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    boolean isValidPaymentMethod(UUID paymentDataId)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException;
    
    /**
     * Retrieves payment data records for a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of payment data records associated with the organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByOrganizationId(UUID organizationId)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Retrieves payment data records for a specific account.
     *
     * @param accountId the unique identifier of the account
     * @return a list of payment data records associated with the account
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByAccountId(UUID accountId)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Retrieves payment data records for a specific merchant.
     *
     * @param merchantId the unique identifier of the merchant
     * @return a list of payment data records associated with the merchant
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByMerchantId(String merchantId)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Retrieves the most recently created payment data record for a transaction.
     *
     * @param transactionId the unique identifier of the transaction
     * @return an Optional containing the most recent payment data, or empty if none exists
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Optional<PaymentData> findMostRecentByTransactionId(UUID transactionId)
            throws ConnectionException, QueryExecutionException;
    
    /**
     * Retrieves payment data records created within a date range.
     *
     * @param startDate the start date of the creation range (inclusive)
     * @param endDate the end date of the creation range (inclusive)
     * @return a list of payment data records created within the date range
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentData> findByCreationDateRange(LocalDate startDate, LocalDate endDate)
            throws ConnectionException, QueryExecutionException;
}