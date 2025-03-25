package io.briklabs.sample.payments.data.exception;

import com.zaxxer.hikari.pool.HikariPool;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Exception thrown when database connection issues occur during payment data operations.
 * <p>
 * This class handles failures in connection acquisition, connection pool exhaustion,
 * network connectivity issues, and database unavailability scenarios. It provides
 * specific error codes and context for connection-related failures, facilitating
 * proper error handling and recovery for database connectivity problems.
 * </p>
 * <p>
 * The exception includes information about the type of connection failure, suggested
 * retry strategies, and integration with HikariCP-specific error conditions.
 * </p>
 */
public class ConnectionException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code for general connection acquisition failure.
     */
    public static final String CONN_ACQUISITION_FAILED = "CONN-1001";

    /**
     * Error code for connection pool exhaustion (no available connections).
     */
    public static final String CONN_POOL_EXHAUSTED = "CONN-1002";

    /**
     * Error code for connection timeout.
     */
    public static final String CONN_TIMEOUT = "CONN-1003";

    /**
     * Error code for network connectivity issues.
     */
    public static final String CONN_NETWORK_ERROR = "CONN-1004";

    /**
     * Error code for database server unavailability.
     */
    public static final String CONN_DB_UNAVAILABLE = "CONN-1005";

    /**
     * Error code for connection validation failure.
     */
    public static final String CONN_VALIDATION_FAILED = "CONN-1006";

    /**
     * Error code for connection configuration issues.
     */
    public static final String CONN_CONFIG_ERROR = "CONN-1007";

    /**
     * Error code for connection closing failure.
     */
    public static final String CONN_CLOSE_FAILED = "CONN-1008";

    /**
     * Enum representing the type of connection failure.
     */
    public enum ConnectionFailureType {
        /** Connection acquisition failed due to pool exhaustion */
        POOL_EXHAUSTED,
        
        /** Connection acquisition timed out */
        TIMEOUT,
        
        /** Network connectivity issue */
        NETWORK_ERROR,
        
        /** Database server is unavailable */
        DATABASE_UNAVAILABLE,
        
        /** Connection validation failed */
        VALIDATION_FAILED,
        
        /** Connection configuration error */
        CONFIGURATION_ERROR,
        
        /** Connection closing failed */
        CLOSE_FAILED,
        
        /** Unknown or unclassified connection error */
        UNKNOWN
    }

    /**
     * The type of connection failure.
     */
    private final ConnectionFailureType failureType;

    /**
     * Whether a retry attempt is recommended for this connection failure.
     */
    private final boolean retryRecommended;

    /**
     * Suggested delay in milliseconds before retry attempt.
     */
    private final long suggestedRetryDelayMs;

    /**
     * Creates a new ConnectionException with a default error code.
     *
     * @param message the error message
     */
    public ConnectionException(String message) {
        super(message, CONN_ACQUISITION_FAILED);
        this.failureType = ConnectionFailureType.UNKNOWN;
        this.retryRecommended = false;
        this.suggestedRetryDelayMs = 0;
    }

    /**
     * Creates a new ConnectionException with a specific error code.
     *
     * @param message the error message
     * @param errorCode the specific error code
     */
    public ConnectionException(String message, String errorCode) {
        super(message, errorCode);
        this.failureType = determineFailureType(errorCode);
        this.retryRecommended = isRetryRecommendedForErrorCode(errorCode);
        this.suggestedRetryDelayMs = calculateRetryDelay(this.failureType);
    }

    /**
     * Creates a new ConnectionException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, CONN_ACQUISITION_FAILED, cause);
        this.failureType = determineFailureTypeFromCause(cause);
        this.retryRecommended = isRetryRecommendedForCause(cause);
        this.suggestedRetryDelayMs = calculateRetryDelay(this.failureType);
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
        this.failureType = determineFailureType(errorCode, cause);
        this.retryRecommended = isRetryRecommendedForErrorCode(errorCode) && isRetryRecommendedForCause(cause);
        this.suggestedRetryDelayMs = calculateRetryDelay(this.failureType);
    }

    /**
     * Creates a new ConnectionException with a formatted message.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param args the arguments to be formatted into the message pattern
     */
    public ConnectionException(String messagePattern, Object... args) {
        super(messagePattern, CONN_ACQUISITION_FAILED, args);
        this.failureType = ConnectionFailureType.UNKNOWN;
        this.retryRecommended = false;
        this.suggestedRetryDelayMs = 0;
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
        this.failureType = determineFailureType(errorCode);
        this.retryRecommended = isRetryRecommendedForErrorCode(errorCode);
        this.suggestedRetryDelayMs = calculateRetryDelay(this.failureType);
    }

    /**
     * Creates a new ConnectionException with a formatted message and cause.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param cause the underlying cause of the exception
     * @param args the arguments to be formatted into the message pattern
     */
    public ConnectionException(String messagePattern, Throwable cause, Object... args) {
        super(messagePattern, CONN_ACQUISITION_FAILED, cause, args);
        this.failureType = determineFailureTypeFromCause(cause);
        this.retryRecommended = isRetryRecommendedForCause(cause);
        this.suggestedRetryDelayMs = calculateRetryDelay(this.failureType);
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
        this.failureType = determineFailureType(errorCode, cause);
        this.retryRecommended = isRetryRecommendedForErrorCode(errorCode) && isRetryRecommendedForCause(cause);
        this.suggestedRetryDelayMs = calculateRetryDelay(this.failureType);
    }

    /**
     * Creates a ConnectionException with specific failure type and retry recommendations.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     * @param failureType the type of connection failure
     * @param retryRecommended whether a retry is recommended
     * @param suggestedRetryDelayMs suggested delay before retry in milliseconds
     */
    public ConnectionException(String message, String errorCode, Throwable cause, 
                              ConnectionFailureType failureType, boolean retryRecommended, 
                              long suggestedRetryDelayMs) {
        super(message, errorCode, cause);
        this.failureType = failureType;
        this.retryRecommended = retryRecommended;
        this.suggestedRetryDelayMs = suggestedRetryDelayMs;
    }

    /**
     * Factory method to create a ConnectionException from an SQLException.
     *
     * @param sqlException the SQLException to convert
     * @return a new ConnectionException with appropriate error code and failure type
     */
    public static ConnectionException fromSQLException(SQLException sqlException) {
        String errorCode;
        ConnectionFailureType failureType;
        boolean retryRecommended;
        long retryDelay;

        if (sqlException instanceof SQLTimeoutException) {
            errorCode = CONN_TIMEOUT;
            failureType = ConnectionFailureType.TIMEOUT;
            retryRecommended = true;
            retryDelay = 1000; // 1 second
        } else if (sqlException.getMessage().contains("Connection is not available")) {
            errorCode = CONN_POOL_EXHAUSTED;
            failureType = ConnectionFailureType.POOL_EXHAUSTED;
            retryRecommended = true;
            retryDelay = 500; // 500 milliseconds
        } else if (sqlException.getMessage().contains("Connection is invalid")) {
            errorCode = CONN_VALIDATION_FAILED;
            failureType = ConnectionFailureType.VALIDATION_FAILED;
            retryRecommended = true;
            retryDelay = 1000; // 1 second
        } else if (sqlException.getMessage().contains("Connection refused") || 
                  sqlException.getCause() instanceof ConnectException) {
            errorCode = CONN_DB_UNAVAILABLE;
            failureType = ConnectionFailureType.DATABASE_UNAVAILABLE;
            retryRecommended = true;
            retryDelay = 5000; // 5 seconds
        } else if (sqlException.getCause() instanceof SocketTimeoutException) {
            errorCode = CONN_NETWORK_ERROR;
            failureType = ConnectionFailureType.NETWORK_ERROR;
            retryRecommended = true;
            retryDelay = 2000; // 2 seconds
        } else {
            errorCode = CONN_ACQUISITION_FAILED;
            failureType = ConnectionFailureType.UNKNOWN;
            retryRecommended = false;
            retryDelay = 0;
        }

        return new ConnectionException(
            "Database connection error: " + sqlException.getMessage(),
            errorCode,
            sqlException,
            failureType,
            retryRecommended,
            retryDelay
        );
    }

    /**
     * Factory method to create a ConnectionException from a HikariPool exception.
     *
     * @param hikariException the HikariPool exception to convert
     * @return a new ConnectionException with appropriate error code and failure type
     */
    public static ConnectionException fromHikariException(Exception hikariException) {
        String errorCode;
        ConnectionFailureType failureType;
        boolean retryRecommended;
        long retryDelay;

        String message = hikariException.getMessage();
        if (message == null) {
            message = hikariException.getClass().getName();
        }

        if (hikariException instanceof HikariPool.PoolInitializationException) {
            errorCode = CONN_CONFIG_ERROR;
            failureType = ConnectionFailureType.CONFIGURATION_ERROR;
            retryRecommended = false;
            retryDelay = 0;
        } else if (message.contains("Connection is not available")) {
            errorCode = CONN_POOL_EXHAUSTED;
            failureType = ConnectionFailureType.POOL_EXHAUSTED;
            retryRecommended = true;
            retryDelay = 500; // 500 milliseconds
        } else if (message.contains("Connection timeout")) {
            errorCode = CONN_TIMEOUT;
            failureType = ConnectionFailureType.TIMEOUT;
            retryRecommended = true;
            retryDelay = 1000; // 1 second
        } else if (message.contains("Connection is invalid")) {
            errorCode = CONN_VALIDATION_FAILED;
            failureType = ConnectionFailureType.VALIDATION_FAILED;
            retryRecommended = true;
            retryDelay = 1000; // 1 second
        } else {
            errorCode = CONN_ACQUISITION_FAILED;
            failureType = ConnectionFailureType.UNKNOWN;
            retryRecommended = false;
            retryDelay = 0;
        }

        return new ConnectionException(
            "HikariCP connection error: " + message,
            errorCode,
            hikariException,
            failureType,
            retryRecommended,
            retryDelay
        );
    }

    /**
     * Factory method to create a pool exhaustion exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new ConnectionException for pool exhaustion
     */
    public static ConnectionException poolExhausted(String message, Throwable cause) {
        return new ConnectionException(
            message,
            CONN_POOL_EXHAUSTED,
            cause,
            ConnectionFailureType.POOL_EXHAUSTED,
            true,
            500 // 500 milliseconds
        );
    }

    /**
     * Factory method to create a connection timeout exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new ConnectionException for connection timeout
     */
    public static ConnectionException timeout(String message, Throwable cause) {
        return new ConnectionException(
            message,
            CONN_TIMEOUT,
            cause,
            ConnectionFailureType.TIMEOUT,
            true,
            1000 // 1 second
        );
    }

    /**
     * Factory method to create a network error exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new ConnectionException for network error
     */
    public static ConnectionException networkError(String message, Throwable cause) {
        return new ConnectionException(
            message,
            CONN_NETWORK_ERROR,
            cause,
            ConnectionFailureType.NETWORK_ERROR,
            true,
            2000 // 2 seconds
        );
    }

    /**
     * Factory method to create a database unavailable exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new ConnectionException for database unavailability
     */
    public static ConnectionException databaseUnavailable(String message, Throwable cause) {
        return new ConnectionException(
            message,
            CONN_DB_UNAVAILABLE,
            cause,
            ConnectionFailureType.DATABASE_UNAVAILABLE,
            true,
            5000 // 5 seconds
        );
    }

    /**
     * Factory method to create a connection validation failure exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new ConnectionException for validation failure
     */
    public static ConnectionException validationFailed(String message, Throwable cause) {
        return new ConnectionException(
            message,
            CONN_VALIDATION_FAILED,
            cause,
            ConnectionFailureType.VALIDATION_FAILED,
            true,
            1000 // 1 second
        );
    }

    /**
     * Factory method to create a connection configuration error exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new ConnectionException for configuration error
     */
    public static ConnectionException configurationError(String message, Throwable cause) {
        return new ConnectionException(
            message,
            CONN_CONFIG_ERROR,
            cause,
            ConnectionFailureType.CONFIGURATION_ERROR,
            false,
            0
        );
    }

    /**
     * Factory method to create a connection close failure exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new ConnectionException for close failure
     */
    public static ConnectionException closeFailed(String message, Throwable cause) {
        return new ConnectionException(
            message,
            CONN_CLOSE_FAILED,
            cause,
            ConnectionFailureType.CLOSE_FAILED,
            false,
            0
        );
    }

    /**
     * Gets the type of connection failure.
     *
     * @return the connection failure type
     */
    public ConnectionFailureType getFailureType() {
        return failureType;
    }

    /**
     * Checks if a retry attempt is recommended for this connection failure.
     *
     * @return true if retry is recommended, false otherwise
     */
    public boolean isRetryRecommended() {
        return retryRecommended;
    }

    /**
     * Gets the suggested delay in milliseconds before retry attempt.
     *
     * @return the suggested retry delay in milliseconds
     */
    public long getSuggestedRetryDelayMs() {
        return suggestedRetryDelayMs;
    }

    /**
     * Determines the failure type based on the error code.
     *
     * @param errorCode the error code
     * @return the determined connection failure type
     */
    private ConnectionFailureType determineFailureType(String errorCode) {
        switch (errorCode) {
            case CONN_POOL_EXHAUSTED:
                return ConnectionFailureType.POOL_EXHAUSTED;
            case CONN_TIMEOUT:
                return ConnectionFailureType.TIMEOUT;
            case CONN_NETWORK_ERROR:
                return ConnectionFailureType.NETWORK_ERROR;
            case CONN_DB_UNAVAILABLE:
                return ConnectionFailureType.DATABASE_UNAVAILABLE;
            case CONN_VALIDATION_FAILED:
                return ConnectionFailureType.VALIDATION_FAILED;
            case CONN_CONFIG_ERROR:
                return ConnectionFailureType.CONFIGURATION_ERROR;
            case CONN_CLOSE_FAILED:
                return ConnectionFailureType.CLOSE_FAILED;
            default:
                return ConnectionFailureType.UNKNOWN;
        }
    }

    /**
     * Determines the failure type based on the error code and cause.
     *
     * @param errorCode the error code
     * @param cause the underlying cause
     * @return the determined connection failure type
     */
    private ConnectionFailureType determineFailureType(String errorCode, Throwable cause) {
        ConnectionFailureType fromErrorCode = determineFailureType(errorCode);
        if (fromErrorCode != ConnectionFailureType.UNKNOWN) {
            return fromErrorCode;
        }
        return determineFailureTypeFromCause(cause);
    }

    /**
     * Determines the failure type based on the cause.
     *
     * @param cause the underlying cause
     * @return the determined connection failure type
     */
    private ConnectionFailureType determineFailureTypeFromCause(Throwable cause) {
        if (cause == null) {
            return ConnectionFailureType.UNKNOWN;
        }

        if (cause instanceof SQLTimeoutException) {
            return ConnectionFailureType.TIMEOUT;
        } else if (cause instanceof ConnectException || 
                  (cause.getMessage() != null && cause.getMessage().contains("Connection refused"))) {
            return ConnectionFailureType.DATABASE_UNAVAILABLE;
        } else if (cause instanceof SocketTimeoutException) {
            return ConnectionFailureType.NETWORK_ERROR;
        } else if (cause instanceof SQLException) {
            String message = cause.getMessage();
            if (message != null) {
                if (message.contains("Connection is not available")) {
                    return ConnectionFailureType.POOL_EXHAUSTED;
                } else if (message.contains("Connection is invalid")) {
                    return ConnectionFailureType.VALIDATION_FAILED;
                } else if (message.contains("Connection refused")) {
                    return ConnectionFailureType.DATABASE_UNAVAILABLE;
                } else if (message.contains("timeout")) {
                    return ConnectionFailureType.TIMEOUT;
                }
            }
        }

        // Check for HikariCP specific exceptions
        String className = cause.getClass().getName();
        if (className.contains("HikariPool")) {
            String message = cause.getMessage();
            if (message != null) {
                if (message.contains("Connection is not available")) {
                    return ConnectionFailureType.POOL_EXHAUSTED;
                } else if (message.contains("Connection timeout")) {
                    return ConnectionFailureType.TIMEOUT;
                } else if (message.contains("Connection is invalid")) {
                    return ConnectionFailureType.VALIDATION_FAILED;
                }
            }
            if (className.contains("PoolInitializationException")) {
                return ConnectionFailureType.CONFIGURATION_ERROR;
            }
        }

        return ConnectionFailureType.UNKNOWN;
    }

    /**
     * Determines if retry is recommended based on the error code.
     *
     * @param errorCode the error code
     * @return true if retry is recommended, false otherwise
     */
    private boolean isRetryRecommendedForErrorCode(String errorCode) {
        switch (errorCode) {
            case CONN_POOL_EXHAUSTED:
            case CONN_TIMEOUT:
            case CONN_NETWORK_ERROR:
            case CONN_DB_UNAVAILABLE:
            case CONN_VALIDATION_FAILED:
                return true;
            case CONN_CONFIG_ERROR:
            case CONN_CLOSE_FAILED:
            default:
                return false;
        }
    }

    /**
     * Determines if retry is recommended based on the cause.
     *
     * @param cause the underlying cause
     * @return true if retry is recommended, false otherwise
     */
    private boolean isRetryRecommendedForCause(Throwable cause) {
        if (cause == null) {
            return false;
        }

        if (cause instanceof SQLTimeoutException ||
            cause instanceof SocketTimeoutException ||
            cause instanceof ConnectException) {
            return true;
        }

        String message = cause.getMessage();
        if (message != null) {
            if (message.contains("Connection is not available") ||
                message.contains("Connection timeout") ||
                message.contains("Connection is invalid") ||
                message.contains("Connection refused")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates the recommended retry delay based on the failure type.
     *
     * @param failureType the connection failure type
     * @return the recommended retry delay in milliseconds
     */
    private long calculateRetryDelay(ConnectionFailureType failureType) {
        switch (failureType) {
            case POOL_EXHAUSTED:
                return 500; // 500 milliseconds
            case TIMEOUT:
            case VALIDATION_FAILED:
                return 1000; // 1 second
            case NETWORK_ERROR:
                return 2000; // 2 seconds
            case DATABASE_UNAVAILABLE:
                return 5000; // 5 seconds
            case CONFIGURATION_ERROR:
            case CLOSE_FAILED:
            case UNKNOWN:
            default:
                return 0; // No retry
        }
    }

    /**
     * Returns a string representation of this exception including the failure type and retry information.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        return super.toString() + " [Type: " + failureType + 
               ", Retry: " + (retryRecommended ? "Recommended (delay: " + suggestedRetryDelayMs + "ms)" : "Not recommended") + "]";
    }
}