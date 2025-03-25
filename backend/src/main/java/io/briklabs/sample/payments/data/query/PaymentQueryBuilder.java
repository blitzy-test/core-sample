package io.briklabs.sample.payments.data.query;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fluent query builder for constructing complex SQL queries for payment transactions.
 * This class provides a type-safe, parameterized approach to building SQL statements
 * with support for conditional clauses, joins, sorting, and pagination.
 * 
 * The builder prevents SQL injection by using prepared statements and parameter binding.
 * It supports complex filtering scenarios required for payment transaction management.
 */
public class PaymentQueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PaymentQueryBuilder.class);
    
    private StringBuilder query;
    private List<Object> parameters;
    private boolean hasWhere;
    private boolean hasOrderBy;
    private boolean hasLimit;
    private boolean hasOffset;
    
    /**
     * Creates a new query builder instance.
     */
    public PaymentQueryBuilder() {
        this.query = new StringBuilder();
        this.parameters = new ArrayList<>();
        this.hasWhere = false;
        this.hasOrderBy = false;
        this.hasLimit = false;
        this.hasOffset = false;
    }
    
    /**
     * Starts a SELECT query with the specified columns.
     * 
     * @param columns The columns to select
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder select(String... columns) {
        query.append("SELECT ");
        if (columns == null || columns.length == 0) {
            query.append("*");
        } else {
            query.append(String.join(", ", columns));
        }
        return this;
    }
    
    /**
     * Adds a COUNT query for getting total records.
     * 
     * @param column The column to count (or * for all)
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder count(String column) {
        query.append("SELECT COUNT(").append(column).append(")");
        return this;
    }
    
    /**
     * Specifies the FROM clause with the table name.
     * 
     * @param table The table name
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder from(String table) {
        query.append(" FROM ").append(table);
        return this;
    }
    
    /**
     * Adds a JOIN clause to the query.
     * 
     * @param joinType The type of join (INNER, LEFT, RIGHT)
     * @param table The table to join
     * @param condition The join condition
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder join(String joinType, String table, String condition) {
        query.append(" ").append(joinType).append(" JOIN ").append(table).append(" ON ").append(condition);
        return this;
    }
    
    /**
     * Adds an INNER JOIN clause to the query.
     * 
     * @param table The table to join
     * @param condition The join condition
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder innerJoin(String table, String condition) {
        return join("INNER", table, condition);
    }
    
    /**
     * Adds a LEFT JOIN clause to the query.
     * 
     * @param table The table to join
     * @param condition The join condition
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder leftJoin(String table, String condition) {
        return join("LEFT", table, condition);
    }
    
    /**
     * Adds a WHERE clause to the query.
     * 
     * @param condition The condition to add
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder where(String condition) {
        query.append(" WHERE ").append(condition);
        hasWhere = true;
        return this;
    }
    
    /**
     * Adds an AND condition to the query.
     * If no WHERE clause exists yet, it will be added automatically.
     * 
     * @param condition The condition to add
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder and(String condition) {
        if (hasWhere) {
            query.append(" AND ").append(condition);
        } else {
            where(condition);
        }
        return this;
    }
    
    /**
     * Adds an OR condition to the query.
     * If no WHERE clause exists yet, it will be added automatically.
     * 
     * @param condition The condition to add
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder or(String condition) {
        if (hasWhere) {
            query.append(" OR ").append(condition);
        } else {
            where(condition);
        }
        return this;
    }
    
    /**
     * Adds a parameter to the query for binding.
     * 
     * @param value The parameter value
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder addParameter(Object value) {
        parameters.add(value);
        return this;
    }
    
    /**
     * Adds multiple parameters to the query for binding.
     * 
     * @param values The parameter values
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder addParameters(Object... values) {
        parameters.addAll(Arrays.asList(values));
        return this;
    }
    
    /**
     * Adds an ORDER BY clause to the query.
     * 
     * @param columns The columns to order by
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder orderBy(String... columns) {
        if (!hasOrderBy) {
            query.append(" ORDER BY ");
            query.append(String.join(", ", columns));
            hasOrderBy = true;
        } else {
            query.append(", ").append(String.join(", ", columns));
        }
        return this;
    }
    
    /**
     * Adds a LIMIT clause to the query.
     * 
     * @param limit The maximum number of rows to return
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder limit(int limit) {
        if (!hasLimit) {
            query.append(" LIMIT ?");
            parameters.add(limit);
            hasLimit = true;
        } else {
            logger.warn("LIMIT clause already added to query, ignoring additional limit");
        }
        return this;
    }
    
    /**
     * Adds an OFFSET clause to the query.
     * 
     * @param offset The number of rows to skip
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder offset(int offset) {
        if (!hasOffset) {
            query.append(" OFFSET ?");
            parameters.add(offset);
            hasOffset = true;
        } else {
            logger.warn("OFFSET clause already added to query, ignoring additional offset");
        }
        return this;
    }
    
    /**
     * Adds pagination to the query using LIMIT and OFFSET.
     * 
     * @param limit The maximum number of rows to return
     * @param offset The number of rows to skip
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder paginate(int limit, int offset) {
        return limit(limit).offset(offset);
    }
    
    /**
     * Adds a GROUP BY clause to the query.
     * 
     * @param columns The columns to group by
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder groupBy(String... columns) {
        query.append(" GROUP BY ").append(String.join(", ", columns));
        return this;
    }
    
    /**
     * Adds a HAVING clause to the query.
     * 
     * @param condition The having condition
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder having(String condition) {
        query.append(" HAVING ").append(condition);
        return this;
    }
    
    /**
     * Adds a date range condition to the query.
     * 
     * @param column The date column to filter
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder dateRange(String column, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            and(column + " BETWEEN ? AND ?");
            addParameter(Timestamp.valueOf(startDate));
            addParameter(Timestamp.valueOf(endDate));
        } else if (startDate != null) {
            and(column + " >= ?");
            addParameter(Timestamp.valueOf(startDate));
        } else if (endDate != null) {
            and(column + " <= ?");
            addParameter(Timestamp.valueOf(endDate));
        }
        return this;
    }
    
    /**
     * Adds an amount range condition to the query.
     * 
     * @param column The amount column to filter
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder amountRange(String column, BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null) {
            and(column + " BETWEEN ? AND ?");
            addParameter(minAmount);
            addParameter(maxAmount);
        } else if (minAmount != null) {
            and(column + " >= ?");
            addParameter(minAmount);
        } else if (maxAmount != null) {
            and(column + " <= ?");
            addParameter(maxAmount);
        }
        return this;
    }
    
    /**
     * Adds an IN condition for multiple status values.
     * 
     * @param column The status column to filter
     * @param statuses The status values to include
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder statusIn(String column, Collection<String> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < statuses.size(); i++) {
                if (i > 0) {
                    placeholders.append(", ");
                }
                placeholders.append("?");
            }
            and(column + " IN (" + placeholders + ")");
            parameters.addAll(statuses);
        }
        return this;
    }
    
    /**
     * Adds a condition for organization ID.
     * 
     * @param column The organization ID column
     * @param organizationId The organization ID value
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder forOrganization(String column, UUID organizationId) {
        if (organizationId != null) {
            and(column + " = ?");
            addParameter(organizationId);
        }
        return this;
    }
    
    /**
     * Adds a condition for account ID.
     * 
     * @param column The account ID column
     * @param accountId The account ID value
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder forAccount(String column, UUID accountId) {
        if (accountId != null) {
            and(column + " = ?");
            addParameter(accountId);
        }
        return this;
    }
    
    /**
     * Adds a condition for merchant ID.
     * 
     * @param column The merchant ID column
     * @param merchantId The merchant ID value
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder forMerchant(String column, String merchantId) {
        if (merchantId != null && !merchantId.isEmpty()) {
            and(column + " = ?");
            addParameter(merchantId);
        }
        return this;
    }
    
    /**
     * Adds a condition for payment type.
     * 
     * @param column The payment type column
     * @param paymentType The payment type value
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder forPaymentType(String column, String paymentType) {
        if (paymentType != null && !paymentType.isEmpty()) {
            and(column + " = ?");
            addParameter(paymentType);
        }
        return this;
    }
    
    /**
     * Adds a LIKE condition for text search.
     * 
     * @param column The column to search
     * @param searchTerm The search term
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder textSearch(String column, String searchTerm) {
        if (searchTerm != null && !searchTerm.isEmpty()) {
            and(column + " LIKE ?");
            addParameter("%" + searchTerm + "%");
        }
        return this;
    }
    
    /**
     * Adds a condition for transaction ID.
     * 
     * @param column The transaction ID column
     * @param transactionId The transaction ID value
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder forTransaction(String column, UUID transactionId) {
        if (transactionId != null) {
            and(column + " = ?");
            addParameter(transactionId);
        }
        return this;
    }
    
    /**
     * Applies a complete set of filter parameters from a PaymentFilterParams object.
     * 
     * @param filters The filter parameters to apply
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder applyFilters(PaymentFilterParams filters) {
        if (filters == null) {
            return this;
        }
        
        // Apply organization and account filters
        if (filters.getOrganizationId() != null) {
            forOrganization("t.organization_id", filters.getOrganizationId());
        }
        
        if (filters.getAccountId() != null) {
            forAccount("t.account_id", filters.getAccountId());
        }
        
        // Apply date range filter
        if (filters.getDateRange() != null) {
            dateRange("t.created_at", 
                    filters.getDateRange().getStartDate(), 
                    filters.getDateRange().getEndDate());
        }
        
        // Apply amount range filter
        if (filters.getAmountRange() != null) {
            amountRange("t.amount", 
                    filters.getAmountRange().getMinAmount(), 
                    filters.getAmountRange().getMaxAmount());
            
            if (filters.getAmountRange().getCurrency() != null) {
                and("t.currency = ?");
                addParameter(filters.getAmountRange().getCurrency());
            }
        }
        
        // Apply status filter
        if (filters.getStatusFilter() != null && !filters.getStatusFilter().getStatuses().isEmpty()) {
            statusIn("t.status", filters.getStatusFilter().getStatuses());
        }
        
        // Apply merchant filter
        if (filters.getMerchantId() != null) {
            forMerchant("t.merchant_id", filters.getMerchantId());
        }
        
        // Apply payment type filter
        if (filters.getPaymentType() != null) {
            forPaymentType("t.payment_type", filters.getPaymentType());
        }
        
        // Apply text search
        if (filters.getSearchTerm() != null && !filters.getSearchTerm().isEmpty()) {
            or("t.transaction_reference LIKE ?");
            addParameter("%" + filters.getSearchTerm() + "%");
            or("t.description LIKE ?");
            addParameter("%" + filters.getSearchTerm() + "%");
        }
        
        // Apply sorting
        if (filters.getSortCriteria() != null && !filters.getSortCriteria().isEmpty()) {
            for (SortCriteria criteria : filters.getSortCriteria()) {
                orderBy(criteria.getColumn() + " " + criteria.getDirection());
            }
        } else {
            // Default sorting by created_at descending
            orderBy("t.created_at DESC");
        }
        
        // Apply pagination
        if (filters.getPagination() != null) {
            paginate(filters.getPagination().getLimit(), filters.getPagination().getOffset());
        }
        
        return this;
    }
    
    /**
     * Builds a common query for payment transactions with standard joins.
     * 
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder buildTransactionQuery() {
        select("t.*")
            .from("payment_transaction t");
        return this;
    }
    
    /**
     * Builds a query for payment transaction details including related data.
     * 
     * @param transactionId The transaction ID to query
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder buildTransactionDetailsQuery(UUID transactionId) {
        select("t.*")
            .from("payment_transaction t")
            .where("t.transaction_id = ?")
            .addParameter(transactionId);
        return this;
    }
    
    /**
     * Builds a query for payment events related to a transaction.
     * 
     * @param transactionId The transaction ID to query events for
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder buildTransactionEventsQuery(UUID transactionId) {
        select("e.*")
            .from("payment_event e")
            .where("e.transaction_id = ?")
            .addParameter(transactionId)
            .orderBy("e.created_at ASC");
        return this;
    }
    
    /**
     * Builds a query for payment data related to a transaction.
     * 
     * @param transactionId The transaction ID to query payment data for
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder buildPaymentDataQuery(UUID transactionId) {
        select("pd.*")
            .from("payment_data pd")
            .where("pd.transaction_id = ?")
            .addParameter(transactionId);
        return this;
    }
    
    /**
     * Builds a query for payment fees related to a transaction.
     * 
     * @param transactionId The transaction ID to query fees for
     * @return This builder for method chaining
     */
    public PaymentQueryBuilder buildFeesQuery(UUID transactionId) {
        select("f.*")
            .from("payment_fee f")
            .where("f.transaction_id = ?")
            .addParameter(transactionId)
            .orderBy("f.created_at ASC");
        return this;
    }
    
    /**
     * Returns the current SQL query string.
     * 
     * @return The SQL query string
     */
    public String getQueryString() {
        return query.toString();
    }
    
    /**
     * Returns the current list of parameters.
     * 
     * @return The list of parameters
     */
    public List<Object> getParameters() {
        return parameters;
    }
    
    /**
     * Creates a prepared statement from this query using the provided connection.
     * 
     * @param connection The database connection
     * @return A prepared statement with parameters bound
     * @throws SQLException If a database access error occurs
     */
    public PreparedStatement buildPreparedStatement(Connection connection) throws SQLException {
        String sql = getQueryString();
        logger.debug("Building prepared statement: {}", sql);
        
        PreparedStatement stmt = connection.prepareStatement(sql);
        
        // Bind parameters
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            int paramIndex = i + 1; // JDBC parameters are 1-based
            
            if (param == null) {
                stmt.setNull(paramIndex, java.sql.Types.NULL);
            } else if (param instanceof String) {
                stmt.setString(paramIndex, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(paramIndex, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(paramIndex, (Long) param);
            } else if (param instanceof Double) {
                stmt.setDouble(paramIndex, (Double) param);
            } else if (param instanceof BigDecimal) {
                stmt.setBigDecimal(paramIndex, (BigDecimal) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(paramIndex, (Boolean) param);
            } else if (param instanceof Date) {
                stmt.setTimestamp(paramIndex, new Timestamp(((Date) param).getTime()));
            } else if (param instanceof LocalDateTime) {
                stmt.setTimestamp(paramIndex, Timestamp.valueOf((LocalDateTime) param));
            } else if (param instanceof Timestamp) {
                stmt.setTimestamp(paramIndex, (Timestamp) param);
            } else if (param instanceof UUID) {
                stmt.setObject(paramIndex, param);
            } else {
                stmt.setObject(paramIndex, param);
            }
        }
        
        return stmt;
    }
    
    /**
     * Creates a new query builder instance.
     * 
     * @return A new query builder
     */
    public static PaymentQueryBuilder create() {
        return new PaymentQueryBuilder();
    }
}