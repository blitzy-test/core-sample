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

import io.swagger.v3.jaxrs2.SwaggerSerializers;
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationScanner;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

public class RestApplication extends ResourceConfig {
	public RestApplication() {
		this(null);
	}

	public RestApplication(String serverUrl) {
		register(new StatusResource());
		register(new CharsetResponseFilter());
		register(new CorsFilter());

		List<Server> servers = serverUrl == null ? List.of() : List.of(new Server().url(serverUrl));

		OpenApiResource openapiResource = new OpenApiResource();
		openapiResource.setOpenApiConfiguration(new SwaggerConfiguration()
				.openAPI31(true)
				.openAPI(new OpenAPI(SpecVersion.V31)
						.info(new Info())
						.servers(servers)
				)
				.prettyPrint(true)
				.scannerClass(JaxrsApplicationScanner.class.getName()));
		register(openapiResource);
		register(SwaggerSerializers.class);

		setProperties(Map.of(ServerProperties.WADL_FEATURE_DISABLE, true));
	}

	public static void start(URI baseURI, RestApplication application) throws IOException {
		Logger logger = LoggerFactory.getLogger(application.getClass());
		ServletContainer servletContainer = new ServletContainer(application);
		HttpServer server = GrizzlyWebContainerFactory.create(baseURI.resolve("/"), servletContainer, null, null);
		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

		logger.info("{} started on {}", application.getClass().getSimpleName(), baseURI);
	}
}
