package io.briklabs.sample.payments.data.query;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data transfer object that encapsulates all possible filtering parameters for payment transaction queries.
 * This class handles parameter validation, type conversion, and default values for search criteria
 * like date ranges, amount ranges, status filters, and merchant identifiers.
 * 
 * It serves as a standardized container for query parameters that can be passed between layers of the application.
 */
public class PaymentFilterParams {
    private static final Logger logger = LoggerFactory.getLogger(PaymentFilterParams.class);
    
    // Standard date format for query parameters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    // Default pagination values
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int MAX_PAGE_SIZE = 100;
    
    // Organization and account context
    private UUID organizationId;
    private UUID accountId;
    
    // Date range filtering
    private DateRangeFilter dateRange;
    
    // Amount range filtering
    private AmountRangeFilter amountRange;
    
    // Status filtering
    private StatusFilter statusFilter;
    
    // Merchant filtering
    private String merchantId;
    
    // Payment type filtering
    private String paymentType;
    
    // Text search
    private String searchTerm;
    
    // Sorting criteria
    private List<SortCriteria> sortCriteria;
    
    // Pagination parameters
    private PaginationParams pagination;
    
    /**
     * Creates a new empty filter parameters object.
     */
    public PaymentFilterParams() {
        this.sortCriteria = new ArrayList<>();
        this.pagination = new PaginationParams(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NUMBER);
    }
    
