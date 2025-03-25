package io.briklabs.sample.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class ConfigSource {

	private static final Logger logger = LoggerFactory.getLogger(ConfigSource.class);
	private Map<String, Object> properties = new HashMap<>();

	/**
	 * Default constructor that loads configuration from the BRIK_CONFIG environment variable.
	 */
	public ConfigSource() {
		this(System.getenv("BRIK_CONFIG"));
	}

	/**
	 * Constructor that loads configuration from the specified path.
	 * Falls back to BRIK_CONFIG environment variable if path is null.
	 * 
	 * @param path Path to the configuration file
	 */
	public ConfigSource(String path) {
		String configPath = path != null ? path : System.getenv("BRIK_CONFIG");
		logger.debug("Loading config path={}", configPath);
		if (configPath != null) {
			try {
				FileInputStream fis = new FileInputStream(new File(configPath));

				Yaml yaml = new Yaml();
				properties = yaml.load(fis);
			} catch (FileNotFoundException e) {
				logger.warn("Cannot read the provided path {}, it will be ignored and other config sources will be attempted", configPath);
			}
		}
	}

	/**
	 * Constructor that loads configuration from the provided input stream.
	 * 
	 * @param is Input stream containing configuration data
	 */
	public ConfigSource(InputStream is) {
		Yaml yaml = new Yaml();
		properties = yaml.load(is);
	}

	/**
	 * Gets a configuration value by property path.
	 * Checks environment variables first, then falls back to the loaded configuration.
	 * 
	 * @param property Dot-separated property path
	 * @return The configuration value, or null if not found
	 */
	@SuppressWarnings("unchecked")
	public String get(String property) {
		String[] split = property.split("\\.");

		// Check if we have an environment variable overriding the value from the config
		// All . in the property name are converted to underscores, all letters are capitalized
		String envName = String.join("_", split).toUpperCase();

		String fromEnv = getEnv(envName);
		if (fromEnv != null) {
			logger.info("Found config property={} value={} env={}", property, fromEnv, envName);
			return fromEnv;
		}

		String result = null;

		// First see if we have the property loaded from an YAML file
		Map<String, Object> map = properties;
		for (int i = 0; i < split.length; i++) {
			String level = split[i];

			Object object = map.get(level);
			if (object == null) {
				logger.debug("No value found property={} envName={}", property, envName);
				return null;
			} else if (object instanceof Map) {
				map = (Map<String, Object>) object;
				continue;
			} else {
				if (i == split.length - 1) {
					result = String.valueOf(object);
					break;
				}
			}
		}

		logger.debug("Found config property={} value={}", property, result);
		return result;
	}

	/**
	 * Set the config value to a non-env derived config property to some value. Note, the value is not stored in the config, this is just an in-memory override.
	 * 
	 * Note, it's package protected, you need to explicitly subclass ConfigSource to use it
	 * 
	 * @param key Configuration key
	 * @param value Value to set
	 * @return true if the override was successful, false otherwise
	 */
	@SuppressWarnings("unchecked")
	boolean override(String key, String value) {
		String[] split = key.split("\\.");

		// Check if we have an environment variable overriding the value from the config
		// All . in the property name are converted to underscores, all letters are capitalized
		String envName = String.join("_", split).toUpperCase();

		String fromEnv = getEnv(envName);
		if (fromEnv != null) {
			logger.info("Cannot override env set config property={} to value={}, leaving it at env={}", key, value, fromEnv);
			return false;
		}

		Map<String, Object> map = properties;
		for (int i = 0; i < split.length; i++) {
			String level = split[i];

			Object object = map.get(level);
			if (object == null) {
				if (i == split.length - 1) {
					map.put(level, value);
					break;
				} else {
					object = new HashMap<String, Object>();
					map.put(level, object);
					map = (Map<String, Object>) object;
				}
			} else if (object instanceof Map) {
				map = (Map<String, Object>) object;
				continue;
			} else {
				if (i == split.length - 1) {
					map.put(level, value);
					break;
				}
			}
		}

		return true;
	}

	/**
	 * Gets a configuration value with a default fallback.
	 * 
	 * @param property Dot-separated property path
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value, or defaultValue if not found
	 */
	public String getOrDefault(String property, String defaultValue) {
		String result = get(property);
		return result != null ? result : defaultValue;
	}

	/**
	 * Gets an integer configuration value with a default fallback.
	 * 
	 * @param property Dot-separated property path
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value as an integer, or defaultValue if not found
	 */
	public int getOrDefault(String property, int defaultValue) {
		String value = get(property);
		return value != null ? Integer.parseInt(value) : defaultValue;
	}

	/**
	 * Gets a boolean configuration value with a default fallback.
	 * 
	 * @param property Dot-separated property path
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value as a boolean, or defaultValue if not found
	 */
	public boolean getOrDefault(String property, boolean defaultValue) {
		String value = get(property);
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}

	/**
	 * Gets a required configuration value, throwing an exception if not found.
	 * 
	 * @param property Dot-separated property path
	 * @return The configuration value
	 * @throws NullPointerException if the property is not found
	 */
	public String getRequired(String property) {
		return Objects.requireNonNull(get(property), () -> "Missing required config variable " + property);
	}

	/**
	 * Gets a long configuration value with a default fallback.
	 * 
	 * @param property Dot-separated property path
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value as a long, or defaultValue if not found
	 */
	public long getOrDefault(String property, long defaultValue) {
		String value = get(property);
		return value != null ? Long.parseLong(value) : defaultValue;
	}

	/**
	 * Gets a required integer configuration value, throwing an exception if not found.
	 * 
	 * @param property Dot-separated property path
	 * @return The configuration value as an integer
	 * @throws NullPointerException if the property is not found
	 * @throws NumberFormatException if the property cannot be parsed as an integer
	 */
	public int getRequiredInt(String property) {
		return Integer.parseInt(getRequired(property));
	}

	/**
	 * Gets a required long configuration value, throwing an exception if not found.
	 * 
	 * @param property Dot-separated property path
	 * @return The configuration value as a long
	 * @throws NullPointerException if the property is not found
	 * @throws NumberFormatException if the property cannot be parsed as a long
	 */
	public long getRequiredLong(String property) {
		return Long.parseLong(getRequired(property));
	}

	/**
	 * Gets a required boolean configuration value, throwing an exception if not found.
	 * 
	 * @param property Dot-separated property path
	 * @return The configuration value as a boolean
	 * @throws NullPointerException if the property is not found
	 */
	public boolean getRequiredBoolean(String property) {
		return Boolean.parseBoolean(getRequired(property));
	}

	/**
	 * Gets an environment variable.
	 * Protected to allow mocking in tests.
	 * 
	 * @param key Environment variable name
	 * @return The environment variable value, or null if not found
	 */
	protected String getEnv(String key) {
		return System.getenv(key);
	}

	/**
	 * Gets all environment variables.
	 * Protected to allow mocking in tests.
	 * 
	 * @return Map of all environment variables
	 */
	protected Map<String, String> getAllEnv() {
		return System.getenv();
	}

	/**
	 * Checks if a configuration section exists.
	 * 
	 * @param property Dot-separated property path
	 * @return true if the section exists, false otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean has(String property) {
		String[] split = property.split("\\.");
		String envName = String.join("_", split).toUpperCase();

		for (String key : getAllEnv().keySet()) {
			if (key.startsWith(envName)) {
				logger.info("Found config section={}", property);
				return true;
			}
		}

		boolean result = false;

		Map<String, Object> map = properties;
		for (int i = 0; i < split.length; i++) {
			String level = split[i];

			Object object = map.get(level);
			if (object == null) {
				logger.info("No value found property={} envName={}", property, envName);
				return false;
			} else if (object instanceof Map) {
				map = (Map<String, Object>) object;

				if (i == split.length - 1) {
					result = true;
					break;
				} else {
					continue;
				}
			} else {
				if (i == split.length - 1) {
					result = true;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Gets a payment-specific configuration value with a default fallback.
	 * Automatically prefixes the property with "payment."
	 * 
	 * @param property Dot-separated property path (without "payment." prefix)
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value, or defaultValue if not found
	 */
	public String getPaymentConfig(String property, String defaultValue) {
		return getOrDefault("payment." + property, defaultValue);
	}

	/**
	 * Gets a required payment-specific configuration value, throwing an exception if not found.
	 * Automatically prefixes the property with "payment."
	 * 
	 * @param property Dot-separated property path (without "payment." prefix)
	 * @return The configuration value
	 * @throws NullPointerException if the property is not found
	 */
	public String getRequiredPaymentConfig(String property) {
		return getRequired("payment." + property);
	}

	/**
	 * Gets a payment-specific integer configuration value with a default fallback.
	 * Automatically prefixes the property with "payment."
	 * 
	 * @param property Dot-separated property path (without "payment." prefix)
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value as an integer, or defaultValue if not found
	 */
	public int getPaymentConfigInt(String property, int defaultValue) {
		return getOrDefault("payment." + property, defaultValue);
	}

	/**
	 * Gets a required payment-specific integer configuration value, throwing an exception if not found.
	 * Automatically prefixes the property with "payment."
	 * 
	 * @param property Dot-separated property path (without "payment." prefix)
	 * @return The configuration value as an integer
	 * @throws NullPointerException if the property is not found
	 * @throws NumberFormatException if the property cannot be parsed as an integer
	 */
	public int getRequiredPaymentConfigInt(String property) {
		return getRequiredInt("payment." + property);
	}

	/**
	 * Gets a payment-specific boolean configuration value with a default fallback.
	 * Automatically prefixes the property with "payment."
	 * 
	 * @param property Dot-separated property path (without "payment." prefix)
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value as a boolean, or defaultValue if not found
	 */
	public boolean getPaymentConfigBoolean(String property, boolean defaultValue) {
		return getOrDefault("payment." + property, defaultValue);
	}

	/**
	 * Gets a payment-specific long configuration value with a default fallback.
	 * Automatically prefixes the property with "payment."
	 * 
	 * @param property Dot-separated property path (without "payment." prefix)
	 * @param defaultValue Default value to return if property is not found
	 * @return The configuration value as a long, or defaultValue if not found
	 */
	public long getPaymentConfigLong(String property, long defaultValue) {
		return getOrDefault("payment." + property, defaultValue);
	}

	/**
	 * Gets HikariCP connection pool parameters for the specified pool name.
	 * Supports both default and payment-specific connection pools.
	 * 
	 * @param poolName The name of the connection pool (e.g., "default", "payment")
	 * @return Map of connection pool parameters
	 */
	public Map<String, Object> getConnectionPoolConfig(String poolName) {
		Map<String, Object> poolConfig = new HashMap<>();
		
		// Base path for connection pool configuration
		String basePath = "database.connectionPool." + poolName + ".";
		
		// Load standard connection pool parameters with reasonable defaults
		poolConfig.put("maximumPoolSize", getOrDefault(basePath + "maximumPoolSize", 10));
		poolConfig.put("minimumIdle", getOrDefault(basePath + "minimumIdle", 5));
		poolConfig.put("connectionTimeout", getOrDefault(basePath + "connectionTimeout", 30000));
		poolConfig.put("idleTimeout", getOrDefault(basePath + "idleTimeout", 600000));
		poolConfig.put("maxLifetime", getOrDefault(basePath + "maxLifetime", 1800000));
		poolConfig.put("autoCommit", getOrDefault(basePath + "autoCommit", true));
		poolConfig.put("connectionTestQuery", getOrDefault(basePath + "connectionTestQuery", "SELECT 1"));
		poolConfig.put("leakDetectionThreshold", getOrDefault(basePath + "leakDetectionThreshold", 0));
		poolConfig.put("registerMbeans", getOrDefault(basePath + "registerMbeans", false));
		
		// Add pool name for monitoring and troubleshooting
		poolConfig.put("poolName", poolName + "HikariPool");
		
		return poolConfig;
	}

	/**
	 * Gets payment-specific HikariCP connection pool parameters.
	 * Uses optimized defaults for payment transaction processing.
	 * 
	 * @return Map of payment-specific connection pool parameters
	 */
	public Map<String, Object> getPaymentConnectionPoolConfig() {
		Map<String, Object> poolConfig = new HashMap<>();
		
		// Base path for payment connection pool configuration
		String basePath = "payment.database.connectionPool.";
		
		// Load payment-specific connection pool parameters with optimized defaults for payment processing
		poolConfig.put("maximumPoolSize", getOrDefault(basePath + "maximumPoolSize", 30));
		poolConfig.put("minimumIdle", getOrDefault(basePath + "minimumIdle", 10));
		poolConfig.put("connectionTimeout", getOrDefault(basePath + "connectionTimeout", 20000));
		poolConfig.put("idleTimeout", getOrDefault(basePath + "idleTimeout", 300000));
		poolConfig.put("maxLifetime", getOrDefault(basePath + "maxLifetime", 1200000));
		poolConfig.put("autoCommit", getOrDefault(basePath + "autoCommit", false));
		poolConfig.put("connectionTestQuery", getOrDefault(basePath + "connectionTestQuery", "SELECT 1"));
		poolConfig.put("leakDetectionThreshold", getOrDefault(basePath + "leakDetectionThreshold", 60000));
		poolConfig.put("registerMbeans", getOrDefault(basePath + "registerMbeans", true));
		
		// Add pool name for monitoring and troubleshooting
		poolConfig.put("poolName", "PaymentHikariPool");
		
		return poolConfig;
	}

	/**
	 * Checks if a payment feature is enabled.
	 * 
	 * @param featureName Name of the payment feature
	 * @param defaultValue Default value if not configured
	 * @return true if the feature is enabled, false otherwise
	 */
	public boolean isPaymentFeatureEnabled(String featureName, boolean defaultValue) {
		return getPaymentConfigBoolean("features." + featureName + ".enabled", defaultValue);
	}

	/**
	 * Validates payment configuration by checking required properties.
	 * 
	 * @return true if all required payment configuration is present, false otherwise
	 */
	public boolean validatePaymentConfig() {
		try {
			// Check essential payment database configuration
			getRequired("payment.database.url");
			getRequired("payment.database.username");
			getRequired("payment.database.password");
			getRequired("payment.database.schema");
			
			// Check payment module configuration
			getRequiredPaymentConfig("module.enabled");
			
			return true;
		} catch (NullPointerException e) {
			logger.error("Payment configuration validation failed: {}", e.getMessage());
			return false;
		}
	}
}