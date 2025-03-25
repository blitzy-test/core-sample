package io.briklabs.sample.payments.data.dao;

import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Interface for payment transaction data access operations providing specialized query methods
 * for transaction filtering, sorting, and status management.
 * 
 * This DAO handles all database interactions for payment transactions, supporting creation,
 * retrieval, updates, and complex querying capabilities. It enables the payment service layer
 * to perform operations like transaction listing, status updates, and financial reporting
 * without direct SQL knowledge.
 */
public interface PaymentTransactionDAO extends PaymentDAO<PaymentTransaction, UUID> {
    
    /**
     * Finds transactions by their current status.
     * 
     * @param status The status to filter by
     * @return A list of transactions with the specified status
     */
    List<PaymentTransaction> findByStatus(PaymentStatus status);
    
    /**
     * Finds transactions by multiple status values.
     * 
     * @param statuses The set of statuses to filter by
     * @return A list of transactions with any of the specified statuses
     */
    List<PaymentTransaction> findByStatusIn(Set<PaymentStatus> statuses);
    
    /**
     * Finds transactions by status and date range.
     * 
     * @param status The status to filter by
     * @param dateRange The date range to filter by
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByStatusAndDate(PaymentStatus status, DateRangeFilter dateRange);
    
    /**
     * Finds transactions by status and date range for a specific organization.
     * 
     * @param organizationId The organization ID
     * @param status The status to filter by
     * @param dateRange The date range to filter by
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByOrganizationStatusAndDate(UUID organizationId, PaymentStatus status, DateRangeFilter dateRange);
    
    /**
     * Finds transactions by status and date range for a specific account.
     * 
     * @param accountId The account ID
     * @param status The status to filter by
     * @param dateRange The date range to filter by
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByAccountStatusAndDate(UUID accountId, PaymentStatus status, DateRangeFilter dateRange);
    
    /**
     * Finds transactions by amount range.
     * 
     * @param amountRange The amount range to filter by
     * @return A list of transactions with amounts in the specified range
     */
    List<PaymentTransaction> findByAmount(AmountRangeFilter amountRange);
    
    /**
     * Finds transactions by amount range and currency.
     * 
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @param currency The currency code
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, String currency);
    
    /**
     * Finds transactions by merchant ID.
     * 
     * @param merchantId The merchant identifier
     * @return A list of transactions for the specified merchant
     */
    List<PaymentTransaction> findByMerchant(String merchantId);
    
    /**
     * Finds transactions by merchant ID and status.
     * 
     * @param merchantId The merchant identifier
     * @param status The status to filter by
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByMerchantAndStatus(String merchantId, PaymentStatus status);
    
    /**
     * Finds transactions by payment type.
     * 
     * @param paymentType The payment type
     * @return A list of transactions with the specified payment type
     */
    List<PaymentTransaction> findByPaymentType(String paymentType);
    
    /**
     * Finds transactions by transaction reference.
     * 
     * @param reference The transaction reference
     * @return A list of transactions with the specified reference
     */
    List<PaymentTransaction> findByReference(String reference);
    
