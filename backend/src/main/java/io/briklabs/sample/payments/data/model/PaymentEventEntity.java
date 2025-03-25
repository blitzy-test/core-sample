package io.briklabs.sample.payments.data.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentStatus;

/**
 * Entity class for payment lifecycle events, mapping to the payment_events table.
 * <p>
 * This class tracks all state changes, user actions, and processing milestones
 * throughout a transaction's lifecycle. It provides a comprehensive audit trail
 * with event type, timestamp, actor information, and detailed event data.
 * </p>
 * <p>
 * The payment_events table maintains a complete history of all activities related
 * to payment transactions, essential for troubleshooting, compliance, and user tracking.
 * Each event is immutable once created and is associated with a specific payment transaction.
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
     * Detailed information about the event in JSONB format.
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
    public PaymentEventEntity() {
        // Default constructor for ORM frameworks
    }

    /**
     * Creates a new payment event entity with required fields.
     *
     * @param transactionId Reference to the associated payment transaction
     * @param eventType Classification of the event
     * @param createdBy Identifier of the actor creating the event
     */
    public PaymentEventEntity(UUID transactionId, String eventType, String createdBy) {
        this.eventId = generateId();
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.createdBy = createdBy;
        this.createdAt = getCurrentTimestamp();
    }

    /**
     * Creates a new payment event entity with all fields.
     *
     * @param eventId Unique identifier for this event
     * @param transactionId Reference to the associated payment transaction
     * @param eventType Classification of the event
     * @param previousStatus Status before the event (may be null)
     * @param newStatus Status after the event (may be null)
     * @param eventData Detailed event information in JSON format
     * @param createdAt Timestamp when the event occurred
     * @param createdBy Identifier of the actor that created this event
     * @param correlationId ID to track related events (may be null)
     */
    public PaymentEventEntity(UUID eventId, UUID transactionId, String eventType,
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
     * Factory method to create a status change event entity.
     *
     * @param transactionId Reference to the associated payment transaction
     * @param previousStatus Status before the change
     * @param newStatus Status after the change
     * @param eventData Additional event details
     * @param createdBy Identifier of the actor making the change
     * @param correlationId ID to track related events (may be null)
     * @return A new PaymentEventEntity instance representing a status change
     */
    public static PaymentEventEntity createStatusChangeEvent(UUID transactionId,
                                                           PaymentStatus previousStatus,
                                                           PaymentStatus newStatus,
                                                           String eventData,
                                                           String createdBy,
                                                           UUID correlationId) {
        return new PaymentEventEntity(
                UUID.randomUUID(),
                transactionId,
                "STATUS_CHANGE",
                previousStatus != null ? previousStatus.name() : null,
                newStatus != null ? newStatus.name() : null,
                eventData,
                Instant.now(),
                createdBy,
                correlationId
        );
    }

    /**
     * Factory method to create a general event entity without status change.
     *
     * @param transactionId Reference to the associated payment transaction
     * @param eventType Classification of the event
     * @param eventData Detailed event information
     * @param createdBy Identifier of the actor creating the event
     * @param correlationId ID to track related events (may be null)
     * @return A new PaymentEventEntity instance
     */
    public static PaymentEventEntity createEvent(UUID transactionId,
                                               String eventType,
                                               String eventData,
                                               String createdBy,
                                               UUID correlationId) {
        return new PaymentEventEntity(
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
     * Factory method to create an error event entity.
     *
     * @param transactionId Reference to the associated payment transaction
     * @param errorMessage Description of the error
     * @param errorDetails Technical details about the error
     * @param createdBy Identifier of the component reporting the error
     * @param correlationId ID to track related events (may be null)
     * @return A new PaymentEventEntity instance representing an error
     */
    public static PaymentEventEntity createErrorEvent(UUID transactionId,
                                                    String errorMessage,
                                                    String errorDetails,
                                                    String createdBy,
                                                    UUID correlationId) {
        String errorData = String.format("{\"message\":\"%s\",\"details\":%s}",
                errorMessage, errorDetails);
        
        return new PaymentEventEntity(
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

    /**
     * Validates the event data.
     *
     * @throws IllegalArgumentException if any validation fails
     */
    @Override
    public void validate() {
        validateId(eventId, "Event ID");
        validateId(transactionId, "Transaction ID");
        validateRequiredString(eventType, "Event type");
        validateTimestamp(createdAt, "Created");
        validateRequiredString(createdBy, "Created by");
        
        // If this is a status change event, validate status values
        if ("STATUS_CHANGE".equals(eventType)) {
            if (previousStatus == null && newStatus == null) {
                throw new IllegalArgumentException("Status change event must have at least one status value");
            }
        }
    }

    /**
     * Prepares the entity for persistence operations.
     * <p>
     * This method sets creation timestamp if it is not already set,
     * and generates an event ID if it is not already set.
     * </p>
     */
    @Override
    public void prepareForPersistence() {
        if (eventId == null) {
            eventId = generateId();
        }
        
        if (createdAt == null) {
            createdAt = getCurrentTimestamp();
        }
    }

    /**
     * Updates the entity's audit information before an update operation.
     * <p>
     * Payment events are immutable once created, so this method throws
     * an exception if called.
     * </p>
     * 
     * @throws UnsupportedOperationException Payment events are immutable
     */
    @Override
    public void prepareForUpdate() {
        throw new UnsupportedOperationException("Payment events are immutable and cannot be updated");
    }

    /**
     * Checks if this entity is new (not yet persisted).
     *
     * @return true if the entity is new, false otherwise
     */
    @Override
    public boolean isNew() {
        return createdAt == null;
    }

    /**
     * Gets the primary key of this entity.
     *
     * @return The event ID (primary key)
     */
    @Override
    public UUID getId() {
        return eventId;
    }

    /**
     * Creates a deep copy of the entity.
     *
     * @return A new PaymentEventEntity with the same values
     */
    @Override
    public Object clone() {
        return new PaymentEventEntity(
            this.eventId,
            this.transactionId,
            this.eventType,
            this.previousStatus,
            this.newStatus,
            this.eventData,
            this.createdAt,
            this.createdBy,
            this.correlationId
        );
    }

    /**
     * Converts this entity to a domain model object.
     *
     * @return A PaymentEvent domain model object
     */
    @Override
    public PaymentEvent toDomainModel() {
        return new PaymentEvent(
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
    public static PaymentEventEntity fromDomainModel(PaymentEvent domainModel) {
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
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", correlationId=" + correlationId +
                '}';
    }
}