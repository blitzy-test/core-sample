package io.briklabs.sample.payments.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Model class representing lifecycle events for payment transactions.
 * <p>
 * This class tracks all state changes, user actions, and processing milestones
 * throughout a transaction's lifecycle. It provides a comprehensive audit trail
 * with event type, timestamp, actor information, and detailed event data.
 * </p>
 * <p>
 * Events are immutable once created and are associated with a specific payment
 * transaction. The event history forms a chronological record of all activities
 * related to a transaction, essential for troubleshooting, compliance, and user tracking.
 * </p>
 */
public class PaymentEvent {

    /**
     * Unique identifier for this event.
     */
    private UUID eventId;

    /**
     * Reference to the associated payment transaction.
     */
    private UUID transactionId;

    /**
     * Classification of the event (e.g., CREATED, STATUS_CHANGE, CAPTURE, REFUND).
     */
    private String eventType;

    /**
     * Status of the transaction before this event occurred.
     * May be null for events that don't involve status changes.
     */
    private String previousStatus;

    /**
     * Status of the transaction after this event occurred.
     * May be null for events that don't involve status changes.
     */
    private String newStatus;

    /**
     * Detailed information about the event in JSON format.
     * This can include operation-specific details, amounts, error messages, etc.
     */
    private String eventData;

    /**
     * Timestamp when the event occurred.
     */
    private Instant createdAt;

    /**
     * Identifier of the user or system component that created this event.
     */
    private String createdBy;

    /**
     * Correlation ID to track related events across a request or operation.
     * Useful for linking events that are part of the same logical operation.
     */
    private UUID correlationId;

    /**
     * Default constructor for ORM frameworks.
     */
    public PaymentEvent() {
    }

    /**
     * Creates a new payment event with the specified details.
     *
     * @param eventId        Unique identifier for this event
     * @param transactionId  Reference to the associated payment transaction
     * @param eventType      Classification of the event
     * @param previousStatus Status before the event (may be null)
     * @param newStatus      Status after the event (may be null)
     * @param eventData      Detailed event information in JSON format
     * @param createdAt      Timestamp when the event occurred
     * @param createdBy      Identifier of the actor that created this event
     * @param correlationId  ID to track related events (may be null)
     */
    public PaymentEvent(UUID eventId, UUID transactionId, String eventType,
                        String previousStatus, String newStatus, String eventData,
                        Instant createdAt, String createdBy, UUID correlationId) {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.eventData = eventData;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.correlationId = correlationId;
    }

    /**
     * Factory method to create a status change event.
     *
     * @param transactionId  Reference to the associated payment transaction
     * @param previousStatus Status before the change
     * @param newStatus      Status after the change
     * @param eventData      Additional event details
     * @param createdBy      Identifier of the actor making the change
     * @param correlationId  ID to track related events (may be null)
     * @return A new PaymentEvent instance representing a status change
     */
    public static PaymentEvent createStatusChangeEvent(UUID transactionId,
                                                      String previousStatus,
                                                      String newStatus,
                                                      String eventData,
                                                      String createdBy,
                                                      UUID correlationId) {
        return new PaymentEvent(
                UUID.randomUUID(),
                transactionId,
                "STATUS_CHANGE",
                previousStatus,
                newStatus,
                eventData,
                Instant.now(),
                createdBy,
                correlationId
        );
    }

    /**
     * Factory method to create a general event without status change.
     *
     * @param transactionId Reference to the associated payment transaction
     * @param eventType     Classification of the event
     * @param eventData     Detailed event information
     * @param createdBy     Identifier of the actor creating the event
     * @param correlationId ID to track related events (may be null)
     * @return A new PaymentEvent instance
     */
    public static PaymentEvent createEvent(UUID transactionId,
                                          String eventType,
                                          String eventData,
                                          String createdBy,
                                          UUID correlationId) {
        return new PaymentEvent(
                UUID.randomUUID(),
                transactionId,
                eventType,
                null,
                null,
                eventData,
                Instant.now(),
                createdBy,
                correlationId
        );
    }

    /**
     * Factory method to create an error event.
     *
     * @param transactionId Reference to the associated payment transaction
     * @param errorMessage  Description of the error
     * @param errorDetails  Technical details about the error
     * @param createdBy     Identifier of the component reporting the error
     * @param correlationId ID to track related events (may be null)
     * @return A new PaymentEvent instance representing an error
     */
    public static PaymentEvent createErrorEvent(UUID transactionId,
                                               String errorMessage,
                                               String errorDetails,
                                               String createdBy,
                                               UUID correlationId) {
        String errorData = String.format("{\"message\":\"%s\",\"details\":%s}",
                errorMessage, errorDetails);
        
        return new PaymentEvent(
                UUID.randomUUID(),
                transactionId,
                "ERROR",
                null,
                null,
                errorData,
                Instant.now(),
                createdBy,
                correlationId
        );
    }

    // Getters and setters

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentEvent that = (PaymentEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "eventId=" + eventId +
                ", transactionId=" + transactionId +
                ", eventType='" + eventType + '\'' +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", correlationId=" + correlationId +
                '}';
    }
}