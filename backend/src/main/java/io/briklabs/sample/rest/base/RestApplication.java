package io.briklabs.sample.rest.base;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.briklabs.sample.payments.rest.TransactionResource;
import io.briklabs.sample.payments.rest.TransactionProcessingResource;
import io.briklabs.sample.payments.rest.TransactionEventResource;

import io.swagger.v3.jaxrs2.SwaggerSerializers;
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationScanner;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public class RestApplication extends ResourceConfig {
	public RestApplication() {
		this(null);
	}

	public RestApplication(String serverUrl) {
		// Register core resources
		register(new StatusResource());
		register(new CharsetResponseFilter());
		register(new CorsFilter());

		// Register payment-specific resources
		register(new TransactionResource());
		register(new TransactionProcessingResource());
		register(new TransactionEventResource());

		List<Server> servers = serverUrl == null ? List.of() : List.of(new Server().url(serverUrl));

		// Configure OpenAPI documentation
		OpenApiResource openapiResource = new OpenApiResource();
		openapiResource.setOpenApiConfiguration(new SwaggerConfiguration()
				.openAPI31(true)
				.openAPI(new OpenAPI(SpecVersion.V31)
						.info(new Info()
							.title("Core Sample API with Payments")
							.description("API for core sample application with payment transaction processing")
							.version("1.0.0"))
						.servers(servers)
						.tags(List.of(
							new Tag().name("Core").description("Core API endpoints"),
							new Tag().name("Payments").description("Payment transaction operations")
						)))
				.prettyPrint(true)
				.scannerClass(JaxrsApplicationScanner.class.getName()));
		register(openapiResource);
		register(SwaggerSerializers.class);

		setProperties(Map.of(ServerProperties.WADL_FEATURE_DISABLE, true));
	}

	public static void start(URI baseURI, RestApplication application) throws IOException {
		Logger logger = LoggerFactory.getLogger(application.getClass());
		ServletContainer servletContainer = new ServletContainer(application);
		
		// Configure Grizzly HTTP server with HikariCP connection pool support
		Map<String, String> initParams = Map.of(
			"com.sun.jersey.config.property.packages", "io.briklabs.sample",
			"hikaricp.maximumPoolSize", "30",
			"hikaricp.minimumIdle", "10",
			"hikaricp.connectionTimeout", "20000",
			"hikaricp.idleTimeout", "300000",
			"hikaricp.maxLifetime", "1200000",
			"hikaricp.leakDetectionThreshold", "60000"
		);
		
		HttpServer server = GrizzlyWebContainerFactory.create(baseURI.resolve("/"), servletContainer, initParams, null);
		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

		logger.info("{} started on {}", application.getClass().getSimpleName(), baseURI);
	}
}