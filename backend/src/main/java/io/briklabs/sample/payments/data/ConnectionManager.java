package io.briklabs.sample.payments.data;

import com.zaxxer.hikari.HikariDataSource;
import io.briklabs.sample.config.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages database connections for payment transaction processing, providing centralized
 * access to the HikariCP connection pool. This class handles connection acquisition, release,
 * and transaction management with proper error handling and resource cleanup.
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    // Connection acquisition timeout in milliseconds
    private static final long DEFAULT_ACQUISITION_TIMEOUT_MS = 10000; // 10 seconds
    
    // Thread-local storage for transaction connections to ensure consistent connection usage within a thread
    private static final ThreadLocal<Connection> transactionConnections = new ThreadLocal<>();
    
    // Connection pool configuration and data source
    private final HikariCPConfig hikariCPConfig;
    private final HikariDataSource dataSource;
    
    // Metrics for monitoring connection usage
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnectionsAcquired = new AtomicInteger(0);
    private final AtomicLong totalConnectionWaitTimeMs = new AtomicLong(0);
    private final AtomicLong totalTransactionTimeMs = new AtomicLong(0);
    private final AtomicInteger totalTransactions = new AtomicInteger(0);
    private final AtomicInteger failedTransactions = new AtomicInteger(0);
    
    // Track connection usage by operation type for monitoring
    private final Map<String, AtomicInteger> connectionsByOperation = new ConcurrentHashMap<>();
    
    // Singleton instance
    private static ConnectionManager instance;
    
    /**
     * Gets the singleton instance of ConnectionManager.
     * 
     * @param configSource The configuration source
     * @return The ConnectionManager instance
     */
    public static synchronized ConnectionManager getInstance(ConfigSource configSource) {
        if (instance == null) {
            instance = new ConnectionManager(configSource);
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern.
     * 
     * @param configSource The configuration source
     */
    private ConnectionManager(ConfigSource configSource) {
        logger.info("Initializing Payment ConnectionManager");
        this.hikariCPConfig = new HikariCPConfig(configSource);
        this.dataSource = hikariCPConfig.getDataSource();
        
        // Register shutdown hook to ensure proper resource cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        logger.info("Payment ConnectionManager initialized with pool size: {}", hikariCPConfig.getMaximumPoolSize());
    }
    
    /**
     * Gets a database connection from the pool.
     * Uses the default acquisition timeout.
     * 
     * @return A database connection
     * @throws SQLException if a connection cannot be acquired
     */
    public Connection getConnection() throws SQLException {
        return getConnection(DEFAULT_ACQUISITION_TIMEOUT_MS, null);
    }
    
    /**
     * Gets a database connection from the pool with operation tracking.
     * Uses the default acquisition timeout.
     * 
     * @param operationType The type of operation requiring the connection (for monitoring)
     * @return A database connection
     * @throws SQLException if a connection cannot be acquired
     */
    public Connection getConnection(String operationType) throws SQLException {
        return getConnection(DEFAULT_ACQUISITION_TIMEOUT_MS, operationType);
    }
    
    /**
     * Gets a database connection from the pool with a custom timeout.
     * 
     * @param timeoutMs The maximum time to wait for a connection in milliseconds
     * @param operationType The type of operation requiring the connection (for monitoring)
     * @return A database connection
     * @throws SQLException if a connection cannot be acquired within the timeout
     */
    public Connection getConnection(long timeoutMs, String operationType) throws SQLException {
        // Check if we're in a transaction context and return the existing connection if so
        Connection existingConnection = transactionConnections.get();
        if (existingConnection != null && !existingConnection.isClosed()) {
            logger.debug("Reusing existing transaction connection");
            return existingConnection;
        }
        
        // Track connection acquisition metrics
        long startTime = System.currentTimeMillis();
        Connection connection = null;
        
        try {
            // Attempt to acquire a connection from the pool
            connection = dataSource.getConnection();
            
            // Update metrics
            long waitTime = System.currentTimeMillis() - startTime;
            activeConnections.incrementAndGet();
            totalConnectionsAcquired.incrementAndGet();
            totalConnectionWaitTimeMs.addAndGet(waitTime);
            
            // Track connection by operation type if provided
            if (operationType != null) {
                connectionsByOperation.computeIfAbsent(operationType, k -> new AtomicInteger(0))
                        .incrementAndGet();
            }
            
            logger.debug("Acquired database connection after {}ms [active: {}, operation: {}]", 
                    waitTime, activeConnections.get(), operationType);
            
            return connection;
        } catch (SQLException e) {
            // Log detailed error information
            logger.error("Failed to acquire database connection after {}ms [active: {}, operation: {}]", 
                    System.currentTimeMillis() - startTime, activeConnections.get(), operationType, e);
            
            // Increment failed transaction count if this was for a transaction
            if (operationType != null && operationType.startsWith("transaction.")) {
                failedTransactions.incrementAndGet();
            }
            
            throw new SQLException("Failed to acquire payment database connection: " + e.getMessage(), e);
        }
    }
    
    /**
     * Releases a database connection back to the pool.
     * 
     * @param connection The connection to release
     * @param operationType The type of operation that was using the connection (for monitoring)
     */
    public void releaseConnection(Connection connection, String operationType) {
        if (connection == null) {
            return;
        }
        
        // Don't release connections that are part of an active transaction
        if (connection == transactionConnections.get()) {
            logger.debug("Not releasing connection as it's part of an active transaction");
            return;
        }
        
        try {
            // Ensure the connection is in a clean state before returning to the pool
            if (!connection.getAutoCommit()) {
                connection.setAutoCommit(true);
            }
            
            connection.close();
            
            // Update metrics
            activeConnections.decrementAndGet();
            
            // Update operation-specific metrics
            if (operationType != null) {
                AtomicInteger count = connectionsByOperation.get(operationType);
                if (count != null) {
                    count.decrementAndGet();
                }
            }
            
            logger.debug("Released database connection [active: {}, operation: {}]", 
                    activeConnections.get(), operationType);
        } catch (SQLException e) {
            logger.warn("Error releasing database connection [operation: {}]", operationType, e);
        }
    }
    
    /**
     * Releases a database connection back to the pool.
     * 
     * @param connection The connection to release
     */
    public void releaseConnection(Connection connection) {
        releaseConnection(connection, null);
    }
    
    /**
     * Begins a database transaction.
     * The connection is stored in ThreadLocal to ensure consistent usage within the transaction.
     * 
     * @return A connection with a transaction started
     * @throws SQLException if the transaction cannot be started
     */
    public Connection beginTransaction() throws SQLException {
        return beginTransaction(Connection.TRANSACTION_READ_COMMITTED);
    }
    
    /**
     * Begins a database transaction with a specific isolation level.
     * The connection is stored in ThreadLocal to ensure consistent usage within the transaction.
     * 
     * @param isolationLevel The transaction isolation level (from java.sql.Connection constants)
     * @return A connection with a transaction started
     * @throws SQLException if the transaction cannot be started
     */
    public Connection beginTransaction(int isolationLevel) throws SQLException {
        // Check if a transaction is already in progress
        Connection existingConnection = transactionConnections.get();
        if (existingConnection != null && !existingConnection.isClosed() && !existingConnection.getAutoCommit()) {
            logger.debug("Reusing existing transaction connection");
            return existingConnection;
        }
        
        // Start a new transaction
        long startTime = System.currentTimeMillis();
        Connection connection = getConnection("transaction.begin");
        
        try {
            // Configure the transaction
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(isolationLevel);
            
            // Store in ThreadLocal for consistent access
            transactionConnections.set(connection);
            
            logger.debug("Transaction started with isolation level: {}", isolationLevelToString(isolationLevel));
            return connection;
        } catch (SQLException e) {
            // Clean up on failure
            releaseConnection(connection, "transaction.begin.failed");
            transactionConnections.remove();
            
            logger.error("Failed to start transaction: {}", e.getMessage(), e);
            throw new SQLException("Failed to start payment transaction: " + e.getMessage(), e);
        }
    }
    
    /**
     * Commits the current transaction and releases the connection.
     * 
     * @throws SQLException if the transaction cannot be committed
     */
    public void commitTransaction() throws SQLException {
        Connection connection = transactionConnections.get();
        if (connection == null || connection.isClosed()) {
            logger.warn("No active transaction to commit");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            connection.commit();
            logger.debug("Transaction committed successfully in {}ms", System.currentTimeMillis() - startTime);
            
            // Update metrics
            totalTransactions.incrementAndGet();
            totalTransactionTimeMs.addAndGet(System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            logger.error("Failed to commit transaction: {}", e.getMessage(), e);
            failedTransactions.incrementAndGet();
            throw new SQLException("Failed to commit payment transaction: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                logger.warn("Error resetting connection state after transaction", e);
            }
            
            // Clear the ThreadLocal reference
            transactionConnections.remove();
            activeConnections.decrementAndGet();
        }
    }
    
    /**
     * Rolls back the current transaction and releases the connection.
     * 
     * @throws SQLException if the transaction cannot be rolled back
     */
    public void rollbackTransaction() throws SQLException {
        Connection connection = transactionConnections.get();
        if (connection == null || connection.isClosed()) {
            logger.warn("No active transaction to roll back");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            connection.rollback();
            logger.debug("Transaction rolled back successfully in {}ms", System.currentTimeMillis() - startTime);
            
            // Update metrics
            failedTransactions.incrementAndGet();
        } catch (SQLException e) {
            logger.error("Failed to roll back transaction: {}", e.getMessage(), e);
            throw new SQLException("Failed to roll back payment transaction: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                logger.warn("Error resetting connection state after transaction rollback", e);
            }
            
            // Clear the ThreadLocal reference
            transactionConnections.remove();
            activeConnections.decrementAndGet();
        }
    }
    
    /**
     * Executes a database operation within a transaction.
     * Automatically handles transaction management, including commit and rollback.
     * 
     * @param operation The database operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws SQLException if the operation fails
     */
    public <T> T executeInTransaction(TransactionOperation<T> operation) throws SQLException {
        Connection connection = null;
        boolean success = false;
        
        try {
            // Begin a transaction
            connection = beginTransaction();
            
            // Execute the operation
            T result = operation.execute(connection);
            
            // Commit the transaction
            commitTransaction();
            success = true;
            
            return result;
        } catch (SQLException e) {
            // Roll back on failure
            if (connection != null) {
                rollbackTransaction();
            }
            throw e;
        } finally {
            // Ensure transaction is cleaned up if not already done
            if (!success && connection != null) {
                try {
                    if (!connection.isClosed() && !connection.getAutoCommit()) {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        connection.close();
                        transactionConnections.remove();
                        activeConnections.decrementAndGet();
                    }
                } catch (SQLException e) {
                    logger.warn("Error cleaning up transaction resources", e);
                }
            }
        }
    }
    
    /**
     * Executes a database operation within a transaction with a specific isolation level.
     * Automatically handles transaction management, including commit and rollback.
     * 
     * @param isolationLevel The transaction isolation level
     * @param operation The database operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws SQLException if the operation fails
     */
    public <T> T executeInTransaction(int isolationLevel, TransactionOperation<T> operation) throws SQLException {
        Connection connection = null;
        boolean success = false;
        
        try {
            // Begin a transaction with the specified isolation level
            connection = beginTransaction(isolationLevel);
            
            // Execute the operation
            T result = operation.execute(connection);
            
            // Commit the transaction
            commitTransaction();
            success = true;
            
            return result;
        } catch (SQLException e) {
            // Roll back on failure
            if (connection != null) {
                rollbackTransaction();
            }
            throw e;
        } finally {
            // Ensure transaction is cleaned up if not already done
            if (!success && connection != null) {
                try {
                    if (!connection.isClosed() && !connection.getAutoCommit()) {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        connection.close();
                        transactionConnections.remove();
                        activeConnections.decrementAndGet();
                    }
                } catch (SQLException e) {
                    logger.warn("Error cleaning up transaction resources", e);
                }
            }
        }
    }
    
    /**
     * Safely closes a PreparedStatement, handling any exceptions.
     * 
     * @param statement The PreparedStatement to close
     */
    public void closeStatement(PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.warn("Error closing prepared statement", e);
            }
        }
    }
    
    /**
     * Safely closes a ResultSet, handling any exceptions.
     * 
     * @param resultSet The ResultSet to close
     */
    public void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.warn("Error closing result set", e);
            }
        }
    }
    
    /**
     * Safely closes a Connection, handling any exceptions.
     * This method should only be used for connections that are not part of a transaction.
     * For transaction connections, use commitTransaction() or rollbackTransaction().
     * 
     * @param connection The Connection to close
     */
    public void closeConnection(Connection connection) {
        releaseConnection(connection);
    }
    
    /**
     * Safely closes all resources (ResultSet, PreparedStatement, and Connection).
     * This method should only be used for connections that are not part of a transaction.
     * 
     * @param resultSet The ResultSet to close
     * @param statement The PreparedStatement to close
     * @param connection The Connection to close
     */
    public void closeResources(ResultSet resultSet, PreparedStatement statement, Connection connection) {
        closeResultSet(resultSet);
        closeStatement(statement);
        releaseConnection(connection);
    }
    
    /**
     * Safely closes ResultSet and PreparedStatement resources.
     * Does not close the connection, which is useful for transaction contexts.
     * 
     * @param resultSet The ResultSet to close
     * @param statement The PreparedStatement to close
     */
    public void closeResources(ResultSet resultSet, PreparedStatement statement) {
        closeResultSet(resultSet);
        closeStatement(statement);
    }
    
    /**
     * Checks if a connection is valid and can be used for database operations.
     * 
     * @param connection The connection to validate
     * @param timeoutSeconds The timeout in seconds for validation
     * @return true if the connection is valid, false otherwise
     */
    public boolean isConnectionValid(Connection connection, int timeoutSeconds) {
        if (connection == null) {
            return false;
        }
        
        try {
            return !connection.isClosed() && connection.isValid(timeoutSeconds);
        } catch (SQLException e) {
            logger.warn("Error validating connection", e);
            return false;
        }
    }
    
    /**
     * Gets the current health status of the connection pool.
     * 
     * @return A string representation of the pool health
     */
    public String getHealthStatus() {
        return hikariCPConfig.getHealthStatus();
    }
    
    /**
     * Gets the current number of active connections.
     * 
     * @return The number of active connections
     */
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }
    
    /**
     * Gets the total number of connections acquired since startup.
     * 
     * @return The total number of connections acquired
     */
    public int getTotalConnectionsAcquired() {
        return totalConnectionsAcquired.get();
    }
    
    /**
     * Gets the average connection wait time in milliseconds.
     * 
     * @return The average connection wait time
     */
    public double getAverageConnectionWaitTimeMs() {
        int total = totalConnectionsAcquired.get();
        return total > 0 ? (double) totalConnectionWaitTimeMs.get() / total : 0;
    }
    
    /**
     * Gets the average transaction time in milliseconds.
     * 
     * @return The average transaction time
     */
    public double getAverageTransactionTimeMs() {
        int total = totalTransactions.get();
        return total > 0 ? (double) totalTransactionTimeMs.get() / total : 0;
    }
    
    /**
     * Gets the total number of transactions processed.
     * 
     * @return The total number of transactions
     */
    public int getTotalTransactions() {
        return totalTransactions.get();
    }
    
    /**
     * Gets the total number of failed transactions.
     * 
     * @return The total number of failed transactions
     */
    public int getFailedTransactions() {
        return failedTransactions.get();
    }
    
    /**
     * Gets the transaction success rate as a percentage.
     * 
     * @return The transaction success rate (0-100)
     */
    public double getTransactionSuccessRate() {
        int total = totalTransactions.get();
        int failed = failedTransactions.get();
        return total > 0 ? 100.0 * (total - failed) / total : 0;
    }
    
    /**
     * Gets the number of connections currently in use for a specific operation type.
     * 
     * @param operationType The operation type
     * @return The number of connections in use for the operation
     */
    public int getConnectionsForOperation(String operationType) {
        AtomicInteger count = connectionsByOperation.get(operationType);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Shuts down the connection manager and releases all resources.
     */
    public void shutdown() {
        logger.info("Shutting down Payment ConnectionManager");
        
        // Close any active transaction connections
        Connection connection = transactionConnections.get();
        if (connection != null) {
            try {
                if (!connection.getAutoCommit()) {
                    logger.warn("Rolling back uncommitted transaction during shutdown");
                    connection.rollback();
                }
                connection.close();
            } catch (SQLException e) {
                logger.warn("Error closing transaction connection during shutdown", e);
            } finally {
                transactionConnections.remove();
            }
        }
        
        // Close the connection pool
        if (hikariCPConfig != null) {
            hikariCPConfig.close();
        }
        
        logger.info("Payment ConnectionManager shutdown complete");
    }
    
    /**
     * Converts a transaction isolation level constant to a human-readable string.
     * 
     * @param isolationLevel The isolation level constant from java.sql.Connection
     * @return A human-readable string representation of the isolation level
     */
    private String isolationLevelToString(int isolationLevel) {
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
     * Functional interface for operations that execute within a transaction.
     * 
     * @param <T> The return type of the operation
     */
    @FunctionalInterface
    public interface TransactionOperation<T> {
        /**
         * Executes a database operation using the provided connection.
         * 
         * @param connection The database connection
         * @return The result of the operation
         * @throws SQLException if the operation fails
         */
        T execute(Connection connection) throws SQLException;
    }
}