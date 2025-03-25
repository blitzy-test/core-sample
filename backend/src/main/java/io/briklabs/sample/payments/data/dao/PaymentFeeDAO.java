package io.briklabs.sample.payments.data.dao;

import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.model.PaymentFee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     */
    List<PaymentFee> findByTransactionId(UUID transactionId);
    
    /**
     * Finds fees by fee type.
     *
     * @param feeType The fee type to filter by
     * @param filterParams Additional filtering parameters
     * @return List of fees of the specified type
     */
    List<PaymentFee> findByFeeType(String feeType, PaymentFilterParams filterParams);
    
    /**
     * Finds fees by organization ID.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for the specified organization
     */
    List<PaymentFee> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams);
    
    /**
     * Finds fees by organization ID and account ID.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for the specified organization and account
     */
    List<PaymentFee> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, 
            PaymentFilterParams filterParams);
    
    /**
     * Calculates the total fee amount for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return The total fee amount
     */
    BigDecimal calculateTotalFeeAmountForTransaction(UUID transactionId);
    
    /**
     * Calculates the total fee amount by fee type for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return Map of fee type to total amount
     */
    Map<String, BigDecimal> calculateFeeAmountByTypeForTransaction(UUID transactionId);
    
    /**
     * Calculates the total fee amount for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return The total fee amount
     */
    BigDecimal calculateTotalFeeAmountForOrganization(UUID organizationId, DateRangeFilter dateRange, 
            String currency);
    
    /**
     * Calculates the total fee amount by fee type for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return Map of fee type to total amount
     */
    Map<String, BigDecimal> calculateFeeAmountByTypeForOrganization(UUID organizationId, 
            DateRangeFilter dateRange, String currency);
    
    /**
     * Calculates the total fee amount by account for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return Map of account ID to total fee amount
     */
    Map<UUID, BigDecimal> calculateFeeAmountByAccountForOrganization(UUID organizationId, 
            DateRangeFilter dateRange, String currency);
    
    /**
     * Finds fees for a specific time period grouped by day.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of date to total fee amount
     */
    Map<LocalDate, BigDecimal> getFeeAmountByDay(UUID organizationId, DateRangeFilter dateRange);
    
    /**
     * Finds fees for a specific time period grouped by fee type.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of fee type to total fee amount
     */
    Map<String, BigDecimal> getFeeAmountByType(UUID organizationId, DateRangeFilter dateRange);
    
    /**
     * Creates a new fee associated with a transaction.
     *
     * @param transactionId The transaction identifier
     * @param fee The fee to create
     * @return The created fee with any database-generated values
     */
    PaymentFee createFeeForTransaction(UUID transactionId, PaymentFee fee);
    
    /**
     * Creates multiple fees for a transaction in a batch operation.
     *
     * @param transactionId The transaction identifier
     * @param fees The list of fees to create
     * @return The list of created fees with any database-generated values
     */
    List<PaymentFee> batchCreateFeesForTransaction(UUID transactionId, List<PaymentFee> fees);
    
    /**
     * Updates a fee associated with a transaction.
     *
     * @param fee The fee to update
     * @return The updated fee
     */
    PaymentFee updateFee(PaymentFee fee);
    
    /**
     * Deletes all fees associated with a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The number of fees deleted
     */
    int deleteAllFeesForTransaction(UUID transactionId);
    
    /**
     * Finds fees with external reference matching the provided value.
     *
     * @param feeReference The external fee reference
     * @return List of fees with the matching reference
     */
    List<PaymentFee> findByFeeReference(String feeReference);
    
    /**
     * Finds fees for all accounts of an organization.
     * This is a special case of the organization query that uses the "_all" placeholder.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for all accounts of the specified organization
     */
    List<PaymentFee> findByOrganizationIdForAllAccounts(UUID organizationId, PaymentFilterParams filterParams);
    
    /**
     * Finds fees for all organizations.
     * This is a special case for administrative access that uses the "_all" placeholder.
     *
     * @param filterParams Additional filtering parameters
     * @return List of fees across all organizations
     */
    List<PaymentFee> findForAllOrganizations(PaymentFilterParams filterParams);
}