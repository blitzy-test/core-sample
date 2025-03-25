package io.briklabs.sample.rest.base;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import io.briklabs.sample.App;
import io.briklabs.sample.config.ConfigSource;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource that provides health check and status information for the application.
 * This endpoint has been enhanced to include payment system health information
 * and connection pool statistics.
 */
@Path("up")
public class StatusResource {

    private static final Logger logger = LoggerFactory.getLogger(StatusResource.class);
    private static final String PAYMENT_MODULE_VERSION = "1.0.0";
    private static final String PAYMENT_HIKARI_POOL_NAME = "PaymentHikariPool";
    
    private final long start;

    /**
     * Initializes the status resource with the current timestamp.
     */
    public StatusResource() {
        start = System.currentTimeMillis();
    }

    /**
     * Retrieves the health status of the application including payment system health
     * and connection pool statistics.
     * 
     * @param uriInfo URI information for the request
     * @return Response containing health status information
     */
    @GET
    @Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
    public Response get(@Context UriInfo uriInfo) {
        long uptime = System.currentTimeMillis() - start;
        
        // For text/plain requests, return simple uptime
        if (uriInfo.getAcceptableMediaTypes().contains(MediaType.TEXT_PLAIN_TYPE)) {
            return Response.ok(uptime).build();
        }
        
        // For JSON requests, return comprehensive health information
        Map<String, Object> healthInfo = buildHealthResponse(uptime);
        return Response.ok(healthInfo).build();
    }
    
    /**
     * Builds a comprehensive health response including payment system health
     * and connection pool statistics.
     * 
     * @param uptime The application uptime in milliseconds
     * @return Map containing health information
     */
    private Map<String, Object> buildHealthResponse(long uptime) {
        Map<String, Object> response = new HashMap<>();
        
        // Basic application information
        response.put("status", "UP");
        response.put("uptime", uptime);
        response.put("uptimeFormatted", formatUptime(uptime));
        
        // Version information
        Map<String, Object> versions = new HashMap<>();
        versions.put("application", "1.0.0");
        versions.put("paymentModule", PAYMENT_MODULE_VERSION);
        response.put("versions", versions);
        
        // Component health information
        Map<String, Object> components = new HashMap<>();
        
        // Add core component status
        components.put("core", Map.of("status", "UP"));
        
        // Add payment system health information
        components.put("payment", getPaymentSystemHealth());
        
        response.put("components", components);
        
        return response;
    }
    
    /**
     * Retrieves payment system health information including connection pool status
     * and transaction metrics.
     * 
     * @return Map containing payment system health information
     */
    private Map<String, Object> getPaymentSystemHealth() {
        Map<String, Object> paymentHealth = new HashMap<>();
        
        // Set basic payment system status
        paymentHealth.put("status", "UP");
        
        // Add connection pool statistics
        paymentHealth.put("connectionPool", getConnectionPoolStats());
        
        // Add transaction metrics
        paymentHealth.put("transactionMetrics", getTransactionMetrics());
        
        // Add query performance indicators
        paymentHealth.put("queryPerformance", getQueryPerformanceIndicators());
        
        return paymentHealth;
    }
    
    /**
     * Retrieves Hikari connection pool statistics for the payment database.
     * 
     * @return Map containing connection pool statistics
     */
    private Map<String, Object> getConnectionPoolStats() {
        Map<String, Object> poolStats = new HashMap<>();
        
        try {
            // Try to get HikariCP pool metrics through JMX
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + PAYMENT_HIKARI_POOL_NAME + ")");
            
            if (mBeanServer.isRegistered(poolName)) {
                HikariPoolMXBean poolProxy = JMX.newMXBeanProxy(
                    mBeanServer, poolName, HikariPoolMXBean.class);
                
                poolStats.put("activeConnections", poolProxy.getActiveConnections());
                poolStats.put("idleConnections", poolProxy.getIdleConnections());
                poolStats.put("totalConnections", poolProxy.getTotalConnections());
                poolStats.put("threadsAwaitingConnection", poolProxy.getThreadsAwaitingConnection());
                poolStats.put("status", "healthy");
            } else {
                // Fallback if JMX metrics are not available
                poolStats.put("status", "unknown");
                poolStats.put("message", "Connection pool metrics not available via JMX");
            }
        } catch (Exception e) {
            logger.warn("Error retrieving connection pool metrics", e);
            poolStats.put("status", "unknown");
            poolStats.put("message", "Error retrieving connection pool metrics: " + e.getMessage());
        }
        
        return poolStats;
    }
    
    /**
     * Retrieves payment transaction metrics including volume, success rate, and processing time.
     * Note: In a real implementation, these metrics would be collected from a metrics service.
     * 
     * @return Map containing transaction metrics
     */
    private Map<String, Object> getTransactionMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // In a real implementation, these values would be retrieved from a metrics service
        // For now, we'll provide placeholder values
        
        // Transaction volume by status
        Map<String, Integer> volumeByStatus = new HashMap<>();
        volumeByStatus.put("CREATED", 120);
        volumeByStatus.put("AUTHORIZED", 85);
        volumeByStatus.put("CAPTURED", 65);
        volumeByStatus.put("SETTLED", 60);
        volumeByStatus.put("FAILED", 5);
        volumeByStatus.put("REFUNDED", 10);
        metrics.put("volume", volumeByStatus);
        
        // Success rate
        metrics.put("successRate", 95.8); // percentage
        
        // Processing time (in milliseconds)
        Map<String, Object> processingTime = new HashMap<>();
        processingTime.put("average", 145);
        processingTime.put("p95", 320);
        processingTime.put("p99", 450);
        metrics.put("processingTime", processingTime);
        
        // Last hour transaction count
        metrics.put("lastHourTransactions", 42);
        
        return metrics;
    }
    
    /**
     * Retrieves basic performance indicators for payment query operations.
     * Note: In a real implementation, these metrics would be collected from a metrics service.
     * 
     * @return Map containing query performance indicators
     */
    private Map<String, Object> getQueryPerformanceIndicators() {
        Map<String, Object> performance = new HashMap<>();
        
        // In a real implementation, these values would be retrieved from a metrics service
        // For now, we'll provide placeholder values
        
        // Average query execution time by operation type (in milliseconds)
        Map<String, Integer> avgExecutionTime = new HashMap<>();
        avgExecutionTime.put("transactionLookup", 12);
        avgExecutionTime.put("transactionList", 45);
        avgExecutionTime.put("complexFilter", 85);
        avgExecutionTime.put("eventHistory", 30);
        performance.put("avgExecutionTime", avgExecutionTime);
        
        // Query throughput (queries per second)
        performance.put("throughput", 28.5);
        
        // Cache hit ratio (percentage)
        performance.put("cacheHitRatio", 78.2);
        
        return performance;
    }
    
    /**
     * Formats uptime in a human-readable format.
     * 
     * @param uptimeMillis Uptime in milliseconds
     * @return Formatted uptime string
     */
    private String formatUptime(long uptimeMillis) {
        long days = TimeUnit.MILLISECONDS.toDays(uptimeMillis);
        uptimeMillis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis);
        uptimeMillis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis);
        uptimeMillis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis);
        
        return String.format("%d days, %d hours, %d minutes, %d seconds", 
                days, hours, minutes, seconds);
    }
}