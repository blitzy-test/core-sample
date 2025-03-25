package io.briklabs.sample.payments.data.dao.impl;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.ConnectionManager;
import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.exception.PaymentDataAccessException;
import io.briklabs.sample.payments.data.model.PaymentEventEntity;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;
import io.briklabs.sample.payments.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the PaymentEventDAO interface that manages the storage and retrieval
 * of payment lifecycle events.
 * <p>
 * This class provides methods for event creation, timeline retrieval, event type filtering,
 * and transaction history management. It ensures comprehensive audit trail generation for
 * all payment operations, supporting debugging, compliance, and user activity tracking.
 * </p>
 */
public class PaymentEventDaoImpl extends AbstractPaymentDaoImpl<PaymentEventEntity, UUID> implements PaymentEventDAO {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventDaoImpl.class);
    
    private static final String TABLE_NAME = "payment_event";
    private static final String ALL_COLUMNS = "event_id, transaction_id, event_type, previous_status, new_status, " +
            "event_data, created_at, created_by, correlation_id";
    
    /**
     * Creates a new PaymentEventDaoImpl with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     */
    public PaymentEventDaoImpl(DatabaseConfig databaseConfig, ConfigSource configSource) {
        super(databaseConfig, configSource);
        logger.debug("Initialized PaymentEventDaoImpl");
    }
    
    /**
     * Creates a new PaymentEventDaoImpl with the specified connection manager.
     *
     * @param connectionManager the connection manager
     */
    public PaymentEventDaoImpl(ConnectionManager connectionManager) {
        super(connectionManager);
        logger.debug("Initialized PaymentEventDaoImpl with provided connection manager");
    }
    
    @Override
    protected PaymentEventEntity executeCreate(Connection connection, PaymentEventEntity entity) throws SQLException {
        logger.debug("Creating payment event: {}", entity);
        
        entity.prepareForPersistence();
        entity.validate();
        
        String sql = "INSERT INTO " + TABLE_NAME + " (" + ALL_COLUMNS + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            int paramIndex = 1;
            stmt.setObject(paramIndex++, entity.getEventId());
            stmt.setObject(paramIndex++, entity.getTransactionId());
            stmt.setString(paramIndex++, entity.getEventType());
            stmt.setString(paramIndex++, entity.getPreviousStatus());
            stmt.setString(paramIndex++, entity.getNewStatus());
            stmt.setString(paramIndex++, entity.getEventData());
            stmt.setTimestamp(paramIndex++, Timestamp.from(entity.getCreatedAt()));
            stmt.setString(paramIndex++, entity.getCreatedBy());
            stmt.setObject(paramIndex, entity.getCorrelationId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Failed to create payment event, expected 1 row affected but got " + rowsAffected);
            }
            
            return entity;
        }
    }
    
    @Override
    protected Optional<PaymentEventEntity> executeFindById(Connection connection, UUID id) throws SQLException {
        logger.debug("Finding payment event by ID: {}", id);
        
        String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + " WHERE event_id = ?";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntity(rs));
                }
                return Optional.empty();
            }
        }
    }
    
    @Override
    protected PaymentEventEntity executeUpdate(Connection connection, PaymentEventEntity entity) throws SQLException {
        // Payment events are immutable once created
        throw new UnsupportedOperationException("Payment events are immutable and cannot be updated");
    }
    
    @Override
    protected boolean executeDelete(Connection connection, UUID id) throws SQLException {
        logger.debug("Deleting payment event with ID: {}", id);
        
        // In a production system, we might want to prevent deletion of payment events
        // for audit and compliance reasons. Consider implementing a soft delete instead.
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE event_id = ?";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    @Override
    protected List<PaymentEventEntity> executeQuery(Connection connection, Object params) throws SQLException {
        logger.debug("Querying payment events with params: {}", params);
        
        PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                .select(ALL_COLUMNS)
                .from(TABLE_NAME);
        
        // Apply filters if params is a PaymentFilterParams object
        if (params != null) {
            queryBuilder.applyFilters(params);
        }
        
        try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection);
             ResultSet rs = stmt.executeQuery()) {
            
            List<PaymentEventEntity> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs));
            }
            return results;
        }
    }
    
    @Override
    protected List<PaymentEventEntity> executeBatchCreate(Connection connection, List<PaymentEventEntity> entities) throws SQLException {
        logger.debug("Batch creating {} payment events", entities.size());
        
        String sql = "INSERT INTO " + TABLE_NAME + " (" + ALL_COLUMNS + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            for (PaymentEventEntity entity : entities) {
                entity.prepareForPersistence();
                entity.validate();
                
                int paramIndex = 1;
                stmt.setObject(paramIndex++, entity.getEventId());
                stmt.setObject(paramIndex++, entity.getTransactionId());
                stmt.setString(paramIndex++, entity.getEventType());
                stmt.setString(paramIndex++, entity.getPreviousStatus());
                stmt.setString(paramIndex++, entity.getNewStatus());
                stmt.setString(paramIndex++, entity.getEventData());
                stmt.setTimestamp(paramIndex++, Timestamp.from(entity.getCreatedAt()));
                stmt.setString(paramIndex++, entity.getCreatedBy());
                stmt.setObject(paramIndex, entity.getCorrelationId());
                
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            return entities;
        }
    }
    
    @Override
    protected List<PaymentEventEntity> executeBatchUpdate(Connection connection, List<PaymentEventEntity> entities) throws SQLException {
        // Payment events are immutable once created
        throw new UnsupportedOperationException("Payment events are immutable and cannot be updated");
    }
    
    @Override
    protected long executeCount(Connection connection, Object params) throws SQLException {
        logger.debug("Counting payment events with params: {}", params);
        
        PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                .count("*")
                .from(TABLE_NAME);
        
        // Apply filters if params is a PaymentFilterParams object
        if (params != null) {
            queryBuilder.applyFilters(params);
        }
        
        try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }
    
    @Override
    protected boolean executeExists(Connection connection, UUID id) throws SQLException {
        logger.debug("Checking if payment event exists with ID: {}", id);
        
        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE event_id = ?";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    @Override
    protected List<PaymentEventEntity> executeFindByOrganizationId(Connection connection, UUID organizationId) throws SQLException {
        logger.debug("Finding payment events by organization ID: {}", organizationId);
        
        String sql = "SELECT e." + ALL_COLUMNS + " FROM " + TABLE_NAME + " e " +
                "JOIN payment_transaction t ON e.transaction_id = t.transaction_id " +
                "WHERE t.organization_id = ? " +
                "ORDER BY e.created_at DESC";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, organizationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<PaymentEventEntity> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
                return results;
            }
        }
    }
    
    @Override
    protected List<PaymentEventEntity> executeFindByAccountId(Connection connection, UUID accountId) throws SQLException {
        logger.debug("Finding payment events by account ID: {}", accountId);
        
        String sql = "SELECT e." + ALL_COLUMNS + " FROM " + TABLE_NAME + " e " +
                "JOIN payment_transaction t ON e.transaction_id = t.transaction_id " +
                "WHERE t.account_id = ? " +
                "ORDER BY e.created_at DESC";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<PaymentEventEntity> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
                return results;
            }
        }
    }
    
    @Override
    protected List<PaymentEventEntity> executeFindByOrganizationAndAccountId(Connection connection, UUID organizationId, UUID accountId) throws SQLException {
        logger.debug("Finding payment events by organization ID: {} and account ID: {}", organizationId, accountId);
        
        String sql = "SELECT e." + ALL_COLUMNS + " FROM " + TABLE_NAME + " e " +
                "JOIN payment_transaction t ON e.transaction_id = t.transaction_id " +
                "WHERE t.organization_id = ? AND t.account_id = ? " +
                "ORDER BY e.created_at DESC";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, organizationId);
            stmt.setObject(2, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<PaymentEventEntity> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
                return results;
            }
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByTransactionId(UUID transactionId) {
        logger.debug("Finding payment events by transaction ID: {}", transactionId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE transaction_id = ? ORDER BY created_at ASC";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by transaction ID: " + transactionId, e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByTransactionIdAndEventType(UUID transactionId, String eventType) {
        logger.debug("Finding payment events by transaction ID: {} and event type: {}", transactionId, eventType);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE transaction_id = ? AND event_type = ? ORDER BY created_at ASC";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    stmt.setString(2, eventType);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by transaction ID and event type", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByTransactionIdAndTimeRange(UUID transactionId, Instant startTime, Instant endTime) {
        logger.debug("Finding payment events by transaction ID: {} and time range: {} to {}", 
                transactionId, startTime, endTime);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE transaction_id = ? AND created_at BETWEEN ? AND ? ORDER BY created_at ASC";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    stmt.setTimestamp(2, startTime != null ? Timestamp.from(startTime) : null);
                    stmt.setTimestamp(3, endTime != null ? Timestamp.from(endTime) : null);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by transaction ID and time range", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByTransactionIdAndResultingStatus(UUID transactionId, PaymentStatus status) {
        logger.debug("Finding payment events by transaction ID: {} and resulting status: {}", 
                transactionId, status);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE transaction_id = ? AND new_status = ? ORDER BY created_at ASC";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    stmt.setString(2, status != null ? status.name() : null);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by transaction ID and resulting status", e);
        }
    }
    
    @Override
    public Optional<PaymentEventEntity> findMostRecentByTransactionId(UUID transactionId) {
        logger.debug("Finding most recent payment event by transaction ID: {}", transactionId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE transaction_id = ? ORDER BY created_at DESC LIMIT 1";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return Optional.of(mapResultSetToEntity(rs));
                        }
                        return Optional.empty();
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find most recent payment event by transaction ID", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByCorrelationId(UUID correlationId) {
        logger.debug("Finding payment events by correlation ID: {}", correlationId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE correlation_id = ? ORDER BY created_at ASC";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, correlationId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by correlation ID", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByCreatedBy(String createdBy, int limit, int offset) {
        logger.debug("Finding payment events by created by: {} with limit: {} and offset: {}", 
                createdBy, limit, offset);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE created_by = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setString(1, createdBy);
                    stmt.setInt(2, limit);
                    stmt.setInt(3, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by created by", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByOrganizationIdAndTimeRange(UUID organizationId, Instant startTime, 
                                                                    Instant endTime, int limit, int offset) {
        logger.debug("Finding payment events by organization ID: {} and time range: {} to {} with limit: {} and offset: {}", 
                organizationId, startTime, endTime, limit, offset);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT e." + ALL_COLUMNS + " FROM " + TABLE_NAME + " e " +
                        "JOIN payment_transaction t ON e.transaction_id = t.transaction_id " +
                        "WHERE t.organization_id = ? AND e.created_at BETWEEN ? AND ? " +
                        "ORDER BY e.created_at DESC LIMIT ? OFFSET ?";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, organizationId);
                    stmt.setTimestamp(2, startTime != null ? Timestamp.from(startTime) : null);
                    stmt.setTimestamp(3, endTime != null ? Timestamp.from(endTime) : null);
                    stmt.setInt(4, limit);
                    stmt.setInt(5, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by organization ID and time range", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByAccountIdAndTimeRange(UUID accountId, Instant startTime, 
                                                              Instant endTime, int limit, int offset) {
        logger.debug("Finding payment events by account ID: {} and time range: {} to {} with limit: {} and offset: {}", 
                accountId, startTime, endTime, limit, offset);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT e." + ALL_COLUMNS + " FROM " + TABLE_NAME + " e " +
                        "JOIN payment_transaction t ON e.transaction_id = t.transaction_id " +
                        "WHERE t.account_id = ? AND e.created_at BETWEEN ? AND ? " +
                        "ORDER BY e.created_at DESC LIMIT ? OFFSET ?";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, accountId);
                    stmt.setTimestamp(2, startTime != null ? Timestamp.from(startTime) : null);
                    stmt.setTimestamp(3, endTime != null ? Timestamp.from(endTime) : null);
                    stmt.setInt(4, limit);
                    stmt.setInt(5, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by account ID and time range", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findStatusChangesByTransactionId(UUID transactionId) {
        logger.debug("Finding status change events by transaction ID: {}", transactionId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE transaction_id = ? AND event_type = 'STATUS_CHANGE' ORDER BY created_at ASC";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find status change events by transaction ID", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByNewStatusAndTimeRange(PaymentStatus status, Instant startTime, 
                                                              Instant endTime, int limit, int offset) {
        logger.debug("Finding payment events by new status: {} and time range: {} to {} with limit: {} and offset: {}", 
                status, startTime, endTime, limit, offset);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE new_status = ? AND created_at BETWEEN ? AND ? " +
                        "ORDER BY created_at DESC LIMIT ? OFFSET ?";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setString(1, status != null ? status.name() : null);
                    stmt.setTimestamp(2, startTime != null ? Timestamp.from(startTime) : null);
                    stmt.setTimestamp(3, endTime != null ? Timestamp.from(endTime) : null);
                    stmt.setInt(4, limit);
                    stmt.setInt(5, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by new status and time range", e);
        }
    }
    
    @Override
    public long countByTransactionId(UUID transactionId) {
        logger.debug("Counting payment events by transaction ID: {}", transactionId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE transaction_id = ?";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                        return 0;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to count payment events by transaction ID", e);
        }
    }
    
    @Override
    public PaymentEventEntity createTransactionCreatedEvent(UUID transactionId, String createdBy) {
        logger.debug("Creating transaction created event for transaction ID: {}", transactionId);
        
        try {
            PaymentEventEntity event = new PaymentEventEntity();
            event.setEventId(UUID.randomUUID());
            event.setTransactionId(transactionId);
            event.setEventType("CREATED");
            event.setNewStatus(PaymentStatus.CREATED.name());
            event.setEventData("{\"action\":\"transaction_created\"}");
            event.setCreatedAt(Instant.now());
            event.setCreatedBy(createdBy);
            
            return create(event);
        } catch (Exception e) {
            throw handleException("Failed to create transaction created event", e);
        }
    }
    
    @Override
    public PaymentEventEntity createStatusChangeEvent(UUID transactionId, PaymentStatus previousStatus, 
                                                    PaymentStatus newStatus, String createdBy, UUID correlationId) {
        logger.debug("Creating status change event for transaction ID: {} from status: {} to status: {}", 
                transactionId, previousStatus, newStatus);
        
        // Validate the status transition
        if (previousStatus != null && newStatus != null) {
            PaymentStatus.validateTransition(previousStatus, newStatus);
        }
        
        try {
            PaymentEventEntity event = new PaymentEventEntity();
            event.setEventId(UUID.randomUUID());
            event.setTransactionId(transactionId);
            event.setEventType("STATUS_CHANGE");
            event.setPreviousStatus(previousStatus != null ? previousStatus.name() : null);
            event.setNewStatus(newStatus != null ? newStatus.name() : null);
            
            String eventData = String.format(
                    "{\"action\":\"status_change\",\"previous\":\"%s\",\"new\":\"%s\"}",
                    previousStatus != null ? previousStatus.name() : "null",
                    newStatus != null ? newStatus.name() : "null"
            );
            event.setEventData(eventData);
            
            event.setCreatedAt(Instant.now());
            event.setCreatedBy(createdBy);
            event.setCorrelationId(correlationId);
            
            return create(event);
        } catch (IllegalStateException e) {
            throw e; // Rethrow validation exceptions
        } catch (Exception e) {
            throw handleException("Failed to create status change event", e);
        }
    }
    
    @Override
    public PaymentEventEntity createCaptureEvent(UUID transactionId, String captureAmount, 
                                               String createdBy, UUID correlationId) {
        logger.debug("Creating capture event for transaction ID: {} with amount: {}", 
                transactionId, captureAmount);
        
        try {
            PaymentEventEntity event = new PaymentEventEntity();
            event.setEventId(UUID.randomUUID());
            event.setTransactionId(transactionId);
            event.setEventType("CAPTURE");
            event.setPreviousStatus(PaymentStatus.AUTHORIZED.name());
            event.setNewStatus(PaymentStatus.CAPTURED.name());
            
            String eventData = String.format(
                    "{\"action\":\"capture\",\"amount\":\"%s\"}",
                    captureAmount
            );
            event.setEventData(eventData);
            
            event.setCreatedAt(Instant.now());
            event.setCreatedBy(createdBy);
            event.setCorrelationId(correlationId);
            
            return create(event);
        } catch (Exception e) {
            throw handleException("Failed to create capture event", e);
        }
    }
    
    @Override
    public PaymentEventEntity createRefundEvent(UUID transactionId, String refundAmount, 
                                              String reason, String createdBy, UUID correlationId) {
        logger.debug("Creating refund event for transaction ID: {} with amount: {} and reason: {}", 
                transactionId, refundAmount, reason);
        
        try {
            PaymentEventEntity event = new PaymentEventEntity();
            event.setEventId(UUID.randomUUID());
            event.setTransactionId(transactionId);
            event.setEventType("REFUND");
            event.setPreviousStatus(PaymentStatus.CAPTURED.name());
            event.setNewStatus(PaymentStatus.REFUNDED.name());
            
            String eventData = String.format(
                    "{\"action\":\"refund\",\"amount\":\"%s\",\"reason\":\"%s\"}",
                    refundAmount,
                    reason != null ? reason.replace("\"", "\\\"") : ""
            );
            event.setEventData(eventData);
            
            event.setCreatedAt(Instant.now());
            event.setCreatedBy(createdBy);
            event.setCorrelationId(correlationId);
            
            return create(event);
        } catch (Exception e) {
            throw handleException("Failed to create refund event", e);
        }
    }
    
    @Override
    public PaymentEventEntity createVoidEvent(UUID transactionId, String reason, 
                                            String createdBy, UUID correlationId) {
        logger.debug("Creating void event for transaction ID: {} with reason: {}", 
                transactionId, reason);
        
        try {
            PaymentEventEntity event = new PaymentEventEntity();
            event.setEventId(UUID.randomUUID());
            event.setTransactionId(transactionId);
            event.setEventType("VOID");
            event.setPreviousStatus(PaymentStatus.AUTHORIZED.name());
            event.setNewStatus(PaymentStatus.VOIDED.name());
            
            String eventData = String.format(
                    "{\"action\":\"void\",\"reason\":\"%s\"}",
                    reason != null ? reason.replace("\"", "\\\"") : ""
            );
            event.setEventData(eventData);
            
            event.setCreatedAt(Instant.now());
            event.setCreatedBy(createdBy);
            event.setCorrelationId(correlationId);
            
            return create(event);
        } catch (Exception e) {
            throw handleException("Failed to create void event", e);
        }
    }
    
    @Override
    public PaymentEventEntity createErrorEvent(UUID transactionId, String errorCode, 
                                             String errorMessage, String createdBy, UUID correlationId) {
        logger.debug("Creating error event for transaction ID: {} with error code: {} and message: {}", 
                transactionId, errorCode, errorMessage);
        
        try {
            PaymentEventEntity event = new PaymentEventEntity();
            event.setEventId(UUID.randomUUID());
            event.setTransactionId(transactionId);
            event.setEventType("ERROR");
            
            String eventData = String.format(
                    "{\"action\":\"error\",\"code\":\"%s\",\"message\":\"%s\"}",
                    errorCode != null ? errorCode : "",
                    errorMessage != null ? errorMessage.replace("\"", "\\\"") : ""
            );
            event.setEventData(eventData);
            
            event.setCreatedAt(Instant.now());
            event.setCreatedBy(createdBy);
            event.setCorrelationId(correlationId);
            
            return create(event);
        } catch (Exception e) {
            throw handleException("Failed to create error event", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> findByEventDataContent(String key, String value, int limit, int offset) {
        logger.debug("Finding payment events by event data content with key: {} and value: {}", key, value);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                // Using PostgreSQL JSONB containment operator @> for searching within JSON
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE event_data::jsonb @> ?::jsonb " +
                        "ORDER BY created_at DESC LIMIT ? OFFSET ?";
                
                String jsonPattern = String.format("{\"%s\":\"%s\"}", 
                        key != null ? key : "", 
                        value != null ? value : "");
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setString(1, jsonPattern);
                    stmt.setInt(2, limit);
                    stmt.setInt(3, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find payment events by event data content", e);
        }
    }
    
    @Override
    public List<PaymentEventEntity> getTransactionTimeline(UUID transactionId, int limit, int offset) {
        logger.debug("Getting transaction timeline for transaction ID: {} with limit: {} and offset: {}", 
                transactionId, limit, offset);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE_NAME + 
                        " WHERE transaction_id = ? ORDER BY created_at ASC LIMIT ? OFFSET ?";
                
                try (PreparedStatement stmt = prepareStatement(connection, sql)) {
                    stmt.setObject(1, transactionId);
                    stmt.setInt(2, limit);
                    stmt.setInt(3, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<PaymentEventEntity> results = new ArrayList<>();
                        while (rs.next()) {
                            results.add(mapResultSetToEntity(rs));
                        }
                        return results;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to get transaction timeline", e);
        }
    }
    
    /**
     * Maps a result set row to a PaymentEventEntity.
     *
     * @param rs the result set
     * @return the mapped entity
     * @throws SQLException if a database error occurs
     */
    private PaymentEventEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        PaymentEventEntity entity = new PaymentEventEntity();
        
        entity.setEventId((UUID) rs.getObject("event_id"));
        entity.setTransactionId((UUID) rs.getObject("transaction_id"));
        entity.setEventType(rs.getString("event_type"));
        entity.setPreviousStatus(rs.getString("previous_status"));
        entity.setNewStatus(rs.getString("new_status"));
        entity.setEventData(rs.getString("event_data"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            entity.setCreatedAt(createdAt.toInstant());
        }
        
        entity.setCreatedBy(rs.getString("created_by"));
        entity.setCorrelationId((UUID) rs.getObject("correlation_id"));
        
        return entity;
    }
}