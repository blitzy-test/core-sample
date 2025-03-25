package io.briklabs.sample.payments.data.query;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized filter component for handling date range operations in payment queries.
 * This class provides structured representation and validation of date-based filtering
 * with support for various date formats, open-ended ranges, and relative date expressions.
 * 
 * It contains logic for converting between different date formats and generating
 * appropriate SQL conditions for date comparisons.
 */
public class DateRangeFilter {
    private static final Logger logger = LoggerFactory.getLogger(DateRangeFilter.class);
    
    // Standard date format patterns
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter CUSTOM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Pattern for relative date expressions like "last 30 days", "this month", etc.
    private static final Pattern RELATIVE_DATE_PATTERN = 
            Pattern.compile("(last|this|next)\\s+(\\d+)\\s+(day|days|week|weeks|month|months|year|years)");
    
    // The start date (inclusive) for the range
    private LocalDateTime startDate;
    
    // The end date (inclusive) for the range
    private LocalDateTime endDate;
    
    // The original expression used to create this filter (for debugging)
    private String originalExpression;
    
    // The timezone ID to use for date calculations
    private ZoneId zoneId;
    
    /**
     * Creates a new date range filter with explicit start and end dates.
     * 
     * @param startDate The start date (inclusive), can be null for open-ended ranges
     * @param endDate The end date (inclusive), can be null for open-ended ranges
     */
    public DateRangeFilter(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.zoneId = ZoneId.systemDefault();
    }
    
