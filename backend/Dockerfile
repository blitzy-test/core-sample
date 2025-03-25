FROM eclipse-temurin:21-jre

EXPOSE 5900

ENV JAVA_OPTS=""
ENV HIKARI_CONFIG=""
ENV PAYMENT_OPTS="-Dpayment.processing.threads=4 -Dpayment.transaction.timeout=300000"

ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -Dhikaricp.configurationFile=${HIKARI_CONFIG} ${PAYMENT_OPTS} ${JAVA_OPTS} -jar /brik/app.jar

ADD target/lib /brik/lib
ARG JAR_FILE
ADD target/${JAR_FILE} /brik/app.jar