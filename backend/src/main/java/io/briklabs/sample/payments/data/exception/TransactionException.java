package io.briklabs.sample.payments.data.exception;

/**
 * Exception thrown when database transaction processing fails during payment operations.
 * <p>
 * This class handles transaction begin/commit/rollback failures, isolation level issues,
 * and other transaction management errors. It provides context about the transaction state,
 * operations attempted, and appropriate recovery actions.
 * </p>
 * <p>
 * This exception is critical for ensuring payment data consistency during failures,
 * as it helps maintain the atomic nature of financial transactions.
 * </p>
 */
public class TransactionException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Enum representing the state of the transaction when the exception occurred.
     */
    public enum TransactionState {
        /** Transaction had not yet been started */
        NOT_STARTED,
        
        /** Transaction was started but neither committed nor rolled back */
        STARTED,
        
        /** Transaction commit was attempted but failed */
        COMMIT_FAILED,
        
        /** Transaction rollback was attempted but failed */
        ROLLBACK_FAILED,
        
        /** Transaction state is unknown or cannot be determined */
        UNKNOWN
    }

    /**
     * The state of the transaction when the exception occurred.
     */
    private final TransactionState transactionState;
    
    /**
     * The operation that was being attempted when the transaction failed.
     */
    private final String operation;

    /**
     * Creates a new TransactionException with a default error code.
     *
     * @param message the error message
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     */
    public TransactionException(String message, TransactionState transactionState, String operation) {
        super(message, "TX-0001");
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Creates a new TransactionException with a specific error code.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     */
    public TransactionException(String message, String errorCode, TransactionState transactionState, String operation) {
        super(message, errorCode);
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Creates a new TransactionException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     */
    public TransactionException(String message, Throwable cause, TransactionState transactionState, String operation) {
        super(message, "TX-0001", cause);
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Creates a new TransactionException with a specific error code and cause.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     */
    public TransactionException(String message, String errorCode, Throwable cause, TransactionState transactionState, String operation) {
        super(message, errorCode, cause);
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Creates a new TransactionException with a formatted message.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     * @param args the arguments to be formatted into the message pattern
     */
    public TransactionException(String messagePattern, TransactionState transactionState, String operation, Object... args) {
        super(messagePattern, "TX-0001", args);
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Creates a new TransactionException with a formatted message and specific error code.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     * @param args the arguments to be formatted into the message pattern
     */
    public TransactionException(String messagePattern, String errorCode, TransactionState transactionState, String operation, Object... args) {
        super(messagePattern, errorCode, args);
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Creates a new TransactionException with a formatted message and cause.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param cause the underlying cause of the exception
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     * @param args the arguments to be formatted into the message pattern
     */
    public TransactionException(String messagePattern, Throwable cause, TransactionState transactionState, String operation, Object... args) {
        super(messagePattern, "TX-0001", cause, args);
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Creates a new TransactionException with a formatted message, specific error code, and cause.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     * @param transactionState the state of the transaction when the exception occurred
     * @param operation the operation that was being attempted
     * @param args the arguments to be formatted into the message pattern
     */
    public TransactionException(String messagePattern, String errorCode, Throwable cause, TransactionState transactionState, String operation, Object... args) {
        super(messagePattern, errorCode, cause, args);
        this.transactionState = transactionState;
        this.operation = operation;
    }

    /**
     * Factory method for creating a transaction begin failure exception.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param operation the operation that was being attempted
     * @return a new TransactionException for begin failure
     */
    public static TransactionException beginFailed(String message, Throwable cause, String operation) {
        return new TransactionException(message, "TX-1001", cause, TransactionState.NOT_STARTED, operation);
    }

    /**
     * Factory method for creating a transaction commit failure exception.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param operation the operation that was being attempted
     * @return a new TransactionException for commit failure
     */
    public static TransactionException commitFailed(String message, Throwable cause, String operation) {
        return new TransactionException(message, "TX-2001", cause, TransactionState.COMMIT_FAILED, operation);
    }

    /**
     * Factory method for creating a transaction rollback failure exception.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param operation the operation that was being attempted
     * @return a new TransactionException for rollback failure
     */
    public static TransactionException rollbackFailed(String message, Throwable cause, String operation) {
        return new TransactionException(message, "TX-3001", cause, TransactionState.ROLLBACK_FAILED, operation);
    }

    /**
     * Factory method for creating a transaction isolation level exception.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param operation the operation that was being attempted
     * @return a new TransactionException for isolation level issues
     */
    public static TransactionException isolationLevelError(String message, Throwable cause, String operation) {
        return new TransactionException(message, "TX-4001", cause, TransactionState.STARTED, operation);
    }

    /**
     * Factory method for creating a transaction timeout exception.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param operation the operation that was being attempted
     * @return a new TransactionException for transaction timeout
     */
    public static TransactionException transactionTimeout(String message, Throwable cause, String operation) {
        return new TransactionException(message, "TX-5001", cause, TransactionState.STARTED, operation);
    }

    /**
     * Gets the state of the transaction when the exception occurred.
     *
     * @return the transaction state
     */
    public TransactionState getTransactionState() {
        return transactionState;
    }

    /**
     * Gets the operation that was being attempted when the transaction failed.
     *
     * @return the operation description
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Determines if a transaction rollback should be attempted based on the transaction state.
     *
     * @return true if rollback should be attempted, false otherwise
     */
    public boolean shouldAttemptRollback() {
        return transactionState == TransactionState.STARTED;
    }

    /**
     * Determines if the transaction failure might have left the database in an inconsistent state.
     *
     * @return true if data consistency might be compromised, false otherwise
     */
    public boolean mightHaveInconsistentState() {
        return transactionState == TransactionState.COMMIT_FAILED || 
               transactionState == TransactionState.ROLLBACK_FAILED;
    }

    /**
     * Returns a string representation of this exception including the transaction state and operation.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        return super.toString() + " [State: " + transactionState + ", Operation: " + operation + "]";
    }
}