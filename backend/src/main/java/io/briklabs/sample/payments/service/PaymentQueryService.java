package io.briklabs.sample.payments.service;

import java.util.List;
import java.util.UUID;

import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaginationParams;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.SortCriteria;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentTransaction;

/**
 * Service interface for complex payment data querying operations.
 * <p>
 * This service provides methods for advanced query capabilities including filtering,
 * sorting, and pagination of payment transactions. It supports complex filtering scenarios
 * with multiple criteria, dynamic sorting, and efficient pagination for large result sets.
 * </p>
 * <p>
 * The interface is designed to support the requirements for payment transaction listing,
 * reporting, and data analysis while abstracting the underlying data access implementation.
 * </p>
 */
public interface PaymentQueryService {

    /**
     * Retrieves payment transactions using a comprehensive filter parameter object.
     * <p>
     * This method provides a unified approach to querying payment transactions with
     * support for multiple filter criteria, sorting options, and pagination parameters.
     * </p>
     *
     * @param filterParams The filter parameters containing all query criteria
     * @return List of payment transactions matching the filter criteria
     */
    List<PaymentTransaction> findTransactions(PaymentFilterParams filterParams);
    
    /**
     * Retrieves payment transactions for a specific organization.
     * <p>
     * This method filters transactions by organization ID with optional additional
     * filter criteria, sorting, and pagination.
     * </p>
     *
     * @param organizationId The organization ID to filter by
     * @param filterParams Additional filter parameters (optional)
     * @return List of payment transactions for the specified organization
     */
    List<PaymentTransaction> findTransactionsByOrganization(UUID organizationId, PaymentFilterParams filterParams);
    
    /**
     * Retrieves payment transactions for a specific account within an organization.
     * <p>
     * This method filters transactions by organization ID and account ID with optional
     * additional filter criteria, sorting, and pagination.
     * </p>
     *
     * @param organizationId The organization ID to filter by
     * @param accountId The account ID to filter by
     * @param filterParams Additional filter parameters (optional)
     * @return List of payment transactions for the specified account
     */
    List<PaymentTransaction> findTransactionsByAccount(UUID organizationId, UUID accountId, PaymentFilterParams filterParams);
    
    /**
     * Retrieves payment transactions by status.
     * <p>
     * This method filters transactions by status with optional additional filter criteria,
     * sorting, and pagination.
     * </p>
     *
     * @param statusFilter The status filter criteria
     * @param filterParams Additional filter parameters (optional)
     * @return List of payment transactions matching the status criteria
     */
    List<PaymentTransaction> findTransactionsByStatus(StatusFilter statusFilter, PaymentFilterParams filterParams);
    
    /**
     * Retrieves payment transactions within a date range.
     * <p>
     * This method filters transactions by creation date range with optional additional
     * filter criteria, sorting, and pagination.
     * </p>
     *
     * @param dateRange The date range filter criteria
     * @param filterParams Additional filter parameters (optional)
     * @return List of payment transactions within the specified date range
     */
    List<PaymentTransaction> findTransactionsByDateRange(DateRangeFilter dateRange, PaymentFilterParams filterParams);
    
    /**
     * Retrieves payment transactions within an amount range.
     * <p>
     * This method filters transactions by amount range with optional additional filter
     * criteria, sorting, and pagination.
     * </p>
     *
     * @param amountRange The amount range filter criteria
     * @param filterParams Additional filter parameters (optional)
     * @return List of payment transactions within the specified amount range
     */
    List<PaymentTransaction> findTransactionsByAmountRange(AmountRangeFilter amountRange, PaymentFilterParams filterParams);
    
    /**
     * Retrieves payment transactions for a specific merchant.
     * <p>
     * This method filters transactions by merchant ID with optional additional filter
     * criteria, sorting, and pagination.
     * </p>
     *
     * @param merchantId The merchant ID to filter by
     * @param filterParams Additional filter parameters (optional)
     * @return List of payment transactions for the specified merchant
     */
    List<PaymentTransaction> findTransactionsByMerchant(String merchantId, PaymentFilterParams filterParams);
    
    /**
     * Retrieves payment transactions with a specific payment type.
     * <p>
     * This method filters transactions by payment type with optional additional filter
     * criteria, sorting, and pagination.
     * </p>
     *
     * @param paymentType The payment type to filter by
     * @param filterParams Additional filter parameters (optional)
     * @return List of payment transactions with the specified payment type
     */
    List<PaymentTransaction> findTransactionsByPaymentType(String paymentType, PaymentFilterParams filterParams);
    
    /**
     * Counts the total number of payment transactions matching the filter criteria.
     * <p>
     * This method provides an efficient way to get the total count without retrieving
     * the actual transaction data, useful for pagination and reporting.
     * </p>
     *
     * @param filterParams The filter parameters containing query criteria
     * @return The total count of matching transactions
     */
    long countTransactions(PaymentFilterParams filterParams);
    
    /**
     * Applies sorting to a payment transaction query.
     * <p>
     * This method provides a way to sort payment transactions by various criteria
     * with support for multiple sort fields and directions.
     * </p>
     *
     * @param transactions The list of transactions to sort
     * @param sortCriteria The sorting criteria to apply
     * @return The sorted list of transactions
     */
    List<PaymentTransaction> sortTransactions(List<PaymentTransaction> transactions, SortCriteria sortCriteria);
    
    /**
     * Applies pagination to a payment transaction query result.
     * <p>
     * This method provides a way to paginate large result sets with support for
     * offset-based pagination.
     * </p>
     *
     * @param transactions The list of transactions to paginate
     * @param paginationParams The pagination parameters to apply
     * @return The paginated list of transactions
     */
    List<PaymentTransaction> paginateTransactions(List<PaymentTransaction> transactions, PaginationParams paginationParams);
    
    /**
     * Validates and normalizes query parameters for payment transaction queries.
     * <p>
     * This method ensures that filter parameters are valid and properly formatted
     * before executing queries, preventing invalid query conditions.
     * </p>
     *
     * @param filterParams The filter parameters to validate
     * @return The validated and normalized filter parameters
     * @throws IllegalArgumentException if the filter parameters are invalid
     */
    PaymentFilterParams validateQueryParameters(PaymentFilterParams filterParams);
}