    /**
     * Finds transactions by creation date range.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A list of transactions created within the date range
     */
    List<PaymentTransaction> findByCreationDate(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds transactions by update date range.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A list of transactions updated within the date range
     */
    List<PaymentTransaction> findByUpdateDate(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Updates the status of a transaction.
     * 
     * @param transactionId The transaction ID
     * @param newStatus The new status
     * @return The updated transaction
     * @throws io.briklabs.sample.payments.data.exception.PaymentEntityNotFoundException if the transaction does not exist
     * @throws io.briklabs.sample.payments.data.exception.PaymentInvalidStateException if the status transition is invalid
     */
    PaymentTransaction updateStatus(UUID transactionId, PaymentStatus newStatus);
    
    /**
     * Finds transactions using complex filter parameters.
     * 
     * @param filterParams The filter parameters
     * @return A list of transactions matching the filter criteria
     */
    List<PaymentTransaction> findByFilterParams(PaymentFilterParams filterParams);
    
    /**
     * Counts transactions matching the filter parameters.
     * 
     * @param filterParams The filter parameters
     * @return The count of matching transactions
     */
    long countByFilterParams(PaymentFilterParams filterParams);
    
    /**
     * Finds transactions for a specific organization with pagination.
     * 
     * @param organizationId The organization ID
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByOrganizationWithPagination(UUID organizationId, int limit, int offset);
    
    /**
     * Finds transactions for a specific account with pagination.
     * 
     * @param accountId The account ID
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByAccountWithPagination(UUID accountId, int limit, int offset);
    
    /**
     * Finds transactions for a specific organization and account with pagination.
     * 
     * @param organizationId The organization ID
     * @param accountId The account ID
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByOrganizationAndAccountWithPagination(UUID organizationId, UUID accountId, int limit, int offset);
    
    /**
     * Searches for transactions by text in reference or description.
     * 
     * @param searchTerm The search term
     * @return A list of transactions matching the search term
     */
    List<PaymentTransaction> searchByText(String searchTerm);
    
    /**
     * Finds transactions that require settlement (typically AUTHORIZED transactions).
     * 
     * @param cutoffTime The cutoff time for settlement eligibility
     * @return A list of transactions eligible for settlement
     */
    List<PaymentTransaction> findTransactionsForSettlement(LocalDateTime cutoffTime);
    
    /**
     * Finds transactions that have been authorized but not captured within a time period.
     * 
     * @param expirationTime The time threshold for expiration
     * @return A list of transactions with expiring authorizations
     */
    List<PaymentTransaction> findExpiringAuthorizations(LocalDateTime expirationTime);
    
    /**
     * Aggregates transaction amounts by status.
     * 
     * @param organizationId The organization ID (optional)
     * @param accountId The account ID (optional)
     * @param startDate The start date (optional)
     * @param endDate The end date (optional)
     * @return A map of status to total amount
     */
    java.util.Map<PaymentStatus, BigDecimal> aggregateAmountsByStatus(UUID organizationId, UUID accountId, 
                                                                      LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Aggregates transaction counts by status.
     * 
     * @param organizationId The organization ID (optional)
     * @param accountId The account ID (optional)
     * @param startDate The start date (optional)
     * @param endDate The end date (optional)
     * @return A map of status to count
     */
    java.util.Map<PaymentStatus, Long> aggregateCountsByStatus(UUID organizationId, UUID accountId, 
                                                              LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds transactions that need reconciliation (typically CAPTURED transactions without matching settlement).
     * 
     * @param cutoffTime The cutoff time for reconciliation
     * @return A list of transactions needing reconciliation
     */
    List<PaymentTransaction> findTransactionsForReconciliation(LocalDateTime cutoffTime);
    
    /**
     * Finds transactions by multiple filter criteria with sorting and pagination.
     * 
     * @param organizationId The organization ID (optional)
     * @param accountId The account ID (optional)
     * @param statuses The set of statuses to filter by (optional)
     * @param startDate The start date (optional)
     * @param endDate The end date (optional)
     * @param minAmount The minimum amount (optional)
     * @param maxAmount The maximum amount (optional)
     * @param currency The currency code (optional)
     * @param merchantId The merchant ID (optional)
     * @param paymentType The payment type (optional)
     * @param sortField The field to sort by (optional)
     * @param sortDirection The sort direction (ASC or DESC)
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated and sorted list of transactions matching the criteria
     */
    List<PaymentTransaction> findByMultipleCriteria(UUID organizationId, UUID accountId, Set<PaymentStatus> statuses,
                                                  LocalDateTime startDate, LocalDateTime endDate,
                                                  BigDecimal minAmount, BigDecimal maxAmount, String currency,
                                                  String merchantId, String paymentType,
                                                  String sortField, String sortDirection,
                                                  int limit, int offset);
    
    /**
     * Counts transactions by multiple filter criteria.
     * 
     * @param organizationId The organization ID (optional)
     * @param accountId The account ID (optional)
     * @param statuses The set of statuses to filter by (optional)
     * @param startDate The start date (optional)
     * @param endDate The end date (optional)
     * @param minAmount The minimum amount (optional)
     * @param maxAmount The maximum amount (optional)
     * @param currency The currency code (optional)
     * @param merchantId The merchant ID (optional)
     * @param paymentType The payment type (optional)
     * @return The count of transactions matching the criteria
     */
    long countByMultipleCriteria(UUID organizationId, UUID accountId, Set<PaymentStatus> statuses,
                               LocalDateTime startDate, LocalDateTime endDate,
                               BigDecimal minAmount, BigDecimal maxAmount, String currency,
                               String merchantId, String paymentType);
    
    /**
     * Finds transactions that can be transitioned to the specified status.
     * 
     * @param targetStatus The target status
     * @return A list of transactions that can transition to the target status
     */
    List<PaymentTransaction> findTransactionsEligibleForStatus(PaymentStatus targetStatus);
    
    /**
     * Finds transactions by organization ID and status with pagination.
     * 
     * @param organizationId The organization ID
     * @param status The status to filter by
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByOrganizationAndStatusWithPagination(UUID organizationId, PaymentStatus status, 
                                                                     int limit, int offset);
    
    /**
     * Finds transactions by account ID and status with pagination.
     * 
     * @param accountId The account ID
     * @param status The status to filter by
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByAccountAndStatusWithPagination(UUID accountId, PaymentStatus status, 
                                                               int limit, int offset);
    
    /**
     * Finds transactions by organization ID, account ID, and status with pagination.
     * 
     * @param organizationId The organization ID
     * @param accountId The account ID
     * @param status The status to filter by
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByOrganizationAccountAndStatusWithPagination(UUID organizationId, UUID accountId, 
                                                                           PaymentStatus status, int limit, int offset);
    
    /**
     * Finds transactions by merchant ID with pagination.
     * 
     * @param merchantId The merchant ID
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByMerchantWithPagination(String merchantId, int limit, int offset);
    
    /**
     * Finds transactions by merchant ID and status with pagination.
     * 
     * @param merchantId The merchant ID
     * @param status The status to filter by
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return A paginated list of transactions
     */
    List<PaymentTransaction> findByMerchantAndStatusWithPagination(String merchantId, PaymentStatus status, 
                                                                int limit, int offset);
    
    /**
     * Finds the latest transaction for an account.
     * 
     * @param accountId The account ID
     * @return An optional containing the latest transaction, or empty if none exists
     */
    Optional<PaymentTransaction> findLatestByAccount(UUID accountId);
    
    /**
     * Finds the latest transaction for a merchant.
     * 
     * @param merchantId The merchant ID
     * @return An optional containing the latest transaction, or empty if none exists
     */
    Optional<PaymentTransaction> findLatestByMerchant(String merchantId);
    
    /**
     * Finds transactions with a specific status created before a cutoff time.
     * 
     * @param status The status to filter by
     * @param cutoffTime The cutoff time
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByStatusCreatedBefore(PaymentStatus status, LocalDateTime cutoffTime);
    
    /**
     * Finds transactions with a specific status updated before a cutoff time.
     * 
     * @param status The status to filter by
     * @param cutoffTime The cutoff time
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByStatusUpdatedBefore(PaymentStatus status, LocalDateTime cutoffTime);
    
    /**
     * Finds transactions for a specific organization created within a date range.
     * 
     * @param organizationId The organization ID
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByOrganizationAndDateRange(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds transactions for a specific account created within a date range.
     * 
     * @param accountId The account ID
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByAccountAndDateRange(UUID accountId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds transactions for a specific merchant created within a date range.
     * 
     * @param merchantId The merchant ID
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByMerchantAndDateRange(String merchantId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds transactions by description containing a search term.
     * 
     * @param searchTerm The search term
     * @return A list of transactions with descriptions containing the search term
     */
    List<PaymentTransaction> findByDescriptionContaining(String searchTerm);
    
    /**
     * Finds transactions by reference containing a search term.
     * 
     * @param searchTerm The search term
     * @return A list of transactions with references containing the search term
     */
    List<PaymentTransaction> findByReferenceContaining(String searchTerm);
    
    /**
     * Finds transactions by organization ID and payment type.
     * 
     * @param organizationId The organization ID
     * @param paymentType The payment type
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByOrganizationAndPaymentType(UUID organizationId, String paymentType);
    
    /**
     * Finds transactions by account ID and payment type.
     * 
     * @param accountId The account ID
     * @param paymentType The payment type
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByAccountAndPaymentType(UUID accountId, String paymentType);
    
    /**
     * Finds transactions by merchant ID and payment type.
     * 
     * @param merchantId The merchant ID
     * @param paymentType The payment type
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByMerchantAndPaymentType(String merchantId, String paymentType);
    
    /**
     * Finds transactions by currency.
     * 
     * @param currency The currency code
     * @return A list of transactions with the specified currency
     */
    List<PaymentTransaction> findByCurrency(String currency);
    
    /**
     * Finds transactions by currency and status.
     * 
     * @param currency The currency code
     * @param status The status to filter by
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByCurrencyAndStatus(String currency, PaymentStatus status);
    
    /**
     * Finds transactions by organization ID, currency, and status.
     * 
     * @param organizationId The organization ID
     * @param currency The currency code
     * @param status The status to filter by
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByOrganizationCurrencyAndStatus(UUID organizationId, String currency, PaymentStatus status);
    
    /**
     * Finds transactions by account ID, currency, and status.
     * 
     * @param accountId The account ID
     * @param currency The currency code
     * @param status The status to filter by
     * @return A list of transactions matching the criteria
     */
    List<PaymentTransaction> findByAccountCurrencyAndStatus(UUID accountId, String currency, PaymentStatus status);
}