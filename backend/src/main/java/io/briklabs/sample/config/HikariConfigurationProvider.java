package io.briklabs.sample.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Provider for HikariCP connection pool configuration.
 * This class creates and configures HikariConfig objects with optimized settings
 * for database connection pooling, particularly for payment transaction processing.
 */
public class HikariConfigurationProvider {
    private static final Logger logger = LoggerFactory.getLogger(HikariConfigurationProvider.class);
    
    // Default connection pool parameters
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 10;
    private static final int DEFAULT_MINIMUM_IDLE = 5;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10 minutes
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30 minutes
    private static final boolean DEFAULT_AUTO_COMMIT = true;
    private static final String DEFAULT_CONNECTION_TEST_QUERY = "SELECT 1";
    private static final long DEFAULT_LEAK_DETECTION_THRESHOLD = 0; // disabled by default
    private static final boolean DEFAULT_REGISTER_MBEANS = false;
    
    // Payment-specific default connection pool parameters
    private static final int PAYMENT_DEFAULT_MAXIMUM_POOL_SIZE = 30;
    private static final int PAYMENT_DEFAULT_MINIMUM_IDLE = 10;
    private static final long PAYMENT_DEFAULT_CONNECTION_TIMEOUT = 20000; // 20 seconds
    private static final long PAYMENT_DEFAULT_IDLE_TIMEOUT = 300000; // 5 minutes
    private static final long PAYMENT_DEFAULT_MAX_LIFETIME = 1200000; // 20 minutes
    private static final boolean PAYMENT_DEFAULT_AUTO_COMMIT = false;
    private static final long PAYMENT_DEFAULT_LEAK_DETECTION_THRESHOLD = 60000; // 60 seconds
    private static final boolean PAYMENT_DEFAULT_REGISTER_MBEANS = true;
    
    private final ConfigSource configSource;
    
    /**
     * Constructs a new HikariConfigurationProvider with the specified ConfigSource.
     * 
     * @param configSource The configuration source
     */
    public HikariConfigurationProvider(ConfigSource configSource) {
        this.configSource = configSource;
        logger.info("Initializing HikariCP configuration provider");
    }
    
    /**
     * Creates a HikariConfig object with settings from the provided DatabaseConfig.
     * 
     * @param dbConfig The database configuration
     * @return A configured HikariConfig object
     */
    public HikariConfig createHikariConfig(DatabaseConfig dbConfig) {
        logger.debug("Creating HikariConfig for database URL: {}", dbConfig.getDatabaseURL());
        
        HikariConfig config = new HikariConfig();
        
        // Set basic connection properties
        config.setJdbcUrl(dbConfig.getDatabaseURL());
        config.setUsername(dbConfig.getDatabaseUsername());
        config.setPassword(dbConfig.getDatabasePassword());
        config.setSchema(dbConfig.getDatabaseSchema());
        
        // Get connection pool configuration from DatabaseConfig if available
        Optional<Map<String, Object>> poolConfigOpt = dbConfig.getConnectionPoolConfig();
        
        if (poolConfigOpt.isPresent()) {
            Map<String, Object> poolConfig = poolConfigOpt.get();
            applyPoolConfiguration(config, poolConfig);
        } else {
            // Apply default configuration
            applyDefaultConfiguration(config);
        }
        
        return config;
    }
    
    /**
     * Creates a HikariConfig object specifically optimized for payment transaction processing.
     * 
     * @param dbConfig The database configuration
     * @return A configured HikariConfig object optimized for payment processing
     */
    public HikariConfig createPaymentHikariConfig(DatabaseConfig dbConfig) {
        logger.debug("Creating payment-optimized HikariConfig for database URL: {}", dbConfig.getDatabaseURL());
        
        HikariConfig config = new HikariConfig();
        
        // Set basic connection properties
        config.setJdbcUrl(dbConfig.getDatabaseURL());
        config.setUsername(dbConfig.getDatabaseUsername());
        config.setPassword(dbConfig.getDatabasePassword());
        config.setSchema(dbConfig.getDatabaseSchema());
        
        // Get connection pool configuration from DatabaseConfig if available
        Optional<Map<String, Object>> poolConfigOpt = dbConfig.getConnectionPoolConfig();
        
        if (poolConfigOpt.isPresent()) {
            Map<String, Object> poolConfig = poolConfigOpt.get();
            applyPoolConfiguration(config, poolConfig);
        } else {
            // Apply payment-specific default configuration
            applyPaymentDefaultConfiguration(config);
        }
        
        // Set pool name for monitoring and troubleshooting
        config.setPoolName("PaymentHikariPool");
        
        return config;
    }
    
