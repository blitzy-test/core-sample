package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface for validating payment operations and data.
 * This service provides methods for ensuring data integrity and business rule compliance
 * throughout the payment lifecycle.
 */
public interface PaymentValidationService {

    /**
     * Validates a new payment transaction data before creation.
     * Ensures all required fields are present and valid, including amount, currency,
     * and merchant information.
     *
     * @param transaction The transaction to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateTransactionData(PaymentTransaction transaction);

    /**
     * Validates that a transaction amount is valid.
     * Amount must be greater than zero and properly formatted.
     *
     * @param amount The amount to validate
     * @param currency The currency code (ISO 4217)
     * @throws IllegalArgumentException if the amount is invalid
     */
    void validateAmount(BigDecimal amount, String currency);

    /**
     * Validates that a currency code is valid according to ISO 4217 standard.
     *
     * @param currency The currency code to validate
     * @throws IllegalArgumentException if the currency code is invalid
     */
    void validateCurrency(String currency);

    /**
     * Validates that a state transition is allowed for a payment transaction.
     * Ensures the transition follows the defined payment lifecycle rules.
     *
     * @param currentStatus The current status of the transaction
     * @param newStatus The target status for the transition
     * @throws IllegalStateException if the transition is not allowed
     */
    void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus);

    /**
     * Validates that a transaction can be captured.
     * Transaction must be in AUTHORIZED status to be captured.
     *
     * @param transaction The transaction to validate
     * @throws IllegalStateException if the transaction cannot be captured
     */
    void validateCanCapture(PaymentTransaction transaction);

    /**
     * Validates that a transaction can be refunded.
     * Transaction must be in CAPTURED status to be refunded.
     *
     * @param transaction The transaction to validate
     * @throws IllegalStateException if the transaction cannot be refunded
     */
    void validateCanRefund(PaymentTransaction transaction);

    /**
     * Validates that a transaction can be voided.
     * Transaction must be in AUTHORIZED status to be voided.
     *
     * @param transaction The transaction to validate
     * @throws IllegalStateException if the transaction cannot be voided
     */
    void validateCanVoid(PaymentTransaction transaction);

    /**
     * Validates that a capture amount is valid for a transaction.
     * Capture amount must be greater than zero and less than or equal to the original transaction amount.
     *
     * @param transaction The transaction being captured
     * @param captureAmount The amount to capture
     * @throws IllegalArgumentException if the capture amount is invalid
     */
    void validateCaptureAmount(PaymentTransaction transaction, BigDecimal captureAmount);

    /**
     * Validates that a refund amount is valid for a transaction.
     * Refund amount must be greater than zero and less than or equal to the captured amount.
     *
     * @param transaction The transaction being refunded
     * @param refundAmount The amount to refund
     * @throws IllegalArgumentException if the refund amount is invalid
     */
    void validateRefundAmount(PaymentTransaction transaction, BigDecimal refundAmount);

    /**
     * Validates that the organization ID is valid and the user has access to it.
     *
     * @param organizationId The organization ID to validate
     * @throws IllegalArgumentException if the organization ID is invalid
     * @throws SecurityException if the user does not have access to the organization
     */
    void validateOrganizationAccess(UUID organizationId);

    /**
     * Validates that the account ID is valid and belongs to the specified organization.
     *
     * @param organizationId The organization ID
     * @param accountId The account ID to validate
     * @throws IllegalArgumentException if the account ID is invalid or does not belong to the organization
     */
    void validateAccountAccess(UUID organizationId, UUID accountId);

    /**
     * Validates that the merchant ID is valid and associated with the specified organization.
     *
     * @param organizationId The organization ID
     * @param merchantId The merchant ID to validate
     * @throws IllegalArgumentException if the merchant ID is invalid or not associated with the organization
     */
    void validateMerchantId(UUID organizationId, String merchantId);
}