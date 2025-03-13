package io.briklabs.sample.config;

import java.util.Objects;

public interface DatabaseConfig {

	String getDatabaseURL();

	String getDatabaseUsername();

	String getDatabasePassword();

	String getDatabaseSchema();

	static String requiredEnv(String env) {
		return Objects.requireNonNull(System.getenv(env), () -> "Missing required environment variable " + env);
	}

	static String envOrDefault(String env, String defaultValue) {
		return Objects.requireNonNullElse(System.getenv(env), defaultValue);
	}
}
