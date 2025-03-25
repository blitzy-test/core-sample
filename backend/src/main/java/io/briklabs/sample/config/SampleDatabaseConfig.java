package io.briklabs.sample.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DatabaseConfig for the sample database.
 * Provides database connection details and connection pool configuration
 * for the sample database.
 */
public class SampleDatabaseConfig implements DatabaseConfig {

	private static final Logger logger = LoggerFactory.getLogger(SampleDatabaseConfig.class);
	private final ConfigSource cs;
	private final HikariConfigurationProvider hikariProvider;

	/**
	 * Constructs a new SampleDatabaseConfig with the specified ConfigSource.
	 * 
	 * @param cs The configuration source
	 */
	public SampleDatabaseConfig(ConfigSource cs) {
		this.cs = cs;
		this.hikariProvider = new HikariConfigurationProvider(cs);
		logger.debug("Initializing SampleDatabaseConfig with ConfigSource");
	}

	@Override
	public String getDatabaseURL() {
		return cs.getRequired("brik.database.sample.url");
	}

	@Override
	public String getDatabaseUsername() {
		return cs.getRequired("brik.database.sample.user");
	}

	@Override
	public String getDatabasePassword() {
		return cs.getRequired("brik.database.sample.password");
	}

	@Override
	public String getDatabaseSchema() {
		return "sample";
	}
	
	/**
	 * Gets the connection pool configuration parameters.
	 * Retrieves connection pool settings from the configuration source
	 * or uses default values if not specified.
	 * 
	 * @return Optional containing a map of connection pool configuration parameters
	 */
	@Override
	public Optional<Map<String, Object>> getConnectionPoolConfig() {
		logger.debug("Getting connection pool configuration for sample database");
		
		// Check if connection pooling is enabled
		boolean poolingEnabled = cs.getOrDefault("brik.database.sample.connectionPool.enabled", true);
		if (!poolingEnabled) {
			logger.info("Connection pooling is disabled for sample database");
			return Optional.empty();
		}
		
		// Get connection pool configuration from ConfigSource
		Map<String, Object> poolConfig = new HashMap<>();
		
		// Try to get configuration from HikariConfigurationProvider first
		try {
			poolConfig = hikariProvider.getConnectionPoolConfigFromSource("sample");
			logger.debug("Retrieved connection pool configuration from HikariConfigurationProvider");
		} catch (Exception e) {
			logger.warn("Failed to get connection pool configuration from HikariConfigurationProvider: {}", e.getMessage());
			
			// Fallback to direct configuration retrieval
			poolConfig = cs.getConnectionPoolConfig("sample");
			logger.debug("Retrieved connection pool configuration directly from ConfigSource");
		}
		
		// Add database-specific settings if needed
		String poolName = (String) poolConfig.getOrDefault("poolName", "SampleHikariPool");
		poolConfig.put("poolName", poolName);
		
		return Optional.of(poolConfig);
	}
	
	/**
	 * Validates the database connection parameters.
	 * Checks that all required parameters are present and valid.
	 * 
	 * @return true if connection parameters are valid, false otherwise
	 */
	@Override
	public boolean validateConnectionParameters() {
		try {
			// Validate basic connection parameters
			String url = getDatabaseURL();
			String username = getDatabaseUsername();
			String password = getDatabasePassword();
			String schema = getDatabaseSchema();
			
			boolean valid = url != null && !url.isEmpty() &&
					        username != null && !username.isEmpty() &&
					        password != null &&
					        schema != null && !schema.isEmpty();
			
			if (!valid) {
				logger.error("Invalid database connection parameters: missing required values");
				return false;
			}
			
			// Validate URL format
			if (!url.startsWith("jdbc:postgresql://")) {
				logger.error("Invalid database URL format: {}", url);
				return false;
			}
			
			// Validate connection pool configuration if present
			Optional<Map<String, Object>> poolConfig = getConnectionPoolConfig();
			if (poolConfig.isPresent()) {
				Map<String, Object> config = poolConfig.get();
				
				// Validate pool size parameters
				int maxPoolSize = getIntValue(config, "maximumPoolSize", 10);
				int minIdle = getIntValue(config, "minimumIdle", 5);
				
				if (minIdle > maxPoolSize) {
					logger.error("Invalid connection pool configuration: minimumIdle ({}) > maximumPoolSize ({})", 
							minIdle, maxPoolSize);
					return false;
				}
				
				// Validate timeout parameters
				long connectionTimeout = getLongValue(config, "connectionTimeout", 30000);
				long idleTimeout = getLongValue(config, "idleTimeout", 600000);
				long maxLifetime = getLongValue(config, "maxLifetime", 1800000);
				
				if (connectionTimeout <= 0 || idleTimeout <= 0 || maxLifetime <= 0) {
					logger.error("Invalid connection pool timeout values: all timeout values must be positive");
					return false;
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
			
			logger.debug("Database connection parameters validated successfully");
			return true;
		} catch (Exception e) {
			logger.error("Error validating database connection parameters: {}", e.getMessage(), e);
			return false;
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