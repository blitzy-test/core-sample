package io.briklabs.sample.payments.rest;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.service.PaymentCaptureService;
import io.briklabs.sample.payments.service.PaymentRefundService;
import io.briklabs.sample.payments.service.PaymentTransactionService;
import io.briklabs.sample.payments.service.PaymentEventService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAX-RS resource for payment transaction lifecycle operations including processing,
 * capturing, and refunding payments. This resource handles state transitions throughout
 * the payment lifecycle.
 */
@Path("/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionProcessingResource {

    private static final Logger LOGGER = Logger.getLogger(TransactionProcessingResource.class.getName());
    
    private final PaymentTransactionService transactionService;
    private final PaymentCaptureService captureService;
    private final PaymentRefundService refundService;
    private final PaymentEventService eventService;
    
    /**
     * Constructor with dependency injection.
     *
     * @param transactionService The payment transaction service
     * @param captureService The payment capture service
     * @param refundService The payment refund service
     * @param eventService The payment event service
     */
    @Inject
    public TransactionProcessingResource(
            PaymentTransactionService transactionService,
            PaymentCaptureService captureService,
            PaymentRefundService refundService,
            PaymentEventService eventService) {
        this.transactionService = transactionService;
        this.captureService = captureService;
        this.refundService = refundService;
        this.eventService = eventService;
    }
    
    /**
     * Processes a payment transaction, moving it from CREATED to PROCESSING state.
     * This is the first step in the payment lifecycle after creation.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param operationReference Optional reference for idempotent operations
     * @return Response with the updated transaction
     */
    @POST
    @Path("/process")
    public Response processTransaction(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            @QueryParam("operation_ref") String operationReference) {
        
        LOGGER.info(String.format("Processing transaction %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        try {
            // Validate access rights
            validateAccessRights(orgId, accountId, "PAYMENT_PROCESS");
            
            // Convert string IDs to UUIDs
            UUID transactionUuid = UUID.fromString(transactionId);
            
            // Check if transaction exists and belongs to the specified organization and account
            PaymentTransaction transaction = validateTransactionOwnership(
                    transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
            
            // Check if transaction can be processed
            if (!transactionService.canProcessTransaction(transactionUuid)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse("Transaction cannot be processed in its current state",
                                "INVALID_STATE", transaction.getStatus().name()))
                        .build();
            }
            
            // Process the transaction
            PaymentTransaction updatedTransaction = transactionService.processTransaction(transactionUuid);
            
            return Response.ok(updatedTransaction).build();
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid argument in process transaction request", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                    .build();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Invalid state in process transaction request", e);
            return Response.status(Response.Status.CONFLICT)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_STATE", null))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing transaction", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                    .build();
        }
    }
    
    /**
     * Processes a payment transaction asynchronously, moving it from CREATED to PROCESSING state.
     * This endpoint is designed for long-running operations.
     *
     * @param asyncResponse The asynchronous response
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param operationReference Optional reference for idempotent operations
     */
    @POST
    @Path("/process/async")
    public void processTransactionAsync(
            @Suspended final AsyncResponse asyncResponse,
            @PathParam("org_id") final String orgId,
            @PathParam("account_id") final String accountId,
            @PathParam("transaction_id") final String transactionId,
            @QueryParam("operation_ref") final String operationReference) {
        
        LOGGER.info(String.format("Processing transaction asynchronously %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        // Set timeout for async response
        asyncResponse.setTimeout(30, TimeUnit.SECONDS);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Validate access rights
                validateAccessRights(orgId, accountId, "PAYMENT_PROCESS");
                
                // Convert string IDs to UUIDs
                UUID transactionUuid = UUID.fromString(transactionId);
                
                // Check if transaction exists and belongs to the specified organization and account
                PaymentTransaction transaction = validateTransactionOwnership(
                        transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
                
                // Check if transaction can be processed
                if (!transactionService.canProcessTransaction(transactionUuid)) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(createErrorResponse("Transaction cannot be processed in its current state",
                                    "INVALID_STATE", transaction.getStatus().name()))
                            .build();
                }
                
                // Process the transaction
                PaymentTransaction updatedTransaction = transactionService.processTransaction(transactionUuid);
                
                return Response.ok(updatedTransaction).build();
                
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid argument in async process transaction request", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                        .build();
            } catch (IllegalStateException e) {
                LOGGER.log(Level.WARNING, "Invalid state in async process transaction request", e);
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse(e.getMessage(), "INVALID_STATE", null))
                        .build();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing transaction asynchronously", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                        .build();
            }
        }).thenAccept(asyncResponse::resume);
    }
    
    /**
     * Captures a payment transaction, moving it from AUTHORIZED to CAPTURED state.
     * This endpoint supports both full and partial captures.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param captureRequest The capture request containing amount information
     * @param operationReference Optional reference for idempotent operations
     * @return Response with the updated transaction
     */
    @POST
    @Path("/capture")
    public Response captureTransaction(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            CaptureRequest captureRequest,
            @QueryParam("operation_ref") String operationReference) {
        
        LOGGER.info(String.format("Capturing transaction %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        try {
            // Validate access rights
            validateAccessRights(orgId, accountId, "PAYMENT_CAPTURE");
            
            // Convert string IDs to UUIDs
            UUID transactionUuid = UUID.fromString(transactionId);
            
            // Check if transaction exists and belongs to the specified organization and account
            PaymentTransaction transaction = validateTransactionOwnership(
                    transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
            
            // Check if transaction can be captured
            if (!captureService.canCapture(transactionUuid)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse("Transaction cannot be captured in its current state",
                                "INVALID_STATE", transaction.getStatus().name()))
                        .build();
            }
            
            PaymentTransaction updatedTransaction;
            
            // Determine if this is a full or partial capture
            if (captureRequest != null && captureRequest.getAmount() != null) {
                // Partial capture
                BigDecimal captureAmount = captureRequest.getAmount();
                
                // Validate capture amount
                try {
                    captureService.validateCaptureAmount(transaction, captureAmount);
                } catch (IllegalArgumentException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse(e.getMessage(), "INVALID_AMOUNT", null))
                            .build();
                }
                
                // Perform partial capture
                updatedTransaction = captureService.capturePartialTransaction(transactionUuid, captureAmount);
            } else {
                // Full capture
                updatedTransaction = captureService.captureTransaction(transactionUuid);
            }
            
            return Response.ok(updatedTransaction).build();
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid argument in capture transaction request", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                    .build();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Invalid state in capture transaction request", e);
            return Response.status(Response.Status.CONFLICT)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_STATE", null))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error capturing transaction", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                    .build();
        }
    }
    
    /**
     * Captures a payment transaction asynchronously, moving it from AUTHORIZED to CAPTURED state.
     * This endpoint is designed for long-running operations and supports both full and partial captures.
     *
     * @param asyncResponse The asynchronous response
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param captureRequest The capture request containing amount information
     * @param operationReference Optional reference for idempotent operations
     */
    @POST
    @Path("/capture/async")
    public void captureTransactionAsync(
            @Suspended final AsyncResponse asyncResponse,
            @PathParam("org_id") final String orgId,
            @PathParam("account_id") final String accountId,
            @PathParam("transaction_id") final String transactionId,
            final CaptureRequest captureRequest,
            @QueryParam("operation_ref") final String operationReference) {
        
        LOGGER.info(String.format("Capturing transaction asynchronously %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        // Set timeout for async response
        asyncResponse.setTimeout(30, TimeUnit.SECONDS);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Validate access rights
                validateAccessRights(orgId, accountId, "PAYMENT_CAPTURE");
                
                // Convert string IDs to UUIDs
                UUID transactionUuid = UUID.fromString(transactionId);
                
                // Check if transaction exists and belongs to the specified organization and account
                PaymentTransaction transaction = validateTransactionOwnership(
                        transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
                
                // Check if transaction can be captured
                if (!captureService.canCapture(transactionUuid)) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(createErrorResponse("Transaction cannot be captured in its current state",
                                    "INVALID_STATE", transaction.getStatus().name()))
                            .build();
                }
                
                PaymentTransaction updatedTransaction;
                
                // Determine if this is a full or partial capture
                if (captureRequest != null && captureRequest.getAmount() != null) {
                    // Partial capture
                    BigDecimal captureAmount = captureRequest.getAmount();
                    
                    // Validate capture amount
                    try {
                        captureService.validateCaptureAmount(transaction, captureAmount);
                    } catch (IllegalArgumentException e) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(createErrorResponse(e.getMessage(), "INVALID_AMOUNT", null))
                                .build();
                    }
                    
                    // Perform partial capture
                    updatedTransaction = captureService.capturePartialTransaction(transactionUuid, captureAmount);
                } else {
                    // Full capture
                    updatedTransaction = captureService.captureTransaction(transactionUuid);
                }
                
                return Response.ok(updatedTransaction).build();
                
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid argument in async capture transaction request", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                        .build();
            } catch (IllegalStateException e) {
                LOGGER.log(Level.WARNING, "Invalid state in async capture transaction request", e);
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse(e.getMessage(), "INVALID_STATE", null))
                        .build();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error capturing transaction asynchronously", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                        .build();
            }
        }).thenAccept(asyncResponse::resume);
    }
    
    /**
     * Refunds a payment transaction, moving it from CAPTURED to REFUNDED state.
     * This endpoint supports both full and partial refunds.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param refundRequest The refund request containing amount information
     * @param operationReference Optional reference for idempotent operations
     * @return Response with the updated transaction
     */
    @POST
    @Path("/refund")
    public Response refundTransaction(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            RefundRequest refundRequest,
            @QueryParam("operation_ref") String operationReference) {
        
        LOGGER.info(String.format("Refunding transaction %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        try {
            // Validate access rights
            validateAccessRights(orgId, accountId, "PAYMENT_REFUND");
            
            // Convert string IDs to UUIDs
            UUID transactionUuid = UUID.fromString(transactionId);
            
            // Check if transaction exists and belongs to the specified organization and account
            PaymentTransaction transaction = validateTransactionOwnership(
                    transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
            
            // Check if transaction can be refunded
            if (!refundService.canRefund(transactionUuid)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse("Transaction cannot be refunded in its current state",
                                "INVALID_STATE", transaction.getStatus().name()))
                        .build();
            }
            
            PaymentTransaction updatedTransaction;
            
            // Determine if this is a full or partial refund
            if (refundRequest != null && refundRequest.getAmount() != null) {
                // Partial refund
                BigDecimal refundAmount = refundRequest.getAmount();
                
                // Validate refund amount
                try {
                    refundService.validateRefundAmount(transaction, refundAmount);
                } catch (IllegalArgumentException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse(e.getMessage(), "INVALID_AMOUNT", null))
                            .build();
                }
                
                // Perform partial refund
                updatedTransaction = refundService.refundPartialTransaction(transactionUuid, refundAmount);
            } else {
                // Full refund
                updatedTransaction = refundService.refundTransaction(transactionUuid);
            }
            
            return Response.ok(updatedTransaction).build();
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid argument in refund transaction request", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                    .build();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Invalid state in refund transaction request", e);
            return Response.status(Response.Status.CONFLICT)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_STATE", null))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refunding transaction", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                    .build();
        }
    }
    
    /**
     * Refunds a payment transaction asynchronously, moving it from CAPTURED to REFUNDED state.
     * This endpoint is designed for long-running operations and supports both full and partial refunds.
     *
     * @param asyncResponse The asynchronous response
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param refundRequest The refund request containing amount information
     * @param operationReference Optional reference for idempotent operations
     */
    @POST
    @Path("/refund/async")
    public void refundTransactionAsync(
            @Suspended final AsyncResponse asyncResponse,
            @PathParam("org_id") final String orgId,
            @PathParam("account_id") final String accountId,
            @PathParam("transaction_id") final String transactionId,
            final RefundRequest refundRequest,
            @QueryParam("operation_ref") final String operationReference) {
        
        LOGGER.info(String.format("Refunding transaction asynchronously %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        // Set timeout for async response
        asyncResponse.setTimeout(30, TimeUnit.SECONDS);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Validate access rights
                validateAccessRights(orgId, accountId, "PAYMENT_REFUND");
                
                // Convert string IDs to UUIDs
                UUID transactionUuid = UUID.fromString(transactionId);
                
                // Check if transaction exists and belongs to the specified organization and account
                PaymentTransaction transaction = validateTransactionOwnership(
                        transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
                
                // Check if transaction can be refunded
                if (!refundService.canRefund(transactionUuid)) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(createErrorResponse("Transaction cannot be refunded in its current state",
                                    "INVALID_STATE", transaction.getStatus().name()))
                            .build();
                }
                
                PaymentTransaction updatedTransaction;
                
                // Determine if this is a full or partial refund
                if (refundRequest != null && refundRequest.getAmount() != null) {
                    // Partial refund
                    BigDecimal refundAmount = refundRequest.getAmount();
                    
                    // Validate refund amount
                    try {
                        refundService.validateRefundAmount(transaction, refundAmount);
                    } catch (IllegalArgumentException e) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(createErrorResponse(e.getMessage(), "INVALID_AMOUNT", null))
                                .build();
                    }
                    
                    // Perform partial refund
                    updatedTransaction = refundService.refundPartialTransaction(transactionUuid, refundAmount);
                } else {
                    // Full refund
                    updatedTransaction = refundService.refundTransaction(transactionUuid);
                }
                
                return Response.ok(updatedTransaction).build();
                
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid argument in async refund transaction request", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                        .build();
            } catch (IllegalStateException e) {
                LOGGER.log(Level.WARNING, "Invalid state in async refund transaction request", e);
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse(e.getMessage(), "INVALID_STATE", null))
                        .build();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error refunding transaction asynchronously", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                        .build();
            }
        }).thenAccept(asyncResponse::resume);
    }
    
    /**
     * Voids a payment transaction, moving it from AUTHORIZED to VOIDED state.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param operationReference Optional reference for idempotent operations
     * @return Response with the updated transaction
     */
    @POST
    @Path("/void")
    public Response voidTransaction(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            @QueryParam("operation_ref") String operationReference) {
        
        LOGGER.info(String.format("Voiding transaction %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        try {
            // Validate access rights
            validateAccessRights(orgId, accountId, "PAYMENT_VOID");
            
            // Convert string IDs to UUIDs
            UUID transactionUuid = UUID.fromString(transactionId);
            
            // Check if transaction exists and belongs to the specified organization and account
            PaymentTransaction transaction = validateTransactionOwnership(
                    transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
            
            // Check if transaction can be voided
            if (!transactionService.canVoidTransaction(transactionUuid)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse("Transaction cannot be voided in its current state",
                                "INVALID_STATE", transaction.getStatus().name()))
                        .build();
            }
            
            // Void the transaction
            PaymentTransaction updatedTransaction = transactionService.voidTransaction(transactionUuid);
            
            return Response.ok(updatedTransaction).build();
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid argument in void transaction request", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                    .build();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Invalid state in void transaction request", e);
            return Response.status(Response.Status.CONFLICT)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_STATE", null))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error voiding transaction", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                    .build();
        }
    }
    
    /**
     * Gets the current status of a transaction.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @return Response with the transaction status
     */
    @GET
    @Path("/status")
    public Response getTransactionStatus(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId) {
        
        LOGGER.info(String.format("Getting status for transaction %s for organization %s, account %s", 
                transactionId, orgId, accountId));
        
        try {
            // Validate access rights
            validateAccessRights(orgId, accountId, "PAYMENT_VIEW");
            
            // Convert string IDs to UUIDs
            UUID transactionUuid = UUID.fromString(transactionId);
            
            // Check if transaction exists and belongs to the specified organization and account
            PaymentTransaction transaction = validateTransactionOwnership(
                    transactionUuid, UUID.fromString(orgId), UUID.fromString(accountId));
            
            // Create status response
            TransactionStatusResponse statusResponse = new TransactionStatusResponse(
                    transaction.getTransactionId(),
                    transaction.getStatus(),
                    transaction.getStatus().getDisplayName(),
                    transaction.getStatus().getDescription(),
                    transaction.getUpdatedAt(),
                    transaction.getStatus().isFinalState()
            );
            
            return Response.ok(statusResponse).build();
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid argument in get transaction status request", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage(), "INVALID_ARGUMENT", null))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting transaction status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Internal server error", "SERVER_ERROR", null))
                    .build();
        }
    }
    
    /**
     * Validates that the user has the required access rights for the operation.
     * This method integrates with the Access Rights module to enforce authorization.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param permission The required permission
     * @throws WebApplicationException if the user does not have the required permission
     */
    private void validateAccessRights(String orgId, String accountId, String permission) {
        // Integration with Access Rights module would be implemented here
        // For now, we'll assume all requests are authorized
        
        // Example implementation:
        // boolean hasAccess = accessRightsService.hasPermission(
        //     SecurityContext.getCurrentUser(), orgId, accountId, permission);
        // if (!hasAccess) {
        //     throw new WebApplicationException(
        //         Response.status(Response.Status.FORBIDDEN)
        //             .entity(createErrorResponse("Access denied", "FORBIDDEN", null))
        //             .build());
        // }
    }
    
    /**
     * Validates that a transaction exists and belongs to the specified organization and account.
     *
     * @param transactionId The transaction identifier
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @return The validated transaction
     * @throws WebApplicationException if the transaction does not exist or does not belong to the organization/account
     */
    private PaymentTransaction validateTransactionOwnership(
            UUID transactionId, UUID organizationId, UUID accountId) {
        
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        
        if (transaction == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.NOT_FOUND)
                    .entity(createErrorResponse("Transaction not found", "NOT_FOUND", null))
                    .build());
        }
        
        if (!transaction.getOrganizationId().equals(organizationId)) {
            throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("Transaction does not belong to the specified organization", 
                            "FORBIDDEN", null))
                    .build());
        }
        
        if (!transaction.getAccountId().equals(accountId)) {
            throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("Transaction does not belong to the specified account", 
                            "FORBIDDEN", null))
                    .build());
        }
        
        return transaction;
    }
    
    /**
     * Creates a standardized error response.
     *
     * @param message The error message
     * @param code The error code
     * @param details Additional error details
     * @return The error response object
     */
    private ErrorResponse createErrorResponse(String message, String code, String details) {
        return new ErrorResponse(message, code, details);
    }
    
    /**
     * Model class for capture requests.
     */
    public static class CaptureRequest {
        private BigDecimal amount;
        
        public CaptureRequest() {
            // Default constructor for serialization
        }
        
        public CaptureRequest(BigDecimal amount) {
            this.amount = amount;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
    
    /**
     * Model class for refund requests.
     */
    public static class RefundRequest {
        private BigDecimal amount;
        private String reason;
        
        public RefundRequest() {
            // Default constructor for serialization
        }
        
        public RefundRequest(BigDecimal amount, String reason) {
            this.amount = amount;
            this.reason = reason;
        }
        
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
    }
    
    /**
     * Model class for transaction status responses.
     */
    public static class TransactionStatusResponse {
        private UUID transactionId;
        private PaymentStatus status;
        private String statusName;
        private String statusDescription;
        private Instant lastUpdated;
        private boolean isFinalState;
        
        public TransactionStatusResponse() {
            // Default constructor for serialization
        }
        
        public TransactionStatusResponse(UUID transactionId, PaymentStatus status, String statusName,
                                        String statusDescription, Instant lastUpdated, boolean isFinalState) {
            this.transactionId = transactionId;
            this.status = status;
            this.statusName = statusName;
            this.statusDescription = statusDescription;
            this.lastUpdated = lastUpdated;
            this.isFinalState = isFinalState;
        }
        
        public UUID getTransactionId() {
            return transactionId;
        }
        
        public void setTransactionId(UUID transactionId) {
            this.transactionId = transactionId;
        }
        
        public PaymentStatus getStatus() {
            return status;
        }
        
        public void setStatus(PaymentStatus status) {
            this.status = status;
        }
        
        public String getStatusName() {
            return statusName;
        }
        
        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }
        
        public String getStatusDescription() {
            return statusDescription;
        }
        
        public void setStatusDescription(String statusDescription) {
            this.statusDescription = statusDescription;
        }
        
        public Instant getLastUpdated() {
            return lastUpdated;
        }
        
        public void setLastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
        
        public boolean isFinalState() {
            return isFinalState;
        }
        
        public void setFinalState(boolean finalState) {
            isFinalState = finalState;
        }
    }
    
    /**
     * Model class for error responses.
     */
    public static class ErrorResponse {
        private String message;
        private String code;
        private String details;
        private Instant timestamp;
        
        public ErrorResponse() {
            // Default constructor for serialization
        }
        
        public ErrorResponse(String message, String code, String details) {
            this.message = message;
            this.code = code;
            this.details = details;
            this.timestamp = Instant.now();
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getDetails() {
            return details;
        }
        
        public void setDetails(String details) {
            this.details = details;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}