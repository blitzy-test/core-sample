package io.briklabs.sample.payments.data.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
import io.briklabs.sample.payments.data.dao.PaymentFeeDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;
import io.briklabs.sample.payments.model.PaymentFee;

/**
 * Concrete implementation of the PaymentFeeDAO interface that handles all database operations
 * for payment fee information.
 * <p>
 * This class implements methods for fee creation, retrieval by transaction ID, fee type filtering,
 * and fee aggregation for reporting. It supports both individual fee management and bulk fee operations,
 * enabling comprehensive financial reporting and analysis for payment transactions.
 * </p>
 * <p>
 * The implementation ensures accurate tracking and retrieval of all fee-related data with proper
 * decimal precision for financial calculations.
 * </p>
 */
public class PaymentFeeDaoImpl extends AbstractPaymentDaoImpl<PaymentFee, UUID> implements PaymentFeeDAO {

    private static final Logger logger = LoggerFactory.getLogger(PaymentFeeDaoImpl.class);
    
    /**
     * The name of the payment fee table in the database.
     */
    private static final String TABLE_NAME = "payment_fee";
    
    /**
     * SQL query to find fees by transaction ID.
     */
    private static final String FIND_BY_TRANSACTION_ID_SQL = 
            "SELECT * FROM " + TABLE_NAME + " WHERE transaction_id = ? ORDER BY created_at ASC";
    
    /**
     * SQL query to find fees by fee type.
     */
    private static final String FIND_BY_FEE_TYPE_SQL = 
            "SELECT * FROM " + TABLE_NAME + " WHERE fee_type = ?";
    
    /**
     * SQL query to find fees by organization ID.
     */
    private static final String FIND_BY_ORGANIZATION_ID_SQL = 
            "SELECT f.* FROM " + TABLE_NAME + " f " +
            "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
            "WHERE t.organization_id = ?";
    
    /**
     * SQL query to find fees by organization ID and account ID.
     */
    private static final String FIND_BY_ORGANIZATION_AND_ACCOUNT_SQL = 
            "SELECT f.* FROM " + TABLE_NAME + " f " +
            "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
            "WHERE t.organization_id = ? AND t.account_id = ?";
    
    /**
     * SQL query to calculate total fee amount for a transaction.
     */
    private static final String CALCULATE_TOTAL_FEE_AMOUNT_SQL = 
            "SELECT SUM(amount) FROM " + TABLE_NAME + " WHERE transaction_id = ?";
    
    /**
     * SQL query to calculate fee amount by type for a transaction.
     */
    private static final String CALCULATE_FEE_AMOUNT_BY_TYPE_SQL = 
            "SELECT fee_type, SUM(amount) FROM " + TABLE_NAME + " " +
            "WHERE transaction_id = ? GROUP BY fee_type";
    
    /**
     * SQL query to calculate total fee amount for an organization.
     */
    private static final String CALCULATE_TOTAL_FEE_AMOUNT_FOR_ORGANIZATION_SQL = 
            "SELECT SUM(f.amount) FROM " + TABLE_NAME + " f " +
            "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
            "WHERE t.organization_id = ?";
    
    /**
     * SQL query to calculate fee amount by type for an organization.
     */
    private static final String CALCULATE_FEE_AMOUNT_BY_TYPE_FOR_ORGANIZATION_SQL = 
            "SELECT f.fee_type, SUM(f.amount) FROM " + TABLE_NAME + " f " +
            "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
            "WHERE t.organization_id = ? GROUP BY f.fee_type";
    
    /**
     * SQL query to calculate fee amount by account for an organization.
     */
    private static final String CALCULATE_FEE_AMOUNT_BY_ACCOUNT_SQL = 
            "SELECT t.account_id, SUM(f.amount) FROM " + TABLE_NAME + " f " +
            "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
            "WHERE t.organization_id = ? GROUP BY t.account_id";
    
    /**
     * SQL query to get fee amount by day.
     */
    private static final String GET_FEE_AMOUNT_BY_DAY_SQL = 
            "SELECT DATE(f.created_at) as fee_date, SUM(f.amount) FROM " + TABLE_NAME + " f " +
            "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
            "WHERE t.organization_id = ? GROUP BY fee_date ORDER BY fee_date";
    
