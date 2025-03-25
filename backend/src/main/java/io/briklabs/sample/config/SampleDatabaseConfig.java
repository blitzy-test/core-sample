package io.briklabs.sample.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of DatabaseConfig for the sample database.
 * Provides database connection parameters and connection pool configuration.
 */
public class SampleDatabaseConfig implements DatabaseConfig {

	private ConfigSource cs;

	/**
	 * Constructs a new SampleDatabaseConfig with the specified ConfigSource.
	 * 
	 * @param cs The configuration source
	 */
	public SampleDatabaseConfig(ConfigSource cs) {
		this.cs = cs;
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
	 * Gets the connection pool configuration parameters for the sample database.
	 * Retrieves connection pool settings from the configuration source.
	 * 
	 * @return Optional containing a map of connection pool configuration parameters
	 */
	@Override
	public Optional<Map<String, Object>> getConnectionPoolConfig() {
		// Check if connection pooling is enabled for this database
		if (!cs.getOrDefault("brik.database.sample.connectionPool.enabled", false)) {
			return Optional.empty();
		}
		
		// Get connection pool configuration from ConfigSource
		Map<String, Object> poolConfig = new HashMap<>(cs.getConnectionPoolConfig("sample"));
		
		// Add database-specific overrides if needed
		String connectionTestQuery = cs.getOrDefault("brik.database.sample.connectionPool.connectionTestQuery", "SELECT 1");
		poolConfig.put("connectionTestQuery", connectionTestQuery);
		
		// Set pool name for monitoring and troubleshooting
		poolConfig.put("poolName", "SampleHikariPool");
		
		return Optional.of(poolConfig);
	}
	
	/**
	 * Validates the database connection parameters.
	 * Uses the default implementation from DatabaseConfig interface.
	 * 
	 * @return true if connection parameters are valid, false otherwise
	 */
	@Override
	public boolean validateConnectionParameters() {
		// Use the default implementation from DatabaseConfig
		return DatabaseConfig.super.validateConnectionParameters();
	}
}