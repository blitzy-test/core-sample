package io.briklabs.sample.payments.data.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

/**
 * A specialized filter component for handling status-based filtering in payment queries.
 * This class provides structured representation and validation of transaction status filters
 * with support for multiple status values, status groups, and state-based filtering.
 * 
 * It handles the validation of status values against the allowed transaction states and
 * generates appropriate SQL conditions for status filtering.
 */
public class StatusFilter {
    private static final Logger logger = LoggerFactory.getLogger(StatusFilter.class);
    
    /**
     * Predefined status groups for common filtering scenarios.
     */
    public enum StatusGroup {
        /**
         * Active transactions that are in progress (not in terminal state).
         */
        ACTIVE(PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.AUTHORIZED, 
               PaymentStatus.PARTIALLY_CAPTURED, PaymentStatus.CAPTURED, PaymentStatus.PARTIALLY_REFUNDED),
        
        /**
         * Completed transactions that have reached a terminal state.
         */
        COMPLETED(PaymentStatus.CAPTURED, PaymentStatus.REFUNDED),
        
        /**
         * Failed transactions that did not complete successfully.
         */
        FAILED(PaymentStatus.FAILED, PaymentStatus.DECLINED, PaymentStatus.VOIDED),
        
        /**
         * Transactions that have been authorized but not yet fully captured.
         */
        AUTHORIZED(PaymentStatus.AUTHORIZED, PaymentStatus.PARTIALLY_CAPTURED),
        
        /**
         * Transactions that have been captured (fully or partially).
         */
        CAPTURED(PaymentStatus.CAPTURED, PaymentStatus.PARTIALLY_CAPTURED),
        
        /**
         * Transactions that have been refunded (fully or partially).
         */
        REFUNDED(PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED),
        
        /**
         * Transactions that are in a partial state (partially captured or refunded).
         */
        PARTIAL(PaymentStatus.PARTIALLY_CAPTURED, PaymentStatus.PARTIALLY_REFUNDED);
        
        private final Set<PaymentStatus> statuses;
        
