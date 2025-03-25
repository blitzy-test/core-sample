package io.briklabs.sample.payments.data.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.SecurityException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.model.PaymentData;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;

/**
 * Interface for payment method data access operations.
 * <p>
 * This DAO manages all interactions with payment method information, including tokenized
 * card data, digital wallet identifiers, and billing information. It supports secure
 * storage patterns for sensitive payment data with appropriate masking and encryption,
 * while enabling retrieval by transaction ID and payment method type.
 * </p>
 * <p>
 * The implementation of this interface must adhere to security best practices for
 * handling sensitive payment information, including PCI DSS requirements where applicable.
 * </p>
 */
public interface PaymentDataDAO extends PaymentDAO<PaymentData, UUID> {

    /**
     * Retrieves all payment data associated with a specific transaction.
     * <p>
     * This method returns all payment method information linked to the given transaction ID,
     * with sensitive data appropriately masked based on the caller's access rights.
     * </p>
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
     * <p>
     * This method returns payment data associated with a specific payment method ID,
     * which may be a tokenized card, digital wallet identifier, or other payment instrument.
     * </p>
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
     * <p>
     * This method allows filtering payment data for a specific transaction by the
     * type of payment method (e.g., credit card, digital wallet, bank transfer).
     * </p>
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
     * <p>
     * This method ensures proper encryption and tokenization of sensitive payment information
     * before storing it in the database. It follows security best practices for payment data
     * protection.
     * </p>
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
     * <p>
     * This method provides unmasked access to sensitive payment data and should only
     * be used by authorized services with appropriate security clearance. It requires
     * elevated permissions and is subject to strict audit logging.
     * </p>
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
     * <p>
     * This method allows updating the tokenized representation of a payment method
     * while maintaining the association with the original transaction.
     * </p>
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
     * <p>
     * This method allows updating the expiration date of a payment instrument,
     * typically used when a card is reissued or extended.
     * </p>
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
     * <p>
     * This method allows updating the billing address and related information
     * for a payment method without changing the payment instrument itself.
     * </p>
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
     * <p>
     * This method returns payment data with appropriate field masking applied
     * according to the caller's role and permissions.
     * </p>
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
     * <p>
     * This method provides advanced search capabilities for payment methods,
     * supporting filtering by payment type, expiration date ranges, and other attributes.
     * </p>
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
     * <p>
     * This method updates the status of a payment method to indicate it is no longer
     * valid for use in new transactions, without deleting the record.
     * </p>
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
     * <p>
     * This method implements secure deletion of sensitive payment information
     * while preserving the transaction history and non-sensitive metadata.
     * It supports compliance with data retention policies and privacy regulations.
     * </p>
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
}