package io.briklabs.sample.payments.data.dao;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generic interface for payment data access operations.
 * Provides a consistent contract for database operations across all payment entities.
 * 
 * @param <T> The entity type this DAO operates on
 * @param <ID> The type of the entity's primary key (typically UUID)
 */
public interface PaymentDAO<T, ID> {
    
    /**
     * Creates a new entity in the database.
     * 
     * @param entity The entity to create
     * @return The created entity with any generated fields populated
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    T create(T entity);
    
    /**
     * Retrieves an entity by its primary identifier.
     * 
     * @param id The primary identifier of the entity
     * @return An Optional containing the entity if found, or empty if not found
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    Optional<T> findById(ID id);
    
    /**
     * Updates an existing entity in the database.
     * 
     * @param entity The entity to update
     * @return The updated entity
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     * @throws io.briklabs.sample.payments.data.exception.PaymentEntityNotFoundException if the entity does not exist
     */
    T update(T entity);
    
    /**
     * Deletes an entity from the database.
     * 
     * @param id The primary identifier of the entity to delete
     * @return true if the entity was deleted, false if it did not exist
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    boolean delete(ID id);
    
    /**
     * Queries for entities based on the provided filter parameters.
     * 
     * @param params The query parameters to filter by
     * @return A list of entities matching the query parameters
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<T> query(Object params);
    
    /**
     * Begins a database transaction.
     * 
     * @return A Connection with transaction started
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    Connection beginTransaction();
    
    /**
     * Commits a database transaction.
     * 
     * @param connection The connection with an active transaction
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    void commitTransaction(Connection connection);
    
    /**
     * Rolls back a database transaction.
     * 
     * @param connection The connection with an active transaction
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    void rollbackTransaction(Connection connection);
    
    /**
     * Creates multiple entities in a batch operation.
     * 
     * @param entities The list of entities to create
     * @return The list of created entities with any generated fields populated
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<T> batchCreate(List<T> entities);
    
    /**
     * Updates multiple entities in a batch operation.
     * 
     * @param entities The list of entities to update
     * @return The list of updated entities
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<T> batchUpdate(List<T> entities);
    
    /**
     * Executes a database operation within a transaction.
     * 
     * @param <R> The return type of the operation
     * @param operation The operation to execute
     * @return The result of the operation
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    <R> R executeInTransaction(TransactionOperation<R> operation);
    
    /**
     * Functional interface for operations that execute within a transaction.
     *
     * @param <R> The return type of the operation
     */
    @FunctionalInterface
    interface TransactionOperation<R> {
        /**
         * Executes an operation within a transaction.
         *
         * @param connection The database connection with an active transaction
         * @return The result of the operation
         * @throws Exception if an error occurs during the operation
         */
        R execute(Connection connection) throws Exception;
    }
    
    /**
     * Counts the total number of entities matching the provided filter parameters.
     * 
     * @param params The query parameters to filter by
     * @return The count of matching entities
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    long count(Object params);
    
    /**
     * Checks if an entity with the given ID exists.
     * 
     * @param id The primary identifier to check
     * @return true if an entity with the given ID exists, false otherwise
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    boolean exists(ID id);
    
    /**
     * Retrieves entities by their organization ID.
     * 
     * @param organizationId The organization ID to filter by
     * @return A list of entities belonging to the specified organization
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<T> findByOrganizationId(UUID organizationId);
    
    /**
     * Retrieves entities by their account ID.
     * 
     * @param accountId The account ID to filter by
     * @return A list of entities belonging to the specified account
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<T> findByAccountId(UUID accountId);
    
    /**
     * Retrieves entities by both organization ID and account ID.
     * 
     * @param organizationId The organization ID to filter by
     * @param accountId The account ID to filter by
     * @return A list of entities belonging to the specified organization and account
     * @throws io.briklabs.sample.payments.data.exception.PaymentDataAccessException if a database error occurs
     */
    List<T> findByOrganizationAndAccountId(UUID organizationId, UUID accountId);
}