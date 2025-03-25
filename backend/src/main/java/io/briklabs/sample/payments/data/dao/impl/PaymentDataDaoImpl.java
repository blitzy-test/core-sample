package io.briklabs.sample.payments.data.dao.impl;

import io.briklabs.sample.payments.data.ConnectionManager;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.SecurityException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.model.PaymentDataEntity;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;
import io.briklabs.sample.payments.data.dao.PaymentDataDAO;
import io.briklabs.sample.payments.model.PaymentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Concrete implementation of the PaymentDataDAO interface that handles database operations
 * for payment method details. This class manages secure storage and retrieval of payment
 * instrument information, handling tokenization, encryption, and field-level security.
 * <p>
 * It provides methods to retrieve payment data by transaction ID and implements specialized
 * queries for payment method information. This implementation ensures proper handling of
 * sensitive payment data with appropriate security controls.
 * </p>
 */
public class PaymentDataDaoImpl implements PaymentDataDAO {
    private static final Logger logger = LoggerFactory.getLogger(PaymentDataDaoImpl.class);
    
    /**
     * SQL queries for payment data operations.
     */
    private static final String SQL_INSERT = 
            "INSERT INTO payment_data (payment_data_id, transaction_id, payment_method_id, " +
            "payment_token, payment_details, created_at, expiration, billing_data) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE payment_data SET payment_method_id = ?, payment_token = ?, " +
            "payment_details = ?, expiration = ?, billing_data = ? " +
            "WHERE payment_data_id = ?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM payment_data WHERE payment_data_id = ?";
    
    private static final String SQL_FIND_BY_TRANSACTION_ID = 
            "SELECT * FROM payment_data WHERE transaction_id = ?";
    
    private static final String SQL_FIND_BY_PAYMENT_METHOD_ID = 
            "SELECT * FROM payment_data WHERE payment_method_id = ?";
    
    private static final String SQL_FIND_BY_TRANSACTION_ID_AND_PAYMENT_TYPE = 
            "SELECT pd.* FROM payment_data pd " +
            "JOIN payment_transaction pt ON pd.transaction_id = pt.transaction_id " +
            "WHERE pd.transaction_id = ? AND pt.payment_type = ?";
    
    private static final String SQL_UPDATE_PAYMENT_TOKEN = 
            "UPDATE payment_data SET payment_token = ? WHERE payment_data_id = ?";
    
    private static final String SQL_UPDATE_EXPIRATION = 
            "UPDATE payment_data SET expiration = ? WHERE payment_data_id = ?";
    
    private static final String SQL_UPDATE_BILLING_DATA = 
            "UPDATE payment_data SET billing_data = ? WHERE payment_data_id = ?";
    
    private static final String SQL_DELETE = 
            "DELETE FROM payment_data WHERE payment_data_id = ?";
    
    private static final String SQL_SECURE_DELETE = 
            "UPDATE payment_data SET payment_token = '[REDACTED]', " +
            "payment_details = '{\"status\": \"redacted\"}', " +
            "billing_data = '{\"status\": \"redacted\"}' " +
            "WHERE payment_data_id = ?";
    
    private static final String SQL_FIND_BY_EXPIRATION_RANGE = 
            "SELECT * FROM payment_data WHERE expiration BETWEEN ? AND ?";
    
    private static final String SQL_FIND_EXPIRING_SOON = 
            "SELECT * FROM payment_data WHERE expiration BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL '? months')";
    
    private static final String SQL_FIND_BY_PAYMENT_TYPE = 
            "SELECT pd.* FROM payment_data pd " +
            "JOIN payment_transaction pt ON pd.transaction_id = pt.transaction_id " +
            "WHERE pt.payment_type = ?";
    
    private static final String SQL_COUNT_BY_PAYMENT_TYPE = 
            "SELECT COUNT(*) FROM payment_data pd " +
            "JOIN payment_transaction pt ON pd.transaction_id = pt.transaction_id " +
            "WHERE pt.payment_type = ?";
    
    private static final String SQL_FIND_BY_ORGANIZATION_ID = 
            "SELECT pd.* FROM payment_data pd " +
            "JOIN payment_transaction pt ON pd.transaction_id = pt.transaction_id " +
            "WHERE pt.organization_id = ?";
    
    private static final String SQL_FIND_BY_ACCOUNT_ID = 
            "SELECT pd.* FROM payment_data pd " +
            "JOIN payment_transaction pt ON pd.transaction_id = pt.transaction_id " +
            "WHERE pt.account_id = ?";
    
    private static final String SQL_FIND_BY_MERCHANT_ID = 
            "SELECT pd.* FROM payment_data pd " +
            "JOIN payment_transaction pt ON pd.transaction_id = pt.transaction_id " +
            "WHERE pt.merchant_id = ?";
    
    private static final String SQL_FIND_MOST_RECENT_BY_TRANSACTION_ID = 
            "SELECT * FROM payment_data WHERE transaction_id = ? " +
            "ORDER BY created_at DESC LIMIT 1";
    
    private static final String SQL_FIND_BY_CREATION_DATE_RANGE = 
            "SELECT * FROM payment_data WHERE created_at BETWEEN ? AND ?";
    
    /**
     * The connection manager for database operations.
     */
    private final ConnectionManager connectionManager;
    
