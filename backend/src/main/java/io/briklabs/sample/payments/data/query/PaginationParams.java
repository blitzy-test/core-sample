package io.briklabs.sample.payments.data.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class that handles pagination parameters for payment transaction queries.
 * It provides a structured representation of limit, offset, and page-based navigation
 * with validation and default values. This class handles the conversion between different
 * pagination models (page/size vs offset/limit) and generates appropriate SQL clauses.
 */
public class PaginationParams {
    private static final Logger logger = LoggerFactory.getLogger(PaginationParams.class);
    
    // Default pagination values
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int MAX_PAGE_SIZE = 100;
    
    private int limit;
    private int offset;
    private boolean calculateTotalCount;
    
    /**
     * Creates new pagination parameters with default values.
     */
    public PaginationParams() {
        this(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NUMBER);
    }
    
    /**
     * Creates new pagination parameters with page size and number.
     * 
     * @param pageSize The page size
     * @param pageNumber The page number (0-based)
     */
    public PaginationParams(int pageSize, int pageNumber) {
        this.limit = validatePageSize(pageSize);
        this.offset = calculateOffset(this.limit, validatePageNumber(pageNumber));
        this.calculateTotalCount = true;
    }
    
    /**
     * Creates new pagination parameters with limit and offset.
     * 
     * @param limit The maximum number of records to return
     * @param offset The number of records to skip
     */
    public PaginationParams(int limit, int offset) {
        this.limit = validatePageSize(limit);
        this.offset = validateOffset(offset);
        this.calculateTotalCount = true;
    }
    
    /**
     * Validates and normalizes the page size.
     * 
     * @param pageSize The page size to validate
     * @return The validated page size
     */
    private int validatePageSize(int pageSize) {
        if (pageSize < 1) {
            logger.warn("Invalid page size: {}, using default", pageSize);
            return DEFAULT_PAGE_SIZE;
        } else if (pageSize > MAX_PAGE_SIZE) {
            logger.warn("Page size too large: {}, using maximum", pageSize);
            return MAX_PAGE_SIZE;
        }
        return pageSize;
    }
    
    /**
     * Validates and normalizes the page number.
     * 
     * @param pageNumber The page number to validate
     * @return The validated page number
     */
    private int validatePageNumber(int pageNumber) {
        if (pageNumber < 0) {
            logger.warn("Invalid page number: {}, using default", pageNumber);
            return DEFAULT_PAGE_NUMBER;
        }
        return pageNumber;
    }
    
    /**
     * Validates and normalizes the offset.
     * 
     * @param offset The offset to validate
     * @return The validated offset
     */
    private int validateOffset(int offset) {
        if (offset < 0) {
            logger.warn("Invalid offset: {}, using 0", offset);
            return 0;
        }
        return offset;
    }
    
    /**
     * Calculates the offset from page size and number.
     * 
     * @param pageSize The page size
     * @param pageNumber The page number (0-based)
     * @return The calculated offset
     */
    private int calculateOffset(int pageSize, int pageNumber) {
        return pageNumber * pageSize;
    }
    
    /**
     * Creates pagination parameters from page size and number strings.
     * 
     * @param pageSize The page size as string
     * @param pageNumber The page number as string (0-based)
     * @return New pagination parameters
     */
    public static PaginationParams fromPageParams(String pageSize, String pageNumber) {
        int size = DEFAULT_PAGE_SIZE;
        int page = DEFAULT_PAGE_NUMBER;
        
        if (pageSize != null && !pageSize.isEmpty()) {
            try {
                size = Integer.parseInt(pageSize);
            } catch (NumberFormatException e) {
                logger.warn("Invalid page size format: {}, using default", pageSize);
            }
        }
        
        if (pageNumber != null && !pageNumber.isEmpty()) {
            try {
                page = Integer.parseInt(pageNumber);
            } catch (NumberFormatException e) {
                logger.warn("Invalid page number format: {}, using default", pageNumber);
            }
        }
        
        return new PaginationParams(size, page);
    }
    
    /**
     * Creates pagination parameters from limit and offset strings.
     * 
     * @param limit The limit as string
     * @param offset The offset as string
     * @return New pagination parameters
     */
    public static PaginationParams fromLimitOffset(String limit, String offset) {
        int limitValue = DEFAULT_PAGE_SIZE;
        int offsetValue = 0;
        
        if (limit != null && !limit.isEmpty()) {
            try {
                limitValue = Integer.parseInt(limit);
            } catch (NumberFormatException e) {
                logger.warn("Invalid limit format: {}, using default", limit);
            }
        }
        
        if (offset != null && !offset.isEmpty()) {
            try {
                offsetValue = Integer.parseInt(offset);
            } catch (NumberFormatException e) {
                logger.warn("Invalid offset format: {}, using default", offset);
            }
        }
        
        return new PaginationParams(limitValue, offsetValue);
    }
    
