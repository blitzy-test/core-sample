package io.briklabs.sample.rest;

import com.zaxxer.hikari.HikariDataSource;
import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.rest.TransactionResource;
import io.briklabs.sample.payments.rest.TransactionProcessingResource;
import io.briklabs.sample.payments.rest.TransactionEventResource;
import io.briklabs.sample.payments.service.PaymentTransactionService;
import io.briklabs.sample.payments.service.PaymentQueryService;
import io.briklabs.sample.payments.service.PaymentValidationService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routing configuration for payment-related REST endpoints.
 * <p>
 * This class serves as a bridge between the core REST application and the specialized
 * payment endpoints, mapping URI patterns to their respective resources. It handles
 * special path parameters like the '_all' placeholder for retrieving data across
 * accounts or transactions.
 * </p>
 * <p>
 * The routing follows the pattern: /organizations/{org_id}/accounts/{account_id}/transactions/
 * </p>
 */
@Provider
@OpenAPIDefinition(
    info = @Info(
        title = "Payment API",
        version = "1.0.0",
        description = "API for payment transaction processing"
    ),
    tags = {
        @Tag(name = "Payments", description = "Payment transaction operations")
    }
)
public class PaymentRestRouting implements ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(PaymentRestRouting.class);
    
    // URI pattern for payment endpoints
    private static final Pattern PAYMENT_URI_PATTERN = Pattern.compile(
            "/organizations/([^/]+)/accounts/([^/]+)/transactions(?:/([^/]+))?(?:/([^/]+))?");
    
    private final HikariDataSource dataSource;
    private final PaymentTransactionService transactionService;
    private final PaymentQueryService queryService;
    private final PaymentValidationService validationService;
    
    /**
     * Constructor with HikariDataSource dependency.
     * 
     * @param dataSource The HikariCP connection pool for database operations
     */
    public PaymentRestRouting(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        
        // Initialize services using the DAO factory
        PaymentDAOFactory daoFactory = new PaymentDAOFactory(dataSource);
        this.transactionService = new PaymentTransactionService(daoFactory);
        this.queryService = new PaymentQueryService(daoFactory);
        this.validationService = new PaymentValidationService();
        
        logger.info("Payment REST routing initialized");
    }
    
    /**
     * Filters incoming requests to handle payment-specific routing.
     * <p>
     * This method intercepts requests matching the payment URI pattern and performs
     * validation and preprocessing before the request reaches the appropriate resource.
     * It handles special path parameters like '_all' for retrieving data across
     * accounts or transactions.
     * </p>
     * 
     * @param requestContext The container request context
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        
        // Check if the request matches the payment URI pattern
        Matcher matcher = PAYMENT_URI_PATTERN.matcher(path);
        if (!matcher.matches()) {
            // Not a payment endpoint, continue with normal processing
            return;
        }
        
        logger.debug("Processing payment request: {}", path);
        
        try {
            // Extract path parameters
            String orgId = matcher.group(1);
            String accountId = matcher.group(2);
            String transactionId = matcher.group(3); // May be null for collection endpoints
            String action = matcher.group(4); // May be null for standard endpoints
            
            // Validate organization ID
            try {
                UUID.fromString(orgId);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid organization ID format: {}", orgId);
                sendErrorResponse(requestContext, Response.Status.BAD_REQUEST, 
                        "Invalid organization ID format");
                return;
            }
            
            // Validate account ID (unless it's the special '_all' placeholder)
            if (!"_all".equals(accountId)) {
                try {
                    UUID.fromString(accountId);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid account ID format: {}", accountId);
                    sendErrorResponse(requestContext, Response.Status.BAD_REQUEST, 
                            "Invalid account ID format");
                    return;
                }
            }
            
            // Validate transaction ID if present (unless it's the special '_all' placeholder)
            if (transactionId != null && !"_all".equals(transactionId)) {
                try {
                    UUID.fromString(transactionId);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid transaction ID format: {}", transactionId);
                    sendErrorResponse(requestContext, Response.Status.BAD_REQUEST, 
                            "Invalid transaction ID format");
                    return;
                }
            }
            
            // Check for valid actions if specified
            if (action != null && !isValidAction(action)) {
                logger.warn("Invalid payment action: {}", action);
                sendErrorResponse(requestContext, Response.Status.NOT_FOUND, 
                        "Invalid payment action");
                return;
            }
            
            // Store validated parameters in request properties for use by resources
            requestContext.setProperty("organizationId", orgId);
            requestContext.setProperty("accountId", accountId);
            if (transactionId != null) {
                requestContext.setProperty("transactionId", transactionId);
            }
            if (action != null) {
                requestContext.setProperty("action", action);
            }
            
            // Set services in request properties for dependency injection
            requestContext.setProperty("transactionService", transactionService);
            requestContext.setProperty("queryService", queryService);
            requestContext.setProperty("validationService", validationService);
            
            // Integrate with Access Rights module for payment endpoint authorization
            if (!checkAccessRights(requestContext)) {
                logger.warn("Access denied for payment operation");
                sendErrorResponse(requestContext, Response.Status.FORBIDDEN, 
                        "Insufficient permissions for this payment operation");
                return;
            }
            
            logger.debug("Payment request validated and routed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing payment request", e);
            sendErrorResponse(requestContext, Response.Status.INTERNAL_SERVER_ERROR, 
                    "Error processing payment request: " + e.getMessage());
        }
    }
    
    /**
     * Checks if the specified action is valid for payment transactions.
     * 
     * @param action The action to validate
     * @return true if the action is valid, false otherwise
     */
    private boolean isValidAction(String action) {
        return "process".equals(action) || 
               "capture".equals(action) || 
               "refund".equals(action) || 
               "void".equals(action) || 
               "events".equals(action);
    }
    
    /**
     * Checks access rights for the current request against the Access Rights module.
     * 
     * @param requestContext The container request context
     * @return true if access is granted, false otherwise
     */
    private boolean checkAccessRights(ContainerRequestContext requestContext) {
        // This is a placeholder for integration with the Access Rights module
        // In a real implementation, this would check the user's permissions
        // against the requested operation and resource
        
        // For now, we'll assume access is granted
        // TODO: Implement actual integration with Access Rights module
        return true;
    }
    
    /**
     * Sends an error response and aborts the request processing.
     * 
     * @param requestContext The container request context
     * @param status The HTTP status code
     * @param message The error message
     */
    private void sendErrorResponse(ContainerRequestContext requestContext, 
                                  Response.Status status, 
                                  String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        
        requestContext.abortWith(
                Response.status(status)
                        .entity(errorResponse)
                        .build());
    }
    
    /**
     * Gets the transaction service instance.
     * 
     * @return The transaction service
     */
    public PaymentTransactionService getTransactionService() {
        return transactionService;
    }
    
    /**
     * Gets the query service instance.
     * 
     * @return The query service
     */
    public PaymentQueryService getQueryService() {
        return queryService;
    }
    
    /**
     * Gets the validation service instance.
     * 
     * @return The validation service
     */
    public PaymentValidationService getValidationService() {
        return validationService;
    }
}