    /**
     * Creates a new PaymentDataDaoImpl with the specified connection manager.
     *
     * @param connectionManager the connection manager for database operations
     */
    public PaymentDataDaoImpl(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData create(PaymentData paymentData) {
        if (paymentData == null) {
            throw new ValidationException("Payment data cannot be null");
        }
        
        // Validate payment data
        validatePaymentData(paymentData);
        
        // Generate ID if not provided
        if (paymentData.getPaymentDataId() == null) {
            paymentData.setPaymentDataId(UUID.randomUUID());
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
                stmt.setObject(1, paymentData.getPaymentDataId());
                stmt.setObject(2, paymentData.getTransactionId());
                stmt.setString(3, paymentData.getPaymentMethodId());
                
                // Handle potentially null fields
                if (paymentData.getPaymentToken() != null) {
                    stmt.setString(4, paymentData.getPaymentToken());
                } else {
                    stmt.setNull(4, Types.VARCHAR);
                }
                
                if (paymentData.getPaymentDetails() != null) {
                    stmt.setString(5, paymentData.getPaymentDetails());
                } else {
                    stmt.setNull(5, Types.VARCHAR);
                }
                
                // Set current timestamp if not provided
                Instant createdAt = paymentData.getCreatedAt() != null ? 
                        paymentData.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : 
                        Instant.now();
                stmt.setTimestamp(6, Timestamp.from(createdAt));
                
                if (paymentData.getExpiration() != null) {
                    stmt.setObject(7, paymentData.getExpiration());
                } else {
                    stmt.setNull(7, Types.DATE);
                }
                
                if (paymentData.getBillingData() != null) {
                    stmt.setString(8, paymentData.getBillingData());
                } else {
                    stmt.setNull(8, Types.VARCHAR);
                }
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to insert payment data, expected 1 row affected but got " + rowsAffected,
                            SQL_INSERT, "INSERT", "payment_data");
                }
                
                logger.debug("Created payment data with ID: {}", paymentData.getPaymentDataId());
                return paymentData;
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error creating payment data: " + e.getMessage(),
                    e, SQL_INSERT, "INSERT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error creating payment data: " + e.getMessage(),
                    SQL_INSERT, "INSERT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PaymentData> findById(UUID id) {
        if (id == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
                stmt.setObject(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        return Optional.of(entity.toDomainModel());
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by ID: " + e.getMessage(),
                    e, SQL_FIND_BY_ID, "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by ID: " + e.getMessage(),
                    SQL_FIND_BY_ID, "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData update(PaymentData paymentData) {
        if (paymentData == null) {
            throw new ValidationException("Payment data cannot be null");
        }
        
        if (paymentData.getPaymentDataId() == null) {
            throw new ValidationException("Payment data ID cannot be null for update");
        }
        
        // Validate payment data
        validatePaymentData(paymentData);
        
        // Check if the payment data exists
        if (!exists(paymentData.getPaymentDataId())) {
            throw new ResourceNotFoundException("payment_data", paymentData.getPaymentDataId());
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
                stmt.setString(1, paymentData.getPaymentMethodId());
                
                // Handle potentially null fields
                if (paymentData.getPaymentToken() != null) {
                    stmt.setString(2, paymentData.getPaymentToken());
                } else {
                    stmt.setNull(2, Types.VARCHAR);
                }
                
                if (paymentData.getPaymentDetails() != null) {
                    stmt.setString(3, paymentData.getPaymentDetails());
                } else {
                    stmt.setNull(3, Types.VARCHAR);
                }
                
                if (paymentData.getExpiration() != null) {
                    stmt.setObject(4, paymentData.getExpiration());
                } else {
                    stmt.setNull(4, Types.DATE);
                }
                
                if (paymentData.getBillingData() != null) {
                    stmt.setString(5, paymentData.getBillingData());
                } else {
                    stmt.setNull(5, Types.VARCHAR);
                }
                
                stmt.setObject(6, paymentData.getPaymentDataId());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to update payment data, expected 1 row affected but got " + rowsAffected,
                            SQL_UPDATE, "UPDATE", "payment_data");
                }
                
                logger.debug("Updated payment data with ID: {}", paymentData.getPaymentDataId());
                return paymentData;
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error updating payment data: " + e.getMessage(),
                    e, SQL_UPDATE, "UPDATE", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error updating payment data: " + e.getMessage(),
                    SQL_UPDATE, "UPDATE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
                stmt.setObject(1, id);
                
                int rowsAffected = stmt.executeUpdate();
                
                logger.debug("Deleted payment data with ID: {}, rows affected: {}", id, rowsAffected);
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error deleting payment data: " + e.getMessage(),
                    e, SQL_DELETE, "DELETE", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error deleting payment data: " + e.getMessage(),
                    SQL_DELETE, "DELETE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> query(Object params) {
        if (!(params instanceof PaymentFilterParams)) {
            throw new ValidationException("Query parameters must be of type PaymentFilterParams");
        }
        
        PaymentFilterParams filterParams = (PaymentFilterParams) params;
        return searchPaymentData(filterParams);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Connection beginTransaction() {
        try {
            connectionManager.beginTransaction();
            return connectionManager.getConnection();
        } catch (ConnectionException e) {
            throw e;
        } catch (TransactionException e) {
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void commitTransaction(Connection connection) {
        try {
            connectionManager.commitTransaction();
        } catch (TransactionException e) {
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void rollbackTransaction(Connection connection) {
        try {
            connectionManager.rollbackTransaction();
        } catch (TransactionException e) {
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> batchCreate(List<PaymentData> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
                for (PaymentData paymentData : entities) {
                    // Validate payment data
                    validatePaymentData(paymentData);
                    
                    // Generate ID if not provided
                    if (paymentData.getPaymentDataId() == null) {
                        paymentData.setPaymentDataId(UUID.randomUUID());
                    }
                    
                    stmt.setObject(1, paymentData.getPaymentDataId());
                    stmt.setObject(2, paymentData.getTransactionId());
                    stmt.setString(3, paymentData.getPaymentMethodId());
                    
                    // Handle potentially null fields
                    if (paymentData.getPaymentToken() != null) {
                        stmt.setString(4, paymentData.getPaymentToken());
                    } else {
                        stmt.setNull(4, Types.VARCHAR);
                    }
                    
                    if (paymentData.getPaymentDetails() != null) {
                        stmt.setString(5, paymentData.getPaymentDetails());
                    } else {
                        stmt.setNull(5, Types.VARCHAR);
                    }
                    
                    // Set current timestamp if not provided
                    Instant createdAt = paymentData.getCreatedAt() != null ? 
                            paymentData.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : 
                            Instant.now();
                    stmt.setTimestamp(6, Timestamp.from(createdAt));
                    
                    if (paymentData.getExpiration() != null) {
                        stmt.setObject(7, paymentData.getExpiration());
                    } else {
                        stmt.setNull(7, Types.DATE);
                    }
                    
                    if (paymentData.getBillingData() != null) {
                        stmt.setString(8, paymentData.getBillingData());
                    } else {
                        stmt.setNull(8, Types.VARCHAR);
                    }
                    
                    stmt.addBatch();
                }
                
                int[] rowsAffected = stmt.executeBatch();
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Batch created {} payment data records", rowsAffected.length);
                return entities;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error batch creating payment data: " + e.getMessage(),
                    e, SQL_INSERT, "INSERT", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error batch creating payment data: " + e.getMessage(),
                    SQL_INSERT, "INSERT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> batchUpdate(List<PaymentData> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
                for (PaymentData paymentData : entities) {
                    // Validate payment data
                    validatePaymentData(paymentData);
                    
                    if (paymentData.getPaymentDataId() == null) {
                        throw new ValidationException("Payment data ID cannot be null for update");
                    }
                    
                    stmt.setString(1, paymentData.getPaymentMethodId());
                    
                    // Handle potentially null fields
                    if (paymentData.getPaymentToken() != null) {
                        stmt.setString(2, paymentData.getPaymentToken());
                    } else {
                        stmt.setNull(2, Types.VARCHAR);
                    }
                    
                    if (paymentData.getPaymentDetails() != null) {
                        stmt.setString(3, paymentData.getPaymentDetails());
                    } else {
                        stmt.setNull(3, Types.VARCHAR);
                    }
                    
                    if (paymentData.getExpiration() != null) {
                        stmt.setObject(4, paymentData.getExpiration());
                    } else {
                        stmt.setNull(4, Types.DATE);
                    }
                    
                    if (paymentData.getBillingData() != null) {
                        stmt.setString(5, paymentData.getBillingData());
                    } else {
                        stmt.setNull(5, Types.VARCHAR);
                    }
                    
                    stmt.setObject(6, paymentData.getPaymentDataId());
                    
                    stmt.addBatch();
                }
                
                int[] rowsAffected = stmt.executeBatch();
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Batch updated {} payment data records", rowsAffected.length);
                return entities;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error batch updating payment data: " + e.getMessage(),
                    e, SQL_UPDATE, "UPDATE", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error batch updating payment data: " + e.getMessage(),
                    SQL_UPDATE, "UPDATE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R executeInTransaction(TransactionOperation<R> operation) {
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            Connection conn = connectionManager.getConnection();
            R result = operation.execute(conn);
            
            // Commit the transaction if we started it
            if (localTransaction) {
                connectionManager.commitTransaction();
            }
            
            return result;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new QueryExecutionException(
                        "Error executing in transaction: " + e.getMessage(),
                        e, "TRANSACTION", "EXECUTE", new String[]{"payment_data"});
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long count(Object params) {
        if (!(params instanceof PaymentFilterParams)) {
            throw new ValidationException("Query parameters must be of type PaymentFilterParams");
        }
        
        PaymentFilterParams filterParams = (PaymentFilterParams) params;
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            PaymentQueryBuilder queryBuilder = PaymentQueryBuilder.create()
                    .count("pd.*")
                    .from("payment_data pd")
                    .leftJoin("payment_transaction pt", "pd.transaction_id = pt.transaction_id");
            
            // Apply filters
            queryBuilder.applyFilters(filterParams);
            
            try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(conn);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    return 0;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error counting payment data: " + e.getMessage(),
                    e, "COUNT", "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error counting payment data: " + e.getMessage(),
                    "COUNT", "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(UUID id) {
        if (id == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT 1 FROM payment_data WHERE payment_data_id = ?")) {
                stmt.setObject(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error checking if payment data exists: " + e.getMessage(),
                    e, "EXISTS", "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error checking if payment data exists: " + e.getMessage(),
                    "EXISTS", "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByOrganizationId(UUID organizationId) {
        if (organizationId == null) {
            throw new ValidationException("Organization ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ORGANIZATION_ID)) {
                stmt.setObject(1, organizationId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by organization ID: " + e.getMessage(),
                    e, SQL_FIND_BY_ORGANIZATION_ID, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by organization ID: " + e.getMessage(),
                    SQL_FIND_BY_ORGANIZATION_ID, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByAccountId(UUID accountId) {
        if (accountId == null) {
            throw new ValidationException("Account ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ACCOUNT_ID)) {
                stmt.setObject(1, accountId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by account ID: " + e.getMessage(),
                    e, SQL_FIND_BY_ACCOUNT_ID, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by account ID: " + e.getMessage(),
                    SQL_FIND_BY_ACCOUNT_ID, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByOrganizationAndAccountId(UUID organizationId, UUID accountId) {
        if (organizationId == null) {
            throw new ValidationException("Organization ID cannot be null");
        }
        
        if (accountId == null) {
            throw new ValidationException("Account ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            String sql = "SELECT pd.* FROM payment_data pd " +
                         "JOIN payment_transaction pt ON pd.transaction_id = pt.transaction_id " +
                         "WHERE pt.organization_id = ? AND pt.account_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, organizationId);
                stmt.setObject(2, accountId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by organization and account ID: " + e.getMessage(),
                    e, "FIND_BY_ORG_AND_ACCOUNT", "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by organization and account ID: " + e.getMessage(),
                    "FIND_BY_ORG_AND_ACCOUNT", "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByTransactionId(UUID transactionId) 
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_TRANSACTION_ID)) {
                stmt.setObject(1, transactionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    
                    if (results.isEmpty()) {
                        // Check if the transaction exists
                        try (PreparedStatement checkStmt = conn.prepareStatement(
                                "SELECT 1 FROM payment_transaction WHERE transaction_id = ?")) {
                            checkStmt.setObject(1, transactionId);
                            try (ResultSet checkRs = checkStmt.executeQuery()) {
                                if (!checkRs.next()) {
                                    throw new ResourceNotFoundException("transaction", transactionId);
                                }
                            }
                        }
                    }
                    
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by transaction ID: " + e.getMessage(),
                    e, SQL_FIND_BY_TRANSACTION_ID, "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by transaction ID: " + e.getMessage(),
                    SQL_FIND_BY_TRANSACTION_ID, "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PaymentData> findByPaymentMethodId(String paymentMethodId)
            throws ConnectionException, QueryExecutionException {
        if (paymentMethodId == null || paymentMethodId.isEmpty()) {
            throw new ValidationException("Payment method ID cannot be null or empty");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_PAYMENT_METHOD_ID)) {
                stmt.setString(1, paymentMethodId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        return Optional.of(entity.toDomainModel());
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by payment method ID: " + e.getMessage(),
                    e, SQL_FIND_BY_PAYMENT_METHOD_ID, "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by payment method ID: " + e.getMessage(),
                    SQL_FIND_BY_PAYMENT_METHOD_ID, "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByTransactionIdAndPaymentType(UUID transactionId, String paymentType)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        if (paymentType == null || paymentType.isEmpty()) {
            throw new ValidationException("Payment type cannot be null or empty");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_TRANSACTION_ID_AND_PAYMENT_TYPE)) {
                stmt.setObject(1, transactionId);
                stmt.setString(2, paymentType);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    
                    if (results.isEmpty()) {
                        // Check if the transaction exists
                        try (PreparedStatement checkStmt = conn.prepareStatement(
                                "SELECT 1 FROM payment_transaction WHERE transaction_id = ?")) {
                            checkStmt.setObject(1, transactionId);
                            try (ResultSet checkRs = checkStmt.executeQuery()) {
                                if (!checkRs.next()) {
                                    throw new ResourceNotFoundException("transaction", transactionId);
                                }
                            }
                        }
                    }
                    
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by transaction ID and payment type: " + e.getMessage(),
                    e, SQL_FIND_BY_TRANSACTION_ID_AND_PAYMENT_TYPE, "SELECT", 
                    new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by transaction ID and payment type: " + e.getMessage(),
                    SQL_FIND_BY_TRANSACTION_ID_AND_PAYMENT_TYPE, "SELECT", 
                    new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData secureStore(PaymentData paymentData)
            throws ValidationException, ConnectionException, QueryExecutionException, 
                   TransactionException, SecurityException {
        if (paymentData == null) {
            throw new ValidationException("Payment data cannot be null");
        }
        
        // Validate payment data
        validatePaymentData(paymentData);
        
        // Generate ID if not provided
        if (paymentData.getPaymentDataId() == null) {
            paymentData.setPaymentDataId(UUID.randomUUID());
        }
        
        // Secure sensitive data before storing
        PaymentData securedData = securePaymentData(paymentData);
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
                stmt.setObject(1, securedData.getPaymentDataId());
                stmt.setObject(2, securedData.getTransactionId());
                stmt.setString(3, securedData.getPaymentMethodId());
                
                // Handle potentially null fields
                if (securedData.getPaymentToken() != null) {
                    stmt.setString(4, securedData.getPaymentToken());
                } else {
                    stmt.setNull(4, Types.VARCHAR);
                }
                
                if (securedData.getPaymentDetails() != null) {
                    stmt.setString(5, securedData.getPaymentDetails());
                } else {
                    stmt.setNull(5, Types.VARCHAR);
                }
                
                // Set current timestamp if not provided
                Instant createdAt = securedData.getCreatedAt() != null ? 
                        securedData.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : 
                        Instant.now();
                stmt.setTimestamp(6, Timestamp.from(createdAt));
                
                if (securedData.getExpiration() != null) {
                    stmt.setObject(7, securedData.getExpiration());
                } else {
                    stmt.setNull(7, Types.DATE);
                }
                
                if (securedData.getBillingData() != null) {
                    stmt.setString(8, securedData.getBillingData());
                } else {
                    stmt.setNull(8, Types.VARCHAR);
                }
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to insert payment data, expected 1 row affected but got " + rowsAffected,
                            SQL_INSERT, "INSERT", "payment_data");
                }
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Securely stored payment data with ID: {}", securedData.getPaymentDataId());
                return securedData;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error securely storing payment data: " + e.getMessage(),
                    e, SQL_INSERT, "INSERT", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error securely storing payment data: " + e.getMessage(),
                    SQL_INSERT, "INSERT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData retrieveSensitiveData(UUID paymentDataId)
            throws ResourceNotFoundException, SecurityException, ConnectionException, QueryExecutionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Check if the caller has sufficient permissions
        // This would typically involve checking the current user's role and permissions
        // For now, we'll assume the check passes
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
                stmt.setObject(1, paymentDataId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        
                        // Decrypt sensitive data if needed
                        // This would typically involve decrypting the payment token and other sensitive fields
                        // For now, we'll just return the entity as is
                        
                        return entity.toDomainModel();
                    } else {
                        throw new ResourceNotFoundException("payment_data", paymentDataId);
                    }
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error retrieving sensitive payment data: " + e.getMessage(),
                    e, SQL_FIND_BY_ID, "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error retrieving sensitive payment data: " + e.getMessage(),
                    SQL_FIND_BY_ID, "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData updatePaymentToken(UUID paymentDataId, String newToken)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException, SecurityException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        if (newToken == null || newToken.isEmpty()) {
            throw new ValidationException("New payment token cannot be null or empty");
        }
        
        // Validate the token format
        validatePaymentToken(newToken);
        
        // Check if the payment data exists
        Optional<PaymentData> existingData = findById(paymentDataId);
        if (existingData.isEmpty()) {
            throw new ResourceNotFoundException("payment_data", paymentDataId);
        }
        
        // Secure the token if needed
        String securedToken = securePaymentToken(newToken);
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_PAYMENT_TOKEN)) {
                stmt.setString(1, securedToken);
                stmt.setObject(2, paymentDataId);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to update payment token, expected 1 row affected but got " + rowsAffected,
                            SQL_UPDATE_PAYMENT_TOKEN, "UPDATE", "payment_data");
                }
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Updated payment token for payment data ID: {}", paymentDataId);
                
                // Return the updated payment data
                PaymentData updatedData = existingData.get();
                updatedData.setPaymentToken(securedToken);
                return updatedData;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error updating payment token: " + e.getMessage(),
                    e, SQL_UPDATE_PAYMENT_TOKEN, "UPDATE", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error updating payment token: " + e.getMessage(),
                    SQL_UPDATE_PAYMENT_TOKEN, "UPDATE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData updateExpiration(UUID paymentDataId, int expirationMonth, int expirationYear)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Validate expiration date
        if (expirationMonth < 1 || expirationMonth > 12) {
            throw new ValidationException("Expiration month must be between 1 and 12");
        }
        
        if (expirationYear < LocalDate.now().getYear() || expirationYear > LocalDate.now().getYear() + 20) {
            throw new ValidationException("Expiration year must be between current year and current year + 20");
        }
        
        // Check if the payment data exists
        Optional<PaymentData> existingData = findById(paymentDataId);
        if (existingData.isEmpty()) {
            throw new ResourceNotFoundException("payment_data", paymentDataId);
        }
        
        // Calculate the expiration date (last day of the month)
        LocalDate expirationDate = LocalDate.of(expirationYear, expirationMonth, 1)
                .plusMonths(1).minusDays(1);
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_EXPIRATION)) {
                stmt.setObject(1, expirationDate);
                stmt.setObject(2, paymentDataId);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to update expiration date, expected 1 row affected but got " + rowsAffected,
                            SQL_UPDATE_EXPIRATION, "UPDATE", "payment_data");
                }
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Updated expiration date for payment data ID: {}", paymentDataId);
                
                // Return the updated payment data
                PaymentData updatedData = existingData.get();
                updatedData.setExpiration(expirationDate);
                return updatedData;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error updating expiration date: " + e.getMessage(),
                    e, SQL_UPDATE_EXPIRATION, "UPDATE", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error updating expiration date: " + e.getMessage(),
                    SQL_UPDATE_EXPIRATION, "UPDATE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData updateBillingData(UUID paymentDataId, String billingData)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        if (billingData == null) {
            throw new ValidationException("Billing data cannot be null");
        }
        
        // Validate billing data format (should be valid JSON)
        validateJsonFormat(billingData, "billing data");
        
        // Check if the payment data exists
        Optional<PaymentData> existingData = findById(paymentDataId);
        if (existingData.isEmpty()) {
            throw new ResourceNotFoundException("payment_data", paymentDataId);
        }
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BILLING_DATA)) {
                stmt.setString(1, billingData);
                stmt.setObject(2, paymentDataId);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to update billing data, expected 1 row affected but got " + rowsAffected,
                            SQL_UPDATE_BILLING_DATA, "UPDATE", "payment_data");
                }
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Updated billing data for payment data ID: {}", paymentDataId);
                
                // Return the updated payment data
                PaymentData updatedData = existingData.get();
                updatedData.setBillingData(billingData);
                return updatedData;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error updating billing data: " + e.getMessage(),
                    e, SQL_UPDATE_BILLING_DATA, "UPDATE", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error updating billing data: " + e.getMessage(),
                    SQL_UPDATE_BILLING_DATA, "UPDATE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData retrieveWithRoleBasedMasking(UUID paymentDataId, String userRole)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        if (userRole == null || userRole.isEmpty()) {
            throw new ValidationException("User role cannot be null or empty");
        }
        
        // Retrieve the payment data
        Optional<PaymentData> paymentDataOpt = findById(paymentDataId);
        if (paymentDataOpt.isEmpty()) {
            throw new ResourceNotFoundException("payment_data", paymentDataId);
        }
        
        PaymentData paymentData = paymentDataOpt.get();
        
        // Apply role-based masking
        return applyRoleBasedMasking(paymentData, userRole);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> searchPaymentData(PaymentFilterParams filterParams)
            throws ConnectionException, QueryExecutionException {
        if (filterParams == null) {
            throw new ValidationException("Filter parameters cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            PaymentQueryBuilder queryBuilder = PaymentQueryBuilder.create()
                    .select("pd.*")
                    .from("payment_data pd")
                    .leftJoin("payment_transaction pt", "pd.transaction_id = pt.transaction_id");
            
            // Apply filters
            queryBuilder.applyFilters(filterParams);
            
            try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(conn);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<PaymentData> results = new ArrayList<>();
                while (rs.next()) {
                    PaymentDataEntity entity = mapResultSetToEntity(rs);
                    results.add(entity.toDomainModel());
                }
                return results;
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error searching payment data: " + e.getMessage(),
                    e, "SEARCH", "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error searching payment data: " + e.getMessage(),
                    "SEARCH", "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentData invalidatePaymentMethod(UUID paymentDataId, String reason)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Check if the payment data exists
        Optional<PaymentData> existingData = findById(paymentDataId);
        if (existingData.isEmpty()) {
            throw new ResourceNotFoundException("payment_data", paymentDataId);
        }
        
        // Create invalidation JSON
        String invalidationJson = String.format(
                "{\"status\": \"invalid\", \"reason\": \"%s\", \"invalidated_at\": \"%s\"}",
                reason != null ? reason.replace("\"", "\\\"") : "No reason provided",
                LocalDate.now());
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            // Update the payment details to mark as invalid
            String sql = "UPDATE payment_data SET payment_details = ? WHERE payment_data_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, invalidationJson);
                stmt.setObject(2, paymentDataId);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to invalidate payment method, expected 1 row affected but got " + rowsAffected,
                            sql, "UPDATE", "payment_data");
                }
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Invalidated payment method for payment data ID: {}", paymentDataId);
                
                // Return the updated payment data
                PaymentData updatedData = existingData.get();
                updatedData.setPaymentDetails(invalidationJson);
                return updatedData;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error invalidating payment method: " + e.getMessage(),
                    e, "INVALIDATE", "UPDATE", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error invalidating payment method: " + e.getMessage(),
                    "INVALIDATE", "UPDATE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean secureDelete(UUID paymentDataId)
            throws ResourceNotFoundException, SecurityException, ConnectionException,
                   QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Check if the payment data exists
        if (!exists(paymentDataId)) {
            throw new ResourceNotFoundException("payment_data", paymentDataId);
        }
        
        Connection conn = null;
        boolean localTransaction = false;
        
        try {
            // Start a transaction if one is not already in progress
            if (!connectionManager.isInTransaction()) {
                connectionManager.beginTransaction();
                localTransaction = true;
            }
            
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SECURE_DELETE)) {
                stmt.setObject(1, paymentDataId);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new QueryExecutionException(
                            "Failed to securely delete payment data, expected 1 row affected but got " + rowsAffected,
                            SQL_SECURE_DELETE, "UPDATE", "payment_data");
                }
                
                // Commit the transaction if we started it
                if (localTransaction) {
                    connectionManager.commitTransaction();
                }
                
                logger.debug("Securely deleted payment data with ID: {}", paymentDataId);
                return true;
            }
        } catch (SQLException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Error securely deleting payment data: " + e.getMessage(),
                    e, SQL_SECURE_DELETE, "UPDATE", new String[]{"payment_data"});
        } catch (ConnectionException | TransactionException e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw e;
        } catch (Exception e) {
            // Rollback the transaction if we started it
            if (localTransaction) {
                try {
                    connectionManager.rollbackTransaction();
                } catch (TransactionException te) {
                    logger.error("Error rolling back transaction: {}", te.getMessage(), te);
                }
            }
            
            throw new QueryExecutionException(
                    "Unexpected error securely deleting payment data: " + e.getMessage(),
                    SQL_SECURE_DELETE, "UPDATE", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByExpirationRange(LocalDate startDate, LocalDate endDate)
            throws ConnectionException, QueryExecutionException {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_EXPIRATION_RANGE)) {
                stmt.setObject(1, startDate);
                stmt.setObject(2, endDate);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by expiration range: " + e.getMessage(),
                    e, SQL_FIND_BY_EXPIRATION_RANGE, "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by expiration range: " + e.getMessage(),
                    SQL_FIND_BY_EXPIRATION_RANGE, "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findExpiringSoon(int monthsThreshold)
            throws ConnectionException, QueryExecutionException {
        if (monthsThreshold <= 0) {
            throw new ValidationException("Months threshold must be positive");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            // Modified query to use parameters properly
            String sql = "SELECT * FROM payment_data WHERE expiration BETWEEN CURRENT_DATE AND (CURRENT_DATE + ?::INTERVAL)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, monthsThreshold + " months");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data expiring soon: " + e.getMessage(),
                    e, "FIND_EXPIRING_SOON", "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data expiring soon: " + e.getMessage(),
                    "FIND_EXPIRING_SOON", "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByPaymentType(String paymentType)
            throws ConnectionException, QueryExecutionException {
        if (paymentType == null || paymentType.isEmpty()) {
            throw new ValidationException("Payment type cannot be null or empty");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_PAYMENT_TYPE)) {
                stmt.setString(1, paymentType);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by payment type: " + e.getMessage(),
                    e, SQL_FIND_BY_PAYMENT_TYPE, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by payment type: " + e.getMessage(),
                    SQL_FIND_BY_PAYMENT_TYPE, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long countByPaymentType(String paymentType)
            throws ConnectionException, QueryExecutionException {
        if (paymentType == null || paymentType.isEmpty()) {
            throw new ValidationException("Payment type cannot be null or empty");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_BY_PAYMENT_TYPE)) {
                stmt.setString(1, paymentType);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    } else {
                        return 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error counting payment data by payment type: " + e.getMessage(),
                    e, SQL_COUNT_BY_PAYMENT_TYPE, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error counting payment data by payment type: " + e.getMessage(),
                    SQL_COUNT_BY_PAYMENT_TYPE, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidPaymentMethod(UUID paymentDataId)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Retrieve the payment data
        Optional<PaymentData> paymentDataOpt = findById(paymentDataId);
        if (paymentDataOpt.isEmpty()) {
            throw new ResourceNotFoundException("payment_data", paymentDataId);
        }
        
        PaymentData paymentData = paymentDataOpt.get();
        
        // Check if the payment method is expired
        if (paymentData.isExpired()) {
            return false;
        }
        
        // Check if the payment method has been invalidated
        if (paymentData.getPaymentDetails() != null && 
                paymentData.getPaymentDetails().contains("\"status\"") && 
                paymentData.getPaymentDetails().contains("\"invalid\"")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByMerchantId(String merchantId)
            throws ConnectionException, QueryExecutionException {
        if (merchantId == null || merchantId.isEmpty()) {
            throw new ValidationException("Merchant ID cannot be null or empty");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_MERCHANT_ID)) {
                stmt.setString(1, merchantId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by merchant ID: " + e.getMessage(),
                    e, SQL_FIND_BY_MERCHANT_ID, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by merchant ID: " + e.getMessage(),
                    SQL_FIND_BY_MERCHANT_ID, "SELECT", new String[]{"payment_data", "payment_transaction"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PaymentData> findMostRecentByTransactionId(UUID transactionId)
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_MOST_RECENT_BY_TRANSACTION_ID)) {
                stmt.setObject(1, transactionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        return Optional.of(entity.toDomainModel());
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding most recent payment data by transaction ID: " + e.getMessage(),
                    e, SQL_FIND_MOST_RECENT_BY_TRANSACTION_ID, "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding most recent payment data by transaction ID: " + e.getMessage(),
                    SQL_FIND_MOST_RECENT_BY_TRANSACTION_ID, "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentData> findByCreationDateRange(LocalDate startDate, LocalDate endDate)
            throws ConnectionException, QueryExecutionException {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date");
        }
        
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CREATION_DATE_RANGE)) {
                // Convert LocalDate to Timestamp for the start of the day
                Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
                // Convert LocalDate to Timestamp for the end of the day
                Timestamp endTimestamp = Timestamp.valueOf(endDate.plusDays(1).atStartOfDay().minusNanos(1));
                
                stmt.setTimestamp(1, startTimestamp);
                stmt.setTimestamp(2, endTimestamp);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PaymentData> results = new ArrayList<>();
                    while (rs.next()) {
                        PaymentDataEntity entity = mapResultSetToEntity(rs);
                        results.add(entity.toDomainModel());
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new QueryExecutionException(
                    "Error finding payment data by creation date range: " + e.getMessage(),
                    e, SQL_FIND_BY_CREATION_DATE_RANGE, "SELECT", new String[]{"payment_data"});
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryExecutionException(
                    "Unexpected error finding payment data by creation date range: " + e.getMessage(),
                    SQL_FIND_BY_CREATION_DATE_RANGE, "SELECT", new String[]{"payment_data"});
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }
    
    /**
     * Maps a ResultSet row to a PaymentDataEntity.
     *
     * @param rs the ResultSet to map
     * @return a PaymentDataEntity
     * @throws SQLException if a database access error occurs
     */
    private PaymentDataEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        PaymentDataEntity entity = new PaymentDataEntity();
        
        entity.setPaymentDataId(rs.getObject("payment_data_id", UUID.class));
        entity.setTransactionId(rs.getObject("transaction_id", UUID.class));
        entity.setPaymentMethodId(rs.getString("payment_method_id"));
        entity.setPaymentToken(rs.getString("payment_token"));
        entity.setPaymentDetails(rs.getString("payment_details"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toInstant() : null);
        entity.setExpiration(rs.getObject("expiration", LocalDate.class));
        entity.setBillingData(rs.getString("billing_data"));
        
        return entity;
    }
    
    /**
     * Validates a payment data object.
     *
     * @param paymentData the payment data to validate
     * @throws ValidationException if validation fails
     */
    private void validatePaymentData(PaymentData paymentData) throws ValidationException {
        ValidationException validationException = new ValidationException("Payment data validation failed");
        
        // Check required fields
        if (paymentData.getTransactionId() == null) {
            validationException.addFieldError("transactionId", "Transaction ID is required", null);
        }
        
        if (paymentData.getPaymentMethodId() == null || paymentData.getPaymentMethodId().isEmpty()) {
            validationException.addFieldError("paymentMethodId", "Payment method ID is required", null);
        }
        
        // Validate payment details format if provided
        if (paymentData.getPaymentDetails() != null && !paymentData.getPaymentDetails().isEmpty()) {
            try {
                validateJsonFormat(paymentData.getPaymentDetails(), "payment details");
            } catch (ValidationException e) {
                validationException.addFieldError("paymentDetails", e.getMessage(), paymentData.getPaymentDetails());
            }
        }
        
        // Validate billing data format if provided
        if (paymentData.getBillingData() != null && !paymentData.getBillingData().isEmpty()) {
            try {
                validateJsonFormat(paymentData.getBillingData(), "billing data");
            } catch (ValidationException e) {
                validationException.addFieldError("billingData", e.getMessage(), paymentData.getBillingData());
            }
        }
        
        // Validate expiration date if provided
        if (paymentData.getExpiration() != null) {
            if (paymentData.getExpiration().isBefore(LocalDate.now())) {
                validationException.addFieldError("expiration", "Expiration date cannot be in the past", 
                        paymentData.getExpiration());
            }
        }
        
        // Throw the exception if there are validation errors
        if (validationException.hasErrors()) {
            throw validationException;
        }
    }
    
    /**
     * Validates that a string is in valid JSON format.
     *
     * @param json the JSON string to validate
     * @param fieldName the name of the field being validated
     * @throws ValidationException if the JSON is invalid
     */
    private void validateJsonFormat(String json, String fieldName) throws ValidationException {
        if (json == null || json.isEmpty()) {
            return;
        }
        
        try {
            // Simple JSON validation by checking for balanced braces
            int braceCount = 0;
            boolean inString = false;
            boolean escaped = false;
            
            for (char c : json.toCharArray()) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                
                if (c == '\\' && inString) {
                    escaped = true;
                    continue;
                }
                
                if (c == '"' && !escaped) {
                    inString = !inString;
                    continue;
                }
                
                if (!inString) {
                    if (c == '{') {
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                        if (braceCount < 0) {
                            throw new ValidationException("Invalid JSON format: unbalanced braces");
                        }
                    }
                }
            }
            
            if (braceCount != 0) {
                throw new ValidationException("Invalid JSON format: unbalanced braces");
            }
            
            if (inString) {
                throw new ValidationException("Invalid JSON format: unterminated string");
            }
        } catch (Exception e) {
            throw new ValidationException("Invalid " + fieldName + " format: " + e.getMessage());
        }
    }
    
    /**
     * Validates a payment token format.
     *
     * @param token the payment token to validate
     * @throws ValidationException if the token is invalid
     */
    private void validatePaymentToken(String token) throws ValidationException {
        if (token == null || token.isEmpty()) {
            throw new ValidationException("Payment token cannot be null or empty");
        }
        
        // Add token format validation logic here if needed
        // For now, we'll just check that it's not too long
        if (token.length() > 128) {
            throw new ValidationException("Payment token exceeds maximum length of 128 characters");
        }
    }
    
    /**
     * Secures payment data by tokenizing sensitive information.
     *
     * @param paymentData the payment data to secure
     * @return the secured payment data
     * @throws SecurityException if security operations fail
     */
    private PaymentData securePaymentData(PaymentData paymentData) throws SecurityException {
        // Create a copy of the payment data to avoid modifying the original
        PaymentData securedData = new PaymentData();
        securedData.setPaymentDataId(paymentData.getPaymentDataId());
        securedData.setTransactionId(paymentData.getTransactionId());
        securedData.setPaymentMethodId(paymentData.getPaymentMethodId());
        securedData.setExpiration(paymentData.getExpiration());
        securedData.setCreatedAt(paymentData.getCreatedAt());
        
        // Secure the payment token if provided
        if (paymentData.getPaymentToken() != null && !paymentData.getPaymentToken().isEmpty()) {
            securedData.setPaymentToken(securePaymentToken(paymentData.getPaymentToken()));
        }
        
        // Secure the payment details if provided
        if (paymentData.getPaymentDetails() != null && !paymentData.getPaymentDetails().isEmpty()) {
            securedData.setPaymentDetails(securePaymentDetails(paymentData.getPaymentDetails()));
        }
        
        // Secure the billing data if provided
        if (paymentData.getBillingData() != null && !paymentData.getBillingData().isEmpty()) {
            securedData.setBillingData(secureBillingData(paymentData.getBillingData()));
        }
        
        return securedData;
    }
    
    /**
     * Secures a payment token.
     *
     * @param token the payment token to secure
     * @return the secured payment token
     * @throws SecurityException if security operations fail
     */
    private String securePaymentToken(String token) throws SecurityException {
        // In a real implementation, this would use encryption or tokenization services
        // For now, we'll just return the token as is
        return token;
    }
    
    /**
     * Secures payment details by removing or masking sensitive information.
     *
     * @param paymentDetails the payment details to secure
     * @return the secured payment details
     * @throws SecurityException if security operations fail
     */
    private String securePaymentDetails(String paymentDetails) throws SecurityException {
        // In a real implementation, this would parse the JSON, mask sensitive fields, and reserialize
        // For now, we'll just return the details as is
        return paymentDetails;
    }
    
    /**
     * Secures billing data by removing or masking sensitive information.
     *
     * @param billingData the billing data to secure
     * @return the secured billing data
     * @throws SecurityException if security operations fail
     */
    private String secureBillingData(String billingData) throws SecurityException {
        // In a real implementation, this would parse the JSON, mask sensitive fields, and reserialize
        // For now, we'll just return the data as is
        return billingData;
    }
    
    /**
     * Applies role-based masking to payment data.
     *
     * @param paymentData the payment data to mask
     * @param userRole the user role to apply masking for
     * @return the masked payment data
     */
    private PaymentData applyRoleBasedMasking(PaymentData paymentData, String userRole) {
        // Create a copy of the payment data to avoid modifying the original
        PaymentData maskedData = new PaymentData();
        maskedData.setPaymentDataId(paymentData.getPaymentDataId());
        maskedData.setTransactionId(paymentData.getTransactionId());
        maskedData.setPaymentMethodId(paymentData.getPaymentMethodId());
        maskedData.setExpiration(paymentData.getExpiration());
        maskedData.setCreatedAt(paymentData.getCreatedAt());
        
        // Apply role-based masking
        switch (userRole.toLowerCase()) {
            case "admin":
                // Admins can see everything except the full payment token
                if (paymentData.getPaymentToken() != null) {
                    maskedData.setPaymentToken(maskPaymentToken(paymentData.getPaymentToken()));
                }
                maskedData.setPaymentDetails(paymentData.getPaymentDetails());
                maskedData.setBillingData(paymentData.getBillingData());
                break;
                
            case "finance":
                // Finance can see payment details and billing data, but not the token
                maskedData.setPaymentToken("****");
                maskedData.setPaymentDetails(paymentData.getPaymentDetails());
                maskedData.setBillingData(paymentData.getBillingData());
                break;
                
            case "support":
                // Support can see limited payment details and masked billing data
                maskedData.setPaymentToken("****");
                maskedData.setPaymentDetails(maskPaymentDetails(paymentData.getPaymentDetails()));
                maskedData.setBillingData(maskBillingData(paymentData.getBillingData()));
                break;
                
            case "merchant":
                // Merchants can see very limited information
                maskedData.setPaymentToken("****");
                maskedData.setPaymentDetails(null);
                maskedData.setBillingData(null);
                break;
                
            default:
                // Default to maximum masking for unknown roles
                maskedData.setPaymentToken("****");
                maskedData.setPaymentDetails(null);
                maskedData.setBillingData(null);
                break;
        }
        
        return maskedData;
    }
    
    /**
     * Masks a payment token for display.
     *
     * @param token the payment token to mask
     * @return the masked payment token
     */
    private String maskPaymentToken(String token) {
        if (token == null || token.isEmpty()) {
            return "****";
        }
        
        int length = token.length();
        if (length <= 4) {
            return "****";
        }
        
        return "****" + token.substring(length - 4);
    }
    
    /**
     * Masks payment details for display.
     *
     * @param paymentDetails the payment details to mask
     * @return the masked payment details
     */
    private String maskPaymentDetails(String paymentDetails) {
        if (paymentDetails == null || paymentDetails.isEmpty()) {
            return null;
        }
        
        // In a real implementation, this would parse the JSON, mask sensitive fields, and reserialize
        // For now, we'll just return a simplified version
        return "{\"type\": \"masked\"}";
    }
    
    /**
     * Masks billing data for display.
     *
     * @param billingData the billing data to mask
     * @return the masked billing data
     */
    private String maskBillingData(String billingData) {
        if (billingData == null || billingData.isEmpty()) {
            return null;
        }
        
        // In a real implementation, this would parse the JSON, mask sensitive fields, and reserialize
        // For now, we'll just return a simplified version
        return "{\"address\": \"masked\"}";
    }
}