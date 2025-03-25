package io.briklabs.sample.payments.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.config.PaymentDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configures and initializes the HikariCP connection pool specifically for payment operations.
 * This class manages database connection parameters, pool sizing, timeouts, and health monitoring
 * settings optimized for payment transaction processing.
 */
public class HikariCPConfig {
    private static final Logger logger = LoggerFactory.getLogger(HikariCPConfig.class);
    
    /**
     * Thread-safe reference to the HikariCP data source.
     */
    private static final AtomicReference<HikariDataSource> dataSourceRef = new AtomicReference<>();
    
    /**
     * Default connection pool parameters for payment transaction processing.
     */
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 30;
    private static final int DEFAULT_MINIMUM_IDLE = 10;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 20000; // 20 seconds
    private static final long DEFAULT_VALIDATION_TIMEOUT = 5000; // 5 seconds
    private static final long DEFAULT_IDLE_TIMEOUT = 300000; // 5 minutes
    private static final long DEFAULT_MAX_LIFETIME = 1200000; // 20 minutes
    private static final boolean DEFAULT_AUTO_COMMIT = false;
    private static final String DEFAULT_CONNECTION_TEST_QUERY = "SELECT 1";
    private static final long DEFAULT_LEAK_DETECTION_THRESHOLD = 60000; // 1 minute
    private static final boolean DEFAULT_REGISTER_MBEANS = true;
    private static final String DEFAULT_POOL_NAME = "PaymentHikariPool";
    
    /**
     * Private constructor to prevent instantiation.
     * This class provides static methods only.
     */
    private HikariCPConfig() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Initializes and returns the HikariCP data source for payment operations.
     * This method is thread-safe and ensures the data source is initialized only once.
     *
     * @param databaseConfig The database configuration
     * @param configSource The configuration source
     * @return The initialized HikariDataSource
     */
    public static synchronized HikariDataSource getDataSource(DatabaseConfig databaseConfig, ConfigSource configSource) {
        if (dataSourceRef.get() == null) {
            logger.info("Initializing HikariCP connection pool for payment database");
            try {
                HikariConfig config = createHikariConfig(databaseConfig, configSource);
                HikariDataSource dataSource = new HikariDataSource(config);
                dataSourceRef.set(dataSource);
                logger.info("HikariCP connection pool initialized successfully with maximumPoolSize={}, minimumIdle={}",
                        config.getMaximumPoolSize(), config.getMinimumIdle());
            } catch (Exception e) {
                logger.error("Failed to initialize HikariCP connection pool: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize connection pool: " + e.getMessage(), e);
            }
        }
        
        return dataSourceRef.get();
    }
    
    /**
     * Creates a HikariCP configuration with settings optimized for payment transaction processing.
     *
     * @param databaseConfig The database configuration
     * @param configSource The configuration source
     * @return The HikariCP configuration
     */
    private static HikariConfig createHikariConfig(DatabaseConfig databaseConfig, ConfigSource configSource) {
        HikariConfig config = new HikariConfig();
        
        // Set basic connection properties
        config.setJdbcUrl(databaseConfig.getDatabaseURL());
        config.setUsername(databaseConfig.getDatabaseUsername());
        config.setPassword(databaseConfig.getDatabasePassword());
        config.setSchema(databaseConfig.getDatabaseSchema());
        
        // Get connection pool configuration from database config if available
        Optional<Map<String, Object>> poolConfigOpt = databaseConfig.getConnectionPoolConfig();
        
        if (poolConfigOpt.isPresent()) {
            Map<String, Object> poolConfig = poolConfigOpt.get();
            logger.debug("Using connection pool configuration from database config");
            
            // Apply pool configuration from database config
            applyPoolConfiguration(config, poolConfig);
        } else {
            // Use payment-specific defaults
            logger.debug("Using default payment-specific connection pool configuration");
            setDefaultPaymentPoolConfiguration(config);
        }
        
        // Ensure essential properties are set
        validateAndApplyDefaults(config);
        
        return config;
    }
    
