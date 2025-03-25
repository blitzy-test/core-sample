package io.briklabs.sample.payments.data.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.dao.PaymentDataDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.PaymentDataException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.SecurityException;
import io.briklabs.sample.payments.data.exception.TransactionException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.model.PaymentData;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.data.query.PaymentQueryBuilder;
import io.briklabs.sample.payments.data.security.PaymentEncryptionService;

/**
 * Concrete implementation of the PaymentDataDAO interface that handles database operations
 * for payment method details.
 * <p>
 * This class manages secure storage and retrieval of payment instrument information,
 * handling tokenization, encryption, and field-level security. It provides methods to
 * retrieve payment data by transaction ID and implements specialized queries for payment
 * method information.
 * </p>
 * <p>
 * The implementation ensures proper handling of sensitive payment data with appropriate
 * security controls, following PCI DSS requirements where applicable.
 * </p>
 */
public class PaymentDataDaoImpl extends AbstractPaymentDaoImpl<PaymentData, UUID> implements PaymentDataDAO {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDataDaoImpl.class);
    
    private static final String TABLE_NAME = "payment_data";
    private static final String ID_COLUMN = "payment_data_id";
    private static final String TRANSACTION_ID_COLUMN = "transaction_id";
    private static final String PAYMENT_METHOD_ID_COLUMN = "payment_method_id";
    private static final String PAYMENT_TOKEN_COLUMN = "payment_token";
    private static final String PAYMENT_DETAILS_COLUMN = "payment_details";
    private static final String CREATED_AT_COLUMN = "created_at";
    private static final String EXPIRATION_COLUMN = "expiration";
    private static final String BILLING_DATA_COLUMN = "billing_data";
    
    private final PaymentEncryptionService encryptionService;

    /**
     * Creates a new PaymentDataDaoImpl with the specified database configuration.
     *
     * @param databaseConfig the database configuration
     * @param configSource the configuration source
     * @param encryptionService the encryption service for sensitive data
     */
    public PaymentDataDaoImpl(DatabaseConfig databaseConfig, ConfigSource configSource, 
                             PaymentEncryptionService encryptionService) {
        super(databaseConfig, configSource, TABLE_NAME);
        this.encryptionService = encryptionService;
    }

    /**
     * Validates a payment data entity before database operations.
     *
     * @param paymentData the payment data to validate
     * @throws ValidationException if the payment data fails validation
     */
    @Override
    protected void validateEntity(PaymentData paymentData) throws ValidationException {
        if (paymentData == null) {
            throw new ValidationException("Payment data cannot be null");
        }
        
        if (paymentData.getTransactionId() == null) {
            throw new ValidationException("Transaction ID is required");
        }
        
        if (paymentData.getPaymentMethodId() == null || paymentData.getPaymentMethodId().trim().isEmpty()) {
            throw new ValidationException("Payment method ID is required");
        }
        
        // Additional validation for payment details
        if (paymentData.getPaymentDetails() != null) {
            // Validate payment details structure based on payment type
            validatePaymentDetails(paymentData);
        }
    }

    /**
     * Validates payment details based on payment type.
     *
     * @param paymentData the payment data to validate
     * @throws ValidationException if the payment details fail validation
     */
    private void validatePaymentDetails(PaymentData paymentData) throws ValidationException {
        String paymentDetails = paymentData.getPaymentDetails();
        
        if (paymentDetails == null || paymentDetails.trim().isEmpty()) {
            return; // Empty details are allowed in some cases
        }
        
        try {
            // Validate JSON structure
            if (!isValidJson(paymentDetails)) {
                throw new ValidationException("Payment details must be valid JSON");
            }
            
            // Additional validation based on payment type could be implemented here
            
        } catch (Exception e) {
            throw new ValidationException("Invalid payment details format: " + e.getMessage());
        }
    }

    /**
     * Checks if a string is valid JSON.
     *
     * @param json the string to check
     * @return true if the string is valid JSON, false otherwise
     */
    private boolean isValidJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Maps a ResultSet row to a PaymentData entity.
     *
     * @param rs the result set
     * @return the mapped PaymentData entity
     * @throws SQLException if a database access error occurs
     */
    @Override
    protected PaymentData mapRow(ResultSet rs) throws SQLException {
        PaymentData paymentData = new PaymentData();
        
        paymentData.setPaymentDataId(UUID.fromString(rs.getString(ID_COLUMN)));
        paymentData.setTransactionId(UUID.fromString(rs.getString(TRANSACTION_ID_COLUMN)));
        paymentData.setPaymentMethodId(rs.getString(PAYMENT_METHOD_ID_COLUMN));
        paymentData.setPaymentToken(rs.getString(PAYMENT_TOKEN_COLUMN));
        paymentData.setPaymentDetails(rs.getString(PAYMENT_DETAILS_COLUMN));
        
        Timestamp createdAt = rs.getTimestamp(CREATED_AT_COLUMN);
        if (createdAt != null) {
            paymentData.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        java.sql.Date expiration = rs.getDate(EXPIRATION_COLUMN);
        if (expiration != null) {
            paymentData.setExpiration(expiration.toLocalDate());
        }
        
        paymentData.setBillingData(rs.getString(BILLING_DATA_COLUMN));
        
        return paymentData;
    }

    /**
     * Maps a ResultSet row to a PaymentData entity with sensitive data.
     * This method should only be used by authorized services.
     *
     * @param rs the result set
     * @return the mapped PaymentData entity with sensitive data
     * @throws SQLException if a database access error occurs
     */
    private PaymentData mapRowWithSensitiveData(ResultSet rs) throws SQLException {
        PaymentData paymentData = mapRow(rs);
        
        // Decrypt sensitive data if it's encrypted
        if (paymentData.getPaymentToken() != null) {
            try {
                String decryptedToken = encryptionService.decryptPaymentToken(paymentData.getPaymentToken());
                paymentData.setDecryptedToken(decryptedToken);
            } catch (SecurityException e) {
                logger.warn("Failed to decrypt payment token: {}", e.getMessage());
                // Continue without decrypted data
            }
        }
        
        return paymentData;
    }

    /**
     * Maps a ResultSet row to a PaymentData entity with role-based masking.
     *
     * @param rs the result set
     * @param userRole the role of the requesting user
     * @return the mapped PaymentData entity with appropriate masking
     * @throws SQLException if a database access error occurs
     */
    private PaymentData mapRowWithMasking(ResultSet rs, String userRole) throws SQLException {
        PaymentData paymentData = mapRow(rs);
        
        // Apply role-based masking
        if (paymentData.getPaymentToken() != null) {
            paymentData.setPaymentToken(maskPaymentToken(paymentData.getPaymentToken(), userRole));
        }
        
        if (paymentData.getPaymentDetails() != null) {
            paymentData.setPaymentDetails(maskPaymentDetails(paymentData.getPaymentDetails(), userRole));
        }
        
        if (paymentData.getBillingData() != null) {
            paymentData.setBillingData(maskBillingData(paymentData.getBillingData(), userRole));
        }
        
        return paymentData;
    }

    /**
     * Masks a payment token based on user role.
     *
     * @param token the payment token
     * @param userRole the role of the requesting user
     * @return the masked payment token
     */
    private String maskPaymentToken(String token, String userRole) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        
        // Apply different masking levels based on role
        if ("ADMIN".equalsIgnoreCase(userRole) || "PAYMENT_ADMIN".equalsIgnoreCase(userRole)) {
            // Minimal masking for admins
            return token;
        } else if ("FINANCE".equalsIgnoreCase(userRole) || "FINANCE_MANAGER".equalsIgnoreCase(userRole)) {
            // Partial masking for finance roles
            return maskString(token, 4, 4);
        } else {
            // Full masking for other roles
            return "**********";
        }
    }

    /**
     * Masks payment details JSON based on user role.
     *
     * @param details the payment details JSON
     * @param userRole the role of the requesting user
     * @return the masked payment details
     */
    private String maskPaymentDetails(String details, String userRole) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(details);
            com.fasterxml.jackson.databind.node.ObjectNode maskedNode = rootNode.deepCopy();
            
            // Apply different masking levels based on role
            if ("ADMIN".equalsIgnoreCase(userRole) || "PAYMENT_ADMIN".equalsIgnoreCase(userRole)) {
                // Minimal masking for admins - mask only CVV/security code
                if (maskedNode.has("securityCode")) {
                    maskedNode.put("securityCode", "***");
                }
            } else if ("FINANCE".equalsIgnoreCase(userRole) || "FINANCE_MANAGER".equalsIgnoreCase(userRole)) {
                // Moderate masking for finance roles
                if (maskedNode.has("cardNumber")) {
                    String cardNumber = maskedNode.get("cardNumber").asText();
                    maskedNode.put("cardNumber", maskCardNumber(cardNumber));
                }
                if (maskedNode.has("securityCode")) {
                    maskedNode.put("securityCode", "***");
                }
            } else {
                // Full masking for other roles
                if (maskedNode.has("cardNumber")) {
                    maskedNode.put("cardNumber", "************1234");
                }
                if (maskedNode.has("securityCode")) {
                    maskedNode.put("securityCode", "***");
                }
                if (maskedNode.has("accountNumber")) {
                    maskedNode.put("accountNumber", "******1234");
                }
            }
            
            return mapper.writeValueAsString(maskedNode);
        } catch (Exception e) {
            logger.warn("Failed to mask payment details: {}", e.getMessage());
            return details;
        }
    }

    /**
     * Masks billing data JSON based on user role.
     *
     * @param billingData the billing data JSON
     * @param userRole the role of the requesting user
     * @return the masked billing data
     */
    private String maskBillingData(String billingData, String userRole) {
        if (billingData == null || billingData.isEmpty()) {
            return billingData;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(billingData);
            com.fasterxml.jackson.databind.node.ObjectNode maskedNode = rootNode.deepCopy();
            
            // Apply different masking levels based on role
            if ("ADMIN".equalsIgnoreCase(userRole) || "PAYMENT_ADMIN".equalsIgnoreCase(userRole)) {
                // No masking for admins
                return billingData;
            } else {
                // Mask personal information for other roles
                if (maskedNode.has("address")) {
                    com.fasterxml.jackson.databind.JsonNode addressNode = maskedNode.get("address");
                    if (addressNode.isObject()) {
                        com.fasterxml.jackson.databind.node.ObjectNode address = (com.fasterxml.jackson.databind.node.ObjectNode) addressNode;
                        if (address.has("line1")) {
                            String line1 = address.get("line1").asText();
                            address.put("line1", maskAddress(line1));
                        }
                    }
                }
                
                if (maskedNode.has("email")) {
                    String email = maskedNode.get("email").asText();
                    maskedNode.put("email", maskEmail(email));
                }
                
                if (maskedNode.has("phone")) {
                    String phone = maskedNode.get("phone").asText();
                    maskedNode.put("phone", maskPhone(phone));
                }
            }
            
            return mapper.writeValueAsString(maskedNode);
        } catch (Exception e) {
            logger.warn("Failed to mask billing data: {}", e.getMessage());
            return billingData;
        }
    }

    /**
     * Masks a card number, showing only the last 4 digits.
     *
     * @param cardNumber the card number
     * @return the masked card number
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        int length = cardNumber.length();
        return "************" + cardNumber.substring(length - 4);
    }

    /**
     * Masks an address, showing only the house number and zip code.
     *
     * @param address the address
     * @return the masked address
     */
    private String maskAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        
        // Extract house number (assuming it's at the beginning)
        String[] parts = address.split(" ", 2);
        if (parts.length > 1) {
            return parts[0] + " ********";
        } else {
            return "********";
        }
    }

    /**
     * Masks an email address, showing only the first character and domain.
     *
     * @param email the email address
     * @return the masked email address
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            String username = email.substring(0, atIndex);
            String domain = email.substring(atIndex);
            
            if (username.length() > 1) {
                return username.charAt(0) + "******" + domain;
            } else {
                return username + "******" + domain;
            }
        }
        
        return email;
    }

    /**
     * Masks a phone number, showing only the last 4 digits.
     *
     * @param phone the phone number
     * @return the masked phone number
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        
        int length = phone.length();
        return "******" + phone.substring(length - 4);
    }

    /**
     * Masks a string, showing only the specified number of characters at the beginning and end.
     *
     * @param str the string to mask
     * @param prefixLength the number of characters to show at the beginning
     * @param suffixLength the number of characters to show at the end
     * @return the masked string
     */
    private String maskString(String str, int prefixLength, int suffixLength) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        int length = str.length();
        
        if (length <= prefixLength + suffixLength) {
            return str;
        }
        
        String prefix = str.substring(0, prefixLength);
        String suffix = str.substring(length - suffixLength);
        
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < length - prefixLength - suffixLength; i++) {
            masked.append('*');
        }
        masked.append(suffix);
        
        return masked.toString();
    }

    /**
     * Builds a query for finding a payment data entity by ID.
     *
     * @param id the payment data ID
     * @return the SQL query
     */
    @Override
    protected String buildFindByIdQuery(UUID id) {
        return "SELECT * FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + " = ?";
    }

    /**
     * Builds a filter query for payment data.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    @Override
    protected PaymentQueryBuilder buildFilterQuery(PaymentFilterParams filterParams) {
        PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder();
        
        queryBuilder.select("*")
                   .from(TABLE_NAME);
        
        applyFilterParams(queryBuilder, filterParams);
        
        // Apply sorting
        if (filterParams.getSortBy() != null && !filterParams.getSortBy().isEmpty()) {
            String sortDirection = filterParams.getSortDirection() != null && 
                                  filterParams.getSortDirection().equalsIgnoreCase("DESC") ? "DESC" : "ASC";
            queryBuilder.orderBy(filterParams.getSortBy() + " " + sortDirection);
        } else {
            queryBuilder.orderBy(CREATED_AT_COLUMN + " DESC");
        }
        
        // Apply pagination
        if (filterParams.getLimit() > 0) {
            queryBuilder.limit(filterParams.getLimit());
            
            if (filterParams.getOffset() > 0) {
                queryBuilder.offset(filterParams.getOffset());
            }
        }
        
        return queryBuilder;
    }

    /**
     * Builds a count query for payment data.
     *
     * @param filterParams the filter parameters
     * @return the query builder
     */
    @Override
    protected PaymentQueryBuilder buildCountQuery(PaymentFilterParams filterParams) {
        PaymentQueryBuilder queryBuilder = new PaymentQueryBuilder();
        
        queryBuilder.select("COUNT(*)")
                   .from(TABLE_NAME);
        
        applyFilterParams(queryBuilder, filterParams);
        
        return queryBuilder;
    }

    /**
     * Applies filter parameters to a query builder.
     *
     * @param queryBuilder the query builder
     * @param filterParams the filter parameters
     */
    private void applyFilterParams(PaymentQueryBuilder queryBuilder, PaymentFilterParams filterParams) {
        boolean whereAdded = false;
        
        // Filter by transaction ID
        if (filterParams.getTransactionId() != null) {
            queryBuilder.where(TRANSACTION_ID_COLUMN + " = ?", filterParams.getTransactionId());
            whereAdded = true;
        }
        
        // Filter by payment method ID
        if (filterParams.getPaymentMethodId() != null && !filterParams.getPaymentMethodId().isEmpty()) {
            if (whereAdded) {
                queryBuilder.and(PAYMENT_METHOD_ID_COLUMN + " = ?", filterParams.getPaymentMethodId());
            } else {
                queryBuilder.where(PAYMENT_METHOD_ID_COLUMN + " = ?", filterParams.getPaymentMethodId());
                whereAdded = true;
            }
        }
        
        // Filter by payment type (from payment details)
        if (filterParams.getPaymentType() != null && !filterParams.getPaymentType().isEmpty()) {
            if (whereAdded) {
                queryBuilder.and(PAYMENT_DETAILS_COLUMN + " @> ?::jsonb", 
                        "{\"paymentType\": \"" + filterParams.getPaymentType() + "\"}");
            } else {
                queryBuilder.where(PAYMENT_DETAILS_COLUMN + " @> ?::jsonb", 
                        "{\"paymentType\": \"" + filterParams.getPaymentType() + "\"}");
                whereAdded = true;
            }
        }
        
        // Filter by expiration date range
        if (filterParams.getExpirationStart() != null) {
            if (whereAdded) {
                queryBuilder.and(EXPIRATION_COLUMN + " >= ?", filterParams.getExpirationStart());
            } else {
                queryBuilder.where(EXPIRATION_COLUMN + " >= ?", filterParams.getExpirationStart());
                whereAdded = true;
            }
        }
        
        if (filterParams.getExpirationEnd() != null) {
            if (whereAdded) {
                queryBuilder.and(EXPIRATION_COLUMN + " <= ?", filterParams.getExpirationEnd());
            } else {
                queryBuilder.where(EXPIRATION_COLUMN + " <= ?", filterParams.getExpirationEnd());
                whereAdded = true;
            }
        }
        
        // Filter by creation date range
        if (filterParams.getCreatedStart() != null) {
            if (whereAdded) {
                queryBuilder.and(CREATED_AT_COLUMN + " >= ?", filterParams.getCreatedStart());
            } else {
                queryBuilder.where(CREATED_AT_COLUMN + " >= ?", filterParams.getCreatedStart());
                whereAdded = true;
            }
        }
        
        if (filterParams.getCreatedEnd() != null) {
            if (whereAdded) {
                queryBuilder.and(CREATED_AT_COLUMN + " <= ?", filterParams.getCreatedEnd());
            } else {
                queryBuilder.where(CREATED_AT_COLUMN + " <= ?", filterParams.getCreatedEnd());
                whereAdded = true;
            }
        }
    }

    /**
     * Builds an insert query for a payment data entity.
     *
     * @param paymentData the payment data to insert
     * @return the SQL query
     */
    @Override
    protected String buildInsertQuery(PaymentData paymentData) {
        return "INSERT INTO " + TABLE_NAME + " (" +
                ID_COLUMN + ", " +
                TRANSACTION_ID_COLUMN + ", " +
                PAYMENT_METHOD_ID_COLUMN + ", " +
                PAYMENT_TOKEN_COLUMN + ", " +
                PAYMENT_DETAILS_COLUMN + ", " +
                CREATED_AT_COLUMN + ", " +
                EXPIRATION_COLUMN + ", " +
                BILLING_DATA_COLUMN +
                ") VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?::jsonb)";
    }

    /**
     * Builds an update query for a payment data entity.
     *
     * @param paymentData the payment data to update
     * @return the SQL query
     */
    @Override
    protected String buildUpdateQuery(PaymentData paymentData) {
        return "UPDATE " + TABLE_NAME + " SET " +
                PAYMENT_METHOD_ID_COLUMN + " = ?, " +
                PAYMENT_TOKEN_COLUMN + " = ?, " +
                PAYMENT_DETAILS_COLUMN + " = ?::jsonb, " +
                EXPIRATION_COLUMN + " = ?, " +
                BILLING_DATA_COLUMN + " = ?::jsonb " +
                "WHERE " + ID_COLUMN + " = ?";
    }

    /**
     * Builds a delete query for a payment data entity.
     *
     * @param id the payment data ID
     * @return the SQL query
     */
    @Override
    protected String buildDeleteQuery(UUID id) {
        return "DELETE FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + " = ?";
    }

    /**
     * Gets the parameters for an insert query.
     *
     * @param paymentData the payment data to insert
     * @return the query parameters
     */
    @Override
    protected Object[] getInsertParameters(PaymentData paymentData) {
        return new Object[] {
                paymentData.getPaymentDataId() != null ? paymentData.getPaymentDataId() : UUID.randomUUID(),
                paymentData.getTransactionId(),
                paymentData.getPaymentMethodId(),
                paymentData.getPaymentToken(),
                paymentData.getPaymentDetails(),
                paymentData.getCreatedAt() != null ? paymentData.getCreatedAt() : LocalDateTime.now(),
                paymentData.getExpiration(),
                paymentData.getBillingData()
        };
    }

    /**
     * Gets the parameters for an update query.
     *
     * @param paymentData the payment data to update
     * @return the query parameters
     */
    @Override
    protected Object[] getUpdateParameters(PaymentData paymentData) {
        return new Object[] {
                paymentData.getPaymentMethodId(),
                paymentData.getPaymentToken(),
                paymentData.getPaymentDetails(),
                paymentData.getExpiration(),
                paymentData.getBillingData(),
                paymentData.getPaymentDataId()
        };
    }

    /**
     * Gets the parameters for a delete query.
     *
     * @param id the payment data ID
     * @return the query parameters
     */
    @Override
    protected Object[] getDeleteParameters(UUID id) {
        return new Object[] { id };
    }

    /**
     * Retrieves all payment data associated with a specific transaction.
     *
     * @param transactionId the unique identifier of the transaction
     * @return a list of payment data records associated with the transaction
     * @throws ResourceNotFoundException if the transaction does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentData> findByTransactionId(UUID transactionId) 
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + TRANSACTION_ID_COLUMN + " = ?";
        
        List<PaymentData> results = executeQuery(sql, this::mapRows, transactionId);
        
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("No payment data found for transaction ID: " + transactionId);
        }
        
        return results;
    }

    /**
     * Retrieves payment data by payment method identifier.
     *
     * @param paymentMethodId the unique identifier of the payment method
     * @return an Optional containing the payment data if found, or empty if not found
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public Optional<PaymentData> findByPaymentMethodId(String paymentMethodId)
            throws ConnectionException, QueryExecutionException {
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            throw new ValidationException("Payment method ID cannot be null or empty");
        }
        
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + PAYMENT_METHOD_ID_COLUMN + " = ?";
        
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            } else {
                return Optional.empty();
            }
        }, paymentMethodId);
    }

    /**
     * Retrieves payment data by transaction ID and payment method type.
     *
     * @param transactionId the unique identifier of the transaction
     * @param paymentType the type of payment method
     * @return a list of payment data records matching the criteria
     * @throws ResourceNotFoundException if the transaction does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentData> findByTransactionIdAndPaymentType(UUID transactionId, String paymentType)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        if (paymentType == null || paymentType.trim().isEmpty()) {
            throw new ValidationException("Payment type cannot be null or empty");
        }
        
        String sql = "SELECT * FROM " + TABLE_NAME + 
                     " WHERE " + TRANSACTION_ID_COLUMN + " = ? AND " +
                     PAYMENT_DETAILS_COLUMN + " @> ?::jsonb";
        
        String paymentTypeJson = "{\"paymentType\": \"" + paymentType + "\"}";
        
        List<PaymentData> results = executeQuery(sql, this::mapRows, transactionId, paymentTypeJson);
        
        if (results.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No payment data found for transaction ID: " + transactionId + 
                    " and payment type: " + paymentType);
        }
        
        return results;
    }

    /**
     * Stores a new payment method with secure handling of sensitive data.
     *
     * @param paymentData the payment data to store
     * @return the stored payment data with generated IDs and tokenized information
     * @throws ValidationException if the payment data fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     * @throws SecurityException if secure storage operations fail
     */
    @Override
    public PaymentData secureStore(PaymentData paymentData)
            throws ValidationException, ConnectionException, QueryExecutionException, 
                   TransactionException, SecurityException {
        validateEntity(paymentData);
        
        // Ensure payment data ID is set
        if (paymentData.getPaymentDataId() == null) {
            paymentData.setPaymentDataId(UUID.randomUUID());
        }
        
        // Ensure created timestamp is set
        if (paymentData.getCreatedAt() == null) {
            paymentData.setCreatedAt(LocalDateTime.now());
        }
        
        // Process sensitive data if present
        if (paymentData.getDecryptedToken() != null && !paymentData.getDecryptedToken().isEmpty()) {
            // Encrypt the sensitive token
            String encryptedToken = encryptionService.encryptPaymentToken(paymentData.getDecryptedToken());
            paymentData.setPaymentToken(encryptedToken);
            
            // Clear the decrypted token from memory
            paymentData.setDecryptedToken(null);
        }
        
        // Process payment details if present
        if (paymentData.getPaymentDetails() != null && !paymentData.getPaymentDetails().isEmpty()) {
            // Sanitize and secure payment details
            String securedDetails = securePaymentDetails(paymentData.getPaymentDetails());
            paymentData.setPaymentDetails(securedDetails);
        }
        
        // Create the payment data record
        return executeWithTransaction(() -> create(paymentData));
    }

    /**
     * Secures payment details by removing sensitive information and applying encryption.
     *
     * @param paymentDetails the payment details JSON
     * @return the secured payment details
     * @throws SecurityException if secure storage operations fail
     */
    private String securePaymentDetails(String paymentDetails) throws SecurityException {
        if (paymentDetails == null || paymentDetails.isEmpty()) {
            return paymentDetails;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(paymentDetails);
            com.fasterxml.jackson.databind.node.ObjectNode securedNode = rootNode.deepCopy();
            
            // Secure card number if present
            if (securedNode.has("cardNumber")) {
                String cardNumber = securedNode.get("cardNumber").asText();
                // Store only last 4 digits and tokenize the rest
                String last4 = cardNumber.substring(Math.max(0, cardNumber.length() - 4));
                securedNode.put("cardNumberLast4", last4);
                
                // Replace full card number with tokenized version
                String tokenizedCard = encryptionService.tokenizeCardNumber(cardNumber);
                securedNode.put("cardNumber", tokenizedCard);
            }
            
            // Remove CVV/security code entirely
            if (securedNode.has("securityCode")) {
                securedNode.remove("securityCode");
            }
            
            // Secure account number if present
            if (securedNode.has("accountNumber")) {
                String accountNumber = securedNode.get("accountNumber").asText();
                // Store only last 4 digits and tokenize the rest
                String last4 = accountNumber.substring(Math.max(0, accountNumber.length() - 4));
                securedNode.put("accountNumberLast4", last4);
                
                // Replace full account number with tokenized version
                String tokenizedAccount = encryptionService.tokenizeAccountNumber(accountNumber);
                securedNode.put("accountNumber", tokenizedAccount);
            }
            
            return mapper.writeValueAsString(securedNode);
        } catch (Exception e) {
            throw new SecurityException("Failed to secure payment details: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves payment data with full access to sensitive information.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @return the payment data with unmasked sensitive information
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws SecurityException if the caller lacks sufficient permissions
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public PaymentData retrieveSensitiveData(UUID paymentDataId)
            throws ResourceNotFoundException, SecurityException, ConnectionException, QueryExecutionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Check if the caller has sufficient permissions
        // This would typically be done through a security context check
        // For now, we'll assume the check has been done at a higher level
        
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + " = ?";
        
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return mapRowWithSensitiveData(rs);
            } else {
                throw new ResourceNotFoundException("Payment data not found with ID: " + paymentDataId);
            }
        }, paymentDataId);
    }

    /**
     * Updates the payment token for an existing payment method.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param newToken the new payment token
     * @return the updated payment data
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ValidationException if the new token fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     * @throws SecurityException if secure storage operations fail
     */
    @Override
    public PaymentData updatePaymentToken(UUID paymentDataId, String newToken)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException, SecurityException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        if (newToken == null) {
            throw new ValidationException("New token cannot be null");
        }
        
        return executeWithTransaction(() -> {
            // Retrieve the existing payment data
            Optional<PaymentData> existingDataOpt = findById(paymentDataId);
            if (!existingDataOpt.isPresent()) {
                throw new ResourceNotFoundException("Payment data not found with ID: " + paymentDataId);
            }
            
            PaymentData existingData = existingDataOpt.get();
            
            // Encrypt the new token if it's not already encrypted
            String encryptedToken = encryptionService.isEncrypted(newToken) ? 
                    newToken : encryptionService.encryptPaymentToken(newToken);
            
            // Update the token
            existingData.setPaymentToken(encryptedToken);
            
            // Save the updated payment data
            return update(existingData);
        });
    }

    /**
     * Updates the expiration date for a payment method.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param expirationMonth the new expiration month (1-12)
     * @param expirationYear the new expiration year (4-digit format)
     * @return the updated payment data
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ValidationException if the expiration date is invalid
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public PaymentData updateExpiration(UUID paymentDataId, int expirationMonth, int expirationYear)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Validate expiration date
        if (expirationMonth < 1 || expirationMonth > 12) {
            throw new ValidationException("Invalid expiration month: " + expirationMonth);
        }
        
        if (expirationYear < LocalDate.now().getYear() || expirationYear > LocalDate.now().getYear() + 20) {
            throw new ValidationException("Invalid expiration year: " + expirationYear);
        }
        
        return executeWithTransaction(() -> {
            // Retrieve the existing payment data
            Optional<PaymentData> existingDataOpt = findById(paymentDataId);
            if (!existingDataOpt.isPresent()) {
                throw new ResourceNotFoundException("Payment data not found with ID: " + paymentDataId);
            }
            
            PaymentData existingData = existingDataOpt.get();
            
            // Create expiration date (last day of the month)
            int lastDay = LocalDate.of(expirationYear, expirationMonth, 1)
                                  .plusMonths(1)
                                  .minusDays(1)
                                  .getDayOfMonth();
            
            LocalDate expirationDate = LocalDate.of(expirationYear, expirationMonth, lastDay);
            existingData.setExpiration(expirationDate);
            
            // Update payment details if it contains expiration information
            if (existingData.getPaymentDetails() != null && !existingData.getPaymentDetails().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(existingData.getPaymentDetails());
                    
                    if (rootNode.isObject()) {
                        com.fasterxml.jackson.databind.node.ObjectNode detailsNode = (com.fasterxml.jackson.databind.node.ObjectNode) rootNode;
                        
                        // Update expiration in payment details
                        detailsNode.put("expirationMonth", expirationMonth);
                        detailsNode.put("expirationYear", expirationYear);
                        
                        existingData.setPaymentDetails(mapper.writeValueAsString(detailsNode));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to update expiration in payment details: {}", e.getMessage());
                    // Continue with the update even if this part fails
                }
            }
            
            // Save the updated payment data
            return update(existingData);
        });
    }

    /**
     * Updates the billing information associated with a payment method.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param billingData JSON representation of billing information
     * @return the updated payment data
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ValidationException if the billing data fails validation
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public PaymentData updateBillingData(UUID paymentDataId, String billingData)
            throws ResourceNotFoundException, ValidationException, ConnectionException,
                   QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        // Validate billing data
        if (billingData != null && !billingData.isEmpty()) {
            if (!isValidJson(billingData)) {
                throw new ValidationException("Billing data must be valid JSON");
            }
        }
        
        return executeWithTransaction(() -> {
            // Retrieve the existing payment data
            Optional<PaymentData> existingDataOpt = findById(paymentDataId);
            if (!existingDataOpt.isPresent()) {
                throw new ResourceNotFoundException("Payment data not found with ID: " + paymentDataId);
            }
            
            PaymentData existingData = existingDataOpt.get();
            
            // Update billing data
            existingData.setBillingData(billingData);
            
            // Save the updated payment data
            return update(existingData);
        });
    }

    /**
     * Retrieves payment data with masked sensitive information based on user role.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param userRole the role of the requesting user
     * @return the payment data with role-appropriate masking applied
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public PaymentData retrieveWithRoleBasedMasking(UUID paymentDataId, String userRole)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + ID_COLUMN + " = ?";
        
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return mapRowWithMasking(rs, userRole);
            } else {
                throw new ResourceNotFoundException("Payment data not found with ID: " + paymentDataId);
            }
        }, paymentDataId);
    }

    /**
     * Searches for payment data across multiple transactions based on filter criteria.
     *
     * @param filterParams the parameters to filter the search results
     * @return a list of payment data records matching the search criteria
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     */
    @Override
    public List<PaymentData> searchPaymentData(PaymentFilterParams filterParams)
            throws ConnectionException, QueryExecutionException {
        if (filterParams == null) {
            filterParams = new PaymentFilterParams();
        }
        
        return query(filterParams);
    }

    /**
     * Marks a payment method as expired or invalid.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @param reason the reason for invalidation
     * @return the updated payment data with invalid status
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public PaymentData invalidatePaymentMethod(UUID paymentDataId, String reason)
            throws ResourceNotFoundException, ConnectionException, QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        return executeWithTransaction(() -> {
            // Retrieve the existing payment data
            Optional<PaymentData> existingDataOpt = findById(paymentDataId);
            if (!existingDataOpt.isPresent()) {
                throw new ResourceNotFoundException("Payment data not found with ID: " + paymentDataId);
            }
            
            PaymentData existingData = existingDataOpt.get();
            
            // Update payment details to mark as invalid
            if (existingData.getPaymentDetails() != null && !existingData.getPaymentDetails().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(existingData.getPaymentDetails());
                    
                    if (rootNode.isObject()) {
                        com.fasterxml.jackson.databind.node.ObjectNode detailsNode = (com.fasterxml.jackson.databind.node.ObjectNode) rootNode;
                        
                        // Mark as invalid
                        detailsNode.put("valid", false);
                        detailsNode.put("invalidReason", reason);
                        detailsNode.put("invalidatedAt", LocalDateTime.now().toString());
                        
                        existingData.setPaymentDetails(mapper.writeValueAsString(detailsNode));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to update payment details for invalidation: {}", e.getMessage());
                    throw new QueryExecutionException("Failed to invalidate payment method: " + e.getMessage(), e);
                }
            }
            
            // Set expiration to yesterday to ensure it's considered expired
            existingData.setExpiration(LocalDate.now().minusDays(1));
            
            // Save the updated payment data
            return update(existingData);
        });
    }

    /**
     * Securely deletes sensitive payment data while maintaining transaction records.
     *
     * @param paymentDataId the unique identifier of the payment data
     * @return true if the sensitive data was successfully deleted
     * @throws ResourceNotFoundException if the payment data does not exist
     * @throws SecurityException if the secure deletion operation fails
     * @throws ConnectionException if a database connection cannot be established
     * @throws QueryExecutionException if the query execution fails
     * @throws TransactionException if the transaction management fails
     */
    @Override
    public boolean secureDelete(UUID paymentDataId)
            throws ResourceNotFoundException, SecurityException, ConnectionException,
                   QueryExecutionException, TransactionException {
        if (paymentDataId == null) {
            throw new ValidationException("Payment data ID cannot be null");
        }
        
        return executeWithTransaction(() -> {
            // Retrieve the existing payment data
            Optional<PaymentData> existingDataOpt = findById(paymentDataId);
            if (!existingDataOpt.isPresent()) {
                throw new ResourceNotFoundException("Payment data not found with ID: " + paymentDataId);
            }
            
            PaymentData existingData = existingDataOpt.get();
            
            // Create a redacted version of the payment data
            PaymentData redactedData = new PaymentData();
            redactedData.setPaymentDataId(existingData.getPaymentDataId());
            redactedData.setTransactionId(existingData.getTransactionId());
            redactedData.setPaymentMethodId(existingData.getPaymentMethodId());
            redactedData.setCreatedAt(existingData.getCreatedAt());
            
            // Replace sensitive data with redaction markers
            redactedData.setPaymentToken("[REDACTED]");
            
            // Redact payment details while preserving non-sensitive metadata
            if (existingData.getPaymentDetails() != null && !existingData.getPaymentDetails().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(existingData.getPaymentDetails());
                    
                    if (rootNode.isObject()) {
                        com.fasterxml.jackson.databind.node.ObjectNode detailsNode = (com.fasterxml.jackson.databind.node.ObjectNode) rootNode;
                        
                        // Preserve payment type and other non-sensitive fields
                        com.fasterxml.jackson.databind.node.ObjectNode redactedDetails = mapper.createObjectNode();
                        
                        if (detailsNode.has("paymentType")) {
                            redactedDetails.put("paymentType", detailsNode.get("paymentType").asText());
                        }
                        
                        if (detailsNode.has("cardNumberLast4")) {
                            redactedDetails.put("cardNumberLast4", detailsNode.get("cardNumberLast4").asText());
                        }
                        
                        // Add redaction marker
                        redactedDetails.put("redacted", true);
                        redactedDetails.put("redactedAt", LocalDateTime.now().toString());
                        
                        redactedData.setPaymentDetails(mapper.writeValueAsString(redactedDetails));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to redact payment details: {}", e.getMessage());
                    redactedData.setPaymentDetails("{\"redacted\": true}");
                }
            } else {
                redactedData.setPaymentDetails("{\"redacted\": true}");
            }
            
            // Redact billing data
            redactedData.setBillingData("{\"redacted\": true}");
            
            // Update with redacted data
            update(redactedData);
            
            return true;
        });
    }
}