    /**
     * Validates the pagination parameters.
     * 
     * @throws IllegalArgumentException If the pagination parameters are invalid
     */
    public void validate() {
        if (limit < 1) {
            throw new IllegalArgumentException("Page size must be at least 1");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
    }
    
    /**
     * Gets the limit (page size).
     * 
     * @return The limit
     */
    public int getLimit() {
        return limit;
    }
    
    /**
     * Sets the limit (page size).
     * 
     * @param limit The limit to set
     * @return This object for method chaining
     */
    public PaginationParams setLimit(int limit) {
        this.limit = validatePageSize(limit);
        return this;
    }
    
    /**
     * Gets the offset.
     * 
     * @return The offset
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * Sets the offset.
     * 
     * @param offset The offset to set
     * @return This object for method chaining
     */
    public PaginationParams setOffset(int offset) {
        this.offset = validateOffset(offset);
        return this;
    }
    
    /**
     * Gets the page number calculated from limit and offset.
     * 
     * @return The page number (0-based)
     */
    public int getPageNumber() {
        return limit > 0 ? offset / limit : 0;
    }
    
    /**
     * Sets the page number and recalculates the offset.
     * 
     * @param pageNumber The page number to set (0-based)
     * @return This object for method chaining
     */
    public PaginationParams setPageNumber(int pageNumber) {
        int validPageNumber = validatePageNumber(pageNumber);
        this.offset = calculateOffset(this.limit, validPageNumber);
        return this;
    }
    
    /**
     * Gets the page size (same as limit).
     * 
     * @return The page size
     */
    public int getPageSize() {
        return limit;
    }
    
    /**
     * Sets the page size and recalculates the offset to maintain the current page.
     * 
     * @param pageSize The page size to set
     * @return This object for method chaining
     */
    public PaginationParams setPageSize(int pageSize) {
        int currentPage = getPageNumber();
        this.limit = validatePageSize(pageSize);
        this.offset = calculateOffset(this.limit, currentPage);
        return this;
    }
    
    /**
     * Checks if total count calculation is enabled.
     * 
     * @return true if total count should be calculated, false otherwise
     */
    public boolean isCalculateTotalCount() {
        return calculateTotalCount;
    }
    
    /**
     * Sets whether to calculate the total count.
     * 
     * @param calculateTotalCount true to calculate total count, false otherwise
     * @return This object for method chaining
     */
    public PaginationParams setCalculateTotalCount(boolean calculateTotalCount) {
        this.calculateTotalCount = calculateTotalCount;
        return this;
    }
    
    /**
     * Advances to the next page by incrementing the page number.
     * 
     * @return This object for method chaining
     */
    public PaginationParams nextPage() {
        this.offset += this.limit;
        return this;
    }
    
    /**
     * Moves to the previous page by decrementing the page number.
     * 
     * @return This object for method chaining
     */
    public PaginationParams previousPage() {
        if (this.offset >= this.limit) {
            this.offset -= this.limit;
        } else {
            this.offset = 0;
        }
        return this;
    }
    
    /**
     * Moves to the first page by setting the offset to 0.
     * 
     * @return This object for method chaining
     */
    public PaginationParams firstPage() {
        this.offset = 0;
        return this;
    }
    
    /**
     * Generates the SQL LIMIT clause for this pagination.
     * 
     * @return The SQL LIMIT clause
     */
    public String toLimitClause() {
        return "LIMIT " + limit;
    }
    
    /**
     * Generates the SQL OFFSET clause for this pagination.
     * 
     * @return The SQL OFFSET clause
     */
    public String toOffsetClause() {
        return "OFFSET " + offset;
    }
    
    /**
     * Generates the combined SQL LIMIT and OFFSET clauses for this pagination.
     * 
     * @return The combined SQL LIMIT and OFFSET clauses
     */
    public String toSqlClauses() {
        return toLimitClause() + " " + toOffsetClause();
    }
    
    /**
     * Creates a new PaginationParams instance with default values.
     * 
     * @return A new PaginationParams instance
     */
    public static PaginationParams getDefault() {
        return new PaginationParams();
    }
    
    /**
     * Creates a new PaginationParams instance with the specified page size and default page number.
     * 
     * @param pageSize The page size
     * @return A new PaginationParams instance
     */
    public static PaginationParams ofSize(int pageSize) {
        return new PaginationParams(pageSize, DEFAULT_PAGE_NUMBER);
    }
    
    /**
     * Creates a new PaginationParams instance with the specified page size and page number.
     * 
     * @param pageSize The page size
     * @param pageNumber The page number (0-based)
     * @return A new PaginationParams instance
     */
    public static PaginationParams of(int pageSize, int pageNumber) {
        return new PaginationParams(pageSize, pageNumber);
    }
    
    /**
     * Creates a new PaginationParams instance with the specified limit and offset.
     * 
     * @param limit The limit
     * @param offset The offset
     * @return A new PaginationParams instance
     */
    public static PaginationParams ofLimitOffset(int limit, int offset) {
        return new PaginationParams(limit, offset);
    }
    
    @Override
    public String toString() {
        return "PaginationParams{" +
                "limit=" + limit +
                ", offset=" + offset +
                ", pageNumber=" + getPageNumber() +
                ", calculateTotalCount=" + calculateTotalCount +
                '}';
    }
}