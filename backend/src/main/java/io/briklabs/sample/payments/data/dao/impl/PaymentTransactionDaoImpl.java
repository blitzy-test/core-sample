package io.briklabs.sample.payments.data.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentType;

/**
 * Implementation of the PaymentTransactionDAO interface that handles all database operations
 * for payment transactions. This class provides CRUD operations, complex filtering, status updates,
 * and specialized queries for payment transactions.
 * <p>
 * It uses the PaymentQueryBuilder to construct optimized SQL queries for transaction filtering
 * and implements all transaction lifecycle methods required by the payment service layer.
 * </p>
 */
public class PaymentTransactionDaoImpl implements PaymentTransactionDAO {
    private static final Logger logger = LoggerFactory.getLogger(PaymentTransactionDaoImpl.class);
    
    private final HikariDataSource dataSource;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    
    /**
     * Constructs a new PaymentTransactionDaoImpl with the specified database configuration.
     * Initializes the HikariCP connection pool with optimized settings for payment processing.
     *
     * @param dbConfig The database configuration
     */
    public PaymentTransactionDaoImpl(DatabaseConfig dbConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbConfig.getDatabaseURL());
        config.setUsername(dbConfig.getDatabaseUsername());
        config.setPassword(dbConfig.getDatabasePassword());
        config.setSchema(dbConfig.getDatabaseSchema());
        
