package io.briklabs.sample.payments.data.dao;

import io.briklabs.sample.payments.data.model.PaymentEventEntity;
import io.briklabs.sample.payments.model.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Interface for payment event data access operations enabling comprehensive event tracking and audit trails.
 * <p>
 * This DAO manages the storage and retrieval of all lifecycle events for payment transactions,
 * providing a complete history of status changes, user actions, and processing milestones.
 * It supports event querying by transaction ID, event type, and timeline filtering, essential
 * for transaction debugging, audit compliance, and user activity tracking.
 * </p>
 * <p>
 * The implementation of this interface should ensure proper indexing for efficient timeline
 * queries and maintain the relationship between transactions and their events.
 * </p>
 */
public interface PaymentEventDAO extends PaymentDAO<PaymentEventEntity, UUID> {

    /**
     * Retrieves all events for a specific transaction, ordered chronologically.
     *
     * @param transactionId The transaction identifier
     * @return A list of events for the transaction in chronological order
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByTransactionId(UUID transactionId);

    /**
     * Retrieves events for a specific transaction filtered by event type.
     *
     * @param transactionId The transaction identifier
     * @param eventType The event type to filter by (e.g., "CREATED", "STATUS_CHANGE", "CAPTURE")
     * @return A list of events matching the transaction ID and event type
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByTransactionIdAndEventType(UUID transactionId, String eventType);

    /**
     * Retrieves events for a specific transaction within a time range.
     *
     * @param transactionId The transaction identifier
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @return A list of events within the specified time range
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByTransactionIdAndTimeRange(UUID transactionId, Instant startTime, Instant endTime);

    /**
     * Retrieves events for a specific transaction that resulted in a particular status.
     *
     * @param transactionId The transaction identifier
     * @param status The resulting status to filter by
     * @return A list of events that resulted in the specified status
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByTransactionIdAndResultingStatus(UUID transactionId, PaymentStatus status);

    /**
     * Retrieves the most recent event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The most recent event for the transaction, or empty if no events exist
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    java.util.Optional<PaymentEventEntity> findMostRecentByTransactionId(UUID transactionId);

    /**
     * Retrieves events by correlation ID to track related events across requests.
     *
     * @param correlationId The correlation identifier
     * @return A list of events with the specified correlation ID
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByCorrelationId(UUID correlationId);

    /**
     * Retrieves events created by a specific user or system.
     *
     * @param createdBy The identifier of the user or system that created the events
     * @param limit Maximum number of events to return
     * @param offset Offset for pagination
     * @return A list of events created by the specified user or system
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByCreatedBy(String createdBy, int limit, int offset);

    /**
     * Retrieves events for all transactions in an organization within a time range.
     *
     * @param organizationId The organization identifier
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @param limit Maximum number of events to return
     * @param offset Offset for pagination
     * @return A list of events for the organization within the specified time range
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByOrganizationIdAndTimeRange(UUID organizationId, Instant startTime, Instant endTime, int limit, int offset);

    /**
     * Retrieves events for all transactions in an account within a time range.
     *
     * @param accountId The account identifier
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @param limit Maximum number of events to return
     * @param offset Offset for pagination
     * @return A list of events for the account within the specified time range
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByAccountIdAndTimeRange(UUID accountId, Instant startTime, Instant endTime, int limit, int offset);

    /**
     * Retrieves status change events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return A list of status change events for the transaction
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findStatusChangesByTransactionId(UUID transactionId);

    /**
     * Retrieves events for transactions that transitioned to a specific status within a time range.
     *
     * @param status The status to filter by
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @param limit Maximum number of events to return
     * @param offset Offset for pagination
     * @return A list of events for transactions that transitioned to the specified status
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByNewStatusAndTimeRange(PaymentStatus status, Instant startTime, Instant endTime, int limit, int offset);

    /**
     * Counts the number of events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The count of events for the transaction
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    long countByTransactionId(UUID transactionId);

    /**
     * Creates a transaction creation event.
     *
     * @param transactionId The transaction identifier
     * @param createdBy The identifier of the user or system that created the transaction
     * @return The created event entity
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    PaymentEventEntity createTransactionCreatedEvent(UUID transactionId, String createdBy);

    /**
     * Creates a status change event.
     *
     * @param transactionId The transaction identifier
     * @param previousStatus The previous status
     * @param newStatus The new status
     * @param createdBy The identifier of the user or system that changed the status
     * @param correlationId Optional correlation ID for tracking related events
     * @return The created event entity
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     * @throws IllegalStateException if the status transition is not allowed
     */
    PaymentEventEntity createStatusChangeEvent(UUID transactionId, PaymentStatus previousStatus, 
                                              PaymentStatus newStatus, String createdBy, UUID correlationId);

    /**
     * Creates a capture event.
     *
     * @param transactionId The transaction identifier
     * @param captureAmount The amount captured
     * @param createdBy The identifier of the user or system that performed the capture
     * @param correlationId Optional correlation ID for tracking related events
     * @return The created event entity
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    PaymentEventEntity createCaptureEvent(UUID transactionId, String captureAmount, 
                                         String createdBy, UUID correlationId);

    /**
     * Creates a refund event.
     *
     * @param transactionId The transaction identifier
     * @param refundAmount The amount refunded
     * @param reason The reason for the refund
     * @param createdBy The identifier of the user or system that performed the refund
     * @param correlationId Optional correlation ID for tracking related events
     * @return The created event entity
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    PaymentEventEntity createRefundEvent(UUID transactionId, String refundAmount, 
                                        String reason, String createdBy, UUID correlationId);

    /**
     * Creates a void event.
     *
     * @param transactionId The transaction identifier
     * @param reason The reason for voiding the transaction
     * @param createdBy The identifier of the user or system that voided the transaction
     * @param correlationId Optional correlation ID for tracking related events
     * @return The created event entity
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    PaymentEventEntity createVoidEvent(UUID transactionId, String reason, 
                                      String createdBy, UUID correlationId);

    /**
     * Creates an error event.
     *
     * @param transactionId The transaction identifier
     * @param errorCode The error code
     * @param errorMessage The error message
     * @param createdBy The identifier of the system that reported the error
     * @param correlationId Optional correlation ID for tracking related events
     * @return The created event entity
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    PaymentEventEntity createErrorEvent(UUID transactionId, String errorCode, 
                                       String errorMessage, String createdBy, UUID correlationId);

    /**
     * Retrieves events with specific event data content.
     * This method allows searching within the JSON event data for specific keys or values.
     *
     * @param key The JSON key to search for
     * @param value The value to match
     * @param limit Maximum number of events to return
     * @param offset Offset for pagination
     * @return A list of events with matching event data content
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> findByEventDataContent(String key, String value, int limit, int offset);

    /**
     * Retrieves a timeline of events for a transaction with pagination.
     *
     * @param transactionId The transaction identifier
     * @param limit Maximum number of events to return
     * @param offset Offset for pagination
     * @return A paginated list of events for the transaction in chronological order
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<PaymentEventEntity> getTransactionTimeline(UUID transactionId, int limit, int offset);
}