FROM openjdk:11.0.7
ARG JAR_FILE=target/kafka-rest-ingest-*.jar
COPY ${JAR_FILE} app.jar


EXPOSE 9000

ENTRYPOINT ["java","-jar","/app.jar"]
