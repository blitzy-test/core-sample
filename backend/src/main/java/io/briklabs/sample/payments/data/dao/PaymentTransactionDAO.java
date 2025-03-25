package io.briklabs.sample.payments.data.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentType;

/**
 * Interface for payment transaction data access operations providing specialized query methods
 * for transaction filtering, sorting, and status management.
 * <p>
 * This DAO handles all database interactions for payment transactions, supporting creation,
 * retrieval, updates, and complex querying capabilities. It enables the payment service layer
 * to perform operations like transaction listing, status updates, and financial reporting
 * without direct SQL knowledge.
 * </p>
 */
public interface PaymentTransactionDAO extends PaymentDAO<PaymentTransaction, UUID> {

    /**
     * Finds transactions by organization ID.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of transactions for the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions by organization ID and account ID.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param filterParams Additional filtering parameters
     * @return List of transactions for the specified organization and account
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, 
            PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions by status.
     *
     * @param status The transaction status
     * @param filterParams Additional filtering parameters
     * @return List of transactions with the specified status
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByStatus(PaymentStatus status, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions by multiple status values.
     *
     * @param statusFilter Filter containing multiple status values
     * @param filterParams Additional filtering parameters
     * @return List of transactions matching any of the specified statuses
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByStatusIn(StatusFilter statusFilter, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions created within a specific date range.
     *
     * @param dateRange The date range filter
     * @param filterParams Additional filtering parameters
     * @return List of transactions created within the specified date range
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByCreatedAtBetween(DateRangeFilter dateRange, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions updated within a specific date range.
     *
     * @param dateRange The date range filter
     * @param filterParams Additional filtering parameters
     * @return List of transactions updated within the specified date range
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByUpdatedAtBetween(DateRangeFilter dateRange, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions with amounts within a specific range.
     *
     * @param amountRange The amount range filter
     * @param filterParams Additional filtering parameters
     * @return List of transactions with amounts within the specified range
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByAmountBetween(AmountRangeFilter amountRange, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions by merchant ID.
     *
     * @param merchantId The merchant identifier
     * @param filterParams Additional filtering parameters
     * @return List of transactions for the specified merchant
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByMerchantId(String merchantId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions by payment type.
     *
     * @param paymentType The payment method type
     * @param filterParams Additional filtering parameters
     * @return List of transactions with the specified payment type
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByPaymentType(PaymentType paymentType, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions by external reference number.
     *
     * @param reference The external reference number
     * @return Optional containing the transaction if found, empty otherwise
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Optional<PaymentTransaction> findByTransactionReference(String reference) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Updates the status of a transaction.
     *
     * @param transactionId The transaction identifier
     * @param newStatus The new status to set
     * @return The updated transaction
     * @throws ResourceNotFoundException if the transaction is not found
     * @throws ValidationException if the status transition is invalid
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentTransaction updateStatus(UUID transactionId, PaymentStatus newStatus) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException;

    /**
     * Captures an authorized transaction.
     *
     * @param transactionId The transaction identifier
     * @param captureAmount The amount to capture (may be less than the authorized amount for partial captures)
     * @return The updated transaction
     * @throws ResourceNotFoundException if the transaction is not found
     * @throws ValidationException if the transaction cannot be captured or the amount is invalid
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentTransaction captureTransaction(UUID transactionId, BigDecimal captureAmount) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException;

    /**
     * Refunds a captured transaction.
     *
     * @param transactionId The transaction identifier
     * @param refundAmount The amount to refund (may be less than the captured amount for partial refunds)
     * @return The updated transaction
     * @throws ResourceNotFoundException if the transaction is not found
     * @throws ValidationException if the transaction cannot be refunded or the amount is invalid
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentTransaction refundTransaction(UUID transactionId, BigDecimal refundAmount) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException;

    /**
     * Voids an authorized transaction.
     *
     * @param transactionId The transaction identifier
     * @return The updated transaction
     * @throws ResourceNotFoundException if the transaction is not found
     * @throws ValidationException if the transaction cannot be voided
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentTransaction voidTransaction(UUID transactionId) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException;

    /**
     * Finds transactions that require processing (e.g., stuck in PROCESSING state).
     *
     * @param cutoffTime The cutoff time for identifying stuck transactions
     * @param filterParams Additional filtering parameters
     * @return List of transactions that require processing
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findTransactionsRequiringProcessing(Instant cutoffTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Counts transactions by status for an organization.
     *
     * @param organizationId The organization identifier
     * @return Map of status to count
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    java.util.Map<PaymentStatus, Long> countByStatusForOrganization(UUID organizationId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Calculates the total amount of transactions by status for an organization.
     *
     * @param organizationId The organization identifier
     * @param status The transaction status
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return The total amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    BigDecimal calculateTotalAmountByStatusForOrganization(UUID organizationId, PaymentStatus status, String currency) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions with full-text search across reference numbers and descriptions.
     *
     * @param searchText The text to search for
     * @param filterParams Additional filtering parameters
     * @return List of transactions matching the search text
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByFullTextSearch(String searchText, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions for a specific time period grouped by day.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of date to transaction count
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    java.util.Map<java.time.LocalDate, Long> getTransactionCountByDay(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions for a specific time period grouped by payment type.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of payment type to transaction count
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    java.util.Map<PaymentType, Long> getTransactionCountByPaymentType(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions for a specific time period grouped by status.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of status to transaction count
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    java.util.Map<PaymentStatus, Long> getTransactionCountByStatus(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions for all accounts of an organization.
     * This is a special case of the organization query that uses the "_all" placeholder.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of transactions for all accounts of the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findByOrganizationIdForAllAccounts(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds transactions for all organizations.
     * This is a special case for administrative access that uses the "_all" placeholder.
     *
     * @param filterParams Additional filtering parameters
     * @return List of transactions across all organizations
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentTransaction> findForAllOrganizations(PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;
}