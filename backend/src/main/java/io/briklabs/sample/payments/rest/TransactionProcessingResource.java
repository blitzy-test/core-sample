package io.briklabs.sample.payments.rest;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.service.PaymentCaptureService;
import io.briklabs.sample.payments.service.PaymentEventService;
import io.briklabs.sample.payments.service.PaymentLifecycleService;
import io.briklabs.sample.payments.service.PaymentRefundService;
import io.briklabs.sample.payments.service.PaymentTransactionService;
import io.briklabs.sample.payments.service.PaymentValidationService;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAX-RS resource for payment transaction lifecycle operations including processing, capturing, and refunding.
 * This resource handles state transitions throughout the payment lifecycle following the pattern:
 * /organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}/(process|capture|refund)
 */
@Path("/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionProcessingResource {
    
    private static final Logger LOGGER = Logger.getLogger(TransactionProcessingResource.class.getName());
    
    private final PaymentTransactionService transactionService;
    private final PaymentLifecycleService lifecycleService;
    private final PaymentCaptureService captureService;
    private final PaymentRefundService refundService;
    private final PaymentEventService eventService;
    private final PaymentValidationService validationService;
    
    /**
     * Constructor with dependency injection for required services.
     */
    public TransactionProcessingResource(
            PaymentTransactionService transactionService,
            PaymentLifecycleService lifecycleService,
            PaymentCaptureService captureService,
            PaymentRefundService refundService,
            PaymentEventService eventService,
            PaymentValidationService validationService) {
        this.transactionService = transactionService;
        this.lifecycleService = lifecycleService;
        this.captureService = captureService;
        this.refundService = refundService;
        this.eventService = eventService;
        this.validationService = validationService;
    }
    
    /**
     * Process a payment transaction, transitioning it from CREATED to PROCESSING state.
     * This is typically the first step in the payment lifecycle after creation.
     *
     * @param orgId Organization ID
     * @param accountId Account ID (or "_all" for all accounts)
     * @param transactionId Transaction ID to process
     * @param operationRef Optional idempotency key for operation
     * @param securityContext Security context for authorization
     * @return Response with updated transaction data
     */
    @POST
    @Path("/process")
    public Response processTransaction(
            @PathParam("org_id") UUID orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") UUID transactionId,
            @QueryParam("operation_ref") String operationRef,
            @Context SecurityContext securityContext) {
        
        LOGGER.log(Level.INFO, "Processing transaction {0} for organization {1}, account {2}",
                new Object[]{transactionId, orgId, accountId});
        
        // Validate access rights
        if (!hasAccessRights(securityContext, orgId, accountId, "PROCESS_PAYMENT")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Insufficient permissions to process this transaction\"}")
                    .build();
        }
        
        try {
            // Check if transaction exists
            PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
            if (transaction == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Transaction not found\"}")
                        .build();
            }
            
            // Validate organization and account match
            if (!transaction.getOrganizationId().equals(orgId) || 
                    (!accountId.equals("_all") && !transaction.getAccountId().toString().equals(accountId))) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Transaction does not belong to the specified organization or account\"}")
                        .build();
            }
            
            // Validate current status allows processing
            if (!lifecycleService.canTransitionTo(transaction, PaymentStatus.PROCESSING)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Cannot process transaction in current state: " + 
                                transaction.getStatus() + "\"}")
                        .build();
            }
            
            // Process the transaction
            PaymentTransaction processedTransaction = lifecycleService.transitionStatus(
                    transaction, 
                    PaymentStatus.PROCESSING, 
                    securityContext.getUserPrincipal().getName(),
                    operationRef);
            
            // Return the updated transaction
            return Response.ok(processedTransaction).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing transaction: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to process transaction: " + e.getMessage() + "\"}")
                    .build();
        }
    }
    
    /**
     * Capture a payment transaction, transitioning it from AUTHORIZED to CAPTURED state.
     * Supports both full and partial captures with amount validation.
     *
     * @param orgId Organization ID
     * @param accountId Account ID (or "_all" for all accounts)
     * @param transactionId Transaction ID to capture
     * @param captureRequest Capture details including amount
     * @param operationRef Optional idempotency key for operation
     * @param securityContext Security context for authorization
     * @param asyncResponse Async response for long-running operation
     */
    @POST
    @Path("/capture")
    public void captureTransaction(
            @PathParam("org_id") UUID orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") UUID transactionId,
            CaptureRequest captureRequest,
            @QueryParam("operation_ref") String operationRef,
            @Context SecurityContext securityContext,
            @Suspended AsyncResponse asyncResponse) {
        
        LOGGER.log(Level.INFO, "Capturing transaction {0} for organization {1}, account {2}, amount {3}",
                new Object[]{transactionId, orgId, accountId, 
                        captureRequest != null ? captureRequest.getAmount() : "full amount"});
        
        // Set timeout for async response
        asyncResponse.setTimeout(30, TimeUnit.SECONDS);
        
        // Validate access rights
        if (!hasAccessRights(securityContext, orgId, accountId, "CAPTURE_PAYMENT")) {
            asyncResponse.resume(Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Insufficient permissions to capture this transaction\"}")
                    .build());
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Check if transaction exists
                PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
                if (transaction == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Transaction not found\"}")
                            .build();
                }
                
                // Validate organization and account match
                if (!transaction.getOrganizationId().equals(orgId) || 
                        (!accountId.equals("_all") && !transaction.getAccountId().toString().equals(accountId))) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Transaction does not belong to the specified organization or account\"}")
                            .build();
                }
                
                // Validate current status allows capture
                if (!lifecycleService.canTransitionTo(transaction, PaymentStatus.CAPTURED)) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\": \"Cannot capture transaction in current state: " + 
                                    transaction.getStatus() + "\"}")
                            .build();
                }
                
                // Determine capture amount
                BigDecimal captureAmount = (captureRequest != null && captureRequest.getAmount() != null) 
                        ? captureRequest.getAmount() 
                        : transaction.getAmount(); // Full amount if not specified
                
                // Validate capture amount
                if (!validationService.isValidCaptureAmount(transaction, captureAmount)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Invalid capture amount: " + captureAmount + "\"}")
                            .build();
                }
                
                // Process the capture
                PaymentTransaction capturedTransaction;
                if (captureAmount.compareTo(transaction.getAmount()) == 0) {
                    // Full capture
                    capturedTransaction = captureService.captureTransaction(
                            transaction, 
                            securityContext.getUserPrincipal().getName(),
                            operationRef);
                } else {
                    // Partial capture
                    capturedTransaction = captureService.captureTransactionPartial(
                            transaction,
                            captureAmount,
                            securityContext.getUserPrincipal().getName(),
                            operationRef);
                }
                
                // Return the updated transaction
                return Response.ok(capturedTransaction).build();
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error capturing transaction: " + e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\": \"Failed to capture transaction: " + e.getMessage() + "\"}")
                        .build();
            }
        }).thenAccept(asyncResponse::resume);
    }
    
    /**
     * Refund a payment transaction, transitioning it from CAPTURED to REFUNDED state.
     * Supports both full and partial refunds with amount validation.
     *
     * @param orgId Organization ID
     * @param accountId Account ID (or "_all" for all accounts)
     * @param transactionId Transaction ID to refund
     * @param refundRequest Refund details including amount
     * @param operationRef Optional idempotency key for operation
     * @param securityContext Security context for authorization
     * @param asyncResponse Async response for long-running operation
     */
    @POST
    @Path("/refund")
    public void refundTransaction(
            @PathParam("org_id") UUID orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") UUID transactionId,
            RefundRequest refundRequest,
            @QueryParam("operation_ref") String operationRef,
            @Context SecurityContext securityContext,
            @Suspended AsyncResponse asyncResponse) {
        
        LOGGER.log(Level.INFO, "Refunding transaction {0} for organization {1}, account {2}, amount {3}",
                new Object[]{transactionId, orgId, accountId, 
                        refundRequest != null ? refundRequest.getAmount() : "full amount"});
        
        // Set timeout for async response
        asyncResponse.setTimeout(30, TimeUnit.SECONDS);
        
        // Validate access rights
        if (!hasAccessRights(securityContext, orgId, accountId, "REFUND_PAYMENT")) {
            asyncResponse.resume(Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Insufficient permissions to refund this transaction\"}")
                    .build());
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Check if transaction exists
                PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
                if (transaction == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Transaction not found\"}")
                            .build();
                }
                
                // Validate organization and account match
                if (!transaction.getOrganizationId().equals(orgId) || 
                        (!accountId.equals("_all") && !transaction.getAccountId().toString().equals(accountId))) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Transaction does not belong to the specified organization or account\"}")
                            .build();
                }
                
                // Validate current status allows refund
                if (!lifecycleService.canTransitionTo(transaction, PaymentStatus.REFUNDED)) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\": \"Cannot refund transaction in current state: " + 
                                    transaction.getStatus() + "\"}")
                            .build();
                }
                
                // Determine refund amount
                BigDecimal refundAmount = (refundRequest != null && refundRequest.getAmount() != null) 
                        ? refundRequest.getAmount() 
                        : transaction.getAmount(); // Full amount if not specified
                
                // Validate refund amount
                if (!validationService.isValidRefundAmount(transaction, refundAmount)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Invalid refund amount: " + refundAmount + "\"}")
                            .build();
                }
                
                // Process the refund
                PaymentTransaction refundedTransaction;
                if (refundAmount.compareTo(transaction.getAmount()) == 0) {
                    // Full refund
                    refundedTransaction = refundService.refundTransaction(
                            transaction, 
                            securityContext.getUserPrincipal().getName(),
                            operationRef);
                } else {
                    // Partial refund
                    refundedTransaction = refundService.refundTransactionPartial(
                            transaction,
                            refundAmount,
                            securityContext.getUserPrincipal().getName(),
                            operationRef);
                }
                
                // Return the updated transaction
                return Response.ok(refundedTransaction).build();
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error refunding transaction: " + e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\": \"Failed to refund transaction: " + e.getMessage() + "\"}")
                        .build();
            }
        }).thenAccept(asyncResponse::resume);
    }
    
    /**
     * Void a payment transaction, transitioning it to VOIDED state.
     * This cancels a transaction that has not been captured yet.
     *
     * @param orgId Organization ID
     * @param accountId Account ID (or "_all" for all accounts)
     * @param transactionId Transaction ID to void
     * @param operationRef Optional idempotency key for operation
     * @param securityContext Security context for authorization
     * @return Response with updated transaction data
     */
    @POST
    @Path("/void")
    public Response voidTransaction(
            @PathParam("org_id") UUID orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") UUID transactionId,
            @QueryParam("operation_ref") String operationRef,
            @Context SecurityContext securityContext) {
        
        LOGGER.log(Level.INFO, "Voiding transaction {0} for organization {1}, account {2}",
                new Object[]{transactionId, orgId, accountId});
        
        // Validate access rights
        if (!hasAccessRights(securityContext, orgId, accountId, "VOID_PAYMENT")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Insufficient permissions to void this transaction\"}")
                    .build();
        }
        
        try {
            // Check if transaction exists
            PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
            if (transaction == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Transaction not found\"}")
                        .build();
            }
            
            // Validate organization and account match
            if (!transaction.getOrganizationId().equals(orgId) || 
                    (!accountId.equals("_all") && !transaction.getAccountId().toString().equals(accountId))) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Transaction does not belong to the specified organization or account\"}")
                        .build();
            }
            
            // Validate current status allows voiding
            if (!lifecycleService.canTransitionTo(transaction, PaymentStatus.VOIDED)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Cannot void transaction in current state: " + 
                                transaction.getStatus() + "\"}")
                        .build();
            }
            
            // Process the void operation
            PaymentTransaction voidedTransaction = lifecycleService.transitionStatus(
                    transaction, 
                    PaymentStatus.VOIDED, 
                    securityContext.getUserPrincipal().getName(),
                    operationRef);
            
            // Return the updated transaction
            return Response.ok(voidedTransaction).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error voiding transaction: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to void transaction: " + e.getMessage() + "\"}")
                    .build();
        }
    }
    
    /**
     * Check if the current user has the required access rights for the operation.
     * Integrates with the Access Rights module to enforce authorization rules.
     *
     * @param securityContext Security context containing user information
     * @param orgId Organization ID
     * @param accountId Account ID
     * @param permission Required permission
     * @return true if user has access, false otherwise
     */
    private boolean hasAccessRights(SecurityContext securityContext, UUID orgId, String accountId, String permission) {
        // In a real implementation, this would integrate with the Access Rights module
        // For now, we'll assume the user has the required permissions if authenticated
        return securityContext.getUserPrincipal() != null;
    }
    
    /**
     * Request object for capture operations.
     */
    public static class CaptureRequest {
        private BigDecimal amount;
        private String description;
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    /**
     * Request object for refund operations.
     */
    public static class RefundRequest {
        private BigDecimal amount;
        private String reason;
        private String description;
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}