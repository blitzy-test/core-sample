package io.briklabs.sample.payments.data.query;

import java.util.Objects;

/**
 * Handles pagination parameters for payment transaction queries.
 * <p>
 * This class provides a structured representation of pagination parameters with support for
 * both page/size and offset/limit models. It includes validation, default values, and
 * conversion between different pagination models. The class generates optimized SQL clauses
 * for LIMIT and OFFSET operations to support efficient pagination of large result sets.
 * </p>
 */
public class PaginationParams {

    /**
     * Default page size when not specified
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum allowed page size to prevent excessive resource usage
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Default starting page (1-based indexing for pages)
     */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /**
     * Default offset value (0-based indexing for records)
     */
    public static final int DEFAULT_OFFSET = 0;

    private Integer limit;
    private Integer offset;
    private Integer pageSize;
    private Integer pageNumber;
    private boolean calculateTotalCount = true;

    /**
     * Creates a new instance with default pagination values.
     * <p>
     * Default values:
     * <ul>
     *   <li>Page size: 20 records</li>
     *   <li>Page number: 1 (first page)</li>
     *   <li>Calculate total count: true</li>
     * </ul>
     * </p>
     */
    public PaginationParams() {
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.pageNumber = DEFAULT_PAGE_NUMBER;
        this.limit = DEFAULT_PAGE_SIZE;
        this.offset = DEFAULT_OFFSET;
    }

    /**
     * Creates a new instance with specified limit and offset values.
     *
     * @param limit  Maximum number of records to return
     * @param offset Number of records to skip
     * @throws IllegalArgumentException if limit is negative or exceeds maximum allowed value,
     *                                  or if offset is negative
     */
    public PaginationParams(Integer limit, Integer offset) {
        setLimit(limit);
        setOffset(offset);
        // Calculate equivalent page size and number
        this.pageSize = this.limit;
        this.pageNumber = (this.offset / this.limit) + 1;
    }

    /**
     * Creates a new instance with specified page size and page number.
     *
     * @param pageSize   Number of records per page
     * @param pageNumber Page number (1-based)
     * @throws IllegalArgumentException if pageSize is negative or exceeds maximum allowed value,
     *                                  or if pageNumber is less than 1
     */
    public PaginationParams(Integer pageSize, Integer pageNumber, boolean usePageNumbering) {
        if (!usePageNumbering) {
            setLimit(pageSize);
            setOffset(pageNumber);
            this.pageSize = this.limit;
            this.pageNumber = (this.offset / this.limit) + 1;
        } else {
            setPageSize(pageSize);
            setPageNumber(pageNumber);
            // Calculate equivalent limit and offset
            this.limit = this.pageSize;
            this.offset = (this.pageNumber - 1) * this.pageSize;
        }
    }

