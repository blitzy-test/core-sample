package io.briklabs.sample.payments.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Model class representing lifecycle events for payment transactions.
 * This class tracks all state changes, user actions, and processing milestones
 * throughout a transaction's lifecycle. It provides a comprehensive audit trail
 * with event type, timestamp, actor information, and detailed event data.
 */
public class PaymentEvent {

    /**
     * Unique identifier for the event.
     */
    private UUID eventId;

    /**
     * Identifier of the transaction this event is associated with.
     */
    private UUID transactionId;

    /**
     * Type of event that occurred.
     */
    private String eventType;

    /**
     * Status of the transaction before the event occurred.
     */
    private String previousStatus;

    /**
     * Status of the transaction after the event occurred.
     */
    private String newStatus;

    /**
     * Detailed information about the event in JSON format.
     * This can include additional context, parameters, or results.
     */
    private String eventData;

    /**
     * Timestamp when the event occurred.
     */
    private Instant createdAt;

    /**
     * Identifier of the user or system that created the event.
     */
    private String createdBy;

    /**
     * Correlation identifier for tracking related events across systems.
     * Useful for distributed tracing and cross-service event correlation.
     */
    private UUID correlationId;

    /**
     * Default constructor for serialization frameworks.
     */
    public PaymentEvent() {
        // Default constructor for serialization frameworks
    }

    /**
     * Creates a new payment event with required fields.
     *
     * @param transactionId The transaction identifier
     * @param eventType The type of event
     * @param createdBy The identifier of the actor creating the event
     */
    public PaymentEvent(UUID transactionId, String eventType, String createdBy) {
        this.eventId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new payment event for a status change.
     *
     * @param transactionId The transaction identifier
     * @param previousStatus The previous transaction status
     * @param newStatus The new transaction status
     * @param createdBy The identifier of the actor creating the event
     */
    public PaymentEvent(UUID transactionId, String previousStatus, String newStatus, String createdBy) {
        this.eventId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.eventType = "STATUS_CHANGE";
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new payment event with all fields.
     *
     * @param eventId The event identifier
     * @param transactionId The transaction identifier
     * @param eventType The type of event
     * @param previousStatus The previous transaction status
     * @param newStatus The new transaction status
     * @param eventData The detailed event data
     * @param createdAt The creation timestamp
     * @param createdBy The identifier of the actor creating the event
     * @param correlationId The correlation identifier
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
     * Creates a status change event for a transaction.
     *
     * @param transaction The transaction being updated
     * @param newStatus The new status being applied
     * @param createdBy The identifier of the actor creating the event
     * @return A new PaymentEvent representing the status change
     */
    public static PaymentEvent createStatusChangeEvent(PaymentTransaction transaction, 
                                                     PaymentStatus newStatus, 
                                                     String createdBy) {
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transaction.getTransactionId());
        event.setEventType("STATUS_CHANGE");
        event.setPreviousStatus(transaction.getStatus().name());
        event.setNewStatus(newStatus.name());
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        return event;
    }

    /**
     * Creates a transaction creation event.
     *
     * @param transaction The newly created transaction
     * @param createdBy The identifier of the actor creating the transaction
     * @return A new PaymentEvent representing the transaction creation
     */
    public static PaymentEvent createTransactionCreatedEvent(PaymentTransaction transaction, 
                                                           String createdBy) {
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transaction.getTransactionId());
        event.setEventType("TRANSACTION_CREATED");
        event.setNewStatus(transaction.getStatus().name());
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        return event;
    }

    /**
     * Creates a transaction processing event.
     *
     * @param transaction The transaction being processed
     * @param createdBy The identifier of the actor initiating the processing
     * @param eventData Additional processing details
     * @return A new PaymentEvent representing the processing action
     */
    public static PaymentEvent createProcessingEvent(PaymentTransaction transaction, 
                                                   String createdBy, 
                                                   String eventData) {
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transaction.getTransactionId());
        event.setEventType("PROCESSING_INITIATED");
        event.setPreviousStatus(transaction.getStatus().name());
        event.setEventData(eventData);
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        return event;
    }

    /**
     * Creates a transaction capture event.
     *
     * @param transaction The transaction being captured
     * @param createdBy The identifier of the actor initiating the capture
     * @param amount The amount being captured
     * @return A new PaymentEvent representing the capture action
     */
    public static PaymentEvent createCaptureEvent(PaymentTransaction transaction, 
                                                String createdBy, 
                                                String amount) {
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transaction.getTransactionId());
        event.setEventType("CAPTURE_INITIATED");
        event.setPreviousStatus(transaction.getStatus().name());
        event.setEventData("{\"captureAmount\":\"" + amount + "\"}");
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        return event;
    }

    /**
     * Creates a transaction refund event.
     *
     * @param transaction The transaction being refunded
     * @param createdBy The identifier of the actor initiating the refund
     * @param amount The amount being refunded
     * @param reason The reason for the refund
     * @return A new PaymentEvent representing the refund action
     */
    public static PaymentEvent createRefundEvent(PaymentTransaction transaction, 
                                               String createdBy, 
                                               String amount, 
                                               String reason) {
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transaction.getTransactionId());
        event.setEventType("REFUND_INITIATED");
        event.setPreviousStatus(transaction.getStatus().name());
        event.setEventData("{\"refundAmount\":\"" + amount + "\",\"reason\":\"" + reason + "\"}");
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        return event;
    }

    /**
     * Creates a transaction error event.
     *
     * @param transaction The transaction experiencing an error
     * @param createdBy The identifier of the system reporting the error
     * @param errorCode The error code
     * @param errorMessage The error message
     * @return A new PaymentEvent representing the error
     */
    public static PaymentEvent createErrorEvent(PaymentTransaction transaction, 
                                              String createdBy, 
                                              String errorCode, 
                                              String errorMessage) {
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transaction.getTransactionId());
        event.setEventType("ERROR");
        event.setPreviousStatus(transaction.getStatus().name());
        event.setEventData("{\"errorCode\":\"" + errorCode + "\",\"errorMessage\":\"" + errorMessage + "\"}");
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        return event;
    }

    /**
     * Validates the event data.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        if (createdBy == null || createdBy.isEmpty()) {
            throw new IllegalArgumentException("Created by is required");
        }
        
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at timestamp is required");
        }
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