    /**
     * Sets the organization ID for filtering.
     * 
     * @param organizationId The organization UUID
     * @return This object for method chaining
     */
    public PaymentFilterParams withOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        return this;
    }
    
    /**
     * Sets the organization ID for filtering from a string.
     * 
     * @param organizationId The organization UUID as a string
     * @return This object for method chaining
     * @throws IllegalArgumentException If the string is not a valid UUID
     */
    public PaymentFilterParams withOrganizationId(String organizationId) {
        if (organizationId != null && !organizationId.isEmpty()) {
            try {
                this.organizationId = UUID.fromString(organizationId);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid organization ID format: {}", organizationId);
                throw new IllegalArgumentException("Invalid organization ID format. Expected UUID format.");
            }
        }
        return this;
    }
    
    /**
     * Sets the account ID for filtering.
     * 
     * @param accountId The account UUID
     * @return This object for method chaining
     */
    public PaymentFilterParams withAccountId(UUID accountId) {
        this.accountId = accountId;
        return this;
    }
    
    /**
     * Sets the account ID for filtering from a string.
     * 
     * @param accountId The account UUID as a string
     * @return This object for method chaining
     * @throws IllegalArgumentException If the string is not a valid UUID
     */
    public PaymentFilterParams withAccountId(String accountId) {
        if (accountId != null && !accountId.isEmpty()) {
            try {
                this.accountId = UUID.fromString(accountId);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid account ID format: {}", accountId);
                throw new IllegalArgumentException("Invalid account ID format. Expected UUID format.");
            }
        }
        return this;
    }
    
    /**
     * Sets the date range filter with start and end dates.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return This object for method chaining
     */
    public PaymentFilterParams withDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        this.dateRange = new DateRangeFilter(startDate, endDate);
        return this;
    }
    
    /**
     * Sets the date range filter with string dates.
     * 
     * @param fromDate The start date as ISO string (inclusive)
     * @param toDate The end date as ISO string (inclusive)
     * @return This object for method chaining
     * @throws DateTimeParseException If the date strings are not in valid ISO format
     */
    public PaymentFilterParams withDateRange(String fromDate, String toDate) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                startDate = LocalDateTime.parse(fromDate, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid fromDate format: {}", fromDate);
                throw new IllegalArgumentException("Invalid fromDate format. Expected ISO date-time format.");
            }
        }
        
        if (toDate != null && !toDate.isEmpty()) {
            try {
                endDate = LocalDateTime.parse(toDate, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid toDate format: {}", toDate);
                throw new IllegalArgumentException("Invalid toDate format. Expected ISO date-time format.");
            }
        }
        
        if (startDate != null || endDate != null) {
            this.dateRange = new DateRangeFilter(startDate, endDate);
        }
        
        return this;
    }
    
    /**
     * Sets the amount range filter with minimum and maximum amounts.
     * 
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @param currency The currency code (optional)
     * @return This object for method chaining
     */
    public PaymentFilterParams withAmountRange(BigDecimal minAmount, BigDecimal maxAmount, String currency) {
        this.amountRange = new AmountRangeFilter(minAmount, maxAmount, currency);
        return this;
    }
    
    /**
     * Sets the amount range filter with string amounts.
     * 
     * @param minAmount The minimum amount as string (inclusive)
     * @param maxAmount The maximum amount as string (inclusive)
     * @param currency The currency code (optional)
     * @return This object for method chaining
     * @throws NumberFormatException If the amount strings are not valid decimal numbers
     */
    public PaymentFilterParams withAmountRange(String minAmount, String maxAmount, String currency) {
        BigDecimal min = null;
        BigDecimal max = null;
        
        if (minAmount != null && !minAmount.isEmpty()) {
            try {
                min = new BigDecimal(minAmount);
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    logger.warn("Negative minimum amount: {}", minAmount);
                    throw new IllegalArgumentException("Minimum amount cannot be negative.");
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid minAmount format: {}", minAmount);
                throw new IllegalArgumentException("Invalid minAmount format. Expected decimal number.");
            }
        }
        
        if (maxAmount != null && !maxAmount.isEmpty()) {
            try {
                max = new BigDecimal(maxAmount);
                if (max.compareTo(BigDecimal.ZERO) < 0) {
                    logger.warn("Negative maximum amount: {}", maxAmount);
                    throw new IllegalArgumentException("Maximum amount cannot be negative.");
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid maxAmount format: {}", maxAmount);
                throw new IllegalArgumentException("Invalid maxAmount format. Expected decimal number.");
            }
        }
        
        if (min != null && max != null && min.compareTo(max) > 0) {
            logger.warn("Minimum amount greater than maximum amount: {} > {}", min, max);
            throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount.");
        }
        
        if (min != null || max != null) {
            this.amountRange = new AmountRangeFilter(min, max, currency);
        }
        
        return this;
    }
    
    /**
     * Sets the status filter with a single status value.
     * 
     * @param status The status value to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams withStatus(String status) {
        if (status != null && !status.isEmpty()) {
            this.statusFilter = new StatusFilter(status);
        }
        return this;
    }
    
    /**
     * Sets the status filter with multiple status values.
     * 
     * @param statuses The status values to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams withStatusIn(String... statuses) {
        if (statuses != null && statuses.length > 0) {
            this.statusFilter = new StatusFilter(Arrays.asList(statuses));
        }
        return this;
    }
    
    /**
     * Sets the status filter with a comma-separated list of status values.
     * 
     * @param statusList The comma-separated list of status values
     * @return This object for method chaining
     */
    public PaymentFilterParams withStatusList(String statusList) {
        if (statusList != null && !statusList.isEmpty()) {
            String[] statuses = statusList.split(",");
            Set<String> statusSet = new HashSet<>();
            
            for (String status : statuses) {
                String trimmed = status.trim();
                if (!trimmed.isEmpty()) {
                    statusSet.add(trimmed);
                }
            }
            
            if (!statusSet.isEmpty()) {
                this.statusFilter = new StatusFilter(statusSet);
            }
        }
        return this;
    }
    
    /**
     * Sets the merchant ID for filtering.
     * 
     * @param merchantId The merchant identifier
     * @return This object for method chaining
     */
    public PaymentFilterParams withMerchantId(String merchantId) {
        if (merchantId != null && !merchantId.isEmpty()) {
            this.merchantId = merchantId;
        }
        return this;
    }
    
    /**
     * Sets the payment type for filtering.
     * 
     * @param paymentType The payment type value
     * @return This object for method chaining
     */
    public PaymentFilterParams withPaymentType(String paymentType) {
        if (paymentType != null && !paymentType.isEmpty()) {
            this.paymentType = paymentType;
        }
        return this;
    }
    
    /**
     * Sets the search term for text searching.
     * 
     * @param searchTerm The search term
     * @return This object for method chaining
     */
    public PaymentFilterParams withSearchTerm(String searchTerm) {
        if (searchTerm != null && !searchTerm.isEmpty()) {
            this.searchTerm = searchTerm;
        }
        return this;
    }
    
    /**
     * Adds a sort criterion to the filter.
     * 
     * @param field The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @return This object for method chaining
     */
    public PaymentFilterParams addSortCriterion(String field, String direction) {
        if (field != null && !field.isEmpty()) {
            SortCriteria criteria = new SortCriteria(field, direction);
            this.sortCriteria.add(criteria);
        }
        return this;
    }
    
    /**
     * Sets the pagination parameters.
     * 
     * @param pageSize The page size
     * @param pageNumber The page number (0-based)
     * @return This object for method chaining
     */
    public PaymentFilterParams withPagination(int pageSize, int pageNumber) {
        int size = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        int page = Math.max(0, pageNumber);
        this.pagination = new PaginationParams(size, page);
        return this;
    }
    
    /**
     * Sets the pagination parameters from string values.
     * 
     * @param pageSize The page size as string
     * @param pageNumber The page number as string (0-based)
     * @return This object for method chaining
     * @throws NumberFormatException If the string values are not valid integers
     */
    public PaymentFilterParams withPagination(String pageSize, String pageNumber) {
        int size = DEFAULT_PAGE_SIZE;
        int page = DEFAULT_PAGE_NUMBER;
        
        if (pageSize != null && !pageSize.isEmpty()) {
            try {
                size = Integer.parseInt(pageSize);
                if (size < 1) {
                    logger.warn("Invalid page size: {}, using default", pageSize);
                    size = DEFAULT_PAGE_SIZE;
                } else if (size > MAX_PAGE_SIZE) {
                    logger.warn("Page size too large: {}, using maximum", pageSize);
                    size = MAX_PAGE_SIZE;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid page size format: {}, using default", pageSize);
                size = DEFAULT_PAGE_SIZE;
            }
        }
        
        if (pageNumber != null && !pageNumber.isEmpty()) {
            try {
                page = Integer.parseInt(pageNumber);
                if (page < 0) {
                    logger.warn("Invalid page number: {}, using default", pageNumber);
                    page = DEFAULT_PAGE_NUMBER;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid page number format: {}, using default", pageNumber);
                page = DEFAULT_PAGE_NUMBER;
            }
        }
        
        this.pagination = new PaginationParams(size, page);
        return this;
    }
    
    /**
     * Sets the pagination parameters with limit and offset.
     * 
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return This object for method chaining
     */
    public PaymentFilterParams withLimitOffset(int limit, int offset) {
        int validLimit = Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
        int validOffset = Math.max(0, offset);
        this.pagination = new PaginationParams(validLimit, validOffset);
        return this;
    }
    
    /**
     * Sets the pagination parameters with limit and offset from string values.
     * 
     * @param limit The maximum number of records to return as string
     * @param offset The number of records to skip as string
     * @return This object for method chaining
     * @throws NumberFormatException If the string values are not valid integers
     */
    public PaymentFilterParams withLimitOffset(String limit, String offset) {
        int validLimit = DEFAULT_PAGE_SIZE;
        int validOffset = 0;
        
        if (limit != null && !limit.isEmpty()) {
            try {
                validLimit = Integer.parseInt(limit);
                if (validLimit < 1) {
                    logger.warn("Invalid limit: {}, using default", limit);
                    validLimit = DEFAULT_PAGE_SIZE;
                } else if (validLimit > MAX_PAGE_SIZE) {
                    logger.warn("Limit too large: {}, using maximum", limit);
                    validLimit = MAX_PAGE_SIZE;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid limit format: {}, using default", limit);
                validLimit = DEFAULT_PAGE_SIZE;
            }
        }
        
        if (offset != null && !offset.isEmpty()) {
            try {
                validOffset = Integer.parseInt(offset);
                if (validOffset < 0) {
                    logger.warn("Invalid offset: {}, using 0", offset);
                    validOffset = 0;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid offset format: {}, using 0", offset);
                validOffset = 0;
            }
        }
        
        this.pagination = new PaginationParams(validLimit, validOffset);
        return this;
    }
    
    /**
     * Creates a new filter parameters object from a set of query parameters.
     * This is a convenience method for creating filters from HTTP request parameters.
     * 
     * @param organizationId The organization ID
     * @param accountId The account ID
     * @param fromDate The start date for filtering
     * @param toDate The end date for filtering
     * @param minAmount The minimum amount for filtering
     * @param maxAmount The maximum amount for filtering
     * @param currency The currency code for amount filtering
     * @param status The status value or comma-separated list of statuses
     * @param merchantId The merchant ID for filtering
     * @param paymentType The payment type for filtering
     * @param searchTerm The search term for text searching
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction (ASC or DESC)
     * @param pageSize The page size for pagination
     * @param pageNumber The page number for pagination
     * @return A new filter parameters object
     */
    public static PaymentFilterParams fromQueryParams(
            String organizationId,
            String accountId,
            String fromDate,
            String toDate,
            String minAmount,
            String maxAmount,
            String currency,
            String status,
            String merchantId,
            String paymentType,
            String searchTerm,
            String sortBy,
            String sortDirection,
            String pageSize,
            String pageNumber) {
        
        PaymentFilterParams filters = new PaymentFilterParams();
        
        try {
            // Apply organization and account context
            filters.withOrganizationId(organizationId)
                   .withAccountId(accountId);
            
            // Apply date range filter
            filters.withDateRange(fromDate, toDate);
            
            // Apply amount range filter
            filters.withAmountRange(minAmount, maxAmount, currency);
            
            // Apply status filter
            filters.withStatusList(status);
            
            // Apply merchant and payment type filters
            filters.withMerchantId(merchantId)
                   .withPaymentType(paymentType);
            
            // Apply text search
            filters.withSearchTerm(searchTerm);
            
            // Apply sorting
            if (sortBy != null && !sortBy.isEmpty()) {
                filters.addSortCriterion(sortBy, sortDirection);
            }
            
            // Apply pagination
            filters.withPagination(pageSize, pageNumber);
            
        } catch (Exception e) {
            logger.error("Error creating filter parameters from query params", e);
            throw new IllegalArgumentException("Invalid filter parameters: " + e.getMessage());
        }
        
        return filters;
    }
    
    /**
     * Validates the filter parameters for consistency and correctness.
     * 
     * @throws IllegalArgumentException If the filter parameters are invalid
     */
    public void validate() {
        // Validate date range
        if (dateRange != null) {
            dateRange.validate();
        }
        
        // Validate amount range
        if (amountRange != null) {
            amountRange.validate();
        }
        
        // Validate status filter
        if (statusFilter != null) {
            statusFilter.validate();
        }
        
        // Validate pagination
        if (pagination != null) {
            pagination.validate();
        }
    }
    
    /**
     * Gets the organization ID filter.
     * 
     * @return The organization ID
     */
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    /**
     * Gets the account ID filter.
     * 
     * @return The account ID
     */
    public UUID getAccountId() {
        return accountId;
    }
    
    /**
     * Gets the date range filter.
     * 
     * @return The date range filter
     */
    public DateRangeFilter getDateRange() {
        return dateRange;
    }
    
    /**
     * Gets the amount range filter.
     * 
     * @return The amount range filter
     */
    public AmountRangeFilter getAmountRange() {
        return amountRange;
    }
    
    /**
     * Gets the status filter.
     * 
     * @return The status filter
     */
    public StatusFilter getStatusFilter() {
        return statusFilter;
    }
    
    /**
     * Gets the merchant ID filter.
     * 
     * @return The merchant ID
     */
    public String getMerchantId() {
        return merchantId;
    }
    
    /**
     * Gets the payment type filter.
     * 
     * @return The payment type
     */
    public String getPaymentType() {
        return paymentType;
    }
    
    /**
     * Gets the search term for text searching.
     * 
     * @return The search term
     */
    public String getSearchTerm() {
        return searchTerm;
    }
    
    /**
     * Gets the sort criteria.
     * 
     * @return The list of sort criteria
     */
    public List<SortCriteria> getSortCriteria() {
        return sortCriteria;
    }
    
    /**
     * Gets the pagination parameters.
     * 
     * @return The pagination parameters
     */
    public PaginationParams getPagination() {
        return pagination;
    }
    
    /**
     * Inner class representing a date range filter.
     */
    public static class DateRangeFilter {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        
        /**
         * Creates a new date range filter.
         * 
         * @param startDate The start date (inclusive)
         * @param endDate The end date (inclusive)
         */
        public DateRangeFilter(LocalDateTime startDate, LocalDateTime endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        /**
         * Validates the date range for consistency.
         * 
         * @throws IllegalArgumentException If the date range is invalid
         */
        public void validate() {
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }
        }
        
        /**
         * Gets the start date.
         * 
         * @return The start date
         */
        public LocalDateTime getStartDate() {
            return startDate;
        }
        
        /**
         * Gets the end date.
         * 
         * @return The end date
         */
        public LocalDateTime getEndDate() {
            return endDate;
        }
    }
    
    /**
     * Inner class representing an amount range filter.
     */
    public static class AmountRangeFilter {
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String currency;
        
        /**
         * Creates a new amount range filter.
         * 
         * @param minAmount The minimum amount (inclusive)
         * @param maxAmount The maximum amount (inclusive)
         * @param currency The currency code (optional)
         */
        public AmountRangeFilter(BigDecimal minAmount, BigDecimal maxAmount, String currency) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.currency = currency;
        }
        
        /**
         * Validates the amount range for consistency.
         * 
         * @throws IllegalArgumentException If the amount range is invalid
         */
        public void validate() {
            if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Minimum amount cannot be negative");
            }
            
            if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Maximum amount cannot be negative");
            }
            
            if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
                throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount");
            }
            
            if (currency != null && !currency.isEmpty() && currency.length() != 3) {
                throw new IllegalArgumentException("Currency code must be 3 characters");
            }
        }
        
        /**
         * Gets the minimum amount.
         * 
         * @return The minimum amount
         */
        public BigDecimal getMinAmount() {
            return minAmount;
        }
        
        /**
         * Gets the maximum amount.
         * 
         * @return The maximum amount
         */
        public BigDecimal getMaxAmount() {
            return maxAmount;
        }
        
        /**
         * Gets the currency code.
         * 
         * @return The currency code
         */
        public String getCurrency() {
            return currency;
        }
    }
    
    /**
     * Inner class representing a status filter.
     */
    public static class StatusFilter {
        private Set<String> statuses;
        
        /**
         * Creates a new status filter with a single status.
         * 
         * @param status The status value
         */
        public StatusFilter(String status) {
            this.statuses = new HashSet<>();
            if (status != null && !status.isEmpty()) {
                this.statuses.add(status);
            }
        }
        
        /**
         * Creates a new status filter with multiple statuses.
         * 
         * @param statuses The collection of status values
         */
        public StatusFilter(Collection<String> statuses) {
            this.statuses = new HashSet<>();
            if (statuses != null) {
                for (String status : statuses) {
                    if (status != null && !status.trim().isEmpty()) {
                        this.statuses.add(status.trim());
                    }
                }
            }
        }
        
        /**
         * Validates the status filter.
         * 
         * @throws IllegalArgumentException If the status filter is invalid
         */
        public void validate() {
            // In a real implementation, this would validate against allowed status values
            // from the PaymentStatus enum or other source of valid statuses
        }
        
        /**
         * Gets the status values.
         * 
         * @return The set of status values
         */
        public Set<String> getStatuses() {
            return statuses;
        }
    }
    
    /**
     * Inner class representing sort criteria.
     */
    public static class SortCriteria {
        private String column;
        private String direction;
        
        /**
         * Creates new sort criteria.
         * 
         * @param column The column to sort by
         * @param direction The sort direction (ASC or DESC)
         */
        public SortCriteria(String column, String direction) {
            this.column = column;
            
            // Default to ASC if direction is not specified or invalid
            if (direction == null || direction.isEmpty()) {
                this.direction = "ASC";
            } else {
                String dir = direction.toUpperCase();
                if (dir.equals("ASC") || dir.equals("DESC")) {
                    this.direction = dir;
                } else {
                    this.direction = "ASC";
                }
            }
        }
        
        /**
         * Gets the column to sort by.
         * 
         * @return The column name
         */
        public String getColumn() {
            return column;
        }
        
        /**
         * Gets the sort direction.
         * 
         * @return The sort direction (ASC or DESC)
         */
        public String getDirection() {
            return direction;
        }
    }
    
    /**
     * Inner class representing pagination parameters.
     */
    public static class PaginationParams {
        private int limit;
        private int offset;
        
        /**
         * Creates new pagination parameters with page size and number.
         * 
         * @param pageSize The page size
         * @param pageNumber The page number (0-based)
         */
        public PaginationParams(int pageSize, int pageNumber) {
            this.limit = pageSize;
            this.offset = pageNumber * pageSize;
        }
        
        /**
         * Creates new pagination parameters with limit and offset.
         * 
         * @param limit The maximum number of records to return
         * @param offset The number of records to skip
         * @param isOffset Flag indicating if offset is directly provided (true) or calculated from page number (false)
         */
        public PaginationParams(int limit, int offset, boolean isOffset) {
            this.limit = limit;
            if (isOffset) {
                this.offset = offset;
            } else {
                this.offset = offset * limit;
            }
        }
        
        /**
         * Validates the pagination parameters.
         * 
         * @throws IllegalArgumentException If the pagination parameters are invalid
         */
        public void validate() {
            if (limit < 1) {
                throw new IllegalArgumentException("Page size must be at least 1");
            }
            
            if (offset < 0) {
                throw new IllegalArgumentException("Offset cannot be negative");
            }
        }
        
        /**
         * Gets the limit (page size).
         * 
         * @return The limit
         */
        public int getLimit() {
            return limit;
        }
        
        /**
         * Gets the offset.
         * 
         * @return The offset
         */
        public int getOffset() {
            return offset;
        }
        
        /**
         * Gets the page number calculated from limit and offset.
         * 
         * @return The page number (0-based)
         */
        public int getPageNumber() {
            return limit > 0 ? offset / limit : 0;
        }
    }
}