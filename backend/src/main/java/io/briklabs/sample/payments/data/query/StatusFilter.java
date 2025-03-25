package io.briklabs.sample.payments.data.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.payments.model.PaymentStatus;

/**
 * A specialized filter component for handling status-based filtering in payment queries.
 * This class provides structured representation and validation of transaction status filters
 * with support for multiple status values, status groups, and state-based filtering.
 */
public class StatusFilter {
    private static final Logger logger = LoggerFactory.getLogger(StatusFilter.class);
    
    /**
     * Set of status values to filter by
     */
    private final Set<String> statusValues;
    
    /**
     * Flag indicating if this is a negation filter (exclude these statuses)
     */
    private final boolean negated;
    
    /**
     * Predefined status groups for common filtering scenarios
     */
    private static final Set<String> ACTIVE_STATUSES = Collections.unmodifiableSet(
            Arrays.stream(PaymentStatus.values())
                  .filter(PaymentStatus::isActiveState)
                  .map(PaymentStatus::name)
                  .collect(Collectors.toSet()));
    
    private static final Set<String> FINAL_STATUSES = Collections.unmodifiableSet(
            Arrays.stream(PaymentStatus.values())
                  .filter(PaymentStatus::isFinalState)
                  .map(PaymentStatus::name)
                  .collect(Collectors.toSet()));
    
    private static final Set<String> SUCCESSFUL_STATUSES = Collections.unmodifiableSet(
            Arrays.stream(PaymentStatus.values())
                  .filter(PaymentStatus::isSuccessful)
                  .map(PaymentStatus::name)
                  .collect(Collectors.toSet()));
    
    private static final Set<String> FAILED_STATUSES = Collections.unmodifiableSet(
            Arrays.stream(PaymentStatus.values())
                  .filter(PaymentStatus::isFailed)
                  .map(PaymentStatus::name)
                  .collect(Collectors.toSet()));
    
    /**
     * Creates a new status filter with a single status value.
     * 
     * @param status The status value to filter by
     */
    public StatusFilter(String status) {
        this(status, false);
    }
    
    /**
     * Creates a new status filter with a single status value and negation flag.
     * 
     * @param status The status value to filter by
     * @param negated If true, the filter will exclude this status rather than include it
     */
    public StatusFilter(String status, boolean negated) {
        this.statusValues = new HashSet<>();
        this.negated = negated;
        
        if (status != null && !status.trim().isEmpty()) {
            addStatusValue(status.trim());
        }
    }
    
    /**
     * Creates a new status filter with multiple status values.
     * 
     * @param statuses The collection of status values to filter by
     */
    public StatusFilter(Collection<String> statuses) {
        this(statuses, false);
    }
    
    /**
     * Creates a new status filter with multiple status values and negation flag.
     * 
     * @param statuses The collection of status values to filter by
     * @param negated If true, the filter will exclude these statuses rather than include them
     */
    public StatusFilter(Collection<String> statuses, boolean negated) {
        this.statusValues = new HashSet<>();
        this.negated = negated;
        
        if (statuses != null) {
            for (String status : statuses) {
                if (status != null && !status.trim().isEmpty()) {
                    addStatusValue(status.trim());
                }
            }
        }
    }
    
    /**
     * Creates a new status filter for a predefined status group.
     * 
     * @param statusGroup The name of the status group ("ACTIVE", "FINAL", "SUCCESSFUL", "FAILED")
     * @return A new status filter for the specified group
     * @throws IllegalArgumentException If the status group name is invalid
     */
    public static StatusFilter forStatusGroup(String statusGroup) {
        return forStatusGroup(statusGroup, false);
    }
    
    /**
     * Creates a new status filter for a predefined status group with negation option.
     * 
     * @param statusGroup The name of the status group ("ACTIVE", "FINAL", "SUCCESSFUL", "FAILED")
     * @param negated If true, the filter will exclude these statuses rather than include them
     * @return A new status filter for the specified group
     * @throws IllegalArgumentException If the status group name is invalid
     */
    public static StatusFilter forStatusGroup(String statusGroup, boolean negated) {
        if (statusGroup == null || statusGroup.trim().isEmpty()) {
            throw new IllegalArgumentException("Status group name cannot be null or empty");
        }
        
        String groupName = statusGroup.trim().toUpperCase();
        Set<String> statuses;
        
        switch (groupName) {
            case "ACTIVE":
                statuses = ACTIVE_STATUSES;
                break;
            case "FINAL":
                statuses = FINAL_STATUSES;
                break;
            case "SUCCESSFUL":
                statuses = SUCCESSFUL_STATUSES;
                break;
            case "FAILED":
                statuses = FAILED_STATUSES;
                break;
            default:
                throw new IllegalArgumentException("Invalid status group name: " + statusGroup);
        }
        
        return new StatusFilter(statuses, negated);
    }
    
