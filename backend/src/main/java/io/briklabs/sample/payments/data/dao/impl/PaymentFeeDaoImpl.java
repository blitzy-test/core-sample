package io.briklabs.sample.payments.data.dao.impl;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.ConnectionManager;
import io.briklabs.sample.payments.data.dao.PaymentFeeDAO;
import io.briklabs.sample.payments.data.exception.PaymentDataException;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;
import io.briklabs.sample.payments.model.PaymentFee;
import io.briklabs.sample.payments.model.PaymentFee.FeeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Concrete implementation of the PaymentFeeDAO interface that handles all database operations
 * for payment fee information. This class implements methods for fee creation, retrieval by
 * transaction ID, fee type filtering, and fee aggregation for reporting.
 * <p>
 * It supports both individual fee management and bulk fee operations, enabling comprehensive
 * financial reporting and analysis for payment transactions. This implementation ensures
 * accurate tracking and retrieval of all fee-related data.
 * </p>
 */
public class PaymentFeeDaoImpl extends AbstractPaymentDaoImpl<PaymentFee, UUID> implements PaymentFeeDAO {

    private static final Logger logger = LoggerFactory.getLogger(PaymentFeeDaoImpl.class);
    
    // SQL statements for fee operations
    private static final String SQL_INSERT_FEE = 
            "INSERT INTO payment_fee (fee_id, transaction_id, fee_type, amount, currency, description, fee_reference, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE_FEE = 
            "UPDATE payment_fee SET fee_type = ?, amount = ?, currency = ?, description = ?, fee_reference = ? " +
            "WHERE fee_id = ?";
    
    private static final String SQL_DELETE_FEE = 
            "DELETE FROM payment_fee WHERE fee_id = ?";
    
    private static final String SQL_FIND_FEE_BY_ID = 
            "SELECT * FROM payment_fee WHERE fee_id = ?";
    
    private static final String SQL_FIND_FEES_BY_TRANSACTION_ID = 
            "SELECT * FROM payment_fee WHERE transaction_id = ? ORDER BY created_at ASC";
    
    private static final String SQL_DELETE_FEES_BY_TRANSACTION_ID = 
            "DELETE FROM payment_fee WHERE transaction_id = ?";
    
    private static final String SQL_FIND_FEES_BY_FEE_REFERENCE = 
            "SELECT * FROM payment_fee WHERE fee_reference = ?";
    
    private static final String SQL_CALCULATE_TOTAL_FEE_AMOUNT_FOR_TRANSACTION = 
            "SELECT SUM(amount) FROM payment_fee WHERE transaction_id = ?";
    
    private static final String SQL_CALCULATE_FEE_AMOUNT_BY_TYPE_FOR_TRANSACTION = 
            "SELECT fee_type, SUM(amount) FROM payment_fee WHERE transaction_id = ? GROUP BY fee_type";
    
    /**
     * Creates a new PaymentFeeDaoImpl with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     */
    public PaymentFeeDaoImpl(DatabaseConfig databaseConfig, ConfigSource configSource) {
        super(databaseConfig, configSource);
        logger.debug("Initialized PaymentFeeDaoImpl with database configuration");
    }

