package io.briklabs.sample.payments.data.dao.impl;

import io.briklabs.sample.payments.data.ConnectionManager;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.exception.PaymentDataAccessException;
import io.briklabs.sample.payments.data.exception.PaymentEntityNotFoundException;
import io.briklabs.sample.payments.data.exception.PaymentInvalidStateException;
import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaginationParams;
import io.briklabs.sample.payments.data.query.SortCriteria;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the PaymentTransactionDAO interface that handles all database operations
 * for payment transactions. This class provides CRUD operations, complex filtering, status updates,
 * and specialized queries for payment transactions.
 * 
 * It uses the PaymentQueryBuilder to construct optimized SQL queries for transaction filtering and
 * implements all transaction lifecycle methods required by the payment service layer.
 */
public class PaymentTransactionDaoImpl implements PaymentTransactionDAO {
    
    private static final Logger LOGGER = Logger.getLogger(PaymentTransactionDaoImpl.class.getName());
    
    // SQL constants for table and column names
    private static final String TABLE_NAME = "payment_transaction";
    private static final String ID_COLUMN = "transaction_id";
    private static final String ORG_ID_COLUMN = "organization_id";
    private static final String ACCOUNT_ID_COLUMN = "account_id";
    private static final String STATUS_COLUMN = "status";
    private static final String AMOUNT_COLUMN = "amount";
    private static final String CURRENCY_COLUMN = "currency";
    private static final String CREATED_AT_COLUMN = "created_at";
    private static final String UPDATED_AT_COLUMN = "updated_at";
    private static final String MERCHANT_ID_COLUMN = "merchant_id";
    private static final String PAYMENT_TYPE_COLUMN = "payment_type";
    private static final String REFERENCE_COLUMN = "transaction_reference";
    private static final String DESCRIPTION_COLUMN = "description";
    
    // SQL query templates
    private static final String INSERT_QUERY = 
            "INSERT INTO " + TABLE_NAME + " (" + 
            ID_COLUMN + ", " + 
            ORG_ID_COLUMN + ", " + 
            ACCOUNT_ID_COLUMN + ", " + 
            STATUS_COLUMN + ", " + 
            AMOUNT_COLUMN + ", " + 
            CURRENCY_COLUMN + ", " + 
            CREATED_AT_COLUMN + ", " + 
            UPDATED_AT_COLUMN + ", " + 
            MERCHANT_ID_COLUMN + ", " + 
            PAYMENT_TYPE_COLUMN + ", " + 
            REFERENCE_COLUMN + ", " + 
            DESCRIPTION_COLUMN + 
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_BY_ID_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + " = ?";
    
    private static final String UPDATE_QUERY = 
            "UPDATE " + TABLE_NAME + " SET " + 
            ORG_ID_COLUMN + " = ?, " + 
            ACCOUNT_ID_COLUMN + " = ?, " + 
            STATUS_COLUMN + " = ?, " + 
            AMOUNT_COLUMN + " = ?, " + 
            CURRENCY_COLUMN + " = ?, " + 
            UPDATED_AT_COLUMN + " = ?, " + 
            MERCHANT_ID_COLUMN + " = ?, " + 
            PAYMENT_TYPE_COLUMN + " = ?, " + 
            REFERENCE_COLUMN + " = ?, " + 
            DESCRIPTION_COLUMN + " = ? " + 
            "WHERE " + ID_COLUMN + " = ?";
    
    private static final String UPDATE_STATUS_QUERY = 
            "UPDATE " + TABLE_NAME + " SET " + 
            STATUS_COLUMN + " = ?, " + 
            UPDATED_AT_COLUMN + " = ? " + 
            "WHERE " + ID_COLUMN + " = ?";
    
    private static final String DELETE_QUERY = 
            "DELETE FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + " = ?";
    
    private static final String SELECT_BY_STATUS_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " = ?";
    
    private static final String SELECT_BY_STATUS_IN_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " IN (%s)";
    
    private static final String SELECT_BY_STATUS_AND_DATE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " = ? AND " + 
            CREATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_ORG_STATUS_AND_DATE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? AND " + 
            STATUS_COLUMN + " = ? AND " + CREATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_ACCOUNT_STATUS_AND_DATE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ? AND " + 
            STATUS_COLUMN + " = ? AND " + CREATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_AMOUNT_RANGE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + AMOUNT_COLUMN + " BETWEEN ? AND ? AND " + 
            CURRENCY_COLUMN + " = ?";
    
    private static final String SELECT_BY_MERCHANT_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + MERCHANT_ID_COLUMN + " = ?";
    
    private static final String SELECT_BY_MERCHANT_AND_STATUS_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + MERCHANT_ID_COLUMN + " = ? AND " + 
            STATUS_COLUMN + " = ?";
    
    private static final String SELECT_BY_PAYMENT_TYPE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + PAYMENT_TYPE_COLUMN + " = ?";
    
    private static final String SELECT_BY_REFERENCE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + REFERENCE_COLUMN + " = ?";
    
    private static final String SELECT_BY_CREATION_DATE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + CREATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_UPDATE_DATE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + UPDATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_ORG_WITH_PAGINATION_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? " + 
            "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
    
    private static final String SELECT_BY_ACCOUNT_WITH_PAGINATION_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ? " + 
            "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
    
    private static final String SELECT_BY_ORG_AND_ACCOUNT_WITH_PAGINATION_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? AND " + 
            ACCOUNT_ID_COLUMN + " = ? ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
    
    private static final String SEARCH_BY_TEXT_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + REFERENCE_COLUMN + " LIKE ? OR " + 
            DESCRIPTION_COLUMN + " LIKE ?";
    
    private static final String SELECT_FOR_SETTLEMENT_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " = ? AND " + 
            CREATED_AT_COLUMN + " <= ?";
    
    private static final String SELECT_EXPIRING_AUTHORIZATIONS_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " = ? AND " + 
            CREATED_AT_COLUMN + " <= ? AND " + STATUS_COLUMN + " NOT IN ('CAPTURED', 'PARTIALLY_CAPTURED', 'VOIDED', 'FAILED')";
    
    private static final String AGGREGATE_AMOUNTS_BY_STATUS_BASE_QUERY = 
            "SELECT " + STATUS_COLUMN + ", SUM(" + AMOUNT_COLUMN + ") as total_amount " + 
            "FROM " + TABLE_NAME + " WHERE 1=1";
    