    /**
     * Creates a HikariDataSource from the provided DatabaseConfig.
     * 
     * @param dbConfig The database configuration
     * @return A configured HikariDataSource
     */
    public HikariDataSource createDataSource(DatabaseConfig dbConfig) {
        HikariConfig config = createHikariConfig(dbConfig);
        logger.info("Creating HikariDataSource with pool size: {}", config.getMaximumPoolSize());
        return new HikariDataSource(config);
    }
    
    /**
     * Creates a HikariDataSource specifically optimized for payment transaction processing.
     * 
     * @param dbConfig The database configuration
     * @return A configured HikariDataSource optimized for payment processing
     */
    public HikariDataSource createPaymentDataSource(DatabaseConfig dbConfig) {
        HikariConfig config = createPaymentHikariConfig(dbConfig);
        logger.info("Creating payment-optimized HikariDataSource with pool size: {}", config.getMaximumPoolSize());
        return new HikariDataSource(config);
    }
    
    /**
     * Applies connection pool configuration from a map to a HikariConfig object.
     * 
     * @param config The HikariConfig to configure
     * @param poolConfig Map of configuration parameters
     */
    private void applyPoolConfiguration(HikariConfig config, Map<String, Object> poolConfig) {
        // Set connection pool parameters from configuration
        config.setMaximumPoolSize(getIntValue(poolConfig, "maximumPoolSize", DEFAULT_MAXIMUM_POOL_SIZE));
        config.setMinimumIdle(getIntValue(poolConfig, "minimumIdle", DEFAULT_MINIMUM_IDLE));
        config.setConnectionTimeout(getLongValue(poolConfig, "connectionTimeout", DEFAULT_CONNECTION_TIMEOUT));
        config.setIdleTimeout(getLongValue(poolConfig, "idleTimeout", DEFAULT_IDLE_TIMEOUT));
        config.setMaxLifetime(getLongValue(poolConfig, "maxLifetime", DEFAULT_MAX_LIFETIME));
        config.setAutoCommit(getBooleanValue(poolConfig, "autoCommit", DEFAULT_AUTO_COMMIT));
        
        // Set connection test query if provided
        String connectionTestQuery = (String) poolConfig.getOrDefault("connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY);
        config.setConnectionTestQuery(connectionTestQuery);
        
        // Set leak detection threshold if provided
        long leakDetectionThreshold = getLongValue(poolConfig, "leakDetectionThreshold", DEFAULT_LEAK_DETECTION_THRESHOLD);
        if (leakDetectionThreshold > 0) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
        }
        
        // Set JMX monitoring if provided
        boolean registerMbeans = getBooleanValue(poolConfig, "registerMbeans", DEFAULT_REGISTER_MBEANS);
        config.setRegisterMbeans(registerMbeans);
        
        // Set pool name if provided
        String poolName = (String) poolConfig.getOrDefault("poolName", "HikariPool");
        config.setPoolName(poolName);
        
        // Apply any additional properties if provided
        if (poolConfig.containsKey("dataSourceProperties")) {
            @SuppressWarnings("unchecked")
            Map<String, String> dataSourceProps = (Map<String, String>) poolConfig.get("dataSourceProperties");
            Properties props = new Properties();
            props.putAll(dataSourceProps);
            config.setDataSourceProperties(props);
        }
        
        logger.debug("Applied connection pool configuration: maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms",
                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getConnectionTimeout());
    }
    
