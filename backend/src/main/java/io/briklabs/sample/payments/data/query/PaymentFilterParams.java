package io.briklabs.sample.payments.data.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.payments.model.PaymentStatus;

/**
 * A data transfer object that encapsulates all possible filtering parameters for payment transaction queries.
 * This class handles parameter validation, type conversion, and default values for search criteria like
 * date ranges, amount ranges, status filters, and merchant identifiers.
 * 
 * It serves as a standardized container for query parameters that can be passed between layers of the application,
 * ensuring consistent handling of complex filtering scenarios required for payment transaction management.
 */
public class PaymentFilterParams {
    private static final Logger logger = LoggerFactory.getLogger(PaymentFilterParams.class);
    
    // Organization and account filters
    private UUID organizationId;
    private UUID accountId;
    
    // Date range filter
    private DateRangeFilter dateRange;
    
    // Amount range filter
    private AmountRangeFilter amountRange;
    
    // Status filter
    private StatusFilter statusFilter;
    
    // Merchant and payment type filters
    private String merchantId;
    private String paymentType;
    
    // Text search
    private String searchTerm;
    
    // Sorting criteria
    private List<SortCriteria> sortCriteria;
    
    // Pagination parameters
    private PaginationParams pagination;
    
    /**
     * Creates a new empty filter parameters object with default pagination.
     */
    public PaymentFilterParams() {
        this.sortCriteria = new ArrayList<>();
        this.pagination = PaginationParams.getDefault();
    }
    
    /**
     * Creates a new filter parameters object with the specified organization ID.
     * 
     * @param organizationId The organization ID to filter by
     */
    public PaymentFilterParams(UUID organizationId) {
        this();
        this.organizationId = organizationId;
    }
    
    /**
     * Creates a new filter parameters object with the specified organization and account IDs.
     * 
     * @param organizationId The organization ID to filter by
     * @param accountId The account ID to filter by
     */
    public PaymentFilterParams(UUID organizationId, UUID accountId) {
        this(organizationId);
        this.accountId = accountId;
    }
    
    /**
     * Validates all filter parameters for consistency and correctness.
     * 
     * @throws IllegalArgumentException If any filter parameter is invalid
     */
    public void validate() {
        // Validate date range if present
        if (dateRange != null) {
            try {
                dateRange.validate();
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid date range: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid date range: " + e.getMessage());
            }
        }
        
        // Validate amount range if present
        if (amountRange != null) {
            try {
                amountRange.validate();
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid amount range: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid amount range: " + e.getMessage());
            }
        }
        
        // Validate status filter if present
        if (statusFilter != null) {
            try {
                statusFilter.validate();
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid status filter: " + e.getMessage());
            }
        }
        
        // Validate pagination parameters
        if (pagination != null) {
            try {
                pagination.validate();
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid pagination parameters: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid pagination parameters: " + e.getMessage());
            }
        }
        
        // Validate sort criteria
        if (sortCriteria != null && !sortCriteria.isEmpty()) {
            for (SortCriteria criteria : sortCriteria) {
                if (!SortCriteria.isAllowedField(criteria.getField())) {
                    logger.warn("Invalid sort field: {}", criteria.getField());
                    throw new IllegalArgumentException("Invalid sort field: " + criteria.getField());
                }
            }
        }
    }
    
    /**
     * Applies these filter parameters to a query builder.
     * 
     * @param queryBuilder The query builder to apply the filters to
     * @return The updated query builder
     */
    public PaymentQueryBuilder applyTo(PaymentQueryBuilder queryBuilder) {
        return queryBuilder.applyFilters(this);
    }
    
