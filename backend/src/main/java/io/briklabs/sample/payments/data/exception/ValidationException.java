package io.briklabs.sample.payments.data.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exception thrown when payment data validation fails before database operations.
 * <p>
 * This class handles constraint violations, data format errors, business rule violations,
 * and other validation failures. It provides detailed information about validation failures,
 * including the field that failed validation, the invalid value, and the validation rule
 * that was violated.
 * </p>
 * <p>
 * ValidationException helps prevent invalid payment data from being persisted,
 * maintaining data integrity and consistency across the payment system.
 * </p>
 */
public class ValidationException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Prefix for validation error codes.
     */
    private static final String ERROR_CODE_PREFIX = "VAL-";

    /**
     * Map of field names to validation errors.
     */
    private final Map<String, List<ValidationError>> fieldErrors;

    /**
     * List of general validation errors not associated with specific fields.
     */
    private final List<ValidationError> generalErrors;

    /**
     * Creates a new ValidationException with a default error code.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message, ERROR_CODE_PREFIX + "0001");
        this.fieldErrors = new HashMap<>();
        this.generalErrors = new ArrayList<>();
    }

    /**
     * Creates a new ValidationException with a specific error code.
     *
     * @param message the error message
     * @param errorCode the specific error code (without prefix)
     */
    public ValidationException(String message, String errorCode) {
        super(message, ERROR_CODE_PREFIX + errorCode);
        this.fieldErrors = new HashMap<>();
        this.generalErrors = new ArrayList<>();
    }

    /**
     * Creates a new ValidationException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, ERROR_CODE_PREFIX + "0001", cause);
        this.fieldErrors = new HashMap<>();
        this.generalErrors = new ArrayList<>();
    }

    /**
     * Creates a new ValidationException with a specific error code and cause.
     *
     * @param message the error message
     * @param errorCode the specific error code (without prefix)
     * @param cause the underlying cause of the exception
     */
    public ValidationException(String message, String errorCode, Throwable cause) {
        super(message, ERROR_CODE_PREFIX + errorCode, cause);
        this.fieldErrors = new HashMap<>();
        this.generalErrors = new ArrayList<>();
    }

    /**
     * Creates a new ValidationException with a formatted message.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param args the arguments to be formatted into the message pattern
     */
    public ValidationException(String messagePattern, Object... args) {
        super(messagePattern, ERROR_CODE_PREFIX + "0001", args);
        this.fieldErrors = new HashMap<>();
        this.generalErrors = new ArrayList<>();
    }

    /**
     * Creates a new ValidationException with a formatted message and specific error code.
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code (without prefix)
     * @param args the arguments to be formatted into the message pattern
     */
    public ValidationException(String messagePattern, String errorCode, Object... args) {
        super(messagePattern, ERROR_CODE_PREFIX + errorCode, args);
        this.fieldErrors = new HashMap<>();
        this.generalErrors = new ArrayList<>();
    }

    /**
     * Adds a field-specific validation error.
     *
     * @param field the name of the field that failed validation
     * @param errorMessage the validation error message
     * @param invalidValue the invalid value that caused the validation failure
     * @return this exception instance for method chaining
     */
    public ValidationException addFieldError(String field, String errorMessage, Object invalidValue) {
        return addFieldError(field, errorMessage, invalidValue, null);
    }

    /**
     * Adds a field-specific validation error with a rule identifier.
     *
     * @param field the name of the field that failed validation
     * @param errorMessage the validation error message
     * @param invalidValue the invalid value that caused the validation failure
     * @param ruleId the identifier of the validation rule that was violated
     * @return this exception instance for method chaining
     */
    public ValidationException addFieldError(String field, String errorMessage, Object invalidValue, String ruleId) {
        if (!fieldErrors.containsKey(field)) {
            fieldErrors.put(field, new ArrayList<>());
        }
        fieldErrors.get(field).add(new ValidationError(errorMessage, invalidValue, ruleId));
        return this;
    }

    /**
     * Adds a general validation error not associated with a specific field.
     *
     * @param errorMessage the validation error message
     * @return this exception instance for method chaining
     */
    public ValidationException addGeneralError(String errorMessage) {
        return addGeneralError(errorMessage, null, null);
    }

    /**
     * Adds a general validation error with a rule identifier.
     *
     * @param errorMessage the validation error message
     * @param ruleId the identifier of the validation rule that was violated
     * @return this exception instance for method chaining
     */
    public ValidationException addGeneralError(String errorMessage, String ruleId) {
        return addGeneralError(errorMessage, null, ruleId);
    }

    /**
     * Adds a general validation error with context and a rule identifier.
     *
     * @param errorMessage the validation error message
     * @param context additional context about the validation failure
     * @param ruleId the identifier of the validation rule that was violated
     * @return this exception instance for method chaining
     */
    public ValidationException addGeneralError(String errorMessage, Object context, String ruleId) {
        generalErrors.add(new ValidationError(errorMessage, context, ruleId));
        return this;
    }

    /**
     * Adds a business rule violation error.
     *
     * @param ruleName the name of the business rule that was violated
     * @param errorMessage the validation error message
     * @param context additional context about the business rule violation
     * @return this exception instance for method chaining
     */
    public ValidationException addBusinessRuleViolation(String ruleName, String errorMessage, Object context) {
        generalErrors.add(new ValidationError(errorMessage, context, "BUSINESS_RULE:" + ruleName));
        return this;
    }

    /**
     * Gets all field validation errors.
     *
     * @return an unmodifiable map of field names to validation errors
     */
    public Map<String, List<ValidationError>> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }

    /**
     * Gets validation errors for a specific field.
     *
     * @param field the field name
     * @return an unmodifiable list of validation errors for the field, or an empty list if none exist
     */
    public List<ValidationError> getFieldErrors(String field) {
        List<ValidationError> errors = fieldErrors.get(field);
        return errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
    }

    /**
     * Gets all general validation errors.
     *
     * @return an unmodifiable list of general validation errors
     */
    public List<ValidationError> getGeneralErrors() {
        return Collections.unmodifiableList(generalErrors);
    }

    /**
     * Checks if there are any validation errors.
     *
     * @return true if there are any field or general validation errors, false otherwise
     */
    public boolean hasErrors() {
        return !fieldErrors.isEmpty() || !generalErrors.isEmpty();
    }

    /**
     * Gets the total number of validation errors.
     *
     * @return the total number of field and general validation errors
     */
    public int getErrorCount() {
        int count = generalErrors.size();
        for (List<ValidationError> errors : fieldErrors.values()) {
            count += errors.size();
        }
        return count;
    }

    /**
     * Returns a detailed string representation of all validation errors.
     *
     * @return a string containing all validation error details
     */
    public String getDetailedErrorMessage() {
        StringBuilder sb = new StringBuilder(getMessage()).append("\n");
        
        if (!generalErrors.isEmpty()) {
            sb.append("General Errors:\n");
            for (ValidationError error : generalErrors) {
                sb.append(" - ").append(error.toString()).append("\n");
            }
        }
        
        if (!fieldErrors.isEmpty()) {
            sb.append("Field Errors:\n");
            for (Map.Entry<String, List<ValidationError>> entry : fieldErrors.entrySet()) {
                sb.append(" Field: ").append(entry.getKey()).append("\n");
                for (ValidationError error : entry.getValue()) {
                    sb.append("  - ").append(error.toString()).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    /**
     * Inner class representing a single validation error.
     */
    public static class ValidationError {
        private final String message;
        private final Object invalidValue;
        private final String ruleId;

        /**
         * Creates a new ValidationError.
         *
         * @param message the validation error message
         * @param invalidValue the invalid value or context
         * @param ruleId the identifier of the validation rule that was violated
         */
        public ValidationError(String message, Object invalidValue, String ruleId) {
            this.message = message;
            this.invalidValue = invalidValue;
            this.ruleId = ruleId;
        }

        /**
         * Gets the validation error message.
         *
         * @return the error message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Gets the invalid value that caused the validation failure.
         *
         * @return the invalid value or context
         */
        public Object getInvalidValue() {
            return invalidValue;
        }

        /**
         * Gets the identifier of the validation rule that was violated.
         *
         * @return the rule identifier, or null if not specified
         */
        public String getRuleId() {
            return ruleId;
        }

        /**
         * Returns a string representation of this validation error.
         *
         * @return a string representation of the validation error
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(message);
            
            if (invalidValue != null) {
                sb.append(" (Invalid value: ");
                // Mask potentially sensitive payment data in error messages
                if (invalidValue instanceof String && isSensitiveField(ruleId)) {
                    sb.append(maskSensitiveData((String) invalidValue));
                } else {
                    sb.append(invalidValue);
                }
                sb.append(")");
            }
            
            if (ruleId != null) {
                sb.append(" [Rule: ").append(ruleId).append("]");
            }
            
            return sb.toString();
        }

        /**
         * Determines if a field or rule is related to sensitive payment data.
         *
         * @param fieldOrRule the field name or rule ID to check
         * @return true if the field or rule is related to sensitive data, false otherwise
         */
        private boolean isSensitiveField(String fieldOrRule) {
            if (fieldOrRule == null) {
                return false;
            }
            
            String lower = fieldOrRule.toLowerCase();
            return lower.contains("card") || 
                   lower.contains("cvv") || 
                   lower.contains("securitycode") || 
                   lower.contains("password") || 
                   lower.contains("token") ||
                   lower.contains("account") ||
                   lower.contains("routing");
        }

        /**
         * Masks sensitive data for secure error reporting.
         *
         * @param data the sensitive data to mask
         * @return the masked data
         */
        private String maskSensitiveData(String data) {
            if (data == null || data.length() <= 4) {
                return "****";
            }
            
            // Keep only the last 4 characters visible
            return "****" + data.substring(Math.max(0, data.length() - 4));
        }
    }
}