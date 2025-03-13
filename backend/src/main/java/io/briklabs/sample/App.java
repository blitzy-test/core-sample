package io.briklabs.sample;

import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.SampleDatabaseConfig;
import io.briklabs.sample.rest.SampleRestApplication;
import io.briklabs.sample.rest.base.RestApplication;

public class App {

	private static final Logger logger = LoggerFactory.getLogger(App.class);

	private static final int httpPort = 5900;
	private static final URI baseURI = URI.create("http://0.0.0.0:" + httpPort);

	private ConfigSource cs;

	public App(ConfigSource cs) {
		this.cs = cs;
	}

	public static void main(String[] args) throws ParseException {

		// Set BRIK_CONFIG env variable to a .yaml file to init this
		// See config.yaml
		ConfigSource cs = new ConfigSource();

		App app = new App(cs);
		app.start();
	}

	public CompletableFuture<Boolean> start() {
		CompletableFuture<Boolean> ready = new CompletableFuture<>();
		try {

			SampleDatabaseConfig databaseConfig = new SampleDatabaseConfig(cs);
			SampleRestApplication application = new SampleRestApplication(databaseConfig);
			RestApplication.start(baseURI, application);

			logger.info("Sample App started");

			ready.complete(true);
		} catch (Exception e) {
			logger.error("Error in service component, process exiting", e);
			ready.completeExceptionally(e);
		}

		return ready;
	}
}
