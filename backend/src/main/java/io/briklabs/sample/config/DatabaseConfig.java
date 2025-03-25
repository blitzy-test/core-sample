package io.briklabs.sample.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Interface defining database configuration requirements.
 * Implementations must provide database connection parameters and may optionally
 * provide connection pool configuration for HikariCP.
 */
public interface DatabaseConfig {

	/**
	 * Gets the database URL.
	 * 
	 * @return Database URL string
	 */
	String getDatabaseURL();

	/**
	 * Gets the database username.
	 * 
	 * @return Database username
	 */
	String getDatabaseUsername();

	/**
	 * Gets the database password.
	 * 
	 * @return Database password
	 */
	String getDatabasePassword();

	/**
	 * Gets the database schema.
	 * 
	 * @return Database schema name
	 */
	String getDatabaseSchema();
	
	/**
	 * Gets the connection pool configuration parameters.
	 * Implementations should provide appropriate connection pool settings
	 * based on their specific requirements, particularly for payment processing
	 * which may require optimized connection pool parameters.
	 * 
	 * @return Map of connection pool configuration parameters, or empty Optional if not supported
	 */
	default Optional<Map<String, Object>> getConnectionPoolConfig() {
		return Optional.empty();
	}
	
	/**
	 * Validates the database connection parameters by attempting to establish a connection.
	 * This is a default method that can be overridden by implementations for custom validation.
	 * 
	 * @return true if connection parameters are valid, false otherwise
	 */
	default boolean validateConnectionParameters() {
		try (Connection conn = DriverManager.getConnection(
				getDatabaseURL(), 
				getDatabaseUsername(), 
				getDatabasePassword())) {
			return conn.isValid(5);
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Utility method to get a required environment variable.
	 * 
	 * @param env Environment variable name
	 * @return Environment variable value
	 * @throws NullPointerException if the environment variable is not set
	 */
	static String requiredEnv(String env) {
		return Objects.requireNonNull(System.getenv(env), 
				() -> "Missing required environment variable " + env);
	}

	/**
	 * Utility method to get an environment variable with a default value.
	 * 
	 * @param env Environment variable name
	 * @param defaultValue Default value to use if environment variable is not set
	 * @return Environment variable value or default value
	 */
	static String envOrDefault(String env, String defaultValue) {
		return Objects.requireNonNullElse(System.getenv(env), defaultValue);
	}
}