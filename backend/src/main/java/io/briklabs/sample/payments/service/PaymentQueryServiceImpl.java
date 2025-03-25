package io.briklabs.sample.payments.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaginationParams;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.SortCriteria;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentTransaction;

/**
 * Implementation of the PaymentQueryService interface that provides advanced payment data querying capabilities.
 * <p>
 * This class handles complex filter construction, query execution, sorting logic, and pagination,
 * working closely with the data access layer to retrieve optimized result sets. It's critical for
 * efficient payment data retrieval and reporting.
 * </p>
 */
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentQueryServiceImpl.class);
    
    private final PaymentTransactionDAO transactionDAO;
    
    /**
     * Default page size for pagination when not specified
     */
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * Maximum allowed page size to prevent excessive resource usage
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Constructs a new PaymentQueryServiceImpl with the specified DAO factory.
     * 
     * @param daoFactory The factory for creating data access objects
     */
    public PaymentQueryServiceImpl(PaymentDAOFactory daoFactory) {
        this.transactionDAO = daoFactory.getPaymentTransactionDAO();
        logger.debug("Initialized PaymentQueryServiceImpl with DAO factory");
    }

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
    @Override
    public List<PaymentTransaction> findTransactions(PaymentFilterParams filterParams) {
        try {
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(filterParams);
            
            logger.debug("Executing payment transaction query with filter parameters: {}", validatedParams);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.query(validatedParams);
            
            // Apply additional sorting if needed (for complex sort criteria that can't be handled at the DB level)
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions matching filter criteria", transactions.size());
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query", e);
            // Translate data access exceptions to service-level exceptions
            throw new PaymentQueryException("Failed to query payment transactions: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query", e);
            throw new PaymentQueryException("Unexpected error querying payment transactions: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> findTransactionsByOrganization(UUID organizationId, PaymentFilterParams filterParams) {
        try {
            // Create a new filter params object if none provided
            PaymentFilterParams params = (filterParams != null) ? filterParams : new PaymentFilterParams();
            
            // Set the organization ID in the filter parameters
            params.setOrganizationId(organizationId);
            
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(params);
            
            logger.debug("Executing payment transaction query for organization {}", organizationId);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.findByOrganizationId(organizationId, validatedParams);
            
            // Apply additional sorting if needed
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions for organization {}", transactions.size(), organizationId);
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query for organization {}", organizationId, e);
            throw new PaymentQueryException("Failed to query payment transactions for organization: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query for organization {}", organizationId, e);
            throw new PaymentQueryException("Unexpected error querying payment transactions for organization: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> findTransactionsByAccount(UUID organizationId, UUID accountId, PaymentFilterParams filterParams) {
        try {
            // Create a new filter params object if none provided
            PaymentFilterParams params = (filterParams != null) ? filterParams : new PaymentFilterParams();
            
            // Set the organization and account IDs in the filter parameters
            params.setOrganizationId(organizationId);
            params.setAccountId(accountId);
            
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(params);
            
            logger.debug("Executing payment transaction query for organization {} and account {}", organizationId, accountId);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.findByOrganizationIdAndAccountId(
                    organizationId, accountId, validatedParams);
            
            // Apply additional sorting if needed
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions for organization {} and account {}", 
                    transactions.size(), organizationId, accountId);
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query for organization {} and account {}", 
                    organizationId, accountId, e);
            throw new PaymentQueryException("Failed to query payment transactions for account: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query for organization {} and account {}", 
                    organizationId, accountId, e);
            throw new PaymentQueryException("Unexpected error querying payment transactions for account: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> findTransactionsByStatus(StatusFilter statusFilter, PaymentFilterParams filterParams) {
        try {
            // Create a new filter params object if none provided
            PaymentFilterParams params = (filterParams != null) ? filterParams : new PaymentFilterParams();
            
            // Set the status filter in the filter parameters
            params.setStatusFilter(statusFilter);
            
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(params);
            
            logger.debug("Executing payment transaction query for status filter: {}", statusFilter);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.findByStatusIn(statusFilter, validatedParams);
            
            // Apply additional sorting if needed
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions matching status filter", transactions.size());
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query for status filter: {}", statusFilter, e);
            throw new PaymentQueryException("Failed to query payment transactions by status: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query for status filter: {}", statusFilter, e);
            throw new PaymentQueryException("Unexpected error querying payment transactions by status: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> findTransactionsByDateRange(DateRangeFilter dateRange, PaymentFilterParams filterParams) {
        try {
            // Create a new filter params object if none provided
            PaymentFilterParams params = (filterParams != null) ? filterParams : new PaymentFilterParams();
            
            // Set the date range filter in the filter parameters
            params.setDateRange(dateRange);
            
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(params);
            
            logger.debug("Executing payment transaction query for date range: {}", dateRange);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.findByCreatedAtBetween(dateRange, validatedParams);
            
            // Apply additional sorting if needed
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions within date range", transactions.size());
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query for date range: {}", dateRange, e);
            throw new PaymentQueryException("Failed to query payment transactions by date range: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query for date range: {}", dateRange, e);
            throw new PaymentQueryException("Unexpected error querying payment transactions by date range: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> findTransactionsByAmountRange(AmountRangeFilter amountRange, PaymentFilterParams filterParams) {
        try {
            // Create a new filter params object if none provided
            PaymentFilterParams params = (filterParams != null) ? filterParams : new PaymentFilterParams();
            
            // Set the amount range filter in the filter parameters
            params.setAmountRange(amountRange);
            
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(params);
            
            logger.debug("Executing payment transaction query for amount range: {}", amountRange);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.findByAmountBetween(amountRange, validatedParams);
            
            // Apply additional sorting if needed
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions within amount range", transactions.size());
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query for amount range: {}", amountRange, e);
            throw new PaymentQueryException("Failed to query payment transactions by amount range: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query for amount range: {}", amountRange, e);
            throw new PaymentQueryException("Unexpected error querying payment transactions by amount range: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> findTransactionsByMerchant(String merchantId, PaymentFilterParams filterParams) {
        try {
            // Create a new filter params object if none provided
            PaymentFilterParams params = (filterParams != null) ? filterParams : new PaymentFilterParams();
            
            // Set the merchant ID in the filter parameters
            params.setMerchantId(merchantId);
            
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(params);
            
            logger.debug("Executing payment transaction query for merchant: {}", merchantId);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.findByMerchantId(merchantId, validatedParams);
            
            // Apply additional sorting if needed
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions for merchant {}", transactions.size(), merchantId);
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query for merchant: {}", merchantId, e);
            throw new PaymentQueryException("Failed to query payment transactions by merchant: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query for merchant: {}", merchantId, e);
            throw new PaymentQueryException("Unexpected error querying payment transactions by merchant: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> findTransactionsByPaymentType(String paymentType, PaymentFilterParams filterParams) {
        try {
            // Create a new filter params object if none provided
            PaymentFilterParams params = (filterParams != null) ? filterParams : new PaymentFilterParams();
            
            // Set the payment type in the filter parameters
            params.setPaymentType(paymentType);
            
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(params);
            
            logger.debug("Executing payment transaction query for payment type: {}", paymentType);
            
            // Execute the query through the DAO
            List<PaymentTransaction> transactions = transactionDAO.query(validatedParams);
            
            // Apply additional sorting if needed
            if (validatedParams.getSortCriteria() != null && !validatedParams.getSortCriteria().isEmpty()) {
                transactions = sortTransactions(transactions, validatedParams.getSortCriteria().get(0));
            }
            
            // Apply pagination if needed
            if (validatedParams.getPagination() != null) {
                transactions = paginateTransactions(transactions, validatedParams.getPagination());
            }
            
            logger.debug("Found {} payment transactions with payment type {}", transactions.size(), paymentType);
            return transactions;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error executing payment transaction query for payment type: {}", paymentType, e);
            throw new PaymentQueryException("Failed to query payment transactions by payment type: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in payment transaction query for payment type: {}", paymentType, e);
            throw new PaymentQueryException("Unexpected error querying payment transactions by payment type: " + e.getMessage(), e);
        }
    }

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
    @Override
    public long countTransactions(PaymentFilterParams filterParams) {
        try {
            // Validate and normalize filter parameters
            PaymentFilterParams validatedParams = validateQueryParameters(filterParams);
            
            logger.debug("Counting payment transactions with filter parameters: {}", validatedParams);
            
            // Execute the count query through the DAO
            long count = transactionDAO.count(validatedParams);
            
            logger.debug("Found {} payment transactions matching filter criteria", count);
            return count;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Error counting payment transactions", e);
            throw new PaymentQueryException("Failed to count payment transactions: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error counting payment transactions", e);
            throw new PaymentQueryException("Unexpected error counting payment transactions: " + e.getMessage(), e);
        }
    }

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
    @Override
    public List<PaymentTransaction> sortTransactions(List<PaymentTransaction> transactions, SortCriteria sortCriteria) {
        if (transactions == null || transactions.isEmpty() || sortCriteria == null) {
            return transactions;
        }
        
        logger.debug("Sorting payment transactions by criteria: {}", sortCriteria);
        
        // Create a comparator based on the sort criteria
        Comparator<PaymentTransaction> comparator = createComparator(sortCriteria);
        
        // Sort the transactions using the comparator
        List<PaymentTransaction> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort(comparator);
        
        return sortedTransactions;
    }

    /**
     * Creates a comparator for sorting payment transactions based on the specified criteria.
     *
     * @param sortCriteria The sorting criteria
     * @return A comparator for sorting payment transactions
     */
    private Comparator<PaymentTransaction> createComparator(SortCriteria sortCriteria) {
        String column = sortCriteria.getColumn();
        boolean ascending = SortCriteria.Direction.ASC.equals(sortCriteria.getDirection());
        
        Comparator<PaymentTransaction> comparator;
        
        // Create a comparator based on the sort column
        switch (column.toLowerCase()) {
            case "transaction_id":
            case "transactionid":
                comparator = Comparator.comparing(PaymentTransaction::getTransactionId);
                break;
            case "created_at":
            case "createdat":
                comparator = Comparator.comparing(PaymentTransaction::getCreatedAt);
                break;
            case "updated_at":
            case "updatedat":
                comparator = Comparator.comparing(PaymentTransaction::getUpdatedAt);
                break;
            case "amount":
                comparator = Comparator.comparing(PaymentTransaction::getAmount);
                break;
            case "status":
                comparator = Comparator.comparing(t -> t.getStatus().name());
                break;
            case "merchant_id":
            case "merchantid":
                comparator = Comparator.comparing(PaymentTransaction::getMerchantId, 
                        Comparator.nullsLast(String::compareTo));
                break;
            case "payment_type":
            case "paymenttype":
                comparator = Comparator.comparing(PaymentTransaction::getPaymentType, 
                        Comparator.nullsLast(String::compareTo));
                break;
            case "currency":
                comparator = Comparator.comparing(PaymentTransaction::getCurrency, 
                        Comparator.nullsLast(String::compareTo));
                break;
            default:
                // Default to sorting by created_at
                logger.warn("Unknown sort column: {}. Defaulting to created_at", column);
                comparator = Comparator.comparing(PaymentTransaction::getCreatedAt);
                break;
        }
        
        // Reverse the comparator if descending order is requested
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return comparator;
    }

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
    @Override
    public List<PaymentTransaction> paginateTransactions(List<PaymentTransaction> transactions, PaginationParams paginationParams) {
        if (transactions == null || transactions.isEmpty() || paginationParams == null) {
            return transactions;
        }
        
        int offset = paginationParams.getOffset();
        int limit = paginationParams.getLimit();
        
        logger.debug("Applying pagination with offset {} and limit {} to {} transactions", 
                offset, limit, transactions.size());
        
        // Validate pagination parameters
        if (offset < 0) {
            offset = 0;
        }
        
        if (limit <= 0) {
            limit = DEFAULT_PAGE_SIZE;
        } else if (limit > MAX_PAGE_SIZE) {
            limit = MAX_PAGE_SIZE;
        }
        
        // Apply pagination
        int fromIndex = Math.min(offset, transactions.size());
        int toIndex = Math.min(fromIndex + limit, transactions.size());
        
        return transactions.subList(fromIndex, toIndex);
    }

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
    @Override
    public PaymentFilterParams validateQueryParameters(PaymentFilterParams filterParams) {
        // If filter params is null, create a new empty one
        if (filterParams == null) {
            return new PaymentFilterParams();
        }
        
        // Create a copy of the filter params to avoid modifying the original
        PaymentFilterParams validatedParams = new PaymentFilterParams();
        
        // Copy and validate organization ID
        validatedParams.setOrganizationId(filterParams.getOrganizationId());
        
        // Copy and validate account ID
        validatedParams.setAccountId(filterParams.getAccountId());
        
        // Validate and normalize date range filter
        if (filterParams.getDateRange() != null) {
            DateRangeFilter dateRange = filterParams.getDateRange();
            
            // Ensure start date is before end date
            if (dateRange.getStartDate() != null && dateRange.getEndDate() != null &&
                    dateRange.getStartDate().isAfter(dateRange.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
            
            validatedParams.setDateRange(dateRange);
        }
        
        // Validate and normalize amount range filter
        if (filterParams.getAmountRange() != null) {
            AmountRangeFilter amountRange = filterParams.getAmountRange();
            
            // Ensure min amount is less than max amount
            if (amountRange.getMinAmount() != null && amountRange.getMaxAmount() != null &&
                    amountRange.getMinAmount().compareTo(amountRange.getMaxAmount()) > 0) {
                throw new IllegalArgumentException("Minimum amount must be less than or equal to maximum amount");
            }
            
            // Ensure amounts are not negative
            if (amountRange.getMinAmount() != null && amountRange.getMinAmount().signum() < 0) {
                throw new IllegalArgumentException("Minimum amount cannot be negative");
            }
            
            if (amountRange.getMaxAmount() != null && amountRange.getMaxAmount().signum() < 0) {
                throw new IllegalArgumentException("Maximum amount cannot be negative");
            }
            
            validatedParams.setAmountRange(amountRange);
        }
        
        // Validate and normalize status filter
        if (filterParams.getStatusFilter() != null) {
            StatusFilter statusFilter = filterParams.getStatusFilter();
            
            // Ensure status filter has at least one status
            if (statusFilter.getStatuses() == null || statusFilter.getStatuses().isEmpty()) {
                logger.warn("Status filter with no statuses provided, ignoring");
            } else {
                validatedParams.setStatusFilter(statusFilter);
            }
        }
        
        // Copy and validate merchant ID
        if (filterParams.getMerchantId() != null && !filterParams.getMerchantId().trim().isEmpty()) {
            validatedParams.setMerchantId(filterParams.getMerchantId().trim());
        }
        
        // Copy and validate payment type
        if (filterParams.getPaymentType() != null && !filterParams.getPaymentType().trim().isEmpty()) {
            validatedParams.setPaymentType(filterParams.getPaymentType().trim());
        }
        
        // Copy and validate search term
        if (filterParams.getSearchTerm() != null && !filterParams.getSearchTerm().trim().isEmpty()) {
            validatedParams.setSearchTerm(filterParams.getSearchTerm().trim());
        }
        
        // Validate and normalize sort criteria
        if (filterParams.getSortCriteria() != null && !filterParams.getSortCriteria().isEmpty()) {
            List<SortCriteria> sortCriteria = new ArrayList<>();
            
            for (SortCriteria criteria : filterParams.getSortCriteria()) {
                // Validate sort column
                if (criteria.getColumn() == null || criteria.getColumn().trim().isEmpty()) {
                    logger.warn("Sort criteria with no column provided, ignoring");
                    continue;
                }
                
                // Normalize column name
                String column = criteria.getColumn().trim().toLowerCase();
                
                // Validate sort direction
                SortCriteria.Direction direction = criteria.getDirection();
                if (direction == null) {
                    direction = SortCriteria.Direction.DESC; // Default to descending
                }
                
                sortCriteria.add(new SortCriteria(column, direction));
            }
            
            if (!sortCriteria.isEmpty()) {
                validatedParams.setSortCriteria(sortCriteria);
            }
        } else {
            // Default sort by created_at descending if no sort criteria provided
            List<SortCriteria> defaultSort = Collections.singletonList(
                    new SortCriteria("created_at", SortCriteria.Direction.DESC));
            validatedParams.setSortCriteria(defaultSort);
        }
        
        // Validate and normalize pagination parameters
        if (filterParams.getPagination() != null) {
            PaginationParams pagination = filterParams.getPagination();
            
            int offset = pagination.getOffset();
            int limit = pagination.getLimit();
            
            // Ensure offset is not negative
            if (offset < 0) {
                offset = 0;
            }
            
            // Ensure limit is within bounds
            if (limit <= 0) {
                limit = DEFAULT_PAGE_SIZE;
            } else if (limit > MAX_PAGE_SIZE) {
                limit = MAX_PAGE_SIZE;
            }
            
            validatedParams.setPagination(new PaginationParams(offset, limit));
        } else {
            // Default pagination if not provided
            validatedParams.setPagination(new PaginationParams(0, DEFAULT_PAGE_SIZE));
        }
        
        return validatedParams;
    }
    
    /**
     * Exception class for payment query errors.
     */
    public static class PaymentQueryException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        /**
         * Constructs a new PaymentQueryException with the specified detail message.
         *
         * @param message The detail message
         */
        public PaymentQueryException(String message) {
            super(message);
        }
        
        /**
         * Constructs a new PaymentQueryException with the specified detail message and cause.
         *
         * @param message The detail message
         * @param cause The cause
         */
        public PaymentQueryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}