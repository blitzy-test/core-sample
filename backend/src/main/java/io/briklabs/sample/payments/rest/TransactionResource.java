package io.briklabs.sample.payments.rest;

import io.briklabs.sample.payments.data.query.AmountRangeFilter;
import io.briklabs.sample.payments.data.query.DateRangeFilter;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaginationParams;
import io.briklabs.sample.payments.data.query.SortCriteria;
import io.briklabs.sample.payments.data.query.StatusFilter;
import io.briklabs.sample.payments.model.PaymentData;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentType;
import io.briklabs.sample.payments.service.PaymentTransactionService;
import io.briklabs.sample.payments.service.PaymentQueryService;
import io.briklabs.sample.payments.service.PaymentValidationService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST resource for managing payment transactions.
 * <p>
 * This resource handles HTTP requests for creating, retrieving, and listing payment transactions
 * following the URI pattern '/organizations/{org_id}/accounts/{account_id}/transactions/'.
 * It supports comprehensive filtering, sorting, and pagination capabilities for transaction queries.
 * </p>
 */
@Path("/organizations/{org_id}/accounts/{account_id}/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    private final PaymentTransactionService transactionService;
    private final PaymentQueryService queryService;
    private final PaymentValidationService validationService;

    /**
     * Constructor with dependency injection.
     *
     * @param transactionService Service for transaction operations
     * @param queryService Service for complex transaction queries
     * @param validationService Service for validating payment data
     */
    @Inject
    public TransactionResource(
            PaymentTransactionService transactionService,
            PaymentQueryService queryService,
            PaymentValidationService validationService) {
        this.transactionService = transactionService;
        this.queryService = queryService;
        this.validationService = validationService;
    }

    /**
     * Creates a new payment transaction.
     *
     * @param orgId Organization identifier
     * @param accountId Account identifier
     * @param transaction Transaction data from request body
     * @param uriInfo URI information for response
     * @return Response with created transaction
     */
    @POST
    public Response createTransaction(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            PaymentTransaction transaction,
            @Context UriInfo uriInfo) {
        
        // Validate input parameters
        if (transaction == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Transaction data is required"))
                    .build();
        }
        
        // Validate organization and account IDs
        UUID organizationId;
        UUID accountUuid;
        try {
            organizationId = UUID.fromString(orgId);
            accountUuid = "_all".equals(accountId) ? null : UUID.fromString(accountId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid organization or account ID format"))
                    .build();
        }
        
        // Set organization and account IDs in the transaction
        transaction.setOrganizationId(organizationId);
        if (accountUuid != null) {
            transaction.setAccountId(accountUuid);
        }
        
        // Validate transaction data
        List<String> validationErrors = validationService.validateTransaction(transaction);
        if (!validationErrors.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Validation failed");
            errorResponse.put("errors", validationErrors);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
        }
        
        // Create the transaction
        try {
            PaymentTransaction createdTransaction = transactionService.createTransaction(transaction);
            
            // Build the location URI for the created resource
            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(createdTransaction.getTransactionId().toString())
                    .build();
            
            // Return 201 Created with the transaction data and location header
            return Response.created(location)
                    .entity(createdTransaction)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to create transaction: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Retrieves a specific payment transaction by ID.
     *
     * @param orgId Organization identifier
     * @param accountId Account identifier
     * @param transactionId Transaction identifier
     * @return Response with transaction data
     */
    @GET
    @Path("/{transaction_id}")
    public Response getTransaction(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @PathParam("transaction_id") String transactionId) {
        
        // Validate input parameters
        UUID organizationId;
        UUID accountUuid;
        UUID transactionUuid;
        
        try {
            organizationId = UUID.fromString(orgId);
            accountUuid = "_all".equals(accountId) ? null : UUID.fromString(accountId);
            transactionUuid = UUID.fromString(transactionId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid ID format"))
                    .build();
        }
        
        // Retrieve the transaction
        try {
            Optional<PaymentTransaction> transaction = transactionService.getTransactionById(
                    transactionUuid, organizationId, accountUuid);
            
            if (transaction.isPresent()) {
                return Response.ok(transaction.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transaction not found"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve transaction: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Lists payment transactions with filtering, sorting, and pagination.
     *
     * @param orgId Organization identifier
     * @param accountId Account identifier
     * @param status Status filter (comma-separated list)
     * @param startDate Start date for filtering (ISO format)
     * @param endDate End date for filtering (ISO format)
     * @param minAmount Minimum amount for filtering
     * @param maxAmount Maximum amount for filtering
     * @param currency Currency code for amount filtering
     * @param merchantId Merchant identifier for filtering
     * @param paymentType Payment type for filtering
     * @param sortBy Field to sort by
     * @param sortOrder Sort direction (asc/desc)
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @return Response with paginated transaction list
     */
    @GET
    public Response listTransactions(
            @PathParam("org_id") String orgId,
            @PathParam("account_id") String accountId,
            @QueryParam("status") String status,
            @QueryParam("start_date") String startDate,
            @QueryParam("end_date") String endDate,
            @QueryParam("min_amount") BigDecimal minAmount,
            @QueryParam("max_amount") BigDecimal maxAmount,
            @QueryParam("currency") String currency,
            @QueryParam("merchant_id") String merchantId,
            @QueryParam("payment_type") String paymentType,
            @QueryParam("sort_by") @DefaultValue("created_at") String sortBy,
            @QueryParam("sort_order") @DefaultValue("desc") String sortOrder,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        
        // Validate organization and account IDs
        UUID organizationId;
        UUID accountUuid = null;
        
        try {
            organizationId = UUID.fromString(orgId);
            if (accountId != null && !"_all".equals(accountId)) {
                accountUuid = UUID.fromString(accountId);
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid organization or account ID format"))
                    .build();
        }
        
        // Build filter parameters
        PaymentFilterParams filterParams = new PaymentFilterParams();
        filterParams.setOrganizationId(organizationId);
        filterParams.setAccountId(accountUuid);
        
        // Set status filter if provided
        if (status != null && !status.isEmpty()) {
            StatusFilter statusFilter = new StatusFilter();
            List<PaymentStatus> statusList = Arrays.stream(status.split(","))
                    .map(String::trim)
                    .map(s -> {
                        try {
                            return PaymentStatus.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            statusFilter.setStatusList(statusList);
            filterParams.setStatusFilter(statusFilter);
        }
        
        // Set date range filter if provided
        if (startDate != null || endDate != null) {
            DateRangeFilter dateFilter = new DateRangeFilter();
            
            if (startDate != null) {
                try {
                    LocalDateTime startDateTime = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE)
                            .atStartOfDay();
                    dateFilter.setStartDate(startDateTime);
                } catch (DateTimeParseException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Invalid start_date format. Use ISO date format (YYYY-MM-DD)"))
                            .build();
                }
            }
            
            if (endDate != null) {
                try {
                    LocalDateTime endDateTime = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE)
                            .plusDays(1)
                            .atStartOfDay();
                    dateFilter.setEndDate(endDateTime);
                } catch (DateTimeParseException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Invalid end_date format. Use ISO date format (YYYY-MM-DD)"))
                            .build();
                }
            }
            
            filterParams.setDateFilter(dateFilter);
        }
        
        // Set amount range filter if provided
        if (minAmount != null || maxAmount != null) {
            AmountRangeFilter amountFilter = new AmountRangeFilter();
            amountFilter.setMinAmount(minAmount);
            amountFilter.setMaxAmount(maxAmount);
            
            if (currency != null && currency.length() == 3) {
                amountFilter.setCurrency(currency.toUpperCase());
            }
            
            filterParams.setAmountFilter(amountFilter);
        }
        
        // Set merchant filter if provided
        if (merchantId != null && !merchantId.isEmpty()) {
            filterParams.setMerchantId(merchantId);
        }
        
        // Set payment type filter if provided
        if (paymentType != null && !paymentType.isEmpty()) {
            try {
                PaymentType type = PaymentType.valueOf(paymentType.toUpperCase());
                filterParams.setPaymentType(type);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("Invalid payment_type. Valid values: " + 
                                Arrays.toString(PaymentType.values())))
                        .build();
            }
        }
        
        // Set pagination parameters
        PaginationParams paginationParams = new PaginationParams();
        paginationParams.setOffset(offset);
        paginationParams.setLimit(Math.min(limit, 100)); // Cap at 100 for performance
        
        // Set sorting parameters
        SortCriteria sortCriteria = new SortCriteria();
        sortCriteria.setField(sortBy);
        sortCriteria.setDirection("asc".equalsIgnoreCase(sortOrder) ? 
                SortCriteria.SortDirection.ASC : SortCriteria.SortDirection.DESC);
        
        // Execute the query
        try {
            Map<String, Object> result = queryService.findTransactions(
                    filterParams, sortCriteria, paginationParams);
            
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to retrieve transactions: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Helper method to create standardized error responses.
     *
     * @param message Error message
     * @return Map containing error details
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        return errorResponse;
    }
}