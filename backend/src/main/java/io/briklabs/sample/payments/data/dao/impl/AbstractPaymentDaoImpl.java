package io.briklabs.sample.payments.data.dao.impl;

import com.zaxxer.hikari.HikariDataSource;
import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.ConnectionManager;
import io.briklabs.sample.payments.data.HikariCPConfig;
import io.briklabs.sample.payments.data.dao.PaymentDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.PaymentDataException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Base implementation class for all payment DAO interfaces that provides common functionality
 * for database operations. It manages HikariCP connection acquisition, transaction handling,
 * prepared statement creation, resource cleanup, and exception translation.
 * <p>
 * This class serves as the foundation for all concrete payment DAO implementations,
 * providing standardized database interaction patterns and reusable utility methods
 * to ensure consistent error handling and resource management across all payment
 * data access operations.
 * </p>
 *
 * @param <T> The entity type this DAO operates on
 * @param <ID> The type of the entity's primary key (typically UUID)
 */
public abstract class AbstractPaymentDaoImpl<T, ID> implements PaymentDAO<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPaymentDaoImpl.class);

    /**
     * Connection manager for database operations.
     */
    protected final ConnectionManager connectionManager;

    /**
     * Creates a new AbstractPaymentDaoImpl with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     */
    protected AbstractPaymentDaoImpl(DatabaseConfig databaseConfig, ConfigSource configSource) {
        this.connectionManager = new ConnectionManager(databaseConfig, configSource);
        logger.debug("Initialized AbstractPaymentDaoImpl with database configuration");
    }

    /**
     * Creates a new AbstractPaymentDaoImpl with the specified connection manager.
     *
     * @param connectionManager the connection manager
     */
    protected AbstractPaymentDaoImpl(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        logger.debug("Initialized AbstractPaymentDaoImpl with provided connection manager");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T create(T entity) {
        logger.debug("Creating entity: {}", entity);
        try {
            return executeInTransaction(connection -> {
                return executeCreate(connection, entity);
            });
        } catch (Exception e) {
            throw handleException("Failed to create entity", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<T> findById(ID id) {
        logger.debug("Finding entity by ID: {}", id);
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeFindById(connection, id);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find entity by ID: " + id, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T update(T entity) {
        logger.debug("Updating entity: {}", entity);
        try {
            return executeInTransaction(connection -> {
                return executeUpdate(connection, entity);
            });
        } catch (Exception e) {
            throw handleException("Failed to update entity", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(ID id) {
        logger.debug("Deleting entity with ID: {}", id);
        try {
            return executeInTransaction(connection -> {
                return executeDelete(connection, id);
            });
        } catch (Exception e) {
            throw handleException("Failed to delete entity with ID: " + id, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> query(Object params) {
        logger.debug("Querying entities with params: {}", params);
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeQuery(connection, params);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to query entities", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection beginTransaction() {
        logger.debug("Beginning transaction");
        try {
            connectionManager.beginTransaction();
            return connectionManager.getConnection();
        } catch (ConnectionException | TransactionException e) {
            throw handleException("Failed to begin transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitTransaction(Connection connection) {
        logger.debug("Committing transaction");
        try {
            connectionManager.commitTransaction();
        } catch (TransactionException e) {
            throw handleException("Failed to commit transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollbackTransaction(Connection connection) {
        logger.debug("Rolling back transaction");
        try {
            connectionManager.rollbackTransaction();
        } catch (TransactionException e) {
            throw handleException("Failed to rollback transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> batchCreate(List<T> entities) {
        logger.debug("Batch creating {} entities", entities.size());
        try {
            return executeInTransaction(connection -> {
                return executeBatchCreate(connection, entities);
            });
        } catch (Exception e) {
            throw handleException("Failed to batch create entities", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> batchUpdate(List<T> entities) {
        logger.debug("Batch updating {} entities", entities.size());
        try {
            return executeInTransaction(connection -> {
                return executeBatchUpdate(connection, entities);
            });
        } catch (Exception e) {
            throw handleException("Failed to batch update entities", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R executeInTransaction(TransactionOperation<R> operation) {
        logger.debug("Executing operation in transaction");
        boolean localTransaction = !connectionManager.isInTransaction();
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            Connection connection = connectionManager.getConnection();
            R result = operation.execute(connection);
            
            if (localTransaction) {
                commitTransaction(connection);
            }
            
            return result;
        } catch (Exception e) {
            if (localTransaction) {
                try {
                    rollbackTransaction(connectionManager.getConnection());
                } catch (Exception rollbackEx) {
                    logger.error("Failed to rollback transaction after error", rollbackEx);
                }
            }
            throw handleException("Transaction operation failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count(Object params) {
        logger.debug("Counting entities with params: {}", params);
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeCount(connection, params);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to count entities", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(ID id) {
        logger.debug("Checking if entity exists with ID: {}", id);
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeExists(connection, id);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to check if entity exists with ID: " + id, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findByOrganizationId(UUID organizationId) {
        logger.debug("Finding entities by organization ID: {}", organizationId);
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeFindByOrganizationId(connection, organizationId);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find entities by organization ID: " + organizationId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findByAccountId(UUID accountId) {
        logger.debug("Finding entities by account ID: {}", accountId);
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeFindByAccountId(connection, accountId);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find entities by account ID: " + accountId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findByOrganizationAndAccountId(UUID organizationId, UUID accountId) {
        logger.debug("Finding entities by organization ID: {} and account ID: {}", organizationId, accountId);
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeFindByOrganizationAndAccountId(connection, organizationId, accountId);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find entities by organization ID: " + organizationId + 
                    " and account ID: " + accountId, e);
        }
    }

    /**
     * Executes the create operation with the provided connection and entity.
     *
     * @param connection the database connection
     * @param entity the entity to create
     * @return the created entity with any generated fields populated
     * @throws SQLException if a database error occurs
     */
    protected abstract T executeCreate(Connection connection, T entity) throws SQLException;

    /**
     * Executes the find by ID operation with the provided connection and ID.
     *
     * @param connection the database connection
     * @param id the ID to find
     * @return an Optional containing the entity if found, or empty if not found
     * @throws SQLException if a database error occurs
     */
    protected abstract Optional<T> executeFindById(Connection connection, ID id) throws SQLException;

    /**
     * Executes the update operation with the provided connection and entity.
     *
     * @param connection the database connection
     * @param entity the entity to update
     * @return the updated entity
     * @throws SQLException if a database error occurs
     */
    protected abstract T executeUpdate(Connection connection, T entity) throws SQLException;

    /**
     * Executes the delete operation with the provided connection and ID.
     *
     * @param connection the database connection
     * @param id the ID of the entity to delete
     * @return true if the entity was deleted, false if it did not exist
     * @throws SQLException if a database error occurs
     */
    protected abstract boolean executeDelete(Connection connection, ID id) throws SQLException;

    /**
     * Executes the query operation with the provided connection and parameters.
     *
     * @param connection the database connection
     * @param params the query parameters
     * @return a list of entities matching the query parameters
     * @throws SQLException if a database error occurs
     */
    protected abstract List<T> executeQuery(Connection connection, Object params) throws SQLException;

    /**
     * Executes the batch create operation with the provided connection and entities.
     *
     * @param connection the database connection
     * @param entities the entities to create
     * @return the created entities with any generated fields populated
     * @throws SQLException if a database error occurs
     */
    protected abstract List<T> executeBatchCreate(Connection connection, List<T> entities) throws SQLException;

    /**
     * Executes the batch update operation with the provided connection and entities.
     *
     * @param connection the database connection
     * @param entities the entities to update
     * @return the updated entities
     * @throws SQLException if a database error occurs
     */
    protected abstract List<T> executeBatchUpdate(Connection connection, List<T> entities) throws SQLException;

    /**
     * Executes the count operation with the provided connection and parameters.
     *
     * @param connection the database connection
     * @param params the query parameters
     * @return the count of matching entities
     * @throws SQLException if a database error occurs
     */
    protected abstract long executeCount(Connection connection, Object params) throws SQLException;

    /**
     * Executes the exists operation with the provided connection and ID.
     *
     * @param connection the database connection
     * @param id the ID to check
     * @return true if an entity with the given ID exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    protected abstract boolean executeExists(Connection connection, ID id) throws SQLException;

    /**
     * Executes the find by organization ID operation with the provided connection and organization ID.
     *
     * @param connection the database connection
     * @param organizationId the organization ID to filter by
     * @return a list of entities belonging to the specified organization
     * @throws SQLException if a database error occurs
     */
    protected abstract List<T> executeFindByOrganizationId(Connection connection, UUID organizationId) throws SQLException;

    /**
     * Executes the find by account ID operation with the provided connection and account ID.
     *
     * @param connection the database connection
     * @param accountId the account ID to filter by
     * @return a list of entities belonging to the specified account
     * @throws SQLException if a database error occurs
     */
    protected abstract List<T> executeFindByAccountId(Connection connection, UUID accountId) throws SQLException;

    /**
     * Executes the find by organization and account ID operation with the provided connection,
     * organization ID, and account ID.
     *
     * @param connection the database connection
     * @param organizationId the organization ID to filter by
     * @param accountId the account ID to filter by
     * @return a list of entities belonging to the specified organization and account
     * @throws SQLException if a database error occurs
     */
    protected abstract List<T> executeFindByOrganizationAndAccountId(Connection connection, UUID organizationId, UUID accountId) throws SQLException;

    /**
     * Creates a prepared statement with the provided connection and SQL.
     *
     * @param connection the database connection
     * @param sql the SQL statement
     * @return the prepared statement
     * @throws SQLException if a database error occurs
     */
    protected PreparedStatement prepareStatement(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    /**
     * Creates a prepared statement with the provided connection, SQL, and generated keys flag.
     *
     * @param connection the database connection
     * @param sql the SQL statement
     * @param returnGeneratedKeys whether to return generated keys
     * @return the prepared statement
     * @throws SQLException if a database error occurs
     */
    protected PreparedStatement prepareStatement(Connection connection, String sql, boolean returnGeneratedKeys) throws SQLException {
        return connection.prepareStatement(sql, returnGeneratedKeys ? PreparedStatement.RETURN_GENERATED_KEYS : PreparedStatement.NO_GENERATED_KEYS);
    }

    /**
     * Sets parameters on a prepared statement.
     *
     * @param statement the prepared statement
     * @param params the parameters to set
     * @throws SQLException if a database error occurs
     */
    protected void setParameters(PreparedStatement statement, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * Executes a query and processes the results with a result handler.
     *
     * @param <R> the result type
     * @param connection the database connection
     * @param sql the SQL statement
     * @param resultHandler the function to process the result set
     * @param params the parameters for the query
     * @return the result of processing the result set
     * @throws SQLException if a database error occurs
     */
    protected <R> R executeQuery(Connection connection, String sql, Function<ResultSet, R> resultHandler, Object... params) throws SQLException {
        try (PreparedStatement statement = prepareStatement(connection, sql)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultHandler.apply(resultSet);
            }
        }
    }

    /**
     * Executes an update statement.
     *
     * @param connection the database connection
     * @param sql the SQL statement
     * @param params the parameters for the update
     * @return the number of rows affected
     * @throws SQLException if a database error occurs
     */
    protected int executeUpdate(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = prepareStatement(connection, sql)) {
            setParameters(statement, params);
            return statement.executeUpdate();
        }
    }

    /**
     * Executes an update statement and returns generated keys.
     *
     * @param <R> the result type
     * @param connection the database connection
     * @param sql the SQL statement
     * @param generatedKeyHandler the function to process the generated keys
     * @param params the parameters for the update
     * @return the result of processing the generated keys
     * @throws SQLException if a database error occurs
     */
    protected <R> R executeUpdateWithGeneratedKeys(Connection connection, String sql, Function<ResultSet, R> generatedKeyHandler, Object... params) throws SQLException {
        try (PreparedStatement statement = prepareStatement(connection, sql, true)) {
            setParameters(statement, params);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                return generatedKeyHandler.apply(generatedKeys);
            }
        }
    }

    /**
     * Executes a batch update.
     *
     * @param connection the database connection
     * @param sql the SQL statement
     * @param batchSize the batch size
     * @param parameterSetter the function to set parameters for each batch
     * @return the number of rows affected for each batch
     * @throws SQLException if a database error occurs
     */
    protected int[] executeBatch(Connection connection, String sql, int batchSize, BatchParameterSetter parameterSetter) throws SQLException {
        try (PreparedStatement statement = prepareStatement(connection, sql)) {
            int count = 0;
            int batchCount = 0;
            int[] result = new int[0];
            
            while (parameterSetter.setParameters(statement, count)) {
                statement.addBatch();
                count++;
                
                if (count % batchSize == 0) {
                    int[] batchResult = statement.executeBatch();
                    result = appendBatchResults(result, batchResult);
                    batchCount++;
                }
            }
            
            if (count % batchSize != 0) {
                int[] batchResult = statement.executeBatch();
                result = appendBatchResults(result, batchResult);
            }
            
            return result;
        }
    }

    /**
     * Appends batch results.
     *
     * @param original the original results
     * @param toAppend the results to append
     * @return the combined results
     */
    private int[] appendBatchResults(int[] original, int[] toAppend) {
        int[] result = new int[original.length + toAppend.length];
        System.arraycopy(original, 0, result, 0, original.length);
        System.arraycopy(toAppend, 0, result, original.length, toAppend.length);
        return result;
    }

    /**
     * Processes a result set into a list of entities.
     *
     * @param resultSet the result set to process
     * @param rowMapper the function to map a row to an entity
     * @return a list of entities
     * @throws SQLException if a database error occurs
     */
    protected List<T> processResultSet(ResultSet resultSet, RowMapper<T> rowMapper) throws SQLException {
        List<T> results = new ArrayList<>();
        while (resultSet.next()) {
            results.add(rowMapper.mapRow(resultSet));
        }
        return results;
    }

    /**
     * Processes a result set into an optional entity.
     *
     * @param resultSet the result set to process
     * @param rowMapper the function to map a row to an entity
     * @return an optional entity
     * @throws SQLException if a database error occurs
     */
    protected Optional<T> processResultSetToOptional(ResultSet resultSet, RowMapper<T> rowMapper) throws SQLException {
        if (resultSet.next()) {
            return Optional.of(rowMapper.mapRow(resultSet));
        }
        return Optional.empty();
    }

    /**
     * Processes a result set into a single value.
     *
     * @param <R> the result type
     * @param resultSet the result set to process
     * @param columnIndex the column index
     * @param type the class of the result type
     * @return the value from the result set
     * @throws SQLException if a database error occurs
     */
    protected <R> R processResultSetToSingleValue(ResultSet resultSet, int columnIndex, Class<R> type) throws SQLException {
        if (resultSet.next()) {
            return type.cast(resultSet.getObject(columnIndex));
        }
        return null;
    }

    /**
     * Processes a result set into a single value.
     *
     * @param <R> the result type
     * @param resultSet the result set to process
     * @param columnName the column name
     * @param type the class of the result type
     * @return the value from the result set
     * @throws SQLException if a database error occurs
     */
    protected <R> R processResultSetToSingleValue(ResultSet resultSet, String columnName, Class<R> type) throws SQLException {
        if (resultSet.next()) {
            return type.cast(resultSet.getObject(columnName));
        }
        return null;
    }

    /**
     * Handles an exception by translating it to an appropriate PaymentDataException.
     *
     * @param message the error message
     * @param e the exception to handle
     * @return a PaymentDataException
     */
    protected PaymentDataException handleException(String message, Exception e) {
        logger.error(message, e);
        
        if (e instanceof PaymentDataException) {
            return (PaymentDataException) e;
        }
        
        if (e instanceof SQLException) {
            return ConnectionException.fromSQLException((SQLException) e);
        }
        
        return new PaymentDataException(message, e);
    }

    /**
     * Closes a resource quietly, ignoring any exceptions.
     *
     * @param autoCloseable the resource to close
     */
    protected void closeQuietly(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                logger.warn("Error closing resource: {}", e.getMessage());
            }
        }
    }

    /**
     * Functional interface for mapping a result set row to an entity.
     *
     * @param <T> the entity type
     */
    @FunctionalInterface
    protected interface RowMapper<T> {
        /**
         * Maps a result set row to an entity.
         *
         * @param resultSet the result set
         * @return the entity
         * @throws SQLException if a database error occurs
         */
        T mapRow(ResultSet resultSet) throws SQLException;
    }

    /**
     * Functional interface for setting parameters in a batch operation.
     */
    @FunctionalInterface
    protected interface BatchParameterSetter {
        /**
         * Sets parameters for a batch operation.
         *
         * @param statement the prepared statement
         * @param batchIndex the batch index
         * @return true if parameters were set, false if no more parameters
         * @throws SQLException if a database error occurs
         */
        boolean setParameters(PreparedStatement statement, int batchIndex) throws SQLException;
    }
}