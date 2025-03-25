package io.briklabs.sample.payments.data.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for all payment-related entities.
 * Provides common fields and functionality for payment data models including:
 * - UUID-based primary key management
 * - Timestamp tracking for audit purposes
 * - Standard object methods (equals, hashCode, toString)
 * - Validation utilities
 * 
 * This class serves as the foundation for the payment entity hierarchy,
 * ensuring consistent implementation patterns across all payment data models.
 */
public abstract class PaymentEntityBase {
    
    /**
     * Unique identifier for the entity.
     * Uses UUID to ensure global uniqueness across distributed systems.
     */
    private UUID id;
    
    /**
     * Timestamp when the entity was created.
     * Used for audit tracking and chronological ordering.
     */
    private Instant createdAt;
    
    /**
     * Timestamp when the entity was last updated.
     * Used for change tracking and optimistic locking.
     * May be null for newly created entities.
     */
    private Instant updatedAt;
    
    /**
     * Default constructor.
     * Initializes a new entity with a random UUID and current timestamp.
     */
    protected PaymentEntityBase() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }
    
    /**
     * Constructor with specified ID.
     * Used for creating entities with pre-defined IDs (e.g., from database).
     * 
     * @param id The unique identifier for this entity
     */
    protected PaymentEntityBase(UUID id) {
        this.id = id;
        this.createdAt = Instant.now();
    }
    
    /**
     * Full constructor with all base fields.
     * Used primarily for reconstructing entities from database records.
     * 
     * @param id The unique identifier for this entity
     * @param createdAt The timestamp when this entity was created
     * @param updatedAt The timestamp when this entity was last updated
     */
    protected PaymentEntityBase(UUID id, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Gets the unique identifier for this entity.
     * 
     * @return The UUID of this entity
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier for this entity.
     * This method should be used with caution, typically only when
     * reconstructing entities from database records.
     * 
     * @param id The UUID to set for this entity
     */
    public void setId(UUID id) {
        this.id = id;
    }
    
    /**
     * Gets the creation timestamp of this entity.
     * 
     * @return The Instant when this entity was created
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the creation timestamp of this entity.
     * This method should be used with caution, typically only when
     * reconstructing entities from database records.
     * 
     * @param createdAt The creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the last update timestamp of this entity.
     * 
     * @return The Instant when this entity was last updated, or null if never updated
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the last update timestamp of this entity.
     * 
     * @param updatedAt The update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Updates the entity's updatedAt timestamp to the current time.
     * Should be called whenever the entity is modified.
     */
    public void markUpdated() {
        this.updatedAt = Instant.now();
    }
    
    /**
     * Validates that a string value is not null or empty.
     * 
     * @param value The string to validate
     * @param fieldName The name of the field being validated (for error messages)
     * @throws IllegalArgumentException if the value is null or empty
     */
    protected void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
    
    /**
     * Validates that an object is not null.
     * 
     * @param value The object to validate
     * @param fieldName The name of the field being validated (for error messages)
     * @throws IllegalArgumentException if the value is null
     */
    protected void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }
    
    /**
     * Validates that a numeric value is positive (greater than zero).
     * 
     * @param value The numeric value to validate
     * @param fieldName The name of the field being validated (for error messages)
     * @throws IllegalArgumentException if the value is not positive
     */
    protected void validatePositive(Number value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        
        if (value.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
    
    /**
     * Validates that a string has a specific length.
     * 
     * @param value The string to validate
     * @param length The exact required length
     * @param fieldName The name of the field being validated (for error messages)
     * @throws IllegalArgumentException if the value's length doesn't match the requirement
     */
    protected void validateExactLength(String value, int length, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        
        if (value.length() != length) {
            throw new IllegalArgumentException(fieldName + " must be exactly " + length + " characters");
        }
    }
    
    /**
     * Validates that a string doesn't exceed a maximum length.
     * 
     * @param value The string to validate
     * @param maxLength The maximum allowed length
     * @param fieldName The name of the field being validated (for error messages)
     * @throws IllegalArgumentException if the value's length exceeds the maximum
     */
    protected void validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }
    
    /**
     * Checks if this entity is new (has not been persisted to the database).
     * This is typically determined by whether the entity has been updated.
     * 
     * @return true if the entity is new, false otherwise
     */
    public boolean isNew() {
        return updatedAt == null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentEntityBase that = (PaymentEntityBase) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}