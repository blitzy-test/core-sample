FROM eclipse-temurin:21-jre

EXPOSE 5900

# Environment variables for configuration
ENV JAVA_OPTS=""
ENV BRIK_CONFIG=""
ENV HIKARI_CONFIG=""
ENV PAYMENT_CONFIG=""

# Command to run the application with HikariCP configuration
ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom \
                     -Dhikaricp.configurationFile=$HIKARI_CONFIG \
                     $JAVA_OPTS \
                     -jar /brik/app.jar

# Add application files
ADD target/lib /brik/lib
ARG JAR_FILE
ADD target/${JAR_FILE} /brik/app.jar

# Documentation for container configuration
# This container supports the following environment variables:
# - JAVA_OPTS: JVM configuration parameters
# - BRIK_CONFIG: Path to the main application configuration file
# - HIKARI_CONFIG: Path to HikariCP connection pool configuration for payment processing
# - PAYMENT_CONFIG: Path to payment module specific configuration
#
# HikariCP connection pooling is used for efficient database operations with the payment module.
# Recommended settings for production:
# - maximumPoolSize: 30-50 connections depending on transaction volume
# - minimumIdle: 10-15 connections for immediate availability
# - connectionTimeout: 20000ms to prevent transaction bottlenecks
# - idleTimeout: 300000ms for efficient resource management
# - maxLifetime: 1200000ms to prevent stale connections
# - leakDetectionThreshold: 60000ms to identify connection leaks
#
# For optimal payment processing performance, consider these JVM parameters:
# -Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200