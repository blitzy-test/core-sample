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

	public ConfigSource() {
		this(System.getenv("BRIK_CONFIG"));
	}

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

	public ConfigSource(InputStream is) {
		Yaml yaml = new Yaml();
		properties = yaml.load(is);
	}

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
	 * @param key
	 * @param value
	 * @return
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

	public String getOrDefault(String property, String defaultValue) {
		String result = get(property);
		return result != null ? result : defaultValue;
	}

	public int getOrDefault(String property, int defaultValue) {
		String value = get(property);
		return value != null ? Integer.parseInt(value) : defaultValue;
	}

	public boolean getOrDefault(String property, boolean defaultValue) {
		String value = get(property);
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}

	public String getRequired(String property) {
		return Objects.requireNonNull(get(property), () -> "Missing required config variable " + property);
	}

	protected String getEnv(String key) {
		return System.getenv(key);
	}

	protected Map<String, String> getAllEnv() {
		return System.getenv();
	}

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
}
