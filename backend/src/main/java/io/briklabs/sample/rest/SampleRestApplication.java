package io.briklabs.sample.rest;

import javax.ws.rs.ApplicationPath;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.briklabs.sample.config.SampleDatabaseConfig;
import io.briklabs.sample.payments.data.HikariCPConfig;
import io.briklabs.sample.payments.rest.TransactionResource;
import io.briklabs.sample.payments.rest.TransactionProcessingResource;
import io.briklabs.sample.payments.rest.TransactionEventResource;
import io.briklabs.sample.payments.rest.PaymentRestRouting;
import io.briklabs.sample.rest.base.RestApplication;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationPath("/")
public class SampleRestApplication extends RestApplication {
    private static final Logger logger = LoggerFactory.getLogger(SampleRestApplication.class);
    private HikariDataSource hikariDataSource;

    public SampleRestApplication(SampleDatabaseConfig databaseConfig) {
        // Register core resources
        register(new SampleResource(databaseConfig));
        
        // Initialize HikariCP connection pool for payment data access
        initializeHikariConnectionPool(databaseConfig);
        
        // Register payment-specific REST resources
        registerPaymentResources(databaseConfig);
        
        // Configure OpenAPI documentation to include payment endpoints
        configureOpenAPIDocumentation();
        
        // Configure CORS settings for payment-specific headers and methods
        configureCorsSettings();
        
        // Add error handling for payment-specific exceptions
        registerPaymentExceptionHandlers();
        
        logger.info("SampleRestApplication initialized with payment module support");
    }
    
    /**
     * Initializes the HikariCP connection pool for efficient database operations
     * with payment data.
     * 
     * @param databaseConfig The database configuration
     */
    private void initializeHikariConnectionPool(SampleDatabaseConfig databaseConfig) {
        try {
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
            config.setLeakDetectionThreshold(60000);
            
            // Enable JMX monitoring for operational visibility
            config.setRegisterMbeans(true);
            
            // Set pool name for monitoring and troubleshooting
            config.setPoolName("PaymentHikariPool");
            
            hikariDataSource = new HikariDataSource(config);
            
            logger.info("HikariCP connection pool initialized for payment data access");
        } catch (Exception e) {
            logger.error("Failed to initialize HikariCP connection pool", e);
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }
    
    /**
     * Registers payment-specific REST resources for handling payment endpoints.
     * 
     * @param databaseConfig The database configuration
     */
    private void registerPaymentResources(SampleDatabaseConfig databaseConfig) {
        try {
            // Create PaymentRestRouting to handle special '_all' placeholder for retrieving
            // data across accounts or transactions
            PaymentRestRouting paymentRouting = new PaymentRestRouting(hikariDataSource);
            
            // Register payment transaction resources
            register(new TransactionResource(hikariDataSource));
            register(new TransactionProcessingResource(hikariDataSource));
            register(new TransactionEventResource(hikariDataSource));
            
            // Register payment routing handler
            register(paymentRouting);
            
            logger.info("Payment REST resources registered successfully");
        } catch (Exception e) {
            logger.error("Failed to register payment REST resources", e);
            throw new RuntimeException("Failed to initialize payment endpoints", e);
        }
    }
    
    /**
     * Configures OpenAPI documentation to include payment endpoint specifications.
     */
    private void configureOpenAPIDocumentation() {
        OpenAPI openAPI = new OpenAPI()
            .info(new Info()
                .title("Core Sample API with Payments")
                .description("API for core sample application with payment transaction processing")
                .version("1.0.0"))
            .tags(List.of(
                new Tag().name("Core").description("Core API endpoints"),
                new Tag().name("Payments").description("Payment transaction operations")
            ));
            
        property("openapi.configuration", openAPI);
    }
    
    /**
     * Configures CORS settings to allow payment-specific headers and methods.
     */
    private void configureCorsSettings() {
        property("cors.allowed.origins", "https://app.briklabs.io,https://admin.briklabs.io,http://localhost:3000");
        property("cors.allowed.headers", "Origin,Content-Type,Accept,Authorization,X-Requested-With,X-Transaction-ID,X-Idempotency-Key,X-Merchant-ID");
        property("cors.allowed.methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD,PATCH");
    }
    
    /**
     * Registers exception handlers for payment-specific exceptions.
     */
    private void registerPaymentExceptionHandlers() {
        // Register exception mappers for payment-specific exceptions
        register(io.briklabs.sample.payments.rest.exception.PaymentExceptionMapper.class);
        register(io.briklabs.sample.payments.rest.exception.ValidationExceptionMapper.class);
        register(io.briklabs.sample.payments.rest.exception.AuthorizationExceptionMapper.class);
    }
    
    /**
     * Releases resources when the application is shut down.
     */
    @Override
    public void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            logger.info("Closing HikariCP connection pool");
            hikariDataSource.close();
        }
        super.close();
    }
    
    /**
     * Updates health check metrics to include payment processing status.
     * 
     * @return Health check metrics including payment processing status
     */
    public Object getHealthMetrics() {
        // This method will be called by StatusResource to include payment metrics
        // in the health check response
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            return new Object[] {
                "paymentPool", hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                "paymentPoolIdle", hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                "paymentPoolTotal", hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                "paymentPoolWaiting", hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            };
        }
        return new Object[] {
            "paymentPool", "unavailable"
        };
    }
}