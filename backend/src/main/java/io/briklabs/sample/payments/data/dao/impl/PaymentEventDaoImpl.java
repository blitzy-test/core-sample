package io.briklabs.sample.payments.data.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;
import io.briklabs.sample.payments.model.PaymentEvent;

/**
 * Implementation of the PaymentEventDAO interface that manages the storage and retrieval
 * of payment lifecycle events.
 * <p>
 * This class provides methods for event creation, timeline retrieval, event type filtering,
 * and transaction history management. It ensures comprehensive audit trail generation for
 * all payment operations, supporting debugging, compliance, and user activity tracking.
 * </p>
 */
public class PaymentEventDaoImpl extends AbstractPaymentDaoImpl<PaymentEvent, UUID> implements PaymentEventDAO {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventDaoImpl.class);
    
    private static final String TABLE_NAME = "payment_event";
    private static final String[] ALL_COLUMNS = {
        "event_id", "transaction_id", "event_type", "previous_status", "new_status", 
        "event_data", "created_at", "created_by", "correlation_id"
    };

    /**
     * Creates a new PaymentEventDaoImpl with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     */
    public PaymentEventDaoImpl(DatabaseConfig databaseConfig, ConfigSource configSource) {
        super(databaseConfig, configSource, TABLE_NAME);
    }

    /**
     * Maps a ResultSet row to a PaymentEvent object.
     *
     * @param rs the result set
     * @return the mapped PaymentEvent
     * @throws SQLException if a database access error occurs
     */
    @Override
    protected PaymentEvent mapRow(ResultSet rs) throws SQLException {
        UUID eventId = (UUID) rs.getObject("event_id");
        UUID transactionId = (UUID) rs.getObject("transaction_id");
        String eventType = rs.getString("event_type");
        String previousStatus = rs.getString("previous_status");
        String newStatus = rs.getString("new_status");
        String eventData = rs.getString("event_data");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Instant createdAt = createdAtTs != null ? createdAtTs.toInstant() : null;
        String createdBy = rs.getString("created_by");
        UUID correlationId = (UUID) rs.getObject("correlation_id");
        
        return new PaymentEvent(
            eventId, 
            transactionId, 
            eventType, 
            previousStatus, 
            newStatus, 
            eventData, 
            createdAt, 
            createdBy, 
            correlationId
        );
    }

    /**
     * Validates a PaymentEvent entity before database operations.
     *
     * @param event the event to validate
     * @throws ValidationException if the event fails validation
     */
    @Override
    protected void validateEntity(PaymentEvent event) throws ValidationException {
        try {
            event.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid payment event: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a query for finding a payment event by ID.
     *
     * @param id the event ID
     * @return the SQL query
     */
    @Override
    protected String buildFindByIdQuery(UUID id) {
        return "SELECT " + String.join(", ", ALL_COLUMNS) + 
               " FROM " + TABLE_NAME + 
               " WHERE event_id = ?";
    }

    /**
     * Builds a query for filtering payment events.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    @Override
    protected PaymentQueryBuilder buildFilterQuery(PaymentFilterParams filterParams) {
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e");
        
        if (filterParams != null) {
            // Apply organization and account filters if provided
            if (filterParams.getOrganizationId() != null) {
                builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id")
                       .and("t.organization_id = ?")
                       .addParameter(filterParams.getOrganizationId());
            }
            
            if (filterParams.getAccountId() != null) {
                if (!builder.getQueryString().contains("payment_transaction t")) {
                    builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id");
                }
                builder.and("t.account_id = ?")
                       .addParameter(filterParams.getAccountId());
            }
            
            // Apply date range filter
            if (filterParams.getDateRange() != null) {
                builder.dateRange("e.created_at", 
                        filterParams.getDateRange().getStartDate() != null ? 
                                filterParams.getDateRange().getStartDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                        filterParams.getDateRange().getEndDate() != null ? 
                                filterParams.getDateRange().getEndDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            }
            
            // Apply event type filter if provided in search term
            if (filterParams.getSearchTerm() != null && !filterParams.getSearchTerm().isEmpty()) {
                builder.and("(e.event_type LIKE ? OR e.event_data LIKE ?)")
                       .addParameter("%" + filterParams.getSearchTerm() + "%")
                       .addParameter("%" + filterParams.getSearchTerm() + "%");
            }
            
            // Apply sorting
            if (filterParams.getSortCriteria() != null && !filterParams.getSortCriteria().isEmpty()) {
                for (PaymentFilterParams.SortCriteria criteria : filterParams.getSortCriteria()) {
                    String column = criteria.getColumn();
                    // Prefix column with alias if not already prefixed
                    if (!column.contains(".")) {
                        column = "e." + column;
                    }
                    builder.orderBy(column + " " + criteria.getDirection());
                }
            } else {
                // Default sort by created_at descending
                builder.orderBy("e.created_at DESC");
            }
            
            // Apply pagination
            if (filterParams.getPagination() != null) {
                builder.paginate(filterParams.getPagination().getLimit(), filterParams.getPagination().getOffset());
            }
        }
        
        return builder;
    }

    /**
     * Builds a count query for payment events.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    @Override
    protected PaymentQueryBuilder buildCountQuery(PaymentFilterParams filterParams) {
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .count("*")
            .from(TABLE_NAME + " e");
        
        if (filterParams != null) {
            // Apply organization and account filters if provided
            if (filterParams.getOrganizationId() != null) {
                builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id")
                       .and("t.organization_id = ?")
                       .addParameter(filterParams.getOrganizationId());
            }
            
            if (filterParams.getAccountId() != null) {
                if (!builder.getQueryString().contains("payment_transaction t")) {
                    builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id");
                }
                builder.and("t.account_id = ?")
                       .addParameter(filterParams.getAccountId());
            }
            
            // Apply date range filter
            if (filterParams.getDateRange() != null) {
                builder.dateRange("e.created_at", 
                        filterParams.getDateRange().getStartDate() != null ? 
                                filterParams.getDateRange().getStartDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                        filterParams.getDateRange().getEndDate() != null ? 
                                filterParams.getDateRange().getEndDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            }
            
            // Apply event type filter if provided in search term
            if (filterParams.getSearchTerm() != null && !filterParams.getSearchTerm().isEmpty()) {
                builder.and("(e.event_type LIKE ? OR e.event_data LIKE ?)")
                       .addParameter("%" + filterParams.getSearchTerm() + "%")
                       .addParameter("%" + filterParams.getSearchTerm() + "%");
            }
        }
        
        return builder;
    }

    /**
     * Builds an insert query for a payment event.
     *
     * @param event the event to insert
     * @return the SQL query
     */
    @Override
    protected String buildInsertQuery(PaymentEvent event) {
        return "INSERT INTO " + TABLE_NAME + 
               " (event_id, transaction_id, event_type, previous_status, new_status, " +
               "event_data, created_at, created_by, correlation_id) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    /**
     * Builds an update query for a payment event.
     * Note: Events should generally be immutable, but this is provided for completeness.
     *
     * @param event the event to update
     * @return the SQL query
     */
    @Override
    protected String buildUpdateQuery(PaymentEvent event) {
        return "UPDATE " + TABLE_NAME + 
               " SET transaction_id = ?, event_type = ?, previous_status = ?, " +
               "new_status = ?, event_data = ?, created_at = ?, created_by = ?, " +
               "correlation_id = ? " +
               "WHERE event_id = ?";
    }

    /**
     * Builds a delete query for a payment event.
     * Note: Events should generally not be deleted, but this is provided for completeness.
     *
     * @param id the event ID
     * @return the SQL query
     */
    @Override
    protected String buildDeleteQuery(UUID id) {
        return "DELETE FROM " + TABLE_NAME + " WHERE event_id = ?";
    }

    /**
     * Gets the parameters for an insert query.
     *
     * @param event the event to insert
     * @return the query parameters
     */
    @Override
    protected Object[] getInsertParameters(PaymentEvent event) {
        return new Object[] {
            event.getEventId(),
            event.getTransactionId(),
            event.getEventType(),
            event.getPreviousStatus(),
            event.getNewStatus(),
            event.getEventData(),
            event.getCreatedAt() != null ? Timestamp.from(event.getCreatedAt()) : null,
            event.getCreatedBy(),
            event.getCorrelationId()
        };
    }

    /**
     * Gets the parameters for an update query.
     *
     * @param event the event to update
     * @return the query parameters
     */
    @Override
    protected Object[] getUpdateParameters(PaymentEvent event) {
        return new Object[] {
            event.getTransactionId(),
            event.getEventType(),
            event.getPreviousStatus(),
            event.getNewStatus(),
            event.getEventData(),
            event.getCreatedAt() != null ? Timestamp.from(event.getCreatedAt()) : null,
            event.getCreatedBy(),
            event.getCorrelationId(),
            event.getEventId() // WHERE clause parameter
        };
    }

    /**
     * Gets the parameters for a delete query.
     *
     * @param id the event ID
     * @return the query parameters
     */
    @Override
    protected Object[] getDeleteParameters(UUID id) {
        return new Object[] { id };
    }

    /**
     * Finds all events for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of events for the specified transaction, ordered chronologically
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByTransactionId(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        String sql = "SELECT " + String.join(", ", ALL_COLUMNS) + 
                     " FROM " + TABLE_NAME + 
                     " WHERE transaction_id = ? " +
                     "ORDER BY created_at ASC";
        
        return executeQuery(sql, this::mapRows, transactionId);
    }

    /**
     * Finds all events for a specific transaction with filtering parameters.
     *
     * @param transactionId The transaction identifier
     * @param filterParams Additional filtering parameters
     * @return List of events for the specified transaction, filtered and ordered according to parameters
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByTransactionId(UUID transactionId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.transaction_id = ?")
            .addParameter(transactionId);
        
        if (filterParams != null) {
            // Apply date range filter
            if (filterParams.getDateRange() != null) {
                builder.dateRange("e.created_at", 
                        filterParams.getDateRange().getStartDate() != null ? 
                                filterParams.getDateRange().getStartDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                        filterParams.getDateRange().getEndDate() != null ? 
                                filterParams.getDateRange().getEndDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            }
            
            // Apply event type filter if provided in search term
            if (filterParams.getSearchTerm() != null && !filterParams.getSearchTerm().isEmpty()) {
                builder.and("(e.event_type LIKE ? OR e.event_data LIKE ?)")
                       .addParameter("%" + filterParams.getSearchTerm() + "%")
                       .addParameter("%" + filterParams.getSearchTerm() + "%");
            }
            
            // Apply sorting
            if (filterParams.getSortCriteria() != null && !filterParams.getSortCriteria().isEmpty()) {
                for (PaymentFilterParams.SortCriteria criteria : filterParams.getSortCriteria()) {
                    String column = criteria.getColumn();
                    // Prefix column with alias if not already prefixed
                    if (!column.contains(".")) {
                        column = "e." + column;
                    }
                    builder.orderBy(column + " " + criteria.getDirection());
                }
            } else {
                // Default sort by created_at ascending for chronological order
                builder.orderBy("e.created_at ASC");
            }
            
            // Apply pagination
            if (filterParams.getPagination() != null) {
                builder.paginate(filterParams.getPagination().getLimit(), filterParams.getPagination().getOffset());
            }
        } else {
            // Default sort by created_at ascending for chronological order
            builder.orderBy("e.created_at ASC");
        }
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events by event type.
     *
     * @param eventType The type of event to find
     * @param filterParams Additional filtering parameters
     * @return List of events of the specified type
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByEventType(String eventType, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.event_type = ?")
            .addParameter(eventType);
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events by multiple event types.
     *
     * @param eventTypes List of event types to find
     * @param filterParams Additional filtering parameters
     * @return List of events matching any of the specified types
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByEventTypeIn(List<String> eventTypes, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (eventTypes == null || eventTypes.isEmpty()) {
            throw new IllegalArgumentException("Event types list cannot be null or empty");
        }
        
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < eventTypes.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.event_type IN (" + placeholders + ")");
        
        // Add event type parameters
        for (String eventType : eventTypes) {
            builder.addParameter(eventType);
        }
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events created within a specific time range.
     *
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @param filterParams Additional filtering parameters
     * @return List of events created within the specified time range
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByCreatedAtBetween(Instant startTime, Instant endTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (startTime == null && endTime == null) {
            throw new IllegalArgumentException("At least one of startTime or endTime must be provided");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e");
        
        if (startTime != null && endTime != null) {
            builder.where("e.created_at BETWEEN ? AND ?")
                   .addParameter(Timestamp.from(startTime))
                   .addParameter(Timestamp.from(endTime));
        } else if (startTime != null) {
            builder.where("e.created_at >= ?")
                   .addParameter(Timestamp.from(startTime));
        } else {
            builder.where("e.created_at <= ?")
                   .addParameter(Timestamp.from(endTime));
        }
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events created by a specific user or system.
     *
     * @param createdBy The identifier of the user or system that created the events
     * @param filterParams Additional filtering parameters
     * @return List of events created by the specified user or system
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByCreatedBy(String createdBy, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (createdBy == null || createdBy.isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.created_by = ?")
            .addParameter(createdBy);
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events with a specific correlation ID.
     *
     * @param correlationId The correlation identifier
     * @param filterParams Additional filtering parameters
     * @return List of events with the specified correlation ID
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByCorrelationId(UUID correlationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (correlationId == null) {
            throw new IllegalArgumentException("Correlation ID cannot be null");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.correlation_id = ?")
            .addParameter(correlationId);
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds status change events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param filterParams Additional filtering parameters
     * @return List of status change events for the specified transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findStatusChangeEvents(UUID transactionId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.transaction_id = ?")
            .and("e.event_type = ?")
            .addParameter(transactionId)
            .addParameter("STATUS_CHANGE");
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds error events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param filterParams Additional filtering parameters
     * @return List of error events for the specified transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findErrorEvents(UUID transactionId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.transaction_id = ?")
            .and("e.event_type = ?")
            .addParameter(transactionId)
            .addParameter("ERROR");
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds the most recent event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The most recent event for the specified transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public PaymentEvent findMostRecentEvent(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        String sql = "SELECT " + String.join(", ", ALL_COLUMNS) + 
                     " FROM " + TABLE_NAME + 
                     " WHERE transaction_id = ? " +
                     "ORDER BY created_at DESC " +
                     "LIMIT 1";
        
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }, transactionId);
    }

    /**
     * Finds the most recent event of a specific type for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param eventType The type of event to find
     * @return The most recent event of the specified type for the transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public PaymentEvent findMostRecentEventByType(UUID transactionId, String eventType) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        
        String sql = "SELECT " + String.join(", ", ALL_COLUMNS) + 
                     " FROM " + TABLE_NAME + 
                     " WHERE transaction_id = ? AND event_type = ? " +
                     "ORDER BY created_at DESC " +
                     "LIMIT 1";
        
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }, transactionId, eventType);
    }

    /**
     * Creates a status change event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param previousStatus The previous status
     * @param newStatus The new status
     * @param createdBy The identifier of the user or system creating the event
     * @return The created event
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public PaymentEvent createStatusChangeEvent(UUID transactionId, String previousStatus, String newStatus, String createdBy) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (createdBy == null || createdBy.isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("STATUS_CHANGE");
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        
        return create(event);
    }

    /**
     * Creates an error event for a transaction.
     *
     * @param transactionId The transaction identifier
     * @param errorCode The error code
     * @param errorMessage The error message
     * @param createdBy The identifier of the system reporting the error
     * @return The created event
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public PaymentEvent createErrorEvent(UUID transactionId, String errorCode, String errorMessage, String createdBy) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        if (createdBy == null || createdBy.isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        
        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setTransactionId(transactionId);
        event.setEventType("ERROR");
        event.setEventData(String.format("{\"errorCode\":\"%s\",\"errorMessage\":\"%s\"}", 
                                         errorCode != null ? errorCode : "", 
                                         errorMessage != null ? errorMessage.replace("\"", "\\\"") : ""));
        event.setCreatedAt(Instant.now());
        event.setCreatedBy(createdBy);
        
        return create(event);
    }

    /**
     * Finds events for multiple transactions.
     *
     * @param transactionIds List of transaction identifiers
     * @param filterParams Additional filtering parameters
     * @return List of events for the specified transactions
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByTransactionIdIn(List<UUID> transactionIds, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (transactionIds == null || transactionIds.isEmpty()) {
            throw new IllegalArgumentException("Transaction IDs list cannot be null or empty");
        }
        
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < transactionIds.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.transaction_id IN (" + placeholders + ")");
        
        // Add transaction ID parameters
        for (UUID transactionId : transactionIds) {
            builder.addParameter(transactionId);
        }
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Counts events by type for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return Map of event type to count
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Map<String, Long> countEventsByType(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        String sql = "SELECT event_type, COUNT(*) as event_count " +
                     "FROM " + TABLE_NAME + 
                     " WHERE transaction_id = ? " +
                     "GROUP BY event_type";
        
        return executeQuery(sql, rs -> {
            Map<String, Long> counts = new HashMap<>();
            while (rs.next()) {
                String eventType = rs.getString("event_type");
                long count = rs.getLong("event_count");
                counts.put(eventType, count);
            }
            return counts;
        }, transactionId);
    }

    /**
     * Finds events for transactions belonging to a specific organization.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of events for transactions belonging to the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id")
            .where("t.organization_id = ?")
            .addParameter(organizationId);
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events for transactions belonging to a specific account.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param filterParams Additional filtering parameters
     * @return List of events for transactions belonging to the specified account
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id")
            .where("t.organization_id = ?")
            .and("t.account_id = ?")
            .addParameter(organizationId)
            .addParameter(accountId);
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events related to status transitions to a specific status.
     *
     * @param status The target status
     * @param filterParams Additional filtering parameters
     * @return List of events representing transitions to the specified status
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByNewStatus(String status, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.event_type = ?")
            .and("e.new_status = ?")
            .addParameter("STATUS_CHANGE")
            .addParameter(status);
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events related to status transitions from a specific status.
     *
     * @param status The source status
     * @param filterParams Additional filtering parameters
     * @return List of events representing transitions from the specified status
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByPreviousStatus(String status, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.event_type = ?")
            .and("e.previous_status = ?")
            .addParameter("STATUS_CHANGE")
            .addParameter(status);
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Finds events containing specific data in the event_data JSON field.
     *
     * @param jsonPath The JSON path expression
     * @param value The value to match
     * @param filterParams Additional filtering parameters
     * @return List of events matching the JSON criteria
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> findByEventDataContains(String jsonPath, String value, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (jsonPath == null || jsonPath.isEmpty()) {
            throw new IllegalArgumentException("JSON path cannot be null or empty");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.event_data::jsonb @> ?::jsonb")
            .addParameter("{\"" + jsonPath + "\":\"" + value + "\"}");
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Gets the complete timeline of events for a transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of events ordered chronologically
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> getTransactionTimeline(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        String sql = "SELECT " + String.join(", ", ALL_COLUMNS) + 
                     " FROM " + TABLE_NAME + 
                     " WHERE transaction_id = ? " +
                     "ORDER BY created_at ASC";
        
        return executeQuery(sql, this::mapRows, transactionId);
    }

    /**
     * Gets the audit trail for a specific time period.
     *
     * @param startTime The start of the time period
     * @param endTime The end of the time period
     * @param filterParams Additional filtering parameters
     * @return List of events within the specified time period
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> getAuditTrail(Instant startTime, Instant endTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (startTime == null && endTime == null) {
            throw new IllegalArgumentException("At least one of startTime or endTime must be provided");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e");
        
        if (startTime != null && endTime != null) {
            builder.where("e.created_at BETWEEN ? AND ?")
                   .addParameter(Timestamp.from(startTime))
                   .addParameter(Timestamp.from(endTime));
        } else if (startTime != null) {
            builder.where("e.created_at >= ?")
                   .addParameter(Timestamp.from(startTime));
        } else {
            builder.where("e.created_at <= ?")
                   .addParameter(Timestamp.from(endTime));
        }
        
        // Apply organization and account filters if provided
        if (filterParams != null) {
            if (filterParams.getOrganizationId() != null) {
                builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id")
                       .and("t.organization_id = ?")
                       .addParameter(filterParams.getOrganizationId());
            }
            
            if (filterParams.getAccountId() != null) {
                if (!builder.getQueryString().contains("payment_transaction t")) {
                    builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id");
                }
                builder.and("t.account_id = ?")
                       .addParameter(filterParams.getAccountId());
            }
            
            // Apply sorting
            if (filterParams.getSortCriteria() != null && !filterParams.getSortCriteria().isEmpty()) {
                for (PaymentFilterParams.SortCriteria criteria : filterParams.getSortCriteria()) {
                    String column = criteria.getColumn();
                    // Prefix column with alias if not already prefixed
                    if (!column.contains(".")) {
                        column = "e." + column;
                    }
                    builder.orderBy(column + " " + criteria.getDirection());
                }
            } else {
                // Default sort by created_at ascending for chronological order
                builder.orderBy("e.created_at ASC");
            }
            
            // Apply pagination
            if (filterParams.getPagination() != null) {
                builder.paginate(filterParams.getPagination().getLimit(), filterParams.getPagination().getOffset());
            }
        } else {
            // Default sort by created_at ascending for chronological order
            builder.orderBy("e.created_at ASC");
        }
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Gets the user activity log for a specific user.
     *
     * @param userId The user identifier
     * @param startTime The start of the time period
     * @param endTime The end of the time period
     * @param filterParams Additional filtering parameters
     * @return List of events created by the specified user within the time period
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentEvent> getUserActivityLog(String userId, Instant startTime, Instant endTime, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (startTime == null && endTime == null) {
            throw new IllegalArgumentException("At least one of startTime or endTime must be provided");
        }
        
        PaymentQueryBuilder builder = PaymentQueryBuilder.create()
            .select(prefixColumns("e", ALL_COLUMNS))
            .from(TABLE_NAME + " e")
            .where("e.created_by = ?")
            .addParameter(userId);
        
        if (startTime != null && endTime != null) {
            builder.and("e.created_at BETWEEN ? AND ?")
                   .addParameter(Timestamp.from(startTime))
                   .addParameter(Timestamp.from(endTime));
        } else if (startTime != null) {
            builder.and("e.created_at >= ?")
                   .addParameter(Timestamp.from(startTime));
        } else {
            builder.and("e.created_at <= ?")
                   .addParameter(Timestamp.from(endTime));
        }
        
        applyCommonFilters(builder, filterParams);
        
        return executeQuery(builder, this::mapRows);
    }

    /**
     * Applies common filters to a query builder.
     *
     * @param builder The query builder
     * @param filterParams The filter parameters
     */
    private void applyCommonFilters(PaymentQueryBuilder builder, PaymentFilterParams filterParams) {
        if (filterParams != null) {
            // Apply organization and account filters if provided
            if (filterParams.getOrganizationId() != null) {
                builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id")
                       .and("t.organization_id = ?")
                       .addParameter(filterParams.getOrganizationId());
            }
            
            if (filterParams.getAccountId() != null) {
                if (!builder.getQueryString().contains("payment_transaction t")) {
                    builder.innerJoin("payment_transaction t", "e.transaction_id = t.transaction_id");
                }
                builder.and("t.account_id = ?")
                       .addParameter(filterParams.getAccountId());
            }
            
            // Apply date range filter
            if (filterParams.getDateRange() != null) {
                builder.dateRange("e.created_at", 
                        filterParams.getDateRange().getStartDate() != null ? 
                                filterParams.getDateRange().getStartDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                        filterParams.getDateRange().getEndDate() != null ? 
                                filterParams.getDateRange().getEndDate().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            }
            
            // Apply event type filter if provided in search term
            if (filterParams.getSearchTerm() != null && !filterParams.getSearchTerm().isEmpty()) {
                builder.and("(e.event_type LIKE ? OR e.event_data LIKE ?)")
                       .addParameter("%" + filterParams.getSearchTerm() + "%")
                       .addParameter("%" + filterParams.getSearchTerm() + "%");
            }
            
            // Apply sorting
            if (filterParams.getSortCriteria() != null && !filterParams.getSortCriteria().isEmpty()) {
                for (PaymentFilterParams.SortCriteria criteria : filterParams.getSortCriteria()) {
                    String column = criteria.getColumn();
                    // Prefix column with alias if not already prefixed
                    if (!column.contains(".")) {
                        column = "e." + column;
                    }
                    builder.orderBy(column + " " + criteria.getDirection());
                }
            } else {
                // Default sort by created_at ascending for chronological order
                builder.orderBy("e.created_at ASC");
            }
            
            // Apply pagination
            if (filterParams.getPagination() != null) {
                builder.paginate(filterParams.getPagination().getLimit(), filterParams.getPagination().getOffset());
            }
        } else {
            // Default sort by created_at ascending for chronological order
            builder.orderBy("e.created_at ASC");
        }
    }

    /**
     * Prefixes column names with an alias.
     *
     * @param alias The alias to prefix
     * @param columns The column names
     * @return The prefixed column names
     */
    private String[] prefixColumns(String alias, String[] columns) {
        String[] prefixed = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            prefixed[i] = alias + "." + columns[i];
        }
        return prefixed;
    }
}