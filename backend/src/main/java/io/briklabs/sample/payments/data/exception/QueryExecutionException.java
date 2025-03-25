package io.briklabs.sample.payments.data.exception;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Exception thrown when SQL query execution fails during payment data operations.
 * <p>
 * This class handles SQL syntax errors, constraint violations, deadlocks, query timeouts,
 * and other execution failures. It provides detailed context about the failed query
 * (with parameter values sanitized for security), error codes specific to query types,
 * and information to help with query troubleshooting.
 * </p>
 * <p>
 * The exception includes mechanisms to sanitize sensitive payment data in error messages
 * to prevent accidental exposure of card numbers, security codes, or other confidential
 * information in logs or error responses.
 * </p>
 */
public class QueryExecutionException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code prefix for query execution errors.
     */
    private static final String ERROR_CODE_PREFIX = "QUERY";

    /**
     * Error codes for specific query execution failure types.
     */
    public static final String SYNTAX_ERROR = ERROR_CODE_PREFIX + "-0001";
    public static final String CONSTRAINT_VIOLATION = ERROR_CODE_PREFIX + "-0002";
    public static final String DEADLOCK = ERROR_CODE_PREFIX + "-0003";
    public static final String TIMEOUT = ERROR_CODE_PREFIX + "-0004";
    public static final String DATA_INTEGRITY = ERROR_CODE_PREFIX + "-0005";
    public static final String PARAMETER_ERROR = ERROR_CODE_PREFIX + "-0006";
    public static final String UNKNOWN_ERROR = ERROR_CODE_PREFIX + "-9999";

    /**
     * The SQL query that failed, with sensitive data sanitized.
     */
    private final String sanitizedQuery;

    /**
     * The type of operation being performed (SELECT, INSERT, UPDATE, DELETE).
     */
    private final String operationType;

    /**
     * The affected table(s) in the query.
     */
    private final String[] affectedTables;

    /**
     * Patterns for identifying sensitive data in queries to be sanitized.
     */
    private static final Pattern[] SENSITIVE_DATA_PATTERNS = {
        // Credit card number pattern
        Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b", Pattern.CASE_INSENSITIVE),
        // CVV/security code pattern
        Pattern.compile("\\b\\d{3,4}\\b(?=.*?(?:cvv|cvc|security.?code))", Pattern.CASE_INSENSITIVE),
        // Account number pattern
        Pattern.compile("\\baccount.?number\\s*?[=:]\\s*?['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE),
        // Payment token pattern
        Pattern.compile("\\bpayment.?token\\s*?[=:]\\s*?['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE),
        // Authentication data pattern
        Pattern.compile("\\bauthentication.?data\\s*?[=:]\\s*?['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Creates a new QueryExecutionException with a default error code.
     *
     * @param message the error message
     * @param query the SQL query that failed (will be sanitized)
     * @param operationType the type of operation (SELECT, INSERT, UPDATE, DELETE)
     * @param affectedTables the affected table(s) in the query
     */
    public QueryExecutionException(String message, String query, String operationType, String... affectedTables) {
        super(message, UNKNOWN_ERROR);
        this.sanitizedQuery = sanitizeQuery(query);
        this.operationType = operationType;
        this.affectedTables = affectedTables != null ? affectedTables.clone() : new String[0];
    }

    /**
     * Creates a new QueryExecutionException with a specific error code.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param query the SQL query that failed (will be sanitized)
     * @param operationType the type of operation (SELECT, INSERT, UPDATE, DELETE)
     * @param affectedTables the affected table(s) in the query
     */
    public QueryExecutionException(String message, String errorCode, String query, String operationType, String... affectedTables) {
        super(message, errorCode);
        this.sanitizedQuery = sanitizeQuery(query);
        this.operationType = operationType;
        this.affectedTables = affectedTables != null ? affectedTables.clone() : new String[0];
    }

    /**
     * Creates a new QueryExecutionException from an SQLException with automatic error code mapping.
     *
     * @param message the error message
     * @param cause the underlying SQLException
     * @param query the SQL query that failed (will be sanitized)
     * @param operationType the type of operation (SELECT, INSERT, UPDATE, DELETE)
     * @param affectedTables the affected table(s) in the query
     */
    public QueryExecutionException(String message, SQLException cause, String query, String operationType, String... affectedTables) {
        super(message, mapSqlExceptionToErrorCode(cause), cause);
        this.sanitizedQuery = sanitizeQuery(query);
        this.operationType = operationType;
        this.affectedTables = affectedTables != null ? affectedTables.clone() : new String[0];
    }

    /**
     * Creates a new QueryExecutionException with a formatted message.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param query the SQL query that failed (will be sanitized)
     * @param operationType the type of operation (SELECT, INSERT, UPDATE, DELETE)
     * @param affectedTables the affected table(s) in the query
     * @param args the arguments to be formatted into the message pattern
     */
    public QueryExecutionException(String messagePattern, String query, String operationType, String[] affectedTables, Object... args) {
        super(messagePattern, UNKNOWN_ERROR, args);
        this.sanitizedQuery = sanitizeQuery(query);
        this.operationType = operationType;
        this.affectedTables = affectedTables != null ? affectedTables.clone() : new String[0];
    }

    /**
     * Creates a new QueryExecutionException with a formatted message and specific error code.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param query the SQL query that failed (will be sanitized)
     * @param operationType the type of operation (SELECT, INSERT, UPDATE, DELETE)
     * @param affectedTables the affected table(s) in the query
     * @param args the arguments to be formatted into the message pattern
     */
    public QueryExecutionException(String messagePattern, String errorCode, String query, String operationType, String[] affectedTables, Object... args) {
        super(messagePattern, errorCode, args);
        this.sanitizedQuery = sanitizeQuery(query);
        this.operationType = operationType;
        this.affectedTables = affectedTables != null ? affectedTables.clone() : new String[0];
    }

    /**
     * Creates a new QueryExecutionException with a formatted message and cause.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param cause the underlying cause of the exception
     * @param query the SQL query that failed (will be sanitized)
     * @param operationType the type of operation (SELECT, INSERT, UPDATE, DELETE)
     * @param affectedTables the affected table(s) in the query
     * @param args the arguments to be formatted into the message pattern
     */
    public QueryExecutionException(String messagePattern, Throwable cause, String query, String operationType, String[] affectedTables, Object... args) {
        super(messagePattern, cause instanceof SQLException ? mapSqlExceptionToErrorCode((SQLException) cause) : UNKNOWN_ERROR, cause, args);
        this.sanitizedQuery = sanitizeQuery(query);
        this.operationType = operationType;
        this.affectedTables = affectedTables != null ? affectedTables.clone() : new String[0];
    }

    /**
     * Maps an SQLException to a specific error code based on its SQLState or error code.
     *
     * @param sqlException the SQLException to map
     * @return the mapped error code
     */
    private static String mapSqlExceptionToErrorCode(SQLException sqlException) {
        if (sqlException == null) {
            return UNKNOWN_ERROR;
        }

        String sqlState = sqlException.getSQLState();
        int errorCode = sqlException.getErrorCode();

        // Map based on SQLState
        if (sqlState != null) {
            // Syntax error
            if (sqlState.startsWith("42")) {
                return SYNTAX_ERROR;
            }
            // Constraint violation
            if (sqlState.startsWith("23")) {
                return CONSTRAINT_VIOLATION;
            }
            // Deadlock
            if (sqlState.equals("40001") || sqlState.equals("40P01")) {
                return DEADLOCK;
            }
            // Timeout
            if (sqlState.equals("57014") || sqlState.equals("57P01")) {
                return TIMEOUT;
            }
            // Data integrity
            if (sqlState.startsWith("22") || sqlState.startsWith("23")) {
                return DATA_INTEGRITY;
            }
        }

        // PostgreSQL specific error codes
        if (errorCode > 0) {
            // Syntax error
            if (errorCode == 42601 || errorCode == 42701) {
                return SYNTAX_ERROR;
            }
            // Constraint violation
            if (errorCode == 23505 || errorCode == 23503) {
                return CONSTRAINT_VIOLATION;
            }
            // Deadlock
            if (errorCode == 40P01) {
                return DEADLOCK;
            }
            // Timeout
            if (errorCode == 57014) {
                return TIMEOUT;
            }
            // Parameter error
            if (errorCode == 42P02 || errorCode == 42703) {
                return PARAMETER_ERROR;
            }
        }

        // Check message for common error patterns
        String message = sqlException.getMessage().toLowerCase();
        if (message.contains("syntax") || message.contains("parse")) {
            return SYNTAX_ERROR;
        }
        if (message.contains("constraint") || message.contains("violates") || message.contains("unique")) {
            return CONSTRAINT_VIOLATION;
        }
        if (message.contains("deadlock") || message.contains("could not serialize")) {
            return DEADLOCK;
        }
        if (message.contains("timeout") || message.contains("timed out")) {
            return TIMEOUT;
        }
        if (message.contains("parameter") || message.contains("argument")) {
            return PARAMETER_ERROR;
        }

        return UNKNOWN_ERROR;
    }

    /**
     * Sanitizes a SQL query by masking sensitive payment data.
     *
     * @param query the original SQL query
     * @return the sanitized query with sensitive data masked
     */
    private String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }

        String sanitized = query;
        for (Pattern pattern : SENSITIVE_DATA_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll(match -> "***MASKED***");
        }

        // Additional sanitization for prepared statement parameters
        // Replace values in patterns like "column = 'value'" with "column = '***'"
        sanitized = sanitized.replaceAll("(=\\s*)'([^']*)'", "$1'***'");
        sanitized = sanitized.replaceAll("(=\\s*)\"([^\"]*)\"", "$1\"***\"");
        
        return sanitized;
    }

    /**
     * Gets the sanitized SQL query that failed.
     *
     * @return the sanitized SQL query
     */
    public String getSanitizedQuery() {
        return sanitizedQuery;
    }

    /**
     * Gets the type of operation being performed.
     *
     * @return the operation type (SELECT, INSERT, UPDATE, DELETE)
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * Gets the affected table(s) in the query.
     *
     * @return the affected tables
     */
    public String[] getAffectedTables() {
        return affectedTables != null ? affectedTables.clone() : new String[0];
    }

    /**
     * Determines if this exception represents a recoverable error that could be retried.
     *
     * @return true if the error is potentially recoverable with a retry, false otherwise
     */
    public boolean isRecoverable() {
        String errorCode = getErrorCode();
        return DEADLOCK.equals(errorCode) || TIMEOUT.equals(errorCode);
    }

    /**
     * Returns a string representation of this exception including query details.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("\nOperation: ").append(operationType);
        sb.append("\nAffected Tables: ").append(Arrays.toString(affectedTables));
        sb.append("\nSanitized Query: ").append(sanitizedQuery);
        return sb.toString();
    }

    /**
     * Creates a builder for constructing QueryExecutionException instances.
     *
     * @return a new QueryExecutionExceptionBuilder
     */
    public static QueryExecutionExceptionBuilder builder() {
        return new QueryExecutionExceptionBuilder();
    }

    /**
     * Builder class for creating QueryExecutionException instances with a fluent API.
     */
    public static class QueryExecutionExceptionBuilder {
        private String message;
        private String errorCode = UNKNOWN_ERROR;
        private Throwable cause;
        private String query;
        private String operationType;
        private String[] affectedTables;

        /**
         * Sets the error message.
         *
         * @param message the error message
         * @return this builder instance
         */
        public QueryExecutionExceptionBuilder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the error code.
         *
         * @param errorCode the error code
         * @return this builder instance
         */
        public QueryExecutionExceptionBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * Sets the underlying cause.
         *
         * @param cause the cause of the exception
         * @return this builder instance
         */
        public QueryExecutionExceptionBuilder cause(Throwable cause) {
            this.cause = cause;
            if (cause instanceof SQLException) {
                this.errorCode = mapSqlExceptionToErrorCode((SQLException) cause);
            }
            return this;
        }

        /**
         * Sets the SQL query that failed.
         *
         * @param query the SQL query
         * @return this builder instance
         */
        public QueryExecutionExceptionBuilder query(String query) {
            this.query = query;
            return this;
        }

        /**
         * Sets the operation type.
         *
         * @param operationType the operation type (SELECT, INSERT, UPDATE, DELETE)
         * @return this builder instance
         */
        public QueryExecutionExceptionBuilder operationType(String operationType) {
            this.operationType = operationType;
            return this;
        }

        /**
         * Sets the affected tables.
         *
         * @param affectedTables the affected tables
         * @return this builder instance
         */
        public QueryExecutionExceptionBuilder affectedTables(String... affectedTables) {
            this.affectedTables = affectedTables;
            return this;
        }

        /**
         * Builds the QueryExecutionException instance.
         *
         * @return a new QueryExecutionException
         */
        public QueryExecutionException build() {
            if (cause != null) {
                return new QueryExecutionException(message, errorCode, cause, query, operationType, affectedTables);
            } else {
                return new QueryExecutionException(message, errorCode, query, operationType, affectedTables);
            }
        }
    }

    /**
     * Constructor used by the builder.
     */
    private QueryExecutionException(String message, String errorCode, Throwable cause, String query, String operationType, String[] affectedTables) {
        super(message, errorCode, cause);
        this.sanitizedQuery = sanitizeQuery(query);
        this.operationType = operationType;
        this.affectedTables = affectedTables != null ? affectedTables.clone() : new String[0];
    }
}