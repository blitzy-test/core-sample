package io.briklabs.sample.payments.data.exception;

import com.zaxxer.hikari.pool.HikariPool;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 * Exception thrown when database connection issues occur during payment data operations.
 * <p>
 * This class handles failures in connection acquisition, connection pool exhaustion,
 * network connectivity issues, and database unavailability scenarios. It provides
 * specific error codes and context for connection-related failures, facilitating
 * proper error handling and recovery for database connectivity problems.
 * </p>
 * <p>
 * Connection exceptions are categorized by specific error codes to enable targeted
 * recovery strategies and appropriate client feedback.
 * </p>
 */
public class ConnectionException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Connection error code constants
     */
    public static final String CONN_ACQUISITION_FAILED = "CONN-0001";
    public static final String CONN_POOL_EXHAUSTED = "CONN-0002";
    public static final String CONN_NETWORK_ERROR = "CONN-0003";
    public static final String CONN_DATABASE_UNAVAILABLE = "CONN-0004";
    public static final String CONN_TIMEOUT = "CONN-0005";
    public static final String CONN_INVALID = "CONN-0006";
    public static final String CONN_CLOSED = "CONN-0007";
    public static final String CONN_TRANSACTION_ACTIVE = "CONN-0008";
    public static final String CONN_HIKARI_ERROR = "CONN-0009";
    public static final String CONN_UNKNOWN_ERROR = "CONN-9999";

    /**
     * The number of retry attempts recommended for this connection exception
     */
    private final int recommendedRetries;

    /**
     * The suggested delay in milliseconds before retrying
     */
    private final long retryDelayMs;

    /**
     * Creates a new ConnectionException with a default error code.
     *
     * @param message the error message
     */
    public ConnectionException(String message) {
        super(message, CONN_UNKNOWN_ERROR);
        this.recommendedRetries = 0;
        this.retryDelayMs = 0;
    }

    /**
     * Creates a new ConnectionException with a specific error code.
     *
     * @param message the error message
     * @param errorCode the specific error code
     */
    public ConnectionException(String message, String errorCode) {
        super(message, errorCode);
        this.recommendedRetries = determineRecommendedRetries(errorCode);
        this.retryDelayMs = determineRetryDelay(errorCode);
    }

    /**
     * Creates a new ConnectionException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, CONN_UNKNOWN_ERROR, cause);
        this.recommendedRetries = 0;
        this.retryDelayMs = 0;
    }

    /**
     * Creates a new ConnectionException with a specific error code and cause.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     */
    public ConnectionException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
        this.recommendedRetries = determineRecommendedRetries(errorCode);
        this.retryDelayMs = determineRetryDelay(errorCode);
    }

    /**
     * Creates a new ConnectionException with a formatted message.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param args the arguments to be formatted into the message pattern
     */
    public ConnectionException(String messagePattern, Object... args) {
        super(messagePattern, CONN_UNKNOWN_ERROR, args);
        this.recommendedRetries = 0;
        this.retryDelayMs = 0;
    }

    /**
     * Creates a new ConnectionException with a formatted message and specific error code.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param args the arguments to be formatted into the message pattern
     */
    public ConnectionException(String messagePattern, String errorCode, Object... args) {
        super(messagePattern, errorCode, args);
        this.recommendedRetries = determineRecommendedRetries(errorCode);
        this.retryDelayMs = determineRetryDelay(errorCode);
    }

    /**
     * Creates a new ConnectionException with a formatted message and cause.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param cause the underlying cause of the exception
     * @param args the arguments to be formatted into the message pattern
     */
    public ConnectionException(String messagePattern, Throwable cause, Object... args) {
        super(messagePattern, CONN_UNKNOWN_ERROR, cause, args);
        this.recommendedRetries = 0;
        this.retryDelayMs = 0;
    }

    /**
     * Creates a new ConnectionException with a formatted message, specific error code, and cause.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     * @param args the arguments to be formatted into the message pattern
     */
    public ConnectionException(String messagePattern, String errorCode, Throwable cause, Object... args) {
        super(messagePattern, errorCode, cause, args);
        this.recommendedRetries = determineRecommendedRetries(errorCode);
        this.retryDelayMs = determineRetryDelay(errorCode);
    }

    /**
     * Creates a ConnectionException from a SQLException with appropriate error code mapping.
     *
     * @param sqlException the SQLException that caused the connection failure
     * @return a new ConnectionException with mapped error code
     */
    public static ConnectionException fromSQLException(SQLException sqlException) {
        String errorCode = mapSQLExceptionToErrorCode(sqlException);
        String message = formatSQLExceptionMessage(sqlException);
        return new ConnectionException(message, errorCode, sqlException);
    }

    /**
     * Creates a ConnectionException from a HikariPool exception with appropriate error code mapping.
     *
     * @param hikariException the HikariPool exception that caused the connection failure
     * @return a new ConnectionException with mapped error code
     */
    public static ConnectionException fromHikariException(Exception hikariException) {
        String errorCode = CONN_HIKARI_ERROR;
        String message = "HikariCP connection pool error: " + hikariException.getMessage();
        
        // Check for specific HikariCP exception types
        if (hikariException instanceof HikariPool.PoolInitializationException) {
            errorCode = CONN_DATABASE_UNAVAILABLE;
            message = "Failed to initialize connection pool: " + hikariException.getMessage();
        } else if (hikariException.getMessage() != null) {
            if (hikariException.getMessage().contains("Connection is not available")) {
                errorCode = CONN_POOL_EXHAUSTED;
                message = "Connection pool exhausted: " + hikariException.getMessage();
            } else if (hikariException.getMessage().contains("timeout")) {
                errorCode = CONN_TIMEOUT;
                message = "Connection acquisition timed out: " + hikariException.getMessage();
            }
        }
        
        return new ConnectionException(message, errorCode, hikariException);
    }

    /**
     * Maps a SQLException to an appropriate connection error code.
     *
     * @param sqlException the SQLException to map
     * @return the mapped error code
     */
    private static String mapSQLExceptionToErrorCode(SQLException sqlException) {
        String sqlState = sqlException.getSQLState();
        int errorCode = sqlException.getErrorCode();
        String message = sqlException.getMessage();
        
        // Check for common connection-related SQL states
        if (sqlState != null) {
            // Connection failure SQL states
            if (sqlState.startsWith("08")) {
                if ("08001".equals(sqlState)) {
                    return CONN_DATABASE_UNAVAILABLE;
                } else if ("08006".equals(sqlState)) {
                    return CONN_DATABASE_UNAVAILABLE;
                } else if ("08S01".equals(sqlState)) {
                    return CONN_NETWORK_ERROR;
                } else {
                    return CONN_ACQUISITION_FAILED;
                }
            }
            
            // Transaction-related SQL states
            if (sqlState.startsWith("25")) {
                return CONN_TRANSACTION_ACTIVE;
            }
        }
        
        // Check message content for common connection issues
        if (message != null) {
            if (message.contains("timeout") || message.contains("timed out")) {
                return CONN_TIMEOUT;
            } else if (message.contains("Connection is closed") || message.contains("connection is closed")) {
                return CONN_CLOSED;
            } else if (message.contains("Connection is invalid") || message.contains("connection is invalid")) {
                return CONN_INVALID;
            } else if (message.contains("maximum pool size") || message.contains("no available connections")) {
                return CONN_POOL_EXHAUSTED;
            } else if (message.contains("network") || message.contains("connection refused") || 
                      message.contains("host") || message.contains("unreachable")) {
                return CONN_NETWORK_ERROR;
            } else if (message.contains("database") && 
                      (message.contains("unavailable") || message.contains("down") || message.contains("not found"))) {
                return CONN_DATABASE_UNAVAILABLE;
            }
        }
        
        // Default to generic connection acquisition failure
        return CONN_ACQUISITION_FAILED;
    }

    /**
     * Formats a user-friendly message from a SQLException.
     *
     * @param sqlException the SQLException to format
     * @return a formatted error message
     */
    private static String formatSQLExceptionMessage(SQLException sqlException) {
        StringBuilder message = new StringBuilder("Database connection error: ");
        message.append(sqlException.getMessage());
        
        // Add SQL state and vendor code if available
        if (sqlException.getSQLState() != null) {
            message.append(" (SQL State: ").append(sqlException.getSQLState());
            if (sqlException.getErrorCode() != 0) {
                message.append(", Vendor Code: ").append(sqlException.getErrorCode());
            }
            message.append(")");
        }
        
        return message.toString();
    }

    /**
     * Determines the recommended number of retry attempts based on the error code.
     *
     * @param errorCode the error code
     * @return the recommended number of retry attempts
     */
    private int determineRecommendedRetries(String errorCode) {
        switch (errorCode) {
            case CONN_ACQUISITION_FAILED:
                return 3;
            case CONN_POOL_EXHAUSTED:
                return 5;
            case CONN_NETWORK_ERROR:
                return 3;
            case CONN_TIMEOUT:
                return 2;
            case CONN_INVALID:
                return 1;
            case CONN_CLOSED:
                return 1;
            case CONN_DATABASE_UNAVAILABLE:
                return 0; // Don't retry if database is unavailable
            case CONN_TRANSACTION_ACTIVE:
                return 0; // Don't retry transaction state issues
            case CONN_HIKARI_ERROR:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Determines the recommended delay before retrying based on the error code.
     *
     * @param errorCode the error code
     * @return the recommended delay in milliseconds
     */
    private long determineRetryDelay(String errorCode) {
        switch (errorCode) {
            case CONN_ACQUISITION_FAILED:
                return 1000; // 1 second
            case CONN_POOL_EXHAUSTED:
                return 500; // 500 milliseconds
            case CONN_NETWORK_ERROR:
                return 2000; // 2 seconds
            case CONN_TIMEOUT:
                return 1500; // 1.5 seconds
            case CONN_INVALID:
                return 100; // 100 milliseconds
            case CONN_CLOSED:
                return 100; // 100 milliseconds
            case CONN_HIKARI_ERROR:
                return 1000; // 1 second
            default:
                return 1000; // Default to 1 second
        }
    }

    /**
     * Gets the recommended number of retry attempts for this connection exception.
     *
     * @return the recommended number of retry attempts
     */
    public int getRecommendedRetries() {
        return recommendedRetries;
    }

    /**
     * Gets the suggested delay before retrying.
     *
     * @return the suggested delay in milliseconds
     */
    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    /**
     * Determines if this connection exception is retryable.
     *
     * @return true if the exception is retryable, false otherwise
     */
    public boolean isRetryable() {
        return recommendedRetries > 0;
    }

    /**
     * Provides recovery guidance based on the exception type.
     *
     * @return a string with recovery guidance
     */
    public String getRecoveryGuidance() {
        switch (getErrorCode()) {
            case CONN_ACQUISITION_FAILED:
                return "Verify database connectivity and credentials. Retry the operation after a short delay.";
            case CONN_POOL_EXHAUSTED:
                return "Connection pool is at capacity. Consider increasing the maximum pool size or optimizing connection usage patterns.";
            case CONN_NETWORK_ERROR:
                return "Network connectivity issue detected. Verify network settings and database server availability.";
            case CONN_DATABASE_UNAVAILABLE:
                return "Database server is unavailable. Verify database server status and connectivity.";
            case CONN_TIMEOUT:
                return "Connection acquisition timed out. Database may be under heavy load or network latency is high.";
            case CONN_INVALID:
                return "Connection is invalid. Request a new connection from the pool.";
            case CONN_CLOSED:
                return "Connection is closed. Request a new connection from the pool.";
            case CONN_TRANSACTION_ACTIVE:
                return "Connection has an active transaction. Commit or rollback the transaction before releasing the connection.";
            case CONN_HIKARI_ERROR:
                return "HikariCP connection pool error. Check pool configuration and database server status.";
            default:
                return "Unknown connection error. Verify database connectivity and application configuration.";
        }
    }

    /**
     * Returns a string representation of this exception including the error code,
     * retry information, and recovery guidance.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        
        if (isRetryable()) {
            sb.append(" [Retryable: ").append(recommendedRetries).append(" attempts with ")
              .append(retryDelayMs).append("ms delay]");
        } else {
            sb.append(" [Not retryable]");
        }
        
        return sb.toString();
    }
}