    /**
     * Creates a new filter parameters object from HTTP request parameters.
     * 
     * @param params A map of parameter names to values
     * @return A new filter parameters object
     */
    public static PaymentFilterParams fromRequestParameters(java.util.Map<String, String[]> params) {
        PaymentFilterParams filters = new PaymentFilterParams();
        
        // Process organization and account IDs
        String orgId = getFirstValue(params, "organizationId");
        if (orgId != null && !orgId.isEmpty()) {
            try {
                filters.setOrganizationId(UUID.fromString(orgId));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid organization ID format: {}", orgId);
            }
        }
        
        String acctId = getFirstValue(params, "accountId");
        if (acctId != null && !acctId.isEmpty()) {
            try {
                filters.setAccountId(UUID.fromString(acctId));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid account ID format: {}", acctId);
            }
        }
        
        // Process date range
        String startDate = getFirstValue(params, "startDate");
        String endDate = getFirstValue(params, "endDate");
        String dateRange = getFirstValue(params, "dateRange");
        
        if ((startDate != null && !startDate.isEmpty()) || (endDate != null && !endDate.isEmpty())) {
            filters.setDateRange(new DateRangeFilter(startDate, endDate));
        } else if (dateRange != null && !dateRange.isEmpty()) {
            filters.setDateRange(new DateRangeFilter(dateRange));
        }
        
        // Process amount range
        String minAmount = getFirstValue(params, "minAmount");
        String maxAmount = getFirstValue(params, "maxAmount");
        String currency = getFirstValue(params, "currency");
        
        if ((minAmount != null && !minAmount.isEmpty()) || (maxAmount != null && !maxAmount.isEmpty())) {
            filters.setAmountRange(new AmountRangeFilter(minAmount, maxAmount, currency));
        }
        
        // Process status filter
        String[] statuses = params.get("status");
        if (statuses != null && statuses.length > 0) {
            filters.setStatusFilter(new StatusFilter(Arrays.asList(statuses)));
        }
        
        String statusGroup = getFirstValue(params, "statusGroup");
        if (statusGroup != null && !statusGroup.isEmpty()) {
            filters.setStatusFilter(StatusFilter.forStatusGroup(statusGroup));
        }
        
        // Process merchant and payment type
        String merchantId = getFirstValue(params, "merchantId");
        if (merchantId != null && !merchantId.isEmpty()) {
            filters.setMerchantId(merchantId);
        }
        
        String paymentType = getFirstValue(params, "paymentType");
        if (paymentType != null && !paymentType.isEmpty()) {
            filters.setPaymentType(paymentType);
        }
        
        // Process search term
        String searchTerm = getFirstValue(params, "search");
        if (searchTerm != null && !searchTerm.isEmpty()) {
            filters.setSearchTerm(searchTerm);
        }
        
        // Process sorting
        String sortField = getFirstValue(params, "sortField");
        String sortDirection = getFirstValue(params, "sortDirection");
        
        if (sortField != null && !sortField.isEmpty()) {
            filters.addSortCriteria(new SortCriteria(sortField, sortDirection));
        } else {
            // Default sort by created_at descending
            filters.addSortCriteria(SortCriteria.descending("created_at"));
        }
        
        // Process pagination
        String pageSize = getFirstValue(params, "pageSize");
        String pageNumber = getFirstValue(params, "pageNumber");
        String limit = getFirstValue(params, "limit");
        String offset = getFirstValue(params, "offset");
        
        if ((pageSize != null && !pageSize.isEmpty()) || (pageNumber != null && !pageNumber.isEmpty())) {
            filters.setPagination(PaginationParams.fromPageParams(pageSize, pageNumber));
        } else if ((limit != null && !limit.isEmpty()) || (offset != null && !offset.isEmpty())) {
            filters.setPagination(PaginationParams.fromLimitOffset(limit, offset));
        }
        
        return filters;
    }
    
    /**
     * Gets the first value from a parameter map.
     * 
     * @param params The parameter map
     * @param name The parameter name
     * @return The first value, or null if not present
     */
    private static String getFirstValue(java.util.Map<String, String[]> params, String name) {
        String[] values = params.get(name);
        return (values != null && values.length > 0) ? values[0] : null;
    }
    
