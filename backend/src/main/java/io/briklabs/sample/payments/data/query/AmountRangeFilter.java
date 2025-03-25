package io.briklabs.sample.payments.data.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized filter component for handling amount range operations in payment queries.
 * This class provides structured representation and validation of monetary amount filtering
 * with support for currency conversion, decimal precision handling, and range validation.
 * 
 * It ensures consistent handling of financial amounts across different currencies while
 * generating appropriate SQL conditions for payment transaction filtering.
 */
public class AmountRangeFilter {
    private static final Logger logger = LoggerFactory.getLogger(AmountRangeFilter.class);
    
    // Standard decimal scale for monetary amounts (4 decimal places)
    private static final int DECIMAL_SCALE = 4;
    
    // Maximum precision for monetary amounts (19 digits total)
    private static final int DECIMAL_PRECISION = 19;
    
    // Minimum amount value allowed (can be negative for adjustments/refunds)
    private static final BigDecimal MIN_ALLOWED_AMOUNT = new BigDecimal("-999999999999.9999");
    
    // Maximum amount value allowed
    private static final BigDecimal MAX_ALLOWED_AMOUNT = new BigDecimal("999999999999.9999");
    
    // Decimal formatter for parsing string amounts with locale awareness
    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMATTER = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("#,##0.####", symbols);
        format.setParseBigDecimal(true);
        return format;
    });
    
    // Minimum and maximum amounts for the range
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    
    // Currency code (ISO 4217) for the amounts
    private String currency;
    
    /**
     * Creates a new empty amount range filter.
     */
    public AmountRangeFilter() {
        // Default constructor creates an empty filter
    }
    
    /**
     * Creates a new amount range filter with the specified minimum and maximum amounts.
     * 
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @param currency The currency code (optional)
     */
    public AmountRangeFilter(BigDecimal minAmount, BigDecimal maxAmount, String currency) {
        setMinAmount(minAmount);
        setMaxAmount(maxAmount);
        setCurrency(currency);
    }
    
    /**
     * Creates a new amount range filter with the specified minimum and maximum amounts as strings.
     * The method will attempt to parse the amounts using various common formats.
     * 
     * @param minAmountStr The minimum amount string (inclusive)
     * @param maxAmountStr The maximum amount string (inclusive)
     * @param currency The currency code (optional)
     * @throws IllegalArgumentException If the amount strings cannot be parsed
     */
    public AmountRangeFilter(String minAmountStr, String maxAmountStr, String currency) {
        if (minAmountStr != null && !minAmountStr.isEmpty()) {
            try {
                this.minAmount = parseAmount(minAmountStr);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid minimum amount format: " + minAmountStr, e);
            }
        }
        
        if (maxAmountStr != null && !maxAmountStr.isEmpty()) {
            try {
                this.maxAmount = parseAmount(maxAmountStr);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid maximum amount format: " + maxAmountStr, e);
            }
        }
        
        setCurrency(currency);
    }
    
    /**
     * Parses an amount string into a BigDecimal, handling various formats.
     * 
     * @param amountStr The amount string to parse
     * @return The parsed BigDecimal amount
     * @throws ParseException If the string cannot be parsed as an amount
     */
    private BigDecimal parseAmount(String amountStr) throws ParseException {
        if (amountStr == null || amountStr.isEmpty()) {
            return null;
        }
        
        // Remove currency symbols and other non-numeric characters except decimal and grouping separators
        String cleanedAmount = amountStr.replaceAll("[^\\d.,\\-]", "");
        
        // Try parsing with the decimal formatter
        BigDecimal amount = (BigDecimal) DECIMAL_FORMATTER.get().parse(cleanedAmount);
        
        // Scale the amount to the standard decimal scale
        return amount.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
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
     * @return This object for method chaining
     */
    public AmountRangeFilter setMinAmount(BigDecimal minAmount) {
        if (minAmount != null) {
            // Ensure proper scale for the amount
            this.minAmount = minAmount.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        } else {
            this.minAmount = null;
        }
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
     * @return This object for method chaining
     */
    public AmountRangeFilter setMaxAmount(BigDecimal maxAmount) {
        if (maxAmount != null) {
            // Ensure proper scale for the amount
            this.maxAmount = maxAmount.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        } else {
            this.maxAmount = null;
        }
        return this;
    }
    
    /**
     * Gets the currency code for the amounts.
     * 
     * @return The currency code, or null if not set
     */
    public String getCurrency() {
        return currency;
    }
    
    /**
     * Sets the currency code for the amounts.
     * 
     * @param currency The currency code to set (ISO 4217)
     * @return This object for method chaining
     */
    public AmountRangeFilter setCurrency(String currency) {
        if (currency != null) {
            // Normalize to uppercase and validate length
            String normalizedCurrency = currency.trim().toUpperCase();
            if (normalizedCurrency.length() > 3) {
                throw new IllegalArgumentException("Currency code must be 3 characters or less: " + currency);
            }
            this.currency = normalizedCurrency;
        } else {
            this.currency = null;
        }
        return this;
    }
    
    /**
     * Checks if this filter has any constraints.
     * 
     * @return true if either min or max amount is set, false otherwise
     */
    public boolean hasConstraints() {
        return minAmount != null || maxAmount != null;
    }
    
    /**
     * Validates the amount range for consistency and correctness.
     * 
     * @throws IllegalArgumentException If the amount range is invalid
     */
    public void validate() {
        // Check if min amount is within allowed range
        if (minAmount != null) {
            if (minAmount.compareTo(MIN_ALLOWED_AMOUNT) < 0) {
                throw new IllegalArgumentException("Minimum amount is below the allowed minimum: " + minAmount);
            }
            if (minAmount.compareTo(MAX_ALLOWED_AMOUNT) > 0) {
                throw new IllegalArgumentException("Minimum amount is above the allowed maximum: " + minAmount);
            }
        }
        
        // Check if max amount is within allowed range
        if (maxAmount != null) {
            if (maxAmount.compareTo(MIN_ALLOWED_AMOUNT) < 0) {
                throw new IllegalArgumentException("Maximum amount is below the allowed minimum: " + maxAmount);
            }
            if (maxAmount.compareTo(MAX_ALLOWED_AMOUNT) > 0) {
                throw new IllegalArgumentException("Maximum amount is above the allowed maximum: " + maxAmount);
            }
        }
        
        // Check if min amount is less than or equal to max amount
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Minimum amount must be less than or equal to maximum amount");
        }
    }
    
    /**
     * Generates a SQL condition for this amount range filter.
     * 
     * @param columnName The database column name to filter on
     * @return A SQL condition string, or null if no constraints
     */
    public String toSqlCondition(String columnName) {
        if (!hasConstraints()) {
            return null;
        }
        
        StringBuilder condition = new StringBuilder();
        
        if (minAmount != null && maxAmount != null) {
            // Both min and max amounts are specified
            condition.append(columnName)
                    .append(" BETWEEN ")
                    .append(minAmount.toString())
                    .append(" AND ")
                    .append(maxAmount.toString());
        } else if (minAmount != null) {
            // Only min amount is specified
            condition.append(columnName)
                    .append(" >= ")
                    .append(minAmount.toString());
        } else if (maxAmount != null) {
            // Only max amount is specified
            condition.append(columnName)
                    .append(" <= ")
                    .append(maxAmount.toString());
        }
        
        // Add currency condition if specified
        if (currency != null) {
            condition.append(" AND currency = '")
                    .append(currency)
                    .append("'");
        }
        
        return condition.toString();
    }
    
    /**
     * Generates a parameterized SQL condition for this amount range filter.
     * 
     * @param columnName The database column name to filter on
     * @return A SQL condition string with ? placeholders, or null if no constraints
     */
    public String toParameterizedSqlCondition(String columnName) {
        if (!hasConstraints()) {
            return null;
        }
        
        StringBuilder condition = new StringBuilder();
        
        if (minAmount != null && maxAmount != null) {
            // Both min and max amounts are specified
            condition.append(columnName)
                    .append(" BETWEEN ? AND ?");
        } else if (minAmount != null) {
            // Only min amount is specified
            condition.append(columnName)
                    .append(" >= ?");
        } else if (maxAmount != null) {
            // Only max amount is specified
            condition.append(columnName)
                    .append(" <= ?");
        }
        
        // Add currency condition if specified
        if (currency != null) {
            condition.append(" AND currency = ?");
        }
        
        return condition.toString();
    }
    
    /**
     * Adds this amount range filter's parameters to a prepared statement.
     * 
     * @param statement The prepared statement
     * @param startIndex The parameter index to start with
     * @return The next parameter index
     * @throws SQLException If a database access error occurs
     */
    public int addParametersToStatement(PreparedStatement statement, int startIndex) 
            throws SQLException {
        int index = startIndex;
        
        if (minAmount != null && maxAmount != null) {
            // Both min and max amounts are specified
            statement.setBigDecimal(index++, minAmount);
            statement.setBigDecimal(index++, maxAmount);
        } else if (minAmount != null) {
            // Only min amount is specified
            statement.setBigDecimal(index++, minAmount);
        } else if (maxAmount != null) {
            // Only max amount is specified
            statement.setBigDecimal(index++, maxAmount);
        }
        
        // Add currency parameter if specified
        if (currency != null) {
            statement.setString(index++, currency);
        }
        
        return index;
    }
    
    /**
     * Applies currency conversion to the amounts in this filter.
     * 
     * @param targetCurrency The target currency to convert to
     * @param exchangeRate The exchange rate to apply
     * @return A new amount range filter with converted amounts
     */
    public AmountRangeFilter convertCurrency(String targetCurrency, BigDecimal exchangeRate) {
        if (targetCurrency == null || targetCurrency.isEmpty()) {
            throw new IllegalArgumentException("Target currency cannot be null or empty");
        }
        
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        
        AmountRangeFilter converted = new AmountRangeFilter();
        converted.setCurrency(targetCurrency);
        
        if (minAmount != null) {
            converted.setMinAmount(minAmount.multiply(exchangeRate).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP));
        }
        
        if (maxAmount != null) {
            converted.setMaxAmount(maxAmount.multiply(exchangeRate).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP));
        }
        
        return converted;
    }
    
    /**
     * Creates a copy of this amount range filter.
     * 
     * @return A new amount range filter with the same values
     */
    public AmountRangeFilter copy() {
        AmountRangeFilter copy = new AmountRangeFilter();
        copy.minAmount = this.minAmount;
        copy.maxAmount = this.maxAmount;
        copy.currency = this.currency;
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AmountRangeFilter{");
        
        if (minAmount != null) {
            sb.append("minAmount=").append(minAmount);
        }
        
        if (maxAmount != null) {
            if (minAmount != null) {
                sb.append(", ");
            }
            sb.append("maxAmount=").append(maxAmount);
        }
        
        if (currency != null) {
            if (minAmount != null || maxAmount != null) {
                sb.append(", ");
            }
            sb.append("currency=").append(currency);
        }
        
        sb.append('}');
        return sb.toString();
    }
}