package io.briklabs.sample.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Configuration source for the application.
 * Loads configuration from YAML files and environment variables.
 * Provides methods for retrieving configuration values with proper validation.
 */
public class ConfigSource {

	private static final Logger logger = LoggerFactory.getLogger(ConfigSource.class);
	private Map<String, Object> properties = new HashMap<>();
	private Map<String, Object> paymentProperties = new HashMap<>();
	private boolean paymentModuleEnabled = false;

	/**
	 * Constructs a new ConfigSource using the BRIK_CONFIG environment variable.
	 */
	public ConfigSource() {
		this(System.getenv("BRIK_CONFIG"));
	}

	/**
	 * Constructs a new ConfigSource with the specified configuration path.
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
				
				// Load payment configuration if enabled
				loadPaymentConfig();
			} catch (FileNotFoundException e) {
				logger.warn("Cannot read the provided path {}, it will be ignored and other config sources will be attempted", configPath);
			}
		}
	}

	/**
	 * Constructs a new ConfigSource with the specified input stream.
	 * 
	 * @param is Input stream containing configuration data
	 */
	public ConfigSource(InputStream is) {
		Yaml yaml = new Yaml();
		properties = yaml.load(is);
		
		// Load payment configuration if enabled
		loadPaymentConfig();
	}

	/**
	 * Gets a configuration value by property name.
	 * Checks environment variables first, then the loaded configuration.
	 * 
	 * @param property Property name (dot-separated)
	 * @return Property value or null if not found
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
	 * @param value Configuration value
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
	 * @param property Property name
	 * @param defaultValue Default value if property is not found
	 * @return Property value or default value
	 */
	public String getOrDefault(String property, String defaultValue) {
		String result = get(property);
		return result != null ? result : defaultValue;
	}

	/**
	 * Gets an integer configuration value with a default fallback.
	 * 
	 * @param property Property name
	 * @param defaultValue Default value if property is not found
	 * @return Property value as integer or default value
	 */
	public int getOrDefault(String property, int defaultValue) {
		String value = get(property);
		return value != null ? Integer.parseInt(value) : defaultValue;
	}

	/**
	 * Gets a long configuration value with a default fallback.
	 * 
	 * @param property Property name
	 * @param defaultValue Default value if property is not found
	 * @return Property value as long or default value
	 */
	public long getOrDefault(String property, long defaultValue) {
		String value = get(property);
		return value != null ? Long.parseLong(value) : defaultValue;
	}

	/**
	 * Gets a boolean configuration value with a default fallback.
	 * 
	 * @param property Property name
	 * @param defaultValue Default value if property is not found
	 * @return Property value as boolean or default value
	 */
	public boolean getOrDefault(String property, boolean defaultValue) {
		String value = get(property);
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}

	/**
	 * Gets a required configuration value.
	 * 
	 * @param property Property name
	 * @return Property value
	 * @throws NullPointerException if the property is not found
	 */
	public String getRequired(String property) {
		return Objects.requireNonNull(get(property), () -> "Missing required config variable " + property);
	}

	/**
	 * Gets an environment variable value.
	 * 
	 * @param key Environment variable name
	 * @return Environment variable value or null if not set
	 */
	protected String getEnv(String key) {
		return System.getenv(key);
	}

	/**
	 * Gets all environment variables.
	 * 
	 * @return Map of environment variables
	 */
	protected Map<String, String> getAllEnv() {
		return System.getenv();
	}

	/**
	 * Checks if a configuration section exists.
	 * 
	 * @param property Property name (section path)
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
	 * Loads payment configuration if enabled.
	 * Reads the payment configuration file specified in the main configuration.
	 */
	private void loadPaymentConfig() {
		// Check if payment module is enabled
		paymentModuleEnabled = getOrDefault("brik.payments.enabled", false);
		
		if (!paymentModuleEnabled) {
			logger.info("Payment module is disabled - skipping payment configuration loading");
			return;
		}
		
		// Get payment configuration file path
		String paymentConfigFile = getOrDefault("brik.payments.configFile", "payment-config.yaml");
		logger.info("Payment module is enabled - loading payment configuration from {}", paymentConfigFile);
		
		try {
			File configFile = new File(paymentConfigFile);
			if (!configFile.exists()) {
				// Try to load from classpath if file doesn't exist
				InputStream is = getClass().getClassLoader().getResourceAsStream(paymentConfigFile);
				if (is != null) {
					loadPaymentConfigFromStream(is);
				} else {
					logger.warn("Payment configuration file not found: {} - using default values", paymentConfigFile);
				}
			} else {
				// Load from file
				FileInputStream fis = new FileInputStream(configFile);
				loadPaymentConfigFromStream(fis);
			}
		} catch (Exception e) {
			logger.error("Failed to load payment configuration: {}", e.getMessage(), e);
		}
	}
	
