package io.briklabs.sample.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for HikariCP connection pool configuration.
 * This class centralizes the creation and configuration of HikariCP connection pools
 * used throughout the application, with specialized support for payment transaction processing.
 */
public class HikariConfigurationProvider {

    private static final Logger logger = LoggerFactory.getLogger(HikariConfigurationProvider.class);
    private final ConfigSource configSource;

    /**
     * Default connection pool parameters for general database operations.
     */
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 10;
    private static final int DEFAULT_MINIMUM_IDLE = 5;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10 minutes
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30 minutes
    private static final boolean DEFAULT_AUTO_COMMIT = true;
    private static final String DEFAULT_CONNECTION_TEST_QUERY = "SELECT 1";
    private static final long DEFAULT_LEAK_DETECTION_THRESHOLD = 0; // disabled
    private static final boolean DEFAULT_REGISTER_MBEANS = false;

    /**
     * Optimized connection pool parameters for payment transaction processing.
     */
    private static final int PAYMENT_MAXIMUM_POOL_SIZE = 30;
    private static final int PAYMENT_MINIMUM_IDLE = 10;
    private static final long PAYMENT_CONNECTION_TIMEOUT = 20000; // 20 seconds
    private static final long PAYMENT_IDLE_TIMEOUT = 300000; // 5 minutes
    private static final long PAYMENT_MAX_LIFETIME = 1200000; // 20 minutes
    private static final boolean PAYMENT_AUTO_COMMIT = false;
    private static final String PAYMENT_CONNECTION_TEST_QUERY = "SELECT 1";
    private static final long PAYMENT_LEAK_DETECTION_THRESHOLD = 60000; // 1 minute
    private static final boolean PAYMENT_REGISTER_MBEANS = true;
    private static final String PAYMENT_POOL_NAME = "PaymentHikariPool";

    /**
     * Constructs a new HikariConfigurationProvider with the specified ConfigSource.
     *
     * @param configSource The configuration source
     */
    public HikariConfigurationProvider(ConfigSource configSource) {
        this.configSource = configSource;
        logger.debug("Initializing HikariConfigurationProvider");
    }

