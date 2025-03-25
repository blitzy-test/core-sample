package io.briklabs.sample.payments.data.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentStatus;

/**
 * Entity class for payment lifecycle events, mapping to the payment_events table.
 * <p>
 * This entity creates a comprehensive audit trail of all activities related to payment
 * transactions, including status changes, processing steps, and user actions. It is critical
 * for payment tracking, debugging issues, and providing a complete history of each transaction
 * from creation through processing, capture, and potential refund operations.
 * </p>
 * <p>
 * The entity maintains a relationship to the parent transaction through the transaction_id
 * foreign key, supporting the one-to-many relationship from transactions to events.
 * </p>
 */
public class PaymentEventEntity extends PaymentEntityBase {

    /**
     * Unique event identifier (PRIMARY KEY).
     */
    private UUID eventId;

    /**
     * Associated transaction identifier (FOREIGN KEY).
     */
    private UUID transactionId;

    /**
     * Event classification (e.g., CREATED, STATUS_CHANGE, CAPTURE, REFUND).
     */
    private String eventType;

    /**
     * Status before event (may be null for non-status-change events).
     */
    private PaymentStatus previousStatus;

    /**
     * Status after event (may be null for non-status-change events).
     */
    private PaymentStatus newStatus;

    /**
     * Detailed event information in JSON format.
     * This field can store structured data specific to each event type,
     * such as capture amounts, refund details, or processing metadata.
     */
    private String eventData;

    /**
     * Event timestamp.
     */
    private Instant createdAt;

    /**
     * Actor identifier (user or system that triggered the event).
     */
    private String createdBy;

    /**
     * Request correlation ID for tracking related events across requests.
     */
    private UUID correlationId;

    /**
     * Default constructor for ORM frameworks.
     */
    public PaymentEventEntity() {
        // Default constructor for ORM frameworks
    }

    /**
     * Creates a new event entity with required fields.
     *
     * @param transactionId The associated transaction identifier
     * @param eventType The event classification
     * @param createdBy The actor identifier
     */
    public PaymentEventEntity(UUID transactionId, String eventType, String createdBy) {
        this.eventId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new status change event entity.
     *
     * @param transactionId The associated transaction identifier
     * @param previousStatus The status before the change
     * @param newStatus The status after the change
     * @param createdBy The actor identifier
     * @param correlationId Optional correlation ID for tracking related events
     */
    public PaymentEventEntity(UUID transactionId, PaymentStatus previousStatus, 
                             PaymentStatus newStatus, String createdBy, UUID correlationId) {
        this.eventId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.eventType = "STATUS_CHANGE";
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.createdBy = createdBy;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new event entity with all fields.
     *
     * @param eventId The event identifier
     * @param transactionId The associated transaction identifier
     * @param eventType The event classification
     * @param previousStatus The status before the event
     * @param newStatus The status after the event
     * @param eventData The detailed event information
     * @param createdAt The event timestamp
     * @param createdBy The actor identifier
     * @param correlationId The request correlation ID
     */
    public PaymentEventEntity(UUID eventId, UUID transactionId, String eventType,
                             PaymentStatus previousStatus, PaymentStatus newStatus,
                             String eventData, Instant createdAt, String createdBy,
                             UUID correlationId) {
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
     * Creates a transaction creation event.
     *
     * @param transactionId The transaction identifier
     * @param createdBy The actor identifier
     * @return A new PaymentEventEntity for transaction creation
     */
    public static PaymentEventEntity createTransactionCreatedEvent(UUID transactionId, String createdBy) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("CREATED");
        event.setNewStatus(PaymentStatus.CREATED);
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        return event;
    }

    /**
     * Creates a status change event.
     *
     * @param transactionId The transaction identifier
     * @param previousStatus The previous status
     * @param newStatus The new status
     * @param createdBy The actor identifier
     * @param correlationId Optional correlation ID
     * @return A new PaymentEventEntity for status change
     */
    public static PaymentEventEntity createStatusChangeEvent(UUID transactionId,
                                                           PaymentStatus previousStatus,
                                                           PaymentStatus newStatus,
                                                           String createdBy,
                                                           UUID correlationId) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("STATUS_CHANGE");
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        event.setCorrelationId(correlationId);
        return event;
    }

    /**
     * Creates a capture event.
     *
     * @param transactionId The transaction identifier
     * @param captureAmount The amount captured
     * @param createdBy The actor identifier
     * @param correlationId Optional correlation ID
     * @return A new PaymentEventEntity for capture
     */
    public static PaymentEventEntity createCaptureEvent(UUID transactionId,
                                                      String captureAmount,
                                                      String createdBy,
                                                      UUID correlationId) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("CAPTURE");
        event.setPreviousStatus(PaymentStatus.AUTHORIZED);
        event.setNewStatus(PaymentStatus.CAPTURED);
        event.setEventData("{\"captureAmount\":\"" + captureAmount + "\"}");
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        event.setCorrelationId(correlationId);
        return event;
    }

