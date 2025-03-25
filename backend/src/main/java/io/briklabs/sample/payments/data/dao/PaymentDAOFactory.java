package io.briklabs.sample.payments.data.dao;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Factory interface for creating and managing DAO instances with proper dependency injection.
 * <p>
 * This factory centralizes the instantiation of payment DAO implementations, managing their
 * dependencies and lifecycle. It provides access to all payment DAOs through a unified interface
 * while hiding implementation details, enabling clean separation between data access usage and
 * implementation while supporting potential alternative implementations or testing mocks.
 * </p>
 */
public interface PaymentDAOFactory {
    
    /**
     * Initializes all DAO instances.
     * <p>
     * This method ensures that all DAO implementations are properly initialized
     * and ready for use. It should be called during application startup.
     * </p>
     */
    void initialize();
    
    /**
     * Releases all resources held by the DAO implementations.
     * <p>
     * This method ensures proper cleanup of resources when the DAOs are no longer
     * needed. It should be called during application shutdown.
     * </p>
     */
    void shutdown();
    
    /**
     * Gets the PaymentTransactionDAO implementation.
     * <p>
     * This DAO provides access to payment transaction data, supporting operations
     * such as creation, retrieval, updates, and complex querying capabilities.
     * </p>
     *
     * @return The PaymentTransactionDAO implementation
     */
    PaymentTransactionDAO getPaymentTransactionDAO();
    
    /**
     * Gets the PaymentDataDAO implementation.
     * <p>
     * This DAO provides access to payment method data, supporting operations
     * for storing and retrieving payment instrument details.
     * </p>
     *
     * @return The PaymentDataDAO implementation
     */
    PaymentDataDAO getPaymentDataDAO();
    
    /**
     * Gets the PaymentFeeDAO implementation.
     * <p>
     * This DAO provides access to payment fee data, supporting operations
     * for fee tracking, reporting, and analysis.
     * </p>
     *
     * @return The PaymentFeeDAO implementation
     */
    PaymentFeeDAO getPaymentFeeDAO();
    
    /**
     * Gets the PaymentEventDAO implementation.
     * <p>
     * This DAO provides access to payment event data, supporting operations
     * for comprehensive event tracking and audit trails.
     * </p>
     *
     * @return The PaymentEventDAO implementation
     */
    PaymentEventDAO getPaymentEventDAO();
    
    /**
     * Checks if the factory is operating in test mode.
     * <p>
     * Test mode may use different implementations or configurations
     * suitable for testing environments.
     * </p>
     *
     * @return true if the factory is in test mode, false otherwise
     */
    boolean isTestMode();
    
    /**
     * Gets the HikariDataSource used by this factory.
     * <p>
     * This method provides access to the underlying connection pool
     * for advanced configuration or monitoring.
     * </p>
     *
     * @return The HikariDataSource instance
     */
    HikariDataSource getDataSource();
    
    /**
     * Creates a new factory instance with the specified data source.
     * <p>
     * This static factory method creates a new PaymentDAOFactory instance
     * with the provided HikariDataSource for database connectivity.
     * </p>
     *
     * @param dataSource The HikariDataSource to use for database connectivity
     * @return A new PaymentDAOFactory instance
     */
    static PaymentDAOFactory create(HikariDataSource dataSource) {
        try {
            // Use reflection to create the implementation class to avoid direct dependency
            Class<?> implClass = Class.forName("io.briklabs.sample.payments.data.dao.impl.PaymentDAOFactoryImpl");
            return (PaymentDAOFactory) implClass.getConstructor(HikariDataSource.class).newInstance(dataSource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PaymentDAOFactory instance", e);
        }
    }
    
    /**
     * Creates a new factory instance with the specified data source and test mode flag.
     * <p>
     * This static factory method creates a new PaymentDAOFactory instance
     * with the provided HikariDataSource and test mode flag.
     * </p>
     *
     * @param dataSource The HikariDataSource to use for database connectivity
     * @param testMode Flag indicating whether to use test implementations
     * @return A new PaymentDAOFactory instance
     */
    static PaymentDAOFactory create(HikariDataSource dataSource, boolean testMode) {
        try {
            // Use reflection to create the implementation class to avoid direct dependency
            Class<?> implClass = Class.forName("io.briklabs.sample.payments.data.dao.impl.PaymentDAOFactoryImpl");
            return (PaymentDAOFactory) implClass.getConstructor(HikariDataSource.class, boolean.class)
                    .newInstance(dataSource, testMode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PaymentDAOFactory instance", e);
        }
    }
}