        // Apply connection pool configuration if available
        dbConfig.getConnectionPoolConfig().ifPresent(poolConfig -> {
            poolConfig.forEach((key, value) -> {
                if (value instanceof Integer) {
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
                            // Ignore unknown integer properties
                            break;
                    }
                } else if (value instanceof Boolean) {
                    switch (key) {
                        case "autoCommit":
                            config.setAutoCommit((Boolean) value);
                            break;
                        case "registerMbeans":
                            config.setRegisterMbeans((Boolean) value);
                            break;
                        default:
                            // Ignore unknown boolean properties
                            break;
                    }
                } else if (value instanceof String) {
                    switch (key) {
                        case "connectionTestQuery":
                            config.setConnectionTestQuery((String) value);
                            break;
                        case "poolName":
                            config.setPoolName((String) value);
                            break;
                        default:
                            // Ignore unknown string properties
                            break;
                    }
                }
            });
        });
        
        // Set default values if not provided in configuration
        if (!config.isAutoCommitSet()) {
            config.setAutoCommit(false);
        }
        if (config.getMaximumPoolSize() == -1) {
            config.setMaximumPoolSize(30); // Default optimized for payment processing
        }
        if (config.getMinimumIdle() == -1) {
            config.setMinimumIdle(10);
        }
        if (config.getConnectionTimeout() == -1) {
            config.setConnectionTimeout(20000);
        }
        if (config.getIdleTimeout() == -1) {
            config.setIdleTimeout(300000);
        }
        if (config.getMaxLifetime() == -1) {
            config.setMaxLifetime(1200000);
        }
        if (config.getLeakDetectionThreshold() == 0) {
            config.setLeakDetectionThreshold(60000);
        }
        if (config.getPoolName() == null) {
            config.setPoolName("PaymentHikariPool");
        }
        if (config.getConnectionTestQuery() == null) {
            config.setConnectionTestQuery("SELECT 1");
        }
        
        // Enable JMX monitoring
        config.setRegisterMbeans(true);
        
        this.dataSource = new HikariDataSource(config);
        logger.info("Initialized PaymentTransactionDaoImpl with connection pool: {}", config.getPoolName());
    }
    
    /**
     * Gets a database connection from the connection pool.
     * If a transaction is in progress, returns the transaction's connection.
     *
     * @return A database connection
     * @throws ConnectionException if a connection cannot be obtained
     */
    private Connection getConnection() throws ConnectionException {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            return conn;
        }
        
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Failed to obtain database connection", e);
            throw new ConnectionException("Failed to obtain database connection", e);
        }
    }
    
    /**
     * Closes a database connection if it's not part of an active transaction.
     *
     * @param connection The connection to close
     */
    private void closeConnection(Connection connection) {
        if (connection != null && transactionConnection.get() != connection) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Error closing database connection", e);
            }
        }
    }
    
    /**
     * Closes a PreparedStatement.
     *
     * @param statement The statement to close
     */
    private void closeStatement(PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.warn("Error closing prepared statement", e);
            }
        }
    }
    
    /**
     * Closes a ResultSet.
     *
     * @param resultSet The result set to close
     */
    private void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.warn("Error closing result set", e);
            }
        }
    }
    
    /**
     * Maps a ResultSet row to a PaymentTransaction object.
     *
     * @param rs The ResultSet containing transaction data
     * @return A PaymentTransaction object
     * @throws SQLException if a database access error occurs
     */
    private PaymentTransaction mapRowToTransaction(ResultSet rs) throws SQLException {
        UUID transactionId = (UUID) rs.getObject("transaction_id");
        UUID organizationId = (UUID) rs.getObject("organization_id");
        UUID accountId = (UUID) rs.getObject("account_id");
        PaymentStatus status = PaymentStatus.valueOf(rs.getString("status"));
        BigDecimal amount = rs.getBigDecimal("amount");
        String currency = rs.getString("currency");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        String merchantId = rs.getString("merchant_id");
        PaymentType paymentType = PaymentType.valueOf(rs.getString("payment_type"));
        String transactionReference = rs.getString("transaction_reference");
        String description = rs.getString("description");
        
        return new PaymentTransaction(
                transactionId, organizationId, accountId, status, amount, currency,
                createdAt, updatedAt, merchantId, paymentType, transactionReference, description);
    }
    
    @Override
    public PaymentTransaction create(PaymentTransaction transaction) 
            throws ValidationException, ConnectionException, QueryExecutionException, TransactionException {
        if (transaction == null) {
            throw new ValidationException("Transaction cannot be null");
        }
        
        try {
            transaction.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid transaction data: " + e.getMessage(), e);
        }
        
        // Generate a new transaction ID if not provided
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(UUID.randomUUID());
        }
        
        // Set timestamps if not provided
        Instant now = Instant.now();
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(now);
        }
        if (transaction.getUpdatedAt() == null) {
            transaction.setUpdatedAt(now);
        }
        
        // Set initial status if not provided
        if (transaction.getStatus() == null) {
            transaction.setStatus(PaymentStatus.CREATED);
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            
            String sql = "INSERT INTO payment_transaction " +
                    "(transaction_id, organization_id, account_id, status, amount, currency, " +
                    "created_at, updated_at, merchant_id, payment_type, transaction_reference, description) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, transaction.getTransactionId());
            stmt.setObject(2, transaction.getOrganizationId());
            stmt.setObject(3, transaction.getAccountId());
            stmt.setString(4, transaction.getStatus().name());
            stmt.setBigDecimal(5, transaction.getAmount());
            stmt.setString(6, transaction.getCurrency());
            stmt.setTimestamp(7, Timestamp.from(transaction.getCreatedAt()));
            stmt.setTimestamp(8, Timestamp.from(transaction.getUpdatedAt()));
            stmt.setString(9, transaction.getMerchantId());
            stmt.setString(10, transaction.getPaymentType().name());
            stmt.setString(11, transaction.getTransactionReference());
            stmt.setString(12, transaction.getDescription());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected != 1) {
                throw new QueryExecutionException("Failed to create transaction, unexpected rows affected: " + rowsAffected);
            }
            
            if (transactionConnection.get() == null) {
                conn.commit();
            }
            
            logger.info("Created payment transaction: {}", transaction.getTransactionId());
            return transaction;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error creating payment transaction");
            return null; // This line will never be reached due to exception handling
        } finally {
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public Optional<PaymentTransaction> findById(UUID id) 
            throws ConnectionException, QueryExecutionException {
        if (id == null) {
            return Optional.empty();
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            PaymentQueryBuilder queryBuilder = PaymentQueryBuilder.create()
                    .buildTransactionDetailsQuery(id);
            
            stmt = queryBuilder.buildPreparedStatement(conn);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                PaymentTransaction transaction = mapRowToTransaction(rs);
                return Optional.of(transaction);
            } else {
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error finding payment transaction by ID: " + id);
            return Optional.empty(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public PaymentTransaction update(PaymentTransaction transaction) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        if (transaction == null) {
            throw new ValidationException("Transaction cannot be null");
        }
        
        if (transaction.getTransactionId() == null) {
            throw new ValidationException("Transaction ID cannot be null for update operation");
        }
        
        try {
            transaction.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid transaction data: " + e.getMessage(), e);
        }
        
        // Verify the transaction exists
        if (!exists(transaction.getTransactionId())) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + transaction.getTransactionId());
        }
        
        // Update the timestamp
        transaction.setUpdatedAt(Instant.now());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            
            String sql = "UPDATE payment_transaction SET " +
                    "organization_id = ?, account_id = ?, status = ?, amount = ?, currency = ?, " +
                    "updated_at = ?, merchant_id = ?, payment_type = ?, transaction_reference = ?, description = ? " +
                    "WHERE transaction_id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, transaction.getOrganizationId());
            stmt.setObject(2, transaction.getAccountId());
            stmt.setString(3, transaction.getStatus().name());
            stmt.setBigDecimal(4, transaction.getAmount());
            stmt.setString(5, transaction.getCurrency());
            stmt.setTimestamp(6, Timestamp.from(transaction.getUpdatedAt()));
            stmt.setString(7, transaction.getMerchantId());
            stmt.setString(8, transaction.getPaymentType().name());
            stmt.setString(9, transaction.getTransactionReference());
            stmt.setString(10, transaction.getDescription());
            stmt.setObject(11, transaction.getTransactionId());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected != 1) {
                throw new QueryExecutionException("Failed to update transaction, unexpected rows affected: " + rowsAffected);
            }
            
            if (transactionConnection.get() == null) {
                conn.commit();
            }
            
            logger.info("Updated payment transaction: {}", transaction.getTransactionId());
            return transaction;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error updating payment transaction: " + transaction.getTransactionId());
            return null; // This line will never be reached due to exception handling
        } finally {
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public boolean delete(UUID id) 
            throws ConnectionException, QueryExecutionException, TransactionException {
        if (id == null) {
            return false;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            
            String sql = "DELETE FROM payment_transaction WHERE transaction_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (transactionConnection.get() == null) {
                conn.commit();
            }
            
            if (rowsAffected > 0) {
                logger.info("Deleted payment transaction: {}", id);
                return true;
            } else {
                logger.info("No payment transaction found to delete with ID: {}", id);
                return false;
            }
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error deleting payment transaction: " + id);
            return false; // This line will never be reached due to exception handling
        } finally {
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public List<PaymentTransaction> query(PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            PaymentQueryBuilder queryBuilder = PaymentQueryBuilder.create()
                    .buildTransactionQuery()
                    .applyFilters(filterParams);
            
            stmt = queryBuilder.buildPreparedStatement(conn);
            rs = stmt.executeQuery();
            
            List<PaymentTransaction> transactions = new ArrayList<>();
            while (rs.next()) {
                transactions.add(mapRowToTransaction(rs));
            }
            
            return transactions;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error querying payment transactions");
            return List.of(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public long count(PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            // Create a count query based on the filter parameters
            PaymentQueryBuilder queryBuilder = PaymentQueryBuilder.create()
                    .count("t.transaction_id")
                    .from("payment_transaction t");
            
            // Apply filters but skip pagination and sorting
            if (filterParams != null) {
                // Create a copy of filter params without pagination and sorting
                PaymentFilterParams countFilters = new PaymentFilterParams();
                countFilters.setOrganizationId(filterParams.getOrganizationId());
                countFilters.setAccountId(filterParams.getAccountId());
                countFilters.setDateRange(filterParams.getDateRange());
                countFilters.setAmountRange(filterParams.getAmountRange());
                countFilters.setStatusFilter(filterParams.getStatusFilter());
                countFilters.setMerchantId(filterParams.getMerchantId());
                countFilters.setPaymentType(filterParams.getPaymentType());
                countFilters.setSearchTerm(filterParams.getSearchTerm());
                
                queryBuilder.applyFilters(countFilters);
            }
            
            stmt = queryBuilder.buildPreparedStatement(conn);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
            return 0;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error counting payment transactions");
            return 0; // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public boolean exists(UUID id) 
            throws ConnectionException, QueryExecutionException {
        if (id == null) {
            return false;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            String sql = "SELECT 1 FROM payment_transaction WHERE transaction_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, id);
            
            rs = stmt.executeQuery();
            return rs.next();
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error checking if payment transaction exists: " + id);
            return false; // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public void beginTransaction() 
            throws ConnectionException, TransactionException {
        if (transactionConnection.get() != null) {
            throw new TransactionException("Transaction already in progress");
        }
        
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            transactionConnection.set(conn);
            logger.debug("Transaction started");
        } catch (SQLException e) {
            logger.error("Failed to begin transaction", e);
            throw new TransactionException("Failed to begin transaction", e);
        }
    }
    
    @Override
    public void commitTransaction() 
            throws TransactionException {
        Connection conn = transactionConnection.get();
        if (conn == null) {
            throw new TransactionException("No transaction in progress");
        }
        
        try {
            conn.commit();
            logger.debug("Transaction committed");
        } catch (SQLException e) {
            logger.error("Failed to commit transaction", e);
            throw new TransactionException("Failed to commit transaction", e);
        } finally {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                logger.warn("Error closing transaction connection", e);
            }
            transactionConnection.remove();
        }
    }
    
    @Override
    public void rollbackTransaction() 
            throws TransactionException {
        Connection conn = transactionConnection.get();
        if (conn == null) {
            throw new TransactionException("No transaction in progress");
        }
        
        try {
            conn.rollback();
            logger.debug("Transaction rolled back");
        } catch (SQLException e) {
            logger.error("Failed to rollback transaction", e);
            throw new TransactionException("Failed to rollback transaction", e);
        } finally {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                logger.warn("Error closing transaction connection", e);
            }
            transactionConnection.remove();
        }
    }
    
    @Override
    public List<PaymentTransaction> batchCreate(List<PaymentTransaction> transactions) 
            throws ValidationException, ConnectionException, QueryExecutionException, TransactionException {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }
        
        // Validate all transactions before processing
        for (PaymentTransaction transaction : transactions) {
            if (transaction == null) {
                throw new ValidationException("Transaction cannot be null");
            }
            
            try {
                transaction.validate();
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid transaction data: " + e.getMessage(), e);
            }
            
            // Generate a new transaction ID if not provided
            if (transaction.getTransactionId() == null) {
                transaction.setTransactionId(UUID.randomUUID());
            }
            
            // Set timestamps if not provided
            Instant now = Instant.now();
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(now);
            }
            if (transaction.getUpdatedAt() == null) {
                transaction.setUpdatedAt(now);
            }
            
            // Set initial status if not provided
            if (transaction.getStatus() == null) {
                transaction.setStatus(PaymentStatus.CREATED);
            }
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean localTransaction = false;
        
        try {
            conn = getConnection();
            
            // Start a local transaction if not already in a transaction
            if (transactionConnection.get() == null) {
                conn.setAutoCommit(false);
                localTransaction = true;
            }
            
            String sql = "INSERT INTO payment_transaction " +
                    "(transaction_id, organization_id, account_id, status, amount, currency, " +
                    "created_at, updated_at, merchant_id, payment_type, transaction_reference, description) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql);
            
            for (PaymentTransaction transaction : transactions) {
                stmt.setObject(1, transaction.getTransactionId());
                stmt.setObject(2, transaction.getOrganizationId());
                stmt.setObject(3, transaction.getAccountId());
                stmt.setString(4, transaction.getStatus().name());
                stmt.setBigDecimal(5, transaction.getAmount());
                stmt.setString(6, transaction.getCurrency());
                stmt.setTimestamp(7, Timestamp.from(transaction.getCreatedAt()));
                stmt.setTimestamp(8, Timestamp.from(transaction.getUpdatedAt()));
                stmt.setString(9, transaction.getMerchantId());
                stmt.setString(10, transaction.getPaymentType().name());
                stmt.setString(11, transaction.getTransactionReference());
                stmt.setString(12, transaction.getDescription());
                
                stmt.addBatch();
            }
            
            int[] rowsAffected = stmt.executeBatch();
            
            // Verify all transactions were created
            for (int i = 0; i < rowsAffected.length; i++) {
                if (rowsAffected[i] != 1) {
                    if (localTransaction) {
                        conn.rollback();
                    }
                    throw new QueryExecutionException("Failed to create transaction at index " + i + 
                            ", unexpected rows affected: " + rowsAffected[i]);
                }
            }
            
            if (localTransaction) {
                conn.commit();
            }
            
            logger.info("Created {} payment transactions in batch", transactions.size());
            return transactions;
            
        } catch (SQLException e) {
            if (localTransaction && conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            handleSqlException(conn, e, "Error creating payment transactions in batch");
            return List.of(); // This line will never be reached due to exception handling
        } finally {
            closeStatement(stmt);
            if (localTransaction) {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    logger.warn("Error resetting auto-commit", e);
                }
            }
            closeConnection(conn);
        }
    }
    
    @Override
    public List<PaymentTransaction> batchUpdate(List<PaymentTransaction> transactions) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }
        
        // Validate all transactions before processing
        for (PaymentTransaction transaction : transactions) {
            if (transaction == null) {
                throw new ValidationException("Transaction cannot be null");
            }
            
            if (transaction.getTransactionId() == null) {
                throw new ValidationException("Transaction ID cannot be null for update operation");
            }
            
            try {
                transaction.validate();
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid transaction data: " + e.getMessage(), e);
            }
            
            // Verify the transaction exists
            if (!exists(transaction.getTransactionId())) {
                throw new ResourceNotFoundException("Transaction not found with ID: " + transaction.getTransactionId());
            }
            
            // Update the timestamp
            transaction.setUpdatedAt(Instant.now());
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean localTransaction = false;
        
        try {
            conn = getConnection();
            
            // Start a local transaction if not already in a transaction
            if (transactionConnection.get() == null) {
                conn.setAutoCommit(false);
                localTransaction = true;
            }
            
            String sql = "UPDATE payment_transaction SET " +
                    "organization_id = ?, account_id = ?, status = ?, amount = ?, currency = ?, " +
                    "updated_at = ?, merchant_id = ?, payment_type = ?, transaction_reference = ?, description = ? " +
                    "WHERE transaction_id = ?";
            
            stmt = conn.prepareStatement(sql);
            
            for (PaymentTransaction transaction : transactions) {
                stmt.setObject(1, transaction.getOrganizationId());
                stmt.setObject(2, transaction.getAccountId());
                stmt.setString(3, transaction.getStatus().name());
                stmt.setBigDecimal(4, transaction.getAmount());
                stmt.setString(5, transaction.getCurrency());
                stmt.setTimestamp(6, Timestamp.from(transaction.getUpdatedAt()));
                stmt.setString(7, transaction.getMerchantId());
                stmt.setString(8, transaction.getPaymentType().name());
                stmt.setString(9, transaction.getTransactionReference());
                stmt.setString(10, transaction.getDescription());
                stmt.setObject(11, transaction.getTransactionId());
                
                stmt.addBatch();
            }
            
            int[] rowsAffected = stmt.executeBatch();
            
            // Verify all transactions were updated
            for (int i = 0; i < rowsAffected.length; i++) {
                if (rowsAffected[i] != 1) {
                    if (localTransaction) {
                        conn.rollback();
                    }
                    throw new QueryExecutionException("Failed to update transaction at index " + i + 
                            ", unexpected rows affected: " + rowsAffected[i]);
                }
            }
            
            if (localTransaction) {
                conn.commit();
            }
            
            logger.info("Updated {} payment transactions in batch", transactions.size());
            return transactions;
            
        } catch (SQLException e) {
            if (localTransaction && conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            handleSqlException(conn, e, "Error updating payment transactions in batch");
            return List.of(); // This line will never be reached due to exception handling
        } finally {
            closeStatement(stmt);
            if (localTransaction) {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    logger.warn("Error resetting auto-commit", e);
                }
            }
            closeConnection(conn);
        }
    }
    
    @Override
    public int batchDelete(List<UUID> ids) 
            throws ConnectionException, QueryExecutionException, TransactionException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean localTransaction = false;
        
        try {
            conn = getConnection();
            
            // Start a local transaction if not already in a transaction
            if (transactionConnection.get() == null) {
                conn.setAutoCommit(false);
                localTransaction = true;
            }
            
            String sql = "DELETE FROM payment_transaction WHERE transaction_id = ?";
            stmt = conn.prepareStatement(sql);
            
            for (UUID id : ids) {
                if (id != null) {
                    stmt.setObject(1, id);
                    stmt.addBatch();
                }
            }
            
            int[] rowsAffected = stmt.executeBatch();
            
            if (localTransaction) {
                conn.commit();
            }
            
            int totalDeleted = 0;
            for (int count : rowsAffected) {
                totalDeleted += count;
            }
            
            logger.info("Deleted {} payment transactions in batch", totalDeleted);
            return totalDeleted;
            
        } catch (SQLException e) {
            if (localTransaction && conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            handleSqlException(conn, e, "Error deleting payment transactions in batch");
            return 0; // This line will never be reached due to exception handling
        } finally {
            closeStatement(stmt);
            if (localTransaction) {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    logger.warn("Error resetting auto-commit", e);
                }
            }
            closeConnection(conn);
        }
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set organization ID in filter params
        params.setOrganizationId(organizationId);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, 
            PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set organization ID and account ID in filter params
        params.setOrganizationId(organizationId);
        params.setAccountId(accountId);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public List<PaymentTransaction> findByStatus(PaymentStatus status, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set status in filter params
        StatusFilter statusFilter = new StatusFilter();
        statusFilter.addStatus(status);
        params.setStatusFilter(statusFilter);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public List<PaymentTransaction> findByStatusIn(StatusFilter statusFilter, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (statusFilter == null || statusFilter.getStatuses().isEmpty()) {
            throw new IllegalArgumentException("Status filter cannot be null or empty");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set status filter in filter params
        params.setStatusFilter(statusFilter);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public List<PaymentTransaction> findByCreatedAtBetween(DateRangeFilter dateRange, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (dateRange == null) {
            throw new IllegalArgumentException("Date range cannot be null");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set date range in filter params
        params.setDateRange(dateRange);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public List<PaymentTransaction> findByUpdatedAtBetween(DateRangeFilter dateRange, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (dateRange == null) {
            throw new IllegalArgumentException("Date range cannot be null");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            PaymentQueryBuilder queryBuilder = PaymentQueryBuilder.create()
                    .buildTransactionQuery();
            
            // Apply date range to updated_at field
            queryBuilder.dateRange("t.updated_at", 
                    dateRange.getStartDate(), 
                    dateRange.getEndDate());
            
            // Apply other filters if provided
            if (filterParams != null) {
                // Create a copy of filter params without date range
                PaymentFilterParams otherFilters = new PaymentFilterParams();
                otherFilters.setOrganizationId(filterParams.getOrganizationId());
                otherFilters.setAccountId(filterParams.getAccountId());
                otherFilters.setAmountRange(filterParams.getAmountRange());
                otherFilters.setStatusFilter(filterParams.getStatusFilter());
                otherFilters.setMerchantId(filterParams.getMerchantId());
                otherFilters.setPaymentType(filterParams.getPaymentType());
                otherFilters.setSearchTerm(filterParams.getSearchTerm());
                otherFilters.setSortCriteria(filterParams.getSortCriteria());
                otherFilters.setPagination(filterParams.getPagination());
                
                queryBuilder.applyFilters(otherFilters);
            }
            
            stmt = queryBuilder.buildPreparedStatement(conn);
            rs = stmt.executeQuery();
            
            List<PaymentTransaction> transactions = new ArrayList<>();
            while (rs.next()) {
                transactions.add(mapRowToTransaction(rs));
            }
            
            return transactions;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error finding payment transactions by updated date range");
            return List.of(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public List<PaymentTransaction> findByAmountBetween(AmountRangeFilter amountRange, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (amountRange == null) {
            throw new IllegalArgumentException("Amount range cannot be null");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set amount range in filter params
        params.setAmountRange(amountRange);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public List<PaymentTransaction> findByMerchantId(String merchantId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (merchantId == null || merchantId.isEmpty()) {
            throw new IllegalArgumentException("Merchant ID cannot be null or empty");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set merchant ID in filter params
        params.setMerchantId(merchantId);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public List<PaymentTransaction> findByPaymentType(PaymentType paymentType, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (paymentType == null) {
            throw new IllegalArgumentException("Payment type cannot be null");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set payment type in filter params
        params.setPaymentType(paymentType.name());
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public Optional<PaymentTransaction> findByTransactionReference(String reference) 
            throws ConnectionException, QueryExecutionException {
        if (reference == null || reference.isEmpty()) {
            return Optional.empty();
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            String sql = "SELECT * FROM payment_transaction WHERE transaction_reference = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, reference);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                PaymentTransaction transaction = mapRowToTransaction(rs);
                return Optional.of(transaction);
            } else {
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error finding payment transaction by reference: " + reference);
            return Optional.empty(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public PaymentTransaction updateStatus(UUID transactionId, PaymentStatus newStatus) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        if (newStatus == null) {
            throw new ValidationException("New status cannot be null");
        }
        
        // Get the current transaction
        PaymentTransaction transaction = findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        // Validate the status transition
        try {
            PaymentStatus.validateTransition(transaction.getStatus(), newStatus);
        } catch (IllegalStateException e) {
            throw new ValidationException("Invalid status transition: " + e.getMessage(), e);
        }
        
        // Update the status
        transaction.updateStatus(newStatus);
        
        // Save the updated transaction
        return update(transaction);
    }
    
    @Override
    public PaymentTransaction captureTransaction(UUID transactionId, BigDecimal captureAmount) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        if (captureAmount == null || captureAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Capture amount must be greater than zero");
        }
        
        // Get the current transaction
        PaymentTransaction transaction = findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        // Validate that the transaction can be captured
        if (!transaction.canCapture()) {
            throw new ValidationException("Transaction cannot be captured. Current status: " + transaction.getStatus());
        }
        
        // Validate the capture amount
        if (captureAmount.compareTo(transaction.getAmount()) > 0) {
            throw new ValidationException("Capture amount cannot exceed the authorized amount");
        }
        
        // Begin a transaction if not already in one
        boolean localTransaction = false;
        if (transactionConnection.get() == null) {
            beginTransaction();
            localTransaction = true;
        }
        
        try {
            // Update the transaction status to CAPTURED
            transaction.updateStatus(PaymentStatus.CAPTURED);
            
            // If partial capture, update the amount
            if (captureAmount.compareTo(transaction.getAmount()) < 0) {
                transaction.setAmount(captureAmount);
            }
            
            // Save the updated transaction
            PaymentTransaction updatedTransaction = update(transaction);
            
            // Create a payment event for the capture
            createPaymentEvent(transaction.getTransactionId(), "CAPTURE", 
                    PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, 
                    Map.of("captureAmount", captureAmount.toString()));
            
            // Commit the transaction if we started it
            if (localTransaction) {
                commitTransaction();
            }
            
            return updatedTransaction;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    rollbackTransaction();
                } catch (TransactionException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            
            // Rethrow the exception
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            } else if (e instanceof ValidationException) {
                throw (ValidationException) e;
            } else if (e instanceof ConnectionException) {
                throw (ConnectionException) e;
            } else if (e instanceof QueryExecutionException) {
                throw (QueryExecutionException) e;
            } else if (e instanceof TransactionException) {
                throw (TransactionException) e;
            } else {
                throw new QueryExecutionException("Error capturing transaction: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public PaymentTransaction refundTransaction(UUID transactionId, BigDecimal refundAmount) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Refund amount must be greater than zero");
        }
        
        // Get the current transaction
        PaymentTransaction transaction = findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        // Validate that the transaction can be refunded
        if (!transaction.canRefund()) {
            throw new ValidationException("Transaction cannot be refunded. Current status: " + transaction.getStatus());
        }
        
        // Validate the refund amount
        if (refundAmount.compareTo(transaction.getAmount()) > 0) {
            throw new ValidationException("Refund amount cannot exceed the captured amount");
        }
        
        // Begin a transaction if not already in one
        boolean localTransaction = false;
        if (transactionConnection.get() == null) {
            beginTransaction();
            localTransaction = true;
        }
        
        try {
            // Update the transaction status to REFUNDED
            transaction.updateStatus(PaymentStatus.REFUNDED);
            
            // Save the updated transaction
            PaymentTransaction updatedTransaction = update(transaction);
            
            // Create a payment event for the refund
            createPaymentEvent(transaction.getTransactionId(), "REFUND", 
                    PaymentStatus.CAPTURED, PaymentStatus.REFUNDED, 
                    Map.of("refundAmount", refundAmount.toString()));
            
            // Commit the transaction if we started it
            if (localTransaction) {
                commitTransaction();
            }
            
            return updatedTransaction;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    rollbackTransaction();
                } catch (TransactionException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            
            // Rethrow the exception
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            } else if (e instanceof ValidationException) {
                throw (ValidationException) e;
            } else if (e instanceof ConnectionException) {
                throw (ConnectionException) e;
            } else if (e instanceof QueryExecutionException) {
                throw (QueryExecutionException) e;
            } else if (e instanceof TransactionException) {
                throw (TransactionException) e;
            } else {
                throw new QueryExecutionException("Error refunding transaction: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public PaymentTransaction voidTransaction(UUID transactionId) 
            throws ResourceNotFoundException, ValidationException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        // Get the current transaction
        PaymentTransaction transaction = findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        // Validate that the transaction can be voided
        if (!transaction.canVoid()) {
            throw new ValidationException("Transaction cannot be voided. Current status: " + transaction.getStatus());
        }
        
        // Begin a transaction if not already in one
        boolean localTransaction = false;
        if (transactionConnection.get() == null) {
            beginTransaction();
            localTransaction = true;
        }
        
        try {
            // Update the transaction status to VOIDED
            transaction.updateStatus(PaymentStatus.VOIDED);
            
            // Save the updated transaction
            PaymentTransaction updatedTransaction = update(transaction);
            
            // Create a payment event for the void
            createPaymentEvent(transaction.getTransactionId(), "VOID", 
                    PaymentStatus.AUTHORIZED, PaymentStatus.VOIDED, 
                    Map.of("voidReason", "Merchant initiated void"));
            
            // Commit the transaction if we started it
            if (localTransaction) {
                commitTransaction();
            }
            
            return updatedTransaction;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    rollbackTransaction();
                } catch (TransactionException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            
            // Rethrow the exception
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            } else if (e instanceof ValidationException) {
                throw (ValidationException) e;
            } else if (e instanceof ConnectionException) {
                throw (ConnectionException) e;
            } else if (e instanceof QueryExecutionException) {
                throw (QueryExecutionException) e;
            } else if (e instanceof TransactionException) {
                throw (TransactionException) e;
            } else {
                throw new QueryExecutionException("Error voiding transaction: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public List<PaymentTransaction> findTransactionsRequiringProcessing(Instant cutoffTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (cutoffTime == null) {
            cutoffTime = Instant.now().minusSeconds(300); // Default to 5 minutes ago
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            PaymentQueryBuilder queryBuilder = PaymentQueryBuilder.create()
                    .buildTransactionQuery()
                    .where("t.status = ?")
                    .addParameter(PaymentStatus.PROCESSING.name())
                    .and("t.updated_at < ?")
                    .addParameter(Timestamp.from(cutoffTime));
            
            // Apply additional filters if provided
            if (filterParams != null) {
                // Create a copy of filter params without status filter
                PaymentFilterParams otherFilters = new PaymentFilterParams();
                otherFilters.setOrganizationId(filterParams.getOrganizationId());
                otherFilters.setAccountId(filterParams.getAccountId());
                otherFilters.setDateRange(filterParams.getDateRange());
                otherFilters.setAmountRange(filterParams.getAmountRange());
                otherFilters.setMerchantId(filterParams.getMerchantId());
                otherFilters.setPaymentType(filterParams.getPaymentType());
                otherFilters.setSearchTerm(filterParams.getSearchTerm());
                otherFilters.setSortCriteria(filterParams.getSortCriteria());
                otherFilters.setPagination(filterParams.getPagination());
                
                queryBuilder.applyFilters(otherFilters);
            }
            
            stmt = queryBuilder.buildPreparedStatement(conn);
            rs = stmt.executeQuery();
            
            List<PaymentTransaction> transactions = new ArrayList<>();
            while (rs.next()) {
                transactions.add(mapRowToTransaction(rs));
            }
            
            return transactions;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error finding transactions requiring processing");
            return List.of(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public Map<PaymentStatus, Long> countByStatusForOrganization(UUID organizationId) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            String sql = "SELECT status, COUNT(*) as count FROM payment_transaction " +
                    "WHERE organization_id = ? GROUP BY status";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, organizationId);
            
            rs = stmt.executeQuery();
            
            Map<PaymentStatus, Long> statusCounts = new HashMap<>();
            while (rs.next()) {
                PaymentStatus status = PaymentStatus.valueOf(rs.getString("status"));
                long count = rs.getLong("count");
                statusCounts.put(status, count);
            }
            
            return statusCounts;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error counting transactions by status for organization: " + organizationId);
            return Map.of(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public BigDecimal calculateTotalAmountByStatusForOrganization(UUID organizationId, PaymentStatus status, String currency) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            StringBuilder sql = new StringBuilder(
                    "SELECT SUM(amount) as total FROM payment_transaction " +
                    "WHERE organization_id = ? AND status = ?");
            
            if (currency != null && !currency.isEmpty()) {
                sql.append(" AND currency = ?");
            }
            
            stmt = conn.prepareStatement(sql.toString());
            stmt.setObject(1, organizationId);
            stmt.setString(2, status.name());
            
            if (currency != null && !currency.isEmpty()) {
                stmt.setString(3, currency);
            }
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            } else {
                return BigDecimal.ZERO;
            }
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error calculating total amount by status for organization: " + organizationId);
            return BigDecimal.ZERO; // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public List<PaymentTransaction> findByFullTextSearch(String searchText, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (searchText == null || searchText.isEmpty()) {
            throw new IllegalArgumentException("Search text cannot be null or empty");
        }
        
        // Create filter params if null
        PaymentFilterParams params = filterParams != null ? filterParams : new PaymentFilterParams();
        
        // Set search term in filter params
        params.setSearchTerm(searchText);
        
        // Use the general query method with the updated filter params
        return query(params);
    }
    
    @Override
    public Map<LocalDate, Long> getTransactionCountByDay(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (dateRange == null || dateRange.getStartDate() == null || dateRange.getEndDate() == null) {
            throw new IllegalArgumentException("Date range with start and end dates is required");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            String sql = "SELECT DATE(created_at) as day, COUNT(*) as count " +
                    "FROM payment_transaction " +
                    "WHERE organization_id = ? AND created_at BETWEEN ? AND ? " +
                    "GROUP BY DATE(created_at) " +
                    "ORDER BY day";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, organizationId);
            stmt.setTimestamp(2, Timestamp.valueOf(dateRange.getStartDate()));
            stmt.setTimestamp(3, Timestamp.valueOf(dateRange.getEndDate()));
            
            rs = stmt.executeQuery();
            
            Map<LocalDate, Long> countByDay = new HashMap<>();
            while (rs.next()) {
                LocalDate day = rs.getDate("day").toLocalDate();
                long count = rs.getLong("count");
                countByDay.put(day, count);
            }
            
            return countByDay;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error getting transaction count by day for organization: " + organizationId);
            return Map.of(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public Map<PaymentType, Long> getTransactionCountByPaymentType(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            StringBuilder sql = new StringBuilder(
                    "SELECT payment_type, COUNT(*) as count " +
                    "FROM payment_transaction " +
                    "WHERE organization_id = ?");
            
            if (dateRange != null && dateRange.getStartDate() != null && dateRange.getEndDate() != null) {
                sql.append(" AND created_at BETWEEN ? AND ?");
            }
            
            sql.append(" GROUP BY payment_type");
            
            stmt = conn.prepareStatement(sql.toString());
            stmt.setObject(1, organizationId);
            
            if (dateRange != null && dateRange.getStartDate() != null && dateRange.getEndDate() != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(dateRange.getStartDate()));
                stmt.setTimestamp(3, Timestamp.valueOf(dateRange.getEndDate()));
            }
            
            rs = stmt.executeQuery();
            
            Map<PaymentType, Long> countByType = new HashMap<>();
            while (rs.next()) {
                PaymentType paymentType = PaymentType.valueOf(rs.getString("payment_type"));
                long count = rs.getLong("count");
                countByType.put(paymentType, count);
            }
            
            return countByType;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error getting transaction count by payment type for organization: " + organizationId);
            return Map.of(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public Map<PaymentStatus, Long> getTransactionCountByStatus(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            
            StringBuilder sql = new StringBuilder(
                    "SELECT status, COUNT(*) as count " +
                    "FROM payment_transaction " +
                    "WHERE organization_id = ?");
            
            if (dateRange != null && dateRange.getStartDate() != null && dateRange.getEndDate() != null) {
                sql.append(" AND created_at BETWEEN ? AND ?");
            }
            
            sql.append(" GROUP BY status");
            
            stmt = conn.prepareStatement(sql.toString());
            stmt.setObject(1, organizationId);
            
            if (dateRange != null && dateRange.getStartDate() != null && dateRange.getEndDate() != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(dateRange.getStartDate()));
                stmt.setTimestamp(3, Timestamp.valueOf(dateRange.getEndDate()));
            }
            
            rs = stmt.executeQuery();
            
            Map<PaymentStatus, Long> countByStatus = new HashMap<>();
            while (rs.next()) {
                PaymentStatus status = PaymentStatus.valueOf(rs.getString("status"));
                long count = rs.getLong("count");
                countByStatus.put(status, count);
            }
            
            return countByStatus;
            
        } catch (SQLException e) {
            handleSqlException(conn, e, "Error getting transaction count by status for organization: " + organizationId);
            return Map.of(); // This line will never be reached due to exception handling
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationIdForAllAccounts(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        // This is the same as findByOrganizationId since we're already filtering by organization ID only
        return findByOrganizationId(organizationId, filterParams);
    }
    
    @Override
    public List<PaymentTransaction> findForAllOrganizations(PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        // Use the general query method without organization ID filter
        return query(filterParams);
    }
    
    /**
     * Creates a payment event record for transaction history tracking.
     *
     * @param transactionId The transaction ID
     * @param eventType The event type
     * @param previousStatus The previous status
     * @param newStatus The new status
     * @param eventData Additional event data
     * @throws SQLException if a database access error occurs
     */
    private void createPaymentEvent(UUID transactionId, String eventType, 
            PaymentStatus previousStatus, PaymentStatus newStatus, Map<String, String> eventData) 
            throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = null;
        
        try {
            String sql = "INSERT INTO payment_event " +
                    "(event_id, transaction_id, event_type, previous_status, new_status, event_data, created_at, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, UUID.randomUUID());
            stmt.setObject(2, transactionId);
            stmt.setString(3, eventType);
            stmt.setString(4, previousStatus.name());
            stmt.setString(5, newStatus.name());
            
            // Convert event data to JSON string
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : eventData.entrySet()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            jsonBuilder.append("}");
            
            stmt.setString(6, jsonBuilder.toString());
            stmt.setTimestamp(7, Timestamp.from(Instant.now()));
            stmt.setString(8, "system"); // Default to system user
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error creating payment event for transaction: {}", transactionId, e);
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }
    
    /**
     * Handles SQL exceptions by rolling back transactions and throwing appropriate exceptions.
     *
     * @param conn The database connection
     * @param e The SQL exception
     * @param message The error message
     * @throws ConnectionException if a connection error occurs
     * @throws QueryExecutionException if a query execution error occurs
     */
    private void handleSqlException(Connection conn, SQLException e, String message) 
            throws ConnectionException, QueryExecutionException {
        logger.error(message, e);
        
        // Rollback transaction if auto-commit is disabled
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                logger.error("Failed to rollback transaction", rollbackEx);
            }
        }
        
        // Determine the type of exception to throw
        if (e.getSQLState() != null) {
            String sqlState = e.getSQLState();
            
            // Connection-related errors (class 08)
            if (sqlState.startsWith("08")) {
                throw new ConnectionException(message + ": " + e.getMessage(), e);
            }
            
            // Constraint violations (class 23)
            if (sqlState.startsWith("23")) {
                throw new ValidationException(message + ": " + e.getMessage(), e);
            }
        }
        
        // Default to query execution exception
        throw new QueryExecutionException(message + ": " + e.getMessage(), e);
    }
    
    /**
     * Closes the connection pool when this DAO is no longer needed.
     * Should be called during application shutdown.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Closed payment transaction DAO connection pool");
        }
    }
}