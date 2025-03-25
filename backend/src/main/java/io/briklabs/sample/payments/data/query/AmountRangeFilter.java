package io.briklabs.sample.payments.data.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized filter component for handling amount range operations in payment queries.
 * This class provides structured representation and validation of monetary amount filtering
 * with support for currency conversion, decimal precision handling, and range validation.
 * 
 * It ensures consistent handling of financial amounts across different currencies while
 * generating appropriate SQL conditions for amount-based filtering.
 */
public class AmountRangeFilter {
    private static final Logger logger = LoggerFactory.getLogger(AmountRangeFilter.class);
    
    // Standard decimal scale for monetary amounts (4 decimal places)
    private static final int AMOUNT_SCALE = 4;
    
    // Maximum precision for monetary amounts (19 digits total)
    private static final int AMOUNT_PRECISION = 19;
    
    // The minimum amount (inclusive) for the range
    private BigDecimal minAmount;
    
    // The maximum amount (inclusive) for the range
    private BigDecimal maxAmount;
    
    // The currency code (ISO 4217) for the amounts
    private String currency;
    
    // Flag indicating whether currency conversion is required
    private boolean requiresConversion;
    
    // Exchange rate for currency conversion (if applicable)
    private BigDecimal exchangeRate;
    
    /**
     * Creates a new amount range filter with explicit minimum and maximum amounts.
     * 
     * @param minAmount The minimum amount (inclusive), can be null for open-ended ranges
     * @param maxAmount The maximum amount (inclusive), can be null for open-ended ranges
     * @param currency The currency code (ISO 4217), can be null
     */
    public AmountRangeFilter(BigDecimal minAmount, BigDecimal maxAmount, String currency) {
        this.minAmount = minAmount != null ? normalizeAmount(minAmount) : null;
        this.maxAmount = maxAmount != null ? normalizeAmount(maxAmount) : null;
        this.currency = currency != null ? currency.toUpperCase() : null;
        this.requiresConversion = false;
        this.exchangeRate = BigDecimal.ONE;
    }
    
    /**
     * Creates a new amount range filter with string representations of amounts.
     * 
     * @param minAmountStr The minimum amount as string (inclusive), can be null for open-ended ranges
     * @param maxAmountStr The maximum amount as string (inclusive), can be null for open-ended ranges
     * @param currency The currency code (ISO 4217), can be null
     * @throws NumberFormatException If the amount strings cannot be parsed as decimal numbers
     */
    public AmountRangeFilter(String minAmountStr, String maxAmountStr, String currency) {
        this.currency = currency != null ? currency.toUpperCase() : null;
        this.requiresConversion = false;
        this.exchangeRate = BigDecimal.ONE;
        
        if (minAmountStr != null && !minAmountStr.isEmpty()) {
            try {
                this.minAmount = normalizeAmount(new BigDecimal(minAmountStr));
            } catch (NumberFormatException e) {
                logger.warn("Invalid minimum amount format: {}", minAmountStr);
                throw new IllegalArgumentException("Invalid minimum amount format. Expected decimal number.");
            }
        }
        
        if (maxAmountStr != null && !maxAmountStr.isEmpty()) {
            try {
                this.maxAmount = normalizeAmount(new BigDecimal(maxAmountStr));
            } catch (NumberFormatException e) {
                logger.warn("Invalid maximum amount format: {}", maxAmountStr);
                throw new IllegalArgumentException("Invalid maximum amount format. Expected decimal number.");
            }
        }
    }
    
    /**
     * Creates a new amount range filter with currency conversion.
     * 
     * @param minAmount The minimum amount (inclusive), can be null for open-ended ranges
     * @param maxAmount The maximum amount (inclusive), can be null for open-ended ranges
     * @param sourceCurrency The source currency code (ISO 4217)
     * @param targetCurrency The target currency code (ISO 4217)
     * @param exchangeRate The exchange rate from source to target currency
     */
    public AmountRangeFilter(BigDecimal minAmount, BigDecimal maxAmount, 
                             String sourceCurrency, String targetCurrency, 
                             BigDecimal exchangeRate) {
        this.minAmount = minAmount != null ? normalizeAmount(minAmount) : null;
        this.maxAmount = maxAmount != null ? normalizeAmount(maxAmount) : null;
        this.currency = sourceCurrency != null ? sourceCurrency.toUpperCase() : null;
        this.requiresConversion = true;
        this.exchangeRate = exchangeRate != null ? exchangeRate : BigDecimal.ONE;
    }
    
