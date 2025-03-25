package io.briklabs.sample.payments.data.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines and validates sorting parameters for payment transaction queries.
 * This class provides a structured way to represent sort order, direction, and
 * multi-column sorting operations with validation against allowed sort fields.
 * It handles the generation of SQL ORDER BY clauses with proper escaping and validation.
 */
public class SortCriteria {

    /**
     * Enumeration of sort directions.
     */
    public enum SortDirection {
        ASC("ASC"),
        DESC("DESC");

        private final String sqlValue;

        SortDirection(String sqlValue) {
            this.sqlValue = sqlValue;
        }

        public String getSqlValue() {
            return sqlValue;
        }

        /**
         * Parse a string value to a SortDirection enum.
         * 
         * @param value The string value to parse
         * @return The corresponding SortDirection enum value
         */
        public static SortDirection fromString(String value) {
            if (value == null) {
                return SortDirection.DESC; // Default to descending
            }
            
            String upperValue = value.trim().toUpperCase();
            if ("ASC".equals(upperValue) || "ASCENDING".equals(upperValue)) {
                return SortDirection.ASC;
            } else if ("DESC".equals(upperValue) || "DESCENDING".equals(upperValue)) {
                return SortDirection.DESC;
            } else {
                return SortDirection.DESC; // Default to descending for invalid values
            }
        }
    }

    /**
     * Represents a single sort field with its direction.
     */
    public static class SortField {
        private final String field;
        private final SortDirection direction;
        private final int priority;

        /**
         * Creates a new SortField with the specified field name and direction.
         * 
         * @param field The field name to sort by
         * @param direction The sort direction
         * @param priority The priority of this sort field (lower values have higher priority)
         */
        public SortField(String field, SortDirection direction, int priority) {
            this.field = field;
            this.direction = direction;
            this.priority = priority;
        }

        /**
         * Creates a new SortField with the specified field name and direction.
         * 
         * @param field The field name to sort by
         * @param direction The sort direction
         */
        public SortField(String field, SortDirection direction) {
            this(field, direction, 0);
        }

        /**
         * Creates a new SortField with the specified field name and default descending direction.
         * 
         * @param field The field name to sort by
         */
        public SortField(String field) {
            this(field, SortDirection.DESC, 0);
        }

        public String getField() {
            return field;
        }

        public SortDirection getDirection() {
            return direction;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SortField sortField = (SortField) o;
            return priority == sortField.priority &&
                   Objects.equals(field, sortField.field) &&
                   direction == sortField.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, direction, priority);
        }