    /**
     * Creates a new date range filter with explicit start and end dates and timezone.
     * 
     * @param startDate The start date (inclusive), can be null for open-ended ranges
     * @param endDate The end date (inclusive), can be null for open-ended ranges
     * @param zoneId The timezone ID to use for date calculations
     */
    public DateRangeFilter(LocalDateTime startDate, LocalDateTime endDate, ZoneId zoneId) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.zoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
    }
    
    /**
     * Creates a new date range filter from string representations of dates.
     * 
     * @param startDateStr The start date as string (inclusive), can be null for open-ended ranges
     * @param endDateStr The end date as string (inclusive), can be null for open-ended ranges
     * @throws DateTimeParseException If the date strings cannot be parsed
     */
    public DateRangeFilter(String startDateStr, String endDateStr) {
        this.zoneId = ZoneId.systemDefault();
        
        if (startDateStr != null && !startDateStr.isEmpty()) {
            this.startDate = parseDateTime(startDateStr);
            this.originalExpression = startDateStr;
        }
        
        if (endDateStr != null && !endDateStr.isEmpty()) {
            this.endDate = parseDateTime(endDateStr);
            this.originalExpression = (this.originalExpression != null ? 
                    this.originalExpression + " to " : "") + endDateStr;
        }
    }
    
    /**
     * Creates a new date range filter from a relative date expression.
     * 
     * @param expression The relative date expression (e.g., "last 30 days", "this month")
     * @throws IllegalArgumentException If the expression cannot be parsed
     */
    public DateRangeFilter(String expression) {
        this.zoneId = ZoneId.systemDefault();
        this.originalExpression = expression;
        
        if (expression == null || expression.isEmpty()) {
            return;
        }
        
        // Try to parse as a relative date expression
        Matcher matcher = RELATIVE_DATE_PATTERN.matcher(expression.toLowerCase().trim());
        if (matcher.matches()) {
            String relation = matcher.group(1); // "last", "this", or "next"
            int amount = Integer.parseInt(matcher.group(2)); // numeric value
            String unit = matcher.group(3); // "day", "days", "week", etc.
            
            calculateRelativeDateRange(relation, amount, unit);
        } else {
            // Try some predefined expressions
            switch (expression.toLowerCase().trim()) {
                case "today":
                    setTodayRange();
                    break;
                case "yesterday":
                    setYesterdayRange();
                    break;
                case "this week":
                    setThisWeekRange();
                    break;
                case "this month":
                    setThisMonthRange();
                    break;
                case "this year":
                    setThisYearRange();
                    break;
                case "last week":
                    setLastWeekRange();
                    break;
                case "last month":
                    setLastMonthRange();
                    break;
                case "last year":
                    setLastYearRange();
                    break;
                default:
                    // Try to parse as a single date (will set both start and end to the same day)
                    try {
                        LocalDateTime date = parseDateTime(expression);
                        setDayRange(date.toLocalDate());
                    } catch (DateTimeParseException e) {
                        logger.warn("Unable to parse date expression: {}", expression);
                        throw new IllegalArgumentException("Invalid date expression: " + expression);
                    }
            }
        }
    }
    
    /**
     * Parses a date-time string using multiple formats.
     * 
     * @param dateTimeStr The date-time string to parse
     * @return The parsed LocalDateTime
     * @throws DateTimeParseException If the string cannot be parsed with any supported format
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        // Try ISO date-time format first
        try {
            return LocalDateTime.parse(dateTimeStr, ISO_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Try ISO date format (will set time to start of day)
            try {
                return LocalDate.parse(dateTimeStr, ISO_DATE_FORMATTER).atStartOfDay();
            } catch (DateTimeParseException e2) {
                // Try custom format
                try {
                    return LocalDateTime.parse(dateTimeStr, CUSTOM_DATE_FORMATTER);
                } catch (DateTimeParseException e3) {
                    // If all parsing attempts fail, throw the original exception
                    logger.warn("Failed to parse date string: {}", dateTimeStr);
                    throw new DateTimeParseException(
                            "Unable to parse date-time string with any supported format", 
                            dateTimeStr, 
                            e.getErrorIndex());
                }
            }
        }
    }
    
    /**
     * Calculates a date range based on a relative expression.
     * 
     * @param relation The relation ("last", "this", or "next")
     * @param amount The numeric amount
     * @param unit The time unit ("day", "days", "week", etc.)
     */
    private void calculateRelativeDateRange(String relation, int amount, String unit) {
        LocalDateTime now = LocalDateTime.now(zoneId);
        
        // Normalize the unit to singular form
        String normalizedUnit = unit.endsWith("s") ? unit.substring(0, unit.length() - 1) : unit;
        
        switch (relation) {
            case "last":
                calculatePastRange(now, amount, normalizedUnit);
                break;
            case "this":
                calculateCurrentRange(now, normalizedUnit);
                break;
            case "next":
                calculateFutureRange(now, amount, normalizedUnit);
                break;
            default:
                throw new IllegalArgumentException("Unsupported relation: " + relation);
        }
    }
    
    /**
     * Calculates a date range for a past period.
     * 
     * @param now The current date-time
     * @param amount The amount to go back
     * @param unit The time unit
     */
    private void calculatePastRange(LocalDateTime now, int amount, String unit) {
        switch (unit) {
            case "day":
                endDate = now;
                startDate = now.minusDays(amount);
                break;
            case "week":
                endDate = now;
                startDate = now.minusWeeks(amount);
                break;
            case "month":
                endDate = now;
                startDate = now.minusMonths(amount);
                break;
            case "year":
                endDate = now;
                startDate = now.minusYears(amount);
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
    
    /**
     * Calculates a date range for the current period.
     * 
     * @param now The current date-time
     * @param unit The time unit
     */
    private void calculateCurrentRange(LocalDateTime now, String unit) {
        switch (unit) {
            case "day":
                setDayRange(now.toLocalDate());
                break;
            case "week":
                setWeekRange(now.toLocalDate());
                break;
            case "month":
                setMonthRange(now.toLocalDate());
                break;
            case "year":
                setYearRange(now.toLocalDate());
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
    
    /**
     * Calculates a date range for a future period.
     * 
     * @param now The current date-time
     * @param amount The amount to go forward
     * @param unit The time unit
     */
    private void calculateFutureRange(LocalDateTime now, int amount, String unit) {
        switch (unit) {
            case "day":
                startDate = now;
                endDate = now.plusDays(amount);
                break;
            case "week":
                startDate = now;
                endDate = now.plusWeeks(amount);
                break;
            case "month":
                startDate = now;
                endDate = now.plusMonths(amount);
                break;
            case "year":
                startDate = now;
                endDate = now.plusYears(amount);
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
    
    /**
     * Sets the date range to cover a single day.
     * 
     * @param date The date to cover
     */
    private void setDayRange(LocalDate date) {
        startDate = date.atStartOfDay();
        endDate = date.atTime(LocalTime.MAX);
    }
    
    /**
     * Sets the date range to cover a week containing the specified date.
     * 
     * @param date A date within the week
     */
    private void setWeekRange(LocalDate date) {
        // Assuming weeks start on Monday
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate endOfWeek = startOfWeek.plusDays(6); // Sunday
        
        startDate = startOfWeek.atStartOfDay();
        endDate = endOfWeek.atTime(LocalTime.MAX);
    }
    
    /**
     * Sets the date range to cover a month containing the specified date.
     * 
     * @param date A date within the month
     */
    private void setMonthRange(LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
        
        startDate = startOfMonth.atStartOfDay();
        endDate = endOfMonth.atTime(LocalTime.MAX);
    }
    
    /**
     * Sets the date range to cover a year containing the specified date.
     * 
     * @param date A date within the year
     */
    private void setYearRange(LocalDate date) {
        LocalDate startOfYear = date.withDayOfYear(1);
        LocalDate endOfYear = date.withMonth(12).with(TemporalAdjusters.lastDayOfMonth());
        
        startDate = startOfYear.atStartOfDay();
        endDate = endOfYear.atTime(LocalTime.MAX);
    }
    
    /**
     * Sets the date range to cover today.
     */
    private void setTodayRange() {
        setDayRange(LocalDate.now(zoneId));
    }
    
    /**
     * Sets the date range to cover yesterday.
     */
    private void setYesterdayRange() {
        setDayRange(LocalDate.now(zoneId).minusDays(1));
    }
    
    /**
     * Sets the date range to cover the current week.
     */
    private void setThisWeekRange() {
        setWeekRange(LocalDate.now(zoneId));
    }
    
    /**
     * Sets the date range to cover the current month.
     */
    private void setThisMonthRange() {
        setMonthRange(LocalDate.now(zoneId));
    }
    
    /**
     * Sets the date range to cover the current year.
     */
    private void setThisYearRange() {
        setYearRange(LocalDate.now(zoneId));
    }
    
    /**
     * Sets the date range to cover the previous week.
     */
    private void setLastWeekRange() {
        setWeekRange(LocalDate.now(zoneId).minusWeeks(1));
    }
    
    /**
     * Sets the date range to cover the previous month.
     */
    private void setLastMonthRange() {
        setMonthRange(LocalDate.now(zoneId).minusMonths(1));
    }
    
    /**
     * Sets the date range to cover the previous year.
     */
    private void setLastYearRange() {
        setYearRange(LocalDate.now(zoneId).minusYears(1));
    }
    
    /**
     * Validates the date range for consistency.
     * 
     * @throws IllegalArgumentException If the date range is invalid
     */
    public void validate() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            logger.warn("Invalid date range: start date {} is after end date {}", startDate, endDate);
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }
    
    /**
     * Applies this date range filter to a query builder.
     * 
     * @param queryBuilder The query builder to apply the filter to
     * @param columnName The database column name to filter on
     * @return The updated query builder
     */
    public PaymentQueryBuilder applyTo(PaymentQueryBuilder queryBuilder, String columnName) {
        if (startDate != null && endDate != null) {
            queryBuilder.and(columnName + " BETWEEN ? AND ?")
                       .addParameter(Timestamp.valueOf(startDate))
                       .addParameter(Timestamp.valueOf(endDate));
        } else if (startDate != null) {
            queryBuilder.and(columnName + " >= ?")
                       .addParameter(Timestamp.valueOf(startDate));
        } else if (endDate != null) {
            queryBuilder.and(columnName + " <= ?")
                       .addParameter(Timestamp.valueOf(endDate));
        }
        
        return queryBuilder;
    }
    
    /**
     * Generates an SQL condition for this date range filter.
     * 
     * @param columnName The database column name to filter on
     * @return The SQL condition string, or null if no filter is applied
     */
    public String toSqlCondition(String columnName) {
        if (startDate != null && endDate != null) {
            return columnName + " BETWEEN ? AND ?";
        } else if (startDate != null) {
            return columnName + " >= ?";
        } else if (endDate != null) {
            return columnName + " <= ?";
        }
        
        return null;
    }
    
    /**
     * Gets the parameters for the SQL condition.
     * 
     * @return An array of parameters, or an empty array if no filter is applied
     */
    public Object[] getSqlParameters() {
        if (startDate != null && endDate != null) {
            return new Object[] { Timestamp.valueOf(startDate), Timestamp.valueOf(endDate) };
        } else if (startDate != null) {
            return new Object[] { Timestamp.valueOf(startDate) };
        } else if (endDate != null) {
            return new Object[] { Timestamp.valueOf(endDate) };
        }
        
        return new Object[0];
    }
    
    /**
     * Gets the start date of the range.
     * 
     * @return The start date, or null if not set
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    /**
     * Sets the start date of the range.
     * 
     * @param startDate The start date to set
     * @return This filter for method chaining
     */
    public DateRangeFilter setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
        return this;
    }
    
    /**
     * Gets the end date of the range.
     * 
     * @return The end date, or null if not set
     */
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    /**
     * Sets the end date of the range.
     * 
     * @param endDate The end date to set
     * @return This filter for method chaining
     */
    public DateRangeFilter setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
        return this;
    }
    
    /**
     * Gets the timezone ID used for date calculations.
     * 
     * @return The timezone ID
     */
    public ZoneId getZoneId() {
        return zoneId;
    }
    
    /**
     * Sets the timezone ID to use for date calculations.
     * 
     * @param zoneId The timezone ID to set
     * @return This filter for method chaining
     */
    public DateRangeFilter setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
        return this;
    }
    
    /**
     * Gets the original expression used to create this filter.
     * 
     * @return The original expression, or null if not created from an expression
     */
    public String getOriginalExpression() {
        return originalExpression;
    }
    
    /**
     * Checks if this date range filter has any constraints.
     * 
     * @return true if either start date or end date is set, false otherwise
     */
    public boolean hasConstraints() {
        return startDate != null || endDate != null;
    }
    
    /**
     * Calculates the duration of this date range in days.
     * 
     * @return The duration in days, or -1 if the range is open-ended
     */
    public long getDurationInDays() {
        if (startDate != null && endDate != null) {
            return ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        }
        return -1;
    }
    
    /**
     * Returns a string representation of this date range filter.
     * 
     * @return A string representation
     */
    @Override
    public String toString() {
        if (originalExpression != null) {
            return "DateRangeFilter[" + originalExpression + "]";
        }
        
        StringBuilder sb = new StringBuilder("DateRangeFilter[");
        if (startDate != null) {
            sb.append("from=").append(startDate);
        }
        if (startDate != null && endDate != null) {
            sb.append(", ");
        }
        if (endDate != null) {
            sb.append("to=").append(endDate);
        }
        sb.append("]");
        return sb.toString();
    }
}