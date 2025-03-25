package io.briklabs.sample.payments.data.exception;

import java.util.Optional;

/**
 * Exception thrown when concurrent data access issues occur during payment processing.
 * <p>
 * This class handles optimistic locking failures, deadlocks, serialization failures,
 * and other concurrency control problems. It provides context about the conflicting
 * operations, versioning information, and suggested retry strategies.
 * </p>
 * <p>
 * This exception is crucial for managing concurrent payment operations safely,
 * ensuring that conflicting operations don't compromise payment data integrity
 * or create race conditions.
 * </p>
 */
public class ConcurrencyException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Enum representing the type of concurrency conflict that occurred.
     */
    public enum ConflictType {
        /** Optimistic locking failure due to version mismatch */
        OPTIMISTIC_LOCK_FAILURE,
        
        /** Database deadlock detected */
        DEADLOCK,
        
        /** Serialization failure in transaction isolation */
        SERIALIZATION_FAILURE,
        
        /** Row-level locking conflict */
        ROW_LOCK_CONFLICT,
        
        /** Table-level locking conflict */
        TABLE_LOCK_CONFLICT,
        
        /** Other concurrency-related conflict */
        OTHER
    }

    /**
     * Enum representing retry strategy recommendations.
     */
    public enum RetryStrategy {
        /** Immediate retry may succeed */
        RETRY_IMMEDIATELY,
        
        /** Retry with exponential backoff */
        RETRY_WITH_BACKOFF,
        
        /** Retry after a specific delay */
        RETRY_AFTER_DELAY,
        
        /** Do not retry automatically */
        NO_RETRY
    }

    /** The type of concurrency conflict that occurred */
    private final ConflictType conflictType;
    
    /** The entity type involved in the conflict */
    private final String entityType;
    
    /** The identifier of the entity involved in the conflict */
    private final String entityId;
    
    /** The expected version for optimistic locking conflicts */
    private final Long expectedVersion;
    
    /** The actual version found for optimistic locking conflicts */
    private final Long actualVersion;
    
    /** The operation that was attempted when the conflict occurred */
    private final String operation;
    
    /** The recommended retry strategy */
    private final RetryStrategy retryStrategy;
    
    /** The recommended delay in milliseconds before retrying (if applicable) */
    private final Long retryDelayMs;

    /**
     * Creates a new ConcurrencyException with detailed conflict information.
     *
     * @param message the error message
     * @param conflictType the type of concurrency conflict
     * @param entityType the entity type involved in the conflict
     * @param entityId the identifier of the entity involved
     * @param operation the operation that was attempted
     * @param retryStrategy the recommended retry strategy
     * @param expectedVersion the expected version (for optimistic locking)
     * @param actualVersion the actual version found (for optimistic locking)
     * @param retryDelayMs the recommended delay before retrying (if applicable)
     */
    private ConcurrencyException(String message, String errorCode, Throwable cause,
                                ConflictType conflictType, String entityType, String entityId,
                                String operation, RetryStrategy retryStrategy,
                                Long expectedVersion, Long actualVersion, Long retryDelayMs) {
        super(message, errorCode, cause);
        this.conflictType = conflictType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.operation = operation;
        this.retryStrategy = retryStrategy;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.retryDelayMs = retryDelayMs;
    }

    /**
     * Factory method for creating an optimistic locking failure exception.
     *
     * @param entityType the entity type involved in the conflict
     * @param entityId the identifier of the entity involved
     * @param operation the operation that was attempted
     * @param expectedVersion the expected version
     * @param actualVersion the actual version found
     * @param cause the underlying cause of the exception
     * @return a new ConcurrencyException for optimistic locking failure
     */
    public static ConcurrencyException optimisticLockFailure(
            String entityType, String entityId, String operation,
            long expectedVersion, long actualVersion, Throwable cause) {
        
        String message = String.format(
                "Optimistic locking failure for %s with ID %s during %s operation. " +
                "Expected version: %d, Actual version: %d",
                entityType, entityId, operation, expectedVersion, actualVersion);
        
        return new ConcurrencyException(
                message, "CONC-1001", cause,
                ConflictType.OPTIMISTIC_LOCK_FAILURE, entityType, entityId,
                operation, RetryStrategy.RETRY_WITH_BACKOFF,
                expectedVersion, actualVersion, 500L);
    }

    /**
     * Factory method for creating a deadlock exception.
     *
     * @param entityType the entity type involved in the conflict
     * @param entityId the identifier of the entity involved
     * @param operation the operation that was attempted
     * @param cause the underlying cause of the exception
     * @return a new ConcurrencyException for deadlock
     */
    public static ConcurrencyException deadlock(
            String entityType, String entityId, String operation, Throwable cause) {
        
        String message = String.format(
                "Database deadlock detected for %s with ID %s during %s operation",
                entityType, entityId, operation);
        
        return new ConcurrencyException(
                message, "CONC-2001", cause,
                ConflictType.DEADLOCK, entityType, entityId,
                operation, RetryStrategy.RETRY_WITH_BACKOFF,
                null, null, 1000L);
    }

    /**
     * Factory method for creating a serialization failure exception.
     *
     * @param entityType the entity type involved in the conflict
     * @param entityId the identifier of the entity involved
     * @param operation the operation that was attempted
     * @param cause the underlying cause of the exception
     * @return a new ConcurrencyException for serialization failure
     */
    public static ConcurrencyException serializationFailure(
            String entityType, String entityId, String operation, Throwable cause) {
        
        String message = String.format(
                "Transaction serialization failure for %s with ID %s during %s operation",
                entityType, entityId, operation);
        
        return new ConcurrencyException(
                message, "CONC-3001", cause,
                ConflictType.SERIALIZATION_FAILURE, entityType, entityId,
                operation, RetryStrategy.RETRY_IMMEDIATELY,
                null, null, 100L);
    }

    /**
     * Factory method for creating a row lock conflict exception.
     *
     * @param entityType the entity type involved in the conflict
     * @param entityId the identifier of the entity involved
     * @param operation the operation that was attempted
     * @param cause the underlying cause of the exception
     * @return a new ConcurrencyException for row lock conflict
     */
    public static ConcurrencyException rowLockConflict(
            String entityType, String entityId, String operation, Throwable cause) {
        
        String message = String.format(
                "Row lock conflict for %s with ID %s during %s operation",
                entityType, entityId, operation);
        
        return new ConcurrencyException(
                message, "CONC-4001", cause,
                ConflictType.ROW_LOCK_CONFLICT, entityType, entityId,
                operation, RetryStrategy.RETRY_AFTER_DELAY,
                null, null, 2000L);
    }

    /**
     * Factory method for creating a table lock conflict exception.
     *
     * @param entityType the entity type involved in the conflict
     * @param entityId the identifier of the entity involved
     * @param operation the operation that was attempted
     * @param cause the underlying cause of the exception
     * @return a new ConcurrencyException for table lock conflict
     */
    public static ConcurrencyException tableLockConflict(
            String entityType, String entityId, String operation, Throwable cause) {
        
        String message = String.format(
                "Table lock conflict for %s during %s operation",
                entityType, operation);
        
        return new ConcurrencyException(
                message, "CONC-5001", cause,
                ConflictType.TABLE_LOCK_CONFLICT, entityType, entityId,
                operation, RetryStrategy.RETRY_AFTER_DELAY,
                null, null, 5000L);
    }

    /**
     * Factory method for creating a generic concurrency exception.
     *
     * @param message the error message
     * @param entityType the entity type involved in the conflict
     * @param entityId the identifier of the entity involved
     * @param operation the operation that was attempted
     * @param cause the underlying cause of the exception
     * @return a new ConcurrencyException for other concurrency issues
     */
    public static ConcurrencyException other(
            String message, String entityType, String entityId, String operation, Throwable cause) {
        
        return new ConcurrencyException(
                message, "CONC-9001", cause,
                ConflictType.OTHER, entityType, entityId,
                operation, RetryStrategy.NO_RETRY,
                null, null, null);
    }

    /**
     * Gets the type of concurrency conflict that occurred.
     *
     * @return the conflict type
     */
    public ConflictType getConflictType() {
        return conflictType;
    }

    /**
     * Gets the entity type involved in the conflict.
     *
     * @return the entity type
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Gets the identifier of the entity involved in the conflict.
     *
     * @return the entity identifier
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Gets the operation that was attempted when the conflict occurred.
     *
     * @return the operation description
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Gets the recommended retry strategy.
     *
     * @return the retry strategy
     */
    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    /**
     * Gets the expected version for optimistic locking conflicts.
     *
     * @return an Optional containing the expected version, or empty if not applicable
     */
    public Optional<Long> getExpectedVersion() {
        return Optional.ofNullable(expectedVersion);
    }

    /**
     * Gets the actual version found for optimistic locking conflicts.
     *
     * @return an Optional containing the actual version, or empty if not applicable
     */
    public Optional<Long> getActualVersion() {
        return Optional.ofNullable(actualVersion);
    }

    /**
     * Gets the recommended delay in milliseconds before retrying.
     *
     * @return an Optional containing the retry delay, or empty if not applicable
     */
    public Optional<Long> getRetryDelayMs() {
        return Optional.ofNullable(retryDelayMs);
    }

    /**
     * Determines if the conflict is likely to be resolved by retrying the operation.
     *
     * @return true if retry might succeed, false otherwise
     */
    public boolean isRetryable() {
        return retryStrategy != RetryStrategy.NO_RETRY;
    }

    /**
     * Determines if the conflict is related to optimistic locking.
     *
     * @return true if it's an optimistic locking conflict, false otherwise
     */
    public boolean isOptimisticLockingConflict() {
        return conflictType == ConflictType.OPTIMISTIC_LOCK_FAILURE;
    }

    /**
     * Determines if the conflict is related to database locking.
     *
     * @return true if it's a locking-related conflict, false otherwise
     */
    public boolean isLockingConflict() {
        return conflictType == ConflictType.DEADLOCK ||
               conflictType == ConflictType.ROW_LOCK_CONFLICT ||
               conflictType == ConflictType.TABLE_LOCK_CONFLICT;
    }

    /**
     * Returns a string representation of this exception including the conflict details.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" [Conflict: ").append(conflictType);
        sb.append(", Entity: ").append(entityType);
        sb.append(", ID: ").append(entityId);
        sb.append(", Operation: ").append(operation);
        
        if (expectedVersion != null && actualVersion != null) {
            sb.append(", Expected Version: ").append(expectedVersion);
            sb.append(", Actual Version: ").append(actualVersion);
        }
        
        sb.append(", Retry Strategy: ").append(retryStrategy);
        
        if (retryDelayMs != null) {
            sb.append(", Retry Delay: ").append(retryDelayMs).append("ms");
        }
        
        sb.append("]");
        return sb.toString();
    }
}