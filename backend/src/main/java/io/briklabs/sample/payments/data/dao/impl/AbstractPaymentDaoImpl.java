package io.briklabs.sample.payments.data.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.dao.PaymentDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.PaymentDataException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.TransactionException.TransactionState;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;

/**
 * Base implementation class for all payment DAO interfaces that provides common
 * functionality for database operations.
 * <p>
 * This abstract class manages HikariCP connection acquisition, transaction handling,
 * prepared statement creation, resource cleanup, and exception translation. It serves
 * as the foundation for all concrete payment DAO implementations, providing standardized
 * database interaction patterns and reusable utility methods.
 * </p>
 * <p>
 * The class implements connection pooling with HikariCP as specified in the technical
 * requirements, ensuring efficient database access for payment operations.
 * </p>
 *
 * @param <T> the entity type this DAO manages
 * @param <ID> the type of the entity's primary identifier
 */
public abstract class AbstractPaymentDaoImpl<T, ID> implements PaymentDAO<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPaymentDaoImpl.class);
    
    /**
     * Thread-local storage for the current database connection.
     * This allows transaction management across multiple DAO operations.
     */
    private static final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();
    
    /**
     * Thread-local flag indicating if the current connection is in a transaction.
     */
    private static final ThreadLocal<Boolean> inTransaction = ThreadLocal.withInitial(() -> false);
    
    /**
     * The HikariCP data source for connection pooling.
     */
    private static final AtomicReference<HikariDataSource> dataSourceRef = new AtomicReference<>();
    
    /**
     * The database configuration.
     */
    private final DatabaseConfig databaseConfig;
    
    /**
     * The configuration source.
     */
    private final ConfigSource configSource;
    
    /**
     * The name of the table this DAO manages.
     */
    private final String tableName;

    /**
     * Creates a new AbstractPaymentDaoImpl with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     * @param tableName the name of the table this DAO manages
     */
    protected AbstractPaymentDaoImpl(DatabaseConfig databaseConfig, ConfigSource configSource, String tableName) {
        this.databaseConfig = databaseConfig;
        this.configSource = configSource;
        this.tableName = tableName;
        
        // Initialize the data source if it hasn't been initialized yet
        initializeDataSource();
    }

    /**
     * Initializes the HikariCP data source with configuration from the database config.
     * This method is thread-safe and ensures the data source is initialized only once.
     */
    private void initializeDataSource() {
        if (dataSourceRef.get() == null) {
            synchronized (dataSourceRef) {
                if (dataSourceRef.get() == null) {
                    try {
                        HikariConfig config = createHikariConfig();
                        HikariDataSource dataSource = new HikariDataSource(config);
                        dataSourceRef.set(dataSource);
                        logger.info("Initialized HikariCP connection pool for payment database");
                    } catch (HikariPool.PoolInitializationException e) {
                        logger.error("Failed to initialize HikariCP connection pool", e);
                        throw ConnectionException.fromHikariException(e);
                    } catch (Exception e) {
                        logger.error("Unexpected error initializing connection pool", e);
                        throw new ConnectionException("Failed to initialize connection pool: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Creates a HikariCP configuration with settings from the database config.
     *
     * @return the HikariCP configuration
     */
    private HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();
        
        // Set basic connection properties
        config.setJdbcUrl(databaseConfig.getDatabaseURL());
        config.setUsername(databaseConfig.getDatabaseUsername());
        config.setPassword(databaseConfig.getDatabasePassword());
        config.setSchema(databaseConfig.getDatabaseSchema());
        
        // Get connection pool configuration from database config if available
        Optional<Map<String, Object>> poolConfigOpt = databaseConfig.getConnectionPoolConfig();
        
        if (poolConfigOpt.isPresent()) {
            Map<String, Object> poolConfig = poolConfigOpt.get();
            
            // Apply pool configuration from database config
            for (Map.Entry<String, Object> entry : poolConfig.entrySet()) {
                setHikariProperty(config, entry.getKey(), entry.getValue());
            }
        } else {
            // Use payment-specific defaults from configuration source
            Map<String, Object> paymentPoolConfig = configSource.getPaymentConnectionPoolConfig();
            
            // Apply payment-specific pool configuration
            for (Map.Entry<String, Object> entry : paymentPoolConfig.entrySet()) {
                setHikariProperty(config, entry.getKey(), entry.getValue());
            }
        }
        
        return config;
    }

    /**
     * Sets a HikariCP configuration property with the appropriate type conversion.
     *
     * @param config the HikariCP configuration
     * @param key the property key
     * @param value the property value
     */
    private void setHikariProperty(HikariConfig config, String key, Object value) {
        if (value == null) {
            return;
        }
        
        try {
            if (value instanceof Integer) {
                // Handle integer properties
                switch (key) {
                    case "maximumPoolSize":
                        config.setMaximumPoolSize((Integer) value);
                        break;
                    case "minimumIdle":
                        config.setMinimumIdle((Integer) value);
                        break;
                    case "connectionTimeout":
                        config.setConnectionTimeout((Integer) value);
                        break;
                    case "idleTimeout":
                        config.setIdleTimeout((Integer) value);
                        break;
                    case "maxLifetime":
                        config.setMaxLifetime((Integer) value);
                        break;
                    case "leakDetectionThreshold":
                        config.setLeakDetectionThreshold((Integer) value);
                        break;
                    default:
                        config.addDataSourceProperty(key, value);
                }
            } else if (value instanceof Long) {
                // Handle long properties
                switch (key) {
                    case "connectionTimeout":
                        config.setConnectionTimeout((Long) value);
                        break;
                    case "idleTimeout":
                        config.setIdleTimeout((Long) value);
                        break;
                    case "maxLifetime":
                        config.setMaxLifetime((Long) value);
                        break;
                    case "leakDetectionThreshold":
                        config.setLeakDetectionThreshold((Long) value);
                        break;
                    default:
                        config.addDataSourceProperty(key, value);
                }
            } else if (value instanceof Boolean) {
                // Handle boolean properties
                switch (key) {
                    case "autoCommit":
                        config.setAutoCommit((Boolean) value);
                        break;
                    case "registerMbeans":
                        config.setRegisterMbeans((Boolean) value);
                        break;
                    default:
                        config.addDataSourceProperty(key, value);
                }
            } else if (value instanceof String) {
                // Handle string properties
                switch (key) {
                    case "poolName":
                        config.setPoolName((String) value);
                        break;
                    case "connectionTestQuery":
                        config.setConnectionTestQuery((String) value);
                        break;
                    default:
                        config.addDataSourceProperty(key, value);
                }
            } else {
                // Handle other property types
                config.addDataSourceProperty(key, value);
            }
        } catch (Exception e) {
            logger.warn("Failed to set HikariCP property {}: {}", key, e.getMessage());
        }
    }

    /**
     * Gets a database connection from the connection pool.
     * If a transaction is active, returns the current connection.
     *
     * @return a database connection
     * @throws ConnectionException if a connection cannot be obtained
     */
    protected Connection getConnection() throws ConnectionException {
        // Check if we're in a transaction and have an active connection
        Connection conn = currentConnection.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    return conn;
                }
            } catch (SQLException e) {
                logger.warn("Error checking connection state", e);
                // Connection is invalid, clear it and get a new one
                currentConnection.remove();
                inTransaction.set(false);
            }
        }
        
        // Get a new connection from the pool
        try {
            HikariDataSource dataSource = dataSourceRef.get();
            if (dataSource == null) {
                initializeDataSource();
                dataSource = dataSourceRef.get();
            }
            
            conn = dataSource.getConnection();
            
            // If not in a transaction, set auto-commit to true
            if (!Boolean.TRUE.equals(inTransaction.get())) {
                conn.setAutoCommit(true);
            }
            
            return conn;
        } catch (SQLException e) {
            throw ConnectionException.fromSQLException(e);
        } catch (HikariPool.PoolInitializationException e) {
            throw ConnectionException.fromHikariException(e);
        } catch (Exception e) {
            throw new ConnectionException("Failed to acquire database connection: " + e.getMessage(), 
                    ConnectionException.CONN_ACQUISITION_FAILED, e);
        }
    }

    /**
     * Begins a database transaction.
     * Sets auto-commit to false and stores the connection in thread-local storage.
     *
     * @throws ConnectionException if a connection cannot be established
     * @throws TransactionException if the transaction cannot be started
     */
    @Override
    public void beginTransaction() throws ConnectionException, TransactionException {
        // Check if we're already in a transaction
        if (Boolean.TRUE.equals(inTransaction.get())) {
            throw TransactionException.beginFailed(
                    "Transaction already started", null, "beginTransaction");
        }
        
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            
            // Store the connection and transaction state
            currentConnection.set(conn);
            inTransaction.set(true);
            
            logger.debug("Transaction started");
        } catch (SQLException e) {
            closeQuietly(conn);
            throw TransactionException.beginFailed(
                    "Failed to start transaction: " + e.getMessage(), e, "beginTransaction");
        } catch (ConnectionException e) {
            closeQuietly(conn);
            throw e;
        } catch (Exception e) {
            closeQuietly(conn);
            throw TransactionException.beginFailed(
                    "Unexpected error starting transaction: " + e.getMessage(), e, "beginTransaction");
        }
    }

    /**
     * Commits the current database transaction.
     *
     * @throws TransactionException if the transaction cannot be committed
     */
    @Override
    public void commitTransaction() throws TransactionException {
        // Check if we're in a transaction
        if (!Boolean.TRUE.equals(inTransaction.get())) {
            throw TransactionException.commitFailed(
                    "No active transaction to commit", null, "commitTransaction");
        }
        
        Connection conn = currentConnection.get();
        if (conn == null) {
            throw TransactionException.commitFailed(
                    "No active connection for transaction", null, "commitTransaction");
        }
        
        try {
            conn.commit();
            logger.debug("Transaction committed");
        } catch (SQLException e) {
            throw TransactionException.commitFailed(
                    "Failed to commit transaction: " + e.getMessage(), e, "commitTransaction");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.warn("Failed to reset auto-commit after transaction", e);
            }
            
            closeQuietly(conn);
            
            // Clear thread-local storage
            currentConnection.remove();
            inTransaction.set(false);
        }
    }

    /**
     * Rolls back the current database transaction.
     *
     * @throws TransactionException if the transaction cannot be rolled back
     */
    @Override
    public void rollbackTransaction() throws TransactionException {
        // Check if we're in a transaction
        if (!Boolean.TRUE.equals(inTransaction.get())) {
            logger.debug("No active transaction to roll back");
            return;
        }
        
        Connection conn = currentConnection.get();
        if (conn == null) {
            logger.debug("No active connection for transaction rollback");
            inTransaction.set(false);
            return;
        }
        
        try {
            conn.rollback();
            logger.debug("Transaction rolled back");
        } catch (SQLException e) {
            throw TransactionException.rollbackFailed(
                    "Failed to roll back transaction: " + e.getMessage(), e, "rollbackTransaction");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.warn("Failed to reset auto-commit after rollback", e);
            }
            
            closeQuietly(conn);
            
            // Clear thread-local storage
            currentConnection.remove();
            inTransaction.set(false);
        }
    }

    /**
     * Creates a prepared statement from the given SQL and parameters.
     *
     * @param conn the database connection
     * @param sql the SQL statement
     * @param params the parameters to bind
     * @return the prepared statement
     * @throws SQLException if a database access error occurs
     */
    protected PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                setParameter(stmt, i + 1, params[i]);
            }
        }
        
        return stmt;
    }

    /**
     * Sets a parameter in a prepared statement with the appropriate type.
     *
     * @param stmt the prepared statement
     * @param index the parameter index (1-based)
     * @param value the parameter value
     * @throws SQLException if a database access error occurs
     */
    protected void setParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.NULL);
        } else if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (Double) value);
        } else if (value instanceof BigDecimal) {
            stmt.setBigDecimal(index, (BigDecimal) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(index, (Boolean) value);
        } else if (value instanceof LocalDateTime) {
            stmt.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else if (value instanceof Timestamp) {
            stmt.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof UUID) {
            stmt.setObject(index, value);
        } else {
            stmt.setObject(index, value);
        }
    }

    /**
     * Executes a query and processes the results with a result handler.
     *
     * @param <R> the result type
     * @param sql the SQL query
     * @param resultHandler the handler for processing results
     * @param params the query parameters
     * @return the result from the handler
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    protected <R> R executeQuery(String sql, ResultHandler<R> resultHandler, Object... params) 
            throws ConnectionException, QueryExecutionException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = prepareStatement(conn, sql, params);
            rs = stmt.executeQuery();
            
            return resultHandler.handle(rs);
        } catch (SQLException e) {
            throw QueryExecutionException.builder()
                    .message("Failed to execute query: " + e.getMessage())
                    .cause(e)
                    .query(sql)
                    .operationType("SELECT")
                    .affectedTables(tableName)
                    .build();
        } finally {
            closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Executes an update statement and returns the number of affected rows.
     *
     * @param sql the SQL update statement
     * @param params the statement parameters
     * @return the number of affected rows
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    protected int executeUpdate(String sql, Object... params) 
            throws ConnectionException, QueryExecutionException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            stmt = prepareStatement(conn, sql, params);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw QueryExecutionException.builder()
                    .message("Failed to execute update: " + e.getMessage())
                    .cause(e)
                    .query(sql)
                    .operationType("UPDATE")
                    .affectedTables(tableName)
                    .build();
        } finally {
            closeQuietly(stmt, conn);
        }
    }

    /**
     * Executes a query using a PaymentQueryBuilder.
     *
     * @param <R> the result type
     * @param queryBuilder the query builder
     * @param resultHandler the handler for processing results
     * @return the result from the handler
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    protected <R> R executeQuery(PaymentQueryBuilder queryBuilder, ResultHandler<R> resultHandler) 
            throws ConnectionException, QueryExecutionException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = queryBuilder.buildPreparedStatement(conn);
            rs = stmt.executeQuery();
            
            return resultHandler.handle(rs);
        } catch (SQLException e) {
            throw QueryExecutionException.builder()
                    .message("Failed to execute query: " + e.getMessage())
                    .cause(e)
                    .query(queryBuilder.getQueryString())
                    .operationType("SELECT")
                    .affectedTables(tableName)
                    .build();
        } finally {
            closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Executes an update using a PaymentQueryBuilder.
     *
     * @param queryBuilder the query builder
     * @return the number of affected rows
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    protected int executeUpdate(PaymentQueryBuilder queryBuilder) 
            throws ConnectionException, QueryExecutionException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            stmt = queryBuilder.buildPreparedStatement(conn);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw QueryExecutionException.builder()
                    .message("Failed to execute update: " + e.getMessage())
                    .cause(e)
                    .query(queryBuilder.getQueryString())
                    .operationType("UPDATE")
                    .affectedTables(tableName)
                    .build();
        } finally {
            closeQuietly(stmt, conn);
        }
    }

    /**
     * Executes a batch update using a prepared statement.
     *
     * @param sql the SQL statement
     * @param batchHandler the handler for preparing the batch
     * @return an array of update counts
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    protected int[] executeBatch(String sql, BatchHandler batchHandler) 
            throws ConnectionException, QueryExecutionException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            
            batchHandler.prepareBatch(stmt);
            
            return stmt.executeBatch();
        } catch (SQLException e) {
            throw QueryExecutionException.builder()
                    .message("Failed to execute batch update: " + e.getMessage())
                    .cause(e)
                    .query(sql)
                    .operationType("BATCH")
                    .affectedTables(tableName)
                    .build();
        } finally {
            closeQuietly(stmt, conn);
        }
    }

    /**
     * Quietly closes database resources, ignoring any exceptions.
     *
     * @param resources the resources to close
     */
    protected void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    // Don't close the connection if it's part of a transaction
                    if (resource instanceof Connection) {
                        Connection conn = (Connection) resource;
                        if (conn == currentConnection.get() && Boolean.TRUE.equals(inTransaction.get())) {
                            continue;
                        }
                    }
                    
                    resource.close();
                } catch (Exception e) {
                    logger.warn("Error closing resource: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Validates an entity before database operations.
     * This method should be implemented by subclasses to perform entity-specific validation.
     *
     * @param entity the entity to validate
     * @throws ValidationException if the entity fails validation
     */
    protected abstract void validateEntity(T entity) throws ValidationException;

    /**
     * Maps a ResultSet row to an entity.
     * This method should be implemented by subclasses to perform entity-specific mapping.
     *
     * @param rs the result set
     * @return the mapped entity
     * @throws SQLException if a database access error occurs
     */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /**
     * Maps a ResultSet to a list of entities.
     *
     * @param rs the result set
     * @return a list of entities
     * @throws SQLException if a database access error occurs
     */
    protected List<T> mapRows(ResultSet rs) throws SQLException {
        List<T> results = new ArrayList<>();
        
        while (rs.next()) {
            results.add(mapRow(rs));
        }
        
        return results;
    }

    /**
     * Builds a query for the entity with the given ID.
     *
     * @param id the entity ID
     * @return the SQL query
     */
    protected abstract String buildFindByIdQuery(ID id);

    /**
     * Builds a query for the given filter parameters.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    protected abstract PaymentQueryBuilder buildFilterQuery(PaymentFilterParams filterParams);

    /**
     * Builds a count query for the given filter parameters.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    protected abstract PaymentQueryBuilder buildCountQuery(PaymentFilterParams filterParams);

    /**
     * Builds an insert query for the given entity.
     *
     * @param entity the entity to insert
     * @return the SQL query
     */
    protected abstract String buildInsertQuery(T entity);

    /**
     * Builds an update query for the given entity.
     *
     * @param entity the entity to update
     * @return the SQL query
     */
    protected abstract String buildUpdateQuery(T entity);

    /**
     * Builds a delete query for the given ID.
     *
     * @param id the entity ID
     * @return the SQL query
     */
    protected abstract String buildDeleteQuery(ID id);

    /**
     * Gets the parameters for an insert query.
     *
     * @param entity the entity to insert
     * @return the query parameters
     */
    protected abstract Object[] getInsertParameters(T entity);

    /**
     * Gets the parameters for an update query.
     *
     * @param entity the entity to update
     * @return the query parameters
     */
    protected abstract Object[] getUpdateParameters(T entity);

    /**
     * Gets the parameters for a delete query.
     *
     * @param id the entity ID
     * @return the query parameters
     */
    protected abstract Object[] getDeleteParameters(ID id);

    /**
     * Creates a new entity in the database.
     *
     * @param entity the entity to create
     * @return the created entity with any database-generated values
     * @throws ValidationException if the entity fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public T create(T entity) throws ValidationException, ConnectionException, 
                                   QueryExecutionException, TransactionException {
        validateEntity(entity);
        
        String sql = buildInsertQuery(entity);
        Object[] params = getInsertParameters(entity);
        
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            executeUpdate(sql, params);
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return entity;
        } catch (ConnectionException | QueryExecutionException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw new QueryExecutionException("Failed to create entity: " + e.getMessage(), e, 
                    sql, "INSERT", new String[]{tableName});
        }
    }

    /**
     * Retrieves an entity by its primary identifier.
     *
     * @param id the entity identifier
     * @return an Optional containing the found entity, or empty if not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Optional<T> findById(ID id) throws ConnectionException, QueryExecutionException {
        String sql = buildFindByIdQuery(id);
        
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            } else {
                return Optional.empty();
            }
        });
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
    @Override
    public T update(T entity) throws ValidationException, ResourceNotFoundException, 
                                   ConnectionException, QueryExecutionException, TransactionException {
        validateEntity(entity);
        
        String sql = buildUpdateQuery(entity);
        Object[] params = getUpdateParameters(entity);
        
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            int rowsAffected = executeUpdate(sql, params);
            
            if (rowsAffected == 0) {
                if (localTransaction) {
                    rollbackTransaction();
                }
                throw new ResourceNotFoundException("Entity not found for update");
            }
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return entity;
        } catch (ConnectionException | QueryExecutionException | ResourceNotFoundException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw new QueryExecutionException("Failed to update entity: " + e.getMessage(), e, 
                    sql, "UPDATE", new String[]{tableName});
        }
    }

    /**
     * Deletes an entity by its primary identifier.
     *
     * @param id the entity identifier
     * @return true if the entity was deleted, false if it did not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public boolean delete(ID id) throws ConnectionException, QueryExecutionException, TransactionException {
        String sql = buildDeleteQuery(id);
        Object[] params = getDeleteParameters(id);
        
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            int rowsAffected = executeUpdate(sql, params);
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return rowsAffected > 0;
        } catch (ConnectionException | QueryExecutionException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw new QueryExecutionException("Failed to delete entity: " + e.getMessage(), e, 
                    sql, "DELETE", new String[]{tableName});
        }
    }

    /**
     * Queries for entities based on the provided filter parameters.
     *
     * @param filterParams the parameters to filter the query results
     * @return a list of entities matching the filter criteria
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<T> query(PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException {
        PaymentQueryBuilder queryBuilder = buildFilterQuery(filterParams);
        
        return executeQuery(queryBuilder, this::mapRows);
    }

    /**
     * Counts the number of entities matching the provided filter parameters.
     *
     * @param filterParams the parameters to filter the count
     * @return the count of matching entities
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public long count(PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException {
        PaymentQueryBuilder queryBuilder = buildCountQuery(filterParams);
        
        return executeQuery(queryBuilder, rs -> {
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return 0L;
            }
        });
    }

    /**
     * Checks if an entity with the given ID exists.
     *
     * @param id the entity identifier
     * @return true if an entity with the given ID exists, false otherwise
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public boolean exists(ID id) throws ConnectionException, QueryExecutionException {
        return findById(id).isPresent();
    }

    /**
     * Creates multiple entities in a batch operation.
     *
     * @param entities the list of entities to create
     * @return the list of created entities with any database-generated values
     * @throws ValidationException if any entity fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public List<T> batchCreate(List<T> entities) throws ValidationException, ConnectionException,
                                                     QueryExecutionException, TransactionException {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Validate all entities first
        for (T entity : entities) {
            validateEntity(entity);
        }
        
        // Get the SQL for the first entity (assuming all entities are of the same type)
        String sql = buildInsertQuery(entities.get(0));
        
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            executeBatch(sql, stmt -> {
                for (T entity : entities) {
                    Object[] params = getInsertParameters(entity);
                    for (int i = 0; i < params.length; i++) {
                        setParameter(stmt, i + 1, params[i]);
                    }
                    stmt.addBatch();
                }
            });
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return entities;
        } catch (ConnectionException | QueryExecutionException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw new QueryExecutionException("Failed to batch create entities: " + e.getMessage(), e, 
                    sql, "BATCH INSERT", new String[]{tableName});
        }
    }

    /**
     * Updates multiple entities in a batch operation.
     *
     * @param entities the list of entities to update
     * @return the list of updated entities
     * @throws ValidationException if any entity fails validation
     * @throws ResourceNotFoundException if any entity to update is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public List<T> batchUpdate(List<T> entities) throws ValidationException, ResourceNotFoundException,
                                                     ConnectionException, QueryExecutionException, TransactionException {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Validate all entities first
        for (T entity : entities) {
            validateEntity(entity);
        }
        
        // Get the SQL for the first entity (assuming all entities are of the same type)
        String sql = buildUpdateQuery(entities.get(0));
        
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            int[] results = executeBatch(sql, stmt -> {
                for (T entity : entities) {
                    Object[] params = getUpdateParameters(entity);
                    for (int i = 0; i < params.length; i++) {
                        setParameter(stmt, i + 1, params[i]);
                    }
                    stmt.addBatch();
                }
            });
            
            // Check if any updates failed (no rows affected)
            for (int i = 0; i < results.length; i++) {
                if (results[i] == 0) {
                    if (localTransaction) {
                        rollbackTransaction();
                    }
                    throw new ResourceNotFoundException("Entity at index " + i + " not found for update");
                }
            }
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return entities;
        } catch (ConnectionException | QueryExecutionException | ResourceNotFoundException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw new QueryExecutionException("Failed to batch update entities: " + e.getMessage(), e, 
                    sql, "BATCH UPDATE", new String[]{tableName});
        }
    }

    /**
     * Deletes multiple entities in a batch operation.
     *
     * @param ids the list of entity identifiers to delete
     * @return the number of entities deleted
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public int batchDelete(List<ID> ids) throws ConnectionException, QueryExecutionException, TransactionException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        // Get the SQL for the first ID (assuming all IDs are of the same type)
        String sql = buildDeleteQuery(ids.get(0));
        
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            int[] results = executeBatch(sql, stmt -> {
                for (ID id : ids) {
                    Object[] params = getDeleteParameters(id);
                    for (int i = 0; i < params.length; i++) {
                        setParameter(stmt, i + 1, params[i]);
                    }
                    stmt.addBatch();
                }
            });
            
            if (localTransaction) {
                commitTransaction();
            }
            
            // Count the total number of deleted rows
            int totalDeleted = 0;
            for (int result : results) {
                if (result > 0) {
                    totalDeleted += result;
                }
            }
            
            return totalDeleted;
        } catch (ConnectionException | QueryExecutionException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw new QueryExecutionException("Failed to batch delete entities: " + e.getMessage(), e, 
                    sql, "BATCH DELETE", new String[]{tableName});
        }
    }

    /**
     * Executes a database operation with transaction management.
     *
     * @param <R> the result type
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws PaymentDataException if the operation fails
     */
    protected <R> R executeWithTransaction(DatabaseOperation<R> operation) throws PaymentDataException {
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            R result = operation.execute();
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return result;
        } catch (PaymentDataException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new PaymentDataException("Database operation failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Functional interface for handling ResultSet data.
     *
     * @param <R> the result type
     */
    @FunctionalInterface
    protected interface ResultHandler<R> {
        /**
         * Handles a ResultSet and produces a result.
         *
         * @param rs the result set
         * @return the result
         * @throws SQLException if a database access error occurs
         */
        R handle(ResultSet rs) throws SQLException;
    }

    /**
     * Functional interface for preparing a batch statement.
     */
    @FunctionalInterface
    protected interface BatchHandler {
        /**
         * Prepares a batch statement by adding parameters and calling addBatch().
         *
         * @param stmt the prepared statement
         * @throws SQLException if a database access error occurs
         */
        void prepareBatch(PreparedStatement stmt) throws SQLException;
    }

    /**
     * Functional interface for database operations with transaction management.
     *
     * @param <R> the result type
     */
    @FunctionalInterface
    protected interface DatabaseOperation<R> {
        /**
         * Executes a database operation.
         *
         * @return the result of the operation
         * @throws Exception if the operation fails
         */
        R execute() throws Exception;
    }

    /**
     * Closes the data source when the application shuts down.
     * This method should be called during application shutdown to release resources.
     */
    public static void shutdown() {
        HikariDataSource dataSource = dataSourceRef.get();
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Shutting down HikariCP connection pool for payment database");
            dataSource.close();
        }
    }
}