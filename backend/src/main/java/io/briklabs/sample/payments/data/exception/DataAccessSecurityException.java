package io.briklabs.sample.payments.data.exception;

import java.text.MessageFormat;
import java.util.UUID;

/**
 * Exception thrown when security-related issues occur during payment data access operations.
 * <p>
 * This exception handles various security violations including:
 * <ul>
 *   <li>Unauthorized access attempts to payment data</li>
 *   <li>Permission violations during payment operations</li>
 *   <li>Data encryption/decryption failures</li>
 *   <li>Secure attribute access violations</li>
 *   <li>Access Rights module integration failures</li>
 * </ul>
 * </p>
 * <p>
 * The exception provides context about security violations while carefully avoiding
 * exposure of sensitive information. It integrates with security audit logging
 * to ensure proper tracking of security-related events.
 * </p>
 */
public class DataAccessSecurityException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Security event identifier for audit trail correlation.
     */
    private final UUID securityEventId;

    /**
     * The type of security violation that occurred.
     */
    private final SecurityViolationType violationType;

    /**
     * The resource type that was being accessed when the security violation occurred.
     */
    private final String resourceType;

    /**
     * Enumeration of security violation types for classification.
     */
    public enum SecurityViolationType {
        /**
         * User lacks required permissions for the requested operation.
         */
        INSUFFICIENT_PERMISSIONS,

        /**
         * User is not authorized to access the requested resource.
         */
        UNAUTHORIZED_ACCESS,

        /**
         * User is attempting to access data outside their organization or account scope.
         */
        SCOPE_VIOLATION,

        /**
         * Failure in encryption or decryption of sensitive payment data.
         */
        ENCRYPTION_FAILURE,

        /**
         * Attempt to access sensitive payment attributes without proper authorization.
         */
        SENSITIVE_DATA_ACCESS,

        /**
         * Failure in the Access Rights module integration.
         */
        ACCESS_RIGHTS_FAILURE,

        /**
         * Attempt to perform an operation that violates security policy.
         */
        POLICY_VIOLATION,

        /**
         * Other security violations not covered by specific types.
         */
        OTHER
    }

    /**
     * Creates a new DataAccessSecurityException with a default error code.
     *
     * @param message the error message
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     */
    public DataAccessSecurityException(String message, SecurityViolationType violationType, String resourceType) {
        super(message, "SEC-0001");
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new DataAccessSecurityException with a specific error code.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     */
    public DataAccessSecurityException(String message, String errorCode, SecurityViolationType violationType, String resourceType) {
        super(message, errorCode);
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new DataAccessSecurityException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     */
    public DataAccessSecurityException(String message, Throwable cause, SecurityViolationType violationType, String resourceType) {
        super(message, "SEC-0001", cause);
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new DataAccessSecurityException with a specific error code and cause.
     *
     * @param message the error message
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     */
    public DataAccessSecurityException(String message, String errorCode, Throwable cause, SecurityViolationType violationType, String resourceType) {
        super(message, errorCode, cause);
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new DataAccessSecurityException with a formatted message.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     * @param args the arguments to be formatted into the message pattern
     */
    public DataAccessSecurityException(String messagePattern, SecurityViolationType violationType, String resourceType, Object... args) {
        super(messagePattern, "SEC-0001", args);
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new DataAccessSecurityException with a formatted message and specific error code.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     * @param args the arguments to be formatted into the message pattern
     */
    public DataAccessSecurityException(String messagePattern, String errorCode, SecurityViolationType violationType, String resourceType, Object... args) {
        super(messagePattern, errorCode, args);
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new DataAccessSecurityException with a formatted message and cause.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param cause the underlying cause of the exception
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     * @param args the arguments to be formatted into the message pattern
     */
    public DataAccessSecurityException(String messagePattern, Throwable cause, SecurityViolationType violationType, String resourceType, Object... args) {
        super(messagePattern, "SEC-0001", cause, args);
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Creates a new DataAccessSecurityException with a formatted message, specific error code, and cause.
     * <p>
     * This constructor uses {@link MessageFormat} to format the message with the provided arguments.
     * </p>
     *
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code
     * @param cause the underlying cause of the exception
     * @param violationType the type of security violation
     * @param resourceType the type of resource being accessed
     * @param args the arguments to be formatted into the message pattern
     */
    public DataAccessSecurityException(String messagePattern, String errorCode, Throwable cause, SecurityViolationType violationType, String resourceType, Object... args) {
        super(messagePattern, errorCode, cause, args);
        this.securityEventId = UUID.randomUUID();
        this.violationType = violationType;
        this.resourceType = resourceType;
    }

    /**
     * Gets the security event ID for audit trail correlation.
     *
     * @return the security event ID
     */
    public UUID getSecurityEventId() {
        return securityEventId;
    }

    /**
     * Gets the type of security violation that occurred.
     *
     * @return the security violation type
     */
    public SecurityViolationType getViolationType() {
        return violationType;
    }

    /**
     * Gets the type of resource that was being accessed when the security violation occurred.
     *
     * @return the resource type
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Creates a security exception for insufficient permissions.
     *
     * @param resourceType the type of resource being accessed
     * @param requiredPermission the permission that was required
     * @param resourceId the identifier of the resource (optional, can be null)
     * @return a new DataAccessSecurityException
     */
    public static DataAccessSecurityException insufficientPermissions(String resourceType, String requiredPermission, String resourceId) {
        String message = resourceId != null ?
                "Insufficient permissions to access {0} with ID {1}. Required permission: {2}" :
                "Insufficient permissions to access {0}. Required permission: {2}";
        
        return new DataAccessSecurityException(
                message,
                "SEC-1001",
                SecurityViolationType.INSUFFICIENT_PERMISSIONS,
                resourceType,
                resourceType,
                resourceId,
                requiredPermission
        );
    }

    /**
     * Creates a security exception for unauthorized access.
     *
     * @param resourceType the type of resource being accessed
     * @param resourceId the identifier of the resource (optional, can be null)
     * @return a new DataAccessSecurityException
     */
    public static DataAccessSecurityException unauthorizedAccess(String resourceType, String resourceId) {
        String message = resourceId != null ?
                "Unauthorized access to {0} with ID {1}" :
                "Unauthorized access to {0}";
        
        return new DataAccessSecurityException(
                message,
                "SEC-1002",
                SecurityViolationType.UNAUTHORIZED_ACCESS,
                resourceType,
                resourceType,
                resourceId
        );
    }

    /**
     * Creates a security exception for organization or account scope violations.
     *
     * @param resourceType the type of resource being accessed
     * @param resourceId the identifier of the resource
     * @param allowedScope the scope the user is allowed to access
     * @param attemptedScope the scope the user attempted to access
     * @return a new DataAccessSecurityException
     */
    public static DataAccessSecurityException scopeViolation(String resourceType, String resourceId, String allowedScope, String attemptedScope) {
        return new DataAccessSecurityException(
                "Scope violation: Attempted to access {0} with ID {1} in scope {2} but user is restricted to scope {3}",
                "SEC-1003",
                SecurityViolationType.SCOPE_VIOLATION,
                resourceType,
                resourceType,
                resourceId,
                attemptedScope,
                allowedScope
        );
    }

    /**
     * Creates a security exception for encryption or decryption failures.
     *
     * @param resourceType the type of resource being accessed
     * @param operation the encryption operation that failed (encrypt/decrypt)
     * @param cause the underlying cause of the encryption failure
     * @return a new DataAccessSecurityException
     */
    public static DataAccessSecurityException encryptionFailure(String resourceType, String operation, Throwable cause) {
        return new DataAccessSecurityException(
                "Failed to {0} sensitive data for {1}",
                "SEC-1004",
                cause,
                SecurityViolationType.ENCRYPTION_FAILURE,
                resourceType,
                operation,
                resourceType
        );
    }

    /**
     * Creates a security exception for unauthorized access to sensitive payment attributes.
     *
     * @param resourceType the type of resource being accessed
     * @param attributeName the name of the sensitive attribute
     * @param requiredRole the role required to access the attribute
     * @return a new DataAccessSecurityException
     */
    public static DataAccessSecurityException sensitiveDataAccess(String resourceType, String attributeName, String requiredRole) {
        return new DataAccessSecurityException(
                "Unauthorized access to sensitive attribute {0} of {1}. Required role: {2}",
                "SEC-1005",
                SecurityViolationType.SENSITIVE_DATA_ACCESS,
                resourceType,
                attributeName,
                resourceType,
                requiredRole
        );
    }

    /**
     * Creates a security exception for Access Rights module integration failures.
     *
     * @param resourceType the type of resource being accessed
     * @param operation the operation being performed
     * @param cause the underlying cause of the integration failure
     * @return a new DataAccessSecurityException
     */
    public static DataAccessSecurityException accessRightsFailure(String resourceType, String operation, Throwable cause) {
        return new DataAccessSecurityException(
                "Failed to verify access rights for {0} operation on {1}",
                "SEC-1006",
                cause,
                SecurityViolationType.ACCESS_RIGHTS_FAILURE,
                resourceType,
                operation,
                resourceType
        );
    }

    /**
     * Creates a security exception for security policy violations.
     *
     * @param resourceType the type of resource being accessed
     * @param policyName the name of the security policy that was violated
     * @param details additional details about the violation (optional, can be null)
     * @return a new DataAccessSecurityException
     */
    public static DataAccessSecurityException policyViolation(String resourceType, String policyName, String details) {
        String message = details != null ?
                "Security policy violation: {0} for {1}. Details: {2}" :
                "Security policy violation: {0} for {1}";
        
        return new DataAccessSecurityException(
                message,
                "SEC-1007",
                SecurityViolationType.POLICY_VIOLATION,
                resourceType,
                policyName,
                resourceType,
                details
        );
    }

    /**
     * Returns a string representation of this exception including the security event ID
     * and violation type, but carefully avoiding inclusion of sensitive information.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        return getClass().getName() + " [" + getErrorCode() + ", Event: " + securityEventId + 
               ", Type: " + violationType + ", Resource: " + resourceType + "]: " + getMessage();
    }

    /**
     * Returns a sanitized message suitable for logging that doesn't expose sensitive details.
     * This method ensures that security-related exceptions can be safely logged without
     * revealing information that could be useful to attackers.
     *
     * @return a sanitized message for logging
     */
    public String getSanitizedMessage() {
        // Create a sanitized message that doesn't include potentially sensitive details
        return MessageFormat.format(
                "Security violation of type {0} occurred when accessing {1} resource. Event ID: {2}",
                violationType,
                resourceType,
                securityEventId
        );
    }

    /**
     * Returns a detailed security event record suitable for audit logging.
     * This method provides comprehensive information about the security violation
     * for security monitoring and compliance purposes.
     *
     * @param userId the ID of the user who triggered the security violation
     * @param ipAddress the IP address from which the request originated
     * @param requestPath the API path that was being accessed
     * @return a formatted security event record
     */
    public String getAuditRecord(String userId, String ipAddress, String requestPath) {
        return MessageFormat.format(
                "SECURITY_VIOLATION|{0}|{1}|{2}|{3}|{4}|{5}|{6}|{7}",
                securityEventId,
                System.currentTimeMillis(),
                userId,
                ipAddress,
                requestPath,
                getErrorCode(),
                violationType,
                resourceType
        );
    }
}