    /**
     * Creates a new PaymentFeeDaoImpl with the specified connection manager.
     *
     * @param connectionManager the connection manager
     */
    public PaymentFeeDaoImpl(ConnectionManager connectionManager) {
        super(connectionManager);
        logger.debug("Initialized PaymentFeeDaoImpl with provided connection manager");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PaymentFee executeCreate(Connection connection, PaymentFee fee) throws SQLException {
        logger.debug("Creating fee: {}", fee);
        
        // Validate fee data
        fee.validate();
        
        // Ensure fee ID is set
        if (fee.getFeeId() == null) {
            fee.setFeeId(UUID.randomUUID());
        }
        
        // Ensure created timestamp is set
        if (fee.getCreatedAt() == null) {
            fee.setCreatedAt(Instant.now());
        }
        
        try (PreparedStatement stmt = prepareStatement(connection, SQL_INSERT_FEE)) {
            stmt.setObject(1, fee.getFeeId());
            stmt.setObject(2, fee.getTransactionId());
            stmt.setString(3, fee.getFeeType().name());
            stmt.setBigDecimal(4, fee.getAmount());
            stmt.setString(5, fee.getCurrency());
            stmt.setString(6, fee.getDescription());
            stmt.setString(7, fee.getFeeReference());
            stmt.setTimestamp(8, Timestamp.from(fee.getCreatedAt()));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Failed to create fee, expected 1 row affected but got " + rowsAffected);
            }
            
            return fee;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Optional<PaymentFee> executeFindById(Connection connection, UUID feeId) throws SQLException {
        logger.debug("Finding fee by ID: {}", feeId);
        
        try (PreparedStatement stmt = prepareStatement(connection, SQL_FIND_FEE_BY_ID)) {
            stmt.setObject(1, feeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return processResultSetToOptional(rs, this::mapFeeFromResultSet);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PaymentFee executeUpdate(Connection connection, PaymentFee fee) throws SQLException {
        logger.debug("Updating fee: {}", fee);
        
        // Validate fee data
        fee.validate();
        
        try (PreparedStatement stmt = prepareStatement(connection, SQL_UPDATE_FEE)) {
            stmt.setString(1, fee.getFeeType().name());
            stmt.setBigDecimal(2, fee.getAmount());
            stmt.setString(3, fee.getCurrency());
            stmt.setString(4, fee.getDescription());
            stmt.setString(5, fee.getFeeReference());
            stmt.setObject(6, fee.getFeeId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Failed to update fee, expected 1 row affected but got " + rowsAffected);
            }
            
            return fee;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeDelete(Connection connection, UUID feeId) throws SQLException {
        logger.debug("Deleting fee with ID: {}", feeId);
        
        try (PreparedStatement stmt = prepareStatement(connection, SQL_DELETE_FEE)) {
            stmt.setObject(1, feeId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected == 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PaymentFee> executeQuery(Connection connection, Object params) throws SQLException {
        logger.debug("Querying fees with params: {}", params);
        
        if (params instanceof PaymentFilterParams) {
            PaymentFilterParams filterParams = (PaymentFilterParams) params;
            
            PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                    .select("f.*")
                    .from("payment_fee f");
            
            // Apply filters
            queryBuilder.applyFilters(filterParams);
            
            try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return processResultSet(rs, this::mapFeeFromResultSet);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported query parameters type: " + 
                    (params != null ? params.getClass().getName() : "null"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PaymentFee> executeBatchCreate(Connection connection, List<PaymentFee> fees) throws SQLException {
        logger.debug("Batch creating {} fees", fees.size());
        
        try (PreparedStatement stmt = prepareStatement(connection, SQL_INSERT_FEE)) {
            for (PaymentFee fee : fees) {
                // Validate fee data
                fee.validate();
                
                // Ensure fee ID is set
                if (fee.getFeeId() == null) {
                    fee.setFeeId(UUID.randomUUID());
                }
                
                // Ensure created timestamp is set
                if (fee.getCreatedAt() == null) {
                    fee.setCreatedAt(Instant.now());
                }
                
                stmt.setObject(1, fee.getFeeId());
                stmt.setObject(2, fee.getTransactionId());
                stmt.setString(3, fee.getFeeType().name());
                stmt.setBigDecimal(4, fee.getAmount());
                stmt.setString(5, fee.getCurrency());
                stmt.setString(6, fee.getDescription());
                stmt.setString(7, fee.getFeeReference());
                stmt.setTimestamp(8, Timestamp.from(fee.getCreatedAt()));
                
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            
            // Verify all inserts were successful
            for (int i = 0; i < results.length; i++) {
                if (results[i] != 1) {
                    throw new SQLException("Failed to create fee at index " + i + 
                            ", expected 1 row affected but got " + results[i]);
                }
            }
            
            return fees;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PaymentFee> executeBatchUpdate(Connection connection, List<PaymentFee> fees) throws SQLException {
        logger.debug("Batch updating {} fees", fees.size());
        
        try (PreparedStatement stmt = prepareStatement(connection, SQL_UPDATE_FEE)) {
            for (PaymentFee fee : fees) {
                // Validate fee data
                fee.validate();
                
                stmt.setString(1, fee.getFeeType().name());
                stmt.setBigDecimal(2, fee.getAmount());
                stmt.setString(3, fee.getCurrency());
                stmt.setString(4, fee.getDescription());
                stmt.setString(5, fee.getFeeReference());
                stmt.setObject(6, fee.getFeeId());
                
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            
            // Verify all updates were successful
            for (int i = 0; i < results.length; i++) {
                if (results[i] != 1) {
                    throw new SQLException("Failed to update fee at index " + i + 
                            ", expected 1 row affected but got " + results[i]);
                }
            }
            
            return fees;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long executeCount(Connection connection, Object params) throws SQLException {
        logger.debug("Counting fees with params: {}", params);
        
        if (params instanceof PaymentFilterParams) {
            PaymentFilterParams filterParams = (PaymentFilterParams) params;
            
            PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                    .count("f.fee_id")
                    .from("payment_fee f");
            
            // Apply filters (excluding pagination and sorting)
            PaymentFilterParams countParams = filterParams.copy();
            countParams.setPagination(null);
            countParams.setSortCriteria(new ArrayList<>());
            queryBuilder.applyFilters(countParams);
            
            try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    return 0;
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported query parameters type: " + 
                    (params != null ? params.getClass().getName() : "null"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeExists(Connection connection, UUID feeId) throws SQLException {
        logger.debug("Checking if fee exists with ID: {}", feeId);
        
        try (PreparedStatement stmt = prepareStatement(connection, "SELECT 1 FROM payment_fee WHERE fee_id = ?")) {
            stmt.setObject(1, feeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PaymentFee> executeFindByOrganizationId(Connection connection, UUID organizationId) throws SQLException {
        logger.debug("Finding fees by organization ID: {}", organizationId);
        
        String sql = "SELECT f.* FROM payment_fee f " +
                     "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                     "WHERE t.organization_id = ?";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, organizationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return processResultSet(rs, this::mapFeeFromResultSet);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PaymentFee> executeFindByAccountId(Connection connection, UUID accountId) throws SQLException {
        logger.debug("Finding fees by account ID: {}", accountId);
        
        String sql = "SELECT f.* FROM payment_fee f " +
                     "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                     "WHERE t.account_id = ?";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return processResultSet(rs, this::mapFeeFromResultSet);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PaymentFee> executeFindByOrganizationAndAccountId(Connection connection, UUID organizationId, UUID accountId) throws SQLException {
        logger.debug("Finding fees by organization ID: {} and account ID: {}", organizationId, accountId);
        
        String sql = "SELECT f.* FROM payment_fee f " +
                     "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                     "WHERE t.organization_id = ? AND t.account_id = ?";
        
        try (PreparedStatement stmt = prepareStatement(connection, sql)) {
            stmt.setObject(1, organizationId);
            stmt.setObject(2, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return processResultSet(rs, this::mapFeeFromResultSet);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> findByTransactionId(UUID transactionId) {
        logger.debug("Finding fees by transaction ID: {}", transactionId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                return executeFindByTransactionId(connection, transactionId);
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find fees by transaction ID: " + transactionId, e);
        }
    }
    
    /**
     * Executes the find by transaction ID operation with the provided connection and transaction ID.
     *
     * @param connection the database connection
     * @param transactionId the transaction ID to find fees for
     * @return a list of fees associated with the transaction
     * @throws SQLException if a database error occurs
     */
    private List<PaymentFee> executeFindByTransactionId(Connection connection, UUID transactionId) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(connection, SQL_FIND_FEES_BY_TRANSACTION_ID)) {
            stmt.setObject(1, transactionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return processResultSet(rs, this::mapFeeFromResultSet);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> findByFeeType(String feeType, PaymentFilterParams filterParams) {
        logger.debug("Finding fees by fee type: {} with filters: {}", feeType, filterParams);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                        .select("f.*")
                        .from("payment_fee f")
                        .where("f.fee_type = ?")
                        .addParameter(feeType);
                
                // Apply additional filters
                if (filterParams != null) {
                    queryBuilder.applyFilters(filterParams);
                }
                
                try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        return processResultSet(rs, this::mapFeeFromResultSet);
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find fees by fee type: " + feeType, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams) {
        logger.debug("Finding fees by organization ID: {} with filters: {}", organizationId, filterParams);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                        .select("f.*")
                        .from("payment_fee f")
                        .innerJoin("payment_transaction t", "f.transaction_id = t.transaction_id")
                        .where("t.organization_id = ?")
                        .addParameter(organizationId);
                
                // Apply additional filters
                if (filterParams != null) {
                    queryBuilder.applyFilters(filterParams);
                }
                
                try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        return processResultSet(rs, this::mapFeeFromResultSet);
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find fees by organization ID: " + organizationId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, PaymentFilterParams filterParams) {
        logger.debug("Finding fees by organization ID: {} and account ID: {} with filters: {}", 
                organizationId, accountId, filterParams);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                        .select("f.*")
                        .from("payment_fee f")
                        .innerJoin("payment_transaction t", "f.transaction_id = t.transaction_id")
                        .where("t.organization_id = ?")
                        .addParameter(organizationId)
                        .and("t.account_id = ?")
                        .addParameter(accountId);
                
                // Apply additional filters
                if (filterParams != null) {
                    queryBuilder.applyFilters(filterParams);
                }
                
                try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        return processResultSet(rs, this::mapFeeFromResultSet);
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find fees by organization ID: " + organizationId + 
                    " and account ID: " + accountId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal calculateTotalFeeAmountForTransaction(UUID transactionId) {
        logger.debug("Calculating total fee amount for transaction ID: {}", transactionId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                try (PreparedStatement stmt = prepareStatement(connection, SQL_CALCULATE_TOTAL_FEE_AMOUNT_FOR_TRANSACTION)) {
                    stmt.setObject(1, transactionId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getBigDecimal(1);
                        }
                        return BigDecimal.ZERO;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to calculate total fee amount for transaction ID: " + transactionId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, BigDecimal> calculateFeeAmountByTypeForTransaction(UUID transactionId) {
        logger.debug("Calculating fee amount by type for transaction ID: {}", transactionId);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                Map<String, BigDecimal> result = new HashMap<>();
                
                try (PreparedStatement stmt = prepareStatement(connection, SQL_CALCULATE_FEE_AMOUNT_BY_TYPE_FOR_TRANSACTION)) {
                    stmt.setObject(1, transactionId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String feeType = rs.getString(1);
                            BigDecimal amount = rs.getBigDecimal(2);
                            result.put(feeType, amount);
                        }
                    }
                }
                
                return result;
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to calculate fee amount by type for transaction ID: " + transactionId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal calculateTotalFeeAmountForOrganization(UUID organizationId, DateRangeFilter dateRange, String currency) {
        logger.debug("Calculating total fee amount for organization ID: {} with date range: {} and currency: {}", 
                organizationId, dateRange, currency);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                StringBuilder sql = new StringBuilder(
                        "SELECT SUM(f.amount) FROM payment_fee f " +
                        "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                        "WHERE t.organization_id = ?");
                
                List<Object> params = new ArrayList<>();
                params.add(organizationId);
                
                // Add date range filter if provided
                if (dateRange != null && dateRange.hasConstraints()) {
                    if (dateRange.getStartDate() != null) {
                        sql.append(" AND t.created_at >= ?");
                        params.add(Timestamp.from(dateRange.getStartDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                    
                    if (dateRange.getEndDate() != null) {
                        sql.append(" AND t.created_at <= ?");
                        params.add(Timestamp.from(dateRange.getEndDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                }
                
                // Add currency filter if provided
                if (currency != null && !currency.isEmpty()) {
                    sql.append(" AND f.currency = ?");
                    params.add(currency);
                }
                
                try (PreparedStatement stmt = prepareStatement(connection, sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getBigDecimal(1);
                        }
                        return BigDecimal.ZERO;
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to calculate total fee amount for organization ID: " + organizationId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, BigDecimal> calculateFeeAmountByTypeForOrganization(UUID organizationId, DateRangeFilter dateRange, String currency) {
        logger.debug("Calculating fee amount by type for organization ID: {} with date range: {} and currency: {}", 
                organizationId, dateRange, currency);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                StringBuilder sql = new StringBuilder(
                        "SELECT f.fee_type, SUM(f.amount) FROM payment_fee f " +
                        "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                        "WHERE t.organization_id = ?");
                
                List<Object> params = new ArrayList<>();
                params.add(organizationId);
                
                // Add date range filter if provided
                if (dateRange != null && dateRange.hasConstraints()) {
                    if (dateRange.getStartDate() != null) {
                        sql.append(" AND t.created_at >= ?");
                        params.add(Timestamp.from(dateRange.getStartDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                    
                    if (dateRange.getEndDate() != null) {
                        sql.append(" AND t.created_at <= ?");
                        params.add(Timestamp.from(dateRange.getEndDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                }
                
                // Add currency filter if provided
                if (currency != null && !currency.isEmpty()) {
                    sql.append(" AND f.currency = ?");
                    params.add(currency);
                }
                
                sql.append(" GROUP BY f.fee_type");
                
                Map<String, BigDecimal> result = new HashMap<>();
                
                try (PreparedStatement stmt = prepareStatement(connection, sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String feeType = rs.getString(1);
                            BigDecimal amount = rs.getBigDecimal(2);
                            result.put(feeType, amount);
                        }
                    }
                }
                
                return result;
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to calculate fee amount by type for organization ID: " + organizationId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<UUID, BigDecimal> calculateFeeAmountByAccountForOrganization(UUID organizationId, DateRangeFilter dateRange, String currency) {
        logger.debug("Calculating fee amount by account for organization ID: {} with date range: {} and currency: {}", 
                organizationId, dateRange, currency);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                StringBuilder sql = new StringBuilder(
                        "SELECT t.account_id, SUM(f.amount) FROM payment_fee f " +
                        "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                        "WHERE t.organization_id = ?");
                
                List<Object> params = new ArrayList<>();
                params.add(organizationId);
                
                // Add date range filter if provided
                if (dateRange != null && dateRange.hasConstraints()) {
                    if (dateRange.getStartDate() != null) {
                        sql.append(" AND t.created_at >= ?");
                        params.add(Timestamp.from(dateRange.getStartDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                    
                    if (dateRange.getEndDate() != null) {
                        sql.append(" AND t.created_at <= ?");
                        params.add(Timestamp.from(dateRange.getEndDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                }
                
                // Add currency filter if provided
                if (currency != null && !currency.isEmpty()) {
                    sql.append(" AND f.currency = ?");
                    params.add(currency);
                }
                
                sql.append(" GROUP BY t.account_id");
                
                Map<UUID, BigDecimal> result = new HashMap<>();
                
                try (PreparedStatement stmt = prepareStatement(connection, sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            UUID accountId = rs.getObject(1, UUID.class);
                            BigDecimal amount = rs.getBigDecimal(2);
                            result.put(accountId, amount);
                        }
                    }
                }
                
                return result;
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to calculate fee amount by account for organization ID: " + organizationId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<LocalDate, BigDecimal> getFeeAmountByDay(UUID organizationId, DateRangeFilter dateRange) {
        logger.debug("Getting fee amount by day for organization ID: {} with date range: {}", 
                organizationId, dateRange);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                StringBuilder sql = new StringBuilder(
                        "SELECT DATE(t.created_at) as day, SUM(f.amount) FROM payment_fee f " +
                        "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                        "WHERE t.organization_id = ?");
                
                List<Object> params = new ArrayList<>();
                params.add(organizationId);
                
                // Add date range filter if provided
                if (dateRange != null && dateRange.hasConstraints()) {
                    if (dateRange.getStartDate() != null) {
                        sql.append(" AND t.created_at >= ?");
                        params.add(Timestamp.from(dateRange.getStartDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                    
                    if (dateRange.getEndDate() != null) {
                        sql.append(" AND t.created_at <= ?");
                        params.add(Timestamp.from(dateRange.getEndDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                }
                
                sql.append(" GROUP BY DATE(t.created_at) ORDER BY DATE(t.created_at)");
                
                Map<LocalDate, BigDecimal> result = new HashMap<>();
                
                try (PreparedStatement stmt = prepareStatement(connection, sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            LocalDate day = rs.getDate(1).toLocalDate();
                            BigDecimal amount = rs.getBigDecimal(2);
                            result.put(day, amount);
                        }
                    }
                }
                
                return result;
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to get fee amount by day for organization ID: " + organizationId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, BigDecimal> getFeeAmountByType(UUID organizationId, DateRangeFilter dateRange) {
        logger.debug("Getting fee amount by type for organization ID: {} with date range: {}", 
                organizationId, dateRange);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                StringBuilder sql = new StringBuilder(
                        "SELECT f.fee_type, SUM(f.amount) FROM payment_fee f " +
                        "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
                        "WHERE t.organization_id = ?");
                
                List<Object> params = new ArrayList<>();
                params.add(organizationId);
                
                // Add date range filter if provided
                if (dateRange != null && dateRange.hasConstraints()) {
                    if (dateRange.getStartDate() != null) {
                        sql.append(" AND t.created_at >= ?");
                        params.add(Timestamp.from(dateRange.getStartDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                    
                    if (dateRange.getEndDate() != null) {
                        sql.append(" AND t.created_at <= ?");
                        params.add(Timestamp.from(dateRange.getEndDate().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                }
                
                sql.append(" GROUP BY f.fee_type");
                
                Map<String, BigDecimal> result = new HashMap<>();
                
                try (PreparedStatement stmt = prepareStatement(connection, sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String feeType = rs.getString(1);
                            BigDecimal amount = rs.getBigDecimal(2);
                            result.put(feeType, amount);
                        }
                    }
                }
                
                return result;
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to get fee amount by type for organization ID: " + organizationId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentFee createFeeForTransaction(UUID transactionId, PaymentFee fee) {
        logger.debug("Creating fee for transaction ID: {}", transactionId);
        
        if (fee == null) {
            throw new IllegalArgumentException("Fee cannot be null");
        }
        
        // Set the transaction ID on the fee
        fee.setTransactionId(transactionId);
        
        return create(fee);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> batchCreateFeesForTransaction(UUID transactionId, List<PaymentFee> fees) {
        logger.debug("Batch creating fees for transaction ID: {}", transactionId);
        
        if (fees == null || fees.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Set the transaction ID on all fees
        for (PaymentFee fee : fees) {
            fee.setTransactionId(transactionId);
        }
        
        return batchCreate(fees);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentFee updateFee(PaymentFee fee) {
        logger.debug("Updating fee: {}", fee);
        
        if (fee == null) {
            throw new IllegalArgumentException("Fee cannot be null");
        }
        
        return update(fee);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteAllFeesForTransaction(UUID transactionId) {
        logger.debug("Deleting all fees for transaction ID: {}", transactionId);
        
        try {
            return executeInTransaction(connection -> {
                try (PreparedStatement stmt = prepareStatement(connection, SQL_DELETE_FEES_BY_TRANSACTION_ID)) {
                    stmt.setObject(1, transactionId);
                    return stmt.executeUpdate();
                }
            });
        } catch (Exception e) {
            throw handleException("Failed to delete fees for transaction ID: " + transactionId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> findByFeeReference(String feeReference) {
        logger.debug("Finding fees by fee reference: {}", feeReference);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                try (PreparedStatement stmt = prepareStatement(connection, SQL_FIND_FEES_BY_FEE_REFERENCE)) {
                    stmt.setString(1, feeReference);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        return processResultSet(rs, this::mapFeeFromResultSet);
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find fees by fee reference: " + feeReference, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> findByOrganizationIdForAllAccounts(UUID organizationId, PaymentFilterParams filterParams) {
        logger.debug("Finding fees for all accounts of organization ID: {} with filters: {}", 
                organizationId, filterParams);
        
        return findByOrganizationId(organizationId, filterParams);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentFee> findForAllOrganizations(PaymentFilterParams filterParams) {
        logger.debug("Finding fees for all organizations with filters: {}", filterParams);
        
        try {
            Connection connection = connectionManager.getConnection();
            try {
                PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                        .select("f.*")
                        .from("payment_fee f");
                
                // Apply filters
                if (filterParams != null) {
                    queryBuilder.applyFilters(filterParams);
                }
                
                try (PreparedStatement stmt = queryBuilder.buildPreparedStatement(connection)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        return processResultSet(rs, this::mapFeeFromResultSet);
                    }
                }
            } finally {
                connectionManager.releaseConnection(connection);
            }
        } catch (Exception e) {
            throw handleException("Failed to find fees for all organizations", e);
        }
    }
    
    /**
     * Maps a ResultSet row to a PaymentFee object.
     *
     * @param rs the ResultSet to map
     * @return the mapped PaymentFee object
     * @throws SQLException if a database error occurs
     */
    private PaymentFee mapFeeFromResultSet(ResultSet rs) throws SQLException {
        PaymentFee fee = new PaymentFee();
        
        fee.setFeeId(rs.getObject("fee_id", UUID.class));
        fee.setTransactionId(rs.getObject("transaction_id", UUID.class));
        
        String feeTypeStr = rs.getString("fee_type");
        try {
            fee.setFeeType(FeeType.valueOf(feeTypeStr));
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown fee type: {}, using OTHER", feeTypeStr);
            fee.setFeeType(FeeType.OTHER);
        }
        
        fee.setAmount(rs.getBigDecimal("amount"));
        fee.setCurrency(rs.getString("currency"));
        fee.setDescription(rs.getString("description"));
        fee.setFeeReference(rs.getString("fee_reference"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            fee.setCreatedAt(createdAt.toInstant());
        }
        
        return fee;
    }
}