    /**
     * Applies default configuration to a HikariConfig object.
     * 
     * @param config The HikariConfig to configure
     */
    private void applyDefaultConfiguration(HikariConfig config) {
        config.setMaximumPoolSize(DEFAULT_MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(DEFAULT_MINIMUM_IDLE);
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME);
        config.setAutoCommit(DEFAULT_AUTO_COMMIT);
        config.setConnectionTestQuery(DEFAULT_CONNECTION_TEST_QUERY);
        
        // Only set leak detection if enabled
        if (DEFAULT_LEAK_DETECTION_THRESHOLD > 0) {
            config.setLeakDetectionThreshold(DEFAULT_LEAK_DETECTION_THRESHOLD);
        }
        
        config.setRegisterMbeans(DEFAULT_REGISTER_MBEANS);
        config.setPoolName("DefaultHikariPool");
        
        logger.debug("Applied default connection pool configuration: maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms",
                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getConnectionTimeout());
    }
    
    /**
     * Applies payment-specific default configuration to a HikariConfig object.
     * These settings are optimized for payment transaction processing.
     * 
     * @param config The HikariConfig to configure
     */
    private void applyPaymentDefaultConfiguration(HikariConfig config) {
        config.setMaximumPoolSize(PAYMENT_DEFAULT_MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(PAYMENT_DEFAULT_MINIMUM_IDLE);
        config.setConnectionTimeout(PAYMENT_DEFAULT_CONNECTION_TIMEOUT);
        config.setIdleTimeout(PAYMENT_DEFAULT_IDLE_TIMEOUT);
        config.setMaxLifetime(PAYMENT_DEFAULT_MAX_LIFETIME);
        config.setAutoCommit(PAYMENT_DEFAULT_AUTO_COMMIT);
        config.setConnectionTestQuery(DEFAULT_CONNECTION_TEST_QUERY);
        config.setLeakDetectionThreshold(PAYMENT_DEFAULT_LEAK_DETECTION_THRESHOLD);
        config.setRegisterMbeans(PAYMENT_DEFAULT_REGISTER_MBEANS);
        
        logger.debug("Applied payment-optimized connection pool configuration: maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms",
                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getConnectionTimeout());
    }
    
    /**
     * Gets a configuration map for a standard connection pool.
     * 
     * @return Map of connection pool configuration parameters
     */
    public Map<String, Object> getDefaultConnectionPoolConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maximumPoolSize", DEFAULT_MAXIMUM_POOL_SIZE);
        config.put("minimumIdle", DEFAULT_MINIMUM_IDLE);
        config.put("connectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        config.put("idleTimeout", DEFAULT_IDLE_TIMEOUT);
        config.put("maxLifetime", DEFAULT_MAX_LIFETIME);
        config.put("autoCommit", DEFAULT_AUTO_COMMIT);
        config.put("connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY);
        config.put("leakDetectionThreshold", DEFAULT_LEAK_DETECTION_THRESHOLD);
        config.put("registerMbeans", DEFAULT_REGISTER_MBEANS);
        return config;
    }
    
    /**
     * Gets a configuration map for a payment-optimized connection pool.
     * 
     * @return Map of connection pool configuration parameters optimized for payment processing
     */
    public Map<String, Object> getPaymentConnectionPoolConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maximumPoolSize", PAYMENT_DEFAULT_MAXIMUM_POOL_SIZE);
        config.put("minimumIdle", PAYMENT_DEFAULT_MINIMUM_IDLE);
        config.put("connectionTimeout", PAYMENT_DEFAULT_CONNECTION_TIMEOUT);
        config.put("idleTimeout", PAYMENT_DEFAULT_IDLE_TIMEOUT);
        config.put("maxLifetime", PAYMENT_DEFAULT_MAX_LIFETIME);
        config.put("autoCommit", PAYMENT_DEFAULT_AUTO_COMMIT);
        config.put("connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY);
        config.put("leakDetectionThreshold", PAYMENT_DEFAULT_LEAK_DETECTION_THRESHOLD);
        config.put("registerMbeans", PAYMENT_DEFAULT_REGISTER_MBEANS);
        config.put("poolName", "PaymentHikariPool");
        return config;
    }
    
    /**
     * Gets a configuration map for a connection pool from the ConfigSource.
     * 
     * @param poolName The name of the connection pool
     * @return Map of connection pool configuration parameters
     */
    public Map<String, Object> getConnectionPoolConfigFromSource(String poolName) {
        Map<String, Object> config = new HashMap<>();
        
        String prefix = "brik.database." + poolName + ".connectionPool.";
        
        config.put("maximumPoolSize", configSource.getOrDefault(prefix + "maximumPoolSize", DEFAULT_MAXIMUM_POOL_SIZE));
        config.put("minimumIdle", configSource.getOrDefault(prefix + "minimumIdle", DEFAULT_MINIMUM_IDLE));
        config.put("connectionTimeout", configSource.getOrDefault(prefix + "connectionTimeout", DEFAULT_CONNECTION_TIMEOUT));
        config.put("idleTimeout", configSource.getOrDefault(prefix + "idleTimeout", DEFAULT_IDLE_TIMEOUT));
        config.put("maxLifetime", configSource.getOrDefault(prefix + "maxLifetime", DEFAULT_MAX_LIFETIME));
        config.put("autoCommit", configSource.getOrDefault(prefix + "autoCommit", DEFAULT_AUTO_COMMIT));
        config.put("connectionTestQuery", configSource.getOrDefault(prefix + "connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY));
        config.put("leakDetectionThreshold", configSource.getOrDefault(prefix + "leakDetectionThreshold", DEFAULT_LEAK_DETECTION_THRESHOLD));
        config.put("registerMbeans", configSource.getOrDefault(prefix + "registerMbeans", DEFAULT_REGISTER_MBEANS));
        config.put("poolName", poolName + "HikariPool");
        
        return config;
    }
    
    /**
     * Gets a configuration map for a payment connection pool from the ConfigSource.
     * 
     * @return Map of connection pool configuration parameters optimized for payment processing
     */
    public Map<String, Object> getPaymentConnectionPoolConfigFromSource() {
        Map<String, Object> config = new HashMap<>();
        
        String prefix = "payment.database.connectionPool.";
        
        config.put("maximumPoolSize", configSource.getOrDefault(prefix + "maximumPoolSize", PAYMENT_DEFAULT_MAXIMUM_POOL_SIZE));
        config.put("minimumIdle", configSource.getOrDefault(prefix + "minimumIdle", PAYMENT_DEFAULT_MINIMUM_IDLE));
        config.put("connectionTimeout", configSource.getOrDefault(prefix + "connectionTimeout", PAYMENT_DEFAULT_CONNECTION_TIMEOUT));
        config.put("idleTimeout", configSource.getOrDefault(prefix + "idleTimeout", PAYMENT_DEFAULT_IDLE_TIMEOUT));
        config.put("maxLifetime", configSource.getOrDefault(prefix + "maxLifetime", PAYMENT_DEFAULT_MAX_LIFETIME));
        config.put("autoCommit", configSource.getOrDefault(prefix + "autoCommit", PAYMENT_DEFAULT_AUTO_COMMIT));
        config.put("connectionTestQuery", configSource.getOrDefault(prefix + "connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY));
        config.put("leakDetectionThreshold", configSource.getOrDefault(prefix + "leakDetectionThreshold", PAYMENT_DEFAULT_LEAK_DETECTION_THRESHOLD));
        config.put("registerMbeans", configSource.getOrDefault(prefix + "registerMbeans", PAYMENT_DEFAULT_REGISTER_MBEANS));
        config.put("poolName", "PaymentHikariPool");
        
        return config;
    }
    
    /**
     * Gets an integer value from a map with a default fallback.
     * 
     * @param map The map to get the value from
     * @param key The key to look up
     * @param defaultValue The default value to use if the key is not found or not an integer
     * @return The integer value
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
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
    private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
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
    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}