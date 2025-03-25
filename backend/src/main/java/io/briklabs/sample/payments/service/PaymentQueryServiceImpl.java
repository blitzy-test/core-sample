package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaginationParams;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.SortCriteria;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the PaymentQueryService interface that provides
 * advanced payment data querying capabilities.
 * 
 * This class handles complex filter construction, query execution, sorting logic,
 * and pagination, working closely with the data access layer to retrieve optimized
 * result sets.
 */
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentQueryServiceImpl.class);
    
    // Constants for validation and defaults
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int DEFAULT_OFFSET = 0;
    
    // Set of allowed sort fields for validation
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "transaction_id", "status", "amount", "currency", "created_at", "updated_at", 
            "merchant_id", "payment_type", "transaction_reference", "description"
    ));
    
    // Default sort criteria if none provided
    private static final List<SortCriteria> DEFAULT_SORT_CRITERIA = Collections.singletonList(
            new SortCriteria("created_at", SortCriteria.Direction.DESC)
    );
    
    private final PaymentTransactionDAO transactionDAO;
    
    /**
     * Constructs a new PaymentQueryServiceImpl with the necessary dependencies.
     * 
     * @param daoFactory Factory for creating data access objects
     */
    public PaymentQueryServiceImpl(PaymentDAOFactory daoFactory) {
        this.transactionDAO = daoFactory.getPaymentTransactionDAO();
    }
    
    /**
     * Alternative constructor for testing or when DAO is directly available.
     * 
     * @param transactionDAO The payment transaction DAO implementation
     */
    public PaymentQueryServiceImpl(PaymentTransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    @Override
    public List<PaymentTransaction> findTransactions(UUID organizationId, UUID accountId, 
                                                    PaymentFilterParams filterParams) {
        logger.debug("Finding transactions for org: {}, account: {}, filters: {}", 
                organizationId, accountId, filterParams);
        
        // Validate and normalize filter parameters
        PaymentFilterParams validatedParams = validateAndNormalizeFilterParams(filterParams);
        
        // Use default sort if none provided
        List<SortCriteria> sortCriteria = validatedParams.getSortCriteria();
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            sortCriteria = DEFAULT_SORT_CRITERIA;
        } else {
            sortCriteria = validateAndNormalizeSortCriteria(sortCriteria);
        }
        
        // Execute query with validated parameters
        return transactionDAO.findTransactions(organizationId, accountId, validatedParams, sortCriteria);
    }

    @Override
    public List<PaymentTransaction> findTransactionsPaginated(UUID organizationId, UUID accountId,
                                                            PaymentFilterParams filterParams,
                                                            PaginationParams paginationParams) {
        logger.debug("Finding paginated transactions for org: {}, account: {}, filters: {}, pagination: {}", 
                organizationId, accountId, filterParams, paginationParams);
        
        // Validate and normalize parameters
        PaymentFilterParams validatedFilters = validateAndNormalizeFilterParams(filterParams);
        PaginationParams validatedPagination = validateAndNormalizePaginationParams(paginationParams);
        
        // Use default sort if none provided
        List<SortCriteria> sortCriteria = validatedFilters.getSortCriteria();
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            sortCriteria = DEFAULT_SORT_CRITERIA;
        } else {
            sortCriteria = validateAndNormalizeSortCriteria(sortCriteria);
        }
        
        // Execute paginated query with validated parameters
        return transactionDAO.findTransactionsPaginated(
                organizationId, accountId, validatedFilters, sortCriteria, validatedPagination);
    }

    @Override
    public long countTransactions(UUID organizationId, UUID accountId, PaymentFilterParams filterParams) {
        logger.debug("Counting transactions for org: {}, account: {}, filters: {}", 
                organizationId, accountId, filterParams);
        
        // Validate filter parameters
        PaymentFilterParams validatedParams = validateAndNormalizeFilterParams(filterParams);
        
        // Execute count query
        return transactionDAO.countTransactions(organizationId, accountId, validatedParams);
    }

    @Override
    public List<PaymentTransaction> findTransactionsByDateRange(UUID organizationId, UUID accountId,
                                                              DateRangeFilter dateRangeFilter,
                                                              Optional<PaginationParams> paginationParams) {
        logger.debug("Finding transactions by date range for org: {}, account: {}, dateRange: {}", 
                organizationId, accountId, dateRangeFilter);
        
        // Validate date range filter
        validateDateRangeFilter(dateRangeFilter);
        
        // Create filter params with date range
        PaymentFilterParams filterParams = new PaymentFilterParams();
        filterParams.setDateRangeFilter(dateRangeFilter);
        
        // Apply pagination if provided
        if (paginationParams.isPresent()) {
            PaginationParams validatedPagination = validateAndNormalizePaginationParams(paginationParams.get());
            return findTransactionsPaginated(organizationId, accountId, filterParams, validatedPagination);
        } else {
            return findTransactions(organizationId, accountId, filterParams);
        }
    }

    @Override
    public List<PaymentTransaction> findTransactionsByAmountRange(UUID organizationId, UUID accountId,
                                                                AmountRangeFilter amountRangeFilter,
                                                                Optional<PaginationParams> paginationParams) {
        logger.debug("Finding transactions by amount range for org: {}, account: {}, amountRange: {}", 
                organizationId, accountId, amountRangeFilter);
        
        // Validate amount range filter
        validateAmountRangeFilter(amountRangeFilter);
        
        // Create filter params with amount range
        PaymentFilterParams filterParams = new PaymentFilterParams();
        filterParams.setAmountRangeFilter(amountRangeFilter);
        
        // Apply pagination if provided
        if (paginationParams.isPresent()) {
            PaginationParams validatedPagination = validateAndNormalizePaginationParams(paginationParams.get());
            return findTransactionsPaginated(organizationId, accountId, filterParams, validatedPagination);
        } else {
            return findTransactions(organizationId, accountId, filterParams);
        }
    }

    @Override
    public List<PaymentTransaction> findTransactionsByStatus(UUID organizationId, UUID accountId,
                                                           StatusFilter statusFilter,
                                                           Optional<PaginationParams> paginationParams) {
        logger.debug("Finding transactions by status for org: {}, account: {}, statusFilter: {}", 
                organizationId, accountId, statusFilter);
        
        // Validate status filter
        validateStatusFilter(statusFilter);
        
        // Create filter params with status filter
        PaymentFilterParams filterParams = new PaymentFilterParams();
        filterParams.setStatusFilter(statusFilter);
        
        // Apply pagination if provided
        if (paginationParams.isPresent()) {
            PaginationParams validatedPagination = validateAndNormalizePaginationParams(paginationParams.get());
            return findTransactionsPaginated(organizationId, accountId, filterParams, validatedPagination);
        } else {
            return findTransactions(organizationId, accountId, filterParams);
        }
    }

    @Override
    public List<PaymentTransaction> findTransactionsByMerchant(UUID organizationId, UUID accountId,
                                                             String merchantId,
                                                             Optional<PaymentFilterParams> filterParams,
                                                             Optional<PaginationParams> paginationParams) {
        logger.debug("Finding transactions by merchant for org: {}, account: {}, merchantId: {}", 
                organizationId, accountId, merchantId);
        
        // Validate merchant ID
        if (merchantId == null || merchantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Merchant ID cannot be null or empty");
        }
        
        // Create or update filter params with merchant ID
        PaymentFilterParams params = filterParams.orElse(new PaymentFilterParams());
        params.setMerchantId(merchantId);
        
        // Validate and normalize filter parameters
        PaymentFilterParams validatedParams = validateAndNormalizeFilterParams(params);
        
        // Apply pagination if provided
        if (paginationParams.isPresent()) {
            PaginationParams validatedPagination = validateAndNormalizePaginationParams(paginationParams.get());
            return findTransactionsPaginated(organizationId, accountId, validatedParams, validatedPagination);
        } else {
            return findTransactions(organizationId, accountId, validatedParams);
        }
    }

    @Override
    public List<PaymentTransaction> findTransactionsSorted(UUID organizationId, UUID accountId,
                                                         PaymentFilterParams filterParams,
                                                         List<SortCriteria> sortCriteria,
                                                         Optional<PaginationParams> paginationParams) {
        logger.debug("Finding sorted transactions for org: {}, account: {}, sortCriteria: {}", 
                organizationId, accountId, sortCriteria);
        
        // Validate and normalize filter parameters
        PaymentFilterParams validatedFilters = validateAndNormalizeFilterParams(filterParams);
        
        // Validate and normalize sort criteria
        List<SortCriteria> validatedSortCriteria = validateAndNormalizeSortCriteria(sortCriteria);
        validatedFilters.setSortCriteria(validatedSortCriteria);
        
        // Apply pagination if provided
        if (paginationParams.isPresent()) {
            PaginationParams validatedPagination = validateAndNormalizePaginationParams(paginationParams.get());
            return findTransactionsPaginated(organizationId, accountId, validatedFilters, validatedPagination);
        } else {
            return findTransactions(organizationId, accountId, validatedFilters);
        }
    }

    @Override
    public PaymentFilterParams validateAndNormalizeFilterParams(PaymentFilterParams filterParams) {
        if (filterParams == null) {
            return new PaymentFilterParams();
        }
        
        // Create a new instance to avoid modifying the input
        PaymentFilterParams validatedParams = new PaymentFilterParams();
        
        // Validate and copy date range filter if present
        if (filterParams.getDateRangeFilter() != null) {
            validateDateRangeFilter(filterParams.getDateRangeFilter());
            validatedParams.setDateRangeFilter(filterParams.getDateRangeFilter());
        }
        
        // Validate and copy amount range filter if present
        if (filterParams.getAmountRangeFilter() != null) {
            validateAmountRangeFilter(filterParams.getAmountRangeFilter());
            validatedParams.setAmountRangeFilter(filterParams.getAmountRangeFilter());
        }
        
        // Validate and copy status filter if present
        if (filterParams.getStatusFilter() != null) {
            validateStatusFilter(filterParams.getStatusFilter());
            validatedParams.setStatusFilter(filterParams.getStatusFilter());
        }
        
        // Copy merchant ID if present
        if (filterParams.getMerchantId() != null && !filterParams.getMerchantId().trim().isEmpty()) {
            validatedParams.setMerchantId(filterParams.getMerchantId().trim());
        }
        
        // Copy payment type if present
        if (filterParams.getPaymentType() != null && !filterParams.getPaymentType().trim().isEmpty()) {
            validatedParams.setPaymentType(filterParams.getPaymentType().trim());
        }
        
        // Copy transaction reference if present
        if (filterParams.getTransactionReference() != null && !filterParams.getTransactionReference().trim().isEmpty()) {
            validatedParams.setTransactionReference(filterParams.getTransactionReference().trim());
        }
        
        // Copy search text if present
        if (filterParams.getSearchText() != null && !filterParams.getSearchText().trim().isEmpty()) {
            // Sanitize search text to prevent SQL injection
            String sanitizedSearchText = filterParams.getSearchText().trim()
                    .replaceAll("[%_\\[\\]^]", "\\\\$0"); // Escape special characters
            validatedParams.setSearchText(sanitizedSearchText);
        }
        
        // Copy and validate sort criteria if present
        if (filterParams.getSortCriteria() != null && !filterParams.getSortCriteria().isEmpty()) {
            validatedParams.setSortCriteria(validateAndNormalizeSortCriteria(filterParams.getSortCriteria()));
        } else {
            validatedParams.setSortCriteria(DEFAULT_SORT_CRITERIA);
        }
        
        return validatedParams;
    }

    @Override
    public PaginationParams validateAndNormalizePaginationParams(PaginationParams paginationParams) {
        if (paginationParams == null) {
            return new PaginationParams(DEFAULT_PAGE_SIZE, DEFAULT_OFFSET);
        }
        
        int limit = paginationParams.getLimit();
        int offset = paginationParams.getOffset();
        
        // Validate and normalize limit
        if (limit < MIN_PAGE_SIZE) {
            limit = DEFAULT_PAGE_SIZE;
            logger.debug("Normalizing page size to default: {}", DEFAULT_PAGE_SIZE);
        } else if (limit > MAX_PAGE_SIZE) {
            limit = MAX_PAGE_SIZE;
            logger.debug("Capping page size to maximum: {}", MAX_PAGE_SIZE);
        }
        
        // Validate and normalize offset
        if (offset < 0) {
            offset = DEFAULT_OFFSET;
            logger.debug("Normalizing negative offset to default: {}", DEFAULT_OFFSET);
        }
        
        return new PaginationParams(limit, offset);
    }

    @Override
    public List<SortCriteria> validateAndNormalizeSortCriteria(List<SortCriteria> sortCriteria) {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            return DEFAULT_SORT_CRITERIA;
        }
        
        List<SortCriteria> validatedCriteria = new ArrayList<>();
        
        for (SortCriteria criteria : sortCriteria) {
            if (criteria == null) {
                continue;
            }
            
            String field = criteria.getField();
            
            // Validate field name
            if (field == null || field.trim().isEmpty()) {
                logger.warn("Ignoring sort criteria with null or empty field");
                continue;
            }
            
            field = field.trim().toLowerCase();
            
            // Check if field is allowed
            if (!ALLOWED_SORT_FIELDS.contains(field)) {
                logger.warn("Ignoring sort criteria with invalid field: {}", field);
                continue;
            }
            
            // Use provided direction or default to ASC
            SortCriteria.Direction direction = criteria.getDirection() != null ? 
                    criteria.getDirection() : SortCriteria.Direction.ASC;
            
            validatedCriteria.add(new SortCriteria(field, direction));
        }
        
        // If all criteria were invalid, use default
        if (validatedCriteria.isEmpty()) {
            logger.debug("All sort criteria were invalid, using default sort");
            return DEFAULT_SORT_CRITERIA;
        }
        
        return validatedCriteria;
    }
    
    /**
     * Validates a date range filter for correctness and security.
     * 
     * @param dateRangeFilter The date range filter to validate
     * @throws IllegalArgumentException if the filter is invalid
     */
    private void validateDateRangeFilter(DateRangeFilter dateRangeFilter) {
        if (dateRangeFilter == null) {
            throw new IllegalArgumentException("Date range filter cannot be null");
        }
        
        LocalDateTime startDate = dateRangeFilter.getStartDate();
        LocalDateTime endDate = dateRangeFilter.getEndDate();
        
        // If both dates are null, that's invalid
        if (startDate == null && endDate == null) {
            throw new IllegalArgumentException("Date range filter must specify at least one date boundary");
        }
        
        // If both dates are specified, ensure start is before end
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date in date range filter");
        }
        
        // Validate date field to filter on
        String dateField = dateRangeFilter.getDateField();
        if (dateField == null || dateField.trim().isEmpty()) {
            // Default to created_at if not specified
            dateRangeFilter.setDateField("created_at");
        } else {
            // Ensure date field is one of the allowed values
            String normalizedField = dateField.trim().toLowerCase();
            if (!Arrays.asList("created_at", "updated_at", "processed_at").contains(normalizedField)) {
                throw new IllegalArgumentException("Invalid date field for filtering: " + dateField);
            }
            dateRangeFilter.setDateField(normalizedField);
        }
        
        // Limit date ranges to reasonable values (e.g., prevent querying all transactions ever)
        if (startDate == null) {
            // If no start date, default to 90 days ago
            LocalDateTime defaultStart = LocalDateTime.now(ZoneOffset.UTC).minusDays(90);
            dateRangeFilter.setStartDate(defaultStart);
            logger.debug("Setting default start date to 90 days ago: {}", defaultStart);
        }
        
        if (endDate == null) {
            // If no end date, default to now
            LocalDateTime defaultEnd = LocalDateTime.now(ZoneOffset.UTC);
            dateRangeFilter.setEndDate(defaultEnd);
            logger.debug("Setting default end date to now: {}", defaultEnd);
        }
    }
    
    /**
     * Validates an amount range filter for correctness and security.
     * 
     * @param amountRangeFilter The amount range filter to validate
     * @throws IllegalArgumentException if the filter is invalid
     */
    private void validateAmountRangeFilter(AmountRangeFilter amountRangeFilter) {
        if (amountRangeFilter == null) {
            throw new IllegalArgumentException("Amount range filter cannot be null");
        }
        
        BigDecimal minAmount = amountRangeFilter.getMinAmount();
        BigDecimal maxAmount = amountRangeFilter.getMaxAmount();
        
        // If both amounts are null, that's invalid
        if (minAmount == null && maxAmount == null) {
            throw new IllegalArgumentException("Amount range filter must specify at least one amount boundary");
        }
        
        // If both amounts are specified, ensure min is less than or equal to max
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Minimum amount must be less than or equal to maximum amount");
        }
        
        // Ensure amounts are not negative
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum amount cannot be negative");
        }
        
        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum amount cannot be negative");
        }
        
        // Validate currency if specified
        String currency = amountRangeFilter.getCurrency();
        if (currency != null) {
            if (currency.trim().isEmpty()) {
                amountRangeFilter.setCurrency(null);
            } else if (!currency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
            }
        }
    }
    
    /**
     * Validates a status filter for correctness and security.
     * 
     * @param statusFilter The status filter to validate
     * @throws IllegalArgumentException if the filter is invalid
     */
    private void validateStatusFilter(StatusFilter statusFilter) {
        if (statusFilter == null) {
            throw new IllegalArgumentException("Status filter cannot be null");
        }
        
        List<String> statuses = statusFilter.getStatuses();
        
        // Must have at least one status
        if (statuses == null || statuses.isEmpty()) {
            throw new IllegalArgumentException("Status filter must include at least one status");
        }
        
        // Validate each status against the PaymentStatus enum
        List<String> validatedStatuses = new ArrayList<>();
        for (String status : statuses) {
            if (status == null || status.trim().isEmpty()) {
                continue;
            }
            
            String normalizedStatus = status.trim().toUpperCase();
            
            try {
                // Verify this is a valid status by parsing it
                PaymentStatus.valueOf(normalizedStatus);
                validatedStatuses.add(normalizedStatus);
            } catch (IllegalArgumentException e) {
                logger.warn("Ignoring invalid status in filter: {}", status);
                // Skip invalid statuses
            }
        }
        
        // If all statuses were invalid, that's an error
        if (validatedStatuses.isEmpty()) {
            throw new IllegalArgumentException("Status filter contains no valid status values");
        }
        
        // Update the filter with validated statuses
        statusFilter.setStatuses(validatedStatuses);
    }
}