    /**
     * Applies connection pool configuration from a map of properties.
     *
     * @param config The HikariCP configuration to update
     * @param poolConfig The map of pool configuration properties
     */
    private static void applyPoolConfiguration(HikariConfig config, Map<String, Object> poolConfig) {
        // Apply each property with appropriate type handling
        for (Map.Entry<String, Object> entry : poolConfig.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            try {
                switch (key) {
                    case "maximumPoolSize":
                        config.setMaximumPoolSize(getIntValue(poolConfig, key, DEFAULT_MAXIMUM_POOL_SIZE));
                        break;
                    case "minimumIdle":
                        config.setMinimumIdle(getIntValue(poolConfig, key, DEFAULT_MINIMUM_IDLE));
                        break;
                    case "connectionTimeout":
                        config.setConnectionTimeout(getLongValue(poolConfig, key, DEFAULT_CONNECTION_TIMEOUT));
                        break;
                    case "validationTimeout":
                        config.setValidationTimeout(getLongValue(poolConfig, key, DEFAULT_VALIDATION_TIMEOUT));
                        break;
                    case "idleTimeout":
                        config.setIdleTimeout(getLongValue(poolConfig, key, DEFAULT_IDLE_TIMEOUT));
                        break;
                    case "maxLifetime":
                        config.setMaxLifetime(getLongValue(poolConfig, key, DEFAULT_MAX_LIFETIME));
                        break;
                    case "autoCommit":
                        config.setAutoCommit(getBooleanValue(poolConfig, key, DEFAULT_AUTO_COMMIT));
                        break;
                    case "connectionTestQuery":
                        config.setConnectionTestQuery(getStringValue(poolConfig, key, DEFAULT_CONNECTION_TEST_QUERY));
                        break;
                    case "leakDetectionThreshold":
                        config.setLeakDetectionThreshold(getLongValue(poolConfig, key, DEFAULT_LEAK_DETECTION_THRESHOLD));
                        break;
                    case "registerMbeans":
                        config.setRegisterMbeans(getBooleanValue(poolConfig, key, DEFAULT_REGISTER_MBEANS));
                        break;
                    case "poolName":
                        config.setPoolName(getStringValue(poolConfig, key, DEFAULT_POOL_NAME));
                        break;
                    default:
                        // For other properties, add as data source property
                        config.addDataSourceProperty(key, value);
                        break;
                }
            } catch (Exception e) {
                logger.warn("Failed to set HikariCP property {}: {}", key, e.getMessage());
            }
        }
    }
    
    /**
     * Sets default payment-specific connection pool configuration.
     *
     * @param config The HikariCP configuration to update
     */
    private static void setDefaultPaymentPoolConfiguration(HikariConfig config) {
        config.setMaximumPoolSize(DEFAULT_MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(DEFAULT_MINIMUM_IDLE);
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        config.setValidationTimeout(DEFAULT_VALIDATION_TIMEOUT);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME);
        config.setAutoCommit(DEFAULT_AUTO_COMMIT);
        config.setConnectionTestQuery(DEFAULT_CONNECTION_TEST_QUERY);
        config.setLeakDetectionThreshold(DEFAULT_LEAK_DETECTION_THRESHOLD);
        config.setRegisterMbeans(DEFAULT_REGISTER_MBEANS);
        config.setPoolName(DEFAULT_POOL_NAME);
        
        // Additional optimizations for payment processing
        config.setInitializationFailTimeout(10000); // 10 seconds
        config.setKeepaliveTime(60000); // 1 minute
    }
    
    /**
     * Validates the configuration and applies defaults for any missing essential properties.
     *
     * @param config The HikariCP configuration to validate and update
     */
    private static void validateAndApplyDefaults(HikariConfig config) {
        // Ensure essential properties have valid values
        if (config.getMaximumPoolSize() <= 0) {
            logger.warn("Invalid maximumPoolSize ({}), using default: {}", 
                    config.getMaximumPoolSize(), DEFAULT_MAXIMUM_POOL_SIZE);
            config.setMaximumPoolSize(DEFAULT_MAXIMUM_POOL_SIZE);
        }
        
        if (config.getMinimumIdle() < 0) {
            logger.warn("Invalid minimumIdle ({}), using default: {}", 
                    config.getMinimumIdle(), DEFAULT_MINIMUM_IDLE);
            config.setMinimumIdle(DEFAULT_MINIMUM_IDLE);
        }
        
        if (config.getMinimumIdle() > config.getMaximumPoolSize()) {
            logger.warn("minimumIdle ({}) > maximumPoolSize ({}), adjusting minimumIdle", 
                    config.getMinimumIdle(), config.getMaximumPoolSize());
            config.setMinimumIdle(Math.min(config.getMaximumPoolSize(), DEFAULT_MINIMUM_IDLE));
        }
        
        if (config.getConnectionTimeout() <= 0) {
            logger.warn("Invalid connectionTimeout ({}), using default: {}", 
                    config.getConnectionTimeout(), DEFAULT_CONNECTION_TIMEOUT);
            config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        }
        
        if (config.getValidationTimeout() <= 0) {
            logger.warn("Invalid validationTimeout ({}), using default: {}", 
                    config.getValidationTimeout(), DEFAULT_VALIDATION_TIMEOUT);
            config.setValidationTimeout(DEFAULT_VALIDATION_TIMEOUT);
        }
        
        if (config.getIdleTimeout() <= 0) {
            logger.warn("Invalid idleTimeout ({}), using default: {}", 
                    config.getIdleTimeout(), DEFAULT_IDLE_TIMEOUT);
            config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        }
        
        if (config.getMaxLifetime() <= 0) {
            logger.warn("Invalid maxLifetime ({}), using default: {}", 
                    config.getMaxLifetime(), DEFAULT_MAX_LIFETIME);
            config.setMaxLifetime(DEFAULT_MAX_LIFETIME);
        }
        
        // Validate relationships between timeout values
        if (config.getIdleTimeout() < config.getConnectionTimeout()) {
            logger.warn("Suboptimal configuration: idleTimeout ({}) < connectionTimeout ({})", 
                    config.getIdleTimeout(), config.getConnectionTimeout());
        }
        
        if (config.getMaxLifetime() <= config.getIdleTimeout()) {
            logger.warn("Suboptimal configuration: maxLifetime ({}) <= idleTimeout ({})", 
                    config.getMaxLifetime(), config.getIdleTimeout());
            // Adjust maxLifetime to be at least 30 minutes longer than idleTimeout
            config.setMaxLifetime(config.getIdleTimeout() + 1800000);
        }
    }
    