    private static final String AGGREGATE_COUNTS_BY_STATUS_BASE_QUERY = 
            "SELECT " + STATUS_COLUMN + ", COUNT(*) as count " + 
            "FROM " + TABLE_NAME + " WHERE 1=1";
    
    private static final String SELECT_FOR_RECONCILIATION_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " = 'CAPTURED' AND " + 
            UPDATED_AT_COLUMN + " <= ?";
    
    private static final String SELECT_BY_ORG_AND_DATE_RANGE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? AND " + 
            CREATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_ACCOUNT_AND_DATE_RANGE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ? AND " + 
            CREATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_MERCHANT_AND_DATE_RANGE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + MERCHANT_ID_COLUMN + " = ? AND " + 
            CREATED_AT_COLUMN + " BETWEEN ? AND ?";
    
    private static final String SELECT_BY_DESCRIPTION_CONTAINING_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + DESCRIPTION_COLUMN + " LIKE ?";
    
    private static final String SELECT_BY_REFERENCE_CONTAINING_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + REFERENCE_COLUMN + " LIKE ?";
    
    private static final String SELECT_BY_ORG_AND_PAYMENT_TYPE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? AND " + 
            PAYMENT_TYPE_COLUMN + " = ?";
    
    private static final String SELECT_BY_ACCOUNT_AND_PAYMENT_TYPE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ? AND " + 
            PAYMENT_TYPE_COLUMN + " = ?";
    
    private static final String SELECT_BY_MERCHANT_AND_PAYMENT_TYPE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + MERCHANT_ID_COLUMN + " = ? AND " + 
            PAYMENT_TYPE_COLUMN + " = ?";
    
    private static final String SELECT_BY_CURRENCY_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + CURRENCY_COLUMN + " = ?";
    
    private static final String SELECT_BY_CURRENCY_AND_STATUS_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + CURRENCY_COLUMN + " = ? AND " + 
            STATUS_COLUMN + " = ?";
    
    private static final String SELECT_BY_ORG_CURRENCY_AND_STATUS_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? AND " + 
            CURRENCY_COLUMN + " = ? AND " + STATUS_COLUMN + " = ?";
    
    private static final String SELECT_BY_ACCOUNT_CURRENCY_AND_STATUS_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ? AND " + 
            CURRENCY_COLUMN + " = ? AND " + STATUS_COLUMN + " = ?";
    
    private static final String SELECT_LATEST_BY_ACCOUNT_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ? " + 
            "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT 1";
    
    private static final String SELECT_LATEST_BY_MERCHANT_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + MERCHANT_ID_COLUMN + " = ? " + 
            "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT 1";
    
    private static final String SELECT_BY_STATUS_CREATED_BEFORE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " = ? AND " + 
            CREATED_AT_COLUMN + " <= ?";
    
    private static final String SELECT_BY_STATUS_UPDATED_BEFORE_QUERY = 
            "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS_COLUMN + " = ? AND " + 
            UPDATED_AT_COLUMN + " <= ?";
    
    private static final String SELECT_BY_ORG_WITH_COUNT_QUERY = 
            "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ?";
    
    private static final String SELECT_BY_ACCOUNT_WITH_COUNT_QUERY = 
            "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ?";
    
    private static final String SELECT_BY_ORG_AND_ACCOUNT_WITH_COUNT_QUERY = 
            "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? AND " + 
            ACCOUNT_ID_COLUMN + " = ?";
    
    private static final String EXISTS_QUERY = 
            "SELECT 1 FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + " = ? LIMIT 1";
    
    // Connection manager for database operations
    private final ConnectionManager connectionManager;
    
