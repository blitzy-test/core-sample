package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentType;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service interface that defines core payment transaction operations.
 * This service handles the business logic for payment transaction management,
 * serving as the main entry point for payment processing functionality.
 */
public interface PaymentTransactionService {

    /**
     * Creates a new payment transaction with the specified details.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param amount The transaction amount
     * @param currency The currency code (ISO 4217)
     * @param merchantId The merchant identifier
     * @param paymentType The payment method type
     * @param description Optional description of the transaction
     * @param transactionReference Optional external reference number
     * @return The created payment transaction
     * @throws IllegalArgumentException if any required parameters are invalid
     */
    PaymentTransaction createTransaction(
            UUID organizationId,
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantId,
            PaymentType paymentType,
            String description,
            String transactionReference);

    /**
     * Retrieves a payment transaction by its unique identifier.
     *
     * @param transactionId The transaction identifier
     * @return The payment transaction if found, or null if not found
     */
    PaymentTransaction getTransactionById(UUID transactionId);

    /**
     * Retrieves all payment transactions for a specific organization.
     *
     * @param organizationId The organization identifier
     * @param limit Maximum number of results to return
     * @param offset Starting position for pagination
     * @return List of payment transactions for the organization
     */
    List<PaymentTransaction> getTransactionsByOrganization(
            UUID organizationId,
            int limit,
            int offset);

    /**
     * Retrieves all payment transactions for a specific account.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier
     * @param limit Maximum number of results to return
     * @param offset Starting position for pagination
     * @return List of payment transactions for the account
     */
    List<PaymentTransaction> getTransactionsByAccount(
            UUID organizationId,
            UUID accountId,
            int limit,
            int offset);

    /**
     * Retrieves payment transactions by status.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier (can be null for all accounts)
     * @param status The payment status to filter by
     * @param limit Maximum number of results to return
     * @param offset Starting position for pagination
     * @return List of payment transactions with the specified status
     */
    List<PaymentTransaction> getTransactionsByStatus(
            UUID organizationId,
            UUID accountId,
            PaymentStatus status,
            int limit,
            int offset);

    /**
     * Retrieves payment transactions created within a date range.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier (can be null for all accounts)
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @param limit Maximum number of results to return
     * @param offset Starting position for pagination
     * @return List of payment transactions created within the date range
     */
    List<PaymentTransaction> getTransactionsByDateRange(
            UUID organizationId,
            UUID accountId,
            Instant startDate,
            Instant endDate,
            int limit,
            int offset);

    /**
     * Retrieves payment transactions with amounts in a specific range.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier (can be null for all accounts)
     * @param minAmount The minimum amount (inclusive)
     * @param maxAmount The maximum amount (inclusive)
     * @param currency The currency code (ISO 4217)
     * @param limit Maximum number of results to return
     * @param offset Starting position for pagination
     * @return List of payment transactions with amounts in the specified range
     */
    List<PaymentTransaction> getTransactionsByAmountRange(
            UUID organizationId,
            UUID accountId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String currency,
            int limit,
            int offset);

    /**
     * Retrieves payment transactions for a specific merchant.
     *
     * @param organizationId The organization identifier
     * @param accountId The account identifier (can be null for all accounts)
     * @param merchantId The merchant identifier
     * @param limit Maximum number of results to return
     * @param offset Starting position for pagination
     * @return List of payment transactions for the merchant
     */
    List<PaymentTransaction> getTransactionsByMerchant(
            UUID organizationId,
            UUID accountId,
            String merchantId,
            int limit,
            int offset);

    /**
     * Retrieves payment transactions using complex filtering criteria.
     *
     * @param filterParams The filter parameters for querying transactions
     * @return List of payment transactions matching the filter criteria
     */
    List<PaymentTransaction> queryTransactions(PaymentFilterParams filterParams);

