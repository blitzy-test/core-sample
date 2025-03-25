package io.briklabs.sample.payments.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.config.PaymentDatabaseConfig;
import io.briklabs.sample.config.HikariConfigurationProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Configures and initializes the HikariCP connection pool specifically for payment operations.
 * This class manages database connection parameters, pool sizing, timeouts, and health monitoring
 * settings optimized for payment transaction processing.
 */
public class HikariCPConfig {
    private static final Logger logger = LoggerFactory.getLogger(HikariCPConfig.class);
    
    // Default connection pool parameters optimized for payment processing
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 40;
    private static final int DEFAULT_MINIMUM_IDLE = 10;
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 20000; // 20 seconds
    private static final long DEFAULT_VALIDATION_TIMEOUT_MS = 5000; // 5 seconds
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 300000; // 5 minutes
    private static final long DEFAULT_MAX_LIFETIME_MS = 1200000; // 20 minutes
    private static final boolean DEFAULT_AUTO_COMMIT = false;
    private static final String DEFAULT_CONNECTION_TEST_QUERY = "SELECT 1";
    private static final long DEFAULT_LEAK_DETECTION_THRESHOLD_MS = 60000; // 60 seconds
    private static final boolean DEFAULT_REGISTER_MBEANS = true;
    
    private final ConfigSource configSource;
    private final DatabaseConfig databaseConfig;
    private final HikariConfigurationProvider hikariConfigProvider;
    private HikariDataSource dataSource;
    
    /**
     * Constructs a new HikariCPConfig with the specified ConfigSource.
     * Uses the PaymentDatabaseConfig for database connection parameters.
     * 
     * @param configSource The configuration source
     */
    public HikariCPConfig(ConfigSource configSource) {
        this.configSource = configSource;
        this.databaseConfig = new PaymentDatabaseConfig(configSource);
        this.hikariConfigProvider = new HikariConfigurationProvider(configSource);
        
        logger.info("Initializing HikariCP connection pool for payment operations");
        initializeDataSource();
    }
    
    /**
     * Constructs a new HikariCPConfig with the specified DatabaseConfig.
     * 
     * @param databaseConfig The database configuration
     * @param configSource The configuration source
     */
    public HikariCPConfig(DatabaseConfig databaseConfig, ConfigSource configSource) {
        this.configSource = configSource;
        this.databaseConfig = databaseConfig;
        this.hikariConfigProvider = new HikariConfigurationProvider(configSource);
        
        logger.info("Initializing HikariCP connection pool for payment operations with custom database config");
        initializeDataSource();
    }
    
    /**
     * Initializes the HikariCP data source with optimized settings for payment processing.
     */
    private void initializeDataSource() {
        try {
            // Validate database connection parameters
            if (!databaseConfig.validateConnectionParameters()) {
                throw new IllegalStateException("Payment database connection validation failed");
            }
            
            // Create HikariConfig with payment-optimized settings
            HikariConfig config = createHikariConfig();
            
            // Create the data source
            dataSource = new HikariDataSource(config);
            
            logger.info("Payment HikariCP connection pool initialized successfully with maximumPoolSize={}, minimumIdle={}",
                    config.getMaximumPoolSize(), config.getMinimumIdle());
        } catch (Exception e) {
            logger.error("Failed to initialize payment HikariCP connection pool", e);
            throw new RuntimeException("Failed to initialize payment database connection pool", e);
        }
    }
    
    /**
     * Creates a HikariConfig with payment-optimized settings.
     * 
     * @return A configured HikariConfig object
     */
    private HikariConfig createHikariConfig() {
        // Try to use the HikariConfigurationProvider first
        if (hikariConfigProvider != null) {
            return hikariConfigProvider.createPaymentHikariConfig(databaseConfig);
        }
        
        // Fall back to manual configuration if provider is not available
        HikariConfig config = new HikariConfig();
        
        // Set basic connection properties
        config.setJdbcUrl(databaseConfig.getDatabaseURL());
        config.setUsername(databaseConfig.getDatabaseUsername());
        config.setPassword(databaseConfig.getDatabasePassword());
        config.setSchema(databaseConfig.getDatabaseSchema());
        
        // Get connection pool configuration from DatabaseConfig if available
        Map<String, Object> poolConfig = databaseConfig.getConnectionPoolConfig()
                .orElseGet(() -> configSource.getPaymentConnectionPoolConfig());
        
        // Set connection pool parameters with payment-optimized defaults
        config.setMaximumPoolSize(getIntValue(poolConfig, "maximumPoolSize", DEFAULT_MAXIMUM_POOL_SIZE));
        config.setMinimumIdle(getIntValue(poolConfig, "minimumIdle", DEFAULT_MINIMUM_IDLE));
        config.setConnectionTimeout(getLongValue(poolConfig, "connectionTimeout", DEFAULT_CONNECTION_TIMEOUT_MS));
        config.setValidationTimeout(getLongValue(poolConfig, "validationTimeout", DEFAULT_VALIDATION_TIMEOUT_MS));
        config.setIdleTimeout(getLongValue(poolConfig, "idleTimeout", DEFAULT_IDLE_TIMEOUT_MS));
        config.setMaxLifetime(getLongValue(poolConfig, "maxLifetime", DEFAULT_MAX_LIFETIME_MS));
        config.setAutoCommit(getBooleanValue(poolConfig, "autoCommit", DEFAULT_AUTO_COMMIT));
        config.setConnectionTestQuery(getStringValue(poolConfig, "connectionTestQuery", DEFAULT_CONNECTION_TEST_QUERY));
        
        // Set leak detection threshold
        long leakDetectionThreshold = getLongValue(poolConfig, "leakDetectionThreshold", DEFAULT_LEAK_DETECTION_THRESHOLD_MS);
        if (leakDetectionThreshold > 0) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
        }
        
