package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for tracking and managing payment lifecycle events.
 * <p>
 * This service provides methods for recording events at each stage of the payment
 * transaction lifecycle, retrieving event history, and constructing event timelines.
 * It serves as a comprehensive audit trail for all payment operations, essential for
 * troubleshooting, compliance, and understanding transaction history.
 * </p>
 */
public interface PaymentEventService {

    /**
     * Records a general event for a payment transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param eventType The type of event (e.g., CREATED, PROCESSED, CAPTURE_INITIATED)
     * @param eventData Additional data about the event in JSON format
     * @param createdBy Identifier of the user or system component creating the event
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordEvent(UUID transactionId, String eventType, String eventData, String createdBy);

    /**
     * Records a general event for a payment transaction with correlation tracking.
     *
     * @param transactionId The unique identifier of the transaction
     * @param eventType The type of event
     * @param eventData Additional data about the event in JSON format
     * @param createdBy Identifier of the user or system component creating the event
     * @param correlationId ID to track related events across a request or operation
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordEvent(UUID transactionId, String eventType, String eventData, 
                            String createdBy, UUID correlationId);

    /**
     * Records a status change event for a payment transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param previousStatus The status before the change
     * @param newStatus The status after the change
     * @param eventData Additional data about the status change
     * @param createdBy Identifier of the user or system component creating the event
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordStatusChangeEvent(UUID transactionId, PaymentStatus previousStatus, 
                                        PaymentStatus newStatus, String eventData, String createdBy);

    /**
     * Records a status change event for a payment transaction with correlation tracking.
     *
     * @param transactionId The unique identifier of the transaction
     * @param previousStatus The status before the change
     * @param newStatus The status after the change
     * @param eventData Additional data about the status change
     * @param createdBy Identifier of the user or system component creating the event
     * @param correlationId ID to track related events across a request or operation
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordStatusChangeEvent(UUID transactionId, PaymentStatus previousStatus, 
                                        PaymentStatus newStatus, String eventData, 
                                        String createdBy, UUID correlationId);

    /**
     * Records an error event for a payment transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param errorMessage User-friendly error message
     * @param errorDetails Technical details about the error in JSON format
     * @param createdBy Identifier of the system component reporting the error
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordErrorEvent(UUID transactionId, String errorMessage, 
                                 String errorDetails, String createdBy);

    /**
     * Records an error event for a payment transaction with correlation tracking.
     *
     * @param transactionId The unique identifier of the transaction
     * @param errorMessage User-friendly error message
     * @param errorDetails Technical details about the error in JSON format
     * @param createdBy Identifier of the system component reporting the error
     * @param correlationId ID to track related events across a request or operation
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordErrorEvent(UUID transactionId, String errorMessage, 
                                 String errorDetails, String createdBy, UUID correlationId);

    /**
     * Records a transaction creation event.
     *
     * @param transaction The newly created transaction
     * @param createdBy Identifier of the user or system component creating the transaction
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordTransactionCreatedEvent(PaymentTransaction transaction, String createdBy);

    /**
     * Records a capture event for a payment transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param captureAmount The amount being captured
     * @param captureReference Reference code for the capture operation
     * @param isPartialCapture Whether this is a partial capture
     * @param createdBy Identifier of the user or system component performing the capture
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordCaptureEvent(UUID transactionId, String captureAmount, 
                                   String captureReference, boolean isPartialCapture, String createdBy);

    /**
     * Records a refund event for a payment transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param refundAmount The amount being refunded
     * @param refundReason The reason for the refund
     * @param refundReference Reference code for the refund operation
     * @param isPartialRefund Whether this is a partial refund
     * @param createdBy Identifier of the user or system component performing the refund
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordRefundEvent(UUID transactionId, String refundAmount, String refundReason,
                                  String refundReference, boolean isPartialRefund, String createdBy);

    /**
     * Records a void event for a payment transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param voidReason The reason for voiding the transaction
     * @param createdBy Identifier of the user or system component performing the void
     * @return The created payment event
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentEvent recordVoidEvent(UUID transactionId, String voidReason, String createdBy);

    /**
     * Gets all events for a specific transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return List of events for the transaction, ordered chronologically
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    List<PaymentEvent> getEventsByTransactionId(UUID transactionId);

    /**
     * Gets events of a specific type for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param eventType The type of events to retrieve
     * @return List of events of the specified type for the transaction
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByType(UUID transactionId, String eventType);

    /**
     * Gets events of multiple types for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param eventTypes List of event types to retrieve
     * @return List of events matching any of the specified types
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByTypes(UUID transactionId, List<String> eventTypes);

    /**
     * Gets status change events for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return List of status change events for the transaction
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    List<PaymentEvent> getStatusChangeEvents(UUID transactionId);

    /**
     * Gets error events for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return List of error events for the transaction
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    List<PaymentEvent> getErrorEvents(UUID transactionId);

    /**
     * Gets the most recent event for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The most recent event for the transaction, or null if no events exist
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentEvent getMostRecentEvent(UUID transactionId);

    /**
     * Gets the most recent event of a specific type for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param eventType The type of event to retrieve
     * @return The most recent event of the specified type, or null if no matching events exist
     * @throws IllegalArgumentException if any parameters are invalid
     */
    PaymentEvent getMostRecentEventByType(UUID transactionId, String eventType);

