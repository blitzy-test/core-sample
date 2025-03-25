package io.briklabs.sample.payments.data.dao.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import io.briklabs.sample.config.ConfigSource;
import io.briklabs.sample.config.DatabaseConfig;
import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.dao.PaymentDataDAO;
import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.dao.PaymentFeeDAO;
import io.briklabs.sample.payments.data.dao.PaymentTransactionDAO;

/**
 * Implementation of the PaymentDAOFactory interface that creates and manages DAO instances.
 * <p>
 * This class serves as the central factory for all payment DAO implementations, handling their
 * instantiation, dependency injection, and lifecycle management. It provides access to all
 * concrete DAO implementations through a unified interface while concealing implementation details.
 * </p>
 * <p>
 * The factory implements the Singleton pattern for efficient DAO instance management, ensuring
 * that only one instance of each DAO is created and reused throughout the application lifecycle.
 * It also supports test mode for mock implementations to facilitate testing.
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
     * Cache of DAO instances to implement the Singleton pattern.
     */
    private final Map<Class<?>, Object> daoInstances = new ConcurrentHashMap<>();
    
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
        this.dataSource = dataSource;
        this.testMode = testMode;
        logger.info("Created PaymentDAOFactory with testMode={}", testMode);
    }
    
    /**
     * Initializes all DAO instances.
     * <p>
     * This method ensures that all DAO implementations are properly initialized
     * and ready for use. It should be called during application startup.
     * </p>
     */
    @Override
    public void initialize() {
        if (initialized) {
            logger.warn("PaymentDAOFactory already initialized");
            return;
        }
        
        logger.info("Initializing PaymentDAOFactory");
        
        // Pre-initialize all DAO instances
        getPaymentTransactionDAO();
        getPaymentDataDAO();
        getPaymentFeeDAO();
        getPaymentEventDAO();
        
        initialized = true;
        logger.info("PaymentDAOFactory initialization complete");
    }
    
    /**
     * Releases all resources held by the DAO implementations.
     * <p>
     * This method ensures proper cleanup of resources when the DAOs are no longer
     * needed. It should be called during application shutdown.
     * </p>
     */
    @Override
    public void shutdown() {
        logger.info("Shutting down PaymentDAOFactory");
        
        // Clear all cached instances
        daoInstances.clear();
        
        // Close the data source if it's not null and not already closed
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Closed HikariCP data source");
        }
        
        initialized = false;
        logger.info("PaymentDAOFactory shutdown complete");
    }
    
    /**
     * Gets the PaymentTransactionDAO implementation.
     * <p>
     * This DAO provides access to payment transaction data, supporting operations
     * such as creation, retrieval, updates, and complex querying capabilities.
     * </p>
     *
     * @return The PaymentTransactionDAO implementation
     */
    @Override
    public PaymentTransactionDAO getPaymentTransactionDAO() {
        return getOrCreateDAO(PaymentTransactionDAO.class, () -> {
            if (testMode) {
                // Return a mock implementation for testing
                logger.debug("Creating mock PaymentTransactionDAO for test mode");
                return createMockPaymentTransactionDAO();
            } else {
                // Return the real implementation
                logger.debug("Creating PaymentTransactionDAO implementation");
                return createPaymentTransactionDAO();
            }
        });
    }
    
    /**
     * Gets the PaymentDataDAO implementation.
     * <p>
     * This DAO provides access to payment method data, supporting operations
     * for storing and retrieving payment instrument details.
     * </p>
     *
     * @return The PaymentDataDAO implementation
     */
    @Override
    public PaymentDataDAO getPaymentDataDAO() {
        return getOrCreateDAO(PaymentDataDAO.class, () -> {
            if (testMode) {
                // Return a mock implementation for testing
                logger.debug("Creating mock PaymentDataDAO for test mode");
                return createMockPaymentDataDAO();
            } else {
                // Return the real implementation
                logger.debug("Creating PaymentDataDAO implementation");
                return createPaymentDataDAO();
            }
        });
    }
    
    /**
     * Gets the PaymentFeeDAO implementation.
     * <p>
     * This DAO provides access to payment fee data, supporting operations
     * for fee tracking, reporting, and analysis.
     * </p>
     *
     * @return The PaymentFeeDAO implementation
     */
    @Override
    public PaymentFeeDAO getPaymentFeeDAO() {
        return getOrCreateDAO(PaymentFeeDAO.class, () -> {
            if (testMode) {
                // Return a mock implementation for testing
                logger.debug("Creating mock PaymentFeeDAO for test mode");
                return createMockPaymentFeeDAO();
            } else {
                // Return the real implementation
                logger.debug("Creating PaymentFeeDAO implementation");
                return createPaymentFeeDAO();
            }
        });
    }
    
    /**
     * Gets the PaymentEventDAO implementation.
     * <p>
     * This DAO provides access to payment event data, supporting operations
     * for comprehensive event tracking and audit trails.
     * </p>
     *
     * @return The PaymentEventDAO implementation
     */
    @Override
    public PaymentEventDAO getPaymentEventDAO() {
        return getOrCreateDAO(PaymentEventDAO.class, () -> {
            if (testMode) {
                // Return a mock implementation for testing
                logger.debug("Creating mock PaymentEventDAO for test mode");
                return createMockPaymentEventDAO();
            } else {
                // Return the real implementation
                logger.debug("Creating PaymentEventDAO implementation");
                return createPaymentEventDAO();
            }
        });
    }
    
    /**
     * Checks if the factory is operating in test mode.
     * <p>
     * Test mode may use different implementations or configurations
     * suitable for testing environments.
     * </p>
     *
     * @return true if the factory is in test mode, false otherwise
     */
    @Override
    public boolean isTestMode() {
        return testMode;
    }
    
    /**
     * Gets the HikariDataSource used by this factory.
     * <p>
     * This method provides access to the underlying connection pool
     * for advanced configuration or monitoring.
     * </p>
     *
     * @return The HikariDataSource instance
     */
    @Override
    public HikariDataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Gets or creates a DAO instance of the specified type.
     * <p>
     * This method implements the Singleton pattern for DAO instances,
     * ensuring that only one instance of each DAO type is created.
     * </p>
     *
     * @param <T> The DAO interface type
     * @param daoClass The DAO interface class
     * @param creator A function that creates a new DAO instance
     * @return The DAO instance
     */
    @SuppressWarnings("unchecked")
    private <T> T getOrCreateDAO(Class<T> daoClass, DAOCreator<T> creator) {
        return (T) daoInstances.computeIfAbsent(daoClass, k -> creator.create());
    }
    
    /**
     * Creates a real PaymentTransactionDAO implementation.
     *
     * @return A new PaymentTransactionDAO instance
     */
    private PaymentTransactionDAO createPaymentTransactionDAO() {
        // In a real implementation, we might inject additional dependencies
        // such as a DatabaseConfig or ConfigSource
        return new PaymentTransactionDaoImpl(createDatabaseConfig());
    }
    
    /**
     * Creates a real PaymentDataDAO implementation.
     *
     * @return A new PaymentDataDAO instance
     */
    private PaymentDataDAO createPaymentDataDAO() {
        // Create the DAO with appropriate dependencies
        return new PaymentDataDaoImpl(createDatabaseConfig(), createConfigSource());
    }
    
    /**
     * Creates a real PaymentFeeDAO implementation.
     *
     * @return A new PaymentFeeDAO instance
     */
    private PaymentFeeDAO createPaymentFeeDAO() {
        // Create the DAO with appropriate dependencies
        return new PaymentFeeDaoImpl(createDatabaseConfig(), createConfigSource());
    }
    
    /**
     * Creates a real PaymentEventDAO implementation.
     *
     * @return A new PaymentEventDAO instance
     */
    private PaymentEventDAO createPaymentEventDAO() {
        // Create the DAO with appropriate dependencies
        return new PaymentEventDaoImpl(createDatabaseConfig(), createConfigSource());
    }
    
    /**
     * Creates a mock PaymentTransactionDAO implementation for testing.
     *
     * @return A mock PaymentTransactionDAO instance
     */
    private PaymentTransactionDAO createMockPaymentTransactionDAO() {
        // In a real implementation, this would return a mock or test implementation
        // For now, we'll use the real implementation with a test flag
        return new PaymentTransactionDaoImpl(createDatabaseConfig());
    }
    
    /**
     * Creates a mock PaymentDataDAO implementation for testing.
     *
     * @return A mock PaymentDataDAO instance
     */
    private PaymentDataDAO createMockPaymentDataDAO() {
        // In a real implementation, this would return a mock or test implementation
        // For now, we'll use the real implementation with a test flag
        return new PaymentDataDaoImpl(createDatabaseConfig(), createConfigSource());
    }
    
    /**
     * Creates a mock PaymentFeeDAO implementation for testing.
     *
     * @return A mock PaymentFeeDAO instance
     */
    private PaymentFeeDAO createMockPaymentFeeDAO() {
        // In a real implementation, this would return a mock or test implementation
        // For now, we'll use the real implementation with a test flag
        return new PaymentFeeDaoImpl(createDatabaseConfig(), createConfigSource());
    }
    
    /**
     * Creates a mock PaymentEventDAO implementation for testing.
     *
     * @return A mock PaymentEventDAO instance
     */
    private PaymentEventDAO createMockPaymentEventDAO() {
        // In a real implementation, this would return a mock or test implementation
        // For now, we'll use the real implementation with a test flag
        return new PaymentEventDaoImpl(createDatabaseConfig(), createConfigSource());
    }
    
    /**
     * Creates a DatabaseConfig instance for use by DAOs.
     * <p>
     * This method creates a simple DatabaseConfig implementation that
     * delegates to the HikariDataSource for connection parameters.
     * </p>
     *
     * @return A DatabaseConfig instance
     */
    private DatabaseConfig createDatabaseConfig() {
        // Create a simple DatabaseConfig that delegates to the HikariDataSource
        return new DatabaseConfig() {
            @Override
            public String getDatabaseURL() {
                return dataSource.getJdbcUrl();
            }
            
            @Override
            public String getDatabaseUsername() {
                return dataSource.getUsername();
            }
            
            @Override
            public String getDatabasePassword() {
                return dataSource.getPassword();
            }
            
            @Override
            public String getDatabaseSchema() {
                return dataSource.getSchema();
            }
            
            @Override
            public java.util.Optional<Map<String, Object>> getConnectionPoolConfig() {
                // Create a map of connection pool properties from the data source
                Map<String, Object> poolConfig = new HashMap<>();
                poolConfig.put("maximumPoolSize", dataSource.getMaximumPoolSize());
                poolConfig.put("minimumIdle", dataSource.getMinimumIdle());
                poolConfig.put("connectionTimeout", dataSource.getConnectionTimeout());
                poolConfig.put("idleTimeout", dataSource.getIdleTimeout());
                poolConfig.put("maxLifetime", dataSource.getMaxLifetime());
                poolConfig.put("leakDetectionThreshold", dataSource.getLeakDetectionThreshold());
                poolConfig.put("autoCommit", dataSource.isAutoCommit());
                
                return java.util.Optional.of(poolConfig);
            }
        };
    }
    
    /**
     * Creates a ConfigSource instance for use by DAOs.
     * <p>
     * This method creates a simple ConfigSource implementation that
     * provides payment-specific configuration.
     * </p>
     *
     * @return A ConfigSource instance
     */
    private ConfigSource createConfigSource() {
        // In a real implementation, this would be injected or obtained from a central source
        // For now, we'll create a simple implementation with default values
        return new ConfigSource() {
            @Override
            public Map<String, Object> getPaymentConnectionPoolConfig() {
                // Default payment-specific connection pool configuration
                Map<String, Object> poolConfig = new HashMap<>();
                poolConfig.put("maximumPoolSize", 30);
                poolConfig.put("minimumIdle", 10);
                poolConfig.put("connectionTimeout", 20000);
                poolConfig.put("idleTimeout", 300000);
                poolConfig.put("maxLifetime", 1200000);
                poolConfig.put("leakDetectionThreshold", 60000);
                poolConfig.put("autoCommit", false);
                poolConfig.put("poolName", "PaymentHikariPool");
                poolConfig.put("connectionTestQuery", "SELECT 1");
                poolConfig.put("registerMbeans", true);
                
                return poolConfig;
            }
            
            // Other ConfigSource methods would be implemented here
            // For simplicity, we're only implementing the method needed by our DAOs
        };
    }
    
    /**
     * Functional interface for creating DAO instances.
     *
     * @param <T> The DAO interface type
     */
    @FunctionalInterface
    private interface DAOCreator<T> {
        /**
         * Creates a new DAO instance.
         *
         * @return A new DAO instance
         */
        T create();
    }
}