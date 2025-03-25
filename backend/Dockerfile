FROM eclipse-temurin:21-jre

EXPOSE 5900

ENV JAVA_OPTS=""
ENV HIKARI_CONFIG=""

ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -Dhikaricp.configurationFile=$HIKARI_CONFIG $JAVA_OPTS -jar /brik/app.jar

ADD target/lib /brik/lib
ARG JAR_FILE
ADD target/${JAR_FILE} /brik/app.jar