        @Override
        public String toString() {
            return field + " " + direction.getSqlValue();
        }
    }

    // Set of allowed sort fields to prevent SQL injection
    private static final Set<String> DEFAULT_ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
        "transaction_id", "organization_id", "account_id", "status", 
        "amount", "currency", "created_at", "updated_at", 
        "merchant_id", "payment_type", "transaction_reference"
    ));

    // Default sort criteria for common views
    public static final SortCriteria DEFAULT_SORT = new SortCriteria(
        new SortField("created_at", SortDirection.DESC)
    );

    public static final SortCriteria RECENT_TRANSACTIONS_SORT = new SortCriteria(
        new SortField("created_at", SortDirection.DESC)
    );

    public static final SortCriteria AMOUNT_DESCENDING_SORT = new SortCriteria(
        new SortField("amount", SortDirection.DESC),
        new SortField("created_at", SortDirection.DESC, 1)
    );

    public static final SortCriteria STATUS_PRIORITY_SORT = new SortCriteria(
        new SortField("status", SortDirection.ASC),
        new SortField("created_at", SortDirection.DESC, 1)
    );

    private final List<SortField> sortFields;
    private final Set<String> allowedFields;

    /**
     * Creates a new SortCriteria with the specified sort fields and default allowed fields.
     * 
     * @param sortFields The sort fields to use
     */
    public SortCriteria(SortField... sortFields) {
        this(DEFAULT_ALLOWED_FIELDS, sortFields);
    }

    /**
     * Creates a new SortCriteria with the specified allowed fields and sort fields.
     * 
     * @param allowedFields The set of allowed field names
     * @param sortFields The sort fields to use
     */
    public SortCriteria(Set<String> allowedFields, SortField... sortFields) {
        this.allowedFields = Collections.unmodifiableSet(new HashSet<>(allowedFields));
        
        if (sortFields == null || sortFields.length == 0) {
            this.sortFields = Collections.singletonList(
                new SortField("created_at", SortDirection.DESC)
            );
        } else {
            List<SortField> validatedFields = new ArrayList<>();
            for (SortField field : sortFields) {
                if (field != null && isValidField(field.getField())) {
                    validatedFields.add(field);
                }
            }
            
            if (validatedFields.isEmpty()) {
                validatedFields.add(new SortField("created_at", SortDirection.DESC));
            }
            
            this.sortFields = Collections.unmodifiableList(validatedFields);
        }
    }

    /**
     * Creates a new SortCriteria from a string representation.
     * Format: "field1:direction1,field2:direction2"
     * 
     * @param sortString The string representation of sort criteria
     * @return A new SortCriteria instance
     */
    public static SortCriteria fromString(String sortString) {
        return fromString(sortString, DEFAULT_ALLOWED_FIELDS);
    }

    /**
     * Creates a new SortCriteria from a string representation with custom allowed fields.
     * Format: "field1:direction1,field2:direction2"
     * 
     * @param sortString The string representation of sort criteria
     * @param allowedFields The set of allowed field names
     * @return A new SortCriteria instance
     */
    public static SortCriteria fromString(String sortString, Set<String> allowedFields) {
        if (sortString == null || sortString.trim().isEmpty()) {
            return new SortCriteria(allowedFields);
        }

        String[] parts = sortString.split(",");
        List<SortField> fields = new ArrayList<>();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            
            String[] fieldParts = part.split(":");
            String fieldName = fieldParts[0].trim();
            
            SortDirection direction = fieldParts.length > 1 
                ? SortDirection.fromString(fieldParts[1].trim())
                : SortDirection.DESC;
                
            fields.add(new SortField(fieldName, direction, i));
        }
        
        return new SortCriteria(allowedFields, fields.toArray(new SortField[0]));
    }

    /**
     * Checks if a field name is valid for sorting.
     * 
     * @param fieldName The field name to validate
     * @return true if the field is valid, false otherwise
     */
    public boolean isValidField(String fieldName) {
        return fieldName != null && allowedFields.contains(fieldName);
    }

    /**
     * Gets the list of sort fields.
     * 
     * @return The list of sort fields
     */
    public List<SortField> getSortFields() {
        return sortFields;
    }

    /**
     * Gets the set of allowed field names.
     * 
     * @return The set of allowed field names
     */
    public Set<String> getAllowedFields() {
        return allowedFields;
    }

    /**
     * Generates an SQL ORDER BY clause based on the sort criteria.
     * 
     * @return The SQL ORDER BY clause
     */
    public String toSqlOrderByClause() {
        if (sortFields.isEmpty()) {
            return "ORDER BY created_at DESC";
        }
        
        String orderByClause = sortFields.stream()
            .map(field -> escapeField(field.getField()) + " " + field.getDirection().getSqlValue())
            .collect(Collectors.joining(", "));
            
        return "ORDER BY " + orderByClause;
    }

    /**
     * Generates an SQL ORDER BY clause with table alias based on the sort criteria.
     * 
     * @param tableAlias The table alias to use
     * @return The SQL ORDER BY clause with table alias
     */
    public String toSqlOrderByClause(String tableAlias) {
        if (sortFields.isEmpty()) {
            return "ORDER BY " + tableAlias + ".created_at DESC";
        }
        
        String orderByClause = sortFields.stream()
            .map(field -> tableAlias + "." + escapeField(field.getField()) + " " + field.getDirection().getSqlValue())
            .collect(Collectors.joining(", "));
            
        return "ORDER BY " + orderByClause;
    }

    /**
     * Escapes a field name for use in SQL queries to prevent SQL injection.
     * 
     * @param fieldName The field name to escape
     * @return The escaped field name
     */
    private String escapeField(String fieldName) {
        // Only allow alphanumeric characters and underscores
        if (!fieldName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid field name: " + fieldName);
        }
        return fieldName;
    }

    /**
     * Creates a new SortCriteria with an additional sort field.
     * 
     * @param field The field name to add
     * @param direction The sort direction
     * @return A new SortCriteria instance with the additional field
     */
    public SortCriteria addSortField(String field, SortDirection direction) {
        if (!isValidField(field)) {
            return this;
        }
        
        List<SortField> newFields = new ArrayList<>(sortFields);
        newFields.add(new SortField(field, direction, newFields.size()));
        
        return new SortCriteria(allowedFields, newFields.toArray(new SortField[0]));
    }

    /**
     * Creates a new SortCriteria with a different primary sort field.
     * 
     * @param field The field name to use as primary sort
     * @param direction The sort direction
     * @return A new SortCriteria instance with the specified primary sort
     */
    public SortCriteria withPrimarySort(String field, SortDirection direction) {
        if (!isValidField(field)) {
            return this;
        }
        
        List<SortField> newFields = new ArrayList<>();
        newFields.add(new SortField(field, direction, 0));
        
        // Add existing fields with incremented priority
        for (SortField sortField : sortFields) {
            if (!sortField.getField().equals(field)) {
                newFields.add(new SortField(
                    sortField.getField(), 
                    sortField.getDirection(), 
                    sortField.getPriority() + 1
                ));
            }
        }
        
        return new SortCriteria(allowedFields, newFields.toArray(new SortField[0]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortCriteria that = (SortCriteria) o;
        return Objects.equals(sortFields, that.sortFields) &&
               Objects.equals(allowedFields, that.allowedFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortFields, allowedFields);
    }

    @Override
    public String toString() {
        return sortFields.stream()
            .map(field -> field.getField() + ":" + field.getDirection().name())
            .collect(Collectors.joining(","));
    }
}