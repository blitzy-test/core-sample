package io.briklabs.sample.payments.data;

import com.zaxxer.hikari.HikariDataSource;
import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.config.PaymentDatabaseConfig;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages database connections for payment transaction processing, providing centralized
 * access to the HikariCP connection pool. This class handles connection acquisition, release,
 * and transaction management with proper error handling and resource cleanup.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Connection acquisition from HikariCP with proper timeout handling</li>
 *   <li>Transaction management support with explicit commit/rollback</li>
 *   <li>Connection release patterns with proper resource cleanup</li>
 *   <li>Connection state validation and health checking</li>
 *   <li>Monitoring hooks for connection usage metrics</li>
 * </ul>
 * </p>
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    /**
     * Thread-local storage for the current database connection.
     * This allows transaction management across multiple operations.
     */
    private static final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();
    
    /**
     * Thread-local flag indicating if the current connection is in a transaction.
     */
    private static final ThreadLocal<Boolean> inTransaction = ThreadLocal.withInitial(() -> false);
    
    /**
     * Thread-local storage for transaction isolation level.
     */
    private static final ThreadLocal<Integer> transactionIsolationLevel = new ThreadLocal<>();
    
    /**
     * Metrics for connection usage monitoring.
     */
    private static final AtomicLong connectionsAcquired = new AtomicLong(0);
    private static final AtomicLong connectionsReleased = new AtomicLong(0);
    private static final AtomicLong transactionsStarted = new AtomicLong(0);
    private static final AtomicLong transactionsCommitted = new AtomicLong(0);
    private static final AtomicLong transactionsRolledBack = new AtomicLong(0);
    private static final ConcurrentHashMap<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    
    /**
     * The HikariCP data source for connection pooling.
     */
    private final HikariDataSource dataSource;
    
    /**
     * Creates a new ConnectionManager with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     */
    public ConnectionManager(DatabaseConfig databaseConfig, ConfigSource configSource) {
        logger.info("Initializing ConnectionManager for payment database");
        this.dataSource = HikariCPConfig.getDataSource(databaseConfig, configSource);
        logger.info("ConnectionManager initialized successfully");
    }
    
    /**
     * Creates a new ConnectionManager with a payment-specific database configuration.
     *
     * @param paymentDbConfig the payment database configuration
     * @param configSource the configuration source
     */
    public ConnectionManager(PaymentDatabaseConfig paymentDbConfig, ConfigSource configSource) {
        logger.info("Initializing ConnectionManager with payment-specific database configuration");
        this.dataSource = HikariCPConfig.getDataSource(paymentDbConfig, configSource);
        logger.info("ConnectionManager initialized successfully");
    }
    
    /**
     * Gets a database connection from the connection pool.
     * If a transaction is active, returns the current connection.
     *
     * @return a database connection
     * @throws ConnectionException if a connection cannot be obtained
     */
    public Connection getConnection() throws ConnectionException {
        // Check if we're in a transaction and have an active connection
        Connection conn = currentConnection.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    // Track operation for metrics
                    incrementOperationCount("getConnection.reuse");
                    return conn;
                }
            } catch (SQLException e) {
                logger.warn("Error checking connection state: {}", e.getMessage());
                // Connection is invalid, clear it and get a new one
                currentConnection.remove();
                inTransaction.set(false);
                transactionIsolationLevel.remove();
            }
        }
        
        // Get a new connection from the pool
        try {
            long startTime = System.currentTimeMillis();
            conn = dataSource.getConnection();
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            // Track metrics
            connectionsAcquired.incrementAndGet();
            incrementOperationCount("getConnection.new");
            
            // Log acquisition time if it took longer than expected
            if (elapsedTime > 100) {
                logger.warn("Connection acquisition took {} ms", elapsedTime);
            }
            
            // If not in a transaction, set auto-commit to true
            if (!Boolean.TRUE.equals(inTransaction.get())) {
                conn.setAutoCommit(true);
            }
            
            return conn;
        } catch (SQLException e) {
            incrementOperationCount("getConnection.error");
            throw ConnectionException.fromSQLException(e);
        } catch (Exception e) {
            incrementOperationCount("getConnection.error");
            throw new ConnectionException("Failed to acquire database connection: " + e.getMessage(), 
                    ConnectionException.CONN_ACQUISITION_FAILED, e);
        }
    }
    
    /**
     * Begins a database transaction with the default isolation level.
     *
     * @throws ConnectionException if a connection cannot be established
     * @throws TransactionException if the transaction cannot be started
     */
    public void beginTransaction() throws ConnectionException, TransactionException {
        beginTransaction(Connection.TRANSACTION_READ_COMMITTED);
    }
    
    /**
     * Begins a database transaction with the specified isolation level.
     *
     * @param isolationLevel the transaction isolation level
     * @throws ConnectionException if a connection cannot be established
     * @throws TransactionException if the transaction cannot be started
     */
    public void beginTransaction(int isolationLevel) throws ConnectionException, TransactionException {
        // Check if we're already in a transaction
        if (Boolean.TRUE.equals(inTransaction.get())) {
            incrementOperationCount("beginTransaction.alreadyStarted");
            throw TransactionException.beginFailed(
                    "Transaction already started", null, "beginTransaction");
        }
        
        Connection conn = null;
        try {
            conn = getConnection();
            
            // Set isolation level
            int originalIsolation = conn.getTransactionIsolation();
            if (originalIsolation != isolationLevel) {
                conn.setTransactionIsolation(isolationLevel);
            }
            
            // Disable auto-commit
            conn.setAutoCommit(false);
            
            // Store the connection and transaction state
            currentConnection.set(conn);
            inTransaction.set(true);
            transactionIsolationLevel.set(isolationLevel);
            
            // Track metrics
            transactionsStarted.incrementAndGet();
            incrementOperationCount("beginTransaction");
            
            logger.debug("Transaction started with isolation level: {}", getIsolationLevelName(isolationLevel));
        } catch (SQLException e) {
            incrementOperationCount("beginTransaction.error");
            closeQuietly(conn);
            throw TransactionException.beginFailed(
                    "Failed to start transaction: " + e.getMessage(), e, "beginTransaction");
        } catch (ConnectionException e) {
            incrementOperationCount("beginTransaction.error");
            closeQuietly(conn);
            throw e;
        } catch (Exception e) {
            incrementOperationCount("beginTransaction.error");
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
    public void commitTransaction() throws TransactionException {
        // Check if we're in a transaction
        if (!Boolean.TRUE.equals(inTransaction.get())) {
            incrementOperationCount("commitTransaction.noTransaction");
            throw TransactionException.commitFailed(
                    "No active transaction to commit", null, "commitTransaction");
        }
        
        Connection conn = currentConnection.get();
        if (conn == null) {
            incrementOperationCount("commitTransaction.noConnection");
            throw TransactionException.commitFailed(
                    "No active connection for transaction", null, "commitTransaction");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            conn.commit();
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            // Track metrics
            transactionsCommitted.incrementAndGet();
            incrementOperationCount("commitTransaction");
            
            // Log commit time if it took longer than expected
            if (elapsedTime > 100) {
                logger.warn("Transaction commit took {} ms", elapsedTime);
            }
            
            logger.debug("Transaction committed successfully");
        } catch (SQLException e) {
            incrementOperationCount("commitTransaction.error");
            throw TransactionException.commitFailed(
                    "Failed to commit transaction: " + e.getMessage(), e, "commitTransaction");
        } finally {
            try {
                // Reset auto-commit
                conn.setAutoCommit(true);
                
                // Reset isolation level if it was changed
                Integer isolationLevel = transactionIsolationLevel.get();
                if (isolationLevel != null && isolationLevel != Connection.TRANSACTION_READ_COMMITTED) {
                    try {
                        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    } catch (SQLException e) {
                        logger.warn("Failed to reset transaction isolation level", e);
                    }
                }
            } catch (SQLException e) {
                logger.warn("Failed to reset auto-commit after transaction", e);
            }
            
            releaseConnection(conn);
            
            // Clear thread-local storage
            currentConnection.remove();
            inTransaction.set(false);
            transactionIsolationLevel.remove();
        }
    }
    
    /**
     * Rolls back the current database transaction.
     *
     * @throws TransactionException if the transaction cannot be rolled back
     */
    public void rollbackTransaction() throws TransactionException {
        // Check if we're in a transaction
        if (!Boolean.TRUE.equals(inTransaction.get())) {
            logger.debug("No active transaction to roll back");
            incrementOperationCount("rollbackTransaction.noTransaction");
            return;
        }
        
        Connection conn = currentConnection.get();
        if (conn == null) {
            logger.debug("No active connection for transaction rollback");
            incrementOperationCount("rollbackTransaction.noConnection");
            inTransaction.set(false);
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            conn.rollback();
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            // Track metrics
            transactionsRolledBack.incrementAndGet();
            incrementOperationCount("rollbackTransaction");
            
            // Log rollback time if it took longer than expected
            if (elapsedTime > 100) {
                logger.warn("Transaction rollback took {} ms", elapsedTime);
            }
            
            logger.debug("Transaction rolled back successfully");
        } catch (SQLException e) {
            incrementOperationCount("rollbackTransaction.error");
            throw TransactionException.rollbackFailed(
                    "Failed to roll back transaction: " + e.getMessage(), e, "rollbackTransaction");
        } finally {
            try {
                // Reset auto-commit
                conn.setAutoCommit(true);
                
                // Reset isolation level if it was changed
                Integer isolationLevel = transactionIsolationLevel.get();
                if (isolationLevel != null && isolationLevel != Connection.TRANSACTION_READ_COMMITTED) {
                    try {
                        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    } catch (SQLException e) {
                        logger.warn("Failed to reset transaction isolation level", e);
                    }
                }
            } catch (SQLException e) {
                logger.warn("Failed to reset auto-commit after rollback", e);
            }
            
            releaseConnection(conn);
            
            // Clear thread-local storage
            currentConnection.remove();
            inTransaction.set(false);
            transactionIsolationLevel.remove();
        }
    }
    
    /**
     * Releases a database connection back to the pool.
     * If the connection is part of a transaction, it is not released.
     *
     * @param conn the connection to release
     */
    public void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        
        // Don't close the connection if it's part of a transaction
        if (conn == currentConnection.get() && Boolean.TRUE.equals(inTransaction.get())) {
            incrementOperationCount("releaseConnection.inTransaction");
            return;
        }
        
        closeQuietly(conn);
        connectionsReleased.incrementAndGet();
        incrementOperationCount("releaseConnection");
    }
    
    /**
     * Quietly closes a database connection, ignoring any exceptions.
     *
     * @param conn the connection to close
     */
    public void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.warn("Error closing connection: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Checks if the current thread is in a transaction.
     *
     * @return true if in a transaction, false otherwise
     */
    public boolean isInTransaction() {
        return Boolean.TRUE.equals(inTransaction.get());
    }
    
    /**
     * Gets the current transaction isolation level.
     *
     * @return the current transaction isolation level, or null if not in a transaction
     */
    public Integer getCurrentTransactionIsolationLevel() {
        return transactionIsolationLevel.get();
    }
    
    /**
     * Validates a connection to ensure it is still valid.
     *
     * @param conn the connection to validate
     * @param timeoutSeconds the timeout in seconds
     * @return true if the connection is valid, false otherwise
     */
    public boolean validateConnection(Connection conn, int timeoutSeconds) {
        if (conn == null) {
            return false;
        }
        
        try {
            return conn.isValid(timeoutSeconds);
        } catch (SQLException e) {
            logger.warn("Error validating connection: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the connection pool metrics.
     *
     * @return a map of connection pool metrics
     */
    public Map<String, Object> getConnectionPoolMetrics() {
        return HikariCPConfig.getPoolMetrics();
    }
    
    /**
     * Gets the connection manager metrics.
     *
     * @return a map of connection manager metrics
     */
    public Map<String, Object> getConnectionManagerMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("connectionsAcquired", connectionsAcquired.get());
        metrics.put("connectionsReleased", connectionsReleased.get());
        metrics.put("transactionsStarted", transactionsStarted.get());
        metrics.put("transactionsCommitted", transactionsCommitted.get());
        metrics.put("transactionsRolledBack", transactionsRolledBack.get());
        metrics.put("activeTransactions", getActiveTransactionCount());
        metrics.put("operationCounts", new HashMap<>(operationCounts));
        
        return metrics;
    }
    
    /**
     * Gets the number of active transactions.
     *
     * @return the number of active transactions
     */
    private long getActiveTransactionCount() {
        return transactionsStarted.get() - transactionsCommitted.get() - transactionsRolledBack.get();
    }
    
    /**
     * Increments the count for a specific operation.
     *
     * @param operation the operation name
     */
    private void incrementOperationCount(String operation) {
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Gets a human-readable name for a transaction isolation level.
     *
     * @param isolationLevel the transaction isolation level
     * @return the isolation level name
     */
    private String getIsolationLevelName(int isolationLevel) {
        switch (isolationLevel) {
            case Connection.TRANSACTION_NONE:
                return "NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED:
                return "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ:
                return "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE:
                return "SERIALIZABLE";
            default:
                return "UNKNOWN (" + isolationLevel + ")";
        }
    }
    
    /**
     * Checks if the connection pool is healthy.
     *
     * @return true if the connection pool is healthy, false otherwise
     */
    public boolean isHealthy() {
        return HikariCPConfig.isHealthy();
    }
    
    /**
     * Performs a health check by acquiring and releasing a connection.
     *
     * @return true if the health check succeeds, false otherwise
     */
    public boolean performHealthCheck() {
        Connection conn = null;
        try {
            conn = getConnection();
            return validateConnection(conn, 5);
        } catch (Exception e) {
            logger.warn("Health check failed: {}", e.getMessage());
            return false;
        } finally {
            releaseConnection(conn);
        }
    }
    
    /**
     * Shuts down the connection manager and releases all resources.
     * This method should be called during application shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down ConnectionManager for payment database");
        HikariCPConfig.shutdown();
    }
    
    /**
     * Executes a database operation with transaction management.
     *
     * @param <T> the result type
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws Exception if the operation fails
     */
    public <T> T executeWithTransaction(TransactionOperation<T> operation) throws Exception {
        boolean localTransaction = !isInTransaction();
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            T result = operation.execute(this);
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return result;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        }
    }
    
    /**
     * Executes a database operation with transaction management and a specific isolation level.
     *
     * @param <T> the result type
     * @param operation the operation to execute
     * @param isolationLevel the transaction isolation level
     * @return the result of the operation
     * @throws Exception if the operation fails
     */
    public <T> T executeWithTransaction(TransactionOperation<T> operation, int isolationLevel) throws Exception {
        boolean localTransaction = !isInTransaction();
        
        if (localTransaction) {
            beginTransaction(isolationLevel);
        }
        
        try {
            T result = operation.execute(this);
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return result;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        }
    }
    
    /**
     * Functional interface for operations that require transaction management.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface TransactionOperation<T> {
        /**
         * Executes an operation within a transaction.
         *
         * @param connectionManager the connection manager
         * @return the result of the operation
         * @throws Exception if the operation fails
         */
        T execute(ConnectionManager connectionManager) throws Exception;
    }
}