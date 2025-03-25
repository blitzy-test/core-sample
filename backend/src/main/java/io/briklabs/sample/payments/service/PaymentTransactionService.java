package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface that defines core payment transaction operations including creation,
 * processing, and retrieval methods. This service handles the business logic for payment
 * transaction management, serving as the main entry point for payment processing functionality.
 */
public interface PaymentTransactionService {

    /**
     * Creates a new payment transaction with the provided details.
     *
     * @param organizationId Organization that owns this transaction
     * @param accountId Account associated with this transaction
     * @param amount Transaction amount
     * @param currency ISO 4217 currency code (3 characters)
     * @param merchantId External merchant identifier
     * @param paymentType Payment method type
     * @param paymentData Map containing payment method details
     * @param description Optional description of the transaction
     * @param reference Optional external reference number
     * @return The created payment transaction
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentTransaction createTransaction(UUID organizationId, UUID accountId, 
                                        BigDecimal amount, String currency,
                                        String merchantId, String paymentType,
                                        Map<String, Object> paymentData,
                                        String description, String reference);

    /**
     * Retrieves a payment transaction by its unique identifier.
     *
     * @param transactionId The unique identifier of the transaction
     * @return An Optional containing the transaction if found, or empty if not found
     */
    Optional<PaymentTransaction> getTransactionById(UUID transactionId);

    /**
     * Retrieves all payment transactions for a specific organization.
     *
     * @param organizationId The organization identifier
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions for the organization
     */
    List<PaymentTransaction> getTransactionsByOrganization(UUID organizationId, int offset, int limit);

    /**
     * Retrieves all payment transactions for a specific account.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions for the account
     */
    List<PaymentTransaction> getTransactionsByAccount(UUID organizationId, UUID accountId, int offset, int limit);

    /**
     * Retrieves payment transactions by status.
     *
     * @param status The payment status to filter by
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions with the specified status
     */
    List<PaymentTransaction> getTransactionsByStatus(PaymentStatus status, int offset, int limit);

    /**
     * Processes a payment transaction, transitioning it from PENDING to AUTHORIZED or DECLINED.
     *
     * @param transactionId The unique identifier of the transaction to process
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction cannot be processed
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction processTransaction(UUID transactionId);

    /**
     * Captures an authorized payment transaction, transitioning it to CAPTURED or PARTIALLY_CAPTURED.
     *
     * @param transactionId The unique identifier of the transaction to capture
     * @param amount The amount to capture (can be less than or equal to the authorized amount)
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction cannot be captured
     * @throws IllegalArgumentException if the transaction ID or amount is invalid
     */
    PaymentTransaction captureTransaction(UUID transactionId, BigDecimal amount);

    /**
     * Refunds a captured payment transaction, transitioning it to REFUNDED or PARTIALLY_REFUNDED.
     *
     * @param transactionId The unique identifier of the transaction to refund
     * @param amount The amount to refund (can be less than or equal to the captured amount)
     * @param reason Optional reason for the refund
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction cannot be refunded
     * @throws IllegalArgumentException if the transaction ID or amount is invalid
     */
    PaymentTransaction refundTransaction(UUID transactionId, BigDecimal amount, String reason);

    /**
     * Voids an authorized payment transaction, transitioning it to VOIDED.
     *
     * @param transactionId The unique identifier of the transaction to void
     * @param reason Optional reason for voiding
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction cannot be voided
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction voidTransaction(UUID transactionId, String reason);

    /**
     * Updates the status of a payment transaction.
     *
     * @param transactionId The unique identifier of the transaction
     * @param newStatus The new status to set
     * @return The updated payment transaction
     * @throws IllegalStateException if the status transition is not allowed
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction updateTransactionStatus(UUID transactionId, PaymentStatus newStatus);

    /**
     * Retrieves payment transactions by date range.
     *
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions within the date range
     */
    List<PaymentTransaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, 
                                                      int offset, int limit);

    /**
     * Retrieves payment transactions by amount range.
     *
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @param currency The currency code (optional, can be null to search across all currencies)
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions within the amount range
     */
    List<PaymentTransaction> getTransactionsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, 
                                                        String currency, int offset, int limit);

    /**
     * Retrieves payment transactions by merchant.
     *
     * @param merchantId The merchant identifier
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions for the merchant
     */
    List<PaymentTransaction> getTransactionsByMerchant(String merchantId, int offset, int limit);

    /**
     * Retrieves payment transactions by payment type.
     *
     * @param paymentType The payment type
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions with the specified payment type
     */
    List<PaymentTransaction> getTransactionsByPaymentType(String paymentType, int offset, int limit);

    /**
     * Performs a complex query for payment transactions with multiple filter criteria.
     *
     * @param organizationId The organization identifier (optional, can be null)
     * @param accountId The account identifier (optional, can be null)
     * @param statuses List of payment statuses to include (optional, can be null)
     * @param startDate The start date (optional, can be null)
     * @param endDate The end date (optional, can be null)
     * @param minAmount The minimum amount (optional, can be null)
     * @param maxAmount The maximum amount (optional, can be null)
     * @param currency The currency code (optional, can be null)
     * @param merchantId The merchant identifier (optional, can be null)
     * @param paymentType The payment type (optional, can be null)
     * @param sortBy Field to sort by (created_at, updated_at, amount, status)
     * @param sortDirection Sort direction (asc, desc)
     * @param offset Pagination offset (0-based)
     * @param limit Maximum number of results to return
     * @return List of payment transactions matching the criteria
     */
    List<PaymentTransaction> queryTransactions(UUID organizationId, UUID accountId,
                                             List<PaymentStatus> statuses,
                                             LocalDateTime startDate, LocalDateTime endDate,
                                             BigDecimal minAmount, BigDecimal maxAmount,
                                             String currency, String merchantId, String paymentType,
                                             String sortBy, String sortDirection,
                                             int offset, int limit);

    /**
     * Gets the total count of transactions matching the specified criteria.
     * This is useful for pagination when combined with queryTransactions.
     *
     * @param organizationId The organization identifier (optional, can be null)
     * @param accountId The account identifier (optional, can be null)
     * @param statuses List of payment statuses to include (optional, can be null)
     * @param startDate The start date (optional, can be null)
     * @param endDate The end date (optional, can be null)
     * @param minAmount The minimum amount (optional, can be null)
     * @param maxAmount The maximum amount (optional, can be null)
     * @param currency The currency code (optional, can be null)
     * @param merchantId The merchant identifier (optional, can be null)
     * @param paymentType The payment type (optional, can be null)
     * @return The total count of matching transactions
     */
    long countTransactions(UUID organizationId, UUID accountId,
                          List<PaymentStatus> statuses,
                          LocalDateTime startDate, LocalDateTime endDate,
                          BigDecimal minAmount, BigDecimal maxAmount,
                          String currency, String merchantId, String paymentType);

    /**
     * Validates a payment transaction before processing.
     *
     * @param transactionId The unique identifier of the transaction to validate
     * @return true if the transaction is valid for processing, false otherwise
     */
    boolean validateTransaction(UUID transactionId);

    /**
     * Verifies that a payment transaction exists and is in the expected state.
     *
     * @param transactionId The unique identifier of the transaction to verify
     * @param expectedStatus The expected status of the transaction
     * @return true if the transaction exists and is in the expected state, false otherwise
     */
    boolean verifyTransactionStatus(UUID transactionId, PaymentStatus expectedStatus);
}