    /**
     * Constructs a new PaymentTransactionDaoImpl with the specified connection manager.
     * 
     * @param connectionManager The connection manager to use for database operations
     */
    public PaymentTransactionDaoImpl(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    /**
     * Maps a ResultSet row to a PaymentTransaction object.
     * 
     * @param rs The ResultSet containing the transaction data
     * @return A PaymentTransaction object populated with data from the ResultSet
     * @throws SQLException if a database error occurs
     */
    private PaymentTransaction mapRowToTransaction(ResultSet rs) throws SQLException {
        PaymentTransaction transaction = new PaymentTransaction();
        
        transaction.setTransactionId(UUID.fromString(rs.getString(ID_COLUMN)));
        transaction.setOrganizationId(UUID.fromString(rs.getString(ORG_ID_COLUMN)));
        transaction.setAccountId(UUID.fromString(rs.getString(ACCOUNT_ID_COLUMN)));
        transaction.setStatus(PaymentStatus.valueOf(rs.getString(STATUS_COLUMN)));
        transaction.setAmount(rs.getBigDecimal(AMOUNT_COLUMN));
        transaction.setCurrency(rs.getString(CURRENCY_COLUMN));
        transaction.setCreatedAt(rs.getTimestamp(CREATED_AT_COLUMN).toLocalDateTime());
        transaction.setUpdatedAt(rs.getTimestamp(UPDATED_AT_COLUMN).toLocalDateTime());
        transaction.setMerchantId(rs.getString(MERCHANT_ID_COLUMN));
        transaction.setPaymentType(rs.getString(PAYMENT_TYPE_COLUMN));
        transaction.setTransactionReference(rs.getString(REFERENCE_COLUMN));
        transaction.setDescription(rs.getString(DESCRIPTION_COLUMN));
        
        return transaction;
    }
    
    /**
     * Sets parameters for a PreparedStatement based on a PaymentTransaction object.
     * 
     * @param ps The PreparedStatement to set parameters on
     * @param transaction The PaymentTransaction containing the parameter values
     * @param includeId Whether to include the ID as a parameter (for updates)
     * @throws SQLException if a database error occurs
     */
    private void setStatementParameters(PreparedStatement ps, PaymentTransaction transaction, boolean includeId) 
            throws SQLException {
        int paramIndex = 1;
        
        if (!includeId) {
            ps.setString(paramIndex++, transaction.getTransactionId().toString());
        }
        
        ps.setString(paramIndex++, transaction.getOrganizationId().toString());
        ps.setString(paramIndex++, transaction.getAccountId().toString());
        ps.setString(paramIndex++, transaction.getStatus().name());
        ps.setBigDecimal(paramIndex++, transaction.getAmount());
        ps.setString(paramIndex++, transaction.getCurrency());
        
        if (!includeId) {
            ps.setTimestamp(paramIndex++, Timestamp.valueOf(transaction.getCreatedAt()));
        }
        
        ps.setTimestamp(paramIndex++, Timestamp.valueOf(
                includeId ? LocalDateTime.now() : transaction.getUpdatedAt()));
        ps.setString(paramIndex++, transaction.getMerchantId());
        ps.setString(paramIndex++, transaction.getPaymentType());
        
        if (transaction.getTransactionReference() != null) {
            ps.setString(paramIndex++, transaction.getTransactionReference());
        } else {
            ps.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (transaction.getDescription() != null) {
            ps.setString(paramIndex++, transaction.getDescription());
        } else {
            ps.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (includeId) {
            ps.setString(paramIndex, transaction.getTransactionId().toString());
        }
    }
    
    /**
     * Builds a dynamic WHERE clause for filtering transactions based on multiple criteria.
     * 
     * @param organizationId The organization ID (optional)
     * @param accountId The account ID (optional)
     * @param statuses The set of statuses to filter by (optional)
     * @param startDate The start date (optional)
     * @param endDate The end date (optional)
     * @param minAmount The minimum amount (optional)
     * @param maxAmount The maximum amount (optional)
     * @param currency The currency code (optional)
     * @param merchantId The merchant ID (optional)
     * @param paymentType The payment type (optional)
     * @param params The list to populate with parameter values
     * @return The WHERE clause SQL string
     */
    private String buildWhereClause(UUID organizationId, UUID accountId, Set<PaymentStatus> statuses,
                                   LocalDateTime startDate, LocalDateTime endDate,
                                   BigDecimal minAmount, BigDecimal maxAmount, String currency,
                                   String merchantId, String paymentType,
                                   List<Object> params) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        
        if (organizationId != null) {
            whereClause.append(" AND ").append(ORG_ID_COLUMN).append(" = ?");
            params.add(organizationId.toString());
        }
        
        if (accountId != null) {
            whereClause.append(" AND ").append(ACCOUNT_ID_COLUMN).append(" = ?");
            params.add(accountId.toString());
        }
        
        if (statuses != null && !statuses.isEmpty()) {
            whereClause.append(" AND ").append(STATUS_COLUMN).append(" IN (");
            boolean first = true;
            for (PaymentStatus status : statuses) {
                if (!first) {
                    whereClause.append(", ");
                }
                whereClause.append("?");
                params.add(status.name());
                first = false;
            }
            whereClause.append(")");
        }
        
        if (startDate != null && endDate != null) {
            whereClause.append(" AND ").append(CREATED_AT_COLUMN).append(" BETWEEN ? AND ?");
            params.add(Timestamp.valueOf(startDate));
            params.add(Timestamp.valueOf(endDate));
        } else if (startDate != null) {
            whereClause.append(" AND ").append(CREATED_AT_COLUMN).append(" >= ?");
            params.add(Timestamp.valueOf(startDate));
        } else if (endDate != null) {
            whereClause.append(" AND ").append(CREATED_AT_COLUMN).append(" <= ?");
            params.add(Timestamp.valueOf(endDate));
        }
        
        if (minAmount != null && maxAmount != null && currency != null) {
            whereClause.append(" AND ").append(AMOUNT_COLUMN).append(" BETWEEN ? AND ? AND ")
                      .append(CURRENCY_COLUMN).append(" = ?");
            params.add(minAmount);
            params.add(maxAmount);
            params.add(currency);
        } else if (minAmount != null && currency != null) {
            whereClause.append(" AND ").append(AMOUNT_COLUMN).append(" >= ? AND ")
                      .append(CURRENCY_COLUMN).append(" = ?");
            params.add(minAmount);
            params.add(currency);
        } else if (maxAmount != null && currency != null) {
            whereClause.append(" AND ").append(AMOUNT_COLUMN).append(" <= ? AND ")
                      .append(CURRENCY_COLUMN).append(" = ?");
            params.add(maxAmount);
            params.add(currency);
        } else if (currency != null) {
            whereClause.append(" AND ").append(CURRENCY_COLUMN).append(" = ?");
            params.add(currency);
        }
        
        if (merchantId != null) {
            whereClause.append(" AND ").append(MERCHANT_ID_COLUMN).append(" = ?");
            params.add(merchantId);
        }
        
        if (paymentType != null) {
            whereClause.append(" AND ").append(PAYMENT_TYPE_COLUMN).append(" = ?");
            params.add(paymentType);
        }
        
        return whereClause.toString();
    }
    
    /**
     * Builds a dynamic ORDER BY clause for sorting transactions.
     * 
     * @param sortField The field to sort by
     * @param sortDirection The sort direction (ASC or DESC)
     * @return The ORDER BY clause SQL string
     */
    private String buildOrderByClause(String sortField, String sortDirection) {
        StringBuilder orderByClause = new StringBuilder(" ORDER BY ");
        
        if (sortField != null) {
            // Validate and sanitize the sort field to prevent SQL injection
            String validatedField;
            switch (sortField.toLowerCase()) {
                case "created_at":
                    validatedField = CREATED_AT_COLUMN;
                    break;
                case "updated_at":
                    validatedField = UPDATED_AT_COLUMN;
                    break;
                case "amount":
                    validatedField = AMOUNT_COLUMN;
                    break;
                case "status":
                    validatedField = STATUS_COLUMN;
                    break;
                case "merchant_id":
                    validatedField = MERCHANT_ID_COLUMN;
                    break;
                case "payment_type":
                    validatedField = PAYMENT_TYPE_COLUMN;
                    break;
                default:
                    // Default to created_at if invalid field is provided
                    validatedField = CREATED_AT_COLUMN;
            }
            
            orderByClause.append(validatedField);
        } else {
            // Default sort field
            orderByClause.append(CREATED_AT_COLUMN);
        }
        
        // Validate sort direction
        if (sortDirection != null && sortDirection.equalsIgnoreCase("ASC")) {
            orderByClause.append(" ASC");
        } else {
            // Default to DESC
            orderByClause.append(" DESC");
        }
        
        return orderByClause.toString();
    }
    
    /**
     * Builds a dynamic LIMIT and OFFSET clause for pagination.
     * 
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     * @return The LIMIT and OFFSET clause SQL string
     */
    private String buildLimitOffsetClause(int limit, int offset) {
        // Validate limit and offset
        int validLimit = Math.max(1, Math.min(limit, 1000)); // Limit between 1 and 1000
        int validOffset = Math.max(0, offset); // Offset must be non-negative
        
        return " LIMIT " + validLimit + " OFFSET " + validOffset;
    }
    
    /**
     * Executes a query and maps the results to a list of PaymentTransaction objects.
     * 
     * @param sql The SQL query to execute
     * @param params The parameters for the query
     * @return A list of PaymentTransaction objects
     * @throws PaymentDataAccessException if a database error occurs
     */
    private List<PaymentTransaction> executeQuery(String sql, Object... params) {
        List<PaymentTransaction> transactions = new ArrayList<>();
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < params.length; i++) {
                setParameter(ps, i + 1, params[i]);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing query: " + sql, e);
            throw new PaymentDataAccessException("Error executing query", e);
        }
        
        return transactions;
    }
    
    /**
     * Sets a parameter on a PreparedStatement with the appropriate type.
     * 
     * @param ps The PreparedStatement to set the parameter on
     * @param index The parameter index
     * @param value The parameter value
     * @throws SQLException if a database error occurs
     */
    private void setParameter(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            ps.setString(index, (String) value);
        } else if (value instanceof UUID) {
            ps.setString(index, value.toString());
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof BigDecimal) {
            ps.setBigDecimal(index, (BigDecimal) value);
        } else if (value instanceof LocalDateTime) {
            ps.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else if (value instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof Boolean) {
            ps.setBoolean(index, (Boolean) value);
        } else if (value instanceof PaymentStatus) {
            ps.setString(index, ((PaymentStatus) value).name());
        } else {
            ps.setObject(index, value);
        }
    }
    
    /**
     * Executes a count query and returns the result.
     * 
     * @param sql The SQL query to execute
     * @param params The parameters for the query
     * @return The count result
     * @throws PaymentDataAccessException if a database error occurs
     */
    private long executeCountQuery(String sql, Object... params) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < params.length; i++) {
                setParameter(ps, i + 1, params[i]);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing count query: " + sql, e);
            throw new PaymentDataAccessException("Error executing count query", e);
        }
    }
    
    @Override
    public PaymentTransaction create(PaymentTransaction transaction) {
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(UUID.randomUUID());
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(now);
        }
        if (transaction.getUpdatedAt() == null) {
            transaction.setUpdatedAt(now);
        }
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_QUERY)) {
            
            setStatementParameters(ps, transaction, false);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected != 1) {
                throw new PaymentDataAccessException("Failed to create transaction, affected rows: " + rowsAffected);
            }
            
            return transaction;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating transaction: " + transaction.getTransactionId(), e);
            throw new PaymentDataAccessException("Error creating transaction", e);
        }
    }
    
    @Override
    public Optional<PaymentTransaction> findById(UUID id) {
        try {
            List<PaymentTransaction> transactions = executeQuery(SELECT_BY_ID_QUERY, id.toString());
            return transactions.isEmpty() ? Optional.empty() : Optional.of(transactions.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding transaction by ID: " + id, e);
            throw new PaymentDataAccessException("Error finding transaction by ID", e);
        }
    }
    
    @Override
    public PaymentTransaction update(PaymentTransaction transaction) {
        // Check if transaction exists
        if (!exists(transaction.getTransactionId())) {
            throw new PaymentEntityNotFoundException("Transaction not found: " + transaction.getTransactionId());
        }
        
        transaction.setUpdatedAt(LocalDateTime.now());
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_QUERY)) {
            
            setStatementParameters(ps, transaction, true);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected != 1) {
                throw new PaymentDataAccessException("Failed to update transaction, affected rows: " + rowsAffected);
            }
            
            return transaction;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating transaction: " + transaction.getTransactionId(), e);
            throw new PaymentDataAccessException("Error updating transaction", e);
        }
    }
    
    @Override
    public boolean delete(UUID id) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_QUERY)) {
            
            ps.setString(1, id.toString());
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected == 1;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting transaction: " + id, e);
            throw new PaymentDataAccessException("Error deleting transaction", e);
        }
    }
    
    @Override
    public List<PaymentTransaction> query(Object params) {
        if (!(params instanceof PaymentFilterParams)) {
            throw new IllegalArgumentException("Query params must be of type PaymentFilterParams");
        }
        
        return findByFilterParams((PaymentFilterParams) params);
    }
    
    @Override
    public Connection beginTransaction() {
        try {
            Connection conn = connectionManager.getConnection();
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error beginning transaction", e);
            throw new PaymentDataAccessException("Error beginning transaction", e);
        }
    }
    
    @Override
    public void commitTransaction(Connection connection) {
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error committing transaction", e);
            throw new PaymentDataAccessException("Error committing transaction", e);
        }
    }
    
    @Override
    public void rollbackTransaction(Connection connection) {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error rolling back transaction", e);
            throw new PaymentDataAccessException("Error rolling back transaction", e);
        }
    }
    
    @Override
    public List<PaymentTransaction> batchCreate(List<PaymentTransaction> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(INSERT_QUERY)) {
                for (PaymentTransaction transaction : entities) {
                    if (transaction.getTransactionId() == null) {
                        transaction.setTransactionId(UUID.randomUUID());
                    }
                    
                    LocalDateTime now = LocalDateTime.now();
                    if (transaction.getCreatedAt() == null) {
                        transaction.setCreatedAt(now);
                    }
                    if (transaction.getUpdatedAt() == null) {
                        transaction.setUpdatedAt(now);
                    }
                    
                    setStatementParameters(ps, transaction, false);
                    ps.addBatch();
                }
                
                int[] results = ps.executeBatch();
                
                // Check if all inserts were successful
                for (int i = 0; i < results.length; i++) {
                    if (results[i] != 1 && results[i] != Statement.SUCCESS_NO_INFO) {
                        conn.rollback();
                        throw new PaymentDataAccessException("Failed to create transaction at index " + i);
                    }
                }
                
                conn.commit();
                return entities;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in batch create", e);
            throw new PaymentDataAccessException("Error in batch create", e);
        }
    }
    
    @Override
    public List<PaymentTransaction> batchUpdate(List<PaymentTransaction> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_QUERY)) {
                for (PaymentTransaction transaction : entities) {
                    // Check if transaction exists
                    if (!exists(transaction.getTransactionId())) {
                        conn.rollback();
                        throw new PaymentEntityNotFoundException("Transaction not found: " + transaction.getTransactionId());
                    }
                    
                    transaction.setUpdatedAt(LocalDateTime.now());
                    setStatementParameters(ps, transaction, true);
                    ps.addBatch();
                }
                
                int[] results = ps.executeBatch();
                
                // Check if all updates were successful
                for (int i = 0; i < results.length; i++) {
                    if (results[i] != 1 && results[i] != Statement.SUCCESS_NO_INFO) {
                        conn.rollback();
                        throw new PaymentDataAccessException("Failed to update transaction at index " + i);
                    }
                }
                
                conn.commit();
                return entities;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in batch update", e);
            throw new PaymentDataAccessException("Error in batch update", e);
        }
    }
    
    @Override
    public <R> R executeInTransaction(TransactionOperation<R> operation) {
        Connection conn = null;
        try {
            conn = beginTransaction();
            R result = operation.execute(conn);
            commitTransaction(conn);
            return result;
        } catch (Exception e) {
            if (conn != null) {
                rollbackTransaction(conn);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new PaymentDataAccessException("Error executing in transaction", e);
        }
    }
    
    @Override
    public long count(Object params) {
        if (!(params instanceof PaymentFilterParams)) {
            throw new IllegalArgumentException("Count params must be of type PaymentFilterParams");
        }
        
        return countByFilterParams((PaymentFilterParams) params);
    }
    
    @Override
    public boolean exists(UUID id) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXISTS_QUERY)) {
            
            ps.setString(1, id.toString());
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if transaction exists: " + id, e);
            throw new PaymentDataAccessException("Error checking if transaction exists", e);
        }
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationId(UUID organizationId) {
        return executeQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ?", 
                organizationId.toString());
    }
    
    @Override
    public List<PaymentTransaction> findByAccountId(UUID accountId) {
        return executeQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + ACCOUNT_ID_COLUMN + " = ?", 
                accountId.toString());
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationAndAccountId(UUID organizationId, UUID accountId) {
        return executeQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + ORG_ID_COLUMN + " = ? AND " + 
                ACCOUNT_ID_COLUMN + " = ?", organizationId.toString(), accountId.toString());
    }
    
    @Override
    public List<PaymentTransaction> findByStatus(PaymentStatus status) {
        return executeQuery(SELECT_BY_STATUS_QUERY, status.name());
    }
    
    @Override
    public List<PaymentTransaction> findByStatusIn(Set<PaymentStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Create placeholders for the IN clause
        String placeholders = statuses.stream()
                .map(s -> "?")
                .collect(Collectors.joining(", "));
        
        String query = String.format(SELECT_BY_STATUS_IN_QUERY, placeholders);
        
        // Convert statuses to parameter array
        Object[] params = statuses.stream()
                .map(PaymentStatus::name)
                .toArray();
        
        return executeQuery(query, params);
    }
    
    @Override
    public List<PaymentTransaction> findByStatusAndDate(PaymentStatus status, DateRangeFilter dateRange) {
        if (dateRange == null || dateRange.getStartDate() == null || dateRange.getEndDate() == null) {
            throw new IllegalArgumentException("Date range must be provided with start and end dates");
        }
        
        return executeQuery(SELECT_BY_STATUS_AND_DATE_QUERY, 
                status.name(), 
                Timestamp.valueOf(dateRange.getStartDate()), 
                Timestamp.valueOf(dateRange.getEndDate()));
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationStatusAndDate(UUID organizationId, PaymentStatus status, 
            DateRangeFilter dateRange) {
        if (dateRange == null || dateRange.getStartDate() == null || dateRange.getEndDate() == null) {
            throw new IllegalArgumentException("Date range must be provided with start and end dates");
        }
        
        return executeQuery(SELECT_BY_ORG_STATUS_AND_DATE_QUERY, 
                organizationId.toString(), 
                status.name(), 
                Timestamp.valueOf(dateRange.getStartDate()), 
                Timestamp.valueOf(dateRange.getEndDate()));
    }
    
    @Override
    public List<PaymentTransaction> findByAccountStatusAndDate(UUID accountId, PaymentStatus status, 
            DateRangeFilter dateRange) {
        if (dateRange == null || dateRange.getStartDate() == null || dateRange.getEndDate() == null) {
            throw new IllegalArgumentException("Date range must be provided with start and end dates");
        }
        
        return executeQuery(SELECT_BY_ACCOUNT_STATUS_AND_DATE_QUERY, 
                accountId.toString(), 
                status.name(), 
                Timestamp.valueOf(dateRange.getStartDate()), 
                Timestamp.valueOf(dateRange.getEndDate()));
    }
    
    @Override
    public List<PaymentTransaction> findByAmount(AmountRangeFilter amountRange) {
        if (amountRange == null || amountRange.getMinAmount() == null || 
                amountRange.getMaxAmount() == null || amountRange.getCurrency() == null) {
            throw new IllegalArgumentException("Amount range must be provided with min, max, and currency");
        }
        
        return executeQuery(SELECT_BY_AMOUNT_RANGE_QUERY, 
                amountRange.getMinAmount(), 
                amountRange.getMaxAmount(), 
                amountRange.getCurrency());
    }
    
    @Override
    public List<PaymentTransaction> findByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, String currency) {
        if (minAmount == null || maxAmount == null || currency == null) {
            throw new IllegalArgumentException("Min amount, max amount, and currency must be provided");
        }
        
        return executeQuery(SELECT_BY_AMOUNT_RANGE_QUERY, minAmount, maxAmount, currency);
    }
    
    @Override
    public List<PaymentTransaction> findByMerchant(String merchantId) {
        return executeQuery(SELECT_BY_MERCHANT_QUERY, merchantId);
    }
    
    @Override
    public List<PaymentTransaction> findByMerchantAndStatus(String merchantId, PaymentStatus status) {
        return executeQuery(SELECT_BY_MERCHANT_AND_STATUS_QUERY, merchantId, status.name());
    }
    
    @Override
    public List<PaymentTransaction> findByPaymentType(String paymentType) {
        return executeQuery(SELECT_BY_PAYMENT_TYPE_QUERY, paymentType);
    }
    
    @Override
    public List<PaymentTransaction> findByReference(String reference) {
        return executeQuery(SELECT_BY_REFERENCE_QUERY, reference);
    }
    
    @Override
    public List<PaymentTransaction> findByCreationDate(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must be provided");
        }
        
        return executeQuery(SELECT_BY_CREATION_DATE_QUERY, 
                Timestamp.valueOf(startDate), 
                Timestamp.valueOf(endDate));
    }
    
    @Override
    public List<PaymentTransaction> findByUpdateDate(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must be provided");
        }
        
        return executeQuery(SELECT_BY_UPDATE_DATE_QUERY, 
                Timestamp.valueOf(startDate), 
                Timestamp.valueOf(endDate));
    }
    
    @Override
    public PaymentTransaction updateStatus(UUID transactionId, PaymentStatus newStatus) {
        Optional<PaymentTransaction> existingOpt = findById(transactionId);
        if (!existingOpt.isPresent()) {
            throw new PaymentEntityNotFoundException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction existing = existingOpt.get();
        
        // Validate state transition
        if (!existing.canTransitionTo(newStatus)) {
            throw new PaymentInvalidStateException(
                    "Invalid state transition from " + existing.getStatus() + " to " + newStatus);
        }
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_QUERY)) {
            
            LocalDateTime now = LocalDateTime.now();
            
            ps.setString(1, newStatus.name());
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setString(3, transactionId.toString());
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected != 1) {
                throw new PaymentDataAccessException("Failed to update transaction status, affected rows: " + rowsAffected);
            }
            
            // Update the transaction object
            existing.setStatus(newStatus);
            existing.setUpdatedAt(now);
            
            return existing;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating transaction status: " + transactionId, e);
            throw new PaymentDataAccessException("Error updating transaction status", e);
        }
    }
    
    @Override
    public List<PaymentTransaction> findByFilterParams(PaymentFilterParams filterParams) {
        if (filterParams == null) {
            throw new IllegalArgumentException("Filter parameters must be provided");
        }
        
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NAME);
        
        // Build WHERE clause
        sql.append(buildWhereClause(
                filterParams.getOrganizationId(),
                filterParams.getAccountId(),
                filterParams.getStatuses(),
                filterParams.getDateRange() != null ? filterParams.getDateRange().getStartDate() : null,
                filterParams.getDateRange() != null ? filterParams.getDateRange().getEndDate() : null,
                filterParams.getAmountRange() != null ? filterParams.getAmountRange().getMinAmount() : null,
                filterParams.getAmountRange() != null ? filterParams.getAmountRange().getMaxAmount() : null,
                filterParams.getAmountRange() != null ? filterParams.getAmountRange().getCurrency() : null,
                filterParams.getMerchantId(),
                filterParams.getPaymentType(),
                params));
        
        // Build ORDER BY clause
        sql.append(buildOrderByClause(
                filterParams.getSortField(),
                filterParams.getSortDirection()));
        
        // Build LIMIT and OFFSET clause
        sql.append(buildLimitOffsetClause(
                filterParams.getLimit(),
                filterParams.getOffset()));
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    @Override
    public long countByFilterParams(PaymentFilterParams filterParams) {
        if (filterParams == null) {
            throw new IllegalArgumentException("Filter parameters must be provided");
        }
        
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + TABLE_NAME);
        
        // Build WHERE clause
        sql.append(buildWhereClause(
                filterParams.getOrganizationId(),
                filterParams.getAccountId(),
                filterParams.getStatuses(),
                filterParams.getDateRange() != null ? filterParams.getDateRange().getStartDate() : null,
                filterParams.getDateRange() != null ? filterParams.getDateRange().getEndDate() : null,
                filterParams.getAmountRange() != null ? filterParams.getAmountRange().getMinAmount() : null,
                filterParams.getAmountRange() != null ? filterParams.getAmountRange().getMaxAmount() : null,
                filterParams.getAmountRange() != null ? filterParams.getAmountRange().getCurrency() : null,
                filterParams.getMerchantId(),
                filterParams.getPaymentType(),
                params));
        
        return executeCountQuery(sql.toString(), params.toArray());
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationWithPagination(UUID organizationId, int limit, int offset) {
        return executeQuery(SELECT_BY_ORG_WITH_PAGINATION_QUERY, 
                organizationId.toString(), limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> findByAccountWithPagination(UUID accountId, int limit, int offset) {
        return executeQuery(SELECT_BY_ACCOUNT_WITH_PAGINATION_QUERY, 
                accountId.toString(), limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationAndAccountWithPagination(UUID organizationId, UUID accountId, 
            int limit, int offset) {
        return executeQuery(SELECT_BY_ORG_AND_ACCOUNT_WITH_PAGINATION_QUERY, 
                organizationId.toString(), accountId.toString(), limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> searchByText(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term must be provided");
        }
        
        String likePattern = "%" + searchTerm + "%";
        return executeQuery(SEARCH_BY_TEXT_QUERY, likePattern, likePattern);
    }
    
    @Override
    public List<PaymentTransaction> findTransactionsForSettlement(LocalDateTime cutoffTime) {
        return executeQuery(SELECT_FOR_SETTLEMENT_QUERY, 
                PaymentStatus.AUTHORIZED.name(), Timestamp.valueOf(cutoffTime));
    }
    
    @Override
    public List<PaymentTransaction> findExpiringAuthorizations(LocalDateTime expirationTime) {
        return executeQuery(SELECT_EXPIRING_AUTHORIZATIONS_QUERY, 
                PaymentStatus.AUTHORIZED.name(), Timestamp.valueOf(expirationTime));
    }
    
    @Override
    public Map<PaymentStatus, BigDecimal> aggregateAmountsByStatus(UUID organizationId, UUID accountId, 
            LocalDateTime startDate, LocalDateTime endDate) {
        
        StringBuilder sql = new StringBuilder(AGGREGATE_AMOUNTS_BY_STATUS_BASE_QUERY);
        List<Object> params = new ArrayList<>();
        
        if (organizationId != null) {
            sql.append(" AND ").append(ORG_ID_COLUMN).append(" = ?");
            params.add(organizationId.toString());
        }
        
        if (accountId != null) {
            sql.append(" AND ").append(ACCOUNT_ID_COLUMN).append(" = ?");
            params.add(accountId.toString());
        }
        
        if (startDate != null && endDate != null) {
            sql.append(" AND ").append(CREATED_AT_COLUMN).append(" BETWEEN ? AND ?");
            params.add(Timestamp.valueOf(startDate));
            params.add(Timestamp.valueOf(endDate));
        } else if (startDate != null) {
            sql.append(" AND ").append(CREATED_AT_COLUMN).append(" >= ?");
            params.add(Timestamp.valueOf(startDate));
        } else if (endDate != null) {
            sql.append(" AND ").append(CREATED_AT_COLUMN).append(" <= ?");
            params.add(Timestamp.valueOf(endDate));
        }
        
        sql.append(" GROUP BY ").append(STATUS_COLUMN);
        
        Map<PaymentStatus, BigDecimal> result = new EnumMap<>(PaymentStatus.class);
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                setParameter(ps, i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentStatus status = PaymentStatus.valueOf(rs.getString(STATUS_COLUMN));
                    BigDecimal totalAmount = rs.getBigDecimal("total_amount");
                    result.put(status, totalAmount);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error aggregating amounts by status", e);
            throw new PaymentDataAccessException("Error aggregating amounts by status", e);
        }
        
        return result;
    }
    
    @Override
    public Map<PaymentStatus, Long> aggregateCountsByStatus(UUID organizationId, UUID accountId, 
            LocalDateTime startDate, LocalDateTime endDate) {
        
        StringBuilder sql = new StringBuilder(AGGREGATE_COUNTS_BY_STATUS_BASE_QUERY);
        List<Object> params = new ArrayList<>();
        
        if (organizationId != null) {
            sql.append(" AND ").append(ORG_ID_COLUMN).append(" = ?");
            params.add(organizationId.toString());
        }
        
        if (accountId != null) {
            sql.append(" AND ").append(ACCOUNT_ID_COLUMN).append(" = ?");
            params.add(accountId.toString());
        }
        
        if (startDate != null && endDate != null) {
            sql.append(" AND ").append(CREATED_AT_COLUMN).append(" BETWEEN ? AND ?");
            params.add(Timestamp.valueOf(startDate));
            params.add(Timestamp.valueOf(endDate));
        } else if (startDate != null) {
            sql.append(" AND ").append(CREATED_AT_COLUMN).append(" >= ?");
            params.add(Timestamp.valueOf(startDate));
        } else if (endDate != null) {
            sql.append(" AND ").append(CREATED_AT_COLUMN).append(" <= ?");
            params.add(Timestamp.valueOf(endDate));
        }
        
        sql.append(" GROUP BY ").append(STATUS_COLUMN);
        
        Map<PaymentStatus, Long> result = new EnumMap<>(PaymentStatus.class);
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                setParameter(ps, i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentStatus status = PaymentStatus.valueOf(rs.getString(STATUS_COLUMN));
                    Long count = rs.getLong("count");
                    result.put(status, count);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error aggregating counts by status", e);
            throw new PaymentDataAccessException("Error aggregating counts by status", e);
        }
        
        return result;
    }
    
    @Override
    public List<PaymentTransaction> findTransactionsForReconciliation(LocalDateTime cutoffTime) {
        return executeQuery(SELECT_FOR_RECONCILIATION_QUERY, Timestamp.valueOf(cutoffTime));
    }
    
    @Override
    public List<PaymentTransaction> findByMultipleCriteria(UUID organizationId, UUID accountId, 
            Set<PaymentStatus> statuses, LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal minAmount, BigDecimal maxAmount, String currency,
            String merchantId, String paymentType,
            String sortField, String sortDirection,
            int limit, int offset) {
        
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NAME);
        
        // Build WHERE clause
        sql.append(buildWhereClause(
                organizationId, accountId, statuses,
                startDate, endDate,
                minAmount, maxAmount, currency,
                merchantId, paymentType,
                params));
        
        // Build ORDER BY clause
        sql.append(buildOrderByClause(sortField, sortDirection));
        
        // Build LIMIT and OFFSET clause
        sql.append(buildLimitOffsetClause(limit, offset));
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    @Override
    public long countByMultipleCriteria(UUID organizationId, UUID accountId, 
            Set<PaymentStatus> statuses, LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal minAmount, BigDecimal maxAmount, String currency,
            String merchantId, String paymentType) {
        
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + TABLE_NAME);
        
        // Build WHERE clause
        sql.append(buildWhereClause(
                organizationId, accountId, statuses,
                startDate, endDate,
                minAmount, maxAmount, currency,
                merchantId, paymentType,
                params));
        
        return executeCountQuery(sql.toString(), params.toArray());
    }
    
    @Override
    public List<PaymentTransaction> findTransactionsEligibleForStatus(PaymentStatus targetStatus) {
        // This is a more complex query that depends on the state transition rules
        // We'll implement a simplified version that gets transactions that could transition to the target status
        
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NAME + " WHERE ");
        List<Object> params = new ArrayList<>();
        
        switch (targetStatus) {
            case AUTHORIZED:
                // Transactions that can be authorized are in PENDING or PROCESSING status
                sql.append(STATUS_COLUMN).append(" IN (?, ?)");
                params.add(PaymentStatus.PENDING.name());
                params.add(PaymentStatus.PROCESSING.name());
                break;
                
            case CAPTURED:
                // Transactions that can be captured are in AUTHORIZED or PARTIALLY_CAPTURED status
                sql.append(STATUS_COLUMN).append(" IN (?, ?)");
                params.add(PaymentStatus.AUTHORIZED.name());
                params.add(PaymentStatus.PARTIALLY_CAPTURED.name());
                break;
                
            case PARTIALLY_CAPTURED:
                // Only AUTHORIZED transactions can be partially captured
                sql.append(STATUS_COLUMN).append(" = ?");
                params.add(PaymentStatus.AUTHORIZED.name());
                break;
                
            case REFUNDED:
                // Transactions that can be refunded are in CAPTURED or PARTIALLY_REFUNDED status
                sql.append(STATUS_COLUMN).append(" IN (?, ?)");
                params.add(PaymentStatus.CAPTURED.name());
                params.add(PaymentStatus.PARTIALLY_REFUNDED.name());
                break;
                
            case PARTIALLY_REFUNDED:
                // Only CAPTURED transactions can be partially refunded
                sql.append(STATUS_COLUMN).append(" = ?");
                params.add(PaymentStatus.CAPTURED.name());
                break;
                
            case VOIDED:
                // Only AUTHORIZED transactions can be voided
                sql.append(STATUS_COLUMN).append(" = ?");
                params.add(PaymentStatus.AUTHORIZED.name());
                break;
                
            default:
                // For other statuses, return empty list as they may have special rules
                return Collections.emptyList();
        }
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationAndStatusWithPagination(UUID organizationId, 
            PaymentStatus status, int limit, int offset) {
        
        String sql = "SELECT * FROM " + TABLE_NAME + 
                " WHERE " + ORG_ID_COLUMN + " = ? AND " + STATUS_COLUMN + " = ? " +
                "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
        
        return executeQuery(sql, organizationId.toString(), status.name(), limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> findByAccountAndStatusWithPagination(UUID accountId, 
            PaymentStatus status, int limit, int offset) {
        
        String sql = "SELECT * FROM " + TABLE_NAME + 
                " WHERE " + ACCOUNT_ID_COLUMN + " = ? AND " + STATUS_COLUMN + " = ? " +
                "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
        
        return executeQuery(sql, accountId.toString(), status.name(), limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationAccountAndStatusWithPagination(UUID organizationId, 
            UUID accountId, PaymentStatus status, int limit, int offset) {
        
        String sql = "SELECT * FROM " + TABLE_NAME + 
                " WHERE " + ORG_ID_COLUMN + " = ? AND " + ACCOUNT_ID_COLUMN + " = ? AND " + 
                STATUS_COLUMN + " = ? " +
                "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
        
        return executeQuery(sql, organizationId.toString(), accountId.toString(), status.name(), limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> findByMerchantWithPagination(String merchantId, int limit, int offset) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                " WHERE " + MERCHANT_ID_COLUMN + " = ? " +
                "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
        
        return executeQuery(sql, merchantId, limit, offset);
    }
    
    @Override
    public List<PaymentTransaction> findByMerchantAndStatusWithPagination(String merchantId, 
            PaymentStatus status, int limit, int offset) {
        
        String sql = "SELECT * FROM " + TABLE_NAME + 
                " WHERE " + MERCHANT_ID_COLUMN + " = ? AND " + STATUS_COLUMN + " = ? " +
                "ORDER BY " + CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?";
        
        return executeQuery(sql, merchantId, status.name(), limit, offset);
    }
    
    @Override
    public Optional<PaymentTransaction> findLatestByAccount(UUID accountId) {
        List<PaymentTransaction> transactions = executeQuery(SELECT_LATEST_BY_ACCOUNT_QUERY, accountId.toString());
        return transactions.isEmpty() ? Optional.empty() : Optional.of(transactions.get(0));
    }
    
    @Override
    public Optional<PaymentTransaction> findLatestByMerchant(String merchantId) {
        List<PaymentTransaction> transactions = executeQuery(SELECT_LATEST_BY_MERCHANT_QUERY, merchantId);
        return transactions.isEmpty() ? Optional.empty() : Optional.of(transactions.get(0));
    }
    
    @Override
    public List<PaymentTransaction> findByStatusCreatedBefore(PaymentStatus status, LocalDateTime cutoffTime) {
        return executeQuery(SELECT_BY_STATUS_CREATED_BEFORE_QUERY, 
                status.name(), Timestamp.valueOf(cutoffTime));
    }
    
    @Override
    public List<PaymentTransaction> findByStatusUpdatedBefore(PaymentStatus status, LocalDateTime cutoffTime) {
        return executeQuery(SELECT_BY_STATUS_UPDATED_BEFORE_QUERY, 
                status.name(), Timestamp.valueOf(cutoffTime));
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationAndDateRange(UUID organizationId, 
            LocalDateTime startDate, LocalDateTime endDate) {
        
        return executeQuery(SELECT_BY_ORG_AND_DATE_RANGE_QUERY, 
                organizationId.toString(), 
                Timestamp.valueOf(startDate), 
                Timestamp.valueOf(endDate));
    }
    
    @Override
    public List<PaymentTransaction> findByAccountAndDateRange(UUID accountId, 
            LocalDateTime startDate, LocalDateTime endDate) {
        
        return executeQuery(SELECT_BY_ACCOUNT_AND_DATE_RANGE_QUERY, 
                accountId.toString(), 
                Timestamp.valueOf(startDate), 
                Timestamp.valueOf(endDate));
    }
    
    @Override
    public List<PaymentTransaction> findByMerchantAndDateRange(String merchantId, 
            LocalDateTime startDate, LocalDateTime endDate) {
        
        return executeQuery(SELECT_BY_MERCHANT_AND_DATE_RANGE_QUERY, 
                merchantId, 
                Timestamp.valueOf(startDate), 
                Timestamp.valueOf(endDate));
    }
    
    @Override
    public List<PaymentTransaction> findByDescriptionContaining(String searchTerm) {
        return executeQuery(SELECT_BY_DESCRIPTION_CONTAINING_QUERY, "%" + searchTerm + "%");
    }
    
    @Override
    public List<PaymentTransaction> findByReferenceContaining(String searchTerm) {
        return executeQuery(SELECT_BY_REFERENCE_CONTAINING_QUERY, "%" + searchTerm + "%");
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationAndPaymentType(UUID organizationId, String paymentType) {
        return executeQuery(SELECT_BY_ORG_AND_PAYMENT_TYPE_QUERY, 
                organizationId.toString(), paymentType);
    }
    
    @Override
    public List<PaymentTransaction> findByAccountAndPaymentType(UUID accountId, String paymentType) {
        return executeQuery(SELECT_BY_ACCOUNT_AND_PAYMENT_TYPE_QUERY, 
                accountId.toString(), paymentType);
    }
    
    @Override
    public List<PaymentTransaction> findByMerchantAndPaymentType(String merchantId, String paymentType) {
        return executeQuery(SELECT_BY_MERCHANT_AND_PAYMENT_TYPE_QUERY, 
                merchantId, paymentType);
    }
    
    @Override
    public List<PaymentTransaction> findByCurrency(String currency) {
        return executeQuery(SELECT_BY_CURRENCY_QUERY, currency);
    }
    
    @Override
    public List<PaymentTransaction> findByCurrencyAndStatus(String currency, PaymentStatus status) {
        return executeQuery(SELECT_BY_CURRENCY_AND_STATUS_QUERY, 
                currency, status.name());
    }
    
    @Override
    public List<PaymentTransaction> findByOrganizationCurrencyAndStatus(UUID organizationId, 
            String currency, PaymentStatus status) {
        
        return executeQuery(SELECT_BY_ORG_CURRENCY_AND_STATUS_QUERY, 
                organizationId.toString(), currency, status.name());
    }
    
    @Override
    public List<PaymentTransaction> findByAccountCurrencyAndStatus(UUID accountId, 
            String currency, PaymentStatus status) {
        
        return executeQuery(SELECT_BY_ACCOUNT_CURRENCY_AND_STATUS_QUERY, 
                accountId.toString(), currency, status.name());
    }
}