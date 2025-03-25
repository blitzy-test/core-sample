package io.briklabs.sample.payments.data.exception;

import java.text.MessageFormat;

/**
 * Base exception class for all payment data access layer exceptions.
 * <p>
 * This class serves as the foundation for the payment data access exception hierarchy,
 * providing common functionality such as error codes, message formatting, and cause tracking.
 * It enables consistent error handling patterns across the payment data access layer,
 * allowing for proper error classification, logging, and recovery.
 * </p>
 * <p>
 * All specialized payment data exceptions should extend this class to maintain
 * a consistent exception handling approach throughout the payment module.
 * </p>
 */
public class PaymentDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code identifying the specific type of data access error.
     * Error codes are categorized by their prefix:
     * <ul>
     *   <li>CONN-*: Connection-related errors</li>
     *   <li>QUERY-*: Query execution errors</li>
     *   <li>TX-*: Transaction management errors</li>
     *   <li>VAL-*: Validation errors</li>
     *   <li>SEC-*: Security-related errors</li>
     *   <li>DATA-*: General data access errors</li>
     * </ul>
     */
    private final String errorCode;

    /**
     * Creates a new PaymentDataException with a default error code.
     *
     * @param message the error message
     */
    public PaymentDataException(String message) {
        super(message);
        this.errorCode = "DATA-0001";
    }

    /**
     * Creates a new PaymentDataException with a specific error code.
     *
     * @param message the error message
     * @param errorCode the specific error code
     */
    public PaymentDataException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a new PaymentDataException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     */
    public PaymentDataException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DATA-0001";
    }

    /**
     * Creates a new PaymentDataException with a specific error code and cause.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     */
    public PaymentDataException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Creates a new PaymentDataException with a formatted message.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param args the arguments to be formatted into the message pattern
     */
    public PaymentDataException(String messagePattern, Object... args) {
        super(MessageFormat.format(messagePattern, args));
        this.errorCode = "DATA-0001";
    }

    /**
     * Creates a new PaymentDataException with a formatted message and specific error code.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param args the arguments to be formatted into the message pattern
     */
    public PaymentDataException(String messagePattern, String errorCode, Object... args) {
        super(MessageFormat.format(messagePattern, args));
        this.errorCode = errorCode;
    }

    /**
     * Creates a new PaymentDataException with a formatted message and cause.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param cause the underlying cause of the exception
     * @param args the arguments to be formatted into the message pattern
     */
    public PaymentDataException(String messagePattern, Throwable cause, Object... args) {
        super(MessageFormat.format(messagePattern, args), cause);
        this.errorCode = "DATA-0001";
    }

    /**
     * Creates a new PaymentDataException with a formatted message, specific error code, and cause.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     * @param args the arguments to be formatted into the message pattern
     */
    public PaymentDataException(String messagePattern, String errorCode, Throwable cause, Object... args) {
        super(MessageFormat.format(messagePattern, args), cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns a string representation of this exception including the error code.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        return getClass().getName() + " [" + errorCode + "]: " + getMessage();
    }
}