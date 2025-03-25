package io.briklabs.sample.payments.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fluent interface for building SQL queries specific to payment transaction operations.
 * This class enables dynamic construction of parameterized SQL queries with support for
 * filtering, sorting, and pagination. It handles parameter binding, type safety, and
 * SQL injection prevention while supporting complex query patterns needed for payment data retrieval.
 * 
 * <p>Example usage:</p>
 * <pre>
 * QueryBuilder queryBuilder = new QueryBuilder()
 *     .select("t.transaction_id", "t.status", "t.amount", "t.created_at")
 *     .from("payment_transaction t")
 *     .where("t.organization_id = ?")
 *     .and("t.account_id = ?")
 *     .and("t.created_at BETWEEN ? AND ?")
 *     .and("t.status IN (?, ?, ?)")
 *     .orderBy("t.created_at DESC")
 *     .limit(20)
 *     .offset(40);
 *     
 * PreparedStatement stmt = queryBuilder.build(connection);
 * queryBuilder.setParameters(stmt, organizationId, accountId, startDate, endDate, 
 *                           "AUTHORIZED", "CAPTURED", "REFUNDED");
 * </pre>
 */
public class QueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
    
    private final StringBuilder query;
    private final List<Object> parameters;
    private boolean hasWhere;
    private boolean hasOrderBy;
    private boolean hasLimit;
    private boolean hasOffset;
    private boolean hasGroupBy;
    private boolean hasHaving;
    
    /**
     * Creates a new QueryBuilder instance with an empty query.
     */
    public QueryBuilder() {
        this.query = new StringBuilder();
        this.parameters = new ArrayList<>();
        this.hasWhere = false;
        this.hasOrderBy = false;
        this.hasLimit = false;
        this.hasOffset = false;
        this.hasGroupBy = false;
        this.hasHaving = false;
    }
    
    /**
     * Adds a SELECT clause to the query with the specified columns.
     * 
     * @param columns The columns to select
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder select(String... columns) {
        query.append("SELECT ");
        
        if (columns == null || columns.length == 0) {
            query.append("*");
        } else {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(columns[i]);
            }
        }
        
        return this;
    }
    
    /**
     * Adds a SELECT DISTINCT clause to the query with the specified columns.
     * 
     * @param columns The columns to select distinctly
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder selectDistinct(String... columns) {
        query.append("SELECT DISTINCT ");
        
        if (columns == null || columns.length == 0) {
            query.append("*");
        } else {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(columns[i]);
            }
        }
        
        return this;
    }
    
    /**
     * Adds a COUNT query for the specified column.
     * 
     * @param column The column to count (or * for all)
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder count(String column) {
        query.append("SELECT COUNT(");
        query.append(column);
        query.append(") ");
        
        return this;
    }
    
    /**
     * Adds a FROM clause to the query with the specified table.
     * 
     * @param table The table to select from
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder from(String table) {
        query.append(" FROM ");
        query.append(table);
        
        return this;
    }
    
    /**
     * Adds a JOIN clause to the query.
     * 
     * @param table The table to join
     * @param condition The join condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder join(String table, String condition) {
        query.append(" JOIN ");
        query.append(table);
        query.append(" ON ");
        query.append(condition);
        
        return this;
    }
    
    /**
     * Adds a LEFT JOIN clause to the query.
     * 
     * @param table The table to left join
     * @param condition The join condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder leftJoin(String table, String condition) {
        query.append(" LEFT JOIN ");
        query.append(table);
        query.append(" ON ");
        query.append(condition);
        
        return this;
    }
    
    /**
     * Adds a RIGHT JOIN clause to the query.
     * 
     * @param table The table to right join
     * @param condition The join condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder rightJoin(String table, String condition) {
        query.append(" RIGHT JOIN ");
        query.append(table);
        query.append(" ON ");
        query.append(condition);
        
        return this;
    }
    
    /**
     * Adds an INNER JOIN clause to the query.
     * 
     * @param table The table to inner join
     * @param condition The join condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder innerJoin(String table, String condition) {
        query.append(" INNER JOIN ");
        query.append(table);
        query.append(" ON ");
        query.append(condition);
        
        return this;
    }
    
    /**
     * Adds a WHERE clause to the query with the specified condition.
     * If a WHERE clause already exists, this will be ignored to prevent SQL syntax errors.
     * 
     * @param condition The WHERE condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder where(String condition) {
        if (!hasWhere) {
            query.append(" WHERE ");
            query.append(condition);
            hasWhere = true;
        } else {
            logger.warn("WHERE clause already exists, use and() or or() instead");
        }
        
        return this;
    }
    
    /**
     * Adds an AND condition to the query.
     * If no WHERE clause exists yet, this will add a WHERE clause instead.
     * 
     * @param condition The AND condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder and(String condition) {
        if (hasWhere) {
            query.append(" AND ");
            query.append(condition);
        } else {
            where(condition);
        }
        
        return this;
    }
    
    /**
     * Adds an OR condition to the query.
     * If no WHERE clause exists yet, this will add a WHERE clause instead.
     * 
     * @param condition The OR condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder or(String condition) {
        if (hasWhere) {
            query.append(" OR ");
            query.append(condition);
        } else {
            where(condition);
        }
        
        return this;
    }
    
    /**
     * Adds a GROUP BY clause to the query with the specified columns.
     * 
     * @param columns The columns to group by
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder groupBy(String... columns) {
        if (!hasGroupBy) {
            query.append(" GROUP BY ");
            
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(columns[i]);
            }
            
            hasGroupBy = true;
        } else {
            logger.warn("GROUP BY clause already exists");
        }
        
        return this;
    }
    
    /**
     * Adds a HAVING clause to the query with the specified condition.
     * 
     * @param condition The HAVING condition
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder having(String condition) {
        if (!hasHaving) {
            query.append(" HAVING ");
            query.append(condition);
            hasHaving = true;
        } else {
            logger.warn("HAVING clause already exists");
        }
        
        return this;
    }
    
    /**
     * Adds an ORDER BY clause to the query with the specified columns and directions.
     * 
     * @param orderClauses The order clauses (e.g., "created_at DESC", "amount ASC")
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder orderBy(String... orderClauses) {
        if (!hasOrderBy) {
            query.append(" ORDER BY ");
            
            for (int i = 0; i < orderClauses.length; i++) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(orderClauses[i]);
            }
            
            hasOrderBy = true;
        } else {
            logger.warn("ORDER BY clause already exists");
        }
        
        return this;
    }
    
    /**
     * Adds a LIMIT clause to the query.
     * 
     * @param limit The maximum number of rows to return
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder limit(int limit) {
        if (!hasLimit) {
            query.append(" LIMIT ");
            query.append(limit);
            hasLimit = true;
        } else {
            logger.warn("LIMIT clause already exists");
        }
        
        return this;
    }
    
    /**
     * Adds an OFFSET clause to the query.
     * 
     * @param offset The number of rows to skip
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder offset(int offset) {
        if (!hasOffset) {
            query.append(" OFFSET ");
            query.append(offset);
            hasOffset = true;
        } else {
            logger.warn("OFFSET clause already exists");
        }
        
        return this;
    }
    
    /**
     * Adds a parameter to be bound to a prepared statement.
     * 
     * @param parameter The parameter value
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder addParameter(Object parameter) {
        parameters.add(parameter);
        return this;
    }
    
    /**
     * Adds multiple parameters to be bound to a prepared statement.
     * 
     * @param parameters The parameter values
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder addParameters(Object... parameters) {
        for (Object parameter : parameters) {
            this.parameters.add(parameter);
        }
        return this;
    }
    
    /**
     * Creates a prepared statement from the built query using the provided connection.
     * 
     * @param connection The database connection
     * @return A prepared statement with the built query
     * @throws SQLException If a database access error occurs
     */
    public PreparedStatement build(Connection connection) throws SQLException {
        String sql = query.toString();
        logger.debug("Building prepared statement with SQL: {}", sql);
        return connection.prepareStatement(sql);
    }
    
    /**
     * Sets the parameters on the prepared statement in the order they were added.
     * 
     * @param statement The prepared statement
     * @param parameters The parameter values
     * @throws SQLException If a database access error occurs
     */
    public void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            setParameter(statement, i + 1, parameters[i]);
        }
    }
    
    /**
     * Sets the stored parameters on the prepared statement.
     * 
     * @param statement The prepared statement
     * @throws SQLException If a database access error occurs
     */
    public void setStoredParameters(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            setParameter(statement, i + 1, parameters.get(i));
        }
    }
    
    /**
     * Sets a parameter on the prepared statement with the appropriate type.
     * 
     * @param statement The prepared statement
     * @param index The parameter index (1-based)
     * @param value The parameter value
     * @throws SQLException If a database access error occurs
     */
    private void setParameter(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.NULL);
            return;
        }
        
        if (value instanceof String) {
            statement.setString(index, (String) value);
        } else if (value instanceof Integer) {
            statement.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            statement.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            statement.setDouble(index, (Double) value);
        } else if (value instanceof Boolean) {
            statement.setBoolean(index, (Boolean) value);
        } else if (value instanceof java.util.Date) {
            statement.setTimestamp(index, new java.sql.Timestamp(((java.util.Date) value).getTime()));
        } else if (value instanceof java.sql.Date) {
            statement.setDate(index, (java.sql.Date) value);
        } else if (value instanceof java.sql.Timestamp) {
            statement.setTimestamp(index, (java.sql.Timestamp) value);
        } else if (value instanceof java.math.BigDecimal) {
            statement.setBigDecimal(index, (java.math.BigDecimal) value);
        } else if (value instanceof byte[]) {
            statement.setBytes(index, (byte[]) value);
        } else if (value instanceof java.util.UUID) {
            statement.setObject(index, value);
        } else {
            // For other types, try to set as object and let JDBC driver handle it
            statement.setObject(index, value);
        }
    }
    
    /**
     * Returns the SQL query string that has been built.
     * 
     * @return The SQL query string
     */
    public String getQueryString() {
        return query.toString();
    }
    
    /**
     * Returns the list of parameters that have been added.
     * 
     * @return The list of parameters
     */
    public List<Object> getParameters() {
        return new ArrayList<>(parameters);
    }
    
    /**
     * Adds a WHERE clause for filtering by organization ID.
     * 
     * @param columnName The column name for organization ID
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereOrganizationId(String columnName) {
        return where(columnName + " = ?");
    }
    
    /**
     * Adds a WHERE clause for filtering by account ID.
     * 
     * @param columnName The column name for account ID
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereAccountId(String columnName) {
        return and(columnName + " = ?");
    }
    
    /**
     * Adds a WHERE clause for filtering by transaction status.
     * 
     * @param columnName The column name for status
     * @param statusCount The number of status values to include
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereStatus(String columnName, int statusCount) {
        if (statusCount <= 0) {
            return this;
        }
        
        StringBuilder placeholders = new StringBuilder();
        placeholders.append(columnName).append(" IN (");
        
        for (int i = 0; i < statusCount; i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        
        placeholders.append(")");
        return and(placeholders.toString());
    }
    
    /**
     * Adds a WHERE clause for filtering by date range.
     * 
     * @param columnName The column name for the date
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereDateRange(String columnName) {
        return and(columnName + " BETWEEN ? AND ?");
    }
    
    /**
     * Adds a WHERE clause for filtering by amount range.
     * 
     * @param columnName The column name for the amount
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereAmountRange(String columnName) {
        return and(columnName + " BETWEEN ? AND ?");
    }
    
    /**
     * Adds a WHERE clause for filtering by merchant ID.
     * 
     * @param columnName The column name for merchant ID
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereMerchantId(String columnName) {
        return and(columnName + " = ?");
    }
    
    /**
     * Adds a WHERE clause for filtering by payment type.
     * 
     * @param columnName The column name for payment type
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder wherePaymentType(String columnName) {
        return and(columnName + " = ?");
    }
    
    /**
     * Adds a WHERE clause for text search on a specific column.
     * 
     * @param columnName The column name to search
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereTextContains(String columnName) {
        return and(columnName + " ILIKE ?");
    }
    
    /**
     * Adds a WHERE clause for filtering by transaction reference.
     * 
     * @param columnName The column name for transaction reference
     * @return This QueryBuilder instance for method chaining
     */
    public QueryBuilder whereTransactionReference(String columnName) {
        return and(columnName + " = ?");
    }
    
    /**
     * Creates a new instance of QueryBuilder for counting records.
     * 
     * @param table The table to count from
     * @return A new QueryBuilder instance configured for counting
     */
    public static QueryBuilder countFrom(String table) {
        return new QueryBuilder().count("*").from(table);
    }
    
    /**
     * Creates a new instance of QueryBuilder for selecting all columns.
     * 
     * @param table The table to select from
     * @return A new QueryBuilder instance configured for selecting all columns
     */
    public static QueryBuilder selectAllFrom(String table) {
        return new QueryBuilder().select().from(table);
    }
    
    /**
     * Creates a new instance of QueryBuilder for selecting specific columns.
     * 
     * @param table The table to select from
     * @param columns The columns to select
     * @return A new QueryBuilder instance configured for selecting specific columns
     */
    public static QueryBuilder selectFrom(String table, String... columns) {
        return new QueryBuilder().select(columns).from(table);
    }
    
    @Override
    public String toString() {
        return "QueryBuilder{" +
                "query=" + query +
                ", parameters=" + parameters +
                ", hasWhere=" + hasWhere +
                ", hasOrderBy=" + hasOrderBy +
                ", hasLimit=" + hasLimit +
                ", hasOffset=" + hasOffset +
                ", hasGroupBy=" + hasGroupBy +
                ", hasHaving=" + hasHaving +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryBuilder that = (QueryBuilder) o;
        return hasWhere == that.hasWhere &&
                hasOrderBy == that.hasOrderBy &&
                hasLimit == that.hasLimit &&
                hasOffset == that.hasOffset &&
                hasGroupBy == that.hasGroupBy &&
                hasHaving == that.hasHaving &&
                Objects.equals(query.toString(), that.query.toString()) &&
                Objects.equals(parameters, that.parameters);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(query.toString(), parameters, hasWhere, hasOrderBy, hasLimit, hasOffset, hasGroupBy, hasHaving);
    }
}