    /**
     * SQL query to get fee amount by type.
     */
    private static final String GET_FEE_AMOUNT_BY_TYPE_SQL = 
            "SELECT f.fee_type, SUM(f.amount) FROM " + TABLE_NAME + " f " +
            "JOIN payment_transaction t ON f.transaction_id = t.transaction_id " +
            "WHERE t.organization_id = ? GROUP BY f.fee_type";
    
    /**
     * SQL query to find fees by fee reference.
     */
    private static final String FIND_BY_FEE_REFERENCE_SQL = 
            "SELECT * FROM " + TABLE_NAME + " WHERE fee_reference = ?";
    
    /**
     * SQL query to delete all fees for a transaction.
     */
    private static final String DELETE_ALL_FEES_FOR_TRANSACTION_SQL = 
            "DELETE FROM " + TABLE_NAME + " WHERE transaction_id = ?";
    
    /**
     * SQL query to check if a transaction exists.
     */
    private static final String CHECK_TRANSACTION_EXISTS_SQL = 
            "SELECT 1 FROM payment_transaction WHERE transaction_id = ?";
    
    /**
     * Creates a new PaymentFeeDaoImpl with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     */
    public PaymentFeeDaoImpl(DatabaseConfig databaseConfig, ConfigSource configSource) {
        super(databaseConfig, configSource, TABLE_NAME);
    }