    /**
     * Gets an integer value from a map with a default fallback.
     *
     * @param map The map to get the value from
     * @param key The key to look up
     * @param defaultValue The default value to use if the key is not found or not an integer
     * @return The integer value
     */
    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for {}: {}", key, value);
                return defaultValue;
            }
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * Gets a long value from a map with a default fallback.
     *
     * @param map The map to get the value from
     * @param key The key to look up
     * @param defaultValue The default value to use if the key is not found or not a long
     * @return The long value
     */
    private static long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long value for {}: {}", key, value);
                return defaultValue;
            }
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
    
    /**
     * Gets a boolean value from a map with a default fallback.
     *
     * @param map The map to get the value from
     * @param key The key to look up
     * @param defaultValue The default value to use if the key is not found or not a boolean
     * @return The boolean value
     */
    private static boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * Gets a string value from a map with a default fallback.
     *
     * @param map The map to get the value from
     * @param key The key to look up
     * @param defaultValue The default value to use if the key is not found or not a string
     * @return The string value
     */
    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Gets the current connection pool metrics.
     *
     * @return A map of connection pool metrics
     */
    public static Map<String, Object> getPoolMetrics() {
        HikariDataSource dataSource = dataSourceRef.get();
        if (dataSource == null) {
            return Map.of("status", "not_initialized");
        }
        
        return Map.of(
            "status", dataSource.isClosed() ? "closed" : "active",
            "activeConnections", dataSource.getHikariPoolMXBean().getActiveConnections(),
            "idleConnections", dataSource.getHikariPoolMXBean().getIdleConnections(),
            "totalConnections", dataSource.getHikariPoolMXBean().getTotalConnections(),
            "threadsAwaitingConnection", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
            "maximumPoolSize", dataSource.getHikariPoolMXBean().getMaximumPoolSize()
        );
    }
    
    /**
     * Checks if the connection pool is healthy.
     *
     * @return true if the connection pool is initialized and not closed, false otherwise
     */
    public static boolean isHealthy() {
        HikariDataSource dataSource = dataSourceRef.get();
        return dataSource != null && !dataSource.isClosed();
    }
    
    /**
     * Closes the connection pool and releases all resources.
     * This method should be called during application shutdown.
     */
    public static void shutdown() {
        HikariDataSource dataSource = dataSourceRef.get();
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Shutting down HikariCP connection pool for payment database");
            dataSource.close();
            dataSourceRef.set(null);
        }
    }
    
    /**
     * Creates a new HikariDataSource specifically for payment operations.
     * This method is useful for testing or when a separate connection pool is needed.
     *
     * @param databaseConfig The database configuration
     * @param configSource The configuration source
     * @return A new HikariDataSource
     */
    public static HikariDataSource createDataSource(DatabaseConfig databaseConfig, ConfigSource configSource) {
        HikariConfig config = createHikariConfig(databaseConfig, configSource);
        return new HikariDataSource(config);
    }
    
    /**
     * Gets a HikariDataSource using a PaymentDatabaseConfig.
     * This is a convenience method for payment-specific database configurations.
     *
     * @param paymentDbConfig The payment database configuration
     * @param configSource The configuration source
     * @return The initialized HikariDataSource
     */
    public static HikariDataSource getDataSource(PaymentDatabaseConfig paymentDbConfig, ConfigSource configSource) {
        return getDataSource((DatabaseConfig) paymentDbConfig, configSource);
    }
}