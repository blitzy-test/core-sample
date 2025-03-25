package io.briklabs.sample.payments.data.query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
 * It handles conversion between different date formats and generates appropriate SQL
 * conditions for date comparisons in payment transaction queries.
 */
public class DateRangeFilter {
    private static final Logger logger = LoggerFactory.getLogger(DateRangeFilter.class);
    
    // Common date formats supported for input parsing
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
    };
    
    // Standard format for SQL timestamp parameters
    private static final DateTimeFormatter SQL_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Pattern for relative date expressions like "last 30 days", "this month", etc.
    private static final Pattern RELATIVE_DATE_PATTERN = Pattern.compile(
            "(?i)(last|this|next)\\s+(\\d+)?\\s*(day|days|week|weeks|month|months|year|years)");
    
    // Default timezone for date operations if not specified
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.systemDefault();
    
    // Start and end dates for the range
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Original expression for relative date ranges
    private String relativeDateExpression;
    
    // Timezone for date operations
    private ZoneId timezone;
    
    /**
     * Creates a new empty date range filter with the system default timezone.
     */
    public DateRangeFilter() {
        this.timezone = DEFAULT_TIMEZONE;
    }
    
    /**
     * Creates a new date range filter with the specified start and end dates.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     */
    public DateRangeFilter(LocalDateTime startDate, LocalDateTime endDate) {
        this();
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    /**
     * Creates a new date range filter with the specified start and end dates as strings.
     * The method will attempt to parse the dates using various common formats.
     * 
     * @param startDateStr The start date string (inclusive)
     * @param endDateStr The end date string (inclusive)
     * @throws IllegalArgumentException If the date strings cannot be parsed
     */
    public DateRangeFilter(String startDateStr, String endDateStr) {
        this();
        
        if (startDateStr != null && !startDateStr.isEmpty()) {
            this.startDate = parseDateTime(startDateStr);
        }
        
        if (endDateStr != null && !endDateStr.isEmpty()) {
            this.endDate = parseDateTime(endDateStr);
            // If only date was provided (no time), set the time to end of day
            if (endDateStr.length() <= 10) {
                this.endDate = this.endDate.with(LocalTime.MAX);
            }
        }
    }
    
    /**
     * Creates a new date range filter using a relative date expression.
     * Supported expressions include:
     * - "last X days/weeks/months/years"
     * - "this day/week/month/year"
     * - "next X days/weeks/months/years"
     * 
     * @param expression The relative date expression
     * @throws IllegalArgumentException If the expression cannot be parsed
     */
    public DateRangeFilter(String expression) {
        this();
        
        if (expression == null || expression.isEmpty()) {
            return;
        }
        
        this.relativeDateExpression = expression.trim();
        
        // Try to parse as a relative date expression
        Matcher matcher = RELATIVE_DATE_PATTERN.matcher(expression);
        if (matcher.matches()) {
            String direction = matcher.group(1).toLowerCase(); // "last", "this", or "next"
            String countStr = matcher.group(2);               // numeric value or null
            String unit = matcher.group(3).toLowerCase();     // "day", "days", "week", etc.
            
            int count = 1;
            if (countStr != null && !countStr.isEmpty()) {
                try {
                    count = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid count in relative date expression: " + expression);
                }
            }
            
            calculateRelativeDateRange(direction, count, unit);
        } else {
            // Handle special predefined ranges
            switch (expression.toLowerCase()) {
                case "today":
                    setToday();
                    break;
                case "yesterday":
                    setYesterday();
                    break;
                case "this week":
                    setThisWeek();
                    break;
                case "last week":
                    setLastWeek();
                    break;
                case "this month":
                    setThisMonth();
                    break;
                case "last month":
                    setLastMonth();
                    break;
                case "this year":
                    setThisYear();
                    break;
                case "last year":
                    setLastYear();
                    break;
                case "last 7 days":
                    setLastNDays(7);
                    break;
                case "last 30 days":
                    setLastNDays(30);
                    break;
                case "last 90 days":
                    setLastNDays(90);
                    break;
                case "last 365 days":
                    setLastNDays(365);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported date range expression: " + expression);
            }
        }
    }
    
    /**
     * Sets the date range to today.
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setToday() {
        LocalDate today = LocalDate.now(timezone);
        this.startDate = today.atStartOfDay();
        this.endDate = today.atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to yesterday.
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setYesterday() {
        LocalDate yesterday = LocalDate.now(timezone).minusDays(1);
        this.startDate = yesterday.atStartOfDay();
        this.endDate = yesterday.atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to this week (Monday to Sunday).
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setThisWeek() {
        LocalDate today = LocalDate.now(timezone);
        this.startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        this.endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to last week (previous Monday to Sunday).
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setLastWeek() {
        LocalDate today = LocalDate.now(timezone);
        LocalDate previousMonday = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        this.startDate = previousMonday.atStartOfDay();
        this.endDate = previousMonday.plusDays(6).atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to this month (1st to last day of current month).
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setThisMonth() {
        LocalDate today = LocalDate.now(timezone);
        this.startDate = today.withDayOfMonth(1).atStartOfDay();
        this.endDate = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to last month (1st to last day of previous month).
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setLastMonth() {
        LocalDate today = LocalDate.now(timezone);
        LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
        this.startDate = firstDayOfLastMonth.atStartOfDay();
        this.endDate = firstDayOfLastMonth.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to this year (January 1st to December 31st of current year).
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setThisYear() {
        LocalDate today = LocalDate.now(timezone);
        this.startDate = today.withDayOfYear(1).atStartOfDay();
        this.endDate = today.withMonth(12).withDayOfMonth(31).atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to last year (January 1st to December 31st of previous year).
     * 
     * @return This object for method chaining
     */
    public DateRangeFilter setLastYear() {
        LocalDate today = LocalDate.now(timezone);
        LocalDate lastYear = today.minusYears(1);
        this.startDate = lastYear.withDayOfYear(1).atStartOfDay();
        this.endDate = lastYear.withMonth(12).withDayOfMonth(31).atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Sets the date range to the last N days (including today).
     * 
     * @param days The number of days to include
     * @return This object for method chaining
     */
    public DateRangeFilter setLastNDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Number of days must be positive");
        }
        
        LocalDate today = LocalDate.now(timezone);
        this.startDate = today.minusDays(days - 1).atStartOfDay();
        this.endDate = today.atTime(LocalTime.MAX);
        return this;
    }
    
    /**
     * Calculates a date range based on a relative expression.
     * 
     * @param direction "last", "this", or "next"
     * @param count The number of units
     * @param unit "day", "days", "week", "weeks", etc.
     */
    private void calculateRelativeDateRange(String direction, int count, String unit) {
        LocalDate today = LocalDate.now(timezone);
        LocalDate start = today;
        LocalDate end = today;
        
        // Normalize unit to singular form
        String normalizedUnit = unit.endsWith("s") ? unit.substring(0, unit.length() - 1) : unit;
        
        switch (direction) {
            case "last":
                switch (normalizedUnit) {
                    case "day":
                        if (count == 1) {
                            // Special case for "last day" (yesterday)
                            start = today.minusDays(1);
                            end = start;
                        } else {
                            // "last N days" includes today
                            start = today.minusDays(count - 1);
                            end = today;
                        }
                        break;
                    case "week":
                        if (count == 1) {
                            // Special case for "last week" (previous Monday-Sunday)
                            start = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
                            end = start.plusDays(6);
                        } else {
                            // "last N weeks" starts N weeks ago on Monday
                            start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(count - 1);
                            end = today;
                        }
                        break;
                    case "month":
                        if (count == 1) {
                            // Special case for "last month" (previous month)
                            start = today.minusMonths(1).withDayOfMonth(1);
                            end = start.with(TemporalAdjusters.lastDayOfMonth());
                        } else {
                            // "last N months" starts N months ago on the 1st
                            start = today.minusMonths(count - 1).withDayOfMonth(1);
                            end = today;
                        }
                        break;
                    case "year":
                        if (count == 1) {
                            // Special case for "last year" (previous year)
                            start = today.minusYears(1).withDayOfYear(1);
                            end = start.withMonth(12).withDayOfMonth(31);
                        } else {
                            // "last N years" starts N years ago on January 1st
                            start = today.minusYears(count - 1).withDayOfYear(1);
                            end = today;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported time unit: " + unit);
                }
                break;
                
            case "this":
                switch (normalizedUnit) {
                    case "day":
                        // "this day" is today
                        start = today;
                        end = today;
                        break;
                    case "week":
                        // "this week" is current Monday-Sunday
                        start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                        end = start.plusDays(6);
                        break;
                    case "month":
                        // "this month" is current month
                        start = today.withDayOfMonth(1);
                        end = today.with(TemporalAdjusters.lastDayOfMonth());
                        break;
                    case "year":
                        // "this year" is current year
                        start = today.withDayOfYear(1);
                        end = today.withMonth(12).withDayOfMonth(31);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported time unit: " + unit);
                }
                break;
                
            case "next":
                switch (normalizedUnit) {
                    case "day":
                        if (count == 1) {
                            // Special case for "next day" (tomorrow)
                            start = today.plusDays(1);
                            end = start;
                        } else {
                            // "next N days" starts tomorrow and goes forward N days
                            start = today.plusDays(1);
                            end = today.plusDays(count);
                        }
                        break;
                    case "week":
                        if (count == 1) {
                            // Special case for "next week" (next Monday-Sunday)
                            start = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                            end = start.plusDays(6);
                        } else {
                            // "next N weeks" starts next Monday and goes forward N weeks
                            start = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                            end = start.plusWeeks(count - 1).plusDays(6);
                        }
                        break;
                    case "month":
                        if (count == 1) {
                            // Special case for "next month" (next month)
                            start = today.plusMonths(1).withDayOfMonth(1);
                            end = start.with(TemporalAdjusters.lastDayOfMonth());
                        } else {
                            // "next N months" starts next month and goes forward N months
                            start = today.plusMonths(1).withDayOfMonth(1);
                            end = today.plusMonths(count).with(TemporalAdjusters.lastDayOfMonth());
                        }
                        break;
                    case "year":
                        if (count == 1) {
                            // Special case for "next year" (next year)
                            start = today.plusYears(1).withDayOfYear(1);
                            end = start.withMonth(12).withDayOfMonth(31);
                        } else {
                            // "next N years" starts next year and goes forward N years
                            start = today.plusYears(1).withDayOfYear(1);
                            end = today.plusYears(count).withMonth(12).withDayOfMonth(31);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported time unit: " + unit);
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
        
        this.startDate = start.atStartOfDay();
        this.endDate = end.atTime(LocalTime.MAX);
    }
    
    /**
     * Attempts to parse a date/time string using various common formats.
     * 
     * @param dateStr The date string to parse
     * @return The parsed LocalDateTime
     * @throws IllegalArgumentException If the string cannot be parsed as a date
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        // Try each formatter in sequence
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // If the string contains only a date (no time), append the start/end of day
                if (dateStr.length() <= 10) {
                    return LocalDate.parse(dateStr, formatter).atStartOfDay();
                } else {
                    return LocalDateTime.parse(dateStr, formatter);
                }
            } catch (DateTimeParseException e) {
                // Try the next formatter
                continue;
            }
        }
        
        // If we get here, none of the formatters worked
        throw new IllegalArgumentException("Unable to parse date string: " + dateStr);
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
     * @return This object for method chaining
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
     * @return This object for method chaining
     */
    public DateRangeFilter setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
        return this;
    }
    
    /**
     * Gets the timezone used for date operations.
     * 
     * @return The timezone
     */
    public ZoneId getTimezone() {
        return timezone;
    }
    
    /**
     * Sets the timezone for date operations.
     * 
     * @param timezone The timezone to set
     * @return This object for method chaining
     */
    public DateRangeFilter setTimezone(ZoneId timezone) {
        this.timezone = timezone != null ? timezone : DEFAULT_TIMEZONE;
        return this;
    }
    
    /**
     * Gets the relative date expression if one was used.
     * 
     * @return The relative date expression, or null if not set
     */
    public String getRelativeDateExpression() {
        return relativeDateExpression;
    }
    
    /**
     * Checks if this filter has any constraints.
     * 
     * @return true if either start or end date is set, false otherwise
     */
    public boolean hasConstraints() {
        return startDate != null || endDate != null;
    }
    
    /**
     * Validates the date range for consistency.
     * 
     * @throws IllegalArgumentException If the date range is invalid
     */
    public void validate() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }
    
    /**
     * Generates a SQL condition for this date range filter.
     * 
     * @param columnName The database column name to filter on
     * @return A SQL condition string, or null if no constraints
     */
    public String toSqlCondition(String columnName) {
        if (!hasConstraints()) {
            return null;
        }
        
        StringBuilder condition = new StringBuilder();
        
        if (startDate != null && endDate != null) {
            // Both start and end dates are specified
            condition.append(columnName)
                    .append(" BETWEEN '")
                    .append(startDate.format(SQL_TIMESTAMP_FORMAT))
                    .append("' AND '")
                    .append(endDate.format(SQL_TIMESTAMP_FORMAT))
                    .append("'");
        } else if (startDate != null) {
            // Only start date is specified
            condition.append(columnName)
                    .append(" >= '")
                    .append(startDate.format(SQL_TIMESTAMP_FORMAT))
                    .append("'");
        } else if (endDate != null) {
            // Only end date is specified
            condition.append(columnName)
                    .append(" <= '")
                    .append(endDate.format(SQL_TIMESTAMP_FORMAT))
                    .append("'");
        }
        
        return condition.toString();
    }
    
    /**
     * Adds this date range filter's parameters to a prepared statement.
     * 
     * @param statement The prepared statement
     * @param startIndex The parameter index to start with
     * @return The next parameter index
     * @throws java.sql.SQLException If a database access error occurs
     */
    public int addParametersToStatement(java.sql.PreparedStatement statement, int startIndex) 
            throws java.sql.SQLException {
        int index = startIndex;
        
        if (startDate != null) {
            statement.setTimestamp(index++, java.sql.Timestamp.valueOf(startDate));
        }
        
        if (endDate != null) {
            statement.setTimestamp(index++, java.sql.Timestamp.valueOf(endDate));
        }
        
        return index;
    }
    
    /**
     * Generates a parameterized SQL condition for this date range filter.
     * 
     * @param columnName The database column name to filter on
     * @return A SQL condition string with ? placeholders, or null if no constraints
     */
    public String toParameterizedSqlCondition(String columnName) {
        if (!hasConstraints()) {
            return null;
        }
        
        StringBuilder condition = new StringBuilder();
        
        if (startDate != null && endDate != null) {
            // Both start and end dates are specified
            condition.append(columnName)
                    .append(" BETWEEN ? AND ?");
        } else if (startDate != null) {
            // Only start date is specified
            condition.append(columnName)
                    .append(" >= ?");
        } else if (endDate != null) {
            // Only end date is specified
            condition.append(columnName)
                    .append(" <= ?");
        }
        
        return condition.toString();
    }
    
    /**
     * Creates a copy of this date range filter.
     * 
     * @return A new date range filter with the same values
     */
    public DateRangeFilter copy() {
        DateRangeFilter copy = new DateRangeFilter();
        copy.startDate = this.startDate;
        copy.endDate = this.endDate;
        copy.relativeDateExpression = this.relativeDateExpression;
        copy.timezone = this.timezone;
        return copy;
    }
    
    @Override
    public String toString() {
        if (relativeDateExpression != null) {
            return "DateRangeFilter{" + relativeDateExpression + "}";
        } else {
            return "DateRangeFilter{" +
                    "startDate=" + (startDate != null ? startDate.format(DateTimeFormatter.ISO_DATE_TIME) : "null") +
                    ", endDate=" + (endDate != null ? endDate.format(DateTimeFormatter.ISO_DATE_TIME) : "null") +
                    "}";
        }
    }
}