    /**
     * Counts the total number of transactions matching the filter criteria.
     *
     * @param filterParams The filter parameters for querying transactions
     * @return The total count of matching transactions
     */
    long countTransactions(PaymentFilterParams filterParams);

    /**
     * Updates the status of a payment transaction.
     *
     * @param transactionId The transaction identifier
     * @param newStatus The new status to set
     * @return The updated payment transaction
     * @throws IllegalStateException if the status transition is not allowed
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction updateTransactionStatus(UUID transactionId, PaymentStatus newStatus);

    /**
     * Processes a payment transaction, moving it from CREATED to PROCESSING state.
     *
     * @param transactionId The transaction identifier
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction is not in CREATED state
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction processTransaction(UUID transactionId);

    /**
     * Authorizes a payment transaction, moving it from PROCESSING to AUTHORIZED state.
     *
     * @param transactionId The transaction identifier
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction is not in PROCESSING state
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction authorizeTransaction(UUID transactionId);

    /**
     * Captures an authorized payment transaction, moving it from AUTHORIZED to CAPTURED state.
     *
     * @param transactionId The transaction identifier
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction is not in AUTHORIZED state
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction captureTransaction(UUID transactionId);

    /**
     * Performs a partial capture on an authorized payment transaction.
     *
     * @param transactionId The transaction identifier
     * @param amount The amount to capture
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction is not in AUTHORIZED state
     * @throws IllegalArgumentException if the amount is invalid or exceeds the authorized amount
     */
    PaymentTransaction capturePartialTransaction(UUID transactionId, BigDecimal amount);

    /**
     * Refunds a captured payment transaction, moving it from CAPTURED to REFUNDED state.
     *
     * @param transactionId The transaction identifier
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction is not in CAPTURED state
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction refundTransaction(UUID transactionId);

    /**
     * Performs a partial refund on a captured payment transaction.
     *
     * @param transactionId The transaction identifier
     * @param amount The amount to refund
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction is not in CAPTURED state
     * @throws IllegalArgumentException if the amount is invalid or exceeds the captured amount
     */
    PaymentTransaction refundPartialTransaction(UUID transactionId, BigDecimal amount);

    /**
     * Voids an authorized payment transaction, moving it from AUTHORIZED to VOIDED state.
     *
     * @param transactionId The transaction identifier
     * @return The updated payment transaction
     * @throws IllegalStateException if the transaction is not in AUTHORIZED state
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction voidTransaction(UUID transactionId);

    /**
     * Marks a payment transaction as failed, moving it to FAILED state.
     *
     * @param transactionId The transaction identifier
     * @param errorReason The reason for the failure
     * @return The updated payment transaction
     * @throws IllegalArgumentException if the transaction ID is invalid
     */
    PaymentTransaction failTransaction(UUID transactionId, String errorReason);

    /**
     * Validates a payment transaction for processing.
     *
     * @param transaction The transaction to validate
     * @throws IllegalArgumentException if the transaction is invalid
     */
    void validateTransaction(PaymentTransaction transaction);

    /**
     * Checks if a transaction can be processed based on its current state.
     *
     * @param transactionId The transaction identifier
     * @return true if the transaction can be processed, false otherwise
     */
    boolean canProcessTransaction(UUID transactionId);

    /**
     * Checks if a transaction can be captured based on its current state.
     *
     * @param transactionId The transaction identifier
     * @return true if the transaction can be captured, false otherwise
     */
    boolean canCaptureTransaction(UUID transactionId);

    /**
     * Checks if a transaction can be refunded based on its current state.
     *
     * @param transactionId The transaction identifier
     * @return true if the transaction can be refunded, false otherwise
     */
    boolean canRefundTransaction(UUID transactionId);

    /**
     * Checks if a transaction can be voided based on its current state.
     *
     * @param transactionId The transaction identifier
     * @return true if the transaction can be voided, false otherwise
     */
    boolean canVoidTransaction(UUID transactionId);
}