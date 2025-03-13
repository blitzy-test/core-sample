package io.briklabs.sample.config;

public class SampleDatabaseConfig implements DatabaseConfig {

	private ConfigSource cs;

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
}