    /**
     * Gets the maximum number of records to return.
     *
     * @return the limit value
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Sets the maximum number of records to return.
     *
     * @param limit Maximum number of records to return
     * @throws IllegalArgumentException if limit is negative or exceeds maximum allowed value
     */
    public void setLimit(Integer limit) {
        if (limit == null) {
            this.limit = DEFAULT_PAGE_SIZE;
        } else if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        } else if (limit > MAX_PAGE_SIZE) {
            this.limit = MAX_PAGE_SIZE;
        } else {
            this.limit = limit;
        }
    }

    /**
     * Gets the number of records to skip.
     *
     * @return the offset value
     */
    public Integer getOffset() {
        return offset;
    }

    /**
     * Sets the number of records to skip.
     *
     * @param offset Number of records to skip
     * @throws IllegalArgumentException if offset is negative
     */
    public void setOffset(Integer offset) {
        if (offset == null) {
            this.offset = DEFAULT_OFFSET;
        } else if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        } else {
            this.offset = offset;
        }
    }

    /**
     * Gets the number of records per page.
     *
     * @return the page size
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the number of records per page.
     *
     * @param pageSize Number of records per page
     * @throws IllegalArgumentException if pageSize is negative or exceeds maximum allowed value
     */
    public void setPageSize(Integer pageSize) {
        if (pageSize == null) {
            this.pageSize = DEFAULT_PAGE_SIZE;
        } else if (pageSize < 0) {
            throw new IllegalArgumentException("Page size cannot be negative");
        } else if (pageSize > MAX_PAGE_SIZE) {
            this.pageSize = MAX_PAGE_SIZE;
        } else {
            this.pageSize = pageSize;
        }
    }

    /**
     * Gets the page number (1-based).
     *
     * @return the page number
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number (1-based).
     *
     * @param pageNumber Page number
     * @throws IllegalArgumentException if pageNumber is less than 1
     */
    public void setPageNumber(Integer pageNumber) {
        if (pageNumber == null) {
            this.pageNumber = DEFAULT_PAGE_NUMBER;
        } else if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be at least 1");
        } else {
            this.pageNumber = pageNumber;
        }
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
     * Sets whether total count calculation is enabled.
     *
     * @param calculateTotalCount true to calculate total count, false otherwise
     */
    public void setCalculateTotalCount(boolean calculateTotalCount) {
        this.calculateTotalCount = calculateTotalCount;
    }

    /**
     * Updates limit and offset based on page size and number.
     * <p>
     * This method should be called after changing page size or page number
     * to ensure that limit and offset values are synchronized.
     * </p>
     */
    public void updateLimitAndOffset() {
        this.limit = this.pageSize;
        this.offset = (this.pageNumber - 1) * this.pageSize;
    }

    /**
     * Updates page size and page number based on limit and offset.
     * <p>
     * This method should be called after changing limit or offset
     * to ensure that page size and page number values are synchronized.
     * </p>
     */
    public void updatePageSizeAndNumber() {
        this.pageSize = this.limit;
        this.pageNumber = (this.offset / this.limit) + 1;
    }

    /**
     * Generates SQL LIMIT clause for the current pagination parameters.
     *
     * @return SQL LIMIT clause string
     */
    public String toLimitClause() {
        return "LIMIT " + limit;
    }

    /**
     * Generates SQL OFFSET clause for the current pagination parameters.
     *
     * @return SQL OFFSET clause string
     */
    public String toOffsetClause() {
        return "OFFSET " + offset;
    }

    /**
     * Generates combined SQL LIMIT and OFFSET clauses for the current pagination parameters.
     *
     * @return Combined SQL LIMIT and OFFSET clause string
     */
    public String toSqlClause() {
        return toLimitClause() + " " + toOffsetClause();
    }

    /**
     * Calculates the total number of pages based on total record count.
     *
     * @param totalCount Total number of records
     * @return Total number of pages
     */
    public int calculateTotalPages(long totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    /**
     * Checks if there is a next page based on current page and total pages.
     *
     * @param totalPages Total number of pages
     * @return true if there is a next page, false otherwise
     */
    public boolean hasNextPage(int totalPages) {
        return pageNumber < totalPages;
    }

    /**
     * Checks if there is a previous page based on current page.
     *
     * @return true if there is a previous page, false otherwise
     */
    public boolean hasPreviousPage() {
        return pageNumber > 1;
    }

    /**
     * Creates a new PaginationParams instance for the next page.
     *
     * @return PaginationParams for the next page
     */
    public PaginationParams nextPage() {
        PaginationParams params = new PaginationParams();
        params.setPageSize(this.pageSize);
        params.setPageNumber(this.pageNumber + 1);
        params.updateLimitAndOffset();
        params.setCalculateTotalCount(this.calculateTotalCount);
        return params;
    }

    /**
     * Creates a new PaginationParams instance for the previous page.
     *
     * @return PaginationParams for the previous page, or current page if already at first page
     */
    public PaginationParams previousPage() {
        if (!hasPreviousPage()) {
            return this;
        }
        
        PaginationParams params = new PaginationParams();
        params.setPageSize(this.pageSize);
        params.setPageNumber(this.pageNumber - 1);
        params.updateLimitAndOffset();
        params.setCalculateTotalCount(this.calculateTotalCount);
        return params;
    }

    /**
     * Creates a new PaginationParams instance for a specific page.
     *
     * @param pageNumber Page number to navigate to
     * @return PaginationParams for the specified page
     * @throws IllegalArgumentException if pageNumber is less than 1
     */
    public PaginationParams goToPage(int pageNumber) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be at least 1");
        }
        
        PaginationParams params = new PaginationParams();
        params.setPageSize(this.pageSize);
        params.setPageNumber(pageNumber);
        params.updateLimitAndOffset();
        params.setCalculateTotalCount(this.calculateTotalCount);
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginationParams that = (PaginationParams) o;
        return calculateTotalCount == that.calculateTotalCount &&
                Objects.equals(limit, that.limit) &&
                Objects.equals(offset, that.offset) &&
                Objects.equals(pageSize, that.pageSize) &&
                Objects.equals(pageNumber, that.pageNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, offset, pageSize, pageNumber, calculateTotalCount);
    }

    @Override
    public String toString() {
        return "PaginationParams{" +
                "limit=" + limit +
                ", offset=" + offset +
                ", pageSize=" + pageSize +
                ", pageNumber=" + pageNumber +
                ", calculateTotalCount=" + calculateTotalCount +
                '}';
    }
}