package io.briklabs.sample;

import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.SampleDatabaseConfig;
import io.briklabs.sample.payments.service.PaymentTransactionService;
import io.briklabs.sample.payments.service.PaymentTransactionServiceImpl;
import io.briklabs.sample.payments.service.PaymentLifecycleService;
import io.briklabs.sample.payments.service.PaymentLifecycleServiceImpl;
import io.briklabs.sample.payments.service.PaymentCaptureService;
import io.briklabs.sample.payments.service.PaymentCaptureServiceImpl;
import io.briklabs.sample.payments.service.PaymentRefundService;
import io.briklabs.sample.payments.service.PaymentRefundServiceImpl;
import io.briklabs.sample.payments.service.PaymentEventService;
import io.briklabs.sample.payments.service.PaymentEventServiceImpl;
import io.briklabs.sample.payments.service.PaymentQueryService;
import io.briklabs.sample.payments.service.PaymentQueryServiceImpl;
import io.briklabs.sample.payments.service.PaymentValidationService;
import io.briklabs.sample.payments.service.PaymentValidationServiceImpl;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import io.briklabs.sample.payments.data.dao.impl.PaymentTransactionDAOImpl;
import io.briklabs.sample.rest.SampleRestApplication;
import io.briklabs.sample.rest.PaymentRestRouting;
import io.briklabs.sample.rest.base.RestApplication;

/**
 * Main application class that initializes and starts the core-sample application.
 * This class is responsible for setting up the configuration, database connections,
 * payment services, and REST application.
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final int httpPort = 5900;
    private static final URI baseURI = URI.create("http://0.0.0.0:" + httpPort);

    private ConfigSource cs;
    private HikariDataSource paymentDataSource;
    private PaymentTransactionService paymentTransactionService;
    private PaymentLifecycleService paymentLifecycleService;
    private PaymentCaptureService paymentCaptureService;
    private PaymentRefundService paymentRefundService;
    private PaymentEventService paymentEventService;
    private PaymentQueryService paymentQueryService;
    private PaymentValidationService paymentValidationService;

    /**
     * Constructs a new App instance with the specified configuration source.
     *
     * @param cs The configuration source for the application
     */
    public App(ConfigSource cs) {
        this.cs = cs;
    }

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments
     * @throws ParseException If there is an error parsing configuration
     */
    public static void main(String[] args) throws ParseException {
        // Set BRIK_CONFIG env variable to a .yaml file to init this
        // See config.yaml
        ConfigSource cs = new ConfigSource();

        App app = new App(cs);
        app.start();
    }

    /**
     * Starts the application, initializing all components and services.
     *
     * @return A CompletableFuture that completes when the application is ready
     */
    public CompletableFuture<Boolean> start() {
        CompletableFuture<Boolean> ready = new CompletableFuture<>();
        try {
            // Initialize database configuration
            SampleDatabaseConfig databaseConfig = new SampleDatabaseConfig(cs);
            
            // Initialize HikariCP connection pool for payment transaction processing
            initializePaymentConnectionPool(databaseConfig);
            
            // Initialize payment services
            initializePaymentServices();
            
            // Initialize and register payment REST resources with Access Rights integration
            PaymentRestRouting paymentRouting = new PaymentRestRouting(
                paymentTransactionService,
                paymentLifecycleService,
                paymentCaptureService,
                paymentRefundService,
                paymentEventService,
                paymentQueryService
            );
            
            // Create and start the REST application
            SampleRestApplication application = new SampleRestApplication(databaseConfig);
            
            // Register payment-specific REST resources
            application.registerPaymentResources(paymentRouting);
            
            // Start the REST application
            RestApplication.start(baseURI, application);

            logger.info("Sample App started with payments module integration");

            ready.complete(true);
        } catch (Exception e) {
            logger.error("Error in service component, process exiting", e);
            
            // Clean up resources if initialization failed
            if (paymentDataSource != null && !paymentDataSource.isClosed()) {
                logger.info("Closing payment connection pool due to startup failure");
                paymentDataSource.close();
            }
            
            ready.completeExceptionally(e);
        }

        return ready;
    }
    
    /**
     * Initializes the HikariCP connection pool for payment transaction processing.
     * 
     * @param databaseConfig The database configuration containing connection parameters
     */
    private void initializePaymentConnectionPool(SampleDatabaseConfig databaseConfig) {
        logger.info("Initializing HikariCP connection pool for payment transaction processing");
        
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
        logger.info("Payment connection pool initialized with {} max connections", config.getMaximumPoolSize());
    }
    
    /**
     * Initializes all payment-related services required for payment processing.
     */
    private void initializePaymentServices() {
        logger.info("Initializing payment services");
        
        // Initialize the payment validation service
        paymentValidationService = new PaymentValidationServiceImpl();
        
        // Initialize the payment transaction DAO
        PaymentTransactionDAO paymentTransactionDAO = new PaymentTransactionDAOImpl(paymentDataSource);
        
        // Initialize the payment event service for tracking payment lifecycle events
        paymentEventService = new PaymentEventServiceImpl(paymentDataSource);
        
        // Initialize the payment lifecycle service for managing payment state transitions
        paymentLifecycleService = new PaymentLifecycleServiceImpl(
            paymentTransactionDAO, 
            paymentEventService,
            paymentValidationService
        );
        
        // Initialize the payment transaction service
        paymentTransactionService = new PaymentTransactionServiceImpl(
            paymentTransactionDAO,
            paymentLifecycleService,
            paymentEventService,
            paymentValidationService
        );
        
        // Initialize the payment capture service for processing payment captures
        paymentCaptureService = new PaymentCaptureServiceImpl(
            paymentTransactionService,
            paymentLifecycleService,
            paymentEventService,
            paymentValidationService
        );
        
        // Initialize the payment refund service for processing payment refunds
        paymentRefundService = new PaymentRefundServiceImpl(
            paymentTransactionService,
            paymentLifecycleService,
            paymentEventService,
            paymentValidationService
        );
        
        // Initialize the payment query service for complex payment data querying
        paymentQueryService = new PaymentQueryServiceImpl(paymentTransactionDAO);
        
        // Set up asynchronous processing for long-running payment operations
        initializeAsynchronousProcessing();
        
        logger.info("Payment services initialized successfully");
    }
    
    /**
     * Initializes asynchronous processing for long-running payment operations
     * like captures and refunds.
     */
    private void initializeAsynchronousProcessing() {
        logger.info("Initializing asynchronous processing for payment operations");
        
        // Register shutdown hook to ensure graceful shutdown of async processing
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down payment asynchronous processing");
            // Additional shutdown logic for async processing would go here
        }));
        
        // Additional async processing setup would go here
        // This could include thread pools, task queues, etc.
        
        logger.info("Asynchronous processing initialized for payment operations");
    }
}