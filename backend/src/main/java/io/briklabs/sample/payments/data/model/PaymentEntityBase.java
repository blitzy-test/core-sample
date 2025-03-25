package io.briklabs.sample.payments.data.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for all payment-related entities.
 * <p>
 * This class provides shared functionality for ID generation, timestamp management,
 * and core data operations across payment entities. It serves as the foundation for
 * the payment entity inheritance hierarchy, ensuring consistent implementation patterns,
 * standardized validation, and centralized core logic for all payment data model classes.
 * </p>
 * <p>
 * By extending this base class, payment entity implementations benefit from:
 * <ul>
 *   <li>Standardized UUID-based primary key handling</li>
 *   <li>Consistent timestamp management for audit and tracking</li>
 *   <li>Common validation patterns</li>
 *   <li>Centralized utility methods</li>
 * </ul>
 * </p>
 */
public abstract class PaymentEntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Validates the entity data.
     * <p>
     * This method should be implemented by all subclasses to perform
     * entity-specific validation logic. It should throw an IllegalArgumentException
     * with a descriptive message if validation fails.
     * </p>
     *
     * @throws IllegalArgumentException if any validation fails
     */
    public abstract void validate();

    /**
     * Generates a new UUID for entity creation.
     * <p>
     * This utility method provides a standardized way to generate UUIDs
     * for primary keys across all payment entities.
     * </p>
     *
     * @return A new randomly generated UUID
     */
    protected UUID generateId() {
        return UUID.randomUUID();
    }

    /**
     * Gets the current timestamp for entity creation or updates.
     * <p>
     * This utility method provides a standardized way to generate timestamps
     * for audit fields across all payment entities.
     * </p>
     *
     * @return The current instant
     */
    protected Instant getCurrentTimestamp() {
        return Instant.now();
    }

    /**
     * Validates that a required field is not null.
     * <p>
     * This utility method provides a standardized way to validate
     * required fields across all payment entities.
     * </p>
     *
     * @param field The field to validate
     * @param fieldName The name of the field for the error message
     * @throws IllegalArgumentException if the field is null
     */
    protected void validateRequired(Object field, String fieldName) {
        if (field == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    /**
     * Validates that a required string field is not null or empty.
     * <p>
     * This utility method provides a standardized way to validate
     * required string fields across all payment entities.
     * </p>
     *
     * @param field The string field to validate
     * @param fieldName The name of the field for the error message
     * @throws IllegalArgumentException if the field is null or empty
     */
    protected void validateRequiredString(String field, String fieldName) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be empty");
        }
    }

    /**
     * Validates that a UUID field is not null.
     * <p>
     * This utility method provides a standardized way to validate
     * UUID fields across all payment entities.
     * </p>
     *
     * @param id The UUID to validate
     * @param fieldName The name of the field for the error message
     * @throws IllegalArgumentException if the UUID is null
     */
    protected void validateId(UUID id, String fieldName) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    /**
     * Validates that a timestamp field is not null.
     * <p>
     * This utility method provides a standardized way to validate
     * timestamp fields across all payment entities.
     * </p>
     *
     * @param timestamp The timestamp to validate
     * @param fieldName The name of the field for the error message
     * @throws IllegalArgumentException if the timestamp is null
     */
    protected void validateTimestamp(Instant timestamp, String fieldName) {
        if (timestamp == null) {
            throw new IllegalArgumentException(fieldName + " timestamp is required");
        }
    }

    /**
     * Validates that a currency code is in the correct format.
     * <p>
     * This utility method validates that a currency code is a 3-character
     * string as per ISO 4217 standard.
     * </p>
     *
     * @param currency The currency code to validate
     * @throws IllegalArgumentException if the currency code is invalid
     */
    protected void validateCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO 4217 code");
        }
    }

    /**
     * Creates a deep copy of the entity.
     * <p>
     * This method should be implemented by subclasses to provide
     * a proper deep copy implementation specific to each entity type.
     * </p>
     *
     * @return A deep copy of this entity
     */
    public abstract Object clone();

    /**
     * Converts the entity to a domain model object.
     * <p>
     * This method should be implemented by subclasses to provide
     * conversion from entity objects to domain model objects.
     * </p>
     *
     * @return A domain model object representing this entity
     */
    public abstract Object toDomainModel();

    /**
     * Prepares the entity for persistence operations.
     * <p>
     * This method performs any necessary operations before the entity
     * is persisted to the database, such as setting creation timestamps
     * or generating IDs if they are not already set.
     * </p>
     * <p>
     * Subclasses should override this method to add entity-specific
     * preparation logic while calling super.prepareForPersistence().
     * </p>
     */
    public void prepareForPersistence() {
        // Default implementation does nothing
        // Subclasses should override as needed
    }

    /**
     * Updates the entity's audit information before an update operation.
     * <p>
     * This method updates timestamps and other audit fields before
     * the entity is updated in the database.
     * </p>
     * <p>
     * Subclasses should override this method to add entity-specific
     * update logic while calling super.prepareForUpdate().
     * </p>
     */
    public void prepareForUpdate() {
        // Default implementation does nothing
        // Subclasses should override as needed
    }

    /**
     * Checks if this entity is new (not yet persisted).
     * <p>
     * This method should be implemented by subclasses to determine
     * if the entity has been persisted to the database.
     * </p>
     *
     * @return true if the entity is new, false otherwise
     */
    public abstract boolean isNew();

    /**
     * Gets the primary key of this entity.
     * <p>
     * This method should be implemented by subclasses to return
     * the primary key field of the entity.
     * </p>
     *
     * @return The primary key of this entity
     */
    public abstract UUID getId();
}