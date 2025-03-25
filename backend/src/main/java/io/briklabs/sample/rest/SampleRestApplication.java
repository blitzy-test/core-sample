package io.briklabs.sample.rest;

import javax.ws.rs.ApplicationPath;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.briklabs.sample.config.SampleDatabaseConfig;
import io.briklabs.sample.payments.rest.TransactionResource;
import io.briklabs.sample.payments.rest.TransactionProcessingResource;
import io.briklabs.sample.payments.rest.TransactionEventResource;
import io.briklabs.sample.rest.base.RestApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Main REST application class that registers all REST resources and configures
 * the application's REST API.
 */
@ApplicationPath("/")
@OpenAPIDefinition(
    info = @Info(
        title = "Core Sample API",
        version = "1.0.0",
        description = "API for the core-sample application with payment processing capabilities",
        contact = @Contact(name = "BrikLabs", url = "https://briklabs.io")
    ),
    tags = {
        @Tag(name = "Sample", description = "Sample operations"),
        @Tag(name = "Payments", description = "Payment transaction operations")
    }
)
public class SampleRestApplication extends RestApplication {

    private HikariDataSource paymentDataSource;

    /**
     * Initializes the REST application with sample and payment resources.
     * 
     * @param databaseConfig The database configuration for connecting to PostgreSQL
     */
    public SampleRestApplication(SampleDatabaseConfig databaseConfig) {
        // Initialize HikariCP connection pool for payment data access
        initializeConnectionPool(databaseConfig);
        
        // Register core sample resource
        register(new SampleResource(databaseConfig));
        
        // Register payment-specific REST resources
        registerPaymentResources(databaseConfig);
        
        // Configure CORS settings to allow payment-specific headers and methods
        configureCorsSettings();
        
        // Configure OpenAPI documentation for payment endpoints
        configureSwagger();
    }
    
    /**
     * Initializes the HikariCP connection pool for efficient database operations
     * with PostgreSQL 17.4, optimized for payment transaction processing.
     * 
     * @param databaseConfig The database configuration containing connection parameters
     */
    private void initializeConnectionPool(SampleDatabaseConfig databaseConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseConfig.getDatabaseURL());
        config.setUsername(databaseConfig.getDatabaseUsername());
        config.setPassword(databaseConfig.getDatabasePassword());
        config.setSchema(databaseConfig.getDatabaseSchema());
        
        // Configure connection pool settings optimized for payment processing
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setAutoCommit(false);
        config.setConnectionTestQuery("SELECT 1");
        config.setLeakDetectionThreshold(60000);
        
        // Register JMX monitoring for operational visibility
        config.setRegisterMbeans(true);
        
        // Set pool name for monitoring and troubleshooting
        config.setPoolName("PaymentHikariPool");
        
        paymentDataSource = new HikariDataSource(config);
    }
    
    /**
     * Registers all payment-specific REST resources for transaction management.
     * 
     * @param databaseConfig The database configuration for connecting to PostgreSQL
     */
    private void registerPaymentResources(SampleDatabaseConfig databaseConfig) {
        // Register transaction management endpoints
        register(new TransactionResource(paymentDataSource));
        
        // Register transaction lifecycle operation endpoints (process, capture, refund)
        register(new TransactionProcessingResource(paymentDataSource));
        
        // Register transaction event history endpoints
        register(new TransactionEventResource(paymentDataSource));
    }
    
    /**
     * Configures CORS settings to allow payment-specific headers and methods.
     */
    private void configureCorsSettings() {
        // Configure CORS headers for payment operations
        property("cors.allowed.origins", "*");
        property("cors.allowed.methods", "GET,POST,PUT,DELETE,OPTIONS");
        property("cors.allowed.headers", 
                "Content-Type,Accept,Authorization,X-Requested-With,X-API-Key," +
                "X-Transaction-ID,X-Idempotency-Key,X-Merchant-ID");
        property("cors.exposed.headers", 
                "Content-Type,X-Transaction-ID,X-Processing-Time,X-Rate-Limit-Remaining");
    }
    
    /**
     * Configures Swagger/OpenAPI documentation settings for payment endpoints.
     */
    private void configureSwagger() {
        // Configure Swagger settings
        property("swagger.pretty", "true");
        property("swagger.mapping.resources", "/api-docs");
        property("swagger.mapping.ui", "/api-docs-ui");
    }
    
    /**
     * Releases resources when the application is shutting down.
     */
    @Override
    public void destroy() {
        // Close the HikariCP connection pool
        if (paymentDataSource != null && !paymentDataSource.isClosed()) {
            paymentDataSource.close();
        }
        
        super.destroy();
    }
}