package io.briklabs.sample.payments.rest;

import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.service.PaymentEventService;
import io.briklabs.sample.payments.service.PaymentTransactionService;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS resource for accessing payment transaction event history.
 * <p>
 * This resource provides endpoints for retrieving a comprehensive audit trail
 * of all actions and state changes for a given transaction, essential for
 * troubleshooting, compliance, and audit purposes.
 * </p>
 */
@Path("/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}/events")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionEventResource {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEventResource.class);
    
    private final PaymentEventService eventService;
    private final PaymentTransactionService transactionService;
    
    /**
     * Constructs a new TransactionEventResource with required services.
     *
     * @param eventService The payment event service
     * @param transactionService The payment transaction service
     */
    public TransactionEventResource(PaymentEventService eventService, PaymentTransactionService transactionService) {
        this.eventService = eventService;
        this.transactionService = transactionService;
        logger.info("TransactionEventResource initialized");
    }
    
    /**
     * Retrieves all events for a specific transaction.
     * <p>
     * This endpoint returns a comprehensive history of all events associated with
     * a transaction, providing a complete audit trail of its lifecycle.
     * </p>
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param eventType Optional filter for specific event types
     * @param startDate Optional filter for events after this date (ISO-8601 format)
     * @param endDate Optional filter for events before this date (ISO-8601 format)
     * @param userId Optional filter for events created by a specific user
     * @param order Sort order for events (asc or desc, default is asc for chronological order)
     * @param limit Maximum number of events to return (for pagination)
     * @param offset Starting position for pagination
     * @return Response containing the list of events
     */
    @GET
    public Response getTransactionEvents(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            @QueryParam("eventType") List<String> eventType,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("userId") String userId,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        
        logger.debug("Retrieving events for transaction {} (org: {}, account: {})", 
                transactionId, orgId, accountId);
        
        try {
            // Validate parameters
            UUID transactionUuid = validateAndParseUuid(transactionId, "Transaction ID");
            UUID orgUuid = validateAndParseUuid(orgId, "Organization ID");
            UUID accountUuid = validateAndParseUuid(accountId, "Account ID");
            
            // Verify transaction exists and belongs to the specified org/account
            verifyTransactionAccess(transactionUuid, orgUuid, accountUuid);
            
            // Parse date parameters
            Instant startInstant = parseDate(startDate);
            Instant endInstant = parseDate(endDate);
            
            // Validate sort order
            String sortOrder = validateSortOrder(order);
            
            // Validate pagination parameters
            int validLimit = validateLimit(limit);
            int validOffset = validateOffset(offset);
            
            // Retrieve events based on filters
            List<PaymentEvent> events;
            if (eventType != null && !eventType.isEmpty() || startInstant != null || endInstant != null) {
                // Use filtered timeline if any filters are provided
                events = eventService.buildFilteredTransactionTimeline(
                        transactionUuid, 
                        eventType, 
                        startInstant, 
                        endInstant);
                
                // Apply user filter if provided
                if (userId != null && !userId.isEmpty()) {
                    events = events.stream()
                            .filter(event -> userId.equals(event.getCreatedBy()))
                            .collect(Collectors.toList());
                }
            } else {
                // Use standard timeline if no filters
                events = eventService.buildTransactionTimeline(transactionUuid);
                
                // Apply user filter if provided
                if (userId != null && !userId.isEmpty()) {
                    events = events.stream()
                            .filter(event -> userId.equals(event.getCreatedBy()))
                            .collect(Collectors.toList());
                }
            }
            
            // Apply sorting
            if ("desc".equalsIgnoreCase(sortOrder)) {
                Collections.reverse(events);
            }
            
            // Apply pagination
            int totalCount = events.size();
            List<PaymentEvent> paginatedEvents = applyPagination(events, validOffset, validLimit);
            
            // Format response
            Map<String, Object> response = formatEventResponse(paginatedEvents, totalCount, validLimit, validOffset);
            
            logger.debug("Retrieved {} events for transaction {} (total: {})", 
                    paginatedEvents.size(), transactionId, totalCount);
            
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameter: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("INVALID_PARAMETER", e.getMessage()))
                    .build();
        } catch (NotFoundException e) {
            logger.warn("Transaction not found: {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(createErrorResponse("TRANSACTION_NOT_FOUND", e.getMessage()))
                    .build();
        } catch (ForbiddenException e) {
            logger.warn("Access denied: {}", e.getMessage());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("ACCESS_DENIED", e.getMessage()))
                    .build();
        } catch (Exception e) {
            logger.error("Error retrieving transaction events: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                    .build();
        }
    }
    
    /**
     * Retrieves a specific event for a transaction.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @param eventId The event identifier
     * @return Response containing the event details
     */
    @GET
    @Path("/{event_id}")
    public Response getTransactionEvent(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId,
            @PathParam("event_id") String eventId) {
        
        logger.debug("Retrieving event {} for transaction {} (org: {}, account: {})", 
                eventId, transactionId, orgId, accountId);
        
        try {
            // Validate parameters
            UUID transactionUuid = validateAndParseUuid(transactionId, "Transaction ID");
            UUID orgUuid = validateAndParseUuid(orgId, "Organization ID");
            UUID accountUuid = validateAndParseUuid(accountId, "Account ID");
            UUID eventUuid = validateAndParseUuid(eventId, "Event ID");
            
            // Verify transaction exists and belongs to the specified org/account
            verifyTransactionAccess(transactionUuid, orgUuid, accountUuid);
            
            // Retrieve the event
            PaymentEvent event = eventService.findById(eventUuid);
            
            // Verify the event belongs to the specified transaction
            if (event == null || !event.getTransactionId().equals(transactionUuid)) {
                throw new NotFoundException("Event not found for the specified transaction");
            }
            
            logger.debug("Retrieved event {} for transaction {}", eventId, transactionId);
            
            return Response.ok(formatEventDetail(event)).build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameter: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("INVALID_PARAMETER", e.getMessage()))
                    .build();
        } catch (NotFoundException e) {
            logger.warn("Resource not found: {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(createErrorResponse("RESOURCE_NOT_FOUND", e.getMessage()))
                    .build();
        } catch (ForbiddenException e) {
            logger.warn("Access denied: {}", e.getMessage());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("ACCESS_DENIED", e.getMessage()))
                    .build();
        } catch (Exception e) {
            logger.error("Error retrieving transaction event: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                    .build();
        }
    }
    
    /**
     * Retrieves a count of events by type for a transaction.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @return Response containing the count of events by type
     */
    @GET
    @Path("/count")
    public Response getEventCountByType(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId) {
        
        logger.debug("Retrieving event counts for transaction {} (org: {}, account: {})", 
                transactionId, orgId, accountId);
        
        try {
            // Validate parameters
            UUID transactionUuid = validateAndParseUuid(transactionId, "Transaction ID");
            UUID orgUuid = validateAndParseUuid(orgId, "Organization ID");
            UUID accountUuid = validateAndParseUuid(accountId, "Account ID");
            
            // Verify transaction exists and belongs to the specified org/account
            verifyTransactionAccess(transactionUuid, orgUuid, accountUuid);
            
            // Get all events for the transaction
            List<PaymentEvent> events = eventService.getEventsByTransactionId(transactionUuid);
            
            // Count events by type
            Map<String, Long> eventCounts = events.stream()
                    .collect(Collectors.groupingBy(PaymentEvent::getEventType, Collectors.counting()));
            
            // Add total count
            eventCounts.put("TOTAL", (long) events.size());
            
            logger.debug("Retrieved event counts for transaction {}: {}", transactionId, eventCounts);
            
            return Response.ok(eventCounts).build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameter: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("INVALID_PARAMETER", e.getMessage()))
                    .build();
        } catch (NotFoundException e) {
            logger.warn("Transaction not found: {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(createErrorResponse("TRANSACTION_NOT_FOUND", e.getMessage()))
                    .build();
        } catch (ForbiddenException e) {
            logger.warn("Access denied: {}", e.getMessage());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("ACCESS_DENIED", e.getMessage()))
                    .build();
        } catch (Exception e) {
            logger.error("Error retrieving event counts: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                    .build();
        }
    }
    
    /**
     * Retrieves the timeline of status changes for a transaction.
     *
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @param transactionId The transaction identifier
     * @return Response containing the status change timeline
     */
    @GET
    @Path("/status-timeline")
    public Response getStatusTimeline(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId) {
        
        logger.debug("Retrieving status timeline for transaction {} (org: {}, account: {})", 
                transactionId, orgId, accountId);
        
        try {
            // Validate parameters
            UUID transactionUuid = validateAndParseUuid(transactionId, "Transaction ID");
            UUID orgUuid = validateAndParseUuid(orgId, "Organization ID");
            UUID accountUuid = validateAndParseUuid(accountId, "Account ID");
            
            // Verify transaction exists and belongs to the specified org/account
            verifyTransactionAccess(transactionUuid, orgUuid, accountUuid);
            
            // Get status change events
            List<PaymentEvent> statusEvents = eventService.getEventsByTransactionIdAndType(
                    transactionUuid, "STATUS_CHANGE");
            
            // Sort chronologically
            statusEvents.sort(Comparator.comparing(PaymentEvent::getCreatedAt));
            
            // Format response
            List<Map<String, Object>> timeline = statusEvents.stream()
                    .map(this::formatStatusEvent)
                    .collect(Collectors.toList());
            
            logger.debug("Retrieved status timeline with {} events for transaction {}", 
                    timeline.size(), transactionId);
            
            return Response.ok(timeline).build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameter: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("INVALID_PARAMETER", e.getMessage()))
                    .build();
        } catch (NotFoundException e) {
            logger.warn("Transaction not found: {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(createErrorResponse("TRANSACTION_NOT_FOUND", e.getMessage()))
                    .build();
        } catch (ForbiddenException e) {
            logger.warn("Access denied: {}", e.getMessage());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(createErrorResponse("ACCESS_DENIED", e.getMessage()))
                    .build();
        } catch (Exception e) {
            logger.error("Error retrieving status timeline: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                    .build();
        }
    }
    
    /**
     * Validates and parses a UUID string.
     *
     * @param uuidString The UUID string to parse
     * @param paramName The parameter name for error messages
     * @return The parsed UUID
     * @throws IllegalArgumentException if the UUID is invalid
     */
    private UUID validateAndParseUuid(String uuidString, String paramName) {
        if (uuidString == null || uuidString.isEmpty()) {
            throw new IllegalArgumentException(paramName + " is required");
        }
        
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(paramName + " must be a valid UUID");
        }
    }
    
    /**
     * Verifies that a transaction exists and belongs to the specified organization and account.
     *
     * @param transactionId The transaction identifier
     * @param orgId The organization identifier
     * @param accountId The account identifier
     * @throws NotFoundException if the transaction does not exist
     * @throws ForbiddenException if the transaction does not belong to the specified org/account
     */
    private void verifyTransactionAccess(UUID transactionId, UUID orgId, UUID accountId) {
        // Check if transaction exists
        if (!transactionService.transactionExists(transactionId)) {
            throw new NotFoundException("Transaction not found");
        }
        
        // Check if transaction belongs to the specified org/account
        if (!transactionService.transactionBelongsToAccount(transactionId, orgId, accountId)) {
            throw new ForbiddenException("Transaction does not belong to the specified organization or account");
        }
    }
    
    /**
     * Parses a date string in ISO-8601 format.
     *
     * @param dateString The date string to parse
     * @return The parsed Instant, or null if the input is null or empty
     * @throws IllegalArgumentException if the date format is invalid
     */
    private Instant parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        try {
            return Instant.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use ISO-8601 format (e.g., 2023-01-01T00:00:00Z)");
        }
    }
    
    /**
     * Validates the sort order parameter.
     *
     * @param order The sort order (asc or desc)
     * @return The validated sort order
     * @throws IllegalArgumentException if the sort order is invalid
     */
    private String validateSortOrder(String order) {
        if (order == null || order.isEmpty()) {
            return "asc";
        }
        
        if (!order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Sort order must be 'asc' or 'desc'");
        }
        
        return order.toLowerCase();
    }
    
    /**
     * Validates the limit parameter for pagination.
     *
     * @param limit The limit value
     * @return The validated limit
     * @throws IllegalArgumentException if the limit is invalid
     */
    private int validateLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be a positive integer");
        }
        
        // Cap the maximum limit to prevent excessive resource usage
        final int MAX_LIMIT = 100;
        return Math.min(limit, MAX_LIMIT);
    }
    
    /**
     * Validates the offset parameter for pagination.
     *
     * @param offset The offset value
     * @return The validated offset
     * @throws IllegalArgumentException if the offset is invalid
     */
    private int validateOffset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be a non-negative integer");
        }
        
        return offset;
    }
    
    /**
     * Applies pagination to a list of events.
     *
     * @param events The full list of events
     * @param offset The starting position
     * @param limit The maximum number of events to return
     * @return The paginated list of events
     */
    private List<PaymentEvent> applyPagination(List<PaymentEvent> events, int offset, int limit) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        
        int fromIndex = Math.min(offset, events.size());
        int toIndex = Math.min(fromIndex + limit, events.size());
        
        return events.subList(fromIndex, toIndex);
    }
    
    /**
     * Formats the event response with pagination metadata.
     *
     * @param events The paginated list of events
     * @param totalCount The total number of events before pagination
     * @param limit The limit used for pagination
     * @param offset The offset used for pagination
     * @return The formatted response
     */
    private Map<String, Object> formatEventResponse(List<PaymentEvent> events, int totalCount, int limit, int offset) {
        Map<String, Object> response = new HashMap<>();
        
        // Add pagination metadata
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total", totalCount);
        pagination.put("limit", limit);
        pagination.put("offset", offset);
        pagination.put("returned", events.size());
        
        // Format events
        List<Map<String, Object>> formattedEvents = events.stream()
                .map(this::formatEvent)
                .collect(Collectors.toList());
        
        response.put("pagination", pagination);
        response.put("events", formattedEvents);
        
        return response;
    }
    
    /**
     * Formats a single event for the response.
     *
     * @param event The event to format
     * @return The formatted event
     */
    private Map<String, Object> formatEvent(PaymentEvent event) {
        Map<String, Object> formatted = new HashMap<>();
        
        formatted.put("eventId", event.getEventId());
        formatted.put("transactionId", event.getTransactionId());
        formatted.put("eventType", event.getEventType());
        formatted.put("createdAt", event.getCreatedAt().toString());
        formatted.put("createdBy", event.getCreatedBy());
        
        if (event.getPreviousStatus() != null) {
            formatted.put("previousStatus", event.getPreviousStatus());
        }
        
        if (event.getNewStatus() != null) {
            formatted.put("newStatus", event.getNewStatus());
        }
        
        if (event.getEventData() != null && !event.getEventData().isEmpty()) {
            // Parse event data if it's valid JSON
            try {
                javax.json.JsonReader jsonReader = javax.json.Json.createReader(
                        new java.io.StringReader(event.getEventData()));
                javax.json.JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                
                Map<String, Object> eventData = new HashMap<>();
                for (Map.Entry<String, javax.json.JsonValue> entry : jsonObject.entrySet()) {
                    eventData.put(entry.getKey(), convertJsonValue(entry.getValue()));
                }
                
                formatted.put("eventData", eventData);
            } catch (Exception e) {
                // If parsing fails, include the raw string
                formatted.put("eventData", event.getEventData());
            }
        }
        
        if (event.getCorrelationId() != null) {
            formatted.put("correlationId", event.getCorrelationId());
        }
        
        return formatted;
    }
    
    /**
     * Formats a single event with detailed information.
     *
     * @param event The event to format
     * @return The formatted event detail
     */
    private Map<String, Object> formatEventDetail(PaymentEvent event) {
        Map<String, Object> formatted = formatEvent(event);
        
        // Add additional details for the detailed view
        if ("STATUS_CHANGE".equals(event.getEventType())) {
            formatted.put("statusTransition", String.format("%s → %s", 
                    event.getPreviousStatus(), event.getNewStatus()));
        }
        
        return formatted;
    }
    
    /**
     * Formats a status change event for the timeline.
     *
     * @param event The status change event
     * @return The formatted status event
     */
    private Map<String, Object> formatStatusEvent(PaymentEvent event) {
        Map<String, Object> formatted = new HashMap<>();
        
        formatted.put("eventId", event.getEventId());
        formatted.put("timestamp", event.getCreatedAt().toString());
        formatted.put("previousStatus", event.getPreviousStatus());
        formatted.put("newStatus", event.getNewStatus());
        formatted.put("actor", event.getCreatedBy());
        
        return formatted;
    }
    
    /**
     * Converts a JsonValue to a Java object.
     *
     * @param jsonValue The JsonValue to convert
     * @return The converted Java object
     */
    private Object convertJsonValue(javax.json.JsonValue jsonValue) {
        if (jsonValue == null) {
            return null;
        }
        
        switch (jsonValue.getValueType()) {
            case STRING:
                return ((javax.json.JsonString) jsonValue).getString();
            case NUMBER:
                javax.json.JsonNumber number = (javax.json.JsonNumber) jsonValue;
                return number.isIntegral() ? number.longValue() : number.doubleValue();
            case TRUE:
                return true;
            case FALSE:
                return false;
            case NULL:
                return null;
            case OBJECT:
                Map<String, Object> map = new HashMap<>();
                javax.json.JsonObject object = (javax.json.JsonObject) jsonValue;
                for (Map.Entry<String, javax.json.JsonValue> entry : object.entrySet()) {
                    map.put(entry.getKey(), convertJsonValue(entry.getValue()));
                }
                return map;
            case ARRAY:
                List<Object> list = new ArrayList<>();
                javax.json.JsonArray array = (javax.json.JsonArray) jsonValue;
                for (javax.json.JsonValue item : array) {
                    list.add(convertJsonValue(item));
                }
                return list;
            default:
                return jsonValue.toString();
        }
    }
    
    /**
     * Creates an error response.
     *
     * @param code The error code
     * @param message The error message
     * @return The error response map
     */
    private Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        
        return response;
    }
}