    /**
     * Creates a filter for a specific organization.
     * 
     * @param organizationId The organization ID to filter by
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forOrganization(UUID organizationId) {
        return new PaymentFilterParams(organizationId);
    }
    
    /**
     * Creates a filter for a specific account within an organization.
     * 
     * @param organizationId The organization ID to filter by
     * @param accountId The account ID to filter by
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forAccount(UUID organizationId, UUID accountId) {
        return new PaymentFilterParams(organizationId, accountId);
    }
    
    /**
     * Creates a filter for transactions created within a date range.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        PaymentFilterParams filters = new PaymentFilterParams();
        filters.setDateRange(new DateRangeFilter(startDate, endDate));
        return filters;
    }
    
    /**
     * Creates a filter for transactions with a specific status.
     * 
     * @param status The status to filter by
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forStatus(PaymentStatus status) {
        PaymentFilterParams filters = new PaymentFilterParams();
        filters.setStatusFilter(new StatusFilter(status.name()));
        return filters;
    }
    
    /**
     * Creates a filter for transactions with any of the specified statuses.
     * 
     * @param statuses The statuses to filter by
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forStatuses(PaymentStatus... statuses) {
        PaymentFilterParams filters = new PaymentFilterParams();
        Set<String> statusNames = new HashSet<>();
        for (PaymentStatus status : statuses) {
            statusNames.add(status.name());
        }
        filters.setStatusFilter(new StatusFilter(statusNames));
        return filters;
    }
    
    /**
     * Creates a filter for transactions from a specific merchant.
     * 
     * @param merchantId The merchant ID to filter by
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forMerchant(String merchantId) {
        PaymentFilterParams filters = new PaymentFilterParams();
        filters.setMerchantId(merchantId);
        return filters;
    }
    
    /**
     * Creates a filter for transactions with a specific payment type.
     * 
     * @param paymentType The payment type to filter by
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forPaymentType(String paymentType) {
        PaymentFilterParams filters = new PaymentFilterParams();
        filters.setPaymentType(paymentType);
        return filters;
    }
    
    /**
     * Creates a filter for transactions with amounts in a specific range.
     * 
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @param currency The currency code (optional)
     * @return A new filter parameters object
     */
    public static PaymentFilterParams forAmountRange(BigDecimal minAmount, BigDecimal maxAmount, String currency) {
        PaymentFilterParams filters = new PaymentFilterParams();
        filters.setAmountRange(new AmountRangeFilter(minAmount, maxAmount, currency));
        return filters;
    }
    
    /**
     * Gets the organization ID filter.
     * 
     * @return The organization ID, or null if not set
     */
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    /**
     * Sets the organization ID filter.
     * 
     * @param organizationId The organization ID to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        return this;
    }
    
    /**
     * Gets the account ID filter.
     * 
     * @return The account ID, or null if not set
     */
    public UUID getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account ID filter.
     * 
     * @param accountId The account ID to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams setAccountId(UUID accountId) {
        this.accountId = accountId;
        return this;
    }
    
    /**
     * Gets the date range filter.
     * 
     * @return The date range filter, or null if not set
     */
    public DateRangeFilter getDateRange() {
        return dateRange;
    }
    
    /**
     * Sets the date range filter.
     * 
     * @param dateRange The date range filter to set
     * @return This object for method chaining
     */
    public PaymentFilterParams setDateRange(DateRangeFilter dateRange) {
        this.dateRange = dateRange;
        return this;
    }
    
