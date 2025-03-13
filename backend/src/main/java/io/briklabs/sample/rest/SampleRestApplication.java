package io.briklabs.sample.rest;

import javax.ws.rs.ApplicationPath;

import io.briklabs.sample.config.SampleDatabaseConfig;
import io.briklabs.sample.rest.base.RestApplication;

@ApplicationPath("/")
public class SampleRestApplication extends RestApplication {

	public SampleRestApplication(SampleDatabaseConfig databaseConfig) {
		register(new SampleResource(databaseConfig));
	}
}
