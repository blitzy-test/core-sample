package io.briklabs.sample.config;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DatabaseConfig for the payment database.
 * Provides payment-specific database connection parameters and connection pool configuration
 * optimized for payment transaction processing.
 */
public class PaymentDatabaseConfig implements DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDatabaseConfig.class);
    private final ConfigSource configSource;

    /**
     * Constructs a new PaymentDatabaseConfig with the specified ConfigSource.
     * 
     * @param configSource The configuration source
     */
    public PaymentDatabaseConfig(ConfigSource configSource) {
        this.configSource = configSource;
        logger.info("Initializing payment database configuration");
        
        // Validate payment configuration
        if (!configSource.validatePaymentConfig()) {
            logger.warn("Payment configuration validation failed - check configuration settings");
        }
    }

    /**
     * Gets the payment database URL.
     * 
     * @return Payment database URL string
     */
    @Override
    public String getDatabaseURL() {
        return configSource.getRequired("payment.database.url");
    }

    /**
     * Gets the payment database username.
     * 
     * @return Payment database username
     */
    @Override
    public String getDatabaseUsername() {
        return configSource.getRequired("payment.database.username");
    }

    /**
     * Gets the payment database password.
     * 
     * @return Payment database password
     */
    @Override
    public String getDatabasePassword() {
        return configSource.getRequired("payment.database.password");
    }

    /**
     * Gets the payment database schema.
     * 
     * @return Payment database schema name
     */
    @Override
    public String getDatabaseSchema() {
        return configSource.getRequired("payment.database.schema");
    }
    
    /**
     * Gets the connection pool configuration parameters for the payment database.
     * Provides optimized HikariCP settings for payment transaction processing.
     * 
     * @return Optional containing a map of connection pool configuration parameters
     */
    @Override
    public Optional<Map<String, Object>> getConnectionPoolConfig() {
        // Check if connection pooling is enabled for payment database
        if (!configSource.getPaymentConfigBoolean("database.connectionPool.enabled", true)) {
            logger.warn("Payment database connection pooling is disabled - this is not recommended for production");
            return Optional.empty();
        }
        
        // Get payment-specific connection pool configuration with optimized defaults
        Map<String, Object> poolConfig = configSource.getPaymentConnectionPoolConfig();
        
        logger.info("Configured payment database connection pool with maximumPoolSize={}, minimumIdle={}",
                poolConfig.get("maximumPoolSize"), poolConfig.get("minimumIdle"));
        
        return Optional.of(poolConfig);
    }
    
    /**
     * Validates the payment database connection parameters.
     * Extends the default implementation with payment-specific validation.
     * 
     * @return true if connection parameters are valid, false otherwise
     */
    @Override
    public boolean validateConnectionParameters() {
        boolean isValid = DatabaseConfig.super.validateConnectionParameters();
        
        if (!isValid) {
            logger.error("Payment database connection validation failed - check connection parameters");
        } else {
            logger.info("Payment database connection validation successful");
        }
        
        return isValid;
    }
    
    /**
     * Gets the PostgreSQL version required for payment processing.
     * 
     * @return PostgreSQL version string (e.g., "17.4")
     */
    public String getRequiredPostgresVersion() {
        return configSource.getPaymentConfig("database.requiredVersion", "17.4");
    }
    
    /**
     * Gets the maximum number of connections for the payment database pool.
     * 
     * @return Maximum pool size
     */
    public int getMaximumPoolSize() {
        return configSource.getPaymentConfigInt("database.connectionPool.maximumPoolSize", 30);
    }
    
    /**
     * Gets the minimum number of idle connections for the payment database pool.
     * 
     * @return Minimum idle connections
     */
    public int getMinimumIdleConnections() {
        return configSource.getPaymentConfigInt("database.connectionPool.minimumIdle", 10);
    }
    
    /**
     * Gets the connection timeout for the payment database pool in milliseconds.
     * 
     * @return Connection timeout in milliseconds
     */
    public long getConnectionTimeoutMs() {
        return configSource.getPaymentConfigLong("database.connectionPool.connectionTimeout", 20000);
    }
    
    /**
     * Gets the idle timeout for the payment database pool in milliseconds.
     * 
     * @return Idle timeout in milliseconds
     */
    public long getIdleTimeoutMs() {
        return configSource.getPaymentConfigLong("database.connectionPool.idleTimeout", 300000);
    }
    
    /**
     * Gets the maximum lifetime for connections in the payment database pool in milliseconds.
     * 
     * @return Maximum connection lifetime in milliseconds
     */
    public long getMaxLifetimeMs() {
        return configSource.getPaymentConfigLong("database.connectionPool.maxLifetime", 1200000);
    }
    
    /**
     * Checks if auto-commit is enabled for payment database connections.
     * Default is false for payment transactions to ensure proper transaction management.
     * 
     * @return true if auto-commit is enabled, false otherwise
     */
    public boolean isAutoCommitEnabled() {
        return configSource.getPaymentConfigBoolean("database.connectionPool.autoCommit", false);
    }
    
    /**
     * Gets the leak detection threshold for the payment database pool in milliseconds.
     * 
     * @return Leak detection threshold in milliseconds
     */
    public long getLeakDetectionThresholdMs() {
        return configSource.getPaymentConfigLong("database.connectionPool.leakDetectionThreshold", 60000);
    }
    
    /**
     * Checks if JMX monitoring is enabled for the payment database connection pool.
     * 
     * @return true if JMX monitoring is enabled, false otherwise
     */
    public boolean isJmxMonitoringEnabled() {
        return configSource.getPaymentConfigBoolean("database.connectionPool.registerMbeans", true);
    }
}