    /**
     * Validates a payment fee entity before database operations.
     *
     * @param fee the fee to validate
     * @throws ValidationException if the fee fails validation
     */
    @Override
    protected void validateEntity(PaymentFee fee) throws ValidationException {
        try {
            if (fee == null) {
                throw new ValidationException("Fee cannot be null");
            }
            
            fee.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Fee validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Maps a ResultSet row to a PaymentFee entity.
     *
     * @param rs the result set
     * @return the mapped PaymentFee entity
     * @throws SQLException if a database access error occurs
     */
    @Override
    protected PaymentFee mapRow(ResultSet rs) throws SQLException {
        UUID feeId = (UUID) rs.getObject("fee_id");
        UUID transactionId = (UUID) rs.getObject("transaction_id");
        String feeType = rs.getString("fee_type");
        BigDecimal amount = rs.getBigDecimal("amount");
        String currency = rs.getString("currency");
        String description = rs.getString("description");
        String feeReference = rs.getString("fee_reference");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        
        return new PaymentFee(
                feeId,
                transactionId,
                feeType,
                amount,
                currency,
                description,
                feeReference,
                createdAt
        );
    }

    /**
     * Builds a query for finding a fee by ID.
     *
     * @param id the fee ID
     * @return the SQL query
     */
    @Override
    protected String buildFindByIdQuery(UUID id) {
        return "SELECT * FROM " + TABLE_NAME + " WHERE fee_id = ?";
    }

    /**
     * Builds a query for filtering fees based on filter parameters.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    @Override
    protected PaymentQueryBuilder buildFilterQuery(PaymentFilterParams filterParams) {
        PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                .select("f.*")
                .from(TABLE_NAME + " f");
        
        // Join with transaction table if organization or account filtering is needed
        if (filterParams.getOrganizationId() != null || filterParams.getAccountId() != null) {
            queryBuilder.innerJoin("payment_transaction t", "f.transaction_id = t.transaction_id");
            
            if (filterParams.getOrganizationId() != null) {
                queryBuilder.and("t.organization_id = ?").addParameter(filterParams.getOrganizationId());
            }
            
            if (filterParams.getAccountId() != null) {
                queryBuilder.and("t.account_id = ?").addParameter(filterParams.getAccountId());
            }
        }
        
        // Apply date range filter if present
        if (filterParams.getDateRange() != null) {
            DateRangeFilter dateRange = filterParams.getDateRange();
            queryBuilder.dateRange("f.created_at", dateRange.getStartDate(), dateRange.getEndDate());
        }
        
        // Apply amount range filter if present
        if (filterParams.getAmountRange() != null) {
            queryBuilder.amountRange("f.amount", 
                    filterParams.getAmountRange().getMinAmount(), 
                    filterParams.getAmountRange().getMaxAmount());
            
            if (filterParams.getAmountRange().getCurrency() != null) {
                queryBuilder.and("f.currency = ?")
                           .addParameter(filterParams.getAmountRange().getCurrency());
            }
        }
        
        // Apply search term if present
        if (filterParams.getSearchTerm() != null && !filterParams.getSearchTerm().isEmpty()) {
            queryBuilder.and("(f.description LIKE ? OR f.fee_reference LIKE ?)")
                       .addParameter("%" + filterParams.getSearchTerm() + "%")
                       .addParameter("%" + filterParams.getSearchTerm() + "%");
        }
        
        // Apply sorting
        if (filterParams.getSortCriteria() != null && !filterParams.getSortCriteria().isEmpty()) {
            for (PaymentFilterParams.SortCriteria criteria : filterParams.getSortCriteria()) {
                String column = criteria.getColumn();
                // Prefix column with table alias if it's a transaction field
                if (column.startsWith("transaction.")) {
                    column = "t." + column.substring("transaction.".length());
                } else if (!column.contains(".")) {
                    column = "f." + column;
                }
                queryBuilder.orderBy(column + " " + criteria.getDirection());
            }
        } else {
            // Default sorting by created_at descending
            queryBuilder.orderBy("f.created_at DESC");
        }
        
        // Apply pagination
        if (filterParams.getPagination() != null) {
            queryBuilder.paginate(
                    filterParams.getPagination().getLimit(),
                    filterParams.getPagination().getOffset());
        }
        
        return queryBuilder;
    }

    /**
     * Builds a count query for fees based on filter parameters.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    @Override
    protected PaymentQueryBuilder buildCountQuery(PaymentFilterParams filterParams) {
        PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder()
                .count("f.fee_id")
                .from(TABLE_NAME + " f");
        
        // Join with transaction table if organization or account filtering is needed
        if (filterParams.getOrganizationId() != null || filterParams.getAccountId() != null) {
            queryBuilder.innerJoin("payment_transaction t", "f.transaction_id = t.transaction_id");
            
            if (filterParams.getOrganizationId() != null) {
                queryBuilder.and("t.organization_id = ?").addParameter(filterParams.getOrganizationId());
            }
            
            if (filterParams.getAccountId() != null) {
                queryBuilder.and("t.account_id = ?").addParameter(filterParams.getAccountId());
            }
        }
        
        // Apply date range filter if present
        if (filterParams.getDateRange() != null) {
            DateRangeFilter dateRange = filterParams.getDateRange();
            queryBuilder.dateRange("f.created_at", dateRange.getStartDate(), dateRange.getEndDate());
        }
        
        // Apply amount range filter if present
        if (filterParams.getAmountRange() != null) {
            queryBuilder.amountRange("f.amount", 
                    filterParams.getAmountRange().getMinAmount(), 
                    filterParams.getAmountRange().getMaxAmount());
            
            if (filterParams.getAmountRange().getCurrency() != null) {
                queryBuilder.and("f.currency = ?")
                           .addParameter(filterParams.getAmountRange().getCurrency());
            }
        }
        
        // Apply search term if present
        if (filterParams.getSearchTerm() != null && !filterParams.getSearchTerm().isEmpty()) {
            queryBuilder.and("(f.description LIKE ? OR f.fee_reference LIKE ?)")
                       .addParameter("%" + filterParams.getSearchTerm() + "%")
                       .addParameter("%" + filterParams.getSearchTerm() + "%");
        }
        
        return queryBuilder;
    }

    /**
     * Builds an insert query for a fee.
     *
     * @param fee the fee to insert
     * @return the SQL query
     */
    @Override
    protected String buildInsertQuery(PaymentFee fee) {
        return "INSERT INTO " + TABLE_NAME + 
               " (fee_id, transaction_id, fee_type, amount, currency, description, fee_reference, created_at) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }

    /**
     * Builds an update query for a fee.
     *
     * @param fee the fee to update
     * @return the SQL query
     */
    @Override
    protected String buildUpdateQuery(PaymentFee fee) {
        return "UPDATE " + TABLE_NAME + 
               " SET transaction_id = ?, fee_type = ?, amount = ?, currency = ?, " +
               "description = ?, fee_reference = ? " +
               "WHERE fee_id = ?";
    }

    /**
     * Builds a delete query for a fee.
     *
     * @param id the fee ID
     * @return the SQL query
     */
    @Override
    protected String buildDeleteQuery(UUID id) {
        return "DELETE FROM " + TABLE_NAME + " WHERE fee_id = ?";
    }

    /**
     * Gets the parameters for an insert query.
     *
     * @param fee the fee to insert
     * @return the query parameters
     */
    @Override
    protected Object[] getInsertParameters(PaymentFee fee) {
        return new Object[] {
                fee.getFeeId() != null ? fee.getFeeId() : UUID.randomUUID(),
                fee.getTransactionId(),
                fee.getFeeType(),
                fee.getAmount(),
                fee.getCurrency(),
                fee.getDescription(),
                fee.getFeeReference(),
                Timestamp.from(fee.getCreatedAt() != null ? fee.getCreatedAt() : Instant.now())
        };
    }

    /**
     * Gets the parameters for an update query.
     *
     * @param fee the fee to update
     * @return the query parameters
     */
    @Override
    protected Object[] getUpdateParameters(PaymentFee fee) {
        return new Object[] {
                fee.getTransactionId(),
                fee.getFeeType(),
                fee.getAmount(),
                fee.getCurrency(),
                fee.getDescription(),
                fee.getFeeReference(),
                fee.getFeeId()
        };
    }

    /**
     * Gets the parameters for a delete query.
     *
     * @param id the fee ID
     * @return the query parameters
     */
    @Override
    protected Object[] getDeleteParameters(UUID id) {
        return new Object[] { id };
    }

    /**
     * Finds all fees associated with a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return List of fees associated with the transaction
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentFee> findByTransactionId(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Finding fees for transaction: {}", transactionId);
        
        return executeQuery(FIND_BY_TRANSACTION_ID_SQL, this::mapRows, transactionId);
    }

    /**
     * Finds fees by fee type.
     *
     * @param feeType The fee type to filter by
     * @param filterParams Additional filtering parameters
     * @return List of fees of the specified type
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentFee> findByFeeType(String feeType, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Finding fees by type: {}", feeType);
        
        PaymentQueryBuilder queryBuilder = buildFilterQuery(filterParams)
                .and("f.fee_type = ?")
                .addParameter(feeType);
        
        return executeQuery(queryBuilder, this::mapRows);
    }

    /**
     * Finds fees by organization ID.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentFee> findByOrganizationId(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Finding fees for organization: {}", organizationId);
        
        PaymentQueryBuilder queryBuilder = buildFilterQuery(filterParams)
                .innerJoin("payment_transaction t", "f.transaction_id = t.transaction_id")
                .and("t.organization_id = ?")
                .addParameter(organizationId);
        
        return executeQuery(queryBuilder, this::mapRows);
    }

    /**
     * Finds fees by organization ID and account ID.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for the specified organization and account
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentFee> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId, 
            PaymentFilterParams filterParams) throws ConnectionException, QueryExecutionException {
        logger.debug("Finding fees for organization: {} and account: {}", organizationId, accountId);
        
        PaymentQueryBuilder queryBuilder = buildFilterQuery(filterParams)
                .innerJoin("payment_transaction t", "f.transaction_id = t.transaction_id")
                .and("t.organization_id = ?")
                .addParameter(organizationId)
                .and("t.account_id = ?")
                .addParameter(accountId);
        
        return executeQuery(queryBuilder, this::mapRows);
    }

    /**
     * Calculates the total fee amount for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return The total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public BigDecimal calculateTotalFeeAmountForTransaction(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Calculating total fee amount for transaction: {}", transactionId);
        
        return executeQuery(CALCULATE_TOTAL_FEE_AMOUNT_SQL, rs -> {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                return total != null ? total : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
        }, transactionId);
    }

    /**
     * Calculates the total fee amount by fee type for a specific transaction.
     *
     * @param transactionId The transaction identifier
     * @return Map of fee type to total amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Map<String, BigDecimal> calculateFeeAmountByTypeForTransaction(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Calculating fee amounts by type for transaction: {}", transactionId);
        
        return executeQuery(CALCULATE_FEE_AMOUNT_BY_TYPE_SQL, rs -> {
            Map<String, BigDecimal> result = new HashMap<>();
            while (rs.next()) {
                String feeType = rs.getString(1);
                BigDecimal amount = rs.getBigDecimal(2);
                result.put(feeType, amount);
            }
            return result;
        }, transactionId);
    }

    /**
     * Calculates the total fee amount for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return The total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public BigDecimal calculateTotalFeeAmountForOrganization(UUID organizationId, DateRangeFilter dateRange, 
            String currency) throws ConnectionException, QueryExecutionException {
        logger.debug("Calculating total fee amount for organization: {}", organizationId);
        
        StringBuilder sql = new StringBuilder(CALCULATE_TOTAL_FEE_AMOUNT_FOR_ORGANIZATION_SQL);
        List<Object> params = new ArrayList<>();
        params.add(organizationId);
        
        // Add date range conditions if present
        if (dateRange != null && dateRange.hasConstraints()) {
            if (dateRange.getStartDate() != null) {
                sql.append(" AND f.created_at >= ?");
                params.add(Timestamp.valueOf(dateRange.getStartDate()));
            }
            
            if (dateRange.getEndDate() != null) {
                sql.append(" AND f.created_at <= ?");
                params.add(Timestamp.valueOf(dateRange.getEndDate()));
            }
        }
        
        // Add currency filter if specified
        if (currency != null && !currency.isEmpty()) {
            sql.append(" AND f.currency = ?");
            params.add(currency);
        }
        
        return executeQuery(sql.toString(), rs -> {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                return total != null ? total : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
        }, params.toArray());
    }

    /**
     * Calculates the total fee amount by fee type for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return Map of fee type to total amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Map<String, BigDecimal> calculateFeeAmountByTypeForOrganization(UUID organizationId, 
            DateRangeFilter dateRange, String currency) throws ConnectionException, QueryExecutionException {
        logger.debug("Calculating fee amounts by type for organization: {}", organizationId);
        
        StringBuilder sql = new StringBuilder(CALCULATE_FEE_AMOUNT_BY_TYPE_FOR_ORGANIZATION_SQL);
        List<Object> params = new ArrayList<>();
        params.add(organizationId);
        
        // Add date range conditions if present
        if (dateRange != null && dateRange.hasConstraints()) {
            if (dateRange.getStartDate() != null) {
                sql.append(" AND f.created_at >= ?");
                params.add(Timestamp.valueOf(dateRange.getStartDate()));
            }
            
            if (dateRange.getEndDate() != null) {
                sql.append(" AND f.created_at <= ?");
                params.add(Timestamp.valueOf(dateRange.getEndDate()));
            }
        }
        
        // Add currency filter if specified
        if (currency != null && !currency.isEmpty()) {
            sql.append(" AND f.currency = ?");
            params.add(currency);
        }
        
        return executeQuery(sql.toString(), rs -> {
            Map<String, BigDecimal> result = new HashMap<>();
            while (rs.next()) {
                String feeType = rs.getString(1);
                BigDecimal amount = rs.getBigDecimal(2);
                result.put(feeType, amount);
            }
            return result;
        }, params.toArray());
    }

    /**
     * Calculates the total fee amount by account for an organization within a date range.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @param currency The currency code (optional, if null will return totals for all currencies)
     * @return Map of account ID to total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Map<UUID, BigDecimal> calculateFeeAmountByAccountForOrganization(UUID organizationId, 
            DateRangeFilter dateRange, String currency) throws ConnectionException, QueryExecutionException {
        logger.debug("Calculating fee amounts by account for organization: {}", organizationId);
        
        StringBuilder sql = new StringBuilder(CALCULATE_FEE_AMOUNT_BY_ACCOUNT_SQL);
        List<Object> params = new ArrayList<>();
        params.add(organizationId);
        
        // Add date range conditions if present
        if (dateRange != null && dateRange.hasConstraints()) {
            if (dateRange.getStartDate() != null) {
                sql.append(" AND f.created_at >= ?");
                params.add(Timestamp.valueOf(dateRange.getStartDate()));
            }
            
            if (dateRange.getEndDate() != null) {
                sql.append(" AND f.created_at <= ?");
                params.add(Timestamp.valueOf(dateRange.getEndDate()));
            }
        }
        
        // Add currency filter if specified
        if (currency != null && !currency.isEmpty()) {
            sql.append(" AND f.currency = ?");
            params.add(currency);
        }
        
        return executeQuery(sql.toString(), rs -> {
            Map<UUID, BigDecimal> result = new HashMap<>();
            while (rs.next()) {
                UUID accountId = (UUID) rs.getObject(1);
                BigDecimal amount = rs.getBigDecimal(2);
                result.put(accountId, amount);
            }
            return result;
        }, params.toArray());
    }

    /**
     * Finds fees for a specific time period grouped by day.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of date to total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Map<LocalDate, BigDecimal> getFeeAmountByDay(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Getting fee amounts by day for organization: {}", organizationId);
        
        StringBuilder sql = new StringBuilder(GET_FEE_AMOUNT_BY_DAY_SQL);
        List<Object> params = new ArrayList<>();
        params.add(organizationId);
        
        // Add date range conditions if present
        if (dateRange != null && dateRange.hasConstraints()) {
            if (dateRange.getStartDate() != null) {
                sql.append(" AND f.created_at >= ?");
                params.add(Timestamp.valueOf(dateRange.getStartDate()));
            }
            
            if (dateRange.getEndDate() != null) {
                sql.append(" AND f.created_at <= ?");
                params.add(Timestamp.valueOf(dateRange.getEndDate()));
            }
        }
        
        return executeQuery(sql.toString(), rs -> {
            Map<LocalDate, BigDecimal> result = new HashMap<>();
            while (rs.next()) {
                LocalDate date = rs.getDate(1).toLocalDate();
                BigDecimal amount = rs.getBigDecimal(2);
                result.put(date, amount);
            }
            return result;
        }, params.toArray());
    }

    /**
     * Finds fees for a specific time period grouped by fee type.
     *
     * @param organizationId The organization identifier
     * @param dateRange The date range filter
     * @return Map of fee type to total fee amount
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Map<String, BigDecimal> getFeeAmountByType(UUID organizationId, DateRangeFilter dateRange) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Getting fee amounts by type for organization: {}", organizationId);
        
        StringBuilder sql = new StringBuilder(GET_FEE_AMOUNT_BY_TYPE_SQL);
        List<Object> params = new ArrayList<>();
        params.add(organizationId);
        
        // Add date range conditions if present
        if (dateRange != null && dateRange.hasConstraints()) {
            if (dateRange.getStartDate() != null) {
                sql.append(" AND f.created_at >= ?");
                params.add(Timestamp.valueOf(dateRange.getStartDate()));
            }
            
            if (dateRange.getEndDate() != null) {
                sql.append(" AND f.created_at <= ?");
                params.add(Timestamp.valueOf(dateRange.getEndDate()));
            }
        }
        
        return executeQuery(sql.toString(), rs -> {
            Map<String, BigDecimal> result = new HashMap<>();
            while (rs.next()) {
                String feeType = rs.getString(1);
                BigDecimal amount = rs.getBigDecimal(2);
                result.put(feeType, amount);
            }
            return result;
        }, params.toArray());
    }

    /**
     * Creates a new fee associated with a transaction.
     *
     * @param transactionId The transaction identifier
     * @param fee The fee to create
     * @return The created fee with any database-generated values
     * @throws ValidationException if the fee fails validation
     * @throws ResourceNotFoundException if the associated transaction is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public PaymentFee createFeeForTransaction(UUID transactionId, PaymentFee fee) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        logger.debug("Creating fee for transaction: {}", transactionId);
        
        // Validate the fee
        validateEntity(fee);
        
        // Ensure the transaction exists
        if (!transactionExists(transactionId)) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
        }
        
        // Set the transaction ID on the fee
        fee.setTransactionId(transactionId);
        
        // Generate a fee ID if not provided
        if (fee.getFeeId() == null) {
            fee.setFeeId(UUID.randomUUID());
        }
        
        // Set creation timestamp if not provided
        if (fee.getCreatedAt() == null) {
            fee.setCreatedAt(Instant.now());
        }
        
        // Create the fee
        return create(fee);
    }

    /**
     * Creates multiple fees for a transaction in a batch operation.
     *
     * @param transactionId The transaction identifier
     * @param fees The list of fees to create
     * @return The list of created fees with any database-generated values
     * @throws ValidationException if any fee fails validation
     * @throws ResourceNotFoundException if the associated transaction is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public List<PaymentFee> batchCreateFeesForTransaction(UUID transactionId, List<PaymentFee> fees) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        logger.debug("Batch creating fees for transaction: {}", transactionId);
        
        if (fees == null || fees.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Ensure the transaction exists
        if (!transactionExists(transactionId)) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
        }
        
        // Set the transaction ID on all fees and validate them
        for (PaymentFee fee : fees) {
            fee.setTransactionId(transactionId);
            validateEntity(fee);
            
            // Generate a fee ID if not provided
            if (fee.getFeeId() == null) {
                fee.setFeeId(UUID.randomUUID());
            }
            
            // Set creation timestamp if not provided
            if (fee.getCreatedAt() == null) {
                fee.setCreatedAt(Instant.now());
            }
        }
        
        // Create the fees in a batch
        return batchCreate(fees);
    }

    /**
     * Updates a fee associated with a transaction.
     *
     * @param fee The fee to update
     * @return The updated fee
     * @throws ValidationException if the fee fails validation
     * @throws ResourceNotFoundException if the fee or associated transaction is not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public PaymentFee updateFee(PaymentFee fee) 
            throws ValidationException, ResourceNotFoundException, ConnectionException, 
                   QueryExecutionException, TransactionException {
        logger.debug("Updating fee: {}", fee.getFeeId());
        
        // Validate the fee
        validateEntity(fee);
        
        // Ensure the fee exists
        if (!exists(fee.getFeeId())) {
            throw new ResourceNotFoundException("Fee not found with ID: " + fee.getFeeId());
        }
        
        // Ensure the transaction exists
        if (!transactionExists(fee.getTransactionId())) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + fee.getTransactionId());
        }
        
        // Update the fee
        return update(fee);
    }

    /**
     * Deletes all fees associated with a transaction.
     *
     * @param transactionId The transaction identifier
     * @return The number of fees deleted
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public int deleteAllFeesForTransaction(UUID transactionId) 
            throws ConnectionException, QueryExecutionException, TransactionException {
        logger.debug("Deleting all fees for transaction: {}", transactionId);
        
        boolean localTransaction = !Boolean.TRUE.equals(inTransaction.get());
        
        if (localTransaction) {
            beginTransaction();
        }
        
        try {
            int rowsAffected = executeUpdate(DELETE_ALL_FEES_FOR_TRANSACTION_SQL, transactionId);
            
            if (localTransaction) {
                commitTransaction();
            }
            
            return rowsAffected;
        } catch (ConnectionException | QueryExecutionException e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw e;
        } catch (Exception e) {
            if (localTransaction) {
                rollbackTransaction();
            }
            throw new QueryExecutionException("Failed to delete fees for transaction: " + e.getMessage(), e, 
                    DELETE_ALL_FEES_FOR_TRANSACTION_SQL, "DELETE", new String[]{TABLE_NAME});
        }
    }

    /**
     * Finds fees with external reference matching the provided value.
     *
     * @param feeReference The external fee reference
     * @return List of fees with the matching reference
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentFee> findByFeeReference(String feeReference) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Finding fees by reference: {}", feeReference);
        
        return executeQuery(FIND_BY_FEE_REFERENCE_SQL, this::mapRows, feeReference);
    }

    /**
     * Finds fees for all accounts of an organization.
     * This is a special case of the organization query that uses the "_all" placeholder.
     *
     * @param organizationId The organization identifier
     * @param filterParams Additional filtering parameters
     * @return List of fees for all accounts of the specified organization
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentFee> findByOrganizationIdForAllAccounts(UUID organizationId, PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Finding fees for all accounts of organization: {}", organizationId);
        
        // This is the same as findByOrganizationId since we're not filtering by account
        return findByOrganizationId(organizationId, filterParams);
    }

    /**
     * Finds fees for all organizations.
     * This is a special case for administrative access that uses the "_all" placeholder.
     *
     * @param filterParams Additional filtering parameters
     * @return List of fees across all organizations
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentFee> findForAllOrganizations(PaymentFilterParams filterParams) 
            throws ConnectionException, QueryExecutionException {
        logger.debug("Finding fees for all organizations");
        
        // Use the filter query builder without organization filtering
        PaymentQueryBuilder queryBuilder = buildFilterQuery(filterParams);
        
        return executeQuery(queryBuilder, this::mapRows);
    }

    /**
     * Checks if a transaction exists.
     *
     * @param transactionId The transaction identifier
     * @return true if the transaction exists, false otherwise
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    private boolean transactionExists(UUID transactionId) 
            throws ConnectionException, QueryExecutionException {
        return executeQuery(CHECK_TRANSACTION_EXISTS_SQL, rs -> rs.next(), transactionId);
    }
}