	/**
	 * Loads payment configuration from an input stream.
	 * 
	 * @param is Input stream containing payment configuration
	 */
	private void loadPaymentConfigFromStream(InputStream is) {
		try {
			Yaml yaml = new Yaml();
			paymentProperties = yaml.load(is);
			logger.info("Payment configuration loaded successfully");
		} catch (Exception e) {
			logger.error("Failed to parse payment configuration: {}", e.getMessage(), e);
		}
	}
	
	/**
	 * Checks if the payment module is enabled.
	 * 
	 * @return true if payment module is enabled, false otherwise
	 */
	public boolean isPaymentModuleEnabled() {
		return paymentModuleEnabled;
	}
	
	/**
	 * Gets a payment configuration value.
	 * 
	 * @param property Property name (dot-separated)
	 * @param defaultValue Default value if property is not found
	 * @return Property value or default value
	 */
	@SuppressWarnings("unchecked")
	public String getPaymentConfig(String property, String defaultValue) {
		if (!paymentModuleEnabled) {
			return defaultValue;
		}
		
		// Check for environment variable override
		String envName = "PAYMENT_" + property.replace('.', '_').toUpperCase();
		String fromEnv = getEnv(envName);
		if (fromEnv != null) {
			return fromEnv;
		}
		
		// Check in payment properties
		String[] split = property.split("\\.");
		Map<String, Object> map = paymentProperties;
		
		for (int i = 0; i < split.length; i++) {
			String level = split[i];
			
			if (map == null) {
				return defaultValue;
			}
			
			Object object = map.get(level);
			if (object == null) {
				return defaultValue;
			} else if (object instanceof Map) {
				map = (Map<String, Object>) object;
				continue;
			} else {
				if (i == split.length - 1) {
					return String.valueOf(object);
				}
			}
		}
		
		// Check in main properties with brik.payments prefix
		return getOrDefault("brik.payments." + property, defaultValue);
	}
	
