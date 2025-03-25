package io.briklabs.sample.payments.rest;

import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaginationParams;
import io.briklabs.sample.payments.data.query.SortCriteria;
import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.service.PaymentEventService;
import io.briklabs.sample.payments.service.PaymentTransactionService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST resource for accessing payment transaction event history.
 * <p>
 * This resource handles HTTP requests for retrieving the comprehensive audit trail
 * of all actions and state changes for a given transaction, following the URI pattern
 * '/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}/events'.
 * It provides essential data for troubleshooting, compliance, and audit purposes.
 * </p>
 */
@Path("/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}/events")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionEventResource {

    private static final Logger LOGGER = Logger.getLogger(TransactionEventResource.class.getName());

    private final PaymentTransactionService transactionService;
    private final PaymentEventService eventService;

    /**
     * Constructor with dependency injection.
     *
     * @param transactionService Service for transaction operations
     * @param eventService Service for event operations
     */
    @Inject
    public TransactionEventResource(
            PaymentTransactionService transactionService,
            PaymentEventService eventService) {
        this.transactionService = transactionService;
        this.eventService = eventService;
    }

    /**
     * Retrieves the event history for a specific transaction with filtering, sorting, and pagination.
     *
     * @param orgId Organization identifier
     * @param accountId Account identifier
     * @param transactionId Transaction identifier
     * @param eventType Event type filter (comma-separated list)
     * @param startDate Start date for filtering (ISO format)
     * @param endDate End date for filtering (ISO format)
     * @param createdBy User filter for events
     * @param sortOrder Sort direction (asc/desc)
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @param securityContext Security context for authorization
     * @return Response with paginated event history
     */
    @GET
    public Response getTransactionEvents(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            @QueryParam("event_type") String eventType,
            @QueryParam("start_date") String startDate,
            @QueryParam("end_date") String endDate,
            @QueryParam("created_by") String createdBy,
            @QueryParam("sort_order") @DefaultValue("desc") String sortOrder,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @Context SecurityContext securityContext) {
        
        LOGGER.log(Level.INFO, "Retrieving events for transaction {0} in organization {1}, account {2}",
                new Object[]{transactionId, orgId, accountId});
        
        // Validate input parameters
        UUID organizationId;
        UUID accountUuid = null;
        UUID transactionUuid;
        
        try {
            organizationId = UUID.fromString(orgId);
            if (accountId != null && !"_all".equals(accountId)) {
                accountUuid = UUID.fromString(accountId);
            }
            transactionUuid = UUID.fromString(transactionId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid ID format"))
                    .build();
        }
        
        // Validate access rights
        if (!hasAccessRights(securityContext, organizationId, accountId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("Insufficient permissions to access transaction events"))
                    .build();
        }
        
        try {
            // Check if transaction exists and belongs to the specified organization/account
            Optional<PaymentTransaction> transactionOpt = transactionService.getTransactionById(
                    transactionUuid, organizationId, accountUuid);
            
            if (!transactionOpt.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transaction not found"))
                        .build();
            }
            
            // Build event filter parameters
            Map<String, Object> filterParams = new HashMap<>();
            
            // Add event type filter if provided
            if (eventType != null && !eventType.isEmpty()) {
                List<String> eventTypes = Arrays.asList(eventType.split(","));
                filterParams.put("eventTypes", eventTypes);
            }
            
            // Add user filter if provided
            if (createdBy != null && !createdBy.isEmpty()) {
                filterParams.put("createdBy", createdBy);
            }
            
            // Set date range filter if provided
            DateRangeFilter dateFilter = null;
            if (startDate != null || endDate != null) {
                dateFilter = new DateRangeFilter();
                
                if (startDate != null) {
                    try {
                        LocalDateTime startDateTime = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE)
                                .atStartOfDay();
                        dateFilter.setStartDate(startDateTime);
                    } catch (DateTimeParseException e) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(createErrorResponse("Invalid start_date format. Use ISO date format (YYYY-MM-DD)"))
                                .build();
                    }
                }
                
                if (endDate != null) {
                    try {
                        LocalDateTime endDateTime = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE)
                                .plusDays(1)
                                .atStartOfDay();
                        dateFilter.setEndDate(endDateTime);
                    } catch (DateTimeParseException e) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(createErrorResponse("Invalid end_date format. Use ISO date format (YYYY-MM-DD)"))
                                .build();
                    }
                }
                
                filterParams.put("dateFilter", dateFilter);
            }
            
            // Set pagination parameters
            PaginationParams paginationParams = new PaginationParams();
            paginationParams.setOffset(offset);
            paginationParams.setLimit(Math.min(limit, 100)); // Cap at 100 for performance
            
            // Set sorting parameters
            SortCriteria sortCriteria = new SortCriteria();
            sortCriteria.setField("created_at"); // Events are always sorted by creation time
            sortCriteria.setDirection("asc".equalsIgnoreCase(sortOrder) ? 
                    SortCriteria.SortDirection.ASC : SortCriteria.SortDirection.DESC);
            
            // Retrieve the events
            Map<String, Object> result = eventService.getTransactionEvents(
                    transactionUuid, filterParams, sortCriteria, paginationParams);
            
            // Enhance the response with transaction context
            result.put("transaction_id", transactionUuid.toString());
            result.put("organization_id", organizationId.toString());
            if (accountUuid != null) {
                result.put("account_id", accountUuid.toString());
            }
            
            return Response.ok(result).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transaction events: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve transaction events: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Retrieves a specific event by ID for a transaction.
     *
     * @param orgId Organization identifier
     * @param accountId Account identifier
     * @param transactionId Transaction identifier
     * @param eventId Event identifier
     * @param securityContext Security context for authorization
     * @return Response with event details
     */
    @GET
    @Path("/{event_id}")
    public Response getTransactionEvent(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            @PathParam("event_id") String eventId,
            @Context SecurityContext securityContext) {
        
        LOGGER.log(Level.INFO, "Retrieving event {0} for transaction {1} in organization {2}, account {3}",
                new Object[]{eventId, transactionId, orgId, accountId});
        
        // Validate input parameters
        UUID organizationId;
        UUID accountUuid = null;
        UUID transactionUuid;
        UUID eventUuid;
        
        try {
            organizationId = UUID.fromString(orgId);
            if (accountId != null && !"_all".equals(accountId)) {
                accountUuid = UUID.fromString(accountId);
            }
            transactionUuid = UUID.fromString(transactionId);
            eventUuid = UUID.fromString(eventId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid ID format"))
                    .build();
        }
        
        // Validate access rights
        if (!hasAccessRights(securityContext, organizationId, accountId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("Insufficient permissions to access transaction events"))
                    .build();
        }
        
        try {
            // Check if transaction exists and belongs to the specified organization/account
            Optional<PaymentTransaction> transactionOpt = transactionService.getTransactionById(
                    transactionUuid, organizationId, accountUuid);
            
            if (!transactionOpt.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transaction not found"))
                        .build();
            }
            
            // Retrieve the specific event
            Optional<PaymentEvent> eventOpt = eventService.getEventById(eventUuid, transactionUuid);
            
            if (!eventOpt.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Event not found"))
                        .build();
            }
            
            return Response.ok(eventOpt.get()).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transaction event: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve transaction event: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Retrieves a timeline of status changes for a transaction.
     * This is a specialized endpoint that focuses only on status transition events.
     *
     * @param orgId Organization identifier
     * @param accountId Account identifier
     * @param transactionId Transaction identifier
     * @param securityContext Security context for authorization
     * @return Response with status timeline
     */
    @GET
    @Path("/timeline")
    public Response getTransactionTimeline(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            @Context SecurityContext securityContext) {
        
        LOGGER.log(Level.INFO, "Retrieving timeline for transaction {0} in organization {1}, account {2}",
                new Object[]{transactionId, orgId, accountId});
        
        // Validate input parameters
        UUID organizationId;
        UUID accountUuid = null;
        UUID transactionUuid;
        
        try {
            organizationId = UUID.fromString(orgId);
            if (accountId != null && !"_all".equals(accountId)) {
                accountUuid = UUID.fromString(accountId);
            }
            transactionUuid = UUID.fromString(transactionId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid ID format"))
                    .build();
        }
        
        // Validate access rights
        if (!hasAccessRights(securityContext, organizationId, accountId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("Insufficient permissions to access transaction timeline"))
                    .build();
        }
        
        try {
            // Check if transaction exists and belongs to the specified organization/account
            Optional<PaymentTransaction> transactionOpt = transactionService.getTransactionById(
                    transactionUuid, organizationId, accountUuid);
            
            if (!transactionOpt.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transaction not found"))
                        .build();
            }
            
            // Retrieve the status timeline
            List<Map<String, Object>> timeline = eventService.getTransactionStatusTimeline(transactionUuid);
            
            Map<String, Object> result = new HashMap<>();
            result.put("transaction_id", transactionUuid.toString());
            result.put("organization_id", organizationId.toString());
            if (accountUuid != null) {
                result.put("account_id", accountUuid.toString());
            }
            result.put("current_status", transactionOpt.get().getStatus().toString());
            result.put("timeline", timeline);
            
            return Response.ok(result).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transaction timeline: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve transaction timeline: " + e.getMessage()))
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
     * @return true if user has access, false otherwise
     */
    private boolean hasAccessRights(SecurityContext securityContext, UUID orgId, String accountId) {
        // In a real implementation, this would integrate with the Access Rights module
        // For now, we'll assume the user has the required permissions if authenticated
        return securityContext.getUserPrincipal() != null;
    }

    /**
     * Helper method to create standardized error responses.
     *
     * @param message Error message
     * @return Map containing error details
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        return errorResponse;
    }
}