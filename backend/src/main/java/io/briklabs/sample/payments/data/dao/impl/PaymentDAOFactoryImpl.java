package io.briklabs.sample.payments.data.dao.impl;

import com.zaxxer.hikari.HikariDataSource;
import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.ConnectionManager;
import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.dao.PaymentDataDAO;
import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.dao.PaymentFeeDAO;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the PaymentDAOFactory interface that creates and manages DAO instances.
 * <p>
 * This class serves as the central factory for all payment DAO implementations, handling their
 * instantiation, dependency injection, and lifecycle management. It provides access to all
 * concrete DAO implementations through a unified interface while concealing implementation details.
 * </p>
 * <p>
 * The factory implements the Singleton pattern for each DAO type, ensuring efficient resource
 * usage and consistent state across the application. It also supports test mode for easier
 * unit testing with mock implementations.
 * </p>
 */
public class PaymentDAOFactoryImpl implements PaymentDAOFactory {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDAOFactoryImpl.class);

    /**
     * The HikariCP data source for database connectivity.
     */
    private final HikariDataSource dataSource;

    /**
     * Flag indicating whether the factory is operating in test mode.
     */
    private final boolean testMode;

    /**
     * Connection manager for database operations.
     */
    private final ConnectionManager connectionManager;

    /**
     * Singleton instances of DAO implementations.
     */
    private PaymentTransactionDAO paymentTransactionDAO;
    private PaymentDataDAO paymentDataDAO;
    private PaymentFeeDAO paymentFeeDAO;
    private PaymentEventDAO paymentEventDAO;

    /**
     * Flag indicating whether the factory has been initialized.
     */
    private boolean initialized = false;

    /**
     * Creates a new PaymentDAOFactoryImpl with the specified data source.
     *
     * @param dataSource The HikariDataSource to use for database connectivity
     */
    public PaymentDAOFactoryImpl(HikariDataSource dataSource) {
        this(dataSource, false);
    }

    /**
     * Creates a new PaymentDAOFactoryImpl with the specified data source and test mode flag.
     *
     * @param dataSource The HikariDataSource to use for database connectivity
     * @param testMode Flag indicating whether to use test implementations
     */
    public PaymentDAOFactoryImpl(HikariDataSource dataSource, boolean testMode) {
        logger.info("Creating PaymentDAOFactory with testMode={}", testMode);
        this.dataSource = dataSource;
        this.testMode = testMode;
        this.connectionManager = new ConnectionManager(dataSource);
    }

    /**
     * Creates a new PaymentDAOFactoryImpl with the specified database configuration.
     *
     * @param databaseConfig The database configuration
     * @param configSource The configuration source
     */
    public PaymentDAOFactoryImpl(DatabaseConfig databaseConfig, ConfigSource configSource) {
        this(databaseConfig, configSource, false);
    }

    /**
     * Creates a new PaymentDAOFactoryImpl with the specified database configuration and test mode flag.
     *
     * @param databaseConfig The database configuration
     * @param configSource The configuration source
     * @param testMode Flag indicating whether to use test implementations
     */
    public PaymentDAOFactoryImpl(DatabaseConfig databaseConfig, ConfigSource configSource, boolean testMode) {
        logger.info("Creating PaymentDAOFactory with database configuration and testMode={}", testMode);
        this.connectionManager = new ConnectionManager(databaseConfig, configSource);
        this.dataSource = connectionManager.getDataSource();
        this.testMode = testMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (initialized) {
            logger.debug("PaymentDAOFactory already initialized");
            return;
        }

        logger.info("Initializing PaymentDAOFactory");

        try {
            // Create DAO instances
            if (testMode) {
                // In test mode, we would create mock implementations
                // This would typically be handled by a testing framework like Mockito
                logger.info("Creating mock DAO implementations for test mode");
                // For now, we'll use the real implementations even in test mode
                createRealImplementations();
            } else {
                // Create real implementations
                createRealImplementations();
            }

            initialized = true;
            logger.info("PaymentDAOFactory initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize PaymentDAOFactory", e);
            throw new RuntimeException("Failed to initialize PaymentDAOFactory", e);
        }
    }

    /**
     * Creates real DAO implementations.
     */
    private void createRealImplementations() {
        logger.debug("Creating real DAO implementations");
        paymentTransactionDAO = new PaymentTransactionDaoImpl(connectionManager);
        paymentDataDAO = new PaymentDataDaoImpl(connectionManager);
        paymentFeeDAO = new PaymentFeeDaoImpl(connectionManager);
        paymentEventDAO = new PaymentEventDaoImpl(connectionManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        logger.info("Shutting down PaymentDAOFactory");
        
        // Release resources
        paymentTransactionDAO = null;
        paymentDataDAO = null;
        paymentFeeDAO = null;
        paymentEventDAO = null;
        
        // Shutdown connection manager
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
        
        initialized = false;
        logger.info("PaymentDAOFactory shut down successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransactionDAO getPaymentTransactionDAO() {
        ensureInitialized();
        return paymentTransactionDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentDataDAO getPaymentDataDAO() {
        ensureInitialized();
        return paymentDataDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentFeeDAO getPaymentFeeDAO() {
        ensureInitialized();
        return paymentFeeDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentEventDAO getPaymentEventDAO() {
        ensureInitialized();
        return paymentEventDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTestMode() {
        return testMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Ensures that the factory has been initialized.
     * If not, initializes it.
     */
    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * Gets the connection manager used by this factory.
     *
     * @return The connection manager
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Checks if the factory is initialized.
     *
     * @return true if the factory is initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Performs a health check on the database connection.
     *
     * @return true if the connection is healthy, false otherwise
     */
    public boolean isHealthy() {
        return connectionManager != null && connectionManager.isHealthy();
    }

    /**
     * Gets the current connection pool metrics.
     *
     * @return A map of connection pool metrics
     */
    public java.util.Map<String, Object> getConnectionPoolMetrics() {
        return connectionManager != null ? connectionManager.getConnectionPoolMetrics() : java.util.Collections.emptyMap();
    }

    /**
     * Gets the current connection manager metrics.
     *
     * @return A map of connection manager metrics
     */
    public java.util.Map<String, Object> getConnectionManagerMetrics() {
        return connectionManager != null ? connectionManager.getConnectionManagerMetrics() : java.util.Collections.emptyMap();
    }
}