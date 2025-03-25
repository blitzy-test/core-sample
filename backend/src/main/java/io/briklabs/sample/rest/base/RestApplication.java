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
import io.swagger.v3.oas.models.tags.Tag;

import io.briklabs.sample.payments.rest.TransactionProcessingResource;
import io.briklabs.sample.payments.rest.TransactionEventResource;

/**
 * Main REST application configuration class that registers resources, filters, and
 * configures the JAX-RS application. This class has been updated to include payment-specific
 * resources and OpenAPI documentation.
 */
public class RestApplication extends ResourceConfig {
    public RestApplication() {
        this(null);
    }

    /**
     * Constructs the REST application with the specified server URL.
     * Registers core and payment-specific resources, filters, and configures OpenAPI documentation.
     *
     * @param serverUrl The base URL for the server, used in OpenAPI documentation
     */
    public RestApplication(String serverUrl) {
        // Register core resources and filters
        register(new StatusResource());
        register(new CharsetResponseFilter());
        register(new CorsFilter());
        
        // Register payment-specific resources
        // These resources implement the payment API endpoints following the pattern
        // /organizations/{org_id}/accounts/{account_id}/transactions/
        register(TransactionProcessingResource.class);
        register(TransactionEventResource.class);
        
        // Configure OpenAPI documentation
        List<Server> servers = serverUrl == null ? List.of() : List.of(new Server().url(serverUrl));

        // Create and configure OpenAPI documentation with payment-specific metadata
        OpenApiResource openapiResource = new OpenApiResource();
        openapiResource.setOpenApiConfiguration(new SwaggerConfiguration()
                .openAPI31(true)
                .openAPI(new OpenAPI(SpecVersion.V31)
                        .info(new Info()
                            .title("Core Sample API with Payments")
                            .description("REST API for core sample application with payment processing capabilities")
                            .version("1.0.0"))
                        .servers(servers)
                        .addTagsItem(new Tag().name("Payments").description("Payment transaction operations"))
                        .addTagsItem(new Tag().name("Payment Events").description("Payment transaction event history"))
                )
                .prettyPrint(true)
                .scannerClass(JaxrsApplicationScanner.class.getName()));
        register(openapiResource);
        register(SwaggerSerializers.class);

        // Disable WADL generation as we're using OpenAPI instead
        setProperties(Map.of(ServerProperties.WADL_FEATURE_DISABLE, true));
    }

    /**
     * Starts the HTTP server with the specified base URI and application configuration.
     * 
     * @param baseURI The base URI for the server
     * @param application The REST application configuration
     * @throws IOException If an error occurs during server startup
     */
    public static void start(URI baseURI, RestApplication application) throws IOException {
        Logger logger = LoggerFactory.getLogger(application.getClass());
        ServletContainer servletContainer = new ServletContainer(application);
        
        // Create and start the HTTP server with the configured application
        HttpServer server = GrizzlyWebContainerFactory.create(baseURI.resolve("/"), servletContainer, null, null);
        
        // Register shutdown hook to gracefully stop the server on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down server...");
            server.shutdownNow();
        }));

        logger.info("{} started on {}", application.getClass().getSimpleName(), baseURI);
    }
}