        StatusGroup(PaymentStatus... statuses) {
            this.statuses = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(statuses)));
        }
        
        /**
         * Gets the set of payment statuses in this group.
         * 
         * @return An unmodifiable set of payment statuses
         */
        public Set<PaymentStatus> getStatuses() {
            return statuses;
        }
    }
    
    private final Set<String> statuses;
    private boolean negated;
    
    /**
     * Creates a new empty status filter.
     */
    public StatusFilter() {
        this.statuses = new HashSet<>();
        this.negated = false;
    }
    
    /**
     * Creates a new status filter with the specified statuses.
     * 
     * @param statuses The status values to include in the filter
     */
    public StatusFilter(Collection<String> statuses) {
        this.statuses = new HashSet<>();
        if (statuses != null) {
            this.statuses.addAll(statuses);
        }
        this.negated = false;
    }
    
    /**
     * Creates a new status filter with the specified status group.
     * 
     * @param group The status group to include in the filter
     */
    public StatusFilter(StatusGroup group) {
        this.statuses = new HashSet<>();
        if (group != null) {
            this.statuses.addAll(group.getStatuses().stream()
                    .map(PaymentStatus::name)
                    .collect(Collectors.toSet()));
        }
        this.negated = false;
    }
    
    /**
     * Adds a status value to the filter.
     * 
     * @param status The status value to add
     * @return This filter for method chaining
     * @throws IllegalArgumentException if the status is not a valid PaymentStatus
     */
    public StatusFilter addStatus(String status) {
        if (status == null || status.isEmpty()) {
            return this;
        }
        
        // Validate that the status is a valid PaymentStatus
        try {
            PaymentStatus.valueOf(status);
            statuses.add(status);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid payment status: {}", status);
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }
        
        return this;
    }
    
    /**
     * Adds multiple status values to the filter.
     * 
     * @param statusValues The status values to add
     * @return This filter for method chaining
     * @throws IllegalArgumentException if any status is not a valid PaymentStatus
     */
    public StatusFilter addStatuses(String... statusValues) {
        if (statusValues == null) {
            return this;
        }
        
        for (String status : statusValues) {
            addStatus(status);
        }
        
        return this;
    }
    
    /**
     * Adds multiple status values to the filter.
     * 
     * @param statusValues The status values to add
     * @return This filter for method chaining
     * @throws IllegalArgumentException if any status is not a valid PaymentStatus
     */
    public StatusFilter addStatuses(Collection<String> statusValues) {
        if (statusValues == null) {
            return this;
        }
        
        for (String status : statusValues) {
            addStatus(status);
        }
        
        return this;
    }
    
    /**
     * Adds a PaymentStatus enum value to the filter.
     * 
     * @param status The PaymentStatus enum value to add
     * @return This filter for method chaining
     */
    public StatusFilter addStatus(PaymentStatus status) {
        if (status == null) {
            return this;
        }
        
        statuses.add(status.name());
        return this;
    }
    
    /**
     * Adds multiple PaymentStatus enum values to the filter.
     * 
     * @param statusValues The PaymentStatus enum values to add
     * @return This filter for method chaining
     */
    public StatusFilter addStatuses(PaymentStatus... statusValues) {
        if (statusValues == null) {
            return this;
        }
        
        for (PaymentStatus status : statusValues) {
            addStatus(status);
        }
        
        return this;
    }
    
    /**
     * Adds all statuses from a status group to the filter.
     * 
     * @param group The status group to add
     * @return This filter for method chaining
     */
    public StatusFilter addStatusGroup(StatusGroup group) {
        if (group == null) {
            return this;
        }
        
        for (PaymentStatus status : group.getStatuses()) {
            addStatus(status);
        }
        
        return this;
    }
    
    /**
     * Sets whether this filter should be negated (exclude the specified statuses).
     * 
     * @param negated true to negate the filter, false otherwise
     * @return This filter for method chaining
     */
    public StatusFilter setNegated(boolean negated) {
        this.negated = negated;
        return this;
    }
    
    /**
     * Negates this filter (exclude the specified statuses).
     * 
     * @return This filter for method chaining
     */
    public StatusFilter negate() {
        this.negated = true;
        return this;
    }
    
    /**
     * Checks if this filter is negated.
     * 
     * @return true if the filter is negated, false otherwise
     */
    public boolean isNegated() {
        return negated;
    }
    
    /**
     * Gets the set of status values in this filter.
     * 
     * @return An unmodifiable set of status values
     */
    public Set<String> getStatuses() {
        return Collections.unmodifiableSet(statuses);
    }
    
    /**
     * Checks if this filter is empty (no status values).
     * 
     * @return true if the filter is empty, false otherwise
     */
    public boolean isEmpty() {
        return statuses.isEmpty();
    }
    
    /**
     * Clears all status values from this filter.
     * 
     * @return This filter for method chaining
     */
    public StatusFilter clear() {
        statuses.clear();
        return this;
    }
    
    /**
     * Generates an SQL condition for this status filter.
     * 
     * @param columnName The name of the status column in the database
     * @return An SQL condition string, or null if the filter is empty
     */
    public String toSqlCondition(String columnName) {
        if (isEmpty()) {
            return null;
        }
        
        StringBuilder condition = new StringBuilder();
        
        if (negated) {
            condition.append(columnName).append(" NOT IN (");
        } else {
            condition.append(columnName).append(" IN (");
        }
        
        List<String> quotedStatuses = new ArrayList<>();
        for (String status : statuses) {
            quotedStatuses.add("'" + status + "'");
        }
        
        condition.append(String.join(", ", quotedStatuses));
        condition.append(")");
        
        return condition.toString();
    }
    
    /**
     * Applies this status filter to a PaymentQueryBuilder.
     * 
     * @param queryBuilder The query builder to apply the filter to
     * @param columnName The name of the status column in the database
     * @return The query builder with the filter applied
     */
    public PaymentQueryBuilder applyToQuery(PaymentQueryBuilder queryBuilder, String columnName) {
        if (isEmpty() || queryBuilder == null) {
            return queryBuilder;
        }
        
        if (negated) {
            queryBuilder.and(columnName + " NOT IN (" + 
                    statuses.stream().map(s -> "?").collect(Collectors.joining(", ")) + ")");
        } else {
            queryBuilder.and(columnName + " IN (" + 
                    statuses.stream().map(s -> "?").collect(Collectors.joining(", ")) + ")");
        }
        
        for (String status : statuses) {
            queryBuilder.addParameter(status);
        }
        
        return queryBuilder;
    }
    
    /**
     * Creates a new status filter with the specified statuses.
     * 
     * @param statuses The status values to include in the filter
     * @return A new status filter
     */
    public static StatusFilter of(String... statuses) {
        return new StatusFilter().addStatuses(statuses);
    }
    
    /**
     * Creates a new status filter with the specified PaymentStatus values.
     * 
     * @param statuses The PaymentStatus values to include in the filter
     * @return A new status filter
     */
    public static StatusFilter of(PaymentStatus... statuses) {
        return new StatusFilter().addStatuses(statuses);
    }
    
    /**
     * Creates a new status filter with the specified status group.
     * 
     * @param group The status group to include in the filter
     * @return A new status filter
     */
    public static StatusFilter ofGroup(StatusGroup group) {
        return new StatusFilter(group);
    }
    
    /**
     * Creates a new negated status filter with the specified statuses.
     * 
     * @param statuses The status values to exclude from the filter
     * @return A new negated status filter
     */
    public static StatusFilter not(String... statuses) {
        return new StatusFilter().addStatuses(statuses).negate();
    }
    
    /**
     * Creates a new negated status filter with the specified PaymentStatus values.
     * 
     * @param statuses The PaymentStatus values to exclude from the filter
     * @return A new negated status filter
     */
    public static StatusFilter not(PaymentStatus... statuses) {
        return new StatusFilter().addStatuses(statuses).negate();
    }
    
    /**
     * Creates a new negated status filter with the specified status group.
     * 
     * @param group The status group to exclude from the filter
     * @return A new negated status filter
     */
    public static StatusFilter notGroup(StatusGroup group) {
        return new StatusFilter(group).negate();
    }
    
    @Override
    public String toString() {
        return "StatusFilter{" +
                "statuses=" + statuses +
                ", negated=" + negated +
                '}';
    }
}