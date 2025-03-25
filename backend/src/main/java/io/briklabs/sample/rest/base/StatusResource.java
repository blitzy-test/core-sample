package io.briklabs.sample.rest.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.briklabs.sample.payments.data.ConnectionManager;
import io.briklabs.sample.payments.data.HikariCPConfig;
import io.briklabs.sample.config.ConfigSource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource that provides health check and status information for the application.
 * Includes payment system health information and connection pool statistics.
 */
@Path("up")
public class StatusResource {

    private static final Logger logger = LoggerFactory.getLogger(StatusResource.class);
    private static final String VERSION = "1.0.0";
    private static final String PAYMENT_MODULE_VERSION = "1.0.0";

    private final long start;
    private final ConfigSource configSource;
    private HikariCPConfig hikariCPConfig;
    private ConnectionManager connectionManager;

    /**
     * Constructs a new StatusResource.
     */
    @Inject
    public StatusResource(ConfigSource configSource) {
        this.start = System.currentTimeMillis();
        this.configSource = configSource;
        
        try {
            // Initialize HikariCPConfig and ConnectionManager for payment system health monitoring
            this.hikariCPConfig = new HikariCPConfig(configSource);
            this.connectionManager = ConnectionManager.getInstance(configSource);
            logger.info("Payment system health monitoring initialized successfully");
        } catch (Exception e) {
            logger.warn("Failed to initialize payment system health monitoring: {}", e.getMessage());
        }
    }

    /**
     * Returns the application status including uptime and payment system health information.
     * Supports both plain text and JSON formats based on Accept header.
     *
     * @return Response containing status information
     */
    @GET
    @Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
    public Response get() {
        long uptime = System.currentTimeMillis() - start;
        
        // For plain text requests, return simple uptime
        if (MediaType.TEXT_PLAIN.equals(getPreferredMediaType())) {
            return Response.ok(formatUptimeText(uptime)).build();
        }
        
        // For JSON requests, return detailed status including payment system health
        StatusResponse status = buildStatusResponse(uptime);
        return Response.ok(status).build();
    }
    
    /**
     * Builds a comprehensive status response including payment system health information.
     *
     * @param uptime The application uptime in milliseconds
     * @return StatusResponse object with detailed health information
     */
    private StatusResponse buildStatusResponse(long uptime) {
        StatusResponse status = new StatusResponse();
        status.setStatus("UP");
        status.setUptime(uptime);
        status.setFormattedUptime(formatUptime(uptime));
        status.setVersion(VERSION);
        
        // Add payment system health information if available
        if (hikariCPConfig != null && connectionManager != null) {
            try {
                PaymentHealthInfo paymentHealth = new PaymentHealthInfo();
                paymentHealth.setStatus("UP");
                paymentHealth.setVersion(PAYMENT_MODULE_VERSION);
                
                // Add connection pool statistics
                ConnectionPoolStats poolStats = new ConnectionPoolStats();
                poolStats.setStatus(hikariCPConfig.getHealthStatus());
                poolStats.setMaximumPoolSize(hikariCPConfig.getMaximumPoolSize());
                poolStats.setActiveConnections(hikariCPConfig.getActiveConnections());
                poolStats.setIdleConnections(hikariCPConfig.getIdleConnections());
                poolStats.setThreadsAwaiting(hikariCPConfig.getThreadsAwaitingConnection());
                poolStats.setConnectionTimeout(hikariCPConfig.getConnectionTimeout());
                poolStats.setConnectionTimeoutFormatted(HikariCPConfig.formatDuration(hikariCPConfig.getConnectionTimeout()));
                poolStats.setValidationTimeout(hikariCPConfig.getValidationTimeout());
                poolStats.setValidationTimeoutFormatted(HikariCPConfig.formatDuration(hikariCPConfig.getValidationTimeout()));
                poolStats.setLeakDetectionThreshold(hikariCPConfig.getLeakDetectionThreshold());
                poolStats.setLeakDetectionThresholdFormatted(HikariCPConfig.formatDuration(hikariCPConfig.getLeakDetectionThreshold()));
                
                paymentHealth.setConnectionPool(poolStats);
                
                // Add payment transaction metrics
                TransactionMetrics metrics = new TransactionMetrics();
                metrics.setTotalTransactions(connectionManager.getTotalTransactions());
                metrics.setFailedTransactions(connectionManager.getFailedTransactions());
                metrics.setSuccessRate(connectionManager.getTransactionSuccessRate());
                metrics.setAverageProcessingTimeMs(connectionManager.getAverageTransactionTimeMs());
                metrics.setAverageConnectionWaitTimeMs(connectionManager.getAverageConnectionWaitTimeMs());
                
                paymentHealth.setTransactionMetrics(metrics);
                
                // Add query performance indicators
                Map<String, Object> queryPerformance = new HashMap<>();
                queryPerformance.put("averageQueryTimeMs", connectionManager.getAverageConnectionWaitTimeMs());
                queryPerformance.put("activeQueries", connectionManager.getConnectionsForOperation("query"));
                
                paymentHealth.setQueryPerformance(queryPerformance);
                
                status.setPaymentSystem(paymentHealth);
            } catch (Exception e) {
                logger.warn("Error collecting payment system health information: {}", e.getMessage());
                
                PaymentHealthInfo paymentHealth = new PaymentHealthInfo();
                paymentHealth.setStatus("DEGRADED");
                paymentHealth.setErrorMessage("Error collecting health information: " + e.getMessage());
                status.setPaymentSystem(paymentHealth);
            }
        }
        
        return status;
    }
    