    /**
     * Creates a refund event.
     *
     * @param transactionId The transaction identifier
     * @param refundAmount The amount refunded
     * @param reason The refund reason
     * @param createdBy The actor identifier
     * @param correlationId Optional correlation ID
     * @return A new PaymentEventEntity for refund
     */
    public static PaymentEventEntity createRefundEvent(UUID transactionId,
                                                     String refundAmount,
                                                     String reason,
                                                     String createdBy,
                                                     UUID correlationId) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("REFUND");
        event.setPreviousStatus(PaymentStatus.CAPTURED);
        event.setNewStatus(PaymentStatus.REFUNDED);
        event.setEventData("{\"refundAmount\":\"" + refundAmount + "\",\"reason\":\"" + reason + "\"}");
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        event.setCorrelationId(correlationId);
        return event;
    }

    /**
     * Creates a void event.
     *
     * @param transactionId The transaction identifier
     * @param reason The void reason
     * @param createdBy The actor identifier
     * @param correlationId Optional correlation ID
     * @return A new PaymentEventEntity for void
     */
    public static PaymentEventEntity createVoidEvent(UUID transactionId,
                                                   String reason,
                                                   String createdBy,
                                                   UUID correlationId) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("VOID");
        event.setPreviousStatus(PaymentStatus.AUTHORIZED);
        event.setNewStatus(PaymentStatus.VOIDED);
        event.setEventData("{\"reason\":\"" + reason + "\"}");
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        event.setCorrelationId(correlationId);
        return event;
    }

    /**
     * Creates a processing error event.
     *
     * @param transactionId The transaction identifier
     * @param errorCode The error code
     * @param errorMessage The error message
     * @param createdBy The actor identifier
     * @param correlationId Optional correlation ID
     * @return A new PaymentEventEntity for processing error
     */
    public static PaymentEventEntity createErrorEvent(UUID transactionId,
                                                    String errorCode,
                                                    String errorMessage,
                                                    String createdBy,
                                                    UUID correlationId) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("ERROR");
        event.setPreviousStatus(PaymentStatus.PROCESSING);
        event.setNewStatus(PaymentStatus.FAILED);
        event.setEventData("{\"errorCode\":\"" + errorCode + "\",\"errorMessage\":\"" + errorMessage + "\"}");
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        event.setCorrelationId(correlationId);
        return event;
    }

    /**
     * Validates the event data.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp is required");
        }
        
        if (createdBy == null || createdBy.isEmpty()) {
            throw new IllegalArgumentException("Created by is required");
        }
        
        // Validate status transitions if this is a status change event
        if ("STATUS_CHANGE".equals(eventType)) {
            if (previousStatus == null) {
                throw new IllegalArgumentException("Previous status is required for status change events");
            }
            
            if (newStatus == null) {
                throw new IllegalArgumentException("New status is required for status change events");
            }
            
            // Validate that the status transition is allowed
            PaymentStatus.validateTransition(previousStatus, newStatus);
        }
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentEvent domain model object
     */
    public io.briklabs.sample.payments.model.PaymentEvent toDomainModel() {
        return new io.briklabs.sample.payments.model.PaymentEvent(
            eventId,
            transactionId,
            eventType,
            previousStatus,
            newStatus,
            eventData,
            createdAt,
            createdBy,
            correlationId
        );
    }

    /**
     * Creates an entity from a domain model object.
     *
     * @param domainModel The domain model object
     * @return A PaymentEventEntity
     */
    public static PaymentEventEntity fromDomainModel(
            io.briklabs.sample.payments.model.PaymentEvent domainModel) {
        return new PaymentEventEntity(
            domainModel.getEventId(),
            domainModel.getTransactionId(),
            domainModel.getEventType(),
            domainModel.getPreviousStatus(),
            domainModel.getNewStatus(),
            domainModel.getEventData(),
            domainModel.getCreatedAt(),
            domainModel.getCreatedBy(),
            domainModel.getCorrelationId()
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

    public PaymentStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(PaymentStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public PaymentStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PaymentStatus newStatus) {
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
        PaymentEventEntity that = (PaymentEventEntity) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "PaymentEventEntity{" +
                "eventId=" + eventId +
                ", transactionId=" + transactionId +
                ", eventType='" + eventType + '\'' +
                ", previousStatus=" + previousStatus +
                ", newStatus=" + newStatus +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", correlationId=" + correlationId +
                '}';
    }
}