    /**
     * Adds a status value to this filter.
     * 
     * @param status The status value to add
     */
    private void addStatusValue(String status) {
        // Handle special status group names
        if (status.startsWith("@")) {
            String groupName = status.substring(1).toUpperCase();
            Set<String> groupStatuses = null;
            
            switch (groupName) {
                case "ACTIVE":
                    groupStatuses = ACTIVE_STATUSES;
                    break;
                case "FINAL":
                    groupStatuses = FINAL_STATUSES;
                    break;
                case "SUCCESSFUL":
                    groupStatuses = SUCCESSFUL_STATUSES;
                    break;
                case "FAILED":
                    groupStatuses = FAILED_STATUSES;
                    break;
                default:
                    logger.warn("Unknown status group: {}", groupName);
                    break;
            }
            
            if (groupStatuses != null) {
                statusValues.addAll(groupStatuses);
            }
        } else {
            // Handle individual status values
            try {
                // Validate that the status is a valid PaymentStatus enum value
                PaymentStatus.valueOf(status.toUpperCase());
                statusValues.add(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status value: {}", status);
                // Don't add invalid status values
            }
        }
    }
    
    /**
     * Validates that all status values in this filter are valid.
     * 
     * @throws IllegalArgumentException If any status value is invalid
     */
    public void validate() {
        if (statusValues.isEmpty()) {
            return; // Empty filter is valid (no filtering)
        }
        
        Set<String> invalidStatuses = new HashSet<>();
        
        for (String status : statusValues) {
            try {
                PaymentStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                invalidStatuses.add(status);
            }
        }
        
        if (!invalidStatuses.isEmpty()) {
            throw new IllegalArgumentException("Invalid status values: " + String.join(", ", invalidStatuses));
        }
    }
    
    /**
     * Generates a SQL condition for this status filter.
     * 
     * @param columnName The name of the status column in the SQL query
     * @return A SQL condition string, or null if no filtering should be applied
     */
    public String toSqlCondition(String columnName) {
        if (statusValues.isEmpty()) {
            return null; // No filtering
        }
        
        StringBuilder condition = new StringBuilder();
        
        if (statusValues.size() == 1) {
            // Single status value
            String status = statusValues.iterator().next();
            condition.append(columnName)
                    .append(negated ? " <> '" : " = '")
                    .append(status)
                    .append("'");
        } else {
            // Multiple status values
            condition.append(columnName)
                    .append(negated ? " NOT IN (" : " IN (");
            
            boolean first = true;
            for (String status : statusValues) {
                if (!first) {
                    condition.append(", ");
                }
                condition.append("'").append(status).append("'");
                first = false;
            }
            
            condition.append(")");
        }
        
        return condition.toString();
    }
    
    /**
     * Generates SQL parameters for this status filter.
     * 
     * @return An array of status values as parameters, or an empty array if no filtering
     */
    public Object[] toSqlParameters() {
        if (statusValues.isEmpty()) {
            return new Object[0];
        }
        
        return statusValues.toArray();
    }
    
    /**
     * Gets the status values in this filter.
     * 
     * @return The set of status values
     */
    public Set<String> getStatusValues() {
        return Collections.unmodifiableSet(statusValues);
    }
    
    /**
     * Checks if this is a negation filter.
     * 
     * @return true if this filter excludes the specified statuses, false if it includes them
     */
    public boolean isNegated() {
        return negated;
    }
    
    /**
     * Checks if this filter is empty (no status values).
     * 
     * @return true if this filter has no status values, false otherwise
     */
    public boolean isEmpty() {
        return statusValues.isEmpty();
    }
    
    /**
     * Creates a negated version of this filter.
     * 
     * @return A new status filter with the same status values but opposite negation
     */
    public StatusFilter negate() {
        return new StatusFilter(statusValues, !negated);
    }
    
    @Override
    public String toString() {
        if (statusValues.isEmpty()) {
            return "StatusFilter[empty]";
        }
        
        return "StatusFilter[" + (negated ? "NOT " : "") + 
               String.join(", ", statusValues) + "]";
    }
}