        // Enable JMX monitoring for operational visibility
        config.setRegisterMbeans(getBooleanValue(poolConfig, "registerMbeans", DEFAULT_REGISTER_MBEANS));
        
        // Set pool name for monitoring and troubleshooting
        config.setPoolName("PaymentHikariPool");
        
        // Configure additional PostgreSQL-specific properties
        configurePostgresProperties(config);
        
        return config;
    }
    
    /**
     * Configures PostgreSQL-specific properties for the HikariConfig.
     * 
     * @param config The HikariConfig to configure
     */
    private void configurePostgresProperties(HikariConfig config) {
        // Set PostgreSQL-specific properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // Enable SSL if configured
        boolean sslEnabled = configSource.getPaymentConfigBoolean("database.ssl.enabled", true);
        if (sslEnabled) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "verify-full");
            
            // Use appropriate SSL factory based on environment
            String environment = configSource.getOrDefault("brik.environment", "development");
            if ("production".equals(environment)) {
                config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
            } else {
                config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
            }
        }
    }
    
    /**
     * Gets the HikariDataSource for payment database operations.
     * 
     * @return The configured HikariDataSource
     */
    public HikariDataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }
        return dataSource;
    }
    
    /**
     * Closes the HikariDataSource, releasing all resources.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing payment HikariCP connection pool");
            dataSource.close();
        }
    }
    
    /**
     * Gets the current health status of the connection pool.
     * 
     * @return A string representation of the pool health
     */
    public String getHealthStatus() {
        if (dataSource == null || dataSource.isClosed()) {
            return "CLOSED";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("ACTIVE: ");
        status.append("Total=").append(dataSource.getHikariPoolMXBean().getTotalConnections());
        status.append(", Active=").append(dataSource.getHikariPoolMXBean().getActiveConnections());
        status.append(", Idle=").append(dataSource.getHikariPoolMXBean().getIdleConnections());
        status.append(", Waiting=").append(dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        
        return status.toString();
    }
    
    /**
     * Gets the maximum pool size.
     * 
     * @return The maximum number of connections in the pool
     */
    public int getMaximumPoolSize() {
        return dataSource.getHikariConfigMXBean().getMaximumPoolSize();
    }
    
    /**
     * Gets the current number of active connections.
     * 
     * @return The number of active connections
     */
    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }
    
    /**
     * Gets the current number of idle connections.
     * 
     * @return The number of idle connections
     */
    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }
    
    /**
     * Gets the current number of threads waiting for a connection.
     * 
     * @return The number of threads waiting for a connection
     */
    public int getThreadsAwaitingConnection() {
        return dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
    }
    
    /**
     * Gets the connection timeout in milliseconds.
     * 
     * @return The connection timeout in milliseconds
     */
    public long getConnectionTimeout() {
        return dataSource.getHikariConfigMXBean().getConnectionTimeout();
    }
    
    /**
     * Gets the validation timeout in milliseconds.
     * 
     * @return The validation timeout in milliseconds
     */
    public long getValidationTimeout() {
        return dataSource.getHikariConfigMXBean().getValidationTimeout();
    }
    
    /**
     * Gets the leak detection threshold in milliseconds.
     * 
     * @return The leak detection threshold in milliseconds
     */
    public long getLeakDetectionThreshold() {
        return dataSource.getHikariConfigMXBean().getLeakDetectionThreshold();
    }
    
    /**
     * Formats a duration in a human-readable format.
     * 
     * @param millis Duration in milliseconds
     * @return Human-readable duration string
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + seconds + "s";
        }
        
        long hours = TimeUnit.MINUTES.toHours(minutes);
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
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
    
    /**
     * Gets a string value from a map with a default fallback.
     * 
     * @param map The map to get the value from
     * @param key The key to look up
     * @param defaultValue The default value to use if the key is not found or not a string
     * @return The string value
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : defaultValue;
    }
}