	/**
	 * Gets an integer payment configuration value.
	 * 
	 * @param property Property name (dot-separated)
	 * @param defaultValue Default value if property is not found
	 * @return Property value as integer or default value
	 */
	public int getPaymentConfigInt(String property, int defaultValue) {
		String value = getPaymentConfig(property, null);
		if (value == null) {
			return defaultValue;
		}
		
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			logger.warn("Invalid integer value for payment property {}: {}", property, value);
			return defaultValue;
		}
	}
	
	/**
	 * Gets a long payment configuration value.
	 * 
	 * @param property Property name (dot-separated)
	 * @param defaultValue Default value if property is not found
	 * @return Property value as long or default value
	 */
	public long getPaymentConfigLong(String property, long defaultValue) {
		String value = getPaymentConfig(property, null);
		if (value == null) {
			return defaultValue;
		}
		
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			logger.warn("Invalid long value for payment property {}: {}", property, value);
			return defaultValue;
		}
	}
	
	/**
	 * Gets a boolean payment configuration value.
	 * 
	 * @param property Property name (dot-separated)
	 * @param defaultValue Default value if property is not found
	 * @return Property value as boolean or default value
	 */
	public boolean getPaymentConfigBoolean(String property, boolean defaultValue) {
		String value = getPaymentConfig(property, null);
		if (value == null) {
			return defaultValue;
		}
		
		return Boolean.parseBoolean(value);
	}
	
	/**
	 * Gets a required payment configuration value.
	 * 
	 * @param property Property name (dot-separated)
	 * @return Property value
	 * @throws NullPointerException if the property is not found
	 */
	public String getRequiredPaymentConfig(String property) {
		String value = getPaymentConfig(property, null);
		return Objects.requireNonNull(value, () -> "Missing required payment config property: " + property);
	}
	
	/**
	 * Validates payment configuration.
	 * Checks for required properties and valid values.
	 * 
	 * @return true if configuration is valid, false otherwise
	 */
	public boolean validatePaymentConfig() {
		if (!paymentModuleEnabled) {
			return false;
		}
		
		try {
			// Check required properties
			getRequiredPaymentConfig("database.schema");
			getRequiredPaymentConfig("database.tables.transaction");
			getRequiredPaymentConfig("database.tables.paymentData");
			getRequiredPaymentConfig("database.tables.paymentEvent");
			
			// Validate connection pool settings
			int maxPoolSize = getPaymentConfigInt("database.connectionPool.maximumPoolSize", 30);
			int minIdle = getPaymentConfigInt("database.connectionPool.minimumIdle", 10);
			
			if (minIdle > maxPoolSize) {
				logger.warn("Invalid connection pool configuration: minimumIdle ({}) > maximumPoolSize ({})", 
						minIdle, maxPoolSize);
				return false;
			}
			
			return true;
		} catch (Exception e) {
			logger.error("Payment configuration validation failed: {}", e.getMessage());
			return false;
		}
	}
	
	/**
	 * Gets connection pool configuration for a specific database.
	 * 
	 * @param dbName Database name
	 * @return Map of connection pool configuration parameters
	 */
	public Map<String, Object> getConnectionPoolConfig(String dbName) {
		Map<String, Object> config = new HashMap<>();
		
		String prefix = "brik.database." + dbName + ".connectionPool.";
		
		// Get connection pool parameters with defaults
		config.put("maximumPoolSize", getOrDefault(prefix + "maximumPoolSize", 10));
		config.put("minimumIdle", getOrDefault(prefix + "minimumIdle", 5));
		config.put("connectionTimeout", getOrDefault(prefix + "connectionTimeout", 30000));
		config.put("idleTimeout", getOrDefault(prefix + "idleTimeout", 600000));
		config.put("maxLifetime", getOrDefault(prefix + "maxLifetime", 1800000));
		config.put("autoCommit", getOrDefault(prefix + "autoCommit", true));
		config.put("connectionTestQuery", getOrDefault(prefix + "connectionTestQuery", "SELECT 1"));
		config.put("leakDetectionThreshold", getOrDefault(prefix + "leakDetectionThreshold", 0));
		config.put("registerMbeans", getOrDefault(prefix + "registerMbeans", false));
		
		return config;
	}
	
	/**
	 * Gets payment-specific connection pool configuration.
	 * 
	 * @return Map of connection pool configuration parameters optimized for payment processing
	 */
	public Map<String, Object> getPaymentConnectionPoolConfig() {
		Map<String, Object> config = new HashMap<>();
		
		// Get payment-specific connection pool parameters with optimized defaults
		config.put("maximumPoolSize", getPaymentConfigInt("database.connectionPool.maximumPoolSize", 30));
		config.put("minimumIdle", getPaymentConfigInt("database.connectionPool.minimumIdle", 10));
		config.put("connectionTimeout", getPaymentConfigLong("database.connectionPool.connectionTimeout", 20000));
		config.put("idleTimeout", getPaymentConfigLong("database.connectionPool.idleTimeout", 300000));
		config.put("maxLifetime", getPaymentConfigLong("database.connectionPool.maxLifetime", 1200000));
		config.put("autoCommit", getPaymentConfigBoolean("database.connectionPool.autoCommit", false));
		config.put("connectionTestQuery", getPaymentConfig("database.connectionPool.connectionTestQuery", "SELECT 1"));
		config.put("leakDetectionThreshold", getPaymentConfigLong("database.connectionPool.leakDetectionThreshold", 60000));
		config.put("registerMbeans", getPaymentConfigBoolean("database.connectionPool.registerMbeans", true));
		config.put("poolName", getPaymentConfig("database.connectionPool.poolName", "PaymentHikariPool"));
		
		// Add any additional properties from hikari-config.properties if specified
		String hikariConfigFile = getOrDefault("brik.hikaricp.configurationFile", null);
		if (hikariConfigFile != null) {
			try {
				Properties hikariProps = new Properties();
				hikariProps.load(new FileInputStream(new File(hikariConfigFile)));
				
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
		
		return config;
	}
	
	/**
	 * Gets payment feature flags.
	 * 
	 * @return Map of payment feature flags
	 */
	public Map<String, Boolean> getPaymentFeatureFlags() {
		Map<String, Boolean> flags = new HashMap<>();
		
		// Core payment features
		flags.put("enabled", paymentModuleEnabled);
		flags.put("accessRightsIntegration", getOrDefault("brik.payments.accessRights.integration", false));
		
		// Payment processing features
		flags.put("asyncProcessing", getPaymentConfigBoolean("processing.async.enabled", true));
		flags.put("partialCapture", getPaymentConfigBoolean("merchant.defaults.partialCapture.enabled", true));
		flags.put("partialRefund", getPaymentConfigBoolean("merchant.defaults.partialRefund.enabled", true));
		
		// Query features
		flags.put("fullTextSearch", getPaymentConfigBoolean("query.features.enableFullTextSearch", true));
		flags.put("dateRangeQueries", getPaymentConfigBoolean("query.features.enableDateRangeQueries", true));
		flags.put("amountRangeQueries", getPaymentConfigBoolean("query.features.enableAmountRangeQueries", true));
		flags.put("multiStatusFilter", getPaymentConfigBoolean("query.features.enableMultiStatusFilter", true));
		flags.put("metadataFiltering", getPaymentConfigBoolean("query.features.enableMetadataFiltering", true));
		
		// Logging features
		flags.put("detailedLogging", getPaymentConfigBoolean("logging.detailedLogging", true));
		flags.put("logToDatabase", getPaymentConfigBoolean("logging.logToDatabase", true));
		
		return flags;
	}
	
	/**
	 * Gets payment transaction states configuration.
	 * 
	 * @return Map of transaction states configuration
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getPaymentTransactionStatesConfig() {
		if (!paymentModuleEnabled) {
			return new HashMap<>();
		}
		
		Map<String, Object> result = new HashMap<>();
		
		// Get transaction states from payment properties
		if (paymentProperties.containsKey("transactionStates")) {
			Object statesObj = paymentProperties.get("transactionStates");
			if (statesObj instanceof Map) {
				result = (Map<String, Object>) statesObj;
			}
		}
		
		return result;
	}
}