FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY ./target/simpleobject.jar app.jar
COPY ./src/main/resources/application.properties ./application.properties
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]