    /**
     * Sets the date range filter using explicit start and end dates.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return This object for method chaining
     */
    public PaymentFilterParams setDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        this.dateRange = new DateRangeFilter(startDate, endDate);
        return this;
    }
    
    /**
     * Sets the date range filter using a relative date expression.
     * 
     * @param expression The relative date expression (e.g., "last 30 days", "this month")
     * @return This object for method chaining
     */
    public PaymentFilterParams setDateRange(String expression) {
        this.dateRange = new DateRangeFilter(expression);
        return this;
    }
    
    /**
     * Gets the amount range filter.
     * 
     * @return The amount range filter, or null if not set
     */
    public AmountRangeFilter getAmountRange() {
        return amountRange;
    }
    
    /**
     * Sets the amount range filter.
     * 
     * @param amountRange The amount range filter to set
     * @return This object for method chaining
     */
    public PaymentFilterParams setAmountRange(AmountRangeFilter amountRange) {
        this.amountRange = amountRange;
        return this;
    }
    
    /**
     * Sets the amount range filter using explicit minimum and maximum amounts.
     * 
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @param currency The currency code (optional)
     * @return This object for method chaining
     */
    public PaymentFilterParams setAmountRange(BigDecimal minAmount, BigDecimal maxAmount, String currency) {
        this.amountRange = new AmountRangeFilter(minAmount, maxAmount, currency);
        return this;
    }
    
    /**
     * Gets the status filter.
     * 
     * @return The status filter, or null if not set
     */
    public StatusFilter getStatusFilter() {
        return statusFilter;
    }
    
    /**
     * Sets the status filter.
     * 
     * @param statusFilter The status filter to set
     * @return This object for method chaining
     */
    public PaymentFilterParams setStatusFilter(StatusFilter statusFilter) {
        this.statusFilter = statusFilter;
        return this;
    }
    
    /**
     * Sets the status filter using a single status value.
     * 
     * @param status The status to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams setStatus(PaymentStatus status) {
        if (status != null) {
            this.statusFilter = new StatusFilter(status.name());
        }
        return this;
    }
    
    /**
     * Sets the status filter using multiple status values.
     * 
     * @param statuses The statuses to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams setStatuses(PaymentStatus... statuses) {
        if (statuses != null && statuses.length > 0) {
            Set<String> statusNames = new HashSet<>();
            for (PaymentStatus status : statuses) {
                statusNames.add(status.name());
            }
            this.statusFilter = new StatusFilter(statusNames);
        }
        return this;
    }
    
    /**
     * Gets the merchant ID filter.
     * 
     * @return The merchant ID, or null if not set
     */
    public String getMerchantId() {
        return merchantId;
    }
    
    /**
     * Sets the merchant ID filter.
     * 
     * @param merchantId The merchant ID to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams setMerchantId(String merchantId) {
        this.merchantId = merchantId;
        return this;
    }
    
    /**
     * Gets the payment type filter.
     * 
     * @return The payment type, or null if not set
     */
    public String getPaymentType() {
        return paymentType;
    }
    
    /**
     * Sets the payment type filter.
     * 
     * @param paymentType The payment type to filter by
     * @return This object for method chaining
     */
    public PaymentFilterParams setPaymentType(String paymentType) {
        this.paymentType = paymentType;
        return this;
    }
    
    /**
     * Gets the search term for text search.
     * 
     * @return The search term, or null if not set
     */
    public String getSearchTerm() {
        return searchTerm;
    }
    
    /**
     * Sets the search term for text search.
     * 
     * @param searchTerm The search term to set
     * @return This object for method chaining
     */
    public PaymentFilterParams setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
        return this;
    }
    
    /**
     * Gets the sort criteria.
     * 
     * @return The list of sort criteria
     */
    public List<SortCriteria> getSortCriteria() {
        return Collections.unmodifiableList(sortCriteria);
    }
    
    /**
     * Sets the sort criteria.
     * 
     * @param sortCriteria The sort criteria to set
     * @return This object for method chaining
     */
    public PaymentFilterParams setSortCriteria(List<SortCriteria> sortCriteria) {
        this.sortCriteria = new ArrayList<>(sortCriteria);
        return this;
    }
    
    /**
     * Adds a sort criterion.
     * 
     * @param criteria The sort criterion to add
     * @return This object for method chaining
     */
    public PaymentFilterParams addSortCriteria(SortCriteria criteria) {
        if (criteria != null) {
            this.sortCriteria.add(criteria);
        }
        return this;
    }
    
    /**
     * Adds a sort criterion with the specified field and direction.
     * 
     * @param field The field to sort by
     * @param direction The sort direction
     * @return This object for method chaining
     */
    public PaymentFilterParams addSortCriteria(String field, SortCriteria.SortDirection direction) {
        if (field != null && !field.isEmpty()) {
            this.sortCriteria.add(new SortCriteria(field, direction));
        }
        return this;
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
     * Sets the pagination parameters.
     * 
     * @param pagination The pagination parameters to set
     * @return This object for method chaining
     */
    public PaymentFilterParams setPagination(PaginationParams pagination) {
        this.pagination = pagination != null ? pagination : PaginationParams.getDefault();
        return this;
    }
    
    /**
     * Sets the pagination parameters using page size and number.
     * 
     * @param pageSize The page size
     * @param pageNumber The page number (0-based)
     * @return This object for method chaining
     */
    public PaymentFilterParams setPagination(int pageSize, int pageNumber) {
        this.pagination = new PaginationParams(pageSize, pageNumber);
        return this;
    }
    
    /**
     * Sets the pagination parameters using limit and offset.
     * 
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return This object for method chaining
     */
    public PaymentFilterParams setPaginationLimitOffset(int limit, int offset) {
        this.pagination = PaginationParams.ofLimitOffset(limit, offset);
        return this;
    }
    
    /**
     * Checks if this filter has any constraints.
     * 
     * @return true if any filter parameter is set, false if all are null/empty
     */
    public boolean hasConstraints() {
        return organizationId != null ||
               accountId != null ||
               (dateRange != null && dateRange.hasConstraints()) ||
               (amountRange != null && amountRange.hasConstraints()) ||
               (statusFilter != null && !statusFilter.isEmpty()) ||
               (merchantId != null && !merchantId.isEmpty()) ||
               (paymentType != null && !paymentType.isEmpty()) ||
               (searchTerm != null && !searchTerm.isEmpty());
    }
    
    /**
     * Creates a copy of this filter parameters object.
     * 
     * @return A new filter parameters object with the same values
     */
    public PaymentFilterParams copy() {
        PaymentFilterParams copy = new PaymentFilterParams();
        copy.organizationId = this.organizationId;
        copy.accountId = this.accountId;
        copy.dateRange = this.dateRange;
        copy.amountRange = this.amountRange;
        copy.statusFilter = this.statusFilter;
        copy.merchantId = this.merchantId;
        copy.paymentType = this.paymentType;
        copy.searchTerm = this.searchTerm;
        copy.sortCriteria = new ArrayList<>(this.sortCriteria);
        copy.pagination = this.pagination;
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PaymentFilterParams{");
        
        if (organizationId != null) {
            sb.append("organizationId=").append(organizationId);
        }
        
        if (accountId != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("accountId=").append(accountId);
        }
        
        if (dateRange != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("dateRange=").append(dateRange);
        }
        
        if (amountRange != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("amountRange=").append(amountRange);
        }
        
        if (statusFilter != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("statusFilter=").append(statusFilter);
        }
        
        if (merchantId != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("merchantId=").append(merchantId);
        }
        
        if (paymentType != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("paymentType=").append(paymentType);
        }
        
        if (searchTerm != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("searchTerm=").append(searchTerm);
        }
        
        if (!sortCriteria.isEmpty()) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("sortCriteria=").append(sortCriteria);
        }
        
        if (pagination != null) {
            if (sb.length() > 21) sb.append(", ");
            sb.append("pagination=").append(pagination);
        }
        
        sb.append('}');
        return sb.toString();
    }
}