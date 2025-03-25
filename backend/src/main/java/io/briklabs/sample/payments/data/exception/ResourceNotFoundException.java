package io.briklabs.sample.payments.data.exception;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Exception thrown when requested payment resources (transactions, payment methods, etc.)
 * are not found in the database.
 * <p>
 * This exception provides context about the resource type, identifiers used for lookup,
 * and search criteria that led to the not-found condition. It enables proper error handling
 * for expected not-found scenarios versus unexpected data access errors.
 * </p>
 * <p>
 * ResourceNotFoundException is used to distinguish between missing data (which may be an
 * expected condition in some workflows) and other types of data access errors that indicate
 * actual system problems.
 * </p>
 */
public class ResourceNotFoundException extends PaymentDataException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code prefix for resource not found exceptions.
     */
    private static final String ERROR_CODE_PREFIX = "RNF";

    /**
     * Resource type that was not found.
     */
    private final String resourceType;

    /**
     * Map of search criteria used in the lookup attempt.
     */
    private final Map<String, Object> searchCriteria;

    /**
     * Creates a new ResourceNotFoundException for a specific resource type with a default error code.
     *
     * @param resourceType the type of resource that was not found (e.g., "transaction", "payment_method")
     * @param message the error message
     */
    public ResourceNotFoundException(String resourceType, String message) {
        super(message, ERROR_CODE_PREFIX + "-0001");
        this.resourceType = resourceType;
        this.searchCriteria = new HashMap<>();
    }

    /**
     * Creates a new ResourceNotFoundException for a specific resource type with a specific error code.
     *
     * @param resourceType the type of resource that was not found
     * @param message the error message
     * @param errorCode the specific error code suffix (will be prefixed with RNF-)
     */
    public ResourceNotFoundException(String resourceType, String message, String errorCode) {
        super(message, ERROR_CODE_PREFIX + "-" + errorCode);
        this.resourceType = resourceType;
        this.searchCriteria = new HashMap<>();
    }

    /**
     * Creates a new ResourceNotFoundException for a resource identified by ID.
     *
     * @param resourceType the type of resource that was not found
     * @param id the identifier of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, UUID id) {
        super("Resource of type {0} with ID {1} was not found", resourceType, id);
        this.resourceType = resourceType;
        this.searchCriteria = new HashMap<>();
        this.searchCriteria.put("id", id);
    }

    /**
     * Creates a new ResourceNotFoundException for a resource identified by a string identifier.
     *
     * @param resourceType the type of resource that was not found
     * @param identifierName the name of the identifier field (e.g., "reference", "merchant_id")
     * @param identifierValue the value of the identifier that was not found
     */
    public ResourceNotFoundException(String resourceType, String identifierName, String identifierValue) {
        super("Resource of type {0} with {1} {2} was not found", 
              resourceType, identifierName, identifierValue);
        this.resourceType = resourceType;
        this.searchCriteria = new HashMap<>();
        this.searchCriteria.put(identifierName, identifierValue);
    }

    /**
     * Creates a new ResourceNotFoundException with multiple search criteria.
     *
     * @param resourceType the type of resource that was not found
     * @param criteria the search criteria that led to the not-found condition
     */
    public ResourceNotFoundException(String resourceType, Map<String, Object> criteria) {
        super("Resource of type {0} matching criteria {1} was not found", 
              resourceType, criteria.toString());
        this.resourceType = resourceType;
        this.searchCriteria = new HashMap<>(criteria);
    }

    /**
     * Creates a new ResourceNotFoundException with a formatted message.
     *
     * @param resourceType the type of resource that was not found
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param args the arguments to be formatted into the message pattern
     */
    public ResourceNotFoundException(String resourceType, String messagePattern, Object... args) {
        super(messagePattern, ERROR_CODE_PREFIX + "-0001", args);
        this.resourceType = resourceType;
        this.searchCriteria = new HashMap<>();
    }

    /**
     * Creates a new ResourceNotFoundException with a formatted message and specific error code.
     *
     * @param resourceType the type of resource that was not found
     * @param messagePattern the message pattern using MessageFormat syntax
     * @param errorCode the specific error code suffix (will be prefixed with RNF-)
     * @param args the arguments to be formatted into the message pattern
     */
    public ResourceNotFoundException(String resourceType, String messagePattern, String errorCode, Object... args) {
        super(messagePattern, ERROR_CODE_PREFIX + "-" + errorCode, args);
        this.resourceType = resourceType;
        this.searchCriteria = new HashMap<>();
    }

    /**
     * Adds a search criterion to the exception context.
     *
     * @param key the name of the search criterion
     * @param value the value of the search criterion
     * @return this exception instance for method chaining
     */
    public ResourceNotFoundException addCriterion(String key, Object value) {
        this.searchCriteria.put(key, value);
        return this;
    }

    /**
     * Gets the type of resource that was not found.
     *
     * @return the resource type
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Gets the search criteria that led to the not-found condition.
     *
     * @return an unmodifiable map of search criteria
     */
    public Map<String, Object> getSearchCriteria() {
        return Map.copyOf(searchCriteria);
    }

    /**
     * Returns a string representation of this exception including the resource type
     * and search criteria.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        return getClass().getName() + " [" + getErrorCode() + "] for resource type '" + 
               resourceType + "' with criteria " + searchCriteria + ": " + getMessage();
    }
}