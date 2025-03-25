package io.briklabs.sample.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DatabaseConfig for payment-specific database configuration.
 * This class provides database connection parameters for the payments module,
 * retrieving values from ConfigSource and making them available to payment data access components.
 */
public class PaymentDatabaseConfig implements DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDatabaseConfig.class);
    
    private final ConfigSource configSource;
    private final HikariConfigurationProvider hikariConfigProvider;
    
    /**
     * Constructs a new PaymentDatabaseConfig with the specified ConfigSource.
     * 
     * @param configSource The configuration source
     */
    public PaymentDatabaseConfig(ConfigSource configSource) {
        this.configSource = configSource;
        this.hikariConfigProvider = new HikariConfigurationProvider(configSource);
        logger.info("Initializing payment database configuration");
        
        if (!configSource.isPaymentModuleEnabled()) {
            logger.warn("Payment module is disabled in configuration - some features may not be available");
        }
    }

    @Override
    public String getDatabaseURL() {
        // First try payment-specific database URL
        String paymentDbUrl = configSource.getPaymentConfig("database.url", null);
        if (paymentDbUrl != null) {
            logger.debug("Using payment-specific database URL: {}", paymentDbUrl);
            return paymentDbUrl;
        }
        
        // Fall back to main database URL if payment-specific URL is not defined
        String mainDbUrl = configSource.getRequired("brik.database.payment.url");
        logger.debug("Using main database URL for payments: {}", mainDbUrl);
        return mainDbUrl;
    }

    @Override
    public String getDatabaseUsername() {
        // First try payment-specific database username
        String paymentDbUsername = configSource.getPaymentConfig("database.username", null);
        if (paymentDbUsername != null) {
            logger.debug("Using payment-specific database username");
            return paymentDbUsername;
        }
        
        // Fall back to main database username if payment-specific username is not defined
        String mainDbUsername = configSource.getRequired("brik.database.payment.user");
        logger.debug("Using main database username for payments");
        return mainDbUsername;
    }

    @Override
    public String getDatabasePassword() {
        // First try payment-specific database password
        String paymentDbPassword = configSource.getPaymentConfig("database.password", null);
        if (paymentDbPassword != null) {
            logger.debug("Using payment-specific database password");
            return paymentDbPassword;
        }
        
        // Fall back to main database password if payment-specific password is not defined
        String mainDbPassword = configSource.getRequired("brik.database.payment.password");
        logger.debug("Using main database password for payments");
        return mainDbPassword;
    }

    @Override
    public String getDatabaseSchema() {
        // First try payment-specific database schema
        String paymentDbSchema = configSource.getPaymentConfig("database.schema", null);
        if (paymentDbSchema != null) {
            logger.debug("Using payment-specific database schema: {}", paymentDbSchema);
            return paymentDbSchema;
        }
        
        // Fall back to default payment schema if not explicitly defined
        logger.debug("Using default payment database schema: payment");
        return "payment";
    }
    
    @Override
    public Optional<Map<String, Object>> getConnectionPoolConfig() {
        Map<String, Object> poolConfig = new HashMap<>();
        
        // Check if payment module is enabled
        if (!configSource.isPaymentModuleEnabled()) {
            logger.warn("Payment module is disabled - using default connection pool configuration");
            poolConfig = hikariConfigProvider.getDefaultConnectionPoolConfig();
            return Optional.of(poolConfig);
        }
        
        // Get payment-specific connection pool configuration
        try {
            poolConfig = configSource.getPaymentConnectionPoolConfig();
            
            // Validate minimum configuration requirements
            validateConnectionPoolConfig(poolConfig);
            
            logger.info("Using payment-specific connection pool configuration: maximumPoolSize={}, minimumIdle={}",
                    poolConfig.get("maximumPoolSize"), poolConfig.get("minimumIdle"));
            
            return Optional.of(poolConfig);
        } catch (Exception e) {
            logger.error("Failed to load payment connection pool configuration: {}", e.getMessage(), e);
            
            // Fall back to default payment connection pool configuration
            logger.info("Using default payment connection pool configuration");
            poolConfig = hikariConfigProvider.getPaymentConnectionPoolConfig();
            return Optional.of(poolConfig);
        }
    }
    
    /**
     * Validates the connection pool configuration.
     * 
     * @param poolConfig The connection pool configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validateConnectionPoolConfig(Map<String, Object> poolConfig) {
        // Check for required parameters
        if (!poolConfig.containsKey("maximumPoolSize")) {
            throw new IllegalArgumentException("Missing required connection pool parameter: maximumPoolSize");
        }
        
        if (!poolConfig.containsKey("minimumIdle")) {
            throw new IllegalArgumentException("Missing required connection pool parameter: minimumIdle");
        }
        
        // Validate parameter values
        int maxPoolSize = (int) poolConfig.get("maximumPoolSize");
        int minIdle = (int) poolConfig.get("minimumIdle");
        
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("Invalid maximumPoolSize: " + maxPoolSize + " (must be > 0)");
        }
        
        if (minIdle < 0) {
            throw new IllegalArgumentException("Invalid minimumIdle: " + minIdle + " (must be >= 0)");
        }
        
        if (minIdle > maxPoolSize) {
            throw new IllegalArgumentException("Invalid connection pool configuration: minimumIdle (" + 
                    minIdle + ") > maximumPoolSize (" + maxPoolSize + ")");
        }
    }
    
    @Override
    public boolean validateConnectionParameters() {
        try {
            // Validate basic connection parameters
            boolean basicValidation = super.validateConnectionParameters();
            if (!basicValidation) {
                logger.error("Basic connection parameter validation failed");
                return false;
            }
            
            // Validate payment-specific parameters if payment module is enabled
            if (configSource.isPaymentModuleEnabled()) {
                // Check for required payment configuration
                if (!configSource.validatePaymentConfig()) {
                    logger.error("Payment configuration validation failed");
                    return false;
                }
                
                // Validate connection pool configuration
                Optional<Map<String, Object>> poolConfig = getConnectionPoolConfig();
                if (poolConfig.isPresent()) {
                    try {
                        validateConnectionPoolConfig(poolConfig.get());
                    } catch (Exception e) {
                        logger.error("Connection pool configuration validation failed: {}", e.getMessage());
                        return false;
                    }
                }
            }
            
            logger.info("Payment database configuration validation successful");
            return true;
        } catch (Exception e) {
            logger.error("Payment database configuration validation failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets the payment transaction table name.
     * 
     * @return The payment transaction table name
     */
    public String getPaymentTransactionTable() {
        return configSource.getPaymentConfig("database.tables.transaction", "payment_transaction");
    }
    
    /**
     * Gets the payment data table name.
     * 
     * @return The payment data table name
     */
    public String getPaymentDataTable() {
        return configSource.getPaymentConfig("database.tables.paymentData", "payment_data");
    }
    
    /**
     * Gets the payment fee table name.
     * 
     * @return The payment fee table name
     */
    public String getPaymentFeeTable() {
        return configSource.getPaymentConfig("database.tables.paymentFee", "payment_fee");
    }
    
    /**
     * Gets the payment event table name.
     * 
     * @return The payment event table name
     */
    public String getPaymentEventTable() {
        return configSource.getPaymentConfig("database.tables.paymentEvent", "payment_event");
    }
    
    /**
     * Checks if the payment module is enabled.
     * 
     * @return true if payment module is enabled, false otherwise
     */
    public boolean isPaymentModuleEnabled() {
        return configSource.isPaymentModuleEnabled();
    }
    
    /**
     * Gets the payment feature flags.
     * 
     * @return Map of payment feature flags
     */
    public Map<String, Boolean> getPaymentFeatureFlags() {
        return configSource.getPaymentFeatureFlags();
    }
}