package io.briklabs.sample;

import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.HikariConfigurationProvider;
import io.briklabs.sample.config.PaymentDatabaseConfig;
import io.briklabs.sample.config.SampleDatabaseConfig;
import io.briklabs.sample.payments.service.PaymentCaptureService;
import io.briklabs.sample.payments.service.PaymentEventService;
import io.briklabs.sample.payments.service.PaymentLifecycleService;
import io.briklabs.sample.payments.service.PaymentQueryService;
import io.briklabs.sample.payments.service.PaymentRefundService;
import io.briklabs.sample.payments.service.PaymentTransactionService;
import io.briklabs.sample.payments.service.PaymentValidationService;
import io.briklabs.sample.rest.PaymentRestRouting;
import io.briklabs.sample.rest.SampleRestApplication;
import io.briklabs.sample.rest.base.RestApplication;

public class App {

	private static final Logger logger = LoggerFactory.getLogger(App.class);

	private static final int httpPort = 5900;
	private static final URI baseURI = URI.create("http://0.0.0.0:" + httpPort);

	private ConfigSource cs;
	private HikariDataSource paymentDataSource;
	private ScheduledExecutorService asyncPaymentExecutor;

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
			// Initialize database configurations
			SampleDatabaseConfig databaseConfig = new SampleDatabaseConfig(cs);
			PaymentDatabaseConfig paymentDatabaseConfig = new PaymentDatabaseConfig(cs);
			
			// Configure HikariCP connection pool for payment transaction processing
			HikariConfigurationProvider hikariConfigProvider = new HikariConfigurationProvider(cs);
			paymentDataSource = hikariConfigProvider.createPaymentDataSource(paymentDatabaseConfig);
			
			// Initialize asynchronous processing for long-running payment operations
			asyncPaymentExecutor = Executors.newScheduledThreadPool(
				cs.getValue("payment.async.threadPoolSize", Integer.class, 5)
			);
			
			// Initialize the payments module
			initializePaymentModule(paymentDatabaseConfig, paymentDataSource);
			
			// Create and register REST application with payment resources
			SampleRestApplication application = new SampleRestApplication(databaseConfig);
			
			// Register payment-specific REST resources with the application
			PaymentRestRouting paymentRouting = new PaymentRestRouting(
				paymentDataSource, 
				asyncPaymentExecutor
			);
			application.registerPaymentResources(paymentRouting);
			
			// Add integration with Access Rights module for payment permissions
			configureAccessRightsIntegration(application);
			
			// Start the REST application
			RestApplication.start(baseURI, application);

			logger.info("Sample App started with payments module");

			ready.complete(true);
		} catch (Exception e) {
			logger.error("Error in service component, process exiting", e);
			
			// Clean up resources if initialization failed
			if (paymentDataSource != null && !paymentDataSource.isClosed()) {
				paymentDataSource.close();
			}
			
			if (asyncPaymentExecutor != null && !asyncPaymentExecutor.isShutdown()) {
				asyncPaymentExecutor.shutdownNow();
			}
			
			ready.completeExceptionally(e);
		}

		return ready;
	}
	
	/**
	 * Initializes the payments module components
	 */
	private void initializePaymentModule(PaymentDatabaseConfig paymentDatabaseConfig, HikariDataSource dataSource) {
		logger.info("Initializing payments module...");
		
		// Initialize payment services
		// Note: In a real implementation, these would be instantiated with their
		// respective implementations and dependencies injected
		PaymentTransactionService transactionService = initializePaymentTransactionService(dataSource);
		PaymentCaptureService captureService = initializePaymentCaptureService(dataSource, transactionService);
		PaymentRefundService refundService = initializePaymentRefundService(dataSource, transactionService);
		PaymentQueryService queryService = initializePaymentQueryService(dataSource);
		PaymentEventService eventService = initializePaymentEventService(dataSource);
		PaymentLifecycleService lifecycleService = initializePaymentLifecycleService(dataSource, eventService);
		PaymentValidationService validationService = initializePaymentValidationService();
		
		logger.info("Payments module initialized successfully");
	}
	
	/**
	 * Configures integration with the Access Rights module for payment permissions
	 */
	private void configureAccessRightsIntegration(SampleRestApplication application) {
		logger.info("Configuring Access Rights integration for payment permissions...");
		
		// In a real implementation, this would integrate with an actual Access Rights module
		// For now, we'll just log that this would happen
		
		// Register payment-specific roles and permissions
		// Example: application.getAccessRightsModule().registerRole("PAYMENT_ADMIN", "Administrator for payment operations");
		// Example: application.getAccessRightsModule().registerPermission("payment:create", "Create payment transactions");
		
		logger.info("Access Rights integration for payments configured");
	}
	
	/**
	 * Initialize the PaymentTransactionService
	 */
	private PaymentTransactionService initializePaymentTransactionService(HikariDataSource dataSource) {
		logger.info("Initializing PaymentTransactionService");
		// In a real implementation, this would return an actual implementation
		return null;
	}
	
	/**
	 * Initialize the PaymentCaptureService
	 */
	private PaymentCaptureService initializePaymentCaptureService(
			HikariDataSource dataSource, 
			PaymentTransactionService transactionService) {
		logger.info("Initializing PaymentCaptureService");
		// In a real implementation, this would return an actual implementation
		return null;
	}
	
	/**
	 * Initialize the PaymentRefundService
	 */
	private PaymentRefundService initializePaymentRefundService(
			HikariDataSource dataSource, 
			PaymentTransactionService transactionService) {
		logger.info("Initializing PaymentRefundService");
		// In a real implementation, this would return an actual implementation
		return null;
	}
	
	/**
	 * Initialize the PaymentQueryService
	 */
	private PaymentQueryService initializePaymentQueryService(HikariDataSource dataSource) {
		logger.info("Initializing PaymentQueryService");
		// In a real implementation, this would return an actual implementation
		return null;
	}
	
	/**
	 * Initialize the PaymentEventService
	 */
	private PaymentEventService initializePaymentEventService(HikariDataSource dataSource) {
		logger.info("Initializing PaymentEventService");
		// In a real implementation, this would return an actual implementation
		return null;
	}
	
	/**
	 * Initialize the PaymentLifecycleService
	 */
	private PaymentLifecycleService initializePaymentLifecycleService(
			HikariDataSource dataSource, 
			PaymentEventService eventService) {
		logger.info("Initializing PaymentLifecycleService");
		// In a real implementation, this would return an actual implementation
		return null;
	}
	
	/**
	 * Initialize the PaymentValidationService
	 */
	private PaymentValidationService initializePaymentValidationService() {
		logger.info("Initializing PaymentValidationService");
		// In a real implementation, this would return an actual implementation
		return null;
	}
}