    /**
     * Gets events created within a specific time range.
     *
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events created within the specified time range
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByTimeRange(Instant startTime, Instant endTime, int offset, int limit);

    /**
     * Gets events created by a specific user or system.
     *
     * @param createdBy The identifier of the user or system that created the events
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events created by the specified user or system
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByCreator(String createdBy, int offset, int limit);

    /**
     * Gets events with a specific correlation ID.
     *
     * @param correlationId The correlation identifier
     * @return List of events with the specified correlation ID
     * @throws IllegalArgumentException if the correlation ID is invalid
     */
    List<PaymentEvent> getEventsByCorrelationId(UUID correlationId);

    /**
     * Gets a complete timeline of events for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return List of events ordered chronologically with metadata
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    List<Map<String, Object>> getTransactionTimeline(UUID transactionId);

    /**
     * Gets a filtered timeline of events for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param eventTypes List of event types to include (null for all types)
     * @param startTime The start of the time range (null for no start limit)
     * @param endTime The end of the time range (null for no end limit)
     * @return List of events matching the filter criteria, ordered chronologically
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    List<Map<String, Object>> getFilteredTimeline(UUID transactionId, List<String> eventTypes,
                                                Instant startTime, Instant endTime);

    /**
     * Gets a summary of events by type for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return Map of event type to count
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    Map<String, Long> getEventCountByType(UUID transactionId);

    /**
     * Gets events for transactions belonging to a specific organization.
     *
     * @param organizationId The organization identifier
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events for transactions belonging to the specified organization
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByOrganization(UUID organizationId, int offset, int limit);

    /**
     * Gets events for transactions belonging to a specific account.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events for transactions belonging to the specified account
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByAccount(UUID organizationId, UUID accountId, int offset, int limit);

    /**
     * Gets events related to status transitions to a specific status.
     *
     * @param status The target status
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events representing transitions to the specified status
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByNewStatus(PaymentStatus status, int offset, int limit);

    /**
     * Gets events related to status transitions from a specific status.
     *
     * @param status The source status
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events representing transitions from the specified status
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getEventsByPreviousStatus(PaymentStatus status, int offset, int limit);

    /**
     * Gets an audit trail for a specific time period.
     *
     * @param startTime The start of the time period
     * @param endTime The end of the time period
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events within the specified time period
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getAuditTrail(Instant startTime, Instant endTime, int offset, int limit);

    /**
     * Gets a user activity log for a specific user.
     *
     * @param userId The user identifier
     * @param startTime The start of the time period
     * @param endTime The end of the time period
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events created by the specified user within the time period
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> getUserActivityLog(String userId, Instant startTime, Instant endTime, 
                                         int offset, int limit);

    /**
     * Builds a structured event timeline for a transaction with additional context.
     *
     * @param transactionId The unique identifier of the transaction
     * @return A structured timeline with event details and contextual information
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    Map<String, Object> buildEventTimeline(UUID transactionId);

    /**
     * Searches for events containing specific data in the event_data JSON field.
     *
     * @param jsonPath The JSON path expression
     * @param value The value to match
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of events matching the JSON criteria
     * @throws IllegalArgumentException if any parameters are invalid
     */
    List<PaymentEvent> searchEventData(String jsonPath, String value, int offset, int limit);

    /**
     * Gets the total count of events for a transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @return The total number of events for the transaction
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    long countEventsByTransaction(UUID transactionId);

    /**
     * Gets the total count of events matching specific criteria.
     *
     * @param eventType The type of events to count (null for all types)
     * @param startTime The start of the time range (null for no start limit)
     * @param endTime The end of the time range (null for no end limit)
     * @param createdBy The creator to filter by (null for all creators)
     * @return The total count of events matching the criteria
     */
    long countEvents(String eventType, Instant startTime, Instant endTime, String createdBy);
}