    /**
     * Gets the default connection pool configuration.
     * This configuration is suitable for general database operations.
     *
     * @return Map of connection pool configuration parameters
     */
    public Map<String, Object> getDefaultConnectionPoolConfig() {
        logger.debug("Creating default connection pool configuration");
        Map<String, Object> config = new HashMap<>();

        // Set default connection pool parameters
        config.put("maximumPoolSize", DEFAULT_MAXIMUM_POOL_SIZE);
        config.put("minimumIdle", DEFAULT_MINIMUM_IDLE);
        config.put("connectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        config.put("idleTimeout", DEFAULT_IDLE_TIMEOUT);
        config.put("maxLifetime", DEFAULT_MAX_LIFETIME);
        config.put("autoCommit", DEFAULT_AUTO_COMMIT);
        config.put("connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY);
        config.put("leakDetectionThreshold", DEFAULT_LEAK_DETECTION_THRESHOLD);
        config.put("registerMbeans", DEFAULT_REGISTER_MBEANS);
        config.put("poolName", "DefaultHikariPool");

        return config;
    }

    /**
     * Gets the payment-specific connection pool configuration.
     * This configuration is optimized for payment transaction processing with
     * increased pool size, adjusted timeouts, and enhanced monitoring.
     *
     * @return Map of connection pool configuration parameters optimized for payment processing
     */
    public Map<String, Object> getPaymentConnectionPoolConfig() {
        logger.debug("Creating payment-specific connection pool configuration");
        Map<String, Object> config = new HashMap<>();

        // Set payment-optimized connection pool parameters
        config.put("maximumPoolSize", PAYMENT_MAXIMUM_POOL_SIZE);
        config.put("minimumIdle", PAYMENT_MINIMUM_IDLE);
        config.put("connectionTimeout", PAYMENT_CONNECTION_TIMEOUT);
        config.put("idleTimeout", PAYMENT_IDLE_TIMEOUT);
        config.put("maxLifetime", PAYMENT_MAX_LIFETIME);
        config.put("autoCommit", PAYMENT_AUTO_COMMIT);
        config.put("connectionTestQuery", PAYMENT_CONNECTION_TEST_QUERY);
        config.put("leakDetectionThreshold", PAYMENT_LEAK_DETECTION_THRESHOLD);
        config.put("registerMbeans", PAYMENT_REGISTER_MBEANS);
        config.put("poolName", PAYMENT_POOL_NAME);

        // Add additional payment-specific parameters
        config.put("initializationFailTimeout", 10000); // 10 seconds
        config.put("validationTimeout", 5000); // 5 seconds
        config.put("keepaliveTime", 60000); // 1 minute

        return config;
    }

    /**
     * Gets connection pool configuration from the configuration source for a specific database.
     * This method retrieves configuration values from the ConfigSource, falling back to
     * default values if specific parameters are not defined.
     *
     * @param dbName Database name (e.g., "sample", "payment")
     * @return Map of connection pool configuration parameters
     */
    public Map<String, Object> getConnectionPoolConfigFromSource(String dbName) {
        logger.debug("Retrieving connection pool configuration for database: {}", dbName);
        Map<String, Object> config = new HashMap<>();
        
        String prefix = "brik.database." + dbName + ".connectionPool.";
        
        // Get connection pool parameters with defaults
        config.put("maximumPoolSize", getIntConfig(prefix + "maximumPoolSize", DEFAULT_MAXIMUM_POOL_SIZE));
        config.put("minimumIdle", getIntConfig(prefix + "minimumIdle", DEFAULT_MINIMUM_IDLE));
        config.put("connectionTimeout", getLongConfig(prefix + "connectionTimeout", DEFAULT_CONNECTION_TIMEOUT));
        config.put("idleTimeout", getLongConfig(prefix + "idleTimeout", DEFAULT_IDLE_TIMEOUT));
        config.put("maxLifetime", getLongConfig(prefix + "maxLifetime", DEFAULT_MAX_LIFETIME));
        config.put("autoCommit", getBooleanConfig(prefix + "autoCommit", DEFAULT_AUTO_COMMIT));
        config.put("connectionTestQuery", getStringConfig(prefix + "connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY));
        config.put("leakDetectionThreshold", getLongConfig(prefix + "leakDetectionThreshold", DEFAULT_LEAK_DETECTION_THRESHOLD));
        config.put("registerMbeans", getBooleanConfig(prefix + "registerMbeans", DEFAULT_REGISTER_MBEANS));
        config.put("poolName", getStringConfig(prefix + "poolName", dbName + "HikariPool"));
        
        // Add optional parameters if configured
        addIfConfigured(config, prefix + "initializationFailTimeout", "initializationFailTimeout");
        addIfConfigured(config, prefix + "validationTimeout", "validationTimeout");
        addIfConfigured(config, prefix + "keepaliveTime", "keepaliveTime");
        addIfConfigured(config, prefix + "maxPoolSize", "maxPoolSize"); // Alias for maximumPoolSize
        
        // Load additional properties from external file if specified
        loadExternalProperties(config, dbName);
        
        // Validate configuration
        validateConnectionPoolConfig(config);
        
        return config;
    }

    /**
     * Gets a customized connection pool configuration with specific overrides.
     * This method allows for customization of specific parameters while using defaults for others.
     *
     * @param baseConfig Base configuration to start with (default or payment)
     * @param overrides Map of parameters to override
     * @return Customized connection pool configuration
     */
    public Map<String, Object> getCustomizedConnectionPoolConfig(Map<String, Object> baseConfig, Map<String, Object> overrides) {
        Map<String, Object> config = new HashMap<>(baseConfig);
        
        // Apply overrides
        if (overrides != null) {
            for (Map.Entry<String, Object> entry : overrides.entrySet()) {
                config.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Validate the customized configuration
        validateConnectionPoolConfig(config);
        
        return config;
    }

    /**
     * Creates a connection pool configuration for a specific database with custom settings.
     * This method combines configuration from the source with custom overrides.
     *
     * @param dbName Database name
     * @param customSettings Custom settings to apply
     * @return Connection pool configuration
     */
    public Map<String, Object> createConnectionPoolConfig(String dbName, Map<String, Object> customSettings) {
        // Get base configuration from source
        Map<String, Object> baseConfig = getConnectionPoolConfigFromSource(dbName);
        
        // Apply custom settings
        return getCustomizedConnectionPoolConfig(baseConfig, customSettings);
    }

    /**
     * Validates a connection pool configuration.
     * Checks for required parameters and valid values.
     *
     * @param config Connection pool configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validateConnectionPoolConfig(Map<String, Object> config) {
        // Check for required parameters
        if (!config.containsKey("maximumPoolSize")) {
            throw new IllegalArgumentException("Missing required connection pool parameter: maximumPoolSize");
        }
        
        // Validate parameter values
        int maxPoolSize = getIntValue(config, "maximumPoolSize", DEFAULT_MAXIMUM_POOL_SIZE);
        int minIdle = getIntValue(config, "minimumIdle", DEFAULT_MINIMUM_IDLE);
        
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
        
        // Validate timeout parameters
        long connectionTimeout = getLongValue(config, "connectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        long idleTimeout = getLongValue(config, "idleTimeout", DEFAULT_IDLE_TIMEOUT);
        long maxLifetime = getLongValue(config, "maxLifetime", DEFAULT_MAX_LIFETIME);
        
        if (connectionTimeout <= 0) {
            throw new IllegalArgumentException("Invalid connectionTimeout: " + connectionTimeout + " (must be > 0)");
        }
        
        if (idleTimeout <= 0) {
            throw new IllegalArgumentException("Invalid idleTimeout: " + idleTimeout + " (must be > 0)");
        }
        
        if (maxLifetime <= 0) {
            throw new IllegalArgumentException("Invalid maxLifetime: " + maxLifetime + " (must be > 0)");
        }
        
        if (idleTimeout < connectionTimeout) {
            logger.warn("Suboptimal connection pool configuration: idleTimeout ({}) < connectionTimeout ({})", 
                    idleTimeout, connectionTimeout);
        }
        
        if (maxLifetime <= idleTimeout) {
            logger.warn("Suboptimal connection pool configuration: maxLifetime ({}) <= idleTimeout ({})", 
                    maxLifetime, idleTimeout);
        }
    }

    /**
     * Loads additional properties from an external file if specified.
     *
     * @param config Configuration map to update
     * @param dbName Database name
     */
    private void loadExternalProperties(Map<String, Object> config, String dbName) {
        String hikariConfigFile = configSource.getOrDefault("brik.database." + dbName + ".hikaricp.configFile", null);
        if (hikariConfigFile == null) {
            // Try generic config file
            hikariConfigFile = configSource.getOrDefault("brik.hikaricp.configFile", null);
        }
        
        if (hikariConfigFile != null) {
            try {
                Properties hikariProps = new Properties();
                hikariProps.load(getClass().getClassLoader().getResourceAsStream(hikariConfigFile));
                
                // Add properties to config map
                for (String propName : hikariProps.stringPropertyNames()) {
                    if (!config.containsKey(propName)) {
                        config.put(propName, hikariProps.getProperty(propName));
                    }
                }
                
                logger.info("Loaded additional HikariCP properties from {}", hikariConfigFile);
            } catch (Exception e) {
                logger.warn("Failed to load HikariCP properties from {}: {}", hikariConfigFile, e.getMessage());
            }
        }
    }

    /**
     * Adds a configuration parameter if it exists in the configuration source.
     *
     * @param config Configuration map to update
     * @param sourceKey Key in the configuration source
     * @param targetKey Key in the configuration map
     */
    private void addIfConfigured(Map<String, Object> config, String sourceKey, String targetKey) {
        String value = configSource.get(sourceKey);
        if (value != null) {
            try {
                // Try to parse as numeric or boolean values
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    config.put(targetKey, Boolean.parseBoolean(value));
                } else if (value.matches("\\d+")) {
                    // Check if it's a long or int
                    if (value.length() > 9) {
                        config.put(targetKey, Long.parseLong(value));
                    } else {
                        config.put(targetKey, Integer.parseInt(value));
                    }
                } else {
                    config.put(targetKey, value);
                }
            } catch (NumberFormatException e) {
                // If parsing fails, store as string
                config.put(targetKey, value);
            }
        }
    }

    /**
     * Gets an integer configuration value.
     *
     * @param key Configuration key
     * @param defaultValue Default value
     * @return Configuration value as integer
     */
    private int getIntConfig(String key, int defaultValue) {
        return configSource.getOrDefault(key, defaultValue);
    }

    /**
     * Gets a long configuration value.
     *
     * @param key Configuration key
     * @param defaultValue Default value
     * @return Configuration value as long
     */
    private long getLongConfig(String key, long defaultValue) {
        return configSource.getOrDefault(key, defaultValue);
    }

    /**
     * Gets a boolean configuration value.
     *
     * @param key Configuration key
     * @param defaultValue Default value
     * @return Configuration value as boolean
     */
    private boolean getBooleanConfig(String key, boolean defaultValue) {
        return configSource.getOrDefault(key, defaultValue);
    }

    /**
     * Gets a string configuration value.
     *
     * @param key Configuration key
     * @param defaultValue Default value
     * @return Configuration value as string
     */
    private String getStringConfig(String key, String defaultValue) {
        return configSource.getOrDefault(key, defaultValue);
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
}