    /**
     * Normalizes an amount to the standard decimal scale and precision.
     * 
     * @param amount The amount to normalize
     * @return The normalized amount
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        
        // Check if the amount exceeds the maximum precision
        if (amount.precision() > AMOUNT_PRECISION) {
            logger.warn("Amount precision exceeds maximum allowed ({}): {}", AMOUNT_PRECISION, amount);
            throw new IllegalArgumentException("Amount precision exceeds maximum allowed: " + AMOUNT_PRECISION);
        }
        
        // Scale the amount to the standard decimal places
        return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Validates the amount range for consistency and correctness.
     * 
     * @throws IllegalArgumentException If the amount range is invalid
     */
    public void validate() {
        // Validate minimum amount
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Negative minimum amount: {}", minAmount);
            throw new IllegalArgumentException("Minimum amount cannot be negative");
        }
        
        // Validate maximum amount
        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Negative maximum amount: {}", maxAmount);
            throw new IllegalArgumentException("Maximum amount cannot be negative");
        }
        
        // Validate range consistency
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            logger.warn("Minimum amount greater than maximum amount: {} > {}", minAmount, maxAmount);
            throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount");
        }
        
        // Validate currency code format
        if (currency != null && currency.length() != 3) {
            logger.warn("Invalid currency code length: {}", currency);
            throw new IllegalArgumentException("Currency code must be 3 characters (ISO 4217 format)");
        }
        
        // Validate exchange rate if conversion is required
        if (requiresConversion && (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0)) {
            logger.warn("Invalid exchange rate: {}", exchangeRate);
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
    }
    
    /**
     * Applies this amount range filter to a query builder.
     * 
     * @param queryBuilder The query builder to apply the filter to
     * @param columnName The database column name for the amount
     * @param currencyColumnName The database column name for the currency (optional)
     * @return The updated query builder
     */
    public PaymentQueryBuilder applyTo(PaymentQueryBuilder queryBuilder, 
                                      String columnName, 
                                      String currencyColumnName) {
        // Apply amount range conditions
        if (minAmount != null && maxAmount != null) {
            queryBuilder.and(columnName + " BETWEEN ? AND ?")
                       .addParameter(minAmount)
                       .addParameter(maxAmount);
        } else if (minAmount != null) {
            queryBuilder.and(columnName + " >= ?")
                       .addParameter(minAmount);
        } else if (maxAmount != null) {
            queryBuilder.and(columnName + " <= ?")
                       .addParameter(maxAmount);
        }
        
        // Apply currency condition if specified
        if (currency != null && currencyColumnName != null) {
            queryBuilder.and(currencyColumnName + " = ?")
                       .addParameter(currency);
        }
        
        return queryBuilder;
    }
    
    /**
     * Generates SQL conditions for this amount range filter.
     * 
     * @param columnName The database column name for the amount
     * @param currencyColumnName The database column name for the currency (optional)
     * @return A list of SQL conditions, or an empty list if no filter is applied
     */
    public List<String> toSqlConditions(String columnName, String currencyColumnName) {
        List<String> conditions = new ArrayList<>();
        
        // Add amount range conditions
        if (minAmount != null && maxAmount != null) {
            conditions.add(columnName + " BETWEEN ? AND ?");
        } else if (minAmount != null) {
            conditions.add(columnName + " >= ?");
        } else if (maxAmount != null) {
            conditions.add(columnName + " <= ?");
        }
        
        // Add currency condition if specified
        if (currency != null && currencyColumnName != null) {
            conditions.add(currencyColumnName + " = ?");
        }
        
        return conditions;
    }
    
    /**
     * Gets the parameters for the SQL conditions.
     * 
     * @return A list of parameters, or an empty list if no filter is applied
     */
    public List<Object> getSqlParameters() {
        List<Object> parameters = new ArrayList<>();
        
        // Add amount range parameters
        if (minAmount != null && maxAmount != null) {
            parameters.add(minAmount);
            parameters.add(maxAmount);
        } else if (minAmount != null) {
            parameters.add(minAmount);
        } else if (maxAmount != null) {
            parameters.add(maxAmount);
        }
        
        // Add currency parameter if specified
        if (currency != null) {
            parameters.add(currency);
        }
        
        return parameters;
    }
    
    /**
     * Binds the parameters to a prepared statement.
     * 
     * @param statement The prepared statement
     * @param startIndex The starting parameter index (1-based)
     * @return The next parameter index
     * @throws SQLException If a database access error occurs
     */
    public int bindParameters(PreparedStatement statement, int startIndex) throws SQLException {
        int paramIndex = startIndex;
        
        // Bind amount range parameters
        if (minAmount != null && maxAmount != null) {
            statement.setBigDecimal(paramIndex++, minAmount);
            statement.setBigDecimal(paramIndex++, maxAmount);
        } else if (minAmount != null) {
            statement.setBigDecimal(paramIndex++, minAmount);
        } else if (maxAmount != null) {
            statement.setBigDecimal(paramIndex++, maxAmount);
        }
        
        // Bind currency parameter if specified
        if (currency != null) {
            statement.setString(paramIndex++, currency);
        }
        
        return paramIndex;
    }
    
    /**
     * Converts an amount from the source currency to the target currency.
     * 
     * @param amount The amount to convert
     * @return The converted amount
     */
    public BigDecimal convertAmount(BigDecimal amount) {
        if (amount == null || !requiresConversion || exchangeRate == null) {
            return amount;
        }
        
        return normalizeAmount(amount.multiply(exchangeRate));
    }
    
    /**
     * Gets the minimum amount of the range.
     * 
     * @return The minimum amount, or null if not set
     */
    public BigDecimal getMinAmount() {
        return minAmount;
    }
    
    /**
     * Sets the minimum amount of the range.
     * 
     * @param minAmount The minimum amount to set
     * @return This filter for method chaining
     */
    public AmountRangeFilter setMinAmount(BigDecimal minAmount) {
        this.minAmount = normalizeAmount(minAmount);
        return this;
    }
    
    /**
     * Gets the maximum amount of the range.
     * 
     * @return The maximum amount, or null if not set
     */
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }
    
    /**
     * Sets the maximum amount of the range.
     * 
     * @param maxAmount The maximum amount to set
     * @return This filter for method chaining
     */
    public AmountRangeFilter setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = normalizeAmount(maxAmount);
        return this;
    }
    
    /**
     * Gets the currency code.
     * 
     * @return The currency code, or null if not set
     */
    public String getCurrency() {
        return currency;
    }
    
    /**
     * Sets the currency code.
     * 
     * @param currency The currency code to set
     * @return This filter for method chaining
     */
    public AmountRangeFilter setCurrency(String currency) {
        this.currency = currency != null ? currency.toUpperCase() : null;
        return this;
    }
    
    /**
     * Checks if this filter requires currency conversion.
     * 
     * @return true if currency conversion is required, false otherwise
     */
    public boolean requiresConversion() {
        return requiresConversion;
    }
    
    /**
     * Sets whether this filter requires currency conversion.
     * 
     * @param requiresConversion true if currency conversion is required, false otherwise
     * @return This filter for method chaining
     */
    public AmountRangeFilter setRequiresConversion(boolean requiresConversion) {
        this.requiresConversion = requiresConversion;
        return this;
    }
    
    /**
     * Gets the exchange rate for currency conversion.
     * 
     * @return The exchange rate
     */
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }
    
    /**
     * Sets the exchange rate for currency conversion.
     * 
     * @param exchangeRate The exchange rate to set
     * @return This filter for method chaining
     */
    public AmountRangeFilter setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate != null ? exchangeRate : BigDecimal.ONE;
        return this;
    }
    
    /**
     * Checks if this amount range filter has any constraints.
     * 
     * @return true if either min amount, max amount, or currency is set, false otherwise
     */
    public boolean hasConstraints() {
        return minAmount != null || maxAmount != null || currency != null;
    }
    
    /**
     * Gets the range width (difference between max and min amounts).
     * 
     * @return The range width, or null if the range is open-ended
     */
    public BigDecimal getRangeWidth() {
        if (minAmount != null && maxAmount != null) {
            return maxAmount.subtract(minAmount);
        }
        return null;
    }
    
    /**
     * Returns a string representation of this amount range filter.
     * 
     * @return A string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AmountRangeFilter[");
        if (minAmount != null) {
            sb.append("min=").append(minAmount);
        }
        if (minAmount != null && maxAmount != null) {
            sb.append(", ");
        }
        if (maxAmount != null) {
            sb.append("max=").append(maxAmount);
        }
        if ((minAmount != null || maxAmount != null) && currency != null) {
            sb.append(", ");
        }
        if (currency != null) {
            sb.append("currency=").append(currency);
        }
        if (requiresConversion) {
            sb.append(", exchangeRate=").append(exchangeRate);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Checks if this filter is equal to another object.
     * 
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        AmountRangeFilter other = (AmountRangeFilter) obj;
        return Objects.equals(minAmount, other.minAmount) &&
               Objects.equals(maxAmount, other.maxAmount) &&
               Objects.equals(currency, other.currency) &&
               requiresConversion == other.requiresConversion &&
               Objects.equals(exchangeRate, other.exchangeRate);
    }
    
    /**
     * Generates a hash code for this filter.
     * 
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(minAmount, maxAmount, currency, requiresConversion, exchangeRate);
    }
}