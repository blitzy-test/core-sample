package io.briklabs.sample.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Interface defining database configuration parameters.
 * Implementations provide database connection details and connection pool configuration.
 */
public interface DatabaseConfig {

	/**
	 * Gets the database URL.
	 * 
	 * @return The database connection URL
	 */
	String getDatabaseURL();

	/**
	 * Gets the database username.
	 * 
	 * @return The database username
	 */
	String getDatabaseUsername();

	/**
	 * Gets the database password.
	 * 
	 * @return The database password
	 */
	String getDatabasePassword();

	/**
	 * Gets the database schema.
	 * 
	 * @return The database schema name
	 */
	String getDatabaseSchema();
	
	/**
	 * Gets the connection pool configuration parameters.
	 * Implementing classes should provide connection pool settings for HikariCP,
	 * including pool size, timeout values, and other connection properties.
	 * 
	 * <p>Payment database implementations should configure optimized connection
	 * pool parameters for transaction processing, including increased pool size
	 * and appropriate timeout values.</p>
	 * 
	 * @return Optional containing a map of connection pool configuration parameters,
	 *         or empty if connection pooling is not configured
	 */
	default Optional<Map<String, Object>> getConnectionPoolConfig() {
		// Default implementation returns empty, indicating no connection pool configuration
		return Optional.empty();
	}
	
	/**
	 * Validates the database connection parameters.
	 * Implementing classes can override this method to provide custom validation logic.
	 * 
	 * @return true if connection parameters are valid, false otherwise
	 */
	default boolean validateConnectionParameters() {
		// Basic validation to ensure required parameters are not null or empty
		try {
			String url = getDatabaseURL();
			String username = getDatabaseUsername();
			String password = getDatabasePassword();
			String schema = getDatabaseSchema();
			
			return url != null && !url.isEmpty() &&
				   username != null && !username.isEmpty() &&
				   password != null &&
				   schema != null && !schema.isEmpty();
		} catch (Exception e) {
			// If any exception occurs during validation, consider parameters invalid
			return false;
		}
	}

	/**
	 * Utility method to get a required environment variable.
	 * 
	 * @param env The environment variable name
	 * @return The environment variable value
	 * @throws NullPointerException if the environment variable is not set
	 */
	static String requiredEnv(String env) {
		return Objects.requireNonNull(System.getenv(env), () -> "Missing required environment variable " + env);
	}

	/**
	 * Utility method to get an environment variable with a default value.
	 * 
	 * @param env The environment variable name
	 * @param defaultValue The default value to use if the environment variable is not set
	 * @return The environment variable value or the default value
	 */
	static String envOrDefault(String env, String defaultValue) {
		return Objects.requireNonNullElse(System.getenv(env), defaultValue);
	}
}