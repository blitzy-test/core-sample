package io.briklabs.sample.payments.data.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.model.PaymentFee;

/**
 * Interface for payment fee data access operations supporting fee tracking, reporting, and analysis.
 * <p>
 * This DAO handles storage and retrieval of all fee information associated with payment transactions,
 * enabling financial reporting and reconciliation. It provides methods for fee aggregation, filtering
 * by fee type, and retrieval by transaction ID, supporting both individual fee management and bulk
 * reporting capabilities.
 * </p>
 */
public interface PaymentFeeDAO extends PaymentDAO<PaymentFee, UUID> {

    /**
     * Finds all fees associated with a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of fees associated with the transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentFee> findByTransactionId(UUID transactionId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds fees by fee type.
     *
     * @param feeType The fee type to filter by
     * @param filterParams Additional filtering parameters
     * @return List of fees of the specified type
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentFee> findByFeeType(String feeType, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds fees by organization ID.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentFee> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds fees by organization ID and account ID.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for the specified organization and account
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentFee> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, 
            PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException;

    /**
     * Calculates the total fee amount for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return The total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    BigDecimal calculateTotalFeeAmountForTransaction(UUID transactionId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Calculates the total fee amount by fee type for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return Map of fee type to total amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Map<String, BigDecimal> calculateFeeAmountByTypeForTransaction(UUID transactionId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Calculates the total fee amount for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return The total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    BigDecimal calculateTotalFeeAmountForOrganization(UUID organizationId, DateRangeFilter dateRange, String currency) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Calculates the total fee amount by fee type for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return Map of fee type to total amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Map<String, BigDecimal> calculateFeeAmountByTypeForOrganization(UUID organizationId, 
            DateRangeFilter dateRange, String currency) throws ConnectionException, QueryExecutionException;

    /**
     * Calculates the total fee amount by account for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return Map of account ID to total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Map<UUID, BigDecimal> calculateFeeAmountByAccountForOrganization(UUID organizationId, 
            DateRangeFilter dateRange, String currency) throws ConnectionException, QueryExecutionException;

    /**
     * Finds fees for a specific time period grouped by day.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of date to total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Map<java.time.LocalDate, BigDecimal> getFeeAmountByDay(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds fees for a specific time period grouped by fee type.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of fee type to total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Map<String, BigDecimal> getFeeAmountByType(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Creates a new fee associated with a transaction.
     *
     * @param transactionId The transaction identifier
     * @param fee The fee to create
     * @return The created fee with any database-generated values
     * @throws ValidationException if the fee fails validation
     * @throws ResourceNotFoundException if the associated transaction is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentFee createFeeForTransaction(UUID transactionId, PaymentFee fee) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException;

    /**
     * Creates multiple fees for a transaction in a batch operation.
     *
     * @param transactionId The transaction identifier
     * @param fees The list of fees to create
     * @return The list of created fees with any database-generated values
     * @throws ValidationException if any fee fails validation
     * @throws ResourceNotFoundException if the associated transaction is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    List<PaymentFee> batchCreateFeesForTransaction(UUID transactionId, List<PaymentFee> fees) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException;

    /**
     * Updates a fee associated with a transaction.
     *
     * @param fee The fee to update
     * @return The updated fee
     * @throws ValidationException if the fee fails validation
     * @throws ResourceNotFoundException if the fee or associated transaction is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    PaymentFee updateFee(PaymentFee fee) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException;

    /**
     * Deletes all fees associated with a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The number of fees deleted
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    int deleteAllFeesForTransaction(UUID transactionId) 
            throws ConnectionException, QueryExecutionException, TransactionException;

    /**
     * Finds fees with external reference matching the provided value.
     *
     * @param feeReference The external fee reference
     * @return List of fees with the matching reference
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentFee> findByFeeReference(String feeReference) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds fees for all accounts of an organization.
     * This is a special case of the organization query that uses the "_all" placeholder.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for all accounts of the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentFee> findByOrganizationIdForAllAccounts(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds fees for all organizations.
     * This is a special case for administrative access that uses the "_all" placeholder.
     *
     * @param filterParams Additional filtering parameters
     * @return List of fees across all organizations
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentFee> findForAllOrganizations(PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;
}