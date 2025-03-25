package io.briklabs.sample.payments.data.dao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.model.PaymentEvent;

/**
 * Interface for payment event data access operations enabling comprehensive event tracking and audit trails.
 * <p>
 * This DAO manages the storage and retrieval of all lifecycle events for payment transactions,
 * providing a complete history of status changes, user actions, and processing milestones.
 * It supports event querying by transaction ID, event type, and timeline filtering, essential
 * for transaction debugging, audit compliance, and user activity tracking.
 * </p>
 */
public interface PaymentEventDAO extends PaymentDAO<PaymentEvent, UUID> {

    /**
     * Finds all events for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of events for the specified transaction, ordered chronologically
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByTransactionId(UUID transactionId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds all events for a specific transaction with filtering parameters.
     *
     * @param transactionId The transaction identifier
     * @param filterParams Additional filtering parameters
     * @return List of events for the specified transaction, filtered and ordered according to parameters
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByTransactionId(UUID transactionId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events by event type.
     *
     * @param eventType The type of event to find
     * @param filterParams Additional filtering parameters
     * @return List of events of the specified type
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByEventType(String eventType, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events by multiple event types.
     *
     * @param eventTypes List of event types to find
     * @param filterParams Additional filtering parameters
     * @return List of events matching any of the specified types
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByEventTypeIn(List<String> eventTypes, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events created within a specific time range.
     *
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @param filterParams Additional filtering parameters
     * @return List of events created within the specified time range
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByCreatedAtBetween(Instant startTime, Instant endTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events created by a specific user or system.
     *
     * @param createdBy The identifier of the user or system that created the events
     * @param filterParams Additional filtering parameters
     * @return List of events created by the specified user or system
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByCreatedBy(String createdBy, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events with a specific correlation ID.
     *
     * @param correlationId The correlation identifier
     * @param filterParams Additional filtering parameters
     * @return List of events with the specified correlation ID
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByCorrelationId(UUID correlationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds status change events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param filterParams Additional filtering parameters
     * @return List of status change events for the specified transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findStatusChangeEvents(UUID transactionId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds error events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param filterParams Additional filtering parameters
     * @return List of error events for the specified transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findErrorEvents(UUID transactionId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds the most recent event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The most recent event for the specified transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    PaymentEvent findMostRecentEvent(UUID transactionId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds the most recent event of a specific type for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param eventType The type of event to find
     * @return The most recent event of the specified type for the transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    PaymentEvent findMostRecentEventByType(UUID transactionId, String eventType) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Creates a status change event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param previousStatus The previous status
     * @param newStatus The new status
     * @param createdBy The identifier of the user or system creating the event
     * @return The created event
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    PaymentEvent createStatusChangeEvent(UUID transactionId, String previousStatus, String newStatus, String createdBy) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Creates an error event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param errorCode The error code
     * @param errorMessage The error message
     * @param createdBy The identifier of the system reporting the error
     * @return The created event
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    PaymentEvent createErrorEvent(UUID transactionId, String errorCode, String errorMessage, String createdBy) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events for multiple transactions.
     *
     * @param transactionIds List of transaction identifiers
     * @param filterParams Additional filtering parameters
     * @return List of events for the specified transactions
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByTransactionIdIn(List<UUID> transactionIds, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Counts events by type for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return Map of event type to count
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    java.util.Map<String, Long> countEventsByType(UUID transactionId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events for transactions belonging to a specific organization.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of events for transactions belonging to the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events for transactions belonging to a specific account.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param filterParams Additional filtering parameters
     * @return List of events for transactions belonging to the specified account
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events related to status transitions to a specific status.
     *
     * @param status The target status
     * @param filterParams Additional filtering parameters
     * @return List of events representing transitions to the specified status
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByNewStatus(String status, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events related to status transitions from a specific status.
     *
     * @param status The source status
     * @param filterParams Additional filtering parameters
     * @return List of events representing transitions from the specified status
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByPreviousStatus(String status, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Finds events containing specific data in the event_data JSON field.
     *
     * @param jsonPath The JSON path expression
     * @param value The value to match
     * @param filterParams Additional filtering parameters
     * @return List of events matching the JSON criteria
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> findByEventDataContains(String jsonPath, String value, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Gets the complete timeline of events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of events ordered chronologically
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> getTransactionTimeline(UUID transactionId) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Gets the audit trail for a specific time period.
     *
     * @param startTime The start of the time period
     * @param endTime The end of the time period
     * @param filterParams Additional filtering parameters
     * @return List of events within the specified time period
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> getAuditTrail(Instant startTime, Instant endTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;

    /**
     * Gets the user activity log for a specific user.
     *
     * @param userId The user identifier
     * @param startTime The start of the time period
     * @param endTime The end of the time period
     * @param filterParams Additional filtering parameters
     * @return List of events created by the specified user within the time period
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<PaymentEvent> getUserActivityLog(String userId, Instant startTime, Instant endTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException;
}