    /**
     * Formats uptime in a human-readable format for text response.
     *
     * @param uptime The application uptime in milliseconds
     * @return Formatted uptime string
     */
    private String formatUptimeText(long uptime) {
        return "UP " + formatUptime(uptime);
    }
    
    /**
     * Formats uptime in a human-readable format (days, hours, minutes, seconds).
     *
     * @param millis The time in milliseconds
     * @return Formatted time string
     */
    private String formatUptime(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        
        return sb.toString();
    }
    
    /**
     * Gets the preferred media type from the Accept header.
     *
     * @return The preferred media type
     */
    private String getPreferredMediaType() {
        // In a real implementation, this would check the Accept header
        // For simplicity, we're defaulting to JSON
        return MediaType.APPLICATION_JSON;
    }
    
    /**
     * Status response class for JSON serialization.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusResponse {
        private String status;
        private long uptime;
        private String formattedUptime;
        private String version;
        private PaymentHealthInfo paymentSystem;
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public long getUptime() {
            return uptime;
        }
        
        public void setUptime(long uptime) {
            this.uptime = uptime;
        }
        
        public String getFormattedUptime() {
            return formattedUptime;
        }
        
        public void setFormattedUptime(String formattedUptime) {
            this.formattedUptime = formattedUptime;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        @JsonProperty("payment")
        public PaymentHealthInfo getPaymentSystem() {
            return paymentSystem;
        }
        
        public void setPaymentSystem(PaymentHealthInfo paymentSystem) {
            this.paymentSystem = paymentSystem;
        }
    }
    
    /**
     * Payment system health information class for JSON serialization.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentHealthInfo {
        private String status;
        private String version;
        private String errorMessage;
        private ConnectionPoolStats connectionPool;
        private TransactionMetrics transactionMetrics;
        private Map<String, Object> queryPerformance;
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public ConnectionPoolStats getConnectionPool() {
            return connectionPool;
        }
        
        public void setConnectionPool(ConnectionPoolStats connectionPool) {
            this.connectionPool = connectionPool;
        }
        
        public TransactionMetrics getTransactionMetrics() {
            return transactionMetrics;
        }
        
        public void setTransactionMetrics(TransactionMetrics transactionMetrics) {
            this.transactionMetrics = transactionMetrics;
        }
        
        public Map<String, Object> getQueryPerformance() {
            return queryPerformance;
        }
        
        public void setQueryPerformance(Map<String, Object> queryPerformance) {
            this.queryPerformance = queryPerformance;
        }
    }
    
    /**
     * Connection pool statistics class for JSON serialization.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConnectionPoolStats {
        private String status;
        private int maximumPoolSize;
        private int activeConnections;
        private int idleConnections;
        private int threadsAwaiting;
        private long connectionTimeout;
        private String connectionTimeoutFormatted;
        private long validationTimeout;
        private String validationTimeoutFormatted;
        private long leakDetectionThreshold;
        private String leakDetectionThresholdFormatted;
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }
        
        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
        
        public int getActiveConnections() {
            return activeConnections;
        }
        
        public void setActiveConnections(int activeConnections) {
            this.activeConnections = activeConnections;
        }
        
        public int getIdleConnections() {
            return idleConnections;
        }
        
        public void setIdleConnections(int idleConnections) {
            this.idleConnections = idleConnections;
        }
        
        public int getThreadsAwaiting() {
            return threadsAwaiting;
        }
        
        public void setThreadsAwaiting(int threadsAwaiting) {
            this.threadsAwaiting = threadsAwaiting;
        }
        
        public long getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public String getConnectionTimeoutFormatted() {
            return connectionTimeoutFormatted;
        }
        
        public void setConnectionTimeoutFormatted(String connectionTimeoutFormatted) {
            this.connectionTimeoutFormatted = connectionTimeoutFormatted;
        }
        
        public long getValidationTimeout() {
            return validationTimeout;
        }
        
        public void setValidationTimeout(long validationTimeout) {
            this.validationTimeout = validationTimeout;
        }
        
        public String getValidationTimeoutFormatted() {
            return validationTimeoutFormatted;
        }
        
        public void setValidationTimeoutFormatted(String validationTimeoutFormatted) {
            this.validationTimeoutFormatted = validationTimeoutFormatted;
        }
        
        public long getLeakDetectionThreshold() {
            return leakDetectionThreshold;
        }
        
        public void setLeakDetectionThreshold(long leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
        }
        
        public String getLeakDetectionThresholdFormatted() {
            return leakDetectionThresholdFormatted;
        }
        
        public void setLeakDetectionThresholdFormatted(String leakDetectionThresholdFormatted) {
            this.leakDetectionThresholdFormatted = leakDetectionThresholdFormatted;
        }
    }
    
    /**
     * Transaction metrics class for JSON serialization.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TransactionMetrics {
        private int totalTransactions;
        private int failedTransactions;
        private double successRate;
        private double averageProcessingTimeMs;
        private double averageConnectionWaitTimeMs;
        
        public int getTotalTransactions() {
            return totalTransactions;
        }
        
        public void setTotalTransactions(int totalTransactions) {
            this.totalTransactions = totalTransactions;
        }
        
        public int getFailedTransactions() {
            return failedTransactions;
        }
        
        public void setFailedTransactions(int failedTransactions) {
            this.failedTransactions = failedTransactions;
        }
        
        public double getSuccessRate() {
            return successRate;
        }
        
        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }
        
        public double getAverageProcessingTimeMs() {
            return averageProcessingTimeMs;
        }
        
        public void setAverageProcessingTimeMs(double averageProcessingTimeMs) {
            this.averageProcessingTimeMs = averageProcessingTimeMs;
        }
        
        public double getAverageConnectionWaitTimeMs() {
            return averageConnectionWaitTimeMs;
        }
        
        public void setAverageConnectionWaitTimeMs(double averageConnectionWaitTimeMs) {
            this.averageConnectionWaitTimeMs = averageConnectionWaitTimeMs;
        }
    }
}