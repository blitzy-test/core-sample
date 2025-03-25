package io.briklabs.sample.payments.data.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;

/**
 * Generic interface for payment data access operations.
 * <p>
 * This interface defines the standard operations to be performed on payment entities.
 * It serves as the foundation for all payment-specific DAOs, establishing a consistent
 * contract for database operations such as create, read, update, and delete.
 * </p>
 * <p>
 * All payment entities follow this pattern for data access, enabling uniform handling
 * across different payment data types while supporting specialized operations through
 * concrete implementations.
 * </p>
 *
 * @param <T> the entity type this DAO manages
 * @param <ID> the type of the entity's primary identifier
 */
public interface PaymentDAO<T, ID> {

    /**
     * Creates a new entity in the database.
     *
     * @param entity the entity to create
     * @return the created entity with any database-generated values (e.g., IDs)
     * @throws ValidationException if the entity fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    T create(T entity) throws ValidationException, ConnectionException, 
                             QueryExecutionException, TransactionException;

    /**
     * Retrieves an entity by its primary identifier.
     *
     * @param id the entity identifier
     * @return an Optional containing the found entity, or empty if not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    Optional<T> findById(ID id) throws ConnectionException, QueryExecutionException;

    /**
     * Retrieves an entity by its primary identifier, throwing an exception if not found.
     *
     * @param id the entity identifier
     * @return the found entity
     * @throws ResourceNotFoundException if the entity is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    default T getById(ID id) throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        return findById(id).orElseThrow(() -> 
            new ResourceNotFoundException("Entity not found with id: " + id));
    }

    /**
     * Updates an existing entity in the database.
     *
     * @param entity the entity to update
     * @return the updated entity
     * @throws ValidationException if the entity fails validation
     * @throws ResourceNotFoundException if the entity to update is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    T update(T entity) throws ValidationException, ResourceNotFoundException, 
                             ConnectionException, QueryExecutionException, TransactionException;

    /**
     * Deletes an entity by its primary identifier.
     *
     * @param id the entity identifier
     * @return true if the entity was deleted, false if it did not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    boolean delete(ID id) throws ConnectionException, QueryExecutionException, TransactionException;

    /**
     * Queries for entities based on the provided filter parameters.
     *
     * @param filterParams the parameters to filter the query results
     * @return a list of entities matching the filter criteria
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    List<T> query(PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException;

    /**
     * Counts the number of entities matching the provided filter parameters.
     *
     * @param filterParams the parameters to filter the count
     * @return the count of matching entities
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    long count(PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException;

    /**
     * Checks if an entity with the given ID exists.
     *
     * @param id the entity identifier
     * @return true if an entity with the given ID exists, false otherwise
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    boolean exists(ID id) throws ConnectionException, QueryExecutionException;

    /**
     * Begins a database transaction.
     * <p>
     * This method should be called before a sequence of operations that need to be executed
     * within a single transaction.
     * </p>
     *
     * @throws ConnectionException if a database connection cannot be established
     * @throws TransactionException if the transaction cannot be started
     */
    void beginTransaction() throws ConnectionException, TransactionException;

    /**
     * Commits the current database transaction.
     * <p>
     * This method should be called after a sequence of operations that were executed
     * within a single transaction to persist the changes.
     * </p>
     *
     * @throws TransactionException if the transaction cannot be committed
     */
    void commitTransaction() throws TransactionException;

    /**
     * Rolls back the current database transaction.
     * <p>
     * This method should be called to discard changes made within a transaction
     * when an error occurs.
     * </p>
     *
     * @throws TransactionException if the transaction cannot be rolled back
     */
    void rollbackTransaction() throws TransactionException;

    /**
     * Creates multiple entities in a batch operation.
     * <p>
     * This method is optimized for bulk insertions, providing better performance
     * than individual create operations.
     * </p>
     *
     * @param entities the list of entities to create
     * @return the list of created entities with any database-generated values
     * @throws ValidationException if any entity fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    List<T> batchCreate(List<T> entities) throws ValidationException, ConnectionException,
                                               QueryExecutionException, TransactionException;

    /**
     * Updates multiple entities in a batch operation.
     * <p>
     * This method is optimized for bulk updates, providing better performance
     * than individual update operations.
     * </p>
     *
     * @param entities the list of entities to update
     * @return the list of updated entities
     * @throws ValidationException if any entity fails validation
     * @throws ResourceNotFoundException if any entity to update is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    List<T> batchUpdate(List<T> entities) throws ValidationException, ResourceNotFoundException,
                                               ConnectionException, QueryExecutionException, TransactionException;

    /**
     * Deletes multiple entities in a batch operation.
     * <p>
     * This method is optimized for bulk deletions, providing better performance
     * than individual delete operations.
     * </p>
     *
     * @param ids the list of entity identifiers to delete
     * @return the number of entities deleted
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    int batchDelete(List<ID> ids) throws ConnectionException, QueryExecutionException, TransactionException;
}