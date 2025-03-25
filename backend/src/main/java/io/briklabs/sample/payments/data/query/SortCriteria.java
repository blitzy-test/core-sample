package io.briklabs.sample.payments.data.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class that defines and validates sorting parameters for payment transaction queries.
 * This class provides a structured way to represent sort order, direction, and multi-column
 * sorting operations with validation against allowed sort fields.
 * 
 * It handles the generation of SQL ORDER BY clauses with proper escaping and validation.
 */
public class SortCriteria {
    private static final Logger logger = LoggerFactory.getLogger(SortCriteria.class);
    
    /**
     * Enumeration of valid sort directions.
     */
    public enum SortDirection {
        ASC, DESC;
        
        /**
         * Parses a string into a SortDirection.
         * 
         * @param direction The direction string (case-insensitive)
         * @return The corresponding SortDirection, defaulting to ASC if invalid
         */
        public static SortDirection fromString(String direction) {
            if (direction == null || direction.isEmpty()) {
                return ASC;
            }
            
            try {
                return valueOf(direction.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid sort direction: {}, defaulting to ASC", direction);
                return ASC;
            }
        }
    }
    
    // Set of allowed sort fields for payment transactions
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
        "transaction_id", "organization_id", "account_id", "status", 
        "amount", "currency", "created_at", "updated_at", "merchant_id", 
        "payment_type", "transaction_reference", "description"
    ));
    
    // Default sort field if none specified
    private static final String DEFAULT_SORT_FIELD = "created_at";
    
    // Default sort direction if none specified
    private static final SortDirection DEFAULT_SORT_DIRECTION = SortDirection.DESC;
    
    private String field;
    private SortDirection direction;
    private int priority;
    
    /**
     * Creates a new sort criterion with the specified field and direction.
     * 
     * @param field The field to sort by
     * @param direction The sort direction
     * @throws IllegalArgumentException If the field is not allowed
     */
    public SortCriteria(String field, SortDirection direction) {
        this(field, direction, 0);
    }
    
    /**
     * Creates a new sort criterion with the specified field, direction, and priority.
     * 
     * @param field The field to sort by
     * @param direction The sort direction
     * @param priority The sort priority (lower values are applied first)
     * @throws IllegalArgumentException If the field is not allowed
     */
    public SortCriteria(String field, SortDirection direction, int priority) {
        validateField(field);
        this.field = field;
        this.direction = direction != null ? direction : DEFAULT_SORT_DIRECTION;
        this.priority = priority;
    }
    
    /**
     * Creates a new sort criterion with the specified field and direction string.
     * 
     * @param field The field to sort by
     * @param directionStr The sort direction as a string (ASC or DESC, case-insensitive)
     * @throws IllegalArgumentException If the field is not allowed
     */
    public SortCriteria(String field, String directionStr) {
        this(field, SortDirection.fromString(directionStr), 0);
    }
    
    /**
     * Creates a new sort criterion with the specified field, direction string, and priority.
     * 
     * @param field The field to sort by
     * @param directionStr The sort direction as a string (ASC or DESC, case-insensitive)
     * @param priority The sort priority (lower values are applied first)
     * @throws IllegalArgumentException If the field is not allowed
     */
    public SortCriteria(String field, String directionStr, int priority) {
        this(field, SortDirection.fromString(directionStr), priority);
    }
    
    /**
     * Validates that the field is allowed for sorting.
     * 
     * @param field The field to validate
     * @throws IllegalArgumentException If the field is not allowed
     */
    private void validateField(String field) {
        if (field == null || field.isEmpty()) {
            this.field = DEFAULT_SORT_FIELD;
            return;
        }
        
        // Check if the field is in the allowed list
        String normalizedField = normalizeField(field);
        if (!ALLOWED_SORT_FIELDS.contains(normalizedField)) {
            logger.warn("Invalid sort field: {}, defaulting to {}", field, DEFAULT_SORT_FIELD);
            this.field = DEFAULT_SORT_FIELD;
        } else {
            this.field = normalizedField;
        }
    }
    
    /**
     * Normalizes a field name by removing table aliases and ensuring proper format.
     * 
     * @param field The field name to normalize
     * @return The normalized field name
     */
    private String normalizeField(String field) {
        // Remove table alias if present (e.g., "t.created_at" -> "created_at")
        if (field.contains(".")) {
            return field.substring(field.lastIndexOf('.') + 1);
        }
        return field;
    }
    
    /**
     * Gets the sort field.
     * 
     * @return The sort field
     */
    public String getField() {
        return field;
    }
    
    /**
     * Gets the sort direction.
     * 
     * @return The sort direction
     */
    public SortDirection getDirection() {
        return direction;
    }
    
    /**
     * Gets the sort priority.
     * 
     * @return The sort priority
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Gets the column name with table alias for use in SQL queries.
     * 
     * @param tableAlias The table alias to use
     * @return The column name with table alias
     */
    public String getColumn() {
        return field;
    }
    
    /**
     * Gets the column name with table alias for use in SQL queries.
     * 
     * @param tableAlias The table alias to use
     * @return The column name with table alias
     */
    public String getColumn(String tableAlias) {
        if (tableAlias == null || tableAlias.isEmpty()) {
            return field;
        }
        return tableAlias + "." + field;
    }
    
    /**
     * Gets the SQL ORDER BY clause fragment for this sort criterion.
     * 
     * @return The SQL ORDER BY clause fragment
     */
    public String toSql() {
        return field + " " + direction.name();
    }
    
    /**
     * Gets the SQL ORDER BY clause fragment for this sort criterion with a table alias.
     * 
     * @param tableAlias The table alias to use
     * @return The SQL ORDER BY clause fragment
     */
    public String toSql(String tableAlias) {
        return getColumn(tableAlias) + " " + direction.name();
    }
    
    /**
     * Creates a default sort criterion for created_at in descending order.
     * 
     * @return A default sort criterion
     */
    public static SortCriteria createDefault() {
        return new SortCriteria(DEFAULT_SORT_FIELD, DEFAULT_SORT_DIRECTION);
    }
    
    /**
     * Creates a sort criterion for the specified field with ascending direction.
     * 
     * @param field The field to sort by
     * @return A sort criterion with ascending direction
     */
    public static SortCriteria ascending(String field) {
        return new SortCriteria(field, SortDirection.ASC);
    }
    
    /**
     * Creates a sort criterion for the specified field with descending direction.
     * 
     * @param field The field to sort by
     * @return A sort criterion with descending direction
     */
    public static SortCriteria descending(String field) {
        return new SortCriteria(field, SortDirection.DESC);
    }
    
    /**
     * Checks if a field is allowed for sorting.
     * 
     * @param field The field to check
     * @return true if the field is allowed, false otherwise
     */
    public static boolean isAllowedField(String field) {
        if (field == null || field.isEmpty()) {
            return false;
        }
        
        // Remove table alias if present
        String normalizedField = field;
        if (field.contains(".")) {
            normalizedField = field.substring(field.lastIndexOf('.') + 1);
        }
        
        return ALLOWED_SORT_FIELDS.contains(normalizedField);
    }
    
    /**
     * Gets the set of allowed sort fields.
     * 
     * @return The set of allowed sort fields
     */
    public static Set<String> getAllowedSortFields() {
        return new HashSet<>(ALLOWED_SORT_FIELDS);
    }
    
    @Override
    public String toString() {
        return field + " " + direction.name() + " (priority: " + priority + ")";
    }
}