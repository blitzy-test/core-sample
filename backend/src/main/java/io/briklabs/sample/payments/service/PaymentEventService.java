package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for tracking and managing payment lifecycle events.
 * This service provides methods for recording, retrieving, and analyzing
 * events throughout a payment transaction's lifecycle, creating a comprehensive
 * audit trail for all payment operations.
 */
public interface PaymentEventService {

    /**
     * Records a new event for a payment transaction.
     *
     * @param event The event to record
     * @return The recorded event with generated ID and timestamp
     * @throws IllegalArgumentException if the event data is invalid
     */
    PaymentEvent recordEvent(PaymentEvent event);

    /**
     * Records a status change event for a payment transaction.
     *
     * @param transaction The transaction being updated
     * @param newStatus The new status being applied
     * @param userId The identifier of the user making the change
     * @return The recorded event
     * @throws IllegalArgumentException if the status transition is invalid
     */
    PaymentEvent recordStatusChangeEvent(PaymentTransaction transaction, PaymentStatus newStatus, String userId);

    /**
     * Records a transaction creation event.
     *
     * @param transaction The newly created transaction
     * @param userId The identifier of the user creating the transaction
     * @return The recorded event
     */
    PaymentEvent recordTransactionCreatedEvent(PaymentTransaction transaction, String userId);

    /**
     * Records a payment processing event.
     *
     * @param transaction The transaction being processed
     * @param userId The identifier of the user initiating the processing
     * @param metadata Additional processing details as key-value pairs
     * @return The recorded event
     */
    PaymentEvent recordProcessingEvent(PaymentTransaction transaction, String userId, Map<String, String> metadata);

    /**
     * Records a payment capture event.
     *
     * @param transaction The transaction being captured
     * @param userId The identifier of the user initiating the capture
     * @param amount The amount being captured
     * @param isPartial Flag indicating if this is a partial capture
     * @return The recorded event
     */
    PaymentEvent recordCaptureEvent(PaymentTransaction transaction, String userId, String amount, boolean isPartial);

    /**
     * Records a payment refund event.
     *
     * @param transaction The transaction being refunded
     * @param userId The identifier of the user initiating the refund
     * @param amount The amount being refunded
     * @param reason The reason for the refund
     * @param isPartial Flag indicating if this is a partial refund
     * @return The recorded event
     */
    PaymentEvent recordRefundEvent(PaymentTransaction transaction, String userId, String amount, String reason, boolean isPartial);

    /**
     * Records a payment void event.
     *
     * @param transaction The transaction being voided
     * @param userId The identifier of the user initiating the void
     * @param reason The reason for voiding the transaction
     * @return The recorded event
     */
    PaymentEvent recordVoidEvent(PaymentTransaction transaction, String userId, String reason);

    /**
     * Records an error event for a payment transaction.
     *
     * @param transaction The transaction experiencing an error
     * @param userId The identifier of the user or system reporting the error
     * @param errorCode The error code
     * @param errorMessage The error message
     * @return The recorded event
     */
    PaymentEvent recordErrorEvent(PaymentTransaction transaction, String userId, String errorCode, String errorMessage);

    /**
     * Records a custom event type for a payment transaction.
     *
     * @param transaction The associated transaction
     * @param eventType The type of event
     * @param userId The identifier of the user creating the event
     * @param metadata Additional event data as key-value pairs
     * @return The recorded event
     */
    PaymentEvent recordCustomEvent(PaymentTransaction transaction, String eventType, String userId, Map<String, String> metadata);

    /**
     * Retrieves all events for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return A list of events ordered by creation time (newest first)
     */
    List<PaymentEvent> getEventsByTransactionId(UUID transactionId);

    /**
     * Retrieves all events for a specific transaction within a time range.
     *
     * @param transactionId The transaction identifier
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @return A list of events ordered by creation time (newest first)
     */
    List<PaymentEvent> getEventsByTransactionIdAndTimeRange(UUID transactionId, Instant startTime, Instant endTime);

    /**
     * Retrieves events of a specific type for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param eventType The type of events to retrieve
     * @return A list of events ordered by creation time (newest first)
     */
    List<PaymentEvent> getEventsByTransactionIdAndType(UUID transactionId, String eventType);

    /**
     * Builds a timeline of events for a transaction.
     * The timeline includes all events with their timestamps and details,
     * providing a comprehensive history of the transaction's lifecycle.
     *
     * @param transactionId The transaction identifier
     * @return A chronologically ordered list of events representing the timeline
     */
    List<PaymentEvent> buildTransactionTimeline(UUID transactionId);

    /**
     * Builds a filtered timeline of events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param eventTypes List of event types to include (null for all)
     * @param startTime The start of the time range (null for no lower bound)
     * @param endTime The end of the time range (null for no upper bound)
     * @return A chronologically ordered list of events representing the filtered timeline
     */
    List<PaymentEvent> buildFilteredTransactionTimeline(UUID transactionId, List<String> eventTypes, 
                                                      Instant startTime, Instant endTime);

    /**
     * Retrieves the most recent event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The most recent event or null if no events exist
     */
    PaymentEvent getMostRecentEvent(UUID transactionId);

    /**
     * Retrieves the most recent event of a specific type for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param eventType The type of event to retrieve
     * @return The most recent event of the specified type or null if none exists
     */
    PaymentEvent getMostRecentEventByType(UUID transactionId, String eventType);

    /**
     * Counts the number of events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The total number of events
     */
    int countEventsByTransactionId(UUID transactionId);

    /**
     * Counts the number of events of a specific type for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param eventType The type of events to count
     * @return The number of events of the specified type
     */
    int countEventsByTransactionIdAndType(UUID transactionId, String eventType);

    /**
     * Checks if a transaction has any events of a specific type.
     *
     * @param transactionId The transaction identifier
     * @param eventType The type of events to check for
     * @return true if events of the specified type exist, false otherwise
     */
    boolean hasEventType(UUID transactionId, String eventType);

    /**
     * Retrieves events with correlation ID for cross-service tracing.
     *
     * @param correlationId The correlation identifier
     * @return A list of events with the specified correlation ID
     */
    List<PaymentEvent> getEventsByCorrelationId(UUID correlationId);

    /**
     * Retrieves events created by a specific user.
     *
     * @param userId The identifier of the user who created the events
     * @param limit Maximum number of events to retrieve (for pagination)
     * @param offset Starting position for pagination
     * @return A paginated list of events created by the specified user
     */
    List<PaymentEvent> getEventsByUserId(String userId, int limit, int offset);

    /**
     * Purges events older than the specified retention period for a transaction.
     * This method should be used with caution and only for compliance with data
     * retention policies.
     *
     * @param transactionId The transaction identifier
     * @param retentionPeriod The retention period in days
     * @return The number of events purged
     */
    int purgeOldEvents(UUID transactionId, int retentionPeriod);
}