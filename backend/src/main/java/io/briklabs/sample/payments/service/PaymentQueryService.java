package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaginationParams;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.SortCriteria;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentTransaction;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * Service interface for complex payment data querying operations.
 * Provides methods for filtering, sorting, and paginating payment transactions
 * to support reporting and data analysis requirements.
 */
public interface PaymentQueryService {

    /**
     * Retrieves payment transactions based on comprehensive filter parameters.
     * Supports complex filtering by multiple criteria including date ranges,
     * amount ranges, status values, and merchant identifiers.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param filterParams Complete set of filter parameters for the query
     * @return List of payment transactions matching the filter criteria
     */
    List<PaymentTransaction> findTransactions(UUID organizationId, UUID accountId, PaymentFilterParams filterParams);

    /**
     * Retrieves payment transactions with pagination support.
     * Returns a subset of transactions based on the provided pagination parameters.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param filterParams Filter parameters for the query
     * @param paginationParams Pagination parameters (limit, offset)
     * @return Paginated list of payment transactions
     */
    List<PaymentTransaction> findTransactionsPaginated(UUID organizationId, UUID accountId, 
                                                      PaymentFilterParams filterParams,
                                                      PaginationParams paginationParams);

    /**
     * Counts the total number of transactions matching the provided filter criteria.
     * Used for pagination metadata and reporting.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param filterParams Filter parameters for the query
     * @return Total count of matching transactions
     */
    long countTransactions(UUID organizationId, UUID accountId, PaymentFilterParams filterParams);

    /**
     * Retrieves payment transactions filtered by date range.
     * Allows filtering transactions based on creation date, update date, or processing date.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param dateRangeFilter Date range filter parameters
     * @param paginationParams Optional pagination parameters
     * @return List of payment transactions within the specified date range
     */
    List<PaymentTransaction> findTransactionsByDateRange(UUID organizationId, UUID accountId,
                                                        DateRangeFilter dateRangeFilter,
                                                        Optional<PaginationParams> paginationParams);

    /**
     * Retrieves payment transactions filtered by amount range.
     * Supports filtering by transaction amount with optional currency conversion.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param amountRangeFilter Amount range filter parameters
     * @param paginationParams Optional pagination parameters
     * @return List of payment transactions within the specified amount range
     */
    List<PaymentTransaction> findTransactionsByAmountRange(UUID organizationId, UUID accountId,
                                                          AmountRangeFilter amountRangeFilter,
                                                          Optional<PaginationParams> paginationParams);

    /**
     * Retrieves payment transactions filtered by status.
     * Supports filtering by multiple status values or status groups.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param statusFilter Status filter parameters
     * @param paginationParams Optional pagination parameters
     * @return List of payment transactions with the specified status(es)
     */
    List<PaymentTransaction> findTransactionsByStatus(UUID organizationId, UUID accountId,
                                                     StatusFilter statusFilter,
                                                     Optional<PaginationParams> paginationParams);

    /**
     * Retrieves payment transactions for a specific merchant.
     * Filters transactions by merchant identifier with optional additional filtering.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param merchantId The merchant identifier to filter by
     * @param filterParams Additional filter parameters
     * @param paginationParams Optional pagination parameters
     * @return List of payment transactions for the specified merchant
     */
    List<PaymentTransaction> findTransactionsByMerchant(UUID organizationId, UUID accountId,
                                                       String merchantId,
                                                       Optional<PaymentFilterParams> filterParams,
                                                       Optional<PaginationParams> paginationParams);

    /**
     * Retrieves payment transactions sorted by specified criteria.
     * Supports sorting by multiple fields with direction control.
     *
     * @param organizationId The organization ID to filter transactions by
     * @param accountId The account ID to filter transactions by, or null for all accounts
     * @param filterParams Filter parameters for the query
     * @param sortCriteria Sorting criteria (fields and directions)
     * @param paginationParams Optional pagination parameters
     * @return Sorted list of payment transactions
     */
    List<PaymentTransaction> findTransactionsSorted(UUID organizationId, UUID accountId,
                                                   PaymentFilterParams filterParams,
                                                   List<SortCriteria> sortCriteria,
                                                   Optional<PaginationParams> paginationParams);

    /**
     * Validates query parameters for correctness and security.
     * Ensures that filter parameters meet validation rules and are safe to use.
     *
     * @param filterParams Filter parameters to validate
     * @return Validated and normalized filter parameters
     * @throws IllegalArgumentException if parameters fail validation
     */
    PaymentFilterParams validateAndNormalizeFilterParams(PaymentFilterParams filterParams);

    /**
     * Validates pagination parameters and applies defaults if needed.
     * Ensures that pagination parameters are within allowed ranges.
     *
     * @param paginationParams Pagination parameters to validate
     * @return Validated pagination parameters with defaults applied if needed
     */
    PaginationParams validateAndNormalizePaginationParams(PaginationParams paginationParams);

    /**
     * Validates sort criteria against allowed fields and applies defaults if needed.
     * Ensures that sort fields are valid and secure to use in queries.
     *
     * @param sortCriteria Sort criteria to validate
     * @return Validated sort criteria with defaults applied if needed
     * @throws IllegalArgumentException if sort fields are invalid
     */
    List<SortCriteria> validateAndNormalizeSortCriteria(List<SortCriteria> sortCriteria);
}